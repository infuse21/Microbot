package net.runelite.client.plugins.microbot.util.walker.banking;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.WebWalkLog;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;

import java.util.Map;
import java.util.Objects;

/**
 * Explicit coordinator for the bank leg, withdrawal transaction, and final navigation leg.
 * Each walk remains owned by the navigation engine; this type owns only the multi-leg setup.
 */
public final class BankedTransportCoordinator
{
	public enum Phase
	{
		WALK_TO_BANK,
		OPEN_BANK,
		WITHDRAW_ITEMS,
		CLOSE_BANK,
		PREPARE_FINAL_ROUTE,
		WALK_TO_TARGET,
		COMPLETE,
		FAILED
	}

	public interface Operations
	{
		WalkerState walk(WorldPoint target, int reachedDistance);

		void beforeOpenBank();

		boolean openBank();

		boolean awaitBankOpen();

		int inventoryCount(int itemId);

		boolean hasBankItem(int itemId, int amount);

		boolean withdraw(int itemId, int amount);

		boolean awaitInventoryQuantity(int itemId, int amount);

		void settleWithdrawals();

		void closeBank();

		boolean awaitBankClosed();

		void prepareFinalRoute(WorldPoint target);
	}

	public static final class Result
	{
		private final Phase phase;
		private final WalkerState walkerState;
		private final Integer failedItemId;

		private Result(Phase phase, WalkerState walkerState, Integer failedItemId)
		{
			this.phase = phase;
			this.walkerState = walkerState;
			this.failedItemId = failedItemId;
		}

		public Phase getPhase() { return phase; }
		public WalkerState getWalkerState() { return walkerState; }
		public Integer getFailedItemId() { return failedItemId; }
	}

	private BankedTransportCoordinator()
	{
	}

	public static Result execute(WorldPoint bank, Map<Integer, Integer> requirements,
		WorldPoint target, int reachedDistance, Operations operations)
	{
		if (bank == null || target == null || operations == null)
		{
			return failed(Phase.FAILED, null);
		}
		Map<Integer, Integer> safeRequirements = requirements == null ? Map.of() : requirements;

		WebWalkLog.bankSetupInfo("phase={} bank={} target={} requirements={}",
				Phase.WALK_TO_BANK, bank, target, safeRequirements.size());
		WalkerState bankWalk = operations.walk(bank, reachedDistance);
		if (bankWalk != WalkerState.ARRIVED)
		{
			return new Result(Phase.WALK_TO_BANK, bankWalk, null);
		}

		operations.beforeOpenBank();
		WebWalkLog.bankSetupInfo("phase={}", Phase.OPEN_BANK);
		if (!operations.openBank() || !operations.awaitBankOpen())
		{
			return failed(Phase.OPEN_BANK, null);
		}

		boolean withdrew = false;
		WebWalkLog.bankSetupInfo("phase={}", Phase.WITHDRAW_ITEMS);
		for (Map.Entry<Integer, Integer> requirement : safeRequirements.entrySet())
		{
			int itemId = requirement.getKey();
			int required = Math.max(0, Objects.requireNonNullElse(requirement.getValue(), 0));
			int carried = Math.max(0, operations.inventoryCount(itemId));
			int amount = Rs2WalkerBankingPlanner.amountToWithdraw(required, carried);
			if (amount == 0)
			{
				continue;
			}
			if (!operations.hasBankItem(itemId, amount)
				|| !operations.withdraw(itemId, amount)
				|| !operations.awaitInventoryQuantity(itemId, carried + amount))
			{
				// Leave the UI in a known state even though the route is terminal. The target leg
				// must not run after an unsatisfied requirement.
				operations.closeBank();
				operations.awaitBankClosed();
				return failed(Phase.WITHDRAW_ITEMS, itemId);
			}
			WebWalkLog.bankSetupInfo("withdraw itemId={} amount={} carried={}",
					itemId, amount, carried);
			withdrew = true;
		}
		if (withdrew)
		{
			operations.settleWithdrawals();
		}

		WebWalkLog.bankSetupInfo("phase={}", Phase.CLOSE_BANK);
		operations.closeBank();
		if (!operations.awaitBankClosed())
		{
			return failed(Phase.CLOSE_BANK, null);
		}
		WebWalkLog.bankSetupInfo("phase={} target={}", Phase.PREPARE_FINAL_ROUTE, target);
		operations.prepareFinalRoute(target);

		WebWalkLog.bankSetupInfo("phase={} target={}", Phase.WALK_TO_TARGET, target);
		WalkerState finalWalk = operations.walk(target, reachedDistance);
		if (finalWalk == WalkerState.ARRIVED)
		{
			WebWalkLog.bankSetupInfo("phase={} target={}", Phase.COMPLETE, target);
		}
		return finalWalk == WalkerState.ARRIVED
			? new Result(Phase.COMPLETE, finalWalk, null)
			: new Result(Phase.WALK_TO_TARGET, finalWalk, null);
	}

	private static Result failed(Phase phase, Integer itemId)
	{
		return new Result(phase, WalkerState.EXIT, itemId);
	}
}
