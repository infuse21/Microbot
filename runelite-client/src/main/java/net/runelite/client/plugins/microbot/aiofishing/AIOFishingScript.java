package net.runelite.client.plugins.microbot.aiofishing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingState;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingLocation;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingShop;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.aiofishing.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.aiofishing.enums.HarpoonType;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
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
    /** Stop approaching as soon as the spot is close enough to click. */
    private static final int SPOT_WALK_TOLERANCE = SPOT_INTERACT_RANGE;
    /** How close counts as "in" a pinned location's area. */
    private static final int PINNED_RADIUS = 30;
    /** Let the NPC interaction finish the last few tiles instead of walking to its exact tile. */
    private static final int SHOP_INTERACT_RANGE = 15;
    /** Bank objects are discoverable within 20 tiles; stop just inside that search radius. */
    private static final int BANK_INTERACT_RANGE = 18;
    /** Big-net junk is always dropped; useful bycatch follows the user's keep toggles. */
    private static final List<String> BIG_NET_JUNK = List.of("Leather boots", "Leather gloves");
    /** Give up on a GE buy after this many price escalations, rather than looping. */
    private static final int MAX_BUY_ATTEMPTS = 5;
    /** How long to wait for a buy offer to fill before escalating the price. */
    private static final int BUY_FILL_TIMEOUT_MS = 90000;

    private AIOFishingConfig config;
    private HarpoonType selectedHarpoon = HarpoonType.NONE;

    @Getter
    private volatile AIOFishingState state = AIOFishingState.IDLE;
    @Getter
    private volatile FishingStage activeStage = FishingStage.SHRIMP;
    /** User-controlled pause, independent of the global Microbot pause. */
    @Getter
    private volatile boolean paused = false;

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
    /**
     * Non-null once something unrecoverable happened (no bait and no way to buy it, etc).
     * determineState() is recomputed every tick, so a handler setting state = STOPPED is
     * not enough on its own - this flag is what makes a stop stick.
     */
    @Getter
    private volatile String stopReason;
    /** Catch item currently withdrawn as notes and awaiting a GE listing. */
    private volatile String pendingSaleItem;
    /** Per-item failed listing count, used to undercut progressively. */
    private final Map<String, Integer> sellAttempts = new HashMap<>();
    public boolean run(AIOFishingConfig config) {
        this.config = config;
        this.selectedHarpoon = config.harpoonSpec();
        this.paused = false;
        this.cachedAction = "";
        this.pendingPurchases.clear();
        this.pendingShop = null;
        this.pendingViaGe = false;
        this.buyAttempts.clear();
        this.pendingSaleItem = null;
        this.sellAttempts.clear();
        this.stopReason = null;
        this.state = AIOFishingState.IDLE;

        Rs2Antiban.resetAntibanSettings();
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

            switch (state) {
                case SHOPPING:   handleShopping();  break;
                case SELLING:    handleSelling();   break;
                case BUYING:     handleBuying();    break;
                case GEARING:    handleGearing();   break;
                case TRAVELING:  handleTraveling(); break;
                case FISHING:    handleFishing();   break;
                case BANKING:    handleBanking();   break;
                case DROPPING:   handleDropping();  break;
                case STOPPED:    handleStopped();   break;
                default: break;
            }
        } catch (Exception ex) {
            log.error("AIOFishing loop error", ex);
        }
    }

    // ---------------------------------------------------------------- state

    private FishingStage resolveStage() {
        if (config.autoProgress()) {
            int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
            return FishingStage.bestFor(level, config.membersWorld(),
                    AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS);
        }
        return config.manualStage();
    }

    private AIOFishingState determineState() {
        // A requested stop is terminal - check it first, or the recompute below would
        // silently resurrect the script into an endless retry loop.
        if (stopReason != null) {
            return AIOFishingState.STOPPED;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.FISHING);
        if (level >= config.targetLevel()) {
            requestStop("Target level " + config.targetLevel() + " reached");
            return AIOFishingState.STOPPED;
        }

        // Verify quest/level/members gates BEFORE any walking, so a locked stage can never
        // send us looping around an area we can't actually fish in.
        String lock = activeStage.lockReason(level, config.membersWorld(),
                AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS);
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

    // ------------------------------------------------------------- handlers

    /**
     * Travel logic.
     *
     * <p>With a pinned location (manual mode) we go to that area first and only then home
     * in on a spot, so an explicitly chosen location is never silently swapped for a
     * closer one. Without a pin we prefer any spot already loaded in the scene - that
     * covers being near the area but out of click range - and otherwise head for the
     * nearest curated location.</p>
     */
    private void handleTraveling() {
        cachedAction = "";
        WorldPoint player = Rs2Player.getWorldLocation();
        FishingLocation pinned = pinnedLocation();

        if (isAtLocation(activeStage)) {
            handleFishing();
            return;
        }

        if (pinned != null && (player == null || player.distanceTo(pinned.getPoint()) > PINNED_RADIUS)) {
            Rs2Walker.walkTo(pinned.getPoint(), PINNED_RADIUS);
            return;
        }

        Rs2NpcModel spot = findNearestSpot(activeStage);
        WorldPoint spotLocation = spot != null ? spot.getWorldLocation() : null;
        if (spotLocation != null) {
            Rs2Walker.walkTo(spotLocation, SPOT_WALK_TOLERANCE);
            spot = findNearestSpot(activeStage);
            if (isInRange(spot)) {
                handleFishing();
            }
            return;
        }

        FishingLocation target = pinned != null ? pinned : chooseTravelTarget(player);
        // The curated point identifies the fishing area, not a tile the player needs to
        // stand on. Stop at scene range so the next loop can select the live fishing NPC.
        Rs2Walker.walkTo(target.getPoint(), PINNED_RADIUS);
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
        FishingLocation chosen = activeStage.getFastestLocation(player, AIOFishingScript::pathTiles);
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
        }
    }

    private void handleBanking() {
        dropUnwantedBigNetBycatch();
        if (!openBankAsSoonAsAvailable()) {
            return;
        }
        // Fish barrel support: dump its contents first if one is carried.
        if (Rs2Inventory.hasItem("Fish barrel")) {
            Rs2Bank.emptyFishBarrel();
            Rs2Inventory.waitForInventoryChanges(1000);
        }
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
        FishingShop shop = FishingShop.findNearest(itemName, config.membersWorld(),
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
                : FishingShop.findFor(itemName, config.membersWorld(),
                        AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS);
        if (shop == null) {
            pendingPurchases.remove(itemName);
            return;
        }

        WorldPoint player = Rs2Player.getWorldLocation();
        if (player == null || player.distanceTo(shop.getLocation()) > SHOP_INTERACT_RANGE) {
            Rs2Walker.walkTo(shop.getLocation(), SHOP_INTERACT_RANGE);
        }
        if (Rs2Shop.getNearestShopNpc(shop.getNpcName()) == null) {
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
    private void withdrawStackForSale() {
        if (!config.sellOnGe() || pendingSaleItem != null) {
            return;
        }
        for (String name : activeStage.getCatchItemNames()) {
            if (name.toLowerCase().contains("burnt")) {
                continue;
            }
            int banked = Rs2Bank.count(name);
            if (banked < config.sellStackSize()) {
                continue;
            }
            Rs2ItemModel item = Rs2Bank.getBankItem(name);
            if (item == null) {
                continue;
            }
            int unitPrice = Rs2GrandExchange.getPrice(item.getId());
            if (unitPrice < config.minFishValue()) {
                continue;
            }

            Rs2Bank.setWithdrawAs(true);
            Rs2Bank.withdrawAll(name);
            Rs2Inventory.waitForInventoryChanges(2000);
            Rs2Bank.setWithdrawAs(false);

            if (Rs2Inventory.hasItem(name)) {
                pendingSaleItem = name;
                log.info("Withdrew {} x{} (~{} gp each) as notes to sell.", name, banked, unitPrice);
            }
            return; // one stack per trip
        }
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
        if (!Rs2GrandExchange.walkToGrandExchange()) {
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
            log.warn("Failed to list {} - will retry lower.", pendingSaleItem);
        }
        Rs2GrandExchange.closeExchange();
        safeAntibanCooldown();
    }

    /** Price per item for a sell offer, honouring the configured pricing mode. */
    private int resolveSellPrice(int itemId, int attempt) {
        if (config.sellPricing() == GeSellPricing.CUSTOM) {
            return Math.max(1, config.customSellPrice());
        }
        if (itemId <= 0) {
            return -1;
        }
        if (config.sellPricing() == GeSellPricing.MARKET) {
            return Rs2GrandExchange.getSellPrice(itemId);
        }
        // ADAPTIVE: start at the live price, undercut a further 5% per failed attempt.
        return Rs2GrandExchange.getAdaptiveSellPrice(itemId, 1.0, attempt);
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

        if (!Rs2GrandExchange.walkToGrandExchange()) {
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
        if (Rs2Bank.isOpen() || Rs2Bank.openBank()) {
            return true;
        }

        BankLocation bank = Rs2Bank.getNearestBank();
        if (bank == null) {
            return false;
        }

        Rs2Walker.walkTo(bank.getWorldPoint(), BANK_INTERACT_RANGE);
        return Rs2Bank.openBank();
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
        if (selectedHarpoon != HarpoonType.NONE) {
            tools.replaceAll(t -> t.equalsIgnoreCase("Harpoon") ? selectedHarpoon.getItemName() : t);
        }
        return tools;
    }

    private List<String> itemsToKeep(FishingStage stage) {
        Set<String> keep = new HashSet<>(requiredTools(stage));
        keep.addAll(stage.getMethod().getConsumables());
        keep.add("Fish barrel");
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
        return lower.contains("rod") || lower.contains("harpoon");
    }

    /**
     * We're "at" a location only when a spot is actually within clicking range - being near
     * the curated world point isn't enough, because click() won't close the last few tiles.
     *
     * <p>When a location is pinned we additionally require being in that area, otherwise a
     * pin would be ignored whenever any valid spot happened to be loaded elsewhere.</p>
     */
    private boolean isAtLocation(FishingStage stage) {
        if (!isInRange(findNearestSpot(stage))) {
            return false;
        }
        FishingLocation pinned = pinnedLocation();
        if (pinned == null) {
            return true;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        return player != null && player.distanceTo(pinned.getPoint()) <= PINNED_RADIUS;
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
        return player.distanceTo(spotLocation) <= SPOT_INTERACT_RANGE;
    }

    private Rs2NpcModel findNearestSpot(FishingStage stage) {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        int[] ids = stage.getSpotIds();
        try {
            return Microbot.getRs2NpcCache().query()
                    .where(npc -> Arrays.stream(ids).anyMatch(id -> npc.getId() == id))
                    .nearestOnClientThread();
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

    private void safeAntibanCooldown() {
        try {
            Rs2Antiban.actionCooldown();
            Rs2Antiban.takeMicroBreakByChance();
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
        pendingPurchases.clear();
        pendingShop = null;
        pendingViaGe = false;
        pendingSaleItem = null;
        clearTravelTarget();
        state = AIOFishingState.IDLE;
    }
}
