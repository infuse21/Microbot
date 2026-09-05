package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal;

import java.util.List;

/** Route-order scanner and directed-landing observer for teleportation portals. */
public final class TeleportationPortalRouteScanner
{
	private static final int LANDING_TOLERANCE = 4;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, TeleportationPortalScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.TELEPORTATION_PORTAL)
			{
				continue;
			}
			TeleportationPortal portal = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (portal != null)
			{
				return interaction(plan.getGeneration(), edge, portal, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		TeleportationPortalScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		TeleportationPortal portal = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getObjectId());
		if (portal == null)
		{
			// The source scene unloads during teleportation. Preserve the exact
			// directed landing predicate until the engine deadline.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (portal.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), routeEdge(pending), portal, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		TeleportationPortal portal, WorldPoint player, int interactionDistance)
	{
		boolean ready = player != null
			&& player.getPlane() == portal.getObjectTile().getPlane()
			&& player.distanceTo2D(portal.getObjectTile()) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(),
			edge.getTo(), portal.getObjectTile(),
			RouteInteraction.Kind.TELEPORTATION_PORTAL,
			RouteInteraction.Status.AVAILABLE, portal.getAction(), ready,
			portal.getCatalogObjectId(), portal.getOrigin(), portal.getDestination());
	}

	private static RouteEdge routeEdge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.TELEPORTATION_PORTAL);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
