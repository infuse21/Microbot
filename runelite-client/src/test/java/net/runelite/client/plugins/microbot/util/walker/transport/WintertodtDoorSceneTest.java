package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WintertodtDoorSceneTest
{
	private static final WorldPoint DOOR_ANCHOR = new WorldPoint(1627, 3964, 0);

	@Test
	public void everyApproachResolvesTheSharedMultiTileDoorAnchor()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(CatalogTransitionPolicy::isWintertodtDoor)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(16, rows.size());
		for (Transport row : rows)
		{
			verifyDispatch(row);
		}
	}

	private static void verifyDispatch(Transport row)
	{
		net.runelite.api.Client client = mock(net.runelite.api.Client.class);
		net.runelite.api.Player actor = mock(net.runelite.api.Player.class);
		net.runelite.api.WorldView world = mock(net.runelite.api.WorldView.class);
		when(client.getLocalPlayer()).thenReturn(actor);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(actor.getWorldView()).thenReturn(world);

		net.runelite.client.callback.ClientThread thread =
			mock(net.runelite.client.callback.ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			java.util.Optional.ofNullable(((java.util.concurrent.Callable<?>) call.getArgument(0)).call()));
		when(thread.invoke(any(java.util.function.Supplier.class))).thenAnswer(call ->
			((java.util.function.Supplier<?>) call.getArgument(0)).get());

		net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel object =
			mock(net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel.class);
		when(object.getId()).thenReturn(29322);
		when(object.getWorldLocation()).thenReturn(DOOR_ANCHOR);
		when(object.getSizeX()).thenReturn(7);
		when(object.getSizeY()).thenReturn(3);
		when(object.getWorldView()).thenReturn(world);
		net.runelite.api.ObjectComposition definition = mock(net.runelite.api.ObjectComposition.class);
		when(object.getObjectComposition()).thenReturn(definition);
		when(definition.getName()).thenReturn("Doors of Dinh");
		when(definition.getActions()).thenReturn(new String[]{"Enter"});
		when(object.click("Enter")).thenReturn(true);

		net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache cache =
			mock(net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		when(cache.getStream()).thenAnswer(call -> List.of(object).stream());

		try (org.mockito.MockedStatic<net.runelite.client.plugins.microbot.Microbot> microbot =
			mockStatic(net.runelite.client.plugins.microbot.Microbot.class);
			org.mockito.MockedStatic<net.runelite.client.plugins.microbot.util.walker.Rs2PathApi> path =
				mockStatic(net.runelite.client.plugins.microbot.util.walker.Rs2PathApi.class);
			org.mockito.MockedStatic<net.runelite.client.plugins.microbot.util.player.Rs2Player> player =
				mockStatic(net.runelite.client.plugins.microbot.util.player.Rs2Player.class))
		{
			microbot.when(net.runelite.client.plugins.microbot.Microbot::getClient).thenReturn(client);
			microbot.when(net.runelite.client.plugins.microbot.Microbot::getClientThread).thenReturn(thread);
			microbot.when(net.runelite.client.plugins.microbot.Microbot::getRs2TileObjectCache)
				.thenReturn(cache);
			path.when(net.runelite.client.plugins.microbot.util.walker.Rs2PathApi::getTransports)
				.thenReturn(Map.of(row.getOrigin(), Set.of(row)));
			player.when(net.runelite.client.plugins.microbot.util.player.Rs2Player::getWorldLocation)
				.thenReturn(row.getOrigin());

			PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
				Rs2CatalogTransitionScene.dispatch(edge, "Enter", 29322));
			verify(object, times(1)).click("Enter");
		}
	}
}
