package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TeleportationLeverPolicyTest
{
	@Test
	public void acceptsAllSevenCatalogRows()
	{
		List<Transport> levers = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TELEPORTATION_LEVER)
			.collect(Collectors.toList());

		assertEquals(7, levers.size());
		assertTrue(levers.stream().allMatch(TeleportationLeverPolicy::isEligible));
		assertEquals(Arrays.asList(1814, 1815, 1816, 1817, 5959, 5960, 26761),
			levers.stream().map(Transport::getObjectId).sorted().collect(Collectors.toList()));
	}

	@Test
	public void rejectsUnrelatedOrMalformedRows()
	{
		WorldPoint origin = new WorldPoint(3090, 3475, 0);
		WorldPoint destination = new WorldPoint(3154, 3924, 0);
		Transport ordinary = new Transport(origin, destination, "",
			TransportType.TRANSPORT, true, "Pull", "Lever", 26761);
		Transport wrongAction = new Transport(origin, destination, "",
			TransportType.TELEPORTATION_LEVER, true, "Open", "Lever", 26761);

		assertFalse(TeleportationLeverPolicy.isEligible(ordinary));
		assertFalse(TeleportationLeverPolicy.isEligible(wrongAction));
	}

	@Test
	public void liveIdentityRequiresExactIdNameActionAndOrigin()
	{
		Transport lever = realLever();

		assertTrue(TeleportationLeverPolicy.isLiveObjectMatch(lever,
			lever.getObjectId(), "Lever", Arrays.asList("Pull", "Examine"),
			lever.getOrigin().dx(1)));
		assertFalse(TeleportationLeverPolicy.isLiveObjectMatch(lever,
			lever.getObjectId() + 1, "Lever", Collections.singletonList("Pull"),
			lever.getOrigin()));
		assertFalse(TeleportationLeverPolicy.isLiveObjectMatch(lever,
			lever.getObjectId(), "Switch", Collections.singletonList("Pull"),
			lever.getOrigin()));
		assertFalse(TeleportationLeverPolicy.isLiveObjectMatch(lever,
			lever.getObjectId(), "Lever", Collections.singletonList("Open"),
			lever.getOrigin()));
		assertFalse(TeleportationLeverPolicy.isLiveObjectMatch(lever,
			lever.getObjectId(), "Lever", Collections.singletonList("Pull"),
			lever.getOrigin().dx(3)));
	}

	private static Transport realLever()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(TeleportationLeverPolicy::isEligible)
			.findFirst().orElseThrow(AssertionError::new);
	}
}
