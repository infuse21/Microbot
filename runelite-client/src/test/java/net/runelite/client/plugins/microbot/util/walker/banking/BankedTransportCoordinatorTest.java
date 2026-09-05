package net.runelite.client.plugins.microbot.util.walker.banking;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BankedTransportCoordinatorTest
{
	private static final WorldPoint BANK = new WorldPoint(3208, 3219, 2);
	private static final WorldPoint TARGET = new WorldPoint(3300, 3300, 0);

	@Test
	public void successfulTransactionWalksBothEngineLegsAndWithdrawsOnlyShortfall()
	{
		FakeOperations operations = new FakeOperations();
		operations.inventory.put(995, 2);
		operations.bank.put(995, 20);

		BankedTransportCoordinator.Result result = BankedTransportCoordinator.execute(
			BANK, Map.of(995, 5), TARGET, 10, operations);

		assertEquals(BankedTransportCoordinator.Phase.COMPLETE, result.getPhase());
		assertEquals(WalkerState.ARRIVED, result.getWalkerState());
		assertNull(result.getFailedItemId());
		assertEquals(List.of(BANK, TARGET), operations.walkedTargets);
		assertEquals(Integer.valueOf(3), operations.withdrawn.get(995));
		assertEquals(Integer.valueOf(5), operations.inventory.get(995));
		assertTrue(operations.preparedFinalRoute);
	}

	@Test
	public void unsatisfiedWithdrawalStopsBeforeFinalRoute()
	{
		FakeOperations operations = new FakeOperations();
		operations.bank.put(772, 0);

		BankedTransportCoordinator.Result result = BankedTransportCoordinator.execute(
			BANK, Map.of(772, 1), TARGET, 10, operations);

		assertEquals(BankedTransportCoordinator.Phase.WITHDRAW_ITEMS, result.getPhase());
		assertEquals(WalkerState.EXIT, result.getWalkerState());
		assertEquals(Integer.valueOf(772), result.getFailedItemId());
		assertEquals(List.of(BANK), operations.walkedTargets);
		assertFalse(operations.preparedFinalRoute);
		assertTrue(operations.closedBank);
	}

	@Test
	public void incompleteBankLegPropagatesWithoutOpeningBank()
	{
		FakeOperations operations = new FakeOperations();
		operations.nextWalkState = WalkerState.MOVING;

		BankedTransportCoordinator.Result result = BankedTransportCoordinator.execute(
			BANK, Map.of(), TARGET, 10, operations);

		assertEquals(BankedTransportCoordinator.Phase.WALK_TO_BANK, result.getPhase());
		assertEquals(WalkerState.MOVING, result.getWalkerState());
		assertFalse(operations.openedBank);
	}

	@Test
	public void stackedCurrencyWithdrawalConfirmsTheStackQuantity()
	{
		FakeOperations operations = new FakeOperations();
		operations.bank.put(4278, 45);

		BankedTransportCoordinator.Result result = BankedTransportCoordinator.execute(
			BANK, Map.of(4278, 27), TARGET, 10, operations);

		assertEquals(BankedTransportCoordinator.Phase.COMPLETE, result.getPhase());
		assertEquals(Integer.valueOf(27), operations.withdrawn.get(4278));
		assertEquals(Integer.valueOf(27), operations.inventory.get(4278));
	}

	private static final class FakeOperations implements BankedTransportCoordinator.Operations
	{
		private final Map<Integer, Integer> inventory = new HashMap<>();
		private final Map<Integer, Integer> bank = new HashMap<>();
		private final Map<Integer, Integer> withdrawn = new HashMap<>();
		private final List<WorldPoint> walkedTargets = new ArrayList<>();
		private WalkerState nextWalkState = WalkerState.ARRIVED;
		private boolean openedBank;
		private boolean closedBank;
		private boolean preparedFinalRoute;

		@Override public WalkerState walk(WorldPoint target, int reachedDistance) {
			walkedTargets.add(target);
			return nextWalkState;
		}
		@Override public void beforeOpenBank() { }
		@Override public boolean openBank() { openedBank = true; return true; }
		@Override public boolean awaitBankOpen() { return true; }
		@Override public int inventoryCount(int itemId) { return inventory.getOrDefault(itemId, 0); }
		@Override public boolean hasBankItem(int itemId, int amount) {
			return bank.getOrDefault(itemId, 0) >= amount;
		}
		@Override public boolean withdraw(int itemId, int amount) {
			if (!hasBankItem(itemId, amount)) return false;
			bank.merge(itemId, -amount, Integer::sum);
			inventory.merge(itemId, amount, Integer::sum);
			withdrawn.merge(itemId, amount, Integer::sum);
			return true;
		}
		@Override public boolean awaitInventoryQuantity(int itemId, int amount) {
			return inventoryCount(itemId) >= amount;
		}
		@Override public void settleWithdrawals() { }
		@Override public void closeBank() { closedBank = true; }
		@Override public boolean awaitBankClosed() { return true; }
		@Override public void prepareFinalRoute(WorldPoint target) { preparedFinalRoute = true; }
	}
}
