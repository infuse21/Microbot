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

public class UnsupportedPlankAndSticksSourceTest
{
	private static final int PLANK_ROCKS_ID = 15213;

	@Test
	public void allEightRoyalTroublePlankCrossingsAreEngineOwned()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == PLANK_ROCKS_ID)
			.collect(Collectors.toList());
		Set<String> expected = Set.of(
			"2549,10288,0>2547,10288,0", "2547,10288,0>2549,10288,0",
			"2546,10287,0>2544,10287,0", "2544,10287,0>2546,10287,0",
			"2543,10287,0>2541,10287,0", "2541,10287,0>2543,10287,0",
			"2540,10286,0>2538,10286,0", "2538,10286,0>2540,10286,0");

		assertEquals(8, rows.size());
		assertEquals(expected, rows.stream().map(UnsupportedPlankAndSticksSourceTest::routeKey)
			.collect(Collectors.toSet()));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isRoyalTroublePlankCrossing));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && !row.isConsumable()
			&& row.getDuration() == 0
			&& row.getItemIdRequirements().equals(Set.of(Set.of(960)))
			&& row.getQuests().equals(Map.of(Quest.ROYAL_TROUBLE, QuestState.FINISHED))));
		assertTrue(rows.stream().allMatch(row ->
			Rs2CatalogTransitionScene.requiresPlankPreparation(row, "Use")));
		assertTrue(rows.stream().noneMatch(row ->
			Rs2CatalogTransitionScene.requiresPlankPreparation(row, "Look-at")));

		rows.get(0).getItemIdRequirements().clear();
		assertFalse(CatalogTransitionPolicy.isRoyalTroublePlankCrossing(rows.get(0)));

		Transport foreign = new Transport(new WorldPoint(2550, 10288, 0),
			new WorldPoint(2547, 10288, 0), "", TransportType.TRANSPORT, true,
			"Use", "Plank -> Rocks", PLANK_ROCKS_ID);
		foreign.setItemIdRequirements(Set.of(Set.of(960)));
		foreign.getQuests().put(Quest.ROYAL_TROUBLE, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isRoyalTroublePlankCrossing(foreign));
	}

	private static String routeKey(Transport row)
	{
		return point(row.getOrigin()) + ">" + point(row.getDestination());
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}
}
