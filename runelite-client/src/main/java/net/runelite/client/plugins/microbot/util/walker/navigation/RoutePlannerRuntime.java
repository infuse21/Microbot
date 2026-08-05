package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;

import java.util.Objects;

/**
 * Process-wide production owner of pathfinder calculation and publication state.
 *
 * <p>The static surface is temporary compatibility for the always-on shortest-path plugin and the
 * static Rs2Walker API. Unlike the old plugin fields, callers cannot obtain the executor, Future, or
 * lifecycle mutex. Phase 2 moves request ownership into NavigationEngine; this class remains the
 * planner boundary underneath it.</p>
 */
public final class RoutePlannerRuntime
{
	public static final class Preparation
	{
		private final long requestId;
		private final long sequence;

		private Preparation(long requestId, long sequence)
		{
			this.requestId = requestId;
			this.sequence = sequence;
		}

		public long getRequestId()
		{
			return requestId;
		}
	}

	private static final Object MUTEX = new Object();
	private static RoutePlanner planner = new RoutePlanner();
	private static long nextRequestId;
	private static long currentRequestId;
	private static long preparationSequence;
	private static volatile Pathfinder pathfinder;

	private RoutePlannerRuntime()
	{
	}

	public static Preparation beginNewRequest()
	{
		synchronized (MUTEX)
		{
			ensurePlanner();
			if (currentRequestId != 0)
			{
				planner.cancel(currentRequestId);
			}
			currentRequestId = ++nextRequestId;
			pathfinder = null;
			return new Preparation(currentRequestId, ++preparationSequence);
		}
	}

	public static Preparation beginReplan()
	{
		synchronized (MUTEX)
		{
			ensurePlanner();
			if (currentRequestId == 0)
			{
				currentRequestId = ++nextRequestId;
			}
			else
			{
				planner.supersede(currentRequestId);
			}
			return new Preparation(currentRequestId, ++preparationSequence);
		}
	}

	public static boolean submit(Preparation preparation, Pathfinder nextPathfinder)
	{
		Objects.requireNonNull(preparation, "preparation");
		Objects.requireNonNull(nextPathfinder, "nextPathfinder");
		synchronized (MUTEX)
		{
			ensurePlanner();
			if (!isCurrent(preparation))
			{
				nextPathfinder.cancel();
				return false;
			}
			pathfinder = nextPathfinder;
			planner.submit(preparation.requestId, new PathfinderRouteCalculation(nextPathfinder));
			return true;
		}
	}

	public static boolean publishCompleted(Preparation preparation, Pathfinder completedPathfinder)
	{
		Objects.requireNonNull(preparation, "preparation");
		Objects.requireNonNull(completedPathfinder, "completedPathfinder");
		synchronized (MUTEX)
		{
			ensurePlanner();
			if (!isCurrent(preparation))
			{
				completedPathfinder.cancel();
				return false;
			}
			pathfinder = completedPathfinder;
			planner.publishCompleted(preparation.requestId,
				(requestId, generation) -> PathfinderRouteCalculation.snapshot(
					completedPathfinder, requestId, generation));
			return true;
		}
	}

	public static Pathfinder getPathfinder()
	{
		return pathfinder;
	}

	public static RoutePlan getPublishedPlan()
	{
		synchronized (MUTEX)
		{
			return planner == null ? null : planner.getPublishedPlan();
		}
	}

	public static void cancel()
	{
		synchronized (MUTEX)
		{
			if (planner != null && currentRequestId != 0)
			{
				planner.cancel(currentRequestId);
			}
			currentRequestId = 0;
			preparationSequence++;
			pathfinder = null;
		}
		NavigationEngineRuntime.cancel("route-planner-cancelled");
	}

	public static void shutdown()
	{
		synchronized (MUTEX)
		{
			if (planner != null)
			{
				planner.close();
				planner = null;
			}
			currentRequestId = 0;
			preparationSequence++;
			pathfinder = null;
		}
		NavigationEngineRuntime.cancel("route-planner-shutdown");
	}

	private static boolean isCurrent(Preparation preparation)
	{
		return preparation.requestId == currentRequestId
			&& preparation.sequence == preparationSequence;
	}

	private static void ensurePlanner()
	{
		if (planner == null)
		{
			planner = new RoutePlanner();
		}
	}
}
