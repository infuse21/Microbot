package net.runelite.client.plugins.microbot.util.walker.banking;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Item-gated plain transports must take part in bank planning.
 *
 * <p>Only currency-bearing {@link TransportType#TRANSPORT} entries used to qualify, so a machete for
 * a jungle bush, a pickaxe for a Motherlode rockfall or a Shantay pass fell through
 * {@code hasRequiredTransportItems} to its catch-all {@code return true}, was never counted as
 * missing, and was never withdrawn.
 *
 * <p>That contradicted the pathfinder, which <em>does</em> count bank contents when
 * {@code useBankItems} is set — so a route was planned through the obstacle on the strength of a
 * banked item the planner then refused to fetch, stranding the walk at the obstacle.
 */
public class BankedTransportItemPlanningTest {

    private static List<Transport> all;

    @BeforeClass
    public static void load() {
        HashMap<WorldPoint, Set<Transport>> transports = Transport.loadAllFromResources();
        all = transports.values().stream().flatMap(Set::stream).collect(Collectors.toList());
    }

    /** {@code menuOption;menuTarget} for readable assertion messages. */
    private static String describe(Transport t) {
        String action = t.getAction() == null ? "" : t.getAction();
        String name = t.getName() == null ? "" : t.getName();
        return action + ";" + name;
    }

    private static List<Transport> matching(String menuFragment) {
        return all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> {
                    String target = describe(t);
                    return target.toLowerCase().contains(menuFragment.toLowerCase());
                })
                .collect(Collectors.toList());
    }

    @Test
    public void itemGatedPlainTransportsNowQualifyForPlanning() {
        List<Transport> itemGated = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> t.getItemIdRequirements() != null && !t.getItemIdRequirements().isEmpty())
                .filter(t -> t.getCurrencyAmount() <= 0)
                .collect(Collectors.toList());

        assertFalse("the data should contain item-gated plain transports (rockfalls, jungle bushes, "
                + "Shantay passes)", itemGated.isEmpty());

        for (Transport t : itemGated) {
            assertTrue("an item-gated plain transport must be eligible for bank planning, otherwise "
                            + "the pathfinder routes through it on a banked item nobody withdraws: " + describe(t),
                    Rs2WalkerBankingPlanner.planningCoversPlainTransport(t));
			assertTrue("the shared route-analysis filter must retain the same item-gated row: "
						+ describe(t), Rs2WalkerBankingPlanner.requiresBankPlanning(t));
        }
    }

    private static Transport teleport(String displayInfo) {
        return all.stream()
                .filter(t -> displayInfo.equals(t.getDisplayInfo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("catalog transport missing: " + displayInfo));
    }

	@Test
	public void seasonalItemTeleportsParticipateInBankPlanning() {
		List<Transport> seasonal = all.stream()
				.filter(t -> t.getType() == TransportType.SEASONAL_TRANSPORT)
				.collect(Collectors.toList());
		assertEquals(169, seasonal.size());
		assertTrue(seasonal.stream().allMatch(Rs2WalkerBankingPlanner::requiresBankPlanning));
	}

	@Test
	public void hotAirBalloonLogsParticipateInBankPlanning() {
		List<Transport> balloons = all.stream()
				.filter(t -> t.getType() == TransportType.HOT_AIR_BALLOON)
				.collect(Collectors.toList());
		Set<Integer> logIds = balloons.stream()
				.flatMap(t -> t.getItemIdRequirements().stream())
				.flatMap(Set::stream)
				.collect(Collectors.toSet());

		assertEquals(225, balloons.size());
		assertTrue(balloons.stream().allMatch(
				Rs2WalkerBankingPlanner::requiresBankPlanning));
		assertTrue(balloons.stream().allMatch(Transport::isConsumable));
		assertEquals(Set.of(ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS,
				ItemID.YEW_LOGS, ItemID.MAGIC_LOGS), logIds);
	}

	@Test
	public void repeatedHotAirBalloonTripsSumDestinationLogs() {
		Transport varrock = all.stream()
				.filter(t -> t.getType() == TransportType.HOT_AIR_BALLOON)
				.filter(t -> "Varrock".equals(t.getDisplayInfo()))
				.findFirst().orElseThrow(() ->
						new AssertionError("Varrock balloon row missing"));

		Map<Integer, Integer> requirements =
				Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
						List.of(varrock, varrock));

		assertEquals(2, requirements.getOrDefault(ItemID.WILLOW_LOGS, 0).intValue());
	}

	@Test
	public void repeatedNonConsumableSeasonalEdgesNeedOneItem() {
		Transport map = all.stream()
				.filter(t -> t.getType() == TransportType.SEASONAL_TRANSPORT)
				.filter(t -> t.getDisplayInfo().startsWith("Map of Alacrity:"))
				.findFirst().orElseThrow(() -> new AssertionError("Map of Alacrity row missing"));

		java.util.Map<Integer, Integer> requirements =
				Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(List.of(map, map));
		assertEquals("a reusable relic is withdrawn once, not once per route edge", 1,
				requirements.getOrDefault(33233, 0).intValue());
	}

    @Test
    public void repeatedConsumableTabletsAreAggregatedAcrossTheWholeRoute() {
        Transport tablet = teleport("Draynor Manor tablet");
        java.util.Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(tablet, tablet));

        assertEquals("two consumed route edges need two tablets",
                2, requirements.getOrDefault(19615, 0).intValue());
    }

	@Test
	public void repeatedMasterScrollBookEdgesWithdrawOneReusableBook()
	{
		Transport book = teleport("Master Scroll Book: Nardah");
		Map<Integer, Integer> requirements =
				Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
						List.of(book, book));

		assertTrue(book.isConsumable());
		assertEquals("stored charges are consumed, but the container is reusable", 1,
				requirements.getOrDefault(21389, 0).intValue());
	}

    @Test
    public void repeatedChargedJewelleryUsesAreConservativelyCovered() {
        Transport ring = teleport("Ring of dueling: Castle Wars");
        java.util.Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(ring, ring));

        assertEquals("until charge capacity is modeled explicitly, one banked item per route use "
                        + "is the safe headless requirement",
                2, requirements.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void mothAndWaystoneRequirementsExcludeInertDisplayVariants() {
        assertEquals(Set.of(Set.of(29090)),
                teleport("Calcified moth: Crush").getItemIdRequirements());
        assertEquals(Set.of(Set.of(31099)),
                teleport("Mokhaiotl waystone: Channel").getItemIdRequirements());
    }

    @Test
    public void repeatedMothsAndWaystonesConsumeOneRealItemPerUse() {
        for (String display : List.of("Calcified moth: Crush", "Mokhaiotl waystone: Channel")) {
            Transport row = teleport(display);
            int id = display.startsWith("Calcified") ? 29090 : 31099;
            Map<Integer, Integer> requirements =
                    Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(List.of(row, row));
            assertEquals(display, Map.of(id, 2), requirements);
            assertTrue(display, row.isConsumable());
        }
    }

    @Test
    public void calcifiedMothsAreNotPlannedAboveLevelTwentyWilderness() {
        assertEquals(20, teleport("Calcified moth: Crush").getMaxWildernessLevel());
    }

    @Test
    public void repeatedReusableCapeTeleportsNeedOneCape() {
        Transport cape = teleport("Crafting cape: Teleport");
        java.util.Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(cape, cape));

        assertEquals("a reusable cape covers every matching route edge",
                1, requirements.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void repeatedSpellEdgesAggregateTheirRunes() {
        Transport spell = teleport("Varrock Teleport");
        java.util.Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(spell, spell));

        assertEquals(6, requirements.getOrDefault(ItemID.AIRRUNE, 0).intValue());
        assertEquals(2, requirements.getOrDefault(ItemID.FIRERUNE, 0).intValue());
        assertEquals(2, requirements.getOrDefault(ItemID.LAWRUNE, 0).intValue());
    }

    @Test
    public void combinationRunesCanCoverBothElementalRequirements() {
        Map<Integer, Integer> withdrawals = Rs2WalkerBankingPlanner.planRuneWithdrawals(
                Map.of(Runes.AIR, 3, Runes.WATER, 2, Runes.LAW, 1),
                Map.of(),
                Map.of(Runes.MIST, 3, Runes.LAW, 1));

        assertEquals(3, withdrawals.getOrDefault(ItemID.MISTRUNE, 0).intValue());
        assertEquals(1, withdrawals.getOrDefault(ItemID.LAWRUNE, 0).intValue());
        assertFalse(withdrawals.containsKey(ItemID.AIRRUNE));
        assertFalse(withdrawals.containsKey(ItemID.WATERRUNE));
    }

    @Test
    public void runeSupplyFromAlreadyEquippedStaffNeedsNoWithdrawal() {
        Map<Integer, Integer> withdrawals = Rs2WalkerBankingPlanner.planRuneWithdrawals(
                Map.of(Runes.AIR, 3, Runes.LAW, 1),
                Map.of(Runes.AIR, Integer.MAX_VALUE),
                Map.of(Runes.AIR, 100, Runes.LAW, 1));

        assertFalse(withdrawals.containsKey(ItemID.AIRRUNE));
        assertEquals(1, withdrawals.getOrDefault(ItemID.LAWRUNE, 0).intValue());
    }

    @Test
    public void ectoTokenFaresAreAggregated() {
        Transport ectoBoat = all.stream()
                .filter(t -> t.getCurrencyAmount() == 25)
                .filter(t -> "Ecto-token".equalsIgnoreCase(t.getCurrencyName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("catalog ecto-token boat missing"));

        java.util.Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(ectoBoat, ectoBoat));
        assertEquals(50, requirements.getOrDefault(ItemID.ECTOTOKEN, 0).intValue());
        assertEquals("the reusable ghostspeak requirement is planned once alongside the fare",
                1, requirements.getOrDefault(ItemID.AMULET_OF_GHOSTSPEAK, 0).intValue());
    }

    @Test
    public void preQuestEctoBarriersParticipateInBankPlanning() {
        List<Transport> barriers = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> "Pay-toll(2-Ecto)".equals(t.getAction()))
                .filter(t -> "Energy Barrier".equals(t.getName()))
                .collect(Collectors.toList());

        assertEquals(16, barriers.size());
        assertTrue(barriers.stream().allMatch(
                Rs2WalkerBankingPlanner::planningCoversPlainTransport));
        assertTrue(barriers.stream().allMatch(
                Rs2WalkerBankingPlanner::requiresBankPlanning));

		Transport paidBarrier = barriers.stream()
				.filter(t -> t.getDuration() == 2)
				.findFirst().orElseThrow(() -> new AssertionError("paid barrier row missing"));
		Map<Integer, Integer> requirements =
				Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
						List.of(paidBarrier, paidBarrier));
		assertEquals("each paid crossing consumes two ecto-tokens", 4,
				requirements.getOrDefault(ItemID.ECTOTOKEN, 0).intValue());
		assertEquals("the equipped ghostspeak item is reusable", 1,
				requirements.getOrDefault(ItemID.AMULET_OF_GHOSTSPEAK, 0).intValue());
    }

    @Test
    public void everyCatalogTeleportItemParticipatesInBankPlanning() {
        List<Transport> itemTeleports = all.stream()
                .filter(t -> t.getType() == TransportType.TELEPORTATION_ITEM)
                .collect(Collectors.toList());
        assertFalse(itemTeleports.isEmpty());
        assertTrue(itemTeleports.stream().allMatch(
                Rs2WalkerBankingPlanner::requiresBankPlanning));
    }

	@Test
	public void withdrawalSubtractsItemsAlreadyCarried() {
		assertEquals(3, Rs2WalkerBankingPlanner.amountToWithdraw(5, 2));
		assertEquals(0, Rs2WalkerBankingPlanner.amountToWithdraw(5, 5));
		assertEquals(0, Rs2WalkerBankingPlanner.amountToWithdraw(5, 8));
	}

    /** A transport with no item and no currency requirement must stay out of planning. */
    @Test
    public void unrestrictedTransportsAreStillIgnored() {
        List<Transport> unrestricted = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> t.getItemIdRequirements() == null || t.getItemIdRequirements().isEmpty())
                .filter(t -> t.getCurrencyAmount() <= 0)
                .filter(t -> !"Pay-toll(2-Ecto)".equals(t.getAction())
                        || !"Energy Barrier".equals(t.getName()))
                .collect(Collectors.toList());

        assertFalse("precondition: most doors and stairs require nothing", unrestricted.isEmpty());
        for (Transport t : unrestricted) {
            assertFalse("a transport requiring nothing must never trigger a bank trip: " + describe(t),
                    Rs2WalkerBankingPlanner.planningCoversPlainTransport(t));
        }
    }

    /**
     * Pure currency transports (charter fares, magic carpets, the Shantay coin row) have EMPTY
     * itemIdRequirements — the withdrawal-quantity collector's item loop never ran for them, so their
     * coins were never withdrawn at the bank and the post-bank replan dropped the transport ("banked
     * walking does not withdraw gold"). Every real currency transport in the catalog must contribute its
     * fare to the withdrawal map, and fares must SUM across multiple currency hops.
     */
    @Test
    public void pureCurrencyFaresEnterTheWithdrawalMap() {
        List<Transport> pureCurrency = all.stream()
                .filter(t -> t.getCurrencyAmount() > 0)
                .filter(t -> "Coins".equalsIgnoreCase(t.getCurrencyName()))
                .filter(t -> t.getItemIdRequirements() == null || t.getItemIdRequirements().isEmpty())
                // Match the collector's own type gate, or this can pick a catalog row the collector
                // never considers and fail for a reason the test is not about.
                .filter(t -> Rs2WalkerBankingPlanner.isCurrencyBasedTransport(t.getType()))
                .collect(Collectors.toList());
        assertFalse("precondition: the catalog has pure coin-fare transports (charters etc.)",
                pureCurrency.isEmpty());

        Transport one = pureCurrency.get(0);
        java.util.Map<Integer, Integer> map =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(java.util.List.of(one));
        assertTrue("a pure coin fare must appear in the withdrawal map: " + describe(one),
                map.getOrDefault(net.runelite.api.gameval.ItemID.COINS, 0) >= one.getCurrencyAmount());

        java.util.Map<Integer, Integer> summed =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(java.util.List.of(one, one));
        assertTrue("fares must sum across currency hops",
                summed.getOrDefault(net.runelite.api.gameval.ItemID.COINS, 0) >= one.getCurrencyAmount() * 2);
    }

    /**
     * An item-gated row whose item the bank cannot supply, but which is vendor-purchasable at the
     * transport (the Shantay ticket row): the planner must withdraw the FARE, not request an item
     * the withdrawal step can never satisfy. Headless bank counts read as zero, which is exactly
     * the "not banked" case.
     */
    @Test
    public void unbankedPurchasableItemFallsBackToItsFare() {
        Transport ticketRow = all.stream()
                .filter(t -> t.getObjectId() == 4031)
                .filter(t -> t.getItemIdRequirements() != null && !t.getItemIdRequirements().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("catalog should contain the Shantay ticket row"));

        java.util.Map<Integer, Integer> map =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(java.util.List.of(ticketRow));

        assertEquals("the planner must withdraw exactly one 5-coin fare",
                5, map.getOrDefault(net.runelite.api.gameval.ItemID.COINS, 0).intValue());
        assertFalse("the unbankable ticket itself must not be requested",
                map.containsKey(1854));
    }

    /** Currency-bearing transports kept their existing eligibility. */
    @Test
    public void currencyTransportsRemainEligible() {
        List<Transport> currency = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> t.getCurrencyAmount() > 0)
                .collect(Collectors.toList());

        assertFalse("precondition: the data should contain currency-bearing plain transports",
                currency.isEmpty());
        for (Transport t : currency) {
            assertTrue("currency transports must keep qualifying: " + describe(t),
                    Rs2WalkerBankingPlanner.planningCoversPlainTransport(t));
        }
    }

    /** The complete upstream Kharazi family uses either one machete or one axe alternative. */
    @Test
    public void everyMacheteAndAxeGatedJungleObstacleQualifies() {
        List<Transport> jungle = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> "Chop-down".equals(t.getAction()))
                .filter(t -> "Jungle Bush".equals(t.getName())
                        || "Jungle tree".equals(t.getName()))
                .collect(Collectors.toList());

        assertEquals(92, jungle.size());
        assertTrue(jungle.stream().allMatch(t -> t.getItemIdRequirements().size() == 1));
        assertTrue(jungle.stream().allMatch(
                Rs2WalkerBankingPlanner::planningCoversPlainTransport));
        assertTrue(jungle.stream().allMatch(
                Rs2WalkerBankingPlanner::requiresBankPlanning));
    }

    /** Every Brimhaven vine row requests one reusable axe alternative from the bank. */
    @Test
    public void everyBrimhavenVineQualifiesForReusableAxePlanning() {
        List<Transport> vines = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> "Chop-down".equals(t.getAction()))
                .filter(t -> "Vines".equals(t.getName()))
                .filter(t -> t.getObjectId() >= 21731 && t.getObjectId() <= 21735)
                .collect(Collectors.toList());

        assertEquals(10, vines.size());
        assertTrue(vines.stream().allMatch(t -> t.getItemIdRequirements().stream()
                .flatMap(Set::stream).collect(Collectors.toSet()).size() == 13));
        assertTrue(vines.stream().allMatch(
                Rs2WalkerBankingPlanner::planningCoversPlainTransport));
        assertTrue(vines.stream().allMatch(
                Rs2WalkerBankingPlanner::requiresBankPlanning));

        Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(vines.get(0), vines.get(1)));
        assertEquals("one reusable axe covers repeated vine edges", 1,
                requirements.values().stream().mapToInt(Integer::intValue).sum());
    }

    /** Exact web rows use one reusable, guaranteed-success Wilderness sword alternative. */
    @Test
    public void everySlashableWebQualifiesAndRepeatedEdgesNeedOneSword() {
        List<Transport> webs = all.stream()
                .filter(t -> t.getType() == TransportType.TRANSPORT)
                .filter(t -> "Slash".equals(t.getAction()))
                .filter(t -> "Web".equals(t.getName()))
                .filter(t -> t.getObjectId() == 733)
                .collect(Collectors.toList());

        Set<Integer> wildernessSwords = Set.of(
                ItemID.WILDERNESS_SWORD_EASY, ItemID.WILDERNESS_SWORD_MEDIUM,
                ItemID.WILDERNESS_SWORD_HARD, ItemID.WILDERNESS_SWORD_ELITE);
        assertEquals(28, webs.size());
        assertTrue(webs.stream().allMatch(t ->
                t.getItemIdRequirements().equals(Set.of(wildernessSwords))));
        assertTrue(webs.stream().allMatch(
                Rs2WalkerBankingPlanner::planningCoversPlainTransport));
        assertTrue(webs.stream().allMatch(
                Rs2WalkerBankingPlanner::requiresBankPlanning));

        Map<Integer, Integer> requirements =
                Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(
                        List.of(webs.get(0), webs.get(1)));
        assertEquals("one reusable sword covers repeated web edges", 1,
                requirements.entrySet().stream()
                        .filter(entry -> wildernessSwords.contains(entry.getKey()))
                        .mapToInt(Map.Entry::getValue).sum());
    }

    @Test
    public void nullAndNonPlainTransportsAreRejectedSafely() {
        assertFalse("null must not qualify", Rs2WalkerBankingPlanner.planningCoversPlainTransport(null));

        List<Transport> teleports = all.stream()
                .filter(t -> t.getType() == TransportType.TELEPORTATION_ITEM)
                .collect(Collectors.toList());
        for (Transport t : teleports) {
            assertFalse("non-plain types are handled by their own branch and must not match here",
                    Rs2WalkerBankingPlanner.planningCoversPlainTransport(t));
        }
    }
}
