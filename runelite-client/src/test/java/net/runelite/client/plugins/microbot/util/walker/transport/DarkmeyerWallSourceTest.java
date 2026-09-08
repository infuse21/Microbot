package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DarkmeyerWallSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void unencodedPermanentRopeWallsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == 39541 || row.getObjectId() == 39542));
	}

	@Test
	public void allEightUngatedRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Map<String, Long> expected = Map.of(
			"3667 3375 0>3670 3375 0:39542", 1L,
			"3670 3375 0>3667 3375 0:39542", 1L,
			"3670 3375 0>3673 3375 0:39541", 1L,
			"3673 3375 0>3670 3375 0:39541", 1L,
			"3672 3376 0>3670 3375 0:39541", 2L,
			"3672 3374 0>3670 3375 0:39541", 2L);
		InputStream resource = DarkmeyerWallSourceTest.class.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, Long> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& (line.contains(";39541") || line.contains(";39542")))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertTrue(noRequirements(columns)))
				.map(DarkmeyerWallSourceTest::sourceSignature)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
			assertEquals(expected, disabled);
			assertEquals(8L, disabled.values().stream().mapToLong(Long::longValue).sum());
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

	private static String sourceSignature(String[] columns)
	{
		String objectId = columns[2].substring(columns[2].lastIndexOf(';') + 1);
		return columns[0] + ">" + columns[1] + ":" + objectId;
	}
}
