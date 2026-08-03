package net.runelite.client.plugins.microbot.simplemining;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.simplemining.enums.DesertHeatItem;
import net.runelite.client.plugins.microbot.simplemining.enums.WorldMode;
import net.runelite.client.plugins.microbot.simplemining.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.simplemining.enums.InventoryMode;
import net.runelite.client.plugins.microbot.simplemining.enums.InfernalShaleMethod;
import net.runelite.client.plugins.microbot.simplemining.enums.OreStage;
import net.runelite.client.plugins.microbot.simplemining.enums.SmeltingRecipe;

@ConfigGroup("SimpleMining")
public interface SimpleMiningConfig extends Config {

    @ConfigSection(name = "Progression", description = "What to mine and when to switch", position = 0)
    String PROGRESSION_SECTION = "progression";

    @ConfigSection(name = "Inventory", description = "What to do when the inventory is full", position = 1)
    String INVENTORY_SECTION = "inventory";

    @ConfigSection(name = "Ore mixtures", description = "Mine ores in bar-smelting ratios",
            position = 2, closedByDefault = true)
    String ORE_MIXTURES_SECTION = "oreMixtures";

    @ConfigSection(name = "Infernal shale", description = "Options used only when mining infernal shale",
            position = 3, closedByDefault = true)
    String INFERNAL_SHALE_SECTION = "infernalShale";

    @ConfigSection(name = "Desert heat",
            description = "Staying alive in the Kharidian Desert, used when mining granite",
            position = 4, closedByDefault = true)
    String DESERT_SECTION = "desert";

    @ConfigSection(name = "Grand Exchange", description = "Selling ore on the GE",
            position = 5, closedByDefault = true)
    String GE_SECTION = "grandexchange";

    // ---- Progression (mode, ore and location are set in the sidebar; hidden but persisted) ----

    @ConfigItem(keyName = "autoProgress", name = "Auto progression",
            description = "Automatically pick the best ore for your level and switch as you level up",
            position = 0, section = PROGRESSION_SECTION, hidden = true)
    default boolean autoProgress() {
        return true;
    }

    @ConfigItem(keyName = "manualStage", name = "Manual ore",
            description = "Ore to mine when auto progression is off",
            position = 1, section = PROGRESSION_SECTION, hidden = true)
    default OreStage manualStage() {
        return OreStage.COPPER;
    }

    @ConfigItem(keyName = "manualLocation", name = "Manual location",
            description = "Pinned mine for the manual ore; empty means nearest",
            position = 2, section = PROGRESSION_SECTION, hidden = true)
    default String manualLocation() {
        return "";
    }

    @ConfigItem(keyName = "worldMode", name = "World type",
            description = "Which ores and mines to consider. Auto reads the world you are logged "
                    + "into, so hopping between free and members worlds adapts by itself.",
            position = 3, section = PROGRESSION_SECTION)
    default WorldMode worldMode() {
        return WorldMode.AUTO;
    }

    @Range(min = 2, max = 99)
    @ConfigItem(keyName = "targetLevel", name = "Target level",
            description = "Stop once your Mining level reaches this",
            position = 4, section = PROGRESSION_SECTION)
    default int targetLevel() {
        return 99;
    }

    // ---- Inventory ----

    @ConfigItem(keyName = "inventoryMode", name = "When full",
            description = "Bank the ore, or drop it to power-mine",
            position = 0, section = INVENTORY_SECTION)
    default InventoryMode inventoryMode() {
        return InventoryMode.BANK;
    }

    @ConfigItem(keyName = "keepGems", name = "Keep gems",
            description = "Keep gems and other uncommon drops when dropping ore",
            position = 1, section = INVENTORY_SECTION)
    default boolean keepGems() {
        return true;
    }

    @ConfigItem(keyName = "bankClueGeodes", name = "Bank clue geodes",
            description = "Deposit Beginner, Easy, Medium, Hard and Elite clue geodes. In Drop "
                    + "mode, a full inventory containing a clue geode makes a bank trip instead.",
            position = 2, section = INVENTORY_SECTION)
    default boolean bankClueGeodes() {
        return true;
    }

    // ---- Ore mixtures ----

    @ConfigItem(keyName = "smeltingRecipe", name = "Recipe",
            description = "Mine and bank ores in this smelting ratio. Any enabled recipe overrides "
                    + "Auto progression and Manual ore selection.",
            position = 0, section = ORE_MIXTURES_SECTION)
    default SmeltingRecipe smeltingRecipe() {
        return SmeltingRecipe.NONE;
    }

    @Range(min = 1, max = 1000)
    @ConfigItem(keyName = "recipeBarsPerCycle", name = "Bars per cycle",
            description = "How many bars worth of ore to collect before banking the completed mixture. "
                    + "For example, 14 Bronze means 14 copper and 14 tin.",
            position = 1, section = ORE_MIXTURES_SECTION)
    default int recipeBarsPerCycle() {
        return 14;
    }

    // ---- Infernal shale ----

    @ConfigItem(keyName = "infernalShaleMethod", name = "Mining method",
            description = "Active deposit is slow and relaxed; rocks are faster; Jim's wet cloth "
                    + "uses the cloth, movement and rock clicks on consecutive game ticks.",
            position = 0, section = INFERNAL_SHALE_SECTION)
    default InfernalShaleMethod infernalShaleMethod() {
        return InfernalShaleMethod.ACTIVE_DEPOSIT;
    }

    // ---- Desert heat ----

    @ConfigItem(keyName = "desertHeatItem", name = "Wear",
            description = "Equip this before mining in the desert, taking it from the bank if it "
                    + "is not already worn. Only the Desert amulet 4 stops heat damage outright; "
                    + "the circlet of water spends a charge per hit and amulets 1-3 do nothing "
                    + "for heat, so waterskins are still bought alongside them.",
            position = 0, section = DESERT_SECTION)
    default DesertHeatItem desertHeatItem() {
        return DesertHeatItem.NONE;
    }

    @ConfigItem(keyName = "buyWaterskins", name = "Keep waterskins stocked",
            description = "Top waterskins up before heading into the desert: withdraw them from "
                    + "the bank first, then buy the rest from Shantay at the Shantay Pass. "
                    + "Skipped while a Desert amulet 4 is worn.",
            position = 1, section = DESERT_SECTION)
    default boolean buyWaterskins() {
        return false;
    }

    @Range(min = 1, max = 10)
    @ConfigItem(keyName = "waterskinCount", name = "Waterskins to carry",
            description = "How many full waterskins to keep in the inventory. Each one holds "
                    + "four doses and each dose covers one hit of heat.",
            position = 2, section = DESERT_SECTION)
    default int waterskinCount() {
        return 2;
    }

    // ---- Grand Exchange ----

    @ConfigItem(keyName = "sellOnGe", name = "Sell ore on GE",
            description = "Once enough of a valuable ore has built up in the bank, withdraw it "
                    + "noted and list it on the Grand Exchange.",
            position = 0, section = GE_SECTION)
    default boolean sellOnGe() {
        return false;
    }

    @Range(min = 1, max = 100000)
    @ConfigItem(keyName = "minOreValue", name = "Only sell above (gp)",
            description = "Minimum value per ore before it is worth selling.",
            position = 1, section = GE_SECTION)
    default int minOreValue() {
        return 100;
    }

    @Range(min = 50, max = 20000)
    @ConfigItem(keyName = "sellStackSize", name = "Sell at stack of",
            description = "How much ore must be banked before a sell trip is made.",
            position = 2, section = GE_SECTION)
    default int sellStackSize() {
        return 1000;
    }

    @ConfigItem(keyName = "sellPricing", name = "Pricing",
            description = "Adaptive starts at the live market price and undercuts only if the "
                    + "offer doesn't fill - recommended. Market never undercuts. Fixed uses your price.",
            position = 3, section = GE_SECTION)
    default GeSellPricing sellPricing() {
        return GeSellPricing.ADAPTIVE;
    }

    @Range(min = 1, max = 100000000)
    @ConfigItem(keyName = "customSellPrice", name = "Fixed price (gp)",
            description = "Price per ore, used only when Pricing is set to Fixed price.",
            position = 4, section = GE_SECTION)
    default int customSellPrice() {
        return 200;
    }
}
