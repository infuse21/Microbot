package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

public final class ItemTeleportRouteScanner
{
	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges, ItemTeleportScene scene)
	{
		if (plan == null || scene == null)
		{
			return null;
		}
		int start = Math.max(0, startRawIndex);
		int end = Math.min(plan.getRouteEdges().size(), start + Math.max(0, maxEdges));
		for (int i = start; i < end; i++)
		{
			RouteEdge edge = plan.getRouteEdges().get(i);
			if (edge.getKind() != RouteEdge.Kind.ITEM_TELEPORT)
			{
				continue;
			}
			ItemTeleport item = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (item != null)
			{
				return interaction(plan.getGeneration(), edge, item);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player, ItemTeleportScene scene)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (player != null && player.getPlane() == pending.getCrossingTo().getPlane()
			&& player.distanceTo2D(pending.getCrossingTo()) <= 3)
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		ItemTeleport item = scene.observe(new PlannedEdge(pending.getFrom(), pending.getTo()),
			pending.getAction());
		if (item == null)
		{
			if (isTerminalAction(pending.getAction()))
			{
				// A consumed charge can remove the enabled row before the remote landing.
				return pending.withStatus(RouteInteraction.Status.AVAILABLE, true);
			}
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), new RouteEdge(pending.getRawEdgeIndex(),
			pending.getFrom(), pending.getTo(), RouteEdge.Kind.ITEM_TELEPORT), item);
	}

	private static boolean isTerminalAction(String action)
	{
		return action.startsWith("item-use:") || action.startsWith("book-select:")
			|| action.startsWith("book-confirm:");
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge, ItemTeleport item)
	{
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			edge.getFrom(), RouteInteraction.Kind.ITEM_TELEPORT, RouteInteraction.Status.AVAILABLE,
			item.command(), true, item.getItemId(), edge.getFrom(), edge.getTo());
	}
}
