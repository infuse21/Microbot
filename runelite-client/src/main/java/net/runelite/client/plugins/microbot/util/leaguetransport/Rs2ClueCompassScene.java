package net.runelite.client.plugins.microbot.util.leaguetransport;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

/** Inventory preparation and one exact compass input; NavigationEngine owns the voyage. */
public final class Rs2ClueCompassScene
{
	private static final String OPEN = "compass-open:";
	private static final String USE = "compass-use:";

	private Rs2ClueCompassScene() { }

	public static boolean owns(String command)
	{
		return command != null && (command.startsWith(OPEN) || command.startsWith(USE));
	}

	public static ItemTeleport observe(Collection<Transport> rows, String pending)
	{
		Transport row = find(rows, pending);
		if (row == null) return null;
		if (pending != null && pending.startsWith(USE))
		{
			return new ItemTeleport(Rs2ClueCompassTransport.ITEM_ID, "", pending);
		}
		Selection selection = snapshot(row);
		return selection == null ? null : selection.item;
	}

	public static boolean dispatch(RouteInteraction interaction)
	{
		if (interaction == null || Thread.currentThread().isInterrupted()
			|| interaction.getObjectId() != Rs2ClueCompassTransport.ITEM_ID) return false;
		Transport row = find(TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
			interaction.getFrom(), interaction.getTo()), interaction.getAction());
		if (row == null) return false;
		Selection selection = snapshot(row);
		if (selection == null || !selection.item.isAvailable()
			|| !selection.item.command().equals(interaction.getAction())
			|| Thread.currentThread().isInterrupted()) return false;
		if (interaction.getAction().startsWith(OPEN))
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				Microbot.getClient().runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
				return true;
			}).orElse(false);
		}
		Microbot.doInvoke(new NewMenuEntry().option(selection.item.getAction()).target("")
			.param0(selection.slot).param1(ComponentID.INVENTORY_CONTAINER)
			.opcode(MenuAction.CC_OP.getId()).identifier(selection.identifier)
			.itemId(Rs2ClueCompassTransport.ITEM_ID), selection.bounds);
		Rs2LeaguesTransport.recordTransportAttempt(row);
		return true;
	}

	private static Transport find(Collection<Transport> rows, String pending)
	{
		List<Transport> matches = rows.stream().filter(Rs2ClueCompassTransport::isStagedRoute)
			.filter(row -> pending == null || owns(pending)
				&& pending.substring(pending.indexOf(':') + 1).equals(row.getDisplayInfo()))
			.collect(Collectors.toList());
		return matches.size() == 1 ? matches.get(0) : null;
	}

	private static Selection snapshot(Transport row)
	{
		if (Thread.currentThread().isInterrupted()) return null;
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			String action = Rs2ClueCompassTransport.destination(row.getDisplayInfo());
			ItemTeleport unavailable = new ItemTeleport(Rs2ClueCompassTransport.ITEM_ID, action,
				USE + row.getDisplayInfo(), false);
			Rs2ItemModel item = Rs2Inventory.get(Rs2ClueCompassTransport.ITEM_ID);
			if (item == null || item.isNoted() || Rs2Inventory.isItemSelected())
				return new Selection(unavailable, -1, -1, null);
			Widget inventory = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
			if (!Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY) || inventory == null || inventory.isHidden())
				return new Selection(new ItemTeleport(Rs2ClueCompassTransport.ITEM_ID, action,
					OPEN + row.getDisplayInfo()), -1, -1, null);
			Widget[] children = inventory.getChildren();
			Widget child = null;
			if (children != null)
			{
				for (Widget candidate : children)
				{
					if (candidate != null && candidate.getIndex() == item.getSlot()
						&& candidate.getItemId() == Rs2ClueCompassTransport.ITEM_ID)
					{
						if (child != null) return new Selection(unavailable, -1, -1, null);
						child = candidate;
					}
				}
			}
			if (child == null || child.isHidden()) return new Selection(unavailable, -1, -1, null);
			String[] actions = child.getActions() == null ? item.getInventoryActions() : child.getActions();
			int identifier = exactIdentifier(actions, item.getInventoryActions(), item.getSubops(), action);
			Rectangle bounds = child.getBounds();
			if (identifier < 1 || bounds == null || bounds.width <= 0 || bounds.height <= 0)
				return new Selection(unavailable, -1, -1, null);
			return new Selection(new ItemTeleport(Rs2ClueCompassTransport.ITEM_ID, action,
				USE + row.getDisplayInfo()), item.getSlot(), identifier, new Rectangle(bounds));
		}).orElse(null);
	}

	static int exactIdentifier(String[] actions, String[] parents, String[][] subops, String expected)
	{
		if (actions == null || expected == null) return -1;
		int found = -1;
		for (int i = 0; i < actions.length; i++)
		{
			if (expected.equalsIgnoreCase(actions[i]))
			{
				if (found != -1) return -1;
				found = i + 1;
			}
			if (actions[i] == null || parents == null || subops == null) continue;
			for (int p = 0; p < Math.min(parents.length, subops.length); p++)
			{
				if (!actions[i].equalsIgnoreCase(parents[p]) || subops[p] == null) continue;
				for (int s = 0; s < subops[p].length; s++)
				{
					if (!expected.equalsIgnoreCase(subops[p][s])) continue;
					if (found != -1) return -1;
					found = NewMenuEntry.findIdentifier(s + 1, i + 1);
				}
			}
		}
		return found;
	}

	private static final class Selection
	{
		final ItemTeleport item;
		final int slot;
		final int identifier;
		final Rectangle bounds;
		Selection(ItemTeleport item, int slot, int identifier, Rectangle bounds)
		{
			this.item = item;
			this.slot = slot;
			this.identifier = identifier;
			this.bounds = bounds;
		}
	}
}
