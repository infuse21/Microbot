package net.runelite.client.plugins.microbot.util.walker.banking;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy.TransportRequirementPolicy;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.shortestpath.PurchasableItemCatalog;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.magic.RuneFilter;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.TransportRouteAnalysis;
import net.runelite.client.plugins.microbot.util.walker.WebWalkLog;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class Rs2WalkerBankingPlanner {

    private Rs2WalkerBankingPlanner() {
    }

    public static List<Transport> getTransportsForDestination(WorldPoint destination, boolean useBankItems, TransportType prefTransportType) {
        if (destination == null) {
            return new ArrayList<>();
        }

        boolean originalUseBankItems = Rs2PathApi.getPathfinderConfig().isUseBankItems();
        try {
            Rs2PathApi.getPathfinderConfig().setUseBankItems(useBankItems);
            Rs2PathApi.getPathfinderConfig().refresh();
            Pathfinder pf = new Pathfinder(Rs2PathApi.getPathfinderConfig(), Rs2Player.getWorldLocation(), destination);
            pf.run();

            List<WorldPoint> path = pf.getPath();
            if (path.isEmpty()) {
                log.debug("Unable to find path to destination: " + destination);
                return new ArrayList<>();
            }

            List<Transport> transports = Rs2Walker.getTransportsForPath(path, 0, prefTransportType, true);
            transports.forEach(t -> log.debug("Transport found: " + t));
            return transports;
        } finally {
            Rs2PathApi.getPathfinderConfig().setUseBankItems(originalUseBankItems);
            Rs2PathApi.getPathfinderConfig().refresh();
        }
    }

    /**
     * Whether a plain {@link TransportType#TRANSPORT} takes part in bank planning.
     *
     * <p>Previously only currency-bearing ones did, so an item-gated obstacle — a machete for a
     * jungle bush, a pickaxe for a rockfall, a Shantay pass — fell through
     * {@link #hasRequiredTransportItems} to its catch-all {@code return true}, was never reported as
     * missing, and was never withdrawn. That contradicted the pathfinder:
     * {@code PathfinderConfig.hasRequiredItems} counts <em>bank</em> contents when
     * {@code useBankItems} is set, so a route was planned through the obstacle on the strength of a
     * banked item the planner then declined to fetch, stranding the walk at the obstacle.
     *
     * <p>This only widens which transports are <em>eligible</em>. Collection is path-scoped —
     * {@link #getTransportsForDestination} pathfinds first and inspects only transports on the
     * resulting route — so an item is fetched solely when the chosen route actually needs it.
     */
    static boolean planningCoversPlainTransport(Transport transport) {
        if (transport == null || transport.getType() != TransportType.TRANSPORT) {
            return false;
        }
        return TransportRequirementPolicy.currencyAmount(transport) > 0
                || !TransportRequirementPolicy.itemIdRequirements(transport).isEmpty();
    }

	/** Shared inclusion boundary for route analysis and missing-item planning. */
	public static boolean requiresBankPlanning(Transport transport) {
		if (transport == null) {
			return false;
		}
		switch (transport.getType()) {
			case TELEPORTATION_ITEM:
			case TELEPORTATION_SPELL:
			case SEASONAL_TRANSPORT:
			case FAIRY_RING:
			case CANOE:
			case BOAT:
			case CHARTER_SHIP:
			case SHIP:
			case MINECART:
			case MAGIC_CARPET:
			case HOT_AIR_BALLOON:
			case SPIRIT_TREE:
				return true;
			default:
				return planningCoversPlainTransport(transport);
		}
	}

    public static boolean hasRequiredTransportItems(Transport transport) {
        if (transport == null) {
            return false;
        }

        if (transport.getType() == TransportType.FAIRY_RING) {
            return Rs2Inventory.hasItem(ItemID.DRAMEN_STAFF)
                    || Rs2Equipment.isWearing(ItemID.DRAMEN_STAFF)
                    || Rs2Inventory.hasItem(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF)
                    || Rs2Equipment.isWearing(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF)
                    || Microbot.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) == 1;
        } else if (transport.getType() == TransportType.TELEPORTATION_ITEM
                || transport.getType() == TransportType.TELEPORTATION_SPELL
				|| transport.getType() == TransportType.SEASONAL_TRANSPORT
                || transport.getType() == TransportType.CANOE
                || transport.getType() == TransportType.BOAT
                || transport.getType() == TransportType.CHARTER_SHIP
                || transport.getType() == TransportType.SHIP
                || transport.getType() == TransportType.MINECART
                || transport.getType() == TransportType.MAGIC_CARPET
				|| transport.getType() == TransportType.HOT_AIR_BALLOON
                || planningCoversPlainTransport(transport)) {
            if (transport.getType() == TransportType.TELEPORTATION_SPELL && transport.getDisplayInfo() != null) {
                String spellName = transport.getDisplayInfo().contains(":")
                        ? transport.getDisplayInfo().split(":")[0].trim()
                        : transport.getDisplayInfo().trim();
                boolean hasMultipleDestination = transport.getDisplayInfo().contains(":");
                String displayInfo = hasMultipleDestination
                        ? transport.getDisplayInfo().split(":")[0].trim().toLowerCase()
                        : transport.getDisplayInfo();
                log.debug("Looking for spell rune requirements for: '{}' - display info {}", spellName, displayInfo);
                Rs2Spells rs2Spell = Rs2Magic.getRs2Spell(displayInfo);
                return Rs2Magic.hasRequiredRunes(rs2Spell);
            }
            int currencyAmount = TransportRequirementPolicy.currencyAmount(transport);
            String currencyName = TransportRequirementPolicy.currencyName(transport);
            if (isCurrencyBasedTransport(transport.getType())
                    && currencyName != null
                    && !currencyName.isEmpty()
                    && currencyAmount > 0) {
                int currencyItemId = getCurrencyItemId(currencyName);
                if (currencyItemId <= 0 || Rs2Inventory.count(currencyItemId) < currencyAmount) {
                    return false;
                }
            }
            Set<Set<Integer>> itemRequirements =
                    TransportRequirementPolicy.itemIdRequirements(transport);
            if (itemRequirements.isEmpty()) {
                return true;
            }

            return itemRequirements
                    .stream()
                    .flatMap(Collection::stream)
                    .anyMatch(itemId -> Rs2Equipment.isWearing(itemId) || Rs2Inventory.hasItem(itemId));
        }

        return true;
    }

    public static List<Transport> getMissingTransports(List<Transport> transports) {
        if (transports == null) {
            return new ArrayList<>();
        }

        return transports.stream()
                .filter(t -> !hasRequiredTransportItems(t))
                .collect(Collectors.toList());
    }

    public static Map<Integer, Integer> getMissingTransportItemIdsWithQuantities(List<Transport> transports) {
        if (transports == null) {
            return new HashMap<>();
        }

        Map<Integer, Integer> exactWithdrawals = new HashMap<>();
        Map<Integer, Integer> fungibleRequirements = new HashMap<>();
        Map<Set<Integer>, Integer> consumableAlternatives = new HashMap<>();
        Set<Set<Integer>> reusableAlternatives = new HashSet<>();
        Map<Runes, Integer> spellRequirements = new EnumMap<>(Runes.class);

        transports.stream().filter(Rs2WalkerBankingPlanner::requiresBankPlanning).forEach(transport -> {
            if (transport.getType() == TransportType.TELEPORTATION_SPELL) {
                getSpellRequirements(transport).forEach(
                        (rune, quantity) -> spellRequirements.merge(rune, quantity, Integer::sum));
                return;
            }

            if (transport.getType() == TransportType.FAIRY_RING
                    && Microbot.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) != 1) {
                reusableAlternatives.add(Set.of(
                        ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF));
                return;
            }

            int currencyAmount = TransportRequirementPolicy.currencyAmount(transport);
            String currencyName = TransportRequirementPolicy.currencyName(transport);
            if (isCurrencyBasedTransport(transport.getType()) && currencyAmount > 0) {
                int currencyItemId = getCurrencyItemId(currencyName);
                if (currencyItemId > 0) {
                    fungibleRequirements.merge(
                            currencyItemId, currencyAmount, Integer::sum);
                }
            }

            Set<Set<Integer>> itemRequirements =
                    TransportRequirementPolicy.itemIdRequirements(transport);
            if (!itemRequirements.isEmpty()) {
                Set<Integer> alternatives = itemRequirements.stream()
                        .filter(java.util.Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                if (!alternatives.isEmpty()) {
                    // PathfinderConfig and hasRequiredTransportItems both treat every encoded
                    // item ID as an OR alternative. Preserve that exact boundary here: charged
                    // jewellery and trimmed/untrimmed capes are variants of one requirement,
                    // not several independent items to withdraw.
					if (transport.isConsumable() && !isReusableItemContainer(transport)) {
                        consumableAlternatives.merge(alternatives, 1, Integer::sum);
                    } else {
                        reusableAlternatives.add(alternatives);
                    }
                }
            }
        });

        addSpellWithdrawals(spellRequirements, exactWithdrawals);
        consumableAlternatives.forEach((alternatives, uses) ->
                addAlternativeWithdrawal(alternatives, uses, exactWithdrawals, fungibleRequirements));
        reusableAlternatives.forEach(alternatives ->
                addAlternativeWithdrawal(alternatives, 1, exactWithdrawals, fungibleRequirements));

        fungibleRequirements.forEach((itemId, required) -> {
            int shortfall = amountToWithdraw(required, Rs2Inventory.itemQuantity(itemId));
            if (shortfall > 0) {
                exactWithdrawals.merge(itemId, shortfall, Integer::sum);
            }
        });

        Map<Integer, Integer> itemQuantityMap = new HashMap<>();
        exactWithdrawals.forEach((itemId, withdrawal) -> {
            if (withdrawal > 0) {
                // The coordinator accepts total required quantities and subtracts the inventory
                // count immediately before withdrawal. Encode the exact planned shortfall without
                // losing items already carried under this same ID.
                itemQuantityMap.put(itemId, Rs2Inventory.itemQuantity(itemId) + withdrawal);
            }
        });
		return itemQuantityMap;
	}

	private static boolean isReusableItemContainer(Transport transport) {
		return transport.getType() == TransportType.TELEPORTATION_ITEM
				&& transport.getDisplayInfo() != null
				&& transport.getDisplayInfo().startsWith("Master Scroll Book:")
				&& transport.getItemIdRequirements().equals(
						Set.of(Set.of(ItemID.BOOKOFSCROLLS_CHARGED)));
	}

    private static void addAlternativeWithdrawal(Set<Integer> alternatives, int requiredUses,
            Map<Integer, Integer> exactWithdrawals, Map<Integer, Integer> fungibleRequirements) {
        int carried = alternatives.stream().mapToInt(itemId ->
                Rs2Inventory.itemQuantity(itemId) + (Rs2Equipment.isWearing(itemId) ? 1 : 0)).sum();
        int shortfall = amountToWithdraw(requiredUses, carried);
        if (shortfall == 0) {
            return;
        }

        Map<Integer, Integer> bankQuantities = new HashMap<>();
        alternatives.forEach(itemId -> bankQuantities.put(itemId, safeBankCount(itemId)));
        List<Integer> rankedAlternatives = alternatives.stream()
                .sorted(Comparator
                        .comparingInt((Integer itemId) -> bankQuantities.get(itemId))
                        .reversed()
                        .thenComparingInt(Integer::intValue))
                .collect(Collectors.toList());

        int remaining = shortfall;
        for (Integer itemId : rankedAlternatives) {
            int amount = Math.min(remaining, bankQuantities.get(itemId));
            if (amount > 0) {
                exactWithdrawals.merge(itemId, amount, Integer::sum);
                remaining -= amount;
            }
            if (remaining == 0) {
                return;
            }
        }

        if (remaining > 0) {
            PurchasableItemCatalog.PurchasableItem purchasable = alternatives.stream()
                    .map(PurchasableItemCatalog::byItemId)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            int currencyItemId = purchasable == null
                    ? -1 : getCurrencyItemId(purchasable.costCurrencyName);
            if (currencyItemId > 0) {
                fungibleRequirements.merge(
                        currencyItemId, purchasable.costAmount * remaining, Integer::sum);
                return;
            }
        }
        if (remaining > 0 && !rankedAlternatives.isEmpty()) {
            // Keep the unsatisfied remainder explicit. The coordinator preflights each requested
            // quantity and exits before the final target leg when the bank cannot supply it.
            exactWithdrawals.merge(rankedAlternatives.get(0), remaining, Integer::sum);
        }
    }

    private static void addSpellWithdrawals(Map<Runes, Integer> required,
            Map<Integer, Integer> exactWithdrawals) {
        if (required.isEmpty()) {
            return;
        }
        Map<Runes, Integer> available;
        try {
            available = Rs2Magic.getRunes(RuneFilter.builder().includeBank(false).build());
        } catch (RuntimeException ex) {
            available = Map.of();
        }
        final Map<Runes, Integer> availableRunes = available;

        Map<Runes, Integer> bankRemaining = new EnumMap<>(Runes.class);
        for (Runes rune : Runes.values()) {
            bankRemaining.put(rune, safeBankCount(rune.getItemId()));
        }
        planRuneWithdrawals(required, availableRunes, bankRemaining).forEach(
                (itemId, quantity) -> exactWithdrawals.merge(itemId, quantity, Integer::sum));
    }

    static Map<Integer, Integer> planRuneWithdrawals(Map<Runes, Integer> required,
            Map<Runes, Integer> available, Map<Runes, Integer> bankQuantities) {
        Map<Integer, Integer> withdrawals = new HashMap<>();
        Map<Runes, Integer> deficits = new EnumMap<>(Runes.class);
        required.forEach((rune, quantity) -> {
            int missing = amountToWithdraw(quantity, available.getOrDefault(rune, 0));
            if (missing > 0) {
                deficits.put(rune, missing);
            }
        });
        Map<Runes, Integer> bankRemaining = new EnumMap<>(Runes.class);
        bankRemaining.putAll(bankQuantities);

        while (!deficits.isEmpty()) {
            Runes best = null;
            int bestCoverage = 0;
            int bestQuantity = 0;
            for (Runes candidate : Runes.values()) {
                int quantity = bankRemaining.getOrDefault(candidate, 0);
                if (quantity <= 0) {
                    continue;
                }
                int coverage = (int) deficits.keySet().stream()
                        .filter(candidate::providesRune)
                        .count();
                if (coverage > bestCoverage
                        || (coverage == bestCoverage && quantity > bestQuantity)) {
                    best = candidate;
                    bestCoverage = coverage;
                    bestQuantity = quantity;
                }
            }
            if (best == null || bestCoverage == 0) {
                deficits.forEach((rune, quantity) ->
                        withdrawals.merge(rune.getItemId(), quantity, Integer::sum));
                break;
            }

            final Runes selected = best;
            int amount = deficits.entrySet().stream()
                    .filter(entry -> selected.providesRune(entry.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .max()
                    .orElse(0);
            amount = Math.min(amount, bankRemaining.get(selected));
            withdrawals.merge(selected.getItemId(), amount, Integer::sum);
            bankRemaining.put(selected, bankRemaining.get(selected) - amount);
            int supplied = amount;
            deficits.replaceAll((rune, quantity) ->
                    selected.providesRune(rune) ? Math.max(0, quantity - supplied) : quantity);
            deficits.entrySet().removeIf(entry -> entry.getValue() == 0);
        }
        return withdrawals;
    }

    private static int safeBankCount(int itemId) {
        try {
            return Rs2Bank.count(itemId);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

	public static int amountToWithdraw(int requiredQuantity, int inventoryQuantity) {
		return Math.max(0, requiredQuantity - inventoryQuantity);
	}

    public static List<Integer> getMissingTransportItemIds(List<Transport> transports) {
        return new ArrayList<>(getMissingTransportItemIdsWithQuantities(transports).keySet());
    }

    public static TransportRouteAnalysis compareRoutes(WorldPoint startPoint, WorldPoint target) {
        long totalStartTime = System.nanoTime();
        StringBuilder performanceLog = new StringBuilder();
        performanceLog.append("\n\t=== compareRoutes Performance Analysis ===\n");
        if (target == null) {
            return new TransportRouteAnalysis(new ArrayList<>(), null, null, new ArrayList<>(), new ArrayList<>(), "Target location is null");
        }

        if (startPoint == null) {
            startPoint = Rs2Player.getWorldLocation();
        }

        if (startPoint == null) {
            return new TransportRouteAnalysis(new ArrayList<>(), null, null, new ArrayList<>(), new ArrayList<>(), "Cannot determine starting location");
        }

        try {
            performanceLog.append("\tStart Point: ").append(startPoint).append(", Target: ").append(target).append("\n");
            long directPathStartTime = System.nanoTime();
            List<WorldPoint> directPath = Rs2Walker.getWalkPath(startPoint, target);
            long directPathEndTime = System.nanoTime();
            double directPathTimeMs = (directPathEndTime - directPathStartTime) / 1_000_000.0;

            int directDistance = Rs2Walker.getTotalTravelTicksFromPath(directPath, target);
            performanceLog.append("\t-Direct path calculation: ").append(String.format("%.2f ms", directPathTimeMs))
                    .append(" (").append(directPath.size()).append(" waypoints, ").append(directDistance).append(" ticks)\n");

            BankLocation nearestBank = null;
            List<WorldPoint> pathToBank = new ArrayList<>();
            List<WorldPoint> pathFromBankToTarget = new ArrayList<>();
            int bankingRouteDistance = -1;

            try {
                boolean originalUseBankItems = Rs2PathApi.getPathfinderConfig().isUseBankItems();
                try {
                    Rs2PathApi.getPathfinderConfig().setUseBankItems(true);
                    Rs2PathApi.getPathfinderConfig().refresh(target);

                    performanceLog.append("\t-Bank items available: ").append(Rs2Bank.bankItems().size()).append("\n");

                    long bankSearchStartTime = System.nanoTime();
                    nearestBank = Rs2Bank.getNearestBank(startPoint);
                    long bankSearchEndTime = System.nanoTime();
                    double bankSearchTimeMs = (bankSearchEndTime - bankSearchStartTime) / 1_000_000.0;

                    if (nearestBank != null) {
                        WorldPoint bankLocation = nearestBank.getWorldPoint();
                        performanceLog.append("\t-Nearest bank search: ").append(String.format("%.2f ms", bankSearchTimeMs));
                        performanceLog.append("\t -> Found: ").append(nearestBank).append(" at ").append(bankLocation).append("\n");

                        long pathToBankStartTime = System.nanoTime();
                        pathToBank = Rs2Walker.getWalkPath(startPoint, bankLocation);
                        long pathToBankEndTime = System.nanoTime();
                        double pathToBankTimeMs = (pathToBankEndTime - pathToBankStartTime) / 1_000_000.0;
                        int distanceToBank = Rs2Walker.getTotalTravelTicksFromPath(pathToBank, bankLocation);

                        long pathFromBankStartTime = System.nanoTime();
                        pathFromBankToTarget = Rs2Walker.getWalkPath(bankLocation, target);
                        long pathFromBankEndTime = System.nanoTime();
                        double pathFromBankTimeMs = (pathFromBankEndTime - pathFromBankStartTime) / 1_000_000.0;
                        List<Transport> bankLegTransports = Rs2Walker.getTransportsForPath(
                                pathFromBankToTarget, 0, TransportType.TELEPORTATION_SPELL, true);
                        long spellCount = bankLegTransports.stream()
                                .filter(t -> t.getType() == TransportType.TELEPORTATION_SPELL)
                                .count();
                        long itemCount = bankLegTransports.stream()
                                .filter(t -> t.getType() == TransportType.TELEPORTATION_ITEM)
                                .count();
                        int distanceFromBank = Rs2Walker.getTotalTravelTicksFromPath(
                                pathFromBankToTarget, target);

                        performanceLog.append("\t-Path to bank calculation: ").append(String.format("%.2f ms", pathToBankTimeMs))
                                .append(" (").append(pathToBank.size()).append(" waypoints, ").append(distanceToBank).append(" ticks)\n");
                        performanceLog.append("\t-Path from bank to target with banked items: ").append(String.format("%.2f ms", pathFromBankTimeMs))
                                .append(" (").append(pathFromBankToTarget.size()).append(" waypoints, ").append(distanceFromBank).append(" ticks)\n");
                        performanceLog.append("\t-Bank leg transports: total=").append(bankLegTransports.size())
                                .append(" spells=").append(spellCount)
                                .append(" items=").append(itemCount)
                                .append("\n");
                        Transport firstSpellTransport = bankLegTransports.stream()
                                .filter(t -> t.getType() == TransportType.TELEPORTATION_SPELL)
                                .findFirst()
                                .orElse(null);
                        if (firstSpellTransport != null) {
                            performanceLog.append("\t-First bank-leg spell transport: ")
                                    .append(firstSpellTransport.getDisplayInfo())
                                    .append(" -> ")
                                    .append(firstSpellTransport.getDestination())
                                    .append("\n");
                        }
                        WebWalkLog.spInfo("compare_bank_leg | total={} spells={} items={} firstSpell={}",
                                bankLegTransports.size(),
                                spellCount,
                                itemCount,
                                firstSpellTransport == null
                                        ? "none"
                                        : firstSpellTransport.getDisplayInfo() + " -> " + firstSpellTransport.getDestination());
                        if (distanceToBank != -1
                                && distanceFromBank != -1
                                && distanceToBank != Integer.MAX_VALUE
                                && distanceFromBank != Integer.MAX_VALUE) {
                            bankingRouteDistance = distanceToBank + distanceFromBank;
                        }
                        performanceLog.append("\t-Total banking route cost: ").append(bankingRouteDistance).append(" ticks\n");
                    } else {
                        performanceLog.append("\t-Nearest bank search: ").append(String.format("%.2f ms", bankSearchTimeMs))
                                .append("\t -> No accessible bank found\n");
                    }
                } finally {
                    Rs2PathApi.getPathfinderConfig().setUseBankItems(originalUseBankItems);
                    Rs2PathApi.getPathfinderConfig().refresh();
                }
            } catch (Exception e) {
                performanceLog.append("Banking route calculation failed: ").append(e.getMessage()).append("\n");
                log.debug("Could not calculate banking route: " + e.getMessage());
            }

            long totalEndTime = System.nanoTime();
            double totalTimeMs = (totalEndTime - totalStartTime) / 1_000_000.0;
            performanceLog.append("\t=== Total compareRoutes time: ").append(String.format("%.2f ms", totalTimeMs)).append(" ===\n");

            if (bankingRouteDistance == -1) {
                performanceLog.append("\tResult: Direct route only (banking route unavailable)\n");
                WebWalkLog.compareDetail(performanceLog.toString());
                WebWalkLog.compareSummary(totalTimeMs, directDistance, -1, "direct_only_bank_unavailable");
                return new TransportRouteAnalysis(directPath, null, null, new ArrayList<>(), new ArrayList<>(),
                        "Direct route only (banking route unavailable)");
            }

            final boolean tie = directDistance == bankingRouteDistance;
            final boolean directStrictlyFaster = directDistance < bankingRouteDistance;
            final boolean preferTransportToTarget = Rs2PathApi.override("preferTransportToTarget", false);
            final String recommendation;
            final String verdictOneLine;
            if (tie) {
                if (preferTransportToTarget) {
                    recommendation = String.format("\tSame travel cost (%d ticks); prefer banking route (prefer transport to target enabled)", directDistance);
                    verdictOneLine = String.format("tie %dt (prefer bank: transport-to-target)", directDistance);
                } else {
                    recommendation = String.format("\tSame travel cost (%d ticks); prefer direct (no bank hop)", directDistance);
                    verdictOneLine = String.format("tie %dt (prefer direct)", directDistance);
                }
            } else if (directStrictlyFaster) {
                recommendation = String.format("\tDirect route is faster (%d vs %d ticks)", directDistance, bankingRouteDistance);
                verdictOneLine = String.format("direct faster %dt vs %dt", directDistance, bankingRouteDistance);
            } else {
                recommendation = String.format("\tBanking route is faster (%d vs %d ticks)", bankingRouteDistance, directDistance);
                verdictOneLine = String.format("bank faster %dt vs %dt", bankingRouteDistance, directDistance);
            }

            performanceLog.append("\tResult:\n\t\t ").append(recommendation).append("\n");
            WebWalkLog.compareDetail(performanceLog.toString());
            WebWalkLog.compareSummary(totalTimeMs, directDistance, bankingRouteDistance, verdictOneLine);

            return new TransportRouteAnalysis(directPath,
                    nearestBank, nearestBank != null ? nearestBank.getWorldPoint() : null, pathToBank, pathFromBankToTarget, recommendation,
                    directDistance, bankingRouteDistance);
        } catch (Exception e) {
            long totalEndTime = System.nanoTime();
            double totalTimeMs = (totalEndTime - totalStartTime) / 1_000_000.0;
            performanceLog.append("ERROR after ").append(String.format("%.2f ms", totalTimeMs)).append(": ").append(e.getMessage()).append("\n");
            WebWalkLog.compareDetail(performanceLog.toString());
            WebWalkLog.compareError(totalTimeMs, target, e.getMessage());
            return new TransportRouteAnalysis(new ArrayList<>(), null, null, new ArrayList<>(), new ArrayList<>(), "Error calculating routes: " + e.getMessage());
        }
    }

    private static Map<Runes, Integer> getSpellRequirements(Transport transport) {
        Map<Runes, Integer> runeRequirements = new EnumMap<>(Runes.class);
        if (transport.getType() != TransportType.TELEPORTATION_SPELL || transport.getDisplayInfo() == null) {
            return runeRequirements;
        }
        try {
            String spellName = transport.getDisplayInfo().contains(":")
                    ? transport.getDisplayInfo().split(":")[0].trim()
                    : transport.getDisplayInfo().trim();
            boolean hasMultipleDestination = transport.getDisplayInfo().contains(":");
            String displayInfo = hasMultipleDestination
                    ? transport.getDisplayInfo().split(":")[0].trim().toLowerCase()
                    : transport.getDisplayInfo();
            log.debug("Looking for spell rune requirements for: '{}' - display info {}", spellName, displayInfo);
            Rs2Spells rs2Spell = Rs2Magic.getRs2Spell(displayInfo);
            if (rs2Spell == null) {
                return runeRequirements;
            }
            Map<Runes, Integer> requiredRunes = Rs2Magic.getRequiredRunes(rs2Spell, 1);
            List<Runes> elementalRunes = rs2Spell.getElementalRunes();
            log.debug("Spell '{}' requires {} runes, including {} elemental runes",
                    spellName, requiredRunes.size(), elementalRunes.size());
            requiredRunes.forEach((rune, quantity) -> {
                runeRequirements.put(rune, quantity);
                log.debug("Spell '{}' requires {} x {} (ID: {})",
                        spellName, quantity, rune.name(), rune.getItemId());
            });
        } catch (Exception e) {
            log.warn("Error getting spell rune requirements for transport '{}': {}",
                    transport.getDisplayInfo(), e.getMessage());
        }

        return runeRequirements;
    }

    /** Package-private so the planning tests can select the same rows this collector accepts. */
    static boolean isCurrencyBasedTransport(TransportType transportType) {
        return transportType == TransportType.BOAT
                || transportType == TransportType.CHARTER_SHIP
                || transportType == TransportType.SHIP
                || transportType == TransportType.MINECART
                || transportType == TransportType.MAGIC_CARPET
                || transportType == TransportType.TRANSPORT;
    }

    private static int getCurrencyItemId(String currencyName) {
        if (currencyName == null || currencyName.trim().isEmpty()) {
            return -1;
        }

        String currency = currencyName.trim().toLowerCase();
        switch (currency) {
            case "coins":
                return ItemID.COINS;
            case "ecto-token":
                return ItemID.ECTOTOKEN;
            default:
                log.warn("Unknown currency type: {}", currencyName);
                return -1;
        }
    }

}
