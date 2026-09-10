package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.Runes;

/** Pure selection shared by future bank-aware spell eligibility and equipment execution. */
public final class BankedSpellEquipmentPlanner
{
	private BankedSpellEquipmentPlanner() { }

	/**
	 * availableWithoutWeapon contains physical inventory/pouch runes and any independent
	 * infinite supplies, but excludes the current weapon's contribution. Staff IDs must
	 * represent actual available items, not the entire staff catalogue.
	 */
	public static Plan choose(List<Map<Runes, Integer>> casts,
		Map<Runes, Integer> availableWithoutWeapon, Map<Runes, Integer> bankRunes,
		Set<Integer> availableStaffIds, Rs2Staff wornStaff,
		int realAttackLevel, int realMagicLevel, boolean membersWorld)
	{
		Objects.requireNonNull(wornStaff, "wornStaff");
		Plan unchanged = evaluate(casts, availableWithoutWeapon, bankRunes, wornStaff, membersWorld);
		if (unchanged != null) return unchanged;
		for (Rs2Staff staff : Rs2Staff.values())
		{
			if (staff == wornStaff || !availableStaffIds.contains(staff.getItemID())
				|| !staff.canEquip(realAttackLevel, realMagicLevel, membersWorld)) continue;
			Plan candidate = evaluate(casts, availableWithoutWeapon, bankRunes, staff, membersWorld);
			if (candidate != null) return candidate;
		}
		return null;
	}

	private static Plan evaluate(List<Map<Runes, Integer>> casts,
		Map<Runes, Integer> availableWithoutWeapon, Map<Runes, Integer> bankRunes,
		Rs2Staff staff, boolean membersWorld)
	{
		Map<Runes, Integer> supply = new EnumMap<>(Runes.class);
		supply.putAll(availableWithoutWeapon);
		if (!staff.isMembersOnly() || membersWorld)
		{
			staff.getRunes().forEach(rune -> supply.put(rune, Integer.MAX_VALUE));
		}
		Map<Integer, Integer> withdrawals =
			Rs2WalkerBankingPlanner.planRuneWithdrawals(casts, supply, bankRunes);
		for (Map.Entry<Integer, Integer> entry : withdrawals.entrySet())
		{
			Runes rune = Runes.byItemId(entry.getKey());
			if (rune == null || entry.getValue() > bankRunes.getOrDefault(rune, 0)) return null;
		}
		return new Plan(staff, withdrawals);
	}

	public static final class Plan
	{
		private final Rs2Staff staff;
		private final Map<Integer, Integer> runeWithdrawals;

		private Plan(Rs2Staff staff, Map<Integer, Integer> runeWithdrawals)
		{
			this.staff = staff;
			this.runeWithdrawals = Map.copyOf(runeWithdrawals);
		}

		public Rs2Staff getStaff() { return staff; }
		public Map<Integer, Integer> getRuneWithdrawals() { return runeWithdrawals; }
	}
}
