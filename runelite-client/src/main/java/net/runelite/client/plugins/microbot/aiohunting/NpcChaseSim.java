package net.runelite.client.plugins.microbot.aiohunting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;

/**
 * Simulation of OSRS "dumb" NPC chase pathing against the live scene collision flags.
 *
 * <p>A chasing NPC takes one greedy step per tick toward its target: the diagonal first, then the
 * x-only step, then the y-only step - and if all three are blocked it simply stops. It can never
 * route around anything (this is the mechanic safespotting is built on). Because the algorithm is
 * exact and the collision flags are available, we can predict - BEFORE moving - whether a lure
 * target would wedge the prey behind scenery, and choose lead tiles it can provably follow to.</p>
 *
 * <p>The snapshot is captured in one client-thread hop; all simulation afterwards is pure math.
 * Wall-direction flags are approximated by full-tile blocking only, which matches the hunter areas
 * (vegetation blockers), and any mis-prediction is caught by the breadcrumb recovery.</p>
 */
final class NpcChaseSim
{
	private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL
		| CollisionDataFlag.BLOCK_MOVEMENT_FLOOR
		| CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION
		| CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;

	private NpcChaseSim()
	{
	}

	/** Captures the current scene's collision flags in a single client-thread hop, or null. */
	static Snapshot capture()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			WorldView wv = Microbot.getClient().getTopLevelWorldView();
			if (wv == null)
			{
				return null;
			}
			CollisionData[] maps = wv.getCollisionMaps();
			int plane = wv.getPlane();
			if (maps == null || plane < 0 || plane >= maps.length || maps[plane] == null)
			{
				return null;
			}
			return new Snapshot(maps[plane].getFlags(), wv.getBaseX(), wv.getBaseY(), plane);
		}).orElse(null);
	}

	static final class Snapshot
	{
		private final int[][] flags;
		private final int baseX;
		private final int baseY;
		private final int plane;

		private Snapshot(int[][] flags, int baseX, int baseY, int plane)
		{
			this.flags = flags;
			this.baseX = baseX;
			this.baseY = baseY;
			this.plane = plane;
		}

		private boolean walkable(int worldX, int worldY)
		{
			int sx = worldX - baseX;
			int sy = worldY - baseY;
			if (sx < 0 || sy < 0 || sx >= flags.length || sy >= flags[sx].length)
			{
				return false;
			}
			return (flags[sx][sy] & BLOCKED) == 0;
		}

		/** Whether an NPC of {@code size} can stand with its south-west tile at ({@code swX},{@code swY}). */
		private boolean canPlace(int swX, int swY, int size)
		{
			for (int dx = 0; dx < size; dx++)
			{
				for (int dy = 0; dy < size; dy++)
				{
					if (!walkable(swX + dx, swY + dy))
					{
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * Simulates the greedy chase from the NPC's south-west tile toward {@code target}.
		 * @return true when it gets adjacent to the target; false when it wedges on the way.
		 */
		boolean canNpcReach(WorldPoint npcSw, int size, WorldPoint target, int maxSteps)
		{
			if (npcSw == null || target == null || npcSw.getPlane() != plane || target.getPlane() != plane)
			{
				// Can't evaluate - stay optimistic rather than blocking movement on missing data.
				return true;
			}
			int x = npcSw.getX();
			int y = npcSw.getY();
			int tx = target.getX();
			int ty = target.getY();
			for (int step = 0; step < maxSteps; step++)
			{
				// Reached = the target tile touches the NPC's footprint (it can attack from there).
				if (tx >= x - 1 && tx <= x + size && ty >= y - 1 && ty <= y + size)
				{
					return true;
				}
				int dx = tx < x ? -1 : (tx > x + size - 1 ? 1 : 0);
				int dy = ty < y ? -1 : (ty > y + size - 1 ? 1 : 0);
				if (dx != 0 && dy != 0 && canPlace(x + dx, y + dy, size)
					&& canPlace(x + dx, y, size) && canPlace(x, y + dy, size))
				{
					x += dx;
					y += dy;
				}
				else if (dx != 0 && canPlace(x + dx, y, size))
				{
					x += dx;
				}
				else if (dy != 0 && canPlace(x, y + dy, size))
				{
					y += dy;
				}
				else
				{
					return false; // wedged - every greedy step is blocked
				}
			}
			return false;
		}

		/**
		 * Shortest player-walkable path (BFS, 8-directional, diagonals need both cardinals open)
		 * from {@code from} to {@code to}, both inclusive. Empty when unreachable within the radius.
		 */
		List<WorldPoint> playerPath(WorldPoint from, WorldPoint to, int maxRadius)
		{
			if (from == null || to == null || from.getPlane() != plane || to.getPlane() != plane)
			{
				return List.of();
			}
			if (from.equals(to))
			{
				return List.of(from);
			}
			int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
			Map<WorldPoint, WorldPoint> cameFrom = new HashMap<>();
			ArrayDeque<WorldPoint> queue = new ArrayDeque<>();
			cameFrom.put(from, from);
			queue.add(from);
			while (!queue.isEmpty())
			{
				WorldPoint cur = queue.poll();
				for (int[] dir : dirs)
				{
					int nx = cur.getX() + dir[0];
					int ny = cur.getY() + dir[1];
					if (Math.max(Math.abs(nx - from.getX()), Math.abs(ny - from.getY())) > maxRadius
						|| !walkable(nx, ny))
					{
						continue;
					}
					if (dir[0] != 0 && dir[1] != 0
						&& (!walkable(cur.getX() + dir[0], cur.getY()) || !walkable(cur.getX(), cur.getY() + dir[1])))
					{
						continue;
					}
					WorldPoint next = new WorldPoint(nx, ny, plane);
					if (cameFrom.containsKey(next))
					{
						continue;
					}
					cameFrom.put(next, cur);
					if (next.equals(to))
					{
						List<WorldPoint> path = new ArrayList<>();
						for (WorldPoint p = next; !p.equals(from); p = cameFrom.get(p))
						{
							path.add(0, p);
						}
						path.add(0, from);
						return path;
					}
					queue.add(next);
				}
			}
			return List.of();
		}
	}
}
