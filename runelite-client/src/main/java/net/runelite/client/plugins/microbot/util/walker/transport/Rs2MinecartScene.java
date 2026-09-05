package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinecartTransport;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed, non-blocking adapter for minecart equipment, object, and menu stages. */
public final class Rs2MinecartScene implements MinecartScene
{
	private static final int TAB_SWITCH_SCRIPT = 915;

	@Override
	public MinecartTransport find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		return transport == null ? null : resolveStage(transport, null);
	}

	@Override
	public MinecartTransport observe(PlannedEdge edge, String pendingAction,
		int catalogObjectId)
	{
		Transport transport = findTransport(edge);
		return transport == null || transport.getObjectId() != catalogObjectId
			? null : resolveStage(transport, pendingAction);
	}

	@Override
	public MinecartTransport restore(PlannedEdge edge, String pendingAction,
		int catalogObjectId, WorldPoint player)
	{
		if (edge == null || edge.from() == null || edge.to() == null || player == null
			|| !MinecartPolicy.hasEncodedEquipment(pendingAction))
		{
			return null;
		}
		int weaponId = MinecartPolicy.originalWeaponId(pendingAction);
		int shieldId = MinecartPolicy.originalShieldId(pendingAction);
		boolean weaponWorn = weaponId > 0 && Rs2Equipment.isWearing(weaponId);
		boolean shieldWorn = shieldId > 0 && Rs2Equipment.isWearing(shieldId);
		if (restorationComplete(weaponId, shieldId, weaponWorn, shieldWorn))
		{
			return null;
		}
		int currentWeapon = equippedId(EquipmentInventorySlot.WEAPON);
		int currentShield = equippedId(EquipmentInventorySlot.SHIELD);
		if (currentWeapon > 0 && currentWeapon != weaponId
			|| currentShield > 0 && currentShield != shieldId)
		{
			return stage(edge, catalogObjectId, MinecartPolicy.UNAVAILABLE_ACTION,
				MinecartTransport.Stage.UNAVAILABLE, weaponId, shieldId, false, player);
		}
		if (weaponId > 0 && currentWeapon != weaponId)
		{
			return restoreItemStage(edge, catalogObjectId, pendingAction, weaponId,
				shieldId, true, player);
		}
		return restoreItemStage(edge, catalogObjectId, pendingAction, weaponId,
			shieldId, false, player);
	}

	static boolean restorationComplete(int weaponId, int shieldId,
		boolean weaponWorn, boolean shieldWorn)
	{
		return (weaponId <= 0 || weaponWorn) && (shieldId <= 0 || shieldWorn);
	}

	public static boolean dispatch(PlannedEdge edge, String action, int catalogObjectId)
	{
		if (MinecartPolicy.isUnequipWeaponAction(action))
		{
			return unEquipExpected(EquipmentInventorySlot.WEAPON,
				MinecartPolicy.originalWeaponId(action));
		}
		if (MinecartPolicy.isUnequipShieldAction(action))
		{
			return unEquipExpected(EquipmentInventorySlot.SHIELD,
				MinecartPolicy.originalShieldId(action));
		}
		if (MinecartPolicy.isObjectAction(action))
		{
			return interactObject(edge, MinecartPolicy.objectLiveAction(action),
				catalogObjectId);
		}
		if (MinecartPolicy.isDestinationAction(action))
		{
			Widget destination = findDestinationWidget(
				MinecartPolicy.destinationDisplayInfo(action));
			return destination != null && Rs2Widget.clickWidget(destination);
		}
		if (MinecartPolicy.isRestoreOpenAction(action))
		{
			return forceOpenInventory();
		}
		if (MinecartPolicy.isRestoreWeaponAction(action))
		{
			int itemId = MinecartPolicy.originalWeaponId(action);
			return itemId > 0 && Rs2Inventory.equip(itemId);
		}
		if (MinecartPolicy.isRestoreShieldAction(action))
		{
			int itemId = MinecartPolicy.originalShieldId(action);
			return itemId > 0 && Rs2Inventory.equip(itemId);
		}
		return false;
	}

	private static MinecartTransport resolveStage(Transport transport,
		String pendingAction)
	{
		if (MinecartPolicy.hasDestinationMenu(transport) && menuVisible())
		{
			String action = MinecartPolicy.destinationAction(transport.getDisplayInfo(),
				MinecartPolicy.originalWeaponId(pendingAction),
				MinecartPolicy.originalShieldId(pendingAction));
			return stage(transport, transport.getOrigin(), action,
				findDestinationWidget(transport.getDisplayInfo()) == null
					? MinecartTransport.Stage.DESTINATION_UNAVAILABLE
					: MinecartTransport.Stage.DESTINATION,
				MinecartPolicy.originalWeaponId(action),
				MinecartPolicy.originalShieldId(action), true, null);
		}
		if (MinecartPolicy.isObjectAction(pendingAction)
			|| MinecartPolicy.isDestinationAction(pendingAction))
		{
			return null;
		}
		MinecartObject object = findObject(transport);
		if (object == null)
		{
			return null;
		}
		int weaponId = MinecartPolicy.hasEncodedEquipment(pendingAction)
			? MinecartPolicy.originalWeaponId(pendingAction)
			: MinecartPolicy.isTrainCart(transport)
				? equippedId(EquipmentInventorySlot.WEAPON) : -1;
		int shieldId = MinecartPolicy.hasEncodedEquipment(pendingAction)
			? MinecartPolicy.originalShieldId(pendingAction)
			: MinecartPolicy.isTrainCart(transport)
				? equippedId(EquipmentInventorySlot.SHIELD) : -1;
		MinecartTransport preparation = preparationStage(transport, object,
			weaponId, shieldId);
		if (preparation != null)
		{
			return preparation;
		}
		return new MinecartTransport(object.object, object.tile,
			transport.getObjectId(), MinecartPolicy.objectAction(object.action,
				weaponId, shieldId), transport.getOrigin(), transport.getDestination(),
			MinecartTransport.Stage.OBJECT, weaponId, shieldId, true);
	}

	private static MinecartTransport preparationStage(Transport transport,
		MinecartObject object, int weaponId, int shieldId)
	{
		if (!MinecartPolicy.isTrainCart(transport))
		{
			return null;
		}
		int currentWeapon = equippedId(EquipmentInventorySlot.WEAPON);
		int currentShield = equippedId(EquipmentInventorySlot.SHIELD);
		if (currentWeapon > 0 && currentWeapon != weaponId
			|| currentShield > 0 && currentShield != shieldId)
		{
			return stage(transport, object.tile, MinecartPolicy.UNAVAILABLE_ACTION,
				MinecartTransport.Stage.UNAVAILABLE, weaponId, shieldId, false,
				object.object);
		}
		if (weaponId > 0 && currentWeapon == weaponId)
		{
			return stage(transport, object.tile,
				MinecartPolicy.unequipWeaponAction(weaponId, shieldId),
				MinecartTransport.Stage.UNEQUIP_WEAPON, weaponId, shieldId, true,
				object.object);
		}
		if (weaponId > 0 && !Rs2Inventory.hasItem(weaponId))
		{
			return stage(transport, object.tile,
				MinecartPolicy.unequipWeaponAction(weaponId, shieldId),
				MinecartTransport.Stage.UNEQUIP_WEAPON, weaponId, shieldId, false,
				object.object);
		}
		if (shieldId > 0 && currentShield == shieldId)
		{
			return stage(transport, object.tile,
				MinecartPolicy.unequipShieldAction(weaponId, shieldId),
				MinecartTransport.Stage.UNEQUIP_SHIELD, weaponId, shieldId, true,
				object.object);
		}
		if (shieldId > 0 && !Rs2Inventory.hasItem(shieldId))
		{
			return stage(transport, object.tile,
				MinecartPolicy.unequipShieldAction(weaponId, shieldId),
				MinecartTransport.Stage.UNEQUIP_SHIELD, weaponId, shieldId, false,
				object.object);
		}
		return null;
	}

	private static MinecartTransport restoreItemStage(PlannedEdge edge,
		int catalogObjectId, String pendingAction, int weaponId, int shieldId,
		boolean weapon, WorldPoint player)
	{
		int itemId = weapon ? weaponId : shieldId;
		if (itemId <= 0)
		{
			return null;
		}
		if (inventoryContainerReady() && Rs2Inventory.hasItem(itemId))
		{
			String action = weapon
				? MinecartPolicy.restoreWeaponAction(weaponId, shieldId)
				: MinecartPolicy.restoreShieldAction(weaponId, shieldId);
			return stage(edge, catalogObjectId, action, weapon
				? MinecartTransport.Stage.RESTORE_WEAPON
				: MinecartTransport.Stage.RESTORE_SHIELD,
				weaponId, shieldId, true, player);
		}
		String action = MinecartPolicy.restoreOpenAction(weaponId, shieldId);
		return stage(edge, catalogObjectId, action,
			MinecartTransport.Stage.RESTORE_INVENTORY, weaponId, shieldId,
			!action.equals(pendingAction), player);
	}

	private static boolean interactObject(PlannedEdge edge, String expectedAction,
		int catalogObjectId)
	{
		Transport transport = findTransport(edge);
		MinecartObject object = transport == null
			|| transport.getObjectId() != catalogObjectId ? null : findObject(transport);
		return object != null && expectedAction != null
			&& expectedAction.equalsIgnoreCase(object.action)
			&& object.object.click(object.action);
	}

	private static MinecartObject findObject(Transport transport)
	{
		return Microbot.getRs2TileObjectCache().query()
			.within(transport.getOrigin(), MinecartPolicy.LIVE_OBJECT_ORIGIN_TOLERANCE)
			.toList().stream().map(candidate -> minecartObject(transport, candidate))
			.filter(Objects::nonNull)
			.min(Comparator.comparingInt(candidate ->
				candidate.tile.distanceTo2D(transport.getOrigin())))
			.orElse(null);
	}

	private static MinecartObject minecartObject(Transport transport,
		Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		List<String> actions = composition == null || composition.getActions() == null
			? Collections.emptyList() : Arrays.stream(composition.getActions())
				.filter(Objects::nonNull).collect(Collectors.toList());
		String name = composition == null ? null : composition.getName();
		String action = MinecartPolicy.exactAction(actions, transport.getAction());
		return action != null && MinecartPolicy.isLiveObjectMatch(transport,
			object.getId(), name, actions, object.getWorldLocation())
			? new MinecartObject(object, object.getWorldLocation(), action) : null;
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(MinecartPolicy::isEligible).findFirst().orElse(null);
	}

	private static boolean menuVisible()
	{
		Widget choices = choicesWidget();
		return choices != null && !choices.isHidden()
			|| !Rs2Widget.isHidden(ComponentID.ADVENTURE_LOG_CONTAINER);
	}

	private static Widget findDestinationWidget(String displayInfo)
	{
		Widget choices = choicesWidget();
		return choices == null ? null : Rs2Widget.findWidget(
			MinecartPolicy.destinationLabel(displayInfo), List.of(choices), false);
	}

	private static Widget choicesWidget()
	{
		Widget choices = Rs2Widget.getWidget(InterfaceID.MenuNew.TEXT);
		return choices != null ? choices : Rs2Widget.getWidget(187, 3);
	}

	private static boolean unEquipExpected(EquipmentInventorySlot slot, int itemId)
	{
		return itemId > 0 && equippedId(slot) == itemId && Rs2Equipment.unEquip(slot);
	}

	private static int equippedId(EquipmentInventorySlot slot)
	{
		Rs2ItemModel item = Rs2Equipment.get(slot);
		return item == null ? -1 : item.getId();
	}

	private static boolean forceOpenInventory()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Microbot.getClient().runScript(TAB_SWITCH_SCRIPT,
				InterfaceTab.INVENTORY.getVarcIntIndex());
			return true;
		}).orElse(false);
	}

	private static boolean inventoryContainerReady()
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget inventory = Microbot.getClient().getWidget(
				ComponentID.INVENTORY_CONTAINER);
			return inventory != null && !inventory.isHidden()
				&& inventory.getChildren() != null;
		}).orElse(false);
	}

	private static MinecartTransport stage(Transport transport, WorldPoint tile,
		String action, MinecartTransport.Stage stage, int weaponId, int shieldId,
		boolean ready, Rs2TileObjectModel object)
	{
		return new MinecartTransport(object, tile, transport.getObjectId(), action,
			transport.getOrigin(), transport.getDestination(), stage, weaponId,
			shieldId, ready);
	}

	private static MinecartTransport stage(PlannedEdge edge, int catalogObjectId,
		String action, MinecartTransport.Stage stage, int weaponId, int shieldId,
		boolean ready, WorldPoint player)
	{
		return new MinecartTransport(null, player, catalogObjectId, action,
			edge.from(), edge.to(), stage, weaponId, shieldId, ready);
	}

	private static final class MinecartObject
	{
		private final Rs2TileObjectModel object;
		private final WorldPoint tile;
		private final String action;

		private MinecartObject(Rs2TileObjectModel object, WorldPoint tile, String action)
		{
			this.object = object;
			this.tile = tile;
			this.action = action;
		}
	}
}
