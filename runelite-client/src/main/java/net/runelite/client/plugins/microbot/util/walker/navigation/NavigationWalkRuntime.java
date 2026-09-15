package net.runelite.client.plugins.microbot.util.walker.navigation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Process-wide owner of the blocking compatibility call which drives one NavigationEngine walk.
 *
 * <p>The shortest-path UI only publishes start, replace and cancel intent here. The compatibility
 * call into {@link Rs2Walker} is a thin NavigationEngine delegate; request replacement is generation
 * guarded so a stale worker cannot clear a newer UI target.</p>
 */
@Slf4j
public final class NavigationWalkRuntime
{
	@FunctionalInterface
	interface WalkCommand
	{
		WalkerState walk(WorldPoint target, boolean withBankedTransports);
	}

	@FunctionalInterface
	interface RouteCanceller
	{
		void cancel(String reason);
	}

	private static final Object MUTEX = new Object();
	private static ExecutorService executor;
	private static Future<?> activeTask;
	private static volatile WorldPoint target;
	private static long generation;
	private static WalkCommand walkCommand = NavigationWalkRuntime::walk;
	private static RouteCanceller routeCanceller = Rs2Walker::clearWalkingRoute;

	private NavigationWalkRuntime()
	{
	}

	public static void start(WorldPoint nextTarget, boolean withBankedTransports)
	{
		Objects.requireNonNull(nextTarget, "nextTarget");
		synchronized (MUTEX)
		{
			ensureExecutor();
			if (activeTask != null && !activeTask.isDone())
			{
				routeCanceller.cancel("shortest-path-ui:replace-target");
				activeTask.cancel(true);
			}
			activeTask = null;
			target = nextTarget;
			long nextGeneration = ++generation;
			activeTask = executor.submit(() -> run(nextGeneration, nextTarget,
				withBankedTransports));
		}
	}

	public static void cancel(String reason)
	{
		synchronized (MUTEX)
		{
			cancelLocked(normalizeReason(reason));
		}
	}

	public static WorldPoint getTarget()
	{
		return target;
	}

	public static void shutdown()
	{
		final ExecutorService previous;
		synchronized (MUTEX)
		{
			cancelLocked("shortest-path-ui:shutdown");
			previous = executor;
			executor = null;
		}
		if (previous != null)
		{
			previous.shutdownNow();
		}
	}

	private static void run(long ownerGeneration, WorldPoint ownerTarget,
		boolean withBankedTransports)
	{
		try
		{
			walkCommand.walk(ownerTarget, withBankedTransports);
			finish(ownerGeneration, ownerTarget, "shortest-path-ui:terminal");
		}
		catch (RuntimeException failure)
		{
			if (!Thread.currentThread().isInterrupted())
			{
				log.warn("Navigation walk failed: {}", failure.getMessage());
			}
			finish(ownerGeneration, ownerTarget, "shortest-path-ui:exception");
		}
	}

	private static void finish(long ownerGeneration, WorldPoint ownerTarget, String reason)
	{
		synchronized (MUTEX)
		{
			if (generation != ownerGeneration || !ownerTarget.equals(target))
			{
				return;
			}
			target = null;
			activeTask = null;
			generation++;
			routeCanceller.cancel(reason);
		}
	}

	private static void cancelLocked(String reason)
	{
		target = null;
		generation++;
		Future<?> previous = activeTask;
		activeTask = null;
		routeCanceller.cancel(reason);
		if (previous != null && !previous.isDone())
		{
			previous.cancel(true);
		}
	}

	private static WalkerState walk(WorldPoint destination, boolean withBankedTransports)
	{
		return withBankedTransports
			? Rs2Walker.walkWithBankedTransportsAndState(destination, 10, false)
			: Rs2Walker.walkWithState(destination);
	}

	private static String normalizeReason(String reason)
	{
		return reason == null || reason.isBlank() ? "shortest-path-ui:cancel" : reason;
	}

	private static void ensureExecutor()
	{
		if (executor == null || executor.isShutdown())
		{
			executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
				.setNameFormat("shortest-path-walk-%d")
				.build());
		}
	}

	static void installForTesting(ExecutorService testExecutor, WalkCommand testWalkCommand,
		RouteCanceller testRouteCanceller)
	{
		synchronized (MUTEX)
		{
			if (activeTask != null)
			{
				activeTask.cancel(true);
			}
			if (executor != null)
			{
				executor.shutdownNow();
			}
			activeTask = null;
			target = null;
			generation++;
			executor = Objects.requireNonNull(testExecutor, "testExecutor");
			walkCommand = Objects.requireNonNull(testWalkCommand, "testWalkCommand");
			routeCanceller = Objects.requireNonNull(testRouteCanceller, "testRouteCanceller");
		}
	}

	static void resetForTesting()
	{
		synchronized (MUTEX)
		{
			if (activeTask != null)
			{
				activeTask.cancel(true);
			}
			if (executor != null)
			{
				executor.shutdownNow();
			}
			executor = null;
			activeTask = null;
			walkCommand = NavigationWalkRuntime::walk;
			routeCanceller = Rs2Walker::clearWalkingRoute;
			target = null;
			generation = 0;
		}
	}
}
