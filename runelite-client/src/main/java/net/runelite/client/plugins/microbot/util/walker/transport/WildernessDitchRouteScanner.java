package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.WildernessDitch;

import java.util.List;

/** Route-order scanner for ditch object, optional warning, and directed landing. */
public final class WildernessDitchRouteScanner
{
	private static final int LANDING_TOLERANCE = 2;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, WildernessDitchScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.WILDERNESS_DITCH)
			{
				continue;
			}
			WildernessDitch ditch = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (ditch != null)
			{
				return interaction(plan.getGeneration(), edge, ditch, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		WildernessDitchScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		WildernessDitch ditch = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction(),
			pending.getObjectId());
		if (ditch == null)
		{
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (ditch.getCatalogObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.WILDERNESS_DITCH);
		return interaction(pending.getGeneration(), edge, ditch, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		WildernessDitch ditch, WorldPoint player, int interactionDistance)
	{
		boolean ready = ditch.getStage() == WildernessDitch.Stage.WARNING
			|| player != null && player.getPlane() == ditch.getObjectTile().getPlane()
				&& player.distanceTo2D(ditch.getObjectTile()) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			ditch.getObjectTile(), RouteInteraction.Kind.WILDERNESS_DITCH,
			RouteInteraction.Status.AVAILABLE, ditch.getAction(), ready,
			ditch.getCatalogObjectId(), ditch.getOrigin(), ditch.getDestination());
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
