package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.Objects;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.util.walker.WebWalkLog;

/**
 * Owns the worker-thread compatibility lifecycle for one NavigationEngine request.
 * The driver may use bounded waits between passes, but this coordinator must never run on the client thread.
 */
public final class NavigationWalkCoordinator
{
	public interface Driver
	{
		NavigationExecutionResult executePass(String reason);

		boolean isCancelled(WorldPoint target);

		void clearTarget(String reason);

		void requestReplan();

		void awaitReplan(WorldPoint target, long generation);

		void awaitProgress(WorldPoint target);
	}

	private NavigationWalkCoordinator()
	{
	}

	public static WalkerState walk(WorldPoint target, int passBudget, Driver driver)
	{
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(driver, "driver");
		if (passBudget <= 0)
		{
			throw new IllegalArgumentException("passBudget must be positive");
		}

		NavigationExecutionResult result = driver.executePass("engine-start");
		if (!result.isEngineOwned())
		{
			WebWalkLog.spInfo("navigation-owner-unavailable | stopping without legacy fallback");
			return WalkerState.EXIT;
		}

		for (int pass = 0; pass < passBudget; pass++)
		{
			if (driver.isCancelled(target))
			{
				NavigationEngineRuntime.cancel("engine-walk-cancelled");
				return WalkerState.EXIT;
			}

			NavigationDecision decision = result.getDecision();
			NavigationSnapshot snapshot = NavigationEngineRuntime.getSnapshot();
			if (decision.getType() == NavigationDecision.Type.COMPLETE)
			{
				driver.clearTarget("rs2walker:navigation-engine:arrived");
				return WalkerState.ARRIVED;
			}
			if (decision.getType() == NavigationDecision.Type.FAIL)
			{
				NavigationPhase phase = snapshot == null ? NavigationPhase.FAILED : snapshot.getPhase();
				driver.clearTarget("rs2walker:navigation-engine:" + decision.getReason());
				return phase == NavigationPhase.UNREACHABLE
					? WalkerState.UNREACHABLE : WalkerState.EXIT;
			}
			if (decision.getType() == NavigationDecision.Type.REQUEST_REPLAN)
			{
				long generation = snapshot == null ? 0 : snapshot.getGeneration();
				driver.requestReplan();
				driver.awaitReplan(target, generation);
			}
			else
			{
				driver.awaitProgress(target);
			}
			result = driver.executePass("engine-pass-" + pass);
		}

		NavigationEngineRuntime.cancel("engine-pass-budget-exhausted");
		driver.clearTarget("rs2walker:navigation-engine:pass-budget-exhausted");
		return WalkerState.EXIT;
	}
}
