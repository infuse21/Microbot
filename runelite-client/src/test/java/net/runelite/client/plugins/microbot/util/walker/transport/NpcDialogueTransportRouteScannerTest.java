package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.NpcDialogueTransport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NpcDialogueTransportRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(1342, 3645, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(1408, 3612, 0);
	private static final long COINS = 1_000L;

	@Test
	public void progressesFromActorThroughContinueToDestinationStage()
	{
		NpcDialogueTransportRouteScanner scanner = new NpcDialogueTransportRouteScanner();
		MutableScene scene = new MutableScene(NpcDialogueTransport.Stage.ACTOR);
		RouteInteraction actor = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13, COINS);

		assertEquals(RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT, actor.getKind());
		assertEquals("Board", actor.getAction());
		assertEquals(33614, actor.getObjectId());
		assertTrue(actor.isReady());

		scene.stage = NpcDialogueTransport.Stage.CONTINUE;
		RouteInteraction cont = scanner.observePending(actor, ORIGIN, scene, 13, COINS);
		assertEquals(NpcDialogueTransportPolicy.CONTINUE_ACTION, cont.getAction());
		assertTrue(cont.isReady());

		scene.stage = NpcDialogueTransport.Stage.DESTINATION;
		RouteInteraction destination = scanner.observePending(cont, ORIGIN, scene, 13, COINS);
		assertEquals(NpcDialogueTransportPolicy.destinationAction("Shayzien"),
			destination.getAction());
		assertTrue(destination.isReady());
	}

	@Test
	public void paidRowConfirmsFareAndMissingFareIsUnavailable()
	{
		NpcDialogueTransportRouteScanner scanner = new NpcDialogueTransportRouteScanner();
		MutableScene scene = new MutableScene(NpcDialogueTransport.Stage.ACTOR);
		scene.fareCoins = 30;

		RouteInteraction funded = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13, 30L);
		assertEquals(RouteInteraction.Status.AVAILABLE, funded.getStatus());

		RouteInteraction broke = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13, 29L);
		assertEquals(RouteInteraction.Status.UNAVAILABLE, broke.getStatus());
		assertFalse(broke.isReady());

		scene.stage = NpcDialogueTransport.Stage.CONFIRM;
		RouteInteraction confirm = scanner.observePending(funded, ORIGIN, scene, 13, 30L);
		assertEquals(NpcDialogueTransportPolicy.CONFIRM_ACTION, confirm.getAction());
		assertTrue(confirm.isReady());
	}

	@Test
	public void openDialogueStageIsNotFareGated()
	{
		NpcDialogueTransportRouteScanner scanner = new NpcDialogueTransportRouteScanner();
		MutableScene scene = new MutableScene(NpcDialogueTransport.Stage.CONFIRM);
		scene.fareCoins = 30;
		RouteInteraction pending = scanner.scan(plan(), 0, 1, ORIGIN,
			new MutableScene(NpcDialogueTransport.Stage.ACTOR), 13, COINS);

		RouteInteraction confirm = scanner.observePending(pending, ORIGIN, scene, 13, 0L);

		assertEquals(RouteInteraction.Status.AVAILABLE, confirm.getStatus());
		assertEquals(NpcDialogueTransportPolicy.CONFIRM_ACTION, confirm.getAction());
	}

	@Test
	public void closedDialogueWaitsForExactLanding()
	{
		NpcDialogueTransportRouteScanner scanner = new NpcDialogueTransportRouteScanner();
		MutableScene scene = new MutableScene(NpcDialogueTransport.Stage.DESTINATION);
		RouteInteraction pending = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13, COINS);
		scene.visible = false;

		RouteInteraction waiting = scanner.observePending(pending, ORIGIN, scene, 13, COINS);
		RouteInteraction landed = scanner.observePending(pending, DESTINATION, scene, 13, COINS);

		assertEquals(RouteInteraction.Status.AVAILABLE, waiting.getStatus());
		assertFalse(waiting.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void actorIdentityMismatchIsUnavailable()
	{
		NpcDialogueTransportRouteScanner scanner = new NpcDialogueTransportRouteScanner();
		MutableScene scene = new MutableScene(NpcDialogueTransport.Stage.ACTOR);
		RouteInteraction pending = scanner.scan(plan(), 0, 1, ORIGIN, scene, 13, COINS);
		scene.actorId = 999;

		RouteInteraction observed = scanner.observePending(pending, ORIGIN, scene, 13, COINS);

		assertEquals(RouteInteraction.Status.UNAVAILABLE, observed.getStatus());
	}

	@Test
	public void distantActorIsNotReady()
	{
		NpcDialogueTransportRouteScanner scanner = new NpcDialogueTransportRouteScanner();
		MutableScene scene = new MutableScene(NpcDialogueTransport.Stage.ACTOR);

		RouteInteraction actor = scanner.scan(plan(), 0, 1,
			new WorldPoint(ORIGIN.getX() + 20, ORIGIN.getY(), 0), scene, 13, COINS);

		assertFalse(actor.isReady());
	}

	@Test
	public void unsupportedEdgeIsIgnored()
	{
		assertNull(new NpcDialogueTransportRouteScanner().scan(
			plan(RouteEdge.Kind.NPC_TRANSPORT), 0, 1, ORIGIN,
			new MutableScene(NpcDialogueTransport.Stage.ACTOR), 13, COINS));
	}

	private static RoutePlan plan()
	{
		return plan(RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		RouteEdge edge = new RouteEdge(0, ORIGIN, DESTINATION, kind);
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION),
			true, Collections.singletonList(edge));
	}

	private static final class MutableScene implements NpcDialogueTransportScene
	{
		private NpcDialogueTransport.Stage stage;
		private boolean visible = true;
		private int actorId = 33614;
		private int fareCoins;

		private MutableScene(NpcDialogueTransport.Stage stage)
		{
			this.stage = stage;
		}

		@Override
		public NpcDialogueTransport find(PlannedEdge edge)
		{
			return transport();
		}

		@Override
		public NpcDialogueTransport observe(PlannedEdge edge, String pendingAction)
		{
			return visible ? transport() : null;
		}

		private NpcDialogueTransport transport()
		{
			return new NpcDialogueTransport(ORIGIN, DESTINATION, actorId, "Boaty",
				"Board", "Shayzien", fareCoins, ORIGIN, stage);
		}
	}
}
