package net.runelite.client.plugins.microbot.util.death;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeathsOfficeLocationTest {
    @Test
    public void missingPlayerLocationHasNoDestination() {
        assertNull(DeathsOfficeLocation.getNearest(null));
    }

    @Test
    public void choosesNearbyEntranceEvenWhenPlayerIsUpstairs() {
        assertEquals(DeathsOfficeLocation.FALADOR,
                DeathsOfficeLocation.getNearest(new WorldPoint(2964, 3331, 2)));
    }

    @Test
    public void choosesLocalEntranceInsteadOfFirstEnumEntry() {
        assertEquals(DeathsOfficeLocation.EDGEVILLE,
                DeathsOfficeLocation.getNearest(new WorldPoint(3096, 3475, 0)));
    }
}
