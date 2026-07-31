package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;

import java.util.List;

/**
 * A way of catching fish. Bundles the menu actions to look for on a fishing spot,
 * the tools that must be in the inventory/worn, and any consumables that deplete
 * while fishing (feathers, bait). Consumables are tracked separately so the script
 * knows when it must bank for a refill rather than just when the inventory is full.
 */
@Getter
public enum FishingMethod {
    NET("Small net", List.of("Net", "Small net"), List.of("Small fishing net"), List.of()),
    BIG_NET("Big net", List.of("Big net"), List.of("Big fishing net"), List.of()),
    BAIT("Rod + bait", List.of("Bait"), List.of("Fishing rod"), List.of("Fishing bait")),
    LURE("Fly rod + feathers", List.of("Lure"), List.of("Fly fishing rod"), List.of("Feather")),
    /**
     * Rainbow fish. Identical spots and menu action to {@link #LURE} - the bait is the only
     * thing that decides the catch. Stripy feathers come from Hunter (tropical wagtails) and
     * are not sold by any fishing shop, so they can only be topped up from the bank.
     */
    LURE_STRIPY("Fly rod + stripy", List.of("Lure"), List.of("Fly fishing rod"),
            List.of("Stripy feather")),
    CAGE("Lobster pot", List.of("Cage"), List.of("Lobster pot"), List.of()),
    HARPOON("Harpoon", List.of("Harpoon"), List.of("Harpoon"), List.of()),
    OILY_ROD("Oily rod + bait", List.of("Bait"), List.of("Oily fishing rod"), List.of("Fishing bait")),
    /**
     * Infernal eels in Mor Ul Rek. Two extra pieces of gear are listed as tools on purpose:
     * ice gloves, without which the eels are too hot to hold, and a fire cape, which the
     * TzHaar guard demands before letting anyone into the city.
     *
     * <p>Listing them here means a missing one is caught at the bank with a named reason
     * instead of after a pointless walk to a city we can't enter.</p>
     */
    OILY_ROD_ICE("Oily rod + ice gloves", List.of("Bait"),
            List.of("Oily fishing rod", "Ice gloves", "Fire cape"), List.of("Fishing bait")),
    SANDWORMS("Rod + sandworms", List.of("Bait"), List.of("Fishing rod"), List.of("Sandworms")),
    KARAMBWAN_VESSEL("Karambwan vessel", List.of("Fish"), List.of("Karambwan vessel"),
            List.of("Raw karambwanji")),
    DARK_CRAB_CAGE("Pot + dark bait", List.of("Cage"), List.of("Lobster pot"),
            List.of("Dark fishing bait")),
    /**
     * Barbarian fishing at Otto's Grotto. The spot's menu action is "Use-rod" (confirmed
     * against the barbarianfishing plugin and RuneLite's own IdleNotifier test fixture).
     *
     * <p>The tool name is matched as a substring, so the equipable <em>Pearl</em> barbarian
     * rod satisfies "Barbarian rod" without needing a separate alternate.</p>
     *
     * <p>Bait can also be fish offcuts, roe or caviar; feathers are used here because they
     * are the cheapest and are already stocked by the fishing shops we know about.</p>
     */
    BARBARIAN_ROD("Barbarian rod", List.of("Use-rod"), List.of("Barbarian rod"),
            List.of("Feather"));

    /** Short label used by the overlay and sidebar. */
    private final String displayName;
    private final List<String> actions;
    /** Reusable tools that stay in the inventory/equipment across banking trips. */
    private final List<String> tools;
    /** Items consumed while fishing that must be topped up when banking. */
    private final List<String> consumables;

    FishingMethod(String displayName, List<String> actions, List<String> tools, List<String> consumables) {
        this.displayName = displayName;
        this.actions = actions;
        this.tools = tools;
        this.consumables = consumables;
    }
}
