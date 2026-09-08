package net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy;

import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransportRequirementPolicyTest {
    private static final WorldPoint A = new WorldPoint(3651, 3486, 0);
    private static final WorldPoint B = new WorldPoint(3653, 3486, 0);

    @Test
    public void raftRequiresBothEquippedToolsNotHighAgilityOrAnUnfinishedGrapple() {
        assertFalse(TransportRequirementPolicy.brokenRaftEquipmentReady(-1, "Rune crossbow"));
        assertFalse(TransportRequirementPolicy.brokenRaftEquipmentReady(9419, null));
        assertFalse(TransportRequirementPolicy.brokenRaftEquipmentReady(9418, "Rune crossbow"));
        assertFalse(TransportRequirementPolicy.brokenRaftEquipmentReady(9419, "Love crossbow"));
        assertFalse(TransportRequirementPolicy.brokenRaftEquipmentReady(9419, "Heavy ballista"));
        assertTrue(TransportRequirementPolicy.brokenRaftEquipmentReady(9419, "Rune crossbow"));
        assertTrue(TransportRequirementPolicy.brokenRaftEquipmentReady(9419, "Crossbow"));
        assertTrue(TransportRequirementPolicy.brokenRaftEquipmentReady(9419, "Dorgeshuun crossbow"));
    }

    @Test
    public void alKharidPaidActionIsNotAvailableAfterQuest() {
        var rows = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
                .filter(TransportRequirementPolicy::isAlKharidPaidGate)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(4, rows.size());
        for (Transport row : rows) {
            assertEquals(10, row.getCurrencyAmount());
            assertTrue(TransportRequirementPolicy.alKharidPaidVariantAvailable(row, null));
            assertTrue(TransportRequirementPolicy.alKharidPaidVariantAvailable(row, QuestState.NOT_STARTED));
            assertFalse(TransportRequirementPolicy.alKharidPaidVariantAvailable(row, QuestState.FINISHED));
        }
    }

    @Test
    public void shantayTicketsAreConsumedAndEliteExemptionIsScopedToEntry() {
        var rows = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
                .filter(t -> "Shantay pass".equals(t.getName()))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(14, rows.size());
        assertEquals(6, rows.stream().filter(Transport::isConsumable).count());
        for (Transport row : rows) {
            boolean entry = !row.getItemIdRequirements().isEmpty() || row.getCurrencyAmount() > 0;
            assertEquals(entry, TransportRequirementPolicy.freeShantayEntry(row, true));
            assertFalse(TransportRequirementPolicy.freeShantayEntry(row, false));
            if (!row.getItemIdRequirements().isEmpty()) assertTrue(row.isConsumable());
        }
    }

    @Test
    public void ectoBarrierFareDependsOnGhostsAhoyCompletion() {
        Transport barrier = new Transport(A, B, "test", TransportType.TRANSPORT,
                false, "Pay-toll(2-Ecto)", "Energy Barrier", 16105);

        assertEquals(2, TransportRequirementPolicy.currencyAmount(
                barrier, QuestState.NOT_STARTED));
        assertEquals(2, TransportRequirementPolicy.currencyAmount(
                barrier, QuestState.IN_PROGRESS));
        assertEquals(2, TransportRequirementPolicy.currencyAmount(barrier, null));
        assertEquals(0, TransportRequirementPolicy.currencyAmount(
                barrier, QuestState.FINISHED));
        assertEquals("ecto-token", TransportRequirementPolicy.currencyName(barrier));
		assertEquals(Set.of(TransportRequirementPolicy.ghostspeakItemIds()),
				TransportRequirementPolicy.itemIdRequirements(barrier, QuestState.NOT_STARTED));
		assertTrue(TransportRequirementPolicy.itemIdRequirements(
				barrier, QuestState.FINISHED).isEmpty());
    }

	@Test
	public void paidBarrierVariantIsRemovedAfterQuestCompletion() {
		Transport barrier = Transport.loadAllFromResources().values().stream()
				.flatMap(Set::stream)
				.filter(candidate -> candidate.getDuration() == 2)
				.filter(candidate -> "Pay-toll(2-Ecto)".equals(candidate.getAction()))
				.findFirst().orElseThrow(() -> new AssertionError("paid barrier row missing"));

		assertTrue(TransportRequirementPolicy.questVariantAvailable(
				barrier, QuestState.NOT_STARTED));
		assertTrue(TransportRequirementPolicy.questVariantAvailable(barrier, null));
		assertFalse(TransportRequirementPolicy.questVariantAvailable(
				barrier, QuestState.FINISHED));
	}

    @Test
    public void unrelatedTransportKeepsEncodedFare() {
        HashMap<WorldPoint, Set<Transport>> catalog = Transport.loadAllFromResources();
        Transport ectoBoat = catalog.values().stream()
                .flatMap(Set::stream)
                .filter(transport -> transport.getCurrencyAmount() == 25)
                .filter(transport -> "Ecto-token".equalsIgnoreCase(
                        transport.getCurrencyName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("catalog ecto-token boat missing"));

        assertEquals(25, TransportRequirementPolicy.currencyAmount(
                ectoBoat, QuestState.FINISHED));
        assertEquals("Ecto-token", TransportRequirementPolicy.currencyName(ectoBoat));
    }
}
