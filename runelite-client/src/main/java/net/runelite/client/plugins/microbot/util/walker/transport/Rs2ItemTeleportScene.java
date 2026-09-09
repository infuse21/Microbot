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
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact, cache-backed item actions. No generic Rub/Teleport fallback or dialogue loop. */
public final class Rs2ItemTeleportScene implements ItemTeleportScene
{
	private static final String BOOK_SELECT_PREFIX = "book-select:";
	private static final String BOOK_CONFIRM_PREFIX = "book-confirm:";
	private static final String WILDERNESS_CONFIRM_PREFIX = "wilderness-confirm:";
	private static final String UNEQUIP_PREFIX = "item-prepare:unequip:";
	private static final String ITEM_OPEN_RESTORE_PREFIX = "item-open-restore:";
	private static final String ITEM_USE_RESTORE_PREFIX = "item-use-restore:";
	private static final String WILDERNESS_CONFIRM_RESTORE_PREFIX = "wilderness-confirm-restore:";
	private static final String RESTORE_OPEN_PREFIX = "item-restore-open:";
	private static final String RESTORE_EQUIP_PREFIX = "item-restore-equip:";
	private static final String RESTORED_PREFIX = "item-restored:";
	private static final String QUETZAL_WHISTLE_DESTINATION_PREFIX = "whistle-destination:";
	private static final String REVENANT_CONFIRM = "Yes, teleport me now";
	private static final String BURNING_AMULET_CONFIRM = "Okay, teleport to level";

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
		Transport whistle = transports.stream().filter(ItemTeleportPolicy::isEligible)
			.filter(ItemTeleportPolicy::isQuetzalWhistle).findFirst().orElse(null);
		if (whistle != null && quetzalWhistleMapVisible())
		{
			String destinationName = ItemTeleportPolicy.quetzalWhistleDestination(whistle);
			Widget destination = quetzalWhistleDestinationWidget(destinationName);
			if (destination == null || isLocked(destination))
			{
				return null;
			}
			return new ItemTeleport(firstItemId(whistle), destinationName,
				QUETZAL_WHISTLE_DESTINATION_PREFIX + destinationName);
		}
		if (isItemUse(pendingAction)
			&& transports.stream().anyMatch(ItemTeleportPolicy::isBurningAmulet)
			&& Rs2Dialogue.hasDialogueOption(BURNING_AMULET_CONFIRM, false))
		{
			return new ItemTeleport(21166, BURNING_AMULET_CONFIRM,
				WILDERNESS_CONFIRM_PREFIX + BURNING_AMULET_CONFIRM);
		}
		Transport blackHunterArea = transports.stream().filter(ItemTeleportPolicy::isEligible)
			.filter(ItemTeleportPolicy::isBlackHunterArea).findFirst().orElse(null);
		if (isItemUse(pendingAction)
			&& blackHunterArea != null
			&& Rs2Dialogue.hasDialogueOption(BURNING_AMULET_CONFIRM, false))
		{
			boolean restore = pendingAction.startsWith(ITEM_USE_RESTORE_PREFIX);
			return new ItemTeleport(firstItemId(blackHunterArea), BURNING_AMULET_CONFIRM,
				restore ? WILDERNESS_CONFIRM_RESTORE_PREFIX
					+ restorationId(pendingAction) : WILDERNESS_CONFIRM_PREFIX + BURNING_AMULET_CONFIRM);
		}
		if (pendingAction != null && (pendingAction.startsWith(WILDERNESS_CONFIRM_PREFIX)
			|| pendingAction.startsWith(WILDERNESS_CONFIRM_RESTORE_PREFIX)))
		{
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			boolean restoreAfterUse = pendingAction != null && (pendingAction.startsWith(UNEQUIP_PREFIX)
				|| pendingAction.startsWith(ITEM_OPEN_RESTORE_PREFIX)
				|| pendingAction.startsWith(ITEM_USE_RESTORE_PREFIX));
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
						if (item != null && restoreAfterUse)
						{
							item = withRestoration(item);
						}
						if (item == null)
						{
							Rs2ItemModel equipped = Rs2Equipment.get(id);
							item = equipped != null && ItemTeleportPolicy.requiresInventorySurface(transport)
								? new ItemTeleport(id, "Remove", UNEQUIP_PREFIX + id)
								: observe(equipped, transport, true);
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

	@Override
	public ItemTeleport restore(PlannedEdge edge, String pendingAction)
	{
		int id = restorationId(pendingAction);
		if (id <= 0 || !isExactInventoryPreparation(edge, id))
		{
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			if (Rs2Equipment.get(id) != null)
			{
				return new ItemTeleport(id, "Wear", RESTORED_PREFIX + id);
			}
			Rs2ItemModel item = Rs2Inventory.get(id);
			if (item == null || !hasExactAction(item.getInventoryActions(), item.getInventoryActions(),
				item.getSubops(), "Wear"))
			{
				return null;
			}
			return new ItemTeleport(id, "Wear", inventoryReady()
				? RESTORE_EQUIP_PREFIX + id : RESTORE_OPEN_PREFIX + id);
		}).orElse(null);
	}

	private static ItemTeleport observeMasterScrollBook(Transport transport)
	{
		Widget contents = Rs2Widget.getWidget(InterfaceID.Bookofscrolls.CONTENTS);
		if (visible(contents))
		{
			Widget destination = Rs2Widget.getWidget(
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
		if (interaction.getAction().startsWith(ITEM_OPEN_RESTORE_PREFIX)
			|| interaction.getAction().startsWith(RESTORE_OPEN_PREFIX))
		{
			return openInventory();
		}
		if (interaction.getAction().startsWith(ITEM_USE_RESTORE_PREFIX))
		{
			int id = restorationId(interaction.getAction());
			String action = restorationAction(interaction.getAction());
			return isExactInventoryPreparation(new PlannedEdge(interaction.getFrom(), interaction.getTo()), id)
				&& Rs2Inventory.get(id) != null && Rs2Inventory.interact(id, action);
		}
		if (interaction.getAction().startsWith(RESTORE_EQUIP_PREFIX))
		{
			int id = restorationId(interaction.getAction());
			return isExactInventoryPreparation(new PlannedEdge(interaction.getFrom(), interaction.getTo()), id)
				&& Rs2Inventory.get(id) != null && Rs2Inventory.interact(id, "Wear");
		}
		if (interaction.getAction().startsWith(WILDERNESS_CONFIRM_RESTORE_PREFIX))
		{
			return Rs2Dialogue.clickOption(BURNING_AMULET_CONFIRM, false);
		}
		if (interaction.getAction().startsWith(UNEQUIP_PREFIX))
		{
			int id;
			try
			{
				id = Integer.parseInt(interaction.getAction().substring(UNEQUIP_PREFIX.length()));
			}
		catch (NumberFormatException ex)
		{
			return false;
		}
		return isExactInventoryPreparation(new PlannedEdge(interaction.getFrom(), interaction.getTo()), id)
			&& interaction.getObjectId() == id && Rs2Equipment.get(id) != null
			&& Rs2Equipment.unEquip(id);
		}
		if (interaction.getAction().startsWith(QUETZAL_WHISTLE_DESTINATION_PREFIX))
		{
			String destinationName = interaction.getAction().substring(
				QUETZAL_WHISTLE_DESTINATION_PREFIX.length());
			Widget destination = quetzalWhistleDestinationWidget(destinationName);
			return destination != null && !isLocked(destination) && Rs2Widget.clickWidget(destination);
		}
		if (interaction.getAction().equals(BOOK_CONFIRM_PREFIX + REVENANT_CONFIRM))
		{
			return Rs2Dialogue.clickOption(REVENANT_CONFIRM, true);
		}
		if (interaction.getAction().equals(WILDERNESS_CONFIRM_PREFIX + BURNING_AMULET_CONFIRM))
		{
			return Rs2Dialogue.clickOption(BURNING_AMULET_CONFIRM, false);
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
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget widget = Microbot.getClient().getWidget(ComponentID.INVENTORY_CONTAINER);
			return Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY) && widget != null
				&& !widget.isHidden() && widget.getChildren() != null;
		}).orElse(false);
	}

	private static boolean openInventory()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Microbot.getClient().runScript(915, InterfaceTab.INVENTORY.getVarcIntIndex());
			return true;
		}).orElse(false);
	}

	private static ItemTeleport withRestoration(ItemTeleport item)
	{
		String prefix = item.isTabReady() ? ITEM_USE_RESTORE_PREFIX : ITEM_OPEN_RESTORE_PREFIX;
		return new ItemTeleport(item.getItemId(), item.getAction(),
			prefix + item.getItemId() + ":" + item.getAction());
	}

	private static boolean isItemUse(String action)
	{
		return action != null && (action.startsWith("item-use:")
			|| action.startsWith(ITEM_USE_RESTORE_PREFIX));
	}

	private static int restorationId(String action)
	{
		if (action == null)
		{
			return -1;
		}
		for (String prefix : new String[] {ITEM_OPEN_RESTORE_PREFIX, ITEM_USE_RESTORE_PREFIX,
			WILDERNESS_CONFIRM_RESTORE_PREFIX, RESTORE_OPEN_PREFIX, RESTORE_EQUIP_PREFIX, RESTORED_PREFIX})
		{
			if (action.startsWith(prefix))
			{
				String value = action.substring(prefix.length()).split(":", 2)[0];
				try
				{
					return Integer.parseInt(value);
				}
				catch (NumberFormatException ignored)
				{
					return -1;
				}
			}
		}
		return -1;
	}

	private static String restorationAction(String command)
	{
		String[] values = command.substring(ITEM_USE_RESTORE_PREFIX.length()).split(":", 2);
		return values.length == 2 ? values[1] : "";
	}

	private static boolean isExactInventoryPreparation(PlannedEdge edge, int id)
	{
		return edge != null && ItemTeleportPolicy.isInventoryRestorationTarget(id, edge.to());
	}

	private static boolean visible(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget != null && !widget.isHidden()).orElse(false);
	}

	private static boolean quetzalWhistleMapVisible()
	{
		return visible(Rs2Widget.getWidget(InterfaceID.QuetzalwhistleMenu.UNIVERSE));
	}

	private static Widget quetzalWhistleDestinationWidget(String destinationName)
	{
		for (int rootId : new int[] {
			InterfaceID.QuetzalwhistleMenu.ICONS,
			InterfaceID.QuetzalwhistleMenu.MAP,
			InterfaceID.QuetzalwhistleMenu.SCROLL,
			InterfaceID.QuetzalwhistleMenu.CONTENTS})
		{
			Widget root = Rs2Widget.getWidget(rootId);
			if (!visible(root))
			{
				continue;
			}
			Widget destination = Rs2Widget.findWidget(destinationName, List.of(root), true);
			if (destination != null)
			{
				return destination;
			}
		}
		return null;
	}

	private static boolean isLocked(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			String text = widget.getText();
			return widget.isHidden() || text != null && text.toLowerCase().contains("<str>");
		}).orElse(true);
	}

	private static int firstItemId(Transport transport)
	{
		return transport.getItemIdRequirements().stream().flatMap(Set::stream)
			.findFirst().orElse(-1);
	}
}
