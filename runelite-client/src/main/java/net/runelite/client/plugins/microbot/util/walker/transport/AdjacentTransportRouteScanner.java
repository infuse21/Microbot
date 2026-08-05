package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;

import java.util.List;

/** Pure route-order scanner for the first engine-owned transport family. */
public final class AdjacentTransportRouteScanner
{
	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, AdjacentTransportScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.ADJACENT_TRANSPORT)
			{
				continue;
			}
			AdjacentTransport transport = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (transport != null)
			{
				return interaction(plan.getGeneration(), edge, transport, player, interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		AdjacentTransportScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasCrossedCatalogBoundary(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		AdjacentTransport transport = scene.find(new PlannedEdge(pending.getFrom(), pending.getTo()));
		if (transport == null || transport.getObjectId() != pending.getObjectId())
		{
			return pending.withStatus(AdjacentTransportPolicy.actionClearsObject(pending.getAction())
				? RouteInteraction.Status.CLEARED : RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.ADJACENT_TRANSPORT);
		return interaction(pending.getGeneration(), edge, transport, player, interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		AdjacentTransport transport, WorldPoint player, int interactionDistance)
	{
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			transport.getObjectTile(), RouteInteraction.Kind.ADJACENT_TRANSPORT,
			RouteInteraction.Status.AVAILABLE, transport.getAction(),
			player != null && player.distanceTo2D(transport.getObjectTile()) <= interactionDistance,
			transport.getObjectId(),
			transport.getOrigin() == null ? edge.getFrom() : transport.getOrigin(),
			transport.getDestination() == null ? edge.getTo() : transport.getDestination());
	}

	private static boolean hasCrossedCatalogBoundary(RouteInteraction pending, WorldPoint player)
	{
		if (player == null || player.getPlane() != pending.getCrossingTo().getPlane()
			|| player.distanceTo2D(pending.getCrossingTo()) > 2)
		{
			return false;
		}
		WorldPoint from = pending.getCrossingFrom();
		WorldPoint to = pending.getCrossingTo();
		int dx = Integer.compare(to.getX(), from.getX());
		int dy = Integer.compare(to.getY(), from.getY());
		return (dx != 0 && (player.getX() - to.getX()) * dx >= 0)
			|| (dy != 0 && (player.getY() - to.getY()) * dy >= 0);
	}
}
