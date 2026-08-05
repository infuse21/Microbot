package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RoutePlannerRuntimeTest
{
	private static SplitFlagMap collisionMap;

	@BeforeClass
	public static void loadCollisionMap()
	{
		collisionMap = SplitFlagMap.fromResources();
	}

	@After
	public void resetRuntime()
	{
		RoutePlannerRuntime.shutdown();
	}

	@Test
	public void stalePreparationCannotInstallAPathfinder() throws Exception
	{
		RoutePlannerRuntime.Preparation stale = RoutePlannerRuntime.beginNewRequest();
		RoutePlannerRuntime.Preparation current = RoutePlannerRuntime.beginNewRequest();
		Pathfinder stalePathfinder = pathfinder(1);
		Pathfinder currentPathfinder = pathfinder(2);

		assertFalse(RoutePlannerRuntime.submit(stale, stalePathfinder));
		assertTrue(RoutePlannerRuntime.submit(current, currentPathfinder));
		awaitPlanGeneration(1);

		assertSame(currentPathfinder, RoutePlannerRuntime.getPathfinder());
		assertEquals(current.getRequestId(), RoutePlannerRuntime.getPublishedPlan().getRequestId());
	}

	@Test
	public void replanKeepsRequestIdentityAndAdvancesGeneration() throws Exception
	{
		RoutePlannerRuntime.Preparation first = RoutePlannerRuntime.beginNewRequest();
		assertTrue(RoutePlannerRuntime.submit(first, pathfinder(1)));
		awaitPlanGeneration(1);

		RoutePlannerRuntime.Preparation second = RoutePlannerRuntime.beginReplan();
		assertEquals(first.getRequestId(), second.getRequestId());
		assertTrue(RoutePlannerRuntime.submit(second, pathfinder(2)));
		awaitPlanGeneration(2);

		assertEquals(first.getRequestId(), RoutePlannerRuntime.getPublishedPlan().getRequestId());
		assertEquals(2, RoutePlannerRuntime.getPublishedPlan().getGeneration());
	}

	@Test
	public void cancellationClearsCompatibilityAndImmutableViews() throws Exception
	{
		RoutePlannerRuntime.Preparation preparation = RoutePlannerRuntime.beginNewRequest();
		assertTrue(RoutePlannerRuntime.submit(preparation, pathfinder(1)));
		awaitPlanGeneration(1);

		RoutePlannerRuntime.cancel();

		assertNull(RoutePlannerRuntime.getPathfinder());
		assertNull(RoutePlannerRuntime.getPublishedPlan());
	}

	private static Pathfinder pathfinder(int offset)
	{
		WorldPoint start = new WorldPoint(3222, 3218, 0);
		WorldPoint target = new WorldPoint(3222 + offset, 3218 + offset, 0);
		PathfinderConfig config = new PathfinderConfig(
			collisionMap, new HashMap<>(), Collections.emptyList(), null, null);
		return new Pathfinder(config, start, target);
	}

	private static void awaitPlanGeneration(long generation) throws InterruptedException
	{
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
		while (System.nanoTime() < deadline)
		{
			RoutePlan plan = RoutePlannerRuntime.getPublishedPlan();
			if (plan != null && plan.getGeneration() == generation)
			{
				return;
			}
			Thread.sleep(5);
		}
		throw new AssertionError("Route generation " + generation + " was not published");
	}
}
