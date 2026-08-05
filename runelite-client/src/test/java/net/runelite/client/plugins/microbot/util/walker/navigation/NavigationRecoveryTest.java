package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NavigationRecoveryTest
{
	private static final WorldPoint START = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint OFF_ROUTE = new WorldPoint(3300, 3300, 0);

	@After
	public void resetRuntime()
	{
		NavigationEngineRuntime.resetForTesting();
	}

	@Test
	public void recoveryCategoriesHaveIndependentBudgets()
	{
		NavigationEngine engine = engine();
		NavigationDecision offRoute = engine.observe(observation(1, OFF_ROUTE, plan(1), false));

		NavigationDecision click = engine.observe(observation(2, START, plan(2), false));
		engine.recordCommandResult(click, false, 2);
		NavigationDecision rejected = engine.observe(observation(3, START, plan(2), false));

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, offRoute.getType());
		assertEquals(RecoveryCause.OFF_ROUTE, offRoute.getRecoveryCause());
		assertEquals(1, offRoute.getRecoveryAttempt());
		assertEquals(3, offRoute.getRecoveryBudget());
		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, rejected.getType());
		assertEquals(RecoveryCause.NO_ACKNOWLEDGEMENT, rejected.getRecoveryCause());
		assertEquals(1, rejected.getRecoveryAttempt());
		assertEquals(2, rejected.getRecoveryBudget());
		assertEquals(1, engine.snapshot().getRecoveryAttempts(RecoveryCause.OFF_ROUTE));
		assertEquals(1, engine.snapshot().getRecoveryAttempts(
			RecoveryCause.NO_ACKNOWLEDGEMENT));
	}

	@Test
	public void exhaustedCategoryNamesItsTerminalReason()
	{
		NavigationEngine engine = engine();
		NavigationDecision decision = null;
		for (int generation = 1; generation <= 4; generation++)
		{
			decision = engine.observe(observation(generation, OFF_ROUTE,
				plan(generation), false));
		}

		assertEquals(NavigationDecision.Type.FAIL, decision.getType());
		assertEquals("off-route-budget-exhausted", engine.snapshot().getTerminalReason());
		assertEquals(4, engine.snapshot().getRecoveryAttempts(RecoveryCause.OFF_ROUTE));
		assertEquals(0, engine.snapshot().getRecoveryAttempts(
			RecoveryCause.NO_ACKNOWLEDGEMENT));
	}

	@Test
	public void noAcknowledgementHasItsOwnTerminalBudget()
	{
		NavigationEngine engine = engine();
		NavigationDecision decision = null;
		for (int generation = 1; generation <= 3; generation++)
		{
			NavigationDecision click = engine.observe(observation(generation * 10L,
				START, plan(generation), false));
			engine.recordCommandResult(click, false, generation * 10L);
			decision = engine.observe(observation(generation * 10L + 1,
				START, plan(generation), false));
		}

		assertEquals(NavigationDecision.Type.FAIL, decision.getType());
		assertEquals("no-acknowledgement-budget-exhausted",
			engine.snapshot().getTerminalReason());
		assertEquals(3, engine.snapshot().getRecoveryAttempts(
			RecoveryCause.NO_ACKNOWLEDGEMENT));
		assertEquals(0, engine.snapshot().getRecoveryAttempts(RecoveryCause.OFF_ROUTE));
	}

	@Test
	public void interactionWaitNeverConsumesRecoveryBudget()
	{
		NavigationEngine engine = engine();
		NavigationObservation waiting = observation(1, START, plan(1), false)
			.withRecovery(RecoveryCause.INTERACTION_WAIT, -1);

		NavigationDecision decision = engine.observe(waiting);

		assertEquals(NavigationDecision.Type.WAIT, decision.getType());
		assertEquals(0, engine.snapshot().getRecoveryAttempts());
	}

	@Test
	public void recentValidCommandDefersBlockedEdgeWithoutConsumingBudget()
	{
		NavigationEngine engine = engine();
		NavigationDecision click = engine.observe(observation(1, START, plan(1), false));
		engine.recordCommandResult(click, true, 1);

		NavigationObservation blocked = observation(100, START, plan(1), false)
			.withRecovery(RecoveryCause.BLOCKED_EDGE, 4);
		NavigationDecision decision = engine.observe(blocked);

		assertEquals(NavigationDecision.Type.WAIT, decision.getType());
		assertEquals(0, engine.snapshot().getRecoveryAttempts(RecoveryCause.BLOCKED_EDGE));
	}

	@Test
	public void blockedEdgeReplansOncePerGeneration()
	{
		NavigationEngine engine = engine();
		NavigationObservation first = observation(1, START, plan(1), false)
			.withRecovery(RecoveryCause.BLOCKED_EDGE, 4);
		NavigationObservation repeated = observation(2, START, plan(1), false)
			.withRecovery(RecoveryCause.BLOCKED_EDGE, 4);
		NavigationObservation nextGeneration = observation(3, START, plan(2), false)
			.withRecovery(RecoveryCause.BLOCKED_EDGE, 4);

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, engine.observe(first).getType());
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(repeated).getType());
		assertEquals(NavigationDecision.Type.REQUEST_REPLAN,
			engine.observe(nextGeneration).getType());
		assertEquals(4, engine.snapshot().getDecision().getBlockedEdgeIndex());
		assertEquals(2, engine.snapshot().getRecoveryAttempts(RecoveryCause.BLOCKED_EDGE));
	}

	@Test
	public void noProgressRecoveryIssuesAtMostOneCommandPerPass()
	{
		NavigationEngineRuntime.ensureRequest(request());
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};
		RoutePlan plan = plan(1);
		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, START, plan, false), actions);
		WorldPoint displaced = new WorldPoint(START.getX(), START.getY() + 1, 0);
		NavigationEngineRuntime.execute(observation(10, displaced, plan, true)
			.withMovementDestination(first.getDecision().getTarget()), actions);
		int beforeRecovery = commands.get();

		NavigationExecutionResult recovery = NavigationEngineRuntime.execute(
			observation(2_500, displaced, plan, false), actions);

		assertEquals(NavigationDecision.Type.CLICK_TILE, recovery.getDecision().getType());
		assertEquals("no-tile-progress-rejoin-click", recovery.getDecision().getReason());
		assertEquals(RecoveryCause.NO_TILE_PROGRESS,
			recovery.getDecision().getRecoveryCause());
		assertEquals(1, recovery.getDecision().getRecoveryAttempt());
		assertEquals(2, recovery.getDecision().getRecoveryBudget());
		assertTrue(recovery.getDecision().getRecoveryAgeMs() >= 2_400L);
		assertEquals(beforeRecovery + 1, commands.get());
		assertEquals(1, NavigationEngineRuntime.getSnapshot().getRecoveryAttempts(
			RecoveryCause.NO_TILE_PROGRESS));
	}

	@Test
	public void idleCommandWaitsForNoProgressWindowWithoutResettingTimer()
	{
		NavigationEngineRuntime.ensureRequest(request());
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};
		RoutePlan plan = plan(1);
		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, START, plan, false), actions);
		WorldPoint displaced = new WorldPoint(START.getX(), START.getY() + 1, 0);
		NavigationEngineRuntime.execute(observation(10, displaced, plan, true)
			.withMovementDestination(first.getDecision().getTarget()), actions);

		NavigationExecutionResult waiting = NavigationEngineRuntime.execute(
			observation(1_000, displaced, plan, false), actions);
		NavigationExecutionResult recovery = NavigationEngineRuntime.execute(
			observation(2_500, displaced, plan, false), actions);

		assertEquals(NavigationDecision.Type.WAIT, waiting.getDecision().getType());
		assertEquals("movement-command-progress-window", waiting.getDecision().getReason());
		assertEquals(NavigationDecision.Type.CLICK_TILE, recovery.getDecision().getType());
		assertEquals("no-tile-progress-rejoin-click", recovery.getDecision().getReason());
		assertEquals(2, commands.get());
	}

	@Test
	public void unavailableRejoinRetainsNoTileProgressClassification()
	{
		List<WorldPoint> raw = rawPath();
		NavigationRequest request = new NavigationRequest(41,
			Collections.singleton(raw.get(raw.size() - 1)), 0,
			new NavigationRouteOptions(true, true, false, true, 100),
			"no-progress-no-rejoin-test");
		NavigationEngineRuntime.ensureRequest(request);
		RoutePlan plan = plan(1);
		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, START, plan, false), target -> true);
		WorldPoint displaced = new WorldPoint(START.getX(), START.getY() + 20, 0);
		NavigationEngineRuntime.execute(observation(10, displaced, plan, true)
			.withMovementDestination(first.getDecision().getTarget()), target -> true);

		NavigationExecutionResult recovery = NavigationEngineRuntime.execute(
			observation(2_500, displaced, plan, false), target -> true);

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, recovery.getDecision().getType());
		assertEquals(RecoveryCause.NO_TILE_PROGRESS,
			recovery.getDecision().getRecoveryCause());
		assertEquals("no-tile-progress-no-rejoin-target",
			recovery.getDecision().getReason());
		assertEquals(1, recovery.getDecision().getRecoveryAttempt());
		assertEquals(0, NavigationEngineRuntime.getSnapshot().getRecoveryAttempts(
			RecoveryCause.ROUTE_EXHAUSTED));
	}

	@Test
	public void runtimeKeepsDeferredCollisionUntilEngineCanHandleIt()
	{
		NavigationEngineRuntime.ensureRequest(request());
		RoutePlan plan = plan(1);
		NavigationEngineRuntime.execute(observation(1, START, plan, false), target -> true);
		assertTrue(NavigationEngineRuntime.reportBlockedEdge(1, 4));

		WorldPoint progressed = plan.getRawPath().get(1);
		NavigationExecutionResult recent = NavigationEngineRuntime.execute(
			observation(100, progressed, plan, true), target -> true);
		NavigationExecutionResult handled = NavigationEngineRuntime.execute(
			observation(1_300, progressed, plan, false), target -> true);

		assertEquals(NavigationDecision.Type.WAIT, recent.getDecision().getType());
		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, handled.getDecision().getType());
		assertEquals(1, NavigationEngineRuntime.getSnapshot().getRecoveryAttempts(
			RecoveryCause.BLOCKED_EDGE));
	}

	@Test
	public void runtimeSuppressesCollisionOwnedByPendingInteraction()
	{
		NavigationEngineRuntime.ensureRequest(request());
		RoutePlan plan = plan(1);
		RouteInteraction door = new RouteInteraction(1, 4,
			plan.getRawPath().get(4), plan.getRawPath().get(5), plan.getRawPath().get(5),
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.AVAILABLE, "open", true);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				return true;
			}
		};
		NavigationEngineRuntime.execute(observation(1, START, plan, false)
			.withRouteInteraction(door), actions);

		assertTrue(NavigationEngineRuntime.reportBlockedEdge(1, 5));
		assertTrue(!NavigationEngineRuntime.hasUnobservedRecovery());
		assertTrue(NavigationEngineRuntime.reportBlockedEdge(1, 8));
		assertTrue(NavigationEngineRuntime.hasUnobservedRecovery());
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(request());
		return engine;
	}

	private static NavigationRequest request()
	{
		List<WorldPoint> raw = rawPath();
		return new NavigationRequest(41, Collections.singleton(raw.get(raw.size() - 1)), 0,
			new NavigationRouteOptions(true, true, false, true), "phase-4-test");
	}

	private static NavigationObservation observation(long time, WorldPoint player,
		RoutePlan plan, boolean moving)
	{
		return NavigationObservation.route(time, player, plan, moving, false, false,
			false, false, false, null, "phase-4-test");
	}

	private static RoutePlan plan(long generation)
	{
		List<WorldPoint> raw = rawPath();
		return new RoutePlan(41, generation, START,
			Collections.singleton(raw.get(raw.size() - 1)), raw,
			Collections.singletonList(raw.get(raw.size() - 1)), true);
	}

	private static List<WorldPoint> rawPath()
	{
		List<WorldPoint> raw = new ArrayList<>();
		for (int i = 0; i <= 14; i++)
		{
			raw.add(new WorldPoint(START.getX() + i, START.getY(), 0));
		}
		return raw;
	}
}
