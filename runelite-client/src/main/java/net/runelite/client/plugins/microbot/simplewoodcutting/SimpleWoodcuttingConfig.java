package net.runelite.client.plugins.microbot.simplewoodcutting;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.GeSellPricing;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.FletchingDisposition;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.InventoryMode;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.TreeStage;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.WalkBackMode;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.skills.fletching.data.FletchingItem;

@ConfigGroup("SimpleWoodcutting")
public interface SimpleWoodcuttingConfig extends Config {

    @ConfigSection(name = "Progression", description = "What to chop and when to switch", position = 0)
    String PROGRESSION_SECTION = "progression";

    @ConfigSection(name = "Inventory", description = "What to do when the inventory is full", position = 1)
    String INVENTORY_SECTION = "inventory";

    @ConfigSection(name = "Safety and looting",
            description = "Player detection and forestry ground loot",
            position = 2, closedByDefault = true)
    String SAFETY_SECTION = "safety";

    @ConfigSection(name = "Grand Exchange", description = "Selling logs on the GE",
            position = 3, closedByDefault = true)
    String GE_SECTION = "grandexchange";

    @ConfigSection(name = "Forestry", description = "Forestry event support",
            position = 4, closedByDefault = true)
    String FORESTRY_SECTION = "forestry";

    // ---- Progression (mode + tree + location live in the sidebar; hidden but persisted) ----

    @ConfigItem(keyName = "autoProgress", name = "Auto progression",
            description = "Automatically pick the best tree for your level and switch as you level up",
            position = 0, section = PROGRESSION_SECTION, hidden = true)
    default boolean autoProgress() {
        return true;
    }

    @ConfigItem(keyName = "manualStage", name = "Manual tree",
            description = "Tree to chop when auto progression is off",
            position = 1, section = PROGRESSION_SECTION, hidden = true)
    default TreeStage manualStage() {
        return TreeStage.TREE;
    }

    @ConfigItem(keyName = "manualLocation", name = "Manual location",
            description = "Pinned location name for the manual tree; empty means nearest",
            position = 2, section = PROGRESSION_SECTION, hidden = true)
    default String manualLocation() {
        return "";
    }

    @ConfigItem(keyName = "membersWorld", name = "Members world",
            description = "Allow members-only trees (maple, magic, redwood...). Turn off on F2P",
            position = 3, section = PROGRESSION_SECTION)
    default boolean membersWorld() {
        return true;
    }

    @Range(min = 2, max = 99)
    @ConfigItem(keyName = "targetLevel", name = "Target level",
            description = "Stop once your Woodcutting level reaches this",
            position = 4, section = PROGRESSION_SECTION)
    default int targetLevel() {
        return 99;
    }

    // ---- Inventory ----

    @ConfigItem(keyName = "inventoryMode", name = "When full",
            description = "What to do with a full inventory of logs",
            position = 0, section = INVENTORY_SECTION)
    default InventoryMode inventoryMode() {
        return InventoryMode.BANK;
    }

    @ConfigItem(keyName = "fletchingType", name = "Fletching type",
            description = "Product to make when Fletch logs is selected.",
            position = 1, section = INVENTORY_SECTION)
    default FletchingItem fletchingType() {
        return FletchingItem.ARROW_SHAFT;
    }

    @ConfigItem(keyName = "fletchingDisposition", name = "After fletching",
            description = "Keep, drop, bank, or string the resulting products.",
            position = 2, section = INVENTORY_SECTION)
    default FletchingDisposition fletchingDisposition() {
        return FletchingDisposition.DROP;
    }

    @ConfigItem(keyName = "useLogBasket", name = "Use log basket",
            description = "Fill a carried log basket before banking and empty it at the bank.",
            position = 3, section = INVENTORY_SECTION)
    default boolean useLogBasket() {
        return true;
    }

    @ConfigItem(keyName = "dropOrder", name = "Drop order",
            description = "Inventory interaction order used while dropping.",
            position = 4, section = INVENTORY_SECTION)
    default InteractOrder dropOrder() {
        return InteractOrder.STANDARD;
    }

    @ConfigItem(keyName = "itemsToKeep", name = "Items to keep",
            description = "Comma-separated item-name fragments retained while dropping.",
            position = 5, section = INVENTORY_SECTION)
    default String itemsToKeep() {
        return "axe,tinderbox,knife,bow string,log basket,forestry kit,anima-infused bark";
    }

    @ConfigItem(keyName = "itemsToBank", name = "Additional items to bank",
            description = "Comma-separated item-name fragments deposited during bank trips.",
            position = 6, section = INVENTORY_SECTION)
    default String itemsToBank() {
        return "logs,seed,nest,sturdy beehive parts,petal garland,golden pheasant egg,"
                + "pheasant tail feathers,fox whistle,key,anima-infused bark";
    }

    @ConfigItem(keyName = "walkBackMode", name = "Walk back to",
            description = "Destination used after banking, GE selling, campfires or fletching.",
            position = 7, section = INVENTORY_SECTION)
    default WalkBackMode walkBackMode() {
        return WalkBackMode.CURATED_LOCATION;
    }

    // ---- Safety and looting ----

    @ConfigItem(keyName = "hopWhenPlayerDetected", name = "Hop near players",
            description = "Log out for the autologin/world-selection workflow when another player "
                    + "is detected nearby. Disabled while Forestry events are enabled.",
            position = 0, section = SAFETY_SECTION)
    default boolean hopWhenPlayerDetected() {
        return false;
    }

    @Range(min = 1, max = 15)
    @ConfigItem(keyName = "playerDetectionRadius", name = "Player radius",
            description = "Distance used by player-detection hopping.",
            position = 1, section = SAFETY_SECTION)
    default int playerDetectionRadius() {
        return 1;
    }

    @ConfigItem(keyName = "lootBirdNests", name = "Loot bird nests",
            description = "Pick up nearby bird nests while chopping.",
            position = 2, section = SAFETY_SECTION)
    default boolean lootBirdNests() {
        return true;
    }

    @ConfigItem(keyName = "lootSeeds", name = "Loot seeds",
            description = "Pick up nearby seed drops while chopping.",
            position = 3, section = SAFETY_SECTION)
    default boolean lootSeeds() {
        return true;
    }

    @ConfigItem(keyName = "lootMyItemsOnly", name = "Only my ground items",
            description = "Only loot items owned by this player, useful for Ironman accounts.",
            position = 4, section = SAFETY_SECTION)
    default boolean lootMyItemsOnly() {
        return false;
    }

    // ---- Grand Exchange ----

    @ConfigItem(keyName = "sellOnGe", name = "Sell logs on GE",
            description = "Once enough of a valuable log has built up in the bank, withdraw it "
                    + "noted and list it on the Grand Exchange. Cheap logs are left alone.",
            position = 0, section = GE_SECTION)
    default boolean sellOnGe() {
        return false;
    }

    @Range(min = 1, max = 100000)
    @ConfigItem(keyName = "minLogValue", name = "Only sell above (gp)",
            description = "Minimum GE value per log before it's worth selling.",
            position = 1, section = GE_SECTION)
    default int minLogValue() {
        return 100;
    }

    @Range(min = 50, max = 20000)
    @ConfigItem(keyName = "sellStackSize", name = "Sell at stack of",
            description = "How many logs must be banked before a sell trip is made.",
            position = 2, section = GE_SECTION)
    default int sellStackSize() {
        return 1000;
    }

    @ConfigItem(keyName = "withdrawAllForSale", name = "Withdraw all when selling",
            description = "When logs reach the sell threshold, withdraw the entire banked stack. "
                    + "When off, withdraw only the configured sell stack size.",
            position = 3, section = GE_SECTION)
    default boolean withdrawAllForSale() {
        return false;
    }

    @ConfigItem(keyName = "sellPricing", name = "Pricing",
            description = "Adaptive starts at the live market price and undercuts only if the offer "
                    + "doesn't fill - recommended. Market never undercuts. Fixed uses your price.",
            position = 4, section = GE_SECTION)
    default GeSellPricing sellPricing() {
        return GeSellPricing.ADAPTIVE;
    }

    @Range(min = 1, max = 100000000)
    @ConfigItem(keyName = "customSellPrice", name = "Fixed price (gp)",
            description = "Price per log, used only when Pricing is set to Fixed price.",
            position = 5, section = GE_SECTION)
    default int customSellPrice() {
        return 200;
    }

    // ---- Forestry ----

    @ConfigItem(keyName = "enableForestry", name = "Enable Forestry",
            description = "Participate in configured Forestry events. Use a Forestry world.",
            position = 0, section = FORESTRY_SECTION)
    default boolean enableForestry() {
        return false;
    }

    @ConfigItem(keyName = "eggEvent", name = "Pheasant event",
            description = "Complete the pheasant egg Forestry event.",
            position = 1, section = FORESTRY_SECTION)
    default boolean eggEvent() {
        return true;
    }

    @ConfigItem(keyName = "entlingsEvent", name = "Entlings event",
            description = "Complete the friendly ent Forestry event.",
            position = 2, section = FORESTRY_SECTION)
    default boolean entlingsEvent() {
        return true;
    }

    @ConfigItem(keyName = "flowersEvent", name = "Flowering tree event",
            description = "Complete the flowering tree Forestry event.",
            position = 3, section = FORESTRY_SECTION)
    default boolean flowersEvent() {
        return false;
    }

    @ConfigItem(keyName = "foxEvent", name = "Fox event",
            description = "Complete the poachers and fox Forestry event.",
            position = 4, section = FORESTRY_SECTION)
    default boolean foxEvent() {
        return true;
    }

    @ConfigItem(keyName = "hivesEvent", name = "Beehive event",
            description = "Complete the beehive Forestry event.",
            position = 5, section = FORESTRY_SECTION)
    default boolean hivesEvent() {
        return true;
    }

    @ConfigItem(keyName = "leprechaunEvent", name = "Leprechaun event",
            description = "Complete the leprechaun rainbow Forestry event.",
            position = 6, section = FORESTRY_SECTION)
    default boolean leprechaunEvent() {
        return true;
    }

    @ConfigItem(keyName = "ritualEvent", name = "Ritual event",
            description = "Complete the enchanted ritual Forestry event.",
            position = 7, section = FORESTRY_SECTION)
    default boolean ritualEvent() {
        return true;
    }

    @ConfigItem(keyName = "rootEvent", name = "Roots event",
            description = "Complete the rising roots Forestry event.",
            position = 8, section = FORESTRY_SECTION)
    default boolean rootEvent() {
        return true;
    }

    @ConfigItem(keyName = "saplingEvent", name = "Sapling event",
            description = "Complete the struggling sapling Forestry event.",
            position = 9, section = FORESTRY_SECTION)
    default boolean saplingEvent() {
        return true;
    }
}
