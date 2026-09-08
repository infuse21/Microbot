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

public class KaruulmHeatBarrierSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void unprotectedHeatBarrierRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == 34542));
	}

	@Test
	public void allFourRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"1293 10091 0>1293 10093 0:Pass;Mystical barrier;34542",
			"1292 10091 0>1292 10093 0:Pass;Mystical barrier;34542",
			"1293 10093 0>1293 10091 0:Pass;Mystical barrier;34542",
			"1292 10093 0>1292 10091 0:Pass;Mystical barrier;34542");
		InputStream resource = KaruulmHeatBarrierSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.contains(";34542"))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertEquals(3, columns.length))
				.map(columns -> columns[0] + ">" + columns[1] + ":" + columns[2])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}
}
