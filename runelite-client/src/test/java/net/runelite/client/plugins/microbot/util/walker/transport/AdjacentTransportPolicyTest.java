package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
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
	public void acceptsOnlyItemFreeExactAdjacentClimbRocksContract()
	{
		Transport rocks = transport(A, B, "Climb", "Rocks", TransportType.TRANSPORT);
		assertTrue(AdjacentTransportPolicy.isEligible(rocks));
		rocks.setItemIdRequirements(Set.of(Set.of(3105)));
		assertFalse(AdjacentTransportPolicy.isEligible(rocks));
		assertFalse(AdjacentTransportPolicy.isEligible(
			transport(A, B, "Climb", "Climbing rocks", TransportType.TRANSPORT)));
	}

	@Test
	public void acceptsOnlyExactItemFreeFeroxEntryBarriers()
	{
		Transport west = new Transport(A, B, "test", TransportType.TRANSPORT,
			false, "Pass-Through", "Barrier", 39652);
		Transport east = new Transport(A, B, "test", TransportType.TRANSPORT,
			false, "Pass-Through", "Barrier", 39653);

		assertTrue(AdjacentTransportPolicy.isEligible(west));
		assertTrue(AdjacentTransportPolicy.isEligible(east));
		west.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(AdjacentTransportPolicy.isEligible(west));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A, B, "test",
			TransportType.TRANSPORT, false, "Pass-Through", "Barrier", 39651)));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A, B, "test",
			TransportType.TRANSPORT, false, "Pass-Through", "Gate", 39652)));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A, B, "test",
			TransportType.TRANSPORT, false, "Pass-Thru", "Barrier", 39652)));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A,
			new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass-Through", "Barrier", 39652)));
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
	public void acceptsOnlyExactReusableWildernessSwordWebContract()
	{
		WorldPoint twoTilesAway = new WorldPoint(100, 102, 0);
		Set<Set<Integer>> wildernessSwords = Set.of(Set.of(
			ItemID.WILDERNESS_SWORD_EASY, ItemID.WILDERNESS_SWORD_MEDIUM,
			ItemID.WILDERNESS_SWORD_HARD, ItemID.WILDERNESS_SWORD_ELITE));
		Transport web = new Transport(A, twoTilesAway, "test", TransportType.TRANSPORT,
			false, "Slash", "Web", 733);
		web.setItemIdRequirements(wildernessSwords);

		assertTrue(AdjacentTransportPolicy.isEligible(web));
		assertEquals(wildernessSwords, web.getItemIdRequirements());

		Transport wrongObject = new Transport(A, twoTilesAway, "test", TransportType.TRANSPORT,
			false, "Slash", "Web", 734);
		wrongObject.setItemIdRequirements(wildernessSwords);
		assertFalse(AdjacentTransportPolicy.isEligible(wrongObject));

		Transport missingRequirement = new Transport(A, twoTilesAway, "test", TransportType.TRANSPORT,
			false, "Slash", "Web", 733);
		assertFalse(AdjacentTransportPolicy.isEligible(missingRequirement));

		Transport wrongRequirement = new Transport(A, twoTilesAway, "test", TransportType.TRANSPORT,
			false, "Slash", "Web", 733);
		wrongRequirement.setItemIdRequirements(Set.of(Set.of(ItemID.BRONZE_SWORD)));
		assertFalse(AdjacentTransportPolicy.isEligible(wrongRequirement));

		Transport tooFar = new Transport(A, new WorldPoint(100, 103, 0), "test",
			TransportType.TRANSPORT, false, "Slash", "Web", 733);
		tooFar.setItemIdRequirements(wildernessSwords);
		assertFalse(AdjacentTransportPolicy.isEligible(tooFar));
	}

	@Test
	public void acceptsOnlyExactMolchMysticalBarrierContract()
	{
		WorldPoint twoTilesAway = new WorldPoint(100, 102, 0);
		for (int objectId : Set.of(34643, 34644, 34645, 34646))
		{
			assertTrue(AdjacentTransportPolicy.isEligible(new Transport(A, twoTilesAway, "test",
				TransportType.TRANSPORT, false, "Pass", "Mystical barrier", objectId)));
		}

		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A, twoTilesAway, "test",
			TransportType.TRANSPORT, false, "Pass", "Mystical barrier", 34542)));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A, twoTilesAway, "test",
			TransportType.TRANSPORT, false, "Pass", "Barrier", 34643)));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A,
			new WorldPoint(100, 103, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Mystical barrier", 34643)));

		Transport itemGated = new Transport(A, twoTilesAway, "test", TransportType.TRANSPORT,
			false, "Pass", "Mystical barrier", 34643);
		itemGated.setItemIdRequirements(Set.of(Set.of(ItemID.ROPE)));
		assertFalse(AdjacentTransportPolicy.isEligible(itemGated));
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
	public void acceptsOnlyTheFourExactAlKharidTollRows()
	{
		java.util.List<Transport> tolls = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Pay-toll(10gp)".equals(candidate.getAction()))
			.filter(candidate -> "Gate".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(4, tolls.size());
		assertTrue(tolls.stream().allMatch(AdjacentTransportPolicy::isEligible));
		assertFalse(AdjacentTransportPolicy.isEligible(new Transport(A, B, "test",
			TransportType.TRANSPORT, false, "Pay-toll(10gp)", "Gate", 2786)));
	}

	@Test
	public void identifiesActionsWhoseObjectTransformationProvesClearance()
	{
		assertTrue(AdjacentTransportPolicy.actionClearsObject("Open"));
		assertTrue(AdjacentTransportPolicy.actionClearsObject("Walk-through"));
		assertTrue(AdjacentTransportPolicy.actionClearsObject("Slash"));
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
