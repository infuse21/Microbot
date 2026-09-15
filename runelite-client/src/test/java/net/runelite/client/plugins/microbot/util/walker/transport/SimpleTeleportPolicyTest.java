package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SimpleTeleportPolicyTest
{
	private static final WorldPoint DESTINATION = new WorldPoint(3213, 3424, 0);

	@Test
	public void ectophialRequiresTheFullVialRatherThanItsUnusableEmptyVariant()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream).filter(row -> "Ectophial".equals(row.getDisplayInfo()))
			.collect(Collectors.toList());
		assertEquals(1, rows.size());
		Transport row = rows.get(0);
		assertEquals(Set.of(Set.of(4251)), row.getItemIdRequirements());
		assertFalse(row.isConsumable());
		assertTrue(DirectItemTeleportPolicy.isEligible(row));
		assertFalse(SimpleTeleportPolicy.isEligible(row));
	}

	@Test
	public void everyEligiblePackagedSpellHasAnUnambiguousSpellbookAndSpriteIdentity()
	{
		int audited = 0;
		for (Set<Transport> rows : Transport.loadAllFromResources().values())
		{
			for (Transport row : rows)
			{
				if (row.getType() != TransportType.TELEPORTATION_SPELL
					|| !SimpleTeleportPolicy.isEligible(row)) continue;
				List<net.runelite.client.plugins.skillcalculator.skills.MagicAction> matches =
					java.util.Arrays.stream(net.runelite.client.plugins.skillcalculator.skills.MagicAction.values())
						.filter(spell -> spell.getName().equalsIgnoreCase(SimpleTeleportPolicy.spellName(row)))
						.collect(Collectors.toList());
				assertEquals(row.getDisplayInfo(), 1, matches.size());
				net.runelite.client.plugins.skillcalculator.skills.MagicAction spell = matches.get(0);
				assertTrue(row.getDisplayInfo(), spell.getSpellbook() != null && spell.getSprite() > 0);
				assertEquals(row.getDisplayInfo(), 1L,
					java.util.Arrays.stream(net.runelite.client.plugins.skillcalculator.skills.MagicAction.values())
						.filter(candidate -> candidate.getSpellbook() == spell.getSpellbook()
							&& candidate.getSprite() == spell.getSprite()).count());
				audited++;
			}
		}
		assertTrue("Packaged spell catalog must not be empty", audited > 0);
	}

	@Test
	public void acceptsDirectSpellButItemsRequireStagedOwnership()
	{
		assertTrue(SimpleTeleportPolicy.isEligible(spell("Varrock Teleport")));
		assertFalse(SimpleTeleportPolicy.isEligible(item("Varrock tablet")));
	}

	@Test
	public void rejectsMenusOtherHomeTeleportsAndNonTeleportRows()
	{
		assertFalse(SimpleTeleportPolicy.isEligible(spell("Varrock Teleport: Grand Exchange")));
		assertFalse(SimpleTeleportPolicy.isEligible(spell("Teleport to House: Outside")));
		assertFalse(SimpleTeleportPolicy.isEligible(item("Games necklace: Burthorpe")));
		assertFalse(SimpleTeleportPolicy.isEligible(new Transport(null, DESTINATION,
			"walk", TransportType.TRANSPORT, false, 1)));
	}

	@Test
	public void acceptsOnlyTheTenPackagedAlternateDestinationSpells()
	{
		List<Transport> alternateSpells = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TELEPORTATION_SPELL)
			.filter(row -> row.getDisplayInfo().contains(":"))
			.collect(Collectors.toList());
		assertEquals(10, alternateSpells.size());
		assertTrue(alternateSpells.stream().allMatch(SimpleTeleportPolicy::isEligible));
		assertEquals(Map.of("Teleport to House: Outside", 8L,
			"Varrock Teleport: Grand Exchange", 1L,
			"Watchtower Teleport: Yanille", 1L), alternateSpells.stream()
			.collect(Collectors.groupingBy(Transport::getDisplayInfo, Collectors.counting())));
		for (Transport row : alternateSpells)
		{
			assertEquals(2, SimpleTeleportPolicy.spellIdentifier(row));
			String display = row.getDisplayInfo();
			assertEquals(display.substring(0, display.indexOf(':')),
				SimpleTeleportPolicy.spellName(row));
			assertEquals(display.substring(display.indexOf(':') + 1).trim().toLowerCase(),
				SimpleTeleportPolicy.spellOption(row));
		}
		assertFalse(SimpleTeleportPolicy.isEligible(new Transport(
			new WorldPoint(2952, 3224, 0), "Teleport to House: Outside",
			TransportType.TELEPORTATION_SPELL, true, 19, Map.of(Skill.MAGIC, 40))));
		Transport missingGate = alternateSpells.stream()
			.filter(row -> "Varrock Teleport: Grand Exchange".equals(row.getDisplayInfo()))
			.findFirst().orElseThrow();
		missingGate.getVarbits().clear();
		assertFalse(SimpleTeleportPolicy.isEligible(missingGate));
	}

	@Test
	public void acceptsOnlyTheExactLongHomeTeleportContract()
	{
		Transport home = spell("Lumbridge Home Teleport");
		assertTrue(SimpleTeleportPolicy.isEligible(home));
		assertTrue(SimpleTeleportPolicy.isLumbridgeHomeTeleport(home));
		assertFalse(SimpleTeleportPolicy.isLumbridgeHomeTeleport(
			spell("Lumbridge Home Teleport: Alternate")));
	}

	@Test
	public void acceptsOnlyExecutableSeasonalRows()
	{
		assertFalse(SimpleTeleportPolicy.isEligible(seasonal(
			"Map of Alacrity: Asgarnia - Falador wall")));
		assertFalse(SimpleTeleportPolicy.isEligible(seasonal(
			"Clue compass: B. Barbarian Village")));
		assertFalse(SimpleTeleportPolicy.isEligible(seasonal("Unknown relic: Somewhere")));
	}

	private static Transport spell(String display)
	{
		return new Transport(DESTINATION, display, TransportType.TELEPORTATION_SPELL,
			false, 19, Map.of(Skill.MAGIC, 1));
	}

	private static Transport item(String display)
	{
		return new Transport(DESTINATION, display, TransportType.TELEPORTATION_ITEM,
			false, 19, Set.of(Collections.singleton(8007)));
	}

	private static Transport seasonal(String display)
	{
		return new Transport(DESTINATION, display, TransportType.SEASONAL_TRANSPORT,
			false, 20, Set.of(Collections.singleton(33233)));
	}
}
