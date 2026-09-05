package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MinecartPolicyTest
{
	@Test
	public void acceptsAllGeneratedEdgesFromFortySevenCatalogRows()
	{
		List<Transport> minecarts = minecarts();

		assertEquals(254, minecarts.size());
		assertTrue(minecarts.stream().allMatch(MinecartPolicy::isEligible));
		assertEquals(Map.of(16168, 4L, 7028, 6L, 7029, 2L, 7030, 2L, 28835, 240L),
			minecarts.stream().collect(Collectors.groupingBy(Transport::getObjectId,
				Collectors.counting())));
	}

	@Test
	public void rejectsMalformedOrUnrelatedRows()
	{
		WorldPoint origin = new WorldPoint(3140, 3503, 0);
		WorldPoint destination = new WorldPoint(2909, 10174, 0);
		Transport ordinary = new Transport(origin, destination, "",
			TransportType.TRANSPORT, true, "Travel", "Trapdoor", 16168);
		Transport wrongObject = new Transport(origin, destination, "",
			TransportType.MINECART, true, "Travel", "Trapdoor", 999);

		assertFalse(MinecartPolicy.isEligible(ordinary));
		assertFalse(MinecartPolicy.isEligible(wrongObject));
	}

	@Test
	public void encodedStagesRoundTripEquipmentAndDestination()
	{
		String object = MinecartPolicy.objectAction("Ride", 4151, 8850);
		String destination = MinecartPolicy.destinationAction(
			"A: Port Piscarilius", 4151, 8850);

		assertTrue(MinecartPolicy.isObjectAction(object));
		assertEquals("Ride", MinecartPolicy.objectLiveAction(object));
		assertEquals(4151, MinecartPolicy.originalWeaponId(object));
		assertEquals(8850, MinecartPolicy.originalShieldId(object));
		assertTrue(MinecartPolicy.isDestinationAction(destination));
		assertEquals("A: Port Piscarilius",
			MinecartPolicy.destinationDisplayInfo(destination));
		assertEquals("Port Piscarilius",
			MinecartPolicy.destinationLabel("A: Port Piscarilius"));
	}

	@Test
	public void liveIdentityRequiresExactObjectNameActionAndProximity()
	{
		Transport trapdoor = minecarts().stream()
			.filter(candidate -> candidate.getObjectId() == 16168)
			.findFirst().orElseThrow();

		assertTrue(MinecartPolicy.isLiveObjectMatch(trapdoor, 16168, "Trapdoor",
			Arrays.asList("Travel", "Examine"), trapdoor.getOrigin().dy(1)));
		assertFalse(MinecartPolicy.isLiveObjectMatch(trapdoor, 16169, "Trapdoor",
			List.of("Travel"), trapdoor.getOrigin()));
		assertFalse(MinecartPolicy.isLiveObjectMatch(trapdoor, 16168, "Trapdoor",
			List.of("Open"), trapdoor.getOrigin()));
		assertFalse(MinecartPolicy.isLiveObjectMatch(trapdoor, 16168, "Trapdoor",
			List.of("Travel"), trapdoor.getOrigin().dx(5)));
	}

	@Test
	public void restorationRequiresEveryRememberedHandSlot()
	{
		assertTrue(Rs2MinecartScene.restorationComplete(-1, -1, false, false));
		assertFalse(Rs2MinecartScene.restorationComplete(4151, 8850, true, false));
		assertTrue(Rs2MinecartScene.restorationComplete(4151, 8850, true, true));
	}

	private static List<Transport> minecarts()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.MINECART)
			.collect(Collectors.toList());
	}
}
