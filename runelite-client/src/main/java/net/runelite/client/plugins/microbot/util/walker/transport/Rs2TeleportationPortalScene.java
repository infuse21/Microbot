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
		PortalObject object = Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			PortalObject best = null;
			int bestDistance = Integer.MAX_VALUE;
			for (Rs2TileObjectModel candidate : Microbot.getRs2TileObjectCache().query()
				.withId(transport.getObjectId()).toList())
			{
				ObjectComposition composition = candidate.getObjectComposition();
				String[] rawActions = composition == null ? null : composition.getActions();
				List<String> actions = rawActions == null ? Collections.emptyList()
					: Arrays.stream(rawActions).filter(Objects::nonNull)
						.collect(Collectors.toList());
				net.runelite.api.coords.WorldPoint tile = candidate.getWorldLocation();
				String action = TeleportationPortalPolicy.exactAction(actions,
					transport.getAction());
				if (composition == null || tile == null || action == null
					|| !TeleportationPortalPolicy.isLiveObjectMatch(transport,
						candidate.getId(), composition.getName(), actions, tile))
				{
					continue;
				}
				int distance = tile.distanceTo2D(transport.getOrigin());
				if (distance < bestDistance)
				{
					best = new PortalObject(candidate, tile, action);
					bestDistance = distance;
				}
			}
			return best;
		}).orElse(null);
		return object == null ? null : new TeleportationPortal(object.object, object.tile,
			transport.getObjectId(), object.action, transport.getOrigin(),
			transport.getDestination());
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
