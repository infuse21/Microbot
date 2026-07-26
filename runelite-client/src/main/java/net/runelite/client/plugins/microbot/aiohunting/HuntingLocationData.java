package net.runelite.client.plugins.microbot.aiohunting;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;

final class HuntingLocationData
{
	private HuntingLocationData()
	{
	}

	static List<WorldPoint> locations(HuntingMethod method)
	{
		switch (method)
		{
			// Bird snares camp a single fixed spot amid their curated tiles (see SnareSpotData);
			// one cluster keeps the bot parked there instead of roaming off the congregation.
			case CRIMSON_SWIFT:
				return points(2613, 2883);
			case GOLDEN_WARBLER:
				return points(3424, 3109);
			case COPPER_LONGTAIL:
				return points(2340, 3591);
			case CERULEAN_TWITCH:
				return points(2728, 3778);
			case TROPICAL_WAGTAIL:
				// Single camp spot beside the three curated wagtail snare tiles (see SnareSpotData);
				// a single cluster keeps the bot parked here rather than roaming.
				return points(2542, 2887);
			case COMMON_KEBBIT:
				return points(2331, 3562, 2322, 3576, 2341, 3577);
			case FELDIP_WEASEL:
				return points(2525, 2889, 2554, 2882);
			case DESERT_DEVIL:
				return points(3402, 3131, 3396, 3106);
			case RAZOR_BACKED_KEBBIT:
				return points(2360, 3611, 2353, 3595);
			case RUBY_HARVEST:
				return points(2328, 3598, 2349, 3554, 2342, 3540);
			case SAPPHIRE_GLACIALIS:
				return points(2730, 3778, 2704, 3797);
			case SNOWY_KNIGHT:
				return points(2737, 3778, 2737, 3792, 2712, 3797);
			case BLACK_WARLOCK:
				return points(2566, 2886, 2540, 2898, 2540, 2914, 2563, 2920);
			case SUNLIGHT_MOTH:
				return points(1634, 3032, 1603, 3021, 1578, 3020, 1575, 2998);
			case MOONLIGHT_MOTH:
				return pointsOnPlane(0, 1554, 9433, 1568, 9439, 1573, 9446);
			case FERRET:
				return points(2311, 3519, 2313, 3510);
			case EMBERTAILED_JERBOA:
				return points(1519, 3048, 1511, 3050, 1524, 3046);
			case CHINCHOMPA:
				return points(2354, 3540, 2322, 3545, 2364, 3565, 2339, 3593,
					2316, 3606, 2341, 3618);
			case CARNIVOROUS_CHINCHOMPA:
				return points(2556, 2914, 2503, 2881, 2501, 2906, 2557, 2936);
			case BLACK_CHINCHOMPA:
				return points(3139, 3780, 3154, 3773);
			case SWAMP_LIZARD:
				return points(3537, 3445, 3549, 3449, 3549, 3437, 3558, 3444);
			case ORANGE_SALAMANDER:
				return points(3405, 3133, 3408, 3095, 3412, 3081, 3417, 3073);
			case RED_SALAMANDER:
				return points(2472, 3239, 2449, 3224);
			case BLACK_SALAMANDER:
				return points(3298, 3679, 3296, 3675, 3298, 3666);
			case TECU_SALAMANDER:
				return points(1475, 3101, 1472, 3096, 1473, 3086);
			default:
				return List.of(method.getLocation());
		}
	}

	static int closestIndex(HuntingMethod method, WorldPoint origin)
	{
		List<WorldPoint> locations = locations(method);
		if (origin == null)
		{
			return 0;
		}
		int closestIndex = 0;
		int closestDistance = Integer.MAX_VALUE;
		for (int index = 0; index < locations.size(); index++)
		{
			WorldPoint location = locations.get(index);
			if (location.getPlane() != origin.getPlane())
			{
				continue;
			}
			int distance = origin.distanceTo(location);
			if (distance < closestDistance)
			{
				closestDistance = distance;
				closestIndex = index;
			}
		}
		return closestIndex;
	}

	private static List<WorldPoint> points(int... coordinates)
	{
		return pointsOnPlane(0, coordinates);
	}

	private static List<WorldPoint> pointsOnPlane(int plane, int... coordinates)
	{
		WorldPoint[] points = new WorldPoint[coordinates.length / 2];
		for (int index = 0; index < coordinates.length; index += 2)
		{
			points[index / 2] = new WorldPoint(
				coordinates[index], coordinates[index + 1], plane);
		}
		return List.of(points);
	}
}
