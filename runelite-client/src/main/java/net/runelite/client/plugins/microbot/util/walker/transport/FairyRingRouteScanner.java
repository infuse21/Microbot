package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.FairyRing;

import java.util.List;

/** Route-order scanner and staged landing observer for fairy-ring travel. */
public final class FairyRingRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, FairyRingScene scene, int interactionDistance)
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
			if (edge.getKind() != RouteEdge.Kind.FAIRY_RING)
			{
				continue;
			}
			FairyRing ring = scene.find(new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (ring != null)
			{
				return interaction(plan.getGeneration(), edge, ring, player,
					interactionDistance);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		FairyRingScene scene, int interactionDistance)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		PlannedEdge planned = new PlannedEdge(pending.getFrom(), pending.getTo());
		if (hasLanded(pending, player))
		{
			FairyRing restore = scene.restore(planned, pending.getAction(),
				pending.getObjectId(), player);
			if (restore == null)
			{
				return pending.withStatus(RouteInteraction.Status.CLEARED, false);
			}
			if (pending.getAction().equalsIgnoreCase(restore.getAction()))
			{
				// Dispatch is not acknowledgement. Hold until the inventory becomes
				// observable or the equipment cache proves the weapon is worn.
				return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
			}
			return interaction(pending.getGeneration(), edge(pending), restore, player,
				interactionDistance);
		}
		FairyRing ring = scene.observe(planned, pending.getAction(), pending.getObjectId());
		if (ring == null)
		{
			// A direct last-destination or final teleport click unloads the interface
			// while the animation runs. Preserve the exact landing predicate until
			// the engine's bounded command deadline decides the attempt.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		return interaction(pending.getGeneration(), edge(pending), ring, player,
			interactionDistance);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		FairyRing ring, WorldPoint player, int interactionDistance)
	{
		boolean ready = ring.getStage() != FairyRing.Stage.OBJECT
			|| player != null && player.getPlane() == ring.getObjectTile().getPlane()
				&& player.distanceTo2D(ring.getObjectTile()) <= interactionDistance;
		return new RouteInteraction(generation, edge.getRawIndex(), edge.getFrom(), edge.getTo(),
			ring.getObjectTile(), RouteInteraction.Kind.FAIRY_RING,
			RouteInteraction.Status.AVAILABLE, ring.getAction(), ready,
			ring.getOriginalWeaponId(), ring.getOrigin(), ring.getDestination());
	}

	private static RouteEdge edge(RouteInteraction pending)
	{
		return new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(), pending.getTo(),
			RouteEdge.Kind.FAIRY_RING);
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
