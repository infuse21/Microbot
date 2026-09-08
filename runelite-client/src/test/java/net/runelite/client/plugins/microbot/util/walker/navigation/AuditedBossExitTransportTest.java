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

public class AuditedBossExitTransportTest
{
	private static final Set<Integer> IDS = Set.of(21772, 26763);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	@Test
	public void allSixExactExitsUseNavigationEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(6, rows.size());
		assertEquals(3, rows.stream().filter(row -> row.getObjectId() == 21772).count());
		assertEquals(3, rows.stream().filter(row -> row.getObjectId() == 26763).count());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(1, row.getDuration());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertTrue(java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
		}
	}

	@Test
	public void eachExitRequiresItsExactDirectedLanding()
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
	public void nonMemberAndShiftedCopiesStayLegacy()
	{
		Transport nonMember = new Transport(new WorldPoint(1240, 1226, 0),
			new WorldPoint(1291, 1253, 0), "test", TransportType.TRANSPORT,
			false, "Exit", "Portcullis", 21772);
		assertFalse(CatalogTransitionPolicy.isEligible(nonMember));
		Transport shifted = new Transport(new WorldPoint(3232, 10350, 0),
			new WorldPoint(3233, 3950, 0), "test", TransportType.TRANSPORT,
			true, "Use", "Crevice", 26763);
		assertFalse(CatalogTransitionPolicy.isEligible(shifted));
	}
}
