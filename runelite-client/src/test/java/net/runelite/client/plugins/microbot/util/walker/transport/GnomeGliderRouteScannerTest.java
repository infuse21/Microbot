package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.GnomeGlider;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GnomeGliderRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2465, 3501, 3);
	private static final WorldPoint DESTINATION = new WorldPoint(3284, 3210, 0);

	@Test
	public void advancesNpcAndDestinationThenWaitsForDirectedLanding()
	{
		GnomeGliderRouteScanner scanner = new GnomeGliderRouteScanner();
		MutableScene scene = new MutableScene(GnomeGlider.Stage.NPC);
		RouteInteraction npc = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.GNOME_GLIDER, npc.getKind());
		assertEquals("Glider", npc.getAction());
		assertTrue(npc.isReady());

		scene.stage = GnomeGlider.Stage.DESTINATION;
		RouteInteraction destination = scanner.observePending(npc, ORIGIN, scene, 13);
		assertEquals(GnomeGliderPolicy.destinationAction("Kar-Hewo"),
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

	@Test
	public void hiddenDestinationIsUnavailableAndNeverReadyToClick()
	{
		GnomeGliderRouteScanner scanner = new GnomeGliderRouteScanner();
		MutableScene scene = new MutableScene(GnomeGlider.Stage.NPC);
		RouteInteraction npc = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		scene.stage = GnomeGlider.Stage.DESTINATION_UNAVAILABLE;
		RouteInteraction unavailable = scanner.observePending(npc, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Status.UNAVAILABLE, unavailable.getStatus());
		assertEquals(GnomeGliderPolicy.destinationAction("Kar-Hewo"),
			unavailable.getAction());
		assertFalse(unavailable.isReady());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION,
			RouteEdge.Kind.GNOME_GLIDER);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static GnomeGlider glider(GnomeGlider.Stage stage)
	{
		return new GnomeGlider(ORIGIN, DESTINATION, 10467, "Glider",
			"Kar-Hewo", ORIGIN, stage);
	}

	private static final class MutableScene implements GnomeGliderScene
	{
		private GnomeGlider.Stage stage;
		private boolean visible = true;

		private MutableScene(GnomeGlider.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public GnomeGlider find(PlannedEdge edge)
		{
			return visible ? glider(stage) : null;
		}

		@Override
		public GnomeGlider observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? glider(stage) : null;
		}
	}
}
