package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CatalogTransitionPolicyTest
{
	private static final WorldPoint SURFACE = new WorldPoint(100, 100, 0);
	private static final WorldPoint UPSTAIRS = new WorldPoint(100, 100, 1);

	@Test
	public void acceptsDirectStairsLaddersTrapdoorsCavesAndGangplanks()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Climb-up", "Ladder")));
		assertTrue(CatalogTransitionPolicy.isEligible(
			transport(UPSTAIRS, SURFACE, "Climb-down", "Trapdoor")));
		assertTrue(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(200, 200, 0), "Enter", "Cave entrance")));
		assertTrue(CatalogTransitionPolicy.isEligible(transport(
			new WorldPoint(3041, 3199, 1), new WorldPoint(3041, 3202, 0),
			"Cross", "Gangplank")));
	}

	@Test
	public void rejectsAdjacentDialogueOpenOnlyAndUnrelatedObjects()
	{
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(101, 100, 0), "Climb-up", "Ladder")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Pay-toll", "Stairs")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Open", "Trapdoor")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Climb-up", "Tree")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Cross", "Bridge")));
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String action, String name)
	{
		return new Transport(origin, destination, "test", TransportType.TRANSPORT,
			false, action, name, 123);
	}
}
