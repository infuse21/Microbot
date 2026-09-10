package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class RemainingTransportClassificationTest
{
	@Test
	public void allResourceTransportTypesLoadAndPublishEngineOwnership()
	{
		Set<TransportType> loadedTypes = EnumSet.noneOf(TransportType.class);
		Map<TransportType, Integer> byType = new EnumMap<>(TransportType.class);
		List<Transport> legacyRows = new ArrayList<>();
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				loadedTypes.add(row.getType());
				if (PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
					!= RouteEdge.Kind.TRANSPORT)
				{
					continue;
				}
				byType.merge(row.getType(), 1, Integer::sum);
				legacyRows.add(row);
			}
		}
		// POH facilities are generated from the configured house, not these resources.
		// Random obelisk rows intentionally do not publish directed routes.
		Set<TransportType> resourceTypes = EnumSet.allOf(TransportType.class);
		resourceTypes.remove(TransportType.POH);
		resourceTypes.remove(TransportType.WILDERNESS_OBELISK);
		assertEquals("Resource-family coverage changed", resourceTypes, loadedTypes);
		assertEquals("Legacy resource rows: " + legacyRows, Collections.emptyMap(), byType);
		assertEquals(Collections.emptyList(), legacyRows);
	}
}
