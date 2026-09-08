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

public class MeiyerditchPostQuestAccessTest
{
	private static final Set<Integer> IDS = Set.of(17980, 32659, 32660, 39172, 39173);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void tenPostQuestAccessRowsUseExactDirectOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(10, rows.size());
		assertEquals(6, rows.stream().filter(row -> row.getObjectId() == 32659
			|| row.getObjectId() == 32660).count());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 17980).count());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 39172
			|| row.getObjectId() == 39173).count());
		for (Transport row : rows)
		{
			assertTrue(java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0));
			assertEquals(Map.of(Quest.DARKNESS_OF_HALLOWVALE, QuestState.FINISHED),
				row.getQuests());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			Transport foreign = new Transport(new WorldPoint(100, 100, row.getOrigin().getPlane()),
				row.getDestination(), "", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void postQuestAccessRowsRequireExactLanding()
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
