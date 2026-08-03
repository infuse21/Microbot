package net.runelite.client.plugins.microbot.aiohunting;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;

/**
 * Learns and persists each pitfall pit's two "jump-from" tiles - the tiles either side of the pit
 * along its axis, which sit exactly 3 apart (pit origin -1 and +2; the pit occupies the two tiles
 * between). Positioning on the side nearest the prey and jumping to the far side is what makes it
 * path straight through the pit, so this geometry is the core of the catch.
 *
 * <p>Learned at runtime by watching a completed jump (see {@code PitfallActivity#learnPitSides}) and
 * persisted to the RuneLite profile, so a pit is only ever figured out once - no hardcoded
 * coordinates, and it works at any pitfall area (larupia, graahk, kyatt, antelopes).</p>
 */
final class PitSideMemory
{
	private static final String KEY = "pitSides";

	private final ConfigManager configManager;
	private final Map<WorldPoint, WorldPoint[]> memory = new HashMap<>();

	PitSideMemory(ConfigManager configManager)
	{
		this.configManager = configManager;
		load();
	}

	WorldPoint[] get(WorldPoint pit)
	{
		return pit == null ? null : memory.get(pit);
	}

	void record(WorldPoint pit, WorldPoint sideA, WorldPoint sideB)
	{
		if (pit == null || sideA == null || sideB == null)
		{
			return;
		}
		WorldPoint[] existing = memory.get(pit);
		if (existing != null && existing[0].equals(sideA) && existing[1].equals(sideB))
		{
			return;
		}
		memory.put(pit, new WorldPoint[]{sideA, sideB});
		save();
	}

	private void load()
	{
		String raw = configManager.getConfiguration(AIOHuntingConfig.GROUP, KEY);
		if (raw == null || raw.isEmpty())
		{
			return;
		}
		for (String entry : raw.split(";"))
		{
			parseEntry(entry);
		}
	}

	/** Entry format: {@code pitX,pitY,plane=aX,aY|bX,bY} */
	private void parseEntry(String entry)
	{
		try
		{
			int eq = entry.indexOf('=');
			if (eq < 0)
			{
				return;
			}
			String[] pit = entry.substring(0, eq).split(",");
			int plane = Integer.parseInt(pit[2].trim());
			WorldPoint pitPoint = new WorldPoint(
				Integer.parseInt(pit[0].trim()), Integer.parseInt(pit[1].trim()), plane);
			String[] sides = entry.substring(eq + 1).split("\\|");
			memory.put(pitPoint, new WorldPoint[]{parsePoint(sides[0], plane), parsePoint(sides[1], plane)});
		}
		catch (RuntimeException ex)
		{
			// Ignore a malformed entry - it will simply be relearned.
		}
	}

	private static WorldPoint parsePoint(String raw, int plane)
	{
		String[] xy = raw.split(",");
		return new WorldPoint(Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim()), plane);
	}

	private void save()
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<WorldPoint, WorldPoint[]> e : memory.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(';');
			}
			WorldPoint pit = e.getKey();
			WorldPoint[] sides = e.getValue();
			sb.append(pit.getX()).append(',').append(pit.getY()).append(',').append(pit.getPlane())
				.append('=')
				.append(sides[0].getX()).append(',').append(sides[0].getY())
				.append('|')
				.append(sides[1].getX()).append(',').append(sides[1].getY());
		}
		configManager.setConfiguration(AIOHuntingConfig.GROUP, KEY, sb.toString());
	}
}
