package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2AdjacentTransportSceneTest
{
	@Test
	public void transformedObjectCanMatchCatalogNameAndAction()
	{
		Transport transport = new Transport(new WorldPoint(3267, 3227, 0),
			new WorldPoint(3268, 3227, 0), "test", TransportType.TRANSPORT,
			false, "Open", "Gate", 2786);

		assertTrue(Rs2AdjacentTransportScene.matchesCatalogIdentity("Gate",
			new String[]{"Open", null, "Examine"}, transport));
	}

	@Test
	public void nearbyUnrelatedObjectCannotMatchCatalogTransport()
	{
		Transport transport = new Transport(new WorldPoint(3267, 3227, 0),
			new WorldPoint(3268, 3227, 0), "test", TransportType.TRANSPORT,
			false, "Open", "Gate", 2786);

		assertFalse(Rs2AdjacentTransportScene.matchesCatalogIdentity("Door",
			new String[]{"Open"}, transport));
	}
}
