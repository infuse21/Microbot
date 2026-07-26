package net.runelite.client.plugins.microbot.util.walker.geometry;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

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

    /**
     * Raw-route index to start a forward scan from, given the smoothed anchor {@code rawAnchorIndex}.
     * <p>
     * When an anchor is known ({@code >= 0}) this stays on the forward branch: it searches the window
     * {@code [anchor, anchor + forwardSearchTiles]} for the tile physically closest to the player and never
     * looks backward, so a switch-backing route can't snap the anchor to an earlier tile that happens to be
     * Euclidean-near. When no anchor is known ({@code < 0}) it falls back to the reachable-closest index,
     * supplied lazily so the (game-coupled) fallback is only computed when actually needed.
     *
     * @param closestIndexFallback invoked only for the {@code rawAnchorIndex < 0} case
     * @return the anchor index, or {@code -1} for an empty path / null player
     */
    public static int rawPathForwardAnchorIndex(List<WorldPoint> rawPath, WorldPoint playerLoc,
                                                int rawAnchorIndex, int forwardSearchTiles,
                                                IntSupplier closestIndexFallback) {
        if (rawPath == null || rawPath.isEmpty() || playerLoc == null) {
            return -1;
        }
        if (rawAnchorIndex < 0) {
            return closestIndexFallback.getAsInt();
        }

        int start = Math.max(0, Math.min(rawAnchorIndex, rawPath.size() - 1));
        int endExclusive = Math.min(rawPath.size(), start + forwardSearchTiles + 1);
        int bestIdx = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int i = start; i < endExclusive; i++) {
            WorldPoint point = rawPath.get(i);
            if (point == null) {
                continue;
            }
            if (point.getPlane() != playerLoc.getPlane()) {
                break;
            }
            int dist = point.distanceTo2D(playerLoc);
            if (dist < bestDist) {
                bestIdx = i;
                bestDist = dist;
                if (dist == 0) {
                    break;
                }
            }
        }
        return bestIdx >= 0 ? bestIdx : start;
    }

    /**
     * Furthest-forward raw-route point, from the forward anchor onward, that satisfies {@code isCandidate}
     * and stays within both the Euclidean radius {@code maxEuclidean} and a matching along-route step budget.
     * <p>
     * The dual radius/step cap is what keeps a minimap recovery click on the planned route: the Euclidean
     * cap bounds how far the click reaches, and the route-step cap stops a switch-backing route (which can
     * fold back Euclidean-near while being many tiles away along the path) from selecting a point behind a
     * wall. Pure — the reachability predicate and the anchor fallback are injected — so it is unit-testable.
     *
     * @return the furthest matching point, or {@code null} when none qualifies
     */
    public static WorldPoint findFurthestRawPathPointMatching(List<WorldPoint> rawPath, WorldPoint playerLoc,
                                                              int maxEuclidean, int rawAnchorIndex,
                                                              Predicate<WorldPoint> isCandidate,
                                                              int forwardSearchTiles,
                                                              IntSupplier closestIndexFallback) {
        if (rawPath == null || rawPath.isEmpty() || playerLoc == null) {
            return null;
        }
        int closestRawIndex = rawPathForwardAnchorIndex(rawPath, playerLoc, rawAnchorIndex, forwardSearchTiles,
                closestIndexFallback);
        if (closestRawIndex < 0) {
            return null;
        }

        int maxSq = maxEuclidean * maxEuclidean;
        int maxRouteSteps = maxEuclidean + 2;
        int routeSteps = 0;
        WorldPoint best = null;
        for (int rawIndex = closestRawIndex; rawIndex < rawPath.size(); rawIndex++) {
            WorldPoint candidate = rawPath.get(rawIndex);
            if (candidate == null || candidate.getPlane() != playerLoc.getPlane()) {
                break;
            }
            if (rawIndex > closestRawIndex) {
                WorldPoint previous = rawPath.get(rawIndex - 1);
                routeSteps += rawPathStepDistance(previous, candidate);
                if (routeSteps > maxRouteSteps) {
                    break;
                }
            }
            if (euclideanSq(candidate, playerLoc) > maxSq) {
                break;
            }
            if (!candidate.equals(playerLoc) && (isCandidate == null || isCandidate.test(candidate))) {
                best = candidate;
            }
        }
        return best;
    }

    private static int rawPathStepDistance(WorldPoint previous, WorldPoint current) {
        if (previous == null || current == null || previous.getPlane() != current.getPlane()) {
            return Integer.MAX_VALUE / 4;
        }
        return Math.max(1, previous.distanceTo2D(current));
    }

    private static int euclideanSq(WorldPoint a, WorldPoint b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }
}
