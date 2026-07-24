package net.runelite.client.plugins.microbot.aiofishing.enums;

/**
 * Optional workflow overrides for live testing. Automatic leaves the production state
 * machine untouched; every other value deliberately bypasses its normal entry condition.
 */
public enum AIOFishingDebugMode {
    AUTOMATIC("Automatic"),
    SELLING_ON_GE("Selling on GE"),
    RESUPPLYING_FROM_SHOP("Resupplying from shop"),
    RESUPPLYING_FROM_GE("Resupplying from GE"),
    WALKING_TO_BANK("Walking to bank"),
    WALKING_TO_FISHING_SPOT("Walking to fishing spot");

    private final String displayName;

    AIOFishingDebugMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
