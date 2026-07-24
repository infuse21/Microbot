package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * A named place you can chop a given {@link TreeStage}.
 *
 * <p>One point per named area is enough: the walker only needs to get us into the
 * region, after which the script chops whichever matching tree actually loaded.</p>
 */
@Getter
public final class TreeLocation {

    private final String name;
    /** Optional requirement hint shown in the sidebar, e.g. "60 WC". Nullable. */
    private final String note;
    private final WorldPoint point;

    private TreeLocation(String name, String note, WorldPoint point) {
        this.name = name;
        this.note = note;
        this.point = point;
    }

    public static TreeLocation of(String name, int x, int y) {
        return new TreeLocation(name, null, new WorldPoint(x, y, 0));
    }

    public static TreeLocation of(String name, int x, int y, String note) {
        return new TreeLocation(name, note, new WorldPoint(x, y, 0));
    }

    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
