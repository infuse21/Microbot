package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Worn kit that helps a miner survive the Kharidian Desert heat.
 *
 * <p>Only the Desert amulet 4 is real immunity. The circlet of water spends one of its charges
 * per heat tick, so it replaces waterskins rather than removing the problem, and amulets 1 to 3
 * do nothing for heat at all - they are offered because their teleports still shorten the bank
 * trip. Anything short of the elite amulet therefore leaves waterskin buying switched on.</p>
 */
@Getter
public enum DesertHeatItem {

    NONE("None", -1, false),
    CIRCLET_OF_WATER("Circlet of water", ItemID.WATER_CIRCLET_CHARGED, false),
    DESERT_AMULET_1("Desert amulet 1", ItemID.DESERT_AMULET_EASY, false),
    DESERT_AMULET_2("Desert amulet 2", ItemID.DESERT_AMULET_MEDIUM, false),
    DESERT_AMULET_3("Desert amulet 3", ItemID.DESERT_AMULET_HARD, false),
    DESERT_AMULET_4("Desert amulet 4", ItemID.DESERT_AMULET_ELITE, true);

    private final String displayName;
    /** Item to wear, or -1 for {@link #NONE}. */
    private final int itemId;
    /** Whether wearing this alone stops heat damage. Only the elite amulet does. */
    private final boolean negatesHeat;

    DesertHeatItem(String displayName, int itemId, boolean negatesHeat) {
        this.displayName = displayName;
        this.itemId = itemId;
        this.negatesHeat = negatesHeat;
    }

    public boolean isEnabled() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
