package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RoutePlannerTest
{
	private final ExecutorService executor = Executors.newFixedThreadPool(2);
	private final RoutePlanner planner = new RoutePlanner(executor, false);

	@After
	public void tearDown()
	{
		planner.close();
		executor.shutdownNow();
	}

	@Test
	public void staleCompletionCannotReplaceNewerRequest() throws Exception
	{
		CountDownLatch oldStarted = new CountDownLatch(1);
		CountDownLatch releaseOld = new CountDownLatch(1);
		planner.submit(1, (requestId, generation) -> {
			oldStarted.countDown();
			awaitIgnoringInterrupt(releaseOld);
			return plan(requestId, generation, 1);
		});
		assertTrue(oldStarted.await(2, TimeUnit.SECONDS));

		planner.submit(2, (requestId, generation) -> plan(requestId, generation, 2));
		awaitPublishedRequest(2);
		releaseOld.countDown();
		Thread.sleep(50);

		assertEquals(2, planner.getPublishedPlan().getRequestId());
		assertEquals(new WorldPoint(2, 0, 0), planner.getPublishedPlan().getEndpoint());
	}

	@Test
	public void recalculationAdvancesGenerationAndRejectsOldResult() throws Exception
	{
		CountDownLatch oldStarted = new CountDownLatch(1);
		CountDownLatch releaseOld = new CountDownLatch(1);
		RoutePlanner.Ticket first = planner.submit(7, (requestId, generation) -> {
			oldStarted.countDown();
			awaitIgnoringInterrupt(releaseOld);
			return plan(requestId, generation, 1);
		});
		assertTrue(oldStarted.await(2, TimeUnit.SECONDS));

		RoutePlanner.Ticket second = planner.submit(7,
			(requestId, generation) -> plan(requestId, generation, 2));
		awaitPublishedRequest(7);
		releaseOld.countDown();

		assertEquals(1, first.getGeneration());
		assertEquals(2, second.getGeneration());
		assertEquals(2, planner.getPublishedPlan().getGeneration());
	}

	@Test
	public void cancellationIsTerminalForInFlightResult() throws Exception
	{
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicBoolean cancelCalled = new AtomicBoolean();
		RoutePlanner.Calculation calculation = new RoutePlanner.Calculation()
		{
			@Override
			public RoutePlan calculate(long requestId, long generation)
			{
				started.countDown();
				awaitIgnoringInterrupt(release);
				return plan(requestId, generation, 1);
			}

			@Override
			public void cancel()
			{
				cancelCalled.set(true);
			}
		};

		planner.submit(9, calculation);
		assertTrue(started.await(2, TimeUnit.SECONDS));
		planner.cancel(9);
		release.countDown();
		Thread.sleep(50);

		assertTrue(cancelCalled.get());
		assertNull(planner.getActiveTicket());
		assertNull(planner.getPublishedPlan());
	}

	@Test
	public void supersededCalculationKeepsRequestAndAdvancesGeneration() throws Exception
	{
		RoutePlanner.Ticket first = planner.submit(12,
			(requestId, generation) -> plan(requestId, generation, 1));
		awaitPublishedRequest(12);
		planner.supersede(12);
		RoutePlanner.Ticket second = planner.submit(12,
			(requestId, generation) -> plan(requestId, generation, 2));
		awaitPublishedRequest(12);

		assertEquals(1, first.getGeneration());
		assertEquals(2, second.getGeneration());
		assertEquals(2, planner.getPublishedPlan().getGeneration());
	}

	@Test
	public void synchronousPublicationUsesTheSameGenerationSequence()
	{
		RoutePlanner.Ticket first = planner.publishCompleted(15,
			(requestId, generation) -> plan(requestId, generation, 1));
		RoutePlanner.Ticket second = planner.publishCompleted(15,
			(requestId, generation) -> plan(requestId, generation, 2));

		assertEquals(1, first.getGeneration());
		assertEquals(2, second.getGeneration());
		assertEquals(new WorldPoint(2, 0, 0), planner.getPublishedPlan().getEndpoint());
	}

	@Test
	public void routePlanDefensivelyCopiesPaths()
	{
		java.util.ArrayList<WorldPoint> raw = new java.util.ArrayList<>();
		raw.add(new WorldPoint(1, 0, 0));
		RoutePlan plan = new RoutePlan(1, 1, new WorldPoint(0, 0, 0),
			Collections.singleton(new WorldPoint(1, 0, 0)), raw, raw, true);
		raw.add(new WorldPoint(2, 0, 0));

		assertEquals(1, plan.getRawPath().size());
		try
		{
			plan.getRawPath().add(new WorldPoint(3, 0, 0));
			fail("route path must be immutable");
		}
		catch (UnsupportedOperationException expected)
		{
			// expected
		}
		assertFalse(plan.getTargets().isEmpty());
	}

	@Test
	public void routePlanMapsSmoothedWaypointsToRawAnchorsOnce()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(2, 1, 0);
		WorldPoint c = new WorldPoint(3, 1, 0);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(c),
			java.util.Arrays.asList(a, b, c), java.util.Arrays.asList(a, c), true);

		assertTrue(plan.isOrdinaryWalkOnly());
		assertEquals(java.util.Arrays.asList(0, 2), boxed(plan.getSmoothedToRaw()));
		assertEquals(2, plan.getRouteEdges().size());
	}

	@Test
	public void explicitAdjacentTransportMakesRouteIneligibleForOrdinaryExecution()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(2, 1, 0);
		RouteEdge transport = new RouteEdge(0, a, b, RouteEdge.Kind.TRANSPORT);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(b),
			java.util.Arrays.asList(a, b), java.util.Arrays.asList(a, b), true,
			Collections.singletonList(transport));

		assertFalse(plan.isOrdinaryWalkOnly());
		assertFalse(plan.isEngineSupported());
	}

	@Test
	public void migratedAdjacentTransportIsEngineSupportedButNotOrdinary()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(2, 1, 0);
		RouteEdge transport = new RouteEdge(0, a, b, RouteEdge.Kind.ADJACENT_TRANSPORT);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(b),
			java.util.Arrays.asList(a, b), java.util.Arrays.asList(a, b), true,
			Collections.singletonList(transport));

		assertFalse(plan.isOrdinaryWalkOnly());
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void migratedCatalogTransitionIsEngineSupportedButNotOrdinary()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(1, 1, 1);
		RouteEdge transition = new RouteEdge(0, a, b,
			RouteEdge.Kind.CATALOG_TRANSITION);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(b),
			java.util.Arrays.asList(a, b), java.util.Arrays.asList(a, b), true,
			Collections.singletonList(transition));

		assertFalse(plan.isOrdinaryWalkOnly());
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void migratedSimpleTeleportIsEngineSupportedButNotOrdinary()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(100, 100, 0);
		RouteEdge teleport = new RouteEdge(0, a, b, RouteEdge.Kind.SIMPLE_TELEPORT);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(b),
			java.util.Arrays.asList(a, b), java.util.Arrays.asList(a, b), true,
			Collections.singletonList(teleport));

		assertFalse(plan.isOrdinaryWalkOnly());
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void migratedNpcTransportIsEngineSupportedButNotOrdinary()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(100, 100, 0);
		RouteEdge transport = new RouteEdge(0, a, b, RouteEdge.Kind.NPC_TRANSPORT);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(b),
			java.util.Arrays.asList(a, b), java.util.Arrays.asList(a, b), true,
			Collections.singletonList(transport));

		assertFalse(plan.isOrdinaryWalkOnly());
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void migratedCharterShipIsEngineSupportedButNotOrdinary()
	{
		WorldPoint a = new WorldPoint(1, 1, 0);
		WorldPoint b = new WorldPoint(100, 100, 1);
		RouteEdge transport = new RouteEdge(0, a, b, RouteEdge.Kind.CHARTER_SHIP);
		RoutePlan plan = new RoutePlan(1, 1, a, Collections.singleton(b),
			java.util.Arrays.asList(a, b), java.util.Arrays.asList(a, b), true,
			Collections.singletonList(transport));

		assertFalse(plan.isOrdinaryWalkOnly());
		assertTrue(plan.isEngineSupported());
	}

	private static List<Integer> boxed(int[] values)
	{
		java.util.ArrayList<Integer> boxed = new java.util.ArrayList<>(values.length);
		for (int value : values)
		{
			boxed.add(value);
		}
		return boxed;
	}

	private void awaitPublishedRequest(long requestId) throws InterruptedException
	{
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		while (System.nanoTime() < deadline)
		{
			RoutePlan plan = planner.getPublishedPlan();
			if (plan != null && plan.getRequestId() == requestId)
			{
				return;
			}
			Thread.sleep(5);
		}
		throw new AssertionError("Route " + requestId + " was not published");
	}

	private static RoutePlan plan(long requestId, long generation, int endpointX)
	{
		WorldPoint start = new WorldPoint(0, 0, 0);
		WorldPoint endpoint = new WorldPoint(endpointX, 0, 0);
		List<WorldPoint> path = java.util.Arrays.asList(start, endpoint);
		return new RoutePlan(requestId, generation, start, Collections.singleton(endpoint), path, path, true);
	}

	private static void awaitIgnoringInterrupt(CountDownLatch latch)
	{
		boolean interrupted = false;
		for (;;)
		{
			try
			{
				latch.await();
				break;
			}
			catch (InterruptedException ignored)
			{
				interrupted = true;
			}
		}
		if (interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}
}
