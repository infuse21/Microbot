package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.client.plugins.microbot.util.walker.door.model.OrdinaryDoor;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;

/** Live-scene boundary used by the pure ordinary-door route scanner. */
public interface DoorScene
{
	OrdinaryDoor findOrdinaryDoor(PlannedEdge edge);
}
