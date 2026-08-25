package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.FairyRing;

/** Live resolver boundary for fairy-ring equipment, object, dial, and landing stages. */
public interface FairyRingScene
{
	FairyRing find(PlannedEdge edge);

	FairyRing observe(PlannedEdge edge, String pendingAction, int originalWeaponId);

	FairyRing restore(PlannedEdge edge, String pendingAction, int originalWeaponId,
		WorldPoint player);
}
