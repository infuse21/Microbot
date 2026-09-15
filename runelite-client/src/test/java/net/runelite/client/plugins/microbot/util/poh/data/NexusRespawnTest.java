package net.runelite.client.plugins.microbot.util.poh.data;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mockStatic;

public class NexusRespawnTest
{
    @Test
    public void namedFlagsSelectOnlyTheirOwnLandingWhenInstalled()
    {
        int[] flags = {621, 668, 3910, 9805, 10528, 12310};
        int[][] tiles = {{3095, 3469}, {2964, 3378}, {2757, 3479},
            {1682, 3135}, {3151, 3636}, {1630, 3674}};
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class))
        {
            for (int i = 0; i < flags.length; i++)
            {
                microbot.reset();
                int flag = flags[i];
                microbot.when(() -> Microbot.getVarbitValue(flag)).thenReturn(1);
                assertEquals(new WorldPoint(tiles[i][0], tiles[i][1], 0), NexusPortal.RESPAWN.getDestination());
                assertFalse(NexusPortal.getAvailableTeleports().contains(NexusPortal.RESPAWN));
                microbot.when(() -> Microbot.getVarbitValue(NexusPortal.VARBITS[0])).thenReturn(40);
                assertTrue(NexusPortal.getAvailableTeleports().contains(NexusPortal.RESPAWN));
            }
        }
    }

    @Test
    public void ambiguousConflictingAndUnknownFlagsNeverPublishRespawn()
    {
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class))
        {
            microbot.when(() -> Microbot.getVarbitValue(NexusPortal.VARBITS[0])).thenReturn(40);
            assertNull(NexusPortal.RESPAWN.getDestination());
            assertTrue(NexusPortal.getAvailableTeleports().isEmpty());
            microbot.when(() -> Microbot.getVarbitValue(621)).thenReturn(1);
            microbot.when(() -> Microbot.getVarbitValue(668)).thenReturn(1);
            assertNull(NexusPortal.RESPAWN.getDestination());
            assertTrue(NexusPortal.getAvailableTeleports().isEmpty());
            microbot.when(() -> Microbot.getVarbitValue(621)).thenReturn(0);
            microbot.when(() -> Microbot.getVarbitValue(668)).thenReturn(2);
            assertNull(NexusPortal.RESPAWN.getDestination());
            assertTrue(NexusPortal.getAvailableTeleports().isEmpty());
        }
    }
}
