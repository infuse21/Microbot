package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedStatefulDirectRoutePolicyTest
{
	private static final Set<Integer> MIGRATED_IDS = Set.of(
		4869, 5167, 8742, 15239, 15242, 21035, 21245);

	@Test
	public void exactFourteenRowsUseStatefulCatalogOwnership()
	{
		List<Transport> rows = rows(MIGRATED_IDS);
		assertEquals(14, rows.size());
		assertEquals(5, count(rows, 8742));
		assertEquals(2, count(rows, 15239));
		assertEquals(1, count(rows, 15242));
		assertEquals(2, count(rows, 21245));
		assertEquals(1, count(rows, 21035));
		assertEquals(1, count(rows, 4869));
		assertEquals(2, count(rows, 5167));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isAuditedStatefulDirectRoute));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
	}

	@Test
	public void sourceDisablesMisidentifiedGateAndOpenOnlyWall()
	{
		assertTrue(rows(Set.of(31967)).isEmpty());
	}

	@Test
	public void lletyaAndMemorialUnlocksAreExact()
	{
		for (Transport tree : rows(Set.of(8742)))
		{
			assertEquals(Map.of(Quest.MOURNINGS_END_PART_I, QuestState.IN_PROGRESS),
				tree.getQuests());
			assertTrue(tree.isMembers());
			assertTrue(tree.getVarbits().isEmpty());
			assertTrue(hasOnlyVarplayer(tree, 517, 1, TransportVarPlayer.Operator.GREATER_THAN));
			assertEquals(5, tree.getDuration());
		}

		for (Transport memorial : rows(Set.of(5167)))
		{
			assertEquals(Map.of(Quest.CREATURE_OF_FENKENSTRAIN, QuestState.IN_PROGRESS),
				memorial.getQuests());
			assertTrue(hasOnlyVarbit(memorial, 192, 1, TransportVarbit.Operator.EQUAL));
			assertEquals(2, memorial.getDuration());
		}
	}

	@Test
	public void liftRepairAndFloorToggleAreBothEncoded()
	{
		for (Transport lift : rows(Set.of(15239, 15242)))
		{
			assertEquals(Map.of(Quest.ROYAL_TROUBLE, QuestState.IN_PROGRESS), lift.getQuests());
			assertEquals(2, lift.getVarbits().size());
			assertTrue(hasVarbit(lift, 2146, 6, TransportVarbit.Operator.GREATER_THAN));
			int expectedFloor = lift.getObjectId() == 15242 ? 1 : 0;
			assertTrue(hasVarbit(lift, 2155, expectedFloor, TransportVarbit.Operator.EQUAL));
			assertEquals(1, lift.getDuration());
		}
	}

	@Test
	public void postQuestAndHangarContractsAreExact()
	{
		for (Transport penguin : rows(Set.of(21035, 21245)))
		{
			assertEquals(Map.of(Quest.COLD_WAR, QuestState.FINISHED), penguin.getQuests());
			assertTrue(penguin.getVarbits().isEmpty());
			assertTrue(penguin.getVarplayers().isEmpty());
		}

		Transport device = rows(Set.of(4869)).get(0);
		assertEquals(Map.of(Quest.MONKEY_MADNESS_I, QuestState.IN_PROGRESS),
			device.getQuests());
		assertEquals(6, device.getDuration());
	}

	@Test
	public void everyRouteRequiresItsExactDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows(MIGRATED_IDS))
		{
			RouteInteraction pending = pending(row);
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 13).getStatus());
		}
	}

	@Test
	public void missingOrMutatedStateGateIsRejected()
	{
		Transport tree = rows(Set.of(8742)).get(0);
		tree.getVarplayers().clear();
		assertFalse(CatalogTransitionPolicy.isAuditedStatefulDirectRoute(tree));

		Transport lift = rows(Set.of(15242)).get(0);
		lift.getVarbits().removeIf(requirement -> requirement.getVarbitId() == 2155);
		assertFalse(CatalogTransitionPolicy.isAuditedStatefulDirectRoute(lift));

		Transport memorial = rows(Set.of(5167)).get(0);
		memorial.getVarbits().clear();
		assertFalse(CatalogTransitionPolicy.isAuditedStatefulDirectRoute(memorial));
	}

	private static long count(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId).count();
	}

	private static List<Transport> rows(Set<Integer> objectIds)
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> objectIds.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	private static RouteInteraction pending(Transport row)
	{
		WorldPoint origin = row.getOrigin();
		return new RouteInteraction(1, 0, origin, row.getDestination(), origin,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			row.getAction(), true, row.getObjectId(), origin, row.getDestination());
	}

	private static boolean hasOnlyVarbit(Transport row, int id, int value,
		TransportVarbit.Operator operator)
	{
		return row.getVarbits().size() == 1 && hasVarbit(row, id, value, operator);
	}

	private static boolean hasVarbit(Transport row, int id, int value,
		TransportVarbit.Operator operator)
	{
		return row.getVarbits().stream().anyMatch(requirement -> requirement.getVarbitId() == id
			&& requirement.getValue() == value && requirement.getOperator() == operator);
	}

	private static boolean hasOnlyVarplayer(Transport row, int id, int value,
		TransportVarPlayer.Operator operator)
	{
		if (row.getVarplayers().size() != 1)
		{
			return false;
		}
		TransportVarPlayer requirement = row.getVarplayers().iterator().next();
		return requirement.getVarplayerId() == id && requirement.getValue() == value
			&& requirement.getOperator() == operator;
	}
}
