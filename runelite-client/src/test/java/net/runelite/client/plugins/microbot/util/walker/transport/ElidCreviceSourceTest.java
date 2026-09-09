package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.questhelper.collections.ItemCollections;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy.TransportRequirementPolicy;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElidCreviceSourceTest
{
	@Test
	public void twelveCanonicalRowsCarryTheConsumedRopeRequirement()
	{
		List<Transport> rows = rows();
		assertEquals(12, rows.size());
		assertEquals(Set.of(
			"3375 2904 0", "3375 2905 0", "3375 2906 0",
			"3374 2902 0", "3374 2903 0", "3374 2907 0",
			"3372 2902 0", "3372 2903 0", "3372 2904 0",
			"3372 2905 0", "3372 2906 0", "3372 2907 0"),
			rows.stream().map(row -> point(row.getOrigin())).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertEquals(new WorldPoint(3374, 9305, 0), row.getDestination());
			assertEquals("Climb-down", row.getAction());
			assertEquals("Crevice", row.getName());
			assertTrue(row.isMembers());
			assertTrue(row.isConsumable());
			assertEquals(1, row.getDuration());
			assertEquals(Set.of(Set.of(ItemID.ROPE)), row.getItemIdRequirements());
			assertEquals(Map.of(Quest.SPIRITS_OF_THE_ELID, QuestState.IN_PROGRESS),
				row.getQuests());
			assertTrue(ElidCrevicePolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void litLightSourcesAreASeparateReusableRequirement()
	{
		Transport row = rows().get(0);
		Set<Integer> expected = Set.copyOf(ItemCollections.LIGHT_SOURCES.getItems());
		assertFalse(expected.isEmpty());
		assertEquals(expected, ElidCrevicePolicy.lightSourceIds(row));
		assertEquals(expected, TransportRequirementPolicy.additionalReusableItemIds(row));
		assertTrue(expected.contains(ItemID.LIT_CANDLE));
		assertFalse(expected.contains(ItemID.ROPE));
	}

	@Test
	public void questStageGateFailsClosedUntilTheCreviceStage()
	{
		Transport row = rows().get(0);
		assertFalse(ElidCrevicePolicy.questStageAvailable(
			row, QuestState.NOT_STARTED, 100));
		assertFalse(ElidCrevicePolicy.questStageAvailable(
			row, QuestState.IN_PROGRESS, 39));
		assertTrue(ElidCrevicePolicy.questStageAvailable(
			row, QuestState.IN_PROGRESS, 40));
		assertTrue(ElidCrevicePolicy.questStageAvailable(
			row, QuestState.FINISHED, 0));
	}

	@Test
	public void descentRequiresTheExactUndergroundLanding()
	{
		Transport row = rows().get(0);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(),
			row.getDestination(), row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
			RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
			row.getOrigin(), row.getDestination());
		CatalogTransition transition = new CatalogTransition(null, row.getOrigin(),
			row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(),
			row.getDestination());
		WorldPoint near = new WorldPoint(3375, 9305, 0);
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(pending, near, edge -> transition, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, row.getDestination(), edge -> transition, 13)
				.getStatus());
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 10416)
			.collect(Collectors.toList());
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
	}
}
