package net.runelite.client.plugins.microbot.simplemining;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.simplemining.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.simplemining.enums.InfernalShaleMethod;
import net.runelite.client.plugins.microbot.simplemining.enums.InventoryMode;
import net.runelite.client.plugins.microbot.simplemining.enums.MiningLocation;
import net.runelite.client.plugins.microbot.simplemining.enums.OreStage;
import net.runelite.client.plugins.microbot.simplemining.enums.PickaxeType;
import net.runelite.client.plugins.microbot.simplemining.enums.SimpleMiningState;
import net.runelite.client.plugins.microbot.simplemining.enums.SmeltingRecipe;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntilNextTick;

@Slf4j
public class SimpleMiningScript extends Script {

    /** Rocks must be this close to click - click() does not auto-walk. */
    private static final int ROCK_INTERACT_RANGE = 12;
    private static final int WALK_TOLERANCE = 6;
    /** How close counts as "in" a pinned mine's area. */
    private static final int PINNED_RADIUS = 30;
    /**
     * How close to a curated point counts as being in that mine. Stops a long walk finishing
     * at an unrelated rock of the same ore that it merely passes - several ores appear in
     * mines scattered across the map.
     */
    private static final int MINE_AREA_RADIUS = 20;
    /** Abandon a GE sale after this many failed attempts rather than retrying forever. */
    private static final int MAX_SELL_ATTEMPTS = 4;
    /** Ten or more raw shale has a worse expected crushed-shale yield. */
    private static final int INFERNAL_SHALE_BATCH_SIZE = 9;
    private static final int[] CLUE_GEODE_IDS = {
            ItemID.CLUE_GEODE_BEGINNER,
            ItemID.CLUE_GEODE_EASY,
            ItemID.CLUE_GEODE_MEDIUM,
            ItemID.CLUE_GEODE_HARD,
            ItemID.CLUE_GEODE_ELITE
    };

    private SimpleMiningConfig config;

    @Getter
    private volatile SimpleMiningState state = SimpleMiningState.IDLE;
    @Getter
    private volatile OreStage activeStage = OreStage.COPPER;
    @Getter
    private volatile boolean paused = false;
    @Getter
    private volatile String stopReason;

    private long lastMineTime = 0;
    private volatile MiningLocation travelTarget;
    private volatile OreStage travelTargetStage;
    private volatile String pendingSaleItem;
    /** Keeps PROCESSING selected until every shale in the current batch has been crushed. */
    private boolean processingInfernalShaleBatch;
    /** The previous 3-tick cycle ended by clicking the cloth, ready to move next tick. */
    private boolean threeTickPrimed;
    /** Recipe progress counts actual ores obtained, including Varrock armour double ores. */
    private final Map<OreStage, Integer> recipeProgress = new HashMap<>();
    private SmeltingRecipe activeRecipe = SmeltingRecipe.NONE;
    /** A completed recipe is banked as a unit before its counters are reset. */
    private boolean recipeCycleCompletePendingBank;
    private final Map<String, Integer> sellAttempts = new HashMap<>();
    /** Ores we gave up selling - do not re-withdraw them every bank trip. */
    private final Set<String> unsellable = new HashSet<>();

    public boolean run(SimpleMiningConfig config) {
        this.config = config;
        this.paused = false;
        this.stopReason = null;
        this.pendingSaleItem = null;
        this.processingInfernalShaleBatch = false;
        this.threeTickPrimed = false;
        this.recipeProgress.clear();
        this.activeRecipe = SmeltingRecipe.NONE;
        this.recipeCycleCompletePendingBank = false;
        this.sellAttempts.clear();
        this.unsellable.clear();
        this.state = SimpleMiningState.IDLE;

        Rs2Antiban.resetAntibanSettings();
        // The template's values are used as-is. In particular takeMicroBreaks stays false:
        // micro breaks are Break Handler's job, and switching them on from here sets a flag
        // only BreakHandlerScript clears.
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(
                this::loop, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    public void togglePause() {
        this.paused = !this.paused;
    }

    /** Where we are headed, for display. Cached choice only - never pathfinds. */
    public MiningLocation getDisplayLocation() {
        MiningLocation pinned = pinnedLocation();
        return pinned != null ? pinned : travelTarget;
    }

    private void loop() {
        try {
            if (paused || !super.run() || !Microbot.isLoggedIn()) {
                return;
            }

            activeStage = resolveStage();
            state = determineState();

            // Honour antiban cooldowns. The templates set universalAntiban = false, so the
            // framework does not pause us - the script must check this itself or the cooldowns
            // are armed and ignored. AntibanPlugin clears the flag on its own tick.
            if (Rs2AntibanSettings.actionCooldownActive && state != SimpleMiningState.STOPPED) {
                Microbot.status = "Antiban cooldown";
                return;
            }

            if (state == SimpleMiningState.MINING
                    && (Rs2Player.isAnimating() || Rs2Player.isInteracting())
                    && System.currentTimeMillis() - lastMineTime < 12000) {
                return;
            }
            if (Rs2Player.isMoving() && state != SimpleMiningState.TRAVELING
                    && !isThreeTickInfernalShale()) {
                return;
            }
            if (state != SimpleMiningState.TRAVELING) {
                clearTravelTarget();
            }

            switch (state) {
                case GEARING:   handleGearing();   break;
                case TRAVELING: handleTraveling(); break;
                case MINING:    handleMining();    break;
                case PROCESSING: handleProcessing(); break;
                case BANKING:   handleBanking();   break;
                case DROPPING:  handleDropping();  break;
                case SELLING:   handleSelling();   break;
                case STOPPED:   handleStopped();   break;
                default: break;
            }
        } catch (Exception ex) {
            log.error("SimpleMining loop error", ex);
        }
    }

    // ---------------------------------------------------------------- state

    private OreStage resolveStage() {
        SmeltingRecipe configuredRecipe = config.smeltingRecipe();
        if (configuredRecipe != activeRecipe) {
            activeRecipe = configuredRecipe;
            recipeProgress.clear();
            recipeCycleCompletePendingBank = false;
        }
        if (activeRecipe.isEnabled()) {
            if (recipeCycleCompletePendingBank) {
                return activeStage;
            }
            int bars = config.recipeBarsPerCycle();
            for (OreStage stage : activeRecipe.getOres()) {
                if (recipeProgress.getOrDefault(stage, 0) < activeRecipe.targetFor(stage, bars)) {
                    return stage;
                }
            }
            recipeCycleCompletePendingBank = true;
            return activeStage;
        }
        if (config.autoProgress()) {
            int level = Rs2Player.getRealSkillLevel(Skill.MINING);
            return OreStage.bestFor(level, SimpleMiningPlugin.isMembersWorld(config),
                    SimpleMiningPlugin.QUEST_STATES, SimpleMiningPlugin.SKILL_LEVELS);
        }
        return config.manualStage();
    }

    private SimpleMiningState determineState() {
        if (stopReason != null) {
            return SimpleMiningState.STOPPED;
        }
        int level = Rs2Player.getRealSkillLevel(Skill.MINING);
        if (level >= config.targetLevel()) {
            requestStop("Target level " + config.targetLevel() + " reached");
            return SimpleMiningState.STOPPED;
        }

        // Gate check before any walking, so a locked ore never sends us to a mine we cannot use.
        String lock = activeStage.lockReason(level, SimpleMiningPlugin.isMembersWorld(config),
                SimpleMiningPlugin.QUEST_STATES, SimpleMiningPlugin.SKILL_LEVELS);
        if (lock != null) {
            requestStop("Cannot mine " + activeStage.getDisplayName() + ": " + lock);
            return SimpleMiningState.STOPPED;
        }

        if (pendingSaleItem != null) {
            return SimpleMiningState.SELLING;
        }
        if (PickaxeType.bestHeld() == null || needsInfernalTools()) {
            return SimpleMiningState.GEARING;
        }
        if (shouldProcessInfernalShale()) {
            return SimpleMiningState.PROCESSING;
        }
        if (recipeCycleCompletePendingBank) {
            return SimpleMiningState.BANKING;
        }
        if (Rs2Inventory.isFull()) {
            if (activeRecipe.isEnabled()) {
                return SimpleMiningState.BANKING;
            }
            boolean bankProtectedGeode = config.bankClueGeodes() && hasClueGeode();
            return config.inventoryMode() == InventoryMode.DROP
                    && !(activeStage == OreStage.GEM_ROCKS && config.keepGems())
                    && !bankProtectedGeode
                    ? SimpleMiningState.DROPPING
                    : SimpleMiningState.BANKING;
        }
        if (!isAtLocation(activeStage)) {
            return SimpleMiningState.TRAVELING;
        }
        return SimpleMiningState.MINING;
    }

    // ------------------------------------------------------------- handlers

    private void handleGearing() {
        if (!Rs2Bank.walkToBankAndUseBank()) {
            return;
        }
        if (Rs2Inventory.isFull()) {
            depositActiveProducts();
            Rs2Inventory.waitForInventoryChanges(1800);
        }

        if (PickaxeType.bestHeld() == null) {
            PickaxeType target = bestBankOrHeldPickaxe();
            if (target == null) {
                closeBankAndStop("No usable pickaxe in inventory or bank");
                return;
            }
            if (!target.isHeld() && Rs2Bank.hasBankItem(target.getItemId(), 1)) {
                Rs2Bank.withdrawItem(target.getItemId());
                sleepUntil(() -> Rs2Inventory.hasItem(target.getItemId()), 3000);
            }
            // Wield it when possible so it does not use an inventory slot; an unwieldable
            // pickaxe (Attack requirement) still mines perfectly well from the inventory.
            if (Rs2Inventory.hasItem(target.getItemId())) {
                Rs2Inventory.wield(target.getItemId());
                sleepUntil(() -> Rs2Equipment.isWearing(target.getItemId()), 1500);
            }
        }

        if (activeStage == OreStage.INFERNAL_SHALE) {
            if (!Rs2Inventory.hasItem(ItemID.CHISEL)
                    && !withdrawRequiredItem(ItemID.CHISEL)) {
                closeBankAndStop("A chisel is required to process infernal shale");
                return;
            }
            if (!hasInfernalHammer()) {
                int hammerId = firstBankedItem(ItemID.IMCANDO_HAMMER_OFFHAND,
                        ItemID.IMCANDO_HAMMER, ItemID.HAMMER);
                if (hammerId == -1 || !withdrawRequiredItem(hammerId)) {
                    closeBankAndStop("A hammer or Imcando hammer is required to process infernal shale");
                    return;
                }
                if (hammerId != ItemID.HAMMER) {
                    Rs2Inventory.wield(hammerId);
                    sleepUntil(() -> Rs2Equipment.isWearing(hammerId)
                            || Rs2Inventory.hasItem(hammerId), 1500);
                }
            }
        }
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);

        if (PickaxeType.bestHeld() == null) {
            requestStop("Could not obtain a pickaxe");
        } else if (activeStage == OreStage.INFERNAL_SHALE && needsInfernalTools()) {
            requestStop("Could not obtain the tools required to process infernal shale");
        }
    }

    private void handleTraveling() {
        WorldPoint player = Rs2Player.getWorldLocation();
        MiningLocation pinned = pinnedLocation();

        if (pinned != null && (player == null || player.distanceTo(pinned.getPoint()) > PINNED_RADIUS)) {
            Rs2Walker.walkTo(pinned.getPoint());
            return;
        }
        Rs2TileObjectModel rock = findNearestRock(activeStage);
        if (rock != null && rock.getWorldLocation() != null && isNearArea(rock.getWorldLocation())) {
            Rs2Walker.walkTo(rock.getWorldLocation(), WALK_TOLERANCE);
            return;
        }
        MiningLocation target = pinned != null ? pinned : chooseTravelTarget(player);
        Rs2Walker.walkTo(target.getPoint(), WALK_TOLERANCE);
    }

    private void handleMining() {
        if (isThreeTickInfernalShale()) {
            handleThreeTickMining();
            return;
        }
        threeTickPrimed = false;
        Rs2TileObjectModel rock = findNearestRock(activeStage);
        if (!isInRange(rock)) {
            return; // routed back to TRAVELING next tick
        }
        if (Rs2Player.isAnimating() && System.currentTimeMillis() - lastMineTime < 12000) {
            return;
        }
        if (rock.click("Mine")) {
            lastMineTime = System.currentTimeMillis();
            int oreBefore = Rs2Inventory.itemQuantity(activeStage.getOreName(), true);
            if (Rs2Player.waitForXpDrop(Skill.MINING, true)) {
                int mined = Math.max(1,
                        Rs2Inventory.itemQuantity(activeStage.getOreName(), true) - oreBefore);
                recordRecipeOre(activeStage, mined);
            }
            safeAntibanCooldown();
        }
    }

    /**
     * Jim's cycle is cloth -> move -> mine on consecutive game ticks. After mining, the
     * cloth is clicked on the following tick so the next scheduled pass can continue the
     * rhythm without priming from scratch.
     */
    private void handleThreeTickMining() {
        if (!ensureJimsWetCloth()) {
            threeTickPrimed = false;
            return;
        }
        Rs2TileObjectModel rock = findNearestRock(activeStage);
        if (!isInRange(rock)) {
            threeTickPrimed = false;
            return;
        }
        WorldPoint movementTile = adjacentMovementTile(rock);
        if (movementTile == null) {
            threeTickPrimed = false;
            return;
        }

        if (!threeTickPrimed) {
            if (!Rs2Inventory.use(ItemID.JIMS_WET_CLOTH) || !sleepUntilNextTick(1200)) {
                threeTickPrimed = false;
                return;
            }
        }
        if (!Rs2Walker.walkFastCanvas(movementTile, false) || !sleepUntilNextTick(1200)) {
            threeTickPrimed = false;
            return;
        }
        if (!rock.click("Mine") || !sleepUntilNextTick(1200)) {
            threeTickPrimed = false;
            return;
        }
        threeTickPrimed = Rs2Inventory.use(ItemID.JIMS_WET_CLOTH);
    }

    /** Crush one raw shale per loop; the next state decision repeats until the batch is gone. */
    private void handleProcessing() {
        threeTickPrimed = false;
        int rawBefore = Rs2Inventory.count(ItemID.INFERNAL_SHALE);
        if (rawBefore == 0) {
            return;
        }
        if (!Rs2Inventory.combine(ItemID.CHISEL, ItemID.INFERNAL_SHALE)) {
            return;
        }
        sleepUntil(() -> Rs2Inventory.count(ItemID.INFERNAL_SHALE) < rawBefore, 3000);
    }

    private void handleBanking() {
        if (!Rs2Bank.walkToBankAndUseBank()) {
            return;
        }
        depositActiveProducts();
        Rs2Inventory.waitForInventoryChanges(1800);
        sleepUntil(() -> !Rs2Inventory.isFull(), 5000);
        if (recipeCycleCompletePendingBank) {
            recipeProgress.clear();
            recipeCycleCompletePendingBank = false;
        }
        withdrawStackForSale();
        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        safeAntibanCooldown();
    }

    private void handleDropping() {
        for (String product : activeStage.getProductNames()) {
            Rs2Inventory.dropAll(product);
        }
        Rs2Inventory.waitForInventoryChanges(2000);
        safeAntibanCooldown();
    }

    private void handleStopped() {
        log.info("SimpleMining stopped: {}", stopReason == null ? "requested" : stopReason);
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        shutdown();
    }

    // ------------------------------------------------------------- GE selling

    private void withdrawStackForSale() {
        // Multi-product rocks (currently gems) need independent pricing and offer handling;
        // bank them normally instead of silently selling only the first possible product.
        if (activeRecipe.isEnabled() || !config.sellOnGe() || pendingSaleItem != null
                || activeStage.getProductNames().size() != 1) {
            return;
        }
        String name = activeStage.getOreName();
        if (unsellable.contains(name.toLowerCase())) {
            return;
        }
        int banked = Rs2Bank.count(name);
        if (banked < config.sellStackSize()) {
            return;
        }
        Rs2ItemModel item = Rs2Bank.getBankItem(name);
        if (item == null) {
            return;
        }
        int unitValue = config.sellPricing() == GeSellPricing.CUSTOM
                ? config.customSellPrice()
                : itemPrice(item.getId());
        if (unitValue < config.minOreValue()) {
            return;
        }
        Rs2Bank.setWithdrawAs(true);
        Rs2Bank.withdrawAll(name);
        Rs2Inventory.waitForInventoryChanges(2000);
        Rs2Bank.setWithdrawAs(false);
        if (Rs2Inventory.hasItem(name)) {
            pendingSaleItem = name;
            log.info("Withdrew {} x{} as notes to sell.", name, banked);
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
        if (!Rs2GrandExchange.walkToGrandExchange()) {
            return;
        }
        if (!Rs2GrandExchange.openExchange()) {
            requestStop("Could not open the Grand Exchange");
            return;
        }
        Rs2GrandExchange.collectAllToBank();
        sleep(600);

        int attempt = sellAttempts.getOrDefault(pendingSaleItem, 0);
        // Every failure path funnels through this counter so a sale can never retry forever.
        if (attempt >= MAX_SELL_ATTEMPTS) {
            log.warn("Giving up selling {} after {} attempts - leaving it banked.",
                    pendingSaleItem, attempt);
            unsellable.add(pendingSaleItem.toLowerCase());
            sellAttempts.remove(pendingSaleItem);
            pendingSaleItem = null;
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
            log.warn("Failed to list {} (no price or listing rejected) - attempt {} of {}.",
                    pendingSaleItem, attempt + 1, MAX_SELL_ATTEMPTS);
        }
        Rs2GrandExchange.closeExchange();
        safeAntibanCooldown();
    }

    /**
     * Price per ore. Pricing comes from RuneLite's own ItemManager (client-side wiki price
     * data, no HTTP call); the Rs2GrandExchange price helpers each hit an external API that
     * can be unavailable and throw.
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

    /** GE price from RuneLite's client-side data; -1 if unknown. Never hits the network. */
    private int itemPrice(int itemId) {
        if (itemId <= 0) {
            return -1;
        }
        try {
            return Microbot.getClientThread()
                    .runOnClientThreadOptional(() -> Microbot.getItemManager().getItemPrice(itemId))
                    .orElse(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    // -------------------------------------------------------------- helpers

    private void requestStop(String reason) {
        if (stopReason == null) {
            stopReason = reason;
            log.warn("SimpleMining stopping: {}", reason);
        }
        state = SimpleMiningState.STOPPED;
    }

    private boolean needsInfernalTools() {
        return activeStage == OreStage.INFERNAL_SHALE
                && (!Rs2Inventory.hasItem(ItemID.CHISEL) || !hasInfernalHammer());
    }

    private void recordRecipeOre(OreStage stage, int amount) {
        if (!activeRecipe.isEnabled() || !activeRecipe.contains(stage) || amount <= 0) {
            return;
        }
        recipeProgress.merge(stage, amount, Integer::sum);
        int bars = config.recipeBarsPerCycle();
        recipeCycleCompletePendingBank = activeRecipe.getOres().stream().allMatch(ore ->
                recipeProgress.getOrDefault(ore, 0) >= activeRecipe.targetFor(ore, bars));
    }

    private boolean hasInfernalHammer() {
        return Rs2Inventory.hasItem(ItemID.HAMMER, ItemID.IMCANDO_HAMMER,
                ItemID.IMCANDO_HAMMER_OFFHAND)
                || Rs2Equipment.isWearing(ItemID.IMCANDO_HAMMER,
                ItemID.IMCANDO_HAMMER_OFFHAND);
    }

    private boolean shouldProcessInfernalShale() {
        if (activeStage != OreStage.INFERNAL_SHALE) {
            processingInfernalShaleBatch = false;
            return false;
        }
        int rawShale = Rs2Inventory.count(ItemID.INFERNAL_SHALE);
        if (rawShale == 0) {
            processingInfernalShaleBatch = false;
            return false;
        }
        if (rawShale >= INFERNAL_SHALE_BATCH_SIZE || Rs2Inventory.isFull()) {
            processingInfernalShaleBatch = true;
        }
        return processingInfernalShaleBatch;
    }

    private boolean isThreeTickInfernalShale() {
        return activeStage == OreStage.INFERNAL_SHALE
                && config.infernalShaleMethod() == InfernalShaleMethod.JIMS_WET_CLOTH;
    }

    /** Obtain another cloth directly from Jim's post-introduction NPC form when needed. */
    private boolean ensureJimsWetCloth() {
        if (Rs2Inventory.hasItem(ItemID.JIMS_WET_CLOTH)) {
            return true;
        }
        var jim = Microbot.getRs2NpcCache().query()
                .withId(NpcID.JIM_14202)
                .nearestOnClientThread();
        if (jim == null) {
            requestStop("Jim's wet cloth is required; talk to Jim in the shale mine first");
            return false;
        }
        if (!jim.click("Take-from")) {
            return false;
        }
        if (!sleepUntil(() -> Rs2Inventory.hasItem(ItemID.JIMS_WET_CLOTH), 3000)) {
            requestStop("Could not obtain Jim's wet cloth");
            return false;
        }
        return true;
    }

    /** Pick a reachable tile beside the rock which causes a real one-tile movement click. */
    private WorldPoint adjacentMovementTile(Rs2TileObjectModel rock) {
        WorldArea footprint = rock.getFootprint();
        WorldPoint player = Rs2Player.getWorldLocation();
        if (footprint == null || player == null) {
            return null;
        }
        WorldPoint best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int x = footprint.getX() - 1; x <= footprint.getX() + footprint.getWidth(); x++) {
            for (int y = footprint.getY() - 1; y <= footprint.getY() + footprint.getHeight(); y++) {
                WorldPoint tile = new WorldPoint(x, y, footprint.getPlane());
                if (footprint.contains(tile) || tile.equals(player) || !Rs2Tile.isTileReachable(tile)) {
                    continue;
                }
                int distance = player.distanceTo(tile);
                if (distance < bestDistance) {
                    best = tile;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private boolean withdrawRequiredItem(int itemId) {
        if (!Rs2Bank.hasBankItem(itemId, 1)) {
            return false;
        }
        Rs2Bank.withdrawItem(itemId);
        return sleepUntil(() -> Rs2Inventory.hasItem(itemId), 3000);
    }

    private int firstBankedItem(int... itemIds) {
        for (int itemId : itemIds) {
            if (Rs2Bank.hasBankItem(itemId, 1)) {
                return itemId;
            }
        }
        return -1;
    }

    private void depositActiveProducts() {
        if (config.bankClueGeodes()) {
            for (int clueGeodeId : CLUE_GEODE_IDS) {
                Rs2Bank.depositAll(clueGeodeId);
            }
        }
        if (activeRecipe.isEnabled()) {
            for (OreStage stage : activeRecipe.getOres()) {
                for (String product : stage.getProductNames()) {
                    Rs2Bank.depositAll(product);
                }
            }
            return;
        }
        for (String product : activeStage.getProductNames()) {
            Rs2Bank.depositAll(product);
        }
    }

    private boolean hasClueGeode() {
        return Rs2Inventory.hasItem(CLUE_GEODE_IDS);
    }

    private void closeBankAndStop(String reason) {
        Rs2Bank.closeBank();
        requestStop(reason);
    }

    private MiningLocation pinnedLocation() {
        if (config == null || config.autoProgress()) {
            return null;
        }
        return activeStage.findLocation(config.manualLocation());
    }

    private MiningLocation chooseTravelTarget(WorldPoint player) {
        if (travelTarget != null && travelTargetStage == activeStage) {
            return travelTarget;
        }
        MiningLocation chosen = activeStage.getFastestLocation(player, SimpleMiningScript::pathTiles,
                SimpleMiningPlugin.isMembersWorld(config));
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

    /** True when the player is inside the intended mine, not merely near some rock. */
    private boolean isNearArea(WorldPoint area) {
        WorldPoint player = Rs2Player.getWorldLocation();
        return player != null && area != null && player.distanceTo(area) <= MINE_AREA_RADIUS;
    }

    /**
     * "Arrived": a rock is interactable and we are in one of this ore's curated mines. The
     * area test is not redundant - the same ore appears in mines all over the map, so a bare
     * "is a rock in range" check also passes at any incidental rock we happen to walk past.
     */
    private boolean isAtLocation(OreStage stage) {
        if (!isInRange(findNearestRock(stage))) {
            return false;
        }
        MiningLocation pinned = pinnedLocation();
        if (pinned != null) {
            WorldPoint player = Rs2Player.getWorldLocation();
            return player != null && player.distanceTo(pinned.getPoint()) <= PINNED_RADIUS;
        }
        MiningLocation nearest = stage.getClosestLocation(Rs2Player.getWorldLocation(),
                SimpleMiningPlugin.isMembersWorld(config));
        return nearest == null || isNearArea(nearest.getPoint());
    }

    private boolean isInRange(Rs2TileObjectModel rock) {
        if (rock == null) {
            return false;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        WorldPoint rockLocation = rock.getWorldLocation();
        return player != null && rockLocation != null
                && player.distanceTo(rockLocation) <= ROCK_INTERACT_RANGE;
    }

    /**
     * Nearest rock of this ore. Matched by object name - ore rocks are individually named
     * ("iron rocks"), unlike most scenery.
     */
    private Rs2TileObjectModel findNearestRock(OreStage stage) {
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        try {
            if (stage == OreStage.INFERNAL_SHALE) {
                String objectName = config.infernalShaleMethod() == InfernalShaleMethod.ACTIVE_DEPOSIT
                        ? "infernal shale deposit"
                        : "infernal shale rocks";
                return Microbot.getRs2TileObjectCache().query()
                        .withName(objectName)
                        .nearestOnClientThread();
            }
            return Microbot.getRs2TileObjectCache().query()
                    .withName(stage.getRockObjectName())
                    .nearestOnClientThread();
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("interrupted waiting for client thread")) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /** Best pickaxe the player owns, considering inventory, equipment and the open bank. */
    private PickaxeType bestBankOrHeldPickaxe() {
        PickaxeType best = null;
        for (PickaxeType pickaxe : PickaxeType.values()) {
            if (!pickaxe.meetsLevel()) {
                continue;
            }
            if (pickaxe.isHeld() || Rs2Bank.hasBankItem(pickaxe.getItemId(), 1)) {
                best = pickaxe; // values() is worst-to-best, so keep the last match
            }
        }
        return best;
    }

    /**
     * Start the post-action cooldown. Deliberately does <em>not</em> trigger micro breaks.
     *
     * <p>{@code takeMicroBreakByChance()} only raises {@code microBreakActive}; the flag is
     * cleared by BreakHandlerScript, so calling it without the Break Handler plugin running
     * leaves a latch nobody lowers. That is also why the mining template sets
     * {@code takeMicroBreaks = false} - it is opt-in through Break Handler, not something a
     * script switches on for the user. Breaks stay with Break Handler; this method owns only
     * the action cooldown, which the loop self-gates on via {@code actionCooldownActive}.</p>
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
        processingInfernalShaleBatch = false;
        threeTickPrimed = false;
        recipeProgress.clear();
        activeRecipe = SmeltingRecipe.NONE;
        recipeCycleCompletePendingBank = false;
        clearTravelTarget();
        state = SimpleMiningState.IDLE;
    }
}
