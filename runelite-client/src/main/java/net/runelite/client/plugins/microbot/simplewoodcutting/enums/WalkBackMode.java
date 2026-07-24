package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;

@Getter
public enum WalkBackMode {
    CURATED_LOCATION("Selected tree location"),
    LAST_TREE("Last chopped tree"),
    STARTING_LOCATION("Script starting location");

    private final String displayName;

    WalkBackMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
