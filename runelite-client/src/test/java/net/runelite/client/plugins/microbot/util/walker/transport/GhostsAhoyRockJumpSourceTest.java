package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GhostsAhoyRockJumpSourceTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream).filter(row -> row.getObjectId() == 16115)
			.collect(Collectors.toList());
	}

	@Test
	public void allTenExactRockJumpsAreEngineOwnedWithRequirements()
	{
		Set<String> expected = Set.of(
			"3604 3550 0>3602 3550 0", "3602 3550 0>3604 3550 0",
			"3599 3552 0>3597 3552 0", "3597 3552 0>3599 3552 0",
			"3595 3554 0>3595 3556 0", "3595 3556 0>3595 3554 0",
			"3597 3559 0>3597 3561 0", "3597 3561 0>3597 3559 0",
			"3599 3564 0>3601 3564 0", "3601 3564 0>3599 3564 0");
		List<Transport> rows = rows();
		assertEquals(10, rows.size());
		assertEquals(expected, rows.stream().map(row -> point(row.getOrigin()) + ">"
			+ point(row.getDestination())).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertEquals(25, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertTrue(row.isMembers());
			assertEquals(0, row.getDuration());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void runEnergyGateUsesRawClientUnitsWithoutBlocking()
	{
		assertFalse(CatalogTransitionPolicy.hasGhostShipRunEnergy(499));
		assertTrue(CatalogTransitionPolicy.hasGhostShipRunEnergy(500));
	}

	@Test
	public void exactLandingIsRequiredAfterEitherSuccessOrDamagingFailure()
	{
		Transport row = rows().get(0);
		WorldPoint from = row.getOrigin();
		WorldPoint to = row.getDestination();
		RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			row.getAction(), true, row.getObjectId(), from, to);
		CatalogTransition transition = new CatalogTransition(null, from, row.getObjectId(),
			row.getAction(), row.getAction(), from, to);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(pending, new WorldPoint(to.getX() + 1, to.getY(), to.getPlane()),
				edge -> transition, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, to, edge -> null, 13).getStatus());
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
	}
}
