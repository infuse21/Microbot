package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationLever;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TeleportationLeverRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3090, 3475, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(3154, 3924, 0);
	private static final int OBJECT_ID = 26761;

	@Test
	public void progressesFromObjectThroughWildernessWarningToLanding()
	{
		TeleportationLeverRouteScanner scanner = new TeleportationLeverRouteScanner();
		MutableScene scene = new MutableScene(TeleportationLever.Stage.OBJECT);
		RouteInteraction object = scanner.scan(plan(RouteEdge.Kind.TELEPORTATION_LEVER),
			0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.TELEPORTATION_LEVER, object.getKind());
		assertEquals("Pull", object.getAction());
		assertTrue(object.isReady());

		scene.stage = TeleportationLever.Stage.WARNING;
		RouteInteraction warning = scanner.observePending(object, ORIGIN, scene, 13);
		assertEquals(TeleportationLeverPolicy.WARNING_CONTINUE_ACTION,
			warning.getAction());
		assertTrue(warning.isReady());

		scene.stage = TeleportationLever.Stage.CONFIRM;
		RouteInteraction confirm = scanner.observePending(warning, ORIGIN, scene, 13);
		assertEquals(TeleportationLeverPolicy.CONFIRM_ACTION, confirm.getAction());
		assertTrue(confirm.isReady());

		scene.visible = false;
		RouteInteraction transit = scanner.observePending(confirm, ORIGIN, scene, 13);
		RouteInteraction landed = scanner.observePending(confirm, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, transit.getStatus());
		assertFalse(transit.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void directLeverObjectDisappearanceWaitsForDirectedLanding()
	{
		TeleportationLeverRouteScanner scanner = new TeleportationLeverRouteScanner();
		MutableScene scene = new MutableScene(TeleportationLever.Stage.OBJECT);
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.TELEPORTATION_LEVER),
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
		TeleportationLeverRouteScanner scanner = new TeleportationLeverRouteScanner();
		MutableScene scene = new MutableScene(TeleportationLever.Stage.OBJECT);
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.TELEPORTATION_LEVER),
			0, 1, ORIGIN, scene, 13);
		scene.objectId = OBJECT_ID + 1;

		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new TeleportationLeverRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, ORIGIN, new MutableScene(TeleportationLever.Stage.OBJECT), 13));
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements TeleportationLeverScene
	{
		private TeleportationLever.Stage stage;
		private boolean visible = true;
		private int objectId = OBJECT_ID;

		private MutableScene(TeleportationLever.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public TeleportationLever find(PlannedEdge edge)
		{
			return visible ? lever() : null;
		}

		@Override
		public TeleportationLever observe(PlannedEdge edge, String pendingAction,
			int catalogObjectId)
		{
			return visible ? lever() : null;
		}

		private TeleportationLever lever()
		{
			String action;
			switch (stage)
			{
				case WARNING:
					action = TeleportationLeverPolicy.WARNING_CONTINUE_ACTION;
					break;
				case CONFIRM:
					action = TeleportationLeverPolicy.CONFIRM_ACTION;
					break;
				case OBJECT:
				default:
					action = "Pull";
					break;
			}
			return new TeleportationLever(null, ORIGIN, objectId, action,
				ORIGIN, DESTINATION, stage);
		}
	}
}
