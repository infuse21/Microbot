package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngine;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRequest;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRouteOptions;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationObservation;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationDecision;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResourceAreaGatePolicyTest
{
	@Test
	public void engineOwnsOpenConfirmationAndLandingWithoutAnotherGateClick()
	{
		WorldPoint from = ResourceAreaGatePolicy.OUTSIDE;
		WorldPoint to = ResourceAreaGatePolicy.INSIDE;
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Set.of(to), 0, NavigationRouteOptions.defaults(), "resource-gate-test"));
		RoutePlan plan = new RoutePlan(1, 1, from, Set.of(to), List.of(from, to), List.of(from, to),
			true, List.of(new RouteEdge(0, from, to, RouteEdge.Kind.CATALOG_TRANSITION)));
		RouteInteraction open = new RouteInteraction(1, 0, from, to, from,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Open", true, ResourceAreaGatePolicy.GATE, from, to);
		RouteInteraction confirm = new RouteInteraction(1, 0, from, to, from,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			ResourceAreaGatePolicy.CONFIRM, true, ResourceAreaGatePolicy.GATE, from, to);
		NavigationDecision first = engine.observe(observation(1, from, plan, open));
		assertEquals(NavigationDecision.Type.INTERACT, first.getType());
		engine.recordCommandResult(first, true, 1);
		NavigationDecision second = engine.observe(observation(601, from, plan, confirm));
		assertEquals(NavigationDecision.Type.INTERACT, second.getType());
		assertEquals(ResourceAreaGatePolicy.CONFIRM, second.getInteraction().getAction());
		engine.recordCommandResult(second, true, 601);
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(1201, from, plan,
			confirm.withStatus(RouteInteraction.Status.UNAVAILABLE, false))).getType());
		RouteInteraction landed = new CatalogTransitionRouteScanner().observePending(confirm, to, edge -> null, 6);
		engine.observe(observation(1801, to, plan, landed));
		assertEquals(NavigationDecision.Type.COMPLETE, engine.observe(observation(2401, to, plan, null)).getType());
	}

	private static NavigationObservation observation(long time, WorldPoint player, RoutePlan plan, RouteInteraction interaction)
	{
		return NavigationObservation.route(time, player, plan, false, false, false, false, false, false,
			null, "resource-gate-test").withRouteInteraction(interaction);
	}

	@Test
	public void confirmationRequiresExactFareAndCompleteYesNoMenu()
	{
		for (int fare : new int[]{7500, 6000, 3750})
		{
			assertEquals(0, ResourceAreaGatePolicy.confirmationIndex(fare,
				ResourceAreaGatePolicy.prompt(fare), List.of("Yes", "No")));
			assertEquals(1, ResourceAreaGatePolicy.confirmationIndex(fare,
				ResourceAreaGatePolicy.prompt(fare), List.of("No", "Yes")));
			assertEquals(-1, ResourceAreaGatePolicy.confirmationIndex(fare,
				"Pay 1 coins to enter?", List.of("Yes", "No")));
			assertEquals(-1, ResourceAreaGatePolicy.confirmationIndex(fare,
				ResourceAreaGatePolicy.prompt(fare), List.of("Yes", "Yes")));
		}
		assertEquals(-1, ResourceAreaGatePolicy.confirmationIndex(0, "Pay 0 coins to enter?", List.of("Yes", "No")));
	}

	@Test
	public void onlyAnOwnedOpenCanAdvanceToPaymentAndPaymentNeverReopens()
	{
		assertEquals("Open", next(7500, true, false, null));
		assertNull(next(7499, true, false, null));
		assertNull(next(7500, false, true, "Open"));
		assertNull(next(7500, true, true, null));
		assertEquals(ResourceAreaGatePolicy.CONFIRM, next(7500, true, true, "Open"));
		assertNull(next(0, true, false, ResourceAreaGatePolicy.CONFIRM));
		assertNull(next(7500, true, true, ResourceAreaGatePolicy.CONFIRM));
		assertEquals("Open", ResourceAreaGatePolicy.nextAction(0, 0, true, false, "", List.of(), null));
	}

	@Test
	public void onlyExactOppositeTileAcknowledgesEitherDirection()
	{
		for (boolean entry : new boolean[]{true, false})
		{
			WorldPoint from = entry ? ResourceAreaGatePolicy.OUTSIDE : ResourceAreaGatePolicy.INSIDE;
			WorldPoint to = entry ? ResourceAreaGatePolicy.INSIDE : ResourceAreaGatePolicy.OUTSIDE;
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				ResourceAreaGatePolicy.CONFIRM, true, ResourceAreaGatePolicy.GATE, from, to);
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertEquals(RouteInteraction.Status.UNAVAILABLE, scanner.observePending(pending, from, edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE, scanner.observePending(pending,
				new WorldPoint(to.getX() + 1, to.getY(), 0), edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED, scanner.observePending(pending, to, edge -> null, 6).getStatus());
		}
	}

	private static String next(int carried, boolean ready, boolean dialogue, String pending)
	{
		return ResourceAreaGatePolicy.nextAction(7500, carried, ready, dialogue,
			"Pay 7500 coins to enter?", List.of("Yes", "No"), pending);
	}
}
