package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;

import java.util.List;

/** Route-order scanner and landing observer for direct item/spell teleports. */
public final class SimpleTeleportRouteScanner
{
	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, SimpleTeleportScene scene)
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
			if (edge.getKind() != RouteEdge.Kind.SIMPLE_TELEPORT)
			{
				continue;
			}
			SimpleTeleport teleport = scene.observe(new PlannedEdge(edge.getFrom(), edge.getTo()), null);
			if (teleport != null)
			{
				return interaction(plan.getGeneration(), edge, teleport);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		SimpleTeleportScene scene)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (scene.hasLanded(new PlannedEdge(pending.getCrossingFrom(), pending.getCrossingTo()), player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		if (pending.getObjectId() == net.runelite.client.plugins.microbot.shortestpath.TransportType.TELEPORTATION_SPELL.ordinal()
			&& !Rs2SpellTeleportScene.isPreparation(pending.getAction()))
		{
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, true);
		}
		SimpleTeleport teleport = scene.observe(new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (teleport == null)
		{
			// Consumables and runes can disappear from the enabled catalog immediately
			// after an accepted command, before the player lands. Preserve the pending
			// interaction so the engine's acknowledgement deadline, rather than a
			// transient inventory refresh, decides whether the command failed.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, true);
		}
		if (!teleport.getDisplayInfo().equalsIgnoreCase(Rs2SpellTeleportScene.display(pending.getAction())))
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.SIMPLE_TELEPORT);
		return interaction(pending.getGeneration(), edge, teleport);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		SimpleTeleport teleport)
	{
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			teleport.getRouteOrigin(), RouteInteraction.Kind.SIMPLE_TELEPORT,
			teleport.isAvailable() ? RouteInteraction.Status.AVAILABLE : RouteInteraction.Status.UNAVAILABLE,
			teleport.getCommand(), teleport.isAvailable(),
			teleport.getType().ordinal(), teleport.getRouteOrigin(), teleport.getDestination());
	}
}
