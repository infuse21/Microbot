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

public class UnsupportedDoorStateSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> UNSUPPORTED_IDS = Set.of(6545, 6547, 6553, 6555, 22119);

	@Test
	public void unsafeDoorStateRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> UNSUPPORTED_IDS.contains(row.getObjectId())));
	}

	@Test
	public void allTenRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"3233 2900 0>3233 2898 0:Open;Pyramid entrance;6545",
			"3232 2900 0>3232 2898 0:Open;Pyramid entrance;6547",
			"3233 2898 0>3233 2900 0:Open;Pyramid entrance;6545",
			"3232 2898 0>3232 2900 0:Open;Pyramid entrance;6547",
			"3233 9323 0>3233 9325 0:Open;Doorway;6555",
			"3233 9325 0>3233 9323 0:Open;Doorway;6555",
			"3234 9323 0>3234 9325 0:Open;Doorway;6553",
			"3234 9325 0>3234 9323 0:Open;Doorway;6553",
			"3804 2844 0>3806 2844 0:Open;Door;22119",
			"3806 2844 0>3804 2844 0:Open;Door;22119");
		InputStream resource = UnsupportedDoorStateSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& UNSUPPORTED_IDS.stream().anyMatch(id -> line.contains(";" + id)))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(UnsupportedDoorStateSourceTest::assertPreservedRequirements)
				.map(columns -> columns[0] + ">" + columns[1] + ":" + columns[2])
				.collect(Collectors.toSet());
			assertEquals(expected, disabled);
		}
	}

	private static void assertPreservedRequirements(String[] columns)
	{
		if (columns[2].endsWith(";22119"))
		{
			assertEquals("3393>4", columns[7]);
			assertEquals("1", columns[10]);
			return;
		}
		for (int index = 3; index <= 10; index++)
		{
			assertTrue(index >= columns.length || columns[index].trim().isEmpty());
		}
	}
}
