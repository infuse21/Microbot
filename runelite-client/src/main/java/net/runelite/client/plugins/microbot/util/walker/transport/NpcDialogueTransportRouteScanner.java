package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcDialogueTransport;

import java.util.List;

/** Route-order scanner and stage observer for dialogue-menu NPC/ship/boat travel. */
public final class NpcDialogueTransportRouteScanner
{
	private static final int LANDING_TOLERANCE = 3;

	public RouteInteraction scan(RoutePlan plan, int startRawIndex, int maxEdges,
		WorldPoint player, NpcDialogueTransportScene scene, int interactionDistance,
		long coinsHeld)
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
			if (edge.getKind() != RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT)
			{
				continue;
			}
			NpcDialogueTransport transport = scene.find(
				new PlannedEdge(edge.getFrom(), edge.getTo()));
			if (transport != null)
			{
				return interaction(plan.getGeneration(), edge, transport, player,
					interactionDistance, coinsHeld);
			}
		}
		return null;
	}

	public RouteInteraction observePending(RouteInteraction pending, WorldPoint player,
		NpcDialogueTransportScene scene, int interactionDistance, long coinsHeld)
	{
		if (pending == null || scene == null)
		{
			return null;
		}
		if (hasLanded(pending, player))
		{
			if (scene.hasContinue())
			{
				// Payment can remove this row from the active catalogue before landing.
				// Finish the dialogue from the already-owned edge instead of requiring
				// the now-unaffordable transport to resolve again.
				return new RouteInteraction(pending.getGeneration(), pending.getRawEdgeIndex(),
					pending.getFrom(), pending.getTo(), pending.getObjectTile(), pending.getKind(),
					RouteInteraction.Status.AVAILABLE, NpcDialogueTransportPolicy.CONTINUE_ACTION,
					true, pending.getObjectId(), pending.getCrossingFrom(), pending.getCrossingTo());
			}
			return pending.withStatus(RouteInteraction.Status.CLEARED, false);
		}
		NpcDialogueTransport transport = scene.observe(
			new PlannedEdge(pending.getFrom(), pending.getTo()), pending.getAction());
		if (transport == null)
		{
			// The dialogue and source actor disappear while the voyage runs. Preserve the
			// exact landing predicate until the engine's bounded deadline decides.
			return pending.withStatus(RouteInteraction.Status.AVAILABLE, false);
		}
		if (transport.getActorId() != pending.getObjectId())
		{
			return pending.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		RouteEdge edge = new RouteEdge(pending.getRawEdgeIndex(), pending.getFrom(),
			pending.getTo(), RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT);
		return interaction(pending.getGeneration(), edge, transport, player,
			interactionDistance, coinsHeld);
	}

	private static RouteInteraction interaction(long generation, RouteEdge edge,
		NpcDialogueTransport transport, WorldPoint player, int interactionDistance,
		long coinsHeld)
	{
		String action;
		boolean ready;
		switch (transport.getStage())
		{
			case EQUIP_REQUIREMENT:
				action = "Dondakan the Dwarf".equals(transport.getActorName())
					? NpcDialogueTransportPolicy.EQUIP_GOLD_HELMET_ACTION
					: NpcDialogueTransportPolicy.EQUIP_GHOSTSPEAK_ACTION;
				ready = true;
				break;
			case TRAVEL_REQUEST:
				action = NpcDialogueTransportPolicy.TRAVEL_REQUEST_ACTION;
				ready = true;
				break;
			case DESTINATION_CANCEL:
				action = NpcDialogueTransportPolicy.CANCEL_UNAVAILABLE_ACTION;
				ready = true;
				break;
			case DESTINATION_UNAVAILABLE:
				action = NpcDialogueTransportPolicy.DESTINATION_UNAVAILABLE_ACTION;
				ready = false;
				break;
			case DESTINATION:
				action = NpcDialogueTransportPolicy.destinationAction(
					transport.getDestinationOption());
				ready = true;
				break;
			case CONFIRM:
				action = NpcDialogueTransportPolicy.CONFIRM_ACTION;
				ready = true;
				break;
			case CONTINUE:
				action = NpcDialogueTransportPolicy.CONTINUE_ACTION;
				ready = true;
				break;
			case ACTOR:
			default:
				action = transport.getActorAction();
				// The actor wanders around (and can stand above) the route anchor, so the
				// click is ready from either the actor's vicinity or the route frontier.
				ready = player != null
					&& (player.distanceTo2D(transport.getActorTile()) <= interactionDistance
						|| player.distanceTo2D(edge.getFrom()) <= interactionDistance);
				break;
		}
		RouteInteraction interaction = new RouteInteraction(generation, edge.getRawIndex(),
			edge.getFrom(), edge.getTo(), transport.getActorTile(),
			RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT,
			RouteInteraction.Status.AVAILABLE, action, ready, transport.getActorId(),
			transport.getOrigin(), transport.getDestination());
		// Missing fare is bounded unavailability evidence, like a missing pickaxe. Once a
		// dialogue stage is open the payment is already in the game's hands.
		if (transport.getStage() == NpcDialogueTransport.Stage.ACTOR
			&& transport.getFareCoins() > coinsHeld)
		{
			return interaction.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		if (transport.getStage() == NpcDialogueTransport.Stage.DESTINATION_UNAVAILABLE)
		{
			return interaction.withStatus(RouteInteraction.Status.UNAVAILABLE, false);
		}
		return interaction;
	}

	private static boolean hasLanded(RouteInteraction pending, WorldPoint player)
	{
		WorldPoint destination = pending.getCrossingTo();
		return player != null && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= LANDING_TOLERANCE;
	}
}
