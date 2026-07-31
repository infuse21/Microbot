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
    /**
     * Whether reaching this patch needs membership.
     *
     * <p>Held as real data rather than inferred from {@link #note}, because several trees grow
     * on both world types in different places - willows are free-to-play at Draynor but not at
     * the Barbarian Outpost, and yews are free-to-play at Varrock and Falador but not in the
     * Woodcutting Guild. Without the flag, nearest-location picking would route a free account
     * to a patch it cannot reach.</p>
     */
    private final boolean membersOnly;

    private TreeLocation(String name, String note, WorldPoint point, boolean membersOnly) {
        this.name = name;
        this.note = note;
        this.point = point;
        this.membersOnly = membersOnly;
    }

    public static TreeLocation of(String name, int x, int y) {
        return new TreeLocation(name, null, new WorldPoint(x, y, 0), false);
    }

    public static TreeLocation of(String name, int x, int y, String note) {
        return new TreeLocation(name, note, new WorldPoint(x, y, 0), false);
    }

    /** A patch that needs membership to reach. */
    public static TreeLocation members(String name, int x, int y) {
        return new TreeLocation(name, null, new WorldPoint(x, y, 0), true);
    }

    /** A patch that needs membership to reach. */
    public static TreeLocation members(String name, int x, int y, String note) {
        return new TreeLocation(name, note, new WorldPoint(x, y, 0), true);
    }

    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
