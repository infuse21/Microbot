package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.LeafPitPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LeafPitRecoveryTest
{
	private static final WorldPoint SOUTH = new WorldPoint(2274, 3172, 0);
	private static final WorldPoint NORTH = new WorldPoint(2274, 3176, 0);
	private static final WorldPoint PIT = new WorldPoint(2313, 9656, 0);

	@Test
	public void fallClimbReturnAndRetryRemainOnePendingEdgeInBothDirections()
	{
		for (boolean northbound : new boolean[]{true, false})
		{
			Run run = new Run(northbound);
			run.issue(1, run.from, run.jump);
			RouteInteraction recovery = run.stage(PIT, "Climb");
			run.issue(1001, PIT, recovery);
			assertEquals(NavigationDecision.Type.WAIT, run.observe(1601, PIT, recovery).getType());
			assertEquals(run.to, run.engine.snapshot().getPendingInteraction().getCrossingTo());
			run.issue(2001, run.from, run.jump);
			RouteInteraction landed = new CatalogTransitionRouteScanner().observePending(run.jump,
				run.to, edge -> null, 13);
			assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
			assertFalse(run.observe(3001, run.to, landed).issuesInput());
			assertEquals(NavigationDecision.Type.COMPLETE, run.observe(3601, run.to, null).getType());
		}
	}

	@Test
	public void fifthFallStillClimbsOutButDoesNotIssueSixthJump()
	{
		Run run = new Run(true);
		run.issue(1, run.from, run.jump);
		for (int attempt = 1; attempt <= 5; attempt++)
		{
			run.issue(attempt * 2000L, PIT, run.stage(PIT, "Climb"));
			NavigationDecision next = run.observe(attempt * 2000L + 600, run.from, run.jump);
			assertEquals(attempt < 5 ? NavigationDecision.Type.INTERACT
				: NavigationDecision.Type.REQUEST_REPLAN, next.getType());
			if (attempt < 5) run.engine.recordCommandResult(next, true, attempt * 2000L + 600);
		}
	}

	@Test
	public void failedClimbStopsWithoutTryingToWalkOutOfThePit()
	{
		Run run = new Run(true);
		run.issue(1, run.from, run.jump);
		RouteInteraction recovery = run.stage(PIT, "Climb");
		run.issue(1001, PIT, recovery);
		NavigationDecision stopped = run.observe(8001, PIT, recovery);
		assertEquals(NavigationDecision.Type.FAIL, stopped.getType());
		assertEquals("leaf-pit-recovery-not-acknowledged", stopped.getReason());
	}

	@Test
	public void missingHandholdsStopAfterTheExistingCommandDeadline()
	{
		Run run = new Run(false);
		run.issue(1, run.from, run.jump);
		RouteInteraction missing = run.jump.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		assertEquals(NavigationDecision.Type.WAIT, run.observe(1001, PIT, missing).getType());
		NavigationDecision stopped = run.observe(8001, PIT, missing);
		assertEquals(NavigationDecision.Type.FAIL, stopped.getType());
		assertEquals("leaf-pit-recovery-unavailable", stopped.getReason());
	}

	@Test
	public void transientMissingSceneDoesNotHideTheRecoveryStage()
	{
		Run run = new Run(false);
		run.issue(1, run.from, run.jump);
		run.observe(601, PIT, run.jump.withStatus(RouteInteraction.Status.UNAVAILABLE, false));
		run.issue(1201, PIT, run.stage(PIT, "Climb"));
	}

	@Test
	public void depletedRequirementsAfterClimbingOutReplanInsteadOfJumpingAgain()
	{
		Run run = new Run(true);
		run.issue(1, run.from, run.jump);
		RouteInteraction recovery = run.stage(PIT, "Climb");
		run.issue(1001, PIT, recovery);
		RouteInteraction unavailable = recovery.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		NavigationDecision result = run.observe(8001, run.from, unavailable);
		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, result.getType());
		assertFalse(result.issuesInput());
	}

	@Test
	public void rejectedRecoveryAndCancellationNeverIssueAnotherInput()
	{
		Run run = new Run(true);
		run.issue(1, run.from, run.jump);
		RouteInteraction recovery = run.stage(PIT, "Climb");
		NavigationDecision command = run.observe(1001, PIT, recovery);
		run.engine.recordCommandResult(command, false, 1001);
		assertEquals(NavigationDecision.Type.FAIL, run.observe(1601, PIT, recovery).getType());
		Run cancelled = new Run(true);
		cancelled.issue(1, cancelled.from, cancelled.jump);
		cancelled.engine.cancel("test-cancel-in-pit");
		assertEquals(NavigationDecision.Type.NO_ACTION,
			cancelled.observe(1001, PIT, cancelled.stage(PIT, "Climb")).getType());
	}

	private static final class Run
	{
		private final NavigationEngine engine = new NavigationEngine();
		private final WorldPoint from;
		private final WorldPoint to;
		private final RoutePlan plan;
		private final RouteInteraction jump;

		private Run(boolean northbound)
		{
			from = northbound ? SOUTH : NORTH;
			to = northbound ? NORTH : SOUTH;
			engine.start(new NavigationRequest(1, Collections.singleton(to), 0,
				NavigationRouteOptions.defaults(), "leaf-pit-test"));
			plan = new RoutePlan(1, 1, from, Collections.singleton(to), Arrays.asList(from, to),
				Arrays.asList(from, to), true, Collections.singletonList(
					new RouteEdge(0, from, to, RouteEdge.Kind.CATALOG_TRANSITION)));
			jump = stage(from, "Jump");
		}

		private RouteInteraction stage(WorldPoint tile, String action)
		{
			CatalogTransition transition = new CatalogTransition(null, tile, LeafPitPolicy.LEAVES,
				action, "Jump", from, to);
			return new CatalogTransitionRouteScanner().scan(plan, 0, 1, tile, edge -> transition, 13);
		}

		private NavigationDecision observe(long time, WorldPoint player, RouteInteraction interaction)
		{
			return engine.observe(NavigationObservation.route(time, player, plan, false, false,
				false, false, false, false, "leaf-pit-test").withRouteInteraction(interaction));
		}

		private void issue(long time, WorldPoint player, RouteInteraction interaction)
		{
			NavigationDecision decision = observe(time, player, interaction);
			assertEquals(NavigationDecision.Type.INTERACT, decision.getType());
			assertEquals(interaction.getAction(), decision.getInteraction().getAction());
			engine.recordCommandResult(decision, true, time);
		}
	}
}
