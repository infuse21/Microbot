package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable output of one route calculation.
 *
 * <p>The request and generation identifiers make publication explicit: a calculation may finish
 * after cancellation, but a planner can only publish it when both identifiers still match the
 * active request. Raw and smoothed paths are copied once at the boundary so runtime execution never
 * observes a pathfinder mutating underneath it.</p>
 */
public final class RoutePlan
{
	private final long requestId;
	private final long generation;
	private final WorldPoint start;
	private final Set<WorldPoint> targets;
	private final List<WorldPoint> rawPath;
	private final List<WorldPoint> smoothedPath;
	private final List<RouteEdge> routeEdges;
	private final int[] smoothedToRaw;
	private final boolean complete;

	public RoutePlan(long requestId, long generation, WorldPoint start, Set<WorldPoint> targets,
		List<WorldPoint> rawPath, List<WorldPoint> smoothedPath, boolean complete)
	{
		this(requestId, generation, start, targets, rawPath, smoothedPath, complete, null);
	}

	public RoutePlan(long requestId, long generation, WorldPoint start, Set<WorldPoint> targets,
		List<WorldPoint> rawPath, List<WorldPoint> smoothedPath, boolean complete,
		List<RouteEdge> routeEdges)
	{
		if (requestId <= 0)
		{
			throw new IllegalArgumentException("requestId must be positive");
		}
		if (generation <= 0)
		{
			throw new IllegalArgumentException("generation must be positive");
		}
		this.requestId = requestId;
		this.generation = generation;
		this.start = Objects.requireNonNull(start, "start");
		this.targets = Collections.unmodifiableSet(new LinkedHashSet<>(
			Objects.requireNonNull(targets, "targets")));
		this.rawPath = immutablePath(rawPath, "rawPath");
		this.smoothedPath = immutablePath(smoothedPath, "smoothedPath");
		this.routeEdges = Collections.unmodifiableList(new ArrayList<>(routeEdges == null
			? deriveEdges(this.rawPath) : routeEdges));
		validateEdges(this.rawPath, this.routeEdges);
		this.smoothedToRaw = mapSmoothedToRaw(this.smoothedPath, this.rawPath);
		this.complete = complete;
	}

	private static List<WorldPoint> immutablePath(List<WorldPoint> path, String name)
	{
		return Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(path, name)));
	}

	private static List<RouteEdge> deriveEdges(List<WorldPoint> rawPath)
	{
		List<RouteEdge> edges = new ArrayList<>(Math.max(0, rawPath.size() - 1));
		for (int i = 0; i + 1 < rawPath.size(); i++)
		{
			WorldPoint from = rawPath.get(i);
			WorldPoint to = rawPath.get(i + 1);
			boolean adjacent = from.getPlane() == to.getPlane() && from.distanceTo2D(to) <= 1;
			edges.add(new RouteEdge(i, from, to,
				adjacent ? RouteEdge.Kind.WALK : RouteEdge.Kind.TRANSPORT));
		}
		return edges;
	}

	private static void validateEdges(List<WorldPoint> rawPath, List<RouteEdge> edges)
	{
		if (edges.size() != Math.max(0, rawPath.size() - 1))
		{
			throw new IllegalArgumentException("routeEdges must describe every raw-path edge");
		}
		for (int i = 0; i < edges.size(); i++)
		{
			RouteEdge edge = edges.get(i);
			if (edge.getRawIndex() != i || !edge.getFrom().equals(rawPath.get(i))
				|| !edge.getTo().equals(rawPath.get(i + 1)))
			{
				throw new IllegalArgumentException("routeEdges do not align with rawPath at " + i);
			}
		}
	}

	private static int[] mapSmoothedToRaw(List<WorldPoint> smoothed, List<WorldPoint> raw)
	{
		if (smoothed.isEmpty() || raw.isEmpty())
		{
			return new int[0];
		}
		int[] mapping = new int[smoothed.size()];
		int rawIndex = 0;
		for (int i = 0; i < smoothed.size(); i++)
		{
			while (rawIndex < raw.size() && !raw.get(rawIndex).equals(smoothed.get(i)))
			{
				rawIndex++;
			}
			mapping[i] = Math.min(rawIndex, raw.size() - 1);
		}
		return mapping;
	}

	public long getRequestId()
	{
		return requestId;
	}

	public long getGeneration()
	{
		return generation;
	}

	public WorldPoint getStart()
	{
		return start;
	}

	public Set<WorldPoint> getTargets()
	{
		return targets;
	}

	public List<WorldPoint> getRawPath()
	{
		return rawPath;
	}

	public List<WorldPoint> getSmoothedPath()
	{
		return smoothedPath;
	}

	public List<RouteEdge> getRouteEdges()
	{
		return routeEdges;
	}

	public int[] getSmoothedToRaw()
	{
		return smoothedToRaw.clone();
	}

	public boolean isOrdinaryWalkOnly()
	{
		return routeEdges.stream()
			.allMatch(edge -> edge.getKind() == RouteEdge.Kind.WALK);
	}

	/** Whether every edge is owned by a transport/obstacle family already migrated to the engine. */
	public boolean isEngineSupported()
	{
		return routeEdges.stream().allMatch(edge -> edge.getKind() == RouteEdge.Kind.WALK
			|| edge.getKind() == RouteEdge.Kind.SIMPLE_TELEPORT
			|| edge.getKind() == RouteEdge.Kind.NPC_TRANSPORT
			|| edge.getKind() == RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT
			|| edge.getKind() == RouteEdge.Kind.CHARTER_SHIP
			|| edge.getKind() == RouteEdge.Kind.ADJACENT_TRANSPORT
			|| edge.getKind() == RouteEdge.Kind.CATALOG_TRANSITION);
	}

	public boolean isComplete()
	{
		return complete;
	}

	public WorldPoint getEndpoint()
	{
		return rawPath.isEmpty() ? null : rawPath.get(rawPath.size() - 1);
	}
}
