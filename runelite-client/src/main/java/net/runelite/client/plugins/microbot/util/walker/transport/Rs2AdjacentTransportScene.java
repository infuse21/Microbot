package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;

import java.util.Comparator;

/** Live adapter for object-backed adjacent same-plane catalog transports. */
public final class Rs2AdjacentTransportScene implements AdjacentTransportScene
{
	@Override
	public AdjacentTransport find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() -> findOnClientThread(edge))
			.orElse(null);
	}

	private static AdjacentTransport findOnClientThread(PlannedEdge edge)
	{
		for (Transport transport : TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
			edge.from(), edge.to()))
		{
			if (!AdjacentTransportPolicy.isEligible(transport))
			{
				continue;
			}
			TileObject object = Rs2GameObject.getAll(candidate -> true, transport.getOrigin(), 2).stream()
				.filter(candidate -> candidate.getWorldLocation() != null
					&& candidate.getWorldLocation().getPlane() == transport.getOrigin().getPlane()
					&& candidate.getWorldLocation().distanceTo2D(transport.getOrigin()) <= 1)
				.filter(candidate -> candidate.getId() == transport.getObjectId()
					|| matchesCatalogIdentity(candidate, transport))
				.min(Comparator.comparingInt(candidate ->
					(candidate.getId() == transport.getObjectId() ? 0 : 100)
						+ candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
				.orElse(null);
			if (object != null)
			{
				return new AdjacentTransport(object, object.getWorldLocation(), transport.getObjectId(),
					transport.getAction(), transport.getOrigin(), transport.getDestination());
			}
		}
		return null;
	}

	private static boolean matchesCatalogIdentity(TileObject object, Transport transport)
	{
		return matchesCatalogIdentity(Rs2GameObject.convertToObjectComposition(object), transport);
	}

	static boolean matchesCatalogIdentity(ObjectComposition composition, Transport transport)
	{
		if (composition == null || !sameText(composition.getName(), transport.getName()))
		{
			return false;
		}
		String[] actions = composition.getActions();
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if (sameText(action, transport.getAction()))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean sameText(String left, String right)
	{
		return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
	}
}
