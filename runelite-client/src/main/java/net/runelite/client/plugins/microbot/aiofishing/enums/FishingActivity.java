package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;

/**
 * Which activity the plugin is running.
 *
 * <p>Aerial fishing is a separate activity rather than another {@link FishingStage} because
 * almost nothing about the normal loop applies: there is no rod or net, the catch is taken
 * by a cormorant, the "bait" is produced by cutting up your own catch, and it pays Hunter
 * and Cooking experience alongside Fishing. It gets its own sidebar page and its own loop.</p>
 */
@Getter
public enum FishingActivity {
    PROGRESSION("Progression"),
    AERIAL("Aerial");

    private final String displayName;

    FishingActivity(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
