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
    CAGE("Lobster pot", List.of("Cage"), List.of("Lobster pot"), List.of()),
    HARPOON("Harpoon", List.of("Harpoon"), List.of("Harpoon"), List.of()),
    OILY_ROD("Oily rod + bait", List.of("Bait"), List.of("Oily fishing rod"), List.of("Fishing bait")),
    SANDWORMS("Rod + sandworms", List.of("Bait"), List.of("Fishing rod"), List.of("Sandworms")),
    KARAMBWAN_VESSEL("Vessel + karambwanji", List.of("Fish"), List.of("Karambwan vessel"),
            List.of("Raw karambwanji")),
    DARK_CRAB_CAGE("Pot + dark bait", List.of("Cage"), List.of("Lobster pot"),
            List.of("Dark fishing bait"));

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
