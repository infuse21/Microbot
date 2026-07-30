package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Rada's blessing tiers, a Kourend &amp; Kebos Diary reward.
 *
 * <p>Each tier gives a chance of catching an extra fish - 2/4/6/8% by tier. The extra fish
 * grants no additional experience, so this is pure loot throughput, not faster levelling.</p>
 *
 * <p>It occupies the ammo slot, so it never conflicts with fishing tools or the Angler's
 * outfit - which is why it is safe to wear it unconditionally when the user asks for it.
 * The item ids are named {@code ZEAH_BLESSING_*} in the game data.</p>
 */
@Getter
public enum RadasBlessing {
    NONE("None", -1, 0),
    ONE("Rada's blessing 1", ItemID.ZEAH_BLESSING_EASY, 2),
    TWO("Rada's blessing 2", ItemID.ZEAH_BLESSING_MEDIUM, 4),
    THREE("Rada's blessing 3", ItemID.ZEAH_BLESSING_HARD, 6),
    FOUR("Rada's blessing 4", ItemID.ZEAH_BLESSING_ELITE, 8);

    private final String displayName;
    private final int itemId;
    /** Percentage chance of an extra fish. */
    private final int extraFishPercent;

    RadasBlessing(String displayName, int itemId, int extraFishPercent) {
        this.displayName = displayName;
        this.itemId = itemId;
        this.extraFishPercent = extraFishPercent;
    }

    public boolean isEnabled() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
