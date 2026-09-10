package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BrimhavenEntrancePolicyTest
{
	@Test
	public void allDoorStatesKeepSouthernUnlockIndependentOfEntryFee()
	{
		for (int state = 0; state < 16; state++)
		{
			boolean open = state >= 8 || state % 2 == 1;
			assertEquals(open, BrimhavenEntrancePolicy.isOpen(state));
			assertEquals(open ? 0 : 875, BrimhavenEntrancePolicy.fare(state));
			assertEquals(open ? "Enter" : null, next(state, 0, null));
			assertEquals(open ? "Enter" : "Pay", next(state, 875, null));
			assertEquals(open ? "Enter" : null, next(state, 874, null));
		}
		for (int invalid : new int[] {-1, 16, Integer.MAX_VALUE})
		{
			assertEquals(-1, BrimhavenEntrancePolicy.fare(invalid));
			assertNull(next(invalid, Integer.MAX_VALUE, null));
		}
	}

	@Test
	public void bothMenusSelectOnlySingleVisitRegardlessOfOptionOrder()
	{
		assertEquals(0, index("Pay 875 coins to enter?", "Yes", "No"));
		assertEquals(1, index("Pay 875 coins to enter?", "No", "Yes"));
		assertEquals(2, index("Select an option", "Cancel",
			"Pay 1,000,000 coins for permanent access", "Pay 875 coins to enter once"));
		assertEquals(-1, index("Pay 7500 coins to enter?", "Yes", "No"));
		assertEquals(-1, index("Select an Option", "Yes", "No"));
		assertEquals(-1, index("Pay 875 coins to enter?", "Yes", "Yes"));
		assertEquals(-1, index("Select an Option", "Pay 1,000,000 coins for permanent access", "Cancel"));
		assertEquals(-1, index("Select an Option", "Pay 875 coins to enter once",
			"Pay 1,000,000 coins for permanent access", "Cancel", "Unrelated"));
	}

	@Test
	public void paymentRequiresOwnedMenuAndEnoughCoinsAndCannotRepeat()
	{
		for (int coins : new int[] {875, 999999, 1000000, Integer.MAX_VALUE})
		{
			assertEquals(BrimhavenEntrancePolicy.CONFIRM, menu(coins, "Pay"));
		}
		assertNull(menu(874, "Pay"));
		assertNull(menu(875, null));
		assertNull(menu(875, "Enter"));
		assertNull(menu(875, BrimhavenEntrancePolicy.CONFIRM));
		for (String pending : List.of(BrimhavenEntrancePolicy.CONFIRM,
			BrimhavenEntrancePolicy.CONTINUE, "Enter"))
		{
			assertNull(next(0, 1000000, pending));
			assertEquals("Enter", next(1, 0, pending));
		}
	}

	@Test
	public void onlyOwnedReceiptWithConfirmedAccessCanContinue()
	{
		String receipt = "Many thanks. You may now pass the door. May your death be a glorious one!";
		assertEquals(BrimhavenEntrancePolicy.CONTINUE, receipt(1, "Saniboch", receipt,
			BrimhavenEntrancePolicy.CONFIRM));
		assertNull(receipt(0, "Saniboch", receipt, BrimhavenEntrancePolicy.CONFIRM));
		assertNull(receipt(1, "Other NPC", receipt, BrimhavenEntrancePolicy.CONFIRM));
		assertNull(receipt(1, "Saniboch", "Unrelated dialogue", BrimhavenEntrancePolicy.CONFIRM));
		assertNull(receipt(1, "Saniboch", receipt, null));
		assertNull(receipt(1, "Saniboch", receipt, BrimhavenEntrancePolicy.CONTINUE));
	}

	private static String next(int state, int coins, String pending)
	{
		return BrimhavenEntrancePolicy.nextAction(state, coins, false, "", List.of(), "", "", pending);
	}

	private static int index(String title, String... options)
	{
		return BrimhavenEntrancePolicy.confirmationIndex(title, List.of(options));
	}

	private static String menu(int coins, String pending)
	{
		return BrimhavenEntrancePolicy.nextAction(0, coins, true, "Pay 875 coins to enter?",
			List.of("Yes", "No"), "", "", pending);
	}

	private static String receipt(int state, String speaker, String text, String pending)
	{
		return BrimhavenEntrancePolicy.nextAction(state, 0, true, "", List.of(), speaker, text, pending);
	}
}
