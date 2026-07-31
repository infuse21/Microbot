package net.runelite.client.plugins.microbot.aiofishing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingDebugMode;
import net.runelite.client.plugins.microbot.aiofishing.enums.CatchProcessing;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingState;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingLocation;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.aiofishing.enums.HarpoonType;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingActivity;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2BankID;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@Slf4j
public class AIOFishingScript extends Script {

    /**
     * How close a fishing spot must be before we click it. Rs2NpcModel.click() does NOT
     * auto-walk (unlike the legacy Rs2GameObject.clickObject), so anything further than
     * this has to be walked to first or the script would click into the void forever.
     */
    private static final int SPOT_INTERACT_RANGE = 12;
    /** Ignore cached spots outside the local fishing area. */
    private static final int SPOT_DISCOVERY_RANGE = 32;
    /** Curated points are safe land-side destinations, so approach them closely. */
    private static final int FISHING_AREA_WALK_TOLERANCE = 8;
    /**
     * How close to a curated point counts as being in that fishing area. Used to stop a
     * long walk finishing at an unrelated spot of the same type that it merely passes.
     *
     * <p>Kept tight enough to separate the closest pair of curated points (Al Kharid and
     * Lumbridge Swamp are 30 apart) while staying well above the walk tolerance of 8.</p>
     */
    private static final int FISHING_AREA_RADIUS = 20;
    /** How close counts as "in" a pinned location's area. */
    private static final int PINNED_RADIUS = 30;
    /** Shop NPC readiness radius; the fallback destination remains tighter than this. */
    private static final int SHOP_INTERACT_RANGE = 10;
    private static final int SHOP_WALK_TOLERANCE = 4;
    /**
     * How close to a shop's own location counts as being at that shop. Needed because the
     * shop lookup matches NPC names as a substring and is not tied to a location, so a
     * generically-named shopkeeper ("Fish monger") could otherwise satisfy the walk's
     * early-finish somewhere else entirely.
     */
    private static final int SHOP_AREA_RADIUS = 20;
    /**
     * A bank can enter the entity cache before it is close enough for a reliable interaction.
     * Keep this at canvas interaction range; using the cache's wider discovery radius makes
     * walkUntil complete immediately on every retry while openBank() is still too far away.
     */
    private static final int BANK_INTERACT_RANGE = 4;
    private static final int BANK_WALK_TOLERANCE = 4;
    /** GE booths are wall objects and are not included in the normal bank-object ID set. */
    private static final Set<Integer> GRAND_EXCHANGE_BOOTH_IDS = Set.of(10060, 30389);
    /**
     * Give up on knife/hammer processing after this many passes that consumed nothing.
     * Without it an unexpected interface (or a catch the tool won't act on) would leave a
     * full inventory that never drains, and the loop would retry forever.
     */
    private static final int MAX_PROCESSING_STALLS = 3;

    private AIOFishingConfig config;
    private HarpoonType selectedHarpoon = HarpoonType.NONE;

    @Getter
    private volatile AIOFishingState state = AIOFishingState.IDLE;
    @Getter
    private volatile FishingStage activeStage = FishingStage.SHRIMP;
    /** User-controlled pause, independent of the global Microbot pause. */
    @Getter
    private volatile boolean paused = false;
    /**
     * True only while a successful fishing-spot interaction is producing catches. The client
     * thread uses this to distinguish caught inventory gains from bank/GE item transfers
     * without racing the script's coarse state transitions.
     */
    @Getter
    private volatile boolean fishingInteractionActive = false;

    private String cachedAction = "";
    private long lastFishActionTime = 0;
    /** Cached path-aware travel choice; recomputed per trip, not per tick. */
    private volatile FishingLocation travelTarget;
    private volatile FishingStage travelTargetStage;
    /** Consecutive processing passes that consumed nothing; see MAX_PROCESSING_STALLS. */
    private int processingStalls;
    /**
     * Non-null once something unrecoverable happened (no bait and no way to buy it, etc).
     * determineState() is recomputed every tick, so a handler setting state = STOPPED is
     * not enough on its own - this flag is what makes a stop stick.
     */
    @Getter
    private volatile String stopReason;
    /** Fatal loop failure captured before ScheduledExecutorService suppresses it. */
    @Getter
    private volatile String lastFatalError;

    /**
     * Grand Exchange trading and aerial fishing, each owning its own state.
     *
     * <p>Both were handlers on this class until it reached thirteen of them. They are the
     * two parts that share least with the core loop - trading has its own attempt counters
     * and blacklist, aerial bypasses the stage/tool/bank pipeline entirely - so they lift
     * out cleanly and the state machine keeps only dispatch.</p>
     */
    private final AIOFishingGrandExchange grandExchange = new AIOFishingGrandExchange(this);
    private final AIOFishingAerial aerial = new AIOFishingAerial(this);
    private final AIOFishingGear gear = new AIOFishingGear(this);
    private final AIOFishingSupplies supplies = new AIOFishingSupplies(this);
    private final AIOFishingDebug debug = new AIOFishingDebug(this);

    AIOFishingGear gear() {
        return gear;
    }

    AIOFishingSupplies supplies() {
        return supplies;
    }

    AIOFishingDebug debug() {
        return debug;
    }

    AIOFishingGrandExchange grandExchange() {
        return grandExchange;
    }

    void setDebugBlockReason(String reason) {
        debug.setBlockReason(reason);
    }

    // Package-private seams for the two collaborators above. Deliberately not public: they
    // are internal wiring, not part of the script's API to the plugin or the UI.

    HarpoonType selectedHarpoon() {
        return selectedHarpoon;
    }

    AIOFishingConfig config() {
        return config;
    }

    void clearDebugBlockReason() {
        debug.clearBlockReason();
    }

    /** A completed purchase: drop it from the order book and stop routing to the GE. */
    void finishBuy(String itemName) {
        supplies.finishBuy(itemName);
        grandExchange.forgetBuyAttempts(itemName);
    }

    public AIOFishingDebugMode getActiveDebugMode() {
        return debug.getActiveMode();
    }

    public String getDebugBlockReason() {
        return debug.getBlockReason();
    }

    /** Catch item currently withdrawn as notes and awaiting a GE listing, or null. */
    public String getPendingSaleItem() {
        return grandExchange.getPendingSaleItem();
    }

    /** Last sale scan result, surfaced for the debug overlay. */
    public String getLastSaleScanDiagnostic() {
        return grandExchange.getLastSaleScanDiagnostic();
    }

    public boolean run(AIOFishingConfig config) {
        this.config = config;
        this.selectedHarpoon = config.harpoonSpec();
        this.paused = false;
        this.fishingInteractionActive = false;
        this.cachedAction = "";
        this.supplies.reset();
        this.processingStalls = 0;
        this.grandExchange.reset();
        this.debug.reset();
        this.lastFatalError = null;
        this.stopReason = null;
        this.state = AIOFishingState.IDLE;

        Rs2Antiban.resetAntibanSettings();
        // The template's values are used as-is. In particular takeMicroBreaks stays false:
        // micro breaks are Break Handler's job, and switching them on from here sets a flag
        // only BreakHandlerScript clears.
        Rs2Antiban.antibanSetupTemplates.applyFishingSetup();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::loop, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    public void togglePause() {
        this.paused = !this.paused;
    }

    /** Terminally stop the script with a reason the UI can show. Logged once. */
    void requestStop(String reason) {
        if (stopReason == null) {
            stopReason = reason;
            log.warn("AIO Fishing stopping: {}", reason);
        }
        state = AIOFishingState.STOPPED;
    }

    /**
     * Where we're currently headed, for display. Returns the cached choice so the overlay
     * never triggers a pathfind on the client thread.
     */
    public FishingLocation getDisplayLocation() {
        FishingLocation pinned = pinnedLocation();
        if (pinned != null) {
            return pinned;
        }
        return travelTarget;
    }

    private void loop() {
        try {
            if (paused) {
                return;
            }
            if (!super.run() || !Microbot.isLoggedIn()) {
                return;
            }

            activeStage = resolveStage();
            state = determineState();
            // Honour antiban action cooldowns. The fishing template sets
            // universalAntiban = false, so the framework does NOT pause us - the script has
            // to check this itself. Without this the cooldowns were armed on every action and
            // then completely ignored. AntibanPlugin counts TIMEOUT down and clears the flag
            // on its own game tick, so this can never deadlock. STOPPED is exempt so a
            // shutdown is never held up by a cooldown.
            if (Rs2AntibanSettings.actionCooldownActive && state != AIOFishingState.STOPPED) {
                Microbot.status = "Antiban cooldown";
                return;
            }
            if (debug.getBlockReason() != null) {
                Microbot.status = debug.getBlockReason();
                return;
            }

            // Let the current animation/interaction finish before acting again while fishing.
            if (state == AIOFishingState.FISHING
                    && (Rs2Player.isAnimating() || Rs2Player.isInteracting())
                    && System.currentTimeMillis() - lastFishActionTime < 15000) {
                return;
            }
            // Entity interactions can naturally replace a queued movement destination. Keep
            // inventory/config actions still until arrival, but do not make the player stand
            // idle before clicking a visible fishing spot, bank, shop NPC, or GE booth.
            if (Rs2Player.isMoving() && !canInteractWhileMoving(state)) {
                return;
            }

            // Once we're not travelling, drop the cached choice so the next trip is
            // measured fresh from wherever we end up (e.g. after a bank run).
            if (state != AIOFishingState.TRAVELING) {
                clearTravelTarget();
            }

            if (debug.getActiveMode() == AIOFishingDebugMode.WALKING_TO_BANK) {
                openBankAsSoonAsAvailable();
                return;
            }
            if (debug.getActiveMode() == AIOFishingDebugMode.WALKING_TO_FISHING_SPOT) {
                handleTraveling();
                return;
            }
            if (debug.getActiveMode() == AIOFishingDebugMode.SELLING_ON_GE) {
                grandExchange.handleDebugSelling();
                return;
            }

            switch (state) {
                case SHOPPING:   supplies.handleShopping(); break;
                case SELLING:    grandExchange.handleSelling(); break;
                case BUYING:     grandExchange.handleBuying();  break;
                case GEARING:    gear.handleGearing();  break;
                case TRAVELING:  handleTraveling(); break;
                case FISHING:    handleFishing();   break;
                case PROCESSING: handleProcessing(); break;
                case BANKING:    handleBanking();   break;
                case DROPPING:   handleDropping();  break;
                case AERIAL_FISHING: aerial.handleFishing(); break;
                case AERIAL_CUTTING: aerial.handleCutting(); break;
                case AERIAL_BAITING: aerial.handleBaiting(); break;
                case STOPPED:    handleStopped();   break;
                default: break;
            }
        } catch (Exception ex) {
            log.error("AIOFishing loop error", ex);
        } catch (Throwable fatal) {
            lastFatalError = fatal.getClass().getName() + ": " + fatal.getMessage();
            log.error("AIOFishing fatal loop error", fatal);
            throw fatal;
        }
    }

    public boolean isMainFutureDone() {
        return mainScheduledFuture != null && mainScheduledFuture.isDone();
    }

    public boolean isMainFutureCancelled() {
        return mainScheduledFuture != null && mainScheduledFuture.isCancelled();
    }

    public String getMainFutureFailure() {
        if (mainScheduledFuture == null || !mainScheduledFuture.isDone()
                || mainScheduledFuture.isCancelled()) {
            return null;
        }
        try {
            mainScheduledFuture.get();
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            return cause == null
                    ? e.toString()
                    : cause.getClass().getName() + ": " + cause.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Interrupted while reading completed future";
        }
    }

    // ---------------------------------------------------------------- state

    private FishingStage resolveStage() {
        if (config.autoProgress()) {
            int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
            return FishingStage.bestFor(level, AIOFishingPlugin.isMembersWorld(config),
                    AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS,
                    AIOFishingPlugin.VARBIT_VALUES);
        }
        return config.manualStage();
    }

    private AIOFishingState determineState() {
        // A requested stop is terminal - check it first, or the recompute below would
        // silently resurrect the script into an endless retry loop.
        if (stopReason != null) {
            return AIOFishingState.STOPPED;
        }
        AIOFishingState forcedState = debug.determineState();
        if (forcedState != null) {
            return forcedState;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
        if (level >= config.targetLevel()) {
            requestStop("Target level " + config.targetLevel() + " reached");
            return AIOFishingState.STOPPED;
        }
        // Aerial fishing shares none of the stage/tool/bank pipeline below, so it branches
        // out here into its own small loop.
        if (config.activity() == FishingActivity.AERIAL) {
            return aerial.determineState();
        }

        // Verify quest/level/members gates BEFORE any walking, so a locked stage can never
        // send us looping around an area we can't actually fish in.
        String lock = activeStage.lockReason(level, AIOFishingPlugin.isMembersWorld(config),
                AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS,
                AIOFishingPlugin.VARBIT_VALUES);
        if (lock != null) {
            requestStop("Cannot fish " + activeStage.getDisplayName() + ": " + lock);
            return AIOFishingState.STOPPED;
        }
        // A pending purchase outranks everything else - we can't fish without the item.
        if (supplies.hasOrders()) {
            return supplies.isViaGe() ? AIOFishingState.BUYING : AIOFishingState.SHOPPING;
        }
        // Carrying notes we have not listed yet - clear them before anything else, or
        // they would just be deposited again on the next bank trip.
        if (grandExchange.hasPendingSale()) {
            return AIOFishingState.SELLING;
        }
        if (Rs2Inventory.isFull()) {
            // Sacred/infernal eels are worthless uncracked, and their output stacks - so a
            // full inventory is processed rather than banked, which frees the slots and
            // lets fishing resume without a trip. Banking still happens once the
            // non-stackable oddments (clue bottles, bolt tips) fill up again.
            if (needsProcessing()) {
                return AIOFishingState.PROCESSING;
            }
            return config.useBank() ? AIOFishingState.BANKING : AIOFishingState.DROPPING;
        }
        if (!gear.hasGear(activeStage)) {
            return AIOFishingState.GEARING;
        }
        if (!isAtLocation(activeStage)) {
            return AIOFishingState.TRAVELING;
        }
        return AIOFishingState.FISHING;
    }

    // ------------------------------------------------------------- handlers

    /**
     * Travel logic.
     *
     * <p>Travel is always anchored to a curated land-side location. Fishing NPCs can be
     * geometrically close while terrain makes them hundreds of walking tiles away, so a
     * loaded spot must never replace the path-aware travel destination.</p>
     */
    private void handleTraveling() {
        if (config.activity() == FishingActivity.AERIAL) {
            Rs2Walker.walkTo(AerialFishing.MOLCH_ISLAND_SPOT, 2);
            return;
        }
        cachedAction = "";
        WorldPoint player = Rs2Player.getWorldLocation();
        FishingLocation pinned = pinnedLocation();

        if (isAtLocation(activeStage)) {
            handleFishing();
            return;
        }

        FishingLocation target = pinned != null ? pinned : chooseTravelTarget(player);
        if (target == null) {
            return;
        }
        // The curated point identifies the fishing area, not a tile the player needs to
        // stand on, so finish early once a spot there is interactable. That must be limited
        // to the destination area: many stages have spots dotted across the map (shrimp alone
        // has five), and without the proximity guard the walk ends at the first incidental
        // spot it passes - which was cancelling journeys ~180 tiles short of the target.
        final WorldPoint destination = target.getPoint();
        if (!walkUntilNotPaused(destination, FISHING_AREA_WALK_TOLERANCE,
                () -> isNearArea(destination) && isAtLocation(activeStage))) {
            return;
        }
        if (isAtLocation(activeStage)) {
            handleFishing();
        }
    }

    /**
     * Picks where to travel using real path distance (which accounts for teleports and
     * shortcuts) rather than straight-line distance.
     *
     * <p>Each measurement runs a full pathfind, so the choice is computed once per trip
     * and held in {@link #travelTarget} until we arrive or the stage changes. This runs on
     * the script thread - never call it from the client thread.</p>
     */
    private FishingLocation chooseTravelTarget(WorldPoint player) {
        if (travelTarget != null && travelTargetStage == activeStage) {
            return travelTarget;
        }
        FishingLocation chosen = activeStage.getFastestLocation(player, AIOFishingScript::pathTiles,
                AIOFishingPlugin.isMembersWorld(config));
        travelTarget = chosen;
        travelTargetStage = activeStage;
        log.debug("Travel target for {}: {}", activeStage, chosen.getName());
        return chosen;
    }

    static int pathTiles(WorldPoint from, WorldPoint to) {
        try {
            return Rs2Walker.getTotalTiles(from, to);
        } catch (Exception e) {
            return Integer.MAX_VALUE; // unreachable -> caller falls back to straight line
        }
    }

    /** Forget the cached travel choice so the next trip re-measures from where we are now. */
    private void clearTravelTarget() {
        travelTarget = null;
        travelTargetStage = null;
    }

    /**
     * The location the user pinned for the manual fish, or null when we should just use
     * the nearest one. Auto progression never pins - it always picks nearest.
     */
    private FishingLocation pinnedLocation() {
        // config is only set once run() is called; the overlay asks for this while stopped.
        if (config == null || config.autoProgress()) {
            return null;
        }
        return activeStage.findLocation(config.manualLocation());
    }

    private void handleFishing() {
        gear.ensureFishStorageOpen();
        Rs2NpcModel spot = findNearestSpot(activeStage);
        if (!isInRange(spot)) {
            return; // determineState() will route us back to TRAVELING next tick
        }

        // Already fishing this exact spot? Leave it alone until it despawns.
        Actor interacting = Rs2Player.getInteracting();
        if (interacting instanceof NPC
                && ((NPC) interacting).getIndex() == spot.getNpc().getIndex()
                && System.currentTimeMillis() - lastFishActionTime < 15000) {
            return;
        }

        activateHarpoonSpec();

        if (cachedAction.isEmpty()) {
            cachedAction = resolveAction(spot);
        }
        if (cachedAction.isEmpty()) {
            return;
        }

        int spotIndex = spot.getNpc().getIndex();
        if (spot.click(cachedAction)) {
            fishingInteractionActive = true;
            try {
                lastFishActionTime = System.currentTimeMillis();
                Rs2Player.waitForXpDrop(Skill.FISHING, true);

                // One click per spot lifetime: keep waiting on xp until the spot moves or we fill up.
                while (Microbot.isLoggedIn() && !Rs2Inventory.isFull()) {
                    Actor current = Rs2Player.getInteracting();
                    if (!(current instanceof NPC) || ((NPC) current).getIndex() != spotIndex) {
                        break;
                    }
                    Rs2Player.waitForXpDrop(Skill.FISHING, 10000, true);
                }
                lastFishActionTime = System.currentTimeMillis();
                safeAntibanCooldown();
            } finally {
                fishingInteractionActive = false;
            }
        }
    }

    /**
     * True when the active stage's catch needs a knife/hammer step and we can perform it.
     *
     * <p>All three conditions are checked rather than assumed: the user can turn processing
     * off, the Cooking gate on sacred eels can be unmet, and the tool can have been lost.
     * Any of those simply falls through to normal banking, so the catch is never stranded.</p>
     */
    private boolean needsProcessing() {
        CatchProcessing processing = activeStage.getProcessing();
        if (!config.processCatch() || !processing.isUsable(AIOFishingPlugin.SKILL_LEVELS)) {
            return false;
        }
        return Rs2Inventory.hasItem(processing.getToolName())
                && Rs2Inventory.hasItem(processing.getRawItemName());
    }

    /**
     * Turn the raw catch into its reward - knife on sacred eels, hammer on infernal eels.
     *
     * <p>One item per pass, re-entered from the state machine, so a combine that silently
     * does nothing can't wedge the script inside a long blocking sleep. If the count stops
     * falling the guard below stops the script with a reason instead of spinning.</p>
     */
    private void handleProcessing() {
        CatchProcessing processing = activeStage.getProcessing();
        String rawName = processing.getRawItemName();
        Microbot.status = processing.getLabel();

        int before = Rs2Inventory.itemQuantity(rawName);
        if (before <= 0) {
            processingStalls = 0;
            return;
        }
        Rs2Inventory.combine(processing.getToolName(), rawName);
        boolean progressed = sleepUntil(() -> Rs2Inventory.itemQuantity(rawName) < before, 5000);
        if (progressed) {
            processingStalls = 0;
            safeAntibanCooldown();
            return;
        }
        if (++processingStalls >= MAX_PROCESSING_STALLS) {
            requestStop("Could not " + processing.getToolName().toLowerCase()
                    + " the " + rawName.toLowerCase() + " - processing made no progress");
        }
    }

    private void handleBanking() {
        gear.dropUnwantedBigNetBycatch();
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        // Dump the configured barrel first so its fish are banked with the rest. Handles the
        // fish sack barrel too, which Rs2Bank.emptyFishBarrel() does not cover.
        gear.emptyFishStorage();
        Rs2Bank.depositAllExcept(gear.itemsToKeep(activeStage).toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(1800);
        sleepUntil(() -> !Rs2Inventory.isFull(), 5000);

        // With enough of a valuable fish banked, take it out as notes for a GE trip.
        grandExchange.withdrawStackForSale();

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        safeAntibanCooldown();
    }

    private void handleDropping() {
        List<String> toDrop = new ArrayList<>(activeStage.getCatchItemNames());
        toDrop.addAll(gear.unwantedBigNetBycatch());
        Rs2Inventory.dropAll(toDrop.toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(2000);
        safeAntibanCooldown();
    }

    private void handleStopped() {
        log.info("AIOFishing stopping (target reached or requirement missing).");
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        shutdown();
    }

    // -------------------------------------------------------------- helpers

    /**
     * Click a bank as soon as one is loaded. When none is loaded yet, approach only far
     * enough for the normal bank query to discover it, then let the interaction path the
     * final tiles. This avoids walking to the bank coordinate and waiting to become idle.
     */
    boolean openBankAsSoonAsAvailable() {
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
        if (!isBankInteractable()) {
            if (!walkUntilNotPaused(bankTarget, BANK_WALK_TOLERANCE,
                    this::isBankInteractable)) {
                return false;
            }
        }
        return isBankInteractable() && Rs2Bank.openBank();
    }

    /**
     * Lets the thread that owns the walker observe pause at its normal cancellation
     * checkpoints. Returning false prevents the caller from interacting after cancellation.
     */
    boolean walkUntilNotPaused(
            WorldPoint target,
            int distance,
            BooleanSupplier completionCondition) {
        boolean arrived = Rs2Walker.walkUntil(target, distance,
                () -> paused || completionCondition.getAsBoolean());
        return arrived && !paused;
    }

    boolean walkToGrandExchange() {
        if (Rs2GrandExchange.isOpen()) {
            return !paused;
        }
        return walkUntilNotPaused(
                BankLocation.GRAND_EXCHANGE.getWorldPoint(),
                BANK_WALK_TOLERANCE,
                Rs2GrandExchange::isOpen);
    }

    boolean isShopNpcInteractable(String npcName, int interactRange) {
        var npc = Rs2Shop.getNearestShopNpc(npcName);
        if (npc == null) {
            return false;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint npcLocation = npc.getWorldLocation();
        return player != null && npcLocation != null
                && player.distanceTo(npcLocation) <= interactRange;
    }

    private boolean isBankInteractable() {
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint bank = findLoadedBankLocation();
        return Rs2Bank.isOpen() || player != null && bank != null
                && player.distanceTo(bank) <= BANK_INTERACT_RANGE;
    }

    private WorldPoint findLoadedBankLocation() {
        var object = Microbot.getRs2TileObjectCache().query()
                .where(candidate -> (Rs2BankID.BANK_ID_SET.contains(candidate.getId())
                                || GRAND_EXCHANGE_BOOTH_IDS.contains(candidate.getId()))
                        && hasAction(candidate.getObjectComposition(), "Bank", "Collect"))
                .nearestOnClientThread();
        if (object != null) {
            return object.getWorldLocation();
        }

        var banker = Microbot.getRs2NpcCache().query()
                .where(npc -> hasAction(npc.getNpc().getTransformedComposition(), "Bank")
                        || hasAction(npc.getNpc().getComposition(), "Bank"))
                .nearestOnClientThread();
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

    private boolean canInteractWhileMoving(AIOFishingState currentState) {
        return currentState == AIOFishingState.TRAVELING
                || currentState == AIOFishingState.FISHING
                || currentState == AIOFishingState.BANKING
                || currentState == AIOFishingState.SHOPPING
                || currentState == AIOFishingState.BUYING
                || currentState == AIOFishingState.SELLING;
    }

    /**
     * We're "at" a location only when a spot is actually within clicking range - being near
     * the curated world point isn't enough, because click() won't close the last few tiles.
     *
     * <p>When a location is pinned we additionally require being in that area, otherwise a
     * pin would be ignored whenever any valid spot happened to be loaded elsewhere.</p>
     */
    /**
     * "Arrived": a spot for this stage is interactable <em>and</em> we are in one of the
     * stage's curated areas.
     *
     * <p>The area check is not redundant. Several stages have spots scattered across the map,
     * so a bare "is a spot in range" test also passes at any incidental spot the player
     * happens to stand near - which made the script settle wherever a walk was interrupted
     * instead of at the bank-adjacent location it chose.</p>
     */
    private boolean isAtLocation(FishingStage stage) {
        if (!isInRange(findNearestSpot(stage))) {
            return false;
        }
        FishingLocation pinned = pinnedLocation();
        if (pinned != null) {
            WorldPoint player = Rs2Player.getWorldLocation();
            return player != null && player.distanceTo(pinned.getPoint()) <= PINNED_RADIUS;
        }
        FishingLocation nearest = stage.getClosestLocation(Rs2Player.getWorldLocation(),
                AIOFishingPlugin.isMembersWorld(config));
        return nearest == null || isNearArea(nearest.getPoint());
    }

    /** True when the player is inside the given fishing area, not merely near some spot. */
    private boolean isNearArea(WorldPoint area) {
        return isNearArea(area, FISHING_AREA_RADIUS);
    }

    boolean isNearArea(WorldPoint area, int radius) {
        WorldPoint player = Rs2Player.getWorldLocation();
        return player != null && area != null && player.distanceTo(area) <= radius;
    }

    private boolean isInRange(Rs2NpcModel spot) {
        if (spot == null) {
            return false;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint spotLocation = spot.getWorldLocation();
        if (player == null || spotLocation == null) {
            return false;
        }
        if (player.distanceTo(spotLocation) > SPOT_INTERACT_RANGE) {
            return false;
        }
        return Microbot.getClientThread()
                .runOnClientThreadOptional(spot::hasLineOfSight)
                .orElse(false);
    }

    private Rs2NpcModel findNearestSpot(FishingStage stage) {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        int[] ids = stage.getSpotIds();
        try {
            return Microbot.getRs2NpcCache().query()
                    .where(npc -> Arrays.stream(ids).anyMatch(id -> npc.getId() == id))
                    .nearestOnClientThread(SPOT_DISCOVERY_RANGE);
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("interrupted waiting for client thread")) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private String resolveAction(Rs2NpcModel spot) {
        if (spot == null || spot.getNpc() == null) {
            return "";
        }
        NPCComposition composition = spot.getNpc().getTransformedComposition();
        if (composition == null || composition.getActions() == null) {
            return "";
        }
        List<String> wanted = activeStage.getMethod().getActions();
        return Arrays.stream(composition.getActions())
                .filter(a -> a != null && wanted.stream().anyMatch(w -> w.equalsIgnoreCase(a)))
                .findFirst()
                .orElse("");
    }

    private void activateHarpoonSpec() {
        if (selectedHarpoon == HarpoonType.NONE
                || activeStage.getMethod() != net.runelite.client.plugins.microbot.aiofishing.enums.FishingMethod.HARPOON) {
            return;
        }
        if (!Rs2Equipment.isWearing(selectedHarpoon.getItemName())) {
            return;
        }
        if (Rs2Combat.getSpecEnergy() >= 100) {
            Rs2Combat.setSpecState(true, 1000);
            sleepUntil(() -> Rs2Combat.getSpecEnergy() < 100, 2000);
        }
    }

    /**
     * Start the post-action cooldown. Deliberately does <em>not</em> trigger micro breaks.
     *
     * <p>{@code takeMicroBreakByChance()} only raises {@code microBreakActive}; the flag is
     * cleared by BreakHandlerScript, so calling it without the Break Handler plugin running
     * leaves a latch nobody lowers. That is also why the fishing template sets
     * {@code takeMicroBreaks = false} - breaks are opt-in through Break Handler, not
     * something a script switches on behalf of the user. This method owns only the action
     * cooldown, which the loop self-gates on via {@code actionCooldownActive}.</p>
     */
    void safeAntibanCooldown() {
        try {
            Rs2Antiban.actionCooldown();
        } catch (IllegalArgumentException e) {
            log.warn("Antiban cooldown skipped: {}", e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        // Closing the bank touches widgets via the client thread. If that thread is busy
        // (or we were called from an awkward one) it can time out - never let that stop the
        // rest of the reset from happening, or the script is left in a half-stopped state.
        try {
            if (Rs2Bank.isOpen()) {
                Rs2Bank.closeBank();
            }
        } catch (Exception ex) {
            log.debug("Could not close bank during shutdown: {}", ex.toString());
        }
        cachedAction = "";
        paused = false;
        fishingInteractionActive = false;
        supplies.reset();
        grandExchange.reset();
        debug.reset();
        lastFatalError = null;
        clearTravelTarget();
        state = AIOFishingState.IDLE;
    }
}
