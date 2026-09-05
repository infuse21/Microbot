package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.HotAirBalloonTransport;

/** Live resolver boundary for the staged hot-air-balloon map. */
public interface HotAirBalloonScene
{
	HotAirBalloonTransport find(PlannedEdge edge);

	HotAirBalloonTransport observe(PlannedEdge edge, String pendingAction);
}
