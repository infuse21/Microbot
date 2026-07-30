package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;
import java.util.function.Function;

/**
 * The four aerial fishing catches, worst to best.
 *
 * <p>You do not pick one: which fish the cormorant brings back is rolled from your Fishing
 * and Hunter levels, so this table exists to show what you currently qualify for and to
 * identify the raw fish that need cutting into offcuts.</p>
 *
 * <p>Levels are per the wiki: bluegill 43/35, common tench 56/51, mottled eel 73/68,
 * greater siren 91/87 (Fishing/Hunter).</p>
 */
@Getter
public enum AerialCatch {
    BLUEGILL("Bluegill", 43, 35, ItemID.AERIAL_FISHING_BLUEGILL),
    COMMON_TENCH("Common tench", 56, 51, ItemID.AERIAL_FISHING_COMMON_TENCH),
    MOTTLED_EEL("Mottled eel", 73, 68, ItemID.AERIAL_FISHING_MOTTLED_EEL),
    GREATER_SIREN("Greater siren", 91, 87, ItemID.AERIAL_FISHING_GREATER_SIREN);

    private final String displayName;
    private final int fishingLevel;
    private final int hunterLevel;
    /** Raw (uncut) item id - these are what get knifed into offcuts. */
    private final int itemId;

    AerialCatch(String displayName, int fishingLevel, int hunterLevel, int itemId) {
        this.displayName = displayName;
        this.fishingLevel = fishingLevel;
        this.hunterLevel = hunterLevel;
        this.itemId = itemId;
    }

    /** The lowest catch - its levels are the entry requirement for the activity as a whole. */
    public static AerialCatch entry() {
        return BLUEGILL;
    }

    public boolean isUnlocked(Function<Skill, Integer> skillLevels) {
        Integer fishing = skillLevels == null ? null : skillLevels.apply(Skill.FISHING);
        Integer hunter = skillLevels == null ? null : skillLevels.apply(Skill.HUNTER);
        return fishing != null && hunter != null
                && fishing >= fishingLevel && hunter >= hunterLevel;
    }

    /** Raw item ids for every catch, for inventory lookups. */
    public static int[] rawItemIds() {
        return Arrays.stream(values()).mapToInt(AerialCatch::getItemId).toArray();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
