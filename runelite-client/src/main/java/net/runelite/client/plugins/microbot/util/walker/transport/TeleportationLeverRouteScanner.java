package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationLever;

import java.util.List;

/** Route-order scanner and staged landing observer for teleportation levers. */
public final class TeleportationLeverRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, TeleportationLeverScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.TELEPORTATION_LEVER)
			{
				continue;
			}
			TeleportationLever lever = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (lever != null)
			{
				return interaction(plan.getGeneration(), edge, lever, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		TeleportationLeverScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		TeleportationLever lever = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction(),
			pending.getObjectId());
		if (lever == null)
		{
			// The source object or warning UI can disappear while the teleport runs.
			// Preserve the exact directed landing predicate until the engine deadline.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (lever.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.TELEPORTATION_LEVER);
		return interaction(pending.getGeneration(), edge, lever, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		TeleportationLever lever, WorldPoint player, int interactionDistance)
	{
		boolean ready = lever.getStage() != TeleportationLever.Stage.OBJECT
			|| player != null && player.getPlane() == lever.getObjectTile().getPlane()
				&& player.distanceTo2D(lever.getObjectTile()) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(),
			edge.getTo(), lever.getObjectTile(), RouteInteraction.Kind.TELEPORTATION_LEVER,
			RouteInteraction.Status.AVAILABLE, lever.getAction(), ready,
			lever.getCatalogObjectId(), lever.getOrigin(), lever.getDestination());
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
