package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MolchLizardTempleTransportTest
{
	private static final Set<Integer> IDS = Set.of(34402, 34403, 34405, 34422);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.filter(row -> "Lizard dwelling".equals(row.getName())
				|| "Strange hole".equals(row.getName()))
			.collect(Collectors.toList());
	}

	@Test
	public void sixteenOneWayTempleLinksUseExactEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(16, rows.size());
		assertEquals(8, rows.stream().filter(row -> "Lizard dwelling".equals(row.getName())).count());
		assertEquals(8, rows.stream().filter(row -> "Strange hole".equals(row.getName())).count());
		for (Transport row : rows)
		{
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void sharedDwellingIdCannotSelectTheOtherDirectedInterior()
	{
		List<Transport> shared = rows().stream().filter(row -> row.getObjectId() == 34403)
			.collect(Collectors.toList());
		assertEquals(4, shared.size());
		assertEquals(2, shared.stream().map(Transport::getDestination).distinct().count());
		Transport row = shared.get(0);
		WorldPoint otherDestination = shared.stream().map(Transport::getDestination)
			.filter(destination -> !destination.equals(row.getDestination())).findFirst().orElseThrow();
		Transport wrongPair = new Transport(row.getOrigin(), otherDestination, "", row.getType(), true,
			row.getAction(), row.getName(), row.getObjectId());
		assertFalse(CatalogTransitionPolicy.isEligible(wrongPair));
	}

	@Test
	public void pendingLinkCompletesOnlyAtItsDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition transition = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> transition, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
