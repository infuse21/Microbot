package net.runelite.client.plugins.microbot.util.leaguetransport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Rs2MapOfAlacrityTransportTest
{
	@Test
	public void parsesRegionAndDestination()
	{
		Rs2MapOfAlacrityTransport.Destination parsed =
			Rs2MapOfAlacrityTransport.parseDestination(
				"Map of Alacrity: Asgarnia - Chaos Temple Stepping Stone");

		assertEquals("Asgarnia", parsed.region);
		assertEquals("Chaos Temple Stepping Stone", parsed.name);
		assertNull(Rs2MapOfAlacrityTransport.parseDestination(
			"Map of Alacrity: Asgarnia"));
	}

	@Test
	public void widgetMatchingIgnoresMarkupPunctuationAndCase()
	{
		assertTrue(Rs2MapOfAlacrityTransport.normalizedTextContainsAllTokens(
			"<col=ff0000>[A] Chaos Temple: Stepping Stone</col>",
			"Chaos Temple Stepping Stone"));
		assertFalse(Rs2MapOfAlacrityTransport.normalizedTextContainsAllTokens(
			"Falador wall", "Chaos Temple"));
	}

	@Test
	public void parsesAndComputesMenuHotkeys()
	{
		assertEquals(Character.valueOf('1'), Rs2MapOfAlacrityTransport.extractHotkey("[1] Asgarnia"));
		assertEquals(Character.valueOf('A'), Rs2MapOfAlacrityTransport.extractHotkey("a. Karamja"));
		assertEquals(Character.valueOf('9'), Rs2MapOfAlacrityTransport.indexToHotkey(8));
		assertEquals(Character.valueOf('A'), Rs2MapOfAlacrityTransport.indexToHotkey(9));
		assertEquals(Character.valueOf('Z'), Rs2MapOfAlacrityTransport.indexToHotkey(34));
		assertNull(Rs2MapOfAlacrityTransport.indexToHotkey(35));
	}
}
