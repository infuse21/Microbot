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
