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

    private FishingLocation(String name, String note, WorldPoint point) {
        this.name = name;
        this.note = note;
        this.point = point;
    }

    public static FishingLocation of(String name, int x, int y) {
        return new FishingLocation(name, null, new WorldPoint(x, y, 0));
    }

    public static FishingLocation of(String name, int x, int y, String note) {
        return new FishingLocation(name, note, new WorldPoint(x, y, 0));
    }

    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
