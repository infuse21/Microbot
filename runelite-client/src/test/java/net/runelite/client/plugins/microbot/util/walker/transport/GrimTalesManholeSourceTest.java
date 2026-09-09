package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GrimTalesManholeSourceTest
{
	private static final WorldPoint LANDING = new WorldPoint(2901, 9867, 0);

	@Test
	public void fourApproachesRequireThePermanentUnlock()
	{
		List<Transport> rows = rows();
		assertEquals(4, rows.size());
		assertEquals(Set.of(
			new WorldPoint(2898, 3469, 0),
			new WorldPoint(2899, 3469, 0),
			new WorldPoint(2899, 3470, 0),
			new WorldPoint(2899, 3468, 0)),
			rows.stream().map(Transport::getOrigin).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertEquals(LANDING, row.getDestination());
			assertEquals("Enter", row.getAction());
			assertEquals("Manhole", row.getName());
			assertTrue(row.isMembers());
			assertEquals(1, row.getDuration());
			assertEquals(1, row.getVarbits().size());
			TransportVarbit gate = row.getVarbits().iterator().next();
			assertEquals(VarbitID.GRIM_MANHOLE_OPEN, gate.getVarbitId());
			assertEquals(1, gate.getValue());
			assertEquals(TransportVarbit.Operator.EQUAL, gate.getOperator());
			assertTrue(GrimTalesManholePolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void malformedOrUngatedRowsStayIneligible()
	{
		WorldPoint origin = new WorldPoint(2898, 3469, 0);
		assertFalse(GrimTalesManholePolicy.isEligible(new Transport(origin, LANDING, "",
			TransportType.TRANSPORT, true, "Enter", "Manhole",
			GrimTalesManholePolicy.MANHOLE_ID)));
		assertFalse(GrimTalesManholePolicy.isEligible(new Transport(origin, LANDING, "",
			TransportType.TRANSPORT, true, "Climb-down", "Manhole",
			GrimTalesManholePolicy.MANHOLE_ID)));
		assertFalse(GrimTalesManholePolicy.isEligible(new Transport(origin,
			new WorldPoint(2902, 9867, 0), "", TransportType.TRANSPORT, true,
			"Enter", "Manhole", GrimTalesManholePolicy.MANHOLE_ID)));
	}

	@Test
	public void manholeClearsOnlyAtTheExactBasementLanding()
	{
		Transport row = rows().get(0);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(),
			row.getDestination(), row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
			RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
			row.getOrigin(), row.getDestination());
		CatalogTransition transition = new CatalogTransition(null, row.getOrigin(),
			row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(),
			row.getDestination());
		WorldPoint near = new WorldPoint(LANDING.getX() + 1, LANDING.getY(), LANDING.getPlane());
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(pending, near, edge -> transition, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, LANDING, edge -> transition, 13).getStatus());
	}

	@Test
	public void existingStaircaseOwnsTheReturnRoute()
	{
		List<Transport> reverse = Transport.loadAllFromResources().get(LANDING).stream()
			.filter(row -> row.getObjectId() == 24687)
			.collect(Collectors.toList());
		assertEquals(1, reverse.size());
		assertEquals(new WorldPoint(2899, 3469, 0), reverse.get(0).getDestination());
		assertTrue(CatalogTransitionPolicy.isEligible(reverse.get(0)));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == GrimTalesManholePolicy.MANHOLE_ID)
			.collect(Collectors.toList());
	}
}
