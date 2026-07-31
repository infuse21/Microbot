package net.runelite.client.plugins.microbot.simplemining.enums;

public enum SimpleMiningState {
    /** Initial / no work decided yet. */
    IDLE,
    /** No usable pickaxe held; withdraw the best one from the bank. */
    GEARING,
    /** Walking to the current ore's mine. */
    TRAVELING,
    /** Actively mining rocks. */
    MINING,
    /** Crushing a small batch of infernal shale into its stackable form. */
    PROCESSING,
    /** Inventory full, banking mode; deposit the ore. */
    BANKING,
    /** Inventory full, drop mode; power-mine. */
    DROPPING,
    /** Enough valuable ore banked; list it on the Grand Exchange. */
    SELLING,
    /** Target level reached or an unrecoverable requirement is missing. */
    STOPPED
}
