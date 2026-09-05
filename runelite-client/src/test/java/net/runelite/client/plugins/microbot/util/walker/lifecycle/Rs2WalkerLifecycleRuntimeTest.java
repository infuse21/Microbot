package net.runelite.client.plugins.microbot.util.walker.lifecycle;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationDecision;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngineRuntime;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationObservation;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationSnapshot;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlannerRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2WalkerLifecycleRuntimeTest
{
    private static final WorldPoint START = new WorldPoint(3313, 3233, 0);
    private static final WorldPoint TARGET = new WorldPoint(3317, 3233, 0);
    private ShortestPathConfig originalConfig;

    @Before public void setUp()
    {
        originalConfig = Rs2Walker.config;
        Rs2Walker.config = mock(ShortestPathConfig.class);
        when(Rs2Walker.config.reachedDistance()).thenReturn(5);
        NavigationEngineRuntime.cancel("radius-test-reset");
        RoutePlannerRuntime.cancel();
    }

    @After public void tearDown()
    {
        NavigationEngineRuntime.cancel("radius-test-reset");
        RoutePlannerRuntime.cancel();
        Rs2Walker.config = originalConfig;
    }

    @Test public void explicitRadiusOverridesSidebarDefaultAndControlsArrival()
    {
        for (int radius : new int[]{0, 3, 6})
        {
            RoutePlannerRuntime.Preparation preparation = Rs2WalkerLifecycleRuntime.prepareRouteRequest(
                    START, Set.of(TARGET), true, radius);
            NavigationSnapshot snapshot = NavigationEngineRuntime.getSnapshot();
            assertEquals(radius, snapshot.getRequest().getReachedDistance());
            RoutePlan plan = new RoutePlan(preparation.getRequestId(), 1, START, Set.of(TARGET),
                    Arrays.asList(START, new WorldPoint(3314, 3233, 0), new WorldPoint(3315, 3233, 0),
                            new WorldPoint(3316, 3233, 0), TARGET), Arrays.asList(START, TARGET), true);
            NavigationDecision decision = NavigationEngineRuntime.observe(NavigationObservation.route(
                    1, START, plan, false, false, false, false, false, false, null, "radius-test"));
            assertEquals(radius >= 4 ? NavigationDecision.Type.COMPLETE : NavigationDecision.Type.CLICK_TILE,
                    decision.getType());
        }
    }

    @Test public void replanKeepsOriginalRadiusWhenSidebarChanges()
    {
        Rs2WalkerLifecycleRuntime.prepareRouteRequest(START, Set.of(TARGET), true, 0);
        NavigationSnapshot snapshot = NavigationEngineRuntime.getSnapshot();
        when(Rs2Walker.config.reachedDistance()).thenReturn(12);
        int radius = Rs2WalkerLifecycleRuntime.reachedDistanceFor(Set.of(TARGET));
        assertEquals(0, radius);
        Rs2WalkerLifecycleRuntime.prepareRouteRequest(START, Set.of(TARGET), false, radius);
        assertSame(snapshot.getRequest(), NavigationEngineRuntime.getSnapshot().getRequest());
    }

    @Test public void unrelatedAndTerminalRequestsDoNotLeakTheirRadius()
    {
        assertEquals(5, Rs2WalkerLifecycleRuntime.reachedDistanceFor(Set.of(TARGET)));
        Rs2WalkerLifecycleRuntime.prepareRouteRequest(START, Set.of(TARGET), true, 0);
        assertEquals(5, Rs2WalkerLifecycleRuntime.reachedDistanceFor(Set.of(START)));
        NavigationEngineRuntime.cancel("test-complete");
        assertEquals(5, Rs2WalkerLifecycleRuntime.reachedDistanceFor(Set.of(TARGET)));
    }
}
