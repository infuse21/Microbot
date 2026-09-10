package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedStepsAndClimbObstaclePolicyTest
{
	private static final Set<Integer> SUPPORTED_IDS = Set.of(
		8966, 24749, 27362, 29993, 30189, 30190, 33261, 37417, 42009,
		6881, 6882, 60120);

	@Test
	public void exactTwentyThreeRowsUseCatalogOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(23, rows.size());
		assertEquals(12, rows.stream().filter(row -> row.getName().equals("Steps")).count());
		assertEquals(4, rows.stream().filter(row -> row.getName().equals("Handholds")).count());
		assertEquals(3, rows.stream().filter(row -> row.getName().equals("Crushed barricade")).count());
		assertEquals(2, rows.stream().filter(row -> row.getName().equals("Uneven stone ledges")).count());
		assertEquals(2, rows.stream().filter(row -> row.getName().equals("Crumbling wall")).count());

		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertFalse(row.isConsumable());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertEquals(0, row.getCurrencyAmount());
			assertTrue(row.getVarplayers().isEmpty());
			assertTrue(CatalogTransitionPolicy.isAuditedStepsAndClimbObstacle(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void exactRequirementsAreRetained()
	{
		assertEquals(2, rows().stream().filter(row -> row.getObjectId() == 37417)
			.filter(row -> row.getQuests().equals(
				java.util.Map.of(Quest.THE_FREMENNIK_EXILES, QuestState.FINISHED))).count());
		assertEquals(3, rows().stream().filter(row -> row.getObjectId() == 33261)
			.filter(row -> row.getQuests().equals(
				java.util.Map.of(Quest.MAKING_FRIENDS_WITH_MY_ARM, QuestState.IN_PROGRESS))).count());
		assertEquals(3, rows().stream().filter(row -> row.getObjectId() == 6881
				|| row.getObjectId() == 6882)
			.filter(row -> row.getQuests().equals(
				java.util.Map.of(Quest.ZOGRE_FLESH_EATERS, QuestState.IN_PROGRESS)))
			.filter(AuditedStepsAndClimbObstaclePolicyTest::hasJiggigVarbit).count());
		assertEquals(2, rows().stream().filter(row -> row.getObjectId() == 60120)
			.filter(row -> hasOnlySkill(row, Skill.AGILITY, 83)).count());
		assertEquals(1, rows().stream().filter(row -> row.getObjectId() == 24749)
			.filter(row -> row.getOrigin().getX() == 2972)
			.filter(row -> hasOnlySkill(row, Skill.THIEVING, 58)).count());
	}

	@Test
	public void sourceDisabledRowsCannotBeSelected()
	{
		List<Transport> all = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream).collect(Collectors.toList());
		assertTrue(all.stream().noneMatch(row -> row.getObjectId() == 8729
			&& row.getName().equals("Steps")));
		assertEquals(1, all.stream().filter(row -> row.getObjectId() == 6878)
			.filter(row -> QuestStatePassagePolicy.entry(row) != null).count());
	}

	@Test
	public void mutatedContractStaysLegacyOwned()
	{
		Transport action = rows().get(0);
		action.setAction("Enter");
		assertFalse(CatalogTransitionPolicy.isAuditedStepsAndClimbObstacle(action));
		assertFalse(CatalogTransitionPolicy.isEligible(action));

		Transport varbit = rows().stream().filter(row -> row.getObjectId() == 6881).findFirst().orElseThrow();
		varbit.getVarbits().clear();
		assertFalse(CatalogTransitionPolicy.isAuditedStepsAndClimbObstacle(varbit));

		Transport skill = rows().stream().filter(row -> row.getObjectId() == 60120).findFirst().orElseThrow();
		skill.getSkillLevels()[Skill.AGILITY.ordinal()] = 82;
		assertFalse(CatalogTransitionPolicy.isAuditedStepsAndClimbObstacle(skill));
	}

	@Test
	public void sourceCannotReplaceTheDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> SUPPORTED_IDS.contains(row.getObjectId()))
			.filter(row -> Set.of("Steps", "Handholds", "Crushed barricade",
				"Uneven stone ledges", "Crumbling wall").contains(row.getName()))
			.collect(Collectors.toList());
	}

	private static boolean hasJiggigVarbit(Transport row)
	{
		if (row.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit requirement = row.getVarbits().iterator().next();
		return requirement.getVarbitId() == 496 && requirement.getValue() == 1
			&& requirement.getOperator() == TransportVarbit.Operator.EQUAL;
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
