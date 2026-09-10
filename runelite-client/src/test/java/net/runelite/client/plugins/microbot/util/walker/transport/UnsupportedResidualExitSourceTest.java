package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UnsupportedResidualExitSourceTest
{
	private static final String RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> IDS = Set.of(17900);
	private static final Map<String, String> EXPECTED = Map.of(
		"3493 9726 0>3485 3322 0:Open;Solid bronze door;17900", "|1");

	@Test
	public void unresolvedAndMalformedRowsAreDisabledAndPreserveSourceShape()
		throws IOException
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> IDS.contains(row.getObjectId())));
		InputStream resource = UnsupportedResidualExitSourceTest.class
			.getResourceAsStream(RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, String> actual = reader.lines()
				.filter(line -> line.startsWith("# "))
				.map(line -> line.substring(2).split("\\t", -1))
				.filter(columns -> columns.length > 2 && IDS.contains(objectId(columns[2])))
				.collect(Collectors.toMap(UnsupportedResidualExitSourceTest::key,
					columns -> value(columns, 6) + "|" + duration(columns)));
			assertEquals(EXPECTED, actual);
		}
	}

	private static int objectId(String identity)
	{
		int separator = identity.lastIndexOf(';');
		return separator < 0 ? -1 : Integer.parseInt(identity.substring(separator + 1));
	}

	private static String key(String[] columns)
	{
		return columns[0] + ">" + columns[1] + ":" + columns[2];
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
