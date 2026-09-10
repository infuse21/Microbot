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

public class PostQuestCaveAndDoorTransportTest
{
	private static final Set<Integer> SUPPORTED_IDS = Set.of(21585, 5056, 5057, 5060, 5061);
	private static final Set<Integer> REMOVED_IDS = Set.of(4132, 4133);

	private static List<Transport> supportedRows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> SUPPORTED_IDS.contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void exactPostQuestEntrancesAreEngineOwned()
	{
		List<Transport> rows = supportedRows();
		assertEquals(8, rows.size());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 21585).count());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() != 21585).count());
		for (Transport row : rows)
		{
			Quest quest = row.getObjectId() == 21585
				? Quest.THE_FREMENNIK_ISLES : Quest.IN_SEARCH_OF_THE_MYREQUE;
			assertTrue(row.isMembers());
			assertEquals(1, row.getDuration());
			assertEquals(Map.of(quest, QuestState.FINISHED), row.getQuests());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(row.getVarbits().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertTrue(java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));

			row.getQuests().clear();
			assertFalse(CatalogTransitionPolicy.isEligible(row));
			row.getQuests().put(quest, QuestState.FINISHED);
		}
	}

	@Test
	public void ungatedShadeDoorsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.noneMatch(row -> REMOVED_IDS.contains(row.getObjectId())));
	}

	@Test
	public void eastCaveOnlyPublishesExactPreKingDeathQuestStages()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 21584).collect(Collectors.toList());
		assertEquals(6, rows.size());
		for (Transport row : rows)
		{
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			assertEquals(1, row.getVarbits().size());
			var gate = row.getVarbits().iterator().next();
			assertEquals(3311, gate.getVarbitId());
			assertTrue(gate.matches(300) || gate.matches(310));
			assertFalse(gate.matches(290));
			assertFalse(gate.matches(320));
			assertFalse(gate.matches(340));
		}
	}

	@Test
	public void crossingsRequireTheirExactDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : supportedRows())
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
