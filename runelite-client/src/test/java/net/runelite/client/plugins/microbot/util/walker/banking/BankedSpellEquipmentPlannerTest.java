package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BankedSpellEquipmentPlannerTest
{
	@Test
	public void everyStaffCanCoverItsElementsWhenOwnedAndEquipable()
	{
		for (Rs2Staff staff : Rs2Staff.values())
		{
			if (staff == Rs2Staff.NONE) continue;
			Map<Runes, Integer> cast = staff.getRunes().stream()
				.collect(Collectors.toMap(rune -> rune, rune -> 3));
			BankedSpellEquipmentPlanner.Plan plan = BankedSpellEquipmentPlanner.choose(
				List.of(cast, cast), Map.of(), Map.of(), Set.of(staff.getItemID()), Rs2Staff.NONE,
				staff.getRequiredAttackLevel(), staff.getRequiredMagicLevel(), true);
			assertNotNull(staff.name(), plan);
			assertEquals(staff, plan.getStaff());
			assertEquals(Map.of(), plan.getRuneWithdrawals());
			assertNull(BankedSpellEquipmentPlanner.choose(List.of(cast), Map.of(), Map.of(),
				Set.of(), Rs2Staff.NONE, 99, 99, true));
		}
	}

	@Test
	public void sufficientRunesPreferNoWeaponChange()
	{
		BankedSpellEquipmentPlanner.Plan plan = BankedSpellEquipmentPlanner.choose(
			List.of(Map.of(Runes.AIR, 3, Runes.LAW, 1)), Map.of(),
			Map.of(Runes.AIR, 3, Runes.LAW, 1), Set.of(Rs2Staff.STAFF_OF_AIR.getItemID()),
			Rs2Staff.NONE, 99, 99, true);
		assertEquals(Rs2Staff.NONE, plan.getStaff());
		assertEquals(Map.of(Runes.AIR.getItemId(), 3, Runes.LAW.getItemId(), 1), plan.getRuneWithdrawals());
	}

	@Test
	public void cannotReplaceOneStaffWhileKeepingItsInfiniteRunes()
	{
		assertNull(BankedSpellEquipmentPlanner.choose(List.of(Map.of(Runes.AIR, 1, Runes.FIRE, 1)),
			Map.of(), Map.of(), Set.of(Rs2Staff.STAFF_OF_FIRE.getItemID()), Rs2Staff.STAFF_OF_AIR,
			99, 99, true));
	}

	@Test
	public void missingCatalyticRunesRejectOtherwiseUsefulStaff()
	{
		assertNull(BankedSpellEquipmentPlanner.choose(List.of(Map.of(Runes.AIR, 1, Runes.LAW, 1)),
			Map.of(), Map.of(), Set.of(Rs2Staff.STAFF_OF_AIR.getItemID()), Rs2Staff.NONE,
			99, 99, true));
	}

	@Test
	public void realLevelsAndMembershipGateNewEquipment()
	{
		Set<Integer> staves = Set.of(Rs2Staff.MYSTIC_AIR_STAFF.getItemID());
		List<Map<Runes, Integer>> casts = List.of(Map.of(Runes.AIR, 1));
		assertNull(BankedSpellEquipmentPlanner.choose(casts, Map.of(), Map.of(), staves,
			Rs2Staff.NONE, 39, 99, true));
		assertNull(BankedSpellEquipmentPlanner.choose(casts, Map.of(), Map.of(), staves,
			Rs2Staff.NONE, 99, 39, true));
		assertNull(BankedSpellEquipmentPlanner.choose(casts, Map.of(), Map.of(), staves,
			Rs2Staff.NONE, 99, 99, false));
	}

	@Test
	public void independentSupplyAndPouchCountsSurviveAWeaponChange()
	{
		BankedSpellEquipmentPlanner.Plan plan = BankedSpellEquipmentPlanner.choose(
			List.of(Map.of(Runes.AIR, 1, Runes.FIRE, 1, Runes.LAW, 2)),
			Map.of(Runes.FIRE, Integer.MAX_VALUE, Runes.LAW, 1), Map.of(Runes.LAW, 1),
			Set.of(Rs2Staff.STAFF_OF_AIR.getItemID()), Rs2Staff.NONE, 1, 1, true);
		assertEquals(Rs2Staff.STAFF_OF_AIR, plan.getStaff());
		assertEquals(Map.of(Runes.LAW.getItemId(), 1), plan.getRuneWithdrawals());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void resultCannotBeMutatedByConsumers()
	{
		BankedSpellEquipmentPlanner.choose(List.of(), Map.of(), Map.of(), Set.of(),
			Rs2Staff.NONE, 1, 1, false).getRuneWithdrawals().put(Runes.LAW.getItemId(), 1);
	}
}
