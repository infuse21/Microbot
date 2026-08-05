package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.WallObject;

/** Direct API capture called only from a client-thread boundary (or headless unit tests). */
final class DoorOrientationSnapshot
{
	private DoorOrientationSnapshot() { }

	static int[] capture(WallObject wall)
	{
		return wall == null
			? new int[] {0, 0}
			: new int[] {wall.getOrientationA(), wall.getOrientationB()};
	}
}
