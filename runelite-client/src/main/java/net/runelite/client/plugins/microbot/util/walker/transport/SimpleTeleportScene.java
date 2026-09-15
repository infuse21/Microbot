package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;

/** Resolves an enabled direct teleport for one planned route edge. */
public interface SimpleTeleportScene
{
	SimpleTeleport find(PlannedEdge edge);

	default SimpleTeleport observe(PlannedEdge edge, String pending)
	{
		return find(edge);
	}

	default boolean hasLanded(PlannedEdge edge, WorldPoint player)
	{
		return player != null && player.getPlane() == edge.to().getPlane()
			&& player.distanceTo2D(edge.to()) <= 3;
	}
}
