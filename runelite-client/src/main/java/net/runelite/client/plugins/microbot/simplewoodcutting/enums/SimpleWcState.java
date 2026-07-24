package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

public enum SimpleWcState {
    /** Initial / no work decided yet. */
    IDLE,
    /** No usable axe held; withdraw and wield the best one from the bank. */
    GEARING,
    /** Walking to the current tree's location. */
    TRAVELING,
    /** Actively chopping trees. */
    CHOPPING,
    /** Inventory full, banking mode; deposit logs. */
    BANKING,
    /** Inventory full, drop mode; drop logs. */
    DROPPING,
    /** Inventory full, firemaking mode; burn logs. */
    FIREMAKING,
    /** Inventory full, fletching mode; fletch logs. */
    FLETCHING,
    /** Enough valuable logs banked; list them on the Grand Exchange. */
    SELLING,
    /** Target level reached or an unrecoverable requirement missing. */
    STOPPED
}
