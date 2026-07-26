package net.runelite.client.plugins.microbot.aiohunting;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;

/**
 * Priority-ordered deadfall boulder tiles per method. Deadfall boulders (object 19215) are fixed,
 * single-occupancy spots shared with other players - when someone has armed a boulder its base
 * object is gone, so {@link TrapActivity} walks the list in order and uses the first boulder still
 * present (available), falling back to the next when a preferred one is in use.
 *
 * <p>Methods without an entry fall back to nearest-boulder behaviour.</p>
 */
final class DeadfallSpotData
{
	private DeadfallSpotData()
	{
	}

	static List<WorldPoint> boulders(HuntingMethod method)
	{
		switch (method)
		{
			case WILD_KEBBIT:
				// Use (2318,3561) first; fall back to (2330,3553) when it's in use.
				return points(2318, 3561, 2330, 3553);
			default:
				return List.of();
		}
	}

	private static List<WorldPoint> points(int... coordinates)
	{
		WorldPoint[] points = new WorldPoint[coordinates.length / 2];
		for (int index = 0; index < coordinates.length; index += 2)
		{
			points[index / 2] = new WorldPoint(
				coordinates[index], coordinates[index + 1], 0);
		}
		return List.of(points);
	}
}
