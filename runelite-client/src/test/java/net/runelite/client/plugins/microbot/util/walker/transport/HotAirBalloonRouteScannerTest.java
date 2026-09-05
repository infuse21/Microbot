package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.HotAirBalloonTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HotAirBalloonRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2939, 3423, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(3299, 3482, 0);

	@Test
	public void advancesBasketAndDestinationThenRetainsDirectedLanding()
	{
		HotAirBalloonRouteScanner scanner = new HotAirBalloonRouteScanner();
		MutableScene scene = new MutableScene(HotAirBalloonTransport.Stage.OBJECT);
		RouteInteraction basket = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.HOT_AIR_BALLOON, basket.getKind());
		assertEquals("Use", basket.getAction());
		assertEquals(19133, basket.getObjectId());
		assertTrue(basket.isReady());

		scene.stage = HotAirBalloonTransport.Stage.DESTINATION;
		scene.objectId = 19129;
		RouteInteraction destination = scanner.observePending(basket, ORIGIN, scene, 13);
		assertEquals(HotAirBalloonPolicy.destinationAction("Varrock"),
			destination.getAction());
		assertTrue(destination.isReady());

		scene.visible = false;
		RouteInteraction travelling = scanner.observePending(destination,
			new WorldPoint(3100, 3450, 0), scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(travelling, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void missingDestinationButtonIsUnavailableAndNeverReady()
	{
		HotAirBalloonRouteScanner scanner = new HotAirBalloonRouteScanner();
		MutableScene scene = new MutableScene(HotAirBalloonTransport.Stage.OBJECT);
		RouteInteraction basket = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		scene.stage = HotAirBalloonTransport.Stage.DESTINATION_UNAVAILABLE;
		RouteInteraction unavailable = scanner.observePending(basket, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Status.UNAVAILABLE, unavailable.getStatus());
		assertFalse(unavailable.isReady());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION,
			RouteEdge.Kind.HOT_AIR_BALLOON);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static HotAirBalloonTransport balloon(HotAirBalloonTransport.Stage stage,
		int objectId)
	{
		return new HotAirBalloonTransport(ORIGIN, DESTINATION, objectId, "Use",
			"Varrock", ORIGIN, stage);
	}

	private static final class MutableScene implements HotAirBalloonScene
	{
		private HotAirBalloonTransport.Stage stage;
		private boolean visible = true;
		private int objectId = 19133;

		private MutableScene(HotAirBalloonTransport.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public HotAirBalloonTransport find(PlannedEdge edge)
		{
			return visible ? balloon(stage, objectId) : null;
		}

		@Override
		public HotAirBalloonTransport observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? balloon(stage, objectId) : null;
		}
	}
}
