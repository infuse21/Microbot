package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
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

public class CrashSiteOpeningPolicyTest
{
	@Test
	public void exactPostQuestOpeningRowsUseCatalogOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(2, rows.size());
		assertEquals(Set.of("2435,3519,0->1987,5568,0", "1987,5568,0->2435,3519,0"),
			rows.stream().map(CrashSiteOpeningPolicyTest::directedRoute)
				.collect(Collectors.toSet()));
		assertTrue(rows.stream().allMatch(row -> row.getAction().equals("Pass-through")
			&& row.isMembers() && row.getDuration() == 1 && !row.isConsumable()
			&& row.getQuests().equals(Map.of(Quest.MONKEY_MADNESS_II, QuestState.FINISHED))
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0)
			&& CatalogTransitionPolicy.isCrashSiteOpening(row)
			&& CatalogTransitionPolicy.isEligible(row)));
	}

	@Test
	public void nearbyOrMutatedOpeningContractsStayLegacyOwned()
	{
		Transport action = rows().get(0);
		action.setAction("Enter");
		assertFalse(CatalogTransitionPolicy.isCrashSiteOpening(action));
		assertFalse(CatalogTransitionPolicy.isEligible(action));

		Transport destination = new Transport(new WorldPoint(2435, 3519, 0),
			new WorldPoint(1988, 5568, 0), "test", TransportType.TRANSPORT,
			true, "Pass-through", "Opening", 28807);
		destination.getQuests().put(Quest.MONKEY_MADNESS_II, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isCrashSiteOpening(destination));
		assertFalse(CatalogTransitionPolicy.isEligible(destination));

		Transport quest = rows().get(0);
		quest.getQuests().put(Quest.MONKEY_MADNESS_II, QuestState.IN_PROGRESS);
		assertFalse(CatalogTransitionPolicy.isCrashSiteOpening(quest));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> row.getObjectId() == 28807 && row.getName().equals("Opening"))
			.collect(Collectors.toList());
	}

	private static String directedRoute(Transport transport)
	{
		return point(transport.getOrigin()) + "->" + point(transport.getDestination());
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}
}
