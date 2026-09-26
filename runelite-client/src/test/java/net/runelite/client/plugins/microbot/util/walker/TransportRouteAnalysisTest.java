package net.runelite.client.plugins.microbot.util.walker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransportRouteAnalysisTest {
    private static final WorldPoint START = new WorldPoint(3200, 3200, 0);
    private static final WorldPoint TARGET = new WorldPoint(3210, 3210, 0);

    @Test
    public void snapshotsOrderedBankRouteTransportsWithoutDeduplicatingUses() {
        Transport tablet = new Transport(START, TARGET, "Test tablet",
                TransportType.TELEPORTATION_ITEM, true, 0);
        List<Transport> source = new ArrayList<>(List.of(tablet, tablet));

        TransportRouteAnalysis analysis = new TransportRouteAnalysis(
                List.of(START, TARGET), null, null, List.of(START), List.of(START, TARGET),
                "test", 1, 1, source);
        source.clear();

        assertEquals(2, analysis.getBankRouteTransports().size());
        assertSame(tablet, analysis.getBankRouteTransports().get(0));
        assertSame(tablet, analysis.getBankRouteTransports().get(1));
        try {
            analysis.getBankRouteTransports().clear();
            fail("route transport snapshot must be immutable");
        } catch (UnsupportedOperationException expected) {
            assertTrue(analysis.getBankRouteTransports().contains(tablet));
        }
    }

    @Test
    public void legacyConstructorsDefaultToNoCapturedBankRoute() {
        TransportRouteAnalysis analysis = new TransportRouteAnalysis(
                List.of(START, TARGET), null, null, List.of(START), List.of(START, TARGET), "test");

        assertTrue(analysis.getBankRouteTransports().isEmpty());
    }
}
