package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrimhavenIslandGeometryTest
{
	@Test
	public void bothInwardRowsRequireFiftySixAndBothOutwardRowsAreFree()
	{
		java.util.List<net.runelite.client.plugins.microbot.shortestpath.Transport> rows = BrimhavenIslandSceneTest.rows();
		org.junit.Assert.assertEquals(4, rows.size());
		int inward = 0;
		for (net.runelite.client.plugins.microbot.shortestpath.Transport row : rows)
		{
			boolean towardsIsland = row.getDestination().equals(new WorldPoint(2690, 9547, 0))
				|| row.getDestination().equals(new WorldPoint(2695, 9533, 0));
			org.junit.Assert.assertEquals(towardsIsland ? 56 : 0,
				row.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()]);
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			if (towardsIsland) inward++;
			row.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()] = towardsIsland ? 0 : 56;
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
		org.junit.Assert.assertEquals(2, inward);
	}

	@Test
	public void easternShortcutIslandEndpointsShareGroundButNotEitherOuterBank()
	{
		CollisionMap map = new CollisionMap(SplitFlagMap.fromResources());
		Set<WorldPoint> island = new HashSet<>();
		ArrayDeque<WorldPoint> frontier = new ArrayDeque<>();
		WorldPoint start = new WorldPoint(2690, 9547, 0);
		island.add(start);
		frontier.add(start);
		while (!frontier.isEmpty())
		{
			WorldPoint point = frontier.removeFirst();
			int x = point.getX();
			int y = point.getY();
			if (map.n(x, y, 0)) add(island, frontier, x, y + 1);
			if (map.s(x, y, 0)) add(island, frontier, x, y - 1);
			if (map.e(x, y, 0)) add(island, frontier, x + 1, y);
			if (map.w(x, y, 0)) add(island, frontier, x - 1, y);
		}
		assertTrue("Both island endpoints must be connected without a transport",
			island.contains(new WorldPoint(2695, 9533, 0)));
		assertFalse(island.contains(new WorldPoint(2682, 9548, 0)));
		assertFalse(island.contains(new WorldPoint(2697, 9525, 0)));
	}

	private static void add(Set<WorldPoint> visited, ArrayDeque<WorldPoint> frontier, int x, int y)
	{
		if (x < 2670 || x > 2710 || y < 9510 || y > 9560) return;
		WorldPoint point = new WorldPoint(x, y, 0);
		if (visited.add(point)) frontier.add(point);
	}
}
