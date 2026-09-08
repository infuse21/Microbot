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

public class AbyssExitRiftTransportTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 25382 && "Exit-through".equals(row.getAction()))
			.collect(Collectors.toList());
	}

	@Test
	public void elevenDirectRiftsAreOwnedWhileLawAndSoulRemainLocked()
	{
		List<Transport> rows = rows();
		assertEquals(13, rows.size());
		Map<String, Quest> gated = Map.of(
			"Law rift", Quest.TROLL_STRONGHOLD,
			"Death rift", Quest.MOURNINGS_END_PART_II,
			"Blood rift", Quest.SINS_OF_THE_FATHER,
			"Cosmic rift", Quest.LOST_CITY);
		assertEquals(gated.keySet(), rows.stream().filter(row -> !row.getQuests().isEmpty())
			.map(Transport::getName).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			boolean specialised = Set.of("Law rift", "Soul rift").contains(row.getName());
			assertEquals(specialised ? RouteEdge.Kind.TRANSPORT : RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			assertEquals(gated.containsKey(row.getName())
				? Map.of(gated.get(row.getName()), QuestState.FINISHED) : Collections.emptyMap(),
				row.getQuests());
			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void riftRemainsPendingUntilItsDirectedAltarLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			if (Set.of("Law rift", "Soul rift").contains(row.getName()))
			{
				continue;
			}
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition rift = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> rift, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
