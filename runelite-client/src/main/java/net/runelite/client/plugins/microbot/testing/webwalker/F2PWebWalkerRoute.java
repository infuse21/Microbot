package net.runelite.client.plugins.microbot.testing.webwalker;

import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class F2PWebWalkerRoute {
    static final List<F2PWebWalkerRoute> ROUTES = List.of(
            route("F2P-01", "Lumbridge courtyard to Lumbridge bank",
                    point(3222, 3218, 0), point(3208, 3220, 2), 1, 1),
            route("F2P-02", "Lumbridge bank to cow pen",
                    point(3208, 3220, 2), point(3253, 3266, 0), 1, 2),
            route("F2P-03", "Cow pen to Draynor bank",
                    point(3253, 3266, 0), point(3092, 3245, 0), 2, 2),
            route("F2P-04", "Draynor bank to Wizards' Tower bridge",
                    point(3092, 3245, 0), point(3109, 3168, 0), 2, 2),
            route("F2P-05", "Wizards' Tower bridge to Port Sarim docks",
                    point(3109, 3168, 0), point(3029, 3217, 0), 2, 2),
            route("F2P-06", "Port Sarim docks to Rimmington",
                    point(3029, 3217, 0), point(2957, 3214, 0), 2, 2),
            route("F2P-07", "Rimmington to Falador west bank",
                    point(2957, 3214, 0), point(2946, 3368, 0), 2, 2),
            route("F2P-08", "Falador west bank to Barbarian Village",
                    point(2946, 3368, 0), point(3082, 3420, 0), 2, 2),
            route("F2P-09", "Barbarian Village to Edgeville bank",
                    point(3082, 3420, 0), point(3093, 3493, 0), 2, 2),
            route("F2P-10", "Edgeville bank to Grand Exchange",
                    point(3093, 3493, 0), point(3164, 3486, 0), 2, 2),
            route("F2P-11", "Grand Exchange to Varrock west bank",
                    point(3164, 3486, 0), point(3185, 3441, 0), 2, 2),
            route("F2P-12", "Varrock west bank to Varrock east bank",
                    point(3185, 3441, 0), point(3253, 3420, 0), 2, 2),
            route("F2P-13", "Varrock east bank to Lumbridge courtyard",
                    point(3253, 3420, 0), point(3222, 3218, 0), 2, 2),
            route("F2P-14", "Draynor bank to Draynor Manor outside",
                    point(3092, 3245, 0), point(3109, 3341, 0), 2, 1),
            route("F2P-15", "Draynor Manor outside to Draynor Manor interior",
                    point(3109, 3341, 0), point(3106, 3363, 0), 1, 1),
            route("F2P-16", "Draynor Manor interior to Draynor bank",
                    point(3106, 3363, 0), point(3092, 3245, 0), 1, 2),
            currentRoute("F2P-17", "Current location to Varrock Sewers",
                    point(3237, 9858, 0), 1, 1, 5, true, true)
    );

    final String id;
    final String name;
    final WorldPoint start;
    final WorldPoint destination;
    final int startTolerance;
    final int destinationTolerance;
    final int repetitions;
    final boolean currentLocationStart;
    final boolean requireF2PWorld;
    final boolean forceNoAgilityShortcuts;
    final boolean forceNoTeleports;

    private F2PWebWalkerRoute(
            String id,
            String name,
            WorldPoint start,
            WorldPoint destination,
            int startTolerance,
            int destinationTolerance,
            int repetitions,
            boolean currentLocationStart,
            boolean requireF2PWorld,
            boolean forceNoAgilityShortcuts,
            boolean forceNoTeleports
    ) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.destination = destination;
        this.startTolerance = startTolerance;
        this.destinationTolerance = destinationTolerance;
        this.repetitions = repetitions;
        this.currentLocationStart = currentLocationStart;
        this.requireF2PWorld = requireF2PWorld;
        this.forceNoAgilityShortcuts = forceNoAgilityShortcuts;
        this.forceNoTeleports = forceNoTeleports;
    }

    static List<F2PWebWalkerRoute> selected(String routeFilter) {
        if (routeFilter == null || routeFilter.isBlank() || "all".equalsIgnoreCase(routeFilter)) {
            return ROUTES;
        }

        List<String> requested = Arrays.stream(routeFilter.split(","))
                .map(id -> id.trim().toUpperCase(Locale.ROOT))
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        List<F2PWebWalkerRoute> routes = ROUTES.stream()
                .filter(route -> requested.contains(route.id.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toList());

        if (routes.size() != requested.size()) {
            List<String> known = ROUTES.stream().map(route -> route.id).collect(Collectors.toList());
            throw new IllegalArgumentException("Unknown webwalker route filter '" + routeFilter + "'. Known routes: " + known);
        }

        return routes;
    }

    private static F2PWebWalkerRoute route(
            String id,
            String name,
            WorldPoint start,
            WorldPoint destination,
            int startTolerance,
            int destinationTolerance
    ) {
        return new F2PWebWalkerRoute(id, name, start, destination, startTolerance, destinationTolerance,
                1, false, false, false, false);
    }

    private static F2PWebWalkerRoute currentRoute(
            String id,
            String name,
            WorldPoint destination,
            int startTolerance,
            int destinationTolerance,
            int repetitions,
            boolean forceNoAgilityShortcuts,
            boolean forceNoTeleports
    ) {
        return new F2PWebWalkerRoute(id, name, null, destination, startTolerance, destinationTolerance,
                repetitions, true, true, forceNoAgilityShortcuts, forceNoTeleports);
    }

    private static WorldPoint point(int x, int y, int plane) {
        return new WorldPoint(x, y, plane);
    }
}
