package net.runelite.client.plugins.microbot.util.leaguetransport;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.*;
import net.runelite.client.plugins.microbot.util.walker.transport.ItemTeleportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2ItemTeleportScene;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ClueCompassOwnershipTest
{
	private static final WorldPoint FROM = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TO = new WorldPoint(3110, 3420, 0);
	private static final String ACTION = "b. barbarian village";

	@Test
	public void engineSeparatesOpeningFromExactInputAndRetainsTheVoyage()
	{
		try (Fixture f = new Fixture())
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			verify(f.client).runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
			f.microbot.verify(() -> Microbot.doInvoke(any(NewMenuEntry.class), any(Rectangle.class)), never());
			f.ready();
			RouteInteraction use = f.observe(open, FROM);
			f.issue(engine, use, 2);
			f.microbot.verify(() -> Microbot.doInvoke(argThat(entry -> entry.getItemId() == 30363
				&& entry.getParam0() == 3 && entry.getParam1() == ComponentID.INVENTORY_CONTAINER
				&& entry.getIdentifier() == 1 && ACTION.equals(entry.getOption())), eq(new Rectangle(10, 20, 30, 30))), times(1));
			f.inventory.verify(() -> Rs2Inventory.interact(anyInt(), anyString()), never());
			f.leagues.verify(() -> Rs2LeaguesTransport.recordTransportAttempt(f.row), times(1));
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(false);
			assertEquals(use.getAction(), f.observe(use, FROM).getAction());
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			assertEquals(NavigationDecision.Type.WAIT,
				engine.observe(observation(3, f.observe(use, FROM), FROM)).getType());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				f.observe(use, new WorldPoint(TO.getX(), TO.getY(), 1)).getStatus());
			RouteInteraction landed = f.observe(use, TO);
			assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
			assertEquals("interaction-edge-crossed", engine.observe(observation(4, landed, TO)).getReason());
			assertEquals(NavigationDecision.Type.COMPLETE, engine.observe(observation(5, null, TO)).getType());
		}
	}

	@Test
	public void exactSubmenuUsesItsActualWidgetParentIndex()
	{
		try (Fixture f = new Fixture())
		{
			f.ready();
			when(f.item.getInventoryActions()).thenReturn(new String[]{"Teleport", "Destroy"});
			when(f.item.getSubops()).thenReturn(new String[][]{{"Other destination", ACTION}, null});
			when(f.child.getActions()).thenReturn(new String[]{"Use", "Teleport", "Destroy"});
			assertTrue(f.scene.dispatch(f.scan()));
			f.microbot.verify(() -> Microbot.doInvoke(argThat(entry ->
				entry.getIdentifier() == NewMenuEntry.findIdentifier(2, 2)), any(Rectangle.class)), times(1));
		}
	}

	@Test
	public void absentPartialAndAmbiguousActionsNeverAuthorizeInput()
	{
		assertEquals(-1, Rs2ClueCompassScene.exactIdentifier(new String[]{ACTION + " north"}, null, null, ACTION));
		assertEquals(-1, Rs2ClueCompassScene.exactIdentifier(new String[]{ACTION, ACTION}, null, null, ACTION));
		assertEquals(-1, Rs2ClueCompassScene.exactIdentifier(new String[]{"Teleport"}, new String[]{"Teleport"},
			new String[][]{{ACTION, ACTION}}, ACTION));
		try (Fixture f = new Fixture())
		{
			f.ready();
			when(f.child.getActions()).thenReturn(new String[]{"Teleport", ACTION + " north"});
			RouteInteraction unavailable = f.scan();
			assertEquals(RouteInteraction.Status.UNAVAILABLE, unavailable.getStatus());
			assertFalse(f.scene.dispatch(unavailable));
			f.microbot.verify(() -> Microbot.doInvoke(any(NewMenuEntry.class), any(Rectangle.class)), never());
		}
	}

	@Test
	public void staleTabItemCatalogAndInterruptedCommandsCannotDispatch()
	{
		try (Fixture f = new Fixture())
		{
			f.ready();
			RouteInteraction use = f.scan();
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(false);
			assertFalse(f.scene.dispatch(use));
			f.ready();
			when(f.child.getItemId()).thenReturn(1);
			assertFalse(f.scene.dispatch(use));
			when(f.child.getItemId()).thenReturn(30363);
			Thread.currentThread().interrupt();
			try { assertFalse(f.scene.dispatch(use)); }
			finally { Thread.interrupted(); }
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			assertFalse(f.scene.dispatch(use));
			f.microbot.verify(() -> Microbot.doInvoke(any(NewMenuEntry.class), any(Rectangle.class)), never());
		}
	}

	@Test
	public void missingPreparationAcknowledgementReplansWithoutRetryingTheInput()
	{
		try (Fixture f = new Fixture())
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(3999, f.observe(open, FROM), FROM)).getType());
			NavigationDecision expired = engine.observe(observation(4001, f.observe(open, FROM), FROM));
			assertEquals(NavigationDecision.Type.REQUEST_REPLAN, expired.getType());
			assertEquals("compass-stage-not-acknowledged", expired.getReason());
			verify(f.client, times(1)).runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
		}
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Set.of(TO), 0,
			new NavigationRouteOptions(true, true, false, true), "compass-test"));
		return engine;
	}

	@Test
	public void missingLandingExpiresWithoutSendingAnotherCompassInput()
	{
		try (Fixture f = new Fixture())
		{
			f.ready();
			NavigationEngine engine = engine();
			RouteInteraction use = f.scan();
			f.issue(engine, use, 1);
			NavigationDecision expired = engine.observe(observation(60_001, f.observe(use, FROM), FROM));
			assertEquals(NavigationDecision.Type.REQUEST_REPLAN, expired.getType());
			assertEquals("compass-stage-not-acknowledged", expired.getReason());
			f.microbot.verify(() -> Microbot.doInvoke(any(NewMenuEntry.class), any(Rectangle.class)), times(1));
		}
	}

	@Test
	public void wrongRequirementVariantsDoNotBecomeStagedCompassRoutes()
	{
		assertFalse(Rs2ClueCompassTransport.isStagedRoute(new Transport(TO,
			"Clue compass: B. Barbarian Village", TransportType.SEASONAL_TRANSPORT,
			false, 20, Set.of(Set.of(33233)))));
		assertFalse(Rs2ClueCompassTransport.isStagedRoute(new Transport(TO,
			"Clue compass: B. Barbarian Village", TransportType.SEASONAL_TRANSPORT,
			false, 20, Set.of(Set.of(30363)), true)));
	}

	private static RoutePlan plan()
	{
		return new RoutePlan(1, 1, FROM, Set.of(TO), List.of(FROM, TO), List.of(FROM, TO), true,
			List.of(new RouteEdge(0, FROM, TO, RouteEdge.Kind.ITEM_TELEPORT)));
	}

	private static NavigationObservation observation(long time, RouteInteraction interaction, WorldPoint player)
	{
		return NavigationObservation.route(time, player, plan(), false, false, false, false,
			false, false, null, "compass-test").withRouteInteraction(interaction);
	}

	private static final class Fixture implements AutoCloseable
	{
		final Client client = mock(Client.class);
		final Widget child = mock(Widget.class);
		final Rs2ItemModel item = mock(Rs2ItemModel.class);
		final Transport row = new Transport(TO, "Clue compass: B. Barbarian Village",
			TransportType.SEASONAL_TRANSPORT, false, 20, Set.of(Set.of(30363)));
		final Rs2ItemTeleportScene scene = new Rs2ItemTeleportScene();
		final ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		final MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
		final MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
		final MockedStatic<Rs2Tab> tab = mockStatic(Rs2Tab.class);
		final MockedStatic<Rs2PathApi> paths = mockStatic(Rs2PathApi.class);
		final MockedStatic<Rs2LeaguesTransport> leagues = mockStatic(Rs2LeaguesTransport.class);
		final MockedStatic<PohPanel> poh = mockStatic(PohPanel.class);

		Fixture()
		{
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			paths.when(Rs2PathApi::getTransports).thenReturn(Map.of(FROM, Set.of(row)));
			inventory.when(() -> Rs2Inventory.get(30363)).thenReturn(item);
			when(item.getSlot()).thenReturn(3);
			when(item.getInventoryActions()).thenReturn(new String[]{ACTION, "Destroy"});
			when(child.getIndex()).thenReturn(3);
			when(child.getItemId()).thenReturn(30363);
			when(child.getBounds()).thenReturn(new Rectangle(10, 20, 30, 30));
			Widget root = mock(Widget.class);
			when(root.getChildren()).thenReturn(new Widget[]{child});
			when(client.getWidget(ComponentID.INVENTORY_CONTAINER)).thenReturn(root);
		}

		void ready() { tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(true); }
		RouteInteraction scan() { return scanner.scan(plan(), 0, 1, scene); }
		RouteInteraction observe(RouteInteraction pending, WorldPoint player) { return scanner.observePending(pending, player, scene); }
		void issue(NavigationEngine engine, RouteInteraction stage, long time)
		{
			NavigationDecision decision = engine.observe(observation(time, stage, FROM));
			assertEquals(NavigationDecision.Type.INTERACT, decision.getType());
			assertTrue(scene.dispatch(decision.getInteraction()));
			engine.recordCommandResult(decision, true, time);
		}
		@Override
		public void close()
		{
			poh.close(); leagues.close(); paths.close(); tab.close(); inventory.close(); microbot.close();
		}
	}
}
