package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;
import net.runelite.client.plugins.microbot.util.poh.data.MountedGlory;
import net.runelite.client.plugins.microbot.util.poh.data.MountedMythical;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PohChamberSceneTest
{
	@Test
	public void directHouseFacilitiesRequireHouseExactObjectAndDestinationAction()
	{
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			java.util.Optional.ofNullable(((java.util.concurrent.Callable<?>) call.getArgument(0)).call()));
		when(thread.invoke(any(java.util.function.Supplier.class))).thenAnswer(call ->
			((java.util.function.Supplier<?>) call.getArgument(0)).get());
		net.runelite.api.Client client = mock(net.runelite.api.Client.class);
		net.runelite.api.Player actor = mock(net.runelite.api.Player.class);
		net.runelite.api.WorldView world = mock(net.runelite.api.WorldView.class);
		net.runelite.api.WorldView otherWorld = mock(net.runelite.api.WorldView.class);
		when(client.getLocalPlayer()).thenReturn(actor);
		when(actor.getWorldView()).thenReturn(world);
		when(world.getId()).thenReturn(1);
		when(otherWorld.getId()).thenReturn(2);
		Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		when(object.getWorldView()).thenReturn(world);
		ObjectComposition definition = mock(ObjectComposition.class);
		when(object.getObjectComposition()).thenReturn(definition);
		when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
		WorldPoint anchor = new WorldPoint(1859, 7051, 0);
		WorldPoint room = new WorldPoint(1900, 7100, 0);
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
			MockedStatic<PohTeleports> house = mockStatic(PohTeleports.class);
			MockedStatic<Rs2SceneLocation> locations = mockStatic(Rs2SceneLocation.class))
		{
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
			locations.when(() -> Rs2SceneLocation.templateLocation(object)).thenReturn(room);
			List<PohTeleport> facilities = new java.util.ArrayList<>(java.util.Arrays.asList(PohPortal.values()));
			facilities.addAll(java.util.Arrays.asList(MountedGlory.values()));
			facilities.addAll(java.util.Arrays.asList(MountedMythical.values()));
			assertEquals(45, facilities.size());
			for (PohTeleport portal : facilities)
			{
				PohTransport row = new PohTransport(anchor, portal);
				PlannedEdge edge = new PlannedEdge(anchor, row.getDestination());
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));
				when(definition.getActions()).thenReturn(new String[]{row.getAction(), "Remove"});
				when(object.click(row.getAction())).thenReturn(true);
				house.when(PohTeleports::isInHouse).thenReturn(true);
				Integer[] ids = portal instanceof PohPortal ? ((PohPortal) portal).getObjectIds()
					: new Integer[]{row.getObjectId()};
				for (int id : ids)
				{
					when(object.getId()).thenReturn(id);
					assertNotNull(portal.name(), new Rs2TeleportationPortalScene().find(edge));
					assertTrue(Rs2TeleportationPortalScene.interactObject(edge, row.getAction(), row.getObjectId()));
				}
				house.when(PohTeleports::isInHouse).thenReturn(false);
				assertNull(new Rs2TeleportationPortalScene().find(edge));
				house.when(PohTeleports::isInHouse).thenReturn(true);
				when(object.getWorldView()).thenReturn(otherWorld);
				assertNull(new Rs2TeleportationPortalScene().find(edge));
				when(object.getWorldView()).thenReturn(world);
				when(object.getId()).thenReturn(-1);
				assertNull(new Rs2TeleportationPortalScene().find(edge));
				when(definition.getId()).thenReturn(row.getObjectId());
				assertNotNull(new Rs2TeleportationPortalScene().find(edge));
				when(definition.getId()).thenReturn(0);
				if (portal == PohPortal.GRAND_EXCHANGE)
				{
					when(object.getId()).thenReturn(13622);
					when(definition.getId()).thenReturn(33098);
					assertTrue(Rs2TeleportationPortalScene.interactObject(edge, "Grand Exchange", row.getObjectId()));
					when(definition.getId()).thenReturn(0);
				}
				when(object.getId()).thenReturn(row.getObjectId());
				when(definition.getActions()).thenReturn(new String[]{"Toggle", "Remove"});
				assertNull(new Rs2TeleportationPortalScene().find(edge));
			}
		}
	}
}
