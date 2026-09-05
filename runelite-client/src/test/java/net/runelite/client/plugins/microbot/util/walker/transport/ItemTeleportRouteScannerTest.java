package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationDecision;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngine;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationObservation;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRequest;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRouteOptions;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ItemTeleportRouteScannerTest
{
	private static final WorldPoint FROM = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TO = new WorldPoint(2900, 3500, 0);

	@Test
	public void opensTabUsesItemOnceAndRetainsOwnershipThroughChargeDisappearance()
	{
		for (boolean equipped : new boolean[]{false, true})
		{
			ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
			RoutePlan plan = plan(RouteEdge.Kind.ITEM_TELEPORT);
			assertTrue(plan.isEngineSupported());
			RouteInteraction open = scanner.scan(plan, 0, 1,
				edge -> new ItemTeleport(3853, "Burthorpe", equipped, false));
			assertTrue(open.getAction().startsWith("item-open:"));
			RouteInteraction use = scanner.observePending(open, FROM,
				edge -> new ItemTeleport(3853, "Burthorpe", equipped, true));
			assertTrue(use.getAction().startsWith("item-use:"));
			NavigationEngine engine = new NavigationEngine();
			engine.start(new NavigationRequest(1, Collections.singleton(TO), 0,
				NavigationRouteOptions.defaults(), "item-test"));
			NavigationDecision first = engine.observe(observation(1, FROM, plan, open));
			assertEquals(NavigationDecision.Type.INTERACT, first.getType());
			engine.recordCommandResult(first, true, 1);
			NavigationDecision second = engine.observe(observation(2, FROM, plan, use));
			assertEquals(NavigationDecision.Type.INTERACT, second.getType());
			engine.recordCommandResult(second, true, 2);
			RouteInteraction waiting = scanner.observePending(use, FROM, edge -> null);
			assertEquals(use.getAction(), waiting.getAction());
			assertEquals(NavigationDecision.Type.WAIT,
				engine.observe(observation(3, FROM, plan, waiting)).getType());
			WorldPoint nearLanding = new WorldPoint(TO.getX() + 10, TO.getY(), 0);
			assertEquals("interaction-command-in-flight",
				engine.observe(observation(4, nearLanding, plan, waiting)).getReason());
			RouteInteraction landed = scanner.observePending(use, TO, edge -> null);
			assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
			assertEquals("interaction-edge-crossed",
				engine.observe(observation(5, TO, plan, landed)).getReason());
			assertEquals(NavigationDecision.Type.COMPLETE,
				engine.observe(observation(6, TO, plan, null)).getType());
		}
	}

	@Test
	public void wrongPlaneAndSourceDisappearanceDoNotAcknowledgeTeleport()
	{
		ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		RouteInteraction open = scanner.scan(plan(RouteEdge.Kind.ITEM_TELEPORT), 0, 1,
			edge -> new ItemTeleport(3853, "Burthorpe", false, false));
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(open, FROM, edge -> null).getStatus());
		RouteInteraction use = scanner.observePending(open, FROM,
			edge -> new ItemTeleport(3853, "Burthorpe", false, true));
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(use, new WorldPoint(TO.getX(), TO.getY(), 1), edge -> null).getStatus());
		assertNull(scanner.scan(plan(RouteEdge.Kind.TRANSPORT), 0, 1,
			edge -> new ItemTeleport(3853, "Burthorpe", false, true)));
	}

	@Test
	public void masterScrollBookOpensTabThenBookThenExactDestination()
	{
		ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		RoutePlan plan = plan(RouteEdge.Kind.ITEM_TELEPORT);
		RouteInteraction openTab = scanner.scan(plan, 0, 1,
			edge -> new ItemTeleport(21389, "Open", false, false));
		assertEquals("item-open:inventory:Open", openTab.getAction());

		RouteInteraction openBook = scanner.observePending(openTab, FROM,
			edge -> new ItemTeleport(21389, "Open", "item-prepare:inventory:Open"));
		assertEquals("item-prepare:inventory:Open", openBook.getAction());
		RouteInteraction select = scanner.observePending(openBook, FROM,
			edge -> new ItemTeleport(21389, "Nardah", "book-select:Nardah"));
		assertEquals("book-select:Nardah", select.getAction());

		RouteInteraction waiting = scanner.observePending(select, FROM, edge -> null);
		assertEquals(RouteInteraction.Status.AVAILABLE, waiting.getStatus());
		assertEquals("book-select:Nardah", waiting.getAction());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(waiting, TO, edge -> null).getStatus());
	}

	@Test
	public void revenantBookSelectionAdvancesToExactConfirmation()
	{
		ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		RouteInteraction select = scanner.scan(plan(RouteEdge.Kind.ITEM_TELEPORT), 0, 1,
			edge -> new ItemTeleport(21389, "Revenant cave", "book-select:Revenant cave"));
		RouteInteraction confirm = scanner.observePending(select, FROM,
			edge -> new ItemTeleport(21389, "Yes, teleport me now",
				"book-confirm:Yes, teleport me now"));

		assertEquals("book-confirm:Yes, teleport me now", confirm.getAction());
		assertEquals(RouteInteraction.Status.AVAILABLE,
			scanner.observePending(confirm, FROM, edge -> null).getStatus());
	}

	@Test
	public void offCentreLandingFinishesTheExactTargetWithoutAnotherTeleport()
	{
		ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		RoutePlan plan = plan(RouteEdge.Kind.ITEM_TELEPORT);
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Collections.singleton(TO), 0,
			NavigationRouteOptions.defaults(), "exact-landing-test"));
		RouteInteraction use = scanner.scan(plan, 0, 1,
			edge -> new ItemTeleport(2566, "Castle Wars", true, true));
		NavigationDecision input = engine.observe(observation(1, FROM, plan, use));
		engine.recordCommandResult(input, true, 1);
		WorldPoint landing = new WorldPoint(TO.getX() + 1, TO.getY() + 1, TO.getPlane());
		RouteInteraction cleared = scanner.observePending(use, landing, edge -> null);
		assertEquals("interaction-edge-crossed",
			engine.observe(observation(2, landing, plan, cleared)).getReason());
		NavigationDecision finish = engine.observe(observation(3, landing, plan, null));
		assertEquals(NavigationDecision.Type.CLICK_TILE, finish.getType());
		assertEquals(TO, finish.getTarget());
		assertEquals("route-end-approach", finish.getTargetSelection());
		engine.recordCommandResult(finish, true, 3);
		assertEquals(NavigationDecision.Type.COMPLETE,
			engine.observe(observation(4, TO, plan, null)).getType());
	}

	private static NavigationObservation observation(long time, WorldPoint player, RoutePlan plan,
		RouteInteraction interaction)
	{
		return NavigationObservation.route(time, player, plan, false, false, false, false,
			false, false, null, "item-test").withRouteInteraction(interaction);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, FROM, Collections.singleton(TO), Arrays.asList(FROM, TO),
			Arrays.asList(FROM, TO), true, Collections.singletonList(new RouteEdge(0, FROM, TO, kind)));
	}
}
