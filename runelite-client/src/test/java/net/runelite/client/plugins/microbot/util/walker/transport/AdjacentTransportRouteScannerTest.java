package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AdjacentTransportRouteScannerTest
{
	private static final WorldPoint A = new WorldPoint(10, 10, 0);
	private static final WorldPoint B = new WorldPoint(11, 10, 0);
	private static final WorldPoint C = new WorldPoint(12, 10, 0);

	@Test
	public void selectsOnlyPublishedAdjacentTransportEdges()
	{
		RoutePlan plan = plan(RouteEdge.Kind.WALK, RouteEdge.Kind.ADJACENT_TRANSPORT);
		AdjacentTransportRouteScanner scanner = new AdjacentTransportRouteScanner();
		AdjacentTransportScene scene = edge -> edge.from().equals(B)
			? new AdjacentTransport(null, B, 123, "Climb-over") : null;

		RouteInteraction interaction = scanner.scan(plan, 0, 2, A, scene, 13);

		assertEquals(1, interaction.getRawEdgeIndex());
		assertEquals(RouteInteraction.Kind.ADJACENT_TRANSPORT, interaction.getKind());
		assertEquals(123, interaction.getObjectId());
		assertTrue(interaction.isReady());
	}

	@Test
	public void missingPendingObjectIsUnavailableRatherThanCleared()
	{
		RouteInteraction pending = new RouteInteraction(1, 1, B, C, B,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Climb-over", true, 123);

		RouteInteraction observed = new AdjacentTransportRouteScanner().observePending(
			pending, B, edge -> null, 13);

		assertEquals(RouteInteraction.Status.UNAVAILABLE, observed.getStatus());
		assertFalse(observed.isReady());
	}

	@Test
	public void missingOpenedCatalogDoorIsObservedAsCleared()
	{
		RouteInteraction pending = new RouteInteraction(1, 1, B, C, B,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Open", true, 123);

		RouteInteraction observed = new AdjacentTransportRouteScanner().observePending(
			pending, B, edge -> null, 13);

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
	}

	@Test
	public void destinationSideRetiresTransportEvenWhenObjectStillExists()
	{
		WorldPoint parallelRouteFrom = new WorldPoint(10, 11, 0);
		WorldPoint parallelRouteTo = new WorldPoint(11, 11, 0);
		RouteInteraction pending = new RouteInteraction(1, 0,
			parallelRouteFrom, parallelRouteTo, B,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Open", true, 123, A, B);
		AdjacentTransportScene stillOpenable = edge ->
			new AdjacentTransport(null, B, 123, "Open", A, B);

		RouteInteraction observed = new AdjacentTransportRouteScanner().observePending(
			pending, B, stillOpenable, 13);

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
		assertFalse(observed.isReady());
	}

	@Test
	public void originSideDoesNotRetireTransportWhileObjectStillExists()
	{
		RouteInteraction pending = new RouteInteraction(1, 0, A, B, B,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Open", true, 123, A, B);
		AdjacentTransportScene stillOpenable = edge ->
			new AdjacentTransport(null, B, 123, "Open", A, B);

		RouteInteraction observed = new AdjacentTransportRouteScanner().observePending(
			pending, A, stillOpenable, 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, observed.getStatus());
		assertTrue(observed.isReady());
	}

	@Test
	public void twoTileDoorRetiresOnlyOnDirectedDestinationSide()
	{
		WorldPoint origin = new WorldPoint(20, 20, 0);
		WorldPoint destination = new WorldPoint(20, 22, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, origin, destination, origin,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Open", true, 1967, origin, destination);
		AdjacentTransportScene stillOpenable = edge ->
			new AdjacentTransport(null, origin, 1967, "Open", origin, destination);

		RouteInteraction before = new AdjacentTransportRouteScanner().observePending(
			pending, new WorldPoint(20, 21, 0), stillOpenable, 13);
		RouteInteraction after = new AdjacentTransportRouteScanner().observePending(
			pending, destination, stillOpenable, 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, before.getStatus());
		assertEquals(RouteInteraction.Status.CLEARED, after.getStatus());
	}

	@Test
	public void ignoresUnsupportedTransportEdges()
	{
		RoutePlan plan = plan(RouteEdge.Kind.WALK, RouteEdge.Kind.TRANSPORT);
		RouteInteraction interaction = new AdjacentTransportRouteScanner().scan(plan, 0, 2, A,
			edge -> new AdjacentTransport(null, B, 123, "Climb-over"), 13);

		assertNull(interaction);
	}

	private static RoutePlan plan(RouteEdge.Kind first, RouteEdge.Kind second)
	{
		return new RoutePlan(1, 1, A, Collections.singleton(C), Arrays.asList(A, B, C),
			Arrays.asList(A, C), true, Arrays.asList(
				new RouteEdge(0, A, B, first), new RouteEdge(1, B, C, second)));
	}
}
