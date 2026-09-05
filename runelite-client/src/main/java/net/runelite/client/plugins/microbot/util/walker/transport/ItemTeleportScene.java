package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

public interface ItemTeleportScene
{
	ItemTeleport find(PlannedEdge edge);

	default ItemTeleport observe(PlannedEdge edge, String pendingAction)
	{
		return find(edge);
	}
}
