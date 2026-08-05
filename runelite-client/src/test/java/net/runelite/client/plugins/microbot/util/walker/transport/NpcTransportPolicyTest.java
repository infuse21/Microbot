package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NpcTransportPolicyTest
{
	private static final WorldPoint A = new WorldPoint(2503, 3193, 0);
	private static final WorldPoint B = new WorldPoint(2515, 3159, 0);

	@Test
	public void acceptsOneClickNpcShipAndBoatRows()
	{
		assertTrue(NpcTransportPolicy.isEligible(row(TransportType.NPC,
			"Follow", "Elkoy", null)));
		assertTrue(NpcTransportPolicy.isEligible(row(TransportType.SHIP,
			"Travel", "Squire", null)));
		assertTrue(NpcTransportPolicy.isEligible(row(TransportType.BOAT,
			"Waterbirth Island", "Jarvald", null)));
	}

	@Test
	public void rejectsDialogueMenusAndOtherFamilies()
	{
		assertFalse(NpcTransportPolicy.isEligible(row(TransportType.SHIP,
			"Talk-to", "Captain Shanks", null)));
		assertFalse(NpcTransportPolicy.isEligible(row(TransportType.BOAT,
			"Board", "Boaty", "Shayzien")));
		assertFalse(NpcTransportPolicy.isEligible(row(TransportType.TRANSPORT,
			"Travel", "Guide", null)));
	}

	@Test
	public void objectBackedRowMatchesExactIdOrExactNameWithCatalogAction()
	{
		Transport row = row(TransportType.BOAT, "Board", "Swamp Boaty", null);

		assertTrue(NpcTransportPolicy.isLiveObjectMatch(row, 123, null,
			new String[]{"Board"}, A));
		assertTrue(NpcTransportPolicy.isLiveObjectMatch(row, 999, "Swamp Boaty",
			new String[]{null, "Board"}, A));
		assertFalse(NpcTransportPolicy.isLiveObjectMatch(row, 999, "Boaty",
			new String[]{"Board"}, A));
		assertFalse(NpcTransportPolicy.isLiveObjectMatch(row, 123, "Swamp Boaty",
			new String[]{"Look-at"}, A));
	}

	@Test
	public void objectBackedRowRequiresCatalogOriginProximity()
	{
		Transport row = row(TransportType.BOAT, "Board", "Swamp Boaty", null);

		assertTrue(NpcTransportPolicy.isLiveObjectMatch(row, 123, "Swamp Boaty",
			new String[]{"Board"},
			new WorldPoint(A.getX() + NpcTransportPolicy.LIVE_OBJECT_ORIGIN_TOLERANCE,
				A.getY(), 0)));
		assertFalse(NpcTransportPolicy.isLiveObjectMatch(row, 123, "Swamp Boaty",
			new String[]{"Board"},
			new WorldPoint(A.getX() + NpcTransportPolicy.LIVE_OBJECT_ORIGIN_TOLERANCE + 1,
				A.getY(), 0)));
		assertFalse(NpcTransportPolicy.isLiveObjectMatch(row, 123, "Swamp Boaty",
			new String[]{"Board"}, new WorldPoint(A.getX(), A.getY(), 1)));
	}

	@Test
	public void liveObjectActionFormattingCanDifferFromCatalog()
	{
		Transport row = row(TransportType.SHIP, "Quick-Travel", "Barge guard", null);

		assertTrue(NpcTransportPolicy.isLiveObjectMatch(row, 123, null,
			new String[]{"Quick travel"}, A));
		assertEquals("Quick travel", NpcTransportPolicy.matchAction(
			new String[]{"Look-at", "Quick travel"}, "Quick-Travel"));
	}

	@Test
	public void ineligibleRowCannotObjectMatch()
	{
		assertFalse(NpcTransportPolicy.isLiveObjectMatch(
			row(TransportType.BOAT, "Board", "Boaty", "Shayzien"), 123, "Boaty",
			new String[]{"Board"}, A));
	}

	private static Transport row(TransportType type, String action, String name,
		String display)
	{
		return new Transport(A, B, display, type, false, action, name, 123);
	}
}
