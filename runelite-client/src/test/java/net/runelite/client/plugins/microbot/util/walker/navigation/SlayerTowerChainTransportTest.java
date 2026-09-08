package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SlayerTowerChainTransportTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	private static List<Transport> chains()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> row.getObjectId() == 16537 || row.getObjectId() == 16538)
			.collect(Collectors.toList());
	}

	@Test
	public void allEightMediumChainsHaveExactEngineOwnedContracts()
	{
		List<Transport> rows = chains();
		assertEquals(8, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && row.getDuration() == 1
			&& row.getSkillLevels()[Skill.AGILITY.ordinal()] == 61
			&& CatalogTransitionPolicy.isEligible(row)
			&& PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row))
				== RouteEdge.Kind.CATALOG_TRANSITION));

		Transport mutated = rows.get(0);
		mutated.getSkillLevels()[Skill.AGILITY.ordinal()] = 60;
		assertFalse(CatalogTransitionPolicy.isEligible(mutated));
		assertEquals(RouteEdge.Kind.TRANSPORT, PathfinderRouteCalculation.classifyTransportEdge(
			Collections.singleton(mutated)));
	}

	@Test
	public void chainFailureOnlyClearsAfterTheDifferentLevelIsReached()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : chains())
		{
			RouteInteraction pending = new RouteInteraction(1, 0, row.getOrigin(), row.getDestination(),
				row.getDestination(), RouteInteraction.Kind.CATALOG_TRANSITION,
				RouteInteraction.Status.AVAILABLE, row.getAction(), true, row.getObjectId(),
				row.getOrigin(), row.getDestination());
			CatalogTransition object = new CatalogTransition(null, row.getDestination(),
				row.getObjectId(), row.getAction(), row.getAction(), row.getOrigin(), row.getDestination());
			WorldPoint wrongPlane = new WorldPoint(row.getDestination().getX(),
				row.getDestination().getY(), row.getOrigin().getPlane());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, wrongPlane, edge -> object, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, row.getDestination(), edge -> object, 13).getStatus());
		}
	}

	@Test
	public void failureProneStrangeFloorsRemainDisabledSourceEvidence()
		throws IOException
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.noneMatch(row -> row.getObjectId() == 16544));
		Set<String> expected = Set.of(
			"2775 10003 0>2773 10003 0", "2773 10003 0>2775 10003 0",
			"2770 10002 0>2768 10002 0", "2768 10002 0>2770 10002 0");
		InputStream resource = SlayerTowerChainTransportTest.class.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.contains(";16544"))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertEquals("43 Agility", columns[3]))
				.map(columns -> columns[0] + ">" + columns[1])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}
}
