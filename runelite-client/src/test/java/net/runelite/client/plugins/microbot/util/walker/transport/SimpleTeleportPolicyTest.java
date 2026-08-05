package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SimpleTeleportPolicyTest
{
	private static final WorldPoint DESTINATION = new WorldPoint(3213, 3424, 0);

	@Test
	public void acceptsDirectSpellAndSingleActionItem()
	{
		assertTrue(SimpleTeleportPolicy.isEligible(spell("Varrock Teleport")));
		assertTrue(SimpleTeleportPolicy.isEligible(item("Varrock tablet")));
	}

	@Test
	public void rejectsMenusHomeTeleportAndNonTeleportRows()
	{
		assertFalse(SimpleTeleportPolicy.isEligible(spell("Varrock Teleport: Grand Exchange")));
		assertFalse(SimpleTeleportPolicy.isEligible(spell("Lumbridge Home Teleport")));
		assertFalse(SimpleTeleportPolicy.isEligible(item("Games necklace: Burthorpe")));
		assertFalse(SimpleTeleportPolicy.isEligible(new Transport(null, DESTINATION,
			"walk", TransportType.TRANSPORT, false, 1)));
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
}
