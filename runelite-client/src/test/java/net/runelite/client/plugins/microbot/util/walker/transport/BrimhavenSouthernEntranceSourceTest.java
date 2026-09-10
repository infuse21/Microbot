package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrimhavenSouthernEntranceSourceTest
{
	@Test
	public void allFiveDirectedRowsRequireThePermanentUnlockWithoutAnotherFare()
	{
		List<Transport> rows = rows();
		assertEquals(5, rows.size());
		assertEquals(Set.of("2761,3062>2734,9478|66", "2761,3063>2734,9478|66",
			"2760,3064>2734,9478|66", "2760,3061>2734,9478|66", "2734,9478>2760,3061|30201"),
			rows.stream().map(row -> point(row.getOrigin()) + ">" + point(row.getDestination())
				+ "|" + row.getObjectId()).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertFalse(Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			assertTrue(row.isMembers());
			assertEquals(0, row.getDuration());
			assertEquals(0, row.getCurrencyAmount());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertEquals(2, row.getVarbits().size());
			for (int state = -1; state <= 4; state++)
			{
				int value = state;
				assertEquals(state >= 1 && state <= 3,
					row.getVarbits().stream().allMatch(gate -> gate.getVarbitId() == 5629 && gate.matches(value)));
			}
		}
	}

	@Test
	public void missingUnlockBoundsAndForeignGeometryCannotGainOwnership()
	{
		for (Transport row : rows())
		{
			Transport foreign = new Transport(row.getOrigin(), new WorldPoint(1, 1, 0), "",
				TransportType.TRANSPORT, true, row.getAction(), row.getName(), row.getObjectId());
			foreign.getVarbits().addAll(row.getVarbits());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
			row.getVarbits().removeIf(gate -> gate.getValue() == 4);
			assertFalse(CatalogTransitionPolicy.isEligible(row));
			row.getVarbits().clear();
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void neitherDisappearanceNorNearbyLandingCompletesEitherDirection()
	{
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE, scanner.observePending(pending,
				new WorldPoint(row.getDestination().getX() + 1, row.getDestination().getY(), 0),
				edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 6).getStatus());
		}
	}

	static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getObjectId() == 66 || row.getObjectId() == 30201)
			.collect(Collectors.toList());
	}

	private static String point(WorldPoint point)
	{
		assertEquals(0, point.getPlane());
		return point.getX() + "," + point.getY();
	}
}
