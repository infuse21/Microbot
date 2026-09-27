package net.runelite.client.plugins.microbot.util.walker;

import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.WorldPointUtil;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2PathApiCompatibilityTest
{
    private PathfinderConfig previousConfig;

    @Before
    public void saveConfig()
    {
        previousConfig = Rs2PathApi.getPathfinderConfig();
    }

    @After
    public void restoreConfig()
    {
        Rs2PathApi.setPathfinderConfig(previousConfig);
    }

    @Test
    public void queriesAreSafeBeforeConfiguration()
    {
        Rs2PathApi.setPathfinderConfig(null);
        WorldPoint point = new WorldPoint(3200, 3200, 0);
        assertFalse(Rs2PathApi.hasCatalogTransportOrigin(point));
        assertFalse(Rs2PathApi.hasCatalogTransportEdge(point, point));
        assertFalse(Rs2PathApi.shouldAvoidDangerousTile(point));
        assertFalse(Rs2PathApi.isSpiritTreeTravelEnabled());
        assertFalse(Rs2PathApi.isInWilderness(null));
        assertFalse(Rs2PathApi.isInWilderness(point));
        assertTrue(Rs2PathApi.isInWilderness(new WorldPoint(3200, 3700, 0)));
    }

    @Test
    public void catalogQueriesUseUnfilteredDirectedEdges()
    {
        PathfinderConfig config = mock(PathfinderConfig.class);
        Transport transport = mock(Transport.class);
        WorldPoint origin = new WorldPoint(3200, 3200, 0);
        WorldPoint destination = new WorldPoint(3200, 3201, 1);
        when(transport.getDestination()).thenReturn(destination);
        when(config.getAllTransports()).thenReturn(Map.of(origin, Set.of(transport)));
        Rs2PathApi.setPathfinderConfig(config);

        assertTrue(Rs2PathApi.hasCatalogTransportOrigin(origin));
        assertTrue(Rs2PathApi.hasCatalogTransportEdge(origin, destination));
        assertFalse(Rs2PathApi.hasCatalogTransportEdge(destination, origin));
        assertFalse(Rs2PathApi.hasCatalogTransportEdge(origin, origin));
        assertFalse(Rs2PathApi.hasCatalogTransportEdge(origin, null));
        assertFalse(Rs2PathApi.hasCatalogTransportOrigin(null));
    }

    @Test
    public void policyQueriesHonorRefreshedConfiguration()
    {
        PathfinderConfig config = mock(PathfinderConfig.class);
        WorldPoint point = new WorldPoint(3200, 3200, 0);
        when(config.isDangerousAdjacentTile(WorldPointUtil.packWorldPoint(point))).thenReturn(true);
        Rs2PathApi.setPathfinderConfig(config);
        assertFalse(Rs2PathApi.shouldAvoidDangerousTile(point));
        assertFalse(Rs2PathApi.isSpiritTreeTravelEnabled());
        when(config.isAvoidDangerousNpcs()).thenReturn(true);
        when(config.isUseSpiritTrees()).thenReturn(true);
        assertTrue(Rs2PathApi.shouldAvoidDangerousTile(point));
        assertFalse(Rs2PathApi.shouldAvoidDangerousTile(null));
        assertTrue(Rs2PathApi.isSpiritTreeTravelEnabled());
    }
}
