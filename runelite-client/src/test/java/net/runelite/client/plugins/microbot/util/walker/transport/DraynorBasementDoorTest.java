package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DraynorBasementDoorTest
{
	private static List<Transport> doors()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT
				&& row.getObjectId() >= 137 && row.getObjectId() <= 145).collect(Collectors.toList());
	}

	@Test
	public void allEighteenCrossingsPreserveEveryLeverCombination()
	{
		int[] masks = {49, 42, 53, 24, 44, 48, 42, 39, 24};
		int[] values = {48, 40, 1, 8, 44, 32, 8, 3, 8};
		List<Transport> rows = doors();
		assertEquals(18, rows.size());
		for (Transport row : rows)
		{
			assertTrue(AdjacentTransportPolicy.isEligible(row));
			assertFalse(CatalogTransitionPolicy.isEligible(row));
			assertEquals(2, row.getOrigin().distanceTo2D(row.getDestination()));
			for (int combination = 0; combination < 64; combination++)
			{
				final int state = combination;
				boolean unlocked = row.getVarbits().stream()
					.allMatch(gate -> gate.matches((state >> (gate.getVarbitId() - 1788)) & 1));
				int door = row.getObjectId() - 137;
				assertEquals((state & masks[door]) == values[door], unlocked);
				assertEquals(unlocked, AdjacentTransportPolicy.hasRequiredDraynorLevers(row,
					id -> (state >> (id - 1788)) & 1));
			}
			row.getVarbits().clear();
			assertFalse(AdjacentTransportPolicy.isEligible(row));
		}
	}

	@Test
	public void genericTwoTileDoorsDoNotInheritPuzzleOwnership()
	{
		Transport foreign = new Transport(new WorldPoint(3108, 9756, 0),
			new WorldPoint(3108, 9758, 0), "Draynor basement puzzle door",
			TransportType.TRANSPORT, false, "Open", "Door", 144);
		assertFalse(AdjacentTransportPolicy.isEligible(foreign));
		Transport adjacentWithoutGates = new Transport(new WorldPoint(3108, 9757, 0),
			new WorldPoint(3108, 9758, 0), "", TransportType.TRANSPORT, false, "Open", "Door", 144);
		assertFalse(AdjacentTransportPolicy.isEligible(adjacentWithoutGates));
	}

	@Test
	public void relockedDoorIsUnavailableRatherThanMistakenForClearance()
	{
		Transport row = doors().get(0);
		RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
			row.getOrigin(), RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Open", true, row.getObjectId(), row.getOrigin(), row.getDestination());
		AdjacentTransportScene locked = new AdjacentTransportScene()
		{
			@Override
			public boolean isEnabled(PlannedEdge edge, int id)
			{
				assertEquals(row.getObjectId(), id);
				return false;
			}

			@Override
			public AdjacentTransport find(PlannedEdge edge)
			{
				throw new AssertionError("a relocked edge must not use missing-object clearance");
			}
		};
		AdjacentTransportRouteScanner scanner = new AdjacentTransportRouteScanner();
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, row.getOrigin(), locked, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, row.getDestination(), locked, 13).getStatus());
	}

	@Test
	public void persistentDoorsRetireOnlyOnTheDirectedDestinationSide()
	{
		AdjacentTransportRouteScanner scanner = new AdjacentTransportRouteScanner();
		for (Transport row : doors())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			WorldPoint middle = new WorldPoint((from.getX() + to.getX()) / 2,
				(from.getY() + to.getY()) / 2, 0);
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, middle,
				RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
				"Open", true, row.getObjectId(), from, to);
			AdjacentTransport door = new AdjacentTransport(null, middle, row.getObjectId(), "Open", from, to);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> door, 13).getStatus());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, middle, edge -> door, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> door, 13).getStatus());
		}
	}
}
