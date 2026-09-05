package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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
	public void rejectsUnknownTalkToDirectAndNonCoinRows()
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
		assertTrue(NpcDialogueTransportPolicy.isEligible(ghostCaptain));
		assertTrue(NpcDialogueTransportPolicy.isGhostCaptain(ghostCaptain));
		assertEquals(Set.of(552, 4250, 13113, 13114, 13115),
			ghostCaptain.getItemIdRequirements().iterator().next());
	}

	@Test
	public void acceptsOnlyTheTwoExactPiratePeteRows()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Pirate Pete".equals(candidate.getName()))
			.collect(Collectors.toList());

		assertEquals(2, rows.size());
		assertTrue(rows.stream().allMatch(NpcDialogueTransportPolicy::isPiratePete));
		assertTrue(rows.stream().allMatch(NpcDialogueTransportPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> "Okay!".equals(
			NpcDialogueTransportPolicy.destinationOption(row))));
	}

	@Test
	public void acceptsOnlyTheTwoExactCaptainShanksRows()
	{
		List<Transport> shanksRows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Captain Shanks".equals(candidate.getName()))
			.collect(Collectors.toList());

		assertEquals(2, shanksRows.size());
		assertTrue(shanksRows.stream().allMatch(NpcDialogueTransportPolicy::isCaptainShanks));
		assertTrue(shanksRows.stream().allMatch(NpcDialogueTransportPolicy::isEligible));
		assertTrue(shanksRows.stream().allMatch(row -> row.getCurrencyAmount() == 50));
		assertTrue(shanksRows.stream().allMatch(row -> row.getQuests().size() == 1));
		assertTrue(shanksRows.stream().map(Transport::getDisplayInfo)
			.collect(Collectors.toSet()).equals(Set.of("Khazard Port", "Port Sarim")));
	}

	@Test
	public void acceptsOnlyTheFourExactCabinBoyHerbertTalkToRows()
	{
		List<Transport> herbertRows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Cabin Boy Herbert".equals(candidate.getName()))
			.collect(Collectors.toList());

		assertEquals(4, herbertRows.size());
		assertTrue(herbertRows.stream().allMatch(NpcDialogueTransportPolicy::isEligible));
		assertTrue(herbertRows.stream().allMatch(
			NpcDialogueTransportPolicy::isCabinBoyHerbert));

		Transport malformed = row(TransportType.SHIP, "Talk-to", "Cabin Boy Herbert",
			"Port Sarim");
		assertFalse(NpcDialogueTransportPolicy.isCabinBoyHerbert(malformed));
		assertFalse(NpcDialogueTransportPolicy.isEligible(malformed));
	}

	@Test
	public void acceptsOnlyTheSixExactMountainGuideShadowRows()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Mountain Guide".equals(candidate.getName()))
			.filter(candidate -> "Travel".equals(candidate.getAction()))
			.collect(Collectors.toList());

		assertEquals(6, rows.size());
		assertTrue(rows.stream().allMatch(NpcDialogueTransportPolicy::isMountainGuideShadow));
		assertTrue(rows.stream().allMatch(NpcDialogueTransportPolicy::isEligible));
		assertEquals(Set.of("Mount Quidamortem.", "South of Quidamortem.",
			"The Shayzien Outpost."), rows.stream()
			.map(NpcDialogueTransportPolicy::destinationOption).collect(Collectors.toSet()));
		assertTrue(rows.stream().allMatch(row ->
			Rs2NpcDialogueTransportScene.matchOptionIndex(List.of(
				"The Shayzien Outpost.", "Mount Quidamortem.",
				"South of Quidamortem.", "Nowhere."),
				NpcDialogueTransportPolicy.destinationOption(row)) >= 0));

		Transport malformed = rows.get(0);
		malformed.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(NpcDialogueTransportPolicy.isMountainGuideShadow(malformed));
		assertFalse(NpcDialogueTransportPolicy.isEligible(malformed));
	}

	@Test
	public void acceptsOnlyTheExactDondakanGoldHelmetRoute()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Dondakan the Dwarf".equals(candidate.getName()))
			.collect(Collectors.toList());

		assertEquals(1, rows.size());
		Transport dondakan = rows.get(0);
		assertTrue(NpcDialogueTransportPolicy.isDondakan(dondakan));
		assertTrue(NpcDialogueTransportPolicy.isEligible(dondakan));
		assertEquals(Set.of(Set.of(4567)), dondakan.getItemIdRequirements());
		assertEquals("Can you shoot me into the rock again?",
			NpcDialogueTransportPolicy.destinationOption(dondakan));
		assertEquals(0, Rs2NpcDialogueTransportScene.matchOptionIndex(List.of(
			"Can you shoot me into the rock again?", "That's great, but I have to go."),
			NpcDialogueTransportPolicy.destinationOption(dondakan)));

		dondakan.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(NpcDialogueTransportPolicy.isDondakan(dondakan));
		assertFalse(NpcDialogueTransportPolicy.isEligible(dondakan));
	}

	@Test
	public void cabinBoyHerbertTravelRequestIsAnExactDialogueStage()
	{
		assertEquals(0, Rs2NpcDialogueTransportScene.matchTravelRequestIndex(List.of(
			"Can you take me somewhere?", "I'm good.")));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchTravelRequestIndex(List.of(
			"Can you take me somewhere else?", "I'm good.")));
		assertEquals(-1, Rs2NpcDialogueTransportScene.matchTravelRequestIndex(List.of(
			"Can you take me somewhere?", "Can you take me somewhere?")));
		assertTrue(NpcDialogueTransportPolicy.isStageAction(
			NpcDialogueTransportPolicy.TRAVEL_REQUEST_ACTION));
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
	public void fossilIslandRowboatSelectsDigsiteInsteadOfStoppingAtTheBarge()
	{
		Transport boat = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.SHIP && row.getObjectId() == 30914)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(new WorldPoint(3362, 3445, 0), boat.getDestination());
		assertEquals("Row to the barge and travel to the Digsite.", boat.getDisplayInfo());
		assertFalse(NpcTransportPolicy.isEligible(boat));
		assertTrue(NpcDialogueTransportPolicy.isEligible(boat));
		assertEquals(1, Rs2NpcDialogueTransportScene.matchOptionIndex(List.of(
			"Row to the barge.", "Row to the barge and travel to the Digsite.", "Cancel."),
			boat.getDisplayInfo()));
	}

	@Test
	public void fossilRowboatDestinationsMatchTextWithoutFixedOrdinals()
	{
		int checked = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getType() != TransportType.BOAT
					|| !Set.of(30914, 30915, 30919).contains(row.getObjectId())) continue;
				String destination = NpcDialogueTransportPolicy.destinationOption(row);
				assertTrue(Set.of("North", "Sea", "Camp").contains(destination));
				assertEquals(-1, Rs2NpcDialogueTransportScene.matchOptionIndex(List.of(
					"Row to the barge.", "Row to the barge and travel to the Digsite.", "Cancel."), destination));
				String option = destination.equals("North") ? "Row to the north of the island."
					: destination.equals("Sea") ? "Row out to sea." : "Row to the camp.";
				assertEquals(0, Rs2NpcDialogueTransportScene.matchOptionIndex(List.of(option, "Cancel."), destination));
				assertEquals(1, Rs2NpcDialogueTransportScene.matchOptionIndex(List.of("Row to the barge.", option, "Cancel."), destination));
				assertEquals(-1, Rs2NpcDialogueTransportScene.matchOptionIndex(List.of(option, option), destination));
				checked++;
			}
		}
		assertEquals(6, checked);
	}

	@Test
	public void voyageAcknowledgementIsNotReplacedByClosingMenuLockEvidence()
	{
		assertTrue(NpcDialogueTransportPolicy.isVoyageStageAction(
			NpcDialogueTransportPolicy.destinationAction("Sea")));
		assertTrue(NpcDialogueTransportPolicy.isVoyageStageAction(
			NpcDialogueTransportPolicy.CONTINUE_ACTION));
		assertFalse(NpcDialogueTransportPolicy.isVoyageStageAction("Travel"));
		assertFalse(NpcDialogueTransportPolicy.isVoyageStageAction(
			NpcDialogueTransportPolicy.CANCEL_UNAVAILABLE_ACTION));
		assertFalse(NpcDialogueTransportPolicy.isVoyageStageAction(
			NpcDialogueTransportPolicy.DESTINATION_UNAVAILABLE_ACTION));
	}

	@Test
	public void acceptsOnlyTheExactShiloVillageTravelCartContract()
	{
		List<Transport> carts = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Pay-fare".equals(candidate.getAction()))
			.filter(candidate -> "Travel cart".equals(candidate.getName()))
			.collect(Collectors.toList());

		assertEquals(20, carts.size());
		assertTrue(carts.stream().allMatch(NpcDialogueTransportPolicy::isTravelCart));
		assertTrue(carts.stream().allMatch(NpcDialogueTransportPolicy::isEligible));
		Transport cart = carts.get(0);
		assertTrue(NpcDialogueTransportPolicy.isLiveObjectMatch(cart,
			cart.getObjectId(), "Travel cart", new String[]{"Board", "Pay-fare"},
			cart.getOrigin()));
		assertFalse(NpcDialogueTransportPolicy.isLiveObjectMatch(cart,
			cart.getObjectId(), "Travel cart", new String[]{"Board"}, cart.getOrigin()));
		cart.setItemIdRequirements(Set.of(Set.of(995)));
		assertFalse(NpcDialogueTransportPolicy.isTravelCart(cart));
	}

	@Test
	public void acceptsAllTwelveExactMagicCarpetRows()
	{
		List<Transport> carpets = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.MAGIC_CARPET)
			.collect(Collectors.toList());

		assertEquals(12, carpets.size());
		assertTrue(carpets.stream().allMatch(NpcDialogueTransportPolicy::isEligible));
	}

	@Test
	public void rejectsMalformedMagicCarpetRows()
	{
		assertFalse(NpcDialogueTransportPolicy.isEligible(new Transport(A, B, "Uzer",
			TransportType.MAGIC_CARPET, true, "Talk-to", "Rug Merchant", 17)));
		assertFalse(NpcDialogueTransportPolicy.isEligible(new Transport(A, B, null,
			TransportType.MAGIC_CARPET, true, "Travel", "Rug Merchant", 17)));
		assertFalse(NpcDialogueTransportPolicy.isEligible(new Transport(A, B, "Uzer",
			TransportType.MAGIC_CARPET, true, "Travel", "Rug Merchant", 999)));
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
