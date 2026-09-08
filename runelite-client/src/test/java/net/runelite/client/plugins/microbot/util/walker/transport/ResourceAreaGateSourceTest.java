package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceAreaGateSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";

	@Test
	public void resourceAreaGateRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == 26760));
	}

	@Test
	public void allFiveRowsRetainTheirExactFareAndDiaryShape()
		throws IOException
	{
		Map<String, String> expected = Map.of(
			"3184 3944 0>3184 3945 0", "||2",
			"3184 3945 0>3184 3944 0", "7500 Coins|4469=0;4468=0;4467=0;|2",
			"3184 3945 0>3184 3944 0#6000", "6000 Coins|4469=0;4468=0;4467=1|2",
			"3184 3945 0>3184 3944 0#3750", "3750 Coins|4469=0;4468=1|2",
			"3184 3945 0>3184 3944 0#elite", "|4469=1|2");
		InputStream resource = ResourceAreaGateSourceTest.class.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, String> disabled = reader.lines()
				.filter(line -> line.startsWith("# ") && line.contains(";26760"))
				.map(line -> line.substring(2).split("\\t", -1))
				.collect(Collectors.toMap(ResourceAreaGateSourceTest::key,
					columns -> value(columns, 5) + "|" + value(columns, 7)
						+ "|" + value(columns, 10)));
			assertEquals(expected, disabled);
		}
	}

	private static String key(String[] columns)
	{
		String base = columns[0] + ">" + columns[1];
		String fare = value(columns, 5);
		String diary = value(columns, 7);
		if ("6000 Coins".equals(fare))
		{
			return base + "#6000";
		}
		if ("3750 Coins".equals(fare))
		{
			return base + "#3750";
		}
		return "4469=1".equals(diary) ? base + "#elite" : base;
	}

	private static String value(String[] columns, int index)
	{
		return index < columns.length ? columns[index].trim() : "";
	}
}
