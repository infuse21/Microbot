package net.runelite.client.plugins.microbot.aiohunting;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;

/**
 * Curated, exact trap tiles for bird-snare methods where the community has mapped the precise
 * tiles that several bird spawns overlap on. Because birds wander randomly after spawning, the
 * efficient setup is to pin snares to these fixed tiles so respawned birds land straight back on
 * a trap, rather than chasing whichever bird happens to be nearest.
 *
 * <p>When a method has curated spots and the player is camped in that area, {@link TrapActivity}
 * prefers these tiles over the dynamic bird-anchoring fallback. Methods without curated data fall
 * through to that dynamic behaviour, so this table can be filled in one method at a time.</p>
 */
final class SnareSpotData
{
	private SnareSpotData()
	{
	}

	static List<WorldPoint> spots(HuntingMethod method)
	{
		switch (method)
		{
			case CRIMSON_SWIFT:
				// Feldip Hunter area - eastern coastline beach, SE of fairy ring AKS.
				return points(2611, 2884, 2612, 2882, 2615, 2883);
			case GOLDEN_WARBLER:
				// Uzer Hunter area - desert west of the ruins of Uzer.
				return points(3422, 3108, 3423, 3110, 3426, 3110);
			case COPPER_LONGTAIL:
				// Piscatoris Hunter area - south of the falconry area, fairy ring AKQ.
				return points(2339, 3591, 2341, 3592, 2341, 3589);
			case CERULEAN_TWITCH:
				// Rellekka Hunter area - snowy region NE of Rellekka, fairy ring DKS then north.
				return points(2727, 3778, 2729, 3779, 2729, 3776);
			case TROPICAL_WAGTAIL:
				// Feldip Hunter area - three overlapping wagtail spawns congregate here.
				return points(2543, 2888, 2542, 2888, 2541, 2887);
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
