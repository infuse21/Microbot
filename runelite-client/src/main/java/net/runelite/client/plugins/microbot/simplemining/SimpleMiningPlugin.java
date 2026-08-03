package net.runelite.client.plugins.microbot.simplemining;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.simplemining.enums.WorldMode;
import net.runelite.client.plugins.microbot.simplemining.enums.OreStage;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Simple Mining",
        enabledByDefault = false,
        description = "All-in-one mining with automatic level progression, banking, "
                + "power-mining and GE selling",
        tags = {"mining", "skilling", "aio", "progression"},
        authors = {"AI Agent"},
        version = SimpleMiningPlugin.version,
        minClientVersion = "1.9.8",
        iconUrl = "https://chsami.github.io/Microbot-Hub/SimpleMiningPlugin/assets/icon.png",
        cardUrl = "https://chsami.github.io/Microbot-Hub/SimpleMiningPlugin/assets/card.png"
)
@Slf4j
public class SimpleMiningPlugin extends Plugin {
    public static final String version = "1.5.1";

    @Inject
    @Getter
    private SimpleMiningConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private SimpleMiningOverlay overlay;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private MouseManager mouseManager;
    @Inject
    @Getter
    SimpleMiningScript script;

    private SimpleMiningPanel panel;
    private NavigationButton navButton;
    private OverlayMouseAdapter mouseAdapter;
    /** Runs overlay button actions off the AWT event thread - see submitAction(). */
    private ExecutorService uiExecutor;

    private int startXp = 0;
    private long startTime = 0;
    /**
     * When Mining XP last arrived. The script treats this as proof that a swing is actually
     * making progress, rather than guessing from how long ago it clicked. Static because the
     * script reaches this plugin statically, the same way it reads {@link #QUEST_STATES}.
     */
    @Getter
    private static volatile long lastMiningXpAt = 0;

    @Provides
    SimpleMiningConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SimpleMiningConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        uiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SimpleMining-ui");
            t.setDaemon(true);
            return t;
        });

        mouseAdapter = new OverlayMouseAdapter();
        mouseManager.registerMouseListener(mouseAdapter);

        panel = injector.getInstance(SimpleMiningPanel.class);
        navButton = NavigationButton.builder()
                .tooltip("Simple Mining")
                .icon(buildIcon())
                .priority(7)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        refreshPanel();

        // Deliberately does NOT start the script - the user starts it from the overlay.
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
     * <p>Overlay buttons arrive on the AWT event thread. Doing game work there deadlocks:
     * calls like {@code Rs2Bank.isOpen()} hop to the client thread and block waiting for it,
     * which stalls the client loop and times out every other thread's client-thread call.</p>
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
                log.error("Simple Mining control action failed", ex);
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
            log.info("Simple Mining started.");
        });
    }

    public void stopScript() {
        submitAction(() -> {
            if (!script.isRunning()) {
                return;
            }
            script.shutdown();
            log.info("Simple Mining stopped.");
        });
    }

    public void togglePause() {
        // Just a volatile flag flip - safe on any thread, no client-thread work involved.
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
        startXp = Microbot.getClient().getSkillExperience(Skill.MINING);
        startTime = System.currentTimeMillis();
    }

    /**
     * Reads the world we are actually logged into, for {@link WorldMode#AUTO}.
     *
     * <p>The world type is a plain flag set on login, so this stays correct across a hop
     * without any subscription - which is why it beats a tickbox the user has to remember
     * to flip after hopping.</p>
     */
    public static final BooleanSupplier MEMBER_WORLD = () -> {
        Client client = Microbot.getClient();
        if (client == null) {
            return false;
        }
        EnumSet<WorldType> types = client.getWorldType();
        return types != null && types.contains(WorldType.MEMBERS);
    };

    /** The configured world mode resolved to a plain members/free answer. */
    public static boolean isMembersWorld(SimpleMiningConfig config) {
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

    /** Live skill lookup for non-Mining requirements. */
    public static final Function<Skill, Integer> SKILL_LEVELS = skill -> {
        try {
            return Rs2Player.getRealSkillLevel(skill);
        } catch (Exception e) {
            return null;
        }
    };

    public OreStage resolveDisplayStage(int miningLevel) {
        if (config.smeltingRecipe().isEnabled()) {
            if (script != null && script.isRunning()) {
                return script.getActiveStage();
            }
            return config.smeltingRecipe().getPrimaryOre();
        }
        return config.autoProgress()
                ? OreStage.bestFor(miningLevel, isMembersWorld(config), QUEST_STATES, SKILL_LEVELS)
                : config.manualStage();
    }

    /** Per-stage lock reasons (null = available) so the sidebar can grey out and explain. */
    private Map<OreStage, String> lockReasons(int miningLevel) {
        Map<OreStage, String> reasons = new EnumMap<>(OreStage.class);
        for (OreStage stage : OreStage.values()) {
            String reason = stage.lockReason(miningLevel, isMembersWorld(config),
                    QUEST_STATES, SKILL_LEVELS);
            if (reason != null) {
                reasons.put(stage, reason);
            }
        }
        return reasons;
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.MINING) {
            lastMiningXpAt = System.currentTimeMillis();
            refreshPanel();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (SimpleMiningPanel.CONFIG_GROUP.equals(event.getGroup())) {
            refreshPanel();
        }
    }

    /** Called from client-thread handlers; the panel marshals onto the EDT itself. */
    private void refreshPanel() {
        if (panel == null) {
            return;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.MINING);
        panel.update(level, resolveDisplayStage(level), isMembersWorld(config),
                config.autoProgress(), lockReasons(level), config.manualLocation());
    }

    // -------------------------------------------------------------------- stats

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(Skill.MINING) - startXp;
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
            // Pickaxe: brown haft with a grey head.
            g.setColor(new Color(140, 94, 52));
            g.drawLine(5, 14, 11, 4);
            g.setColor(new Color(185, 190, 200));
            g.drawLine(7, 3, 14, 6);
            g.drawLine(7, 3, 9, 7);
            g.drawLine(14, 6, 11, 8);
        } finally {
            g.dispose();
        }
        return image;
    }
}
