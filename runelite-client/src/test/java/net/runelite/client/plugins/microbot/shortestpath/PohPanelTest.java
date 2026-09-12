package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.util.poh.data.HouseLocation;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PohPanelTest
{
	@Test
	public void unknownNexusSlotDoesNotDiscardKnownDestinations()
	{
		try (org.mockito.MockedStatic<net.runelite.client.plugins.microbot.Microbot> client =
			org.mockito.Mockito.mockStatic(net.runelite.client.plugins.microbot.Microbot.class))
		{
			int[] slots = net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.VARBITS;
			client.when(() -> net.runelite.client.plugins.microbot.Microbot.getVarbitValue(slots[0]))
				.thenReturn(Integer.MAX_VALUE);
			client.when(() -> net.runelite.client.plugins.microbot.Microbot.getVarbitValue(slots[1]))
				.thenReturn(2);
			assertEquals(java.util.List.of(net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.LUMBRIDGE),
				net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.getAvailableTeleports());
		}
	}

	@Test
	public void nexusReadsTheExtendedSavedSlots()
	{
		try (org.mockito.MockedStatic<net.runelite.client.plugins.microbot.Microbot> client =
			org.mockito.Mockito.mockStatic(net.runelite.client.plugins.microbot.Microbot.class))
		{
			for (int slot = 20111; slot <= 20120; slot++)
			{
				client.reset();
				int savedSlot = slot;
				client.when(() -> net.runelite.client.plugins.microbot.Microbot.getVarbitValue(savedSlot))
					.thenReturn(2);
				assertEquals("slot " + slot,
					java.util.List.of(net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.LUMBRIDGE),
					net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.getAvailableTeleports());
			}
		}
	}

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

	@Test
	public void networkGeneratorConnectsEveryOriginAndDestinationWithoutCrossContamination()
	{
		WorldPoint house = new WorldPoint(1859, 7051, 0);
		WorldPoint a = new WorldPoint(1000, 1000, 0);
		WorldPoint b = new WorldPoint(2000, 2000, 0);
		WorldPoint c = new WorldPoint(3000, 3000, 0);
		WorldPoint d = new WorldPoint(4000, 4000, 0);
		Transport originA = endpoint("Origin", a, "11", "");
		Transport originC = endpoint("Origin", c, "33", "");
		Transport destinationB = endpoint("Destination", b, "22", "B");
		Transport destinationD = endpoint("Destination", d, "44", "D");
		Map<WorldPoint, Set<Transport>> network = Map.of(
			a, Set.of(new Transport(originA, destinationB),
				new Transport(originA, destinationD)),
			c, Set.of(new Transport(originC, destinationB),
				new Transport(originC, destinationD)));
		Transport houseEndpoint = new Transport(house, house, "DIQ",
			TransportType.FAIRY_RING, true, 5);

		Map<WorldPoint, Set<Transport>> generated = PohPanel.createTransportsToPoh(
			houseEndpoint, network);

		Transport aToHouse = find(generated.get(a), house);
		Transport cToHouse = find(generated.get(c), house);
		Transport houseToB = find(generated.get(house), b);
		Transport houseToD = find(generated.get(house), d);
		assertRequirements(aToHouse, 11, 22, 33, 44);
		assertRequirements(cToHouse, 33, 11, 22, 44);
		assertRequirements(houseToB, 22, 11, 33, 44);
		assertRequirements(houseToD, 44, 11, 22, 33);
		assertEquals(2, generated.get(house).size());
	}

	@Test
	public void houseSpiritTreeHasItsOwnOutboundIdentityOnly()
	{
		WorldPoint house = new WorldPoint(1859, 7051, 0);
		WorldPoint outside = new WorldPoint(2545, 3169, 0);
		Transport external = new Transport(outside, outside, "1: Tree Gnome Village",
			TransportType.SPIRIT_TREE, true, "Travel", "Spirit Tree", 1293);
		Map<WorldPoint, Set<Transport>> graph = PohPanel.createSpiritTreeMap(house,
			Map.of(outside, Set.of(external)));
		Transport outbound = find(graph.get(house), outside);
		assertEquals(29227, outbound.getObjectId());
		assertEquals("Travel", outbound.getAction());
		assertEquals("Spirit tree", outbound.getName());
		assertEquals(5, outbound.getDuration());
		assertTrue(outbound.isMembers());
		assertEquals("1: Tree Gnome Village", outbound.getDisplayInfo());
		Transport inbound = find(graph.get(outside), house);
		assertEquals(1293, inbound.getObjectId());
		assertEquals("C: Your house", inbound.getDisplayInfo());
	}

	private static Transport endpoint(String coordinateField, WorldPoint point,
		String itemId, String displayInfo)
	{
		String otherField = "Origin".equals(coordinateField)
			? "Destination" : "Origin";
		return new Transport(Map.of(coordinateField,
			point.getX() + " " + point.getY() + " " + point.getPlane(),
			otherField, "*", "Item IDs", itemId, "Display info", displayInfo),
			TransportType.FAIRY_RING);
	}

	private static void assertRequirements(Transport transport, int expected,
		int... absent)
	{
		assertTrue(transport.getItemIdRequirements().contains(Set.of(expected)));
		for (int itemId : absent)
		{
			assertFalse(transport.getItemIdRequirements().contains(Set.of(itemId)));
		}
	}

	private static Transport find(Set<Transport> transports, WorldPoint destination)
	{
		assertNotNull(transports);
		return transports.stream().filter(transport -> destination.equals(transport.getDestination()))
			.findFirst().orElseThrow();
	}
}
