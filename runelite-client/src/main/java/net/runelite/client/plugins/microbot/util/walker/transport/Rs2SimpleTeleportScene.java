package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;

/** Resolves direct teleports from the same enabled catalog snapshot used by pathfinding. */
public final class Rs2SimpleTeleportScene implements SimpleTeleportScene
{
	@Override
	public SimpleTeleport find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(SimpleTeleportPolicy::isEligible)
			.map(transport -> from(edge, transport)).findFirst().orElse(null);
	}

	private static SimpleTeleport from(PlannedEdge edge, Transport transport)
	{
		return new SimpleTeleport(edge.from(), transport.getDestination(),
			transport.getType(), transport.getDisplayInfo());
	}
}
