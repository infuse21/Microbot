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
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DarkmeyerWallSourceTest
{
	@Test
	public void sixCanonicalRowsRequireBothPermanentRopes()
	{
		List<Transport> rows = rows();
		assertEquals(6, rows.size());
		assertEquals(Set.of(
			"3667 3375 0>3670 3375 0:39542",
			"3670 3375 0>3667 3375 0:39542",
			"3670 3375 0>3673 3375 0:39541",
			"3673 3375 0>3670 3375 0:39541",
			"3672 3376 0>3670 3375 0:39541",
			"3672 3374 0>3670 3375 0:39541"),
			rows.stream().map(DarkmeyerWallSourceTest::signature).collect(Collectors.toSet()));
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertEquals(0, row.getDuration());
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(row.getVarplayers().isEmpty());
			assertEquals(Map.of(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED),
				row.getQuests());
			assertEquals(63, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertEquals(63, java.util.Arrays.stream(row.getSkillLevels()).sum());
			assertEquals(Set.of("10449=1", "10450=1"), row.getVarbits().stream()
				.map(DarkmeyerWallSourceTest::varbit).collect(Collectors.toSet()));
			assertTrue(CatalogTransitionPolicy.isDarkmeyerInstalledWall(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void installedObjectTransformsAreMappedToTheirCatalogWalls()
	{
		for (Transport row : rows())
		{
			int installed = row.getObjectId() == 39541 ? 39166 : 39168;
			int wrong = row.getObjectId() == 39541 ? 39168 : 39166;
			assertTrue(CatalogTransitionPolicy.matchesDarkmeyerInstalledObject(row, installed));
			assertFalse(CatalogTransitionPolicy.matchesDarkmeyerInstalledObject(row, wrong));
		}
	}

	@Test
	public void everyApproachRequiresItsExactDirectedLanding()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : rows())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(),
				row.getDestination(), row.getOrigin(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition transition = new CatalogTransition(null, row.getOrigin(),
				row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(),
				row.getDestination());
			WorldPoint near = new WorldPoint(row.getDestination().getX() + 1,
				row.getDestination().getY(), row.getDestination().getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, near, edge -> transition, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> transition, 13)
					.getStatus());
		}
	}

	@Test
	public void missingInstalledRopeGateIsRejected()
	{
		Transport row = rows().get(0);
		row.getVarbits().removeIf(requirement -> requirement.getVarbitId() == 10450);
		assertFalse(CatalogTransitionPolicy.isDarkmeyerInstalledWall(row));
		assertFalse(CatalogTransitionPolicy.isEligible(row));
	}

	private static List<Transport> rows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 39541 || row.getObjectId() == 39542)
			.collect(Collectors.toList());
	}

	private static String signature(Transport row)
	{
		return point(row.getOrigin()) + ">" + point(row.getDestination()) + ":"
			+ row.getObjectId();
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
	}

	private static String varbit(TransportVarbit requirement)
	{
		assertEquals(TransportVarbit.Operator.EQUAL, requirement.getOperator());
		return requirement.getVarbitId() + "=" + requirement.getValue();
	}
}
