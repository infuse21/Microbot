package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.GnomeGlider;

import java.util.List;

/** Route-order scanner and directed landing observer for gnome-glider travel. */
public final class GnomeGliderRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, GnomeGliderScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.GNOME_GLIDER)
			{
				continue;
			}
			GnomeGlider glider = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (glider != null)
			{
				return interaction(plan.getGeneration(), edge, glider, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		GnomeGliderScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		GnomeGlider glider = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (glider == null)
		{
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (glider.getNpcId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), glider, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		GnomeGlider glider, WorldPoint player, int interactionDistance)
	{
		boolean unavailable = glider.getStage()
			== GnomeGlider.Stage.DESTINATION_UNAVAILABLE;
		boolean destination = glider.getStage() == GnomeGlider.Stage.DESTINATION
			|| unavailable;
		String action = destination
			? GnomeGliderPolicy.destinationAction(glider.getDestinationName())
			: glider.getNpcAction();
		boolean ready = !unavailable && (destination || player != null
			&& player.getPlane() == glider.getNpcTile().getPlane()
			&& player.distanceTo2D(glider.getNpcTile()) <= interactionDistance);
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			glider.getNpcTile(), RouteInteraction.Kind.GNOME_GLIDER,
			unavailable ? RouteInteraction.Status.UNAVAILABLE
				: RouteInteraction.Status.AVAILABLE,
			action, ready, glider.getNpcId(), glider.getOrigin(), glider.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.GNOME_GLIDER);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
