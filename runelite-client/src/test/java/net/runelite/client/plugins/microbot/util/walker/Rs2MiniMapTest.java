package net.runelite.client.plugins.microbot.util.walker;

import java.awt.Rectangle;
import java.awt.Shape;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Rs2MiniMapTest
{
	@Test
	public void zoomAwareReachPreservesNormalRangeAndBoundsZoomedOutClicks()
	{
		Rectangle bounds = new Rectangle(100, 200, 150, 150);
		org.junit.Assert.assertEquals(10, Rs2MiniMap.navigationClickReach(bounds, 4, 52, 52));
		org.junit.Assert.assertEquals(20, Rs2MiniMap.navigationClickReach(bounds, 2, 52, 52));
		org.junit.Assert.assertEquals(40, Rs2MiniMap.navigationClickReach(bounds, 1, 52, 52));
		org.junit.Assert.assertEquals(40, Rs2MiniMap.navigationClickReach(bounds, 0.1, 52, 52));
		org.junit.Assert.assertEquals(8, Rs2MiniMap.navigationClickReach(bounds, 1, 10, 52));
		org.junit.Assert.assertEquals(5, Rs2MiniMap.navigationClickReach(new Rectangle(20, 20), 1, 52, 52));
		org.junit.Assert.assertEquals(10, Rs2MiniMap.navigationClickReach(bounds, Double.NaN, 52, 52));
	}

	@Test
	public void conservativeClipUsesOnlyTheSuppliedBoundsSnapshot()
	{
		Rectangle bounds = new Rectangle(100, 200, 150, 150);
		Shape clip = Rs2MiniMap.createMinimapClipArea(bounds, 0.94);

		bounds.setBounds(0, 0, 1, 1);
		assertTrue(clip.contains(175, 275));
		assertFalse(clip.contains(101, 201));
	}

	@Test
	public void invalidBoundsFailClosed()
	{
		assertNull(Rs2MiniMap.createMinimapClipArea(null, 0.94));
		assertNull(Rs2MiniMap.createMinimapClipArea(new Rectangle(), 0.94));
	}
}
