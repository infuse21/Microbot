package net.runelite.client.plugins.microbot.aiofishing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingDebugMode;
import net.runelite.client.plugins.microbot.aiofishing.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.item.Rs2ItemManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * The Grand Exchange side of AIO Fishing: listing caught stock for sale, and buying the
 * supplies no fishing shop stocks.
 *
 * <p>Split out of {@link AIOFishingScript}, which had grown to hold thirteen handlers in one
 * file. The state that belongs to trading - the pending sale, the per-item attempt counters,
 * the abandoned list - moved here with the behaviour that uses it, so the script keeps only
 * the state machine and asks this class what it needs via {@link #hasPendingSale()}.</p>
 *
 * <p>Both directions are bounded. Selling gives up after {@link #MAX_SELL_ATTEMPTS} and
 * blacklists the item rather than leaving a pending sale that makes the state machine
 * return SELLING forever; buying gives up after {@link #MAX_BUY_ATTEMPTS}.</p>
 */
@Slf4j
class AIOFishingGrandExchange {

    /**
     * Abandon a GE sale after this many failed attempts. Without a cap, an item the price
     * lookup cannot value (or that the GE keeps rejecting) leaves the pending sale set, so
     * the script retries on every tick instead of getting on with fishing.
     */
    private static final int MAX_SELL_ATTEMPTS = 4;
    /** Give up on a GE buy after this many price escalations, rather than looping. */
    private static final int MAX_BUY_ATTEMPTS = 5;
    /** How long to wait for a buy offer to fill before escalating the price. */
    private static final int BUY_FILL_TIMEOUT_MS = 90000;

    private final AIOFishingScript script;

    /** Item withdrawn as notes and not yet listed. Non-null means a GE trip is owed. */
    @Getter
    private volatile String pendingSaleItem;
    /** Why the last debug sale scan rejected everything, for the overlay. */
    @Getter
    private volatile String lastSaleScanDiagnostic;
    private final Map<String, Integer> sellAttempts = new HashMap<>();
    /** Items we gave up trying to sell (no price data) - do not re-withdraw them. */
    private final Set<String> unsellable = new HashSet<>();
    /** Per-item failed buy count, used to escalate the offer price. */
    private final Map<String, Integer> buyAttempts = new HashMap<>();

    AIOFishingGrandExchange(AIOFishingScript script) {
        this.script = script;
    }

    /** Clear all trading state. Called when the script starts or the debug mode changes. */
    void reset() {
        pendingSaleItem = null;
        lastSaleScanDiagnostic = null;
        sellAttempts.clear();
        unsellable.clear();
        buyAttempts.clear();
    }

    /** Carrying notes we have not listed yet - the state machine must clear them first. */
    boolean hasPendingSale() {
        return pendingSaleItem != null;
    }

    void clearPendingSale() {
        pendingSaleItem = null;
    }

    void forgetBuyAttempts(String itemName) {
        buyAttempts.remove(itemName);
    }

    // ------------------------------------------------------------------ selling

    /**
     * If a catch item has built up past the configured stack size and is worth more than
     * the configured floor, withdraw it as notes so the next state is a GE trip. Only the
     * current stage's catch items are considered, so tools can never be sold by accident.
     */
    boolean withdrawStackForSale() {
        return withdrawStackForSale(false);
    }

    boolean withdrawStackForSale(boolean debugOverride) {
        AIOFishingConfig config = script.config();
        if ((!config.sellOnGe() && !debugOverride) || pendingSaleItem != null) {
            return false;
        }
        String rejection = "no active-stage catch found in the bank";
        for (String name : script.getActiveStage().getCatchItemNames()) {
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
        if (script.getActiveDebugMode() != AIOFishingDebugMode.SELLING_ON_GE
                || detail.equals(lastSaleScanDiagnostic)) {
            return;
        }
        lastSaleScanDiagnostic = detail;
        Microbot.status = "Debug selling: " + detail;
        log.info("Debug sale scan: {}", detail);
    }

    /** Pick up a carried catch item so the forced sale workflow has something to list. */
    void prepareDebugSale() {
        if (pendingSaleItem != null) {
            script.clearDebugBlockReason();
            return;
        }
        for (String itemName : script.getActiveStage().getCatchItemNames()) {
            if (!itemName.toLowerCase().contains("burnt") && Rs2Inventory.hasItem(itemName)) {
                pendingSaleItem = itemName;
                script.clearDebugBlockReason();
                return;
            }
        }
        // No carried catch is expected when testing automatic withdrawal. The forced
        // workflow will visit the bank and run the normal stack/value selection there.
        script.clearDebugBlockReason();
    }

    /**
     * Exercise the complete automatic-sale path without waiting for a full inventory.
     * Normal stack size, value floor and withdraw-all settings still apply.
     */
    void handleDebugSelling() {
        prepareDebugSale();
        if (pendingSaleItem != null) {
            handleSelling();
            return;
        }
        if (!script.openBankAsSoonAsAvailable()) {
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
    void handleSelling() {
        if (pendingSaleItem == null) {
            return;
        }
        if (!Rs2Inventory.hasItem(pendingSaleItem)) {
            pendingSaleItem = null; // notes gone (already listed or deposited)
            return;
        }
        if (!script.walkToGrandExchange()) {
            return;
        }
        if (!Rs2GrandExchange.openExchange()) {
            script.requestStop("Could not open the Grand Exchange");
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
        script.safeAntibanCooldown();
    }

    /** Price per item for a sell offer, honouring the configured pricing mode. */
    private int resolveSellPrice(int itemId, int attempt) {
        AIOFishingConfig config = script.config();
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

    // ------------------------------------------------------------------- buying

    /**
     * Buy a supply on the Grand Exchange. Used only when no reachable fishing shop stocks
     * the item (dark fishing bait, raw karambwanji) and the GE fallback is enabled.
     *
     * <p>Unlike selling, we cannot walk away - the item is needed before fishing can
     * resume - so this waits for the offer to fill, then escalates the price on the next
     * attempt if it did not. It gives up after {@link #MAX_BUY_ATTEMPTS} rather than
     * retrying forever.</p>
     */
    void handleBuying() {
        Map<String, Integer> pendingPurchases = script.supplies().orders();
        if (pendingPurchases.isEmpty()) {
            return;
        }
        Map.Entry<String, Integer> next = pendingPurchases.entrySet().iterator().next();
        String itemName = next.getKey();
        int quantity = next.getValue();

        // Already collected on an earlier pass.
        if (Rs2Inventory.hasItem(itemName)) {
            script.finishBuy(itemName);
            return;
        }

        int attempt = buyAttempts.getOrDefault(itemName, 0);
        if (attempt >= MAX_BUY_ATTEMPTS) {
            script.supplies().cancelOrders();
            script.requestStop("Could not buy '" + itemName + "' on the GE after "
                    + MAX_BUY_ATTEMPTS + " attempts");
            return;
        }

        if (!script.walkToGrandExchange()) {
            return;
        }
        if (!Rs2GrandExchange.openExchange()) {
            script.requestStop("Could not open the Grand Exchange");
            return;
        }

        Rs2GrandExchange.collectAllToInventory();
        sleep(600);
        if (Rs2Inventory.hasItem(itemName)) {
            Rs2GrandExchange.closeExchange();
            script.finishBuy(itemName);
            return;
        }

        int itemId = Rs2ItemManager.getItemIdByName(itemName, false);
        if (itemId <= 0) {
            Rs2GrandExchange.closeExchange();
            script.supplies().cancelOrders();
            script.requestStop("No GE item found named '" + itemName + "'");
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
            script.supplies().cancelOrders();
            script.requestStop("No GE price available for '" + itemName + "'");
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
            script.finishBuy(itemName);
            log.info("Bought {} on the Grand Exchange.", itemName);
        } else {
            buyAttempts.put(itemName, attempt + 1);
            log.warn("GE offer for {} did not fill; retrying higher.", itemName);
        }
        script.safeAntibanCooldown();
    }
}
