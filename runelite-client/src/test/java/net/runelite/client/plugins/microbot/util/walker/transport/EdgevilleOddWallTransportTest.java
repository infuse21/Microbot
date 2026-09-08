package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EdgevilleOddWallTransportTest
{
	private static final WorldPoint STRONGHOLD_ORIGIN = new WorldPoint(2150, 5279, 0);
	private static final WorldPoint STRONGHOLD_DESTINATION = new WorldPoint(2123, 5252, 0);

	@Test
	public void acceptsOnlyTheFourExactEdgevilleWallRows()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> "Odd looking wall".equals(row.getName()))
			.collect(Collectors.toList());

		assertEquals(4, rows.size());
		assertTrue(rows.stream().allMatch(AdjacentTransportPolicy::isEdgevilleOddWall));
		assertTrue(rows.stream().allMatch(AdjacentTransportPolicy::isEligible));
	}

	@Test
	public void rejectsMutatedOrUngatedCopies()
	{
		WorldPoint west = new WorldPoint(3093, 9895, 0);
		WorldPoint east = new WorldPoint(3094, 9895, 0);
		assertFalse(AdjacentTransportPolicy.isEdgevilleOddWall(transport(west, east,
			"Open", "Odd looking wall", 1736, false)));
		assertFalse(AdjacentTransportPolicy.isEdgevilleOddWall(transport(west, east,
			"Push", "Wall", 1736, false)));
		assertFalse(AdjacentTransportPolicy.isEdgevilleOddWall(transport(west, east,
			"Push", "Odd looking wall", 1734, false)));
		assertFalse(AdjacentTransportPolicy.isEdgevilleOddWall(transport(west,
			new WorldPoint(3094, 9897, 0), "Push", "Odd looking wall", 1736, false)));
		assertFalse(AdjacentTransportPolicy.isEdgevilleOddWall(transport(west, east,
			"Push", "Odd looking wall", 1736, true)));

		Transport itemGated = transport(west, east, "Push", "Odd looking wall", 1736, false);
		itemGated.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(AdjacentTransportPolicy.isEdgevilleOddWall(itemGated));
	}

	@Test
	public void exactLandingRetiresTheWallInteraction()
	{
		WorldPoint east = new WorldPoint(3094, 9895, 0);
		WorldPoint west = new WorldPoint(3093, 9895, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, east, west, east,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Push", true, 1736, east, west);

		assertFalse(AdjacentTransportRouteScanner.hasCrossedCatalogBoundary(pending, east));
		assertTrue(AdjacentTransportRouteScanner.hasCrossedCatalogBoundary(pending, west));
	}

	@Test
	public void malformedStrongholdDuplicateIsEvidenceOnly() throws IOException
	{
		String source = new String(getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv").readAllBytes(),
			StandardCharsets.UTF_8);
		assertTrue(source.contains("# 2150 5279 0\t2123 5252 0\tPush;Odd looking wall;1734"));
		assertEquals(0, Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> STRONGHOLD_ORIGIN.equals(row.getOrigin()))
			.filter(row -> STRONGHOLD_DESTINATION.equals(row.getDestination()))
			.filter(row -> row.getObjectId() == 1734)
			.count());
		assertEquals(1, Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> STRONGHOLD_ORIGIN.equals(row.getOrigin()))
			.filter(row -> STRONGHOLD_DESTINATION.equals(row.getDestination()))
			.filter(row -> row.getObjectId() == 23703)
			.filter(row -> "Climb-up".equals(row.getAction()))
			.filter(row -> "Goo covered vine".equals(row.getName()))
			.count());
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination, String action,
		String name, int objectId, boolean members)
	{
		return new Transport(origin, destination, "", TransportType.TRANSPORT,
			members, action, name, objectId);
	}
}
