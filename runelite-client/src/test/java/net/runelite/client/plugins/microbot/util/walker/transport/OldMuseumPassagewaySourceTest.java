package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OldMuseumPassagewaySourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void fourExactExitRowsUseCatalogOwnershipAndRequireTheirLanding()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 31892).collect(Collectors.toList());
		assertEquals(4, rows.size());
		for (Transport row : rows)
		{
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			org.junit.Assert.assertFalse(AdjacentTransportPolicy.isEligible(row));
			org.junit.Assert.assertFalse(net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction pending =
				new net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction(1, 0,
					row.getOrigin(), row.getDestination(), row.getOrigin(),
					net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction.Kind.CATALOG_TRANSITION,
					net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction.Status.AVAILABLE,
					"Leave", true, 31892, row.getOrigin(), row.getDestination());
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

	@Test
	public void allFourRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"3065 9951 0>3053 3382 0:Leave;Old passageway;31892",
			"3065 9952 0>3053 3382 0:Leave;Old passageway;31892",
			"3014 9952 0>3038 3382 0:Leave;Old passageway;31892",
			"3014 9951 0>3038 3382 0:Leave;Old passageway;31892");
		InputStream resource = OldMuseumPassagewaySourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.contains(";31892"))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertEquals(3, columns.length))
				.map(columns -> columns[0] + ">" + columns[1] + ":" + columns[2])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}
}
