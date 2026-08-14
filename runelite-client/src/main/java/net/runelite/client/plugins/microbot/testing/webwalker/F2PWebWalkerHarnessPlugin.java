package net.runelite.client.plugins.microbot.testing.webwalker;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.testing.TestResult;
import net.runelite.client.plugins.microbot.testing.TestResultWriter;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

@PluginDescriptor(
        name = "F2P Web Walker Harness",
        description = "Runs F2P-only in-game webwalker regression routes in test mode",
        tags = {"microbot", "test", "webwalker", "f2p"},
        hidden = true
)
@Slf4j
public class F2PWebWalkerHarnessPlugin extends Plugin {
    private static final String ROUTE_FILTER_PROPERTY = "microbot.webwalker.case";
    private static final String TEST_ROUTE_FILTER_PROPERTY = "microbot.test.webwalker.case";
    private static final String STOP_ON_FAILURE_PROPERTY = "microbot.webwalker.stopOnFailure";
    private static final String TEST_STOP_ON_FAILURE_PROPERTY = "microbot.test.webwalker.stopOnFailure";
    private static final String WALK_TIMEOUT_PROPERTY = "microbot.webwalker.walkTimeoutMs";
    private static final String TEST_WALK_TIMEOUT_PROPERTY = "microbot.test.webwalker.walkTimeoutMs";
    private static final String USE_TELEPORTATION_SPELLS_PROPERTY = "microbot.webwalker.useTeleportationSpells";
    private static final String TEST_USE_TELEPORTATION_SPELLS_PROPERTY = "microbot.test.webwalker.useTeleportationSpells";
    private static final String TEST_SCRIPT_PROPERTY = "microbot.test.script";
    private static final String SCRIPT_NAME = "F2P Web Walker Harness";
    private static final int DEFAULT_WALK_TIMEOUT_MS = 240000;

    @Inject
    private EventBus eventBus;

    private ExecutorService executor;
    private volatile WorldPoint lastLocation;

    @Override
    protected void startUp() {
        if (!isHarnessTarget()) {
            return;
        }

        executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("F2PWebWalkerHarness-%d")
                .build());
        executor.submit(this::runHarness);
    }

    @Override
    protected void shutDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!isHarnessTarget()) {
            return;
        }

        Player player = Microbot.getClient().getLocalPlayer();
        if (player != null) {
            lastLocation = player.getWorldLocation();
        }
    }

    private void runHarness() {
        WebWalkerTestResult result = new WebWalkerTestResult(SCRIPT_NAME);
        result.routeFilter = property(TEST_ROUTE_FILTER_PROPERTY, ROUTE_FILTER_PROPERTY, "all");
        result.stopOnFailure = Boolean.parseBoolean(property(TEST_STOP_ON_FAILURE_PROPERTY, STOP_ON_FAILURE_PROPERTY, "true"));
        result.walkTimeoutMs = intProperty(TEST_WALK_TIMEOUT_PROPERTY, WALK_TIMEOUT_PROPERTY, DEFAULT_WALK_TIMEOUT_MS);

        int exitCode = 0;
        try {
            List<F2PWebWalkerRoute> routes = F2PWebWalkerRoute.selected(result.routeFilter);
            result.selectedRoutes = routes.stream().map(route -> route.id).collect(Collectors.toList());

            if (!sleepUntil(() -> safeLocation() != null, 60000)) {
                result.addError("Timed out waiting for local player location before starting webwalker routes");
                result.complete("login_failure");
                writeAndExit(result, result.exitCode);
                return;
            }

            log.info("[F2PWebWalkerHarness] Starting {} route(s): {}", routes.size(), result.selectedRoutes);
            for (F2PWebWalkerRoute route : routes) {
                if (Thread.currentThread().isInterrupted()) {
                    result.addError("Harness interrupted before route " + route.id);
                    exitCode = 1;
                    break;
                }

                applyShortestPathOverrides(route);
                RouteOutcome outcome = runRoute(route, result.walkTimeoutMs);
                result.routes.add(outcome);

                result.addCheck(route.id + " setup", outcome.setupPassed, outcome.setupError);
                result.addCheck(route.id + " route", outcome.passed, outcome.error);

                if (!outcome.passed) {
                    exitCode = 1;
                    if (result.stopOnFailure) {
                        log.warn("[F2PWebWalkerHarness] Stopping on first failed route: {}", route.id);
                        break;
                    }
                }
            }

            result.complete("completed");
            writeAndExit(result, exitCode == 0 ? result.exitCode : exitCode);
        } catch (Throwable t) {
            log.error("[F2PWebWalkerHarness] Harness crashed", t);
            result.addError(t.getClass().getSimpleName() + ": " + t.getMessage());
            result.complete("crash");
            writeAndExit(result, result.exitCode);
        }
    }

    private RouteOutcome runRoute(F2PWebWalkerRoute route, int walkTimeoutMs) {
        RouteOutcome outcome = new RouteOutcome();
        outcome.id = route.id;
        outcome.name = route.name;
        outcome.destination = format(route.destination);
        outcome.startTolerance = route.startTolerance;
        outcome.destinationTolerance = route.destinationTolerance;
        outcome.repetitions = route.repetitions;
        outcome.currentLocationStart = route.currentLocationStart;
        outcome.requireF2PWorld = route.requireF2PWorld;
        outcome.forceNoAgilityShortcuts = route.forceNoAgilityShortcuts;
        outcome.forceNoTeleports = route.forceNoTeleports;
        outcome.startedAt = Instant.now().toString();
        outcome.setupPassed = true;

        log.info("[F2PWebWalkerHarness] Running {}: {} -> {}", route.id, route.start, route.destination);

        WorldPoint current = safeLocation();
        outcome.initialLocation = format(current);
        if (current == null) {
            outcome.setupError = "Player location unavailable before setup";
            outcome.error = outcome.setupError;
            return outcome;
        }

        boolean membersWorld = Microbot.getClient().getWorldType().contains(WorldType.MEMBERS);
        outcome.membersWorld = membersWorld;
        if (route.requireF2PWorld && membersWorld) {
            outcome.setupPassed = false;
            outcome.setupError = "Route " + route.id + " requires a F2P world, but the client is on a members world";
            outcome.error = outcome.setupError;
            outcome.finishedAt = Instant.now().toString();
            return outcome;
        }

        WorldPoint start = route.currentLocationStart ? current : route.start;
        outcome.start = format(start);
        if (start == null) {
            outcome.setupPassed = false;
            outcome.setupError = "Start location unavailable for " + route.id;
            outcome.error = outcome.setupError;
            outcome.finishedAt = Instant.now().toString();
            return outcome;
        }

        for (int i = 1; i <= route.repetitions; i++) {
            RouteAttemptOutcome attempt = runAttempt(route, start, i, walkTimeoutMs);
            outcome.attempts.add(attempt);
            outcome.initialDistanceToStart = i == 1 ? attempt.initialDistanceToStart : outcome.initialDistanceToStart;
            outcome.setupState = attempt.setupState;
            outcome.setupDurationMs += attempt.setupDurationMs;
            outcome.routeStartLocation = attempt.routeStartLocation;
            outcome.distanceToStartAfterSetup = attempt.distanceToStartAfterSetup;
            outcome.walkerState = attempt.walkerState;
            outcome.walkDurationMs += attempt.walkDurationMs;
            outcome.endLocation = attempt.endLocation;
            outcome.distanceToDestination = attempt.distanceToDestination;

            if (!attempt.setupPassed) {
                outcome.setupPassed = false;
                outcome.setupError = attempt.setupError;
                outcome.error = attempt.setupError;
                break;
            }

            if (!attempt.passed) {
                outcome.error = attempt.error;
                break;
            }

            outcome.successfulAttempts++;
        }

        outcome.passed = outcome.setupPassed && outcome.successfulAttempts == route.repetitions;
        outcome.finishedAt = Instant.now().toString();
        log.info("[F2PWebWalkerHarness] {} finished: passed={}, attempts={}/{}, state={}, end={}, distance={}, duration={}ms",
                route.id, outcome.passed, outcome.successfulAttempts, route.repetitions, outcome.walkerState,
                outcome.endLocation, outcome.distanceToDestination, outcome.walkDurationMs);
        return outcome;
    }

    private RouteAttemptOutcome runAttempt(F2PWebWalkerRoute route, WorldPoint start, int attemptNumber, int walkTimeoutMs) {
        RouteAttemptOutcome attempt = new RouteAttemptOutcome();
        attempt.attempt = attemptNumber;
        attempt.start = format(start);
        attempt.destination = format(route.destination);
        attempt.startedAt = Instant.now().toString();

        WorldPoint current = safeLocation();
        attempt.initialLocation = format(current);
        attempt.initialDistanceToStart = distance(current, start);
        if (attempt.initialDistanceToStart > route.startTolerance) {
            long setupStart = System.currentTimeMillis();
            attempt.setupState = walk(start, route.startTolerance, walkTimeoutMs);
            attempt.setupDurationMs = System.currentTimeMillis() - setupStart;
        } else {
            attempt.setupState = WalkerState.ARRIVED.name();
            attempt.setupDurationMs = 0;
        }

        WorldPoint beforeRoute = safeLocation();
        attempt.routeStartLocation = format(beforeRoute);
        attempt.distanceToStartAfterSetup = distance(beforeRoute, start);
        attempt.setupPassed = WalkerState.ARRIVED.name().equals(attempt.setupState)
                && attempt.distanceToStartAfterSetup <= route.startTolerance;
        if (!attempt.setupPassed) {
            attempt.setupError = "Setup webwalk failed for " + route.id + " attempt " + attemptNumber
                    + ": state=" + attempt.setupState
                    + ", location=" + attempt.routeStartLocation
                    + ", distanceToStart=" + attempt.distanceToStartAfterSetup;
            attempt.error = attempt.setupError;
            attempt.finishedAt = Instant.now().toString();
            return attempt;
        }

        long walkStart = System.currentTimeMillis();
        attempt.walkerState = walk(route.destination, route.destinationTolerance, walkTimeoutMs);
        attempt.walkDurationMs = System.currentTimeMillis() - walkStart;

        WorldPoint end = safeLocation();
        attempt.endLocation = format(end);
        attempt.distanceToDestination = distance(end, route.destination);
        attempt.passed = WalkerState.ARRIVED.name().equals(attempt.walkerState)
                && attempt.distanceToDestination <= route.destinationTolerance;
        if (!attempt.passed) {
            attempt.error = "Route webwalk failed for " + route.id + " attempt " + attemptNumber
                    + ": state=" + attempt.walkerState
                    + ", location=" + attempt.endLocation
                    + ", distanceToDestination=" + attempt.distanceToDestination;
        }

        attempt.finishedAt = Instant.now().toString();
        return attempt;
    }

    private String walk(WorldPoint destination, int tolerance, int timeoutMs) {
        ExecutorService walkExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("F2PWebWalkerLeg-%d")
                .build());
        Future<WalkerState> future = walkExecutor.submit(() -> Rs2Walker.walkWithState(destination, tolerance));

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS).name();
        } catch (TimeoutException e) {
            future.cancel(true);
            Rs2Walker.setTarget(null);
            return "TIMEOUT";
        } catch (Exception e) {
            log.warn("[F2PWebWalkerHarness] Webwalker leg failed for destination {}", destination, e);
            return "ERROR:" + e.getClass().getSimpleName();
        } finally {
            walkExecutor.shutdownNow();
        }
    }

    private WorldPoint safeLocation() {
        return lastLocation;
    }

    private void applyShortestPathOverrides(F2PWebWalkerRoute route) {
        Map<String, Object> config = new HashMap<>();

        String value = property(TEST_USE_TELEPORTATION_SPELLS_PROPERTY, USE_TELEPORTATION_SPELLS_PROPERTY, "");
        if (!value.isBlank()) {
            config.put("useTeleportationSpells", Boolean.parseBoolean(value));
        }

        if (route.forceNoAgilityShortcuts) {
            config.put("useAgilityShortcuts", false);
            config.put("useGrappleShortcuts", false);
        }

        if (route.forceNoTeleports) {
            config.put("useTeleportationItems", "None");
            config.put("useTeleportationLevers", false);
            config.put("useTeleportationMinigames", false);
            config.put("useTeleportationPortals", false);
            config.put("useTeleportationSpells", false);
            config.put("useWildernessObelisks", false);
        }

        if (config.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("config", config);

        eventBus.post(new PluginMessage("shortestpath", "path", data));
        log.info("[F2PWebWalkerHarness] Applied shortest path overrides for {}: {}", route.id, config);
    }

    private static boolean isHarnessTarget() {
        return "true".equals(System.getProperty("microbot.test.mode"))
                && System.getProperty(TEST_SCRIPT_PROPERTY, "").contains(SCRIPT_NAME);
    }

    private static String property(String preferred, String legacy, String defaultValue) {
        String value = System.getProperty(preferred);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getProperty(legacy);
        if (value != null && !value.isBlank()) {
            return value;
        }

        return defaultValue;
    }

    private static int intProperty(String preferred, String legacy, int defaultValue) {
        String value = property(preferred, legacy, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int distance(WorldPoint from, WorldPoint to) {
        if (from == null || to == null) {
            return Integer.MAX_VALUE;
        }
        return from.distanceTo(to);
    }

    private static String format(WorldPoint point) {
        if (point == null) {
            return null;
        }
        return point.getX() + "," + point.getY() + "," + point.getPlane();
    }

    private static void writeAndExit(WebWalkerTestResult result, int exitCode) {
        TestResultWriter.write(result);
        System.exit(exitCode);
    }

    public static class WebWalkerTestResult extends TestResult {
        public String routeFilter;
        public boolean stopOnFailure;
        public int walkTimeoutMs;
        public List<String> selectedRoutes = new ArrayList<>();
        public List<RouteOutcome> routes = new ArrayList<>();

        public WebWalkerTestResult(String script) {
            super(script);
        }
    }

    public static class RouteOutcome {
        public String id;
        public String name;
        public String start;
        public String destination;
        public int startTolerance;
        public int destinationTolerance;
        public int repetitions;
        public int successfulAttempts;
        public boolean currentLocationStart;
        public boolean requireF2PWorld;
        public boolean membersWorld;
        public boolean forceNoAgilityShortcuts;
        public boolean forceNoTeleports;
        public String startedAt;
        public String finishedAt;
        public String initialLocation;
        public int initialDistanceToStart;
        public String setupState;
        public boolean setupPassed;
        public String setupError;
        public long setupDurationMs;
        public String routeStartLocation;
        public int distanceToStartAfterSetup;
        public String walkerState;
        public boolean passed;
        public String error;
        public long walkDurationMs;
        public String endLocation;
        public int distanceToDestination;
        public List<RouteAttemptOutcome> attempts = new ArrayList<>();
    }

    public static class RouteAttemptOutcome {
        public int attempt;
        public String start;
        public String destination;
        public String startedAt;
        public String finishedAt;
        public String initialLocation;
        public int initialDistanceToStart;
        public String setupState;
        public boolean setupPassed;
        public String setupError;
        public long setupDurationMs;
        public String routeStartLocation;
        public int distanceToStartAfterSetup;
        public String walkerState;
        public boolean passed;
        public String error;
        public long walkDurationMs;
        public String endLocation;
        public int distanceToDestination;
    }
}
