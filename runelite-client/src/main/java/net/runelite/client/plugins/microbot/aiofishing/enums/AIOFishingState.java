package net.runelite.client.plugins.microbot.aiofishing.enums;

public enum AIOFishingState {
    /** Initial / no work decided yet. */
    IDLE,
    /** Missing tools or consumables for the current stage; go to the bank and gear up. */
    GEARING,
    /** Bank couldn't supply something; buy it from a fishing shop. */
    SHOPPING,
    /** Carrying a noted stack of catch to list on the Grand Exchange. */
    SELLING,
    /** Buying a supply on the Grand Exchange because no shop stocks it. */
    BUYING,
    /** Walking to the current stage's fishing location. */
    TRAVELING,
    /** Actively clicking fishing spots. */
    FISHING,
    /** Inventory full and banking enabled; deposit the catch. */
    BANKING,
    /** Inventory full and banking disabled; drop the catch. */
    DROPPING,
    /** Target level reached or an unrecoverable requirement is missing. */
    STOPPED
}
