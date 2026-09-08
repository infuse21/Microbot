package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AuditedAgilityTraversalTest
{
	private static final Set<Integer> IDS = Set.of(3522, 11948, 11949, 19846, 19847, 26405);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void allTwentyEightExactRowsAreEngineOwnedWithRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(28, rows.size());
		assertEquals(8, rows.stream().filter(row -> row.getObjectId() == 3522).count());
		assertEquals(14, rows.stream().filter(row -> row.getObjectId() == 11948
			|| row.getObjectId() == 11949).count());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 19846
			|| row.getObjectId() == 19847).count());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 26405).count());
		for (Transport row : rows)
		{
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			int expectedLevel = row.getObjectId() == 3522 ? 1
				: row.getObjectId() == 11948 ? 0
				: row.getObjectId() == 11949 ? 30
				: row.getObjectId() == 19846 || row.getObjectId() == 19847 ? 35 : 60;
			assertEquals(expectedLevel, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertEquals(row.getObjectId() == 26405
				? Map.of(Quest.TROLL_STRONGHOLD, QuestState.IN_PROGRESS) : Collections.emptyMap(),
				row.getQuests());
			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
		}
	}

	@Test
	public void movementNearDestinationAndObjectDisappearanceDoNotAcknowledgeLanding()
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
			WorldPoint near = new WorldPoint(to.getX() - Integer.signum(to.getX() - from.getX()),
				to.getY() - Integer.signum(to.getY() - from.getY()), to.getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, near, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, near, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}
}
