package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.door.model.OrdinaryDoor;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;

import java.util.List;

/** Pure forward-route scanner for ordinary door/gate interactions. */
public final class OrdinaryDoorRouteScanner
{
	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, DoorScene scene, int interactionDistance)
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
			RouteEdge routeEdge = edges.get(i);
			if (routeEdge.getKind() != RouteEdge.Kind.WALK)
			{
				continue;
			}
			PlannedEdge edge = new PlannedEdge(routeEdge.getFrom(), routeEdge.getTo());
			OrdinaryDoor door = scene.findOrdinaryDoor(edge);
			if (door != null)
			{
				return interaction(plan.getGeneration(), i, edge, door, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction scan(long generation, List<WorldPoint> rawPath, int startRawIndex,
		int maxEdges, WorldPoint player, DoorScene scene, int interactionDistance)
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
			OrdinaryDoor door = scene.findOrdinaryDoor(edge);
			if (door != null)
			{
				return interaction(generation, i, edge, door, player, interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		DoorScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		PlannedEdge edge = new PlannedEdge(pending.getFrom(), pending.getTo());
		OrdinaryDoor door = scene.findOrdinaryDoor(edge);
		if (door == null || !door.getTile().equals(pending.getObjectTile())
			|| !door.getAction().equalsIgnoreCase(pending.getAction()))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, true);
		}
		return interaction(pending.getGeneration(), pending.getRawEdgeIndex(), edge, door,
			player, interactionDistance);
	}

	private static RouteInteraction interaction(long generation, int edgeIndex, PlannedEdge edge,
		OrdinaryDoor door, WorldPoint player, int interactionDistance)
	{
		return new RouteInteraction(generation, edgeIndex, edge.from(), edge.to(), door.getTile(),
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.AVAILABLE, door.getAction(),
			player != null && player.distanceTo2D(door.getTile()) <= interactionDistance);
	}
}
