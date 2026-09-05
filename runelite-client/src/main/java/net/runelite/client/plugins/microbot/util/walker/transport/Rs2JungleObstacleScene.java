package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.JungleObstacle;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed resolver and non-blocking dispatcher for Kharazi jungle crossings. */
public final class Rs2JungleObstacleScene implements JungleObstacleScene
{
	@Override
	public JungleObstacle find(PlannedEdge edge)
	{
		for (Transport transport : findTransports(edge))
		{
			Rs2TileObjectModel object = Microbot.getRs2TileObjectCache().query()
				.withId(transport.getObjectId()).toList().stream()
				.filter(candidate -> liveMatch(transport, candidate))
				.min(java.util.Comparator.comparingInt(candidate ->
					candidate.getWorldLocation().distanceTo2D(transport.getOrigin())))
				.orElse(null);
			if (object != null)
			{
				return new JungleObstacle(object, object.getWorldLocation(),
					transport.getObjectId(), transport.getAction(), transport.getOrigin(),
					transport.getDestination());
			}
		}
		return null;
	}

	public static boolean interactObject(PlannedEdge edge, String action, int catalogObjectId)
	{
		JungleObstacle obstacle = new Rs2JungleObstacleScene().find(edge);
		return obstacle != null && obstacle.getCatalogObjectId() == catalogObjectId
			&& obstacle.getAction().equalsIgnoreCase(action) && obstacle.getObject() != null
			&& obstacle.getObject().click(action);
	}

	private static List<Transport> findTransports(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return java.util.Collections.emptyList();
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(JungleObstaclePolicy::isEligible)
			.collect(Collectors.toList());
	}

	private static boolean liveMatch(Transport transport, Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition != null && JungleObstaclePolicy.isLiveObjectMatch(transport,
			object.getId(), composition.getName(), actions(composition),
			object.getWorldLocation());
	}

	private static List<String> actions(ObjectComposition composition)
	{
		String[] actions = composition.getActions();
		return actions == null ? java.util.Collections.emptyList() : Arrays.stream(actions)
			.filter(Objects::nonNull).collect(Collectors.toList());
	}
}
