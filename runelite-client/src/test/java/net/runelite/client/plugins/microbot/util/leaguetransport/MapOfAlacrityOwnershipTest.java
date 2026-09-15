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
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.mouse.Mouse;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationDecision;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngine;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationObservation;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRequest;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationRouteOptions;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.transport.ItemTeleportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2ItemTeleportScene;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MapOfAlacrityOwnershipTest
{
	private static final WorldPoint FROM = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TO = new WorldPoint(2936, 3355, 0);

	@Test
	public void engineOwnsEveryMenuCommandAndWaitsForLandingAfterTheRowDisappears()
	{
		try (Fixture f = new Fixture("Asgarnia"))
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			verify(f.client).runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
			f.inventory.verify(() -> Rs2Inventory.interact(anyInt(), anyString()), never());
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(true);
			RouteInteraction read = f.observe(open, FROM);
			f.issue(engine, read, 2);
			f.inventory.verify(() -> Rs2Inventory.interact(33233, "Read"), times(1));

			RouteInteraction loading = f.observe(read, FROM);
			assertEquals(read.getAction(), loading.getAction());
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(3, loading, FROM)).getType());
			f.inventory.verify(() -> Rs2Inventory.interact(33233, "Read"), times(1));
			f.show("[1] Asgarnia");
			RouteInteraction region = f.observe(read, FROM);
			f.issue(engine, region, 4);
			f.keyboard.verify(() -> Rs2Keyboard.keyPress(Character.valueOf('1')), times(1));
			assertEquals(NavigationDecision.Type.WAIT,
				engine.observe(observation(5, f.observe(region, FROM), FROM)).getType());

			Widget target = f.show("[A] Falador wall");
			when(target.getBounds()).thenReturn(new Rectangle(0, 900, 100, 20));
			RouteInteraction destination = f.observe(region, FROM);
			f.issue(engine, destination, 6);
			f.keyboard.verify(() -> Rs2Keyboard.keyPress(Character.valueOf('A')), times(1));
			f.leagues.verify(() -> Rs2LeaguesTransport.recordTransportAttempt(f.transport), times(1));
			f.hide();
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			RouteInteraction inFlight = f.observe(destination, FROM);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(7, inFlight, FROM)).getType());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				f.observe(destination, new WorldPoint(TO.getX(), TO.getY(), 1)).getStatus());
			RouteInteraction landed = f.observe(destination, TO);
			assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
			assertEquals("interaction-edge-crossed", engine.observe(observation(8, landed, TO)).getReason());
			assertEquals(NavigationDecision.Type.COMPLETE, engine.observe(observation(9, null, TO)).getType());
			assertTrue(engine.snapshot().isTerminal());
			verifyNoInteractions(f.mouse);
		}
	}

	@Test
	public void lockedRegionInvalidatesPublicationAndImmediatelyReplansWithoutInput()
	{
		try (Fixture f = new Fixture("Audit locked region"))
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			f.show("<str>[1] Audit locked region</str>");
			RouteInteraction locked = f.observe(open, FROM);
			assertEquals(RouteInteraction.Status.UNAVAILABLE, locked.getStatus());
			assertEquals(NavigationDecision.Type.REQUEST_REPLAN,
				engine.observe(observation(2, locked, FROM)).getType());
			assertFalse(Rs2MapOfAlacrityTransport.isAvailable(f.transport));
			assertFalse(f.scene.dispatch(locked));
			f.leagues.verify(Rs2LeaguesTransport::invalidateContext, times(1));
			f.keyboard.verifyNoInteractions();
		}
	}

	@Test
	public void lockedDestinationDoesNotDisableOtherDestinationsInItsRegion()
	{
		try (Fixture f = new Fixture("Audit destination region"))
		{
			f.show("[2] Audit destination region");
			RouteInteraction region = f.scan();
			assertTrue(f.scene.dispatch(region));
			f.show("<str>[3] Falador wall</str>");
			RouteInteraction locked = f.observe(region, FROM);
			assertEquals(RouteInteraction.Status.UNAVAILABLE, locked.getStatus());
			assertFalse(f.scene.dispatch(locked));
			Transport other = new Transport(new WorldPoint(3000, 3300, 0),
				"Map of Alacrity: Audit destination region - Different place",
				TransportType.SEASONAL_TRANSPORT, false, 20, Set.of(Set.of(33233)));
			assertTrue(Rs2MapOfAlacrityTransport.isAvailable(other));
			f.keyboard.verify(() -> Rs2Keyboard.keyPress(Character.valueOf('3')), never());
		}
	}

	@Test
	public void staleEmptyAndAmbiguousMenusNeverInventAnInput()
	{
		try (Fixture f = new Fixture("Audit ambiguous region"))
		{
			f.show("[1] Audit ambiguous region");
			RouteInteraction region = f.scan();
			f.show("Unrelated option");
			assertFalse(f.scene.dispatch(region));
			f.show("[1] Audit ambiguous region", "[2] Audit ambiguous region");
			assertFalse(f.scene.dispatch(region));
			f.show();
			assertEquals(region.getAction(), f.observe(region, FROM).getAction());
			assertFalse(f.scene.dispatch(region));
			f.keyboard.verifyNoInteractions();
			verifyNoInteractions(f.mouse);
		}
	}

	@Test
	public void noHotkeyMeansOnlyAnActuallyVisibleRowCanBeClicked()
	{
		try (Fixture f = new Fixture("Audit visible region"))
		{
			Widget row = f.show("Audit visible region");
			RouteInteraction region = f.scan();
			assertTrue(f.scene.dispatch(region));
			verify(f.mouse).click(new Rectangle(10, 10, 100, 20));
			when(row.getBounds()).thenReturn(new Rectangle(10, 900, 100, 20));
			assertFalse(f.scene.dispatch(region));
			f.keyboard.verifyNoInteractions();
		}
	}

	@Test
	public void aMissingMenuExpiresInTheEngineWithoutRepeatedReadsOrMovement()
	{
		try (Fixture f = new Fixture("Audit timeout region"))
		{
			f.tab.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(true);
			NavigationEngine engine = engine();
			RouteInteraction read = f.scan();
			f.issue(engine, read, 1);
			RouteInteraction waiting = f.observe(read, FROM);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(3999, waiting, FROM)).getType());
			NavigationDecision expired = engine.observe(observation(4001, waiting, FROM));
			assertEquals(NavigationDecision.Type.REQUEST_REPLAN, expired.getType());
			assertEquals("alacrity-stage-not-acknowledged", expired.getReason());
			f.inventory.verify(() -> Rs2Inventory.interact(33233, "Read"), times(1));
			f.keyboard.verifyNoInteractions();
		}
	}

	@Test
	public void unrecognisedInventoryActionsDoNotAuthorizeInput()
	{
		try (Fixture f = new Fixture("Audit action region"))
		{
			Rs2ItemModel item = Rs2Inventory.get(33233);
			for (String[] actions : new String[][]{null, {}, {"Read something", "Teleport", "Last-destination"}})
			{
				when(item.getInventoryActions()).thenReturn(actions);
				assertFalse(f.scene.dispatch(f.scan()));
			}
			f.keyboard.verifyNoInteractions();
			verifyNoInteractions(f.mouse);
		}
	}

	@Test
	public void cancellationDuringMenuWaitAndInterruptedDispatchProduceNoFurtherInput()
	{
		try (Fixture f = new Fixture("Audit cancelled region"))
		{
			NavigationEngine engine = engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			engine.cancel("test-cancel");
			assertFalse(engine.observe(observation(2, open, FROM)).issuesInput());
			try
			{
				Thread.currentThread().interrupt();
				assertFalse(f.scene.dispatch(open));
			}
			finally
			{
				Thread.interrupted();
			}
			verify(f.client, times(1)).runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
			f.keyboard.verifyNoInteractions();
		}
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(new NavigationRequest(1, Set.of(TO), 0,
			new NavigationRouteOptions(true, true, false), "map-test"));
		return engine;
	}

	private static RoutePlan plan()
	{
		return new RoutePlan(1, 1, FROM, Set.of(TO), List.of(FROM, TO), List.of(FROM, TO), true,
			List.of(new RouteEdge(0, FROM, TO, RouteEdge.Kind.ITEM_TELEPORT)));
	}

	private static NavigationObservation observation(long time, RouteInteraction interaction, WorldPoint player)
	{
		return NavigationObservation.route(time, player, plan(), false, false, false, false,
			false, false, "map-test").withRouteInteraction(interaction);
	}

	private static final class Fixture implements AutoCloseable
	{
		final Set<Integer> unavailable = lockSet("unavailableDestinations");
		final Set<String> lockedRegions = lockSet("lockedRegions");
		final Set<Integer> previousUnavailable = Set.copyOf(unavailable);
		final Set<String> previousRegions = Set.copyOf(lockedRegions);
		final Client client = mock(Client.class);
		final Mouse mouse = mock(Mouse.class);
		final Transport transport;
		final Rs2ItemTeleportScene scene = new Rs2ItemTeleportScene();
		final ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		final MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
		final MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
		final MockedStatic<Rs2Tab> tab = mockStatic(Rs2Tab.class);
		final MockedStatic<Rs2PathApi> paths = mockStatic(Rs2PathApi.class);
		final MockedStatic<Rs2Keyboard> keyboard = mockStatic(Rs2Keyboard.class);
		final MockedStatic<Rs2LeaguesTransport> leagues = mockStatic(Rs2LeaguesTransport.class);
		final MockedStatic<PohPanel> poh = mockStatic(PohPanel.class);

		Fixture(String region)
		{
			transport = new Transport(TO, "Map of Alacrity: " + region + " - Falador wall",
				TransportType.SEASONAL_TRANSPORT, false, 20, Set.of(Set.of(33233)));
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getMouse).thenReturn(mouse);
			paths.when(Rs2PathApi::getTransports).thenReturn(Map.of(FROM, Set.of(transport)));
			Rs2ItemModel map = mock(Rs2ItemModel.class);
			when(map.getInventoryActions()).thenReturn(new String[]{"Read", "Last-destination", "Destroy"});
			inventory.when(() -> Rs2Inventory.get(33233)).thenReturn(map);
			inventory.when(() -> Rs2Inventory.interact(33233, "Read")).thenReturn(true);
			Widget inventoryWidget = mock(Widget.class);
			when(inventoryWidget.getChildren()).thenReturn(new Widget[0]);
			when(client.getWidget(ComponentID.INVENTORY_CONTAINER)).thenReturn(inventoryWidget);
		}

		Widget show(String... labels)
		{
			Widget root = mock(Widget.class);
			Widget[] rows = new Widget[labels.length];
			for (int i = 0; i < labels.length; i++)
			{
				rows[i] = mock(Widget.class);
				when(rows[i].getText()).thenReturn(labels[i]);
				when(rows[i].getBounds()).thenReturn(new Rectangle(10, 10, 100, 20));
			}
			when(root.getDynamicChildren()).thenReturn(rows);
			when(root.getBounds()).thenReturn(new Rectangle(0, 0, 400, 400));
			when(client.getWidget(187, 3)).thenReturn(root);
			return rows.length == 0 ? root : rows[0];
		}

		void hide() { when(client.getWidget(187, 3)).thenReturn(null); }
		RouteInteraction scan() { return scanner.scan(plan(), 0, 1, scene); }
		RouteInteraction observe(RouteInteraction pending, WorldPoint player)
		{
			return scanner.observePending(pending, player, scene);
		}

		void issue(NavigationEngine engine, RouteInteraction stage, long time)
		{
			assertNotNull(stage);
			NavigationDecision decision = engine.observe(observation(time, stage, FROM));
			assertEquals(NavigationDecision.Type.INTERACT, decision.getType());
			assertTrue(scene.dispatch(decision.getInteraction()));
			engine.recordCommandResult(decision, true, time);
		}

		@Override
		public void close()
		{
			poh.close(); leagues.close(); keyboard.close(); paths.close(); tab.close(); inventory.close(); microbot.close();
			unavailable.retainAll(previousUnavailable);
			lockedRegions.retainAll(previousRegions);
		}

		@SuppressWarnings("unchecked")
		private static <T> Set<T> lockSet(String name)
		{
			try
			{
				java.lang.reflect.Field field = Rs2MapOfAlacrityTransport.class.getDeclaredField(name);
				field.setAccessible(true);
				return (Set<T>) field.get(null);
			}
			catch (ReflectiveOperationException ex)
			{
				throw new AssertionError(ex);
			}
		}
	}
}
