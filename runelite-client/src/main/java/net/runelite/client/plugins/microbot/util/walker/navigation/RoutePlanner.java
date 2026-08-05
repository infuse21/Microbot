package net.runelite.client.plugins.microbot.util.walker.navigation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Single owner of asynchronous route-calculation lifecycle.
 *
 * <p>This class deliberately knows nothing about UI markers or walker actions. It serialises
 * submit/cancel/publication state and rejects stale completions using a request ID plus a generation.
 * A later production adapter supplies the existing Pathfinder calculation behind
 * {@link Calculation}; callers never own its Future.</p>
 */
public final class RoutePlanner implements AutoCloseable
{
	@FunctionalInterface
	public interface Calculation
	{
		RoutePlan calculate(long requestId, long generation) throws Exception;

		default void cancel()
		{
		}
	}

	@FunctionalInterface
	public interface CompletedPlanFactory
	{
		RoutePlan create(long requestId, long generation);
	}

	public static final class Ticket
	{
		private final long requestId;
		private final long generation;

		private Ticket(long requestId, long generation)
		{
			this.requestId = requestId;
			this.generation = generation;
		}

		public long getRequestId()
		{
			return requestId;
		}

		public long getGeneration()
		{
			return generation;
		}
	}

	private final Object mutex = new Object();
	private final ExecutorService executor;
	private final boolean ownsExecutor;

	private long activeRequestId;
	private long activeGeneration;
	private Calculation activeCalculation;
	private Future<?> activeFuture;
	private RoutePlan publishedPlan;
	private Throwable lastFailure;
	private boolean closed;

	public RoutePlanner()
	{
		this(Executors.newSingleThreadExecutor(
			new ThreadFactoryBuilder().setNameFormat("route-planner-%d").build()), true);
	}

	RoutePlanner(ExecutorService executor, boolean ownsExecutor)
	{
		this.executor = Objects.requireNonNull(executor, "executor");
		this.ownsExecutor = ownsExecutor;
	}

	public Ticket submit(long requestId, Calculation calculation)
	{
		if (requestId <= 0)
		{
			throw new IllegalArgumentException("requestId must be positive");
		}
		Objects.requireNonNull(calculation, "calculation");

		synchronized (mutex)
		{
			ensureOpen();
			cancelActiveLocked();
			activeGeneration = activeRequestId == requestId ? activeGeneration + 1 : 1;
			activeRequestId = requestId;
			long generation = activeGeneration;
			activeCalculation = calculation;
			publishedPlan = null;
			lastFailure = null;
			activeFuture = executor.submit(() -> runCalculation(requestId, generation, calculation));
			return new Ticket(requestId, generation);
		}
	}

	public void cancel(long requestId)
	{
		synchronized (mutex)
		{
			if (activeRequestId != requestId)
			{
				return;
			}
			cancelActiveLocked();
			activeRequestId = 0;
			activeGeneration = 0;
			publishedPlan = null;
			lastFailure = null;
		}
	}

	/** Cancels the current calculation but preserves identity so the next submit advances generation. */
	public void supersede(long requestId)
	{
		synchronized (mutex)
		{
			if (activeRequestId != requestId)
			{
				return;
			}
			cancelActiveLocked();
			publishedPlan = null;
			lastFailure = null;
		}
	}

	/** Publishes a pathfinder that was calculated synchronously (the cave-routing compatibility path). */
	public Ticket publishCompleted(long requestId, CompletedPlanFactory factory)
	{
		if (requestId <= 0)
		{
			throw new IllegalArgumentException("requestId must be positive");
		}
		Objects.requireNonNull(factory, "factory");
		synchronized (mutex)
		{
			ensureOpen();
			cancelActiveLocked();
			activeGeneration = activeRequestId == requestId ? activeGeneration + 1 : 1;
			activeRequestId = requestId;
			RoutePlan result = Objects.requireNonNull(
				factory.create(requestId, activeGeneration), "completed plan");
			if (result.getRequestId() != requestId || result.getGeneration() != activeGeneration)
			{
				throw new IllegalStateException("Completed plan has mismatched route identity");
			}
			publishedPlan = result;
			lastFailure = null;
			return new Ticket(requestId, activeGeneration);
		}
	}

	public RoutePlan getPublishedPlan()
	{
		synchronized (mutex)
		{
			return publishedPlan;
		}
	}

	public Throwable getLastFailure()
	{
		synchronized (mutex)
		{
			return lastFailure;
		}
	}

	public Ticket getActiveTicket()
	{
		synchronized (mutex)
		{
			return activeRequestId == 0 ? null : new Ticket(activeRequestId, activeGeneration);
		}
	}

	private void runCalculation(long requestId, long generation, Calculation calculation)
	{
		try
		{
			RoutePlan result = Objects.requireNonNull(
				calculation.calculate(requestId, generation), "calculation result");
			if (result.getRequestId() != requestId || result.getGeneration() != generation)
			{
				throw new IllegalStateException("Calculation returned mismatched route identity");
			}
			synchronized (mutex)
			{
				if (isActive(requestId, generation, calculation))
				{
					publishedPlan = result;
					activeCalculation = null;
					activeFuture = null;
				}
			}
		}
		catch (InterruptedException interrupted)
		{
			Thread.currentThread().interrupt();
		}
		catch (Throwable failure)
		{
			synchronized (mutex)
			{
				if (isActive(requestId, generation, calculation))
				{
					lastFailure = failure;
					activeCalculation = null;
					activeFuture = null;
				}
			}
		}
	}

	private boolean isActive(long requestId, long generation, Calculation calculation)
	{
		return activeRequestId == requestId
			&& activeGeneration == generation
			&& activeCalculation == calculation;
	}

	private void cancelActiveLocked()
	{
		if (activeCalculation != null)
		{
			activeCalculation.cancel();
		}
		if (activeFuture != null)
		{
			activeFuture.cancel(true);
		}
		activeCalculation = null;
		activeFuture = null;
	}

	private void ensureOpen()
	{
		if (closed)
		{
			throw new IllegalStateException("RoutePlanner is closed");
		}
	}

	@Override
	public void close()
	{
		synchronized (mutex)
		{
			if (closed)
			{
				return;
			}
			closed = true;
			cancelActiveLocked();
			activeRequestId = 0;
			activeGeneration = 0;
			publishedPlan = null;
			lastFailure = null;
		}
		if (ownsExecutor)
		{
			executor.shutdownNow();
		}
	}
}
