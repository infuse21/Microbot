package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

public class FortisColosseumEntranceTransportTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 50749)
			.collect(Collectors.toList());
	}

	@Test
	public void bothQuestGatedEntrancesUseNavigationEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(2, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(8, row.getDuration());
			assertEquals(Map.of(Quest.CHILDREN_OF_THE_SUN, QuestState.FINISHED), row.getQuests());
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
		}
	}

	@Test
	public void exactLobbyLandingClearsTheEntrance()
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
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, new WorldPoint(1798, 9506, 0), edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> object, 13).getStatus());
		}
	}

	@Test
	public void incompleteOrMutatedEntranceStaysLegacy()
	{
		Transport missingQuest = rows().get(0);
		missingQuest.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isEligible(missingQuest));

		Transport wrongAction = rows().get(1);
		wrongAction.setAction("Open");
		assertFalse(CatalogTransitionPolicy.isEligible(wrongAction));
	}
}
