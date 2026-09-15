package net.runelite.client.plugins.microbot.util.walker.transport;

import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import org.junit.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ItemTeleportPolicyTest
{
	@Test
	public void allAuditedRowsHaveExactInventoryAndEquipmentActions() throws Exception
	{
		Map<Integer, Definition> definitions;
		try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(
			"item-teleport-actions.json"), StandardCharsets.UTF_8))
		{
			definitions = Arrays.stream(new Gson().fromJson(reader, Definition[].class))
				.collect(Collectors.toMap(definition -> definition.id, Function.identity()));
		}
		int eligible = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (!ItemTeleportPolicy.isEligible(row))
				{
					continue;
				}
				eligible++;
				assertFalse(SimpleTeleportPolicy.isEligible(row));
				assertTrue(Rs2WalkerBankingPlanner.requiresBankPlanning(row));
				for (Set<Integer> alternatives : row.getItemIdRequirements())
				{
					for (int id : alternatives)
					{
						Definition definition = definitions.get(id);
						assertNotNull(definition);
						String message = row.getDisplayInfo() + " item=" + id;
						if (row.getDisplayInfo().startsWith("Max cape:"))
						{
							if (id == 13280)
							{
								assertTrue(message, Rs2ItemTeleportScene.hasExactAction(definition.actions,
									definition.actions, definition.subops,
									ItemTeleportPolicy.inventoryAction(row)));
							}
							else
							{
								assertEquals(13342, id);
								if (ItemTeleportPolicy.requiresInventorySurface(row))
								{
									assertNull(ItemTeleportPolicy.equipmentAction(row));
								}
								else
								{
									assertTrue(message, Rs2ItemTeleportScene.hasExactAction(definition.equipment,
										definition.actions, definition.subops,
										ItemTeleportPolicy.equipmentAction(row)));
								}
							}
							continue;
						}
						assertTrue(message, Rs2ItemTeleportScene.hasExactAction(definition.actions,
							definition.actions, definition.subops, ItemTeleportPolicy.inventoryAction(row)));
						String equipmentAction = ItemTeleportPolicy.equipmentAction(row);
						if (ItemTeleportPolicy.requiresInventorySurface(row))
						{
							assertNull(equipmentAction);
							continue;
						}
						if (equipmentAction == null)
						{
							assertFalse(message, Arrays.stream(definition.equipment)
								.anyMatch(value -> value != null && !value.isEmpty()));
						}
						else
						{
							assertTrue(message, Rs2ItemTeleportScene.hasExactAction(definition.equipment,
								definition.actions, definition.subops, equipmentAction));
						}
					}
				}
			}
		}
		assertEquals(339, eligible);
	}

	@Test
	public void burningAmuletRowsUseOnlyTheExactAuditedWildernessContract()
	{
		Map<String, WorldPoint> expected = Map.of(
			"Burning amulet: Chaos Temple", new WorldPoint(3234, 3634, 0),
			"Burning amulet: Bandit Camp", new WorldPoint(3038, 3651, 0),
			"Burning amulet: Lava Maze", new WorldPoint(3028, 3842, 0));
		int rows = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getDisplayInfo() == null || !row.getDisplayInfo().startsWith("Burning amulet:"))
				{
					continue;
				}
				rows++;
				assertEquals(expected.get(row.getDisplayInfo()), row.getDestination());
				assertTrue(ItemTeleportPolicy.isBurningAmulet(row));
				assertTrue(ItemTeleportPolicy.isEligible(row));
				assertEquals(row.getDisplayInfo().split(": ", 2)[1],
					ItemTeleportPolicy.inventoryAction(row));
				assertEquals(ItemTeleportPolicy.inventoryAction(row),
					ItemTeleportPolicy.equipmentAction(row));
			}
		}
		assertEquals(3, rows);
	}

	@Test
	public void allMasterScrollBookRowsHaveOneExactReusableBookRequirement()
	{
		int rows = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getDisplayInfo() == null
					|| !row.getDisplayInfo().startsWith("Master Scroll Book:"))
				{
					continue;
				}
				rows++;
				assertTrue(row.getDisplayInfo(), ItemTeleportPolicy.isEligible(row));
				assertEquals("Open", ItemTeleportPolicy.inventoryAction(row));
				assertTrue(ItemTeleportPolicy.masterScrollBookWidget(row) > 0);
				assertEquals(Collections.singleton(Collections.singleton(21389)),
					row.getItemIdRequirements());
			}
		}
		assertEquals(18, rows);
	}

	@Test
	public void malformedUnknownAndDeferredContractsStayUnsupported()
	{
		assertFalse(ItemTeleportPolicy.isEligible(null));
		assertFalse(ItemTeleportPolicy.isEligible(row("Games necklace: Burthorpe", 1)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Games necklace: Unknown", 3853)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Burning amulet: Lava Maze", 21166)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Max cape: Feldip Hills", 13280)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Camulet: Enakhra's Temple", 6707)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Hunter cape: Black chinchompa", 9948)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Mythical cape: Teleport", 21913)));
		Transport missingItems = row("Games necklace: Burthorpe", 3853);
		missingItems.getItemIdRequirements().clear();
		assertFalse(ItemTeleportPolicy.isEligible(missingItems));
	}

	@Test
	public void mythicalCapeExcludesTheInertPohTrophyVariant()
	{
		int rows = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (!"Mythical cape: Teleport".equals(row.getDisplayInfo()))
				{
					continue;
				}
				rows++;
				assertEquals(Set.of(Set.of(22114), Set.of(24855)), row.getItemIdRequirements());
				assertTrue(ItemTeleportPolicy.isEligible(row));
				assertEquals("Teleport", ItemTeleportPolicy.inventoryAction(row));
				assertEquals("Teleport", ItemTeleportPolicy.equipmentAction(row));
			}
		}
		assertEquals(1, rows);
		assertFalse(ItemTeleportPolicy.isEligible(row("Mythical cape: Teleport", 21913)));
	}

	@Test
	public void maxCapeCatalogContainsNoDuplicateDirectedRows()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getDisplayInfo() != null && row.getDisplayInfo().startsWith("Max cape:"))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(21, rows.size());
		assertEquals(rows.size(), rows.stream().map(row -> row.getDestination() + "|"
			+ row.getDisplayInfo() + "|" + row.getItemIdRequirements()).distinct().count());
	}

	@Test
	public void auditedMaxCapeRowsUseExactInventoryAndDirectWornActions()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> "Max cape: Crafting Guild".equals(row.getDisplayInfo()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(1, rows.size());
		Transport row = rows.get(0);
		assertEquals(new WorldPoint(2931, 3286, 0), row.getDestination());
		assertEquals(Set.of(Set.of(13280), Set.of(13342)), row.getItemIdRequirements());
		assertTrue(ItemTeleportPolicy.isEligible(row));
		assertEquals("Crafting Guild", ItemTeleportPolicy.inventoryAction(row));
		assertEquals("Crafting Guild", ItemTeleportPolicy.equipmentAction(row));

		long maxRowsOwned = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(candidate -> candidate.getDisplayInfo() != null
				&& candidate.getDisplayInfo().startsWith("Max cape:"))
			.filter(ItemTeleportPolicy::isEligible)
			.count();
		assertEquals(21, maxRowsOwned);
		assertEquals(0, Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(candidate -> candidate.getDisplayInfo() != null
				&& candidate.getDisplayInfo().startsWith("Max cape:"))
			.filter(candidate -> !ItemTeleportPolicy.isEligible(candidate)).count());
	}

	@Test
	public void camuletRowsUseExactChargesDiaryAndInventorySubactions()
	{
		java.util.List<Transport> rows = rows("Camulet:");
		assertEquals(2, rows.size());
		for (Transport row : rows)
		{
			assertTrue(ItemTeleportPolicy.isEligible(row));
			assertTrue(ItemTeleportPolicy.requiresInventorySurface(row));
			assertNull(ItemTeleportPolicy.equipmentAction(row));
			assertTrue(hasVarbit(row, 1574, 0, TransportVarbit.Operator.GREATER_THAN));
			assertEquals(row.getDisplayInfo().split(": ", 2)[1], ItemTeleportPolicy.inventoryAction(row));
		}
		Transport entrance = rows.stream().filter(row -> row.getDisplayInfo().endsWith("Entrance"))
			.findFirst().orElseThrow(AssertionError::new);
		assertTrue(hasVarbit(entrance, 4485, 1, TransportVarbit.Operator.EQUAL));
	}

	@Test
	public void hunterAndMaxRowsShareOneExactDailyCounter()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream).filter(ItemTeleportPolicy::isHunterArea).collect(Collectors.toList());
		assertEquals(4, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.getDisplayInfo(), ItemTeleportPolicy.isEligible(row));
			assertEquals(1, row.getVarbits().size());
			assertTrue(hasVarbit(row, 4819, 5, TransportVarbit.Operator.LESS_THAN));
		}
		assertEquals(2, rows.stream().filter(ItemTeleportPolicy::isBlackHunterArea).count());
		assertEquals(2, rows.stream().filter(ItemTeleportPolicy::requiresInventorySurface).count());
	}

	@Test
	public void missingDailyChargeAndDiaryGatesAreRejected()
	{
		Transport hunter = rows("Hunter cape: Black").get(0);
		hunter.getVarbits().clear();
		assertFalse(ItemTeleportPolicy.isEligible(hunter));

		Transport max = rows("Max cape: Feldip").get(0);
		max.getVarbits().clear();
		assertFalse(ItemTeleportPolicy.isEligible(max));

		Transport entrance = rows("Camulet: Enakhra's Temple Entrance").get(0);
		entrance.getVarbits().removeIf(gate -> gate.getVarbitId() == 4485);
		assertFalse(ItemTeleportPolicy.isEligible(entrance));
	}

	@Test
	public void exactLookupRejectsPrefixesUnrelatedParentsAndMissingActions()
	{
		String[] actions = {null, "Wear", "Rub"};
		String[][] subops = {null, null, {"The Outpost", "Eagles' Eyrie"}};
		assertTrue(Rs2ItemTeleportScene.hasExactAction(actions, actions, subops, "The Outpost"));
		assertFalse(Rs2ItemTeleportScene.hasExactAction(actions, actions, subops, "Outpost"));
		assertFalse(Rs2ItemTeleportScene.hasExactAction(new String[]{"Teleport"}, actions,
			subops, "The Outpost"));
		assertFalse(Rs2ItemTeleportScene.hasExactAction(null, null, null, "Teleport"));
		assertFalse(Rs2ItemTeleportScene.hasExactAction(actions, actions, subops, null));
	}

	private static Transport row(String display, int id)
	{
		return new Transport(new WorldPoint(3000, 3000, 0), display, TransportType.TELEPORTATION_ITEM,
			true, 20, Collections.singleton(Collections.singleton(id)));
	}

	private static java.util.List<Transport> rows(String displayPrefix)
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getDisplayInfo() != null && row.getDisplayInfo().startsWith(displayPrefix))
			.collect(Collectors.toList());
	}

	private static boolean hasVarbit(Transport row, int id, int value,
		TransportVarbit.Operator operator)
	{
		return row.getVarbits().stream().anyMatch(gate -> gate.getVarbitId() == id
			&& gate.getValue() == value && gate.getOperator() == operator);
	}

	private static final class Definition
	{
		private int id;
		private String[] actions;
		private String[][] subops;
		private String[] equipment;
	}
}
