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
		List<Transport> transports = findTransports(edge);
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			for (Transport transport : transports)
			{
				Rs2TileObjectModel best = null;
				net.runelite.api.coords.WorldPoint bestTile = null;
				int bestDistance = Integer.MAX_VALUE;
				for (Rs2TileObjectModel candidate : Microbot.getRs2TileObjectCache().query()
					.withId(transport.getObjectId()).toList())
				{
					ObjectComposition composition = candidate.getObjectComposition();
					net.runelite.api.coords.WorldPoint tile = candidate.getWorldLocation();
					String[] rawActions = composition == null ? null : composition.getActions();
					List<String> liveActions = rawActions == null
						? java.util.Collections.emptyList() : Arrays.stream(rawActions)
							.filter(Objects::nonNull).collect(Collectors.toList());
					if (composition == null || tile == null
						|| !JungleObstaclePolicy.isLiveObjectMatch(transport,
							candidate.getId(), composition.getName(), liveActions, tile))
					{
						continue;
					}
					int distance = tile.distanceTo2D(transport.getOrigin());
					if (distance < bestDistance)
					{
						best = candidate;
						bestTile = tile;
						bestDistance = distance;
					}
				}
				if (best != null)
				{
					return new JungleObstacle(best, bestTile, transport.getObjectId(),
						transport.getAction(), transport.getOrigin(), transport.getDestination());
				}
			}
			return null;
		}).orElse(null);
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

}
