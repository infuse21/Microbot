package net.runelite.client.plugins.microbot.util.walker.transport;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

/** One tab, activation or confirmation input; no game-state waits inside dispatch. */
public final class Rs2DirectItemTeleportScene
{
	private static final String OPEN = "direct-item-open:";
	private static final String USE = "direct-item-use:";
	private static final String CONFIRM = "direct-item-confirm:";

	private Rs2DirectItemTeleportScene() { }

	public static boolean owns(String command)
	{
		return command != null && (command.startsWith(OPEN) || isTerminal(command));
	}

	public static boolean isTerminal(String command)
	{
		return command != null && (command.startsWith(USE) || command.startsWith(CONFIRM));
	}

	public static boolean isPreparation(String command)
	{
		return command != null && command.startsWith(OPEN);
	}

	public static ItemTeleport observe(Collection<Transport> rows, String pending, WorldPoint destination)
	{
		if (isTerminal(pending))
		{
			Transport context = context(pending, destination);
			if (context == null) return null;
			if (pending.startsWith(CONFIRM)) return new ItemTeleport(itemId(pending), "", pending);
			String option = PathfinderConfig.isInWilderness(destination) ? confirmation() : null;
			return new ItemTeleport(itemId(pending), option == null ? "" : option,
				option == null ? pending : CONFIRM + pending.substring(USE.length()) + "|" + option);
		}
		for (Transport row : rows)
		{
			if (!DirectItemTeleportPolicy.isEligible(row) || pending != null && !matches(row, pending)) continue;
			Selection selection = snapshot(row);
			if (selection != null) return selection.item;
		}
		return null;
	}

	public static boolean dispatch(RouteInteraction interaction)
	{
		if (interaction == null || Thread.currentThread().isInterrupted() || !owns(interaction.getAction())) return false;
		String command = interaction.getAction();
		if (interaction.getObjectId() != itemId(command)) return false;
		if (command.startsWith(CONFIRM))
		{
			if (context(command, interaction.getTo()) == null || !PathfinderConfig.isInWilderness(interaction.getTo())) return false;
			String option = confirmation();
			return option != null && command.endsWith("|" + option) && !Thread.currentThread().isInterrupted()
				&& Rs2Dialogue.clickOption(true, option);
		}
		Transport row = TransportEdgeMatcher.find(Rs2PathApi.getTransports(), interaction.getFrom(), interaction.getTo())
			.stream().filter(DirectItemTeleportPolicy::isEligible).filter(candidate -> matches(candidate, command))
			.findFirst().orElse(null);
		if (row == null) return false;
		WorldPoint player = net.runelite.client.plugins.microbot.util.player.Rs2Player.getWorldLocation();
		if (net.runelite.client.plugins.microbot.util.player.Rs2Pvp.isInWilderness()
			&& (player == null || net.runelite.client.plugins.microbot.util.player.Rs2Pvp.getWildernessLevelFrom(player)
				> row.getMaxWildernessLevel() + 1)) return false;
		Selection selection = snapshot(row);
		if (selection == null || !command.equals(selection.item.command()) || Thread.currentThread().isInterrupted()) return false;
		if (command.startsWith(OPEN))
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				Microbot.getClient().runScript(915, (selection.equipped ? InterfaceTab.EQUIPMENT
					: InterfaceTab.INVENTORY).getVarcIntIndex());
				return true;
			}).orElse(false);
		}
		Microbot.doInvoke(new NewMenuEntry().option(selection.item.getAction()).target("")
			.param0(selection.index).param1(selection.widgetId).opcode(MenuAction.CC_OP.getId())
			.identifier(selection.identifier).itemId(selection.equipped ? -1 : selection.item.getItemId()), selection.bounds);
		return true;
	}

	private static Selection snapshot(Transport row)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			if (Rs2Inventory.isItemSelected() || !Rs2Dialogue.getDialogueOptions().isEmpty()) return null;
			String action = DirectItemTeleportPolicy.action(row);
			for (Set<Integer> alternatives : row.getItemIdRequirements())
			{
				for (int id : alternatives)
				{
					Selection inventory = selection(Rs2Inventory.get(id), row, action, false);
					if (inventory != null) return inventory;
					Selection equipment = selection(Rs2Equipment.get(id), row, action, true);
					if (equipment != null) return equipment;
				}
			}
			return null;
		}).orElse(null);
	}

	// Called only within snapshot's client-thread callback; no widget escapes it.
	private static Selection selection(Rs2ItemModel item, Transport row, String action, boolean equipped)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
		if (item == null || item.isNoted()) return null;
		String[] metadata = equipped ? item.getEquipmentActions().toArray(new String[0]) : item.getInventoryActions();
		int index = exactIndex(metadata, action);
		if (index < 0) return null;
		String suffix = (equipped ? "equipment:" : "inventory:") + item.getId() + ":" + row.getDisplayInfo();
		InterfaceTab tab = equipped ? InterfaceTab.EQUIPMENT : InterfaceTab.INVENTORY;
		if (!Rs2Tab.isCurrentTab(tab))
			return new Selection(new ItemTeleport(item.getId(), action, OPEN + suffix), equipped, -1, -1, -1, null);
		Widget widget;
		int widgetId;
		int param0;
		int identifier;
		if (equipped)
		{
			int child;
			switch (item.getSlot())
			{
				case 0: child = 15; break;
				case 2: child = 17; break;
				case 3: child = 18; break;
				case 10: child = 23; break;
				default: return null;
			}
			widgetId = (InterfaceID.WORNITEMS << 16) | child;
			widget = Microbot.getClient().getWidget(widgetId);
			param0 = -1;
			identifier = index + 2;
		}
		else
		{
			widgetId = ComponentID.INVENTORY_CONTAINER;
			Widget root = Microbot.getClient().getWidget(widgetId);
			if (root == null || root.isHidden() || root.getChildren() == null) return null;
			widget = null;
			for (Widget child : root.getChildren())
			{
				if (child != null && child.getIndex() == item.getSlot() && child.getItemId() == item.getId())
				{
					if (widget != null) return null;
					widget = child;
				}
			}
			if (widget == null) return null;
			String[] actions = widget.getActions() == null ? metadata : widget.getActions();
			identifier = exactIndex(actions, action) + 1;
			param0 = item.getSlot();
		}
		if (widget == null || widget.isHidden() || identifier <= 0) return null;
		Rectangle bounds = widget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return null;
		return new Selection(new ItemTeleport(item.getId(), action, USE + suffix), equipped,
			widgetId, param0, identifier, new Rectangle(bounds));
		}).orElse(null);
	}

	static int exactIndex(String[] actions, String action)
	{
		if (actions == null || action == null) return -1;
		int found = -1;
		for (int i = 0; i < actions.length; i++)
		{
			if (!action.equalsIgnoreCase(actions[i])) continue;
			if (found >= 0) return -1;
			found = i;
		}
		return found;
	}

	private static String confirmation()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			List<Widget> options = Rs2Dialogue.getDialogueOptions();
			String found = null;
			for (Widget option : options)
			{
				if (option == null || option.isHidden()) continue;
				String text = option.getText();
				if (!"Yes".equalsIgnoreCase(text) && !"Okay".equalsIgnoreCase(text)
					&& !"Yes, teleport me now".equalsIgnoreCase(text) && !"Yes, teleport me now.".equalsIgnoreCase(text)) continue;
				if (found != null) return null;
				found = text;
			}
			return found;
		}).orElse(null);
	}

	private static boolean matches(Transport row, String command)
	{
		String[] parts = parts(command);
		return parts != null && row.getDisplayInfo().equals(parts[3])
			&& row.getItemIdRequirements().stream().anyMatch(ids -> ids.contains(itemId(command)));
	}

	private static Transport context(String command, WorldPoint destination)
	{
		String[] parts = parts(command);
		int id = itemId(command);
		if (parts == null || id < 1) return null;
		Transport row = new Transport(destination, parts[3], TransportType.TELEPORTATION_ITEM,
			false, 19, Set.of(Set.of(id)));
		return DirectItemTeleportPolicy.isEligible(row) ? row : null;
	}

	private static String[] parts(String command)
	{
		if (!owns(command)) return null;
		String[] parts = command.split("\\|", 2)[0].split(":", 4);
		return parts.length == 4 && ("inventory".equals(parts[1]) || "equipment".equals(parts[1])) ? parts : null;
	}

	private static int itemId(String command)
	{
		String[] parts = parts(command);
		if (parts == null) return -1;
		try { return Integer.parseInt(parts[2]); }
		catch (NumberFormatException ignored) { return -1; }
	}

	private static final class Selection
	{
		final ItemTeleport item;
		final boolean equipped;
		final int widgetId;
		final int index;
		final int identifier;
		final Rectangle bounds;
		Selection(ItemTeleport item, boolean equipped, int widgetId, int index, int identifier, Rectangle bounds)
		{
			this.item = item;
			this.equipped = equipped;
			this.widgetId = widgetId;
			this.index = index;
			this.identifier = identifier;
			this.bounds = bounds;
		}
	}
}
