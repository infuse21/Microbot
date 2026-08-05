package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationEngineTest
{
	private static final WorldPoint START = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint MID = new WorldPoint(3201, 3200, 0);
	private static final WorldPoint LATE = new WorldPoint(3202, 3200, 0);
	private static final WorldPoint TARGET = new WorldPoint(3203, 3200, 0);

	@Test
	public void phaseTransitionTableIsExplicit()
	{
		NavigationEngine engine = engine();

		assertDecision(engine, observation(null, START, false, false, false),
			NavigationDecision.Type.WAIT, NavigationPhase.CALCULATING);
		assertDecision(engine, observation(plan(1), START, false, false, false),
			NavigationDecision.Type.CLICK_TILE, NavigationPhase.FOLLOWING_ROUTE);
		assertDecision(engine, observation(plan(1), MID, true, true, false),
			NavigationDecision.Type.WAIT, NavigationPhase.APPROACHING_INTERACTION);
		assertDecision(engine, observation(plan(1), MID, false, true, false),
			NavigationDecision.Type.INTERACT, NavigationPhase.PERFORMING_INTERACTION);

		NavigationObservation verifying = NavigationObservation.route(4, MID, plan(1), false,
			false, true, true, true, false, NavigationDecision.Type.WAIT, "legacy-interaction-wait");
		assertDecision(engine, verifying, NavigationDecision.Type.WAIT,
			NavigationPhase.VERIFYING_INTERACTION);

		NavigationObservation replan = NavigationObservation.route(5, MID, plan(1), false,
			false, false, false, false, true, NavigationDecision.Type.REQUEST_REPLAN,
			"legacy-off-path");
		assertDecision(engine, replan, NavigationDecision.Type.REQUEST_REPLAN,
			NavigationPhase.REPLANNING);

		assertDecision(engine, NavigationObservation.terminal(
			NavigationObservation.TerminalSignal.ARRIVED, "legacy-arrived"),
			NavigationDecision.Type.COMPLETE, NavigationPhase.ARRIVED);
	}

	@Test
	public void onePassReturnsAtMostOneInputCommand()
	{
		NavigationEngine engine = engine();
		NavigationDecision click = engine.observe(observation(plan(1), START, false, false, false));

		assertTrue(click.issuesInput());
		assertEquals(NavigationDecision.Type.CLICK_TILE, click.getType());
		assertEquals(TARGET, click.getTarget());
		assertEquals(3, click.getTargetRawIndex());
		assertEquals(3, click.getTargetSmoothedIndex());
		assertEquals("raw-route-lookahead", click.getTargetSelection());

		NavigationDecision interaction = engine.observe(
			observation(plan(1), MID, false, true, false));
		assertTrue(interaction.issuesInput());
		assertEquals(NavigationDecision.Type.INTERACT, interaction.getType());
		assertEquals(null, interaction.getTarget());
	}

	@Test
	public void cancellationIsTerminal()
	{
		NavigationEngine engine = engine();
		engine.observe(observation(plan(1), START, false, false, false));
		NavigationSnapshot cancelled = engine.cancel("test-cancel");

		assertEquals(NavigationPhase.CANCELLED, cancelled.getPhase());
		assertTrue(cancelled.isTerminal());
		assertTrue(cancelled.getRequest().getCancellationToken().isCancelled());

		NavigationDecision afterCancel = engine.observe(observation(plan(2), MID, false, false, true));
		assertEquals(NavigationDecision.Type.NO_ACTION, afterCancel.getType());
		assertEquals(NavigationPhase.CANCELLED, engine.snapshot().getPhase());
	}

	@Test
	public void progressOnlyMovesBackwardForANewGeneration()
	{
		NavigationEngine engine = engine();
		engine.observe(observation(plan(1), LATE, true, false, false));
		assertEquals(2, engine.snapshot().getSmoothedProgressIndex());

		engine.observe(observation(plan(1), MID, true, false, false));
		assertEquals(2, engine.snapshot().getSmoothedProgressIndex());

		engine.observe(observation(plan(2), START, true, false, false));
		assertEquals(0, engine.snapshot().getSmoothedProgressIndex());
		assertEquals(2, engine.snapshot().getGeneration());
	}

	@Test
	public void movementInFlightDoesNotConsumeRecoveryBudget()
	{
		NavigationEngine engine = engine();
		NavigationObservation movingReplan = NavigationObservation.route(1, START, plan(1), true,
			false, false, false, false, true, NavigationDecision.Type.WAIT, "legacy-moving");

		NavigationDecision decision = engine.observe(movingReplan);

		assertEquals(NavigationDecision.Type.WAIT, decision.getType());
		assertEquals(0, engine.snapshot().getRecoveryAttempts());
		assertEquals(NavigationPhase.FOLLOWING_ROUTE, engine.snapshot().getPhase());
	}

	@Test
	public void legacyComparisonClassifiesDivergence()
	{
		NavigationEngine engine = engine();
		NavigationObservation observation = NavigationObservation.route(1, START, plan(1), false,
			false, false, false, false, false, NavigationDecision.Type.WAIT, "legacy-yield");

		engine.observe(observation);

		assertEquals(NavigationComparison.SHADOW_ONLY, engine.snapshot().getComparison());
		assertFalse(engine.snapshot().isTerminal());
	}

	@Test
	public void mineableInteractionIsVerifiedBeforeCrossingContinues()
	{
		NavigationEngine engine = engine();
		RouteInteraction available = interaction(RouteInteraction.Status.AVAILABLE, true);
		NavigationObservation ready = observation(plan(1), MID, false, false, false)
			.withRouteInteraction(available);

		NavigationDecision interact = engine.observe(ready);
		assertEquals(NavigationDecision.Type.INTERACT, interact.getType());
		assertEquals(available, interact.getInteraction());
		engine.recordCommandResult(interact, true, 1L);

		NavigationDecision verify = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(interaction(RouteInteraction.Status.CLEARED, true)));
		assertEquals(NavigationDecision.Type.WAIT, verify.getType());
		assertEquals(NavigationPhase.VERIFYING_INTERACTION, engine.snapshot().getPhase());
		assertEquals(available.getRawEdgeIndex(),
			engine.snapshot().getPendingInteraction().getRawEdgeIndex());

		NavigationDecision cross = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(interaction(RouteInteraction.Status.CLEARED, true)));
		assertEquals(NavigationDecision.Type.CLICK_TILE, cross.getType());
		assertEquals(TARGET, cross.getTarget());
		assertEquals("raw-route-lookahead", cross.getTargetSelection());
		assertEquals("cross-cleared-interaction-edge", cross.getReason());

		NavigationDecision crossed = engine.observe(observation(plan(1), LATE, true, false, false)
			.withRouteInteraction(interaction(RouteInteraction.Status.CLEARED, true)));
		assertEquals(NavigationDecision.Type.WAIT, crossed.getType());
		assertEquals("interaction-edge-crossed", crossed.getReason());
		assertEquals(null, engine.snapshot().getPendingInteraction());
	}

	@Test
	public void unavailableMineableReportsRecoveryToEngine()
	{
		NavigationEngine engine = engine();
		NavigationDecision decision = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(interaction(RouteInteraction.Status.UNAVAILABLE, true)));

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, decision.getType());
		assertEquals(RecoveryCause.INTERACTION_UNAVAILABLE, decision.getRecoveryCause());
		assertEquals(1, decision.getRecoveryAttempt());
	}

	@Test
	public void unavailableObservationDefersToInFlightInteractionCommand()
	{
		NavigationEngine engine = engine();
		RouteInteraction available = interaction(RouteInteraction.Status.AVAILABLE, true);
		NavigationDecision interact = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(available));
		assertEquals(NavigationDecision.Type.INTERACT, interact.getType());
		engine.recordCommandResult(interact, true, 1L);

		// Mid-crossing the scene can transiently fail to re-resolve the object; the issued
		// command owns the interaction until its acknowledgement deadline.
		NavigationDecision waiting = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(interaction(RouteInteraction.Status.UNAVAILABLE, false)));
		assertEquals(NavigationDecision.Type.WAIT, waiting.getType());
		assertEquals("interaction-unavailable-command-in-flight", waiting.getReason());
		assertEquals(0, engine.snapshot().getRecoveryAttempts());

		NavigationDecision afterDeadline = engine.observe(
			NavigationObservation.route(60_000L, MID, plan(1), false, false, false, false,
				false, false, null, "deadline-passed")
				.withRouteInteraction(interaction(RouteInteraction.Status.UNAVAILABLE, false)));
		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, afterDeadline.getType());
		assertEquals(RecoveryCause.INTERACTION_UNAVAILABLE, afterDeadline.getRecoveryCause());
	}

	@Test
	public void readyMineableSupersedesGroundMovement()
	{
		NavigationEngine engine = engine();
		RouteInteraction available = interaction(RouteInteraction.Status.AVAILABLE, true);

		NavigationDecision decision = engine.observe(observation(plan(1), MID, true, false, false)
			.withRouteInteraction(available));

		assertEquals(NavigationDecision.Type.INTERACT, decision.getType());
		assertEquals(available, decision.getInteraction());
		assertEquals(NavigationPhase.PERFORMING_INTERACTION, engine.snapshot().getPhase());
	}

	@Test
	public void rangedInteractionDoesNotRepeatWhileServerApproachIsMoving()
	{
		NavigationEngine engine = engine();
		RouteInteraction available = interaction(RouteInteraction.Status.AVAILABLE, true);
		NavigationDecision interact = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(available));
		engine.recordCommandResult(interact, true, 1L);

		NavigationObservation movingAfterTimeout = NavigationObservation.route(10_000L, MID,
			plan(1), true, false, false, false, false, false, null, "server-approach")
			.withRouteInteraction(available);
		NavigationDecision wait = engine.observe(movingAfterTimeout);

		assertEquals(NavigationDecision.Type.WAIT, wait.getType());
		assertEquals("interaction-command-in-flight", wait.getReason());
	}

	@Test
	public void rangedInteractionUsesDistanceAwareAcknowledgementWindow()
	{
		NavigationEngine engine = engine();
		RouteInteraction ranged = new RouteInteraction(1, 1, MID, LATE, TARGET,
			RouteInteraction.Kind.MINEABLE, RouteInteraction.Status.AVAILABLE, "mine", true);
		NavigationDecision interact = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(ranged));
		engine.recordCommandResult(interact, true, 1L);

		NavigationObservation settledAfterBaseTimeout = NavigationObservation.route(6_000L, MID,
			plan(1), false, false, false, false, false, false, null, "server-approach")
			.withRouteInteraction(ranged);
		NavigationDecision wait = engine.observe(settledAfterBaseTimeout);

		assertEquals(NavigationDecision.Type.WAIT, wait.getType());
		assertEquals("interaction-command-in-flight", wait.getReason());
	}

	@Test
	public void transformedTrapdoorStageCanDispatchWithoutWaitingForOldDeadline()
	{
		NavigationEngine engine = engine();
		RouteInteraction open = new RouteInteraction(1, 1, MID, LATE, LATE,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Open", true, 123, MID, TARGET);
		NavigationDecision first = engine.observe(observation(plan(1), MID, false, false, false)
			.withRouteInteraction(open));
		engine.recordCommandResult(first, true, 1L);

		RouteInteraction climb = new RouteInteraction(1, 1, MID, LATE, LATE,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Climb-down", true, 123, MID, TARGET);
		NavigationDecision second = engine.observe(
			observation(plan(1), MID, false, false, false).withRouteInteraction(climb));

		assertEquals(NavigationDecision.Type.INTERACT, second.getType());
		assertEquals("Climb-down", second.getInteraction().getAction());
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Collections.singleton(TARGET), 0,
			NavigationRouteOptions.defaults(), "test"));
		return engine;
	}

	private static NavigationObservation observation(RoutePlan plan, WorldPoint player,
		boolean moving, boolean interactionFrontier, boolean replan)
	{
		return NavigationObservation.route(1, player, plan, moving, false, false,
			interactionFrontier, false, replan, null, "test-observation");
	}

	private static RoutePlan plan(long generation)
	{
		return new RoutePlan(1, generation, START, Collections.singleton(TARGET),
			Arrays.asList(START, MID, LATE, TARGET), Arrays.asList(START, MID, LATE, TARGET), true);
	}

	private static RouteInteraction interaction(RouteInteraction.Status status, boolean ready)
	{
		return new RouteInteraction(1, 1, MID, LATE, LATE,
			RouteInteraction.Kind.MINEABLE, status, "mine", ready);
	}

	private static void assertDecision(NavigationEngine engine, NavigationObservation observation,
		NavigationDecision.Type decision, NavigationPhase phase)
	{
		assertEquals(decision, engine.observe(observation).getType());
		assertEquals(phase, engine.snapshot().getPhase());
	}
}
