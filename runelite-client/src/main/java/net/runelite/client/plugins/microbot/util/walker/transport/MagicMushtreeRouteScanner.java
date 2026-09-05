package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MagicMushtreeTransport;

import java.util.List;

/** Route-order scanner and staged landing observer for Magic Mushtree travel. */
public final class MagicMushtreeRouteScanner
{
	private static final int LANDING_TOLERANCE = 6;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, MagicMushtreeScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.MAGIC_MUSHTREE)
			{
				continue;
			}
			MagicMushtreeTransport mushtree = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (mushtree != null)
			{
				return interaction(plan.getGeneration(), edge, mushtree, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		MagicMushtreeScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		MagicMushtreeTransport mushtree = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (mushtree == null)
		{
			return MagicMushtreePolicy.isDestinationAction(pending.getAction())
				? pending.withStatus(RouteInteraction.Status.AVAILABLE, false)
				: pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		if (mushtree.getObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), mushtree, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		MagicMushtreeTransport mushtree, WorldPoint player, int interactionDistance)
	{
		boolean unavailable = mushtree.getStage()
			== MagicMushtreeTransport.Stage.DESTINATION_UNAVAILABLE;
		boolean destination = mushtree.getStage() == MagicMushtreeTransport.Stage.DESTINATION
			|| unavailable;
		String action = destination
			? MagicMushtreePolicy.destinationAction(mushtree.getDestinationName())
			: mushtree.getObjectAction();
		boolean ready = !unavailable && (destination || player != null
			&& player.getPlane() == mushtree.getObjectTile().getPlane()
			&& player.distanceTo2D(mushtree.getObjectTile()) <= interactionDistance);
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			mushtree.getObjectTile(), RouteInteraction.Kind.MAGIC_MUSHTREE,
			unavailable ? RouteInteraction.Status.UNAVAILABLE
				: RouteInteraction.Status.AVAILABLE,
			action, ready, mushtree.getObjectId(), mushtree.getOrigin(),
			mushtree.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.MAGIC_MUSHTREE);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
