package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;

/** Exact leaf-jump requirements and the separate, cache-verified Regicide recovery pits. */
public final class LeafPitPolicy
{
	public static final int LEAVES = 3925;
	public static final int HANDHOLDS = 3927;
	public static final String RECOVER = "Climb";
	private static final Map<WorldPoint, WorldPoint> ROUTES = Map.of(
		new WorldPoint(2274, 3172, 0), new WorldPoint(2274, 3176, 0),
		new WorldPoint(2274, 3176, 0), new WorldPoint(2274, 3172, 0),
		new WorldPoint(2267, 3201, 0), new WorldPoint(2267, 3205, 0),
		new WorldPoint(2267, 3205, 0), new WorldPoint(2267, 3201, 0));
	private static final Set<WorldPoint> PIT_CORNERS = Set.of(
		new WorldPoint(2313, 9656, 0), new WorldPoint(2336, 9656, 0),
		new WorldPoint(2354, 9656, 0), new WorldPoint(2354, 9643, 0));

	private LeafPitPolicy() { }

	public static boolean isRoute(WorldPoint from, WorldPoint to)
	{
		return from != null && to != null && to.equals(ROUTES.get(from));
	}

	public static boolean owns(RouteInteraction interaction)
	{
		return interaction != null && interaction.getKind() == RouteInteraction.Kind.CATALOG_TRANSITION
			&& interaction.getObjectId() == LEAVES
			&& isRoute(interaction.getCrossingFrom(), interaction.getCrossingTo());
	}

	public static boolean isEligible(Transport row)
	{
		if (row == null || row.getObjectId() != LEAVES
			|| !isRoute(row.getOrigin(), row.getDestination())
			|| (row.getType() != TransportType.TRANSPORT && row.getType() != TransportType.AGILITY_SHORTCUT)
			|| !row.isMembers()
			|| !"Jump".equals(row.getAction()) || !"Leaves".equals(row.getName())
			|| row.isConsumable() || row.getCurrencyAmount() != 0
			|| !row.getItemIdRequirements().isEmpty() || !row.getVarbits().isEmpty()
			|| !row.getVarplayers().isEmpty()
			|| !row.getQuests().equals(Map.of(Quest.REGICIDE, QuestState.IN_PROGRESS)))
		{
			return false;
		}
		for (Skill skill : Skill.values())
		{
			int expected = skill == Skill.AGILITY ? 1 : skill == Skill.HITPOINTS ? 19 : 0;
			if (row.getSkillLevels()[skill.ordinal()] != expected) return false;
		}
		return true;
	}

	public static WorldPoint nearSideLeaves(WorldPoint from, WorldPoint to)
	{
		return isRoute(from, to) ? new WorldPoint(from.getX(),
			from.getY() + Integer.signum(to.getY() - from.getY()), from.getPlane()) : null;
	}

	public static boolean inPit(WorldPoint point)
	{
		return pitCorner(point) != null;
	}

	public static boolean samePit(WorldPoint player, WorldPoint object)
	{
		WorldPoint corner = pitCorner(player);
		return corner != null && corner.equals(pitCorner(object));
	}

	private static WorldPoint pitCorner(WorldPoint point)
	{
		if (point == null || point.getPlane() != 0) return null;
		for (WorldPoint corner : PIT_CORNERS)
		{
			if (point.getX() >= corner.getX() && point.getX() <= corner.getX() + 1
				&& point.getY() >= corner.getY() && point.getY() <= corner.getY() + 1)
			{
				return corner;
			}
		}
		return null;
	}
}
