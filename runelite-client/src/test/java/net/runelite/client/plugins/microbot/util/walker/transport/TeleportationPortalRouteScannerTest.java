package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TeleportationPortalRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3081, 3475, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2206, 2858, 0);
	private static final int OBJECT_ID = 40474;

	@Test
	public void directObjectRemainsOwnedUntilDirectedLanding()
	{
		TeleportationPortalRouteScanner scanner =
			new TeleportationPortalRouteScanner();
		MutableScene scene = new MutableScene(portal(OBJECT_ID));
		RouteInteraction pending = scanner.scan(
			plan(RouteEdge.Kind.TELEPORTATION_PORTAL), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.TELEPORTATION_PORTAL, pending.getKind());
		assertTrue(pending.isReady());

		scene.current = null;
		RouteInteraction travelling = scanner.observePending(pending, ORIGIN, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(
			travelling, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void changedCatalogIdentityIsUnavailableAndDistantObjectIsNotReady()
	{
		TeleportationPortalRouteScanner scanner =
			new TeleportationPortalRouteScanner();
		MutableScene scene = new MutableScene(portal(OBJECT_ID));
		RouteInteraction pending = scanner.scan(
			plan(RouteEdge.Kind.TELEPORTATION_PORTAL), 0, 1,
			ORIGIN.dx(-20), scene, 13);
		assertFalse(pending.isReady());

		scene.current = portal(OBJECT_ID + 1);
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new TeleportationPortalRouteScanner().scan(
			plan(RouteEdge.Kind.TRANSPORT), 0, 1, ORIGIN,
			new MutableScene(portal(OBJECT_ID)), 13));
	}

	private static TeleportationPortal portal(int objectId)
	{
		return new TeleportationPortal(null, ORIGIN, objectId, "Enter",
			ORIGIN, DESTINATION);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements TeleportationPortalScene
	{
		private TeleportationPortal current;

		private MutableScene(TeleportationPortal current)
		{
			this.current = current;
		}

		@Override
		public TeleportationPortal find(PlannedEdge edge)
		{
			return current;
		}

		@Override
		public TeleportationPortal observe(PlannedEdge edge, int catalogObjectId)
		{
			return current;
		}
	}
}
