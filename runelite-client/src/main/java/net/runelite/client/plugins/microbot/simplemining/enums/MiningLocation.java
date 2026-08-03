package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * A named mine where a given {@link OreStage} can be worked.
 *
 * <p>One point per mine is enough: the walker only has to reach the area, after which the
 * script picks whichever matching rock actually loaded.</p>
 */
@Getter
public final class MiningLocation {

    private final String name;
    /** Optional hint shown in the sidebar, e.g. "bank close". Nullable. */
    private final String note;
    private final WorldPoint point;
    /**
     * Whether this mine needs membership.
     *
     * <p>Held as real data rather than inferred from {@link #note}: a free-text hint cannot be
     * filtered on, which previously let a free-to-play account be routed to the members-only
     * Heroes' Guild for runite.</p>
     */
    private final boolean membersOnly;
    /**
     * Whether this mine sits in the Kharidian Desert heat zone, where the script has to wear
     * heat protection or carry water. Held as data for the same reason as {@link #membersOnly}:
     * the free-text note cannot be tested against.
     */
    private final boolean desertHeat;

    private MiningLocation(String name, String note, WorldPoint point, boolean membersOnly,
                           boolean desertHeat) {
        this.name = name;
        this.note = note;
        this.point = point;
        this.membersOnly = membersOnly;
        this.desertHeat = desertHeat;
    }

    public static MiningLocation of(String name, int x, int y) {
        return new MiningLocation(name, null, new WorldPoint(x, y, 0), false, false);
    }

    public static MiningLocation of(String name, int x, int y, String note) {
        return new MiningLocation(name, note, new WorldPoint(x, y, 0), false, false);
    }

    /** A mine that requires membership. */
    public static MiningLocation members(String name, int x, int y, String note) {
        return new MiningLocation(name, note, new WorldPoint(x, y, 0), true, false);
    }

    /** This mine, flagged as sitting in the desert heat. */
    public MiningLocation inDesertHeat() {
        return new MiningLocation(name, note, point, membersOnly, true);
    }

    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
