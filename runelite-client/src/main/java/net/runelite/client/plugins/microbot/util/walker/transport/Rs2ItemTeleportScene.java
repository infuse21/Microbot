package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.ItemTeleport;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/** Exact, cache-backed item actions. No generic Rub/Teleport fallback or dialogue loop. */
public final class Rs2ItemTeleportScene implements ItemTeleportScene
{
	private static final String BOOK_SELECT_PREFIX = "book-select:";
	private static final String BOOK_CONFIRM_PREFIX = "book-confirm:";
	private static final String REVENANT_CONFIRM = "Yes, teleport me now";

	@Override
	public ItemTeleport find(PlannedEdge edge)
	{
		return observe(edge, null);
	}

	@Override
	public ItemTeleport observe(PlannedEdge edge, String pendingAction)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		if (pendingAction != null && (pendingAction.startsWith(BOOK_SELECT_PREFIX)
			|| pendingAction.startsWith(BOOK_CONFIRM_PREFIX)))
		{
			if (((BOOK_SELECT_PREFIX + "Revenant cave").equals(pendingAction)
				|| pendingAction.startsWith(BOOK_CONFIRM_PREFIX))
				&& Rs2Dialogue.hasDialogueOption(REVENANT_CONFIRM, true))
			{
				return new ItemTeleport(ItemID.BOOKOFSCROLLS_CHARGED, REVENANT_CONFIRM,
					BOOK_CONFIRM_PREFIX + REVENANT_CONFIRM);
			}
			return null;
		}
		Set<Transport> transports = TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to());
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			for (Transport transport : transports)
			{
				if (!ItemTeleportPolicy.isEligible(transport))
				{
					continue;
				}
				if (ItemTeleportPolicy.isMasterScrollBook(transport))
				{
					return observeMasterScrollBook(transport);
				}
				for (Set<Integer> group : transport.getItemIdRequirements())
				{
					for (int id : group)
					{
						ItemTeleport item = observe(Rs2Inventory.get(id), transport, false);
						if (item == null)
						{
							item = observe(Rs2Equipment.get(id), transport, true);
						}
						if (item != null)
						{
							return item;
						}
					}
				}
			}
			return null;
		}).orElse(null);
	}

	private static ItemTeleport observeMasterScrollBook(Transport transport)
	{
		Widget contents = Microbot.getClient().getWidget(InterfaceID.Bookofscrolls.CONTENTS);
		if (visible(contents))
		{
			Widget destination = Microbot.getClient().getWidget(
				ItemTeleportPolicy.masterScrollBookWidget(transport));
			if (!visible(destination))
			{
				return null;
			}
			String name = ItemTeleportPolicy.destination(transport);
			return new ItemTeleport(ItemID.BOOKOFSCROLLS_CHARGED, name,
				BOOK_SELECT_PREFIX + name);
		}
		Rs2ItemModel book = Rs2Inventory.get(ItemID.BOOKOFSCROLLS_CHARGED);
		ItemTeleport observed = observe(book, transport, false);
		if (observed != null && observed.isTabReady())
		{
			return new ItemTeleport(observed.getItemId(), observed.getAction(),
				"item-prepare:inventory:" + observed.getAction());
		}
		return observed;
	}

	private static ItemTeleport observe(Rs2ItemModel item, Transport transport, boolean equipped)
	{
		if (item == null || item.isNoted())
		{
			return null;
		}
		String action = equipped ? ItemTeleportPolicy.equipmentAction(transport)
			: ItemTeleportPolicy.inventoryAction(transport);
		String[] actions = equipped ? item.getEquipmentActions().toArray(new String[0])
			: item.getInventoryActions();
		if (!hasExactAction(actions, item.getInventoryActions(), item.getSubops(), action))
		{
			return null;
		}
		// The shared interaction helpers use contains-matching for subactions: reject a different first hit.
		boolean direct = Arrays.stream(actions).anyMatch(value -> action.equalsIgnoreCase(value));
		if (!direct)
		{
			Map.Entry<String, Integer> sub = item.getIndexOfSubAction(action);
			if (sub == null || sub.getKey() == null)
			{
				return null;
			}
			int parent = Arrays.asList(item.getInventoryActions()).indexOf(sub.getKey());
			if (parent < 0 || !action.equalsIgnoreCase(item.getSubops()[parent][sub.getValue()]))
			{
				return null;
			}
		}
		boolean ready = equipped ? Rs2Tab.isCurrentTab(InterfaceTab.EQUIPMENT) : inventoryReady();
		return new ItemTeleport(item.getId(), action, equipped, ready);
	}

	static boolean hasExactAction(String[] actions, String[] inventoryActions, String[][] subops, String expected)
	{
		if (actions == null || expected == null)
		{
			return false;
		}
		if (Arrays.stream(actions).anyMatch(expected::equalsIgnoreCase))
		{
			return true;
		}
		if (subops == null || inventoryActions == null)
		{
			return false;
		}
		for (int i = 0; i < Math.min(subops.length, inventoryActions.length); i++)
		{
			String parent = inventoryActions[i];
			if (parent != null && subops[i] != null
				&& Arrays.stream(actions).anyMatch(parent::equalsIgnoreCase)
				&& Arrays.stream(subops[i]).anyMatch(expected::equalsIgnoreCase))
			{
				return true;
			}
		}
		return false;
	}

	public boolean dispatch(RouteInteraction interaction)
	{
		if (interaction.getAction().equals(BOOK_CONFIRM_PREFIX + REVENANT_CONFIRM))
		{
			return Rs2Dialogue.clickOption(REVENANT_CONFIRM, true);
		}
		ItemTeleport item = find(new PlannedEdge(interaction.getFrom(), interaction.getTo()));
		if (item == null || item.getItemId() != interaction.getObjectId()
			|| !item.command().equals(interaction.getAction()))
		{
			return false;
		}
		if (!item.isTabReady())
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
			{
				Microbot.getClient().runScript(915, (item.isEquipped() ? InterfaceTab.EQUIPMENT
					: InterfaceTab.INVENTORY).getVarcIntIndex());
				return true;
			}).orElse(false);
		}
		if (item.command().startsWith(BOOK_SELECT_PREFIX))
		{
			Transport transport = TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
				interaction.getFrom(), interaction.getTo()).stream()
				.filter(ItemTeleportPolicy::isMasterScrollBook).findFirst().orElse(null);
			Widget destination = transport == null ? null
				: Rs2Widget.getWidget(ItemTeleportPolicy.masterScrollBookWidget(transport));
			return destination != null && Rs2Widget.clickWidget(destination);
		}
		return item.isEquipped() ? Rs2Equipment.interact(item.getItemId(), item.getAction())
			: Rs2Inventory.interact(item.getItemId(), item.getAction());
	}

	private static boolean inventoryReady()
	{
		Widget widget = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
		return Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY) && widget != null && !widget.isHidden()
			&& widget.getChildren() != null;
	}

	private static boolean visible(Widget widget)
	{
		return widget != null && !widget.isHidden();
	}
}
