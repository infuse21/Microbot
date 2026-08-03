package net.runelite.client.plugins.microbot.simplewoodcutting;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.AxeType;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.FletchingDisposition;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.InventoryMode;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.SimpleWcState;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.TreeLocation;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.TreeStage;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.WalkBackMode;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2BankID;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.inventory.Rs2LogBasket;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.skills.fletching.Rs2Fletching;
import net.runelite.client.plugins.microbot.util.skills.fletching.data.FletchingItem;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Slf4j
public class SimpleWoodcuttingScript extends Script {

    public static final int FORESTRY_DISTANCE = 15;
    /** How close a tree must be before we click it - click() does not auto-walk. */
    private static final int TREE_INTERACT_RANGE = 12;
    /** Ignore cached trees outside the local chopping area. */
    private static final int TREE_DISCOVERY_RANGE = 32;
    /** Curated points are safe land-side destinations, so approach them closely. */
    private static final int TREE_AREA_WALK_TOLERANCE = 8;
    /** How close counts as "in" a pinned location's area. */
    private static final int PINNED_RADIUS = 30;
    /**
     * How long the work area may stay empty of the target tree before we call it a bad
     * setup. Comfortably above every tree's respawn time, so a normal wait never trips it.
     */
    private static final long WORK_AREA_EMPTY_TIMEOUT_MS = 240000;
    private static final int BANK_DISCOVERY_RANGE = 20;
    private static final int BANK_INTERACT_RANGE = 4;
    private static final int BANK_WALK_TOLERANCE = 4;
    /** GE booths are wall objects and are not included in the normal bank-object ID set. */
    private static final Set<Integer> GRAND_EXCHANGE_BOOTH_IDS = Set.of(10060, 30389);
    /**
     * How long a swing may run without any Woodcutting XP before we assume it is wedged and
     * click again. Generously above the slowest realistic time between logs.
     */
    private static final long CHOP_STALL_TIMEOUT_MS = 45_000;
    /** Abandon a GE sale after this many failed attempts (price lookup or listing). */
    private static final int MAX_SELL_ATTEMPTS = 4;
    private static final Set<Integer> CAMPFIRE_IDS = Set.of(49927, 26185);

    private SimpleWoodcuttingConfig config;
    private final SimpleWoodcuttingPlugin plugin;

    @Getter
    private volatile SimpleWcState state = SimpleWcState.IDLE;
    @Getter
    private volatile TreeStage activeStage = TreeStage.TREE;
    @Getter
    private volatile boolean paused = false;
    @Getter
    private volatile String stopReason;
    @Getter
    private volatile String lastFatalError;

    private long lastChopTime = 0;
    /** Refreshed on every click and every XP drop; see {@link #isBusyChopping()}. */
    private volatile long lastChopProgressAt = 0;
    private volatile TreeLocation travelTarget;
    private volatile TreeStage travelTargetStage;
    private volatile WorldPoint startingLocation;
    private volatile WorldPoint lastTreeLocation;
    private volatile WorldPoint lastChoppingArea;
    private volatile String pendingSaleItem;
    private volatile boolean returnUnsoldToBank;
    @Getter
    private volatile boolean cannotLightFire;
    private final Map<String, Integer> sellAttempts = new HashMap<>();
    /** Logs we gave up trying to sell (no price data) - do not re-withdraw them. */
    private final Set<String> unsellable = new HashSet<>();

    @Inject
    public SimpleWoodcuttingScript(SimpleWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean run(SimpleWoodcuttingConfig config) {
        this.config = config;
        this.paused = false;
        this.stopReason = null;
        this.lastFatalError = null;
        this.pendingSaleItem = null;
        this.returnUnsoldToBank = false;
        this.startingLocation = Rs2Player.getWorldLocation();
        this.lastTreeLocation = null;
        this.lastChoppingArea = null;
        this.cannotLightFire = false;
        // Assume the axe is already swinging, so starting mid-chop does not fire a stray click.
        this.lastChopProgressAt = System.currentTimeMillis();
        this.sellAttempts.clear();
        this.unsellable.clear();
        this.state = SimpleWcState.IDLE;

        Rs2Antiban.resetAntibanSettings();
        Rs2Antiban.antibanSetupTemplates.applyWoodcuttingSetup();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::loop, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    public void togglePause() {
        this.paused = !this.paused;
    }

    /** Where we're headed, for display. Cached choice only - never pathfinds. */
    public TreeLocation getDisplayLocation() {
        TreeLocation pinned = pinnedLocation();
        return pinned != null ? pinned : travelTarget;
    }

    private void loop() {
        try {
            if (paused || !super.run() || !Microbot.isLoggedIn()) {
                return;
            }
            plugin.clearCurrentForestryEvent();

            activeStage = resolveStage();
            if (config.hopWhenPlayerDetected() && !config.enableForestry()
                    && Rs2Player.logoutIfPlayerDetected(config.playerDetectionRadius(), 10000)) {
                return;
            }
            if (tryFillLogBasket()) {
                return;
            }
            if (handleGroundLooting()) {
                return;
            }
            state = determineState();

            if (state == SimpleWcState.CHOPPING && isBusyChopping()) {
                return;
            }
            if (Rs2Player.isMoving() && !canInteractWhileMoving(state)) {
                return;
            }
            if (state != SimpleWcState.TRAVELING) {
                clearTravelTarget();
            }

            switch (state) {
                case GEARING:    handleGearing();   break;
                case TRAVELING:  handleTraveling(); break;
                case CHOPPING:   handleChopping();  break;
                case BANKING:    handleBanking();   break;
                case DROPPING:   handleDropping();  break;
                case FIREMAKING: handleFiremaking(); break;
                case FLETCHING:  handleFletching(); break;
                case SELLING:    handleSelling();   break;
                case STOPPED:    handleStopped();   break;
                default: break;
            }
        } catch (Exception ex) {
            log.error("SimpleWoodcutting loop error", ex);
        } catch (Throwable fatal) {
            lastFatalError = fatal.getClass().getName() + ": " + fatal.getMessage();
            log.error("SimpleWoodcutting fatal loop error", fatal);
            throw fatal;
        }
    }

    // ---------------------------------------------------------------- state

    private TreeStage resolveStage() {
        if (config.autoProgress()) {
            int level = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
            return TreeStage.bestFor(level, SimpleWoodcuttingPlugin.isMembersWorld(config),
                    SimpleWoodcuttingPlugin.QUEST_STATES, SimpleWoodcuttingPlugin.SKILL_LEVELS);
        }
        return config.manualStage();
    }

    private SimpleWcState determineState() {
        if (stopReason != null) {
            return SimpleWcState.STOPPED;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.WOODCUTTING);
        if (level >= config.targetLevel()) {
            requestStop("Target level " + config.targetLevel() + " reached");
            return SimpleWcState.STOPPED;
        }

        // Gate check before any walking.
        String lock = activeStage.lockReason(level, SimpleWoodcuttingPlugin.isMembersWorld(config),
                SimpleWoodcuttingPlugin.QUEST_STATES, SimpleWoodcuttingPlugin.SKILL_LEVELS);
        if (lock != null) {
            requestStop("Cannot chop " + activeStage.getDisplayName() + ": " + lock);
            return SimpleWcState.STOPPED;
        }

        if (returnUnsoldToBank) {
            return SimpleWcState.BANKING;
        }
        if (pendingSaleItem != null) {
            return SimpleWcState.SELLING;
        }
        if (Rs2Inventory.isFull()) {
            return fullInventoryState();
        }
        if (AxeType.bestHeld() == null) {
            return SimpleWcState.GEARING;
        }
        if (!isAtLocation(activeStage)) {
            return SimpleWcState.TRAVELING;
        }
        return SimpleWcState.CHOPPING;
    }

    private SimpleWcState fullInventoryState() {
        switch (config.inventoryMode()) {
            case DROP:     return SimpleWcState.DROPPING;
            case FIREMAKE: return SimpleWcState.FIREMAKING;
            case CAMPFIRE: return SimpleWcState.FIREMAKING;
            case FLETCH:   return SimpleWcState.FLETCHING;
            case BANK:
            default:       return SimpleWcState.BANKING;
        }
    }

    // ------------------------------------------------------------- handlers

    private void handleGearing() {
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        AxeType target = bestBankOrHeldAxe();
        if (target == null) {
            Rs2Bank.closeBank();
            requestStop("No usable axe in inventory or bank");
            return;
        }
        if (!target.isHeld() && Rs2Bank.hasBankItem(target.getItemName())) {
            Rs2Bank.withdrawItem(target.getItemName());
            sleepUntil(() -> Rs2Inventory.hasItem(target.getItemName()), 3000);
        }
        // Wield it so it doesn't occupy an inventory slot.
        if (Rs2Inventory.hasItem(target.getItemName())) {
            Rs2Inventory.wield(target.getItemName());
            sleepUntil(() -> Rs2Equipment.isWearing(target.getItemName()), 2000);
        }
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);

        if (AxeType.bestHeld() == null) {
            requestStop("Could not equip an axe");
        }
    }

    private void handleTraveling() {
        WorldPoint player = Rs2Player.getWorldLocation();
        TreeLocation pinned = pinnedLocation();

        if (isAtLocation(activeStage)) {
            handleChopping();
            return;
        }

        TreeLocation target = pinned != null ? pinned : chooseTravelTarget(player);
        if (target == null) {
            return;
        }
        lastChoppingArea = target.getPoint();
        if (!walkUntilNotPaused(target.getPoint(), TREE_AREA_WALK_TOLERANCE,
                () -> isAtLocation(activeStage))) {
            return;
        }
        if (isAtLocation(activeStage)) {
            handleChopping();
        }
    }

    private void handleChopping() {
        Rs2TileObjectModel tree = findNearestTree(activeStage);
        if (tree == null) {
            // An empty patch is normal - trees despawn when chopped. Only complain once it
            // has stayed empty for far longer than any respawn, which means the work area is
            // too tight or centred somewhere this tree does not grow.
            if (config.workAreaRadius() > 0 && lastTreeLocation != null
                    && System.currentTimeMillis() - lastChopTime > WORK_AREA_EMPTY_TIMEOUT_MS) {
                requestStop("No " + activeStage.getDisplayName() + " tree within "
                        + config.workAreaRadius() + " tiles of the work area"
                        + " - widen the radius or restart on the patch");
                return;
            }
            Microbot.status = "Waiting for a respawn";
            return;
        }
        if (!isInRange(tree)) {
            return; // routed back to TRAVELING next tick
        }
        if (isBusyChopping()) {
            return;
        }
        if (tree.click(activeStage.getAction())) {
            lastTreeLocation = tree.getWorldLocation();
            lastChopTime = System.currentTimeMillis();
            lastChopProgressAt = lastChopTime;
            Rs2Player.waitForXpDrop(Skill.WOODCUTTING, true);
            safeAntibanCooldown();
        }
    }

    /**
     * Whether the axe is mid-swing and should be left alone.
     *
     * <p>Progress is measured by XP arriving, not by how long ago we clicked. The old fixed
     * 12s window expired during any normal yew, magic or redwood chop, so the script clicked
     * again - often onto a different tree - while the current one was still being cut.</p>
     *
     * <p>The stall timeout only exists so a wedged animation cannot freeze the script forever;
     * it is deliberately far longer than the gap between logs on a slow axe.</p>
     */
    private boolean isBusyChopping() {
        if (!Rs2Player.isAnimating() && !Rs2Player.isInteracting()) {
            return false;
        }
        long progressAt = Math.max(lastChopProgressAt, plugin.getLastWoodcuttingXpAt());
        return System.currentTimeMillis() - progressAt < CHOP_STALL_TIMEOUT_MS;
    }

    private void handleBanking() {
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        if (config.useLogBasket() && Rs2LogBasket.hasLogBasket()) {
            Rs2LogBasket.emptyLogBasketAtBank();
        }

        Set<String> bankFragments = configuredNameFragments(config.itemsToBank());
        bankFragments.add(activeStage.getLogName().toLowerCase());
        if (pendingSaleItem != null) {
            bankFragments.add(pendingSaleItem.toLowerCase());
        }
        Rs2Bank.depositAll(item -> containsAny(item.getName(), bankFragments));
        returnUnsoldToBank = false;
        pendingSaleItem = null;
        Rs2Inventory.waitForInventoryChanges(1800);
        sleepUntil(() -> !Rs2Inventory.isFull(), 5000);
        withdrawStackForSale();
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        if (pendingSaleItem == null) {
            walkBackAfterProcessing();
        }
        safeAntibanCooldown();
    }

    private void handleDropping() {
        Set<String> keep = configuredNameFragments(config.itemsToKeep());
        if (config.lootBirdNests()) {
            keep.add("nest");
        }
        if (config.lootSeeds()) {
            keep.add("seed");
        }
        Rs2Inventory.dropAllExcept(false, config.dropOrder(), keep.toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(2000);
        safeAntibanCooldown();
    }

    private void handleFiremaking() {
        if (config.inventoryMode() == InventoryMode.CAMPFIRE && handleCampfireBurning()) {
            return;
        }
        if (!Rs2Inventory.hasItem("Tinderbox")) {
            // Try to grab one from the bank; fall back to banking if impossible.
            if (openBankAsSoonAsAvailable()) {
                if (Rs2Bank.hasBankItem("Tinderbox")) {
                    Rs2Bank.withdrawItem("Tinderbox");
                    Rs2Inventory.waitForInventoryChanges(1500);
                }
                Rs2Bank.closeBank();
            }
            if (!Rs2Inventory.hasItem("Tinderbox")) {
                requestStop("Firemaking needs a tinderbox");
                return;
            }
        }
        // Classic drop-and-burn line: light a log where we stand, then step back a tile.
        if (Rs2Player.isAnimating(2000)) {
            return;
        }
        Rs2Inventory.use("Tinderbox");
        sleepUntil(Rs2Inventory::isItemSelected, 1500);
        Rs2Inventory.useLast(nameToId(activeStage.getLogName()));
        if (Rs2Player.waitForXpDrop(Skill.FIREMAKING, 6000)) {
            cannotLightFire = false;
            safeAntibanCooldown();
        } else {
            // Standing on a fire - step west to clear ground before the next light.
            WorldPoint p = Rs2Player.getWorldLocation();
            if (p != null) {
                Rs2Walker.walkFastCanvas(new WorldPoint(p.getX() - 1, p.getY(), p.getPlane()));
                sleepUntil(() -> !Rs2Player.isMoving(), 2000);
            }
        }
    }

    private void handleFletching() {
        FletchingItem product = config.fletchingType();
        if (!ensureFletchingSupplies(product)) {
            return;
        }
        String productFragment = product.getContainsInventoryName();
        if (Rs2Inventory.hasItem(activeStage.getLogName())) {
            if (!Rs2Fletching.fletchItems(
                    activeStage.getLogName(), productFragment, "All")) {
                return;
            }
            sleepUntil(() -> paused || !Rs2Inventory.hasItem(activeStage.getLogName())
                            || !Rs2Player.isAnimating(3500),
                    60000);
            if (paused) {
                return;
            }
        }
        handleFletchedProducts(product, productFragment);
        safeAntibanCooldown();
    }

    private boolean ensureFletchingSupplies(FletchingItem product) {
        boolean needsString = (config.fletchingDisposition() == FletchingDisposition.STRING_AND_BANK
                || config.fletchingDisposition() == FletchingDisposition.STRING_AND_DROP)
                && (product == FletchingItem.SHORT || product == FletchingItem.LONG);
        boolean needsKnife = !Rs2Fletching.hasKnife();
        boolean needsBowString = needsString && !Rs2Inventory.hasItem("Bow string");
        if (!needsKnife && !needsBowString) {
            return true;
        }

        int slotsNeeded = (needsKnife ? 1 : 0) + (needsBowString ? 1 : 0);
        if (Rs2Inventory.emptySlotCount() < slotsNeeded) {
            int toDrop = slotsNeeded - Rs2Inventory.emptySlotCount();
            Rs2Inventory.dropAmount(activeStage.getLogName(), toDrop, config.dropOrder());
            if (!sleepUntil(() -> Rs2Inventory.emptySlotCount() >= slotsNeeded, 2000)) {
                requestStop("Could not make room for fletching supplies");
                return false;
            }
        }

        if (!openBankAsSoonAsAvailable()) {
            return false;
        }
        if (needsKnife && Rs2Bank.hasBankItem("Knife")) {
            Rs2Bank.withdrawItem("Knife");
        }
        if (needsBowString && Rs2Bank.hasBankItem("Bow string")) {
            Rs2Bank.withdrawX("Bow string",
                    Math.max(1, Rs2Inventory.count(activeStage.getLogName())));
        }
        Rs2Inventory.waitForInventoryChanges(1800);
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);

        if (!Rs2Fletching.hasKnife()) {
            requestStop("Fletching needs a knife");
            return false;
        }
        if (needsBowString && !Rs2Inventory.hasItem("Bow string")) {
            requestStop("Stringing bows needs bow string");
            return false;
        }
        return true;
    }

    private boolean handleCampfireBurning() {
        Rs2TileObjectModel campfire = Microbot.getRs2TileObjectCache().query()
                .where(object -> CAMPFIRE_IDS.contains(object.getId()))
                .nearestOnClientThread(8);
        if (campfire == null) {
            return false;
        }
        if (!Rs2Inventory.hasItem(activeStage.getLogName())) {
            requestStop("No " + activeStage.getLogName() + " available for the campfire");
            return true;
        }
        if (Rs2Player.isAnimating(2000)) {
            return true;
        }

        int logId = nameToId(activeStage.getLogName());
        if (logId <= 0 || !Rs2Inventory.useItemOnObject(logId, campfire.getId())) {
            return true;
        }
        sleepUntil(() -> Rs2Widget.findWidget(
                "How many would you like to burn?", null, false) != null, 5000);
        Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
        sleepUntil(() -> Rs2Player.isAnimating()
                || !Rs2Inventory.hasItem(activeStage.getLogName()), 3000);
        sleepUntil(() -> paused || !Rs2Inventory.hasItem(activeStage.getLogName())
                || !Rs2Player.isAnimating(3500), 60000);
        if (paused) {
            return true;
        }
        if (!Rs2Inventory.hasItem(activeStage.getLogName())) {
            walkBackAfterProcessing();
            safeAntibanCooldown();
        }
        return true;
    }

    private void handleFletchedProducts(FletchingItem product, String productFragment) {
        FletchingDisposition disposition = config.fletchingDisposition();
        if ((disposition == FletchingDisposition.STRING_AND_BANK
                || disposition == FletchingDisposition.STRING_AND_DROP)
                && (product == FletchingItem.SHORT || product == FletchingItem.LONG)
                && Rs2Inventory.hasItem("Bow string")) {
            Rs2Fletching.stringBows(productFragment);
            sleepUntil(() -> paused || !Rs2Player.isAnimating(3500), 60000);
            if (paused) {
                return;
            }
        }

        switch (disposition) {
            case BANK:
            case STRING_AND_BANK:
                bankFletchedProducts(productFragment);
                break;
            case DROP:
            case STRING_AND_DROP:
                Rs2Fletching.dropFletchedItems(productFragment);
                Rs2Inventory.waitForInventoryChanges(2000);
                break;
            case NONE:
                if (Rs2Inventory.isFull()) {
                    requestStop("Inventory full with kept fletched products");
                }
                break;
            default:
                break;
        }
    }

    private void bankFletchedProducts(String productFragment) {
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        String lowerProduct = productFragment.toLowerCase();
        Rs2Bank.depositAll(item -> item.getName() != null
                && item.getName().toLowerCase().contains(lowerProduct));
        Rs2Inventory.waitForInventoryChanges(1800);
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        walkBackAfterProcessing();
    }

    private boolean tryFillLogBasket() {
        if (!config.useLogBasket()
                || config.inventoryMode() != InventoryMode.BANK
                || !Rs2Inventory.isFull()
                || !Rs2LogBasket.hasLogBasket()
                || !Rs2Inventory.hasItem(activeStage.getLogName())) {
            return false;
        }
        int before = Rs2Inventory.count(activeStage.getLogName());
        return Rs2LogBasket.fillLogBasket()
                && sleepUntil(() -> Rs2Inventory.count(activeStage.getLogName()) < before, 3000);
    }

    private boolean handleGroundLooting() {
        if (pendingSaleItem != null || returnUnsoldToBank
                || (!config.lootBirdNests() && !config.lootSeeds())) {
            return false;
        }
        Rs2TileItemModel loot = findDesiredGroundLoot();
        if (loot == null) {
            return false;
        }

        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint lootLocation = loot.getWorldLocation();
        if (player == null || lootLocation == null) {
            return false;
        }
        if (player.distanceTo(lootLocation) > 6) {
            long lootHash = loot.getHash();
            if (!walkUntilNotPaused(lootLocation, 4,
                    () -> !isGroundItemPresent(lootHash))) {
                return false;
            }
            loot = findDesiredGroundLoot();
            if (loot == null) {
                return true;
            }
        }
        if (Rs2Inventory.isFull()) {
            if (!Rs2Inventory.hasItem(activeStage.getLogName())) {
                return false;
            }
            Rs2Inventory.dropAmount(activeStage.getLogName(), 1, config.dropOrder());
            if (!sleepUntil(() -> !Rs2Inventory.isFull(), 2000)) {
                return false;
            }
        }

        String name = loot.getName();
        int before = Rs2Inventory.count(name);
        if (!loot.pickup()) {
            return false;
        }
        sleepUntil(() -> Rs2Inventory.count(name) > before, 3000);
        return true;
    }

    private Rs2TileItemModel findDesiredGroundLoot() {
        return Microbot.getRs2TileItemCache().query()
                .where(item -> {
                    String name = item.getName();
                    if (name == null) {
                        return false;
                    }
                    String lower = name.toLowerCase();
                    boolean wanted = config.lootBirdNests() && lower.contains("nest")
                            || config.lootSeeds() && lower.contains("seed");
                    return wanted && (config.lootMyItemsOnly()
                            ? item.isOwned()
                            : item.isLootAble());
                })
                .nearest(FORESTRY_DISTANCE);
    }

    private boolean isGroundItemPresent(long hash) {
        return Microbot.getRs2TileItemCache().query()
                .where(item -> item.getHash() == hash)
                .count() > 0;
    }

    private void handleStopped() {
        log.info("SimpleWoodcutting stopped: {}", stopReason == null ? "requested" : stopReason);
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        shutdown();
    }

    // ------------------------------------------------------------- GE selling

    private void withdrawStackForSale() {
        if (!config.sellOnGe() || pendingSaleItem != null) {
            return;
        }

        // Current logs first, followed by every other known log. This ensures auto
        // progression does not strand stacks from an earlier stage in the bank.
        Set<String> logNames = new LinkedHashSet<>();
        logNames.add(activeStage.getLogName());
        for (TreeStage stage : TreeStage.values()) {
            logNames.add(stage.getLogName());
        }

        for (String name : logNames) {
            if (unsellable.contains(name.toLowerCase())) {
                continue;
            }
            Rs2ItemModel item = Rs2Bank.getBankItem(name, true);
            if (item == null || item.getQuantity() < config.sellStackSize()) {
                continue;
            }
            int banked = item.getQuantity();
            int unitValue = config.sellPricing() == GeSellPricing.CUSTOM
                    ? config.customSellPrice()
                    : itemPrice(item.getId());
            if (unitValue < config.minLogValue()) {
                continue;
            }

            int quantity = config.withdrawAllForSale()
                    ? banked
                    : Math.min(banked, config.sellStackSize());
            if (!Rs2Bank.setWithdrawAs(true)) {
                return;
            }
            boolean requested;
            try {
                requested = config.withdrawAllForSale()
                        ? Rs2Bank.withdrawAll(item.getId())
                        : Rs2Bank.withdrawX(item.getId(), quantity);
                if (requested && sleepUntil(() -> Rs2Inventory.hasItem(name), 3000)) {
                    pendingSaleItem = name;
                    log.info("Withdrew {} x{} as notes to sell.", name, quantity);
                    return;
                }
            } finally {
                Rs2Bank.setWithdrawAs(false);
            }
        }
    }

    private void handleSelling() {
        if (pendingSaleItem == null) {
            return;
        }
        if (!Rs2Inventory.hasItem(pendingSaleItem)) {
            pendingSaleItem = null;
            return;
        }
        if (!walkToGrandExchangeNotPaused()) {
            return;
        }
        if (!Rs2GrandExchange.openExchange()) {
            requestStop("Could not open the Grand Exchange");
            return;
        }
        Rs2GrandExchange.collectAllToBank();

        int attempt = sellAttempts.getOrDefault(pendingSaleItem, 0);
        // Bound every failure path (no price data, full GE, rejected listing) through one
        // counter so we can never retry forever - the raw-trout NPE loop seen on AIOFishing.
        if (attempt >= MAX_SELL_ATTEMPTS) {
            log.warn("Giving up selling {} after {} attempts - keeping the stack for banking.",
                    pendingSaleItem, attempt);
            unsellable.add(pendingSaleItem.toLowerCase());
            sellAttempts.remove(pendingSaleItem);
            returnUnsoldToBank = true;
            Rs2GrandExchange.closeExchange();
            return;
        }
        if (Rs2GrandExchange.getAvailableSlotsCount() <= 0) {
            Rs2GrandExchange.abortOffer(pendingSaleItem, true);
            sellAttempts.put(pendingSaleItem, attempt + 1);
            Rs2GrandExchange.closeExchange();
            return;
        }

        Rs2ItemModel notes = Rs2Inventory.get(pendingSaleItem);
        int quantity = Rs2Inventory.itemQuantity(pendingSaleItem);
        int price = resolveSellPrice(notes == null ? -1 : notes.getId(), attempt);

        if (price > 0 && Rs2GrandExchange.sellItem(pendingSaleItem, quantity, price)) {
            log.info("Listed {} x{} at {} gp each.", pendingSaleItem, quantity, price);
            sellAttempts.remove(pendingSaleItem);
            pendingSaleItem = null;
        } else {
            sellAttempts.put(pendingSaleItem, attempt + 1);
            log.warn("Failed to list {} (no price or listing rejected) - attempt {}.",
                    pendingSaleItem, attempt + 1);
        }
        Rs2GrandExchange.closeExchange();
        if (pendingSaleItem == null) {
            walkBackAfterProcessing();
        }
        safeAntibanCooldown();
    }

    /**
     * Price per log, honouring the pricing mode. Pricing comes from RuneLite's own
     * ItemManager (client-side wiki price data - no HTTP call); the Rs2GrandExchange price
     * helpers are avoided because they hit an external API that can be unavailable.
     */
    private int resolveSellPrice(int itemId, int attempt) {
        if (config.sellPricing() == GeSellPricing.CUSTOM) {
            return Math.max(1, config.customSellPrice());
        }
        int base = itemPrice(itemId);
        if (base <= 0) {
            return -1;
        }
        if (config.sellPricing() == GeSellPricing.MARKET) {
            return base;
        }
        double multiplier = Math.max(0.5, 1.0 - 0.05 * attempt);
        return Math.max(1, (int) (base * multiplier));
    }

    /** GE price from RuneLite's client-side price data; -1 if unknown. Never hits the network. */
    private int itemPrice(int itemId) {
        if (itemId <= 0) {
            return -1;
        }
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            int canonicalItemId = Microbot.getItemManager().canonicalize(itemId);
            return Microbot.getItemManager().getItemPrice(canonicalItemId);
        }).orElse(-1);
    }

    // -------------------------------------------------------------- helpers

    /**
     * Opens a loaded bank as soon as it is close enough to interact with. When the bank
     * has not loaded yet, the walker approaches the nearest known bank and stops as soon
     * as a bank entity becomes available.
     */
    private boolean openBankAsSoonAsAvailable() {
        if (Rs2Bank.isOpen()) {
            return true;
        }

        WorldPoint bankTarget = findLoadedBankLocation();
        if (bankTarget == null) {
            BankLocation bank = Rs2Bank.getNearestBank();
            if (bank == null) {
                return false;
            }
            if (!walkUntilNotPaused(bank.getWorldPoint(), BANK_WALK_TOLERANCE,
                    () -> findLoadedBankLocation() != null)) {
                return false;
            }
            bankTarget = findLoadedBankLocation();
        }

        if (bankTarget == null) {
            return false;
        }
        if (!isBankInteractable()
                && !walkUntilNotPaused(bankTarget, BANK_WALK_TOLERANCE,
                        this::isBankInteractable)) {
            return false;
        }
        return !paused && isBankInteractable() && Rs2Bank.openBank();
    }

    /**
     * Lets the thread that owns the walker observe pause at normal cancellation
     * checkpoints. Callers only interact when the walk completed without a pause.
     */
    private boolean walkUntilNotPaused(
            WorldPoint target,
            int distance,
            BooleanSupplier completionCondition) {
        boolean arrived = Rs2Walker.walkUntil(target, distance,
                () -> paused || completionCondition.getAsBoolean());
        return arrived && !paused;
    }

    private boolean walkToGrandExchangeNotPaused() {
        if (Rs2GrandExchange.isOpen()) {
            return !paused;
        }
        return walkUntilNotPaused(
                BankLocation.GRAND_EXCHANGE.getWorldPoint(),
                BANK_WALK_TOLERANCE,
                Rs2GrandExchange::isOpen);
    }

    private boolean isBankInteractable() {
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint bank = findLoadedBankLocation();
        return Rs2Bank.isOpen() || player != null && bank != null
                && player.distanceTo(bank) <= BANK_INTERACT_RANGE;
    }

    private WorldPoint findLoadedBankLocation() {
        Rs2TileObjectModel object = Microbot.getRs2TileObjectCache().query()
                .where(candidate -> (Rs2BankID.BANK_ID_SET.contains(candidate.getId())
                                || GRAND_EXCHANGE_BOOTH_IDS.contains(candidate.getId()))
                        && hasAction(candidate.getObjectComposition(), "Bank", "Collect"))
                .nearestOnClientThread(BANK_DISCOVERY_RANGE);
        if (object != null) {
            return object.getWorldLocation();
        }

        var banker = Microbot.getRs2NpcCache().query()
                .where(npc -> hasAction(npc.getNpc().getTransformedComposition(), "Bank")
                        || hasAction(npc.getNpc().getComposition(), "Bank"))
                .nearestOnClientThread(BANK_DISCOVERY_RANGE);
        return banker == null ? null : banker.getWorldLocation();
    }

    private static boolean hasAction(NPCComposition composition, String wanted) {
        return composition != null && composition.getActions() != null
                && Arrays.stream(composition.getActions())
                        .anyMatch(action -> action != null && action.equalsIgnoreCase(wanted));
    }

    private static boolean hasAction(ObjectComposition composition, String... wanted) {
        if (composition == null || composition.getActions() == null) {
            return false;
        }
        return Arrays.stream(composition.getActions())
                .filter(action -> action != null)
                .anyMatch(action -> Arrays.stream(wanted)
                        .anyMatch(candidate -> action.equalsIgnoreCase(candidate)));
    }

    private static boolean canInteractWhileMoving(SimpleWcState currentState) {
        return currentState == SimpleWcState.TRAVELING
                || currentState == SimpleWcState.GEARING
                || currentState == SimpleWcState.BANKING
                || currentState == SimpleWcState.SELLING;
    }

    private void walkBackAfterProcessing() {
        WorldPoint target;
        WalkBackMode mode = config.walkBackMode();
        switch (mode) {
            case LAST_TREE:
                target = lastTreeLocation;
                break;
            case STARTING_LOCATION:
                target = startingLocation;
                break;
            case CURATED_LOCATION:
            default:
                target = lastChoppingArea;
                break;
        }
        if (target == null || paused) {
            return;
        }
        walkUntilNotPaused(target, TREE_AREA_WALK_TOLERANCE,
                mode == WalkBackMode.CURATED_LOCATION
                        ? () -> isAtLocation(activeStage)
                        : () -> false);
    }

    private static Set<String> configuredNameFragments(String configured) {
        if (configured == null || configured.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean containsAny(String itemName, Set<String> fragments) {
        if (itemName == null) {
            return false;
        }
        String lowerName = itemName.toLowerCase();
        return fragments.stream().anyMatch(lowerName::contains);
    }

    public void setCannotLightFire(boolean cannotLightFire) {
        this.cannotLightFire = cannotLightFire;
    }

    private void requestStop(String reason) {
        if (stopReason == null) {
            stopReason = reason;
            log.warn("SimpleWoodcutting stopping: {}", reason);
        }
        state = SimpleWcState.STOPPED;
    }

    private TreeLocation pinnedLocation() {
        if (config == null || config.autoProgress()) {
            return null;
        }
        return activeStage.findLocation(config.manualLocation());
    }

    private TreeLocation chooseTravelTarget(WorldPoint player) {
        if (travelTarget != null && travelTargetStage == activeStage) {
            return travelTarget;
        }
        TreeLocation chosen = activeStage.getFastestLocation(player, SimpleWoodcuttingScript::pathTiles,
                SimpleWoodcuttingPlugin.isMembersWorld(config));
        travelTarget = chosen;
        travelTargetStage = activeStage;
        return chosen;
    }

    private static int pathTiles(WorldPoint from, WorldPoint to) {
        try {
            return Rs2Walker.getTotalTiles(from, to);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void clearTravelTarget() {
        travelTarget = null;
        travelTargetStage = null;
    }

    private boolean isAtLocation(TreeStage stage) {
        WorldPoint player = Rs2Player.getWorldLocation();
        // Once we have actually chopped here, standing inside the work area counts as
        // arrived even when every tree is a stump. Trees despawn the instant they are
        // chopped, so an empty patch is the normal case - without this the script would
        // pace back to the curated point and out again while waiting for a respawn.
        // Gated on having chopped at least once so that starting at a bank still travels.
        int radius = config.workAreaRadius();
        WorldPoint centre = workAreaCentre();
        if (radius > 0 && centre != null && player != null && lastTreeLocation != null
                && player.distanceTo(centre) <= radius) {
            return true;
        }
        if (!isInRange(findNearestTree(stage))) {
            return false;
        }
        TreeLocation pinned = pinnedLocation();
        if (pinned == null) {
            return true;
        }
        return player != null && player.distanceTo(pinned.getPoint()) <= PINNED_RADIUS;
    }

    /**
     * Whether a tree is close enough to click. Distance only - deliberately no line of sight.
     *
     * <p>A line-of-sight test here is always false and stalls the whole script. {@code
     * WorldArea.hasLineOfSightTo} walks the tiles between the two points and rejects any that
     * carries {@code BLOCK_LINE_OF_SIGHT_FULL}, and it checks the destination tile too - but a
     * tree <em>is</em> a solid object filling its own tile, so it always sets that flag. The
     * result is that even a tree you are pressed up against fails the test, {@code
     * isAtLocation} never becomes true, and the script sits in TRAVELING forever without ever
     * chopping. (The same trap is documented in QuestingScript, where a chest you were
     * standing against never became "clickable".)</p>
     *
     * <p>Distance alone is safe here: clicking a scenery object makes the game walk to it, and
     * a click that genuinely cannot be reached answers "I can't reach that!" rather than
     * hanging - so being permissive self-corrects, while being strict deadlocks.</p>
     */
    private boolean isInRange(Rs2TileObjectModel tree) {
        if (tree == null) {
            return false;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint treeLoc = tree.getWorldLocation();
        return player != null && treeLoc != null
                && player.distanceTo(treeLoc) <= TREE_INTERACT_RANGE;
    }

    /**
     * Centre of the work area: the spot we actually travelled to, falling back to where the
     * script was started (which is the right answer when the user began already standing in
     * the trees, so no travelling happened).
     */
    private WorldPoint workAreaCentre() {
        WorldPoint centre = lastChoppingArea;
        return centre != null ? centre : startingLocation;
    }

    /**
     * Nearest tree of this stage, restricted to the work area when one is configured.
     *
     * <p>Without the restriction the search is simply "nearest tree of this type within 32
     * tiles", which lets the bot creep toward whichever cluster happens to be closest and
     * gradually abandon the spot the user picked.</p>
     */
    private Rs2TileObjectModel findNearestTree(TreeStage stage) {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        try {
            int radius = config.workAreaRadius();
            WorldPoint centre = workAreaCentre();
            if (radius > 0 && centre != null) {
                return Microbot.getRs2TileObjectCache().query()
                        .withName(stage.getTreeObjectName())
                        .within(centre, radius)
                        .nearestOnClientThread(TREE_DISCOVERY_RANGE);
            }
            return Microbot.getRs2TileObjectCache().query()
                    .withName(stage.getTreeObjectName())
                    .nearestOnClientThread(TREE_DISCOVERY_RANGE);
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("interrupted waiting for client thread")) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /** Best axe the player owns, considering both inventory/equipment and the open bank. */
    private AxeType bestBankOrHeldAxe() {
        AxeType best = null;
        for (AxeType axe : AxeType.values()) {
            if (!axe.meetsLevel()) {
                continue;
            }
            if (axe.isHeld() || Rs2Bank.hasBankItem(axe.getItemName())) {
                best = axe; // values() is worst-to-best, so keep the last match
            }
        }
        return best;
    }

    private int nameToId(String logName) {
        Rs2ItemModel item = Rs2Inventory.get(logName);
        return item != null ? item.getId() : -1;
    }

    /**
     * Start the post-action cooldown. Deliberately does <em>not</em> trigger micro breaks.
     *
     * <p>{@code takeMicroBreakByChance()} only raises {@code microBreakActive}; the flag is
     * cleared by BreakHandlerScript, so calling it without the Break Handler plugin running
     * leaves a latch nobody lowers. That is also why the woodcutting template sets
     * {@code takeMicroBreaks = false} - breaks are opt-in through Break Handler, not
     * something a script switches on behalf of the user.</p>
     */
    private void safeAntibanCooldown() {
        try {
            Rs2Antiban.actionCooldown();
        } catch (IllegalArgumentException e) {
            log.warn("Antiban cooldown skipped: {}", e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            if (Rs2Bank.isOpen()) {
                Rs2Bank.closeBank();
            }
        } catch (Exception ex) {
            log.debug("Could not close bank during shutdown: {}", ex.toString());
        }
        paused = false;
        pendingSaleItem = null;
        returnUnsoldToBank = false;
        startingLocation = null;
        lastTreeLocation = null;
        lastChoppingArea = null;
        cannotLightFire = false;
        clearTravelTarget();
        state = SimpleWcState.IDLE;
    }
}
