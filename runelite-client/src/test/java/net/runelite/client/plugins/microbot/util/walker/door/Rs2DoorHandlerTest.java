package net.runelite.client.plugins.microbot.util.walker.door;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The edge-scoped global door cooldown. The full window is anti-hammer for re-clicking ONE door; a
 * different door immediately after a successful open is chaining, not hammering, and holding it for
 * the full window serialised every pair of nearby doors at ~1.8s each.
 */
public class Rs2DoorHandlerTest {

    private static final long FULL = 1_800L;
    private static final long CROSS = 600L;
    private static final long CLICKED_AT = 100_000L;
    private static final long NEXT_ALLOWED = CLICKED_AT + FULL;

    @Test
    public void sameEdgeKeepsTheFullWindow() {
        assertTrue(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT + 1_000L, NEXT_ALLOWED, true, FULL, CROSS));
        assertFalse(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT + FULL, NEXT_ALLOWED, true, FULL, CROSS));
    }

    /** A different door owes one tick, no more — that is what a player chaining two doors looks like. */
    @Test
    public void differentEdgeOwesOnlyTheCrossEdgeFloor() {
        assertTrue(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT + 200L, NEXT_ALLOWED, false, FULL, CROSS));
        assertFalse(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT + CROSS, NEXT_ALLOWED, false, FULL, CROSS));
        assertFalse(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT + 1_000L, NEXT_ALLOWED, false, FULL, CROSS));
    }

    /** No window stamped (or long expired): nothing throttles either way. */
    @Test
    public void expiredWindowThrottlesNothing() {
        assertFalse(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT + 10_000L, NEXT_ALLOWED, true, FULL, CROSS));
        assertFalse(Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(
                CLICKED_AT, 0L, false, FULL, CROSS));
    }

    // --- Door strike-out: registerDoorCrossFailure -------------------------------------------------
    //
    // Seeded from the Tithe Farm incident (2026-08-12): Farm door 27445 refused to pass a seedless
    // player, and with no strike-out the walker ping-ponged door->recovery for 4+ minutes until a
    // human cancelled it. Three concluded-but-uncrossed attempts must strike the edge out.

    private static final long DECAY = 300_000L;
    private static final int LIMIT = 3;
    private static final String EDGE = "1804,3501,p0->1805,3501,p0";

    private static java.util.Map<String, long[]> strikes() {
        return new java.util.HashMap<>();
    }

    @Test
    public void thirdConclusiveFailureStrikesOut() {
        java.util.Map<String, long[]> map = strikes();
        assertSame(Rs2DoorHandler.DoorStrike.COUNTED,
                Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 1_000L, DECAY, LIMIT));
        assertSame(Rs2DoorHandler.DoorStrike.COUNTED,
                Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 13_000L, DECAY, LIMIT));
        assertSame(Rs2DoorHandler.DoorStrike.STRIKE_OUT,
                Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 25_000L, DECAY, LIMIT));
        // The strike-out consumed the entry: the edge starts fresh if it is ever attempted again.
        assertSame(Rs2DoorHandler.DoorStrike.COUNTED,
                Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 26_000L, DECAY, LIMIT));
    }

    /** A moving or cancelled sample proves only that the approach was in flight — the Wydin lesson. */
    @Test
    public void inconclusiveSamplesNeverCount() {
        java.util.Map<String, long[]> map = strikes();
        for (int i = 0; i < 10; i++) {
            assertSame(Rs2DoorHandler.DoorStrike.NOT_COUNTED,
                    Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, false, 1_000L + i, DECAY, LIMIT));
        }
        assertTrue(map.isEmpty());
    }

    /** Strikes older than the decay window reset; two failures an hour apart are not a pattern. */
    @Test
    public void staleStrikesDecay() {
        java.util.Map<String, long[]> map = strikes();
        Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 1_000L, DECAY, LIMIT);
        Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 2_000L, DECAY, LIMIT);
        // Third failure arrives after the decay window: the old two evaporate, count restarts at 1.
        assertSame(Rs2DoorHandler.DoorStrike.COUNTED,
                Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 2_000L + DECAY + 1, DECAY, LIMIT));
    }

    @Test
    public void edgesCountIndependently() {
        java.util.Map<String, long[]> map = strikes();
        Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 1_000L, DECAY, LIMIT);
        Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 2_000L, DECAY, LIMIT);
        assertSame(Rs2DoorHandler.DoorStrike.COUNTED,
                Rs2DoorHandler.registerDoorCrossFailure(map, "other-edge", true, 3_000L, DECAY, LIMIT));
    }

    /** A successful crossing forgives accumulated strikes (transient refusals must not accrue). */
    @Test
    public void successfulCrossingClearsStrikes() {
        java.util.Map<String, long[]> map = strikes();
        Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 1_000L, DECAY, LIMIT);
        Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 2_000L, DECAY, LIMIT);
        Rs2DoorHandler.clearDoorCrossFailures(map, EDGE);
        assertSame(Rs2DoorHandler.DoorStrike.COUNTED,
                Rs2DoorHandler.registerDoorCrossFailure(map, EDGE, true, 3_000L, DECAY, LIMIT));
    }
}
