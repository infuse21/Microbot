package net.runelite.client.plugins.microbot.shortestpath.pathfinder.live;

import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;

import static net.runelite.api.Constants.SCENE_SIZE;

/**
 * Telemetry for the static-vs-live collision question: how much does the freshly captured scene
 * disagree with the shipped map? Feeds the persistent-live-store decision with numbers instead of
 * anecdotes (the MLM rockfall / kebbit "falsely locked" reports were live-vs-static conflicts).
 *
 * <p>Deliberately runs once per CAPTURE against the immutable snapshot — never on the pathfinder
 * hot path, where the same comparison would double every edge lookup. Log-only: nothing here
 * changes routing.
 */
public final class LiveCollisionConflicts {

    /** Disagreement tally for one captured scene. */
    public static final class Tally {
        /** Edges the live scene says are walkable where the shipped map says wall (stale map / open door). */
        public final int liveOpensStatic;
        /** Edges the live scene says are blocked where the shipped map says walkable (new obstacle / closed door). */
        public final int liveBlocksStatic;

        Tally(int liveOpensStatic, int liveBlocksStatic) {
            this.liveOpensStatic = liveOpensStatic;
            this.liveBlocksStatic = liveBlocksStatic;
        }

        public boolean isEmpty() {
            return liveOpensStatic == 0 && liveBlocksStatic == 0;
        }
    }

    private LiveCollisionConflicts() {
    }

    /**
     * Compares every KNOWN edge of {@code snapshot} against {@code staticMap}. Unknown edges (outside
     * the scene, the rim, door-mask exclusions) are skipped — they are exactly the edges the pathfinder
     * would fall back to static for, so they cannot conflict.
     */
    public static Tally tally(LiveCollisionSnapshot snapshot, SplitFlagMap staticMap) {
        if (snapshot == null || staticMap == null) {
            return new Tally(0, 0);
        }
        int liveOpensStatic = 0;
        int liveBlocksStatic = 0;
        final int baseX = snapshot.getBaseX();
        final int baseY = snapshot.getBaseY();
        for (int z = 0; z < snapshot.getPlaneCount(); z++) {
            for (int ly = 0; ly < SCENE_SIZE; ly++) {
                for (int lx = 0; lx < SCENE_SIZE; lx++) {
                    final int x = baseX + lx;
                    final int y = baseY + ly;
                    for (int flag = LiveCollisionSnapshot.FLAG_NORTH; flag <= LiveCollisionSnapshot.FLAG_EAST; flag++) {
                        final Boolean live = snapshot.edge(x, y, z, flag);
                        if (live == null) {
                            continue;
                        }
                        final boolean statik = staticMap.get(x, y, z, flag);
                        if (live == statik) {
                            continue;
                        }
                        if (live) {
                            liveOpensStatic++;
                        } else {
                            liveBlocksStatic++;
                        }
                    }
                }
            }
        }
        return new Tally(liveOpensStatic, liveBlocksStatic);
    }
}
