package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OtherItemTeleportPolicyTest
{
	@Test
	public void wholeAuditedProtocolBatchIsOwned()
	{
		Map<String, Integer> expected = Map.ofEntries(
			Map.entry("Ardougne cloak", 1),
			Map.entry("Book of the dead", 5),
			Map.entry("Drakan's medallion", 2),
			Map.entry("Enchanted lyre", 4),
			Map.entry("Enchanted lyre(i)", 4),
			Map.entry("Eternal teleport crystal", 2),
			Map.entry("Icy basalt", 1),
			Map.entry("Karamja gloves", 2),
			Map.entry("Kharedst's memoirs", 5),
			Map.entry("Morytania legs", 4),
			Map.entry("Pharaoh's sceptre", 3),
			Map.entry("Rada's blessing", 5),
			Map.entry("Stony basalt", 2),
			Map.entry("Teleport crystal", 2),
			Map.entry("Varrock tablet", 1),
			Map.entry("Watchtower tablet", 1));
		int count = 0;
		for (Map.Entry<String, Integer> family : expected.entrySet())
		{
			int rows = 0;
			for (Set<Transport> group : Transport.loadAllFromResources().values())
			{
				for (Transport row : group)
				{
					if (row.getType() == TransportType.TELEPORTATION_ITEM
						&& row.getDisplayInfo().startsWith(family.getKey() + ":")
						&& !row.getDisplayInfo().equals("Ardougne cloak: Farm")
						&& !row.getDisplayInfo().equals("Drakan's medallion: Slepe")
						&& !row.getDisplayInfo().equals("Pharaoh's sceptre: Jaltevas"))
					{
						assertTrue(row.getDisplayInfo(), ItemTeleportPolicy.isEligible(row));
						rows++;
					}
				}
			}
			assertEquals(family.getKey(), family.getValue().intValue(), rows);
			count += rows;
		}
		assertEquals(44, count);
	}

	@Test
	public void exactActionsDoNotDependOnSavedToggleOrInventoryEquipmentAliases()
	{
		assertEquals("Monastery Teleport", ItemTeleportPolicy.inventoryAction(row("Ardougne cloak: Monastery", 13121)));
		assertEquals("Kandarin Monastery", ItemTeleportPolicy.equipmentAction(row("Ardougne cloak: Monastery", 13121)));
		assertEquals("Ectofuntus Pit", ItemTeleportPolicy.equipmentAction(row("Morytania legs: Ecto Teleport", 13112)));
		assertEquals("Burgh de Rott", ItemTeleportPolicy.equipmentAction(row("Morytania legs: Burgh Teleport", 13114)));
		assertEquals("Jatiszo", ItemTeleportPolicy.inventoryAction(row("Enchanted lyre: Jatizso", 3691)));
		assertEquals("Jatiszo", ItemTeleportPolicy.equipmentAction(row("Enchanted lyre(i): Jatizso", 23458)));
		assertEquals("Grand Exchange", ItemTeleportPolicy.inventoryAction(row("Varrock tablet: Grand exchange", 8007)));
		assertEquals("Yanille", ItemTeleportPolicy.inventoryAction(row("Watchtower tablet: Yanille", 8012)));
		assertNull(ItemTeleportPolicy.equipmentAction(row("Teleport crystal: Lletya", 6099)));
		assertNull(ItemTeleportPolicy.equipmentAction(row("Varrock tablet: Grand exchange", 8007)));
	}

	@Test
	public void basaltUsesDirectedDestinationAndRetainsQuestAndRoofRequirements()
	{
		int rows = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getType() != TransportType.TELEPORTATION_ITEM || !row.getDisplayInfo().contains("basalt:"))
				{
					continue;
				}
				assertEquals(QuestState.FINISHED, row.getQuests().get(Quest.MAKING_FRIENDS_WITH_MY_ARM));
				assertNull(ItemTeleportPolicy.equipmentAction(row));
				if (row.getDestination().equals(new WorldPoint(2837, 3695, 0)))
				{
					assertEquals("Troll Stronghold roof", ItemTeleportPolicy.inventoryAction(row));
					assertFalse(row.getVarbits().isEmpty());
				}
				else if (row.getDestination().equals(new WorldPoint(2845, 3694, 0)))
				{
					assertEquals("Troll Stronghold entrance", ItemTeleportPolicy.inventoryAction(row));
				}
				else
				{
					assertEquals("Weiss", ItemTeleportPolicy.inventoryAction(row));
				}
				rows++;
			}
		}
		assertEquals(3, rows);
		assertFalse(ItemTeleportPolicy.isEligible(row("Stony basalt: Troll Stronghold", 22601)));
	}

	@Test
	public void unownedChargeUnlockMapAndInvalidIdProtocolsStayDeferred()
	{
		assertFalse(ItemTeleportPolicy.isEligible(row("Ardougne cloak: Farm", 13122)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Chronicle: Teleport", 13660)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Drakan's medallion: Slepe", 22400)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Pharaoh's sceptre: Jaltevas", 26948)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Quetzal whistle: Hunter Guild", 29271)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Calcified moth: Crush", 29092)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Mokhaiotl waystone: Channel", 31101)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Teleport crystal: Lletya", 6103)));
		assertFalse(ItemTeleportPolicy.isEligible(row("Enchanted lyre: Rellekka", 3690)));
	}

	private static Transport row(String display, int id)
	{
		return new Transport(new WorldPoint(3000, 3000, 0), display, TransportType.TELEPORTATION_ITEM,
			true, 20, Collections.singleton(Collections.singleton(id)));
	}
}
