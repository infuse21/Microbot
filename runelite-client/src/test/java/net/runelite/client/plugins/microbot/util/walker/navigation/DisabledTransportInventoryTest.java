package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;
import static org.junit.Assert.*;

/** Diagnostic inventory: a nearby replacement is a research lead, not acceptance evidence. */
public class DisabledTransportInventoryTest
{
	@Test
	public void historicalMushtreeCandidatesRemainEngineOwnedWithConnectivityRecorded() throws Exception
	{
		List<Transport> generated = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getType() == net.runelite.client.plugins.microbot.shortestpath.TransportType.MAGIC_MUSHTREE)
			.collect(Collectors.toList());
		net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap collision =
			new net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap(
				net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap.fromResources());
		int checked = 0;
		List<String> connectivity = new ArrayList<>();
		connectivity.add("historical_origin\thistorical_destination\tconnected_replacements\torigin_cardinal_exits");
		InputStream resource = getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv");
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)))
		{
			for (String line : reader.lines().collect(Collectors.toList()))
			{
				if (!line.startsWith("# ") || !line.contains(";Magic Mushtree;")) continue;
				String[] columns = line.substring(2).split("\\t", -1);
				WorldPoint from = point(columns[0]);
				WorldPoint to = point(columns[1]);
				int id = Integer.parseInt(columns[2].split(";")[2]);
				List<Transport> replacements = generated.stream().filter(row -> row.getObjectId() == id
					&& near(from, row.getOrigin()) && near(to, row.getDestination()))
					.collect(Collectors.toList());
				assertFalse("No nearby generated candidate: " + columns[0] + ">" + columns[1], replacements.isEmpty());
				long connected = replacements.stream().filter(row -> connected(collision, from, row.getOrigin())
					&& connected(collision, row.getDestination(), to)).count();
				int x = from.getX(), y = from.getY(), plane = from.getPlane();
				connectivity.add(columns[0] + "\t" + columns[1] + "\t" + connected + "\t"
					+ collision.n(x, y, plane) + "," + collision.s(x, y, plane) + ","
					+ collision.e(x, y, plane) + "," + collision.w(x, y, plane));
				for (Transport row : replacements)
					assertEquals(RouteEdge.Kind.MAGIC_MUSHTREE, PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)));
				checked++;
			}
		}
		assertEquals(12, checked);
		Path output = Path.of("build", "reports", "walker-mushtree-connectivity.tsv");
		Files.createDirectories(output.getParent());
		Files.write(output, connectivity, StandardCharsets.UTF_8);
	}

	private static boolean connected(net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap map,
		WorldPoint from, WorldPoint to)
	{
		if (from.getPlane() != to.getPlane()) return false;
		Set<WorldPoint> seen = new java.util.HashSet<>();
		java.util.ArrayDeque<WorldPoint> frontier = new java.util.ArrayDeque<>();
		frontier.add(from);
		while (!frontier.isEmpty())
		{
			WorldPoint tile = frontier.removeFirst();
			if (tile.distanceTo2D(from) > 8 || !seen.add(tile)) continue;
			if (tile.equals(to)) return true;
			int x = tile.getX(), y = tile.getY(), plane = tile.getPlane();
			if (map.n(x, y, plane)) frontier.add(new WorldPoint(x, y + 1, plane));
			if (map.s(x, y, plane)) frontier.add(new WorldPoint(x, y - 1, plane));
			if (map.e(x, y, plane)) frontier.add(new WorldPoint(x + 1, y, plane));
			if (map.w(x, y, plane)) frontier.add(new WorldPoint(x - 1, y, plane));
		}
		return false;
	}

	@Test
	public void exactHistoricalAliasesRemainEngineOwned() throws Exception
	{
		List<Transport> loaded = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.collect(Collectors.toList());
		Map<String, Integer> counts = new TreeMap<>();
		List<String> report = new ArrayList<>();
		report.add("status\torigin\tdestination\thistorical_identity\tloaded_candidates");
		int canifisAliases = 0;
		InputStream resource = getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv");
		assertNotNull(resource);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)))
		{
			for (String line : reader.lines().collect(Collectors.toList()))
			{
				if (!line.matches("# \\d+ \\d+ [0-3]\\t\\d+ \\d+ [0-3]\\t.*")) continue;
				String[] columns = line.substring(2).split("\\t", -1);
				WorldPoint from = point(columns[0]);
				WorldPoint to = point(columns[1]);
				String identity = normalize(columns[2]);
				String[] parts = columns[2].split(";");
				int id = Integer.parseInt(parts[parts.length - 1]);
				List<Transport> candidates = loaded.stream().filter(row -> from.equals(row.getOrigin())
					&& to.equals(row.getDestination())).collect(Collectors.toList());
				List<Transport> exact = candidates.stream().filter(row -> identity.equals(normalize(
					row.getAction() + ";" + row.getName() + ";" + row.getObjectId()))).collect(Collectors.toList());
				String status;
				if (!exact.isEmpty())
				{
					status = "EXACT_IDENTITY_REQUIREMENTS_NOT_COMPARED";
					candidates = exact;
					for (Transport row : exact)
						assertNotEquals("Historical alias lost engine ownership: " + columns[2], RouteEdge.Kind.TRANSPORT,
							PathfinderRouteCalculation.classifyTransportEdge(Set.of(row)));
					if (id == 5055) canifisAliases++;
				}
				else if (!candidates.isEmpty()) status = "SAME_DIRECTION_RESEARCH_REQUIRED";
				else
				{
					candidates = loaded.stream().filter(row -> row.getObjectId() == id
						&& near(from, row.getOrigin()) && near(to, row.getDestination())).collect(Collectors.toList());
					status = candidates.isEmpty() ? "NO_LOCAL_MATCH" : "NEARBY_ID_RESEARCH_REQUIRED";
				}
				counts.merge(status, 1, Integer::sum);
				report.add(status + "\t" + columns[0] + "\t" + columns[1] + "\t" + columns[2] + "\t"
					+ candidates.stream().map(row -> row.getType() + ":" + row.getOrigin() + ">" + row.getDestination()
						+ ":" + row.getObjectId()).distinct().sorted().collect(Collectors.joining(" | ")));
			}
		}
		assertEquals("Both migrated Canifis approaches should be audited", 2, canifisAliases);
		assertTrue("Historical inventory unexpectedly empty", report.size() > 100);
		Path output = Path.of("build", "reports", "walker-disabled-transports.tsv");
		Files.createDirectories(output.getParent());
		Files.write(output, report, StandardCharsets.UTF_8);
		System.out.println("Disabled transport inventory (not completion counts): " + counts);
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "");
	}

	private static WorldPoint point(String value) {
		String[] parts = value.split(" ");
		return new WorldPoint(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
	}

	private static boolean near(WorldPoint first, WorldPoint second) {
		return second != null && first.getPlane() == second.getPlane() && first.distanceTo2D(second) <= 3;
	}
}
