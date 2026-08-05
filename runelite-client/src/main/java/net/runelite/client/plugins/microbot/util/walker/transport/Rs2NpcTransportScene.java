package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcTransport;

import java.util.Comparator;

/** Resolves exact eligible NPC or tile-object travel rows against the shared caches. */
public final class Rs2NpcTransportScene implements NpcTransportScene
{
	@Override
	public NpcTransport find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(NpcTransportPolicy::isEligible)
			.map(Rs2NpcTransportScene::resolve).filter(java.util.Objects::nonNull)
			.findFirst().orElse(null);
	}

	private static NpcTransport resolve(Transport transport)
	{
		Rs2NpcModel npc = findNpc(transport);
		if (npc != null)
		{
			return model(transport, npc.getWorldLocation());
		}
		Rs2TileObjectModel object = findTravelObject(transport);
		return object == null ? null : model(transport, object.getWorldLocation());
	}

	public static Rs2NpcModel findNpc(Transport transport)
	{
		Rs2NpcModel npc = Rs2Npc.getNpc(transport.getObjectId());
		if (npc == null || npc.getName() == null
			|| !npc.getName().equalsIgnoreCase(transport.getName()))
		{
			return null;
		}
		return npc;
	}

	/**
	 * Rows such as rowboats and ferries publish a tile-object id in the NPC column.
	 * The model always carries the catalog id even when the live object is transformed.
	 */
	public static Rs2TileObjectModel findTravelObject(Transport transport)
	{
		return Microbot.getRs2TileObjectCache().query()
			.within(transport.getOrigin(), NpcTransportPolicy.LIVE_OBJECT_ORIGIN_TOLERANCE)
			.toList().stream()
			.filter(candidate -> NpcTransportPolicy.isLiveObjectMatch(transport,
				candidate.getId(), compositionName(candidate),
				compositionActions(candidate), candidate.getWorldLocation()))
			.min(Comparator.comparingInt((Rs2TileObjectModel candidate) ->
				(candidate.getId() == transport.getObjectId() ? 0 : 100)
					+ candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
			.orElse(null);
	}

	public static String resolveLiveAction(Rs2TileObjectModel object, String catalogAction)
	{
		return object == null ? null
			: NpcTransportPolicy.matchAction(compositionActions(object), catalogAction);
	}

	private static String compositionName(Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition == null ? null : composition.getName();
	}

	private static String[] compositionActions(Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition == null ? null : composition.getActions();
	}

	private static NpcTransport model(Transport transport, WorldPoint tile)
	{
		return new NpcTransport(transport.getOrigin(), transport.getDestination(),
			transport.getType(), transport.getObjectId(), transport.getName(),
			transport.getAction(), tile);
	}
}
