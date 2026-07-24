package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;

/** What to do with logs once the inventory is full. */
@Getter
public enum InventoryMode {
    BANK("Bank logs"),
    DROP("Drop logs"),
    FIREMAKE("Burn logs"),
    CAMPFIRE("Burn logs at campfire"),
    FLETCH("Fletch logs");

    private final String displayName;

    InventoryMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
