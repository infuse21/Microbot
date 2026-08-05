package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NpcTransportRouteScannerTest
{
	private static final WorldPoint A = new WorldPoint(2503, 3193, 0);
	private static final WorldPoint NPC_TILE = new WorldPoint(2504, 3193, 0);
	private static final WorldPoint B = new WorldPoint(2515, 3159, 0);

	@Test
	public void selectsExactPublishedNpcTransport()
	{
		RouteInteraction interaction = new NpcTransportRouteScanner().scan(
			plan(RouteEdge.Kind.NPC_TRANSPORT), 0, 1, A, edge -> transport(), 13);

		assertEquals(RouteInteraction.Kind.NPC_TRANSPORT, interaction.getKind());
		assertEquals("Follow", interaction.getAction());
		assertEquals(4968, interaction.getObjectId());
		assertTrue(interaction.isReady());
	}

	@Test
	public void landingClearsAndNpcUnloadPreservesPendingWait()
	{
		NpcTransportRouteScanner scanner = new NpcTransportRouteScanner();
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.NPC_TRANSPORT),
			0, 1, A, edge -> transport(), 13);

		RouteInteraction unloaded = scanner.observePending(pending, A, edge -> null, 13);
		RouteInteraction landed = scanner.observePending(pending, B, edge -> null, 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, unloaded.getStatus());
		assertTrue(unloaded.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
		assertFalse(landed.isReady());
	}

	@Test
	public void unsupportedEdgeIsIgnored()
	{
		assertNull(new NpcTransportRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, A, edge -> transport(), 13));
	}

	private static NpcTransport transport()
	{
		return new NpcTransport(A, B, TransportType.NPC, 4968,
			"Elkoy", "Follow", NPC_TILE);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, A, Collections.singleton(B), Arrays.asList(A, B),
			Arrays.asList(A, B), true,
			Collections.singletonList(new RouteEdge(0, A, B, kind)));
	}
}
