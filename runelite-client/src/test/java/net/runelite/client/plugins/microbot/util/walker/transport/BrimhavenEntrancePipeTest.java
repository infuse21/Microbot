package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrimhavenEntrancePipeTest
{
	@Test
	public void pipeRequiresTwentyTwoOnlyTowardsMossGiants()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream).filter(row -> row.getObjectId() == 21728).collect(Collectors.toList());
		assertEquals(2, rows.size());
		for (Transport row : rows)
		{
			boolean inward = row.getOrigin().equals(new WorldPoint(2655, 9573, 0));
			assertEquals(new WorldPoint(2655, inward ? 9566 : 9573, 0), row.getDestination());
			assertEquals(new WorldPoint(2655, inward ? 9573 : 9566, 0), row.getOrigin());
			assertEquals(TransportType.AGILITY_SHORTCUT, row.getType());
			assertEquals("Squeeze-through", row.getAction());
			assertEquals(inward ? 22 : 0, row.getSkillLevels()[Skill.AGILITY.ordinal()]);
			assertTrue(CatalogTransitionPolicy.isEligible(row));
		}
	}

	@Test
	public void southernEndpointConnectsToMossGiantRoomWithoutCrossingPipe()
	{
		CollisionMap map = new CollisionMap(SplitFlagMap.fromResources());
		Set<WorldPoint> visited = new HashSet<>();
		ArrayDeque<WorldPoint> frontier = new ArrayDeque<>();
		add(visited, frontier, 2655, 9566);
		while (!frontier.isEmpty())
		{
			WorldPoint point = frontier.removeFirst();
			int x = point.getX();
			int y = point.getY();
			if (map.n(x, y, 0)) add(visited, frontier, x, y + 1);
			if (map.s(x, y, 0)) add(visited, frontier, x, y - 1);
			if (map.e(x, y, 0)) add(visited, frontier, x + 1, y);
			if (map.w(x, y, 0)) add(visited, frontier, x - 1, y);
		}
		// The Wiki location is a map centre, not necessarily a walkable tile.
		assertTrue("Southern endpoint must connect to the moss-giant area", visited.stream()
			.anyMatch(point -> point.distanceTo(new WorldPoint(2659, 9544, 0)) <= 5));
		assertFalse(visited.contains(new WorldPoint(2655, 9573, 0)));
	}

	private static void add(Set<WorldPoint> visited, ArrayDeque<WorldPoint> frontier, int x, int y)
	{
		if (x < 2620 || x > 2730 || y < 9500 || y > 9600) return;
		WorldPoint point = new WorldPoint(x, y, 0);
		if (visited.add(point)) frontier.add(point);
	}
}
