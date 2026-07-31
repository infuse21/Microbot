package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * A named place you can fish a given {@link FishingStage}.
 *
 * <p>One point per named area is enough: the walker only needs to get us into the
 * region, after which the script walks to whichever spot NPC actually loaded.</p>
 */
@Getter
public final class FishingLocation {

    private final String name;
    /** Optional requirement hint shown in the sidebar, e.g. "68 Fishing". Nullable. */
    private final String note;
    private final WorldPoint point;
    /**
     * Whether reaching this spot needs membership.
     *
     * <p>Held as real data rather than inferred from {@link #note}, because several stages
     * are catchable on both world types from different places - lobsters at Musa Point are
     * free-to-play while the Catherby and Rellekka spots are not. Without the flag,
     * nearest-location picking would happily send a free-to-play account to Catherby.</p>
     */
    private final boolean membersOnly;

    private FishingLocation(String name, String note, WorldPoint point, boolean membersOnly) {
        this.name = name;
        this.note = note;
        this.point = point;
        this.membersOnly = membersOnly;
    }

    public static FishingLocation of(String name, int x, int y) {
        return new FishingLocation(name, null, new WorldPoint(x, y, 0), false);
    }

    public static FishingLocation of(String name, int x, int y, String note) {
        return new FishingLocation(name, note, new WorldPoint(x, y, 0), false);
    }

    /** A spot that needs membership to reach. */
    public static FishingLocation members(String name, int x, int y) {
        return new FishingLocation(name, null, new WorldPoint(x, y, 0), true);
    }

    /** A spot that needs membership to reach. */
    public static FishingLocation members(String name, int x, int y, String note) {
        return new FishingLocation(name, note, new WorldPoint(x, y, 0), true);
    }

    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
