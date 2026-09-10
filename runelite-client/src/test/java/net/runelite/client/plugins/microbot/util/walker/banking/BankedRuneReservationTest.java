package net.runelite.client.plugins.microbot.util.walker.banking;

import java.util.List;
import java.util.Map;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BankedRuneReservationTest
{
	@Test
	public void distinctCastsConsumeDistinctCombinationRunes()
	{
		assertEquals(Map.of(Runes.LAVA.getItemId(), 3), plan(
			List.of(Map.of(Runes.FIRE, 1), Map.of(Runes.EARTH, 2)), Map.of(), Map.of(Runes.LAVA, 3)));
	}

	@Test
	public void oneCastStillBenefitsFromBothElements()
	{
		assertEquals(Map.of(Runes.LAVA.getItemId(), 2), plan(
			List.of(Map.of(Runes.FIRE, 1, Runes.EARTH, 2)), Map.of(), Map.of(Runes.LAVA, 3)));
	}

	@Test
	public void carriedOrPouchedCombinationsAreConsumedOnlyOnce()
	{
		assertEquals(Map.of(Runes.LAVA.getItemId(), 2), plan(
			List.of(Map.of(Runes.FIRE, 1), Map.of(Runes.EARTH, 2)),
			Map.of(Runes.LAVA, 1), Map.of(Runes.LAVA, 2)));
	}

	@Test
	public void repeatedCastsRetainTheirSeparateCapacity()
	{
		Map<Runes, Integer> cast = Map.of(Runes.FIRE, 1, Runes.EARTH, 2);
		assertEquals(Map.of(Runes.LAVA.getItemId(), 3), plan(
			List.of(cast, cast), Map.of(Runes.LAVA, 1), Map.of(Runes.LAVA, 3)));
	}

	@Test
	public void exhaustedCombinationStockLeavesAnUnsatisfiedRequirement()
	{
		assertEquals(Map.of(Runes.LAVA.getItemId(), 2, Runes.EARTH.getItemId(), 1), plan(
			List.of(Map.of(Runes.FIRE, 1), Map.of(Runes.EARTH, 2)), Map.of(), Map.of(Runes.LAVA, 2)));
	}

	@Test
	public void equipmentSuppliesRemainInfiniteAcrossCasts()
	{
		assertTrue(plan(List.of(Map.of(Runes.AIR, 5), Map.of(Runes.AIR, 5)),
			Map.of(Runes.AIR, Integer.MAX_VALUE), Map.of()).isEmpty());
		assertEquals(Map.of(Runes.LAVA.getItemId(), 2), plan(
			List.of(Map.of(Runes.FIRE, 1), Map.of(Runes.EARTH, 2)),
			Map.of(Runes.FIRE, Integer.MAX_VALUE), Map.of(Runes.LAVA, 2)));
	}

	@Test
	public void laterWithdrawalsCanBeConsumedDuringEarlierCasts()
	{
		assertEquals(Map.of(Runes.LAVA.getItemId(), 3), plan(
			List.of(Map.of(Runes.FIRE, 1), Map.of(Runes.EARTH, 2)),
			Map.of(Runes.FIRE, 1), Map.of(Runes.LAVA, 3)));
	}

	@Test
	public void ordinaryRuneInventoryShortfallIsSubtractedAcrossCasts()
	{
		assertEquals(Map.of(Runes.LAW.getItemId(), 2), plan(
			List.of(Map.of(Runes.LAW, 1), Map.of(Runes.LAW, 2)),
			Map.of(Runes.LAW, 1), Map.of(Runes.LAW, 2)));
	}

	private static Map<Integer, Integer> plan(List<Map<Runes, Integer>> casts,
		Map<Runes, Integer> carried, Map<Runes, Integer> bank)
	{
		// All inputs are immutable, including nested maps: the planner must own its reservations.
		return Rs2WalkerBankingPlanner.planRuneWithdrawals(casts, carried, bank);
	}
}
