package net.runelite.client.plugins.microbot.util.walker.obstacle;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MineableRouteScannerTest
{
	private static final WorldPoint A = new WorldPoint(3728, 5683, 0);
	private static final WorldPoint B = new WorldPoint(3727, 5683, 0);
	private static final WorldPoint C = new WorldPoint(3726, 5683, 0);
	private static final WorldPoint NEARBY = new WorldPoint(3727, 5684, 0);
	private final MineableRouteScanner scanner = new MineableRouteScanner(new MineableResolver());

	@Test
	public void selectsBlockerOnForwardRouteEdgeButIgnoresNearbyObject()
	{
		Map<WorldPoint, TileObject> objects = new HashMap<>();
		objects.put(NEARBY, rockfall());
		assertNull(scanner.scan(1, Arrays.asList(A, B, C), 0, 2,
			scene(A, objects), true, 3));

		objects.put(B, rockfall());
		RouteInteraction interaction = scanner.scan(1, Arrays.asList(A, B, C), 0, 2,
			scene(A, objects), true, 3);

		assertEquals(0, interaction.getRawEdgeIndex());
		assertEquals(B, interaction.getObjectTile());
		assertEquals(RouteInteraction.Status.AVAILABLE, interaction.getStatus());
	}

	@Test
	public void pendingBlockerCanDisappearWhileApproaching()
	{
		RouteInteraction pending = new RouteInteraction(1, 0, A, B, B,
			RouteInteraction.Kind.MINEABLE, RouteInteraction.Status.AVAILABLE, "mine", false);

		RouteInteraction observed = scanner.observePending(pending,
			scene(A, Collections.emptyMap()), true, 3);

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
	}

	@Test
	public void doesNotTurnOriginObjectIntoBackwardApproach()
	{
		Map<WorldPoint, TileObject> objects = new HashMap<>();
		objects.put(B, rockfall());

		assertNull(scanner.scan(1, Arrays.asList(A, B, C), 1, 1,
			scene(C, objects), true, 3));
	}

	@Test
	public void unavailableBlockerRetainsItsExactEdge()
	{
		Map<WorldPoint, TileObject> objects = new HashMap<>();
		objects.put(B, rockfall());

		RouteInteraction interaction = scanner.scan(3, Arrays.asList(A, B, C), 0, 2,
			scene(A, objects), false, 3);

		assertEquals(RouteInteraction.Status.UNAVAILABLE, interaction.getStatus());
		assertEquals(0, interaction.getRawEdgeIndex());
	}

	private static TileObject rockfall()
	{
		TileObject object = mock(TileObject.class);
		when(object.getId()).thenReturn(ObjectID.MOTHERLODE_ROCKFALL_1);
		return object;
	}

	private static LiveScene scene(WorldPoint player, Map<WorldPoint, TileObject> objects)
	{
		return new LiveScene()
		{
			@Override
			public WorldPoint playerLocation() { return player; }

			@Override
			public boolean isReachable(WorldPoint tile) { return true; }

			@Override
			public Set<Transport> transportsAt(WorldPoint tile) { return Collections.emptySet(); }

			@Override
			public TileObject objectAt(WorldPoint tile) { return objects.get(tile); }
		};
	}
}
