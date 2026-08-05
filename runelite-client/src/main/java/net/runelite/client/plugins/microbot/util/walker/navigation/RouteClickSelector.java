package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure raw-route target selection; line of sight is deliberately not an input. */
final class RouteClickSelector
{
	private RouteClickSelector()
	{
	}

	static RouteClickSelection select(RoutePlan plan, WorldPoint player, int rawProgressIndex,
		int desiredReach, int maximumReach)
	{
		RouteClickSelection selected = selectAtReach(plan, player, rawProgressIndex, desiredReach,
			"raw-route-lookahead");
		if (selected == null && desiredReach < maximumReach)
		{
			selected = selectAtReach(plan, player, rawProgressIndex, maximumReach,
				"raw-route-full-reach");
		}
		return selected;
	}

	private static RouteClickSelection selectAtReach(RoutePlan plan, WorldPoint player,
		int rawProgressIndex, int reach, String selection)
	{
		if (plan == null || player == null || reach <= 0)
		{
			return null;
		}
		List<WorldPoint> rawPath = plan.getRawPath();
		if (rawPath.isEmpty())
		{
			return null;
		}
		int anchor = Math.max(0, rawProgressIndex);
		int end = Math.min(rawPath.size() - 1, anchor + reach);
		Set<WorldPoint> visited = new HashSet<>();
		visited.add(player);
		if (anchor < rawPath.size())
		{
			visited.add(rawPath.get(anchor));
		}
		int selectedIndex = -1;
		for (int i = Math.min(rawPath.size(), anchor + 1); i <= end; i++)
		{
			WorldPoint candidate = rawPath.get(i);
			if (candidate.getPlane() != player.getPlane() || !visited.add(candidate))
			{
				break;
			}
			if (euclideanDistanceSquared(player, candidate) <= reach * reach)
			{
				selectedIndex = i;
			}
		}
		if (selectedIndex < 0)
		{
			return null;
		}
		WorldPoint target = rawPath.get(selectedIndex);
		return new RouteClickSelection(target, selectedIndex,
			smoothedIndexAtOrAfter(plan.getSmoothedToRaw(), selectedIndex),
			(int) Math.ceil(Math.sqrt(euclideanDistanceSquared(player, target))), reach, selection);
	}

	private static int smoothedIndexAtOrAfter(int[] smoothedToRaw, int rawIndex)
	{
		for (int i = 0; i < smoothedToRaw.length; i++)
		{
			if (smoothedToRaw[i] >= rawIndex)
			{
				return i;
			}
		}
		return smoothedToRaw.length - 1;
	}

	private static int euclideanDistanceSquared(WorldPoint first, WorldPoint second)
	{
		int dx = first.getX() - second.getX();
		int dy = first.getY() - second.getY();
		return dx * dx + dy * dy;
	}
}
