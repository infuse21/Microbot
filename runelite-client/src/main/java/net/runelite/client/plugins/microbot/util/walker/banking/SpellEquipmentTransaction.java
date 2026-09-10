package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.Set;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;

/**
 * Immutable equipment obligation, independent of a route generation or teleport landing.
 * Callers supply confirmed equipment snapshots; -1 denotes an observed empty slot, not
 * an unavailable cache. Returned actions are intentions, never input acknowledgements.
 */
public final class SpellEquipmentTransaction
{
	public enum Action
	{
		STOP,
		WAIT,
		CONFLICT,
		EQUIP_STAFF,
		READY_TO_CAST,
		RESTORE_WEAPON,
		REMOVE_STAFF,
		OPEN_INVENTORY,
		OPEN_EQUIPMENT,
		RESTORED
	}

	private final Rs2Staff staff;
	private final int originalWeaponId;
	private final int originalOffhandId;

	public SpellEquipmentTransaction(Rs2Staff staff, int originalWeaponId, int originalOffhandId)
	{
		if (staff == null || staff == Rs2Staff.NONE)
		{
			throw new IllegalArgumentException("A staff is required for an equipment transaction");
		}
		this.staff = staff;
		this.originalWeaponId = originalWeaponId > 0 ? originalWeaponId : -1;
		this.originalOffhandId = originalOffhandId > 0 ? originalOffhandId : -1;
	}

	public Action beforeCast(int weaponId, int offhandId, Set<Integer> inventoryIds,
		boolean staffEquipable, boolean cancelled)
	{
		if (cancelled) return Action.STOP;
		if (normalize(offhandId) != originalOffhandId) return Action.CONFLICT;
		if (weaponId == staff.getItemID()) return Action.READY_TO_CAST;
		if (normalize(weaponId) != originalWeaponId) return Action.CONFLICT;
		if (!staffEquipable || !inventoryIds.contains(staff.getItemID())) return Action.WAIT;
		return Action.EQUIP_STAFF;
	}

	public Action restore(int weaponId, int offhandId, Set<Integer> inventoryIds,
		int freeInventorySlots, boolean cancelled)
	{
		if (cancelled) return Action.STOP;
		if (normalize(offhandId) != originalOffhandId) return Action.CONFLICT;
		if (normalize(weaponId) == originalWeaponId) return Action.RESTORED;
		if (weaponId != staff.getItemID()) return Action.CONFLICT;
		if (originalWeaponId == -1)
		{
			return freeInventorySlots > 0 ? Action.REMOVE_STAFF : Action.WAIT;
		}
		return inventoryIds.contains(originalWeaponId) ? Action.RESTORE_WEAPON : Action.WAIT;
	}

	private static int normalize(int itemId) { return itemId > 0 ? itemId : -1; }
	public Rs2Staff getStaff() { return staff; }
	public int getOriginalWeaponId() { return originalWeaponId; }
	public int getOriginalOffhandId() { return originalOffhandId; }
}
