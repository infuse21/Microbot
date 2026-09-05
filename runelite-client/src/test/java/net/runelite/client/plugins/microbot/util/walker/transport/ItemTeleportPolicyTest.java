package net.runelite.client.plugins.microbot.util.walker.transport;

import com.google.gson.Gson;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
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
						assertTrue(message, Rs2ItemTeleportScene.hasExactAction(definition.actions,
							definition.actions, definition.subops, ItemTeleportPolicy.inventoryAction(row)));
						String equipmentAction = ItemTeleportPolicy.equipmentAction(row);
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
		assertEquals(161, eligible);
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
		assertFalse(ItemTeleportPolicy.isEligible(row("Max cape: Home", 13280)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Camulet: Enakhra's Temple", 6707)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Hunter cape: Black chinchompa", 9948)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Mythical cape: Teleport", 21913)));
		Transport missingItems = row("Games necklace: Burthorpe", 3853);
		missingItems.getItemIdRequirements().clear();
		assertFalse(ItemTeleportPolicy.isEligible(missingItems));
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

	private static final class Definition
	{
		private int id;
		private String[] actions;
		private String[][] subops;
		private String[] equipment;
	}
}
