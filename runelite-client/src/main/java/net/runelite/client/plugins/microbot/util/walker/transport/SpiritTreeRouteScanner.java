package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SpiritTree;

import java.util.List;

/** Route-order scanner and staged landing observer for spirit-tree travel. */
public final class SpiritTreeRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, SpiritTreeScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.SPIRIT_TREE)
			{
				continue;
			}
			SpiritTree tree = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (tree != null)
			{
				return interaction(plan.getGeneration(), edge, tree, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		SpiritTreeScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		SpiritTree tree = scene.observe(new PlannedEdge(pending.getFrom(), pending.getTo()),
			pending.getAction());
		if (tree == null)
		{
			// Selecting a destination unloads both source object and interface while
			// travel is in flight. Preserve the directed landing predicate.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (tree.getObjectId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), tree, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		SpiritTree tree, WorldPoint player, int interactionDistance)
	{
		boolean originUnavailable = tree.getStage() == SpiritTree.Stage.ORIGIN_UNAVAILABLE;
		boolean destinationUnavailable =
			tree.getStage() == SpiritTree.Stage.DESTINATION_UNAVAILABLE;
		boolean unavailable = originUnavailable || destinationUnavailable;
		boolean destination = tree.getStage() == SpiritTree.Stage.DESTINATION
			|| destinationUnavailable;
		String action = destination
			? SpiritTreePolicy.destinationAction(tree.getDestinationName())
			: tree.getObjectAction();
		boolean ready = !unavailable && (destination || player != null
			&& player.getPlane() == tree.getObjectTile().getPlane()
			&& player.distanceTo2D(tree.getObjectTile()) <= interactionDistance);
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			tree.getObjectTile(), RouteInteraction.Kind.SPIRIT_TREE,
			unavailable ? RouteInteraction.Status.UNAVAILABLE : RouteInteraction.Status.AVAILABLE,
			action, ready, tree.getObjectId(),
			tree.getOrigin(), tree.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.SPIRIT_TREE);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
