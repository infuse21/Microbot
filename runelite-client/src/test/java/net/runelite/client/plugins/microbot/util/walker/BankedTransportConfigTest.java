package net.runelite.client.plugins.microbot.util.walker;

import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class BankedTransportConfigTest
{
	@Test
	public void karuulmBootsAndFairyRingStaffOnlyWithdrawWhenBankedWalkingIsEnabled()
	{
		ShortestPathConfig previous = Rs2Walker.config;
		ShortestPathConfig config = mock(ShortestPathConfig.class);
		try (MockedStatic<Rs2Bank> bank = mockStatic(Rs2Bank.class))
		{
			Rs2Walker.config = config;
			for (int itemId : new int[]{23037, 22951, 21643, 772, 9084})
			{
				bank.clearInvocations();
				when(config.walkWithBankedTransports()).thenReturn(false);
				assertFalse(Rs2Walker.withdrawBankedTransportRequirement(itemId, 1));
				bank.verifyNoInteractions();
				when(config.walkWithBankedTransports()).thenReturn(true);
				bank.when(() -> Rs2Bank.withdrawX(itemId, 1)).thenReturn(true);
				assertTrue(Rs2Walker.withdrawBankedTransportRequirement(itemId, 1));
				when(config.walkWithBankedTransports()).thenReturn(false);
				assertFalse(Rs2Walker.withdrawBankedTransportRequirement(itemId, 1));
				bank.verify(() -> Rs2Bank.withdrawX(itemId, 1), times(1));
			}
		}
		finally { Rs2Walker.config = previous; }
	}

	@Test
	public void bankedElementalStaffsAreOptInEvenWhenBankingIsEnabled()
	{
		assertFalse(new ShortestPathConfig() { }.useBankedElementalStaffs());
		ShortestPathConfig previous = Rs2Walker.config;
		ShortestPathConfig config = mock(ShortestPathConfig.class);
		int staffId = net.runelite.client.plugins.microbot.util.magic.Rs2Staff.STAFF_OF_AIR.getItemID();
		try (MockedStatic<Rs2Bank> bank = mockStatic(Rs2Bank.class))
		{
			Rs2Walker.config = config;
			when(config.walkWithBankedTransports()).thenReturn(true);
			assertFalse(Rs2Walker.withdrawBankedTransportRequirement(staffId, 1));
			bank.verifyNoInteractions();
			when(config.useBankedElementalStaffs()).thenReturn(true);
			bank.when(() -> Rs2Bank.withdrawX(staffId, 1)).thenReturn(true);
			assertTrue(Rs2Walker.withdrawBankedTransportRequirement(staffId, 1));
			when(config.useBankedElementalStaffs()).thenReturn(false);
			assertFalse(Rs2Walker.withdrawBankedTransportRequirement(staffId, 1));
			bank.verify(() -> Rs2Bank.withdrawX(staffId, 1), times(1));
		}
		finally { Rs2Walker.config = previous; }
	}

	@Test
	public void eachWithdrawalRequiresTheCurrentBankedTransportSetting()
	{
		ShortestPathConfig previous = Rs2Walker.config;
		ShortestPathConfig config = mock(ShortestPathConfig.class);
		try (MockedStatic<Rs2Bank> bank = mockStatic(Rs2Bank.class))
		{
			Rs2Walker.config = null;
			assertFalse(Rs2Walker.withdrawBankedTransportRequirement(995, 100));
			Rs2Walker.config = config;
			when(config.navigationEngineOrdinaryWalking()).thenReturn(true);
			assertFalse(Rs2Walker.withdrawBankedTransportRequirement(995, 100));
			bank.verifyNoInteractions();
			when(config.walkWithBankedTransports()).thenReturn(true);
			bank.when(() -> Rs2Bank.withdrawX(995, 100)).thenReturn(true);
			assertTrue(Rs2Walker.withdrawBankedTransportRequirement(995, 100));
			when(config.walkWithBankedTransports()).thenReturn(false);
			assertFalse(Rs2Walker.withdrawBankedTransportRequirement(995, 100));
			bank.verify(() -> Rs2Bank.withdrawX(995, 100), times(1));
		}
		finally { Rs2Walker.config = previous; }
	}
}
