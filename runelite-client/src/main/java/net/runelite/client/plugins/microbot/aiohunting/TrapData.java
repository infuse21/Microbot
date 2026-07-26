package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingStyle;

/**
 * Object-ID catalogue for the four trap-based Hunter methods (bird snare, box, net,
 * deadfall). Maps each trap type to its open / full / failed / in-transition object ids
 * and exposes the union used to recognise any hunter trap on the ground.
 */
final class TrapData
{
	private static final Set<Integer> BIRD_OPEN = Set.of(
		ObjectID.HUNTING_OJIBWAY_TRAP,
		ObjectID.HUNTING_OJIBWAY_TRAP_FAILING);
	private static final Set<Integer> BIRD_FULL = Set.of(
		ObjectID.HUNTING_OJIBWAY_TRAP_FULL_JUNGLE,
		ObjectID.HUNTING_OJIBWAY_TRAP_FULL_POLAR,
		ObjectID.HUNTING_OJIBWAY_TRAP_FULL_DESERT,
		ObjectID.HUNTING_OJIBWAY_TRAP_FULL_WOODLAND,
		ObjectID.HUNTING_OJIBWAY_TRAP_FULL_COLOURED);
	private static final Set<Integer> BIRD_FAILED =
		Set.of(ObjectID.HUNTING_OJIBWAY_TRAP_BROKEN);

	private static final Set<Integer> BOX_OPEN =
		Set.of(ObjectID.HUNTING_BOXTRAP_EMPTY, ObjectID.HUNTING_BOXTRAP_FAILING);
	private static final Set<Integer> BOX_FULL = Set.of(
		ObjectID.HUNTING_BOXTRAP_FULL_CHINCHOMPA_BLACK,
		ObjectID.HUNTING_BOXTRAP_FULL_CHINCHOMPA,
		ObjectID.HUNTING_BOXTRAP_FULL_CHINCHOMPA_BIG,
		ObjectID.HUNTING_BOXTRAP_FULL_FERRET);
	private static final Set<Integer> BOX_FAILED =
		Set.of(ObjectID.HUNTING_BOXTRAP_FAILED);

	private static final Set<Integer> NET_OPEN = Set.of(
		ObjectID.HUNTING_SAPLING_NET_SET_SWAMP,
		ObjectID.HUNTING_SAPLING_NET_SET_ORANGE,
		ObjectID.HUNTING_SAPLING_NET_SET_RED,
		ObjectID.HUNTING_SAPLING_NET_SET_BLACK,
		ObjectID.HUNTING_SAPLING_NET_SET_MOUNTAIN);
	private static final Set<Integer> NET_FULL = Set.of(
		ObjectID.HUNTING_SAPLING_FULL_GREEN,
		ObjectID.HUNTING_SAPLING_FULL_ORANGE,
		ObjectID.HUNTING_SAPLING_FULL_RED,
		ObjectID.HUNTING_SAPLING_FULL_BLACK,
		ObjectID.HUNTING_SAPLING_FULL_MOUNTAIN);
	private static final Set<Integer> NET_FAILED = Set.of(
		ObjectID.HUNTING_SAPLING_FAILED_SWAMP,
		ObjectID.HUNTING_SAPLING_FAILED_ORANGE,
		ObjectID.HUNTING_SAPLING_FAILED_RED,
		ObjectID.HUNTING_SAPLING_FAILED_BLACK,
		ObjectID.HUNTING_SAPLING_FAILED_MOUNTAIN);

	private static final Set<Integer> DEADFALL_OPEN =
		Set.of(ObjectID.HUNTING_DEADFALL_TRAP);
	private static final Set<Integer> DEADFALL_FULL = Set.of(
		ObjectID.HUNTING_DEADFALL_FULL_SPIKE,
		ObjectID.HUNTING_DEADFALL_FULL_SABRE,
		ObjectID.HUNTING_DEADFALL_FULL_BARBED,
		ObjectID.HUNTING_DEADFALL_FULL_CLAW,
		ObjectID.HUNTING_DEADFALL_FULL_FENNEC);
	private static final Set<Integer> DEADFALL_FAILED =
		Set.of(ObjectID.HUNTING_DEADFALL_BOULDER);

	private static final Set<Integer> TRAP_TRANSITIONS = Set.of(
		ObjectID.HUNTING_OJIBWAY_TRAP_TRAPPING_COLOURED,
		ObjectID.HUNTING_OJIBWAY_TRAP_TRAPPING_JUNGLE,
		ObjectID.HUNTING_OJIBWAY_TRAP_TRAPPING_POLAR,
		ObjectID.HUNTING_OJIBWAY_TRAP_TRAPPING_DESERT,
		ObjectID.HUNTING_OJIBWAY_TRAP_TRAPPING_WOODLAND,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BLACK_N,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BLACK_E,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BLACK_S,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BLACK_W,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_N,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_E,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_S,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_W,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BIG_N,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BIG_E,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BIG_S,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_CHINCHOMPA_BIG_W,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_FERRET_N,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_FERRET_E,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_FERRET_S,
		ObjectID.HUNTING_BOXTRAP_TRAPPING_FERRET_W,
		ObjectID.HUNTING_SAPLING_SETTING_SWAMP,
		ObjectID.HUNTING_SAPLING_SETTING_ORANGE,
		ObjectID.HUNTING_SAPLING_SETTING_RED,
		ObjectID.HUNTING_SAPLING_SETTING_BLACK,
		ObjectID.HUNTING_SAPLING_SETTING_MOUNTAIN,
		ObjectID.HUNTING_SAPLING_CATCHING_GREEN,
		ObjectID.HUNTING_SAPLING_CATCHING_ORANGE,
		ObjectID.HUNTING_SAPLING_CATCHING_RED,
		ObjectID.HUNTING_SAPLING_CATCHING_BLACK,
		ObjectID.HUNTING_SAPLING_CATCHING_MOUNTAIN,
		ObjectID.HUNTING_DEADFALL_SETTING,
		ObjectID.HUNTING_DEADFALL_TRAPPING_SPIKE,
		ObjectID.HUNTING_DEADFALL_TRAPPING_SABRE,
		ObjectID.HUNTING_DEADFALL_TRAPPING_BARBED,
		ObjectID.HUNTING_DEADFALL_TRAPPING_CLAW,
		ObjectID.HUNTING_DEADFALL_TRAPPING_FENNEC);

	static final Set<Integer> ALL_TRAP_OBJECTS = union(
		BIRD_OPEN, BIRD_FULL, BIRD_FAILED, BOX_OPEN, BOX_FULL, BOX_FAILED,
		NET_OPEN, NET_FULL, NET_FAILED, DEADFALL_OPEN, DEADFALL_FULL,
		TRAP_TRANSITIONS);

	private TrapData()
	{
	}

	static boolean isHunterTrapObjectId(int objectId)
	{
		return ALL_TRAP_OBJECTS.contains(objectId);
	}

	static Set<Integer> fullIds(HuntingStyle style)
	{
		switch (style)
		{
			case BIRD_SNARE:
				return BIRD_FULL;
			case BOX_TRAP:
				return BOX_FULL;
			case NET_TRAP:
				return NET_FULL;
			case DEADFALL:
				return DEADFALL_FULL;
			default:
				return Collections.emptySet();
		}
	}

	static Set<Integer> failedIds(HuntingStyle style)
	{
		switch (style)
		{
			case BIRD_SNARE:
				return BIRD_FAILED;
			case BOX_TRAP:
				return BOX_FAILED;
			case NET_TRAP:
				return NET_FAILED;
			case DEADFALL:
				return DEADFALL_FAILED;
			default:
				return Collections.emptySet();
		}
	}

	static Set<Integer> openIds(HuntingStyle style)
	{
		switch (style)
		{
			case BIRD_SNARE:
				return BIRD_OPEN;
			case BOX_TRAP:
				return BOX_OPEN;
			case NET_TRAP:
				return NET_OPEN;
			case DEADFALL:
				return DEADFALL_OPEN;
			default:
				return Collections.emptySet();
		}
	}

	@SafeVarargs
	private static Set<Integer> union(Set<Integer>... sets)
	{
		Set<Integer> result = new HashSet<>();
		for (Set<Integer> set : sets)
		{
			result.addAll(set);
		}
		return Collections.unmodifiableSet(result);
	}
}
