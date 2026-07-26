package net.runelite.client.plugins.microbot.util.walker.geometry;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Map;

/**
 * Pure path-geometry helpers shared by the walker's executor and recovery, extracted from {@code Rs2Walker}
 * (P1 decomposition). Nothing here reads live game state — the reachable-tiles map and player tile are
 * passed in — so the logic is unit-testable and reusable across the extracted walker services rather than
 * being duplicated in each. The game-coupled wrappers (which fetch the player location and reachable tiles)
 * stay in {@code Rs2Walker} and delegate here.
 */
public final class WalkerPathGeometry {

    private WalkerPathGeometry() {
    }

    /**
     * Index of the path tile closest to the player. Prefers the tile with the smallest recorded reachable
     * distance in {@code reachableTiles} (the walk-connected frontier); falls back to straight-line
     * {@link WorldPoint#distanceTo} when none of the path is in that set.
     *
     * @return the closest index, or {@code -1} for an empty path / null player.
     */
    public static int getClosestTileIndex(List<WorldPoint> path, WorldPoint playerLoc,
                                          Map<WorldPoint, Integer> reachableTiles) {
        if (path == null || path.isEmpty() || playerLoc == null) {
            return -1;
        }

        int bestReachableIndex = -1;
        int bestReachableDistance = Integer.MAX_VALUE;
        if (reachableTiles != null && !reachableTiles.isEmpty()) {
            for (int i = 0; i < path.size(); i++) {
                WorldPoint point = path.get(i);
                if (point == null) {
                    continue;
                }
                int reachableDistance = reachableTiles.getOrDefault(point, Integer.MAX_VALUE);
                if (reachableDistance < bestReachableDistance) {
                    bestReachableDistance = reachableDistance;
                    bestReachableIndex = i;
                    if (reachableDistance == 0) {
                        break;
                    }
                }
            }
        }

        if (bestReachableIndex >= 0 && bestReachableDistance != Integer.MAX_VALUE) {
            return bestReachableIndex;
        }

        int closestIndex = -1;
        int closestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < path.size(); i++) {
            WorldPoint point = path.get(i);
            if (point == null) {
                continue;
            }
            int distance = playerLoc.distanceTo(point);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }
        return closestIndex;
    }
}
