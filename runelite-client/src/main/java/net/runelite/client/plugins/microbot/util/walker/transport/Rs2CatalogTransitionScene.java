package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy.TransportRequirementPolicy;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Cache-backed live resolver for direct catalog scene transitions. */
public final class Rs2CatalogTransitionScene implements CatalogTransitionScene
{
	public enum DispatchResult
	{
		REJECTED,
		PREPARED,
		ISSUED
	}

	@Override
	public CatalogTransition find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		for (Transport transport : TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
			edge.from(), edge.to()))
		{
			if (!CatalogTransitionPolicy.isEligible(transport))
			{
				continue;
			}
			CatalogTransition transition = find(transport);
			if (transition != null)
			{
				return transition;
			}
		}
		return null;
	}

	private static CatalogTransition find(Transport transport)
	{
		boolean pohPortal = CatalogTransitionPolicy.isPohPortal(transport);
		List<Rs2TileObjectModel> candidates = pohPortal
			? Microbot.getRs2TileObjectCache().query().withId(transport.getObjectId()).toList()
			: Microbot.getRs2TileObjectCache().query().within(transport.getOrigin(), 2).toList();
		Rs2TileObjectModel direct = candidates.stream()
			.filter(candidate -> candidate.getWorldLocation().getPlane()
				== transport.getOrigin().getPlane())
			.filter(candidate -> candidate.getId() == transport.getObjectId()
				|| matchesCatalogIdentity(candidate, transport))
			.min(Comparator.comparingInt(candidate ->
				(candidate.getId() == transport.getObjectId() ? 0 : 100)
					+ candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
			.orElse(null);
		if (direct != null)
		{
			String action = resolveLiveAction(direct, transport);
			if (action != null)
			{
				return transition(direct, transport, action, pohPortal);
			}
		}
		if (!CatalogTransitionPolicy.supportsClosedVariant(transport.getAction()))
		{
			return null;
		}
		Rs2TileObjectModel closed = candidates.stream()
			.filter(candidate -> isClosedEntrance(candidate, transport.getOrigin()))
			.min(Comparator.comparingInt(candidate ->
				candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
			.orElse(null);
		return closed == null ? null : transition(closed, transport, "Open", false);
	}

	private static CatalogTransition transition(Rs2TileObjectModel object, Transport transport,
		String action, boolean logicalOrigin)
	{
		WorldPoint objectTile = logicalOrigin ? transport.getOrigin() : object.getWorldLocation();
		return new CatalogTransition(object, objectTile, transport.getObjectId(),
			action, transport.getAction(), transport.getOrigin(), transport.getDestination());
	}

	private static boolean matchesCatalogIdentity(Rs2TileObjectModel object, Transport transport)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition != null && sameText(composition.getName(), transport.getName())
			&& resolveLiveAction(composition.getActions(), transport) != null;
	}

	/** Dispatches one non-blocking preparation or object command for an exact route edge. */
	public static DispatchResult dispatch(PlannedEdge edge, String action, int catalogObjectId)
	{
		Transport transport = findTransport(edge, catalogObjectId);
		if (transport == null)
		{
			return DispatchResult.REJECTED;
		}
		if (EnergyBarrierPolicy.isEligible(transport)
			&& !TransportRequirementPolicy.itemIdRequirements(transport).isEmpty()
			&& !Rs2Equipment.isWearing(TransportRequirementPolicy.ghostspeakItemIds().stream()
				.mapToInt(Integer::intValue).toArray()))
		{
			return Rs2Inventory.interact(TransportRequirementPolicy.ghostspeakItemIds().stream()
				.mapToInt(Integer::intValue).toArray(), "Wear")
				? DispatchResult.PREPARED : DispatchResult.REJECTED;
		}
		CatalogTransition transition = find(transport);
		boolean issued = transition != null && transition.getCatalogObjectId() == catalogObjectId
			&& transition.getAction().equalsIgnoreCase(action) && transition.getObject() != null
			&& transition.getObject().click(action);
		return issued ? DispatchResult.ISSUED : DispatchResult.REJECTED;
	}

	private static Transport findTransport(PlannedEdge edge, int catalogObjectId)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to()).stream()
			.filter(CatalogTransitionPolicy::isEligible)
			.filter(candidate -> candidate.getObjectId() == catalogObjectId)
			.findFirst().orElse(null);
	}

	private static String resolveLiveAction(Rs2TileObjectModel object, Transport transport)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition == null ? null : resolveLiveAction(composition.getActions(), transport);
	}

	static String resolveLiveAction(String[] actions, Transport transport)
	{
		return resolveLiveAction(actions, transport,
			TransportRequirementPolicy.currencyAmount(transport) == 0);
	}

	static String resolveLiveAction(String[] actions, Transport transport,
		boolean freeEnergyBarrier)
	{
		String direct = resolveAction(actions, transport.getAction());
		if (direct != null || !EnergyBarrierPolicy.isEligible(transport)
			|| !freeEnergyBarrier)
		{
			return direct;
		}
		return resolveAction(actions, "Pass");
	}

	private static boolean isClosedEntrance(Rs2TileObjectModel object, WorldPoint origin)
	{
		if (object.getWorldLocation().getPlane() != origin.getPlane()
			|| object.getWorldLocation().distanceTo2D(origin) > 1)
		{
			return false;
		}
		ObjectComposition composition = object.getObjectComposition();
		if (composition == null || resolveAction(composition.getActions(), "Open") == null)
		{
			return false;
		}
		String name = normalize(composition.getName());
		return name.contains("trapdoor") || name.contains("manhole")
			|| name.contains("grate") || name.contains("hatch");
	}

	static String resolveAction(String[] actions, String catalogAction)
	{
		if (actions == null || catalogAction == null)
		{
			return null;
		}
		String expected = normalizeAction(catalogAction);
		for (String action : actions)
		{
			if (action != null && normalizeAction(action).equals(expected))
			{
				return action;
			}
		}
		return null;
	}

	private static boolean sameText(String left, String right)
	{
		return left != null && right != null && normalize(left).equals(normalize(right));
	}

	private static String normalizeAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
