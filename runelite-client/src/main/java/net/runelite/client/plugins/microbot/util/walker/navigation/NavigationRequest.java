package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Immutable input and cancellation identity for one navigation session. */
public final class NavigationRequest
{
	public static final class CancellationToken
	{
		private final AtomicBoolean cancelled = new AtomicBoolean();

		public boolean isCancelled()
		{
			return cancelled.get();
		}

		private void cancel()
		{
			cancelled.set(true);
		}
	}

	private final long requestId;
	private final Set<WorldPoint> destinations;
	private final int reachedDistance;
	private final NavigationRouteOptions routeOptions;
	private final String source;
	private final CancellationToken cancellationToken = new CancellationToken();

	public NavigationRequest(long requestId, Set<WorldPoint> destinations, int reachedDistance,
		NavigationRouteOptions routeOptions, String source)
	{
		if (requestId <= 0)
		{
			throw new IllegalArgumentException("requestId must be positive");
		}
		if (reachedDistance < 0)
		{
			throw new IllegalArgumentException("reachedDistance cannot be negative");
		}
		this.requestId = requestId;
		this.destinations = Collections.unmodifiableSet(new LinkedHashSet<>(
			Objects.requireNonNull(destinations, "destinations")));
		if (this.destinations.isEmpty())
		{
			throw new IllegalArgumentException("destinations cannot be empty");
		}
		this.reachedDistance = reachedDistance;
		this.routeOptions = Objects.requireNonNull(routeOptions, "routeOptions");
		this.source = Objects.requireNonNull(source, "source");
	}

	public long getRequestId()
	{
		return requestId;
	}

	public Set<WorldPoint> getDestinations()
	{
		return destinations;
	}

	public int getReachedDistance()
	{
		return reachedDistance;
	}

	public NavigationRouteOptions getRouteOptions()
	{
		return routeOptions;
	}

	public String getSource()
	{
		return source;
	}

	public CancellationToken getCancellationToken()
	{
		return cancellationToken;
	}

	void cancel()
	{
		cancellationToken.cancel();
	}
}
