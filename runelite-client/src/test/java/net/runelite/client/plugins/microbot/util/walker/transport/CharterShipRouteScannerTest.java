package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CharterShip;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CharterShipRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2760, 3238, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2792, 3417, 1);

	@Test
	public void progressesFromNpcToDestinationAndConfirmationStages()
	{
		CharterShipRouteScanner scanner = new CharterShipRouteScanner();
		MutableScene scene = new MutableScene(CharterShip.Stage.NPC);
		RouteInteraction npc = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.CHARTER_SHIP, npc.getKind());
		assertEquals("Charter", npc.getAction());
		assertTrue(npc.isReady());

		scene.stage = CharterShip.Stage.DESTINATION;
		RouteInteraction destination = scanner.observePending(npc, ORIGIN, scene, 13);
		assertEquals(CharterShipPolicy.destinationAction("Catherby"),
			destination.getAction());
		assertTrue(destination.isReady());

		scene.stage = CharterShip.Stage.CONFIRM;
		RouteInteraction confirmation = scanner.observePending(destination, ORIGIN, scene, 13);
		assertEquals(CharterShipPolicy.CONFIRM_ACTION, confirmation.getAction());
		assertTrue(confirmation.isReady());
	}

	@Test
	public void disappearingInterfaceWaitsForExactLanding()
	{
		CharterShipRouteScanner scanner = new CharterShipRouteScanner();
		MutableScene scene = new MutableScene(CharterShip.Stage.DESTINATION);
		RouteInteraction pending = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);
		scene.visible = false;

		RouteInteraction waiting = scanner.observePending(pending, ORIGIN, scene, 13);
		RouteInteraction landed = scanner.observePending(pending, DESTINATION, scene, 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, waiting.getStatus());
		assertFalse(waiting.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION,
			RouteEdge.Kind.CHARTER_SHIP);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static CharterShip charter(CharterShip.Stage stage)
	{
		return new CharterShip(ORIGIN, DESTINATION, 9318, "Trader Crewmember",
			"Charter", "Catherby", ORIGIN, stage);
	}

	private static final class MutableScene implements CharterShipScene
	{
		private CharterShip.Stage stage;
		private boolean visible = true;

		private MutableScene(CharterShip.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public CharterShip find(PlannedEdge edge)
		{
			return visible ? charter(stage) : null;
		}

		@Override
		public CharterShip observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? charter(stage) : null;
		}
	}
}
