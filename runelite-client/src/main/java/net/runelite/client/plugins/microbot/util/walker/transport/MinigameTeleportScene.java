package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinigameTeleport;

/** Live grouping-interface boundary for minigame teleports. */
public interface MinigameTeleportScene
{
	MinigameTeleport find(PlannedEdge edge);

	MinigameTeleport observe(PlannedEdge edge, String pendingAction);
}
