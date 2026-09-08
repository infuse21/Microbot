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

public class UnsafeSteppingStoneAndLeafSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final int STEPPING_STONE_ID = 19040;

	@Test
	public void incompleteHazardRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.noneMatch(row -> row.getObjectId() == STEPPING_STONE_ID));
	}

	@Test
	public void allFourSteppingStoneRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"2695 9533 0>2697 9525 0:Cross;Stepping stone;19040",
			"2697 9525 0>2695 9533 0:Cross;Stepping stone;19040",
			"2690 9547 0>2682 9548 0:Cross;Stepping stone;19040",
			"2682 9548 0>2690 9547 0:Cross;Stepping stone;19040");
		InputStream resource = UnsafeSteppingStoneAndLeafSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& line.contains(";19040"))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertEquals(3, columns.length))
				.map(columns -> columns[0] + ">" + columns[1] + ":" + columns[2])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}
}
