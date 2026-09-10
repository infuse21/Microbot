package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.Set;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import org.junit.Test;
import static net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction.Action.*;
import static org.junit.Assert.assertEquals;

public class SpellEquipmentTransactionTest
{
	@Test
	public void everyStaffRequiresObservedEquipAndObservedRestoration()
	{
		for (Rs2Staff staff : Rs2Staff.values())
		{
			if (staff == Rs2Staff.NONE) continue;
			int id = staff.getItemID();
			SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(staff, 4151, 1540);
			assertEquals(EQUIP_STAFF, transaction.beforeCast(4151, 1540, Set.of(id), true, false));
			assertEquals(EQUIP_STAFF, transaction.beforeCast(4151, 1540, Set.of(id), true, false));
			assertEquals(READY_TO_CAST, transaction.beforeCast(id, 1540, Set.of(4151), true, false));
			assertEquals(RESTORE_WEAPON, transaction.restore(id, 1540, Set.of(4151), 0, false));
			assertEquals(RESTORE_WEAPON, transaction.restore(id, 1540, Set.of(4151), 0, false));
			assertEquals(RESTORED, transaction.restore(4151, 1540, Set.of(id), 0, false));
		}
	}

	@Test
	public void emptyOriginalWeaponRequiresRemovalAndSpace()
	{
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, -1, -1);
		int id = Rs2Staff.STAFF_OF_AIR.getItemID();
		assertEquals(WAIT, transaction.restore(id, -1, Set.of(), 0, false));
		assertEquals(REMOVE_STAFF, transaction.restore(id, -1, Set.of(), 1, false));
		assertEquals(RESTORED, transaction.restore(-1, -1, Set.of(id), 0, false));
	}

	@Test
	public void cancellationNeverRequestsInputAndDoesNotDestroyTheObligation()
	{
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, 4151, -1);
		int id = Rs2Staff.STAFF_OF_AIR.getItemID();
		assertEquals(STOP, transaction.beforeCast(4151, -1, Set.of(id), true, true));
		assertEquals(STOP, transaction.restore(id, -1, Set.of(4151), 1, true));
		assertEquals(4151, transaction.getOriginalWeaponId());
		assertEquals(RESTORE_WEAPON, transaction.restore(id, -1, Set.of(4151), 1, false));
	}

	@Test
	public void manualEquipmentChangesAreNotOverwritten()
	{
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, 4151, -1);
		int id = Rs2Staff.STAFF_OF_AIR.getItemID();
		assertEquals(CONFLICT, transaction.beforeCast(1305, -1, Set.of(id), true, false));
		assertEquals(CONFLICT, transaction.restore(1305, -1, Set.of(4151), 1, false));
		assertEquals(CONFLICT, transaction.restore(id, 1540, Set.of(4151), 1, false));
	}

	@Test
	public void missingEquipmentOrEligibilityWaitsWithoutAcknowledgement()
	{
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, 4151, -1);
		int id = Rs2Staff.STAFF_OF_AIR.getItemID();
		assertEquals(WAIT, transaction.beforeCast(4151, -1, Set.of(), true, false));
		assertEquals(WAIT, transaction.beforeCast(4151, -1, Set.of(id), false, false));
		assertEquals(WAIT, transaction.restore(id, -1, Set.of(), 1, false));
	}

	@Test
	public void alreadyWornStaffNeedsNoSwapOrRemoval()
	{
		int id = Rs2Staff.STAFF_OF_AIR.getItemID();
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, id, -1);
		assertEquals(READY_TO_CAST, transaction.beforeCast(id, -1, Set.of(), false, false));
		assertEquals(RESTORED, transaction.restore(id, -1, Set.of(), 0, false));
	}
}
