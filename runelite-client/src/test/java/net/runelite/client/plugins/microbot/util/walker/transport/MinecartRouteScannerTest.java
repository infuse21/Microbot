package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinecartTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MinecartRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2923, 10172, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(3141, 3504, 0);
	private static final int OBJECT_ID = 7028;
	private static final int WEAPON_ID = 4151;
	private static final int SHIELD_ID = 8850;

	@Test
	public void directObjectRemainsOwnedUntilDirectedLanding()
	{
		MinecartRouteScanner scanner = new MinecartRouteScanner();
		MutableScene scene = new MutableScene(stage(MinecartTransport.Stage.OBJECT,
			MinecartPolicy.objectAction("Ride", -1, -1), true));
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.MINECART), 0, 1,
			ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.MINECART, pending.getKind());
		assertTrue(pending.isReady());

		scene.current = null;
		RouteInteraction travelling = scanner.observePending(pending, ORIGIN, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(travelling, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void advancesPreparationMenuAndRestorationOneStageAtATime()
	{
		MinecartRouteScanner scanner = new MinecartRouteScanner();
		MutableScene scene = new MutableScene(stage(
			MinecartTransport.Stage.UNEQUIP_WEAPON,
			MinecartPolicy.unequipWeaponAction(WEAPON_ID, SHIELD_ID), true));
		RouteInteraction current = scanner.scan(plan(RouteEdge.Kind.MINECART), 0, 1,
			ORIGIN, scene, 13);

		current = advance(scanner, current, scene, MinecartTransport.Stage.UNEQUIP_SHIELD,
			MinecartPolicy.unequipShieldAction(WEAPON_ID, SHIELD_ID), ORIGIN);
		current = advance(scanner, current, scene, MinecartTransport.Stage.OBJECT,
			MinecartPolicy.objectAction("Travel", WEAPON_ID, SHIELD_ID), ORIGIN);
		current = advance(scanner, current, scene, MinecartTransport.Stage.DESTINATION,
			MinecartPolicy.destinationAction("1: Arceuus", WEAPON_ID, SHIELD_ID),
			ORIGIN);

		scene.current = null;
		current = scanner.observePending(current, ORIGIN, scene, 13);
		assertFalse(current.isReady());

		scene.restore = stage(MinecartTransport.Stage.RESTORE_INVENTORY,
			MinecartPolicy.restoreOpenAction(WEAPON_ID, SHIELD_ID), true);
		current = scanner.observePending(current, DESTINATION, scene, 13);
		assertTrue(current.isReady());

		RouteInteraction waitingForInventory = scanner.observePending(
			current, DESTINATION, scene, 13);
		assertFalse(waitingForInventory.isReady());

		scene.restore = stage(MinecartTransport.Stage.RESTORE_WEAPON,
			MinecartPolicy.restoreWeaponAction(WEAPON_ID, SHIELD_ID), true);
		current = scanner.observePending(waitingForInventory, DESTINATION, scene, 13);
		assertTrue(current.isReady());

		scene.restore = stage(MinecartTransport.Stage.RESTORE_SHIELD,
			MinecartPolicy.restoreShieldAction(WEAPON_ID, SHIELD_ID), true);
		current = scanner.observePending(current, DESTINATION, scene, 13);
		assertTrue(current.isReady());

		scene.restore = null;
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(current, DESTINATION, scene, 13).getStatus());
	}

	@Test
	public void missingDestinationAndChangedCatalogIdentityAreUnavailable()
	{
		MinecartRouteScanner scanner = new MinecartRouteScanner();
		MutableScene scene = new MutableScene(stage(
			MinecartTransport.Stage.DESTINATION_UNAVAILABLE,
			MinecartPolicy.destinationAction("1: Arceuus", -1, -1), false));
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.MINECART), 0, 1,
			ORIGIN, scene, 13);
		assertEquals(RouteInteraction.Status.UNAVAILABLE, pending.getStatus());

		scene.current = new MinecartTransport(null, ORIGIN, OBJECT_ID + 1,
			MinecartPolicy.objectAction("Ride", -1, -1), ORIGIN, DESTINATION,
			MinecartTransport.Stage.OBJECT, -1, -1, true);
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new MinecartRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT), 0, 1,
			ORIGIN, new MutableScene(stage(MinecartTransport.Stage.OBJECT,
				MinecartPolicy.objectAction("Ride", -1, -1), true)), 13));
	}

	private static RouteInteraction advance(MinecartRouteScanner scanner,
		RouteInteraction pending, MutableScene scene, MinecartTransport.Stage stage,
		String action, WorldPoint player)
	{
		scene.current = stage(stage, action, true);
		RouteInteraction next = scanner.observePending(pending, player, scene, 13);
		assertEquals(action, next.getAction());
		assertTrue(next.isReady());
		return next;
	}

	private static MinecartTransport stage(MinecartTransport.Stage stage,
		String action, boolean ready)
	{
		return new MinecartTransport(null, ORIGIN, OBJECT_ID, action, ORIGIN,
			DESTINATION, stage, MinecartPolicy.originalWeaponId(action),
			MinecartPolicy.originalShieldId(action), ready);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements MinecartScene
	{
		private MinecartTransport current;
		private MinecartTransport restore;

		private MutableScene(MinecartTransport current)
		{
			this.current = current;
		}

		@Override
		public MinecartTransport find(PlannedEdge edge)
		{
			return current;
		}

		@Override
		public MinecartTransport observe(PlannedEdge edge, String pendingAction,
			int catalogObjectId)
		{
			return current;
		}

		@Override
		public MinecartTransport restore(PlannedEdge edge, String pendingAction,
			int catalogObjectId, WorldPoint player)
		{
			return restore;
		}
	}
}
