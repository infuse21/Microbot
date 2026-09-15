package net.runelite.client.plugins.microbot.util.walker.transport;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2Pvp;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.*;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DirectItemTeleportOwnershipTest
{
	private static final WorldPoint FROM = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TO = new WorldPoint(3110, 3350, 0);

	@Test
	public void tabletPreparationAndConsumptionRemainEngineOwnedUntilLanding()
	{
		try (Fixture f = new Fixture("Draynor Manor tablet", 19615, TO, false))
		{
			NavigationEngine engine = f.engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			verify(f.client).runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
			f.noItemInput();
			f.ready();
			RouteInteraction use = f.observe(open, FROM);
			f.issue(engine, use, 2);
			f.microbot.verify(() -> Microbot.doInvoke(argThat(entry -> "Break".equals(entry.getOption())
				&& entry.getItemId() == 19615 && entry.getParam0() == 3), any(Rectangle.class)), times(1));
			f.inventory.when(() -> Rs2Inventory.get(19615)).thenReturn(null);
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			f.tabs.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(false);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(f.observation(3, f.observe(use, FROM), FROM)).getType());
			assertEquals(RouteInteraction.Status.AVAILABLE, f.observe(use, new WorldPoint(TO.getX(), TO.getY(), 1)).getStatus());
			RouteInteraction landed = f.observe(use, TO);
			assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
			assertEquals("interaction-edge-crossed", engine.observe(f.observation(4, landed, TO)).getReason());
			assertEquals(NavigationDecision.Type.COMPLETE, engine.observe(f.observation(5, null, TO)).getType());
		}
	}

	@Test
	public void wornItemsUseTheirExactEquipmentActionWithoutUnequipping()
	{
		try (Fixture f = new Fixture("Skull sceptre", 9013, TO, true))
		{
			NavigationEngine engine = f.engine();
			RouteInteraction open = f.scan();
			f.issue(engine, open, 1);
			verify(f.client).runScript(915, InterfaceTab.EQUIPMENT.getVarcIntIndex());
			f.ready();
			f.issue(engine, f.observe(open, FROM), 2);
			f.microbot.verify(() -> Microbot.doInvoke(argThat(entry -> "Invoke".equals(entry.getOption())
				&& entry.getIdentifier() == 2 && entry.getParam0() == -1
				&& entry.getParam1() == ((InterfaceID.WORNITEMS << 16) | 18)), any(Rectangle.class)), times(1));
			f.equipment.verify(() -> Rs2Equipment.interact(anyInt(), anyString()), never());
		}
	}

	@Test
	public void wildernessConfirmationIsASeparateInputEvenAfterTheItemDisappears()
	{
		WorldPoint wilderness = new WorldPoint(3128, 3832, 0);
		try (Fixture f = new Fixture("Revenant cave teleport", 21802, wilderness, false))
		{
			f.ready();
			NavigationEngine engine = f.engine();
			RouteInteraction use = f.scan();
			f.issue(engine, use, 1);
			f.dialogue.verify(() -> Rs2Dialogue.clickOption(anyBoolean(), any(String[].class)), never());
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			Widget yes = mock(Widget.class);
			when(yes.getText()).thenReturn("Yes, teleport me now");
			f.dialogue.when(Rs2Dialogue::getDialogueOptions).thenReturn(List.of(yes));
			f.dialogue.when(() -> Rs2Dialogue.clickOption(true, "Yes, teleport me now")).thenReturn(true);
			RouteInteraction confirm = f.observe(use, FROM);
			assertTrue(confirm.getAction().startsWith("direct-item-confirm:"));
			f.issue(engine, confirm, 2);
			f.dialogue.verify(() -> Rs2Dialogue.clickOption(true, "Yes, teleport me now"), times(1));
			assertEquals(confirm.getAction(), f.observe(confirm, FROM).getAction());
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(f.observation(3, f.observe(confirm, FROM), FROM)).getType());
		}
	}

	@Test
	public void directedTabletsIgnoreSavedDefaultsAndGrandPodUsesSquash()
	{
		for (String[] values : new String[][]{{"Varrock tablet", "8007", "Varrock"},
			{"Watchtower tablet", "8012", "Watchtower"}, {"Grand seed pod", "9469", "Squash"}})
		{
			try (Fixture f = new Fixture(values[0], Integer.parseInt(values[1]), TO, false))
			{
				f.ready();
				when(f.item.getInventoryActions()).thenReturn(new String[]{"Break", "Launch", values[2]});
				assertTrue(f.scene.dispatch(f.scan()));
				f.microbot.verify(() -> Microbot.doInvoke(argThat(entry -> values[2].equals(entry.getOption())
					&& entry.getIdentifier() == 3), any(Rectangle.class)), times(1));
			}
		}
	}

	@Test
	public void staleTabsWrongItemsAndInterruptionsRejectPreviouslyObservedInput()
	{
		try (Fixture f = new Fixture("Draynor Manor tablet", 19615, TO, false))
		{
			f.ready();
			RouteInteraction use = f.scan();
			f.tabs.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(false);
			assertFalse(f.scene.dispatch(use));
			f.ready();
			when(f.child.getItemId()).thenReturn(1);
			assertFalse(f.scene.dispatch(use));
			when(f.child.getItemId()).thenReturn(19615);
			Thread.currentThread().interrupt();
			try { assertFalse(f.scene.dispatch(use)); }
			finally { Thread.interrupted(); }
			f.paths.when(Rs2PathApi::getTransports).thenReturn(Map.of());
			assertFalse(f.scene.dispatch(use));
			f.noItemInput();
		}
	}

	@Test
	public void missingAcknowledgementsReplanWithoutRepeatingActivation()
	{
		try (Fixture f = new Fixture("Draynor Manor tablet", 19615, TO, false))
		{
			f.ready();
			NavigationEngine engine = f.engine();
			RouteInteraction use = f.scan();
			f.issue(engine, use, 1);
			NavigationDecision expired = engine.observe(f.observation(60_001, f.observe(use, FROM), FROM));
			assertEquals(NavigationDecision.Type.REQUEST_REPLAN, expired.getType());
			assertEquals("direct-item-stage-not-acknowledged", expired.getReason());
			f.microbot.verify(() -> Microbot.doInvoke(any(NewMenuEntry.class), any(Rectangle.class)), times(1));
		}
	}

	@Test
	public void partialAmbiguousAndUnusableDefinitionsDoNotAuthorizeTeleport()
	{
		assertEquals(-1, Rs2DirectItemTeleportScene.exactIndex(new String[]{"Teleport somewhere"}, "Teleport"));
		assertEquals(-1, Rs2DirectItemTeleportScene.exactIndex(new String[]{"Break", "Break"}, "Break"));
		assertFalse(DirectItemTeleportPolicy.isEligible(new Transport(TO, "Ectophial",
			TransportType.TELEPORTATION_ITEM, false, 19, Set.of(Set.of(4252)))));
		try (Fixture f = new Fixture("Draynor Manor tablet", 19615, TO, false))
		{
			f.ready();
			when(f.item.getInventoryActions()).thenReturn(new String[]{"Break something"});
			assertNull(f.scan());
			f.noItemInput();
		}
	}

	@Test
	public void existingDialogueAndExcessWildernessLevelPreventActivation()
	{
		try (Fixture f = new Fixture("Draynor Manor tablet", 19615, TO, false))
		{
			f.ready();
			RouteInteraction use = f.scan();
			f.dialogue.when(Rs2Dialogue::getDialogueOptions).thenReturn(List.of(mock(Widget.class)));
			assertFalse(f.scene.dispatch(use));
			f.dialogue.when(Rs2Dialogue::getDialogueOptions).thenReturn(List.of());
			f.pvp.when(Rs2Pvp::isInWilderness).thenReturn(true);
			f.player.when(Rs2Player::getWorldLocation).thenReturn(FROM);
			f.pvp.when(() -> Rs2Pvp.getWildernessLevelFrom(FROM)).thenReturn(21);
			assertFalse(f.scene.dispatch(use));
			f.noItemInput();
		}
	}

	private static final class Fixture implements AutoCloseable
	{
		final Client client = mock(Client.class);
		final Widget child = mock(Widget.class);
		final Rs2ItemModel item = mock(Rs2ItemModel.class);
		final Transport row;
		final WorldPoint target;
		final boolean worn;
		final Rs2ItemTeleportScene scene = new Rs2ItemTeleportScene();
		final ItemTeleportRouteScanner scanner = new ItemTeleportRouteScanner();
		final MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
		final MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
		final MockedStatic<Rs2Equipment> equipment = mockStatic(Rs2Equipment.class);
		final MockedStatic<Rs2Dialogue> dialogue = mockStatic(Rs2Dialogue.class);
		final MockedStatic<Rs2Tab> tabs = mockStatic(Rs2Tab.class);
		final MockedStatic<Rs2PathApi> paths = mockStatic(Rs2PathApi.class);
		final MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
		final MockedStatic<Rs2Pvp> pvp = mockStatic(Rs2Pvp.class);
		final MockedStatic<PohPanel> poh = mockStatic(PohPanel.class);

		Fixture(String display, int id, WorldPoint target, boolean worn)
		{
			this.target = target;
			this.worn = worn;
			row = new Transport(target, display, TransportType.TELEPORTATION_ITEM, false, 19, Set.of(Set.of(id)));
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call -> Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			paths.when(Rs2PathApi::getTransports).thenReturn(Map.of(FROM, Set.of(row)));
			if (worn) equipment.when(() -> Rs2Equipment.get(id)).thenReturn(item);
			else inventory.when(() -> Rs2Inventory.get(id)).thenReturn(item);
			when(item.getId()).thenReturn(id);
			when(item.getSlot()).thenReturn(3);
			when(item.getInventoryActions()).thenReturn(new String[]{DirectItemTeleportPolicy.action(row)});
			when(item.getEquipmentActions()).thenReturn(List.of(DirectItemTeleportPolicy.action(row)));
			when(child.getIndex()).thenReturn(3);
			when(child.getItemId()).thenReturn(id);
			when(child.getBounds()).thenReturn(new Rectangle(10, 20, 30, 30));
			Widget root = mock(Widget.class);
			when(root.getChildren()).thenReturn(new Widget[]{child});
			when(client.getWidget(ComponentID.INVENTORY_CONTAINER)).thenReturn(root);
			when(client.getWidget((InterfaceID.WORNITEMS << 16) | 18)).thenReturn(child);
		}

		void ready() { tabs.when(() -> Rs2Tab.isCurrentTab(worn ? InterfaceTab.EQUIPMENT : InterfaceTab.INVENTORY)).thenReturn(true); }
		RoutePlan plan() { return new RoutePlan(1, 1, FROM, Set.of(target), List.of(FROM, target), List.of(FROM, target), true,
			List.of(new RouteEdge(0, FROM, target, RouteEdge.Kind.ITEM_TELEPORT))); }
		NavigationEngine engine() { NavigationEngine e = new NavigationEngine(); e.start(new NavigationRequest(1, Set.of(target), 0,
			new NavigationRouteOptions(true, true, false), "direct-item-test")); return e; }
		NavigationObservation observation(long time, RouteInteraction interaction, WorldPoint at) { return NavigationObservation.route(time,
			at, plan(), false, false, false, false, false, false, "direct-item-test").withRouteInteraction(interaction); }
		RouteInteraction scan() { return scanner.scan(plan(), 0, 1, scene); }
		RouteInteraction observe(RouteInteraction pending, WorldPoint at) { return scanner.observePending(pending, at, scene); }
		void noItemInput() { microbot.verify(() -> Microbot.doInvoke(any(NewMenuEntry.class), any(Rectangle.class)), never()); }
		void issue(NavigationEngine e, RouteInteraction interaction, long time)
		{
			NavigationDecision d = e.observe(observation(time, interaction, FROM));
			assertEquals(NavigationDecision.Type.INTERACT, d.getType());
			assertTrue(scene.dispatch(d.getInteraction()));
			e.recordCommandResult(d, true, time);
		}
		@Override public void close() { poh.close(); pvp.close(); player.close(); paths.close(); tabs.close(); dialogue.close(); equipment.close(); inventory.close(); microbot.close(); }
	}
}
