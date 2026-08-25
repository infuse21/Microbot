package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SpiritTree;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpiritTreeRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3185, 3508, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2461, 3444, 0);

	@Test
	public void advancesObjectAndDestinationThenWaitsForDirectedLanding()
	{
		SpiritTreeRouteScanner scanner = new SpiritTreeRouteScanner();
		MutableScene scene = new MutableScene(SpiritTree.Stage.OBJECT);
		RouteInteraction object = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.SPIRIT_TREE, object.getKind());
		assertEquals("Travel", object.getAction());
		assertTrue(object.isReady());

		scene.stage = SpiritTree.Stage.DESTINATION;
		RouteInteraction destination = scanner.observePending(object, ORIGIN, scene, 13);
		assertEquals(SpiritTreePolicy.destinationAction("Gnome Stronghold"),
			destination.getAction());
		assertTrue(destination.isReady());

		scene.visible = false;
		RouteInteraction travelling = scanner.observePending(destination,
			new WorldPoint(500, 500, 1), scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(travelling, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION, RouteEdge.Kind.SPIRIT_TREE);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static SpiritTree tree(SpiritTree.Stage stage)
	{
		return new SpiritTree(ORIGIN, DESTINATION, 1295, "Travel",
			"Gnome Stronghold", ORIGIN, stage);
	}

	private static final class MutableScene implements SpiritTreeScene
	{
		private SpiritTree.Stage stage;
		private boolean visible = true;

		private MutableScene(SpiritTree.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public SpiritTree find(PlannedEdge edge)
		{
			return visible ? tree(stage) : null;
		}

		@Override
		public SpiritTree observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? tree(stage) : null;
		}
	}
}
