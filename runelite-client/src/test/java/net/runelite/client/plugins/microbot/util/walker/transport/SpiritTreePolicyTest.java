package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpiritTreePolicyTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3185, 3508, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2461, 3444, 0);

	@Test
	public void acceptsDirectedOverworldTreeAndParsesDestination()
	{
		Transport tree = transport(ORIGIN, DESTINATION, "2: Gnome Stronghold", 1295);

		assertTrue(SpiritTreePolicy.isEligible(tree, null));
		assertEquals("Gnome Stronghold",
			SpiritTreePolicy.destinationName(tree.getDisplayInfo()));
		assertEquals("spirit-tree-destination:Gnome Stronghold",
			SpiritTreePolicy.destinationAction("Gnome Stronghold"));
	}

	@Test
	public void rejectsPohAndMalformedRows()
	{
		WorldPoint poh = new WorldPoint(3200, 3200, 0);
		assertFalse(SpiritTreePolicy.isEligible(
			transport(poh, DESTINATION, "2: Gnome Stronghold", 1295), poh));
		assertFalse(SpiritTreePolicy.isEligible(
			transport(ORIGIN, poh, "C: Your house", 1295), poh));
		assertFalse(SpiritTreePolicy.isEligible(
			transport(ORIGIN, DESTINATION, "", 1295), null));
		assertFalse(SpiritTreePolicy.isEligible(
			transport(ORIGIN, DESTINATION, "2: Gnome Stronghold", 0), null));
	}

	@Test
	public void distinguishesLockedMenuTextFromSelectableDestinations()
	{
		assertFalse(SpiritTreePolicy.isDestinationSelectable(
			"<col=ffffff>7</col>: <col=5f5f5f>Port Sarim</col>", 0xff981f));
		assertFalse(SpiritTreePolicy.isDestinationSelectable("Port Sarim", 0x5f5f5f));
		assertTrue(SpiritTreePolicy.isDestinationSelectable(
			"<col=ffffff>2</col>: Gnome Stronghold", 0xff981f));
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String displayInfo, int objectId)
	{
		return new Transport(origin, destination, displayInfo, TransportType.SPIRIT_TREE,
			true, "Travel", "Spirit Tree", objectId);
	}
}
