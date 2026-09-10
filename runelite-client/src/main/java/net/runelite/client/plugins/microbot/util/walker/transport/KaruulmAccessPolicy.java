package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;

/** Safe entrance/escape edges and protected onward travel, never an unprotected warning confirmation. */
final class KaruulmAccessPolicy
{
	static final int ELITE_REWARD = 7932;
	private static final Map<String, Boolean> ROUTES = Map.ofEntries(
		Map.entry("34359|1311 3806 0>1311 10189 0", false),
		Map.entry("34359|1311 3808 0>1311 10189 0", false),
		Map.entry("34359|1311 3806 0>1312 10189 0", false),
		Map.entry("34359|1311 3808 0>1312 10189 0", false),
		Map.entry("34544|1303 10205 0>1301 10205 0", true),
		Map.entry("34544|1303 10206 0>1301 10206 0", true),
		Map.entry("34544|1301 10205 0>1303 10205 0", false),
		Map.entry("34544|1301 10206 0>1303 10206 0", false),
		Map.entry("34544|1311 10214 0>1311 10215 0", true),
		Map.entry("34544|1312 10214 0>1312 10215 0", true),
		Map.entry("34544|1311 10215 0>1311 10214 0", false),
		Map.entry("34544|1312 10215 0>1312 10214 0", false),
		Map.entry("34544|1320 10205 0>1322 10205 0", true),
		Map.entry("34544|1320 10206 0>1322 10206 0", true),
		Map.entry("34544|1322 10205 0>1320 10205 0", false),
		Map.entry("34544|1322 10206 0>1320 10206 0", false),
		Map.entry("34530|1329 10205 0>1334 10205 1", true),
		Map.entry("34530|1329 10206 0>1334 10206 1", true),
		Map.entry("34531|1334 10205 1>1329 10205 0", false),
		Map.entry("34531|1334 10206 1>1329 10206 0", false),
		Map.entry("34530|1313 10188 1>1318 10188 2", true),
		Map.entry("34530|1313 10189 1>1318 10189 2", true),
		Map.entry("34531|1318 10188 2>1313 10188 1", false),
		Map.entry("34531|1318 10189 2>1313 10189 1", false));
	private static final Map<Integer, Map<Skill, Integer>> BOOTS = Map.of(
		23037, Map.of(Skill.SLAYER, 44),
		22951, Map.of(Skill.SLAYER, 44, Skill.DEFENCE, 70, Skill.MAGIC, 70, Skill.RANGED, 70),
		21643, Map.of(Skill.DEFENCE, 50, Skill.STRENGTH, 50));

	private KaruulmAccessPolicy() { }

	static boolean ownsObject(int id) { return id == 34359 || id == 34544 || id == 34530 || id == 34531; }

	static boolean isEligible(Transport row)
	{
		if (row == null || row.getType() != TransportType.TRANSPORT || !row.isMembers()
			|| row.getOrigin() == null || row.getDestination() == null || row.isConsumable()
			|| row.getDuration() != 0 || row.getCurrencyAmount() != 0 || !row.getQuests().isEmpty()
			|| !row.getVarplayers().isEmpty()) return false;
		Boolean hot = ROUTES.get(row.getObjectId() + "|" + point(row.getOrigin()) + ">" + point(row.getDestination()));
		if (hot == null || !(row.getObjectId() == 34359 ? "Activate" : "Climb").equals(row.getAction())
			|| !(row.getObjectId() == 34359 ? "Elevator" : row.getObjectId() == 34544 ? "Rocks" : "Steps").equals(row.getName()))
			return false;
		if (!hot) return row.getItemIdRequirements().isEmpty() && row.getVarbits().isEmpty() && skillsEqual(row, Map.of());
		if (row.getVarbits().size() != 1) return false;
		TransportVarbit bit = row.getVarbits().iterator().next();
		if (bit.getVarbitId() != ELITE_REWARD || bit.getValue() != 0) return false;
		if (bit.getOperator() == TransportVarbit.Operator.GREATER_THAN)
			return row.getItemIdRequirements().isEmpty() && skillsEqual(row, Map.of());
		if (bit.getOperator() != TransportVarbit.Operator.EQUAL) return false;
		for (Map.Entry<Integer, Map<Skill, Integer>> boots : BOOTS.entrySet())
			if (row.getItemIdRequirements().equals(Set.of(Set.of(boots.getKey()))) && skillsEqual(row, boots.getValue())) return true;
		return false;
	}

	static boolean skillsMet(Transport row, int[] realLevels)
	{
		if (!isEligible(row) || realLevels == null || realLevels.length != row.getSkillLevels().length) return false;
		for (int i = 0; i < realLevels.length; i++)
			if (realLevels[i] < row.getSkillLevels()[i]) return false;
		return true;
	}

	static boolean hasLanded(int id, WorldPoint from, WorldPoint to, WorldPoint player)
	{
		if (from == null || to == null || player == null || player.getPlane() != to.getPlane()
			|| !ROUTES.containsKey(id + "|" + point(from) + ">" + point(to))) return false;
		if (player.equals(to)) return true;
		// Paired catalog landings describe the same two-tile elevator/stair landing area.
		if (id == 34359) return player.getY() == 10189 && (player.getX() == 1311 || player.getX() == 1312);
		if (id != 34530 && id != 34531 || player.getX() != to.getX()) return false;
		return to.getY() >= 10205 && to.getY() <= 10206 && player.getY() >= 10205 && player.getY() <= 10206
			|| to.getY() >= 10188 && to.getY() <= 10189 && player.getY() >= 10188 && player.getY() <= 10189;
	}

	private static boolean skillsEqual(Transport row, Map<Skill, Integer> expected)
	{
		for (Skill skill : Skill.values())
			if (row.getSkillLevels()[skill.ordinal()] != expected.getOrDefault(skill, 0)) return false;
		return true;
	}

	private static String point(WorldPoint point) { return point.getX() + " " + point.getY() + " " + point.getPlane(); }
}
