package net.runelite.client.plugins.microbot.util.walker.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class UnsupportedUnusualAccessSourceTest
{
	private static final String RESOURCE =
		"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv";
	private static final Map<String, String> EXPECTED = Map.ofEntries(
		entry("2700 9688 0>2323 5104 0:Enter;Wall opening;19124", "|2"),
		entry("2323 5104 0>2700 9688 0:Enter;Passage;18412", "|2"),
		entry("1570 5061 0>3549 10481 0:Climb-up;Goo covered vine;23703", "|0"),
		entry("3309 3452 0>3297 9824 0:Enter;Rift;13968", "|0"),
		entry("2711 5206 0>3514 9521 2:Enter;Blocked tunnel;22656", "|0"),
		entry("2711 5207 0>3514 9520 2:Enter;Blocked tunnel;22657", "|0"),
		entry("3105 9308 2>3105 9306 2:Climb-over;Pile of rubble;33342", "|0"),
		entry("3105 9306 2>3105 9308 2:Climb-over;Pile of rubble;33342", "|0"),
		entry("3104 9308 2>3104 9306 2:Climb-over;Pile of bones;18342", "|0"),
		entry("3104 9306 2>3104 9308 2:Climb-over;Pile of bones;18342", "|0"),
		entry("2477 9437 2>2485 3045 0:Climb-up;Stairs;6842", "|0"),
		entry("2442 9417 0>2446 9417 2:Climb-over;Stairs;6842", "|0"),
		entry("2477 9438 2>2485 3045 0:Climb-up;Stairs;6842", "|7"),
		entry("3039 4805 0>3039 4800 0:Enter;Passage;27054", "|0"),
		entry("3049 4839 0>2464 4826 0:Exit-through;Law rift;25382", "Troll Stronghold|0"),
		entry("3050 4829 0>1816 3856 0:Exit-through;Soul rift;25382", "|0"));

	@Test
	public void allSixteenUnsafeRowsAreDisabledAndPreserveSourceShape()
		throws IOException
	{
		assertFalse(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getOrigin() != null && row.getDestination() != null)
			.anyMatch(row -> EXPECTED.containsKey(key(row))));
		InputStream resource = UnsupportedUnusualAccessSourceTest.class
			.getResourceAsStream(RESOURCE);
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
			StandardCharsets.UTF_8)))
		{
			Map<String, String> actual = reader.lines()
				.filter(line -> line.startsWith("# "))
				.map(line -> line.substring(2).split("\\t", -1))
				.filter(columns -> columns.length >= 3)
				.filter(columns -> EXPECTED.containsKey(key(columns)))
				.collect(Collectors.toMap(UnsupportedUnusualAccessSourceTest::key,
					columns -> value(columns, 6) + "|" + duration(columns)));
			assertEquals(EXPECTED, actual);
		}
	}

	private static Map.Entry<String, String> entry(String signature, String sourceShape)
	{
		return Map.entry(signature, sourceShape);
	}

	private static String key(Transport row)
	{
		return pointKey(row.getOrigin()) + ">" + pointKey(row.getDestination()) + ":" + row.getAction()
			+ ";" + row.getName() + ";" + row.getObjectId();
	}

	private static String pointKey(WorldPoint point)
	{
		return point.getX() + " " + point.getY() + " " + point.getPlane();
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
