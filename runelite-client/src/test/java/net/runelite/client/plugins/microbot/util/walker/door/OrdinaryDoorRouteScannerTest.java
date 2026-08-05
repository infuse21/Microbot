package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.door.model.OrdinaryDoor;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class OrdinaryDoorRouteScannerTest
{
	@Test
	public void catalogTransportEdgeCannotBeClaimedAsOrdinaryDoor()
	{
		RouteEdge transportEdge = new RouteEdge(0, A, B, RouteEdge.Kind.ADJACENT_TRANSPORT);
		RoutePlan plan = new RoutePlan(1, 1, A, Collections.singleton(B),
			Arrays.asList(A, B), Arrays.asList(A, B), true,
			Collections.singletonList(transportEdge));
		DoorScene scene = edge -> new OrdinaryDoor(null, B, "Open");

		RouteInteraction interaction = new OrdinaryDoorRouteScanner().scan(plan, 0, 1,
			A, scene, 13);

		assertNull(interaction);
	}
	private static final WorldPoint A = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint B = new WorldPoint(3201, 3200, 0);
	private static final WorldPoint C = new WorldPoint(3202, 3200, 0);
	private final OrdinaryDoorRouteScanner scanner = new OrdinaryDoorRouteScanner();

	@Test
	public void selectsEarliestExactRouteEdgeAndAllowsRangedDispatch()
	{
		Map<String, OrdinaryDoor> doors = new HashMap<>();
		doors.put(key(A, B), door(B));
		doors.put(key(B, C), door(C));

		RouteInteraction interaction = scanner.scan(4, Arrays.asList(A, B, C), 0, 2,
			new WorldPoint(3195, 3200, 0), scene(doors), 13);

		assertEquals(RouteInteraction.Kind.DOOR, interaction.getKind());
		assertEquals(0, interaction.getRawEdgeIndex());
		assertEquals(B, interaction.getObjectTile());
		assertTrue(interaction.isReady());
	}

	@Test
	public void missingPendingDoorIsObservedAsCleared()
	{
		RouteInteraction pending = new RouteInteraction(2, 0, A, B, B,
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.AVAILABLE, "Open", true);

		RouteInteraction observed = scanner.observePending(pending, A,
			scene(new HashMap<>()), 13);

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
	}

	private static DoorScene scene(Map<String, OrdinaryDoor> doors)
	{
		return edge -> doors.get(key(edge.from(), edge.to()));
	}

	private static OrdinaryDoor door(WorldPoint tile)
	{
		return new OrdinaryDoor(mock(TileObject.class), tile, "Open");
	}

	private static String key(WorldPoint from, WorldPoint to)
	{
		return from + ">" + to;
	}
}
