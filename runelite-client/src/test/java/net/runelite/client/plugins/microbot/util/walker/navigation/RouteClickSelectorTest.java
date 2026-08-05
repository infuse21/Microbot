package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class RouteClickSelectorTest
{
	@Test
	public void longSmoothedSegmentSelectsBoundedRawRouteTile()
	{
		List<WorldPoint> raw = straight(3200, 3200, 20);
		RoutePlan plan = plan(raw, Arrays.asList(raw.get(0), raw.get(20)));

		RouteClickSelection selected = RouteClickSelector.select(plan, raw.get(0), 0, 10, 10);

		assertEquals(raw.get(10), selected.getTarget());
		assertEquals(10, selected.getRawIndex());
		assertEquals(1, selected.getSmoothedIndex());
		assertEquals(10, selected.getDistance());
		assertNotEquals("the distant LOS endpoint must not become the click target",
			raw.get(20), selected.getTarget());
	}

	@Test
	public void cornerDoesNotLimitSelectionToLineOfSightAnchor()
	{
		List<WorldPoint> raw = new ArrayList<>();
		for (int x = 3200; x <= 3205; x++)
		{
			raw.add(new WorldPoint(x, 3200, 0));
		}
		for (int y = 3201; y <= 3210; y++)
		{
			raw.add(new WorldPoint(3205, y, 0));
		}
		RoutePlan plan = plan(raw, Arrays.asList(raw.get(0), raw.get(5), raw.get(15)));

		RouteClickSelection selected = RouteClickSelector.select(plan, raw.get(0), 0, 10, 10);

		assertEquals(10, selected.getRawIndex());
		assertTrue("selection should continue around the corner", selected.getTarget().getY() > 3200);
		assertNotEquals("selection must not stop at the LOS corner", raw.get(5), selected.getTarget());
	}

	@Test
	public void repeatedRawTileStopsSelectionBeforeDoubledBackBranch()
	{
		WorldPoint a = new WorldPoint(3200, 3200, 0);
		WorldPoint b = new WorldPoint(3201, 3200, 0);
		WorldPoint c = new WorldPoint(3201, 3201, 0);
		WorldPoint d = new WorldPoint(3200, 3201, 0);
		List<WorldPoint> raw = Arrays.asList(a, b, c, d, a, b);
		RoutePlan plan = plan(raw, Arrays.asList(a, c, a, b));

		RouteClickSelection selected = RouteClickSelector.select(plan, a, 0, 10, 10);

		assertEquals(d, selected.getTarget());
		assertEquals(3, selected.getRawIndex());
	}

	@Test
	public void shortenedReachFallsBackToFullReachWithoutLeavingRoute()
	{
		List<WorldPoint> raw = straight(3200, 3200, 20);
		WorldPoint displaced = new WorldPoint(3200, 3208, 0);
		RoutePlan plan = plan(raw, Arrays.asList(raw.get(0), raw.get(20)));

		RouteClickSelection selected = RouteClickSelector.select(plan, displaced, 0, 7, 10);

		assertEquals("raw-route-full-reach", selected.getSelection());
		assertTrue(raw.contains(selected.getTarget()));
		assertEquals(10, selected.getReach());
	}

	private static RoutePlan plan(List<WorldPoint> raw, List<WorldPoint> smoothed)
	{
		return new RoutePlan(1, 1, raw.get(0),
			Collections.singleton(raw.get(raw.size() - 1)), raw, smoothed, true);
	}

	private static List<WorldPoint> straight(int x, int y, int steps)
	{
		List<WorldPoint> path = new ArrayList<>();
		for (int i = 0; i <= steps; i++)
		{
			path.add(new WorldPoint(x + i, y, 0));
		}
		return path;
	}
}
