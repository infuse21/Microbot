package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NavigationWalkRuntimeTest
{
	private static final WorldPoint FIRST = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint SECOND = new WorldPoint(3201, 3200, 0);

	@After
	public void resetRuntime()
	{
		NavigationWalkRuntime.resetForTesting();
	}

	@Test
	public void targetReplacementRejectsStaleCompletion() throws Exception
	{
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch firstStarted = new CountDownLatch(1);
		CountDownLatch secondFinished = new CountDownLatch(1);
		List<WorldPoint> walked = new CopyOnWriteArrayList<>();
		NavigationWalkRuntime.installForTesting(executor, (target, banked) -> {
			walked.add(target);
			if (FIRST.equals(target))
			{
				firstStarted.countDown();
				try
				{
					new CountDownLatch(1).await();
				}
				catch (InterruptedException interrupted)
				{
					Thread.currentThread().interrupt();
				}
			}
			else
			{
				secondFinished.countDown();
			}
			return WalkerState.ARRIVED;
		}, reason -> { });

		NavigationWalkRuntime.start(FIRST, false);
		assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
		NavigationWalkRuntime.start(SECOND, false);
		assertEquals(SECOND, NavigationWalkRuntime.getTarget());
		assertTrue(secondFinished.await(1, TimeUnit.SECONDS));
		awaitTargetClear();

		assertEquals(List.of(FIRST, SECOND), walked);
		assertNull(NavigationWalkRuntime.getTarget());
	}

	@Test
	public void cancelClearsTargetAndInterruptsInFlightWalk() throws Exception
	{
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch interrupted = new CountDownLatch(1);
		List<String> reasons = new CopyOnWriteArrayList<>();
		NavigationWalkRuntime.installForTesting(executor, (target, banked) -> {
			started.countDown();
			try
			{
				new CountDownLatch(1).await();
			}
			catch (InterruptedException expected)
			{
				interrupted.countDown();
				Thread.currentThread().interrupt();
			}
			return WalkerState.EXIT;
		}, reasons::add);

		NavigationWalkRuntime.start(FIRST, false);
		assertTrue(started.await(1, TimeUnit.SECONDS));
		NavigationWalkRuntime.cancel("hotkey:ctrl+x");

		assertTrue(interrupted.await(1, TimeUnit.SECONDS));
		assertNull(NavigationWalkRuntime.getTarget());
		assertTrue(reasons.contains("hotkey:ctrl+x"));
	}

	@Test
	public void shutdownCancelsInFlightWalkAndCanRestart() throws Exception
	{
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch interrupted = new CountDownLatch(1);
		CountDownLatch restarted = new CountDownLatch(1);
		AtomicInteger invocation = new AtomicInteger();
		NavigationWalkRuntime.installForTesting(executor, (target, banked) -> {
			if (invocation.getAndIncrement() > 0)
			{
				restarted.countDown();
				return WalkerState.ARRIVED;
			}
			started.countDown();
			try
			{
				new CountDownLatch(1).await();
			}
			catch (InterruptedException expected)
			{
				interrupted.countDown();
				Thread.currentThread().interrupt();
			}
			return WalkerState.EXIT;
		}, reason -> { });

		NavigationWalkRuntime.start(FIRST, true);
		assertTrue(started.await(1, TimeUnit.SECONDS));
		NavigationWalkRuntime.shutdown();

		assertTrue(interrupted.await(1, TimeUnit.SECONDS));
		assertNull(NavigationWalkRuntime.getTarget());

		NavigationWalkRuntime.start(SECOND, false);
		assertTrue(restarted.await(1, TimeUnit.SECONDS));
		awaitTargetClear();
		assertNull(NavigationWalkRuntime.getTarget());
	}

	private static void awaitTargetClear() throws InterruptedException
	{
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
		while (NavigationWalkRuntime.getTarget() != null && System.nanoTime() < deadline)
		{
			Thread.sleep(5);
		}
	}
}
