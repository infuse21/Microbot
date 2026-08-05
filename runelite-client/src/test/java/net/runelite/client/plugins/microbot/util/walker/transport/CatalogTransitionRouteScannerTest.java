package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CatalogTransitionRouteScannerTest
{
	private static final WorldPoint A = new WorldPoint(10, 10, 0);
	private static final WorldPoint B = new WorldPoint(10, 10, 1);

	@Test
	public void selectsOnlyPublishedCatalogTransitionEdges()
	{
		RouteInteraction interaction = new CatalogTransitionRouteScanner().scan(
			plan(RouteEdge.Kind.CATALOG_TRANSITION), 0, 1, A,
			edge -> transition("Climb-up"), 13);

		assertEquals(RouteInteraction.Kind.CATALOG_TRANSITION, interaction.getKind());
		assertEquals("Climb-up", interaction.getAction());
		assertTrue(interaction.isReady());
	}

	@Test
	public void destinationArrivalClearsPersistentTransitionObject()
	{
		RouteInteraction pending = interaction("Climb-up");
		RouteInteraction observed = new CatalogTransitionRouteScanner().observePending(
			pending, B, edge -> transition("Climb-up"), 13);

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
		assertFalse(observed.isReady());
	}

	@Test
	public void closedTrapdoorCanAdvanceFromOpenToClimbStage()
	{
		RouteInteraction observed = new CatalogTransitionRouteScanner().observePending(
			interaction("Open"), A, edge -> transition("Climb-down"), 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, observed.getStatus());
		assertEquals("Climb-down", observed.getAction());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new CatalogTransitionRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, A, edge -> transition("Climb-up"), 13));
	}

	private static RouteInteraction interaction(String action)
	{
		return new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			action, true, 123, A, B);
	}

	private static CatalogTransition transition(String action)
	{
		return new CatalogTransition(null, A, 123, action, "Climb-up", A, B);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, A, Collections.singleton(B), Arrays.asList(A, B),
			Arrays.asList(A, B), true,
			Collections.singletonList(new RouteEdge(0, A, B, kind)));
	}
}
