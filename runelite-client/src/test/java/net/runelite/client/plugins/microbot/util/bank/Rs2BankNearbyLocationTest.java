package net.runelite.client.plugins.microbot.util.bank;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2BankNearbyLocationTest {
    @Test
    public void nearbyChestUsesRegisteredBankWithoutSceneScan() {
        assertEquals(BankLocation.MINING_GUILD, Rs2Bank.findNearbyRegisteredBank(
                new WorldPoint(3012, 9718, 0), 20, bank -> true).orElse(null));
    }

    @Test
    public void inaccessibleOrWrongPlaneBankIsSkipped() {
        WorldPoint edgeville = BankLocation.EDGEVILLE.getWorldPoint();
        assertFalse(Rs2Bank.findNearbyRegisteredBank(edgeville, 8,
                bank -> bank != BankLocation.EDGEVILLE).isPresent());
        assertFalse(Rs2Bank.findNearbyRegisteredBank(
                new WorldPoint(edgeville.getX(), edgeville.getY(), 1), 8, bank -> true).isPresent());
    }

    @Test
    public void outsideLocalRadiusFallsBackToPathfinder() {
        assertFalse(Rs2Bank.findNearbyRegisteredBank(
                new WorldPoint(3012, 9718, 0), 0, bank -> true).isPresent());
    }

    @Test
    public void regionalBankSearchExcludesFarAwayAndOtherPlaneBanks() {
        Set<BankLocation> candidates = Rs2Bank.banksWithinLocalSearch(Set.of(
                BankLocation.EDGEVILLE, BankLocation.GRAND_EXCHANGE, BankLocation.DRAYNOR_VILLAGE,
                BankLocation.GNOME_BANK), BankLocation.EDGEVILLE.getWorldPoint());
        assertTrue(candidates.contains(BankLocation.EDGEVILLE));
        assertTrue(candidates.contains(BankLocation.GRAND_EXCHANGE));
        assertFalse(candidates.contains(BankLocation.DRAYNOR_VILLAGE));
        assertFalse(candidates.contains(BankLocation.GNOME_BANK));
    }
}
