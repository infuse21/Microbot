package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class FloorboardTransportTest
{
	private static List<Transport> floorboards()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> "Floorboards".equals(row.getName()))
			.collect(Collectors.toList());
	}

	@Test
	public void allEighteenDirectedJumpsUseEngineOwnership()
	{
		List<Transport> rows = floorboards();
		assertEquals(18, rows.size());
		for (Transport row : rows)
		{
			assertEquals(26, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertEquals(Map.of(Quest.DARKNESS_OF_HALLOWVALE, QuestState.IN_PROGRESS),
				row.getQuests());
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			Transport foreign = new Transport(new WorldPoint(100, 100, 1), row.getDestination(),
				"", TransportType.TRANSPORT, true, "Jump-to", "Floorboards", row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
			Transport wrongAction = new Transport(row.getOrigin(), row.getDestination(),
				"", TransportType.TRANSPORT, true, "Open", "Floorboards", row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(wrongAction));
		}
	}

	@Test
	public void failedJumpsAndMissingObjectsCannotAcknowledgeArrival()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : floorboards())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, to,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				row.getAction(), true, row.getObjectId(), from, to);
			CatalogTransition object = new CatalogTransition(null, to, row.getObjectId(),
				row.getAction(), row.getAction(), from, to);
			WorldPoint midpoint = new WorldPoint((from.getX() + to.getX()) / 2,
				(from.getY() + to.getY()) / 2, from.getPlane());
			for (WorldPoint failed : new WorldPoint[]{from, midpoint,
				new WorldPoint(to.getX(), to.getY(), to.getPlane() - 1)})
			{
				assertEquals(RouteInteraction.Status.AVAILABLE,
					scanner.observePending(pending, failed, edge -> object, 13).getStatus());
				assertEquals(RouteInteraction.Status.UNAVAILABLE,
					scanner.observePending(pending, failed, edge -> null, 13).getStatus());
			}
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> object, 13).getStatus());
		}
	}

	@Test
	public void rangingGuildOnlyGatesEntry()
	{
		List<Transport> doors = Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 11665).collect(Collectors.toList());
		assertEquals(4, doors.size());
		for (Transport row : doors)
		{
			assertEquals(row.getDestination().getY() == 3439 ? 40 : 0,
				row.getSkillLevels()[Skill.RANGED.ordinal()]);
		}
	}
}
