package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinecartTransport;

import java.util.List;

/** Route-order scanner and directed-landing observer for minecart travel. */
public final class MinecartRouteScanner
{
	private static final int LANDING_TOLERANCE = 4;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, MinecartScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.MINECART)
			{
				continue;
			}
			MinecartTransport minecart = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (minecart != null)
			{
				return interaction(plan.getGeneration(), edge, minecart, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		MinecartScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		PlannedEdge edge = new PlannedEdge(pending.getFrom(), pending.getTo());
		if (hasLanded(pending, player))
		{
			MinecartTransport restore = scene.restore(edge, pending.getAction(),
				pending.getObjectId(), player);
			if (restore == null)
			{
				return pending.withStatus(RouteInteraction.Status.CLEARED, false);
			}
			if (pending.getAction().equals(restore.getAction()))
			{
				return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
			}
			return interaction(pending.getGeneration(), routeEdge(pending), restore,
				player, interactionDistance);
		}
		MinecartTransport minecart = scene.observe(edge, pending.getAction(),
			pending.getObjectId());
		if (minecart == null)
		{
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (minecart.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), routeEdge(pending), minecart,
			player, interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		MinecartTransport minecart, WorldPoint player, int interactionDistance)
	{
		boolean unavailable = minecart.getStage()
			== MinecartTransport.Stage.DESTINATION_UNAVAILABLE
			|| minecart.getStage() == MinecartTransport.Stage.UNAVAILABLE;
		boolean objectStage = minecart.getStage() == MinecartTransport.Stage.OBJECT;
		boolean ready = !unavailable && minecart.isReady() && (!objectStage
			|| player != null && player.getPlane() == minecart.getObjectTile().getPlane()
				&& player.distanceTo2D(minecart.getObjectTile()) <= interactionDistance);
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(),
			edge.getTo(), minecart.getObjectTile(), RouteInteraction.Kind.MINECART,
			unavailable ? RouteInteraction.Status.UNAVAILABLE
				: RouteInteraction.Status.AVAILABLE,
			minecart.getAction(), ready, minecart.getCatalogObjectId(),
			minecart.getOrigin(), minecart.getDestination());
	}

	private static RouteEdge routeEdge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.MINECART);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
