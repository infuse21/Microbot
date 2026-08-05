package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;

/** Resolves an enabled direct teleport for one planned route edge. */
public interface SimpleTeleportScene
{
	SimpleTeleport find(PlannedEdge edge);
}
