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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeissPostQuestDirectTransportTest
{
	private static final Set<Integer> SUPPORTED_IDS = Set.of(33192, 33312, 33329);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> SUPPORTED_IDS.contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void allSevenExactPostQuestRowsAreEngineOwned()
	{
		List<Transport> rows = rows();
		assertEquals(7, rows.size());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 33192).count());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 33312).count());
		assertEquals(3, rows.stream().filter(row -> row.getObjectId() == 33329).count());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(Map.of(Quest.MAKING_FRIENDS_WITH_MY_ARM, QuestState.FINISHED),
				row.getQuests());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertTrue(java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));

			row.getQuests().clear();
			assertFalse(CatalogTransitionPolicy.isEligible(row));
			row.getQuests().put(Quest.MAKING_FRIENDS_WITH_MY_ARM, QuestState.FINISHED);
		}
	}

	@Test
	public void directObstaclesRequireTheirExactDirectedLanding()
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
