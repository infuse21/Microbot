package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NpcDialogueTransportPolicyTest
{
	private static final WorldPoint A = new WorldPoint(1342, 3645, 0);
	private static final WorldPoint B = new WorldPoint(1408, 3612, 0);

	@Test
	public void acceptsRealDialogueMenuCatalogRows()
	{
		Transport boaty = realRow(TransportType.BOAT, new WorldPoint(1342, 3645, 0),
			"Shayzien");
		Transport mountainGuide = realRow(TransportType.NPC, new WorldPoint(1277, 3558, 0),
			"The Shayzien Outpost");

		assertNotNull(boaty);
		assertTrue(NpcDialogueTransportPolicy.isEligible(boaty));
		assertNotNull(mountainGuide);
		assertTrue(NpcDialogueTransportPolicy.isEligible(mountainGuide));
	}

	@Test
	public void rejectsTalkToDirectAndNonCoinRows()
	{
		assertFalse(NpcDialogueTransportPolicy.isEligible(row(TransportType.SHIP,
			"Talk-to", "Captain Shanks", "Port Sarim")));
		assertFalse(NpcDialogueTransportPolicy.isEligible(row(TransportType.BOAT,
			"Travel", "Jarvald", null)));
		assertFalse(NpcDialogueTransportPolicy.isEligible(row(TransportType.TRANSPORT,
			"Travel", "Guide", "Somewhere")));

		Transport ghostCaptain = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Ghost captain".equals(candidate.getName()))
			.filter(candidate -> candidate.getCurrencyAmount() > 0)
			.findFirst().orElse(null);
		assertNotNull(ghostCaptain);
		assertFalse(NpcDialogueTransportPolicy.isEligible(ghostCaptain));
	}

	@Test
	public void acceptsCoinPaidConfirmationRows()
	{
		Transport tobias = realRow(TransportType.SHIP, new WorldPoint(3029, 3217, 0),
			"Musa Point");
		assertNotNull(tobias);
		assertTrue(tobias.getCurrencyAmount() > 0);
		assertTrue(NpcDialogueTransportPolicy.isEligible(tobias));

		Transport quickBoard = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Swamp Boaty".equals(candidate.getName()))
			.filter(candidate -> "Quick-board".equals(candidate.getAction()))
			.findFirst().orElse(null);
		assertNotNull(quickBoard);
		assertTrue(NpcDialogueTransportPolicy.isEligible(quickBoard));
	}

	@Test
	public void directAndDialogueFamiliesAreMutuallyExclusive()
	{
		List<Transport> both = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(NpcDialogueTransportPolicy::isEligible)
			.filter(NpcTransportPolicy::isEligible)
			.collect(Collectors.toList());
		assertTrue(both.toString(), both.isEmpty());
	}

	@Test
	public void stageActionsAreDistinguishable()
	{
		String destination = NpcDialogueTransportPolicy.destinationAction("Shayzien");

		assertTrue(NpcDialogueTransportPolicy.isDestinationAction(destination));
		assertTrue(NpcDialogueTransportPolicy.isStageAction(destination));
		assertTrue(NpcDialogueTransportPolicy.isStageAction(
			NpcDialogueTransportPolicy.CONTINUE_ACTION));
		assertFalse(NpcDialogueTransportPolicy.isStageAction("Board"));
		assertEquals("dialogue-destination:Shayzien", destination);
	}

	@Test
	public void liveNpcMatchRequiresExactNameActionAndLocality()
	{
		Transport row = row(TransportType.NPC, "Travel", "Mountain Guide", "Mount Quidamortem");

		assertTrue(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Mountain Guide",
			java.util.Arrays.asList("Talk-to", "Travel"), A.dx(1)));
		assertFalse(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Guide",
			Collections.singletonList("Travel"), A.dx(1)));
		assertFalse(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Mountain Guide",
			Collections.singletonList("Trade"), A.dx(1)));
		// Captain Barnaby stands eight tiles along the pier from his Ardougne row origin,
		// and a deck actor can stand a plane above the anchor.
		assertTrue(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Mountain Guide",
			Collections.singletonList("Travel"), A.dx(8)));
		assertTrue(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Mountain Guide",
			Collections.singletonList("Travel"),
			new WorldPoint(A.getX() + 8, A.getY(), 1)));
		assertFalse(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Mountain Guide",
			Collections.singletonList("Travel"), A.dx(16)));
	}

	@Test
	public void liveNpcActionToleratesDestinationNamedVariants()
	{
		Transport row = row(TransportType.SHIP, "Travel", "Captain Tobias", "Musa Point");

		assertEquals("Travel", NpcDialogueTransportPolicy.matchLiveNpcAction(
			java.util.Arrays.asList("Talk-to", "Travel"), "Travel", "Musa Point"));
		assertEquals("Musa Point", NpcDialogueTransportPolicy.matchLiveNpcAction(
			java.util.Arrays.asList("Talk-to", "Musa Point", "The Pandemonium"),
			"Travel", "Musa Point"));
		assertEquals(null, NpcDialogueTransportPolicy.matchLiveNpcAction(
			Collections.singletonList("Talk-to"), "Travel", "Musa Point"));
		assertTrue(NpcDialogueTransportPolicy.isLiveNpcMatch(row, "Captain Tobias",
			java.util.Arrays.asList("Talk-to", "Musa Point", "The Pandemonium"), A.dx(1)));
	}

	@Test
	public void liveObjectMatchMirrorsDirectFamilyRules()
	{
		Transport row = row(TransportType.BOAT, "Board", "Boaty", "Shayzien");

		assertTrue(NpcDialogueTransportPolicy.isLiveObjectMatch(row, 123, null,
			new String[]{"Board"}, A));
		assertTrue(NpcDialogueTransportPolicy.isLiveObjectMatch(row, 999, "Boaty",
			new String[]{"Board"},
			new WorldPoint(A.getX() + NpcDialogueTransportPolicy.LIVE_ACTOR_ORIGIN_TOLERANCE,
				A.getY(), 0)));
		assertFalse(NpcDialogueTransportPolicy.isLiveObjectMatch(row, 999, "Rowboat",
			new String[]{"Board"}, A));
		assertFalse(NpcDialogueTransportPolicy.isLiveObjectMatch(row, 123, "Boaty",
			new String[]{"Look-at"}, A));
		assertFalse(NpcDialogueTransportPolicy.isLiveObjectMatch(row, 123, "Boaty",
			new String[]{"Board"},
			new WorldPoint(A.getX() + NpcDialogueTransportPolicy.LIVE_ACTOR_ORIGIN_TOLERANCE + 1,
				A.getY(), 0)));
	}

	private static Transport realRow(TransportType type, WorldPoint origin, String display)
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == type)
			.filter(candidate -> origin.equals(candidate.getOrigin()))
			.filter(candidate -> display.equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);
	}

	private static Transport row(TransportType type, String action, String name,
		String display)
	{
		return new Transport(A, B, display, type, false, action, name, 123);
	}
}
