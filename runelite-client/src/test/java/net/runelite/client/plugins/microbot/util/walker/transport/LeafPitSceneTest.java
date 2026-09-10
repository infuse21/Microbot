package net.runelite.client.plugins.microbot.util.walker.transport;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.playerstate.Rs2PlayerStateCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeafPitSceneTest
{
	private static final PlannedEdge EDGE = new PlannedEdge(new WorldPoint(2274, 3172, 0),
		new WorldPoint(2274, 3176, 0));
	private static final WorldPoint PIT = new WorldPoint(2313, 9656, 0);
	private final Map<String, Object> originals = new LinkedHashMap<>();
	private final List<Rs2TileObjectModel> objects = new ArrayList<>();
	private Rs2PlayerStateCache playerState;
	private WorldView worldView;

	@Before
	public void setUp() throws Exception
	{
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
			Optional.ofNullable(((Callable<?>) invocation.getArgument(0)).call()));
		when(thread.invoke(org.mockito.ArgumentMatchers.<java.util.function.Supplier<Object>>any())).thenAnswer(invocation ->
			((java.util.function.Supplier<?>) invocation.getArgument(0)).get());
		Client client = mock(Client.class);
		Player player = mock(Player.class);
		worldView = mock(WorldView.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldView()).thenReturn(worldView);
		playerState = mock(Rs2PlayerStateCache.class);
		when(playerState.getLocalPlayerPosition()).thenReturn(PIT);
		Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		when(cache.getStream()).thenAnswer(invocation -> objects.stream());
		install("client", client);
		install("clientThread", thread);
		install("rs2PlayerStateCache", playerState);
		install("rs2TileObjectCache", cache);
	}

	@After
	public void tearDown() throws Exception
	{
		for (Map.Entry<String, Object> entry : originals.entrySet())
		{
			Field field = Microbot.class.getDeclaredField(entry.getKey());
			field.setAccessible(true);
			field.set(null, entry.getValue());
		}
	}

	@Test
	public void recoveryUsesOnlyTheCurrentPitsExactObjectAndAction()
	{
		Rs2TileObjectModel wrongPit = object(3927, new WorldPoint(2336, 9656, 0), "Protruding rocks", "Climb");
		Rs2TileObjectModel wrongId = object(3926, PIT, "Protruding rocks", "Climb");
		Rs2TileObjectModel wrongAction = object(3927, PIT, "Protruding rocks", "Enter");
		Rs2TileObjectModel correct = object(3927, PIT, "Protruding rocks", "Climb");
		CatalogTransition recovery = new Rs2CatalogTransitionScene().observe(EDGE, "Jump");
		assertNotNull(recovery);
		assertSame(correct, recovery.getObject());
		assertEquals(EDGE.to(), recovery.getDestination());
		assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
			Rs2CatalogTransitionScene.dispatch(EDGE, "Climb", 3925));
		verify(correct).click("Climb");
		verify(wrongPit, never()).click(any(String.class));
		verify(wrongId, never()).click(any(String.class));
		verify(wrongAction, never()).click(any(String.class));
	}

	@Test
	public void recoveryDoesNotRequireTheJumpToRemainInTheFilteredCatalog()
	{
		// No route snapshot or inventory/HP globals are installed: recovery only needs its owned edge.
		object(3927, PIT, "Protruding rocks", "Climb");
		assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
			Rs2CatalogTransitionScene.dispatch(EDGE, "Climb", 3925));
	}

	@Test
	public void missingHandholdsAndOrdinaryUndergroundLocationsCannotReceiveRecoveryInput()
	{
		assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
			Rs2CatalogTransitionScene.dispatch(EDGE, "Climb", 3925));
		Rs2TileObjectModel outside = object(3927, new WorldPoint(2274, 9574, 0), "Protruding rocks", "Climb");
		when(playerState.getLocalPlayerPosition()).thenReturn(new WorldPoint(2274, 9574, 0));
		assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
			Rs2CatalogTransitionScene.dispatch(EDGE, "Climb", 3925));
		verify(outside, never()).click(any(String.class));
	}

	private Rs2TileObjectModel object(int id, WorldPoint tile, String name, String action)
	{
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		ObjectComposition composition = mock(ObjectComposition.class);
		when(object.getId()).thenReturn(id);
		when(object.getWorldLocation()).thenReturn(tile);
		when(object.getWorldView()).thenReturn(worldView);
		when(object.getObjectComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn(name);
		when(composition.getActions()).thenReturn(new String[]{action});
		when(object.click(action)).thenReturn(true);
		objects.add(object);
		return object;
	}

	private void install(String name, Object value) throws Exception
	{
		Field field = Microbot.class.getDeclaredField(name);
		field.setAccessible(true);
		originals.put(name, field.get(null));
		field.set(null, value);
	}
}
