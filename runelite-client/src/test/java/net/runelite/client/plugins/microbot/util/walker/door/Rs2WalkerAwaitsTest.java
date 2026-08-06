package net.runelite.client.plugins.microbot.util.walker.door;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2WalkerAwaitsTest {
    @Test
    public void shouldAcceptIdleDoorAwait_acceptsStationaryPastMinimum() {
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1200L, false));
        assertTrue(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1201L, true));
    }

    /**
     * The old implementation ended in {@code return edgeResolved}, which made this branch dead code:
     * the traversal wait returns the moment the edge resolves, so it only ever reached here with
     * edgeResolved == false. A stalled interaction burned the full 2200ms budget instead of releasing.
     */
    @Test
    public void shouldAcceptIdleDoorAwait_acceptsUnresolvedEdgeWhenStalled() {
        assertTrue(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1500L, false));
    }

    @Test
    public void shouldAcceptIdleDoorAwait_rejectsMovingOrAnimating() {
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(true, false, 5000L, true));
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, true, 5000L, true));
    }

    @Test
    public void shouldAcceptIdleDoorAwait_rejectsBeforeMinimumElapsed() {
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 1200L, true));
        assertFalse(Rs2WalkerAwaits.shouldAcceptIdleDoorAwait(false, false, 800L, true));
    }

    // ---- door-open observation throttle -------------------------------------------------------------

    /**
     * An unlocked door opens within one game tick, so an observation before then can only report
     * "still shut". The observation is a scene scan, not a field read, which is why it is rationed at
     * all rather than run on every poll of the surrounding wait.
     */
    @Test
    public void shouldPollDoorOpen_notBeforeADoorCouldHaveOpened() {
        assertFalse(Rs2WalkerAwaits.shouldPollDoorOpen(0L, 10_000L));
        assertFalse(Rs2WalkerAwaits.shouldPollDoorOpen(100L, 10_000L));
    }

    @Test
    public void shouldPollDoorOpen_onceTheFirstTickHasPassed() {
        assertTrue(Rs2WalkerAwaits.shouldPollDoorOpen(250L, 10_000L));
        assertTrue(Rs2WalkerAwaits.shouldPollDoorOpen(600L, 10_000L));
    }

    /** Rationed: a fresh observation is not worth a scene scan on every poll of the wait. */
    @Test
    public void shouldPollDoorOpen_notMoreOftenThanTheInterval() {
        assertFalse(Rs2WalkerAwaits.shouldPollDoorOpen(1_000L, 0L));
        assertFalse(Rs2WalkerAwaits.shouldPollDoorOpen(1_000L, 100L));
        assertTrue(Rs2WalkerAwaits.shouldPollDoorOpen(1_000L, 250L));
    }
}
