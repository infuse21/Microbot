package net.runelite.client.plugins.microbot.util.walker.obstacle;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2SceneLocationTest
{
	private Client originalClient;
	private ClientThread originalClientThread;
	private Client client;

	@Before
	public void setUp() throws Exception
	{
		originalClient = Microbot.getClient();
		originalClientThread = Microbot.getClientThread();
		client = mock(Client.class);
		ClientThread clientThread = mock(ClientThread.class);
		when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
		{
			Callable<?> callable = invocation.getArgument(0);
			return Optional.ofNullable(callable.call());
		});
		setStaticField("client", client);
		setStaticField("clientThread", clientThread);
	}

	@After
	public void tearDown() throws Exception
	{
		setStaticField("client", originalClient);
		setStaticField("clientThread", originalClientThread);
	}

	@Test
	public void convertsInstanceObjectAnchorIntoTemplateCoordinates()
	{
		WorldView worldView = mock(WorldView.class);
		TileObject object = mock(TileObject.class);
		int[][][] chunks = emptyChunks();
		chunks[0][1][2] = templateChunk(400, 410, 0, 0);
		LocalPoint local = new LocalPoint(10 * 128 + 64, 18 * 128 + 64, 7);

		when(worldView.isInstance()).thenReturn(true);
		when(worldView.getInstanceTemplateChunks()).thenReturn(chunks);
		when(client.getWorldView(7)).thenReturn(worldView);
		when(object.getWorldView()).thenReturn(worldView);
		when(object.getLocalLocation()).thenReturn(local);
		when(object.getPlane()).thenReturn(0);
		when(object.getWorldLocation()).thenReturn(new WorldPoint(10010, 10018, 0));

		assertEquals(new WorldPoint(3202, 3282, 0),
			Rs2SceneLocation.templateLocation(object));
	}

	@Test
	public void preservesOrdinaryObjectCoordinates()
	{
		WorldView worldView = mock(WorldView.class);
		TileObject object = mock(TileObject.class);
		WorldPoint raw = new WorldPoint(3200, 3200, 1);
		when(worldView.isInstance()).thenReturn(false);
		when(object.getWorldView()).thenReturn(worldView);
		when(object.getWorldLocation()).thenReturn(raw);

		assertEquals(raw, Rs2SceneLocation.templateLocation(object));
	}

	@Test
	public void expandsTemplateCoordinateToEveryMatchingInstanceTile()
	{
		WorldView worldView = mock(WorldView.class);
		int[][][] chunks = emptyChunks();
		chunks[0][1][2] = templateChunk(400, 410, 0, 0);
		when(worldView.isInstance()).thenReturn(true);
		when(worldView.getInstanceTemplateChunks()).thenReturn(chunks);
		when(worldView.getBaseX()).thenReturn(10000);
		when(worldView.getBaseY()).thenReturn(10000);
		when(client.getTopLevelWorldView()).thenReturn(worldView);

		Collection<WorldPoint> locations = Rs2SceneLocation.sceneLocations(
			new WorldPoint(3202, 3282, 0));

		assertEquals(1, locations.size());
		assertEquals(new WorldPoint(10010, 10018, 0), locations.iterator().next());
	}

	@Test
	public void recognizesWrappedClientThreadTimeoutWithoutHidingOtherFailures()
	{
		assertTrue(Rs2SceneLocation.clientThreadUnavailable(
			new RuntimeException(new TimeoutException())));
		assertFalse(Rs2SceneLocation.clientThreadUnavailable(
			new RuntimeException(new IllegalStateException())));
	}

	private static int[][][] emptyChunks()
	{
		int[][][] chunks = new int[4][13][13];
		for (int plane = 0; plane < chunks.length; plane++)
		{
			for (int x = 0; x < chunks[plane].length; x++)
			{
				java.util.Arrays.fill(chunks[plane][x], -1);
			}
		}
		return chunks;
	}

	private static int templateChunk(int chunkX, int chunkY, int plane, int rotation)
	{
		return plane << 24 | chunkX << 14 | chunkY << 3 | rotation << 1;
	}

	private static void setStaticField(String name, Object value) throws Exception
	{
		Field field = Microbot.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}
}
