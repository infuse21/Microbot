package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SimpleTeleportRouteScannerTest
{
	private static final WorldPoint A = new WorldPoint(3000, 3000, 0);
	private static final WorldPoint B = new WorldPoint(3213, 3424, 0);

	@Test
	public void selectsPublishedTeleportAndMarksItReadyAnywhere()
	{
		RouteInteraction interaction = new SimpleTeleportRouteScanner().scan(
			plan(RouteEdge.Kind.SIMPLE_TELEPORT), 0, 1, A, edge -> teleport());

		assertEquals(RouteInteraction.Kind.SIMPLE_TELEPORT, interaction.getKind());
		assertEquals("Varrock Teleport", interaction.getAction());
		assertTrue(interaction.isReady());
	}

	@Test
	public void landingClearsPendingTeleport()
	{
		RouteInteraction pending = new SimpleTeleportRouteScanner().scan(
			plan(RouteEdge.Kind.SIMPLE_TELEPORT), 0, 1, A, edge -> teleport());
		RouteInteraction observed = new SimpleTeleportRouteScanner().observePending(
			pending, B, edge -> teleport());

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
	}

	@Test
	public void consumedItemCatalogDisappearanceDoesNotPreemptLandingWait()
	{
		RouteInteraction pending = new SimpleTeleportRouteScanner().scan(
			plan(RouteEdge.Kind.SIMPLE_TELEPORT), 0, 1, A, edge -> teleport());
		RouteInteraction observed = new SimpleTeleportRouteScanner().observePending(
			pending, A, edge -> null);

		assertEquals(RouteInteraction.Status.AVAILABLE, observed.getStatus());
		assertTrue(observed.isReady());
	}

	@Test
	public void unsupportedEdgeIsIgnored()
	{
		assertNull(new SimpleTeleportRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, A, edge -> teleport()));
	}

	private static SimpleTeleport teleport()
	{
		return new SimpleTeleport(A, B, TransportType.TELEPORTATION_SPELL,
			"Varrock Teleport");
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, A, Collections.singleton(B), Arrays.asList(A, B),
			Arrays.asList(A, B), true,
			Collections.singletonList(new RouteEdge(0, A, B, kind)));
	}
}
