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
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingShop;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.aiofishing.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.aiofishing.enums.HarpoonType;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.microbot.aiofishing.enums.AnglerGear;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishStorage;
import net.runelite.client.plugins.microbot.aiofishing.enums.RadasBlessing;
import net.runelite.client.plugins.microbot.aiofishing.enums.AerialCatch;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingActivity;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2BankID;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.item.Rs2ItemManager;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@Slf4j
public class AIOFishingScript extends Script {

    /**
     * Top up a consumable once the carried amount drops below this. Kept well under the
     * configurable withdraw target so we aren't banking constantly.
     */
    private static final int CONSUMABLE_MIN = 25;
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
    /** How close to the Molch Island shore tile counts as in position. */
    private static final int AERIAL_SPOT_RADIUS = 12;
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
    /** Big-net junk is always dropped; useful bycatch follows the user's keep toggles. */
    private static final List<String> BIG_NET_JUNK = List.of("Leather boots", "Leather gloves");
    /** GE booths are wall objects and are not included in the normal bank-object ID set. */
    private static final Set<Integer> GRAND_EXCHANGE_BOOTH_IDS = Set.of(10060, 30389);
    /** Give up on a GE buy after this many price escalations, rather than looping. */
    private static final int MAX_BUY_ATTEMPTS = 5;
    /**
     * Abandon a GE sale after this many failed attempts. Without a cap, an item the
     * price lookup cannot value (or that the GE keeps rejecting) leaves pendingSaleItem
     * set, so determineState() returns SELLING forever and the script retries on every
     * tick instead of getting on with fishing.
     */
    private static final int MAX_SELL_ATTEMPTS = 4;
    /** How long to wait for a buy offer to fill before escalating the price. */
    private static final int BUY_FILL_TIMEOUT_MS = 90000;
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
    /** Item name -> quantity still to buy. Empty when nothing is on order. */
    private final Map<String, Integer> pendingPurchases = new LinkedHashMap<>();
    /** Shop chosen for the pending purchase, decided once while still at the bank. */
    private volatile FishingShop pendingShop;
    /** True when the pending purchase goes to the Grand Exchange instead of a shop. */
    private volatile boolean pendingViaGe;
    /** Per-item failed buy count, used to escalate the offer price. */
    private final Map<String, Integer> buyAttempts = new HashMap<>();
    /** Consecutive processing passes that consumed nothing; see MAX_PROCESSING_STALLS. */
    private int processingStalls;
    /**
     * Non-null once something unrecoverable happened (no bait and no way to buy it, etc).
     * determineState() is recomputed every tick, so a handler setting state = STOPPED is
     * not enough on its own - this flag is what makes a stop stick.
     */
    @Getter
    private volatile String stopReason;
    /** Catch item currently withdrawn as notes and awaiting a GE listing. */
    @Getter
    private volatile String pendingSaleItem;
    /** Per-item failed listing count, used to undercut progressively. */
    private final Map<String, Integer> sellAttempts = new HashMap<>();
    /** Catch items we gave up selling - don't re-withdraw them every bank trip. */
    private final Set<String> unsellable = new HashSet<>();
    /** Last applied debug override, used to initialise forced workflows exactly once. */
    @Getter
    private volatile AIOFishingDebugMode activeDebugMode = AIOFishingDebugMode.AUTOMATIC;
    /** Why a forced workflow is waiting for the user to provide a safe prerequisite. */
    @Getter
    private volatile String debugBlockReason;
    /** Last sale scan result, preventing the forced debug loop from repeating the same log. */
    @Getter
    private volatile String lastSaleScanDiagnostic;
    /** Fatal loop failure captured before ScheduledExecutorService suppresses it. */
    @Getter
    private volatile String lastFatalError;

    public boolean run(AIOFishingConfig config) {
        this.config = config;
        this.selectedHarpoon = config.harpoonSpec();
        this.paused = false;
        this.fishingInteractionActive = false;
        this.cachedAction = "";
        this.pendingPurchases.clear();
        this.pendingShop = null;
        this.pendingViaGe = false;
        this.processingStalls = 0;
        this.buyAttempts.clear();
        this.pendingSaleItem = null;
        this.sellAttempts.clear();
        this.unsellable.clear();
        this.activeDebugMode = AIOFishingDebugMode.AUTOMATIC;
        this.debugBlockReason = null;
        this.lastSaleScanDiagnostic = null;
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
    private void requestStop(String reason) {
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
            if (debugBlockReason != null) {
                Microbot.status = debugBlockReason;
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

            if (activeDebugMode == AIOFishingDebugMode.WALKING_TO_BANK) {
                openBankAsSoonAsAvailable();
                return;
            }
            if (activeDebugMode == AIOFishingDebugMode.WALKING_TO_FISHING_SPOT) {
                handleTraveling();
                return;
            }
            if (activeDebugMode == AIOFishingDebugMode.SELLING_ON_GE) {
                handleDebugSelling();
                return;
            }

            switch (state) {
                case SHOPPING:   handleShopping();  break;
                case SELLING:    handleSelling();   break;
                case BUYING:     handleBuying();    break;
                case GEARING:    handleGearing();   break;
                case TRAVELING:  handleTraveling(); break;
                case FISHING:    handleFishing();   break;
                case PROCESSING: handleProcessing(); break;
                case BANKING:    handleBanking();   break;
                case DROPPING:   handleDropping();  break;
                case AERIAL_FISHING: handleAerialFishing(); break;
                case AERIAL_CUTTING: handleAerialCutting(); break;
                case AERIAL_BAITING: handleAerialBaiting(); break;
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
        AIOFishingState forcedState = determineDebugState();
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
            return determineAerialState();
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
        if (!pendingPurchases.isEmpty()) {
            return pendingViaGe ? AIOFishingState.BUYING : AIOFishingState.SHOPPING;
        }
        // Carrying notes we have not listed yet - clear them before anything else, or
        // they would just be deposited again on the next bank trip.
        if (pendingSaleItem != null) {
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
        if (!hasGear(activeStage)) {
            return AIOFishingState.GEARING;
        }
        if (!isAtLocation(activeStage)) {
            return AIOFishingState.TRAVELING;
        }
        return AIOFishingState.FISHING;
    }

    private AIOFishingState determineDebugState() {
        AIOFishingDebugMode configuredMode = config.debugMode();
        if (configuredMode != activeDebugMode) {
            pendingPurchases.clear();
            pendingShop = null;
            pendingViaGe = false;
            buyAttempts.clear();
            pendingSaleItem = null;
            sellAttempts.clear();
            unsellable.clear();
            debugBlockReason = null;
            lastSaleScanDiagnostic = null;
            activeDebugMode = configuredMode;
            log.info("AIO Fishing debug workflow changed to {}.", configuredMode);
        }

        switch (configuredMode) {
            case SELLING_ON_GE:
                prepareDebugSale();
                return AIOFishingState.SELLING;
            case RESUPPLYING_FROM_SHOP:
                prepareDebugPurchase(false);
                return AIOFishingState.SHOPPING;
            case RESUPPLYING_FROM_GE:
                prepareDebugPurchase(true);
                return AIOFishingState.BUYING;
            case WALKING_TO_BANK:
                debugBlockReason = null;
                return AIOFishingState.BANKING;
            case WALKING_TO_FISHING_SPOT:
                debugBlockReason = null;
                return AIOFishingState.TRAVELING;
            default:
                debugBlockReason = null;
                return null;
        }
    }

    private void prepareDebugSale() {
        if (pendingSaleItem != null) {
            debugBlockReason = null;
            return;
        }
        for (String itemName : activeStage.getCatchItemNames()) {
            if (!itemName.toLowerCase().contains("burnt") && Rs2Inventory.hasItem(itemName)) {
                pendingSaleItem = itemName;
                debugBlockReason = null;
                return;
            }
        }
        // No carried catch is expected when testing automatic withdrawal. The forced
        // workflow will visit the bank and run the normal stack/value selection there.
        debugBlockReason = null;
    }

    private void prepareDebugPurchase(boolean useGrandExchange) {
        if (!pendingPurchases.isEmpty()) {
            debugBlockReason = null;
            return;
        }
        if (!Rs2Inventory.hasItem("Coins")) {
            setDebugBlockReason("Debug resupply: carry coins in the inventory");
            return;
        }

        String itemName = findMissingDebugSupply();
        if (itemName == null) {
            setDebugBlockReason("Debug resupply: remove an active-stage tool or supply first");
            return;
        }
        int quantity = activeStage.getMethod().getConsumables().contains(itemName)
                ? config.buyQuantity()
                : 1;

        FishingShop shop = null;
        if (!useGrandExchange) {
            shop = FishingShop.findNearest(itemName, AIOFishingPlugin.isMembersWorld(config),
                    AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS,
                    Rs2Player.getWorldLocation(), AIOFishingScript::pathTiles);
            if (shop == null) {
                setDebugBlockReason("Debug shop resupply: no reachable shop stocks " + itemName);
                return;
            }
        }

        pendingPurchases.put(itemName, quantity);
        pendingShop = shop;
        pendingViaGe = useGrandExchange;
        debugBlockReason = null;
        log.info("Debug resupply queued: {} x{} via {}.", itemName, quantity,
                useGrandExchange ? "Grand Exchange" : shop);
    }

    private String findMissingDebugSupply() {
        for (String consumable : activeStage.getMethod().getConsumables()) {
            if (!Rs2Inventory.hasItem(consumable)) {
                return consumable;
            }
        }
        for (String tool : requiredTools(activeStage)) {
            if (!hasToolAvailable(tool)) {
                return tool;
            }
        }
        return null;
    }

    /**
     * Whether a tool is usable: carried, worn, or - with the tackle box enabled - stored in
     * the box. The box keeps fishing gear in one slot, so without this the script would keep
     * withdrawing duplicates of tools it already owns.
     */
    private boolean hasToolAvailable(String tool) {
        if (Rs2Inventory.hasItem(tool) || Rs2Equipment.isWearing(tool)) {
            return true;
        }
        return config.useTackleBox() && Rs2Inventory.hasItem(ItemID.TACKLE_BOX);
    }

    private void setDebugBlockReason(String reason) {
        if (!reason.equals(debugBlockReason)) {
            log.warn(reason);
        }
        debugBlockReason = reason;
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

    private static int pathTiles(WorldPoint from, WorldPoint to) {
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
        ensureFishStorageOpen();
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
        dropUnwantedBigNetBycatch();
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        // Dump the configured barrel first so its fish are banked with the rest. Handles the
        // fish sack barrel too, which Rs2Bank.emptyFishBarrel() does not cover.
        emptyFishStorage();
        Rs2Bank.depositAllExcept(itemsToKeep(activeStage).toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(1800);
        sleepUntil(() -> !Rs2Inventory.isFull(), 5000);

        // With enough of a valuable fish banked, take it out as notes for a GE trip.
        withdrawStackForSale();

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        safeAntibanCooldown();
    }

    private void handleDropping() {
        List<String> toDrop = new ArrayList<>(activeStage.getCatchItemNames());
        toDrop.addAll(unwantedBigNetBycatch());
        Rs2Inventory.dropAll(toDrop.toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(2000);
        safeAntibanCooldown();
    }

    private void handleGearing() {
        dropUnwantedBigNetBycatch();
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        // Keep only what this stage needs; deposit last stage's leftovers and any catch.
        Rs2Bank.depositAllExcept(itemsToKeep(activeStage).toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(1200);

        // Optional bonus gear first: outfit / blessing / barrel. Best-effort, never fatal.
        equipBonusGear();

        // Withdraw & equip the reusable tools.
        for (String tool : requiredTools(activeStage)) {
            if (Rs2Inventory.hasItem(tool) || Rs2Equipment.isWearing(tool)) {
                continue;
            }
            if (!Rs2Bank.hasBankItem(tool)) {
                String failure = queuePurchase(tool, 1);
                if (failure == null) {
                    return; // head to the shop, then come back and finish gearing
                }
                Rs2Bank.closeBank();
                requestStop("No '" + tool + "' in bank - " + failure);
                return;
            }
            Rs2Bank.withdrawItem(tool);
            sleepUntil(() -> Rs2Inventory.hasItem(tool), 3000);
            if (isWieldable(tool)) {
                Rs2Inventory.wield(tool);
                sleepUntil(() -> Rs2Equipment.isWearing(tool), 2000);
            }
        }

        // Top up consumables (feathers/bait).
        for (String consumable : activeStage.getMethod().getConsumables()) {
            if (Rs2Inventory.itemQuantity(consumable) >= CONSUMABLE_MIN) {
                continue;
            }
            if (!Rs2Bank.hasBankItem(consumable)) {
                // Consumables use the configured buy amount; tools are always 1.
                String failure = queuePurchase(consumable, config.buyQuantity());
                if (failure == null) {
                    return; // head to the shop, then come back and finish gearing
                }
                Rs2Bank.closeBank();
                requestStop("Out of '" + consumable + "' - " + failure);
                return;
            }
            Rs2Bank.withdrawDeficit(consumable, config.withdrawAmount(), false);
            Rs2Inventory.waitForInventoryChanges(1500);
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        safeAntibanCooldown();

        if (!hasGear(activeStage)) {
            requestStop("Could not gear up for " + activeStage.getDisplayName());
        }
    }

    /**
     * Queue a shop trip for an item the bank couldn't supply.
     *
     * <p>Only queues when supply-buying is enabled AND a known shop stocks the item, so we
     * never set off towards a shop that can't help. Coins are withdrawn here because we're
     * already standing at the bank.</p>
     *
     * @return null when a trip was queued, otherwise why it couldn't be - the caller puts
     * that in the stop reason, so the user is told the actual cause rather than a guess
     */
    private String queuePurchase(String itemName, int quantity) {
        if (!config.buySupplies()) {
            return "buying is off";
        }
        // Decide the shop once, here at the bank - findNearest runs pathfinds.
        FishingShop shop = FishingShop.findNearest(itemName, AIOFishingPlugin.isMembersWorld(config),
                AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS,
                Rs2Player.getWorldLocation(), AIOFishingScript::pathTiles);
        boolean viaGe = false;
        if (shop == null) {
            if (!config.buyFromGe()) {
                return "no reachable shop sells it";
            }
            viaGe = true; // fall back to the Grand Exchange
        }
        if (!Rs2Bank.hasBankItem("Coins") && !Rs2Inventory.hasItem("Coins")) {
            return "no coins in bank or inventory";
        }
        if (!Rs2Inventory.hasItem("Coins")) {
            Rs2Bank.withdrawAll("Coins");
            Rs2Inventory.waitForInventoryChanges(1500);
        }
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);

        pendingPurchases.put(itemName, quantity);
        pendingShop = shop;
        pendingViaGe = viaGe;
        log.info("Queued purchase: {} x{} from {}", itemName, quantity,
                viaGe ? "the Grand Exchange" : shop);
        return null;
    }

    /**
     * Buy the queued items. Stock is verified once the shop is open - the stock lists in
     * {@link FishingShop} are hints, so if the item isn't really there we stop rather than
     * bounce between bank and shop forever.
     */
    private void handleShopping() {
        if (pendingPurchases.isEmpty()) {
            return; // state can lag a tick behind the queue being cleared
        }
        Map.Entry<String, Integer> next = pendingPurchases.entrySet().iterator().next();
        String itemName = next.getKey();
        int quantity = next.getValue();

        FishingShop shop = pendingShop != null
                ? pendingShop
                : FishingShop.findFor(itemName, AIOFishingPlugin.isMembersWorld(config),
                        AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS);
        if (shop == null) {
            pendingPurchases.remove(itemName);
            return;
        }

        // Bind readiness to this shop's location as well as its NPC name: the name match is
        // a substring and location-agnostic, so a same-named trader elsewhere would otherwise
        // end the walk early - the same defect that was cancelling fishing journeys.
        if (!isAtShop(shop)) {
            if (!walkUntilNotPaused(shop.getLocation(), SHOP_WALK_TOLERANCE,
                    () -> isAtShop(shop))) {
                return;
            }
        }
        if (!isAtShop(shop)) {
            return;
        }

        if (!Rs2Shop.openShop(shop.getNpcName()) || !Rs2Shop.isOpen()) {
            pendingPurchases.clear();
            requestStop("Could not open " + shop);
            return;
        }

        if (!Rs2Shop.hasStock(itemName)) {
            Rs2Shop.closeShop();
            pendingPurchases.clear();
            requestStop(shop + " does not stock '" + itemName + "'");
            return;
        }

        Rs2Shop.buyItemOptimally(itemName, quantity);
        Rs2Inventory.waitForInventoryChanges(2000);
        Rs2Shop.closeShop();
        sleepUntil(() -> !Rs2Shop.isOpen(), 3000);

        if (!Rs2Inventory.hasItem(itemName)) {
            pendingPurchases.clear();
            requestStop("Bought '" + itemName + "' at " + shop + " but none arrived - out of coins?");
            return;
        }

        log.info("Bought {} at {}.", itemName, shop);
        pendingPurchases.remove(itemName);
        pendingShop = null;
        safeAntibanCooldown();
    }

    /**
     * If a catch item has built up past the configured stack size and is worth more than
     * the configured floor, withdraw it as notes so the next state is a GE trip. Only the
     * current stage's catch items are considered, so tools can never be sold by accident.
     */
    private boolean withdrawStackForSale() {
        return withdrawStackForSale(false);
    }

    private boolean withdrawStackForSale(boolean debugOverride) {
        if ((!config.sellOnGe() && !debugOverride) || pendingSaleItem != null) {
            return false;
        }
        String rejection = "no active-stage catch found in the bank";
        for (String name : activeStage.getCatchItemNames()) {
            if (name.toLowerCase().contains("burnt")) {
                continue;
            }
            if (unsellable.contains(name.toLowerCase())) {
                rejection = name + " was abandoned earlier after repeated listing failures";
                continue;
            }
            Rs2ItemModel item = Rs2Bank.getBankItem(name, true);
            if (item == null) {
                continue;
            }
            int banked = item.getQuantity();
            if (banked < config.sellStackSize()) {
                rejection = name + " x" + banked + " is below stack threshold "
                        + config.sellStackSize();
                continue;
            }
            int unitPrice = getRuneLitePrice(item.getId());
            if (unitPrice < config.minFishValue()) {
                rejection = name + " x" + banked + " has RuneLite price " + unitPrice
                        + " gp, below floor " + config.minFishValue();
                continue;
            }

            int quantityToWithdraw = config.withdrawAllForSale()
                    ? banked
                    : Math.min(banked, config.sellStackSize());
            debugSaleDiagnostic(name + " x" + banked + " is eligible at " + unitPrice
                    + " gp; requesting " + quantityToWithdraw + " noted");

            if (!Rs2Bank.setWithdrawAs(true)) {
                debugSaleDiagnostic("could not enable noted withdrawal");
                return false;
            }
            boolean withdrawalRequested = config.withdrawAllForSale()
                    ? Rs2Bank.withdrawAll(item.getId())
                    : Rs2Bank.withdrawX(item.getId(), quantityToWithdraw);
            if (!withdrawalRequested) {
                Rs2Bank.setWithdrawAs(false);
                debugSaleDiagnostic("bank API rejected withdrawal of " + name);
                return false;
            }

            boolean arrived = sleepUntil(() -> Rs2Inventory.hasItem(name), 3000);
            Rs2Bank.setWithdrawAs(false);

            if (arrived) {
                pendingSaleItem = name;
                lastSaleScanDiagnostic = null;
                log.info("Withdrew {} x{} (~{} gp each) as notes to sell.",
                        name, quantityToWithdraw, unitPrice);
                return true;
            }
            debugSaleDiagnostic("withdrawal was invoked but " + name
                    + " did not reach the inventory");
            return false; // one stack per trip
        }
        debugSaleDiagnostic(rejection);
        return false;
    }

    private void debugSaleDiagnostic(String detail) {
        if (activeDebugMode != AIOFishingDebugMode.SELLING_ON_GE
                || detail.equals(lastSaleScanDiagnostic)) {
            return;
        }
        lastSaleScanDiagnostic = detail;
        Microbot.status = "Debug selling: " + detail;
        log.info("Debug sale scan: {}", detail);
    }

    /**
     * Exercise the complete automatic-sale path without waiting for a full inventory.
     * Normal stack size, value floor and withdraw-all settings still apply.
     */
    private void handleDebugSelling() {
        prepareDebugSale();
        if (pendingSaleItem != null) {
            handleSelling();
            return;
        }
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        if (!withdrawStackForSale(true)) {
            return;
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
    }

    /**
     * List the noted stack on the Grand Exchange, then go straight back to fishing - we do
     * not block waiting for the offer to fill. Anything already sold is collected to the
     * bank on arrival, and a stale unsold offer is aborted and relisted lower.
     */
    private void handleSelling() {
        if (pendingSaleItem == null) {
            return;
        }
        if (!Rs2Inventory.hasItem(pendingSaleItem)) {
            pendingSaleItem = null; // notes gone (already listed or deposited)
            return;
        }
        if (!walkToGrandExchangeNotPaused()) {
            return;
        }
        if (!Rs2GrandExchange.openExchange()) {
            requestStop("Could not open the Grand Exchange");
            return;
        }

        // Bank anything that already sold; this also frees slots.
        Rs2GrandExchange.collectAllToBank();
        sleep(600);

        int attempt = sellAttempts.getOrDefault(pendingSaleItem, 0);
        // Every failure path (unpriceable item, full GE, rejected listing) funnels through
        // this counter, so a sale can never retry indefinitely.
        if (attempt >= MAX_SELL_ATTEMPTS) {
            log.warn("Giving up selling {} after {} attempts - leaving the stack banked.",
                    pendingSaleItem, attempt);
            unsellable.add(pendingSaleItem.toLowerCase());
            sellAttempts.remove(pendingSaleItem);
            pendingSaleItem = null;
            Rs2GrandExchange.closeExchange();
            return;
        }
        if (Rs2GrandExchange.getAvailableSlotsCount() <= 0) {
            // Most likely our own unsold offer holds the slot - pull it and relist lower.
            log.info("No free GE slot; aborting stale offer for {} and relisting.", pendingSaleItem);
            Rs2GrandExchange.abortOffer(pendingSaleItem, true);
            sellAttempts.put(pendingSaleItem, attempt + 1);
            Rs2GrandExchange.closeExchange();
            return;
        }

        Rs2ItemModel notes = Rs2Inventory.get(pendingSaleItem);
        int quantity = Rs2Inventory.itemQuantity(pendingSaleItem);
        int price = resolveSellPrice(notes == null ? -1 : notes.getId(), attempt);

        if (price > 0 && Rs2GrandExchange.sellItem(pendingSaleItem, quantity, price)) {
            log.info("Listed {} x{} at {} gp each (attempt {}).",
                    pendingSaleItem, quantity, price, attempt + 1);
            sellAttempts.remove(pendingSaleItem);
            pendingSaleItem = null;
        } else {
            sellAttempts.put(pendingSaleItem, attempt + 1);
            log.warn("Failed to list {} (no price or listing rejected) - attempt {} of {}.",
                    pendingSaleItem, attempt + 1, MAX_SELL_ATTEMPTS);
        }
        Rs2GrandExchange.closeExchange();
        safeAntibanCooldown();
    }

    /** Price per item for a sell offer, honouring the configured pricing mode. */
    private int resolveSellPrice(int itemId, int attempt) {
        if (config.sellPricing() == GeSellPricing.CUSTOM) {
            return Math.max(1, config.customSellPrice());
        }
        int marketPrice = getRuneLitePrice(itemId);
        if (marketPrice <= 0) {
            return -1;
        }
        if (config.sellPricing() == GeSellPricing.MARKET) {
            return marketPrice;
        }
        // ADAPTIVE: start at RuneLite's market price, undercut a further 5% per failed attempt.
        double retryMultiplier = Math.max(0.05, 1.0 - attempt * 0.05);
        return Math.max(1, (int) Math.floor(marketPrice * retryMultiplier));
    }

    /**
     * RuneLite prices are keyed by the canonical tradeable item. Inventory stacks withdrawn
     * as notes have a different ID (for example Raw trout is 335, noted Raw trout is 336),
     * so canonicalize only for pricing while leaving the noted stack untouched for the sale.
     */
    private int getRuneLitePrice(int itemId) {
        if (itemId <= 0) {
            return -1;
        }
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            int canonicalItemId = Microbot.getItemManager().canonicalize(itemId);
            return Microbot.getItemManager().getItemPrice(canonicalItemId);
        }).orElse(-1);
    }

    /**
     * Buy a supply on the Grand Exchange. Used only when no reachable fishing shop stocks
     * the item (dark fishing bait, raw karambwanji) and the GE fallback is enabled.
     *
     * <p>Unlike selling, we cannot walk away - the item is needed before fishing can
     * resume - so this waits for the offer to fill, then escalates the price on the next
     * attempt if it did not. It gives up after {@link #MAX_BUY_ATTEMPTS} rather than
     * retrying forever.</p>
     */
    private void handleBuying() {
        if (pendingPurchases.isEmpty()) {
            return;
        }
        Map.Entry<String, Integer> next = pendingPurchases.entrySet().iterator().next();
        String itemName = next.getKey();
        int quantity = next.getValue();

        // Already collected on an earlier pass.
        if (Rs2Inventory.hasItem(itemName)) {
            finishBuy(itemName);
            return;
        }

        int attempt = buyAttempts.getOrDefault(itemName, 0);
        if (attempt >= MAX_BUY_ATTEMPTS) {
            pendingPurchases.clear();
            pendingViaGe = false;
            requestStop("Could not buy '" + itemName + "' on the GE after "
                    + MAX_BUY_ATTEMPTS + " attempts");
            return;
        }

        if (!walkToGrandExchangeNotPaused()) {
            return;
        }
        if (!Rs2GrandExchange.openExchange()) {
            requestStop("Could not open the Grand Exchange");
            return;
        }

        Rs2GrandExchange.collectAllToInventory();
        sleep(600);
        if (Rs2Inventory.hasItem(itemName)) {
            Rs2GrandExchange.closeExchange();
            finishBuy(itemName);
            return;
        }

        int itemId = Rs2ItemManager.getItemIdByName(itemName, false);
        if (itemId <= 0) {
            Rs2GrandExchange.closeExchange();
            pendingPurchases.clear();
            pendingViaGe = false;
            requestStop("No GE item found named '" + itemName + "'");
            return;
        }

        if (Rs2GrandExchange.getAvailableSlotsCount() <= 0) {
            log.info("No free GE slot; aborting our stale offer for {}.", itemName);
            Rs2GrandExchange.abortOffer(itemName, false);
            buyAttempts.put(itemName, attempt + 1);
            Rs2GrandExchange.closeExchange();
            return;
        }

        // Base 110% of market so it actually fills; +5% per failed attempt.
        int price = Rs2GrandExchange.getAdaptiveBuyPrice(itemId, 1.1, attempt);
        if (price <= 0) {
            Rs2GrandExchange.closeExchange();
            pendingPurchases.clear();
            pendingViaGe = false;
            requestStop("No GE price available for '" + itemName + "'");
            return;
        }

        if (!Rs2GrandExchange.buyItem(itemName, price, quantity)) {
            buyAttempts.put(itemName, attempt + 1);
            Rs2GrandExchange.closeExchange();
            return;
        }
        log.info("Placed GE buy: {} x{} at {} gp each (attempt {}).",
                itemName, quantity, price, attempt + 1);

        // We need the item to carry on fishing, so wait for the fill (bounded).
        sleepUntil(Rs2GrandExchange::hasBoughtOffer, BUY_FILL_TIMEOUT_MS);
        Rs2GrandExchange.collectAllToInventory();
        Rs2Inventory.waitForInventoryChanges(2000);
        Rs2GrandExchange.closeExchange();

        if (Rs2Inventory.hasItem(itemName)) {
            finishBuy(itemName);
            log.info("Bought {} on the Grand Exchange.", itemName);
        } else {
            buyAttempts.put(itemName, attempt + 1);
            log.warn("GE offer for {} did not fill; retrying higher.", itemName);
        }
        safeAntibanCooldown();
    }

    private void finishBuy(String itemName) {
        pendingPurchases.remove(itemName);
        buyAttempts.remove(itemName);
        pendingViaGe = false;
    }

    // ------------------------------------------------------- aerial fishing

    /**
     * Aerial state: verify readiness once, then alternate between throwing the cormorant
     * and knifing the catch into offcuts. Readiness failures stop with a reason rather than
     * silently idling, because none of them can be fixed by waiting.
     */
    private AIOFishingState determineAerialState() {
        String unmet = AerialFishing.unmetReason(AIOFishingPlugin.SKILL_LEVELS);
        if (unmet != null) {
            requestStop("Aerial fishing: " + unmet);
            return AIOFishingState.STOPPED;
        }
        // Cut when there is no room left to receive another fish. One slot has to stay free
        // for the incoming catch, so cut at <=1 free rather than waiting for a full pack.
        if (AerialFishing.hasUncutCatch()
                && (Rs2Inventory.isFull() || Rs2Inventory.emptySlotCount() <= 1)) {
            return AIOFishingState.AERIAL_CUTTING;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        if (player == null || player.distanceTo(AerialFishing.MOLCH_ISLAND_SPOT) > AERIAL_SPOT_RADIUS) {
            return AIOFishingState.TRAVELING;
        }
        // Out of bait: gather king worms off the ground rather than banking for them. Only
        // needed to bootstrap - after the first catch the knifed offcuts are the bait.
        if (!AerialFishing.hasBait() && !AerialFishing.hasUncutCatch()) {
            return AIOFishingState.AERIAL_BAITING;
        }
        return AIOFishingState.AERIAL_FISHING;
    }

    /** Throw the cormorant at the nearest pool and wait for it to come back with a fish. */
    private void handleAerialFishing() {
        if (AerialFishing.birdIsOut() || Rs2Player.isInteracting()) {
            return; // catch already in flight
        }
        Rs2NpcModel pool = Microbot.getRs2NpcCache().query()
                .withId(NpcID.FISHING_SPOT_AERIAL)
                .nearest();
        if (pool == null) {
            return;
        }
        // The pool has to be on screen to be clicked; turn the camera if it is not. Note
        // Rs2Npc.validateInteractable takes a different Rs2NpcModel class (util.npc, not
        // api.npc.models) and also walks, so turn the camera directly instead.
        if (pool.getNpc() != null && !Rs2Camera.isTileOnScreen(pool.getNpc().getLocalLocation())) {
            Rs2Camera.turnTo(pool.getNpc());
        }
        if (pool.click()) {
            // The glove id flips while the bird is away, which is a cleaner "in progress"
            // signal than an animation check.
            sleepUntil(AerialFishing::birdIsOut, 1500);
            sleepUntil(() -> !AerialFishing.birdIsOut(), 6000);
            safeAntibanCooldown();
        }
    }

    /**
     * Pick king worms off the ground to start the bait cycle. Stops only if there are none
     * left to take, since without bait the cormorant cannot be sent.
     */
    private void handleAerialBaiting() {
        if (Rs2Inventory.itemQuantity(AerialFishing.GROUND_BAIT_NAME) >= config.wormsToPickUp()) {
            return; // enough gathered; determineAerialState moves on next tick
        }
        var worm = Microbot.getRs2TileItemCache().query()
                .withName(AerialFishing.GROUND_BAIT_NAME)
                .nearestOnClientThread(AerialFishing.GROUND_BAIT_RANGE);
        if (worm == null) {
            if (!AerialFishing.hasBait()) {
                requestStop("Aerial fishing: no king worms on the ground - wait for a respawn"
                        + " or bring bait");
            }
            return;
        }
        // click() does not walk, so close the gap first (same trap as the fishing spots).
        WorldPoint wormTile = worm.getWorldLocation();
        WorldPoint player = Rs2Player.getWorldLocation();
        if (wormTile != null && player != null && player.distanceTo(wormTile) > 4) {
            Rs2Walker.walkTo(wormTile, 1);
            return;
        }
        if (worm.click("Take")) {
            Rs2Inventory.waitForInventoryChanges(1500);
        }
    }

    /** Knife the catch into stackable offcuts, which double as the next lot of bait. */
    private void handleAerialCutting() {
        Rs2ItemModel knife = Rs2Inventory.get(ItemID.KNIFE);
        if (knife == null) {
            requestStop("Aerial fishing: carry a knife to cut the catch");
            return;
        }
        Rs2ItemModel fish = Rs2Inventory.getRandom(AerialCatch.rawItemIds());
        if (fish == null) {
            return; // nothing left to cut
        }
        Rs2Inventory.combine(knife, fish);
        sleepUntil(() -> !AerialFishing.hasUncutCatch(), 60000);
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

    /** At the intended shop: its keeper is interactable AND we are at its location. */
    private boolean isAtShop(FishingShop shop) {
        return shop != null
                && isNearArea(shop.getLocation(), SHOP_AREA_RADIUS)
                && isShopNpcInteractable(shop.getNpcName());
    }

    private boolean isShopNpcInteractable(String npcName) {
        var npc = Rs2Shop.getNearestShopNpc(npcName);
        if (npc == null) {
            return false;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint npcLocation = npc.getWorldLocation();
        return player != null && npcLocation != null
                && player.distanceTo(npcLocation) <= SHOP_INTERACT_RANGE;
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

    private boolean hasGear(FishingStage stage) {
        for (String tool : requiredTools(stage)) {
            if (!Rs2Inventory.hasItem(tool) && !Rs2Equipment.isWearing(tool)) {
                return false;
            }
        }
        for (String consumable : stage.getMethod().getConsumables()) {
            if (!Rs2Inventory.hasItem(consumable)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tools for a stage, substituting the configured special-attack harpoon for the
     * generic "Harpoon" so it gets withdrawn and worn on harpoon stages.
     */
    private List<String> requiredTools(FishingStage stage) {
        List<String> tools = new ArrayList<>(stage.getMethod().getTools());
        // The knife/hammer is only a requirement while the step can actually run: a
        // sub-72-Cooking account banks sacred eels whole and has no use for a knife.
        CatchProcessing processing = stage.getProcessing();
        if (config.processCatch() && processing.isUsable(AIOFishingPlugin.SKILL_LEVELS)) {
            tools.add(processing.getToolName());
        }
        // Mor Ul Rek's guard accepts an infernal cape too. Players who earned one have
        // usually long since dropped the fire cape, so honour what they actually carry
        // rather than demanding a cape they no longer own.
        if (tools.contains("Fire cape")
                && (Rs2Equipment.isWearing("Infernal cape") || Rs2Inventory.hasItem("Infernal cape"))) {
            tools.replaceAll(t -> t.equalsIgnoreCase("Fire cape") ? "Infernal cape" : t);
        }
        // Bare-handed fishing needs no equipment at all, so a harpoon stops being a
        // requirement (and the weapon slot stays free). Checked before the spec-harpoon
        // substitution below, which would otherwise reinstate it under a different name.
        if (isBareHandedActive()) {
            tools.removeIf(t -> t.equalsIgnoreCase("Harpoon"));
            return tools;
        }
        if (selectedHarpoon != HarpoonType.NONE) {
            tools.replaceAll(t -> t.equalsIgnoreCase("Harpoon") ? selectedHarpoon.getItemName() : t);
        }
        return tools;
    }

    /** True when the user asked for bare-handed fishing and Otto has actually taught it. */
    private boolean isBareHandedActive() {
        return config.bareHandedFishing()
                && BareHandedFishing.isAvailable(AIOFishingPlugin.SKILL_LEVELS,
                        AIOFishingPlugin.VARBIT_VALUES);
    }

    /**
     * Optional bonus gear: the Angler's outfit, a Rada's blessing and a fish barrel.
     *
     * <p>Deliberately best-effort. None of it is required to fish, so a missing piece is
     * skipped rather than treated as a reason to stop - otherwise enabling a nice-to-have
     * would strand the script. Assumes the bank is already open.</p>
     */
    private void equipBonusGear() {
        if (config.equipAnglerOutfit()) {
            for (AnglerGear piece : AnglerGear.values()) {
                if (Rs2Equipment.isWearing(piece.getItemIds())) {
                    continue;
                }
                Integer owned = firstOwnedId(piece.getItemIds());
                if (owned == null) {
                    continue; // not owned - nothing to do
                }
                if (!Rs2Inventory.hasItem(owned)) {
                    Rs2Bank.withdrawItem(owned);
                    sleepUntil(() -> Rs2Inventory.hasItem(owned), 2000);
                }
                if (Rs2Inventory.hasItem(owned)) {
                    Rs2Inventory.wield(owned);
                    sleepUntil(() -> Rs2Equipment.isWearing(owned), 1500);
                }
            }
        }

        RadasBlessing blessing = config.radasBlessing();
        if (blessing.isEnabled() && !Rs2Equipment.isWearing(blessing.getItemId())) {
            int id = blessing.getItemId();
            if (!Rs2Inventory.hasItem(id) && Rs2Bank.hasBankItem(id, 1)) {
                Rs2Bank.withdrawItem(id);
                sleepUntil(() -> Rs2Inventory.hasItem(id), 2000);
            }
            if (Rs2Inventory.hasItem(id)) {
                Rs2Inventory.wield(id);
                sleepUntil(() -> Rs2Equipment.isWearing(id), 1500);
            }
        }

        FishStorage storage = config.fishStorage();
        if (storage.isEnabled() && !Rs2Inventory.hasItem(storage.getItemIds())) {
            Integer owned = firstOwnedId(storage.getItemIds());
            if (owned != null) {
                Rs2Bank.withdrawItem(owned);
                sleepUntil(() -> Rs2Inventory.hasItem(owned), 2000);
            }
        }
    }

    /** First of these ids held in the inventory, worn, or sitting in the open bank. */
    private Integer firstOwnedId(int... ids) {
        for (int id : ids) {
            if (Rs2Inventory.hasItem(id) || Rs2Equipment.isWearing(id)
                    || Rs2Bank.hasBankItem(id, 1)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Open a carried barrel so it starts absorbing the catch.
     *
     * <p>A shut barrel collects nothing, so this is the whole of the "use a barrel" feature -
     * no filling and no counting. Checked from the fishing state rather than only after a
     * bank trip, so it is also correct when the script starts with a closed barrel already
     * in the inventory.</p>
     */
    private void ensureFishStorageOpen() {
        FishStorage storage = config.fishStorage();
        if (!storage.isEnabled()) {
            return;
        }
        Rs2ItemModel closed = Rs2Inventory.get(storage.getClosedId());
        if (closed == null) {
            return; // absent, or already open
        }
        if (Rs2Inventory.interact(closed, "Open")) {
            sleepUntil(() -> Rs2Inventory.hasItem(storage.getOpenId()), 2000);
        }
    }

    /** Empty a carried barrel so its fish are banked along with the inventory. */
    private void emptyFishStorage() {
        FishStorage storage = config.fishStorage();
        if (!storage.isEnabled()) {
            return;
        }
        Rs2ItemModel barrel = Rs2Inventory.get(storage.getItemIds());
        if (barrel != null) {
            Rs2Inventory.interact(barrel, "Empty");
            Rs2Inventory.waitForInventoryChanges(1200);
        }
    }

    private List<String> itemsToKeep(FishingStage stage) {
        Set<String> keep = new HashSet<>(requiredTools(stage));
        keep.addAll(stage.getMethod().getConsumables());
        // Names are matched as substrings, so "Fish barrel" covers both the closed and open
        // ids, and also protects a fish sack barrel from being deposited.
        keep.add("Fish barrel");
        if (config.fishStorage() == FishStorage.FISH_SACK_BARREL) {
            keep.add("Fish sack barrel");
        }
        if (config.useTackleBox()) {
            keep.add("Tackle box");
        }
        return new ArrayList<>(keep);
    }

    private void dropUnwantedBigNetBycatch() {
        List<String> unwanted = unwantedBigNetBycatch();
        if (unwanted.isEmpty()) {
            return;
        }
        unwanted.removeIf(name -> !Rs2Inventory.hasItem(name));
        if (unwanted.isEmpty()) {
            return;
        }
        Rs2Inventory.dropAll(unwanted.toArray(new String[0]));
        Rs2Inventory.waitForInventoryChanges(1200);
    }

    private List<String> unwantedBigNetBycatch() {
        if (activeStage != FishingStage.BIG_NET) {
            return List.of();
        }

        List<String> unwanted = new ArrayList<>(BIG_NET_JUNK);
        if (!config.keepCaskets()) {
            unwanted.add("Casket");
        }
        if (!config.keepOysters()) {
            unwanted.add("Oyster");
        }
        if (!config.keepSeaweed()) {
            unwanted.add("Seaweed");
        }
        return unwanted;
    }

    private boolean isWieldable(String tool) {
        String lower = tool.toLowerCase();
        // Gloves and capes are here for infernal eels: ice gloves stop the catch burning
        // your hands and the fire cape has to be *worn* for the guard to accept it, so
        // neither works sitting in the inventory.
        return lower.contains("rod") || lower.contains("harpoon")
                || lower.contains("gloves") || lower.contains("cape");
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

    private boolean isNearArea(WorldPoint area, int radius) {
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
        pendingPurchases.clear();
        pendingShop = null;
        pendingViaGe = false;
        pendingSaleItem = null;
        activeDebugMode = AIOFishingDebugMode.AUTOMATIC;
        debugBlockReason = null;
        lastSaleScanDiagnostic = null;
        lastFatalError = null;
        clearTravelTarget();
        state = AIOFishingState.IDLE;
    }
}
