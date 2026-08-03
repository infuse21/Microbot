package net.runelite.client.plugins.microbot.simplewoodcutting;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.WorldMode;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.ForestryEvents;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.TreeStage;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.EggEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.EntlingsEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.FlowersEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.FoxEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.HivesEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.LeprechaunEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.RitualEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.RootEvent;
import net.runelite.client.plugins.microbot.simplewoodcutting.forestry.StrugglingSaplingEvent;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.regex.Pattern;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Simple Woodcutting",
        enabledByDefault = false,
        description = "All-in-one woodcutting with progression, Forestry, looting, banking, "
                + "firemaking, fletching and GE selling",
        tags = {"woodcutting", "skilling", "forestry", "aio", "progression"},
        authors = {"AI Agent"},
        version = SimpleWoodcuttingPlugin.version,
        minClientVersion = "1.9.8",
        iconUrl = "https://chsami.github.io/Microbot-Hub/SimpleWoodcuttingPlugin/assets/icon.png",
        cardUrl = "https://chsami.github.io/Microbot-Hub/SimpleWoodcuttingPlugin/assets/card.png"
)
@Slf4j
public class SimpleWoodcuttingPlugin extends Plugin {
    public static final String version = "1.2.1";

    private static final Pattern SAPLING_MESSAGE =
            Pattern.compile("^The sapling seems to love the (first|second|third).*$");

    @Inject
    @Getter
    private SimpleWoodcuttingConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private SimpleWoodcuttingOverlay overlay;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private MouseManager mouseManager;
    @Inject
    @Getter
    SimpleWoodcuttingScript script;

    private SimpleWoodcuttingPanel panel;
    private NavigationButton navButton;
    private OverlayMouseAdapter mouseAdapter;
    private ExecutorService uiExecutor;

    private int startXp = 0;
    private long startTime = 0;
    /**
     * When Woodcutting XP last arrived. The script treats this as proof that a swing is
     * actually making progress, rather than guessing from how long ago it clicked.
     */
    @Getter
    private volatile long lastWoodcuttingXpAt = 0;

    private EggEvent eggEvent;
    private EntlingsEvent entlingsEvent;
    private FlowersEvent flowersEvent;
    private FoxEvent foxEvent;
    private HivesEvent hivesEvent;
    private LeprechaunEvent leprechaunEvent;
    private RitualEvent ritualEvent;
    private RootEvent rootEvent;
    private StrugglingSaplingEvent saplingEvent;

    public final List<Rs2NpcModel> ritualCircles = new ArrayList<>();
    public volatile ForestryEvents currentForestryEvent = ForestryEvents.NONE;
    public final GameObject[] saplingOrder = new GameObject[3];
    public final List<GameObject> saplingIngredients = new ArrayList<>(5);
    private final AtomicInteger completedForestryEvents = new AtomicInteger();

    @Provides
    SimpleWoodcuttingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SimpleWoodcuttingConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        uiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SimpleWoodcutting-ui");
            t.setDaemon(true);
            return t;
        });

        mouseAdapter = new OverlayMouseAdapter();
        mouseManager.registerMouseListener(mouseAdapter);

        panel = injector.getInstance(SimpleWoodcuttingPanel.class);
        navButton = NavigationButton.builder()
                .tooltip("Simple Woodcutting")
                .icon(buildIcon())
                .priority(7)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        refreshPanel();
        if (config.enableForestry()) {
            addForestryEvents();
        }

        // Deliberately does NOT start the script - the user starts it from the overlay.
        resetSessionCounters();
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        removeForestryEvents();
        ritualCircles.clear();
        saplingIngredients.clear();
        currentForestryEvent = ForestryEvents.NONE;
        completedForestryEvents.set(0);
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

    /** Runs a control action off the caller's (AWT event) thread - see AIOFishing note. */
    private void submitAction(Runnable action) {
        ExecutorService executor = uiExecutor;
        if (executor == null || executor.isShutdown()) {
            return;
        }
        executor.submit(() -> {
            try {
                action.run();
            } catch (Exception ex) {
                log.error("Simple Woodcutting control action failed", ex);
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
            log.info("Simple Woodcutting started.");
        });
    }

    public void stopScript() {
        submitAction(() -> {
            if (!script.isRunning()) {
                return;
            }
            script.shutdown();
            log.info("Simple Woodcutting stopped.");
        });
    }

    public void togglePause() {
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
        startXp = Microbot.getClient().getSkillExperience(Skill.WOODCUTTING);
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
    public static boolean isMembersWorld(SimpleWoodcuttingConfig config) {
        return config.worldMode().isMembersWorld(MEMBER_WORLD);
    }

    public static final Function<Quest, QuestState> QUEST_STATES = quest -> {
        try {
            return Rs2Player.getQuestState(quest);
        } catch (Exception e) {
            return null;
        }
    };

    public static final Function<Skill, Integer> SKILL_LEVELS = skill -> {
        try {
            return Rs2Player.getRealSkillLevel(skill);
        } catch (Exception e) {
            return null;
        }
    };

    public TreeStage resolveDisplayStage(int wcLevel) {
        return config.autoProgress()
                ? TreeStage.bestFor(wcLevel, isMembersWorld(config), QUEST_STATES, SKILL_LEVELS)
                : config.manualStage();
    }

    private Map<TreeStage, String> lockReasons(int wcLevel) {
        Map<TreeStage, String> reasons = new EnumMap<>(TreeStage.class);
        for (TreeStage stage : TreeStage.values()) {
            String reason = stage.lockReason(wcLevel, isMembersWorld(config), QUEST_STATES, SKILL_LEVELS);
            if (reason != null) {
                reasons.put(stage, reason);
            }
        }
        return reasons;
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (event.getSkill() == Skill.WOODCUTTING) {
            lastWoodcuttingXpAt = System.currentTimeMillis();
            refreshPanel();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (SimpleWoodcuttingPanel.CONFIG_GROUP.equals(event.getGroup())) {
            refreshPanel();
            updateForestryRegistration(event);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.SPAM
                && event.getType() != ChatMessageType.GAMEMESSAGE
                && event.getType() != ChatMessageType.MESBOX) {
            return;
        }

        String message = event.getMessage().toLowerCase();
        if (message.equals("you can't light a fire here.")) {
            script.setCannotLightFire(true);
        }
        if (!SAPLING_MESSAGE.matcher(event.getMessage()).matches()) {
            return;
        }

        int index = message.contains("first") ? 0
                : message.contains("second") ? 1
                : message.contains("third") ? 2 : -1;
        if (index < 0) {
            return;
        }
        GameObject ingredient = saplingIngredients.stream()
                .filter(object -> {
                    var composition = Microbot.getClient().getObjectDefinition(object.getId());
                    return composition != null && composition.getName() != null
                            && message.contains(composition.getName().toLowerCase());
                })
                .findFirst()
                .orElse(null);
        if (ingredient != null) {
            saplingOrder[index] = ingredient;
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (isRitualCircle(npc.getId())) {
            ritualCircles.add(new Rs2NpcModel(npc));
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (isRitualCircle(npc.getId())) {
            ritualCircles.removeIf(model -> model.getIndex() == npc.getIndex());
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        if (isSaplingIngredient(event.getGameObject().getId())) {
            saplingIngredients.add(event.getGameObject());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        if (isSaplingIngredient(event.getGameObject().getId())) {
            saplingIngredients.remove(event.getGameObject());
        }
    }

    private static boolean isRitualCircle(int id) {
        return id >= NpcID.GATHERING_EVENT_ENCHANTED_RITUAL_A_1
                && id <= NpcID.GATHERING_EVENT_ENCHANTED_RITUAL_D_4;
    }

    private static boolean isSaplingIngredient(int id) {
        return id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_1
                || id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_2
                || id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_3
                || id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_4A
                || id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_4B
                || id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_4C
                || id == ObjectID.GATHERING_EVENT_SAPLING_INGREDIENT_5;
    }

    private void updateForestryRegistration(ConfigChanged event) {
        if ("enableForestry".equals(event.getKey())) {
            if (config.enableForestry()) {
                addForestryEvents();
            } else {
                removeForestryEvents();
            }
            return;
        }
        if (!config.enableForestry()) {
            return;
        }
        removeForestryEvent(event.getKey());
        if ("true".equals(event.getNewValue())) {
            addForestryEvent(event.getKey());
        }
    }

    private void addForestryEvents() {
        removeForestryEvents();
        if (config.eggEvent()) {
            addForestryEvent("eggEvent");
        }
        if (config.entlingsEvent()) {
            addForestryEvent("entlingsEvent");
        }
        if (config.flowersEvent()) {
            addForestryEvent("flowersEvent");
        }
        if (config.foxEvent()) {
            addForestryEvent("foxEvent");
        }
        if (config.hivesEvent()) {
            addForestryEvent("hivesEvent");
        }
        if (config.leprechaunEvent()) {
            addForestryEvent("leprechaunEvent");
        }
        if (config.ritualEvent()) {
            addForestryEvent("ritualEvent");
        }
        if (config.rootEvent()) {
            addForestryEvent("rootEvent");
        }
        if (config.saplingEvent()) {
            addForestryEvent("saplingEvent");
        }
    }

    private void addForestryEvent(String key) {
        var manager = Microbot.getBlockingEventManager();
        switch (key) {
            case "eggEvent":
                eggEvent = new EggEvent(this);
                manager.add(eggEvent);
                break;
            case "entlingsEvent":
                entlingsEvent = new EntlingsEvent(this);
                manager.add(entlingsEvent);
                break;
            case "flowersEvent":
                flowersEvent = new FlowersEvent(this);
                manager.add(flowersEvent);
                break;
            case "foxEvent":
                foxEvent = new FoxEvent(this);
                manager.add(foxEvent);
                break;
            case "hivesEvent":
                hivesEvent = new HivesEvent(this);
                manager.add(hivesEvent);
                break;
            case "leprechaunEvent":
                leprechaunEvent = new LeprechaunEvent(this);
                manager.add(leprechaunEvent);
                break;
            case "ritualEvent":
                ritualEvent = new RitualEvent(this);
                manager.add(ritualEvent);
                break;
            case "rootEvent":
                rootEvent = new RootEvent(this);
                manager.add(rootEvent);
                break;
            case "saplingEvent":
                saplingEvent = new StrugglingSaplingEvent(this);
                manager.add(saplingEvent);
                break;
            default:
                break;
        }
    }

    private void removeForestryEvents() {
        removeForestryEvent("eggEvent");
        removeForestryEvent("entlingsEvent");
        removeForestryEvent("flowersEvent");
        removeForestryEvent("foxEvent");
        removeForestryEvent("hivesEvent");
        removeForestryEvent("leprechaunEvent");
        removeForestryEvent("ritualEvent");
        removeForestryEvent("rootEvent");
        removeForestryEvent("saplingEvent");
    }

    private void removeForestryEvent(String key) {
        var manager = Microbot.getBlockingEventManager();
        switch (key) {
            case "eggEvent":
                if (eggEvent != null) {
                    manager.remove(eggEvent);
                    eggEvent = null;
                }
                break;
            case "entlingsEvent":
                if (entlingsEvent != null) {
                    manager.remove(entlingsEvent);
                    entlingsEvent = null;
                }
                break;
            case "flowersEvent":
                if (flowersEvent != null) {
                    manager.remove(flowersEvent);
                    flowersEvent = null;
                }
                break;
            case "foxEvent":
                if (foxEvent != null) {
                    manager.remove(foxEvent);
                    foxEvent = null;
                }
                break;
            case "hivesEvent":
                if (hivesEvent != null) {
                    manager.remove(hivesEvent);
                    hivesEvent = null;
                }
                break;
            case "leprechaunEvent":
                if (leprechaunEvent != null) {
                    manager.remove(leprechaunEvent);
                    leprechaunEvent = null;
                }
                break;
            case "ritualEvent":
                if (ritualEvent != null) {
                    manager.remove(ritualEvent);
                    ritualEvent = null;
                }
                break;
            case "rootEvent":
                if (rootEvent != null) {
                    manager.remove(rootEvent);
                    rootEvent = null;
                }
                break;
            case "saplingEvent":
                if (saplingEvent != null) {
                    manager.remove(saplingEvent);
                    saplingEvent = null;
                }
                break;
            default:
                break;
        }
    }

    public TreeStage getSelectedTree() {
        return script == null ? config.manualStage() : script.getActiveStage();
    }

    public void incrementForestryEventCompleted() {
        completedForestryEvents.incrementAndGet();
    }

    public int getCompletedForestryEventCount() {
        return completedForestryEvents.get();
    }

    public ForestryEvents getCurrentForestryEvent() {
        return currentForestryEvent;
    }

    public void clearCurrentForestryEvent() {
        currentForestryEvent = ForestryEvents.NONE;
    }

    public boolean ensureInventorySpace(int requiredSlots) {
        int freeSlots = 28 - Rs2Inventory.count();
        if (freeSlots >= requiredSlots) {
            return true;
        }
        TreeStage stage = getSelectedTree();
        int amount = Math.min(requiredSlots - freeSlots,
                Rs2Inventory.count(stage.getLogName()));
        if (amount <= 0) {
            return false;
        }
        Rs2Inventory.dropAmount(stage.getLogName(), amount, config.dropOrder());
        return sleepUntil(() -> 28 - Rs2Inventory.count() >= requiredSlots, 2000);
    }

    private void refreshPanel() {
        if (panel == null) {
            return;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
        panel.update(level, resolveDisplayStage(level), isMembersWorld(config),
                config.autoProgress(), lockReasons(level), config.manualLocation());
    }

    // -------------------------------------------------------------------- stats

    public int getXpGained() {
        return Microbot.getClient().getSkillExperience(Skill.WOODCUTTING) - startXp;
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
            // Simple axe: brown handle + grey head.
            g.setColor(new Color(140, 94, 52));
            g.fillRect(7, 4, 2, 10);
            g.setColor(new Color(180, 180, 190));
            int[] xs = {9, 14, 9};
            int[] ys = {4, 6, 9};
            g.fillPolygon(xs, ys, 3);
        } finally {
            g.dispose();
        }
        return image;
    }
}
