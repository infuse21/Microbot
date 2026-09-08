package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedAccessAndExitTransportTest
{
	private static final Set<Integer> IDS = Set.of(
		23052, 23285, 23286, 23287, 55322, 55323, 3761, 3773, 3774);
	private static final Set<Integer> STATION_IDS = Set.of(23052, 23285, 23286, 23287);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	@Test
	public void allThirteenExactRowsUseNavigationEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(13, rows.size());
		assertEquals(4, rows.stream().filter(row -> STATION_IDS.contains(row.getObjectId())).count());
		assertEquals(4, rows.stream().filter(row -> Set.of(55322, 55323)
			.contains(row.getObjectId())).count());
		assertEquals(5, rows.stream().filter(row -> Set.of(3761, 3773, 3774)
			.contains(row.getObjectId())).count());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(1, row.getDuration());
			assertEquals(STATION_IDS.contains(row.getObjectId())
				? Map.of(Quest.ANOTHER_SLICE_OF_HAM, QuestState.FINISHED) : Map.of(),
				row.getQuests());
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
		}
	}

	@Test
	public void allThirteenRowsRequireTheirExactDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition object = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			WorldPoint near = new WorldPoint(to.getX() + 1, to.getY(), to.getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, near, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}

	@Test
	public void shiftedAndUngatedStationCopiesStayLegacy()
	{
		Transport shifted = new Transport(new WorldPoint(1521, 3247, 0),
			new WorldPoint(1513, 3243, 0), "test", TransportType.TRANSPORT,
			true, "Pass-through", "Gate", 55322);
		assertFalse(CatalogTransitionPolicy.isEligible(shifted));
		Transport station = new Transport(new WorldPoint(2941, 10179, 0),
			new WorldPoint(2438, 5535, 0), "test", TransportType.TRANSPORT,
			true, "Enter", "Doorway", 23287);
		assertFalse(CatalogTransitionPolicy.isEligible(station));
	}
}
