package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.util.poh.data.HouseLocation;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PohPanelTest
{
	@Test
    public void housePortalGraphContainsBothDirectedEdgesInTheirOwningSets()
    {
        WorldPoint inside = new WorldPoint(1859, 7051, 0);
        WorldPoint outside = HouseLocation.RIMMINGTON.getPortalLocation();
        Map<WorldPoint, Set<Transport>> entries = PohPanel.createHouseEntryPortalTransport(
            inside, HouseLocation.RIMMINGTON);
        Map<WorldPoint, Set<Transport>> exits = PohPanel.createHouseExitPortalTransport(
            inside, HouseLocation.RIMMINGTON);

        Transport entry = find(entries.get(outside), inside);
        assertEquals(TransportType.POH, entry.getType());
        assertEquals("Home", entry.getAction());
        assertEquals(HouseLocation.RIMMINGTON.getPortalId(), entry.getObjectId());

        Transport exit = find(exits.get(inside), outside);
        assertEquals(TransportType.POH, exit.getType());
		assertEquals("Enter", exit.getAction());
		assertEquals(ObjectID.POH_EXIT_PORTAL, exit.getObjectId());
	}

	private static Transport find(Set<Transport> transports, WorldPoint destination)
	{
		assertNotNull(transports);
		return transports.stream().filter(transport -> destination.equals(transport.getDestination()))
			.findFirst().orElseThrow();
	}
}
