package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.RuneFilter;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Rs2SpellEquipmentSceneTest
{
	@Test
	public void observesContainersStagesTabAndRejectsAChangedLoadout()
	{
		Client client = mock(Client.class);
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
		ItemContainer worn = mock(ItemContainer.class);
		ItemContainer inventory = mock(ItemContainer.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		Item[] slots = new Item[14];
		slots[3] = new Item(Rs2Staff.STAFF_OF_AIR.getItemID(), 1);
		when(worn.getItems()).thenReturn(slots);
		when(inventory.getItems()).thenReturn(new Item[]{new Item(4151, 1)});
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, 4151, -1);
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2Tab> tabs = mockStatic(Rs2Tab.class);
			MockedStatic<Rs2Inventory> items = mockStatic(Rs2Inventory.class))
		{
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			assertEquals(SpellEquipmentTransaction.Action.OPEN_INVENTORY,
				Rs2SpellEquipmentScene.observe().restorationAction(transaction));
			assertTrue(Rs2SpellEquipmentScene.dispatch(command("OPEN_INVENTORY", 4151), transaction));
			verify(client).runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
			items.verifyNoInteractions();
			Widget widget = mock(Widget.class);
			when(widget.getChildren()).thenReturn(new Widget[0]);
			when(client.getWidget(ComponentID.INVENTORY_CONTAINER)).thenReturn(widget);
			tabs.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(true);
			Rs2ItemModel weapon = mock(Rs2ItemModel.class);
			when(weapon.getInventoryActions()).thenReturn(new String[]{null, "Wield", "Drop"});
			items.when(() -> Rs2Inventory.get(4151)).thenReturn(weapon);
			items.when(() -> Rs2Inventory.interact(weapon, "Wield")).thenReturn(true);
			assertTrue(Rs2SpellEquipmentScene.dispatch(command("RESTORE_WEAPON", 4151), transaction));
			items.verify(() -> Rs2Inventory.interact(weapon, "Wield"), times(1));
			slots[3] = new Item(1234, 1);
			assertFalse(Rs2SpellEquipmentScene.dispatch(command("RESTORE_WEAPON", 4151), transaction));
			items.verify(() -> Rs2Inventory.interact(weapon, "Wield"), times(1));
			when(client.getItemContainer(InventoryID.WORN)).thenReturn(null);
			assertNull(Rs2SpellEquipmentScene.observe());
			assertFalse(Rs2SpellEquipmentScene.dispatch(command("RESTORE_WEAPON", 4151), transaction));
		}
	}

	@Test
	public void equipActionMustActuallyExistAndCannotBeASubstring()
	{
		assertNull(Rs2SpellEquipmentScene.equipAction(null));
		assertNull(Rs2SpellEquipmentScene.equipAction(new String[]{"Re-equip", "Drop"}));
		assertEquals("Wear", Rs2SpellEquipmentScene.equipAction(new String[]{null, "Wear"}));
	}

	@Test
	public void planningUsesOwnedBankStaffOnlyWhenBankPlanningIsEnabled()
	{
		Client client = mock(Client.class);
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
		ItemContainer worn = mock(ItemContainer.class);
		ItemContainer inventory = mock(ItemContainer.class);
		when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
		when(client.getWorldType()).thenReturn(java.util.EnumSet.noneOf(net.runelite.api.WorldType.class));
		when(client.getItemContainer(InventoryID.WORN)).thenReturn(worn);
		when(client.getItemContainer(InventoryID.INV)).thenReturn(inventory);
		when(worn.getItems()).thenReturn(new Item[0]);
		when(inventory.getItems()).thenReturn(new Item[0]);
		Rs2ItemModel staff = mock(Rs2ItemModel.class);
		when(staff.getId()).thenReturn(Rs2Staff.STAFF_OF_AIR.getItemID());
		when(staff.getQuantity()).thenReturn(1);
		Rs2ItemModel law = mock(Rs2ItemModel.class);
		when(law.getId()).thenReturn(Runes.LAW.getItemId());
		when(law.getQuantity()).thenReturn(1);
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2Tab> tabs = mockStatic(Rs2Tab.class);
			MockedStatic<Rs2Magic> magic = mockStatic(Rs2Magic.class, CALLS_REAL_METHODS);
			MockedStatic<Rs2Bank> bank = mockStatic(Rs2Bank.class))
		{
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			magic.when(() -> Rs2Magic.getRunes(any(RuneFilter.class))).thenAnswer(call -> {
				RuneFilter filter = call.getArgument(0);
				assertFalse(filter.isIncludeEquipment());
				assertFalse(filter.isIncludeBank());
				assertFalse(filter.isIncludeComboRunes());
				assertTrue(filter.isIncludeRunePouch());
				return Map.of(Runes.FIRE, 1);
			});
			bank.when(Rs2Bank::bankItems).thenReturn(List.of(staff, law));
			List<Map<Runes, Integer>> casts = List.of(Map.of(Runes.AIR, 1, Runes.FIRE, 1, Runes.LAW, 1));
			assertNull(Rs2SpellEquipmentScene.plan(casts, false));
			bank.verifyNoInteractions();
			BankedSpellEquipmentPlanner.Plan selected = Rs2SpellEquipmentScene.plan(casts, true);
			assertNotNull(selected);
			assertEquals(Rs2Staff.STAFF_OF_AIR, selected.getStaff());
			assertEquals(Map.of(Runes.LAW.getItemId(), 1), selected.getRuneWithdrawals());
			when(staff.getQuantity()).thenReturn(0);
			assertNull(Rs2SpellEquipmentScene.plan(casts, true));
		}
	}

	private static RouteInteraction command(String action, int itemId)
	{
		WorldPoint tile = new WorldPoint(3200, 3200, 0);
		return new RouteInteraction(1, -1, tile, tile, tile, RouteInteraction.Kind.SPELL_EQUIPMENT,
			RouteInteraction.Status.AVAILABLE, action, true, itemId);
	}
}
