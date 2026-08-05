package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NavigationEngineRuntimeTest
{
	@After
	public void resetRuntime()
	{
		NavigationEngineRuntime.resetForTesting();
	}

	@Test
	public void shadowRuntimePublishesSnapshotWithoutActionDependency()
	{
		WorldPoint start = new WorldPoint(3200, 3200, 0);
		WorldPoint target = new WorldPoint(3201, 3200, 0);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(7,
			Collections.singleton(target), 0, NavigationRouteOptions.defaults(), "test"));
		RoutePlan plan = new RoutePlan(7, 1, start, Collections.singleton(target),
			Collections.singletonList(start), Collections.singletonList(start), false);

		NavigationDecision decision = NavigationEngineRuntime.observe(NavigationObservation.route(
			1, start, plan, false, false, false, false, false, false, null, "test"));

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, decision.getType());
		assertNotNull(NavigationEngineRuntime.getSnapshot());
		assertEquals(7, NavigationEngineRuntime.getSnapshot().getRequestId());
	}
}
