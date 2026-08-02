package net.runelite.client.plugins.microbot.questing;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.runelite.client.plugins.microbot.questing.QuestingScript.nextRouteWaypoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Route following for stealth/follow steps.
 *
 * <p>The route is Children of the Sun's, verbatim: {@code linePoints} null-separated into a segment per
 * marked tile, each segment ending on its own cover. Its last segment dog-legs south, east, then north
 * to get round a shop, which is the whole reason a step's authored route has to be followed rather than
 * interpolated towards the cover in a straight line — staging along one put the character inside that
 * shop and out the far side.
 */
public class FollowRouteWaypointTest
{
	private static WorldPoint at(int x, int y)
	{
		return new WorldPoint(x, y, 0);
	}

	/** Children of the Sun's followGuard route, exactly as the quest ships it. */
	private static final List<WorldPoint> ROUTE = Arrays.asList(
		at(3225, 3429), at(3233, 3429), at(3233, 3427),
		null,
		at(3235, 3429), at(3240, 3429), at(3240, 3417),
		null,
		at(3242, 3417), at(3242, 3403), at(3241, 3403),
		null,
		at(3239, 3401), at(3236, 3397), at(3236, 3392),
		null,
		at(3238, 3390), at(3248, 3390), at(3248, 3396), at(3247, 3396), at(3247, 3397));

	private static final WorldPoint COVER_1 = at(3233, 3427);
	private static final WorldPoint COVER_3 = at(3241, 3403);
	private static final WorldPoint COVER_4 = at(3236, 3392);
	private static final WorldPoint COVER_5 = at(3247, 3397);

	@Test
	public void headsForTheNextWaypointFromTheSectionStart()
	{
		assertEquals(at(3233, 3429), nextRouteWaypoint(ROUTE, COVER_1, at(3226, 3428)));
	}

	/**
	 * The regression this test exists for. Standing on the MIDDLE waypoint of segment 1, "the first
	 * waypoint more than a tile away" returned the segment's own start eight tiles behind, walking the
	 * character back towards Alina until the guard was far enough ahead to fail the step.
	 */
	@Test
	public void neverTargetsAWaypointAlreadyWalkedPast()
	{
		assertEquals(at(3233, 3427), nextRouteWaypoint(ROUTE, COVER_1, at(3233, 3429)));
	}

	@Test
	public void staysOnTheCoverOnceThere()
	{
		assertEquals(COVER_1, nextRouteWaypoint(ROUTE, COVER_1, at(3233, 3427)));
	}

	/**
	 * Segment 4 is the leg that used to stage into the shop: a straight line from cover 3 gives
	 * (3238,3396), which is inside the building, while the authored route goes via (3236,3397).
	 */
	@Test
	public void followsTheAuthoredRouteRoundTheBuilding()
	{
		assertEquals(at(3239, 3401), nextRouteWaypoint(ROUTE, COVER_4, at(3241, 3403)));
		assertEquals(at(3236, 3397), nextRouteWaypoint(ROUTE, COVER_4, at(3239, 3401)));
		assertEquals(COVER_4, nextRouteWaypoint(ROUTE, COVER_4, at(3236, 3397)));
	}

	/** The dog-leg proper: south, east, then north, never a straight line at the cover. */
	@Test
	public void followsTheDogLegOnTheFinalSegment()
	{
		assertEquals(at(3238, 3390), nextRouteWaypoint(ROUTE, COVER_5, at(3236, 3392)));
		assertEquals(at(3248, 3390), nextRouteWaypoint(ROUTE, COVER_5, at(3238, 3390)));
		assertEquals(at(3248, 3396), nextRouteWaypoint(ROUTE, COVER_5, at(3248, 3390)));
		assertEquals(at(3247, 3396), nextRouteWaypoint(ROUTE, COVER_5, at(3248, 3396)));
	}

	@Test
	public void picksTheSegmentEndingOnTheRequestedCover()
	{
		assertEquals(at(3242, 3417), nextRouteWaypoint(ROUTE, COVER_3, at(3240, 3417)));
	}

	/** A cover no segment ends on cannot be matched, so decline rather than walk the wrong segment. */
	@Test
	public void declinesWhenNoSegmentEndsOnTheCover()
	{
		assertNull(nextRouteWaypoint(ROUTE, at(3300, 3300), at(3226, 3428)));
	}

	@Test
	public void declinesWhenTheStepShipsNoRoute()
	{
		assertNull(nextRouteWaypoint(null, COVER_1, at(3226, 3428)));
		assertNull(nextRouteWaypoint(Collections.emptyList(), COVER_1, at(3226, 3428)));
		assertNull(nextRouteWaypoint(ROUTE, COVER_1, null));
	}

	/** Knocked off the route, rejoin it going forwards rather than doubling back to its start. */
	@Test
	public void rejoinsForwardsWhenOffRoute()
	{
		assertEquals(at(3233, 3427), nextRouteWaypoint(ROUTE, COVER_1, at(3234, 3430)));
	}
}
