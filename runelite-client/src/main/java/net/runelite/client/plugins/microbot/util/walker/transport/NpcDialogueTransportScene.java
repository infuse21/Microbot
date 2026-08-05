package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcDialogueTransport;

/** Live resolver boundary for staged dialogue-menu NPC/ship/boat travel. */
public interface NpcDialogueTransportScene
{
	NpcDialogueTransport find(PlannedEdge edge);

	NpcDialogueTransport observe(PlannedEdge edge, String pendingAction);
}
