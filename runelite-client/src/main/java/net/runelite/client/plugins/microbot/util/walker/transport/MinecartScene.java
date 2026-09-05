package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinecartTransport;

/** Live object, equipment, menu, and restoration boundary for minecart routes. */
public interface MinecartScene
{
	MinecartTransport find(PlannedEdge edge);

	MinecartTransport observe(PlannedEdge edge, String pendingAction,
		int catalogObjectId);

	MinecartTransport restore(PlannedEdge edge, String pendingAction,
		int catalogObjectId, WorldPoint player);
}
