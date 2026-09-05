package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.WildernessDitch;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WildernessDitchRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2995, 3530, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2998, 3530, 0);
	private static final int OBJECT_ID = 23271;

	@Test
	public void advancesFromObjectThroughOptionalWarningToDirectedLanding()
	{
		WildernessDitchRouteScanner scanner = new WildernessDitchRouteScanner();
		MutableScene scene = new MutableScene(WildernessDitch.Stage.OBJECT);
		RouteInteraction object = scanner.scan(plan(RouteEdge.Kind.WILDERNESS_DITCH),
			0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.WILDERNESS_DITCH, object.getKind());
		assertEquals("Cross", object.getAction());
		assertTrue(object.isReady());

		scene.stage = WildernessDitch.Stage.WARNING;
		RouteInteraction warning = scanner.observePending(object, ORIGIN, scene, 13);
		assertEquals(WildernessDitchPolicy.WARNING_ACTION, warning.getAction());
		assertTrue(warning.isReady());

		scene.visible = false;
		RouteInteraction waiting = scanner.observePending(warning, ORIGIN, scene, 13);
		RouteInteraction landed = scanner.observePending(warning, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, waiting.getStatus());
		assertFalse(waiting.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void directCrossingObjectDisappearanceWaitsForLanding()
	{
		WildernessDitchRouteScanner scanner = new WildernessDitchRouteScanner();
		MutableScene scene = new MutableScene(WildernessDitch.Stage.OBJECT);
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.WILDERNESS_DITCH),
			0, 1, ORIGIN, scene, 13);
		scene.visible = false;

		RouteInteraction waiting = scanner.observePending(pending, ORIGIN, scene, 13);
		RouteInteraction landed = scanner.observePending(pending, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, waiting.getStatus());
		assertFalse(waiting.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void changedCatalogObjectIdentityIsUnavailable()
	{
		WildernessDitchRouteScanner scanner = new WildernessDitchRouteScanner();
		MutableScene scene = new MutableScene(WildernessDitch.Stage.OBJECT);
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.WILDERNESS_DITCH),
			0, 1, ORIGIN, scene, 13);
		scene.objectId = OBJECT_ID + 1;

		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new WildernessDitchRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, ORIGIN, new MutableScene(WildernessDitch.Stage.OBJECT), 13));
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements WildernessDitchScene
	{
		private WildernessDitch.Stage stage;
		private boolean visible = true;
		private int objectId = OBJECT_ID;

		private MutableScene(WildernessDitch.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public WildernessDitch find(PlannedEdge edge)
		{
			return visible ? ditch() : null;
		}

		@Override
		public WildernessDitch observe(PlannedEdge edge, String pendingAction,
			int catalogObjectId)
		{
			return visible ? ditch() : null;
		}

		private WildernessDitch ditch()
		{
			String action = stage == WildernessDitch.Stage.WARNING
				? WildernessDitchPolicy.WARNING_ACTION : "Cross";
			return new WildernessDitch(null, ORIGIN, objectId, action,
				ORIGIN, DESTINATION, stage);
		}
	}
}
