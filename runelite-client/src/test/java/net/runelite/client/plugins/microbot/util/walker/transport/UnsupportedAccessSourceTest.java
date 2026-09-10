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

public class UnsupportedAccessSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> IDS = Set.of(
		20878, 2446, 5055, 12267, 30842, 23552, 23554, 3762, 3780);

	@Test
	public void remainingUnsupportedRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> IDS.contains(row.getObjectId()) && row.getObjectId() != 5055));
	}

	@Test
	public void allFourteenRowsRetainTheirExactSourceShape()
		throws IOException
	{
		Map<String, String> expected = Map.ofEntries(
			entry("3680 3855 0>3595 10291 0:Climb Down;Trap Door;30842", "|0"),
			entry("3680 3853 0>3595 10291 0:Climb Down;Trap Door;30842", "|0"),
			entry("2713 9564 0>2744 3153 0:Leave;Exit;20878", "|0"),
			entry("2713 9564 0>2745 3152 0:Leave;exit;20878", "|2"),
			entry("2464 3497 0>2464 9897 0:Open;Trapdoor;2446", "|0"),
			entry("2998 3931 0>2998 3916 0:Open;Gate;23552", "|0"),
			entry("2997 3931 0>2998 3916 0:Open;Gate;23554", "|0"),
			entry("3495 3465 0>3477 9845 0:Open;Trapdoor;5055", "|0"),
			entry("3495 3464 0>3477 9845 0:Open;Trapdoor;5055", "|0"),
			entry("2827 3646 0>2823 10050 0:Open;Secret Door;3762", "|0"),
			entry("2828 3646 0>2823 10050 0:Open;Secret Door;3762", "|0"),
			entry("2848 10107 1>2847 10107 1:Unlock;Prison Door;3780", "|0"),
			entry("2847 10107 1>2848 10107 1:Unlock;Prison Door;3780", "|0"),
			entry("3077 3493 0>3077 9893 0:Open;trapdoor;12267",
				"Shadow of the Storm|1"));
		InputStream resource = UnsupportedAccessSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& IDS.stream().anyMatch(id -> line.contains(";" + id)))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(UnsupportedAccessSourceTest::assertEmptyOtherRequirements)
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
		for (int index : new int[] {3, 4, 5, 7, 8, 9, 11})
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
