package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;

/** What to do with ore once the inventory is full. */
@Getter
public enum InventoryMode {
    BANK("Bank ore"),
    DROP("Drop ore");

    private final String displayName;

    InventoryMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
