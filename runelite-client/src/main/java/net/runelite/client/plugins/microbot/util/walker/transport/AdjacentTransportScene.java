package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;

public interface AdjacentTransportScene
{
	AdjacentTransport find(PlannedEdge edge);
}
