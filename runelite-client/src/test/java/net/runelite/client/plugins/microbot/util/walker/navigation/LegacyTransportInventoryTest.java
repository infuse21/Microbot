package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Regenerates a behaviour inventory using the production classifier, not action-name guesses. */
public class LegacyTransportInventoryTest
{
	@Test
	public void reportRemainingBehaviourGroups()
	{
		Map<String, Integer> groups = new TreeMap<>();
		Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
				== RouteEdge.Kind.TRANSPORT)
			.forEach(row -> groups.merge(row.getType() + " | " + row.getAction() + " | " + row.getName(),
				1, Integer::sum));
		assertTrue(groups.toString(), groups.isEmpty());
		groups.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
			.thenComparing(Map.Entry.comparingByKey())).forEach(entry ->
				System.out.println(entry.getValue() + " | " + entry.getKey()));
	}
}
