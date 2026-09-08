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
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedAccessDoorTransportTest
{
	private static final Set<Integer> IDS = Set.of(11665, 22945, 34843);

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> IDS.contains(row.getObjectId()))
			.filter(row -> row.getObjectId() != 34843 || row.getOrigin().getX() == 1802)
			.collect(Collectors.toList());
	}

	@Test
	public void allEightExactRowsUseNavigationEngineOwnership()
	{
		List<Transport> rows = rows();
		assertEquals(8, rows.size());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 11665).count());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 22945).count());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 34843).count());
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(row)) == RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void exactLandingIsRequiredToClearEachDoor()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getDestination(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition object = new CatalogTransition(null, row.getDestination(),
				row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, row.getOrigin(), edge -> object, 13).getStatus());
			WorldPoint nearDestination = new WorldPoint(row.getDestination().getX(),
				row.getDestination().getY() + 1, row.getDestination().getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, nearDestination, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> object, 13).getStatus());
		}
	}

	@Test
	public void missingRequirementsAndMutatedRoutesStayLegacy()
	{
		List<Transport> rows = rows();
		Transport guildEntry = rows.stream().filter(row -> row.getObjectId() == 11665
			&& row.getSkillLevels()[Skill.RANGED.ordinal()] == 40).findFirst()
			.orElseThrow(AssertionError::new);
		guildEntry.getSkillLevels()[Skill.RANGED.ordinal()] = 0;
		assertFalse(CatalogTransitionPolicy.isEligible(guildEntry));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(guildEntry)));

		Transport boneDoor = rows.stream().filter(row -> row.getObjectId() == 22945)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(Map.of(Quest.DEATH_TO_THE_DORGESHUUN, QuestState.FINISHED),
			boneDoor.getQuests());
		boneDoor.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isEligible(boneDoor));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(boneDoor)));

		Transport templeDoor = rows.stream().filter(row -> row.getObjectId() == 34843)
			.findFirst().orElseThrow(AssertionError::new);
		templeDoor.getVarbits().clear();
		assertFalse(CatalogTransitionPolicy.isEligible(templeDoor));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(templeDoor)));

		Transport nonMember = new Transport(new WorldPoint(2658, 3437, 0),
			new WorldPoint(2657, 3439, 0), "test", TransportType.TRANSPORT,
			false, "Open", "Guild door", 11665);
		nonMember.getSkillLevels()[Skill.RANGED.ordinal()] = 40;
		assertFalse(CatalogTransitionPolicy.isEligible(nonMember));
		Transport shifted = new Transport(new WorldPoint(2749, 5374, 0),
			new WorldPoint(3317, 9603, 0), "test", TransportType.TRANSPORT,
			true, "Open", "Bone door", 22945);
		shifted.getQuests().put(Quest.DEATH_TO_THE_DORGESHUUN, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isEligible(shifted));
	}
}
