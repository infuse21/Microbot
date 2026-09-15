package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.ArrayDeque;
import java.util.Queue;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NavigationWalkCoordinatorTest
{
	private static final WorldPoint TARGET = new WorldPoint(3200, 3200, 0);

	@Test
	public void completeClearsTheCompatibilityTarget()
	{
		FakeDriver driver = new FakeDriver(owned(NavigationDecision.Type.COMPLETE));

		assertEquals(WalkerState.ARRIVED, NavigationWalkCoordinator.walk(TARGET, 2, driver));
		assertEquals("rs2walker:navigation-engine:arrived", driver.clearReason);
		assertEquals(0, driver.progressWaits);
	}

	@Test
	public void replanIsOwnedByTheCoordinatorBeforeTheNextPass()
	{
		FakeDriver driver = new FakeDriver(
			owned(NavigationDecision.Type.REQUEST_REPLAN),
			owned(NavigationDecision.Type.COMPLETE));

		assertEquals(WalkerState.ARRIVED, NavigationWalkCoordinator.walk(TARGET, 2, driver));
		assertEquals(1, driver.replans);
		assertEquals(1, driver.replanWaits);
		assertEquals(2, driver.passes);
	}

	@Test
	public void unavailableOwnerCannotFallBack()
	{
		FakeDriver driver = new FakeDriver(new NavigationExecutionResult(
			NavigationDecision.of(NavigationDecision.Type.NO_ACTION, "not-started"),
			false, false, "none"));

		assertEquals(WalkerState.EXIT, NavigationWalkCoordinator.walk(TARGET, 2, driver));
		assertEquals(null, driver.clearReason);
	}

	private static NavigationExecutionResult owned(NavigationDecision.Type type)
	{
		return new NavigationExecutionResult(NavigationDecision.of(type, type.name().toLowerCase()),
			true, false, "test");
	}

	private static final class FakeDriver implements NavigationWalkCoordinator.Driver
	{
		private final Queue<NavigationExecutionResult> results = new ArrayDeque<>();
		private int passes;
		private int replans;
		private int replanWaits;
		private int progressWaits;
		private String clearReason;

		private FakeDriver(NavigationExecutionResult... results)
		{
			for (NavigationExecutionResult result : results)
			{
				this.results.add(result);
			}
		}

		@Override
		public NavigationExecutionResult executePass(String reason)
		{
			passes++;
			return results.remove();
		}

		@Override
		public boolean isCancelled(WorldPoint target)
		{
			return false;
		}

		@Override
		public void clearTarget(String reason)
		{
			clearReason = reason;
		}

		@Override
		public void requestReplan()
		{
			replans++;
		}

		@Override
		public void awaitReplan(WorldPoint target, long generation)
		{
			replanWaits++;
		}

		@Override
		public void awaitProgress(WorldPoint target)
		{
			progressWaits++;
		}
	}
}
