package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CanoeTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CanoeRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3132, 3510, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(3141, 3796, 0);
	private static final int OBJECT_ID = 12166;

	@Test
	public void ownsEveryObjectUiWarningAndArrivalStageUntilDirectedLanding()
	{
		CanoeRouteScanner scanner = new CanoeRouteScanner();
		MutableScene scene = new MutableScene(CanoeTransport.Stage.OBJECT, "Chop-down");
		RouteInteraction current = scanner.scan(plan(RouteEdge.Kind.CANOE), 0, 1,
			ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.CANOE, current.getKind());
		assertEquals("Chop-down", current.getAction());
		assertTrue(current.isReady());

		current = advance(scanner, current, scene, CanoeTransport.Stage.OBJECT, "Shape-Canoe");
		current = advance(scanner, current, scene, CanoeTransport.Stage.SHAPE,
			CanoePolicy.shapeAction("Waka canoe"));
		scene.visible = false;
		RouteInteraction shaping = scanner.observePending(current, ORIGIN, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, shaping.getStatus());
		assertFalse(shaping.isReady());

		scene.visible = true;
		current = advance(scanner, current, scene, CanoeTransport.Stage.OBJECT, "Float Canoe");
		current = advance(scanner, current, scene, CanoeTransport.Stage.OBJECT, "Paddle Canoe");
		current = advance(scanner, current, scene, CanoeTransport.Stage.DESTINATION,
			CanoePolicy.destinationAction("Wilderness Pond"));
		current = advance(scanner, current, scene, CanoeTransport.Stage.WARNING,
			CanoePolicy.WARNING_CONTINUE_ACTION);
		current = advance(scanner, current, scene, CanoeTransport.Stage.CONFIRM,
			CanoePolicy.WARNING_CONFIRM_ACTION);
		current = advance(scanner, current, scene, CanoeTransport.Stage.ARRIVAL,
			"canoe-arrival:You arrive in the Wilderness. There are no trees suitable to make a canoe.");
		current = advance(scanner, current, scene, CanoeTransport.Stage.ARRIVAL,
			"canoe-arrival:Looks like you're walking back.");

		scene.visible = false;
		RouteInteraction landed = scanner.observePending(current, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void landingDoesNotClearWhileExactArrivalDialogueRemains()
	{
		CanoeRouteScanner scanner = new CanoeRouteScanner();
		MutableScene scene = new MutableScene(CanoeTransport.Stage.ARRIVAL,
			"canoe-arrival:Your canoe sinks into the water after the hard journey.");
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.CANOE), 0, 1,
			ORIGIN, scene, 13);

		RouteInteraction arrival = scanner.observePending(pending, DESTINATION, scene, 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, arrival.getStatus());
		assertEquals(CanoeTransport.Stage.ARRIVAL, scene.stage);
		assertTrue(arrival.isReady());
	}

	@Test
	public void unavailableShapeOrDestinationRequestsReplan()
	{
		CanoeRouteScanner scanner = new CanoeRouteScanner();
		MutableScene scene = new MutableScene(CanoeTransport.Stage.OBJECT, "Shape-Canoe");
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.CANOE), 0, 1,
			ORIGIN, scene, 13);

		scene.stage = CanoeTransport.Stage.SHAPE_UNAVAILABLE;
		scene.action = "canoe-shape-unavailable";
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());

		scene.stage = CanoeTransport.Stage.DESTINATION_UNAVAILABLE;
		scene.action = CanoePolicy.destinationAction("Wilderness Pond");
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void changedCatalogObjectIdentityIsUnavailable()
	{
		CanoeRouteScanner scanner = new CanoeRouteScanner();
		MutableScene scene = new MutableScene(CanoeTransport.Stage.OBJECT, "Chop-down");
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.CANOE), 0, 1,
			ORIGIN, scene, 13);
		scene.objectId++;

		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new CanoeRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT), 0, 1,
			ORIGIN, new MutableScene(CanoeTransport.Stage.OBJECT, "Chop-down"), 13));
	}

	private static RouteInteraction advance(CanoeRouteScanner scanner,
		RouteInteraction pending, MutableScene scene, CanoeTransport.Stage stage, String action)
	{
		scene.stage = stage;
		scene.action = action;
		RouteInteraction next = scanner.observePending(pending, ORIGIN, scene, 13);
		assertEquals(action, next.getAction());
		assertTrue(next.isReady());
		return next;
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements CanoeScene
	{
		private CanoeTransport.Stage stage;
		private String action;
		private boolean visible = true;
		private int objectId = OBJECT_ID;

		private MutableScene(CanoeTransport.Stage stage, String action)
		{
			this.stage = stage;
			this.action = action;
		}

		@Override
		public CanoeTransport find(PlannedEdge edge)
		{
			return visible ? canoe() : null;
		}

		@Override
		public CanoeTransport observe(PlannedEdge edge, String pendingAction,
			int catalogObjectId)
		{
			return visible ? canoe() : null;
		}

		private CanoeTransport canoe()
		{
			return new CanoeTransport(null, ORIGIN, objectId, action, ORIGIN,
				DESTINATION, stage);
		}
	}
}
