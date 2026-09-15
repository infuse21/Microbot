package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.components.CheckboxPanel;
import net.runelite.client.plugins.microbot.shortestpath.components.EnumListPanel;
import net.runelite.client.plugins.microbot.shortestpath.components.ExitTilePanel;
import net.runelite.client.plugins.microbot.shortestpath.components.JewelleryBoxPanel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.*;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PohGeneratedGraphTest
{
    @Test
    public void configuredGraphIncludesEveryFacilityAndNeverPublishesUnknownLandings() throws Exception
    {
        PohPanel previous = PohPanel.instance;
        PohPanel panel = mock(PohPanel.class);
        ExitTilePanel tile = mock(ExitTilePanel.class);
        EnumListPanel<?> chambers = mock(EnumListPanel.class);
        EnumListPanel<?> nexus = mock(EnumListPanel.class);
        JewelleryBoxPanel jewellery = mock(JewelleryBoxPanel.class);
        CheckboxPanel features = mock(CheckboxPanel.class);
        JCheckBox rings = mock(JCheckBox.class);
        JCheckBox trees = mock(JCheckBox.class);
        set(features, CheckboxPanel.class, "fairyRingCb", rings);
        set(features, CheckboxPanel.class, "spiritTreeCb", trees);
        set(panel, PohPanel.class, "tilePanel", tile);
        set(panel, PohPanel.class, "portalPanel", chambers);
        set(panel, PohPanel.class, "nexusPanel", nexus);
        set(panel, PohPanel.class, "jewelleryBoxPanel", jewellery);
        set(panel, PohPanel.class, "checkboxPanel", features);
        WorldPoint anchor = new WorldPoint(1859, 7051, 0);
        when(tile.getTile()).thenReturn(anchor);
        when(rings.isSelected()).thenReturn(true);
        when(trees.isSelected()).thenReturn(true);
        Set<PohTeleport> mounted = new HashSet<>();
        mounted.addAll(Arrays.asList(MountedGlory.values()));
        mounted.addAll(Arrays.asList(MountedMythical.values()));
        mounted.addAll(Arrays.asList(MountedDigsite.values()));
        mounted.addAll(Arrays.asList(MountedXerics.values()));
        when(features.getTeleports()).thenReturn(mounted);
        when(chambers.getTeleports()).thenReturn(new HashSet<>(Arrays.asList(PohPortal.values())));
        when(nexus.getTeleports()).thenReturn(new HashSet<>(Arrays.asList(NexusPortal.values())));
        when(jewellery.getTeleports()).thenReturn(new HashSet<>(Arrays.asList(JewelleryBox.values())));
        Set<PohTeleport> expected = new HashSet<>(mounted);
        expected.addAll(chambers.getTeleports());
        expected.addAll(nexus.getTeleports());
        expected.addAll(jewellery.getTeleports());
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
             MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class))
        {
            PohPanel.instance = panel;
            microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(1);
            microbot.when(() -> Microbot.getVarbitValue(VarbitID.FALADOR_SPAWN)).thenReturn(1);
            Map<WorldPoint, Set<Transport>> resources = Transport.loadAllFromResources();
            var graph = PohPanel.getAvailableTransports(resources);
            assertEquals(expected, facilities(graph));
            assertTrue(graph.values().stream().flatMap(Set::stream).anyMatch(t -> t.getType() == TransportType.FAIRY_RING));
            assertTrue(graph.values().stream().flatMap(Set::stream).anyMatch(t -> t.getType() == TransportType.SPIRIT_TREE));
            assertOwned(graph);
            Map<WorldPoint, Set<Transport>> inbound = PohPanel.getTransportsToPoh();
            Transport tablet = inbound.get(null).stream().filter(t ->
                    "Teleport to House tablet: Inside".equals(t.getDisplayInfo())).findFirst().orElseThrow();
            assertTrue(tablet.isConsumable());
            assertEquals(RouteEdge.Kind.ITEM_TELEPORT,
                    PathfinderRouteCalculation.classifyTransportEdge(Set.of(tablet)));

            microbot.when(() -> Microbot.getVarbitValue(VarbitID.FALADOR_SPAWN)).thenReturn(0);
            expected.remove(NexusPortal.RESPAWN);
            when(rings.isSelected()).thenReturn(false);
            when(trees.isSelected()).thenReturn(false);
            graph = PohPanel.getAvailableTransports(resources);
            assertEquals(expected, facilities(graph));
            assertFalse(graph.values().stream().flatMap(Set::stream).anyMatch(t ->
                    t.getType() == TransportType.FAIRY_RING || t.getType() == TransportType.SPIRIT_TREE));
            assertOwned(graph);
            assertOwned(inbound);
            for (HouseLocation house : HouseLocation.values())
            {
                microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_HOUSE_LOCATION))
                        .thenReturn(house.getVarbitValue());
                inbound = PohPanel.getTransportsToPoh();
                assertOwned(inbound);
                assertTrue(inbound.get(house.getPortalLocation()).stream().anyMatch(t ->
                        t.getDestination().equals(anchor) && t.getType() == TransportType.POH));
                for (int preference : new int[]{0, 1, 2})
                {
                    microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_TELE_TOGGLE)).thenReturn(preference);
                    Set<Transport> selected = inbound.get(null).stream().filter(t ->
                            !t.getDisplayInfo().contains("tablet")).filter(t -> t.getVarbits().stream()
                            .allMatch(gate -> gate.matches(Microbot.getVarbitValue(gate.getVarbitId()))))
                            .collect(Collectors.toSet());
                    assertEquals(preference == 2 ? 0 : 2, selected.size());
                    for (Transport teleport : selected)
                    {
                        assertEquals(preference == 0 ? anchor : house.getPortalLocation(), teleport.getDestination());
                    }
                }
            }
        }
        finally
        {
            PohPanel.instance = previous;
        }
    }

    private static Set<PohTeleport> facilities(Map<WorldPoint, Set<Transport>> graph)
    {
        return graph.values().stream().flatMap(Set::stream).filter(PohTransport.class::isInstance)
                .map(t -> ((PohTransport) t).getTeleport()).collect(Collectors.toSet());
    }

    private static void assertOwned(Map<WorldPoint, Set<Transport>> graph)
    {
        assertFalse(graph.isEmpty());
        Set<String> unsupported = new java.util.TreeSet<>();
        for (Set<Transport> rows : graph.values())
        {
            for (Transport row : rows)
            {
                assertNotNull(row.getDisplayInfo(), row.getDestination());
                if (PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)) == RouteEdge.Kind.TRANSPORT)
                {
                    unsupported.add(row.getDisplayInfo());
                }
            }
        }
        assertTrue("Generated routes still require legacy ownership: " + unsupported, unsupported.isEmpty());
    }

    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception
    {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
