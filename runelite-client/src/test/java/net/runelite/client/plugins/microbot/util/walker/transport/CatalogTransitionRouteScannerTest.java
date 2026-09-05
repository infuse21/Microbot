package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CatalogTransitionRouteScannerTest
{
	private static final WorldPoint A = new WorldPoint(10, 10, 0);
	private static final WorldPoint B = new WorldPoint(10, 10, 1);

	@Test
	public void selectsOnlyPublishedCatalogTransitionEdges()
	{
		RouteInteraction interaction = new CatalogTransitionRouteScanner().scan(
			plan(RouteEdge.Kind.CATALOG_TRANSITION), 0, 1, A,
			edge -> transition("Climb-up"), 13);

		assertEquals(RouteInteraction.Kind.CATALOG_TRANSITION, interaction.getKind());
		assertEquals("Climb-up", interaction.getAction());
		assertTrue(interaction.isReady());
	}

	@Test
	public void destinationArrivalClearsPersistentTransitionObject()
	{
		RouteInteraction pending = interaction("Climb-up");
		RouteInteraction observed = new CatalogTransitionRouteScanner().observePending(
			pending, B, edge -> transition("Climb-up"), 13);

		assertEquals(RouteInteraction.Status.CLEARED, observed.getStatus());
		assertFalse(observed.isReady());
	}

	@Test
	public void closedTrapdoorCanAdvanceFromOpenToClimbStage()
	{
		RouteInteraction observed = new CatalogTransitionRouteScanner().observePending(
			interaction("Open"), A, edge -> transition("Climb-down"), 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, observed.getStatus());
		assertEquals("Climb-down", observed.getAction());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new CatalogTransitionRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, A, edge -> transition("Climb-up"), 13));
	}

	@Test
	public void shortLinksRequireDestinationSideInEveryDirection()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				if (dx == 0 && dy == 0)
				{
					continue;
				}
				for (int length = 1; length <= 4; length++)
				{
					WorldPoint to = new WorldPoint(A.getX() + dx * length,
						A.getY() + dy * length, 0);
					RouteInteraction pending = new RouteInteraction(1, 0, A, to, A,
						RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
						"Climb", true, 123, A, to);
					CatalogTransition stage = new CatalogTransition(null, A, 123, "Climb", "Climb", A, to);
					for (int x = to.getX() - 3; x <= to.getX() + 3; x++)
					{
						for (int y = to.getY() - 3; y <= to.getY() + 3; y++)
						{
							WorldPoint player = new WorldPoint(x, y, 0);
							boolean destinationSide = (x - to.getX()) * dx + (y - to.getY()) * dy >= 0;
							boolean expected = player.distanceTo2D(to) <= 2 && destinationSide;
							assertEquals("from=" + A + " to=" + to + " player=" + player, expected,
								scanner.observePending(pending, player, edge -> stage, 13).getStatus()
									== RouteInteraction.Status.CLEARED);
						}
					}
				}
			}
		}
	}

	@Test
	public void shortLandingUsesCatalogEndpointsNotRawOrObjectTiles()
	{
		WorldPoint to = new WorldPoint(12, 10, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, B, A, to,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Climb", true, 123, A, to);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, A, edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, new WorldPoint(11, 10, 0), edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, to, edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, new WorldPoint(13, 10, 0), edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, new WorldPoint(12, 10, 1), edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, null, edge -> null, 13).getStatus());
	}

	@Test
	public void distantAndCrossPlaneLandingsKeepTheirTolerance()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (WorldPoint to : Arrays.asList(B, new WorldPoint(15, 10, 0), new WorldPoint(1000, 1000, 0)))
		{
			RouteInteraction pending = new RouteInteraction(1, 0, A, to, A,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				"Climb", true, 123, A, to);
			for (int dx = -3; dx <= 3; dx++)
			{
				for (int dy = -3; dy <= 3; dy++)
				{
					WorldPoint player = new WorldPoint(to.getX() + dx, to.getY() + dy, to.getPlane());
					assertEquals(Math.max(Math.abs(dx), Math.abs(dy)) <= 2,
						scanner.observePending(pending, player, edge -> null, 13).getStatus()
							== RouteInteraction.Status.CLEARED);
				}
			}
		}
	}

	@Test
	public void unequalDiagonalRequiresProgressAlongTheWholeDirectedLink()
	{
		WorldPoint to = new WorldPoint(13, 11, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, A, to, A,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Climb", true, 123, A, to);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, new WorldPoint(12, 12, 0), edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, new WorldPoint(14, 10, 0), edge -> null, 13).getStatus());
	}

	@Test
	public void everyEligibleShortCatalogRowRejectsItsOriginAndAcceptsItsLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		int checked = 0;
		for (Set<Transport> rows : Transport.loadAllFromResources().values())
		{
			for (Transport row : rows)
			{
				if (!CatalogTransitionPolicy.isEligible(row)
					|| row.getOrigin().getPlane() != row.getDestination().getPlane()
					|| row.getOrigin().distanceTo2D(row.getDestination()) > 4)
				{
					continue;
				}
				checked++;
				RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
					row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
					row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
				assertEquals(row.getName(), RouteInteraction.Status.UNAVAILABLE,
					scanner.observePending(pending, row.getOrigin(), edge -> null, 13).getStatus());
				assertEquals(row.getName(), RouteInteraction.Status.CLEARED,
					scanner.observePending(pending, row.getDestination(), edge -> null, 13).getStatus());
			}
		}
		assertTrue(checked >= 348);
	}

	private static RouteInteraction interaction(String action)
	{
		return new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			action, true, 123, A, B);
	}

	private static CatalogTransition transition(String action)
	{
		return new CatalogTransition(null, A, 123, action, "Climb-up", A, B);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, A, Collections.singleton(B), Arrays.asList(A, B),
			Arrays.asList(A, B), true,
			Collections.singletonList(new RouteEdge(0, A, B, kind)));
	}
}
