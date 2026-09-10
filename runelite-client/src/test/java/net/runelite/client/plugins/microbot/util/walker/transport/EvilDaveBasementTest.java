package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvilDaveBasementTest
{
	@Test
	public void exactQuestGatedTrapdoorStagesNeverAcknowledgeOpeningAsLanding()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getObjectId() == 12268).collect(java.util.stream.Collectors.toList());
		assertEquals(1, rows.size());
		Transport row = rows.get(0);
		assertTrue(CatalogTransitionPolicy.isEligible(row));
		Client client = mock(Client.class);
		Player actor = mock(Player.class);
		WorldView world = mock(WorldView.class);
		when(client.getLocalPlayer()).thenReturn(actor);
		when(actor.getWorldView()).thenReturn(world);
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
		when(thread.invoke(any(Supplier.class))).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		when(object.getId()).thenReturn(12267);
		when(object.getWorldLocation()).thenReturn(row.getOrigin());
		when(object.getWorldView()).thenReturn(world);
		ObjectComposition definition = mock(ObjectComposition.class);
		when(object.getObjectComposition()).thenReturn(definition);
		when(definition.getName()).thenReturn("Trapdoor");
		when(definition.getActions()).thenReturn(new String[]{"Open"});
		when(object.click("Open")).thenReturn(true);
		when(object.click("Go-down")).thenReturn(true);
		Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
			MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
			MockedStatic<Rs2LiveScene> live = mockStatic(Rs2LiveScene.class))
		{
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
			path.when(Rs2PathApi::getTransports).thenReturn(Map.of(row.getOrigin(), Set.of(row)));
			player.when(Rs2Player::getWorldLocation).thenReturn(row.getOrigin());
			player.when(() -> Rs2Player.getQuestState(Quest.SHADOW_OF_THE_STORM)).thenReturn(QuestState.FINISHED);
			PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
			Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
			assertEquals("Open", scene.find(edge).getAction());
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
				Rs2CatalogTransitionScene.dispatch(edge, "Open", 12268));
			when(object.getId()).thenReturn(12268);
			when(definition.getName()).thenReturn("Open trapdoor");
			when(definition.getActions()).thenReturn(new String[]{"Go-down", "Close"});
			assertEquals("Go-down", scene.observe(edge, "Open").getAction());
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
				Rs2CatalogTransitionScene.dispatch(edge, "Open", 12268));
			assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
				Rs2CatalogTransitionScene.dispatch(edge, "Go-down", 12268));
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				"Go-down", true, 12268, row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, row.getOrigin(), scene, 6).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), scene, 6).getStatus());
			for (QuestState state : new QuestState[]{QuestState.NOT_STARTED, QuestState.IN_PROGRESS})
			{
				player.when(() -> Rs2Player.getQuestState(Quest.SHADOW_OF_THE_STORM)).thenReturn(state);
				assertNull(scene.find(edge));
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
					Rs2CatalogTransitionScene.dispatch(edge, "Go-down", 12268));
			}
			player.when(() -> Rs2Player.getQuestState(Quest.SHADOW_OF_THE_STORM)).thenReturn(QuestState.FINISHED);
			when(object.getWorldLocation()).thenReturn(row.getDestination());
			assertNull(scene.find(edge));
			verify(object, times(1)).click("Open");
			verify(object, times(1)).click("Go-down");
		}
		row.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isEligible(row));
	}
}
