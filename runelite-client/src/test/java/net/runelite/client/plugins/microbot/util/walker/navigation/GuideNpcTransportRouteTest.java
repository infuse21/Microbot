package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GuideNpcTransportRouteTest
{
	@Test
	public void exactGuideRowsUseNpcTransportOwnership()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> Set.of(997, 4968, 7299, 7301, 14529).contains(row.getObjectId()))
			.filter(row -> Set.of("Dartog", "Elkoy", "Kazgar", "Mistag", "Mountain Guide")
				.contains(row.getName()))
			.collect(Collectors.toList());

		assertEquals(10, rows.size());
		assertTrue(rows.stream().allMatch(row ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
				== RouteEdge.Kind.NPC_TRANSPORT));
	}
}
