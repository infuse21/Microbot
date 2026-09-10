package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.Set;

/** Confirmed, coherent equipment/inventory snapshot; a missing snapshot is represented by null. */
public final class SpellEquipmentObservation
{
	private final int weaponId;
	private final int offhandId;
	private final Set<Integer> inventoryIds;
	private final int freeInventorySlots;
	private final boolean inventoryReady;
	private final boolean equipmentReady;

	public SpellEquipmentObservation(int weaponId, int offhandId, Set<Integer> inventoryIds,
		int freeInventorySlots)
	{
		this(weaponId, offhandId, inventoryIds, freeInventorySlots, true, true);
	}

	public SpellEquipmentObservation(int weaponId, int offhandId, Set<Integer> inventoryIds,
		int freeInventorySlots, boolean inventoryReady, boolean equipmentReady)
	{
		this.weaponId = weaponId;
		this.offhandId = offhandId;
		this.inventoryIds = Set.copyOf(inventoryIds);
		this.freeInventorySlots = Math.max(0, freeInventorySlots);
		this.inventoryReady = inventoryReady;
		this.equipmentReady = equipmentReady;
	}

	public SpellEquipmentTransaction.Action restorationAction(SpellEquipmentTransaction transaction)
	{
		SpellEquipmentTransaction.Action action = transaction.restore(
			weaponId, offhandId, inventoryIds, freeInventorySlots, false);
		if (action == SpellEquipmentTransaction.Action.RESTORE_WEAPON && !inventoryReady)
			return SpellEquipmentTransaction.Action.OPEN_INVENTORY;
		if (action == SpellEquipmentTransaction.Action.REMOVE_STAFF && !equipmentReady)
			return SpellEquipmentTransaction.Action.OPEN_EQUIPMENT;
		return action;
	}
	public int getWeaponId() { return weaponId; }
	public SpellEquipmentTransaction.Action preparationAction(SpellEquipmentTransaction transaction,
		boolean equipable)
	{
		SpellEquipmentTransaction.Action action = transaction.beforeCast(
			weaponId, offhandId, inventoryIds, equipable, false);
		return action == SpellEquipmentTransaction.Action.EQUIP_STAFF && !inventoryReady
			? SpellEquipmentTransaction.Action.OPEN_INVENTORY : action;
	}
	public int getOffhandId() { return offhandId; }
	public Set<Integer> getInventoryIds() { return inventoryIds; }
}
