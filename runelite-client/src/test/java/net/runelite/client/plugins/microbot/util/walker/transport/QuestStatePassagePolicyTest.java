package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuestStatePassagePolicyTest
{
	@Test
	public void allThirteenDirectedContractsPinTwentyTwoStateVariantsAndExactLandings()
	{
		List<Transport> rows = QuestStatePassageSceneTest.rows();
		assertEquals(22, rows.size());
		assertEquals(13, rows.stream().map(row -> row.getOrigin() + ">" + row.getDestination())
			.collect(Collectors.toSet()).size());
		for (Transport row : rows)
		{
			QuestStatePassagePolicy.Entry entry = QuestStatePassagePolicy.entry(row);
			assertNotNull(entry);
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			if (entry.varbit != 0)
			{
				assertEquals(1, row.getVarbits().size());
				for (int state = -1; state <= 333; state++)
				{
					assertEquals(state == row.getVarbits().iterator().next().getValue(),
						row.getVarbits().iterator().next().matches(state));
				}
			}
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				entry.anchor, RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				entry.action, true, entry.id, entry.from, entry.to);
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, entry.from, edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, entry.anchor, edge -> null, 6).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, entry.to, edge -> null, 6).getStatus());
			if (entry.varbit != 0) row.getVarbits().clear();
			else row.setAction("Push");
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}
}
