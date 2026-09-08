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

public class RunecraftingExitPortalTest
{
	private static final Set<Integer> IDS = Set.of(34749, 34750, 34751, 34752, 34753,
		34754, 34756, 34757, 43478);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()) && "Use".equals(row.getAction())
				&& "Portal".equals(row.getName())).collect(Collectors.toList());
	}

	@Test
	public void allSixteenFixedExitRowsUseEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(16, rows.size());
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
			Transport foreign = new Transport(new WorldPoint(100, 100, row.getOrigin().getPlane()),
				row.getDestination(), "", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void exitPortalWaitsForItsDirectedSurfaceLanding()
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
			CatalogTransition portal = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> portal, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
			if (row.getObjectId() == 34757)
			{
				WorldPoint otherChaosLanding = rows.stream()
					.filter(candidate -> candidate.getObjectId() == 34757
						&& !candidate.getDestination().equals(to))
					.map(Transport::getDestination).findFirst().orElseThrow(AssertionError::new);
				assertEquals(RouteInteraction.Status.UNAVAILABLE,
					scanner.observePending(pending, otherChaosLanding, edge -> null, 13).getStatus());
			}
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
