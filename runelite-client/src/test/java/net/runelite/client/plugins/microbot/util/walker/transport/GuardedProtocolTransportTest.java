package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuardedProtocolTransportTest
{
	private static final Set<Integer> IDS = Set.of(5052, 6382, 20822, 2581);

	@Test
	public void exactGuardedRowsAreRestoredWithTheirRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(5, rows.size());
		assertEquals(2, count(rows, 5052));

		for (Transport wall : withId(rows, 5052))
		{
			assertEquals(Map.of(Quest.IN_SEARCH_OF_THE_MYREQUE, QuestState.FINISHED),
				wall.getQuests());
			assertTrue(CatalogTransitionPolicy.isGuardedProtocolRoute(wall));
		}

		Transport root = only(rows, 6382);
		assertEquals(Set.of(Set.of(954)), root.getItemIdRequirements());
		assertTrue(root.isConsumable());
		assertEquals(Map.of(Quest.SPIRITS_OF_THE_ELID, QuestState.IN_PROGRESS),
			root.getQuests());
		assertTrue(Rs2CatalogTransitionScene.requiresRopePreparation(root, "Use rope"));

		Transport tarn = only(rows, 20822);
		assertEquals(Set.of(Set.of(4081, 10588, 12017, 12018)),
			tarn.getItemIdRequirements());
		assertEquals(Map.of(Quest.HAUNTED_MINE, QuestState.FINISHED), tarn.getQuests());

		Transport abyss = only(rows, 2581);
		assertEquals(Map.of(Quest.ENTER_THE_ABYSS, QuestState.FINISHED), abyss.getQuests());
		assertTrue(NpcTransportPolicy.isEligible(abyss));
	}

	@Test
	public void objectProtocolsRequireExactDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows().stream().filter(candidate -> candidate.getObjectId() != 2581)
			.collect(Collectors.toList()))
		{
			RouteInteraction pending = pending(row);
			WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
				row.getDestination().getY(), row.getDestination().getPlane());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, near, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 13).getStatus());
		}
	}

	@Test
	public void missingProtocolRequirementsRejectOwnership()
	{
		Transport root = only(rows(), 6382);
		root.getItemIdRequirements().clear();
		assertFalse(CatalogTransitionPolicy.isGuardedProtocolRoute(root));

		Transport abyss = only(rows(), 2581);
		abyss.getQuests().clear();
		assertFalse(NpcTransportPolicy.isEligible(abyss));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	private static List<Transport> withId(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId)
			.collect(Collectors.toList());
	}

	private static Transport only(List<Transport> rows, int objectId)
	{
		List<Transport> matches = withId(rows, objectId);
		assertEquals(1, matches.size());
		return matches.get(0);
	}

	private static long count(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId).count();
	}

	private static RouteInteraction pending(Transport row)
	{
		return new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(), row.getOrigin(),
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
	}
}
