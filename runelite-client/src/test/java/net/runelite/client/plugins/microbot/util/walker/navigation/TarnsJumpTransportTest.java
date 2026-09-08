package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TarnsJumpTransportTest
{
	private static List<Transport> jumps()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() >= 20540 && row.getObjectId() <= 20569)
			.collect(Collectors.toList());
	}

	@Test
	public void exactFortySixDirectedJumpsArePublished()
	{
		List<Transport> rows = jumps();
		assertEquals(46, rows.size());
		assertEquals(35, rows.stream().filter(row -> "Pillar".equals(row.getName())).count());
		for (Transport row : rows)
		{
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			Transport foreign = new Transport(new WorldPoint(100, 100, 1), row.getDestination(),
				"", TransportType.TRANSPORT, true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void nearbyPillarsAndFallsAreNotThePlannedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : jumps())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, to,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition object = new CatalogTransition(null, to, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			for (int dx = -2; dx <= 2; dx++)
			{
				for (int dy = -2; dy <= 2; dy++)
				{
					WorldPoint location = new WorldPoint(to.getX() + dx, to.getY() + dy, to.getPlane());
					boolean landed = location.equals(to);
					assertEquals(landed ? RouteInteraction.Status.CLEARED : RouteInteraction.Status.AVAILABLE,
						scanner.observePending(pending, location, edge -> object, 13).getStatus());
					assertEquals(landed ? RouteInteraction.Status.CLEARED : RouteInteraction.Status.UNAVAILABLE,
						scanner.observePending(pending, location, edge -> null, 13).getStatus());
				}
			}
			WorldPoint fall = new WorldPoint(to.getX(), to.getY(), to.getPlane() - 1);
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, fall, edge -> null, 13).getStatus());
		}
	}
}
