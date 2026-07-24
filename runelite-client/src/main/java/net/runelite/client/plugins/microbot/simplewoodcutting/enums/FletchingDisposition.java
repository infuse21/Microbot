package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;

@Getter
public enum FletchingDisposition {
    NONE("Keep products"),
    BANK("Bank products"),
    DROP("Drop products"),
    STRING_AND_DROP("String bows, then drop"),
    STRING_AND_BANK("String bows, then bank");

    private final String displayName;

    FletchingDisposition(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
