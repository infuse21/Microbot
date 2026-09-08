package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

public class AbyssPassageTransportTest
{
	private static final int PASSAGE_ID = 26250;

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == PASSAGE_ID && "Go-through".equals(row.getAction())
				&& "Passage".equals(row.getName())).collect(Collectors.toList());
	}

	@Test
	public void allTwelveAbyssConfigurationsUseExactEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(12, rows.size());
		assertEquals(12, rows.stream().map(row -> row.getOrigin() + "->" + row.getDestination())
			.collect(Collectors.toSet()).size());
		assertEquals(Set.of(new WorldPoint(3039, 4844, 0), new WorldPoint(3033, 4843, 0),
			new WorldPoint(3052, 4831, 0)), rows.stream().map(Transport::getDestination)
				.collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertTrue(java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0));
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertEquals(0, row.getCurrencyAmount());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));

			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", TransportType.TRANSPORT, false, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void anotherPassageLandingCannotAcknowledgeTheSelectedRoute()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		List<Transport> rows = rows();
		for (Transport row : rows)
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition passage = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> passage, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());

			WorldPoint otherLanding = rows.stream()
				.filter(candidate -> !candidate.getDestination().equals(to)
					&& candidate.getDestination().distanceTo2D(to) > 2)
				.map(Transport::getDestination).findFirst().orElseThrow(AssertionError::new);
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, otherLanding, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
