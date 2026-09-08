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

public class FremennikBridgeTransportTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() >= 21306 && row.getObjectId() <= 21319)
			.collect(Collectors.toList());
	}

	@Test
	public void surfaceCrossingsAreOwnedButBossCaveRemainsDeferred()
	{
		List<Transport> rows = rows();
		assertEquals(15, rows.size());
		assertEquals(10, rows.stream().filter(row -> row.getObjectId() <= 21315).count());
		for (Transport row : rows)
		{
			assertEquals(row.getObjectId() <= 21315 ? RouteEdge.Kind.CATALOG_TRANSITION : RouteEdge.Kind.TRANSPORT,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			if (row.getObjectId() > 21315) continue;
			assertEquals("Walk-across", row.getAction());
			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void onlyMineShortcutNeedsFortyAgility()
	{
		for (Transport row : rows())
		{
			if (row.getObjectId() > 21315) continue;
			boolean shortcut = row.getObjectId() >= 21314;
			assertEquals(shortcut ? 40 : 0, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			if (shortcut)
			{
				row.getSkillLevels()[Skill.AGILITY.ordinal()] = 0;
				assertFalse(CatalogTransitionPolicy.isEligible(row));
			}
		}
	}

	@Test
	public void bridgeMovementAndMissingSupportDoNotConfirmArrival()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			if (row.getObjectId() > 21315) continue;
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition object = new CatalogTransition(null, from, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			for (WorldPoint incomplete : new WorldPoint[]{from,
				new WorldPoint(to.getX(), to.getY() - Integer.signum(to.getY() - from.getY()), 0),
				new WorldPoint(to.getX(), to.getY(), 1)})
			{
				assertEquals(RouteInteraction.Status.AVAILABLE,
					scanner.observePending(pending, incomplete, edge -> object, 13).getStatus());
				assertEquals(RouteInteraction.Status.UNAVAILABLE,
					scanner.observePending(pending, incomplete, edge -> null, 13).getStatus());
			}
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
