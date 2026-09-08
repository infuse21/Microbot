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

public class GrandTreeRootsAndMudPileTransportTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 2451 || row.getObjectId() == 13)
			.collect(Collectors.toList());
	}

	@Test
	public void allSevenExactRowsUseNavigationEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(7, rows.size());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 2451).count());
		assertEquals(3, rows.stream().filter(row -> row.getObjectId() == 13).count());
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && row.getDuration() == 1
			&& CatalogTransitionPolicy.isEligible(row)
			&& PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		assertTrue(rows.stream().filter(row -> row.getObjectId() == 2451).allMatch(row ->
			row.getQuests().equals(Map.of(Quest.THE_GRAND_TREE, QuestState.FINISHED))));
		assertTrue(rows.stream().filter(row -> row.getObjectId() == 13)
			.allMatch(row -> row.getQuests().isEmpty()));
	}

	@Test
	public void exactDestinationIsTheOnlyArrivalProof()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getDestination(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition object = new CatalogTransition(null, row.getDestination(),
				row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
				row.getDestination().getY(), row.getDestination().getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, near, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> object, 13).getStatus());
		}
	}

	@Test
	public void missingQuestAndForeignGeometryStayLegacy()
	{
		Transport roots = rows().stream().filter(row -> row.getObjectId() == 2451)
			.findFirst().orElseThrow(AssertionError::new);
		roots.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isEligible(roots));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(roots)));

		Transport foreignMud = new Transport(new WorldPoint(2622, 9797, 0),
			new WorldPoint(2623, 3391, 0), "test", TransportType.TRANSPORT,
			true, "Climb-over", "Mud pile", 13);
		assertFalse(CatalogTransitionPolicy.isEligible(foreignMud));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(foreignMud)));
	}
}
