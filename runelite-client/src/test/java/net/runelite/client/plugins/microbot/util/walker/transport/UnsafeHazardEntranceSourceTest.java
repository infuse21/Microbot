package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UnsafeHazardEntranceSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> UNSUPPORTED_IDS = Set.of(5947, 24842);

	@Test
	public void unsafeEntranceRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> UNSUPPORTED_IDS.contains(row.getObjectId())));
	}

	@Test
	public void allEightUnsupportedRowsRetainTheirExactSourceShape()
		throws IOException
	{
		Map<String, String> expected = Map.ofEntries(
			entry("2898 3469 0>2901 9867 0:Enter;Manhole;24842", "|0"),
			entry("2899 3469 0>2901 9867 0:Enter;Manhole;24842", "|0"),
			entry("2899 3470 0>2901 9867 0:Enter;Manhole;24842", "|0"),
			entry("2899 3468 0>2901 9867 0:Enter;Manhole;24842", "|0"),
			entry("3169 3171 0>3169 9571 0:Climb-down;Dark hole;5947", "|2"),
			entry("3168 3172 0>3168 9572 0:Climb-down;Dark hole;5947", "|2"),
			entry("3170 3172 0>3170 9572 0:Climb-down;Dark hole;5947", "|2"),
			entry("3169 3173 0>3167 9573 0:Climb-down;Dark hole;5947", "|2"));
		InputStream resource = UnsafeHazardEntranceSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& UNSUPPORTED_IDS.stream().anyMatch(id -> line.contains(";" + id)))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(UnsafeHazardEntranceSourceTest::assertEmptyOtherRequirements)
				.collect(Collectors.toMap(
					columns -> columns[0] + ">" + columns[1] + ":" + columns[2],
					columns -> value(columns, 6) + "|" + duration(columns)));
			assertEquals(expected, disabled);
		}
	}

	private static Map.Entry<String, String> entry(String signature, String sourceShape)
	{
		return Map.entry(signature, sourceShape);
	}

	private static void assertEmptyOtherRequirements(String[] columns)
	{
		for (int index : new int[] {3, 4, 5, 7, 8, 9})
		{
			assertTrue(value(columns, index).isEmpty());
		}
	}

	private static int duration(String[] columns)
	{
		String duration = value(columns, 10);
		return duration.isEmpty() ? 0 : Integer.parseInt(duration);
	}

	private static String value(String[] columns, int index)
	{
		return index < columns.length ? columns[index].trim() : "";
	}
}
