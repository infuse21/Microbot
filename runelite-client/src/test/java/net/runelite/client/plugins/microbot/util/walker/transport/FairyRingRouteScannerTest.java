package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.FairyRing;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FairyRingRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2705, 3576, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(1826, 3540, 0);
	private static final int ORIGINAL_WEAPON = 4151;

	@Test
	public void restorationCompletesOnlyFromEquipmentObservation()
	{
		assertFalse(Rs2FairyRingScene.restorationComplete(ORIGINAL_WEAPON, false));
		assertTrue(Rs2FairyRingScene.restorationComplete(ORIGINAL_WEAPON, true));
		assertTrue(Rs2FairyRingScene.restorationComplete(-1, false));
	}

	@Test
	public void advancesOneEquipmentObjectDialAndTeleportCommandPerStage()
	{
		FairyRingRouteScanner scanner = new FairyRingRouteScanner();
		MutableScene scene = new MutableScene(FairyRing.Stage.EQUIP_STAFF);
		RouteInteraction equip = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.FAIRY_RING, equip.getKind());
		assertEquals(ORIGINAL_WEAPON, equip.getObjectId());
		assertTrue(FairyRingPolicy.isEquipAction(equip.getAction()));

		scene.stage = FairyRing.Stage.OBJECT;
		RouteInteraction object = scanner.observePending(equip, ORIGIN, scene, 13);
		assertEquals("Configure", object.getAction());
		assertTrue(object.isReady());

		scene.stage = FairyRing.Stage.ROTATE;
		RouteInteraction rotate = scanner.observePending(object, ORIGIN, scene, 13);
		assertTrue(FairyRingPolicy.isRotateAction(rotate.getAction()));

		scene.stage = FairyRing.Stage.TELEPORT;
		RouteInteraction teleport = scanner.observePending(rotate, ORIGIN, scene, 13);
		assertEquals(FairyRingPolicy.TELEPORT_ACTION, teleport.getAction());
		assertTrue(teleport.isReady());
	}

	@Test
	public void disappearingSourceStateWaitsForLandingThenRestoresWeapon()
	{
		FairyRingRouteScanner scanner = new FairyRingRouteScanner();
		MutableScene scene = new MutableScene(FairyRing.Stage.TELEPORT);
		RouteInteraction pending = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);
		scene.visible = false;
		// Source UI and catalog-backed scene resolution are both gone in transit;
		// restoration remains owned by the immutable pending edge.

		RouteInteraction waiting = scanner.observePending(pending, ORIGIN, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, waiting.getStatus());
		assertFalse(waiting.isReady());

		scene.restoreNeeded = true;
		RouteInteraction openInventory = scanner.observePending(pending, DESTINATION, scene, 13);
		assertEquals(FairyRingPolicy.restoreOpenAction(ORIGINAL_WEAPON),
			openInventory.getAction());
		assertTrue(openInventory.isReady());

		RouteInteraction waitingForInventory = scanner.observePending(openInventory,
			DESTINATION, scene, 13);
		assertEquals(FairyRingPolicy.restoreOpenAction(ORIGINAL_WEAPON),
			waitingForInventory.getAction());
		assertFalse(waitingForInventory.isReady());

		scene.inventoryReady = true;
		RouteInteraction restore = scanner.observePending(waitingForInventory,
			DESTINATION, scene, 13);
		assertEquals(FairyRingPolicy.restoreAction(ORIGINAL_WEAPON), restore.getAction());
		assertTrue(restore.isReady());

		RouteInteraction waitingForEquipment = scanner.observePending(restore,
			DESTINATION, scene, 13);
		assertEquals(FairyRingPolicy.restoreAction(ORIGINAL_WEAPON),
			waitingForEquipment.getAction());
		assertFalse(waitingForEquipment.isReady());

		scene.restoreNeeded = false;
		RouteInteraction cleared = scanner.observePending(waitingForEquipment,
			DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, cleared.getStatus());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION, RouteEdge.Kind.FAIRY_RING);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static FairyRing ring(FairyRing.Stage stage)
	{
		String action;
		switch (stage)
		{
			case EQUIP_STAFF:
				action = FairyRingPolicy.equipAction(772);
				break;
			case ROTATE:
				action = FairyRingPolicy.rotateAction(26083347, 512);
				break;
			case TELEPORT:
				action = FairyRingPolicy.TELEPORT_ACTION;
				break;
			case RESTORE_INVENTORY:
				action = FairyRingPolicy.restoreOpenAction(ORIGINAL_WEAPON);
				break;
			case RESTORE_WEAPON:
				action = FairyRingPolicy.restoreAction(ORIGINAL_WEAPON);
				break;
			case OBJECT:
			default:
				action = "Configure";
				break;
		}
		WorldPoint tile = stage == FairyRing.Stage.RESTORE_WEAPON
			|| stage == FairyRing.Stage.RESTORE_INVENTORY ? DESTINATION : ORIGIN;
		return new FairyRing(ORIGIN, DESTINATION, "AKR", tile, 29495, action,
			stage, ORIGINAL_WEAPON);
	}

	private static final class MutableScene implements FairyRingScene
	{
		private FairyRing.Stage stage;
		private boolean visible = true;
		private boolean restoreNeeded;
		private boolean inventoryReady;

		private MutableScene(FairyRing.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public FairyRing find(PlannedEdge edge)
		{
			return visible ? ring(stage) : null;
		}

		@Override
		public FairyRing observe(PlannedEdge edge, String pendingAction,
			int originalWeaponId)
		{
			return visible ? ring(stage) : null;
		}

		@Override
		public FairyRing restore(PlannedEdge edge, String pendingAction,
			int originalWeaponId, WorldPoint player)
		{
			if (!restoreNeeded)
			{
				return null;
			}
			if (FairyRingPolicy.isRestoreAction(pendingAction)
				|| FairyRingPolicy.isRestoreOpenAction(pendingAction) && inventoryReady)
			{
				return ring(FairyRing.Stage.RESTORE_WEAPON);
			}
			return ring(FairyRing.Stage.RESTORE_INVENTORY);
		}
	}
}
