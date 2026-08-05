package net.runelite.client.plugins.microbot.util.walker.lifecycle;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.door.DoorInteractionOwnership;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngineRuntime;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRequest;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRouteOptions;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlannerRuntime;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import java.util.Set;

@Slf4j
public final class Rs2WalkerLifecycleRuntime {

    private Rs2WalkerLifecycleRuntime() {
    }

    public static void applyWalkerDestination(WorldPoint target) {
        applyWalkerDestination(target, false);
    }

    public static void applyWalkerDestination(WorldPoint target, boolean newRequest) {
        if (target == null) {
            return;
        }
        if (!Microbot.isLoggedIn()) {
            log.warn("Unable to apply walker destination: not logged in");
            return;
        }
        Client client = Microbot.getClient();
        if (client == null) {
            log.warn("Unable to apply walker destination: client unavailable");
            return;
        }
        Player localPlayer = Microbot.getClientThread().invoke(() -> client.getLocalPlayer());
        if (!Rs2PathApi.isStartPointSet() && localPlayer == null) {
            log.warn("Start point is not set and player is null");
            return;
        }

        WorldMapPointManager wmm = Microbot.getWorldMapPointManager();
        if (wmm == null) {
            Rs2Walker.clearWalkingRoute("walker:wmm-unavailable retry-setTarget dest=" + target);
            return;
        }
        wmm.removeIf(x -> x == Rs2PathApi.getMarker());
        Rs2PathApi.setMarker(new WorldMapPoint(target, Rs2PathApi.MARKER_IMAGE));
        Rs2PathApi.getMarker().setName("Target");
        Rs2PathApi.getMarker().setTarget(Rs2PathApi.getMarker().getWorldPoint());
        Rs2PathApi.getMarker().setJumpOnClick(true);
        wmm.add(Rs2PathApi.getMarker());

        WorldPoint start = Microbot.getClientThread().invoke(() -> {
            if (client.getTopLevelWorldView().isInstance()) {
                LocalPoint localLoc = Rs2Player.getLocalLocation();
                WorldPoint computed = localLoc != null ? WorldPoint.fromLocalInstance(client, localLoc) : null;
                if (computed == null) {
                    log.warn("[Walker] setTarget: instance localPoint conversion returned null (localLoc={} target={}) — falling back to raw world location",
                            localLoc, target);
                    computed = Rs2Player.getWorldLocation();
                }
                WorldPoint exitPortal = net.runelite.client.plugins.microbot.shortestpath.PohPanel.getExitPortalTile();
                if (exitPortal != null) {
                    Microbot.log("[Walker] In POH instance — remapping pathfinder start " + computed
                            + " -> exit portal " + exitPortal);
                    computed = exitPortal;
                }
                return computed;
            }
            return Rs2Player.getWorldLocation();
        });
        final Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        final WorldPoint effectiveStart = (Rs2PathApi.isStartPointSet() && pathfinder != null)
                ? pathfinder.getStart()
                : start;
        Rs2PathApi.setLastLocation(effectiveStart);
        Microbot.getClientThread().runOnSeperateThread(
                () -> restartPathfinding(effectiveStart, Set.of(target), newRequest));
    }

    public static boolean restartPathfinding(WorldPoint start, WorldPoint end) {
        return restartPathfinding(start, Set.of(end));
    }

    public static boolean restartPathfinding(WorldPoint start, Set<WorldPoint> ends) {
        return restartPathfinding(start, ends, false);
    }

    private static boolean restartPathfinding(WorldPoint start, Set<WorldPoint> ends, boolean newRequest) {
        RoutePlannerRuntime.Preparation preparation = newRequest
                ? RoutePlannerRuntime.beginNewRequest()
                : RoutePlannerRuntime.beginReplan();
        if (ends != null && !ends.isEmpty()) {
            int reachedDistance = Rs2Walker.config != null ? Math.max(0, Rs2Walker.config.reachedDistance()) : 0;
            boolean ordinaryEngineAllowed = Rs2Walker.config != null
                    && Rs2Walker.config.navigationEngineOrdinaryWalking()
                    && DoorInteractionOwnership.ordinaryEngineAllowed(start, ends);
            NavigationRouteOptions routeOptions = new NavigationRouteOptions(!Rs2Walker.disableTeleports,
                    Rs2Walker.config == null || Rs2Walker.config.useAgilityShortcuts(),
                    Rs2Walker.config != null && Rs2Walker.config.walkWithBankedTransports(),
                    ordinaryEngineAllowed,
                    Rs2Walker.config == null ? 10 : Rs2Walker.config.recalculateDistance());
            NavigationEngineRuntime.ensureRequest(new NavigationRequest(preparation.getRequestId(), ends,
                    reachedDistance, routeOptions, "rs2walker"));
        }
        WorldPoint refreshTarget = ends != null && !ends.isEmpty() ? ends.iterator().next() : null;
        Rs2PathApi.getPathfinderConfig().refresh(refreshTarget);
        if (Rs2Player.isInCave()) {
            // Cave pathfinding runs synchronously; the planner preparation above already
            // invalidated any asynchronous work in flight.
            Pathfinder pathfinder = new Pathfinder(Rs2PathApi.getPathfinderConfig(), start, ends);
            pathfinder.run();
            try {
                Rs2PathApi.getPathfinderConfig().setIgnoreTeleportAndItems(true);
                Pathfinder pathfinderWithoutTeleports = new Pathfinder(Rs2PathApi.getPathfinderConfig(), start, ends);
                pathfinderWithoutTeleports.run();

                boolean noTeleportPathAvailable = !pathfinderWithoutTeleports.getPath().isEmpty();
                boolean basePathAvailable = pathfinder != null && !pathfinder.getPath().isEmpty();
                if (!noTeleportPathAvailable) {
                    RoutePlannerRuntime.publishCompleted(preparation,
                            basePathAvailable ? pathfinder : pathfinderWithoutTeleports);
                    return true;
                }

                WorldPoint lastPath = pathfinderWithoutTeleports.getPath().get(pathfinderWithoutTeleports.getPath().size() - 1);
                int reachedDistance = Rs2Walker.config != null ? Rs2Walker.config.reachedDistance() : 10;
                boolean pathWithoutTeleportsIsReachable = lastPath.distanceTo(ends.stream().findFirst().orElse(lastPath)) <= reachedDistance;
                if (pathWithoutTeleportsIsReachable
                        && basePathAvailable
                        && pathfinder.getPath().size() >= pathfinderWithoutTeleports.getPath().size()) {
                    RoutePlannerRuntime.publishCompleted(preparation, pathfinderWithoutTeleports);
                } else {
                    RoutePlannerRuntime.publishCompleted(preparation,
                            basePathAvailable ? pathfinder : pathfinderWithoutTeleports);
                }
            } finally {
                Rs2PathApi.getPathfinderConfig().setIgnoreTeleportAndItems(false);
            }
        } else {
            RoutePlannerRuntime.submit(preparation,
                    new Pathfinder(Rs2PathApi.getPathfinderConfig(), start, ends));
        }
        return true;
    }
}
