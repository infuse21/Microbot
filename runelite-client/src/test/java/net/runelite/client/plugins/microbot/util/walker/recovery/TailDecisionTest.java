package net.runelite.client.plugins.microbot.util.walker.recovery;

import net.runelite.client.plugins.microbot.util.walker.recovery.TailDecision.TailAction;
import net.runelite.client.plugins.microbot.util.walker.state.WalkExit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Decision table for the end of a walk-loop iteration.
 *
 * <p>These interactions used to be inline in {@code processWalk} and could only be verified by
 * walking around in-game, which is how a partial route came to report UNREACHABLE while the player
 * was still advancing.
 */
public class TailDecisionTest
{
	private static final int MAX = TailDecision.MAX_PARTIAL_RETRIES;

	@Test
	public void arrivalWinsOverEverythingElse()
	{
		assertEquals(TailAction.ARRIVED,
			TailDecision.decide(true, true, WalkExit.NOT_NEAR_PATH, MAX, MAX));
		assertEquals(TailAction.ARRIVED,
			TailDecision.decide(true, false, WalkExit.END_OF_PATH, 0, MAX));
	}

	@Test
	public void completeRouteContinues()
	{
		assertEquals(TailAction.CONTINUE,
			TailDecision.decide(false, false, WalkExit.END_OF_PATH, 0, MAX));
	}

	@Test
	public void completeRouteExemptsBenignYieldsFromTheIterationCap()
	{
		assertEquals(TailAction.CONTINUE_TAIL_EXEMPT,
			TailDecision.decide(false, false, WalkExit.INTERIM_IN_FLIGHT, 0, MAX));
		assertEquals(TailAction.CONTINUE_TAIL_EXEMPT,
			TailDecision.decide(false, false, WalkExit.OFF_PATH_DEFERRED, 0, MAX));
	}

	/**
	 * The regression the whole exercise started from: on a partial route an iteration that advanced
	 * the walk must not spend budget, no matter how much is already spent.
	 */
	@Test
	public void partialRouteDoesNotSpendBudgetOnAnIterationThatAdvanced()
	{
		for (WalkExit progress : new WalkExit[]{
			WalkExit.DOOR_HANDLED,
			WalkExit.TRANSPORT_HANDLED_LOCAL_REACHABILITY,
			WalkExit.FRONTIER_OBSTACLE_HANDLED,
			WalkExit.LOCAL_RECOVERY_CLICK,
			WalkExit.DOOR_SETTLING_YIELD,
			WalkExit.DOOR_EDGE_RESOLVED_AFTER_WAIT})
		{
			assertEquals(progress.name() + " advanced the route and must not spend a retry",
				TailAction.PARTIAL_PROGRESS_REPLAN,
				TailDecision.decide(false, true, progress, MAX, MAX));
		}
	}

	@Test
	public void partialRouteSpendsBudgetWhenItDidNotAdvance()
	{
		assertEquals(TailAction.PARTIAL_RETRY_REPLAN,
			TailDecision.decide(false, true, WalkExit.LOCAL_REACHABILITY_MISS_NO_CLICK, 0, MAX));
		assertEquals(TailAction.PARTIAL_RETRY_REPLAN,
			TailDecision.decide(false, true, WalkExit.NOT_NEAR_PATH, MAX - 1, MAX));
	}

	/** The budget must still terminate, or a genuinely unreachable goal never gives up. */
	@Test
	public void partialRouteGivesUpOnceTheBudgetIsSpent()
	{
		assertEquals(TailAction.PARTIAL_EXHAUSTED,
			TailDecision.decide(false, true, WalkExit.NOT_NEAR_PATH, MAX, MAX));
		assertEquals(TailAction.PARTIAL_EXHAUSTED,
			TailDecision.decide(false, true, WalkExit.DOOR_RECOVERY_SUPPRESSED, MAX + 1, MAX));
	}

	@Test
	public void budgetRefillsOnlyWhenTheWalkBothMovedAndAdvancedSinceTheLastRetry()
	{
		assertTrue("moved and route progressed after the last retry — the walk is working",
			TailDecision.shouldRefillPartialRetryBudget(2, true, 500L, 400L));
		assertFalse("standing still: the route timestamp alone is also bumped by a mere replan, "
				+ "so a retry could refill the budget it just spent",
			TailDecision.shouldRefillPartialRetryBudget(2, false, 500L, 400L));
		assertFalse("no route progress since the last retry",
			TailDecision.shouldRefillPartialRetryBudget(2, true, 300L, 400L));
		assertFalse("nothing spent, nothing to refill",
			TailDecision.shouldRefillPartialRetryBudget(0, true, 500L, 400L));
	}

	@Test
	public void wallClockBudgetIgnoresWalksThatHaveNotStartedOrHaveNoBudget()
	{
		assertFalse(TailDecision.isWallClockExhausted(0L, 10_000_000L, 1_000L));
		assertFalse(TailDecision.isWallClockExhausted(1_000L, 10_000_000L, 0L));
	}

	@Test
	public void wallClockBudgetTripsOnlyAfterTheBudgetElapses()
	{
		assertFalse(TailDecision.isWallClockExhausted(1_000L, 1_000L + 300_000L, 300_000L));
		assertTrue(TailDecision.isWallClockExhausted(1_000L, 1_001L + 300_000L, 300_000L));
	}

	/**
	 * The tail cap cannot see this state: every exempt iteration refunds its own charge, so the
	 * counter never rises and the loop can yield forever.
	 */
	@Test
	public void exemptRunIsBoundedSeparatelyFromTheIterationCap()
	{
		assertFalse(TailDecision.isExemptRunTooLong(24, 24));
		assertTrue(TailDecision.isExemptRunTooLong(25, 24));
		assertFalse("a disabled cap must not fire", TailDecision.isExemptRunTooLong(1_000, 0));
	}
}
