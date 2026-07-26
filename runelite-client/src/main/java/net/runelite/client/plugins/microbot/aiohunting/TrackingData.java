package net.runelite.client.plugins.microbot.aiohunting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;

final class TrackingData
{
	private static final Set<Integer> WOODLAND_STARTS = Set.of(
		ObjectID.HUNTING_TRAIL_SPAWN1,
		ObjectID.HUNTING_TRAIL_SPAWN2,
		ObjectID.HUNTING_TRAIL_SPAWN3,
		ObjectID.HUNTING_TRAIL_SPAWN4,
		ObjectID.HUNTING_TRAIL_SPAWN5,
		ObjectID.HUNTING_TRAIL_SPAWN6);
	private static final Set<Integer> JUNGLE_STARTS = Set.of(
		ObjectID.HUNTING_TRAIL_SPAWN_JUNGLE1,
		ObjectID.HUNTING_TRAIL_SPAWN_JUNGLE2);
	private static final Set<Integer> DESERT_STARTS = Set.of(
		ObjectID.HUNTING_TRAIL_SPAWN_DESERT1,
		ObjectID.HUNTING_TRAIL_SPAWN_DESERT2);
	private static final Set<Integer> POLAR_STARTS = Set.of(
		ObjectID.HUNTING_TRAIL_SPAWN_POLAR1,
		ObjectID.HUNTING_TRAIL_SPAWN_POLAR2);

	private static final List<TrailSpot> RAZOR_SPOTS = spotRanges(
		spotRange(VarbitID.HUNTING_TRAIL_STATE1_0, ObjectID.HUNTING_TRAIL_CLUE1_0, 10),
		spotRange(VarbitID.HUNTING_TRAIL_STATE2_0, ObjectID.HUNTING_TRAIL_CLUE2_0, 9));
	private static final List<TrailSpot> COMMON_SPOTS = spotRanges(
		spotRange(VarbitID.HUNTING_TRAIL_STATE3_0, ObjectID.HUNTING_TRAIL_CLUE3_0, 10),
		spotRange(VarbitID.HUNTING_TRAIL_STATE4_0, ObjectID.HUNTING_TRAIL_CLUE4_0, 7));
	private static final List<TrailSpot> DESERT_SPOTS = spotRanges(
		spotRange(VarbitID.HUNTING_TRAIL_STATE4_7, ObjectID.HUNTING_TRAIL_CLUE4_7, 2),
		spotRange(VarbitID.HUNTING_TRAIL_STATE5_0, ObjectID.HUNTING_TRAIL_CLUE5_0, 10),
		spotRange(VarbitID.HUNTING_TRAIL_STATE6_0, ObjectID.HUNTING_TRAIL_CLUE6_0, 3));
	private static final List<TrailSpot> JUNGLE_SPOTS = spotRanges(
		spotRange(VarbitID.HUNTING_TRAIL_STATE6_3, ObjectID.HUNTING_TRAIL_CLUE6_3, 6),
		spotRange(VarbitID.HUNTING_TRAIL_STATE7_0, ObjectID.HUNTING_TRAIL_CLUE7_0, 9));
	private static final List<TrailSpot> POLAR_SPOTS = spotRanges(
		spotRange(VarbitID.HUNTING_TRAIL_STATE8_0, ObjectID.HUNTING_TRAIL_CLUE8_0, 9));

	private TrackingData()
	{
	}

	static Set<Integer> startIds(HuntingMethod method)
	{
		switch (method)
		{
			case POLAR_KEBBIT:
				return POLAR_STARTS;
			case FELDIP_WEASEL:
				return JUNGLE_STARTS;
			case DESERT_DEVIL:
				return DESERT_STARTS;
			case COMMON_KEBBIT:
			case RAZOR_BACKED_KEBBIT:
				return WOODLAND_STARTS;
			default:
				return Set.of();
		}
	}

	static Set<Integer> endIds(HuntingMethod method)
	{
		switch (method)
		{
			case POLAR_KEBBIT:
				return Set.of(ObjectID.HUNTING_TRAIL_END_POLAR);
			case FELDIP_WEASEL:
				return Set.of(ObjectID.HUNTING_TRAIL_END_JUNGLE);
			case DESERT_DEVIL:
				return Set.of(ObjectID.HUNTING_TRAIL_END_DESERT);
			case COMMON_KEBBIT:
			case RAZOR_BACKED_KEBBIT:
				return Set.of(
					ObjectID.HUNTING_TRAIL_END_BUSH29,
					ObjectID.HUNTING_TRAIL_END_BUSH35_55);
			default:
				return Set.of();
		}
	}

	static int activeClueId(HuntingMethod method)
	{
		for (TrailSpot spot : spots(method))
		{
			int value = Microbot.getVarbitValue(spot.varbitId);
			if (value == 1 || value == 2)
			{
				return spot.objectId;
			}
		}
		return -1;
	}

	static int finishValue(HuntingMethod method)
	{
		int varbitId;
		switch (method)
		{
			case RAZOR_BACKED_KEBBIT:
				varbitId = VarbitID.HUNTING_TRAIL_STATE2_9;
				break;
			case COMMON_KEBBIT:
				varbitId = VarbitID.HUNTING_TRAIL_ENDS_WOOD2;
				break;
			case DESERT_DEVIL:
				varbitId = VarbitID.HUNTING_TRAIL_ENDS_DESERT;
				break;
			case FELDIP_WEASEL:
				varbitId = VarbitID.HUNTING_TRAIL_ENDS_JUNGLE;
				break;
			case POLAR_KEBBIT:
				varbitId = VarbitID.HUNTING_TRAIL_ENDS_POLAR;
				break;
			default:
				return 0;
		}
		return Microbot.getVarbitValue(varbitId);
	}

	private static List<TrailSpot> spots(HuntingMethod method)
	{
		switch (method)
		{
			case RAZOR_BACKED_KEBBIT:
				return RAZOR_SPOTS;
			case COMMON_KEBBIT:
				return COMMON_SPOTS;
			case DESERT_DEVIL:
				return DESERT_SPOTS;
			case FELDIP_WEASEL:
				return JUNGLE_SPOTS;
			case POLAR_KEBBIT:
				return POLAR_SPOTS;
			default:
				return List.of();
		}
	}

	private static TrailSpot[] spotRange(int firstVarbitId, int firstObjectId, int count)
	{
		TrailSpot[] result = new TrailSpot[count];
		for (int index = 0; index < count; index++)
		{
			result[index] = new TrailSpot(firstVarbitId + index, firstObjectId + index);
		}
		return result;
	}

	private static List<TrailSpot> spotRanges(TrailSpot[]... ranges)
	{
		List<TrailSpot> result = new ArrayList<>();
		for (TrailSpot[] range : ranges)
		{
			Collections.addAll(result, range);
		}
		return Collections.unmodifiableList(result);
	}

	private static final class TrailSpot
	{
		private final int varbitId;
		private final int objectId;

		private TrailSpot(int varbitId, int objectId)
		{
			this.varbitId = varbitId;
			this.objectId = objectId;
		}
	}
}
