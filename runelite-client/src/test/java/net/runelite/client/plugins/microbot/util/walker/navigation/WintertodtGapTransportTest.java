package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WintertodtGapTransportTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 29326).collect(Collectors.toList());
	}

	@Test
	public void allSixExactRowsAreEngineOwnedWithRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(6, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(60, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			row.getSkillLevels()[Skill.MINING.ordinal()] = 1;
			assertFalse(CatalogTransitionPolicy.isEligible(row));
			row.getSkillLevels()[Skill.MINING.ordinal()] = 0;

			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			foreign.getSkillLevels()[Skill.AGILITY.ordinal()] = 60;
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void failedJumpRetainsOwnershipUntilTheExactLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition object = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			WorldPoint between = new WorldPoint((from.getX() + to.getX()) / 2,
				(from.getY() + to.getY()) / 2, from.getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, between, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
