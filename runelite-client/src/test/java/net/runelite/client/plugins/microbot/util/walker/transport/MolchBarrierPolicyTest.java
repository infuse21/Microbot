package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MolchBarrierPolicyTest
{
	static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> MolchBarrierPolicy.ownsObject(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void allTwentyDirectionsRequireExactShapeAndOppositeLanding()
	{
		List<Transport> rows = rows();
		assertEquals(20, rows.size());
		for (Transport row : rows)
		{
			assertTrue(MolchBarrierPolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			WorldPoint anchor = MolchBarrierPolicy.anchor(row.getObjectId());
			assertFalse(MolchBarrierPolicy.hasCrossed(row.getObjectId(), row.getOrigin(), row.getDestination(), anchor));
			assertFalse(MolchBarrierPolicy.hasCrossed(row.getObjectId(), row.getOrigin(), row.getDestination(), row.getOrigin()));
			assertTrue(MolchBarrierPolicy.hasCrossed(row.getObjectId(), row.getOrigin(), row.getDestination(), row.getDestination()));
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
			CatalogTransition transition = new CatalogTransition(null, anchor, row.getObjectId(),
				row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertTrue(scanner.observePending(pending, row.getOrigin(), edge -> transition, 6).isReady());
			assertFalse(scanner.observePending(pending, anchor, edge -> transition, 6).isReady());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> null, 6).getStatus());
			Transport wrongShape = new Transport(anchor, row.getDestination(), "test",
				TransportType.TRANSPORT, true, "Pass", "Mystical barrier", row.getObjectId());
			assertFalse(MolchBarrierPolicy.isEligible(wrongShape));
			row.setItemIdRequirements(Set.of(Set.of(995)));
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void colourAndConservativeNextCrossingDamageHaveClosedBounds()
	{
		for (int state = 0; state <= 10; state++)
		{
			assertEquals(state < 3 ? 34432 : state < 6 ? 34433 : 34434, MolchBarrierPolicy.liveId(state));
			assertEquals(state < 2 ? 0 : state < 5 ? 10 : 20, MolchBarrierPolicy.damageBudget(state));
		}
		for (int state : new int[]{-1, 11, Integer.MAX_VALUE})
		{
			assertEquals(-1, MolchBarrierPolicy.liveId(state));
			assertEquals(-1, MolchBarrierPolicy.damageBudget(state));
			assertEquals("", MolchBarrierPolicy.liveName(state));
		}
	}
}
