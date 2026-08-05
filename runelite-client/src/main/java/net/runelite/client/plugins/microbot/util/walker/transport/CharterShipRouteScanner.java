package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CharterShip;

import java.util.List;

/** Route-order scanner and stage observer for paid charter-ship travel. */
public final class CharterShipRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, CharterShipScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.CHARTER_SHIP)
			{
				continue;
			}
			CharterShip charter = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (charter != null)
			{
				return interaction(plan.getGeneration(), edge, charter, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		CharterShipScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		CharterShip charter = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (charter == null)
		{
			// The interface and source NPC disappear while the paid voyage is in
			// progress. Preserve the exact landing predicate until the deadline.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (charter.getNpcId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.CHARTER_SHIP);
		return interaction(pending.getGeneration(), edge, charter, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		CharterShip charter, WorldPoint player, int interactionDistance)
	{
		String action;
		boolean ready;
		switch (charter.getStage())
		{
			case DESTINATION:
				action = CharterShipPolicy.destinationAction(charter.getDestinationName());
				ready = true;
				break;
			case CONFIRM:
				action = CharterShipPolicy.CONFIRM_ACTION;
				ready = true;
				break;
			case NPC:
			default:
				action = charter.getNpcAction();
				ready = player != null && player.getPlane() == charter.getNpcTile().getPlane()
					&& player.distanceTo2D(charter.getNpcTile()) <= interactionDistance;
				break;
		}
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			charter.getNpcTile(), RouteInteraction.Kind.CHARTER_SHIP,
			RouteInteraction.Status.AVAILABLE, action, ready, charter.getNpcId(),
			charter.getOrigin(), charter.getDestination());
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
