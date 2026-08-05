package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DoorInteractionOwnershipTest
{
	@Test
	public void strongholdSecurityRouteRemainsLegacyOwned()
	{
		WorldPoint stronghold = new WorldPoint(1859, 5243, 0);
		WorldPoint ordinary = new WorldPoint(3219, 3219, 0);

		assertTrue(DoorInteractionOwnership.isStrongholdSecurityRegion(stronghold));
		assertFalse(DoorInteractionOwnership.ordinaryEngineAllowed(stronghold, Set.of(stronghold)));
		assertFalse(DoorInteractionOwnership.ordinaryEngineAllowed(ordinary, Set.of(stronghold)));
	}

	@Test
	public void ordinarySurfaceRouteRemainsEngineEligible()
	{
		WorldPoint start = new WorldPoint(3219, 3219, 0);
		WorldPoint target = new WorldPoint(3164, 3476, 0);

		assertFalse(DoorInteractionOwnership.isStrongholdSecurityRegion(start));
		assertTrue(DoorInteractionOwnership.ordinaryEngineAllowed(start, Set.of(target)));
	}
}
