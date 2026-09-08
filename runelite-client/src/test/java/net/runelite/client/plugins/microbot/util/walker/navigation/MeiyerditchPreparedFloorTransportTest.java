package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MeiyerditchPreparedFloorTransportTest
{
	private static final Set<Integer> IDS = Set.of(18122, 18124);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void sevenPreparedFloorRowsRequireThePersistentKnockdownState()
	{
		List<Transport> rows = rows();
		assertEquals(7, rows.size());
		for (Transport row : rows)
		{
			assertEquals(26, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertEquals(Map.of(Quest.DARKNESS_OF_HALLOWVALE, QuestState.IN_PROGRESS),
				row.getQuests());
			assertEquals(1, row.getVarbits().size());
			TransportVarbit setup = row.getVarbits().iterator().next();
			assertEquals(2589, setup.getVarbitId());
			assertEquals(1, setup.getValue());
			assertTrue(setup.matches(1));
			assertFalse(setup.matches(0));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			Transport foreign = new Transport(new WorldPoint(100, 100, row.getOrigin().getPlane()),
				row.getDestination(), "", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void preparedFloorMustReachItsExactDestination()
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
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
