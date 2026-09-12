package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PohSpiritTreeSceneTest
{
	@Test
	public void allHouseVariantsResolveTheirRoomTileAndTreeAction()
	{
		WorldPoint anchor = new WorldPoint(1859, 7051, 0);
		WorldPoint room = new WorldPoint(1900, 7100, 0);
		WorldPoint target = new WorldPoint(2545, 3169, 0);
		Transport row = new Transport(anchor, target, "1: Tree Gnome Village",
			TransportType.SPIRIT_TREE, true, "Travel", "Spirit tree", 29227);
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call -> java.util.Optional.ofNullable(
			((java.util.concurrent.Callable<?>) call.getArgument(0)).call()));
		when(thread.invoke(any(java.util.function.Supplier.class))).thenAnswer(call ->
			((java.util.function.Supplier<?>) call.getArgument(0)).get());
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		WorldView world = mock(WorldView.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldView()).thenReturn(world);
		when(world.getId()).thenReturn(1);
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		when(object.getWorldView()).thenReturn(world);
		ObjectComposition composition = mock(ObjectComposition.class);
		when(object.getObjectComposition()).thenReturn(composition);
		Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<PohPanel> panel = mockStatic(PohPanel.class);
			MockedStatic<PohTeleports> house = mockStatic(PohTeleports.class);
			MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
			MockedStatic<Rs2SceneLocation> scene = mockStatic(Rs2SceneLocation.class);
			MockedStatic<net.runelite.client.plugins.microbot.util.player.Rs2Player> playerState =
				mockStatic(net.runelite.client.plugins.microbot.util.player.Rs2Player.class);
			MockedStatic<Rs2Widget> widgets = mockStatic(Rs2Widget.class))
		{
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
			panel.when(PohPanel::getExitPortalTile).thenReturn(anchor);
			house.when(PohTeleports::isInHouse).thenReturn(true);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));
			scene.when(() -> Rs2SceneLocation.templateLocation(object)).thenReturn(room);
			PlannedEdge edge = new PlannedEdge(anchor, target);
			for (int id : new int[]{29227, 40778, 44936, 29229, 40779, 27097})
			{
				String action = id == 29227 || id == 40778 || id == 44936 ? "Travel" : "Tree";
				when(object.getId()).thenReturn(id);
				when(composition.getActions()).thenReturn(new String[]{action, "Ring-configure"});
				when(object.click(action)).thenReturn(true);
				var tree = new Rs2SpiritTreeScene().find(edge);
				assertNotNull(tree);
				assertEquals(room, tree.getObjectTile());
				assertEquals(29227, tree.getObjectId());
				assertEquals(action, tree.getObjectAction());
				assertTrue(Rs2SpiritTreeScene.interactObject(edge, action, 29227));
				assertFalse(Rs2SpiritTreeScene.interactObject(edge, "Ring-configure", 29227));
			}
			when(object.getId()).thenReturn(-1);
			when(composition.getId()).thenReturn(29229);
			assertEquals("Tree", new Rs2SpiritTreeScene().find(edge).getObjectAction());
			WorldView foreign = mock(WorldView.class);
			when(foreign.getId()).thenReturn(2);
			when(object.getWorldView()).thenReturn(foreign);
			assertNull(new Rs2SpiritTreeScene().find(edge));
			when(object.getWorldView()).thenReturn(world);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			assertFalse(Rs2SpiritTreeScene.interactObject(edge, "Tree", 29227));
			assertFalse(Rs2SpiritTreeScene.selectDestination(edge, "Tree Gnome Village"));
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));
			assertFalse(Rs2SpiritTreeScene.selectDestination(edge, "Your house"));
			Transport inbound = new Transport(target, anchor, "C: Your house",
				TransportType.SPIRIT_TREE, true, "Travel", "Spirit Tree", 1293);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(target, Set.of(inbound)));
			PlannedEdge incoming = new PlannedEdge(target, anchor);
			Rs2SpiritTreeScene resolver = new Rs2SpiritTreeScene();
			assertFalse(resolver.hasLanded(incoming, anchor));
			assertFalse(resolver.hasLanded(incoming, room.dx(4)));
			assertFalse(resolver.hasLanded(incoming, new WorldPoint(room.getX(), room.getY(), 1)));
			assertTrue(resolver.hasLanded(incoming, room));
			house.when(PohTeleports::isInHouse).thenReturn(false);
			assertFalse(resolver.hasLanded(incoming, room));
			house.when(PohTeleports::isInHouse).thenReturn(true);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));
			Transport ringRow = new Transport(anchor, target, "BJR", TransportType.FAIRY_RING, true, 5);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(ringRow)));
			microbot.when(() -> Microbot.getVarbitValue(net.runelite.api.gameval.VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE))
				.thenReturn(1);
			for (int id : new int[]{29228, 29229, 40779, 27097})
			{
				String action = id == 29228 ? "Configure" : "Ring-configure";
				when(object.getId()).thenReturn(id);
				when(composition.getActions()).thenReturn(new String[]{action});
				when(object.click(action)).thenReturn(true);
				var ring = new Rs2FairyRingScene().find(edge);
				assertNotNull(ring);
				assertEquals(room, ring.getObjectTile());
				assertEquals(action, ring.getAction());
				assertTrue(Rs2FairyRingScene.interactObject(edge, action));
				assertFalse(Rs2FairyRingScene.interactObject(edge, "Tree"));
			}
			house.when(PohTeleports::isInHouse).thenReturn(false);
			assertNull(new Rs2FairyRingScene().find(edge));
			assertNull(new Rs2SpiritTreeScene().find(edge));
		}
	}
}
