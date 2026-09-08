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

public class IsafdarTransportTest
{
	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> Set.of(3921, 3931, 3932, 3933).contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	@Test
	public void fourteenExactCrossingsRetainRequirements()
	{
		List<Transport> rows = rows();
		assertEquals(14, rows.size());
		assertEquals(8, rows.stream().filter(row -> row.getObjectId() == 3921).count());
		for (Transport row : rows)
		{
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
				"", row.getType(), true, row.getAction(), row.getName(), row.getObjectId());
			assertFalse(CatalogTransitionPolicy.isEligible(foreign));
			if (row.getObjectId() != 3921)
			{
				assertEquals(45, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
				assertEquals(Map.of(Quest.REGICIDE, QuestState.IN_PROGRESS), row.getQuests());
				row.getQuests().clear();
				assertFalse(CatalogTransitionPolicy.isEligible(row));
			}
		}
	}

	@Test
	public void midCrossingDamageAndDisappearanceCannotStandInForLanding()
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
			WorldPoint nearLanding = new WorldPoint(to.getX() - Integer.signum(to.getX() - from.getX()),
				to.getY() - Integer.signum(to.getY() - from.getY()), to.getPlane());
			for (WorldPoint failed : new WorldPoint[]{from, nearLanding,
				new WorldPoint(to.getX(), to.getY() + 6400, 0)})
			{
				assertEquals(RouteInteraction.Status.AVAILABLE,
					scanner.observePending(pending, failed, edge -> object, 13).getStatus());
				assertEquals(RouteInteraction.Status.UNAVAILABLE,
					scanner.observePending(pending, failed, edge -> null, 13).getStatus());
			}
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}

	@Test
	public void leafPitsRemainDeferredUntilClimbOutRecoveryExists()
	{
		List<Transport> leaves = Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getObjectId() == 3925).collect(Collectors.toList());
		assertEquals(0, leaves.size());
	}
}
