package net.runelite.client.plugins.microbot.aiohunting;

import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;

final class HerbiboarData
{
	static final Set<Integer> START_OBJECT_IDS = Set.of(
		ObjectID.HUNTING_TRAIL_SPAWN_FOSSIL1,
		ObjectID.HUNTING_TRAIL_SPAWN_FOSSIL2,
		ObjectID.HUNTING_TRAIL_SPAWN_FOSSIL3,
		ObjectID.HUNTING_TRAIL_SPAWN_FOSSIL4,
		ObjectID.HUNTING_TRAIL_SPAWN_FOSSIL5);

	static final List<WorldPoint> END_LOCATIONS = List.of(
		new WorldPoint(3693, 3798, 0),
		new WorldPoint(3702, 3808, 0),
		new WorldPoint(3703, 3826, 0),
		new WorldPoint(3710, 3881, 0),
		new WorldPoint(3700, 3877, 0),
		new WorldPoint(3715, 3840, 0),
		new WorldPoint(3751, 3849, 0),
		new WorldPoint(3685, 3869, 0),
		new WorldPoint(3681, 3863, 0));

	private static final List<SearchSpot> SEARCH_SPOTS = List.of(
		spot(3670, 3889,
			trail(VarbitID.HUNTING_TRAIL_STATE9_5, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE9_6, 1)),
		spot(3672, 3890, trail(VarbitID.HUNTING_TRAIL_STATE9_1, 2)),
		spot(3728, 3893,
			trail(VarbitID.HUNTING_TRAIL_STATE9_4, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE9_5, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE10_1, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE10_2, 1)),
		spot(3697, 3875, trail(VarbitID.HUNTING_TRAIL_STATE9_0, 2)),
		spot(3699, 3875,
			trail(VarbitID.HUNTING_TRAIL_STATE9_3, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE9_4, 1)),
		spot(3708, 3876,
			trail(VarbitID.HUNTING_TRAIL_STATE9_9, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE10_0, 1)),
		spot(3710, 3877,
			trail(VarbitID.HUNTING_TRAIL_STATE9_3, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE10_2, 2)),
		spot(3668, 3865,
			trail(VarbitID.HUNTING_TRAIL_STATE10_3, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE10_4, 1)),
		spot(3667, 3862, trail(VarbitID.HUNTING_TRAIL_STATE9_6, 2)),
		spot(3681, 3860,
			trail(VarbitID.HUNTING_TRAIL_STATE9_7, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE9_8, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE10_3, 2)),
		spot(3681, 3859, trail(VarbitID.HUNTING_TRAIL_STATE9_2, 2)),
		spot(3694, 3847,
			trail(VarbitID.HUNTING_TRAIL_STATE10_0, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE10_7, 1)),
		spot(3698, 3847, trail(VarbitID.HUNTING_TRAIL_STATE9_8, 2)),
		spot(3715, 3851,
			trail(VarbitID.HUNTING_TRAIL_STATE10_8, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE10_9, 1)),
		spot(3713, 3850,
			trail(VarbitID.HUNTING_TRAIL_STATE9_9, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE11_0, 1)),
		spot(3680, 3838,
			trail(VarbitID.HUNTING_TRAIL_STATE10_5, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE10_6, 1)),
		spot(3680, 3836,
			trail(VarbitID.HUNTING_TRAIL_STATE9_7, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE10_4, 2)),
		spot(3713, 3840,
			trail(VarbitID.HUNTING_TRAIL_STATE10_8, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE11_3, 1)),
		spot(3706, 3811,
			trail(VarbitID.HUNTING_TRAIL_STATE10_5, 2),
			trail(VarbitID.HUNTING_TRAIL_STATE11_1, 1),
			trail(VarbitID.HUNTING_TRAIL_STATE11_2, 1)));

	private HerbiboarData()
	{
	}

	static WorldPoint findNextSearchSpot()
	{
		for (SearchSpot spot : SEARCH_SPOTS)
		{
			for (TrailState trail : spot.trails)
			{
				if (Microbot.getVarbitValue(trail.varbitId) == trail.value)
				{
					return spot.location;
				}
			}
		}
		return null;
	}

	static int getFinishId()
	{
		return Microbot.getVarbitValue(VarbitID.HUNTING_TRAIL_ENDS_FOSSIL);
	}

	private static SearchSpot spot(int x, int y, TrailState... trails)
	{
		return new SearchSpot(new WorldPoint(x, y, 0), List.of(trails));
	}

	private static TrailState trail(int varbitId, int value)
	{
		return new TrailState(varbitId, value);
	}

	private static final class SearchSpot
	{
		private final WorldPoint location;
		private final List<TrailState> trails;

		private SearchSpot(WorldPoint location, List<TrailState> trails)
		{
			this.location = location;
			this.trails = trails;
		}
	}

	private static final class TrailState
	{
		private final int varbitId;
		private final int value;

		private TrailState(int varbitId, int value)
		{
			this.varbitId = varbitId;
			this.value = value;
		}
	}
}
