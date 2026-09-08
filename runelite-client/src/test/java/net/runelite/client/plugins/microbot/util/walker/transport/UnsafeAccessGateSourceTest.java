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

public class UnsafeAccessGateSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> UNSAFE_OBJECT_IDS = Set.of(23104);

	@Test
	public void rowsWithoutRequiredAccessGatesAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> UNSAFE_OBJECT_IDS.contains(row.getObjectId())));
	}

	@Test
	public void allFiveRowsRemainAsExactSourceEvidence()
		throws IOException
	{
		Set<String> expected = Set.of(
			"1292 1253 0>1240 1226 0:Turn;Iron Winch;23104",
			"1291 1253 0>1240 1226 0:Turn;Iron Winch;23104",
			"1309 1269 0>1304 1290 0:Turn;Iron Winch;23104",
			"1328 1253 0>1368 1226 0:Turn;Iron Winch;23104",
			"1329 1253 0>1368 1226 0:Turn;Iron Winch;23104");
		InputStream resource = UnsafeAccessGateSourceTest.class.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Set<String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.contains(";23104"))
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
