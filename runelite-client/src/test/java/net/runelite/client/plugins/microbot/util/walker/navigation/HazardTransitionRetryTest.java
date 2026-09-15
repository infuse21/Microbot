package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class HazardTransitionRetryTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2762, 2989, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2760, 9389, 0);

	@Test
	public void stochasticTransitionsRetryFiveTimesThenRequestBoundedReplan()
	{
		for (int objectId : new int[]{2234, 2236, 3922, 3925, 16544})
		{
			assertBoundedRetry(objectId);
		}
	}

	private static void assertBoundedRetry(int objectId)
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Collections.singleton(DESTINATION), 0,
			NavigationRouteOptions.defaults(), "stochastic-well"));
		RoutePlan plan = new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION,
				RouteEdge.Kind.CATALOG_TRANSITION)));
		RouteInteraction transition = new RouteInteraction(1, 0, ORIGIN, DESTINATION, ORIGIN,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Search", true, objectId, ORIGIN, DESTINATION);

		long observedAt = 1L;
		NavigationDecision command = engine.observe(observation(observedAt, plan, transition));
		assertEquals("objectId=" + objectId, NavigationDecision.Type.INTERACT, command.getType());
		engine.recordCommandResult(command, true, observedAt);
		for (int attempt = 1; attempt < 5; attempt++)
		{
			observedAt += 6_000L;
			command = engine.observe(observation(observedAt, plan, transition));
			assertEquals("objectId=" + objectId, NavigationDecision.Type.INTERACT, command.getType());
			engine.recordCommandResult(command, true, observedAt);
		}
		observedAt += 6_000L;
		NavigationDecision bounded = engine.observe(observation(observedAt, plan, transition));
		assertEquals("objectId=" + objectId, NavigationDecision.Type.REQUEST_REPLAN,
			bounded.getType());
		assertEquals("stochastic-transition-attempts-exhausted", bounded.getReason());
	}

	private static NavigationObservation observation(long observedAt, RoutePlan plan,
		RouteInteraction interaction)
	{
		return NavigationObservation.route(observedAt, ORIGIN, plan, false, false, false,
			false, false, false, "stochastic-well")
			.withRouteInteraction(interaction);
	}
}
