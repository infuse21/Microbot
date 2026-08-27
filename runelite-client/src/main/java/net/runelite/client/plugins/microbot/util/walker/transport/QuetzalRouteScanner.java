package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.Quetzal;

import java.util.List;

/** Route-order scanner and directed landing observer for quetzal travel. */
public final class QuetzalRouteScanner
{
	private static final int LANDING_TOLERANCE = 4;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, QuetzalScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.QUETZAL)
			{
				continue;
			}
			Quetzal quetzal = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (quetzal != null)
			{
				return interaction(plan.getGeneration(), edge, quetzal, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		QuetzalScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		Quetzal quetzal = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (quetzal == null)
		{
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), quetzal, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		Quetzal quetzal, WorldPoint player, int interactionDistance)
	{
		boolean destination = quetzal.getStage() == Quetzal.Stage.DESTINATION;
		String action = destination
			? QuetzalPolicy.destinationAction(quetzal.getDestinationName())
			: QuetzalPolicy.NPC_ACTION;
		boolean ready = destination || player != null
			&& player.getPlane() == quetzal.getNpcTile().getPlane()
			&& player.distanceTo2D(quetzal.getNpcTile()) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			quetzal.getNpcTile(), RouteInteraction.Kind.QUETZAL,
			RouteInteraction.Status.AVAILABLE, action, ready, quetzal.getNpcId(),
			quetzal.getOrigin(), quetzal.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.QUETZAL);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
