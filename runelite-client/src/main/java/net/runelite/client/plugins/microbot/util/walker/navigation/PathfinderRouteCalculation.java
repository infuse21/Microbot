package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.util.walker.transport.AdjacentTransportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CharterShipPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.FairyRingPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.SpiritTreePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.GnomeGliderPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.QuetzalPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.TeleportationLeverPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.WildernessDitchPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.JungleObstaclePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CanoePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.MinecartPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.MinigameTeleportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.MagicMushtreePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.HotAirBalloonPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.SimpleTeleportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.ItemTeleportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcDialogueTransportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcTransportPolicy;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Converts one existing {@link Pathfinder} run into an immutable {@link RoutePlan}. */
public final class PathfinderRouteCalculation implements RoutePlanner.Calculation
{
	private final Pathfinder pathfinder;
	private final AtomicBoolean cancelled = new AtomicBoolean();

	public PathfinderRouteCalculation(Pathfinder pathfinder)
	{
		this.pathfinder = Objects.requireNonNull(pathfinder, "pathfinder");
	}

	@Override
	public RoutePlan calculate(long requestId, long generation)
	{
		if (cancelled.get())
		{
			throw new CancellationException("route calculation cancelled before start");
		}
		pathfinder.run();
		if (cancelled.get() || !pathfinder.isDone())
		{
			throw new CancellationException("route calculation cancelled");
		}

		return snapshot(pathfinder, requestId, generation);
	}

	@Override
	public void cancel()
	{
		cancelled.set(true);
		pathfinder.cancel();
	}

	public static RoutePlan snapshot(Pathfinder pathfinder, long requestId, long generation)
	{
		Objects.requireNonNull(pathfinder, "pathfinder");
		List<WorldPoint> rawPath = pathfinder.getPath();
		List<WorldPoint> smoothedPath = pathfinder.getWalkablePath();
		Set<WorldPoint> targets = pathfinder.getTargets();
		boolean complete = reachesTarget(rawPath, targets);
		List<RouteEdge> edges = routeEdges(pathfinder, rawPath);
		return new RoutePlan(requestId, generation, pathfinder.getStart(), targets,
			rawPath, smoothedPath, complete, edges);
	}

	private static List<RouteEdge> routeEdges(Pathfinder pathfinder, List<WorldPoint> rawPath)
	{
		java.util.ArrayList<RouteEdge> edges = new java.util.ArrayList<>(
			Math.max(0, rawPath.size() - 1));
		for (int i = 0; i + 1 < rawPath.size(); i++)
		{
			WorldPoint from = rawPath.get(i);
			WorldPoint to = rawPath.get(i + 1);
			boolean adjacent = from.getPlane() == to.getPlane() && from.distanceTo2D(to) <= 1;
			boolean transport = !adjacent || pathfinder.isTransportEdge(from, to);
			RouteEdge.Kind kind = RouteEdge.Kind.WALK;
			if (transport)
			{
				Set<net.runelite.client.plugins.microbot.shortestpath.Transport> transports =
					pathfinder.getTransportsForEdge(from, to);
				kind = classifyTransportEdge(transports);
			}
			edges.add(new RouteEdge(i, from, to, kind));
		}
		return edges;
	}

	static RouteEdge.Kind classifyTransportEdge(
		Set<net.runelite.client.plugins.microbot.shortestpath.Transport> transports)
	{
		if (transports == null || transports.isEmpty())
		{
			return RouteEdge.Kind.TRANSPORT;
		}
		if (transports.stream().anyMatch(SimpleTeleportPolicy::isEligible))
		{
			return RouteEdge.Kind.SIMPLE_TELEPORT;
		}
		if (transports.stream().anyMatch(ItemTeleportPolicy::isEligible))
		{
			return RouteEdge.Kind.ITEM_TELEPORT;
		}
		if (transports.stream().anyMatch(NpcTransportPolicy::isEligible))
		{
			return RouteEdge.Kind.NPC_TRANSPORT;
		}
		if (transports.stream().anyMatch(NpcDialogueTransportPolicy::isEligible))
		{
			return RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT;
		}
		if (transports.stream().anyMatch(CharterShipPolicy::isEligible))
		{
			return RouteEdge.Kind.CHARTER_SHIP;
		}
		if (transports.stream().anyMatch(FairyRingPolicy::isEligible))
		{
			return RouteEdge.Kind.FAIRY_RING;
		}
		if (transports.stream().anyMatch(SpiritTreePolicy::isEligible))
		{
			return RouteEdge.Kind.SPIRIT_TREE;
		}
		if (transports.stream().anyMatch(GnomeGliderPolicy::isEligible))
		{
			return RouteEdge.Kind.GNOME_GLIDER;
		}
		if (transports.stream().anyMatch(QuetzalPolicy::isEligible))
		{
			return RouteEdge.Kind.QUETZAL;
		}
		if (transports.stream().anyMatch(TeleportationLeverPolicy::isEligible))
		{
			return RouteEdge.Kind.TELEPORTATION_LEVER;
		}
		if (transports.stream().anyMatch(WildernessDitchPolicy::isEligible))
		{
			return RouteEdge.Kind.WILDERNESS_DITCH;
		}
		if (transports.stream().anyMatch(JungleObstaclePolicy::isEligible))
		{
			return RouteEdge.Kind.JUNGLE_OBSTACLE;
		}
		if (transports.stream().anyMatch(CanoePolicy::isEligible))
		{
			return RouteEdge.Kind.CANOE;
		}
		if (transports.stream().anyMatch(MinecartPolicy::isEligible))
		{
			return RouteEdge.Kind.MINECART;
		}
		if (transports.stream().anyMatch(TeleportationPortalPolicy::isEligible))
		{
			return RouteEdge.Kind.TELEPORTATION_PORTAL;
		}
		if (transports.stream().anyMatch(MinigameTeleportPolicy::isEligible))
		{
			return RouteEdge.Kind.MINIGAME_TELEPORT;
		}
		if (transports.stream().anyMatch(MagicMushtreePolicy::isEligible))
		{
			return RouteEdge.Kind.MAGIC_MUSHTREE;
		}
		if (transports.stream().anyMatch(HotAirBalloonPolicy::isEligible))
		{
			return RouteEdge.Kind.HOT_AIR_BALLOON;
		}
		if (transports.stream().anyMatch(AdjacentTransportPolicy::isEligible))
		{
			return RouteEdge.Kind.ADJACENT_TRANSPORT;
		}
		if (transports.stream().anyMatch(CatalogTransitionPolicy::isEligible))
		{
			return RouteEdge.Kind.CATALOG_TRANSITION;
		}
		return RouteEdge.Kind.TRANSPORT;
	}

	private static boolean reachesTarget(List<WorldPoint> path, Set<WorldPoint> targets)
	{
		if (path == null || path.isEmpty())
		{
			return false;
		}
		WorldPoint endpoint = path.get(path.size() - 1);
		return targets.contains(endpoint);
	}
}
