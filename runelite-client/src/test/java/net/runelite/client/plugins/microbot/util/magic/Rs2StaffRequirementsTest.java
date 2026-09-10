package net.runelite.client.plugins.microbot.util.magic;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2StaffRequirementsTest
{
	@Test
	public void everySupportedStaffHasAnExplicitEquipBoundary()
	{
		int basic = 0;
		int battle = 0;
		int mystic = 0;
		int twinflame = 0;
		for (Rs2Staff staff : Rs2Staff.values())
		{
			if (staff == Rs2Staff.NONE) continue;
			int attack;
			int magic;
			boolean members;
			if (staff.name().startsWith("STAFF_OF_"))
			{
				basic++;
				attack = 0;
				magic = 0;
				members = false;
			}
			else if (staff == Rs2Staff.TWINFLAME_STAFF)
			{
				twinflame++;
				attack = 0;
				magic = 60;
				members = true;
			}
			else if (staff.name().startsWith("MYSTIC_"))
			{
				mystic++;
				attack = 40;
				magic = 40;
				members = true;
			}
			else
			{
				assertTrue(staff.name(), staff.name().endsWith("BATTLESTAFF"));
				battle++;
				attack = 30;
				magic = 30;
				members = true;
			}
			assertEquals(attack, staff.getRequiredAttackLevel());
			assertEquals(magic, staff.getRequiredMagicLevel());
			assertEquals(members, staff.isMembersOnly());
			assertTrue(staff.name(), staff.canEquip(attack, magic, true));
			assertEquals(!members, staff.canEquip(99, 99, false));
			if (attack > 0) assertFalse(staff.canEquip(attack - 1, 99, true));
			if (magic > 0) assertFalse(staff.canEquip(99, magic - 1, true));
			assertFalse(staff.getRunes().isEmpty());
			assertEquals(staff, Rs2Staff.byItemId(staff.getItemID()));
		}
		assertEquals(4, basic);
		assertEquals(10, battle);
		assertEquals(10, mystic);
		assertEquals(1, twinflame);
	}

	@Test
	public void noneAndUnknownIdsCannotSupplyAnEquipCandidate()
	{
		assertFalse(Rs2Staff.NONE.canEquip(99, 99, true));
		assertEquals(Rs2Staff.NONE, Rs2Staff.byItemId(-1));
		assertEquals(Rs2Staff.NONE, Rs2Staff.byItemId(1391));
	}
}
