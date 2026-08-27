package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.GnomeGlider;

/** Live resolver boundary for the staged gnome-glider interface. */
public interface GnomeGliderScene
{
	GnomeGlider find(PlannedEdge edge);

	GnomeGlider observe(PlannedEdge edge, String pendingAction);
}
