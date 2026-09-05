package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.JungleObstacle;

import java.util.List;

/** Route-order scanner for item-gated jungle chopping and directed landing. */
public final class JungleObstacleRouteScanner
{
	private static final int LANDING_TOLERANCE = 1;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, JungleObstacleScene scene, int interactionDistance)
	{
		if (plan == null || scene == null)
		{
			return null;
		}
		List<RouteEdge> edges = plan.getRouteEdges();
		int start = Math.max(0, startRawIndex);
		int end = Math.min(edges.size(), start + Math.max(0, maxEdges));
		for (int i = start; i < end; i++)
		{
			RouteEdge edge = edges.get(i);
			if (edge.getKind() != RouteEdge.Kind.JUNGLE_OBSTACLE)
			{
				continue;
			}
			JungleObstacle obstacle = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (obstacle != null)
			{
				return interaction(plan.getGeneration(), edge, obstacle, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		JungleObstacleScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		JungleObstacle obstacle = scene.find(
			new PlannedEdge(pending.getFrom(), pending.getTo()));
		if (obstacle == null || obstacle.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.JUNGLE_OBSTACLE);
		return interaction(pending.getGeneration(), edge, obstacle, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		JungleObstacle obstacle, WorldPoint player, int interactionDistance)
	{
		WorldPoint tile = obstacle.getObjectTile();
		boolean ready = player != null && player.getPlane() == tile.getPlane()
			&& player.distanceTo2D(tile) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			tile, RouteInteraction.Kind.JUNGLE_OBSTACLE,
			RouteInteraction.Status.AVAILABLE, obstacle.getAction(), ready,
			obstacle.getCatalogObjectId(), obstacle.getOrigin(), obstacle.getDestination());
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
