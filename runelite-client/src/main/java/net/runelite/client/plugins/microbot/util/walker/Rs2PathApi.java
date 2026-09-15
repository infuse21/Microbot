package net.runelite.client.plugins.microbot.util.walker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathConfig;
import net.runelite.client.plugins.microbot.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.microbot.shortestpath.TeleportationItem;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlannerRuntime;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationWalkRuntime;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Set;

/**
 * Microbot-owned compatibility facade over shortest-path configuration and presentation state.
 *
 * <p><b>Why this exists (Stage 2 of the facade migration — see
 * {@code shortestpath/WEBWALKER_IMPROVEMENT_PLAN.md} "Facade migration").</b>
 * Automation code ({@code Rs2Walker} and ~25 other consumers) currently reaches directly into
 * {@link ShortestPathPlugin}'s public static fields and accessors. Every time an upstream
 * (Skretzo/shortest-path) fix touches that internal wiring, the walker is at risk. Routing all
 * plugin-state access through this single class freezes the surface the walker sees, so future
 * upstream backports can change the plugin internals while only this facade (and not every
 * consumer) has to move with them.</p>
 *
 * <p><b>Contract.</b> Pathfinder task ownership lives in {@link RoutePlannerRuntime}. This facade
 * retains a read-only {@link #getPathfinder()} compatibility view and no longer exposes the
 * planner's executor, Future, mutex, or lifecycle setters. The remaining methods delegate plugin
 * configuration and presentation state. The
 * value types it returns ({@link Pathfinder}, {@link PathfinderConfig}, {@link Transport},
 * {@code TransportType}, {@code WorldPointUtil}) are treated as the stable Microbot-facing path API
 * and are deliberately <i>not</i> re-wrapped — they are pure data / pure functions.</p>
 *
 * <p><b>Migration status.</b> Phase 1 removed mutable plugin lifecycle access. Phase 2 moves legacy
 * path consumers to immutable {@code RoutePlan}/{@code NavigationSnapshot} views. The remaining
 * Pathfinder compatibility getter is removed after that cutover. Historical facade notes remain
 * in {@code shortestpath/WEBWALKER_IMPROVEMENT_PLAN.md}.</p>
 */
public final class Rs2PathApi
{
	private static volatile PathfinderConfig pathfinderConfig;
	private static volatile boolean startPointSet;
	private static volatile int reachedDistance;
	private static volatile WorldPoint lastLocation = new WorldPoint(0, 0, 0);
	private static volatile WorldMapPoint marker;

	private Rs2PathApi()
	{
	}

	/** Config group key for the shortest-path plugin ({@link ShortestPathPlugin#CONFIG_GROUP}). */
	public static final String CONFIG_GROUP = ShortestPathPlugin.CONFIG_GROUP;

	/** Shared world-map marker sprite ({@link ShortestPathPlugin#MARKER_IMAGE}). */
	public static final BufferedImage MARKER_IMAGE = ShortestPathPlugin.MARKER_IMAGE;

	// ------------------------------------------------------------------
	// Pathfinder lifecycle
	// ------------------------------------------------------------------

	/** @return the current pathfinder instance, or {@code null} if none is running. */
	public static Pathfinder getPathfinder()
	{
		return RoutePlannerRuntime.getPathfinder();
	}

	// ------------------------------------------------------------------
	// Config
	// ------------------------------------------------------------------

	/** @return the shared pathfinder configuration (transports, restrictions, toggles). */
	public static PathfinderConfig getPathfinderConfig()
	{
		return pathfinderConfig;
	}

	public static void setPathfinderConfig(PathfinderConfig nextPathfinderConfig)
	{
		pathfinderConfig = nextPathfinderConfig;
	}

	public static void configureWalker(ShortestPathConfig config)
	{
		Rs2Walker.setConfig(config);
	}

	/** Distance from the target at which the path is considered reached. */
	public static void setReachedDistance(int reachedDistance)
	{
		Rs2PathApi.reachedDistance = reachedDistance;
	}

	public static int getReachedDistance()
	{
		return reachedDistance;
	}

	public static boolean override(String configOverrideKey, boolean defaultValue)
	{
		return ShortestPathPlugin.override(configOverrideKey, defaultValue);
	}

	public static int override(String configOverrideKey, int defaultValue)
	{
		return ShortestPathPlugin.override(configOverrideKey, defaultValue);
	}

	public static TeleportationItem override(String configOverrideKey, TeleportationItem defaultValue)
	{
		return ShortestPathPlugin.override(configOverrideKey, defaultValue);
	}

	// ------------------------------------------------------------------
	// Target / walker state
	// ------------------------------------------------------------------

	/** Clears the active target and tears down the current path. */
	public static void exit()
	{
		NavigationWalkRuntime.cancel("path-api:exit");
	}

	public static boolean isStartPointSet()
	{
		return startPointSet;
	}

	public static void setStartPointSet(boolean startPointSet)
	{
		Rs2PathApi.startPointSet = startPointSet;
	}

	/** Records the player's last known world location (write-only on the plugin). */
	public static void setLastLocation(WorldPoint lastLocation)
	{
		Rs2PathApi.lastLocation = lastLocation;
	}

	public static WorldPoint getLastLocation()
	{
		return lastLocation;
	}

	// ------------------------------------------------------------------
	// Marker (world-map overlay)
	// ------------------------------------------------------------------

	public static WorldMapPoint getMarker()
	{
		return marker;
	}

	public static void setMarker(WorldMapPoint marker)
	{
		Rs2PathApi.marker = marker;
	}

	// ------------------------------------------------------------------
	// Transport data
	// ------------------------------------------------------------------

	/** @return the transport graph keyed by origin tile. */
	public static Map<WorldPoint, Set<Transport>> getTransports()
	{
		PathfinderConfig config = pathfinderConfig;
		return config == null ? Map.of() : config.getTransports();
	}
}
