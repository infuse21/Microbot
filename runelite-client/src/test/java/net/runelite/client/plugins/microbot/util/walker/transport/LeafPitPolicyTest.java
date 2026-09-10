package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LeafPitPolicyTest
{
	@Test
	public void nearSideObjectMatchesTheCacheForEveryDirectedJump()
	{
		assertLeaves(2274, 3172, 3176, 3173);
		assertLeaves(2274, 3176, 3172, 3175);
		assertLeaves(2267, 3201, 3205, 3202);
		assertLeaves(2267, 3205, 3201, 3204);
		assertNull(LeafPitPolicy.nearSideLeaves(new WorldPoint(2274, 3172, 0),
			new WorldPoint(2274, 3175, 0)));
	}

	@Test
	public void recoveryRejectsOtherPitsAndUnrelatedUndergroundTiles()
	{
		for (WorldPoint corner : new WorldPoint[]{new WorldPoint(2313, 9656, 0),
			new WorldPoint(2336, 9656, 0), new WorldPoint(2354, 9656, 0), new WorldPoint(2354, 9643, 0)})
		{
			assertTrue(LeafPitPolicy.inPit(corner));
			assertTrue(LeafPitPolicy.samePit(corner, new WorldPoint(corner.getX() + 1, corner.getY() + 1, 0)));
			assertFalse(LeafPitPolicy.samePit(corner, new WorldPoint(corner.getX() + 2, corner.getY(), 0)));
			assertFalse(LeafPitPolicy.inPit(new WorldPoint(corner.getX(), corner.getY(), 1)));
		}
		assertFalse(LeafPitPolicy.samePit(new WorldPoint(2313, 9656, 0), new WorldPoint(2336, 9656, 0)));
		assertFalse(LeafPitPolicy.inPit(new WorldPoint(2274, 9574, 0)));
		assertFalse(LeafPitPolicy.inPit(null));
	}

	@Test
	public void jumpRequiresSurvivingTheMaximumFailureDamage()
	{
		Transport row = new Transport(new WorldPoint(2274, 3172, 0), new WorldPoint(2274, 3176, 0),
			"", TransportType.AGILITY_SHORTCUT, true, "Jump", "Leaves", LeafPitPolicy.LEAVES);
		assertTrue(Rs2CatalogTransitionScene.hasSafeCurrentHitpoints(row, 19));
		assertFalse(Rs2CatalogTransitionScene.hasSafeCurrentHitpoints(row, 18));
		assertFalse(Rs2CatalogTransitionScene.hasSafeCurrentHitpoints(row, 1));
		assertFalse(CatalogTransitionPolicy.isEligible(row));
	}

	private static void assertLeaves(int x, int fromY, int toY, int objectY)
	{
		assertEquals(new WorldPoint(x, objectY, 0), LeafPitPolicy.nearSideLeaves(
			new WorldPoint(x, fromY, 0), new WorldPoint(x, toY, 0)));
	}
}
