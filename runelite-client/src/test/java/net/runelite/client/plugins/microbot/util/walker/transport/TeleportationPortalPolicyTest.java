package net.runelite.client.plugins.microbot.util.walker.transport;

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

public class TeleportationPortalPolicyTest
{
	@Test
	public void acceptsEveryDeterministicCatalogRowAndRejectsAmbiguousGuthixRows()
	{
		List<Transport> portals = portals();
		List<Transport> eligible = portals.stream()
			.filter(TeleportationPortalPolicy::isEligible)
			.collect(Collectors.toList());
		List<Transport> unsupported = portals.stream()
			.filter(candidate -> !TeleportationPortalPolicy.isEligible(candidate))
			.collect(Collectors.toList());

		assertEquals(100, portals.size());
		assertEquals(88, eligible.size());
		assertEquals(12, unsupported.size());
		assertTrue(unsupported.stream().allMatch(candidate ->
			candidate.getObjectId() == 4408
				&& "Guthix Portal".equals(candidate.getName())));
		assertEquals(Map.of("Enter", 39L, "Exit", 18L, "Use", 13L,
			"Ferox Enclave", 8L, "Edgeville", 8L, "Enter-member", 1L,
			"Leave", 1L), eligible.stream().collect(Collectors.groupingBy(
				Transport::getAction, Collectors.counting())));
	}

	@Test
	public void rejectsUnknownTypeIdentityActionAndDisplayShape()
	{
		Transport portal = portals().stream()
			.filter(candidate -> candidate.getObjectId() == 40474)
			.findFirst().orElseThrow();
		Transport wrongType = new Transport(portal.getOrigin(), portal.getDestination(),
			"Soul Wars", TransportType.TRANSPORT, true, "Enter",
			"Soul Wars Portal", 40474);
		Transport wrongObject = new Transport(portal.getOrigin(), portal.getDestination(),
			"Soul Wars", TransportType.TELEPORTATION_PORTAL, true, "Enter",
			"Soul Wars Portal", 999);
		Transport wrongAction = new Transport(portal.getOrigin(), portal.getDestination(),
			"Soul Wars", TransportType.TELEPORTATION_PORTAL, true, "Use",
			"Soul Wars Portal", 40474);
		Transport wrongDisplay = new Transport(portal.getOrigin(), portal.getDestination(),
			"", TransportType.TELEPORTATION_PORTAL, true, "Enter",
			"Soul Wars Portal", 40474);

		assertFalse(TeleportationPortalPolicy.isEligible(wrongType));
		assertFalse(TeleportationPortalPolicy.isEligible(wrongObject));
		assertFalse(TeleportationPortalPolicy.isEligible(wrongAction));
		assertFalse(TeleportationPortalPolicy.isEligible(wrongDisplay));
	}

	@Test
	public void liveIdentityRequiresExactObjectNameActionAndOriginProximity()
	{
		Transport portal = portals().stream()
			.filter(candidate -> candidate.getObjectId() == 40474)
			.findFirst().orElseThrow();

		assertTrue(TeleportationPortalPolicy.isLiveObjectMatch(portal, 40474,
			"Soul Wars Portal", Arrays.asList("Enter", "Examine"),
			portal.getOrigin().dy(1)));
		assertFalse(TeleportationPortalPolicy.isLiveObjectMatch(portal, 40475,
			"Soul Wars Portal", List.of("Enter"), portal.getOrigin()));
		assertFalse(TeleportationPortalPolicy.isLiveObjectMatch(portal, 40474,
			"Portal", List.of("Enter"), portal.getOrigin()));
		assertFalse(TeleportationPortalPolicy.isLiveObjectMatch(portal, 40474,
			"Soul Wars Portal", List.of("Use"), portal.getOrigin()));
		assertFalse(TeleportationPortalPolicy.isLiveObjectMatch(portal, 40474,
			"Soul Wars Portal", List.of("Enter"), portal.getOrigin().dx(5)));
	}

	private static List<Transport> portals()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TELEPORTATION_PORTAL)
			.collect(Collectors.toList());
	}
}
