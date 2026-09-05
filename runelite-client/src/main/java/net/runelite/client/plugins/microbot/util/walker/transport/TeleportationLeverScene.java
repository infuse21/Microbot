package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationLever;

/** Live resolver boundary for object, warning, and confirmation lever stages. */
public interface TeleportationLeverScene
{
	TeleportationLever find(PlannedEdge edge);

	TeleportationLever observe(PlannedEdge edge, String pendingAction, int catalogObjectId);
}
