package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

public interface ItemTeleportScene
{
	ItemTeleport find(PlannedEdge edge);

	default boolean hasLanded(PlannedEdge edge, net.runelite.api.coords.WorldPoint player)
	{
		return player != null && player.getPlane() == edge.to().getPlane()
			&& player.distanceTo2D(edge.to()) <= 3;
	}

	default ItemTeleport observe(PlannedEdge edge, String pendingAction)
	{
		return find(edge);
	}

	default ItemTeleport restore(PlannedEdge edge, String pendingAction)
	{
		return null;
	}
}
