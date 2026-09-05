package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MagicMushtreeTransport;

/** Live resolver boundary for the staged Magic Mushtree interface. */
public interface MagicMushtreeScene
{
	MagicMushtreeTransport find(PlannedEdge edge);

	MagicMushtreeTransport observe(PlannedEdge edge, String pendingAction);
}
