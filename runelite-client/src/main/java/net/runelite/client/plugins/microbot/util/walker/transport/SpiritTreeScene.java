package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SpiritTree;

/** Live resolver boundary for the staged spirit-tree interface. */
public interface SpiritTreeScene
{
	SpiritTree find(PlannedEdge edge);

	SpiritTree observe(PlannedEdge edge, String pendingAction);
}
