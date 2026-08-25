package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

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
		assertFalse(FairyRingPolicy.isEligible(new Transport(origin, destination,
			"AKR", TransportType.FAIRY_RING, true, 5), origin));
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
}
