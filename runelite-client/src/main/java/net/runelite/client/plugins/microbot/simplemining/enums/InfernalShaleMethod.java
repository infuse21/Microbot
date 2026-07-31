package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;

@Getter
public enum InfernalShaleMethod {
    ACTIVE_DEPOSIT("Active deposit", "Slowest and least click-intensive"),
    ROCKS("Infernal shale rocks", "Faster, medium intensity"),
    JIMS_WET_CLOTH("Jim's wet cloth (3-tick)", "Fastest and most click-intensive");

    private final String displayName;
    private final String description;

    InfernalShaleMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
