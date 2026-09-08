package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BrimhavenSouthernEntranceSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void ungatedSouthernEntranceRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.noneMatch(row -> row.getObjectId() == 66));
	}

	@Test
	public void allFourRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"2761 3062 0>2734 9478 0:Climb;Rope;66",
			"2761 3063 0>2734 9478 0:Climb;Rope;66",
			"2760 3064 0>2734 9478 0:Climb;Rope;66",
			"2760 3061 0>2734 9478 0:Climb;Rope;66");
		InputStream resource = BrimhavenSouthernEntranceSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.endsWith(";66"))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertEquals(3, columns.length))
				.map(columns -> columns[0] + ">" + columns[1] + ":" + columns[2])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}
}
