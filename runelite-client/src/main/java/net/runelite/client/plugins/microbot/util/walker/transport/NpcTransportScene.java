package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcTransport;

/** Live resolver boundary for direct NPC-backed travel. */
public interface NpcTransportScene
{
	NpcTransport find(PlannedEdge edge);
}
