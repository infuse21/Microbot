package net.runelite.client.plugins.microbot.aiofishing;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.aiofishing.enums.WorldMode;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

@PluginDescriptor(
        name = PluginDescriptor.Default + "AIO Fishing",
        enabledByDefault = false,
        description = "All-in-one fishing with automatic level progression, banking and dropping , GE selling",
        tags = {"fishing", "skilling", "aio", "progression"},
        authors = {"Infuse"},
        version = AIOFishingPlugin.version,
        minClientVersion = "1.9.8",
        iconUrl = "https://chsami.github.io/Microbot-Hub/AIOFishingPlugin/assets/icon.png",
        cardUrl = "https://chsami.github.io/Microbot-Hub/AIOFishingPlugin/assets/card.jpg"
)
@Slf4j
public class AIOFishingPlugin extends Plugin {
    public static final String version = "1.5.0";

    @Inject
    @Getter
    private AIOFishingConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AIOFishingOverlay overlay;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private MouseManager mouseManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    @Getter
    AIOFishingScript script;

    private AIOFishingPanel panel;
    private NavigationButton navButton;
    private OverlayMouseAdapter mouseAdapter;
    /** Runs overlay button actions off the AWT event thread - see submitAction(). */
    private ExecutorService uiExecutor;

    private int startXp = 0;
    private int startLevel = 1;
    private long startTime = 0;
    private final Object catchCounterLock = new Object();
    private Map<Integer, Integer> previousInventory = Collections.emptyMap();
    /** Estimated catches per stage, in the order the stages were fished. */
    private final Map<FishingStage, Integer> caughtByStage = new LinkedHashMap<>();
    /** Stage the XP since { #stageStartXp} belongs to. */
    private FishingStage countedStage;
    private int stageStartXp;

    @Provides
    AIOFishingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AIOFishingConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }

        uiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AIOFishing-ui");
            t.setDaemon(true);
            return t;
        });

        mouseAdapter = new OverlayMouseAdapter();
        mouseManager.registerMouseListener(mouseAdapter);

        panel = injector.getInstance(AIOFishingPanel.class);
        navButton = NavigationButton.builder()
                .tooltip("AIO Fishing")
                .icon(buildIcon())
                .priority(7)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        refreshPanel();

        // Deliberately does NOT start the script - the user starts it from the
        // overlay's START button. Enabling the plugin only arms the UI.
        resetSessionCounters();
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        if (overlayManager != null) {
            overlayManager.remove(overlay);
        }
        if (mouseAdapter != null) {
            mouseManager.unregisterMouseListener(mouseAdapter);
            mouseAdapter = null;
        }
        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        if (uiExecutor != null) {
            uiExecutor.shutdownNow();
            uiExecutor = null;
        }
    }

    // ------------------------------------------------------------- script control

    /**
     * Runs a control action off the caller's thread.
     *
     * <p>Overlay buttons are dispatched from the AWT event thread. Doing game work there
     * is a deadlock: calls like {@code Rs2Bank.isOpen()} hop to the client thread and block
     * waiting for it, which stalls the client loop and makes every other thread's
     * client-thread call time out. So all control actions are handed to our own thread.</p>
     */
    private void submitAction(Runnable action) {
        ExecutorService executor = uiExecutor;
        if (executor == null || executor.isShutdown()) {
            return;
        }
        executor.submit(() -> {
            try {
                action.run();
            } catch (Exception ex) {
                log.error("AIO Fishing control action failed", ex);
            }
        });
    }

    public void startScript() {
        submitAction(() -> {
            if (script.isRunning()) {
                return;
            }
            resetSessionCounters();
            script.run(config);
            log.info("AIO Fishing started.");
        });
    }

    public void stopScript() {
        submitAction(() -> {
            if (!script.isRunning()) {
                return;
            }
            script.shutdown();
            log.info("AIO Fishing stopped.");
        });
    }

    public void togglePause() {
        // Just a volatile flag flip - safe anywhere, no client-thread work involved.
        if (script.isRunning()) {
            script.togglePause();
        }
    }

    public boolean isScriptRunning() {
        return script.isRunning();
    }

    public boolean isScriptPaused() {
        return script.isRunning() && script.isPaused();
    }

    private void resetSessionCounters() {
        startXp = Microbot.getClient().getSkillExperience(Skill.FISHING);
        startLevel = Rs2Player.getRealSkillLevel(Skill.FISHING);
        Map<Integer, Integer> inventory = Microbot.getClientThread().runOnClientThreadOptional(
                        () -> inventoryQuantities(Microbot.getClient().getItemContainer(InventoryID.INV)))
                .orElse(Collections.emptyMap());
        synchronized (catchCounterLock) {
            previousInventory = inventory;
            caughtByStage.clear();
            countedStage = null;
            stageStartXp = startXp;
        }
        startTime = System.currentTimeMillis();
    }

    /**
     * Reads the world we are actually logged into, for {@link WorldMode#AUTO}.
     *
     * <p>The world type is a plain flag set on login, so this stays correct across a hop
     * without any subscription - which is the whole point of preferring it to a tickbox the
     * user has to remember to flip.</p>
     */
    public static final BooleanSupplier MEMBER_WORLD = () -> {
        Client client = Microbot.getClient();
        if (client == null) {
            return false;
        }
        EnumSet<WorldType> types = client.getWorldType();
        return types != null && types.contains(WorldType.MEMBERS);
    };

    /** The configured ladder resolved to a plain members/free answer. */
    public static boolean isMembersWorld(AIOFishingConfig config) {
        return config.worldMode().isMembersWorld(MEMBER_WORLD);
    }

    /** Live quest lookup, shared by the script and UI. Cached by Rs2PlayerStateCache. */
    public static final Function<Quest, QuestState> QUEST_STATES = quest -> {
        try {
            return Rs2Player.getQuestState(quest);
        } catch (Exception e) {
            return null; // unknown -> treated as not met, which is the safe direction
        }
    };

    /** Live skill lookup for non-Fishing requirements (barbarian fishing etc.). */
    public static final Function<Skill, Integer> SKILL_LEVELS = skill -> {
        try {
            return Rs2Player.getRealSkillLevel(skill);
        } catch (Exception e) {
            return null;
        }
    };

    /**
     * Live varbit lookup for stages gated on a miniquest chapter (barbarian fishing).
     * Cache-backed via Rs2PlayerStateCache, so it is safe off the client thread.
     */
    public static final IntUnaryOperator VARBIT_VALUES = varbitId -> {
        try {
            return Microbot.getVarbitValue(varbitId);
        } catch (Exception e) {
            return -1; // unreadable -> treated as locked, the safe direction
        }
    };

    /** The stage the overlay/sidebar should display, whether or not the script is running. */
    public FishingStage resolveDisplayStage(int fishingLevel) {
        return config.autoProgress()
                ? FishingStage.bestFor(fishingLevel, isMembersWorld(config),
                        QUEST_STATES, SKILL_LEVELS, VARBIT_VALUES)
                : config.manualStage();
    }

    /** Per-stage lock reasons (null = available) so the sidebar can grey out and explain. */
    private Map<FishingStage, String> lockReasons(int fishingLevel) {
        Map<FishingStage, String> reasons = new EnumMap<>(FishingStage.class);
        for (FishingStage stage : FishingStage.values()) {
            String reason = stage.lockReason(fishingLevel, isMembersWorld(config),
                    QUEST_STATES, SKILL_LEVELS, VARBIT_VALUES);
            if (reason != null) {
                reasons.put(stage, reason);
            }
        }
        return reasons;
    }

    // -------------------------------------------------------------------- events

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.FISHING) {
            refreshPanel();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV) {
            return;
        }
        synchronized (catchCounterLock) {
            previousInventory = inventoryQuantities(event.getItemContainer());
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (AIOFishingPanel.CONFIG_GROUP.equals(event.getGroup())) {
            refreshPanel();
        }
    }

    /** Called from client-thread event handlers; the panel marshals onto the EDT itself. */
    private void refreshPanel() {
        if (panel == null) {
            return;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
        panel.update(level, resolveDisplayStage(level), isMembersWorld(config),
                config.autoProgress(), lockReasons(level), config.manualLocation(),
                config.activity(), Rs2Player.getRealSkillLevel(Skill.HUNTER),
                AerialFishing.unmetReason(SKILL_LEVELS));
    }

    // -------------------------------------------------------------------- stats

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(Skill.FISHING) - startXp;
    }

    /** Fishing levels gained since the script was started. */
    public int getLevelsGained() {
        return Math.max(0, Rs2Player.getRealSkillLevel(Skill.FISHING) - startLevel);
    }

    public int getFishCaught() {
        synchronized (catchCounterLock) {
            settleStageXp();
            return caughtByStage.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /**
     * Estimated catches per stage, keyed by that stage's fish icon for the overlay.
     *
     * <p>Kept per stage rather than as one running total because auto progression switches
     * fish mid-session: XP earned on shrimp must not be divided by a lobster's XP.</p>
     */
    public Map<Integer, Integer> getFishCaughtByItem() {
        Map<Integer, Integer> byItem = new LinkedHashMap<>();
        synchronized (catchCounterLock) {
            settleStageXp();
            for (Map.Entry<FishingStage, Integer> entry : caughtByStage.entrySet()) {
                if (entry.getValue() > 0) {
                    byItem.merge(entry.getKey().getSpot().getFishSpriteId(), entry.getValue(),
                            Integer::sum);
                }
            }
        }
        return byItem;
    }

    /**
     * Fold the XP earned since the last call into the stage that earned it.
     *
     * <p>Called from the accessors rather than from a tick handler so the figure is correct
     * whenever it is read, without another subscription. Callers hold the lock.</p>
     */
    private void settleStageXp() {
        FishingStage stage = script.getActiveStage();
        int currentXp = Microbot.getClient().getSkillExperience(Skill.FISHING);
        if (countedStage != null && currentXp > stageStartXp) {
            int caught = CatchEstimate.fromXp(countedStage, currentXp - stageStartXp);
            if (caught > 0) {
                caughtByStage.merge(countedStage, caught, Integer::sum);
                stageStartXp = currentXp;
            }
        }
        if (countedStage != stage) {
            countedStage = stage;
            stageStartXp = currentXp;
        }
    }

    public long getRuntimeMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public String getFormattedRuntime() {
        long millis = getRuntimeMillis();
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = ((millis % 3600000) % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public int getXpPerHour() {
        long runtime = getRuntimeMillis();
        if (runtime <= 0) {
            return 0;
        }
        return (int) (getXpGained() * 3600000.0 / runtime);
    }

    private static Map<Integer, Integer> inventoryQuantities(ItemContainer container) {
        if (container == null || container.getItems() == null) {
            return Collections.emptyMap();
        }
        Map<Integer, Integer> quantities = new HashMap<>();
        for (Item item : container.getItems()) {
            if (item.getId() >= 0 && item.getQuantity() > 0) {
                quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }
        return quantities;
    }

    // --------------------------------------------------------------------- misc

    /** Routes canvas clicks to the overlay so its buttons work. */
    private final class OverlayMouseAdapter extends MouseAdapter {
        @Override
        public MouseEvent mousePressed(MouseEvent event) {
            if (event.getButton() == MouseEvent.BUTTON1 && overlay.onClick(event.getPoint())) {
                event.consume();
            }
            return event;
        }

        @Override
        public MouseEvent mouseMoved(MouseEvent event) {
            overlay.onMove(event.getPoint());
            return event;
        }
    }

    private static BufferedImage buildIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Simple fish silhouette: body + tail.
            g.setColor(new Color(90, 175, 235));
            g.fillOval(2, 5, 10, 7);
            int[] tailX = {12, 15, 15};
            int[] tailY = {8, 4, 12};
            g.fillPolygon(tailX, tailY, 3);
            g.setColor(Color.WHITE);
            g.fillOval(4, 7, 2, 2);
        } finally {
            g.dispose();
        }
        return image;
    }
}
