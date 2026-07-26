package net.runelite.client.plugins.microbot.util.walker.recovery;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure route-recovery selection logic, extracted from {@code Rs2Walker} (P1 walker decomposition). The
 * algorithm here is deliberately free of walker/game state — callers pass in the path, the player tile,
 * the reachable-tile set, and a clickability predicate — so it is unit-testable in isolation (see
 * {@code Rs2WalkerUnitTest}). The game-state-coupled wrapper (which reads reachable tiles and binds the
 * minimap-clickable predicate) stays in {@code Rs2Walker} and delegates here.
 */
public final class RouteRecovery {

    /**
     * How many path tiles ahead of the stuck index the forward-recovery scan considers. Beyond this the
     * route is re-evaluated as the player advances, so scanning further wastes work.
     */
    private static final int FORWARD_SCAN_TILES = 24;

    private RouteRecovery() {
    }

    /**
     * Picks the furthest forward path tile (from {@code startIdx}, within {@code FORWARD_SCAN_TILES}) that
     * is on the player's plane, within {@code maxEuclidean} of the player, reachable (when a reachable set
     * is supplied), and clickable per {@code isClickable}; ties break toward the tile nearer the player.
     *
     * @return the chosen path index, or {@code -1} when none qualifies.
     */
    public static int findForwardRecoveryIndex(List<WorldPoint> path,
                                               int startIdx,
                                               WorldPoint playerLoc,
                                               int maxEuclidean,
                                               Set<WorldPoint> reachable,
                                               Predicate<WorldPoint> isClickable) {
        if (path == null || path.isEmpty() || playerLoc == null || startIdx < 0 || startIdx >= path.size()) {
            return -1;
        }
        int maxSq = maxEuclidean * maxEuclidean;
        int endExclusive = Math.min(path.size(), startIdx + FORWARD_SCAN_TILES + 1);
        int bestIdx = -1;
        int bestDistFromPlayer = Integer.MAX_VALUE;
        for (int idx = startIdx; idx < endExclusive; idx++) {
            WorldPoint candidate = path.get(idx);
            if (candidate == null || candidate.getPlane() != playerLoc.getPlane()) {
                continue;
            }
            if (candidate.equals(playerLoc) || euclideanSq(candidate, playerLoc) > maxSq) {
                continue;
            }
            if (reachable != null && !reachable.isEmpty() && !reachable.contains(candidate)) {
                continue;
            }
            if (isClickable != null && !isClickable.test(candidate)) {
                continue;
            }
            int distFromPlayer = euclideanSq(candidate, playerLoc);
            if (bestIdx < 0 || idx > bestIdx || (idx == bestIdx && distFromPlayer > bestDistFromPlayer)) {
                bestIdx = idx;
                bestDistFromPlayer = distFromPlayer;
            }
        }
        return bestIdx;
    }

    /** Squared Euclidean distance; local copy of the trivial helper to keep this class self-contained. */
    private static int euclideanSq(WorldPoint a, WorldPoint b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }
}
