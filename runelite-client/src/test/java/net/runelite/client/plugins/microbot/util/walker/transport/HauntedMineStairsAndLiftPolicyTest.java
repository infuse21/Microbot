package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HauntedMineStairsAndLiftPolicyTest
{
	private static final Set<Integer> OBJECT_IDS = Set.of(4940, 4942, 4971);

	@Test
	public void exactPostQuestRowsUseCatalogTransitionOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(10, rows.size());
		assertEquals(5, rows.stream().filter(row -> row.getObjectId() == 4971).count());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 4942).count());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 4940).count());
		assertTrue(rows.stream().allMatch(row -> row.isMembers()
			&& row.getDuration() == 1 && !row.isConsumable()
			&& row.getCurrencyAmount() == 0
			&& row.getQuests().equals(Map.of(Quest.HAUNTED_MINE, QuestState.FINISHED))
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0)
			&& CatalogTransitionPolicy.isHauntedMineStairsOrLift(row)
			&& CatalogTransitionPolicy.isEligible(row)));
		assertTrue(rows.stream().filter(row -> row.getObjectId() == 4971)
			.allMatch(row -> row.getItemIdRequirements().equals(Set.of(Set.of(4075)))));
		assertTrue(rows.stream().filter(row -> row.getObjectId() != 4971)
			.allMatch(row -> row.getItemIdRequirements().isEmpty()));
	}

	@Test
	public void actionAndQuestMutationsStayLegacyOwned()
	{
		Transport action = rows().stream().filter(row -> row.getObjectId() == 4940)
			.findFirst().orElseThrow(AssertionError::new);
		action.setAction("Use");
		assertFalse(CatalogTransitionPolicy.isHauntedMineStairsOrLift(action));
		assertFalse(CatalogTransitionPolicy.isEligible(action));

		Transport quest = rows().stream().filter(row -> row.getObjectId() == 4971)
			.findFirst().orElseThrow(AssertionError::new);
		quest.getQuests().put(Quest.HAUNTED_MINE, QuestState.IN_PROGRESS);
		assertFalse(CatalogTransitionPolicy.isHauntedMineStairsOrLift(quest));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> OBJECT_IDS.contains(row.getObjectId()))
			.filter(row -> row.getName().equals("Stairs") || row.getName().equals("Lift"))
			.filter(row -> row.getObjectId() != 4971
				|| row.getItemIdRequirements().equals(Set.of(Set.of(4075))))
			.collect(Collectors.toList());
	}
}
