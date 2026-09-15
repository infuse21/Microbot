package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PohInsideTabletTest
{
    private static final WorldPoint HOUSE = new WorldPoint(1859, 7051, 0);

    @Test
    public void insideActionRequiresTheConfiguredHouseAndConsumableTablet()
    {
        try (MockedStatic<PohPanel> panel = mockStatic(PohPanel.class))
        {
            panel.when(PohPanel::getExitPortalTile).thenReturn(HOUSE);
            Transport tablet = tablet(HOUSE, true);
            assertTrue(ItemTeleportPolicy.isEligible(tablet));
            assertEquals("Inside", ItemTeleportPolicy.inventoryAction(tablet));
            assertNull(ItemTeleportPolicy.equipmentAction(tablet));
            assertFalse(ItemTeleportPolicy.isEligible(tablet(HOUSE, false)));
            assertFalse(ItemTeleportPolicy.isEligible(tablet(new WorldPoint(3200, 3200, 0), true)));
            panel.when(PohPanel::getExitPortalTile).thenReturn(null);
            assertFalse(ItemTeleportPolicy.isEligible(tablet));
        }
    }

    @Test
    public void matchingCoordinatesAloneCannotAcknowledgeHouseEntry()
    {
        try (MockedStatic<PohPanel> panel = mockStatic(PohPanel.class);
             MockedStatic<PohTeleports> house = mockStatic(PohTeleports.class))
        {
            panel.when(PohPanel::getExitPortalTile).thenReturn(HOUSE);
            PlannedEdge edge = new PlannedEdge(new WorldPoint(3200, 3200, 0), HOUSE);
            Rs2ItemTeleportScene scene = new Rs2ItemTeleportScene();
            assertFalse(scene.hasLanded(edge, HOUSE));
            house.when(PohTeleports::isInHouse).thenReturn(true);
            assertTrue(scene.hasLanded(edge, HOUSE));
            assertFalse(scene.hasLanded(edge, new WorldPoint(HOUSE.getX(), HOUSE.getY(), 1)));
            assertFalse(scene.hasLanded(edge, edge.from()));
            assertFalse(scene.hasLanded(edge, null));
        }
    }

    private static Transport tablet(WorldPoint destination, boolean consumable)
    {
        return new Transport(destination, "Teleport to House tablet: Inside",
                TransportType.TELEPORTATION_ITEM, true, 19, Set.of(Set.of(8013)), consumable);
    }

    @Test
    public void houseSpellRetainsOwnershipUntilTheHouseSceneIsObserved()
    {
        try (MockedStatic<PohPanel> panel = mockStatic(PohPanel.class);
             MockedStatic<PohTeleports> house = mockStatic(PohTeleports.class);
             MockedStatic<Rs2PathApi> pathApi = mockStatic(Rs2PathApi.class))
        {
            panel.when(PohPanel::getExitPortalTile).thenReturn(HOUSE);
            pathApi.when(Rs2PathApi::getTransports).thenReturn(java.util.Collections.emptyMap());
            WorldPoint origin = new WorldPoint(3200, 3200, 0);
            RouteInteraction pending = new RouteInteraction(1, 0, origin, HOUSE, origin,
                    RouteInteraction.Kind.SIMPLE_TELEPORT, RouteInteraction.Status.AVAILABLE,
                    "Teleport to House", true, TransportType.TELEPORTATION_SPELL.ordinal(), origin, HOUSE);
            SimpleTeleportRouteScanner scanner = new SimpleTeleportRouteScanner();
            Rs2SimpleTeleportScene scene = new Rs2SimpleTeleportScene();

            assertEquals(RouteInteraction.Status.AVAILABLE,
                    scanner.observePending(pending, HOUSE, scene).getStatus());
            house.when(PohTeleports::isInHouse).thenReturn(true);
            assertEquals(RouteInteraction.Status.CLEARED,
                    scanner.observePending(pending, HOUSE, scene).getStatus());
            assertEquals(RouteInteraction.Status.AVAILABLE,
                    scanner.observePending(pending, origin, scene).getStatus());
            assertEquals(RouteInteraction.Status.AVAILABLE,
                    scanner.observePending(pending, new WorldPoint(HOUSE.getX(), HOUSE.getY(), 1), scene).getStatus());
        }
    }

    @Test
    public void capeRequiresDirectedPreferenceAndRejectsAChangedLiveSetting()
    {
        try (MockedStatic<PohPanel> panel = mockStatic(PohPanel.class);
             MockedStatic<Microbot> microbot = mockStatic(Microbot.class))
        {
            panel.when(PohPanel::getExitPortalTile).thenReturn(HOUSE);
            Transport cape = new Transport(HOUSE, "Construction cape: Tele to POH",
                    TransportType.TELEPORTATION_ITEM, true, 19, Set.of(Set.of(9789), Set.of(9790)));
            assertFalse(ItemTeleportPolicy.isEligible(cape));
            cape.getVarbits().add(new TransportVarbit(VarbitID.POH_HOUSE_LOCATION, 1, TransportVarbit.Operator.EQUAL));
            cape.getVarbits().add(new TransportVarbit(VarbitID.POH_TELE_TOGGLE, 0, TransportVarbit.Operator.EQUAL));
            assertTrue(ItemTeleportPolicy.isEligible(cape));
            assertEquals("Tele to POH", ItemTeleportPolicy.inventoryAction(cape));
            assertEquals("Tele to POH", ItemTeleportPolicy.equipmentAction(cape));
            microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(1);
            assertTrue(Rs2SimpleTeleportScene.matchesHousePreference(cape));
            microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_TELE_TOGGLE)).thenReturn(1);
            assertFalse(Rs2SimpleTeleportScene.matchesHousePreference(cape));
            microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_TELE_TOGGLE)).thenReturn(0);
            microbot.when(() -> Microbot.getVarbitValue(VarbitID.POH_HOUSE_LOCATION)).thenReturn(2);
            assertFalse(Rs2SimpleTeleportScene.matchesHousePreference(cape));
        }
    }
}
