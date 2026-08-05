package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;

import java.util.List;

/** Route-order scanner for direct catalog-backed scene transitions. */
public final class CatalogTransitionRouteScanner
{
	private static final int LANDING_TOLERANCE = 2;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, CatalogTransitionScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.CATALOG_TRANSITION)
			{
				continue;
			}
			CatalogTransition transition = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (transition != null)
			{
				return interaction(plan.getGeneration(), edge, transition,
					player, interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		CatalogTransitionScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		CatalogTransition transition = scene.find(
			new PlannedEdge(pending.getFrom(), pending.getTo()));
		if (transition == null || transition.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.CATALOG_TRANSITION);
		return interaction(pending.getGeneration(), edge, transition,
			player, interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		CatalogTransition transition, WorldPoint player, int interactionDistance)
	{
		WorldPoint tile = transition.getObjectTile();
		boolean ready = player != null && player.getPlane() == tile.getPlane()
			&& player.distanceTo2D(tile) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			tile, RouteInteraction.Kind.CATALOG_TRANSITION,
			RouteInteraction.Status.AVAILABLE, transition.getAction(), ready,
			transition.getCatalogObjectId(), transition.getOrigin(), transition.getDestination());
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
