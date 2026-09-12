package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FairyRingPolicyTest
{
	@Test
	public void acceptsDirectedThreeLetterFairyRing()
	{
		Transport ring = new Transport(new WorldPoint(2705, 3576, 0),
			new WorldPoint(1826, 3540, 0), "AKR", TransportType.FAIRY_RING,
			true, 5);

		assertTrue(FairyRingPolicy.isEligible(ring));
	}

	@Test
	public void rejectsInvalidCodesAndOtherTransportTypes()
	{
		WorldPoint origin = new WorldPoint(2705, 3576, 0);
		WorldPoint destination = new WorldPoint(1826, 3540, 0);
		assertFalse(FairyRingPolicy.isEligible(new Transport(origin, destination,
			"A1R", TransportType.FAIRY_RING, true, 5)));
		assertFalse(FairyRingPolicy.isEligible(new Transport(origin, destination,
			"AKR", TransportType.SPIRIT_TREE, true, 5)));
		assertFalse(FairyRingPolicy.isEligible(new Transport(origin, destination,
			"DIQ", TransportType.FAIRY_RING, true, 5), null));
		assertTrue(FairyRingPolicy.isEligible(new Transport(origin, destination,
			"AKR", TransportType.FAIRY_RING, true, 5), origin));
		assertFalse(FairyRingPolicy.isEligible(new Transport(origin, destination,
			"AKR", TransportType.FAIRY_RING, true, 5), destination));
	}

	@Test
	public void stageActionsRoundTripTheirCommandIdentity()
	{
		String equip = FairyRingPolicy.equipAction(772);
		String restoreOpen = FairyRingPolicy.restoreOpenAction(6563);
		String restore = FairyRingPolicy.restoreAction(6563);
		String rotate = FairyRingPolicy.rotateAction(26083347, 512);

		assertTrue(FairyRingPolicy.isEquipAction(equip));
		assertEquals(772, FairyRingPolicy.equipItemId(equip));
		assertTrue(FairyRingPolicy.isRestoreOpenAction(restoreOpen));
		assertEquals(6563, FairyRingPolicy.restoreOpenItemId(restoreOpen));
		assertTrue(FairyRingPolicy.isRestoreAction(restore));
		assertEquals(6563, FairyRingPolicy.restoreItemId(restore));
		assertTrue(FairyRingPolicy.isStageAction(equip));
		assertTrue(FairyRingPolicy.isStageAction(restoreOpen));
		assertTrue(FairyRingPolicy.isStageAction(restore));
		assertTrue(FairyRingPolicy.isStageAction(FairyRingPolicy.TELEPORT_ACTION));
		assertFalse(FairyRingPolicy.isStageAction("Configure"));
		assertTrue(FairyRingPolicy.isRotateAction(rotate));
		assertEquals(26083347, FairyRingPolicy.rotationWidgetId(rotate));
		assertEquals(1024, FairyRingPolicy.desiredRotation('R'));
	}

	@Test
	public void finishedQuestHideoutFanInUsesOneExactFourCodeSequence()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> FairyRingPolicy.HIDEOUT_SEQUENCE.equals(row.getDisplayInfo()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(53, rows.size());
		assertTrue(rows.stream().allMatch(FairyRingPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.getDestination().equals(
			FairyRingPolicy.HIDEOUT_DESTINATION)));
		assertTrue(rows.stream().allMatch(row -> QuestState.FINISHED.equals(
			row.getQuests().get(Quest.FAIRYTALE_II__CURE_A_QUEEN))));
	}

	@Test
	public void sequenceActionsRetainTheirStepAndExpectedIntermediateLanding()
	{
		for (int step = 0; step < 4; step++)
		{
			String object = FairyRingPolicy.sequenceObjectAction(step, "Configure");
			String rotate = FairyRingPolicy.sequenceRotateAction(step, 1000 + step, 512);
			String teleport = FairyRingPolicy.sequenceTeleportAction(step);
			assertEquals(step, FairyRingPolicy.sequenceStep(object));
			assertEquals(step, FairyRingPolicy.sequenceStep(rotate));
			assertEquals(step, FairyRingPolicy.sequenceStep(teleport));
			assertEquals("Configure", FairyRingPolicy.sequenceObjectLiveAction(object));
			assertEquals(1000 + step, FairyRingPolicy.rotationWidgetId(rotate));
			assertTrue(FairyRingPolicy.isStageAction(rotate));
			assertTrue(FairyRingPolicy.isStageAction(teleport));
		}
		assertEquals(new WorldPoint(2700, 3247, 0), FairyRingPolicy.sequenceLanding(0));
		assertEquals(new WorldPoint(2213, 3099, 0), FairyRingPolicy.sequenceLanding(1));
		assertEquals(new WorldPoint(2213, 3099, 0), FairyRingPolicy.sequenceLanding(2));
		assertEquals(FairyRingPolicy.HIDEOUT_DESTINATION, FairyRingPolicy.sequenceLanding(3));
	}

	@Test
	public void sequenceAdvancesOnlyAfterEachObservedIntermediateLanding()
	{
		WorldPoint air = new WorldPoint(2700, 3247, 0);
		WorldPoint dlr = new WorldPoint(2213, 3099, 0);
		assertFalse(FairyRingPolicy.sequenceTeleportCompleted(0, air, true));
		assertFalse(FairyRingPolicy.sequenceTeleportCompleted(0, dlr, false));
		assertTrue(FairyRingPolicy.sequenceTeleportCompleted(0, air, false));
		assertTrue(FairyRingPolicy.sequenceTeleportCompleted(1, dlr, false));
		assertTrue(FairyRingPolicy.sequenceTeleportCompleted(2, dlr, false));
		assertFalse(FairyRingPolicy.sequenceTeleportCompleted(3,
			FairyRingPolicy.HIDEOUT_DESTINATION, false));
	}
}
