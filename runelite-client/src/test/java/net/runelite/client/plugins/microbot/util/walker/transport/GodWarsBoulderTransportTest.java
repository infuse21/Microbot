package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GodWarsBoulderTransportTest
{
	@Test
	public void allFiveRowsHaveExactStrengthAndQuestOwnership()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> row.getObjectId() == 26415)
			.collect(Collectors.toList());
		assertEquals(5, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(60, row.getSkillLevels()[Skill.STRENGTH.ordinal()]);
			assertEquals(Map.of(Quest.TROLL_STRONGHOLD, QuestState.IN_PROGRESS),
				row.getQuests());
			assertTrue(CatalogTransitionPolicy.isGodWarsBoulder(row));
		}
	}

	@Test
	public void failedMoveStaysPendingUntilExactLanding()
	{
		Transport row = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(candidate -> candidate.getObjectId() == 26415).findFirst().orElseThrow();
		RouteInteraction pending = pending(row);
		CatalogTransition object = new CatalogTransition(null, row.getOrigin(), row.getObjectId(),
			row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(pending, row.getOrigin(), edge -> object, 13).getStatus());
		WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
			row.getDestination().getY(), row.getDestination().getPlane());
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, near, edge -> null, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, row.getDestination(), edge -> null, 13).getStatus());
	}

	private static RouteInteraction pending(Transport row)
	{
		return new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(), row.getOrigin(),
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			row.getAction(), true, row.getObjectId(), row.getOrigin(), row.getDestination());
	}
}
