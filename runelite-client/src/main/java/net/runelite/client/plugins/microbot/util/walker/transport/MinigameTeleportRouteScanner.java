package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinigameTeleport;

import java.util.List;

/** Route-order scanner for staged grouping-tab minigame teleports. */
public final class MinigameTeleportRouteScanner
{
	private static final int LANDING_TOLERANCE = 6;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, MinigameTeleportScene scene)
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
			if (edge.getKind() != RouteEdge.Kind.MINIGAME_TELEPORT)
			{
				continue;
			}
			MinigameTeleport teleport = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (teleport != null)
			{
				return interaction(plan.getGeneration(), edge, teleport);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		MinigameTeleportScene scene)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		MinigameTeleport teleport = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (teleport == null)
		{
			return MinigameTeleportPolicy.isTerminalAction(pending.getAction())
				? pending.withStatus(RouteInteraction.Status.AVAILABLE, false)
				: pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), teleport);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		MinigameTeleport teleport)
	{
		boolean unavailable = teleport.getStage() == MinigameTeleport.Stage.UNAVAILABLE;
		boolean waiting = teleport.getStage() == MinigameTeleport.Stage.WAIT_FOR_ACTIVITY;
		String action;
		switch (teleport.getStage())
		{
			case OPEN_GROUPING_TAB:
				action = MinigameTeleportPolicy.OPEN_GROUPING_TAB_ACTION;
				break;
			case OPEN_GROUPING:
				action = MinigameTeleportPolicy.OPEN_GROUPING_ACTION;
				break;
			case OPEN_DROPDOWN:
				action = MinigameTeleportPolicy.OPEN_DROPDOWN_ACTION;
				break;
			case WAIT_FOR_ACTIVITY:
				action = MinigameTeleportPolicy.WAIT_FOR_ACTIVITY_ACTION;
				break;
			case SELECT_ACTIVITY:
				action = MinigameTeleportPolicy.selectActivityAction(
					teleport.getActivityName());
				break;
			case SELECT_DESTINATION:
				action = MinigameTeleportPolicy.selectDestinationAction(
					teleport.getDestinationOption());
				break;
			case TELEPORT:
				action = MinigameTeleportPolicy.TELEPORT_ACTION;
				break;
			default:
				action = "minigame-unavailable";
				break;
		}
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(),
			edge.getTo(), teleport.getRouteOrigin(),
			RouteInteraction.Kind.MINIGAME_TELEPORT,
			unavailable ? RouteInteraction.Status.UNAVAILABLE
				: RouteInteraction.Status.AVAILABLE,
			action, !unavailable && !waiting,
			TransportType.TELEPORTATION_MINIGAME.ordinal(),
			teleport.getRouteOrigin(), teleport.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.MINIGAME_TELEPORT);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
