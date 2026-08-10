package net.runelite.client.plugins.microbot.util.walker.recovery;

import net.runelite.client.plugins.microbot.util.walker.state.WalkExit;

/**
 * What the walk loop does at the end of one iteration: finish, replan, give up, or go round again.
 *
 * <p>Pure and fully injected, so the interactions between the partial-retry budget, its refill rule
 * and the tail-iteration exemption can be pinned in a decision table instead of re-discovered on a
 * live walk. The caller still performs the actions — replanning, telemetry, clearing the target.
 *
 * <p>The partial branch is where this matters. A "partial path" is a route the pathfinder could not
 * run all the way to the goal, which is every long or awkward walk, and on those routes the budget
 * is armed for the entire journey. Getting the classification wrong there does not degrade the
 * walk, it aborts it.
 */
public final class TailDecision
{
	/** Consecutive failures to advance on a partial route before the goal is called unreachable. */
	public static final int MAX_PARTIAL_RETRIES = 3;

	private TailDecision()
	{
	}

	public enum TailAction
	{
		/** Within the arrival threshold. */
		ARRIVED,
		/** Partial route, and the iteration advanced it: replan, but do not spend a retry. */
		PARTIAL_PROGRESS_REPLAN,
		/** Partial route and no progress: spend a retry and replan. */
		PARTIAL_RETRY_REPLAN,
		/** Partial route, budget spent: the goal is unreachable. */
		PARTIAL_EXHAUSTED,
		/** Go round again, charging one tail iteration. */
		CONTINUE,
		/** Go round again without charging a tail iteration (a benign yield). */
		CONTINUE_TAIL_EXEMPT
	}

	/**
	 * Route progress since the last retry means the walk is working, so the budget refills.
	 *
	 * <p>Standing somewhere new is required as well as the progress timestamp: the timestamp is also
	 * bumped when the route is merely REPLACED, and every retry replans, so the timestamp alone
	 * would let a retry refill the budget it just spent. Requiring movement is what still lets the
	 * budget drain when the target is genuinely unreachable and the player has stopped.
	 */
	public static boolean shouldRefillPartialRetryBudget(int retriesSpent,
														 boolean movedSinceLastRetry,
														 long routeProgressAdvancedAtMs,
														 long lastPartialRetryAtMs)
	{
		return retriesSpent > 0
			&& movedSinceLastRetry
			&& routeProgressAdvancedAtMs > lastPartialRetryAtMs;
	}

	/**
	 * @param retriesSpent budget already spent, AFTER any refill from
	 *                     {@link #shouldRefillPartialRetryBudget}
	 */
	public static TailAction decide(boolean withinFinishThreshold,
									boolean partialPath,
									WalkExit exit,
									int retriesSpent,
									int maxRetries)
	{
		if (withinFinishThreshold)
		{
			return TailAction.ARRIVED;
		}
		if (partialPath)
		{
			if (exit != null && exit.isProgress())
			{
				return TailAction.PARTIAL_PROGRESS_REPLAN;
			}
			return retriesSpent < maxRetries
				? TailAction.PARTIAL_RETRY_REPLAN
				: TailAction.PARTIAL_EXHAUSTED;
		}
		return exit != null && exit.isTailExempt()
			? TailAction.CONTINUE_TAIL_EXEMPT
			: TailAction.CONTINUE;
	}

	/**
	 * Whether the walk has run past its wall-clock budget.
	 *
	 * <p>The loop's iteration cap is not a bound on its own: several exit reasons decrement the tail
	 * counter, so a walk that keeps producing one of them goes round forever. Nothing else in the
	 * call chain imposes a time limit either.
	 *
	 * <p>Sized to catch a livelock, not a slow walk — a budget that aborts a working long journey
	 * would be a worse bug than the one it is guarding against.
	 */
	public static boolean isWallClockExhausted(long walkStartedAtMs, long nowMs, long budgetMs)
	{
		return walkStartedAtMs > 0L && budgetMs > 0L && nowMs - walkStartedAtMs > budgetMs;
	}

	/**
	 * Companion bound to {@link #isWallClockExhausted}: an uninterrupted run of tail-exempt
	 * iterations means the loop is yielding without ever advancing, which the iteration cap cannot
	 * see because those iterations refund themselves.
	 */
	public static boolean isExemptRunTooLong(int consecutiveExemptIterations, int cap)
	{
		return cap > 0 && consecutiveExemptIterations > cap;
	}
}
