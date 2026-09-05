package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnergyBarrierPolicyTest
{
	@Test
	public void deterministicCatalogRowsUseDirectEngineOwnership()
	{
		List<Transport> barriers = barriers();
		List<Transport> eligible = barriers.stream()
			.filter(EnergyBarrierPolicy::isEligible)
			.collect(Collectors.toList());
		assertEquals(16, barriers.size());
		assertEquals(16, eligible.size());
		assertTrue(eligible.stream().allMatch(CatalogTransitionPolicy::isEligible));
	}

	@Test
	public void rejectsWrongIdentityGeometryAndRequirementShape()
	{
		WorldPoint origin = new WorldPoint(3659, 3509, 0);
		WorldPoint destination = new WorldPoint(3659, 3507, 0);
		assertFalse(EnergyBarrierPolicy.isEligible(transport(origin, destination,
			"Pass", "Energy Barrier", 16105)));
		assertFalse(EnergyBarrierPolicy.isEligible(transport(origin, destination,
			"Pay-toll(2-Ecto)", "Barrier", 16105)));
		assertFalse(EnergyBarrierPolicy.isEligible(transport(origin, destination,
			"Pay-toll(2-Ecto)", "Energy Barrier", 16106)));
		assertFalse(EnergyBarrierPolicy.isEligible(transport(origin, origin.dx(1),
			"Pay-toll(2-Ecto)", "Energy Barrier", 16105)));
		Transport required = transport(origin, destination, "Pay-toll(2-Ecto)",
			"Energy Barrier", 16105);
		required.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(EnergyBarrierPolicy.isEligible(required));
	}

	private static List<Transport> barriers()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 16105)
			.filter(candidate -> "Pay-toll(2-Ecto)".equals(candidate.getAction()))
			.filter(candidate -> "Energy Barrier".equals(candidate.getName()))
			.collect(Collectors.toList());
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String action, String name, int objectId)
	{
		return new Transport(origin, destination, "test", TransportType.TRANSPORT,
			false, action, name, objectId);
	}
}
