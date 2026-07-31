package net.runelite.client.plugins.microbot.aiofishing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingDebugMode;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishStorage;
import net.runelite.client.plugins.microbot.aiofishing.enums.RadasBlessing;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingActivity;
import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.microbot.aiofishing.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.aiofishing.enums.HarpoonType;
import net.runelite.client.plugins.microbot.aiofishing.enums.WorldMode;

@ConfigGroup("AIOFishing")
public interface AIOFishingConfig extends Config {

    @ConfigSection(
            name = "Progression",
            description = "What to fish and when to switch",
            position = 0
    )
    String PROGRESSION_SECTION = "progression";

    @ConfigSection(
            name = "Inventory",
            description = "What to do when the inventory is full",
            position = 1
    )
    String INVENTORY_SECTION = "inventory";

    @ConfigSection(
            name = "Supplies",
            description = "How much bait/feathers to carry, and shop restocking",
            position = 2,
            closedByDefault = true
    )
    String SUPPLIES_SECTION = "supplies";

    @ConfigSection(
            name = "Grand Exchange",
            description = "Selling the catch on the GE",
            position = 3,
            closedByDefault = true
    )
    String GE_SECTION = "grandexchange";

    @ConfigSection(
            name = "Equipment",
            description = "Outfit, blessing and fish storage",
            position = 4,
            closedByDefault = true
    )
    String EQUIPMENT_SECTION = "equipment";

    @ConfigSection(
            name = "Aerial",
            description = "Aerial fishing at Lake Molch",
            position = 5,
            closedByDefault = true
    )
    String AERIAL_SECTION = "aerial";

    @ConfigSection(
            name = "Debug",
            description = "Force a workflow for live testing",
            position = 6,
            closedByDefault = true
    )
    String DEBUG_SECTION = "debug";

    // ---- Progression ----

    // Mode and fish selection live in the sidebar panel (fish icon in the toolbar), not
    // here - these stay declared so the values persist, but are hidden from the config UI.

    @ConfigItem(
            keyName = "activity",
            name = "Activity",
            description = "Which activity to run; chosen with the sidebar tabs",
            position = 0,
            section = PROGRESSION_SECTION,
            hidden = true
    )
    default FishingActivity activity() {
        return FishingActivity.PROGRESSION;
    }

    @ConfigItem(
            keyName = "autoProgress",
            name = "Auto progression",
            description = "Automatically pick the best fish for your level and switch as you level up",
            position = 0,
            section = PROGRESSION_SECTION,
            hidden = true
    )
    default boolean autoProgress() {
        return true;
    }

    @ConfigItem(
            keyName = "manualStage",
            name = "Manual fish",
            description = "Fish to catch when auto progression is off",
            position = 1,
            section = PROGRESSION_SECTION,
            hidden = true
    )
    default FishingStage manualStage() {
        return FishingStage.SHRIMP;
    }

    @ConfigItem(
            keyName = "manualLocation",
            name = "Manual location",
            description = "Pinned location name for the manual fish; empty means nearest",
            position = 2,
            section = PROGRESSION_SECTION,
            hidden = true
    )
    default String manualLocation() {
        return "";
    }

    @ConfigItem(
            keyName = "worldMode",
            name = "World type",
            description = "Which progression ladder to follow. Auto reads the world you are logged "
                    + "into, so hopping between free and members worlds switches ladder by itself. "
                    + "F2P runs Shrimp > Trout > Lobster > Swordfish; members run "
                    + "Shrimp > Big Net > Lobster > Swordfish > Monkfish > Shark.",
            position = 2,
            section = PROGRESSION_SECTION
    )
    default WorldMode worldMode() {
        return WorldMode.AUTO;
    }

    @Range(min = 2, max = 99)
    @ConfigItem(
            keyName = "targetLevel",
            name = "Target level",
            description = "Stop once your Fishing level reaches this",
            position = 3,
            section = PROGRESSION_SECTION
    )
    default int targetLevel() {
        return 99;
    }

    @ConfigItem(
            keyName = "harpoonSpec",
            name = "Harpoon spec",
            description = "Use a special-attack harpoon on harpoon stages (must be in your bank/inventory)",
            position = 4,
            section = PROGRESSION_SECTION
    )
    default HarpoonType harpoonSpec() {
        return HarpoonType.NONE;
    }

    // ---- Inventory ----

    // ---- Supplies ----

    @Range(min = 50, max = 10000)
    @ConfigItem(
            keyName = "withdrawAmount",
            name = "Withdraw amount",
            description = "How many feathers / bait / sandworms to take from the bank per trip. "
                    + "Tops up to this whenever the carried amount runs low.",
            position = 0,
            section = SUPPLIES_SECTION
    )
    default int withdrawAmount() {
        return 500;
    }

    @ConfigItem(
            keyName = "buySupplies",
            name = "Buy missing supplies",
            description = "Master switch. When the bank can't supply a tool or bait, buy it from the "
                    + "nearest fishing shop. Needs coins in the bank or inventory. Off = stop instead.",
            position = 1,
            section = SUPPLIES_SECTION
    )
    default boolean buySupplies() {
        return false;
    }

    @ConfigItem(
            keyName = "buyFromGe",
            name = "Fall back to GE",
            description = "If no fishing shop stocks the item, buy it on the Grand Exchange instead. "
                    + "Slower than a shop (the offer has to fill) but covers dark fishing bait and "
                    + "raw karambwanji, which no shop sells.",
            position = 3,
            section = SUPPLIES_SECTION
    )
    default boolean buyFromGe() {
        return false;
    }

    @Range(min = 50, max = 5000)
    @ConfigItem(
            keyName = "buyQuantity",
            name = "Buy amount",
            description = "How many feathers / bait / sandworms to buy per shop trip. Tools "
                    + "(nets, rods, harpoon, pot) are always bought one at a time.",
            position = 2,
            section = SUPPLIES_SECTION
    )
    default int buyQuantity() {
        return 500;
    }

    @ConfigItem(
            keyName = "useBank",
            name = "Bank catch",
            description = "Walk to a bank and deposit when full. When off, the catch is dropped",
            position = 0,
            section = INVENTORY_SECTION
    )
    default boolean useBank() {
        return true;
    }

    @ConfigItem(
            keyName = "keepCaskets",
            name = "Keep caskets",
            description = "Keep caskets caught with a big fishing net. When banking, they are deposited; "
                    + "when dropping, they remain in the inventory.",
            position = 1,
            section = INVENTORY_SECTION
    )
    default boolean keepCaskets() {
        return true;
    }

    @ConfigItem(
            keyName = "keepOysters",
            name = "Keep oysters",
            description = "Keep oysters caught with a big fishing net. When banking, they are deposited; "
                    + "when dropping, they remain in the inventory.",
            position = 2,
            section = INVENTORY_SECTION
    )
    default boolean keepOysters() {
        return false;
    }

    @ConfigItem(
            keyName = "keepSeaweed",
            name = "Keep seaweed",
            description = "Keep seaweed caught with a big fishing net. When banking, it is deposited; "
                    + "when dropping, it remains in the inventory.",
            position = 3,
            section = INVENTORY_SECTION
    )
    default boolean keepSeaweed() {
        return false;
    }

    @ConfigItem(
            keyName = "processCatch",
            name = "Process the catch",
            description = "Dissect sacred eels with a knife for Zulrah's scales (needs 72 Cooking) and "
                    + "crack infernal eels with a hammer for tokkul. Both produce stackable output, so "
                    + "processing a full inventory frees the slots without a bank trip. "
                    + "Turn this off to bank the eels whole.",
            position = 5,
            section = INVENTORY_SECTION
    )
    default boolean processCatch() {
        return true;
    }

    // ---- Grand Exchange ----

    @ConfigItem(
            keyName = "sellOnGe",
            name = "Sell catch on GE",
            description = "Once enough of a valuable fish has built up in the bank, withdraw it "
                    + "noted and list it on the Grand Exchange. Cheap fish are left alone.",
            position = 0,
            section = GE_SECTION
    )
    default boolean sellOnGe() {
        return false;
    }

    @Range(min = 1, max = 100000)
    @ConfigItem(
            keyName = "minFishValue",
            name = "Only sell above (gp)",
            description = "Minimum GE value per fish before it's worth selling. Keeps shrimp and "
                    + "other junk out of the Grand Exchange.",
            position = 1,
            section = GE_SECTION
    )
    default int minFishValue() {
        return 500;
    }

    @Range(min = 50, max = 20000)
    @ConfigItem(
            keyName = "sellStackSize",
            name = "Sell at stack of",
            description = "How many of a fish must be banked before a sell trip is made.",
            position = 2,
            section = GE_SECTION
    )
    default int sellStackSize() {
        return 500;
    }

    @ConfigItem(
            keyName = "withdrawAllForSale",
            name = "Withdraw all when selling",
            description = "When a fish reaches the sell threshold, withdraw its entire banked stack. "
                    + "When off, withdraw only the configured sell stack size.",
            position = 3,
            section = GE_SECTION
    )
    default boolean withdrawAllForSale() {
        return false;
    }

    @ConfigItem(
            keyName = "sellPricing",
            name = "Pricing",
            description = "Adaptive starts at the live market price and undercuts only if the "
                    + "offer doesn't fill - recommended. Market never undercuts. Fixed uses your price.",
            position = 4,
            section = GE_SECTION
    )
    default GeSellPricing sellPricing() {
        return GeSellPricing.ADAPTIVE;
    }

    @Range(min = 1, max = 100000000)
    @ConfigItem(
            keyName = "customSellPrice",
            name = "Fixed price (gp)",
            description = "Price per fish, used only when Pricing is set to Fixed price.",
            position = 5,
            section = GE_SECTION
    )
    default int customSellPrice() {
        return 1000;
    }

    // ---- Debug ----

    @ConfigItem(
            keyName = "debugMode",
            name = "Force workflow",
            description = "Bypass the normal state decision for live testing. Selling needs an "
                    + "active-stage catch in the inventory. Resupplying needs coins and a missing "
                    + "active-stage tool or supply.",
            position = 0,
            section = DEBUG_SECTION
    )
    default AIOFishingDebugMode debugMode() {
        return AIOFishingDebugMode.AUTOMATIC;
    }

    // ---- Equipment ----

    @ConfigItem(
            keyName = "equipAnglerOutfit",
            name = "Wear Angler's outfit",
            description = "Withdraw and wear any Angler's outfit pieces you own (2.5% Fishing xp "
                    + "for the full set). Spirit angler pieces count. Missing pieces are skipped.",
            position = 0,
            section = EQUIPMENT_SECTION
    )
    default boolean equipAnglerOutfit() {
        return false;
    }

    @ConfigItem(
            keyName = "radasBlessing",
            name = "Rada's blessing",
            description = "Wear this Rada's blessing for a chance at an extra fish "
                    + "(2/4/6/8% by tier). Ammo slot, so it never clashes with your gear.",
            position = 1,
            section = EQUIPMENT_SECTION
    )
    default RadasBlessing radasBlessing() {
        return RadasBlessing.NONE;
    }

    @ConfigItem(
            keyName = "fishStorage",
            name = "Fish storage",
            description = "Carry a barrel to hold 28 extra fish, roughly doubling trip length. "
                    + "Emptied at the bank alongside the catch.",
            position = 2,
            section = EQUIPMENT_SECTION
    )
    default FishStorage fishStorage() {
        return FishStorage.NONE;
    }

    @ConfigItem(
            keyName = "useTackleBox",
            name = "Use tackle box",
            description = "Treat fishing tools stored in a tackle box as available, so the "
                    + "script does not try to withdraw duplicates.",
            position = 3,
            section = EQUIPMENT_SECTION
    )
    default boolean useTackleBox() {
        return false;
    }

    @ConfigItem(
            keyName = "bareHandedFishing",
            name = "Bare-handed fishing",
            description = "If Otto has taught it (55 Fishing / 35 Strength), catch harpoon fish "
                    + "by hand - no harpoon needed at all.",
            position = 4,
            section = EQUIPMENT_SECTION
    )
    default boolean bareHandedFishing() {
        return false;
    }

    // ---- Aerial ----

    @Range(min = 1, max = 28)
    @ConfigItem(
            keyName = "wormsToPickUp",
            name = "Worms to pick up",
            description = "How many king worms to gather off the ground to start the bait cycle. "
                    + "Only needed once - after the first catch the knifed offcuts are the bait.",
            position = 0,
            section = AERIAL_SECTION
    )
    default int wormsToPickUp() {
        return 5;
    }
}
