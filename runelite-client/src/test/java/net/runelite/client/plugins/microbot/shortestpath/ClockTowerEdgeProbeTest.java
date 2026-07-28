package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Scratch probe: how does the STATIC map route into the Clock Tower ground floor? */
public class ClockTowerEdgeProbeTest {
	@Test
	public void printStaticRouteIntoTower() {
		CollisionMap map = new CollisionMap(SplitFlagMap.fromResources());
		int sx = 2570, sy = 3234, tx = 2571, ty = 3240, plane = 0;

		Map<Long, Long> prev = new HashMap<>();
		ArrayDeque<long[]> queue = new ArrayDeque<>();
		queue.add(new long[]{sx, sy});
		prev.put(key(sx, sy), key(sx, sy));
		int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

		while (!queue.isEmpty()) {
			long[] cur = queue.poll();
			int x = (int) cur[0], y = (int) cur[1];
			if (x == tx && y == ty) break;
			for (int[] d : dirs) {
				int nx = x + d[0], ny = y + d[1];
				if (Math.abs(nx - sx) > 25 || Math.abs(ny - sy) > 25) continue;
				if (prev.containsKey(key(nx, ny))) continue;
				if (!map.canStep(x, y, plane, d[0], d[1])) continue;
				prev.put(key(nx, ny), key(x, y));
				queue.add(new long[]{nx, ny});
			}
		}

		if (!prev.containsKey(key(tx, ty))) {
			System.out.println("PROBE: target NOT reachable in static map from " + sx + "," + sy);
			return;
		}
		StringBuilder sb = new StringBuilder("PROBE static path (reversed): ");
		long k = key(tx, ty);
		while (k != key(sx, sy)) {
			sb.append('(').append(k >> 16).append(',').append(k & 0xffff).append(") ");
			k = prev.get(k);
		}
		sb.append('(').append(sx).append(',').append(sy).append(')');
		System.out.println(sb);
	}

	private static long key(long x, long y) {
		return (x << 16) | y;
	}
}
