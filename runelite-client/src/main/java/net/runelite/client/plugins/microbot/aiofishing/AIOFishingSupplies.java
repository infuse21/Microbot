package net.runelite.client.plugins.microbot.aiofishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingShop;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * The supply order book and the shop trips that fill it.
 *
 * <p>Owning the order book here is the point of the split. It used to live on
 * {@link AIOFishingScript} purely so {@link AIOFishingGrandExchange} could reach it, which
 * meant the script exposed three accessors for state it never used itself. Now gearing
 * queues an order through {@link #queuePurchase}, and whichever route fulfils it - a shop
 * counter or the Grand Exchange - reads the book from here.</p>
 *
 * <p>Nothing is queued speculatively: {@link #queuePurchase} only accepts an order when a
 * reachable shop actually stocks the item or the GE fallback is on, so the script never
 * sets off towards somewhere that cannot help.</p>
 */
@Slf4j
class AIOFishingSupplies {

    private static final int SHOP_WALK_TOLERANCE = 4;
    /** Shop NPC readiness radius; the fallback destination remains tighter than this. */
    private static final int SHOP_INTERACT_RANGE = 10;
    /**
     * How close to a shop's own location counts as being at that shop. Needed because the
     * shop lookup matches NPC names as a substring and is not tied to a location, so a
     * generically-named shopkeeper ("Fish monger") could otherwise satisfy the walk's
     * early-finish somewhere else entirely.
     */
    private static final int SHOP_AREA_RADIUS = 20;

    private final AIOFishingScript script;

    /** Item name -> quantity still to buy. Empty when nothing is on order. */
    private final Map<String, Integer> pendingPurchases = new LinkedHashMap<>();
    /** Shop chosen for the pending purchase, decided once while still at the bank. */
    private volatile FishingShop pendingShop;
    /** True when the pending purchase goes to the Grand Exchange instead of a shop. */
    private volatile boolean pendingViaGe;

    AIOFishingSupplies(AIOFishingScript script) {
        this.script = script;
    }

    void reset() {
        pendingPurchases.clear();
        pendingShop = null;
        pendingViaGe = false;
    }

    boolean hasOrders() {
        return !pendingPurchases.isEmpty();
    }

    /** Whether the outstanding order is being filled at the GE rather than a shop. */
    boolean isViaGe() {
        return pendingViaGe;
    }

    /** The order book, for the GE buy path. */
    Map<String, Integer> orders() {
        return pendingPurchases;
    }

    void cancelOrders() {
        pendingPurchases.clear();
        pendingViaGe = false;
    }

    /** A completed purchase: drop it from the book and stop routing to the GE. */
    void finishBuy(String itemName) {
        pendingPurchases.remove(itemName);
        pendingViaGe = false;
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
    String queuePurchase(String itemName, int quantity) {
        AIOFishingConfig config = script.config();
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

    /** Queue an order by hand so the forced debug workflow has something to fulfil. */
    void prepareDebugPurchase(boolean useGrandExchange) {
        AIOFishingConfig config = script.config();
        if (!pendingPurchases.isEmpty()) {
            script.clearDebugBlockReason();
            return;
        }
        if (!Rs2Inventory.hasItem("Coins")) {
            script.setDebugBlockReason("Debug resupply: carry coins in the inventory");
            return;
        }

        String itemName = script.debug().findMissingSupply();
        if (itemName == null) {
            script.setDebugBlockReason("Debug resupply: remove an active-stage tool or supply first");
            return;
        }
        int quantity = script.getActiveStage().getMethod().getConsumables().contains(itemName)
                ? config.buyQuantity()
                : 1;

        FishingShop shop = null;
        if (!useGrandExchange) {
            shop = FishingShop.findNearest(itemName, AIOFishingPlugin.isMembersWorld(config),
                    AIOFishingPlugin.QUEST_STATES, AIOFishingPlugin.SKILL_LEVELS,
                    Rs2Player.getWorldLocation(), AIOFishingScript::pathTiles);
            if (shop == null) {
                script.setDebugBlockReason("Debug shop resupply: no reachable shop stocks " + itemName);
                return;
            }
        }

        pendingPurchases.put(itemName, quantity);
        pendingShop = shop;
        pendingViaGe = useGrandExchange;
        script.clearDebugBlockReason();
        log.info("Debug resupply queued: {} x{} via {}.", itemName, quantity,
                useGrandExchange ? "Grand Exchange" : shop);
    }

    /**
     * Buy the queued items. Stock is verified once the shop is open - the stock lists in
     * {@link FishingShop} are hints, so if the item isn't really there we stop rather than
     * bounce between bank and shop forever.
     */
    void handleShopping() {
        AIOFishingConfig config = script.config();
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
            if (!script.walkUntilNotPaused(shop.getLocation(), SHOP_WALK_TOLERANCE,
                    () -> isAtShop(shop))) {
                return;
            }
        }
        if (!isAtShop(shop)) {
            return;
        }

        if (!Rs2Shop.openShop(shop.getNpcName()) || !Rs2Shop.isOpen()) {
            pendingPurchases.clear();
            script.requestStop("Could not open " + shop);
            return;
        }

        if (!Rs2Shop.hasStock(itemName)) {
            Rs2Shop.closeShop();
            pendingPurchases.clear();
            script.requestStop(shop + " does not stock '" + itemName + "'");
            return;
        }

        Rs2Shop.buyItemOptimally(itemName, quantity);
        Rs2Inventory.waitForInventoryChanges(2000);
        Rs2Shop.closeShop();
        sleepUntil(() -> !Rs2Shop.isOpen(), 3000);

        if (!Rs2Inventory.hasItem(itemName)) {
            pendingPurchases.clear();
            script.requestStop("Bought '" + itemName + "' at " + shop
                    + " but none arrived - out of coins?");
            return;
        }

        log.info("Bought {} at {}.", itemName, shop);
        pendingPurchases.remove(itemName);
        pendingShop = null;
        script.safeAntibanCooldown();
    }

    /** At the intended shop: its keeper is interactable AND we are at its location. */
    private boolean isAtShop(FishingShop shop) {
        return shop != null
                && script.isNearArea(shop.getLocation(), SHOP_AREA_RADIUS)
                && script.isShopNpcInteractable(shop.getNpcName(), SHOP_INTERACT_RANGE);
    }
}
