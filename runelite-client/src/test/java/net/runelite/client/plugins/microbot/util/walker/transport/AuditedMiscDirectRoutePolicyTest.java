package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedMiscDirectRoutePolicyTest
{
	private static final Set<Integer> SUPPORTED_IDS = Set.of(
		881, 882, 2022, 2447, 10321, 18270, 20790, 28800);

	@Test
	public void exactEightRowsUseCatalogOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(8, rows.size());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 20790).count());
		assertEquals(2, rows.stream().filter(row -> row.getName().equals("Manhole")
			&& row.getOrigin().equals(new WorldPoint(3236, 3458, 0))).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isAuditedMiscDirectRoute));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
	}

	@Test
	public void questSkillAndVarbitContractsAreExact()
	{
		Transport ratPits = row(10321);
		assertTrue(ratPits.isMembers());
		assertEquals(Map.of(Quest.RATCATCHERS, QuestState.IN_PROGRESS), ratPits.getQuests());
		assertTrue(hasVarbit(ratPits, 1404, 104, TransportVarbit.Operator.GREATER_THAN));

		for (int objectId : new int[]{2447, 28800})
		{
			Transport tree = row(objectId);
			assertTrue(tree.isMembers());
			assertEquals(Map.of(Quest.THE_GRAND_TREE, QuestState.IN_PROGRESS), tree.getQuests());
			assertTrue(hasOnlySkill(tree, Skill.AGILITY, 25));
		}

		assertTrue(row(2022).isMembers());
		assertTrue(row(18270).isMembers());
		assertFalse(row(20790).isMembers());
		assertFalse(row(881).isMembers());
		assertFalse(row(882).isMembers());
	}

	@Test
	public void unsafeRowsAreNotLoadedAndTrollRocksRetainTheirGates()
	{
		List<Transport> all = allRows();
		assertTrue(all.stream().noneMatch(row -> row.getObjectId() == 22355));

		List<Transport> trollRocks = all.stream().filter(row -> row.getObjectId() == 3748)
			.filter(row -> row.getName().equals("Rocks")).collect(Collectors.toList());
		assertEquals(14, trollRocks.size());
		assertTrue(trollRocks.stream().allMatch(row -> row.isMembers()
			&& row.getQuests().equals(Map.of(Quest.TROLL_STRONGHOLD, QuestState.IN_PROGRESS))
			&& hasOnlySkill(row, Skill.AGILITY, 15)
			&& CatalogTransitionPolicy.isEligible(row)));
		assertEquals(12, trollRocks.stream()
			.filter(row -> row.getItemIdRequirements().isEmpty()).count());
		assertEquals(2, trollRocks.stream()
			.filter(EquippedSafetyTransitionPolicy::isEligible).count());
	}

	@Test
	public void stagedManholeAdvancesAndEveryRouteNeedsItsDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		Transport closed = row(881);
		Transport open = row(882);
		RouteInteraction pendingManhole = pending(closed);
		CatalogTransition openStage = transition(open);
		RouteInteraction advanced = scanner.observePending(pendingManhole, closed.getOrigin(),
			edge -> openStage, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, advanced.getStatus());
		assertEquals("Climb-down", advanced.getAction());

		for (Transport row : rows())
		{
			RouteInteraction pending = pending(row);
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 13).getStatus());
		}
	}

	@Test
	public void mutatedGateOrSkillIsRejected()
	{
		Transport ratPits = row(10321);
		ratPits.getVarbits().clear();
		assertFalse(CatalogTransitionPolicy.isAuditedMiscDirectRoute(ratPits));

		Transport tree = row(2447);
		tree.getSkillLevels()[Skill.AGILITY.ordinal()] = 24;
		assertFalse(CatalogTransitionPolicy.isAuditedMiscDirectRoute(tree));
	}

	private static List<Transport> rows()
	{
		return allRows().stream().filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> SUPPORTED_IDS.contains(row.getObjectId())).collect(Collectors.toList());
	}

	private static List<Transport> allRows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.collect(Collectors.toList());
	}

	private static Transport row(int objectId)
	{
		return rows().stream().filter(row -> row.getObjectId() == objectId).findFirst().orElseThrow();
	}

	private static RouteInteraction pending(Transport row)
	{
		return new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(), row.getOrigin(),
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
	}

	private static CatalogTransition transition(Transport row)
	{
		return new CatalogTransition(null, row.getOrigin(), row.getObjectId(), row.getAction(),
			row.getAction(), row.getOrigin(), row.getDestination());
	}

	private static boolean hasVarbit(Transport row, int id, int value,
		TransportVarbit.Operator operator)
	{
		if (row.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit requirement = row.getVarbits().iterator().next();
		return requirement.getVarbitId() == id && requirement.getValue() == value
			&& requirement.getOperator() == operator;
	}

	private static boolean hasOnlySkill(Transport row, Skill skill, int level)
	{
		int[] levels = row.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == skill.ordinal() ? level : 0))
			{
				return false;
			}
		}
		return true;
	}
}
