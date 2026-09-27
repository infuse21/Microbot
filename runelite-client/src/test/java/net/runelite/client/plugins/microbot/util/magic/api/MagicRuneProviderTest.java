package net.runelite.client.plugins.microbot.util.magic.api;

import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.Rs2Tome;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MagicRuneProviderTest
{
	@Test
	public void staffProvidesBothElementsForCombinationRunes()
	{
		assertTrue(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.AIR));
		assertTrue(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.EARTH));
		assertTrue(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.DUST));
		assertFalse(Rs2Staff.STAFF_OF_AIR.provides(Runes.DUST));
		assertFalse(Rs2Staff.DUST_BATTLESTAFF.provides(Runes.LAW));
		assertFalse(Rs2Staff.DUST_BATTLESTAFF.provides(null));
		assertFalse(Rs2Staff.NONE.provides(Runes.AIR));
	}

	@Test
	public void tomeProvidesOnlyItsSupportedElement()
	{
		assertTrue(Rs2Tome.TOME_OF_FIRE.provides(Runes.FIRE));
		assertFalse(Rs2Tome.TOME_OF_FIRE.provides(Runes.WATER));
		assertFalse(Rs2Tome.TOME_OF_FIRE.provides(Runes.LAVA));
		assertFalse(Rs2Tome.TOME_OF_FIRE.provides(null));
		assertFalse(Rs2Tome.NONE.provides(Runes.FIRE));
	}
}
