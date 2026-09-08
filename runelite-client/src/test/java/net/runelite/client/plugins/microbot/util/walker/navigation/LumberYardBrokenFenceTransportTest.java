package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
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

public class LumberYardBrokenFenceTransportTest
{
	private static List<Transport> rows()
	{
		return List.copyOf(Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 2618)
			.collect(Collectors.toMap(row -> row.getOrigin() + "->" + row.getDestination(),
				row -> row, (left, right) -> left, LinkedHashMap::new)).values());
	}

	@Test
	public void bothFreeToPlayFenceDirectionsUseNavigationEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(2, rows.size());
		assertTrue(rows.stream().allMatch(row -> !row.isMembers()));
		assertTrue(rows.stream().allMatch(row -> row.getDuration() == 0 || row.getDuration() == 6));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		Transport source = rows.get(0);
		Transport mutated = new Transport(source.getOrigin(), source.getDestination(), "",
			TransportType.TRANSPORT, false, "Open", "Broken fence", 2618);
		assertFalse(CatalogTransitionPolicy.isEligible(mutated));
	}

	@Test
	public void exactOppositeSideIsRequiredToClear()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getDestination(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition object = new CatalogTransition(null, row.getDestination(), row.getObjectId(),
				row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
				row.getDestination().getY(), row.getDestination().getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, near, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> object, 13).getStatus());
		}
	}
}
