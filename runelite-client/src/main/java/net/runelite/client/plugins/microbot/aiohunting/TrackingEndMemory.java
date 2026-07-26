package net.runelite.client.plugins.microbot.aiohunting;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingStyle;

/**
 * Learns and persists the mapping from a tracking trail's finish varbit value to the exact
 * end tile (bush / snowdrift / rock) that holds the kebbit — the same "index into an ordered
 * end list" trick herbiboar uses, except we discover it at runtime instead of hardcoding it.
 *
 * <p>Once an index is known the endpoint is attacked directly (human-like, no searching). The
 * first time an index is seen the caller falls back to searching for "...something hiding..."
 * and records the result here, persisted to the RuneLite profile so it is never relearned.</p>
 */
final class TrackingEndMemory
{
	private static final String KEY_PREFIX = "trailEnds_";

	private final ConfigManager configManager;
	private final Map<HuntingMethod, Map<Integer, WorldPoint>> memory =
		new EnumMap<>(HuntingMethod.class);

	TrackingEndMemory(ConfigManager configManager)
	{
		this.configManager = configManager;
		seedDefaults();
		loadAll();
	}

	WorldPoint get(HuntingMethod method, int finishValue)
	{
		Map<Integer, WorldPoint> map = memory.get(method);
		return map == null ? null : map.get(finishValue);
	}

	void record(HuntingMethod method, int finishValue, WorldPoint location)
	{
		if (location == null || finishValue <= 0)
		{
			return;
		}
		Map<Integer, WorldPoint> map = memory.computeIfAbsent(method, k -> new HashMap<>());
		if (location.equals(map.get(finishValue)))
		{
			return;
		}
		map.put(finishValue, location);
		save(method);
	}

	void forget(HuntingMethod method, int finishValue)
	{
		Map<Integer, WorldPoint> map = memory.get(method);
		if (map != null && map.remove(finishValue) != null)
		{
			save(method);
		}
	}

	private void seedDefaults()
	{
		// Polar kebbit platform, captured live 2026-07-25 (plane 1).
		Map<Integer, WorldPoint> polar = new HashMap<>();
		polar.put(1, new WorldPoint(2712, 3831, 1));
		polar.put(2, new WorldPoint(2716, 3827, 1));
		polar.put(3, new WorldPoint(2708, 3819, 1));
		polar.put(4, new WorldPoint(2718, 3820, 1));
		memory.put(HuntingMethod.POLAR_KEBBIT, polar);
	}

	private void loadAll()
	{
		for (HuntingMethod method : HuntingMethod.values())
		{
			if (method.getStyle() != HuntingStyle.TRACKING)
			{
				continue;
			}
			String raw = configManager.getConfiguration(AIOHuntingConfig.GROUP, KEY_PREFIX + method.name());
			if (raw == null || raw.isEmpty())
			{
				continue;
			}
			Map<Integer, WorldPoint> map = memory.computeIfAbsent(method, k -> new HashMap<>());
			for (String entry : raw.split(";"))
			{
				WorldPoint point = parseEntry(entry, map);
				if (point == null)
				{
					continue;
				}
			}
		}
	}

	private WorldPoint parseEntry(String entry, Map<Integer, WorldPoint> map)
	{
		try
		{
			int eq = entry.indexOf('=');
			if (eq < 0)
			{
				return null;
			}
			int finishValue = Integer.parseInt(entry.substring(0, eq).trim());
			String[] xyz = entry.substring(eq + 1).split(",");
			WorldPoint point = new WorldPoint(
				Integer.parseInt(xyz[0].trim()),
				Integer.parseInt(xyz[1].trim()),
				Integer.parseInt(xyz[2].trim()));
			map.put(finishValue, point);
			return point;
		}
		catch (RuntimeException ex)
		{
			return null;
		}
	}

	private void save(HuntingMethod method)
	{
		Map<Integer, WorldPoint> map = memory.get(method);
		if (map == null)
		{
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, WorldPoint> e : map.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(';');
			}
			WorldPoint p = e.getValue();
			sb.append(e.getKey()).append('=')
				.append(p.getX()).append(',').append(p.getY()).append(',').append(p.getPlane());
		}
		configManager.setConfiguration(AIOHuntingConfig.GROUP, KEY_PREFIX + method.name(), sb.toString());
	}
}
