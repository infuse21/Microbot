package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal;

/** Live resolver boundary for deterministic direct-action teleportation portals. */
public interface TeleportationPortalScene
{
	TeleportationPortal find(PlannedEdge edge);

	TeleportationPortal observe(PlannedEdge edge, int catalogObjectId);
}
