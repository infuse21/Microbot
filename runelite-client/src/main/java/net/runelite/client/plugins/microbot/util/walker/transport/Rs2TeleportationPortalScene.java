package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed, non-blocking adapter for deterministic teleportation portals. */
public final class Rs2TeleportationPortalScene implements TeleportationPortalScene
{
	@Override
	public TeleportationPortal find(PlannedEdge edge)
	{
		for (Transport transport : findTransports(edge))
		{
			TeleportationPortal portal = portal(transport);
			if (portal != null)
			{
				return portal;
			}
		}
		return null;
	}

	@Override
	public TeleportationPortal observe(PlannedEdge edge, int catalogObjectId)
	{
		for (Transport transport : findTransports(edge))
		{
			if (transport.getObjectId() == catalogObjectId)
			{
				return portal(transport);
			}
		}
		return null;
	}

	public static boolean interactObject(PlannedEdge edge, String expectedAction,
		int catalogObjectId)
	{
		TeleportationPortal portal = new Rs2TeleportationPortalScene().find(edge);
		return portal != null && portal.getCatalogObjectId() == catalogObjectId
			&& portal.getAction().equalsIgnoreCase(expectedAction)
			&& portal.getObject() != null && portal.getObject().click(portal.getAction());
	}

	private static List<Transport> findTransports(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return Collections.emptyList();
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(TeleportationPortalPolicy::isEligible)
			.collect(Collectors.toList());
	}

	private static TeleportationPortal portal(Transport transport)
	{
		PortalObject object = Microbot.getRs2TileObjectCache().query()
			.withId(transport.getObjectId()).toList().stream()
			.map(candidate -> liveObject(transport, candidate))
			.filter(Objects::nonNull)
			.min(Comparator.comparingInt(candidate -> candidate.tile.distanceTo2D(
				transport.getOrigin())))
			.orElse(null);
		return object == null ? null : new TeleportationPortal(object.object, object.tile,
			transport.getObjectId(), object.action, transport.getOrigin(),
			transport.getDestination());
	}

	private static PortalObject liveObject(Transport transport, Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		List<String> actions = composition == null || composition.getActions() == null
			? Collections.emptyList() : Arrays.stream(composition.getActions())
				.filter(Objects::nonNull).collect(Collectors.toList());
		String action = TeleportationPortalPolicy.exactAction(actions,
			transport.getAction());
		return composition != null && action != null
			&& TeleportationPortalPolicy.isLiveObjectMatch(transport, object.getId(),
				composition.getName(), actions, object.getWorldLocation())
			? new PortalObject(object, object.getWorldLocation(), action) : null;
	}

	private static final class PortalObject
	{
		private final Rs2TileObjectModel object;
		private final net.runelite.api.coords.WorldPoint tile;
		private final String action;

		private PortalObject(Rs2TileObjectModel object,
			net.runelite.api.coords.WorldPoint tile, String action)
		{
			this.object = object;
			this.tile = tile;
			this.action = action;
		}
	}
}
