package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;
import net.runelite.client.plugins.microbot.util.poh.data.MountedGlory;
import net.runelite.client.plugins.microbot.util.poh.data.MountedMythical;
import net.runelite.client.plugins.microbot.util.poh.data.MountedDigsite;
import net.runelite.client.plugins.microbot.util.poh.data.MountedXerics;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PohChamberOwnershipTest
{
	@Test
	public void allFortyChamberDestinationsPublishPortalOwnershipOnly()
	{
		WorldPoint house = new WorldPoint(1859, 7051, 0);
		assertEquals(40, PohPortal.values().length);
		for (PohPortal portal : PohPortal.values())
		{
			PohTransport row = new PohTransport(house, portal);
			assertEquals(RouteEdge.Kind.TELEPORTATION_PORTAL,
				PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)));
			assertEquals(portal.getObjectIds()[0].intValue(), row.getObjectId());
			assertEquals(portal.getAction(), row.getAction());
		}
		PohTeleport facility = mock(PohTeleport.class);
		when(facility.getDestination()).thenReturn(new WorldPoint(3200, 3200, 0));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Set.of(new PohTransport(house, facility))));
		Transport untyped = new Transport(house, new WorldPoint(3200, 3200, 0),
			"PoHPortal -> Varrock", TransportType.POH, true, 4);
		assertEquals(RouteEdge.Kind.TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Set.of(untyped)));
	}

	@Test
	public void allMountedRoutesPublishPortalOwnership()
	{
		WorldPoint house = new WorldPoint(1859, 7051, 0);
		for (MountedGlory glory : MountedGlory.values())
		{
			PohTransport row = new PohTransport(house, glory);
			assertEquals(13523, row.getObjectId());
			assertEquals(glory.getDestinationName(), row.getAction());
			assertEquals(RouteEdge.Kind.TELEPORTATION_PORTAL,
				PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)));
		}
		PohTransport cape = new PohTransport(house, MountedMythical.MYTHS_GUILD);
		assertEquals(31986, cape.getObjectId());
		assertEquals("Teleport", cape.getAction());
		assertEquals(RouteEdge.Kind.TELEPORTATION_PORTAL,
			PathfinderRouteCalculation.classifyTransportEdge(Set.of(cape)));
		for (MountedDigsite menu : MountedDigsite.values())
		{
			PohTransport row = new PohTransport(house, menu);
			assertEquals(MountedDigsite.IDS[0].intValue(), row.getObjectId());
			assertEquals(menu.getDestinationName(), row.getAction());
			assertEquals(RouteEdge.Kind.TELEPORTATION_PORTAL,
				PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)));
		}
		for (MountedXerics menu : MountedXerics.values())
		{
			PohTransport row = new PohTransport(house, menu);
			assertEquals(MountedXerics.IDS[0].intValue(), row.getObjectId());
			assertEquals(menu.getDestinationName(), row.getAction());
			assertEquals(RouteEdge.Kind.TELEPORTATION_PORTAL,
				PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)));
		}
	}

	@Test
	public void chambersApproachTheirSceneObjectAndWaitForTheSelectedLanding()
	{
		WorldPoint anchor = new WorldPoint(1859, 7051, 0);
		WorldPoint room = new WorldPoint(1900, 7100, 0);
		for (PohPortal portal : PohPortal.values())
		{
			WorldPoint destination = portal.getDestination();
			RoutePlan plan = new RoutePlan(1, 1, anchor, Set.of(destination),
				java.util.List.of(anchor, destination), java.util.List.of(anchor, destination), true,
				java.util.List.of(new RouteEdge(0, anchor, destination, RouteEdge.Kind.TELEPORTATION_PORTAL)));
			net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal object =
				new net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal(
					null, room, portal.getObjectIds()[0], portal.getAction(), anchor, destination);
			net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalRouteScanner scanner =
				new net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalRouteScanner();
			net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalScene scene =
				mock(net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalScene.class);
			when(scene.find(any())).thenReturn(object);
			RouteInteraction approach = scanner.scan(plan, 0, 1, anchor, scene, 13);
			assertFalse(approach.isReady());
			RouteInteraction pending = scanner.scan(plan, 0, 1, room, scene, 13);
			assertTrue(pending.isReady());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, room, scene, 13).getStatus());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, destination.dx(5), scene, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, destination, scene, 13).getStatus());
		}
	}
}
