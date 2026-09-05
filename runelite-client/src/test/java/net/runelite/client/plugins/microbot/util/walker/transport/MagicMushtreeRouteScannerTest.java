package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MagicMushtreeTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MagicMushtreeRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3764, 3879, 1);
	private static final WorldPoint DESTINATION = new WorldPoint(3676, 3755, 0);

	@Test
	public void advancesObjectAndDestinationThenRetainsDirectedLanding()
	{
		MagicMushtreeRouteScanner scanner = new MagicMushtreeRouteScanner();
		MutableScene scene = new MutableScene(MagicMushtreeTransport.Stage.OBJECT);
		RouteInteraction object = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.MAGIC_MUSHTREE, object.getKind());
		assertEquals("Use", object.getAction());
		assertTrue(object.isReady());

		scene.stage = MagicMushtreeTransport.Stage.DESTINATION;
		RouteInteraction destination = scanner.observePending(object, ORIGIN, scene, 13);
		assertEquals(MagicMushtreePolicy.destinationAction("Sticky Swamp"),
			destination.getAction());
		assertTrue(destination.isReady());

		scene.visible = false;
		RouteInteraction travelling = scanner.observePending(destination,
			new WorldPoint(3700, 3800, 0), scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(travelling, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void missingDestinationIsUnavailableAndNeverReady()
	{
		MagicMushtreeRouteScanner scanner = new MagicMushtreeRouteScanner();
		MutableScene scene = new MutableScene(MagicMushtreeTransport.Stage.OBJECT);
		RouteInteraction object = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		scene.stage = MagicMushtreeTransport.Stage.DESTINATION_UNAVAILABLE;
		RouteInteraction unavailable = scanner.observePending(object, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Status.UNAVAILABLE, unavailable.getStatus());
		assertFalse(unavailable.isReady());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION,
			RouteEdge.Kind.MAGIC_MUSHTREE);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static MagicMushtreeTransport mushtree(MagicMushtreeTransport.Stage stage)
	{
		return new MagicMushtreeTransport(ORIGIN, DESTINATION, 30920, "Use",
			"Sticky Swamp", ORIGIN, stage);
	}

	private static final class MutableScene implements MagicMushtreeScene
	{
		private MagicMushtreeTransport.Stage stage;
		private boolean visible = true;

		private MutableScene(MagicMushtreeTransport.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public MagicMushtreeTransport find(PlannedEdge edge)
		{
			return visible ? mushtree(stage) : null;
		}

		@Override
		public MagicMushtreeTransport observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? mushtree(stage) : null;
		}
	}
}
