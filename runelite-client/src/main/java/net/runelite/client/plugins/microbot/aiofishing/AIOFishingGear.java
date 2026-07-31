package net.runelite.client.plugins.microbot.aiofishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.aiofishing.enums.AnglerGear;
import net.runelite.client.plugins.microbot.aiofishing.enums.CatchProcessing;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishStorage;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.aiofishing.enums.HarpoonType;
import net.runelite.client.plugins.microbot.aiofishing.enums.RadasBlessing;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Everything about what the player carries and wears: the bank trip that gears up, which
 * tools a stage actually needs, the optional outfit / blessing / barrel, and which items
 * survive a deposit-all.
 *
 * <p>Split out of {@link AIOFishingScript} because it answers a different question from the
 * rest of the loop. The state machine asks "am I ready?" ({@link #hasGear}) and "what do I
 * keep?" ({@link #itemsToKeep}); how those answers are reached - spec harpoon substitution,
 * the infernal-cape alternate, the processing tool, bare-handed stripping the harpoon - is
 * entirely this class's business.</p>
 */
@Slf4j
class AIOFishingGear {

    /**
     * Top up a consumable once the carried amount drops below this. Kept well under the
     * configurable withdraw target so we aren't banking constantly.
     */
    private static final int CONSUMABLE_MIN = 25;
    /** Big-net junk is always dropped; useful bycatch follows the user's keep toggles. */
    private static final List<String> BIG_NET_JUNK = List.of("Leather boots", "Leather gloves");

    private final AIOFishingScript script;

    AIOFishingGear(AIOFishingScript script) {
        this.script = script;
    }

    /** Bank trip: deposit last stage's leftovers, then withdraw and wear what this one needs. */
    void handleGearing() {
        AIOFishingConfig config = script.config();
        FishingStage activeStage = script.getActiveStage();

        dropUnwantedBigNetBycatch();
        if (!script.openBankAsSoonAsAvailable()) {
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
                String failure = script.supplies().queuePurchase(tool, 1);
                if (failure == null) {
                    return; // head to the shop, then come back and finish gearing
                }
                Rs2Bank.closeBank();
                script.requestStop("No '" + tool + "' in bank - " + failure);
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
                String failure = script.supplies().queuePurchase(consumable, config.buyQuantity());
                if (failure == null) {
                    return; // head to the shop, then come back and finish gearing
                }
                Rs2Bank.closeBank();
                script.requestStop("Out of '" + consumable + "' - " + failure);
                return;
            }
            Rs2Bank.withdrawDeficit(consumable, config.withdrawAmount(), false);
            Rs2Inventory.waitForInventoryChanges(1500);
        }

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
        script.safeAntibanCooldown();

        if (!hasGear(activeStage)) {
            script.requestStop("Could not gear up for " + activeStage.getDisplayName());
        }
    }

    boolean hasGear(FishingStage stage) {
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
    List<String> requiredTools(FishingStage stage) {
        AIOFishingConfig config = script.config();
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
        HarpoonType selectedHarpoon = script.selectedHarpoon();
        if (selectedHarpoon != HarpoonType.NONE) {
            tools.replaceAll(t -> t.equalsIgnoreCase("Harpoon") ? selectedHarpoon.getItemName() : t);
        }
        return tools;
    }

    /** True when the user asked for bare-handed fishing and Otto has actually taught it. */
    boolean isBareHandedActive() {
        return script.config().bareHandedFishing()
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
        AIOFishingConfig config = script.config();
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
    void ensureFishStorageOpen() {
        FishStorage storage = script.config().fishStorage();
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
    void emptyFishStorage() {
        FishStorage storage = script.config().fishStorage();
        if (!storage.isEnabled()) {
            return;
        }
        Rs2ItemModel barrel = Rs2Inventory.get(storage.getItemIds());
        if (barrel != null) {
            Rs2Inventory.interact(barrel, "Empty");
            Rs2Inventory.waitForInventoryChanges(1200);
        }
    }

    /**
     * Whether a tool is usable: carried, worn, or - with the tackle box enabled - stored in
     * the box. The box keeps fishing gear in one slot, so without this the script would keep
     * withdrawing duplicates of tools it already owns.
     */
    boolean hasToolAvailable(String tool) {
        if (Rs2Inventory.hasItem(tool) || Rs2Equipment.isWearing(tool)) {
            return true;
        }
        return script.config().useTackleBox() && Rs2Inventory.hasItem(ItemID.TACKLE_BOX);
    }

    List<String> itemsToKeep(FishingStage stage) {
        AIOFishingConfig config = script.config();
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

    void dropUnwantedBigNetBycatch() {
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

    List<String> unwantedBigNetBycatch() {
        AIOFishingConfig config = script.config();
        if (script.getActiveStage() != FishingStage.BIG_NET) {
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
}
