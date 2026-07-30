package net.runelite.client.plugins.microbot.shortestpath.pathfinder.live;

import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;

import static net.runelite.api.Constants.SCENE_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The conflict tally is log-only telemetry, but its two buckets must be RIGHT or the numbers will
 * misdirect the persistent-live-store decision. Uses the real shipped map: whatever the static value
 * of a probe edge is, a live snapshot that agrees counts nothing, and a snapshot that disagrees
 * lands in exactly one bucket. Unknown edges never count.
 */
public class LiveCollisionConflictsTest {

    private static SplitFlagMap staticMap;

    // Lumbridge courtyard: mapped, ordinary ground.
    private static final int PROBE_X = 3222;
    private static final int PROBE_Y = 3218;
    private static final int BASE_X = PROBE_X - 10;
    private static final int BASE_Y = PROBE_Y - 10;

    @BeforeClass
    public static void load() {
        staticMap = SplitFlagMap.fromResources();
    }

    private static LiveCollisionSnapshot snapshotWithNorthEdge(boolean value) {
        BitSet northKnown = new BitSet();
        BitSet northValue = new BitSet();
        int index = (PROBE_Y - BASE_Y) * SCENE_SIZE + (PROBE_X - BASE_X);
        northKnown.set(index);
        if (value) {
            northValue.set(index);
        }
        return new LiveCollisionSnapshot(BASE_X, BASE_Y, 1,
                northKnown, northValue, new BitSet(), new BitSet());
    }

    @Test
    public void agreementCountsNothing() {
        boolean statik = staticMap.get(PROBE_X, PROBE_Y, 0, LiveCollisionSnapshot.FLAG_NORTH);
        LiveCollisionConflicts.Tally tally =
                LiveCollisionConflicts.tally(snapshotWithNorthEdge(statik), staticMap);
        assertTrue("a live edge equal to static must not count as a conflict", tally.isEmpty());
    }

    @Test
    public void disagreementLandsInExactlyOneBucket() {
        boolean statik = staticMap.get(PROBE_X, PROBE_Y, 0, LiveCollisionSnapshot.FLAG_NORTH);
        LiveCollisionConflicts.Tally tally =
                LiveCollisionConflicts.tally(snapshotWithNorthEdge(!statik), staticMap);
        assertEquals(1, tally.liveOpensStatic + tally.liveBlocksStatic);
        if (statik) {
            assertEquals("live=blocked where static=open is liveBlocksStatic", 1, tally.liveBlocksStatic);
        } else {
            assertEquals("live=open where static=blocked is liveOpensStatic", 1, tally.liveOpensStatic);
        }
    }

    @Test
    public void unknownEdgesNeverCount() {
        LiveCollisionSnapshot allUnknown = new LiveCollisionSnapshot(BASE_X, BASE_Y, 1,
                new BitSet(), new BitSet(), new BitSet(), new BitSet());
        assertTrue(LiveCollisionConflicts.tally(allUnknown, staticMap).isEmpty());
        assertTrue(LiveCollisionConflicts.tally(null, staticMap).isEmpty());
        assertTrue(LiveCollisionConflicts.tally(allUnknown, null).isEmpty());
    }
}
