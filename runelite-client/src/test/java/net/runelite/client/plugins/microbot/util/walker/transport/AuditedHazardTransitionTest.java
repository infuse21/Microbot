package net.runelite.client.plugins.microbot.util.walker.transport;

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedHazardTransitionTest
{
	private static final Set<Integer> IDS = Set.of(412, 2020, 2234, 2236, 25274, 3922, 3925);

	@Test
	public void exactRowsHaveCompleteDirectedRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(16, rows.size());
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isAuditedHazardTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));

		Transport well = rows.stream().filter(row -> row.getObjectId() == 2234)
			.findFirst().orElseThrow(AssertionError::new);
		assertTrue(onlyAgility(well, 32));
		assertEquals(QuestState.IN_PROGRESS, well.getQuests().get(Quest.SHILO_VILLAGE));
		assertTrue(onlySkill(rows.stream().filter(row -> row.getObjectId() == 412)
			.findFirst().orElseThrow(AssertionError::new), Skill.HITPOINTS, 16));

		Transport deadTree = only(rows, 2020);
		assertTrue(onlySkill(deadTree, Skill.HITPOINTS, 10));
		assertEquals(QuestState.IN_PROGRESS,
			deadTree.getQuests().get(Quest.WATERFALL_QUEST));

		assertEquals(6, count(rows, 3922));
		assertEquals(4, count(rows, 3925));
		for (Transport trap : rows.stream().filter(row ->
			row.getObjectId() == 3922 || row.getObjectId() == 3925)
			.collect(Collectors.toList()))
		{
			assertEquals(QuestState.IN_PROGRESS, trap.getQuests().get(Quest.REGICIDE));
			assertTrue(onlySkills(trap, Map.of(Skill.AGILITY, 1, Skill.HITPOINTS,
				trap.getObjectId() == 3922 ? 9 : 19)));
		}

		Transport tombRocks = only(rows, 2236);
		assertEquals(QuestState.IN_PROGRESS,
			tombRocks.getQuests().get(Quest.SHILO_VILLAGE));
		assertTrue(onlySkills(tombRocks, Map.of(Skill.AGILITY, 32, Skill.HITPOINTS, 11)));

		for (Transport whirlpool : rows.stream().filter(row -> row.getObjectId() == 25274)
			.collect(Collectors.toList()))
		{
			assertEquals("Dive in", whirlpool.getAction());
			assertEquals(1, whirlpool.getVarbits().size());
			TransportVarbit gate = whirlpool.getVarbits().iterator().next();
			assertEquals(3759, gate.getVarbitId());
			assertEquals(1, gate.getValue());
			assertEquals(TransportVarbit.Operator.GREATER_THAN, gate.getOperator());
		}
	}

	@Test
	public void mutatedRowsRemainLegacyOwned()
	{
		Transport staleWhirlpool = transport(new WorldPoint(2511, 3511, 0),
			new WorldPoint(1768, 5366, 0), "Jump-into", "Whirlpool", 25274);
		assertFalse(CatalogTransitionPolicy.isEligible(staleWhirlpool));

		Transport ungatedWell = transport(new WorldPoint(2762, 2989, 0),
			new WorldPoint(2760, 9389, 0), "Search", "Well stacked rocks", 2234);
		assertFalse(CatalogTransitionPolicy.isEligible(ungatedWell));

		Transport wrongLanding = transport(new WorldPoint(2572, 9499, 0),
			new WorldPoint(2587, 9573, 0), "Pray-at", "Altar", 412);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongLanding));
	}

	@Test
	public void completionRequiresExactHazardLanding()
	{
		WorldPoint origin = new WorldPoint(2572, 9499, 0);
		WorldPoint destination = new WorldPoint(2588, 9573, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, origin, destination, origin,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Pray-at", true, 412, origin, destination);
		CatalogTransition transition = new CatalogTransition(null, origin, 412,
			"Pray-at", "Pray-at", origin, destination);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();

		assertEquals(RouteInteraction.Status.AVAILABLE, scanner.observePending(pending,
			new WorldPoint(2588, 9572, 0), edge -> transition, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED, scanner.observePending(pending,
			destination, edge -> transition, 13).getStatus());
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	private static boolean onlyAgility(Transport transport, int agility)
	{
		return onlySkill(transport, Skill.AGILITY, agility);
	}

	private static boolean onlySkill(Transport transport, Skill skill, int level)
	{
		return onlySkills(transport, Map.of(skill, level));
	}

	private static boolean onlySkills(Transport transport, Map<Skill, Integer> required)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != required.getOrDefault(Skill.values()[i], 0))
			{
				return false;
			}
		}
		return true;
	}

	private static Transport only(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId)
			.findFirst().orElseThrow(AssertionError::new);
	}

	private static long count(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId).count();
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination, String action,
		String name, int objectId)
	{
		return new Transport(origin, destination, "", TransportType.TRANSPORT,
			true, action, name, objectId);
	}
}
