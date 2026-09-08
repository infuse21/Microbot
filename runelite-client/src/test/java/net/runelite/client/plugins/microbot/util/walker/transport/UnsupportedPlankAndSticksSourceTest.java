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

public class UnsupportedPlankAndSticksSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final int UNSUPPORTED_PLANK_ID = 15213;

	@Test
	public void unstagedPlankAndFailureObstacleRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == UNSUPPORTED_PLANK_ID));
	}

	@Test
	public void allEightPlankRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"2549 10288 0>2547 10288 0:Use;Plank -> Rocks;15213",
			"2547 10288 0>2549 10288 0:Use;Plank -> Rocks;15213",
			"2546 10287 0>2544 10287 0:Use;Plank -> Rocks;15213",
			"2544 10287 0>2546 10287 0:Use;Plank -> Rocks;15213",
			"2543 10287 0>2541 10287 0:Use;Plank -> Rocks;15213",
			"2541 10287 0>2543 10287 0:Use;Plank -> Rocks;15213",
			"2540 10286 0>2538 10286 0:Use;Plank -> Rocks;15213",
			"2538 10286 0>2540 10286 0:Use;Plank -> Rocks;15213");
		InputStream resource = UnsupportedPlankAndSticksSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& line.contains(";15213"))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertTrue(noRequirements(columns)))
				.map(columns -> columns[0] + ">" + columns[1] + ":" + columns[2])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}

	private static boolean noRequirements(String[] columns)
	{
		for (int index = 3; index <= 10; index++)
		{
			if (index < columns.length && !columns[index].trim().isEmpty())
			{
				return false;
			}
		}
		return true;
	}
}
