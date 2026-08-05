package net.runelite.client.plugins.microbot.util.walker.obstacle;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;

import java.util.List;

/** Pure forward-route scanner for mineable interaction observations. */
public final class MineableRouteScanner
{
	private final MineableResolver resolver;

	public MineableRouteScanner(MineableResolver resolver)
	{
		this.resolver = resolver;
	}

	public RouteInteraction scan(long generation, List<WorldPoint> rawPath, int startRawIndex,
		int maxEdges, LiveScene scene, boolean available, int interactionDistance)
	{
		if (rawPath == null || rawPath.size() < 2 || scene == null)
		{
			return null;
		}
		int start = Math.max(0, startRawIndex);
		int end = Math.min(rawPath.size() - 1, start + Math.max(0, maxEdges));
		for (int i = start; i < end; i++)
		{
			PlannedEdge edge = new PlannedEdge(rawPath.get(i), rawPath.get(i + 1));
			WorldPoint blocker = resolver.blockerTile(edge, scene);
			// The engine owns forward edges. An object on the origin belongs to the preceding
			// edge and may already be behind the player; do not turn it into a backward approach.
			if (blocker != null && blocker.equals(edge.to()))
			{
				WorldPoint player = scene.playerLocation();
				return new RouteInteraction(generation, i, edge.from(), edge.to(), blocker,
					RouteInteraction.Kind.MINEABLE, available
						? RouteInteraction.Status.AVAILABLE : RouteInteraction.Status.UNAVAILABLE,
					"mine", player != null && player.distanceTo2D(blocker) <= interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, LiveScene scene,
		boolean available, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		PlannedEdge edge = new PlannedEdge(pending.getFrom(), pending.getTo());
		WorldPoint blocker = resolver.blockerTile(edge, scene);
		if (blocker == null)
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, true);
		}
		WorldPoint player = scene.playerLocation();
		return pending.withStatus(available
				? RouteInteraction.Status.AVAILABLE : RouteInteraction.Status.UNAVAILABLE,
			player != null && player.distanceTo2D(blocker) <= interactionDistance);
	}
}
