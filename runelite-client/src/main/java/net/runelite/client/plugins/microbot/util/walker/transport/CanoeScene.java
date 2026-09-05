package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CanoeTransport;

/** Live scene/UI boundary used by the canoe route scanner. */
public interface CanoeScene
{
	CanoeTransport find(PlannedEdge edge);

	CanoeTransport observe(PlannedEdge edge, String pendingAction, int catalogObjectId);
}
