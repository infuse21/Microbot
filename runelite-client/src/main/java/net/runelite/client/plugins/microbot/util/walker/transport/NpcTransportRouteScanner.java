package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcTransport;

import java.util.List;

/** Route-order scanner and landing observer for direct NPC-backed travel. */
public final class NpcTransportRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, NpcTransportScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.NPC_TRANSPORT)
			{
				continue;
			}
			NpcTransport transport = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (transport != null)
			{
				return interaction(plan.getGeneration(), edge, transport, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		NpcTransportScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		NpcTransport transport = scene.find(new PlannedEdge(pending.getFrom(), pending.getTo()));
		if (transport == null)
		{
			// The source NPC can unload as travel begins. Retain the directed landing
			// predicate and let the engine's bounded acknowledgement deadline decide.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, true);
		}
		if (transport.getNpcId() != pending.getObjectId()
			|| !transport.getAction().equalsIgnoreCase(pending.getAction()))
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.NPC_TRANSPORT);
		return interaction(pending.getGeneration(), edge, transport, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		NpcTransport transport, WorldPoint player, int interactionDistance)
	{
		WorldPoint tile = transport.getNpcTile();
		boolean ready = player != null && player.getPlane() == tile.getPlane()
			&& player.distanceTo2D(tile) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			tile, RouteInteraction.Kind.NPC_TRANSPORT,
			RouteInteraction.Status.AVAILABLE, transport.getAction(), ready,
			transport.getNpcId(), transport.getOrigin(), transport.getDestination());
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
