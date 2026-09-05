package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.HotAirBalloonTransport;

import java.util.List;

/** Route-order scanner and directed landing observer for hot-air-balloon travel. */
public final class HotAirBalloonRouteScanner
{
	private static final int LANDING_TOLERANCE = 6;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, HotAirBalloonScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.HOT_AIR_BALLOON)
			{
				continue;
			}
			HotAirBalloonTransport balloon = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (balloon != null)
			{
				return interaction(plan.getGeneration(), edge, balloon, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		HotAirBalloonScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		HotAirBalloonTransport balloon = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (balloon == null)
		{
			return HotAirBalloonPolicy.isDestinationAction(pending.getAction())
				? pending.withStatus(RouteInteraction.Status.AVAILABLE, false)
				: pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		if (!HotAirBalloonPolicy.isBasketObjectId(balloon.getObjectId())
			|| !HotAirBalloonPolicy.isBasketObjectId(pending.getObjectId()))
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), balloon, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		HotAirBalloonTransport balloon, WorldPoint player, int interactionDistance)
	{
		boolean unavailable = balloon.getStage()
			== HotAirBalloonTransport.Stage.DESTINATION_UNAVAILABLE;
		boolean destination = balloon.getStage() == HotAirBalloonTransport.Stage.DESTINATION
			|| unavailable;
		String action = destination
			? HotAirBalloonPolicy.destinationAction(balloon.getDestinationName())
			: balloon.getObjectAction();
		boolean ready = !unavailable && (destination || player != null
			&& player.getPlane() == balloon.getObjectTile().getPlane()
			&& player.distanceTo2D(balloon.getObjectTile()) <= interactionDistance);
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			balloon.getObjectTile(), RouteInteraction.Kind.HOT_AIR_BALLOON,
			unavailable ? RouteInteraction.Status.UNAVAILABLE
				: RouteInteraction.Status.AVAILABLE,
			action, ready, balloon.getObjectId(), balloon.getOrigin(),
			balloon.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.HOT_AIR_BALLOON);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
