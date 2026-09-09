package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;

import java.util.Comparator;
import java.util.Objects;

/** Live adapter for short object-backed same-plane catalog transports. */
public final class Rs2AdjacentTransportScene implements AdjacentTransportScene
{
	@Override
	public AdjacentTransport find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		try
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() -> findOnClientThread(edge))
				.orElse(null);
		}
		catch (RuntimeException ex)
		{
			if (Thread.currentThread().isInterrupted()
				|| Rs2SceneLocation.clientThreadUnavailable(ex))
			{
				return null;
			}
			throw ex;
		}
	}

	@Override
	public boolean isEnabled(PlannedEdge edge, int catalogObjectId)
	{
		if ((catalogObjectId < 137 || catalogObjectId > 145)
			&& catalogObjectId != 4918 && catalogObjectId != 11728 && catalogObjectId != 11720
			&& catalogObjectId != 11719) return true;
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to()).stream()
				.anyMatch(transport -> transport.getObjectId() == catalogObjectId
					&& requirementsMet(transport)))
			.orElse(false);
	}

	private static boolean requirementsMet(Transport transport)
	{
		if (AdjacentTransportPolicy.isYanillePickLockDoor(transport))
		{
			return AdjacentTransportPolicy.hasRequiredYanillePickLockItemsAndLevel(transport,
				Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.THIEVING),
				net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.contains(1523));
		}
		if (AdjacentTransportPolicy.isEastArdougnePickLockDoor(transport))
		{
			return AdjacentTransportPolicy.hasRequiredEastArdougneThieving(transport,
				Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.THIEVING));
		}
		if (AdjacentTransportPolicy.isHauntedMineCart(transport))
		{
			return AdjacentTransportPolicy.hasRequiredHauntedMineCartAgility(transport,
				Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.AGILITY));
		}
		return AdjacentTransportPolicy.hasRequiredDraynorLevers(transport, Microbot::getVarbitValue);
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
			if ((AdjacentTransportPolicy.isDraynorBasementDoor(transport)
				|| AdjacentTransportPolicy.isYanillePickLockDoor(transport)
				|| AdjacentTransportPolicy.isEastArdougnePickLockDoor(transport)
				|| AdjacentTransportPolicy.isHauntedMineCart(transport))
				&& !requirementsMet(transport))
			{
				continue;
			}
			LiveObject object = Rs2GameObject.getAll(candidate -> true, transport.getOrigin(), 2).stream()
				.map(Rs2AdjacentTransportScene::snapshot)
				.filter(Objects::nonNull)
				.filter(candidate -> !(AdjacentTransportPolicy.isDraynorBasementDoor(transport)
					|| AdjacentTransportPolicy.isDraynorBookcase(transport)
					|| AdjacentTransportPolicy.isYanillePickLockDoor(transport)
					|| AdjacentTransportPolicy.isEastArdougnePickLockDoor(transport)
					|| AdjacentTransportPolicy.isHauntedMineCart(transport))
					|| candidate.id == transport.getObjectId())
				.filter(candidate -> candidate.location != null
					&& candidate.location.getPlane()
						== transport.getOrigin().getPlane()
					&& candidate.location.distanceTo2D(transport.getOrigin()) <= 1)
				.filter(candidate -> candidate.id == transport.getObjectId()
					|| matchesCatalogIdentity(candidate, transport))
				.min(Comparator.comparingInt(candidate ->
					(candidate.id == transport.getObjectId() ? 0 : 100)
						+ candidate.location.distanceTo2D(transport.getOrigin())))
				.orElse(null);
			if (object != null)
			{
				return new AdjacentTransport(object.object, object.location,
					transport.getObjectId(),
					transport.getAction(), transport.getOrigin(), transport.getDestination());
			}
		}
		return null;
	}

	private static boolean matchesCatalogIdentity(TileObject object, Transport transport)
	{
		LiveObject snapshot = snapshot(object);
		return snapshot != null && matchesCatalogIdentity(snapshot, transport);
	}

	static boolean matchesCatalogIdentity(ObjectComposition composition, Transport transport)
	{
		if (composition == null)
		{
			return false;
		}
		LiveObject snapshot = Microbot.getClientThread().runOnClientThreadOptional(() ->
			new LiveObject(null, -1, null, composition.getName(), composition.getActions()))
			.orElse(null);
		return snapshot != null && matchesCatalogIdentity(snapshot, transport);
	}

	static boolean matchesCatalogIdentity(String name, String[] actions, Transport transport)
	{
		return matchesCatalogIdentity(new LiveObject(null, -1, null, name, actions), transport);
	}

	private static boolean matchesCatalogIdentity(LiveObject object, Transport transport)
	{
		if (!sameText(object.name, transport.getName()))
		{
			return false;
		}
		if (object.actions == null)
		{
			return false;
		}
		for (String action : object.actions)
		{
			if (sameText(action, transport.getAction()))
			{
				return true;
			}
		}
		return false;
	}

	private static LiveObject snapshot(TileObject object)
	{
		if (object == null)
		{
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			ObjectComposition composition = Rs2GameObject.convertToObjectComposition(object);
			return new LiveObject(object, object.getId(),
				Rs2SceneLocation.templateLocation(object),
				composition == null ? null : composition.getName(),
				composition == null ? null : composition.getActions());
		}).orElse(null);
	}

	private static final class LiveObject
	{
		private final TileObject object;
		private final int id;
		private final net.runelite.api.coords.WorldPoint location;
		private final String name;
		private final String[] actions;

		private LiveObject(TileObject object, int id,
			net.runelite.api.coords.WorldPoint location, String name, String[] actions)
		{
			this.object = object;
			this.id = id;
			this.location = location;
			this.name = name;
			this.actions = actions;
		}
	}

	private static boolean sameText(String left, String right)
	{
		return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
	}
}
