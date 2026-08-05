package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CharterShip;

/** Live resolver boundary for the staged charter-ship interface. */
public interface CharterShipScene
{
	CharterShip find(PlannedEdge edge);

	CharterShip observe(PlannedEdge edge, String pendingAction);
}
