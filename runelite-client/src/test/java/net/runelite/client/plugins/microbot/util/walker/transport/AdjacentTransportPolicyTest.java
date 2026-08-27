package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdjacentTransportPolicyTest
{
	private static final WorldPoint A = new WorldPoint(100, 100, 0);
	private static final WorldPoint B = new WorldPoint(101, 100, 0);

	@Test
	public void acceptsDirectObjectBackedAdjacentShortcut()
	{
		Transport stile = transport(A, B, "Climb-over", TransportType.AGILITY_SHORTCUT);
		assertTrue(AdjacentTransportPolicy.isEligible(stile));
	}

	@Test
	public void acceptsDirectedTwoTileStrongholdTreeDoor()
	{
		Transport door = transport(A, new WorldPoint(100, 102, 0), "Open", "Tree Door",
			TransportType.TRANSPORT);

		assertTrue(AdjacentTransportPolicy.isEligible(door));
	}

	@Test
	public void doesNotBroadenTwoTileOpenObjectsBeyondStrongholdTreeDoor()
	{
		WorldPoint twoTilesAway = new WorldPoint(100, 102, 0);

		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, twoTilesAway, "Open", "Door", TransportType.TRANSPORT)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, twoTilesAway, "Open", "Tree Door", TransportType.AGILITY_SHORTCUT)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, new WorldPoint(100, 103, 0), "Open", "Tree Door",
				TransportType.TRANSPORT)));
	}

	@Test
	public void rejectsDialogueAndLaterTransportFamilies()
	{
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, B, "Pay-toll(10gp)", TransportType.TRANSPORT)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, B, "Pick-lock", TransportType.TRANSPORT)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, B, "Talk-to", TransportType.NPC)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, B, "Slash", TransportType.TRANSPORT)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, new WorldPoint(102, 100, 0), "Squeeze-through",
				TransportType.AGILITY_SHORTCUT)));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, new WorldPoint(101, 100, 1), "Climb-up", TransportType.TRANSPORT)));
	}

	@Test
	public void identifiesActionsWhoseObjectTransformationProvesClearance()
	{
		assertTrue(AdjacentTransportPolicy.actionClearsObject("Open"));
		assertTrue(AdjacentTransportPolicy.actionClearsObject("Walk-through"));
		assertFalse(AdjacentTransportPolicy.actionClearsObject("Climb-over"));
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination, String action,
		TransportType type)
	{
		return transport(origin, destination, action, "object", type);
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination, String action,
		String name, TransportType type)
	{
		return new Transport(origin, destination, "test", type, false, action, name, 123);
	}
}
