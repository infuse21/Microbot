package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.Quetzal;

/** Live resolver boundary for the staged quetzal interface. */
public interface QuetzalScene
{
	Quetzal find(PlannedEdge edge);

	Quetzal observe(PlannedEdge edge, String pendingAction);
}
