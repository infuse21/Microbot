package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class RemainingTransportClassificationTest
{
	@Test
	public void pinsCurrentLegacyClassificationBoundary()
	{
		Map<TransportType, Integer> byType = new EnumMap<>(TransportType.class);
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
					!= RouteEdge.Kind.TRANSPORT)
				{
					continue;
				}
				byType.merge(row.getType(), 1, Integer::sum);
			}
		}
		Map<TransportType, Integer> expected = new EnumMap<>(TransportType.class);
		expected.put(TransportType.TRANSPORT, 343);
		expected.put(TransportType.TELEPORTATION_ITEM, 23);
		assertEquals(expected, byType);
		assertEquals(366, byType.values().stream().mapToInt(Integer::intValue).sum());
	}
}
