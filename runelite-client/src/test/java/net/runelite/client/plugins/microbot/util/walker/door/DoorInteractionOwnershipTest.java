package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DoorInteractionOwnershipTest
{
	@Test
	public void strongholdSecurityRegionUsesDialogueAwareDoorPolicy()
	{
		WorldPoint stronghold = new WorldPoint(1859, 5243, 0);

		assertTrue(DoorInteractionOwnership.isStrongholdSecurityRegion(stronghold));
	}

	@Test
	public void ordinarySurfaceRouteRemainsEngineEligible()
	{
		WorldPoint start = new WorldPoint(3219, 3219, 0);

		assertFalse(DoorInteractionOwnership.isStrongholdSecurityRegion(start));
	}
}
