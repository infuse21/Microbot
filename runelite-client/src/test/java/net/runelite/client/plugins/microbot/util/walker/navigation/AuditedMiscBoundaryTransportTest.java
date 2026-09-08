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
import static org.junit.Assert.assertTrue;

public class AuditedMiscBoundaryTransportTest
{
	private static final Set<Integer> OBJECT_IDS = Set.of(5847, 12776, 31691, 43724, 43726);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> OBJECT_IDS.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	@Test
	public void allEightExactRowsRetainTheirAuditedRequirementsAndOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(8, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.getOrigin() + " -> " + row.getDestination() + " object=" + row.getObjectId()
				+ " members=" + row.isMembers() + " duration=" + row.getDuration()
				+ " quests=" + row.getQuests() + " skills=" + java.util.Arrays.toString(row.getSkillLevels())
				+ " consumable=" + row.isConsumable() + " currency=" + row.getCurrencyAmount()
				+ " items=" + row.getItemIdRequirements() + " varbits=" + row.getVarbits()
				+ " varplayers=" + row.getVarplayers() + " action=" + row.getAction()
				+ " name=" + row.getName(),
				CatalogTransitionPolicy.isEligible(row));
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row)));
			switch (row.getObjectId())
			{
				case 5847:
					assertTrue(row.isMembers());
					assertEquals(Map.of(Quest.MOUNTAIN_DAUGHTER, QuestState.IN_PROGRESS), row.getQuests());
					break;
				case 12776:
					assertTrue(row.isMembers());
					assertEquals(25, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
					assertEquals(Map.of(Quest.IN_AID_OF_THE_MYREQUE, QuestState.FINISHED), row.getQuests());
					break;
				case 31691:
					assertTrue(row.isMembers());
					assertTrue(row.getVarbits().stream().anyMatch(gate -> gate.getVarbitId() == 6027));
					break;
				case 43724:
				case 43726:
					assertFalse(row.isMembers());
					assertEquals(56, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
					assertEquals(Map.of(Quest.TEMPLE_OF_THE_EYE, QuestState.FINISHED), row.getQuests());
					break;
				default:
					throw new AssertionError("Unexpected object " + row.getObjectId());
			}
		}
	}

	@Test
	public void exactDirectedLandingIsRequiredForEveryRow()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition object = new CatalogTransition(null, row.getOrigin(), row.getObjectId(),
				row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
				row.getDestination().getY(), row.getDestination().getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, near, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> object, 13).getStatus());
		}
	}
}
