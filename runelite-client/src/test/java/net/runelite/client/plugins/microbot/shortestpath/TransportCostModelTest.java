package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TransportCostModelTest
{
	private static final WorldPoint A = new WorldPoint(3129, 3496, 0);
	private static final WorldPoint B = new WorldPoint(3447, 3470, 0);

	@Test
	public void calibratedFamiliesUseObservedFloors()
	{
		assertEquals(24, TransportCostModel.travelTicks(
			transport(TransportType.FAIRY_RING, 5)));
		assertEquals(12, TransportCostModel.travelTicks(
			transport(TransportType.SPIRIT_TREE, 0)));
		assertEquals(8, TransportCostModel.travelTicks(
			transport(TransportType.GNOME_GLIDER, 0)));
		assertEquals(6, TransportCostModel.travelTicks(
			transport(TransportType.QUETZAL, 0)));
		assertEquals(30, TransportCostModel.travelTicks(
			transport(TransportType.FAIRY_RING, 30)));
		assertEquals(1, TransportCostModel.travelTicks(
			transport(TransportType.BOAT, 0)));
	}

	@Test
	public void pathScoreReplacesTransportJumpWithCalibratedTicks()
	{
		Transport fairyRing = transport(TransportType.FAIRY_RING, 5);
		Map<WorldPoint, Set<Transport>> transports = new HashMap<>();
		transports.put(A, new LinkedHashSet<>(List.of(fairyRing)));

		assertEquals(25, TransportCostModel.pathTicks(
			List.of(A, B, new WorldPoint(B.getX() + 1, B.getY(), B.getPlane())),
			transports));
	}

	@Test
	public void originlessFirstEdgeUsesTransportTicks()
	{
		Transport teleport = new Transport(null, B, "Teleport",
			TransportType.TELEPORTATION_SPELL, true, 4);
		Map<WorldPoint, Set<Transport>> transports = new HashMap<>();
		transports.put(null, new LinkedHashSet<>(List.of(teleport)));

		assertEquals(4, TransportCostModel.pathTicks(List.of(A, B), transports));
	}

	private static Transport transport(TransportType type, int duration)
	{
		return new Transport(A, B, type.name(), type, true, duration);
	}
}
