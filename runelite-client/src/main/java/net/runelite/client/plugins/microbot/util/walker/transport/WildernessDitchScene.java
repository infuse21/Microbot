package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.WildernessDitch;

/** Live resolver boundary for ditch object, warning, and landing stages. */
public interface WildernessDitchScene
{
	WildernessDitch find(PlannedEdge edge);

	WildernessDitch observe(PlannedEdge edge, String pendingAction, int catalogObjectId);
}
