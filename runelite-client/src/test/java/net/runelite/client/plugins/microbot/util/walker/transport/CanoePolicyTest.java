package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CanoePolicyTest
{
	@Test
	public void acceptsAllTwentyFiveCatalogRows()
	{
		List<Transport> canoes = canoes();

		assertEquals(25, canoes.size());
		assertTrue(canoes.stream().allMatch(CanoePolicy::isEligible));
		assertEquals(Arrays.asList(12163, 12164, 12165, 12166, 39638),
			canoes.stream().map(Transport::getObjectId).distinct().sorted()
				.collect(Collectors.toList()));
		assertEquals(Set.of("Barbarian Village", "Champions Guild", "Edgeville",
			"Ferox Enclave", "Lumbridge", "Wilderness Pond"),
			canoes.stream().map(Transport::getDisplayInfo).collect(Collectors.toSet()));
	}

	@Test
	public void rejectsMalformedOrUnrelatedRows()
	{
		WorldPoint origin = new WorldPoint(3132, 3510, 0);
		WorldPoint destination = new WorldPoint(3109, 3415, 0);
		Transport ordinary = new Transport(origin, destination, "Barbarian Village",
			TransportType.TRANSPORT, true, "Paddle Canoe", "Canoe Station", 12166);
		Transport wrongAction = new Transport(origin, destination, "Barbarian Village",
			TransportType.CANOE, true, "Travel", "Canoe Station", 12166);

		assertFalse(CanoePolicy.isEligible(ordinary));
		assertFalse(CanoePolicy.isEligible(wrongAction));
	}

	@Test
	public void choosesHighestAvailableCanoeShape()
	{
		assertNull(CanoePolicy.shapeForLevel(11));
		assertEquals("Log canoe", CanoePolicy.shapeForLevel(12));
		assertEquals("Dugout canoe", CanoePolicy.shapeForLevel(27));
		assertEquals("Stable dugout canoe", CanoePolicy.shapeForLevel(42));
		assertEquals("Waka canoe", CanoePolicy.shapeForLevel(57));
	}

	@Test
	public void liveIdentityRequiresExactCatalogObjectAndStationProximity()
	{
		Transport canoe = canoes().get(0);

		assertTrue(CanoePolicy.isLiveObjectMatch(canoe, canoe.getObjectId(),
			"Canoe Station", Arrays.asList("Chop-down", "Examine"),
			canoe.getOrigin().dx(2)));
		assertFalse(CanoePolicy.isLiveObjectMatch(canoe, canoe.getObjectId() + 1,
			"Canoe Station", Collections.singletonList("Chop-down"), canoe.getOrigin()));
		assertFalse(CanoePolicy.isLiveObjectMatch(canoe, canoe.getObjectId(),
			"Tree", Collections.singletonList("Chop-down"), canoe.getOrigin()));
		assertFalse(CanoePolicy.isLiveObjectMatch(canoe, canoe.getObjectId(),
			"Canoe Station", Collections.singletonList("Chop-down"),
			canoe.getOrigin().dx(4)));
	}

	@Test
	public void arrivalDialogueOnlyPublishesKnownExactLines()
	{
		assertEquals("canoe-arrival:You arrive at Lumbridge.",
			CanoePolicy.arrivalAction("You arrive at Lumbridge."));
		assertNull(CanoePolicy.arrivalAction("Click here to continue"));
	}

	private static List<Transport> canoes()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.CANOE)
			.collect(Collectors.toList());
	}
}
