package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class YanillePickLockDoorTest
{
	private static List<Transport> doors()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT && row.getObjectId() == 11728)
			.collect(Collectors.toList());
	}

	@Test
	public void bothDirectionsRetainLevelAndOneReusableBankedLockpick()
	{
		List<Transport> rows = doors();
		assertEquals(2, rows.size());
		for (Transport row : rows)
		{
			assertTrue(AdjacentTransportPolicy.isEligible(row));
			assertEquals(82, row.getSkillLevels()[Skill.THIEVING.ordinal()]);
			assertEquals(Set.of(Set.of(1523)), row.getItemIdRequirements());
			assertFalse(row.isConsumable());
			assertTrue(Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			assertFalse(AdjacentTransportPolicy.hasRequiredYanillePickLockItemsAndLevel(row, 81, true));
			assertFalse(AdjacentTransportPolicy.hasRequiredYanillePickLockItemsAndLevel(row, 99, false));
			assertTrue(AdjacentTransportPolicy.hasRequiredYanillePickLockItemsAndLevel(row, 82, true));
		}
		assertEquals(Map.of(1523, 1), Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(rows));
	}

	@Test
	public void missingRequirementsAndUnreviewedPickLocksRemainUnsupported()
	{
		Transport row = doors().get(0);
		row.getItemIdRequirements().clear();
		assertFalse(AdjacentTransportPolicy.isEligible(row));
		row = doors().get(0);
		row.getSkillLevels()[Skill.THIEVING.ordinal()] = 0;
		assertFalse(AdjacentTransportPolicy.isEligible(row));
		Transport foreign = new Transport(new WorldPoint(2600, 9481, 0),
			new WorldPoint(2600, 9482, 0), "", TransportType.TRANSPORT, true, "Pick-lock", "Door", 11728);
		assertFalse(AdjacentTransportPolicy.isEligible(foreign));
		List<Transport> ardougne = Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(candidate -> "Pick-lock".equals(candidate.getAction()) && candidate.getObjectId() != 11728)
			.collect(Collectors.toList());
		assertEquals(2, ardougne.size());
		ardougne.forEach(candidate -> assertFalse(AdjacentTransportPolicy.isEligible(candidate)));
	}

	@Test
	public void failedPickDoesNotRetireEdgeButOpenedDoorAllowsForwardCrossing()
	{
		AdjacentTransportRouteScanner scanner = new AdjacentTransportRouteScanner();
		for (Transport row : doors())
		{
			RouteInteraction pending = pending(row);
			AdjacentTransport door = new AdjacentTransport(null, row.getOrigin(), 11728,
				"Pick-lock", row.getOrigin(), row.getDestination());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> door, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getOrigin(), edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> door, 13).getStatus());
		}
	}

	@Test
	public void lostRequirementIsUnavailableNotOpenedDoor()
	{
		Transport row = doors().get(0);
		AdjacentTransportScene unavailable = new AdjacentTransportScene()
		{
			@Override
			public boolean isEnabled(PlannedEdge edge, int id)
			{
				return false;
			}

			@Override
			public AdjacentTransport find(PlannedEdge edge)
			{
				throw new AssertionError("lost requirements must precede opened-object inference");
			}
		};
		assertEquals(RouteInteraction.Status.UNAVAILABLE, new AdjacentTransportRouteScanner()
			.observePending(pending(row), row.getOrigin(), unavailable, 13).getStatus());
	}

	private static RouteInteraction pending(Transport row)
	{
		return new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(), row.getOrigin(),
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Pick-lock", true, row.getObjectId(), row.getOrigin(), row.getDestination());
	}
}
