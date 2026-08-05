package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2AdjacentTransportSceneTest
{
	@Test
	public void transformedObjectCanMatchCatalogNameAndAction()
	{
		ObjectComposition gate = mock(ObjectComposition.class);
		when(gate.getName()).thenReturn("Gate");
		when(gate.getActions()).thenReturn(new String[]{"Open", null, "Examine"});
		Transport transport = new Transport(new WorldPoint(3267, 3227, 0),
			new WorldPoint(3268, 3227, 0), "test", TransportType.TRANSPORT,
			false, "Open", "Gate", 2786);

		assertTrue(Rs2AdjacentTransportScene.matchesCatalogIdentity(gate, transport));
	}

	@Test
	public void nearbyUnrelatedObjectCannotMatchCatalogTransport()
	{
		ObjectComposition door = mock(ObjectComposition.class);
		when(door.getName()).thenReturn("Door");
		when(door.getActions()).thenReturn(new String[]{"Open"});
		Transport transport = new Transport(new WorldPoint(3267, 3227, 0),
			new WorldPoint(3268, 3227, 0), "test", TransportType.TRANSPORT,
			false, "Open", "Gate", 2786);

		assertFalse(Rs2AdjacentTransportScene.matchesCatalogIdentity(door, transport));
	}
}
