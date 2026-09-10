package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrimhavenDungeonEntranceSourceTest
{
	static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getObjectId() == 20877).collect(Collectors.toList());
	}

	@Test
	public void allApproachesHaveDisjointPaidTemporaryAndPermanentVariants()
	{
		List<Transport> rows = rows();
		assertEquals(21, rows.size());
		assertEquals(7, rows.stream().map(Transport::getOrigin).distinct().count());
		for (Transport row : rows)
		{
			assertTrue(BrimhavenEntrancePolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertEquals(row.getCurrencyAmount() == 875, Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				"Pay", true, 20877, row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, new WorldPoint(2712, 9564, 0), edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 6).getStatus());
		}
		for (int state = 0; state < 16; state++)
		{
			int paid = state & 1;
			int permanent = (state >> 3) & 1;
			List<Transport> eligible = rows.stream().filter(row -> row.getVarbits().stream()
				.allMatch(bit -> bit.matches(bit.getVarbitId() == 5628 ? paid : permanent)))
				.collect(Collectors.toList());
			assertEquals(7, eligible.size());
			assertEquals(7, eligible.stream().map(Transport::getOrigin).distinct().count());
			assertTrue(eligible.stream().allMatch(row -> row.getCurrencyAmount()
				== (paid == 1 || permanent == 1 ? 0 : 875)));
		}
	}

	@Test
	public void removingAccessPredicatesCannotGainOwnership()
	{
		for (Transport row : rows())
		{
			row.getVarbits().clear();
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}
}
