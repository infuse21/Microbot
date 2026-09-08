package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BrimhavenDungeonEntranceSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void staleEntranceObjectsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == 20876 || row.getObjectId() == 20877));
	}

	@Test
	public void staleEntranceRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"2744 3154 0>2713 9564 0:20877:875 Coins",
			"2744 3153 0>2713 9564 0:20877:875 Coins",
			"2745 3154 0>2713 9564 0:20877:875 Coins",
			"2743 3154 0>2713 9564 0:20877:875 Coins",
			"2744 3154 0>2713 9564 0:20877:ungated",
			"2744 3153 0>2713 9564 0:20877:ungated",
			"2745 3154 0>2713 9564 0:20877:ungated",
			"2743 3154 0>2713 9564 0:20877:ungated",
			"2744 3152 0>2713 9564 0:20876:875 Coins",
			"2744 3152 0>2713 9564 0:20876:5628=1",
			"2745 3152 0>2713 9564 0:20876:875 Coins",
			"2745 3152 0>2713 9564 0:20876:5628=1",
			"2746 3152 0>2713 9564 0:20876:875 Coins",
			"2746 3152 0>2713 9564 0:20876:5628=1");
		InputStream resource = BrimhavenDungeonEntranceSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& (line.contains(";20876") || line.contains(";20877")))
				.map(line -> sourceSignature(line.substring(2)))
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}

	private static String sourceSignature(String line)
	{
		String[] columns = line.split("\\t");
		String display = columns[2];
		String objectId = display.substring(display.lastIndexOf(';') + 1);
		String currency = field(columns, 5);
		String state = field(columns, 7);
		String gate = !currency.isEmpty() ? currency : !state.isEmpty() ? state : "ungated";
		return columns[0] + ">" + columns[1] + ":" + objectId + ":" + gate;
	}

	private static String field(String[] columns, int index)
	{
		return index < columns.length ? columns[index].trim() : "";
	}
}
