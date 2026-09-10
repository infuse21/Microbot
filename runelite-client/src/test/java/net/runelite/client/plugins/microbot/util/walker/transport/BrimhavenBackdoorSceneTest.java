package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2LiveScene;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrimhavenBackdoorSceneTest
{
	@Test
	public void exactObjectsResolveOnlyWhileUnlockedAndDispatchRechecksTheState()
	{
		for (Transport row : BrimhavenSouthernEntranceSourceTest.rows())
		{
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			List<Rs2TileObjectModel> objects = new ArrayList<>();
			Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
			when(cache.query()).thenCallRealMethod();
			when(cache.getStream()).thenAnswer(call -> objects.stream());
			try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
				MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
				MockedStatic<Rs2LiveScene> liveScene = mockStatic(Rs2LiveScene.class))
			{
				microbot.when(Microbot::getClientThread).thenReturn(thread);
				microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(row.getOrigin(), Set.of(row)));
				microbot.when(() -> Microbot.getVarbitValue(5629)).thenReturn(1);
				PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
				Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
				objects.add(object(30199, row.getOrigin(), row.getName(), row.getAction()));
				objects.add(object(row.getObjectId(), row.getOrigin(), "Unrelated", row.getAction()));
				objects.add(object(row.getObjectId(), row.getOrigin(), row.getName(), "Examine"));
				objects.add(object(row.getObjectId(), new WorldPoint(row.getOrigin().getX() + 10,
					row.getOrigin().getY(), 0), row.getName(), row.getAction()));
				objects.add(object(row.getObjectId(), new WorldPoint(row.getOrigin().getX(),
					row.getOrigin().getY(), 1), row.getName(), row.getAction()));
				assertNull(scene.find(edge));
				for (Rs2TileObjectModel wrong : objects) verify(wrong, never()).click(any(String.class));
				objects.clear();
				Rs2TileObjectModel correct = object(row.getObjectId(), row.getOrigin(), row.getName(), row.getAction());
				objects.add(correct);
				for (int state : new int[]{1, 2, 3})
				{
					microbot.when(() -> Microbot.getVarbitValue(5629)).thenReturn(state);
					CatalogTransition found = scene.find(edge);
					assertNotNull(found);
					assertSame(correct, found.getObject());
					assertEquals(row.getDestination(), found.getDestination());
				}
				if (row.getObjectId() == 66)
				{
					when(correct.getId()).thenReturn(30200);
					assertSame(correct, scene.find(edge).getObject());
				}
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
					Rs2CatalogTransitionScene.dispatch(edge, row.getAction(), row.getObjectId()));
				for (int state : new int[]{0, 4, -1})
				{
					microbot.when(() -> Microbot.getVarbitValue(5629)).thenReturn(state);
					assertNull(scene.observe(edge, row.getAction()));
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
						Rs2CatalogTransitionScene.dispatch(edge, row.getAction(), row.getObjectId()));
				}
				verify(correct, times(1)).click(row.getAction());
			}
		}
	}

	private static Rs2TileObjectModel object(int id, WorldPoint point, String name, String action)
	{
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		ObjectComposition composition = mock(ObjectComposition.class);
		when(object.getId()).thenReturn(id);
		when(object.getWorldLocation()).thenReturn(point);
		when(object.getObjectComposition()).thenReturn(composition);
		when(composition.getName()).thenReturn(name);
		when(composition.getActions()).thenReturn(new String[]{action});
		when(object.click(action)).thenReturn(true);
		return object;
	}
}
