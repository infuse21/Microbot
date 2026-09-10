package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

/** Five independently coloured Molch barriers, not Karuulm lava scenery. */
final class MolchBarrierPolicy
{
	private static final List<WorldPoint> ANCHORS = List.of(new WorldPoint(1292, 10092, 0),
		new WorldPoint(1297, 10096, 0), new WorldPoint(1308, 10096, 0),
		new WorldPoint(1317, 10096, 0), new WorldPoint(1325, 10096, 0));

	private MolchBarrierPolicy() { }

	static boolean ownsObject(int id) { return id >= 34642 && id <= 34646; }
	static WorldPoint anchor(int id) { return ownsObject(id) ? ANCHORS.get(id - 34642) : null; }
	static int varbit(int id) { return ownsObject(id) ? 7942 + id - 34642 : -1; }

	static boolean isEligible(Transport row)
	{
		return row != null && row.getType() == TransportType.TRANSPORT && row.isMembers()
			&& isRoute(row.getObjectId(), row.getOrigin(), row.getDestination())
			&& "Pass".equals(row.getAction()) && "Mystical barrier".equals(row.getName())
			&& row.getDuration() == 0 && !row.isConsumable() && row.getCurrencyAmount() == 0
			&& row.getItemIdRequirements().isEmpty() && row.getQuests().isEmpty()
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0);
	}

	private static boolean isRoute(int id, WorldPoint from, WorldPoint to)
	{
		WorldPoint anchor = anchor(id);
		if (anchor == null || from == null || to == null || from.getPlane() != 0 || to.getPlane() != 0)
			return false;
		if (id == 34642)
			return from.getX() == to.getX() && from.getX() >= anchor.getX() && from.getX() <= anchor.getX() + 1
				&& Math.abs(from.getY() - anchor.getY()) == 1 && to.getY() == 2 * anchor.getY() - from.getY();
		return from.getY() == to.getY() && from.getY() >= anchor.getY() && from.getY() <= anchor.getY() + 1
			&& Math.abs(from.getX() - anchor.getX()) == 1 && to.getX() == 2 * anchor.getX() - from.getX();
	}

	static int liveId(int state)
	{
		return state < 0 || state > 10 ? -1 : state < 3 ? 34432 : state < 6 ? 34433 : 34434;
	}

	static String liveName(int state)
	{
		return liveId(state) == 34432 ? "Mystical barrier" : liveId(state) == 34433
			? "Mystical barrier (orange)" : liveId(state) == 34434 ? "Mystical barrier (red)" : "";
	}

	static int damageBudget(int state)
	{
		// Budget the next colour when this crossing could advance the counter.
		return state < 0 || state > 10 ? -1 : state < 2 ? 0 : state < 5 ? 10 : 20;
	}

	static boolean hasCrossed(int id, WorldPoint from, WorldPoint to, WorldPoint player)
	{
		if (!isRoute(id, from, to) || player == null || player.getPlane() != 0) return false;
		WorldPoint anchor = anchor(id);
		return id == 34642 ? player.getY() == to.getY() && player.getX() >= anchor.getX()
			&& player.getX() <= anchor.getX() + 1 : player.getX() == to.getX()
			&& player.getY() >= anchor.getY() && player.getY() <= anchor.getY() + 1;
	}
}
