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
import static org.junit.Assert.assertTrue;

public class WildernessDitchPolicyTest
{
	@Test
	public void acceptsAllExactCatalogRows()
	{
		List<Transport> ditches = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(WildernessDitchPolicy::isEligible)
			.collect(Collectors.toList());

		assertEquals(670, ditches.size());
		assertTrue(ditches.stream().allMatch(candidate -> candidate.getObjectId() == 23271));
	}

	@Test
	public void rejectsWrongIdentityGeometryAndRequirements()
	{
		WorldPoint origin = new WorldPoint(2995, 3530, 0);
		WorldPoint destination = new WorldPoint(2998, 3530, 0);
		assertFalse(WildernessDitchPolicy.isEligible(transport(origin, destination,
			"Cross", "Wilderness Ditch", 23272)));
		assertFalse(WildernessDitchPolicy.isEligible(transport(origin, destination,
			"Pass", "Wilderness Ditch", 23271)));
		assertFalse(WildernessDitchPolicy.isEligible(transport(origin, destination,
			"Cross", "Ditch", 23271)));
		assertFalse(WildernessDitchPolicy.isEligible(transport(origin, origin.dx(2),
			"Cross", "Wilderness Ditch", 23271)));
		Transport required = transport(origin, destination, "Cross", "Wilderness Ditch", 23271);
		required.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(WildernessDitchPolicy.isEligible(required));
	}

	@Test
	public void liveIdentityRequiresExactObjectNameActionAndOrigin()
	{
		Transport ditch = realDitch();
		assertTrue(WildernessDitchPolicy.isLiveObjectMatch(ditch, 23271,
			"Wilderness Ditch", Arrays.asList("Cross", "Examine"),
			ditch.getOrigin().dx(1)));
		assertFalse(WildernessDitchPolicy.isLiveObjectMatch(ditch, 23272,
			"Wilderness Ditch", Collections.singletonList("Cross"), ditch.getOrigin()));
		assertFalse(WildernessDitchPolicy.isLiveObjectMatch(ditch, 23271,
			"Ditch", Collections.singletonList("Cross"), ditch.getOrigin()));
		assertFalse(WildernessDitchPolicy.isLiveObjectMatch(ditch, 23271,
			"Wilderness Ditch", Collections.singletonList("Pass"), ditch.getOrigin()));
		assertFalse(WildernessDitchPolicy.isLiveObjectMatch(ditch, 23271,
			"Wilderness Ditch", Collections.singletonList("Cross"),
			ditch.getOrigin().dx(3)));
	}

	private static Transport realDitch()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(WildernessDitchPolicy::isEligible)
			.findFirst().orElseThrow(AssertionError::new);
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String action, String name, int objectId)
	{
		return new Transport(origin, destination, "test", TransportType.TRANSPORT,
			false, action, name, objectId);
	}
}
