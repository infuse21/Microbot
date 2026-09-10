package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;
import static org.junit.Assert.*;

public class TearsTunnelPolicyTest
{
	@Test
	public void sixExactTunnelsRemainItemFreeAndRequireTheirLanding()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 6658 || row.getObjectId() == 6659).collect(Collectors.toList());
		assertEquals(6, rows.size());
		for (Transport row : rows)
		{
			assertTrue(row.isMembers());
			assertTrue(row.getQuests().isEmpty());
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			org.junit.Assert.assertFalse(AdjacentTransportPolicy.isEligible(row));
			org.junit.Assert.assertFalse(net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction pending =
				new net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction(1, 0,
					row.getOrigin(), row.getDestination(), row.getOrigin(),
					net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction.Kind.CATALOG_TRANSITION,
					net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction.Status.AVAILABLE,
					"Enter", true, row.getObjectId(), row.getOrigin(), row.getDestination());
			CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
			CatalogTransitionScene scene = org.mockito.Mockito.mock(CatalogTransitionScene.class);
			org.junit.Assert.assertNotEquals(net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getOrigin(), scene, 6).getStatus());
			assertEquals(net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), scene, 6).getStatus());
			row.getItemIdRequirements().add(Set.of(995));
			org.junit.Assert.assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}

}
