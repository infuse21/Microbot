package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.Quetzal;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuetzalRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(1697, 3140, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(1437, 3171, 0);

	@Test
	public void advancesNpcAndDestinationThenWaitsForDirectedLanding()
	{
		QuetzalRouteScanner scanner = new QuetzalRouteScanner();
		MutableScene scene = new MutableScene(Quetzal.Stage.NPC);
		RouteInteraction npc = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.QUETZAL, npc.getKind());
		assertEquals("Travel", npc.getAction());
		assertTrue(npc.isReady());

		scene.stage = Quetzal.Stage.DESTINATION;
		RouteInteraction destination = scanner.observePending(npc, ORIGIN, scene, 13);
		assertEquals(QuetzalPolicy.destinationAction("The Teomat"),
			destination.getAction());
		assertTrue(destination.isReady());

		scene.visible = false;
		RouteInteraction travelling = scanner.observePending(destination,
			new WorldPoint(1500, 3000, 0), scene, 13);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(travelling, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void changedRenuVariantBeforeMapOpensRemainsAvailable()
	{
		QuetzalRouteScanner scanner = new QuetzalRouteScanner();
		MutableScene scene = new MutableScene(Quetzal.Stage.NPC);
		RouteInteraction npc = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13);
		scene.npcId = 13353;

		RouteInteraction changed = scanner.observePending(npc, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Status.AVAILABLE, changed.getStatus());
		assertTrue(changed.isReady());
		assertEquals(13353, changed.getObjectId());
	}

	private static RoutePlan plan()
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION, RouteEdge.Kind.QUETZAL);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static Quetzal quetzal(Quetzal.Stage stage, int npcId)
	{
		return new Quetzal(ORIGIN, DESTINATION, npcId, "The Teomat", ORIGIN, stage);
	}

	private static final class MutableScene implements QuetzalScene
	{
		private Quetzal.Stage stage;
		private int npcId = 13350;
		private boolean visible = true;

		private MutableScene(Quetzal.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public Quetzal find(PlannedEdge edge)
		{
			return visible ? quetzal(stage, npcId) : null;
		}

		@Override
		public Quetzal observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? quetzal(stage, npcId) : null;
		}
	}
}
