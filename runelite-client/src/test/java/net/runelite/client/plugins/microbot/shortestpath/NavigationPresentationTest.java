package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngine;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRequest;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRouteOptions;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationSnapshot;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NavigationPresentationTest
{
	private static final WorldPoint START = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TARGET = new WorldPoint(3201, 3200, 0);

	@Test
	public void noSessionAndTerminalSessionRenderNoPlan()
	{
		RoutePlan plan = plan(1);
		assertNull(NavigationPresentation.activePlan(null, plan));

		NavigationEngine engine = new NavigationEngine();
		engine.start(request(1));
		NavigationSnapshot terminal = engine.cancel("test-cancel");
		assertNull(NavigationPresentation.activePlan(terminal, plan));
	}

	@Test
	public void onlyMatchingPublishedPlanCanBackPlanningSnapshot()
	{
		NavigationEngine engine = new NavigationEngine();
		NavigationSnapshot planning = engine.start(request(1));
		RoutePlan matching = plan(1);

		assertSame(matching, NavigationPresentation.activePlan(planning, matching));
		assertNull(NavigationPresentation.activePlan(planning, plan(2)));
	}

	@Test
	public void markerTargetExistsOnlyForOneActiveDestination()
	{
		NavigationEngine engine = new NavigationEngine();
		NavigationSnapshot single = engine.start(request(1));
		assertEquals(TARGET, NavigationPresentation.target(single));

		NavigationRequest multiple = new NavigationRequest(2, Set.of(TARGET, START), 0,
			NavigationRouteOptions.defaults(), "test");
		assertNull(NavigationPresentation.target(engine.start(multiple)));
		assertNull(NavigationPresentation.target(engine.cancel("test-cancel")));
	}

	@Test
	public void logoutAndWorldHopInvalidateUiSession()
	{
		assertTrue(NavigationPresentation.invalidatesSession(GameState.LOGIN_SCREEN));
		assertTrue(NavigationPresentation.invalidatesSession(GameState.HOPPING));
		assertTrue(NavigationPresentation.invalidatesSession(GameState.CONNECTION_LOST));
		assertFalse(NavigationPresentation.invalidatesSession(GameState.LOGGED_IN));
		assertFalse(NavigationPresentation.invalidatesSession(GameState.LOADING));
	}

	private static NavigationRequest request(long requestId)
	{
		return new NavigationRequest(requestId, Set.of(TARGET), 0,
			NavigationRouteOptions.defaults(), "test");
	}

	private static RoutePlan plan(long requestId)
	{
		return new RoutePlan(requestId, 1, START, Set.of(TARGET),
			List.of(START, TARGET), List.of(START, TARGET), true);
	}
}
