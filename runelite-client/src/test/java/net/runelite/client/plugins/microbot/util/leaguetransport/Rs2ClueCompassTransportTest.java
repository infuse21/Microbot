package net.runelite.client.plugins.microbot.util.leaguetransport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class Rs2ClueCompassTransportTest
{
	@Test
	public void extractsInventoryActionDestination()
	{
		assertEquals("b. barbarian village", Rs2ClueCompassTransport.destination(
			"Clue compass: B. Barbarian Village"));
		assertNull(Rs2ClueCompassTransport.destination("Map of Alacrity: Asgarnia - Wall"));
		assertNull(Rs2ClueCompassTransport.destination("Clue compass:"));
	}
}
