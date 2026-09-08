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

public class WeissRockslideSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> ROCKSLIDE_IDS = Set.of(33184, 33185, 33191);

	@Test
	public void unsafeRockslidesAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> ROCKSLIDE_IDS.contains(row.getObjectId())));
	}

	@Test
	public void allSixRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"2852 3966 0>2852 3964 0:Climb;Rockslide;33184",
			"2852 3964 0>2852 3966 0:Climb;Rockslide;33184",
			"2852 3964 0>2855 3964 0:Climb;Rockslide;33185",
			"2855 3964 0>2852 3964 0:Climb;Rockslide;33185",
			"2859 3962 0>2859 3960 0:Climb;Rockslide;33191",
			"2859 3960 0>2859 3962 0:Climb;Rockslide;33191");
		InputStream resource = WeissRockslideSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& ROCKSLIDE_IDS.stream().anyMatch(id -> line.contains(";" + id)))
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
