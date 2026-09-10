package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2LiveScene;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrimhavenCreviceTest
{
	@Test
	public void bothPassagesAreMembersOnlyButDoNotRequireACombatAssignment()
	{
		List<Transport> rows = rows();
		assertEquals(2, rows.size());
		assertEquals(Set.of(new WorldPoint(2684, 9436, 0), new WorldPoint(2697, 9436, 0)),
			rows.stream().map(Transport::getOrigin).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertTrue(row.isMembers());
			assertTrue(row.getVarplayers().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertEquals(0, row.getCurrencyAmount());
			assertEquals(0, row.getDuration());
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(row.getOrigin(), row.getOrigin(), "",
				TransportType.TRANSPORT, true, "Enter", "Crevice", 30198)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(row.getOrigin(), row.getDestination(), "",
				TransportType.TRANSPORT, false, "Enter", "Crevice", 30198)));
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				"Enter", true, 30198, row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE, scanner.observePending(pending,
				new WorldPoint(row.getDestination().getX() + 1, 9436, 0), edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 6).getStatus());
		}
	}

	@Test
	public void cacheAnchorsResolveInBothDirectionsWithoutTaskStateOrNpcDialogue()
	{
		for (Transport row : rows())
		{
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
			ObjectComposition composition = mock(ObjectComposition.class);
			when(object.getId()).thenReturn(30198);
			int objectX = row.getOrigin().getX() == 2684 ? 2685 : 2696;
			when(object.getWorldLocation()).thenReturn(new WorldPoint(objectX, 9436, 0));
			when(object.getObjectComposition()).thenReturn(composition);
			when(composition.getName()).thenReturn("Crevice");
			when(composition.getActions()).thenReturn(new String[]{"Enter"});
			when(object.click("Enter")).thenReturn(true);
			Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
			when(cache.query()).thenCallRealMethod();
			when(cache.getStream()).thenAnswer(call -> java.util.stream.Stream.of(object));
			try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
				MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
				MockedStatic<Rs2LiveScene> liveScene = mockStatic(Rs2LiveScene.class);
				MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class))
			{
				microbot.when(Microbot::getClientThread).thenReturn(thread);
				microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(row.getOrigin(), Set.of(row)));
				PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
				Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
				assertSame(object, scene.find(edge).getObject());
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
					Rs2CatalogTransitionScene.dispatch(edge, "Enter", 30198));
				when(object.getId()).thenReturn(30201);
				assertNull(scene.find(edge));
				when(object.getId()).thenReturn(30198);
				when(composition.getActions()).thenReturn(new String[]{"Use"});
				assertNull(scene.find(edge));
				verify(object, times(1)).click("Enter");
			}
		}
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getObjectId() == 30198).collect(Collectors.toList());
	}
}
