package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedWaterAndBalanceTransportTest
{
	private static final Set<Integer> MIGRATED_IDS = Set.of(
		10283, 1597, 1996, 20882, 20884, 21738, 21739);

	@Test
	public void exactRowsHaveDirectedRequirementsAndEngineOwnership()
	{
		List<Transport> rows = ordinaryRows(MIGRATED_IDS);

		assertEquals(9, rows.size());
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		for (Transport row : rows)
		{
			assertTrue(row.toString(), CatalogTransitionPolicy.isAuditedWaterAndBalance(row));
		}
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 21738)
			.filter(row -> onlyAgility(row, 12)).count());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 20882)
			.filter(row -> onlyAgility(row, 30)).count());
		assertEquals(7, rows.stream().filter(row -> onlyAgility(row, 0)).count());
	}

	@Test
	public void mutatedRowsRemainLegacyOwned()
	{
		Transport wrongRequirement = transport(new WorldPoint(2649, 9562, 0),
			new WorldPoint(2647, 9557, 0), "Jump-from", "Stepping stone", 21738);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongRequirement));

		Transport wrongLanding = transport(new WorldPoint(2512, 3476, 0),
			new WorldPoint(2526, 3413, 0), "Swim", "River", 10283);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongLanding));

		Transport wrongAction = transport(new WorldPoint(2576, 9631, 0),
			new WorldPoint(2575, 9631, 0), "Open", "Wall", 1597);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongAction));

		Transport wrongType = new Transport(new WorldPoint(2512, 3476, 0),
			new WorldPoint(2527, 3413, 0), "", TransportType.AGILITY_SHORTCUT,
			true, "Swim", "River", 10283);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongType));
	}

	@Test
	public void washDownRequiresExactLandingForCompletion()
	{
		WorldPoint origin = new WorldPoint(2512, 3476, 0);
		WorldPoint destination = new WorldPoint(2527, 3413, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, origin, destination, origin,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Swim", true, 10283, origin, destination);
		CatalogTransition stage = new CatalogTransition(null, origin, 10283,
			"Swim", "Swim", origin, destination);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();

		assertEquals(RouteInteraction.Status.AVAILABLE, scanner.observePending(pending,
			new WorldPoint(2527, 3414, 0), edge -> stage, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED, scanner.observePending(pending,
			destination, edge -> stage, 13).getStatus());
	}

	@Test
	public void hazardousRowsUseGuardedExactLandingOwnership()
	{
		List<Transport> rows = ordinaryRows(Set.of(412, 2234, 25274));
		assertEquals(4, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isAuditedHazardTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
	}

	private static List<Transport> ordinaryRows(Set<Integer> ids)
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> ids.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	private static boolean onlyAgility(Transport transport, int agility)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == Skill.AGILITY.ordinal() ? agility : 0))
			{
				return false;
			}
		}
		return true;
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination, String action,
		String name, int objectId)
	{
		return new Transport(origin, destination, "", TransportType.TRANSPORT,
			true, action, name, objectId);
	}
}
