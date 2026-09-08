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

public class UnsafeHazardTunnelSourceTest
{
	private static final String TRANSPORT_RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Set<Integer> UNSUPPORTED_IDS = Set.of(6658, 6659, 23596);

	@Test
	public void hazardTunnelRowsAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> UNSUPPORTED_IDS.contains(row.getObjectId())));
	}

	@Test
	public void allEightRowsAndDurationsRemainAsExactSourceEvidence()
		throws IOException
	{
		Map<String, Integer> expected = Map.of(
			"3514 9521 2>2711 5206 0:Enter;Tunnel;23596", 0,
			"3514 9520 2>2711 5206 0:Enter;Tunnel;23596", 0,
			"3226 9542 0>3219 9532 2:Enter;Tunnel;6659", 0,
			"3219 9532 2>3226 9542 0:Enter;Tunnel;6658", 0,
			"3225 9542 0>3219 9532 2:Enter;Tunnel;6659", 2,
			"3227 9542 0>3219 9532 2:Enter;Tunnel;6659", 2,
			"3218 9532 2>3226 9542 0:Enter;Tunnel;6658", 2,
			"3220 9532 2>3226 9542 0:Enter;Tunnel;6658", 2);
		InputStream resource = UnsafeHazardTunnelSourceTest.class
			.getResourceAsStream(TRANSPORT_RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, Integer> disabled = reader.lines()
				.filter(line -> line.startsWith("# ")
					&& UNSUPPORTED_IDS.stream().anyMatch(id -> line.contains(";" + id)))
				.map(line -> line.substring(2).split("\\t", -1))
				.peek(columns -> assertTrue(noRequirements(columns)))
				.collect(Collectors.toMap(
					columns -> columns[0] + ">" + columns[1] + ":" + columns[2],
					columns -> columns.length > 10 && !columns[10].isEmpty()
						? Integer.parseInt(columns[10]) : 0));
			assertEquals(expected, disabled);
		}
	}

	private static boolean noRequirements(String[] columns)
	{
		for (int index = 3; index <= 9; index++)
		{
			if (index < columns.length && !columns[index].trim().isEmpty())
			{
				return false;
			}
		}
		return true;
	}
}
