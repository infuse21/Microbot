package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy.TransportRequirementPolicy;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EquippedSafetyTransitionTest
{
	private static final Set<Integer> IDS = Set.of(3748, 5015, 6279);

	@Test
	public void exactTenRowsCarryTheirAccessAndEquipmentContracts()
	{
		List<Transport> rows = rows();
		assertEquals(10, rows.size());
		assertEquals(2, count(rows, 3748));
		assertEquals(4, count(rows, 5015));
		assertEquals(4, count(rows, 6279));
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(EquippedSafetyTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));

		for (Transport rocks : withId(rows, 3748))
		{
			assertEquals(Map.of(Quest.TROLL_STRONGHOLD, QuestState.IN_PROGRESS),
				rocks.getQuests());
			assertEquals(Set.of(Set.of(3105)),
				TransportRequirementPolicy.itemIdRequirements(rocks));
			assertFalse(TransportRequirementPolicy.itemIdRequirements(rocks)
				.stream().flatMap(Collection::stream).anyMatch(itemId -> itemId == 3107));
			assertEquals(15, rocks.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertEquals(1, rocks.getDuration());
		}

		for (Transport slope : withId(rows, 5015))
		{
			assertEquals(Map.of(Quest.TROLL_ROMANCE, QuestState.FINISHED), slope.getQuests());
			assertEquals(Set.of(Set.of(4084)),
				TransportRequirementPolicy.itemIdRequirements(slope));
			assertEquals(slope.getDestination().getY() == 3794 ? 18 : 30,
				slope.getDuration());
		}
	}

	@Test
	public void smokeEntranceRequiresFaceProtectionButNoLightSource()
	{
		for (Transport well : withId(rows(), 6279))
		{
			assertEquals(Map.of(Quest.DESERT_TREASURE_I, QuestState.IN_PROGRESS),
				well.getQuests());
			assertTrue(well.getItemIdRequirements().isEmpty());
			Set<Set<Integer>> requirements =
				TransportRequirementPolicy.itemIdRequirements(well);
			assertEquals(1, requirements.size());
			Set<Integer> protection = requirements.iterator().next();
			assertEquals(62, protection.size());
			assertTrue(protection.containsAll(Set.of(1506, 4164, 11864, 11865)));
			assertFalse(protection.contains(33)); // Candle
			assertFalse(protection.contains(4531)); // Unlit candle lantern
		}
	}

	@Test
	public void equipmentPreparationPrecedesTheObjectCommand()
	{
		Transport slope = withId(rows(), 5015).get(0);
		assertNull(Rs2CatalogTransitionScene.safetyEquipmentPreparation(
			slope, true, true, -1));
		assertNull(Rs2CatalogTransitionScene.safetyEquipmentPreparation(
			slope, false, true, -1));

		CatalogTransition open = Rs2CatalogTransitionScene.safetyEquipmentPreparation(
			slope, false, false, 4084);
		assertEquals(EquippedSafetyTransitionPolicy.OPEN_INVENTORY, open.getAction());

		CatalogTransition equip = Rs2CatalogTransitionScene.safetyEquipmentPreparation(
			slope, false, true, 4084);
		assertEquals(4084,
			EquippedSafetyTransitionPolicy.equipmentItemId(equip.getAction()));
	}

	@Test
	public void completionRequiresTheExactDirectedLanding()
	{
		Transport slope = withId(rows(), 5015).get(0);
		RouteInteraction pending = new RouteInteraction(1, 0, slope.getOrigin(),
			slope.getDestination(), slope.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
			RouteInteraction.Status.AVAILABLE, slope.getAction(), true, slope.getObjectId(),
			slope.getOrigin(), slope.getDestination());
		CatalogTransition transition = new CatalogTransition(null, slope.getOrigin(),
			slope.getObjectId(), slope.getAction(), slope.getAction(), slope.getOrigin(),
			slope.getDestination());
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		WorldPoint near = new WorldPoint(slope.getDestination().getX() + 1,
			slope.getDestination().getY(), slope.getDestination().getPlane());

		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(pending, near, edge -> transition, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, slope.getDestination(), edge -> transition, 13)
				.getStatus());
	}

	@Test
	public void mutatedSourceShapeRemainsLegacyOwned()
	{
		Transport rocks = withId(rows(), 3748).get(0);
		rocks.getQuests().clear();
		assertFalse(EquippedSafetyTransitionPolicy.isEligible(rocks));
		assertFalse(CatalogTransitionPolicy.isEligible(rocks));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.filter(EquippedSafetyTransitionPolicy::isEligible)
			.collect(Collectors.toList());
	}

	private static List<Transport> withId(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId)
			.collect(Collectors.toList());
	}

	private static long count(List<Transport> rows, int objectId)
	{
		return rows.stream().filter(row -> row.getObjectId() == objectId).count();
	}
}
