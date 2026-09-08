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
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZanarisOneWayFairyExitTransportTest
{
	private static final Set<Integer> IDS = Set.of(12003, 12094);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	@Test
	public void bothOneWayExitsHaveExactCatalogOwnershipAndRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(2, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(6, row.getDuration());
			assertEquals(Map.of(Quest.LOST_CITY, QuestState.FINISHED), row.getQuests());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
		}
	}

	@Test
	public void completionRequiresTheExactDirectedLanding()
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
}
