package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedUnusualAccessTransportTest
{
	private static final Set<Integer> IDS = Set.of(4918, 6620, 5025, 3771, 3772);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	@Test
	public void allSixSafeRowsHaveExactOwnershipAndMetadata()
	{
		List<Transport> rows = rows();
		assertEquals(6, rows.size());
		assertEquals(2, rows.stream().filter(AdjacentTransportPolicy::isHauntedMineCart).count());
		assertEquals(4, rows.stream().filter(CatalogTransitionPolicy::isAuditedUnusualAccess).count());
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && row.getDuration() == 1));
		assertTrue(rows.stream().allMatch(row -> AdjacentTransportPolicy.isEligible(row)
			|| CatalogTransitionPolicy.isEligible(row)));
	}

	@Test
	public void cartAndQuestRequirementsAreConservative()
	{
		Transport cart = rows().stream().filter(AdjacentTransportPolicy::isHauntedMineCart)
			.findFirst().orElseThrow(AssertionError::new);
		assertFalse(AdjacentTransportPolicy.hasRequiredHauntedMineCartAgility(cart, 14));
		assertTrue(AdjacentTransportPolicy.hasRequiredHauntedMineCartAgility(cart, 15));
		assertEquals(Map.of(Quest.PRIEST_IN_PERIL, QuestState.FINISHED), cart.getQuests());
		assertQuest(6620, Quest.ICTHLARINS_LITTLE_HELPER, QuestState.IN_PROGRESS);
		assertQuest(5025, Quest.TROLL_ROMANCE, QuestState.FINISHED);
		assertQuest(3771, Quest.TROLL_STRONGHOLD, QuestState.FINISHED);
		assertTrue(rows().stream().filter(row -> row.getObjectId() == 3772)
			.findFirst().orElseThrow(AssertionError::new).getQuests().isEmpty());
	}

	private static void assertQuest(int objectId, Quest quest, QuestState state)
	{
		Transport row = rows().stream().filter(transport -> transport.getObjectId() == objectId)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(Map.of(quest, state), row.getQuests());
	}
}
