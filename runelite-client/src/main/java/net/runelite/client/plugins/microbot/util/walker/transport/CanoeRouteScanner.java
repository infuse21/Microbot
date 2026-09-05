package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CanoeTransport;

import java.util.List;

/** Route-order scanner and staged directed-landing observer for River Lum canoes. */
public final class CanoeRouteScanner
{
	private static final int LANDING_TOLERANCE = 6;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, CanoeScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.CANOE)
			{
				continue;
			}
			CanoeTransport canoe = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (canoe != null)
			{
				return interaction(plan.getGeneration(), edge, canoe, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		CanoeScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		CanoeTransport canoe = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction(),
			pending.getObjectId());
		if (hasLanded(pending, player)
			&& (canoe == null || canoe.getStage() != CanoeTransport.Stage.ARRIVAL))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		if (canoe == null)
		{
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (canoe.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), canoe, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		CanoeTransport canoe, WorldPoint player, int interactionDistance)
	{
		boolean unavailable = canoe.getStage() == CanoeTransport.Stage.SHAPE_UNAVAILABLE
			|| canoe.getStage() == CanoeTransport.Stage.DESTINATION_UNAVAILABLE;
		boolean uiStage = canoe.getStage() != CanoeTransport.Stage.OBJECT;
		boolean ready = !unavailable && (uiStage || player != null
			&& player.getPlane() == canoe.getObjectTile().getPlane()
			&& player.distanceTo2D(canoe.getObjectTile()) <= interactionDistance);
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			canoe.getObjectTile(), RouteInteraction.Kind.CANOE,
			unavailable ? RouteInteraction.Status.UNAVAILABLE
				: RouteInteraction.Status.AVAILABLE,
			canoe.getAction(), ready, canoe.getCatalogObjectId(),
			canoe.getOrigin(), canoe.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.CANOE);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
