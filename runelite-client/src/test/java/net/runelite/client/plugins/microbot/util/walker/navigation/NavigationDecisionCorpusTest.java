package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/** Stable headless decision traces for the sole NavigationEngine executor. */
public class NavigationDecisionCorpusTest
{
	private static final WorldPoint A = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint B = new WorldPoint(3201, 3200, 0);
	private static final WorldPoint C = new WorldPoint(3202, 3200, 0);

	@Test
	public void ordinaryRouteTraceStaysStable()
	{
		NavigationEngine engine = engine();

		assertStep(engine, observation(A, false, false, false),
			NavigationDecision.Type.CLICK_TILE, NavigationPhase.FOLLOWING_ROUTE);
		assertStep(engine, observation(B, true, false, false),
			NavigationDecision.Type.WAIT, NavigationPhase.FOLLOWING_ROUTE);
		assertStep(engine, NavigationObservation.terminal(
			NavigationObservation.TerminalSignal.ARRIVED, "arrived"),
			NavigationDecision.Type.COMPLETE, NavigationPhase.ARRIVED);
	}

	@Test
	public void interactionAndUnownedMovementRecoveryTraceStaysStable()
	{
		NavigationEngine engine = engine();

		assertStep(engine, observation(A, false, true, false),
			NavigationDecision.Type.INTERACT, NavigationPhase.PERFORMING_INTERACTION);
		assertStep(engine, observation(A, true, false, true),
			NavigationDecision.Type.REQUEST_REPLAN, NavigationPhase.REPLANNING);
		assertEquals(1, engine.snapshot().getRecoveryAttempts());
		assertStep(engine, observation(A, false, false, true),
			NavigationDecision.Type.REQUEST_REPLAN, NavigationPhase.REPLANNING);
		assertEquals(2, engine.snapshot().getRecoveryAttempts());
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(11, Collections.singleton(C), 0,
			NavigationRouteOptions.defaults(), "decision-corpus"));
		return engine;
	}

	private static NavigationObservation observation(WorldPoint player, boolean moving,
		boolean interaction, boolean replan)
	{
		return NavigationObservation.route(1, player, plan(), moving, false, false,
			interaction, false, replan, "corpus-step");
	}

	private static RoutePlan plan()
	{
		return new RoutePlan(11, 1, A, Collections.singleton(C), Arrays.asList(A, B, C),
			Arrays.asList(A, B, C), true);
	}

	private static void assertStep(NavigationEngine engine, NavigationObservation observation,
		NavigationDecision.Type decision, NavigationPhase phase)
	{
		assertEquals(decision, engine.observe(observation).getType());
		assertEquals(phase, engine.snapshot().getPhase());
	}
}
