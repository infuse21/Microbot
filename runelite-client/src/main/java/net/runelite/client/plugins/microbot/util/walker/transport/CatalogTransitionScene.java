package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;

/** Live-scene lookup for one direct catalog-backed scene transition. */
@FunctionalInterface
public interface CatalogTransitionScene
{
	CatalogTransition find(PlannedEdge edge);
}
