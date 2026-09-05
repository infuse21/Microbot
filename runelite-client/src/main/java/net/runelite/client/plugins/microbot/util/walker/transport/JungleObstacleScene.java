package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.JungleObstacle;

/** Live resolver boundary for one deterministic Kharazi jungle crossing. */
@FunctionalInterface
public interface JungleObstacleScene
{
	JungleObstacle find(PlannedEdge edge);
}
