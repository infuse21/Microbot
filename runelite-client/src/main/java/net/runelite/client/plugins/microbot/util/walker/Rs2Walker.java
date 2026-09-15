package net.runelite.client.plugins.microbot.util.walker;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.annotations.Component;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.devtools.MovementFlag;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.*;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.CollisionMap;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.coords.Rs2LocalPoint;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldArea;
import net.runelite.client.plugins.microbot.util.coords.Rs2WorldPoint;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.magic.Rs2Spells;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2Pvp;
import net.runelite.client.plugins.microbot.util.leaguetransport.Rs2LeaguesTransport;
import net.runelite.client.plugins.microbot.util.leaguetransport.SeasonalTransportHandler;
import net.runelite.client.plugins.microbot.util.leaguetransport.SeasonalTransportHandlers;
import net.runelite.client.plugins.microbot.util.logging.Rs2LogRateLimit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.slf4j.event.Level;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.leaguetransport.LeaguesRegion;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorClassifier;
import net.runelite.client.plugins.microbot.util.walker.door.DoorProbeContext;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorDetection;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorProbe;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorAheadResolver;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorGeometry;
import net.runelite.client.plugins.microbot.util.walker.door.OrdinaryDoorRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorScene;
import net.runelite.client.plugins.microbot.util.walker.door.DoorInteractionOwnership;
import net.runelite.client.plugins.microbot.util.walker.door.model.OrdinaryDoor;
import net.runelite.client.plugins.microbot.util.walker.geometry.WalkerPathGeometry;
import net.runelite.client.plugins.microbot.util.walker.obstacle.MineableResolver;
import net.runelite.client.plugins.microbot.util.walker.obstacle.MineableRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.obstacle.ObstacleResolution;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2LiveScene;
import net.runelite.client.plugins.microbot.util.walker.recovery.RouteRecovery;
import net.runelite.client.plugins.microbot.util.walker.state.WalkerRouteState;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2DoorHandler;
import net.runelite.client.plugins.microbot.util.walker.door.Rs2WalkerAwaits;
import net.runelite.client.plugins.microbot.util.walker.door.model.AwaitTicket;
import net.runelite.client.plugins.microbot.util.walker.door.model.DoorResolution;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import net.runelite.client.plugins.microbot.util.walker.banking.BankedTransportCoordinator;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2SpellEquipmentScene;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentObservation;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentPreparation;
import net.runelite.client.plugins.microbot.util.walker.awaits.Rs2WalkerRuntimeAwaits;
import net.runelite.client.plugins.microbot.util.walker.puzzles.DraynorBasementSolver;
import net.runelite.client.plugins.microbot.util.walker.stall.Rs2WalkerStallPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2WalkerTransportAwaits;
import net.runelite.client.plugins.microbot.util.walker.transport.AdjacentTransportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.CatalogTransitionRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.CharterShipPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CharterShipRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.FairyRingPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.FairyRingRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.SpiritTreePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.SpiritTreeRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.GnomeGliderPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.GnomeGliderRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.QuetzalPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.QuetzalRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.TeleportationLeverPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.TeleportationLeverRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.WildernessDitchPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.WildernessDitchRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.JungleObstacleRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.CanoePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.CanoeRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.MinecartRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.MinigameTeleportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.MagicMushtreePolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.MagicMushtreeRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.HotAirBalloonPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.HotAirBalloonRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2AdjacentTransportScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2CatalogTransitionScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2CharterShipScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2FairyRingScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2SpiritTreeScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2GnomeGliderScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2QuetzalScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2TeleportationLeverScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2WildernessDitchScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2JungleObstacleScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2CanoeScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2MinecartScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2TeleportationPortalScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2MinigameTeleportScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2MagicMushtreeScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2HotAirBalloonScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2SimpleTeleportScene;
import net.runelite.client.plugins.microbot.util.walker.transport.SimpleTeleportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.SimpleTeleportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.ItemTeleportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2ItemTeleportScene;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcDialogueTransportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcDialogueTransportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcTransportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcTransportRouteScanner;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2NpcDialogueTransportScene;
import net.runelite.client.plugins.microbot.util.walker.transport.Rs2NpcTransportScene;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;
import net.runelite.client.plugins.microbot.util.walker.lifecycle.Rs2WalkerLifecycleRuntime;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationDecision;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationEngineRuntime;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationExecutionResult;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationObservation;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationSnapshot;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationWalkCoordinator;
import net.runelite.client.plugins.microbot.util.walker.navigation.RecoveryCause;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlannerRuntime;
import net.runelite.client.plugins.microbot.util.walker.navigation.WalkerActions;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import javax.inject.Named;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.runelite.client.plugins.microbot.util.Global.*;

/**
 * TODO:
 * 1. fix teleports starting from inside the POH
 * <p>
 * Seasonal handlers ({@link Rs2LeaguesTransport#tryHandleLeaguesAreaTransport}, MoA) must not run on the client thread — same contract as {@link Rs2LeaguesTransport#leaguesTeleport}.
 */
@Slf4j
public class Rs2Walker {
    @Setter
    public static ShortestPathConfig config;
    // stuck/movement tracking state migrated to WalkerRouteState (see routeState)
    static volatile WorldPoint currentTarget;
    static int nextWalkingDistance = 10;

    /**
     * Active Microbot walk destination ({@code null} when no scripted walk). ShortestPath overlay
     * must not clear {@link Rs2PathApi#getPathfinder()} while this is non-null, because the active
     * NavigationEngine request still depends on the published route calculation.
     */
    public static WorldPoint getCurrentTarget() {
        return currentTarget;
    }

	/**
	 * Sticky interim minimap click target to avoid destination flapping when the minimap flag
	 * disappears around bends. Once we click a reachable point, keep it until we get close
	 * (<= {@link #INTERIM_CLOSE_TILES}) or progress stalls for {@link #INTERIM_PROGRESS_TIMEOUT_MS}.
	 */
	// interim-target state migrated to WalkerRouteState (see routeState)

	private static final int INTERIM_CLOSE_TILES = 5;

	/**
	 * Floor for the jittered per-click route reach. Deliberately above {@link #INTERIM_CLOSE_TILES}
	 * so a short click cannot land inside the interim-close threshold, which would clear the
	 * checkpoint immediately and cause click thrash.
	 */
	private static final int ROUTE_CLICK_REACH_MIN_TILES = 7;
	private static final int INTERIM_PRECLICK_TILES = 6;
	private static final int INTERIM_RUN_PRECLICK_TILES = 8;
	private static final long INTERIM_PROGRESS_TIMEOUT_MS = 2500L;
	/**
	 * How much further than its closest approach the player may get from an interim checkpoint before
	 * it counts as abandoned. Wide enough to tolerate rounding a wall or a corner on the way to it.
	 */
	private static final int INTERIM_ABANDON_MARGIN_TILES = 4;
	private static final long INTERIM_MAX_AGE_MS = 10_000L;
    private static final long OFF_PATH_RECALC_RECENT_MOVEMENT_MS = 2_000L;
    private static final long OFF_PATH_RECALC_ROUTE_PROGRESS_GRACE_MS = 3_500L;
    private static final long OFF_PATH_RECALC_MINIMAP_CLICK_GRACE_MS = 2_500L;
    // Busy state (moving/animating/interacting) only defers the off-path recalc and only
    // preempts recovery clicks while the walker plausibly CAUSED it — within this window of
    // the walker's last issued click or interaction. A single minimap click moves the player
    // for at most ~8s (13 tiles, walking), so anything past 10s is external movement: combat
    // retaliation, aggro pathing, another script. Unbounded "moving" deferral let ogre combat
    // drag a player a tile per second for 27s while the walker stayed fully passive.
    private static final int OFF_PATH_RECALC_DEFER_WAIT_MIN_MS = 250;
    private static final int OFF_PATH_RECALC_DEFER_WAIT_MAX_MS = 1_200;
    private static final long RAW_SCAN_DOOR_FOCUS_MAX_MS = 2200L;
    private static final int RAW_SCAN_DOOR_FOCUS_MAX_ATTEMPTS = 3;
    private static final long DOOR_POST_INTERACT_SETTLE_MS = 900L;
    /** Longest the walker will hold off re-clicking a door while an unanswered option menu is up. */
    private static final long DOOR_DIALOGUE_DEFER_MAX_MS = 5_000L;
    /** Above this, a single transport object scan is worth naming in the log. */
    /** Furthest a door may be and still be opened while the player is mid-walk toward it. */
    private static final int DOOR_APPROACH_INTERACT_MAX_TILES = 4;
    private static final long RECOVERY_MOVEMENT_IN_FLIGHT_MS = 3_500L;
    private static final int POST_DOOR_FAST_CLICK_MAX_EUCLIDEAN = 13;
    private static final int POST_DOOR_EDGE_NUDGE_MAX_FROM_PLAYER = 3;
    private static final int POST_DOOR_EDGE_NUDGE_WAIT_MS = 1200;
    private static final int HANDLER_RANGE = 13;
	private static final int INTERACTION_CHAIN_RANGE = 25;
    // Raw/smoothed segments can span several walkable tiles before their transport edge.
    // Do not let the transport handler turn that future edge into a long movement command:
    // normal route clicks own the approach, then the handler takes over beside the origin.
    private static final int SEGMENT_DOOR_FAMILY_MARK_RADIUS = 2;
    /**
     * A spatially-near smoothed waypoint can be hundreds of raw route steps ahead when a route
     * doubles back around a mountain or fence. Do not treat that future branch as the immediate
     * local blocker. The bounded reachability sample is roughly 39 tiles, so 48 preserves ordinary
     * false-negative recovery while rejecting distant route folds.
     */
    private static final int NORMAL_MINIMAP_REACH_EUCLIDEAN = 11;
    // UNREACHABLE_RECOVERY_FORWARD_SCAN_TILES moved into recovery/RouteRecovery (P1)
    /**
     * Stationary window before an active route issues a recovery nudge.
     * <p>
     * {@link #tryIssueRouteContinuationClick} is one-shot: it only runs on the pass where the interim
     * checkpoint is cleared. If a guard (interacting/animating/door or transport settling, or a
     * pending route object) blocks it on that single pass, nothing retries and the route only resumes
     * via this nudge — so this window is the visible dead stop between hops. Keep it to a few ticks
     * (movement.md #13) rather than the old 2500ms. The nudge already requires the player to be
     * genuinely still (not moving/animating/interacting) on the same tile, and
     * {@link #ACTIVE_ROUTE_IDLE_NUDGE_COOLDOWN_MS} still prevents click spam.
     */
	private static final long ACTIVE_ROUTE_IDLE_NUDGE_COOLDOWN_MS = 2_000L;
    /**
     * How long after a door-recovery-suppressed tick the idle nudge stays disabled. Rolling — the suppress
     * branch re-stamps it every tick the door stays unresolved, so the nudge is held off for the whole
     * suppression episode plus this tail. Long enough to cover the door cooldowns that cause suppression;
     * short enough that a genuinely abandoned door (player walked away, route replanned) frees the nudge.
     */
	/** Floor for the post-plane-change settle sleep, so an unbounded Gaussian draw cannot go negative. */
	private static final int ROUTE_PROGRESS_FORWARD_SEARCH_TILES = 40;

	/**
	 * How long to wait for {@code Rs2PathApi.getPathfinder()} to become non-null at route start.
	 * <p>
	 * The pathfinder is only published <em>after</em> {@code PathfinderConfig.refresh()} completes
	 * (see {@code Rs2WalkerLifecycleRuntime.restartPathfinding}), and a cache-missing refresh has been
	 * measured at 2.4-2.7s. The previous 2000ms cap expired mid-refresh, sending the walker into
	 * {@code recalculatePath()} — which cancels the in-flight work and starts a second refresh, making
	 * the cold start worse instead of recovering from it. Wait comfortably past the observed refresh
	 * cost so recalculation stays a genuine failure path.
	 */
	private static final int PATHFINDER_NULL_WAIT_MS = 6_000;
    private static final long TRANSPORT_POST_INTERACT_SETTLE_MS = 900L;
    private static final long RECENT_TRANSPORT_EDGE_SUPPRESS_MS = 8_000L;
    // door-interaction state migrated to WalkerRouteState (see routeState)
    /**
     * Minimum settle after a door/transport interaction before the early exit may fire: one game tick of
     * post-action state flux (position sync, object state update). The 900ms constants above remain the
     * CEILING for when the early-exit signal never confirms — previously they were the fixed cost of every
     * single door and transport, because the transport early-exit compared against where the player stood
     * when the transport was marked handled (always true while standing at the destination) and the door
     * settle had no early exit at all.
     */
    private static final long POST_INTERACT_SETTLE_MIN_MS = 300L;
    // misc route-timer state migrated to WalkerRouteState (see routeState)
    /**
     * Consolidated route state (P1 walker decomposition, enabling step). Fields are migrated here in
     * cohesive clusters; first cluster: transport handoff. See {@link WalkerRouteState}.
     */
    private static final WalkerRouteState routeState = new WalkerRouteState();
    // idle-nudge state migrated to WalkerRouteState (see routeState)
    // route-progress state migrated to WalkerRouteState (see routeState)
    private static final Set<String> startupPhasesLogged = ConcurrentHashMap.newKeySet();
    /**
     * Max Chebyshev "radius" for Quetzal / near-destination checks — guards use {@code distanceTo2D &lt; OFFSET}.
     * {@link WorldPoint#distanceTo(WorldPoint)} delegates to {@link WorldPoint#distanceTo2D(WorldPoint)} when both
     * points share a plane, so mixed {@code distanceTo}/{@code distanceTo2D} call sites agree for walking goals.
     * If planes differ, {@code distanceTo} returns {@link Integer#MAX_VALUE} (not {@code distanceTo2D}) — do not use
     * for cross-plane teleport semantics without an explicit plane check.
     * Integer Chebyshev distance: {@code &lt; OFFSET} is the same as {@code &lt;= OFFSET - 1}.
     *
     * @see WorldPoint#distanceTo(WorldPoint)
     */
    static final int OFFSET = 10;

    /** Post-travel poll/timeout for Spirit Tree, Quetzal, glider, fairy ring, and other same-plane landing waits. */
    /** Ship / charter / glider — landing predicate uses {@link #isPlayerWithinChebyshevOf} with this exclusive bound. */
    /** Max wait after ship/NPC/boat dialogue until near destination (must match {@link #sleepUntil} timeout + warn text). */
    private static final int SHIP_NPC_BOAT_LANDING_WAIT_MS = 10_000;

    /** After scene-object transport {@link #handleObject} — landing poll timeout + matching warn (cf. {@link #SHIP_NPC_BOAT_LANDING_WAIT_MS}). */
    /** Teleport “already near destination” skip in path loop — same semantics as prior {@code distanceTo2D &lt; 3}. */
    /**
     * When the last walkable path tile is within this Chebyshev distance of the goal, treat the leg as a
     * "short interior" finish (e.g. door → small room): cap {@link #tightFinishThreshold} so we do not
     * return {@link WalkerState#ARRIVED} while still outside the building.
     */
    private static final int TIGHT_PATH_GOAL_GAP = 4;

    // Set this to true, if you want to calculate the path but do not want to walk to it
    static boolean debug = false;

    /** Bounds one blocking compatibility call without exposing a second executor lifecycle. */
    private static final int MAX_NAVIGATION_ENGINE_PASSES = 256;

    /**
     * Verbose walker traces — enable DEBUG logging for {@code net.runelite.client.plugins.microbot}.
     * Uses {@link Microbot#log(Level, String, Object...)} so levels route consistently.
     */
    private static void walkerDiag(String format, Object... args) {
        Microbot.log(Level.DEBUG, "[WalkerDiag] " + format, args);
    }

    /**
     * Compact {@code x,y,p} for logs (world API coords). Similar comma coords exist in test harnesses — keep here until
     * a shared microbot util is justified.
     */
    private static String compactWorldPoint(WorldPoint wp) {
        if (wp == null) {
            return "?";
        }
        return wp.getX() + "," + wp.getY() + ",p" + wp.getPlane();
    }

    private static void markWalkSessionStart(WorldPoint target) {
        routeState.walkSessionStartedAtMs = System.currentTimeMillis();
        routeState.firstMovementClickMarked = false;
        startupPhasesLogged.clear();
        routeState.lastTransportHandledAtLocation = null;
        routeState.lastTransportOriginLocation = null;
        routeState.lastTransportDestinationLocation = null;
        // The interim target belongs to the PREVIOUS route's click; letting it survive into a fresh walk
        // makes the new walk yield to (and report progress against) a stale objective — repeatedly seen as
        // interim=<old goal> camping at Clock Tower when the script restarts walks every ~40s.
        clearInterimTarget("walk-start");
        resetRouteProgress();
        WebWalkLog.tmark("walk_start", 0, target, Rs2Player.getWorldLocation(), "target_set");
    }

    private static void clearRecentTransportContext() {
        routeState.lastTransportHandledAtMs = 0L;
        routeState.lastTransportHandledAtLocation = null;
        routeState.lastTransportOriginLocation = null;
        routeState.lastTransportDestinationLocation = null;
    }

    private static void markFirstMovementClick(String phase, WorldPoint target, WorldPoint at, String detail) {
        if (routeState.firstMovementClickMarked) {
            return;
        }
        long startedAt = routeState.walkSessionStartedAtMs;
        if (startedAt <= 0) {
            return;
        }
        routeState.firstMovementClickMarked = true;
        WebWalkLog.tmark(phase, System.currentTimeMillis() - startedAt, target, at, detail);
    }

    private enum WalkerPhase {
        STARTUP,
        STEADY
    }

    private static final class WalkLoopSnapshot {
        private final WorldPoint playerLoc;
        private final HashMap<WorldPoint, Integer> closestReachableTiles;

        private WalkLoopSnapshot(WorldPoint playerLoc) {
            this.playerLoc = playerLoc;
            this.closestReachableTiles = getClosestIndexReachableTiles(playerLoc);
        }

        private int closestTileIndex(List<WorldPoint> path) {
            return WalkerPathGeometry.getClosestTileIndex(path, playerLoc, closestReachableTiles);
        }
    }

    private interface ObstaclePolicy {
        long segmentDoorTimeoutMs();
        long unreachableDoorTimeoutMs();
        int edgeResolutionWaitTimeoutMs();
        long pathAdjacentProbeTimeoutMs();
        boolean allowBroadRawHandlers();
        boolean allowPathAdjacentProbe();
        boolean allowNearbyFallback();
    }

    private static final class StartupObstaclePolicy implements ObstaclePolicy {
        @Override
        public long segmentDoorTimeoutMs() {
            return 800L;
        }

        @Override
        public long unreachableDoorTimeoutMs() {
            return 800L;
        }

        @Override
        public int edgeResolutionWaitTimeoutMs() {
            return 700;
        }

        @Override
        public long pathAdjacentProbeTimeoutMs() {
            return 700L;
        }

        @Override
        public boolean allowBroadRawHandlers() {
            return false;
        }

        @Override
        public boolean allowPathAdjacentProbe() {
            return false;
        }

        @Override
        public boolean allowNearbyFallback() {
            return false;
        }
    }

    private static final class SteadyObstaclePolicy implements ObstaclePolicy {
        @Override
        public long segmentDoorTimeoutMs() {
            return 1500L;
        }

        @Override
        public long unreachableDoorTimeoutMs() {
            return 1500L;
        }

        @Override
        public int edgeResolutionWaitTimeoutMs() {
            return 1800;
        }

        @Override
        public long pathAdjacentProbeTimeoutMs() {
            return 1500L;
        }

        @Override
        public boolean allowBroadRawHandlers() {
            return true;
        }

        @Override
        public boolean allowPathAdjacentProbe() {
            return true;
        }

        @Override
        public boolean allowNearbyFallback() {
            return true;
        }
    }

    private static boolean isClientThread() {
        Client client = Microbot.getClient();
        return client != null && client.isClientThread();
    }

    private static int reachedDistanceOrDefault() {
        return config != null ? config.reachedDistance() : 10;
    }

    static boolean shouldSkipStartupPreclickSegmentHandlers(boolean startupBeforeFirstClick,
                                                            int segmentIdx,
                                                            int routeStartIdx,
                                                            boolean recentDoorAttemptNearSegment,
                                                            boolean doorSettling,
                                                            boolean recoveryInFlight) {
        if (!startupBeforeFirstClick || routeStartIdx < 0 || segmentIdx < routeStartIdx) {
            return false;
        }
        if (recentDoorAttemptNearSegment || doorSettling || recoveryInFlight) {
            return false;
        }
        return true;
    }

    static boolean shouldRunActiveRouteIdleNudge(boolean idleNudgeDue,
                                                boolean immediateRouteTransportPending) {
        return idleNudgeDue && !immediateRouteTransportPending;
    }

    /**
     * Caps configured finish distance when the route already ends very close to the marked goal.
     * Without this, a large "Finish distance" (e.g. 5) allows {@link WalkerState#ARRIVED} on the
     * wrong side of a wall/door for small interiors. When {@code dLast &lt; TIGHT_PATH_GOAL_GAP}, cap is {@code 1};
     * when {@code dLast == TIGHT_PATH_GOAL_GAP}, cap is {@code 2} (outdoor micro-walking relief at the gap radius).
     */
    private static int tightFinishThreshold(WorldPoint goal, WorldPoint pathLastWalkable, int configuredChebyshev) {
        int cfg = Math.max(0, configuredChebyshev);
        if (goal == null || pathLastWalkable == null) {
            return cfg;
        }
        if (goal.getPlane() != pathLastWalkable.getPlane()) {
            return cfg;
        }
        int dLast = pathLastWalkable.distanceTo2D(goal);
        if (dLast <= TIGHT_PATH_GOAL_GAP) {
            if (dLast < TIGHT_PATH_GOAL_GAP) {
                return Math.min(cfg, 1);
            }
            return Math.min(cfg, 2);
        }
        return cfg;
    }

    /**
     * After opening a door, if the walk goal is still close, scene-click a random walkable tile near the
     * goal so the next movement is not an immediate minimap path segment (less robotic than
     * door → minimap in the same beat).
     */
    /**
     * Hold-off when the door opened but no canvas nudge was issued (the mid-route case). Long enough that
     * the next beat is not on the door interaction's own tick, short enough that the walk does not stall
     * waiting for a scene click that was never made.
     */
    /** Max wait after scene canvas / recovery clicks until movement stops (avoids minimap churn while in-flight). */
    /** If phase 1 exits on arrival distance while still moving, wait briefly for idle-only (reduces tail churn). */
    private static final int POST_SCENE_WALK_IDLE_SECOND_PHASE_MS_MAX = 4_000;
    private static void waitUntilIdleAfterSceneWalk(WorldPoint cancelGoal, int timeoutMs) {
        waitUntilIdleAfterSceneWalk(cancelGoal, timeoutMs, null, 0);
    }

    /**
     * Waits until idle, walk cancel, or player within {@code arrivalMaxChebyshev} Chebyshev steps of
     * {@code arrivalGoal} (same plane; see {@link WorldPoint#distanceTo2D(WorldPoint)}) — avoids burning full
     * timeout when {@code Rs2Player#isMoving()} lies during animations. Arrival uses an <em>inclusive</em> bound:
     * {@code distanceTo2D(arrivalGoal) <= arrivalMaxChebyshev} (unlike {@link #OFFSET}-style guards that use
     * {@code distanceTo2D &lt; OFFSET}). If arrival distance triggers while still
     * moving, runs a short second phase idle-only wait. Phase 2 does not run when phase 1 ends only due to the
     * outer timeout while still far from {@code arrivalGoal} (by design).
     */
    private static void waitUntilIdleAfterSceneWalk(WorldPoint cancelGoal, int timeoutMs,
            WorldPoint arrivalGoal, int arrivalMaxChebyshev) {
        assert cancelGoal != null;
        assert timeoutMs > 0;
        sleepUntil(() -> {
            if (isWalkCancelled(cancelGoal)) {
                return true;
            }
            WorldPoint pl = Rs2Player.getWorldLocation();
            if (arrivalGoal != null && arrivalMaxChebyshev >= 0 && pl != null
                    && arrivalGoal.getPlane() == pl.getPlane()
                    && pl.distanceTo2D(arrivalGoal) <= arrivalMaxChebyshev) {
                return true;
            }
            return !Rs2Player.isMoving();
        }, timeoutMs);
        // Sample player once after phase 1 — rare tick skew vs isMoving(); phase 2 only refines idle after arrival exit.
        WorldPoint plAfter = Rs2Player.getWorldLocation();
        boolean withinArrival = arrivalGoal != null && arrivalMaxChebyshev >= 0 && plAfter != null
                && arrivalGoal.getPlane() == plAfter.getPlane()
                && plAfter.distanceTo2D(arrivalGoal) <= arrivalMaxChebyshev;
        if (withinArrival && Rs2Player.isMoving()) {
            sleepUntil(() -> isWalkCancelled(cancelGoal) || !Rs2Player.isMoving(),
                    POST_SCENE_WALK_IDLE_SECOND_PHASE_MS_MAX);
        }
    }

    /**
     * Whether any tile within {@code distance} of {@code target} is walkable in the collision map.
     *
     * <p>Pre-flight guard so a destination that does not exist as walkable terrain fails
     * immediately instead of after a full route. A walk to {@code (3087,9720)} — inside the rock
     * east of the Dwarven Mine, with 0 walkable tiles within 6 — spent ~80s covering 100+ tiles to
     * the nearest reachable tile and then reported {@code partial-retries-exhausted}, which reads
     * as a walker fault rather than a bad coordinate.
     *
     * <p><b>Permissive by design.</b> An unmapped region reads as fully blocked (see
     * {@link CollisionMap#hasRegion}), so every ambiguous case returns {@code true} and lets the
     * pathfinder decide. Only a target sitting in mapped, wholly blocked terrain is rejected —
     * otherwise this would refuse instances and any region missing from the collision map.
     */
    /**
     * Whether the pathfinder's collision map considers this tile standable.
     *
     * <p>Anyone <em>choosing</em> a destination should filter candidates through this. {@link #walkTo}
     * pre-flights the target and rejects it outright when nothing walkable lies within the arrival
     * distance, so picking an unwalkable tile fails the entire walk rather than degrading to something
     * close by. The scene's own notion of walkability and this map do not always agree — the Corsair
     * Cove staircase approach at (2531,2834) reads walkable in the scene and blocked here.
     */
    public static boolean isWalkableInCollisionMap(WorldPoint tile) {
        PathfinderConfig config = Rs2PathApi.getPathfinderConfig();
        return hasWalkableTileWithin(config != null ? config.getMap() : null, tile, 0);
    }

    static boolean hasWalkableTileWithin(CollisionMap map, WorldPoint target, int distance) {
        if (map == null || target == null) {
            return true;
        }
        if (!map.hasRegion(target.getX(), target.getY())) {
            return true;
        }
        int radius = Math.max(0, distance);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int x = target.getX() + dx;
                int y = target.getY() + dy;
                if (!map.hasRegion(x, y)) {
                    return true;
                }
                if (!map.isBlocked(x, y, target.getPlane())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Nearest walkable tile to {@code target} within {@code maxRadius}, or null. Diagnostics only. */
    static WorldPoint nearestWalkableTile(CollisionMap map, WorldPoint target, int maxRadius) {
        if (map == null || target == null) {
            return null;
        }
        for (int r = 1; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) {
                        continue; // ring only; inner rings already scanned
                    }
                    int x = target.getX() + dx;
                    int y = target.getY() + dy;
                    if (map.hasRegion(x, y) && !map.isBlocked(x, y, target.getPlane())) {
                        return new WorldPoint(x, y, target.getPlane());
                    }
                }
            }
        }
        return null;
    }

    /** Door / gate from main path loop vs {@link #handleNearbyRawPathSceneObjects} raw-path scan (same nudge UX). */
    /**
     * Exit reasons meaning the path loop ended because the walker <em>did</em> something that
     * advances the route — opened a door, took a transport, cleared a blocker — or because
     * movement is already in flight. These are progress, not a failed attempt.
     *
     * <p>The partial-retry budget exists for "the goal is unreachable and we are stuck". Spending
     * it on these instead conflated the two: a door open ends the iteration, lands in the partial
     * branch, and burns a retry even though the walker just made progress. On a route whose path
     * end is permanently short of the goal (any partial path), the budget is armed for the whole
     * walk, so an ordinary door could exhaust it ~100 tiles into a working route and report
     * UNREACHABLE while the player was still advancing. See {@code movement.md} #25.
     */
    static boolean isRouteProgressExit(String exitReason) {
        if (exitReason == null) {
            return false;
        }
        if (exitReason.startsWith("door-handled")) {
            return true;
        }
        switch (exitReason) {
            case "raw-path-scene-object-handled":
            case "post-click-raw-path-scene-object-handled":
            case "current-tile-transport-handled":
            case "post-click-current-tile-transport-handled":
            case "transport-handled":
            case "rockfall-handled":
            case "path-blocker-handled":
            case "interim-in-flight":
            case "recovery-move-in-flight":
            case "route-fold-continuation-click":
                return true;
            default:
                return false;
        }
    }

    /** @return true only when a canvas click was actually issued, so the caller can size its minimap hold-off. */
    /**
     * Clears walker goal and ShortestPath artifacts. Prefer over {@code setTarget(null)} so logs show why.
     */
    public static void clearWalkingRoute(String reason) {
        setTarget(null, reason != null && !reason.isBlank() ? reason : "unspecified");
    }

    // lastRouteClearReason / lastRouteClearAtMs migrated to WalkerRouteState; the @Getter-generated
    // accessors are preserved explicitly here because ShortestPathScript reads them as public API.
    public static String getLastRouteClearReason() {
        return routeState.lastRouteClearReason;
    }

    public static long getLastRouteClearAtMs() {
        return routeState.lastRouteClearAtMs;
    }

    private static void logRouteClear(String reason) {
        routeState.lastRouteClearReason = reason == null ? "" : reason;
        routeState.lastRouteClearAtMs = System.currentTimeMillis();
        if (reason == null || reason.isBlank()) {
            WebWalkLog.routeClearMissingReason(Thread.currentThread().getName());
        } else {
            WebWalkLog.routeClear(reason);
        }
    }

    /** Substrings for game-object names treated like doors (pathing heuristics). */

    /** Max age for {@link Rs2LeaguesTransport#isLeaguesAreaTeleportPending(long)} in stall / stuck gates. */
    @Named("disableWalkerUpdate")
    static boolean disableWalkerUpdate;

    public static boolean disableTeleports = false;

    // Serializes stateful walker entry points so concurrent scripts don't corrupt
    // routeState.stuckCount / routeState.lastPosition / routeState.lastMovedTimeMs / currentTarget / nextWalkingDistance.
    // Reentrant: same-thread dispatch (walkWithState -> walkWithBankedTransportsAndState
    // -> walkWithStateInternal -> nested NavigationEngine walk) reacquires freely.
    // setTarget() stays unlocked — cross-thread cancel; volatile currentTarget read in the loop
    // can still see null only when setTarget(null) is intended. recalculatePath no longer nulls
    // currentTarget between restarts (avoids false cancel during sleepUntil).
    private static final ReentrantLock walkerLock = new ReentrantLock();
    /**
     * Optional completion rule owned by the thread currently executing {@link #walkUntil}.
     *
     * <p>The walker is globally serialized, but a thread-local keeps nested helper walks from
     * inheriting a rule intended for a different target. Existing walk methods never install a
     * context and therefore retain their exact behaviour.</p>
     */
    private static final ThreadLocal<WalkCompletionContext> walkCompletionContext = new ThreadLocal<>();

    private static final class WalkCompletionContext {
        private final WorldPoint target;
        private final BooleanSupplier condition;
        private boolean met;
        private boolean failed;

        private WalkCompletionContext(WorldPoint target, BooleanSupplier condition) {
            this.target = target;
            this.condition = condition;
        }
    }

    /**
     * One-shot DEBUG when {@link WorldMapPointManager} is null during route clear (shutdown race).
     * Later races same JVM stay silent — intentional noise cap.
     */
    private static final AtomicBoolean WORLD_MAP_REMOVE_NULL_LOGGED = new AtomicBoolean();

    /** Same package (e.g. unit tests) only — not part of script API. */
    static void clearWalkerDedupeForTesting()
    {
        WORLD_MAP_REMOVE_NULL_LOGGED.set(false);
        clearRecentTransportContext();
        resetRouteProgress();
    }

    private static volatile List<SeasonalTransportHandler> seasonalTransportHandlers =
            SeasonalTransportHandlers.defaultHandlerList();

    /**
     * Replaces the seasonal transport handler chain. Non-null, non-empty list; pass
     * {@link SeasonalTransportHandlers#defaultHandlerList()} to restore built-ins.
     * {@link net.runelite.client.plugins.microbot.MicrobotPlugin#startUp} resets defaults each session.
     */
    public static void setSeasonalTransportHandlers(List<SeasonalTransportHandler> handlers)
    {
        if (handlers == null || handlers.isEmpty())
        {
            seasonalTransportHandlers = SeasonalTransportHandlers.defaultHandlerList();
        }
        else
        {
            seasonalTransportHandlers = List.copyOf(handlers);
        }
    }

    public static List<SeasonalTransportHandler> getSeasonalTransportHandlers()
    {
        return seasonalTransportHandlers;
    }

    /**
     * Externally observable counters for walker health checks. The benchmark probe
     * (or any diagnostic script) reads these to decide whether a walk completed
     * without a stall-triggered or off-path-triggered recalculation mid-walk.
     */
    public static final class Telemetry {
        public static final AtomicInteger offPathRecalcCount = new AtomicInteger();
        public static final AtomicInteger offPathRecalcDeferredCount = new AtomicInteger();
        public static final AtomicInteger stallRecalcCount = new AtomicInteger();
        public static final AtomicInteger partialRetryCount = new AtomicInteger();
        public static final AtomicInteger unreachableCount = new AtomicInteger();
        /** Locked-region chat attributed to a recent transport attempt and blacklisted. */
        public static final AtomicInteger leaguesLockAttributedCount = new AtomicInteger();
        /** Locked-region chat with no matching recent attempt or expired attempt snapshot. */
        public static final AtomicInteger leaguesLockStaleCount = new AtomicInteger();
        /** Locked-region chat where region text did not map to {@link LeaguesRegion} (dest-only blacklist path). */
        public static final AtomicInteger leaguesLockParseMissCount = new AtomicInteger();
        public static final AtomicLong lastEventAtMs = new AtomicLong();
        public static volatile String lastReason = "";

        private static final ConcurrentHashMap<String, AtomicInteger> doorRejectByCause = new ConcurrentHashMap<>();
        private static final AtomicInteger doorRejectSummaryLogSeq = new AtomicInteger(0);
        private static final int DOOR_REJECT_SUMMARY_LOG_INTERVAL = 40;
        private static final ConcurrentHashMap<String, AtomicInteger> offPathDeferredByReason = new ConcurrentHashMap<>();
        private static final AtomicInteger offPathDeferredSummaryLogSeq = new AtomicInteger(0);
        private static final int OFF_PATH_DEFERRED_SUMMARY_LOG_INTERVAL = 20;

        /**
         * Rate-limited debug summary of {@link #doorRejectByCause} tallies (noise control on tight door clusters).
         */
        public static void recordDoorReject(String cause) {
            if (cause == null || cause.isEmpty()) {
                cause = "unknown";
            }
            doorRejectByCause.computeIfAbsent(cause, k -> new AtomicInteger()).incrementAndGet();
            if (Rs2LogRateLimit.everyN(doorRejectSummaryLogSeq, DOOR_REJECT_SUMMARY_LOG_INTERVAL)
                    && log.isDebugEnabled()) {
                log.debug("[WalkerTelemetry] DOOR_REJECT summary={}", doorRejectByCause);
            }
        }

        public static void incrementLeaguesLockAttributed() {
            leaguesLockAttributedCount.incrementAndGet();
        }

        public static void incrementLeaguesLockStale() {
            leaguesLockStaleCount.incrementAndGet();
        }

        public static void incrementLeaguesLockParseMiss() {
            leaguesLockParseMissCount.incrementAndGet();
        }

        public static void recordOffPathRecalc(WorldPoint playerPos, int pathSize) {
            offPathRecalcCount.incrementAndGet();
            lastReason = "off-path";
            lastEventAtMs.set(System.currentTimeMillis());
            log.info("[WalkerTelemetry] OFFPATH_RECALC player={} pathSize={} totalOffPath={} totalStall={}",
                    playerPos, pathSize, offPathRecalcCount.get(), stallRecalcCount.get());
        }

        public static void recordOffPathRecalcDeferred(String reason, WorldPoint playerPos,
                                                       WorldPoint target, int pathSize) {
            if (reason == null || reason.isEmpty()) {
                reason = "unknown";
            }
            offPathRecalcDeferredCount.incrementAndGet();
            offPathDeferredByReason.computeIfAbsent(reason, k -> new AtomicInteger()).incrementAndGet();
            lastReason = "off-path-deferred:" + reason;
            lastEventAtMs.set(System.currentTimeMillis());
            if (Rs2LogRateLimit.everyN(offPathDeferredSummaryLogSeq, OFF_PATH_DEFERRED_SUMMARY_LOG_INTERVAL)
                    && log.isDebugEnabled()) {
                log.debug("[WalkerTelemetry] OFFPATH_RECALC_DEFERRED player={} target={} pathSize={} summary={}",
                        playerPos, target, pathSize, offPathDeferredByReason);
            }
        }

        public static void recordStallRecalc(long sinceMovedMs, WorldPoint playerPos) {
            stallRecalcCount.incrementAndGet();
            lastReason = "stall";
            lastEventAtMs.set(System.currentTimeMillis());
            log.info("[WalkerTelemetry] STALL_RECALC sinceMoved={}ms player={} totalStall={} totalOffPath={}",
                    sinceMovedMs, playerPos, stallRecalcCount.get(), offPathRecalcCount.get());
        }

        public static void recordPartialRetry(int attempt, int finalDist) {
            partialRetryCount.incrementAndGet();
            lastReason = "partial-retry";
            lastEventAtMs.set(System.currentTimeMillis());
            log.info("[WalkerTelemetry] PARTIAL_RETRY attempt={} finalDist={} totalPartial={}",
                    attempt, finalDist, partialRetryCount.get());
        }

        public static void recordUnreachable(String cause, WorldPoint player, WorldPoint target,
                                             WorldPoint pathEndpoint, int pathSize, int distanceThreshold,
                                             Pathfinder pathfinder) {
            unreachableCount.incrementAndGet();
            lastReason = "unreachable:" + cause;
            lastEventAtMs.set(System.currentTimeMillis());
            int distToTarget = (pathEndpoint != null && target != null) ? pathEndpoint.distanceTo(target) : -1;
            Pathfinder.PathfinderStats pfStats = (pathfinder != null) ? pathfinder.getStats() : null;
            String stats = (pfStats != null) ? pfStats.toString() : "null";
            log.warn("[WalkerTelemetry] UNREACHABLE cause={} player={} target={} pathEndpoint={} pathSize={} endpointToTarget={} threshold={} pathfinderStats={} totalUnreachable={}",
                    cause, player, target, pathEndpoint, pathSize, distToTarget, distanceThreshold, stats, unreachableCount.get());
        }

        public static void reset() {
            offPathRecalcCount.set(0);
            offPathRecalcDeferredCount.set(0);
            stallRecalcCount.set(0);
            partialRetryCount.set(0);
            unreachableCount.set(0);
            leaguesLockAttributedCount.set(0);
            leaguesLockStaleCount.set(0);
            leaguesLockParseMissCount.set(0);
            doorRejectByCause.clear();
            doorRejectSummaryLogSeq.set(0);
            offPathDeferredByReason.clear();
            offPathDeferredSummaryLogSeq.set(0);
            lastEventAtMs.set(0);
            lastReason = "";
            log.info("[WalkerTelemetry] counters reset");
        }

        public static int totalRecalcs() {
            return offPathRecalcCount.get() + stallRecalcCount.get() + partialRetryCount.get();
        }
    }

    // Trapdoor and manhole mappings for open/closed states
    public static boolean walkTo(int x, int y, int plane) {
        return walkTo(x, y, plane, reachedDistanceOrDefault());
    }

    /**
     * @see #walkTo(WorldPoint)
     */
    public static boolean walkTo(int x, int y, int plane, int distance) {
        return walkWithState(new WorldPoint(x, y, plane), distance) == WalkerState.ARRIVED;
    }


    /**
     * {@code null} {@code target} is rejected by {@link #walkWithState(WorldPoint, int)} ({@link WalkerState#EXIT});
     * result is {@code false}, same as any non-arrival outcome.
     */
    public static boolean walkTo(WorldPoint target) {
        return walkWithState(target, reachedDistanceOrDefault()) == WalkerState.ARRIVED;
    }

    /**
     * @see #walkTo(WorldPoint)
     * <p>{@code null} {@code target}: {@link #walkWithState(WorldPoint, int)} returns {@link WalkerState#EXIT}; this method returns {@code false}.
     */
    public static boolean walkTo(WorldPoint target, int distance) {
        return walkWithState(target, distance) == WalkerState.ARRIVED;
    }

    /**
     * Walks toward {@code target} until either the normal arrival distance is reached or
     * {@code completionCondition} becomes true.
     *
     * <p>The condition is polled by the thread that owns the walk at the walker's existing
     * cancellation checkpoints. It must be fast, read-only, and safe to call from a script
     * thread. The caller should perform the actual NPC/object interaction after this method
     * returns; the condition must not click or mutate game state.</p>
     *
     * <p>This is opt-in. Existing {@link #walkTo(WorldPoint, int)} and
     * {@link #walkWithState(WorldPoint, int)} callers are unaffected.</p>
     *
     * @return {@code true} when the destination is reached or the completion condition is met;
     * otherwise {@code false}
     */
    public static boolean walkUntil(WorldPoint target, int distance, BooleanSupplier completionCondition) {
        return walkWithStateUntil(target, distance, completionCondition) == WalkerState.ARRIVED;
    }

    /**
     * State-returning variant of {@link #walkUntil(WorldPoint, int, BooleanSupplier)}.
     * A satisfied completion condition is reported as {@link WalkerState#ARRIVED}, because the
     * caller-defined interaction destination is ready even if the coordinate destination has
     * not yet reached its distance threshold.
     */
    public static WalkerState walkWithStateUntil(
            WorldPoint target,
            int distance,
            BooleanSupplier completionCondition) {
        Objects.requireNonNull(completionCondition, "completionCondition");
        if (target == null) {
            return walkWithState(null, distance);
        }

        WalkCompletionContext previous = walkCompletionContext.get();
        WalkCompletionContext context = new WalkCompletionContext(target, completionCondition);
        walkCompletionContext.set(context);
        try {
            if (evaluateWalkCompletion(context)) {
                return WalkerState.ARRIVED;
            }
            WalkerState result = walkWithState(target, distance);
            return context.met ? WalkerState.ARRIVED : result;
        } finally {
            if (previous == null) {
                walkCompletionContext.remove();
            } else {
                walkCompletionContext.set(previous);
            }
        }
    }

    /**
     * Runs {@code action} while temporarily releasing {@link #walkerLock} for the current thread.
     * Used by long-running Leagues teleport wait so a second {@link #walkWithState} can proceed instead of blocking
     * on {@link java.util.concurrent.locks.ReentrantLock#lockInterruptibly()} for the full teleport timeout.
     * <p>No-op release path when the current thread does not hold the lock (e.g. calibration daemon).
     */
    public static void runWithWalkerLockReleased(Runnable action)
    {
        if (action == null)
        {
            throw new NullPointerException("action");
        }
        if (!walkerLock.isHeldByCurrentThread())
        {
            action.run();
            return;
        }
        int depth = walkerLock.getHoldCount();
        for (int i = 0; i < depth; i++)
        {
            walkerLock.unlock();
        }
        try
        {
            action.run();
        }
        finally
        {
            for (int i = 0; i < depth; i++)
            {
                walkerLock.lock();
            }
        }
    }

    public static WalkerState walkWithState(WorldPoint target, int distance) {
        if (config == null) {
            return WalkerState.EXIT;
        }
        if (target == null) {
            log.warn("[Walker] walk rejected: null target");
            return WalkerState.EXIT;
        }
        if (isClientThread()) {
            log.warn("Please do not call the walker from the main thread");
            return WalkerState.EXIT;
        }
        if (!walkerLock.tryLock()) {
            log.warn("[Walker] concurrent walk request detected, waiting for in-flight walk (held by {}); new target={}",
                    Thread.currentThread().getName(), target);
            try {
                walkerLock.lockInterruptibly();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return WalkerState.EXIT;
            }
        }
        try {
            if (config.walkWithBankedTransports()) {
                return walkWithBankedTransportsAndState(target, distance, false);
            } else {
                return walkWithStateInternal(target, distance);
            }
        } finally {
            walkerLock.unlock();
        }
    }

    /**
     * Like {@link #walkWithState} but bounds how long this thread waits for {@link #walkerLock}.
     * Use when another walk may hold the lock during Leagues UI (see {@link Rs2LeaguesTransport#leaguesTeleport})
     * or when a bounded wait is preferable to {@link java.util.concurrent.locks.ReentrantLock#lockInterruptibly()}.
     *
     * @param lockWaitMs max wait for the lock; {@code 0} = {@link ReentrantLock#tryLock()} only (no blocking)
     * @return {@link WalkerState#EXIT} if the lock is not acquired in time or the thread is interrupted
     */
    public static WalkerState walkWithStateTry(WorldPoint target, int distance, long lockWaitMs)
    {
        if (config == null)
        {
            return WalkerState.EXIT;
        }
        if (target == null)
        {
            log.warn("[Walker] walk rejected: null target");
            return WalkerState.EXIT;
        }
        if (isClientThread())
        {
            log.warn("Please do not call the walker from the main thread");
            return WalkerState.EXIT;
        }
        if (lockWaitMs < 0)
        {
            throw new IllegalArgumentException("lockWaitMs must be >= 0");
        }
        boolean locked;
        try
        {
            if (lockWaitMs == 0)
            {
                locked = walkerLock.tryLock();
            }
            else
            {
                locked = walkerLock.tryLock(lockWaitMs, TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            return WalkerState.EXIT;
        }
        if (!locked)
        {
            log.warn("[Walker] walkWithStateTry: walkerLock not acquired within {}ms (thread={}) target={}",
                    lockWaitMs, Thread.currentThread().getName(), target);
            return WalkerState.EXIT;
        }
        try
        {
            if (config.walkWithBankedTransports())
            {
                return walkWithBankedTransportsAndStateLocked(target, distance, false);
            }
            return walkWithStateInternal(target, distance);
        }
        finally
        {
            walkerLock.unlock();
        }
    }
    /**
     * Replaces the walkTo method
     *
     * @param target goal tile — non-null enforced at entry ({@code Objects.requireNonNull}); {@link #walkWithState} exits on null before delegating (same intent as {@link #walkWithStateTry}).
     * @param distance
     * @return
     */
    private static WalkerState walkWithStateInternal(WorldPoint target, int distance) {
        Objects.requireNonNull(target, "walk target");
        if (isClientThread()) {
            log.warn("Please do not call the walker from the main thread");
            return WalkerState.EXIT;
        }
        WorldPoint playerLocWalk = Rs2Player.getWorldLocation();
        if (playerLocWalk == null) {
            return WalkerState.MOVING;
        }
        int distToTarget = playerLocWalk.distanceTo(target);
        LocalPoint localTarget = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), target);
        boolean walkableCheck = Rs2Tile.isWalkable(localTarget);
        boolean reachableTileCheck = distToTarget <= distance && Rs2Tile.getReachableTilesFromTile(playerLocWalk, distance).containsKey(target);

        if (reachableTileCheck || (!walkableCheck && distToTarget <= distance)) {
            return WalkerState.ARRIVED;
        }

        final Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        if (pathfinder != null && !pathfinder.isDone()) {
            return WalkerState.MOVING;
        }
        boolean hasCurrentPath = pathfinder != null
                && pathfinder.isDone()
                && pathfinder.getTargets().contains(target)
                && canReuseNavigationRequest(NavigationEngineRuntime.getSnapshot(), target, distance);
        if (!hasCurrentPath) {
            setTarget(target, null, distance);
        } else {
            currentTarget = target;
        }
        Rs2PathApi.setReachedDistance(distance);
        routeState.stuckCount = 0;
        routeState.lastMovedTimeMs = System.currentTimeMillis();
		routeState.interimTargetWp = null;
		routeState.interimTargetIdx = -1;
		routeState.interimSetAtMs = 0L;
        routeState.interimLastProgressAtMs = 0L;
        routeState.interimLastBestPathIdx = -1;
        routeState.interimLastDistanceToTarget = Integer.MAX_VALUE;
        routeState.interimLastRetargetAtMs = 0L;
        routeState.lastPartialTransRecalcMs = 0L;
        routeState.idleNudgeLastObservedLocation = playerLocWalk;
        routeState.idleNudgeStationarySinceMs = System.currentTimeMillis();
        routeState.lastActiveRouteIdleNudgeAtMs = 0L;

		closeWorldMap();
        if (Rs2Bank.isOpen()) {
            Rs2Bank.closeBank();
        }
        markWalkSessionStart(target);
        return executeNavigationWalk(target, distance);
    }

    /**
     * @param target
     * @return
     */
    public static WalkerState walkWithState(WorldPoint target) {
        return walkWithState(target, reachedDistanceOrDefault());
    }

    /**
     * Non-blocking single-step walk. Unlike {@link #walkWithState(WorldPoint, int)} — which owns the
     * loop and blocks until NavigationEngine arrives or gives up — this advances the walk by
     * at most one action and returns immediately. The caller owns the loop: call it every tick and it
     * paths toward {@code target}, one minimap click at a time, so between calls the caller can re-check
     * its own condition (e.g. "is the NPC in range yet?") and act the instant it is.
     *
     * <p>Return values: {@link WalkerState#ARRIVED} when within {@code distance} of {@code target};
     * {@link WalkerState#MOVING} while still approaching (path computing, in transit, or a click issued);
     * {@link WalkerState#UNREACHABLE} when no walkable path reaches within {@code distance};
     * {@link WalkerState#EXIT} on a bad call (null target / client thread / no config).
     *
     * <p>Scope: plain approach-walking. It reuses the shared pathfinder + minimap machinery but NOT the
     * full NavigationEngine transport/door/stuck-recovery pipeline, so a route that needs a transport
     * or a door won't be driven here — use the blocking {@link #walkTo} for those. Clear the goal with
     * {@link #setTarget(WorldPoint, String) setTarget(null, reason)} when you stop (e.g. once you interact).
     */
    public static WalkerState walkStep(WorldPoint target, int distance) {
        if (config == null) {
            return WalkerState.EXIT;
        }
        if (target == null) {
            log.warn("[Walker] walkStep rejected: null target");
            return WalkerState.EXIT;
        }
        if (isClientThread()) {
            log.warn("Please do not call the walker from the main thread");
            return WalkerState.EXIT;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null) {
            return WalkerState.MOVING;
        }

        // Arrived? (mirrors walkWithStateInternal's arrival test)
        int distToTarget = playerLoc.distanceTo(target);
        LocalPoint localTarget = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), target);
        boolean walkableCheck = localTarget != null && Rs2Tile.isWalkable(localTarget);
        boolean reachableTileCheck = distToTarget <= distance
                && Rs2Tile.getReachableTilesFromTile(playerLoc, distance).containsKey(target);
        if (reachableTileCheck || (!walkableCheck && distToTarget <= distance)) {
            return WalkerState.ARRIVED;
        }

        // (Re)start pathfinding ONLY when the caller's goal changes. Do NOT re-target while the path is
        // still computing — setTarget() restarts pathfinding, so re-calling it every tick would reset the
        // pathfinder forever (path drawn, but never finished, so we never click).
        if (currentTarget == null || !currentTarget.equals(target)) {
            setTarget(target);
            return WalkerState.MOVING;
        }
        Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        if (pathfinder == null || !pathfinder.isDone()) {
            return WalkerState.MOVING; // path still computing — wait, don't reset it
        }

        // Already in transit toward the last click — let it resolve instead of spamming clicks.
        if (Rs2Player.isMoving()) {
            return WalkerState.MOVING;
        }

        final List<WorldPoint> rawPath = pathfinder.getPath();
        final List<WorldPoint> path = pathfinder.getWalkablePath();
        if (!walkStepPathReachesTarget(path, target, distance)) {
            setTarget(null, "rs2walker:walkStep:no-walkable-path");
            return WalkerState.UNREACHABLE;
        }

        // One minimap click toward the target (falls back to the furthest visible path point off-clip).
        // Click the target on the minimap, else fall back to the furthest visible planned-path point.
        // The geometric directional fallback (a straight-line b-line toward the target) is gated to NEAR
        // targets only: for a far target it's never right and produces off-route drift. When neither the
        // target nor a planned-path point is clickable (e.g. the route needs a transport walkStep can't
        // cross), no click is issued and we hold on the line rather than wander off it — walkStep is not
        // built for transport routes; use the blocking walkTo/walkUntil for those.
        boolean allowDirectionalFallback = playerLoc.distanceTo(target) <= NORMAL_MINIMAP_REACH_EUCLIDEAN;
        clickMiniMapOrFallback(rawPath, target, playerLoc, NORMAL_MINIMAP_REACH_EUCLIDEAN - 1, allowDirectionalFallback, -1);
        return WalkerState.MOVING;
    }

    /**
     * A completed pathfinder result is usable by {@link #walkStep(WorldPoint, int)} only when its
     * endpoint reaches the requested arrival radius. A multi-tile partial path is still terminal for
     * this non-blocking API: unlike the blocking NavigationEngine delegate, walkStep has no retry loop, so
     * accepting it would repeatedly click the same endpoint and report {@link WalkerState#MOVING}
     * forever.
     */
    static boolean walkStepPathReachesTarget(List<WorldPoint> path, WorldPoint target, int distance) {
        if (path == null || path.isEmpty() || target == null) {
            return false;
        }
        WorldPoint endpoint = path.get(path.size() - 1);
        return endpoint != null && endpoint.distanceTo(target) <= Math.max(0, distance);
    }

    /**
     * Runs any pre-route compatibility preparation, then delegates the complete walking lifecycle
     * to NavigationEngine. There is deliberately no legacy executor fallback.
     *
     * @param target
     * @param distance
     */
    private static WalkerState executeNavigationWalk(WorldPoint target, int distance) {
        // Solve the Draynor basement lever puzzle first if walking to a basement tile, so the
        // door-transports are unlocked before pathfinding. No-op outside the basement. The
        // solver's internal walkTo calls clear currentTarget, so restore it before the real walk.
        if (DraynorBasementSolver.isBasementTarget(target)) {
            DraynorBasementSolver.solveIfNeeded(target);
            // The solver's nested walkTo calls clear currentTarget; restore it so the real walk
            // runs — but not if this walk was interrupted/cancelled while the (blocking) solver
            // ran (the solver itself never interrupts, so an interrupt here is an external cancel).
            if (!Thread.currentThread().isInterrupted()) {
                setTarget(target, "rs2walker:basement-solve-restore");
            }
        }
        return NavigationWalkCoordinator.walk(target, MAX_NAVIGATION_ENGINE_PASSES,
                NAVIGATION_WALK_DRIVER);
    }

    static boolean canReuseNavigationRequest(NavigationSnapshot snapshot, WorldPoint target, int distance) {
        return snapshot != null && !snapshot.isTerminal()
                && snapshot.getRequest().getDestinations().equals(Set.of(target))
                && snapshot.getRequest().getReachedDistance() == Math.max(0, distance);
    }

    public static boolean walkNextTo(GameObject target) {
        Rs2WorldArea gameObjectArea = new Rs2WorldArea(Objects.requireNonNull(Rs2GameObject.getWorldArea(target)));
        List<WorldPoint> interactablePoints = gameObjectArea.getInteractable();

        if (interactablePoints.isEmpty()) {
            interactablePoints.addAll(gameObjectArea.offset(1).toWorldPointList());
            interactablePoints.removeIf(gameObjectArea::contains);
        }

        WorldPoint walkableInteractPoint = interactablePoints.stream()
                .filter(Rs2Tile::isWalkable)
                .findFirst()
                .orElse(null);
        // Priority to a walkable tile, otherwise walk to the first tile next to locatable

        if(walkableInteractPoint != null && walkableInteractPoint.equals(Rs2Player.getWorldLocation()))
            return true;
        return walkableInteractPoint != null ? walkTo(walkableInteractPoint) : walkTo(interactablePoints.get(0));
    }

    public static void walkNextToInstance(GameObject target) {
        Rs2WorldArea gameObjectArea = new Rs2WorldArea(Objects.requireNonNull(Rs2GameObject.getWorldArea(target)));
        List<WorldPoint> interactablePoints = gameObjectArea.getInteractable();

        if (interactablePoints.isEmpty()) {
            interactablePoints.addAll(gameObjectArea.offset(1).toWorldPointList());
            interactablePoints.removeIf(gameObjectArea::contains);
        }

        WorldPoint walkableInteractPoint = interactablePoints.stream()
                .filter(Rs2Tile::isWalkable).min(Comparator.comparingInt(Rs2Player.getWorldLocation()::distanceTo))
                .orElse(null);
        // Priority to a walkable tile, otherwise walk to the first tile next to locatable
        if (walkableInteractPoint != null) {
            if(walkableInteractPoint.equals(Rs2Player.getWorldLocation()))
                return;
            walkFastLocal(LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), walkableInteractPoint));
        } else {
            walkFastLocal(LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), Objects.requireNonNull(interactablePoints.stream().min(Comparator.comparingInt(Rs2Player.getWorldLocation()::distanceTo))
                    .orElse(null))));
        }
    }

    public static WorldPoint getPointWithWallDistance(WorldPoint target) {
        return getPointWithWallDistance(target, null);
    }

    /**
     * Nudges a click target off a wall edge onto an open neighbour. The candidate set
     * ({@link Rs2Tile#getReachableTilesFromTile} radius 1) is an unordered {@link Set}, so returning
     * the first clean tile picks an arbitrary side — that is how the walker ends up clicking the far
     * side of a wall or into a building. When {@code playerLoc} is supplied, choose the clean
     * neighbour reachable from the player and nearest to the player, keeping the nudge on the
     * player's side (the road). See movement.md #19.
     */
    public static WorldPoint getPointWithWallDistance(WorldPoint target, WorldPoint playerLoc) {
        var tiles = Rs2Tile.getReachableTilesFromTile(target, 1);

        var wv = Microbot.getClient().getTopLevelWorldView();
        var localPoint = LocalPoint.fromWorld(wv, target);
        if (wv.getCollisionMaps() != null && localPoint != null) {
            int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();

            Set<WorldPoint> reachableFromPlayer = playerLoc == null
                    ? Collections.emptySet()
                    : Rs2Tile.getReachableTilesFromTile(playerLoc,
                            Math.max(2, NORMAL_MINIMAP_REACH_EUCLIDEAN)).keySet();

            if (hasMinimapRelevantMovementFlag(localPoint, flags)) {
                WorldPoint best = bestWallDistanceNeighbor(tiles.keySet(), playerLoc, reachableFromPlayer,
                        tile -> {
                            var lp = LocalPoint.fromWorld(wv, tile);
                            return lp != null && !hasMinimapRelevantMovementFlag(lp, flags);
                        });
                if (best != null) {
                    return best;
                }
            }

            int data = flags[localPoint.getSceneX()][localPoint.getSceneY()];

            Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

            if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_EAST)
                    || movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_WEST)
                    || movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH)
                    || movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH)) {
                WorldPoint best = bestWallDistanceNeighbor(tiles.keySet(), playerLoc, reachableFromPlayer,
                        tile -> {
                            var lp = LocalPoint.fromWorld(wv, tile);
                            if (lp == null) {
                                return false;
                            }
                            return MovementFlag.getSetFlags(flags[lp.getSceneX()][lp.getSceneY()]).isEmpty();
                        });
                if (best != null) {
                    return best;
                }
            }
        }

        return target;
    }

    /**
     * Picks the wall-distance neighbour that best keeps the click on the player's side of the wall:
     * prefer tiles reachable from the player, then the one nearest the player. Falls back to the
     * first clean tile when the player position is unknown, preserving the original behaviour.
     */
    private static WorldPoint bestWallDistanceNeighbor(Collection<WorldPoint> candidates,
                                                       WorldPoint playerLoc,
                                                       Set<WorldPoint> reachableFromPlayer,
                                                       Predicate<WorldPoint> isClean) {
        WorldPoint best = null;
        boolean bestReachable = false;
        int bestDist = Integer.MAX_VALUE;
        for (WorldPoint tile : candidates) {
            if (tile == null || !isClean.test(tile)) {
                continue;
            }
            if (playerLoc == null) {
                return tile; // no player context: preserve original "first clean tile" behaviour
            }
            boolean reachable = reachableFromPlayer.contains(tile);
            int dist = tile.distanceTo2D(playerLoc);
            boolean better = best == null
                    || (reachable && !bestReachable)
                    || (reachable == bestReachable && dist < bestDist);
            if (better) {
                best = tile;
                bestReachable = reachable;
                bestDist = dist;
            }
        }
        return best;
    }

    private static boolean isKnownWalkableOrUnloaded(WorldPoint target) {
        if (target == null) {
            return false;
        }

        LocalPoint localTarget = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), target);
        return localTarget == null || Rs2Tile.isWalkable(localTarget);
    }

    private static boolean isWalkCancelled(WorldPoint target) {
        WalkCompletionContext completion = walkCompletionContext.get();
        if (completion != null && Objects.equals(completion.target, target)
                && evaluateWalkCompletion(completion)) {
            if (Objects.equals(currentTarget, target)) {
                setTarget(null, "rs2walker:completion-condition-met");
            }
            return true;
        }
        WorldPoint activeTarget = currentTarget;
        return target == null || activeTarget == null || !target.equals(activeTarget)
                || Thread.currentThread().isInterrupted();
    }

    /**
     * Completion callbacks are user-supplied extension code. One bad callback must not strand
     * the global walker lock or abort an otherwise valid route, so disable it after its first
     * exception and let normal distance-based walking continue.
     */
    private static boolean evaluateWalkCompletion(WalkCompletionContext context) {
        if (context.met) {
            return true;
        }
        if (context.failed) {
            return false;
        }
        try {
            context.met = context.condition.getAsBoolean();
        } catch (RuntimeException ex) {
            context.failed = true;
            log.warn("[Walker] completion condition failed; continuing with distance-based walking: {}",
                    ex.toString());
        }
        return context.met;
    }

    static boolean hasMinimapRelevantMovementFlag(LocalPoint point, int[][] flagMap) {
        int data = flagMap[point.getSceneX()][point.getSceneY()];
        Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

        if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_EAST)
                && Rs2Tile.isWalkable(point.dx(1)))
            return true;

        if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_WEST)
                && Rs2Tile.isWalkable(point.dx(-1)))
            return true;

        if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH)
                && Rs2Tile.isWalkable(point.dy(1)))
            return true;

        return movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH)
                && Rs2Tile.isWalkable(point.dy(-1));
    }

    static final int STAMINA_THRESHOLD_MIN = 12;
    static final int STAMINA_THRESHOLD_MAX = 55;
    static final int STAMINA_CASUAL_MIN = 35;
    static final int STAMINA_CASUAL_MAX = 55;
    static final int STAMINA_HARDCORE_MIN = 12;
    static final int STAMINA_HARDCORE_MAX = 24;
    static final double STAMINA_HARDCORE_PROBABILITY = 0.3;
    private static final int STAMINA_THRESHOLD_FALLBACK = 35;
	// Door-scan cooldown state migrated to WalkerRouteState; the fallback/LOS timestamps that
	// used to sit here were dead (written by nothing, read by nothing) and are simply gone.
    /**
     * End-snap guard bounds: a recovery target within this Euclidean radius that is absent from the
     * player-origin reachability BFS is walled/doored off (the recovery BFS's 18-step budget comfortably
     * covers the full ~9-tile recovery click radius; a connected tile needing more steps than that inside
     * this radius is a pathological fold where replanning is also the right answer), so recovery replans
     * instead of clicking through the wall; the cooldown stops replans looping while the fresh path
     * computes. Covers the whole recovery click range — Clock Tower showed goal-clicks from 9 tiles out.
     */
    static int computeStaminaThreshold(String playerName, long installSeed) {
        if (playerName == null || playerName.isEmpty()) {
            return STAMINA_THRESHOLD_FALLBACK;
        }
        long nameHash = mix64(playerName.toLowerCase());
        long seed = nameHash ^ installSeed;
        java.util.Random rng = new java.util.Random(seed);
        if (rng.nextDouble() < STAMINA_HARDCORE_PROBABILITY) {
            int span = STAMINA_HARDCORE_MAX - STAMINA_HARDCORE_MIN + 1;
            return STAMINA_HARDCORE_MIN + rng.nextInt(span);
        }
        int span = STAMINA_CASUAL_MAX - STAMINA_CASUAL_MIN + 1;
        return STAMINA_CASUAL_MIN + rng.nextInt(span);
    }

    private static long mix64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    public static boolean walkMiniMap(WorldPoint worldPoint, double zoomDistance) {
        if (Microbot.getClient().getMinimapZoom() != zoomDistance)
            Microbot.getClient().setMinimapZoom(zoomDistance);

        Point point = Rs2MiniMap.worldToMinimap(worldPoint);

        if (point == null) return false;
        if (!disableWalkerUpdate && !Rs2MiniMap.isPointInsideMinimap(point)) return false;

        Microbot.getMouse().click(point);
        return true;
    }


    public static boolean walkMiniMap(WorldPoint worldPoint) {
        return walkMiniMap(worldPoint, 5);
    }

    private static boolean isMiniMapClickable(WorldPoint worldPoint, double zoomDistance) {
        if (worldPoint == null) {
            return false;
        }
        if (Microbot.getClient().getMinimapZoom() != zoomDistance) {
            Microbot.getClient().setMinimapZoom(zoomDistance);
        }
        Point point = Rs2MiniMap.worldToMinimap(worldPoint);
        return point != null && (disableWalkerUpdate || Rs2MiniMap.isPointInsideMinimap(point));
    }

    private static WorldPoint clickMiniMapOrFallback(List<WorldPoint> rawPath,
                                                     WorldPoint target,
                                                     WorldPoint playerLoc,
                                                     int maxEuclidean,
                                                     boolean allowDirectionalFallback) {
        return clickMiniMapOrFallback(rawPath, target, playerLoc, maxEuclidean, allowDirectionalFallback, -1);
    }

    private static WorldPoint clickMiniMapOrFallback(List<WorldPoint> rawPath,
                                                     WorldPoint target,
                                                     WorldPoint playerLoc,
                                                     int maxEuclidean,
                                                     boolean allowDirectionalFallback,
                                                     int rawAnchorIndex) {
        if (target == null || playerLoc == null || target.equals(playerLoc)) {
            return null;
        }
        if (walkMiniMap(target)) {
            return target;
        }
        WorldPoint rawFallback = walkRawPathMiniMapTargetToward(rawPath, target, playerLoc,
                maxEuclidean, rawAnchorIndex);
        if (rawFallback != null) {
            return rawFallback;
        }
        if (allowDirectionalFallback && walkMiniMapToward(target, playerLoc, maxEuclidean)) {
            return target;
        }
        return null;
    }

    private static WorldPoint walkRawPathMiniMapTargetToward(List<WorldPoint> rawPath,
                                                             WorldPoint target,
                                                             WorldPoint playerLoc,
                                                             int maxEuclidean,
                                                             int rawAnchorIndex) {
        WorldPoint fallback = findFurthestVisibleKnownRawPathPoint(rawPath, playerLoc,
                maxEuclidean, rawAnchorIndex);
        if (fallback == null || fallback.equals(playerLoc) || fallback.equals(target)) {
            return null;
        }
        if (walkMiniMap(fallback)) {
            log.info("[Walker] Minimap click target {} was outside clip; used route fallback {}", target, fallback);
            return fallback;
        }
        return null;
    }

    static boolean walkMiniMapToward(WorldPoint target, WorldPoint playerLoc, int maxEuclidean) {
        if (target == null || playerLoc == null || target.getPlane() != playerLoc.getPlane()) {
            return false;
        }

        int dx = target.getX() - playerLoc.getX();
        int dy = target.getY() - playerLoc.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= 1) {
            return false;
        }

        if (walkReachableMiniMapToward(target, playerLoc, maxEuclidean)) {
            return true;
        }

        int cappedRadius = Math.max(2, maxEuclidean);
        // The scaled-radius points below are geometric guesses toward an off-clip target. Right
        // after a teleport (or when the target sits behind a wall) that guess can be an unreachable
        // tile far off the route, producing the "random click far from the path" behaviour. Only
        // click a guess that is actually reachable from the player.
        Set<WorldPoint> reachable = Rs2Tile
                .getReachableTilesFromTile(playerLoc, Math.max(2, cappedRadius)).keySet();
        int[] radii = new int[] {cappedRadius, 10, 8, 6, 4};
        for (int radius : radii) {
            if (radius >= distance) {
                continue;
            }

            double scale = radius / distance;
            WorldPoint fallback = new WorldPoint(
                    playerLoc.getX() + (int) Math.round(dx * scale),
                    playerLoc.getY() + (int) Math.round(dy * scale),
                    playerLoc.getPlane());
            if (fallback.equals(playerLoc)) {
                continue;
            }
            if (!reachable.contains(fallback)) {
                continue;
            }
            if (Rs2Walker.walkMiniMap(fallback)) {
                log.info("[Walker] Minimap click target {} was outside clip; used fallback {}", target, fallback);
                return true;
            }
        }

        return false;
    }

    private static boolean walkReachableMiniMapToward(WorldPoint target, WorldPoint playerLoc, int maxEuclidean) {
        int currentDistance = euclideanSq(playerLoc, target);
        return Rs2Tile.getReachableTilesFromTile(playerLoc, Math.max(2, maxEuclidean)).keySet().stream()
                .filter(tile -> tile != null
                        && tile.getPlane() == playerLoc.getPlane()
                        && !tile.equals(playerLoc)
                        && euclideanSq(playerLoc, tile) <= maxEuclidean * maxEuclidean
                        && euclideanSq(tile, target) < currentDistance)
                .sorted(Comparator
                        .comparingInt((WorldPoint tile) -> euclideanSq(tile, target))
                        .thenComparing(Comparator.comparingInt((WorldPoint tile) -> euclideanSq(playerLoc, tile)).reversed()))
                .filter(Rs2Walker::walkMiniMap)
                .findFirst()
                .map(tile -> {
                    log.info("[Walker] Minimap click target {} was outside clip; used reachable fallback {}", target, tile);
                    return true;
                })
                .orElse(false);
    }

    // findFurthestRawPathPointMatching (pure) moved to geometry/WalkerPathGeometry (P1); this game-coupled
    // wrapper supplies the constant forward-search window and the lazy reachable-closest fallback. UNGATED —
    // it is the pure-selection unit the tests exercise; live click paths use the gated variant below.
    static WorldPoint findFurthestRawPathPointMatching(List<WorldPoint> rawPath,
                                                       WorldPoint playerLoc,
                                                       int maxEuclidean,
                                                       int rawAnchorIndex,
                                                       Predicate<WorldPoint> isCandidate) {
        return WalkerPathGeometry.findFurthestRawPathPointMatching(rawPath, playerLoc, maxEuclidean,
                rawAnchorIndex, isCandidate, ROUTE_PROGRESS_FORWARD_SEARCH_TILES,
                () -> getClosestTileIndex(rawPath, playerLoc));
    }

    /**
     * Live-click variant of {@link #findFurthestRawPathPointMatching} with the route-blocked scan gate: it
     * additionally supplies the player-origin reachability BFS so forward click selection stops at the near
     * side of a closed door / wall ON the route instead of selecting statically-walkable tiles beyond it
     * (which made the server path the player AROUND buildings — the Clock Tower off-route bug). Kept
     * separate from the ungated wrapper because the gate reads live game state (the BFS), which the
     * pure-selection unit tests must not depend on.
     */
    private static WorldPoint findFurthestRawPathPointMatchingGated(List<WorldPoint> rawPath,
                                                                    WorldPoint playerLoc,
                                                                    int maxEuclidean,
                                                                    int rawAnchorIndex,
                                                                    Predicate<WorldPoint> isCandidate) {
        Map<WorldPoint, Integer> reachable = getClosestIndexReachableTiles(playerLoc);
        WorldPoint selected = WalkerPathGeometry.findFurthestRawPathPointMatching(rawPath, playerLoc, maxEuclidean,
                rawAnchorIndex, isCandidate, ROUTE_PROGRESS_FORWARD_SEARCH_TILES,
                () -> getClosestTileIndex(rawPath, playerLoc),
                reachable, CLOSEST_INDEX_REACHABLE_STEP_BUDGET);
        // Output-side net: whatever path the scan took (stale anchor, player-anchored retry, a fold the
        // along-route gate could not vouch for), a selected tile that is Euclidean-NEAR the player yet
        // absent from the player-origin BFS is on the far side of a wall/door — clicking it walks the
        // player into the wall (Wydin's shop: first click chose the goal 2 tiles away through the
        // back-room wall and the walk unravelled from there). Refuse it; every caller has a
        // reachability-aware fallback (wall-nudge clamp, rejoin, recovery). The log carries the anchor
        // context so a recurrence is diagnosable from a single line.
        if (selected != null && reachable != null && !reachable.isEmpty()
                && !reachable.containsKey(selected)
                && playerLoc != null
                && playerLoc.distanceTo2D(selected) <= CLOSEST_INDEX_REACHABLE_STEP_BUDGET - 2) {
            WebWalkLog.spInfo("route_click_walled | to={} player={} anchorIdx={} — refused, falling back",
                    compactWorldPoint(selected), compactWorldPoint(playerLoc), rawAnchorIndex);
            return null;
        }
        return selected;
    }

    /**
     * Selects the next minimap click target from the raw route, gated on collision reachability.
     * <p>
     * Preference order:
     * <ol>
     *   <li>Furthest-forward raw point that is collision-reachable from the player. A point on the
     *       far side of a wall is Euclidean-close but not reachable within the sampled area, so it is
     *       excluded — this is what stops the walker clicking through castle walls / into buildings.</li>
     *   <li>Furthest-forward raw point that is off the loaded scene. Collision cannot be verified for
     *       unloaded tiles, but a minimap click toward a distant route point is still correct, so long
     *       outdoor routes keep flowing.</li>
     * </ol>
     * Returns {@code null} when neither exists; the caller then falls back to wall-distance nudging
     * plus {@link #findReachableRejoinRawPathPoint} rejoin handling.
     */
    private static WorldPoint selectRouteClickTarget(List<WorldPoint> rawPath, WorldPoint playerLoc,
                                                     int maxEuclidean, int rawAnchorIndex) {
        if (rawPath == null || rawPath.isEmpty() || playerLoc == null) {
            routeState.lastRouteClickTier = "norawpath";
            return null;
        }
        // Anti-ban: vary HOW FAR ALONG the route we click. Selection otherwise always returns the
        // furthest candidate inside a fixed radius, so every click covers the same tile span — a
        // deterministic signature. Varying the reach is the safe axis: it only changes how far
        // forward we pick, never sideways, so the target stays on the planned route (#20). Lateral
        // tile offsets are the wrong axis and were removed for exactly that reason (#15); lateral
        // randomness belongs inside the tile (click-point jitter), not in tile selection.
        int jitteredReach = routeClickReach(maxEuclidean);
        WorldPoint selected = selectRouteClickTargetAnchored(rawPath, playerLoc, jitteredReach, rawAnchorIndex);
        if (selected == null && jitteredReach < maxEuclidean) {
            // A shortened reach must never be the reason selection fails — that would drop the click
            // onto the caller's off-route wall-nudge clamp. Retry at full reach before giving up.
            selected = selectRouteClickTargetAnchored(rawPath, playerLoc, maxEuclidean, rawAnchorIndex);
        }
        if (selected == null && rawAnchorIndex >= 0) {
            // The smoothed->raw anchor can point past the player's vicinity (stale mapping, sparse
            // smoothing, or a replanned route). The anchored forward scan then breaks immediately on
            // the Euclidean bound and yields nothing for EVERY predicate — which is exactly the
            // sel=none case that dropped route clicks onto the off-route wall-nudge clamp. Retry
            // anchored at the player's own closest raw tile before giving up.
            // Keep the jitter on this path too. The player-anchored retry fires on most first clicks
            // of a route, so using full reach here bypassed the reach variation exactly where it is
            // most visible — measured click distances clustered at 9.0-10.0 instead of spreading.
            selected = selectRouteClickTargetAnchored(rawPath, playerLoc, jitteredReach, -1);
            if (selected == null && jitteredReach < maxEuclidean) {
                selected = selectRouteClickTargetAnchored(rawPath, playerLoc, maxEuclidean, -1);
            }
            if (selected != null) {
                routeState.lastRouteClickTier = routeState.lastRouteClickTier + "@player";
            }
        }
        return selected;
    }

    /**
     * Per-click route reach, jittered below {@code maxEuclidean} so consecutive clicks do not all
     * cover the same tile span.
     * <p>
     * The floor matters: it must stay clear of {@link #INTERIM_CLOSE_TILES} or the interim
     * checkpoint clears almost immediately and the walker re-clicks constantly, producing visible
     * stop-start movement. The ceiling is the caller's reach, which is already tuned to the minimap
     * clip — going above it just produces outside-clip fallbacks.
     */
    static int routeClickReach(int maxEuclidean) {
        int floor = Math.min(ROUTE_CLICK_REACH_MIN_TILES, maxEuclidean);
        if (maxEuclidean <= floor) {
            return maxEuclidean;
        }
        return Rs2Random.betweenInclusive(floor, maxEuclidean);
    }

    private static WorldPoint selectRouteClickTargetAnchored(List<WorldPoint> rawPath, WorldPoint playerLoc,
                                                             int maxEuclidean, int rawAnchorIndex) {
        // Click the furthest forward point ON THE RAW ROUTE that is within minimap reach.
        //
        // A minimap click is resolved by the GAME's own pathing, so line of sight is irrelevant to
        // walking: a player clicks past a corner, through a doorway, or around a building and the
        // server routes them there. Requiring straight LOS made the walker advance corner-to-corner,
        // stopping at each one to re-aim — a visible tell, and it bought no correctness. The
        // invariant that actually matters is that the target sits ON the planned route, so wherever
        // the server routes us we still arrive on that route.
        //
        // The off-route click (3176,3428) that started this came from the caller's
        // smoothed-waypoint Euclidean clamp after selection returned null on a stale anchor — not
        // from a lack of line of sight. Pending doors/gates are handled by
        // handlePendingDoorBeforeRouteClick, not by shortening the click.
        WorldPoint forward = findFurthestRawPathPointMatchingGated(rawPath, playerLoc, maxEuclidean,
                rawAnchorIndex, Rs2Walker::isKnownWalkableOrUnloaded);
        if (forward != null && !forward.equals(playerLoc)) {
            routeState.lastRouteClickTier = "route";
            return forward;
        }
        routeState.lastRouteClickTier = "none";
        return null;
    }

    /**
     * Which tier of {@link #selectRouteClickTarget} produced the most recent click target
     * ({@code los} / {@code reach} / {@code offscene} / {@code none}, or {@code wallnudge} /
     * {@code rejoin} when selection fell through). Surfaced in the {@code click_candidate_found}
     * tmark so a log alone shows which selection path a click came from.
     */
    // routeState.lastRouteClickTier migrated to WalkerRouteState (see routeState)




    /**
     * Finds a reachable raw-path point to rejoin the route after the player has been pushed off it
     * (stuck against a wall, knocked back, or landed off-path after a teleport). Unlike the primary
     * forward-only selection, this scans a bounded index window on BOTH sides of the anchor so the
     * walker can step slightly backward onto the path line when nothing ahead is reachable. Forward
     * points are still preferred (highest index first), so normal progress is never sacrificed and
     * the walker cannot snap all the way back to an already-travelled branch.
     */
    private static WorldPoint findReachableRejoinRawPathPoint(List<WorldPoint> rawPath, WorldPoint playerLoc,
                                                              int maxEuclidean, int rawAnchorIndex) {
        if (playerLoc == null) {
            return null;
        }
        Set<WorldPoint> reachable = Rs2Tile
                .getReachableTilesFromTile(playerLoc, Math.max(2, maxEuclidean * 2)).keySet();
        return findReachableRejoinRawPathPoint(rawPath, playerLoc, maxEuclidean, rawAnchorIndex,
                reachable::contains);
    }

    /**
     * Testable core of {@link #findReachableRejoinRawPathPoint(List, WorldPoint, int, int)} with an
     * injectable reachability predicate (so it can be exercised without a live client).
     */
    // findReachableRejoinRawPathPoint (pure core) moved to geometry/WalkerPathGeometry (P1); same-signature
    // wrapper so Rs2WalkerUnitTest and callers are untouched.
    static WorldPoint findReachableRejoinRawPathPoint(List<WorldPoint> rawPath, WorldPoint playerLoc,
                                                      int maxEuclidean, int rawAnchorIndex,
                                                      Predicate<WorldPoint> isReachable) {
        return WalkerPathGeometry.findReachableRejoinRawPathPoint(rawPath, playerLoc, maxEuclidean,
                rawAnchorIndex, isReachable, ROUTE_PROGRESS_FORWARD_SEARCH_TILES,
                () -> getClosestTileIndex(rawPath, playerLoc));
    }

    static WorldPoint findFurthestVisibleKnownRawPathPoint(List<WorldPoint> rawPath,
                                                           WorldPoint playerLoc,
                                                           int maxEuclidean) {
        return findFurthestVisibleKnownRawPathPoint(rawPath, playerLoc, maxEuclidean, -1);
    }

    static WorldPoint findFurthestVisibleKnownRawPathPoint(List<WorldPoint> rawPath,
                                                           WorldPoint playerLoc,
                                                           int maxEuclidean,
                                                           int rawAnchorIndex) {
        if (rawPath == null || rawPath.isEmpty() || playerLoc == null) {
            return null;
        }

        return findFurthestRawPathPointMatchingGated(rawPath, playerLoc, maxEuclidean, rawAnchorIndex,
                candidate -> !candidate.equals(playerLoc)
                        && isKnownWalkableOrUnloaded(candidate)
                        && isMiniMapClickable(candidate, 5));
    }

    // rawPathStepDistance (pure) moved to geometry/WalkerPathGeometry (P1) alongside its only caller,
    // findFurthestRawPathPointMatching; no remaining Rs2Walker callers.

    // isLocalRecoveryCandidateOnForwardRoute extracted to recovery/RouteRecovery (P1)

    // rawPathForwardAnchorIndex (pure) moved to geometry/WalkerPathGeometry (P1); this game-coupled wrapper
    // supplies the forward-search window constant and the lazy reachable-closest fallback.
    static int rawPathForwardAnchorIndex(List<WorldPoint> rawPath, WorldPoint playerLoc, int rawAnchorIndex) {
        return WalkerPathGeometry.rawPathForwardAnchorIndex(rawPath, playerLoc, rawAnchorIndex,
                ROUTE_PROGRESS_FORWARD_SEARCH_TILES, () -> getClosestTileIndex(rawPath, playerLoc));
    }

    private static boolean tryIssueRouteMovementClick(List<WorldPoint> rawPath,
                                                      List<WorldPoint> path,
                                                      WorldPoint target,
                                                      int configuredDistance,
                                                      String logLabel,
                                                      int maxEuclidean,
                                                      boolean markRecoveryCooldown) {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null || path == null || path.isEmpty()) {
            return false;
        }
        if (routeArrivalSatisfied(playerLoc, target, path, configuredDistance)) {
            return false;
        }
        int targetIdx = stabilizeRouteProgressIndex(path, getClosestTileIndex(path, playerLoc), target, playerLoc);
        int startIdx = Math.max(0, targetIdx);
        int[] routeSmoothedToRaw = mapSmoothedToRaw(path, rawPath);
        int rawAnchorIndex = rawIndexForSmoothedIndex(targetIdx, routeSmoothedToRaw, rawPath);

        // Use the same route-backed selector as the main route loop. Recovery/idle-nudge and
        // interim continuation clicks previously had their own walkable-only selection, so which
        // policy applied depended on which path happened to fire (timing-dependent, e.g. a slow
        // pathfinder makes the idle nudge issue the first click instead of the main loop).
        WorldPoint clickTarget = selectRouteClickTarget(rawPath, playerLoc, maxEuclidean, rawAnchorIndex);
        if (clickTarget == null) {
            int clickableIdx = RouteRecovery.findFurthestForwardClickableIndex(path, startIdx, playerLoc,
                    wp -> {
                        Set<Transport> ts = Rs2PathApi.getTransports().get(wp);
                        return ts != null && !ts.isEmpty();
                    },
                    maxEuclidean);
            clickableIdx = Math.max(startIdx, Math.min(clickableIdx, path.size() - 1));
            clickTarget = path.get(clickableIdx);
            targetIdx = clickableIdx;
            if (euclideanSq(clickTarget, playerLoc)
                    > maxEuclidean * maxEuclidean) {
                clickTarget = RouteRecovery.interpolateClickableTarget(
                        path,
                        startIdx,
                        playerLoc,
                        clickTarget,
                        maxEuclidean - 1,
                        Rs2Walker::isKnownWalkableOrUnloaded);
            }
        }

        boolean clicked = false;
        WorldPoint clickedTarget = null;
        if (clickTarget != null && !clickTarget.equals(playerLoc)) {
            clickTarget = RouteRecovery.clampToEuclideanRadius(playerLoc, clickTarget, maxEuclidean - 1);
            clickedTarget = clickMiniMapOrFallback(rawPath, clickTarget, playerLoc,
                    maxEuclidean - 1, rawPath == null || rawPath.isEmpty(), rawAnchorIndex);
            clicked = clickedTarget != null;
        }
        // EVERY movement click logs at info. The interim-continuation label used to log at debug only,
        // which made its clicks invisible: the walker appeared to "randomly click far from the path"
        // and ran for minutes with no clicks in the log while continuation clicks silently chained
        // (interim expires -> unlogged click -> player moving -> off-path recalc deferred -> repeat).
        log.info("[Walker] {}: clicked={} to={} player={} idx={}",
                logLabel, clicked, clicked ? clickedTarget : clickTarget, playerLoc, targetIdx);
        if (!clicked) {
            return false;
        }

        markFirstMovementClick(routeMovementClickPhase(logLabel),
                target,
                playerLoc,
                "to=" + compactWorldPoint(clickedTarget));
        hintRouteProgressIndex(path,
                Math.min(targetIdx, startIdx + INTERIM_CLOSE_TILES),
                target);
        routeState.interimTargetWp = clickedTarget;
        routeState.interimTargetIdx = targetIdx;
        routeState.interimSetAtMs = System.currentTimeMillis();
        routeState.interimLastProgressAtMs = routeState.interimSetAtMs;
        routeState.interimLastBestPathIdx = getClosestTileIndex(path, playerLoc);
        routeState.interimLastDistanceToTarget = distanceToInterimOrMax(clickedTarget, playerLoc);
        routeState.interimLastRetargetAtMs = routeState.interimSetAtMs;
        if (markRecoveryCooldown) {
            routeState.lastUnreachableRecoveryClickAtMs = routeState.interimSetAtMs;
        }
        if ("active route idle nudge".equals(logLabel)) {
            routeState.lastActiveRouteIdleNudgeAtMs = routeState.interimSetAtMs;
        } else {
            routeState.lastMovedTimeMs = routeState.interimSetAtMs;
        }
        routeState.idleNudgeStationarySinceMs = routeState.interimSetAtMs;
        routeState.idleNudgeLastObservedLocation = playerLoc;
        routeState.stuckCount = 0;
        return true;
    }

    static boolean routeArrivalSatisfied(WorldPoint playerLoc,
                                         WorldPoint target,
                                         List<WorldPoint> path,
                                         int configuredDistance) {
        if (playerLoc == null || target == null || path == null || path.isEmpty()
                || playerLoc.getPlane() != target.getPlane()) {
            return false;
        }
        WorldPoint pathEnd = path.get(path.size() - 1);
        int finishThreshold = tightFinishThreshold(target, pathEnd, configuredDistance);
        return playerLoc.distanceTo2D(target) <= finishThreshold;
    }

    static String routeMovementClickPhase(String logLabel) {
        if ("stall recovery click".equals(logLabel)) {
            return "stall_recovery_click";
        }
        if ("active route idle nudge".equals(logLabel)) {
            return "active_route_idle_nudge";
        }
        if ("interim close route click".equals(logLabel)) {
            return "interim_close_route_click";
        }
        return "route_movement_click";
    }

    /**
     * Used in instances like vorkath, jad, nmz
     *
     * @param localPoint A two-dimensional point in the local coordinate space.
     */
    public static void walkFastLocal(LocalPoint localPoint) {
        Point canv = Perspective.localToCanvas(Microbot.getClient(), localPoint, Microbot.getClient().getTopLevelWorldView().getPlane());
        int canvasX = canv != null ? canv.getX() : -1;
        int canvasY = canv != null ? canv.getY() : -1;

        NewMenuEntry entry = new NewMenuEntry()
                .param0(canvasX)
                .param1(canvasY)
                .type(MenuAction.WALK)
                .identifier(0)
                .itemId(-1)
                .option("Walk here");

        Microbot.doInvoke(entry,
                new Rectangle(1, 1, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
        //Rs2Reflection.invokeMenu(canvasX, canvasY, MenuAction.WALK.getId(), 0, -1, "Walk here", "", -1, -1);
    }

    public static boolean walkFastCanvas(WorldPoint worldPoint) {
        return walkFastCanvas(worldPoint, true);
    }

    private static boolean walkFastCanvasOnScreenOnly(WorldPoint worldPoint, boolean toggleRun) {
        LocalPoint localPoint = localPointForWorld(worldPoint);
        if (localPoint == null || !Rs2Camera.isTileOnScreen(localPoint)) {
            return false;
        }
        Point canvasPoint = Perspective.localToCanvas(
                Microbot.getClient(),
                localPoint,
                Microbot.getClient().getTopLevelWorldView().getPlane());
        int canvasX = canvasPoint != null ? canvasPoint.getX() : -1;
        int canvasY = canvasPoint != null ? canvasPoint.getY() : -1;
        if (canvasX < 0 || canvasY < 0) {
            return false;
        }

        if (!Rs2Player.toggleRunEnergy(toggleRun)) {
            return false;
        }
        NewMenuEntry entry = new NewMenuEntry()
                .param0(canvasX)
                .param1(canvasY)
                .type(MenuAction.WALK)
                .identifier(0)
                .itemId(0)
                .option("Walk here");

        Microbot.doInvoke(entry,
                new Rectangle(canvasX, canvasY, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
        return true;
    }

    private static LocalPoint localPointForWorld(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return null;
        }
        LocalPoint localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), worldPoint);
        if (Microbot.getClient().getTopLevelWorldView().isInstance() && localPoint == null) {
            localPoint = Rs2LocalPoint.fromWorldInstance(worldPoint);
        }
        return localPoint;
    }

    public static boolean walkFastCanvas(WorldPoint worldPoint, boolean toggleRun) {
        if (worldPoint == null) {
            log.debug("[Walker] walkFastCanvas rejected: null worldPoint");
            return false;
        }
        Rs2Player.toggleRunEnergy(toggleRun);
        Point canv;
        LocalPoint localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), worldPoint);

        if (Microbot.getClient().getTopLevelWorldView().isInstance() && localPoint == null) {
            localPoint = Rs2LocalPoint.fromWorldInstance(worldPoint);
        }

        if (localPoint == null) {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc != null
                    && playerLoc.getPlane() == worldPoint.getPlane()
                    && walkMiniMapToward(worldPoint, playerLoc, 13)) {
                return true;
            }
            log.debug("[Walker] walkFastCanvas localpoint null for {}", worldPoint);
            return false;
        }

        canv = Perspective.localToCanvas(Microbot.getClient(), localPoint, Microbot.getClient().getTopLevelWorldView().getPlane());

        int canvasX = canv != null ? canv.getX() : -1;
        int canvasY = canv != null ? canv.getY() : -1;

        //if the tile is not on screen, use minimap
        if (!Rs2Camera.isTileOnScreen(localPoint) || canvasX < 0 || canvasY < 0) {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc != null
                    && playerLoc.getPlane() == worldPoint.getPlane()
                    && walkMiniMapToward(worldPoint, playerLoc, 13)) {
                return true;
            }
            return Rs2Walker.walkMiniMap(worldPoint);
        }

        NewMenuEntry entry = new NewMenuEntry()
                .param0(canvasX)
                .param1(canvasY)
                .type(MenuAction.WALK)
                .identifier(0)
                .itemId(0)
                .option("Walk here");

        Microbot.doInvoke(entry,
                new Rectangle(canvasX, canvasY, Microbot.getClient().getCanvasWidth(), Microbot.getClient().getCanvasHeight()));
        return true;
    }

    public static WorldPoint walkCanvas(WorldPoint worldPoint) {
        LocalPoint localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), worldPoint);
        if (localPoint == null) {
            log.error("Tried to walkCanvas but localpoint returned null");
            return null;
        }
        Point point = Perspective.localToCanvas(Microbot.getClient(), localPoint, Microbot.getClient().getTopLevelWorldView().getPlane());

        if (point == null) return null;

        Microbot.getMouse().click(point);

        return worldPoint;
    }

    /**
     * Gets the total amount of tiles to travel to destination
     * @param start source
     * @param destination destination
     * @return total amount of tiles
     */
    public static int getTotalTiles(WorldPoint start, WorldPoint destination) {
        if (Rs2PathApi.getPathfinderConfig().getTransports().isEmpty()) {
            Rs2PathApi.getPathfinderConfig().refresh();
        }
        Pathfinder pathfinder = new Pathfinder(Rs2PathApi.getPathfinderConfig(), start, destination);
        pathfinder.run();
        List<WorldPoint> path = pathfinder.getPath();
        if (path.isEmpty() || path.get(path.size() - 1).getPlane() != destination.getPlane()) return Integer.MAX_VALUE;
        // Create a WorldArea centered on the worldPoint by calculating the south-west corner
        WorldPoint pathPoint_SW = new WorldPoint(
                path.get(path.size() - 1).getX() - 2,
                path.get(path.size() - 1).getY() - 2,
                path.get(path.size() - 1).getPlane()
        );
        // Create a WorldArea centered on the worldPoint by calculating the south-west corner
        WorldPoint objectPoint_SW = new WorldPoint(
                destination.getX() - 2,
                destination.getY() - 2,
                destination.getPlane()
        );
        WorldArea pathArea = new WorldArea(pathPoint_SW, 5, 5);
        WorldArea objectArea = new WorldArea(objectPoint_SW, 5, 5);
        if (!pathArea.intersectsWith2D(objectArea)) {
            return Integer.MAX_VALUE;
        }

        return path.size();
    }

    /**
     * Calculates the total number of tiles from a given path to a destination.
     * This method validates that the path can actually reach the destination by checking
     * if the path's endpoint intersects with the destination area.
     *
     * @param path A list of WorldPoint objects representing the calculated path
     * @param destination The target WorldPoint destination to validate against
     * @return The total number of tiles in the path if valid, or Integer.MAX_VALUE if the path
     *         is empty, on different planes, or doesn't reach the destination
     */
    public static int getTotalTilesFromPath(List<WorldPoint> path, WorldPoint destination) {
        if (path.isEmpty() || path.get(path.size() - 1).getPlane() != destination.getPlane()) return Integer.MAX_VALUE;

        // Create centered WorldAreas instead of corner-based
        WorldPoint pathEndpoint = path.get(path.size() - 1);
        WorldPoint pathSouthWest = new WorldPoint(
                pathEndpoint.getX() - 4,
                pathEndpoint.getY() - 4,
                pathEndpoint.getPlane()
        );
        WorldArea pathArea = new WorldArea(pathSouthWest, 8, 8);

        WorldPoint destSouthWest = new WorldPoint(
                destination.getX() - 4,
                destination.getY() - 4,
                destination.getPlane()
        );
        WorldArea objectArea = new WorldArea(destSouthWest, 8, 8);

        if (!pathArea.intersectsWith2D(objectArea)) {
            return Integer.MAX_VALUE;
        }
        return path.size();
    }

    /**
     * Scores a valid path in tick-equivalent units, replacing transport jumps with their
     * calibrated interaction duration instead of counting every jump as one waypoint.
     */
    public static int getTotalTravelTicksFromPath(List<WorldPoint> path, WorldPoint destination) {
        if (getTotalTilesFromPath(path, destination) == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return TransportCostModel.pathTicks(path, Rs2PathApi.getTransports());
    }

    /**
     * Gets the total amount of tiles to travel to destination
     * @param destination destination
     * @return total amount of tiles
     */
    public static int getTotalTiles(WorldPoint destination) {
        return getTotalTiles(Rs2Player.getWorldLocation(), destination);
    }

    // takes an avg 200-300 ms
    // Used mainly for agility, might have to tweak this for other stuff
    public static boolean canReach(WorldPoint worldPoint, int sizeX, int sizeY, int pathSizeX, int pathSizeY,boolean useBankedItems) {
        boolean originalUseBankItems = Rs2PathApi.getPathfinderConfig().isUseBankItems();
        WorldArea pathArea = null;

        // Create centered WorldArea for the object instead of corner-based
        WorldPoint objectSouthWest = new WorldPoint(
                worldPoint.getX() - (sizeX + 2) / 2,
                worldPoint.getY() - (sizeY + 2) / 2,
                worldPoint.getPlane()
        );
        WorldArea objectArea = new WorldArea(objectSouthWest, sizeX + 2, sizeY + 2);

        try {
            Rs2PathApi.getPathfinderConfig().setUseBankItems(useBankedItems);
            Rs2PathApi.getPathfinderConfig().refresh(worldPoint);
            if (Rs2PathApi.getPathfinderConfig().getTransports().isEmpty()) {
                Rs2PathApi.getPathfinderConfig().refresh(worldPoint);
            }
            Pathfinder pathfinder = new Pathfinder(Rs2PathApi.getPathfinderConfig(), Rs2Player.getWorldLocation(), worldPoint);
            pathfinder.run();

            // Create centered WorldArea for the path endpoint instead of corner-based
            WorldPoint pathEndpoint = pathfinder.getPath().get(pathfinder.getPath().size() - 1);
            WorldPoint pathSouthWest = new WorldPoint(
                    pathEndpoint.getX() - pathSizeX / 2,
                    pathEndpoint.getY() - pathSizeY / 2,
                    pathEndpoint.getPlane()
            );
            pathArea = new WorldArea(pathSouthWest, pathSizeX, pathSizeY);
        } catch (Exception e) {
            log.trace("Exception in canReach: {} - ", e.getMessage(), e);
            return false;
        } finally {
            Rs2PathApi.getPathfinderConfig().setUseBankItems(originalUseBankItems);
            Rs2PathApi.getPathfinderConfig().refresh(worldPoint);
        }
        return pathArea != null ? pathArea.intersectsWith2D(objectArea) : false;
    }
    public static boolean canReach(WorldPoint worldPoint, int sizeX, int sizeY, int pathSizeX, int pathSizeY) {
        return canReach(worldPoint, sizeX, sizeY, pathSizeX, pathSizeY, false);
    }

    // takes an avg 200-300 ms
    // Used mainly for agility, might have to tweak this for other stuff
    public static boolean canReach(WorldPoint worldPoint, int sizeX, int sizeY) {
        return canReach(worldPoint, sizeX, sizeY, 3, 3);
    }

    /**
     * used for quest script interacting with object
     * also used for finding the nearest bank
     * @param worldPoint
     * @return
     */
    public static boolean canReach(WorldPoint worldPoint) {
        return canReach(worldPoint, 2, 2, 2, 2);
    }
    public static boolean canReach(WorldPoint worldPoint, int sizeX, int sizeY, boolean useBankedItems) {
        return canReach(worldPoint, sizeX, sizeY, 2, 2, useBankedItems);
    }
    public static boolean canReach(WorldPoint worldPoint, boolean useBankedItems) {
        return canReach(worldPoint, 2, 2, 2, 2, useBankedItems);
    }
    public static boolean canReach(WorldPoint worldPoint, int sizeX, int sizeY, boolean useBankedItems, int pathSizeX, int pathSizeY) {
        return canReach(worldPoint, sizeX, sizeY, pathSizeX, pathSizeY, useBankedItems);
    }


    /**
     * Retrieves the walk path from the player's current location to the specified target location.
     * @param start The starting `WorldPoint` from which the path should be calculated.
     * @param target The target `WorldPoint` to which the path should be calculated.
     * @return A list of `WorldPoint` objects representing the path from the player's current location to the target.
     */
    public static List<WorldPoint> getWalkPath(WorldPoint start, WorldPoint target) {
        long startTime = System.nanoTime();
        Rs2PathApi.getPathfinderConfig().refresh(target);
        long pathfinderStartTime = System.nanoTime();
        Pathfinder pathfinderLocal = new Pathfinder(Rs2PathApi.getPathfinderConfig(), start, target);
        pathfinderLocal.run();
        List<WorldPoint> path = pathfinderLocal.getPath();
        long pathfinderEndTime = System.nanoTime();
        long totalEndTime = System.nanoTime();
        double configTimeMs = (pathfinderStartTime - startTime) / 1_000_000.0;
        double pathfinderTimeMs = (pathfinderEndTime - pathfinderStartTime) / 1_000_000.0;
        double totalTimeMs = (totalEndTime - startTime) / 1_000_000.0;

        StringBuilder performanceLog = new StringBuilder();
        performanceLog.append("getWalkPath Performance: ")
                .append("Config: ").append(String.format("%.2f ms", configTimeMs))
                .append(", Pathfinder: ").append(String.format("%.2f ms", pathfinderTimeMs))
                .append(", Total: ").append(String.format("%.2f ms", totalTimeMs))
                .append(" | Path: ").append(start).append(" -> ").append(target)
                .append(" (").append(path.size()).append(" waypoints)");

        log.debug(performanceLog.toString());

        return path;
    }
    /**
     * Retrieves the walk path from the player's current location to the specified target location.
     *
     * @param target The target `WorldPoint` to which the path should be calculated.
     * @return A list of `WorldPoint` objects representing the path from the player's current location to the target.
     */
    public static List<WorldPoint> getWalkPath(WorldPoint target) {
        return getWalkPath(Rs2Player.getWorldLocation(), target);
    }

    /**
     * Retrieves all transports found along the given path starting from a specific index.
     * Uses the default preferred transport type of TELEPORTATION_ITEM.
     *
     * @param path A list of WorldPoint objects representing the path to analyze
     * @param indexOfStartPoint The starting index in the path to begin searching for transports
     * @return A list of Transport objects found along the path, prioritizing teleportation items
     */
    public static List<Transport> getTransportsForPath(List<WorldPoint> path, int indexOfStartPoint) {
        return getTransportsForPath(path, indexOfStartPoint, TransportType.TELEPORTATION_ITEM, false);
    }

    /**
     * Retrieves all transports found along the given path starting from a specific index.
     * Analyzes the path for available transport options, prioritizing the specified transport type.
     *
     * This method examines each point in the path starting from the given index and identifies
     * available transport options (teleportation items, spells, objects, etc.) that can be used
     * to optimize travel. Transport types are sorted with teleportation items getting highest priority.
     *
     * @param path A list of WorldPoint objects representing the path to analyze
     * @param indexOfStartPoint The starting index in the path to begin searching for transports
     * @param prefTransportType The preferred transport type to prioritize in the search
     * @return A list of Transport objects found along the path, sorted by transport type priority
     */
    public static List<Transport> getTransportsForPath(List<WorldPoint> path, int indexOfStartPoint, TransportType prefTransportType) {
        return getTransportsForPath(path, indexOfStartPoint, prefTransportType, false);
    }

    /**
     * Retrieves all transports found along the given path starting from a specific index.
     * Analyzes the path for available transport options, prioritizing the specified transport type.
     * This version applies filtering and requirement setup for transports that require items.
     *
     * This method examines each point in the path starting from the given index and identifies
     * available transport options (teleportation items, spells, objects, etc.) that can be used
     * to optimize travel. Transport types are sorted with teleportation items getting highest priority.
     *
     * @param path A list of WorldPoint objects representing the path to analyze
     * @param indexOfStartPoint The starting index in the path to begin searching for transports
     * @param prefTransportType The preferred transport type to prioritize in the search
     * @param applyFiltering Whether to apply transport filtering and requirement setup
     * @return A list of Transport objects found along the path, sorted by transport type priority
     */
    public static List<Transport> getTransportsForPath(List<WorldPoint> path, int indexOfStartPoint, TransportType prefTransportType, boolean applyFiltering) {
        List<Transport> transportList = new ArrayList<>();
        if (path == null || path.isEmpty() || indexOfStartPoint < 0 || indexOfStartPoint >= path.size()) {
            return transportList;
        }
        Map<WorldPoint, Integer> pathFirstIndex = buildPathFirstIndex(path);
        int currentIndex = indexOfStartPoint;

        // Loop through the path until the end
        while (currentIndex < path.size()) {
            WorldPoint currentPoint = path.get(currentIndex);
            // Get any transports that start at this point (or keyed by this point)
            Set<Transport> transportsAtPoint = Rs2PathApi.getTransports().get(currentPoint);
            if (transportsAtPoint == null || transportsAtPoint.isEmpty()) {
                currentIndex++;
                continue;
            }
            boolean foundTransport = false;
            // sort by type to prioritize teleportation items first, then other types
            List<Transport> orderedTransports = new ArrayList<>(transportsAtPoint);
            orderedTransports.sort(Comparator.comparing(Transport::getType, (type1, type2) -> {
                // sort teleportation items by preference transport type for the current path point.
                if (type1 == prefTransportType && type2 != prefTransportType) {
                    return -1;
                }
                if (type2 == prefTransportType && type1 != prefTransportType) {
                    return 1;
                }
                // For all other types, use natural enum ordering
                return type1.compareTo(type2);
            }));
            // Iterate over each available transport
            for (Transport transport : orderedTransports) {

                // Special handling for teleportation-like transports (originless)
                // NOTE: Leagues "Area" teleports are injected as SEASONAL_TRANSPORT with null origin.
				boolean originlessTeleport = TransportType.isTeleport(
						transport.getType(), transport.getOrigin());

				if (originlessTeleport)
                {
                    // For teleportation, we assume origin is null and simply check if the destination exists in the path.
                    Integer destIndex = pathFirstIndex.get(transport.getDestination());
                    if (destIndex != null) {
                        transportList.add(transport);
                        // Advance the current index to the destination tile (or at least one forward)
                        currentIndex = destIndex > currentIndex ? destIndex : currentIndex + 1;
                        foundTransport = true;
                        break;
                    }
                }

                // For non-teleportation transports (or if teleportation had a valid origin, though typically null):
                Collection<WorldPoint> originPoints = resolvePathTransportOrigins(
                        transport, pathFirstIndex,
                        () -> localInstancePoints(transport.getOrigin()));

                for (WorldPoint origin : originPoints) {
                    // For non-teleportation transports, ensure both origin and destination exist in the path
                    // and that the destination comes after the origin.
                    Integer indexOfDestinationValue = pathFirstIndex.get(transport.getDestination());
                    int indexOfDestination = indexOfDestinationValue != null ? indexOfDestinationValue : -1;
					if (!originlessTeleport) {
                        Integer indexOfOriginValue = pathFirstIndex.get(transport.getOrigin());
                        int indexOfOrigin = indexOfOriginValue != null ? indexOfOriginValue : -1;
                        if (indexOfOrigin == -1 || indexOfDestination == -1 || indexOfDestination < indexOfOrigin) {
                            continue;
                        }
                    }

                    // If the current path point equals the transport's origin then add it.
                    if (currentPoint.equals(origin)) {
                        transportList.add(transport);
                        currentIndex = indexOfDestination > currentIndex ? indexOfDestination : currentIndex + 1;
                        foundTransport = true;
                        break;
                    }
                }
                if (foundTransport) {
                    break;
                }
            }

            if (!foundTransport) {
                currentIndex++;
            }
        }

        WebWalkLog.bankPathTransportsDebug(transportList.size(), path.get(0), path.get(path.size() - 1));

        // Apply filtering and requirement setup if requested
        if (applyFiltering) {
            transportList = applyTransportFiltering(transportList);
        }

        return transportList;
    }

    static Collection<WorldPoint> resolvePathTransportOrigins(
            Transport transport,
            Map<WorldPoint, Integer> pathFirstIndex,
            Supplier<Collection<WorldPoint>> localInstanceOrigins) {
        if (transport.getOrigin() == null) {
            return Collections.singleton(null);
        }
        if (pathFirstIndex.containsKey(transport.getOrigin())) {
            return Collections.singleton(transport.getOrigin());
        }
        return localInstanceOrigins.get();
    }

    private static Map<WorldPoint, Integer> buildPathFirstIndex(List<WorldPoint> path) {
        Map<WorldPoint, Integer> pathFirstIndex = new HashMap<>(path.size());
        for (int i = 0; i < path.size(); i++) {
            pathFirstIndex.putIfAbsent(path.get(i), i);
        }
        return pathFirstIndex;
    }

    /**
     * Applies transport filtering and requirement setup for transport items.
     * This method filters transports to only include those that require items and
     * sets up item requirements for fairy rings.
     *
     * @param transports The list of transports to filter and process
     * @return The filtered and processed list of transports
     */
    private static List<Transport> applyTransportFiltering(List<Transport> transports) {
        return transports.stream()
				.filter(Rs2WalkerBankingPlanner::requiresBankPlanning)
                .peek(t -> {
                    // Set fairy ring requirements if not already set
                    if (t.getType() == TransportType.FAIRY_RING &&
                            ((t.getItemIdRequirements() == null || t.getItemIdRequirements().isEmpty()) ) &&  Microbot.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE)  != 1) {
                        t.setItemIdRequirements(Set.of(Set.of(
                                ItemID.DRAMEN_STAFF,
                                ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF
                        )));
                    }
                })
                .collect(Collectors.toList());
    }



    public static boolean isCloseToRegion(int distance, int regionX, int regionY) {
        WorldPoint worldPoint = WorldPoint.fromRegion(Rs2Player.getWorldLocation().getRegionID(),
                regionX,
                regionY,
                Microbot.getClient().getTopLevelWorldView().getPlane());

        return worldPoint.distanceTo(Rs2Player.getWorldLocation()) < distance;
    }

    public static int distanceToRegion(int regionX, int regionY) {
        WorldPoint worldPoint = WorldPoint.fromRegion(Rs2Player.getWorldLocation().getRegionID(),
                regionX,
                regionY,
                Microbot.getClient().getTopLevelWorldView().getPlane());

        return worldPoint.distanceTo(Rs2Player.getWorldLocation());
    }



    /** Applies a rockfall handling outcome to walker route state (facade side of the extraction). */
    /** Stateless obstacle resolver for the P2 unified dispatch (rockfall mining on a planned edge). */
    private static final MineableResolver MINEABLE_RESOLVER = new MineableResolver();
	private static final MineableRouteScanner MINEABLE_ROUTE_SCANNER =
			new MineableRouteScanner(MINEABLE_RESOLVER);
	private static final OrdinaryDoorRouteScanner ORDINARY_DOOR_ROUTE_SCANNER =
			new OrdinaryDoorRouteScanner();
	private static final AdjacentTransportRouteScanner ADJACENT_TRANSPORT_ROUTE_SCANNER =
			new AdjacentTransportRouteScanner();
	private static final CatalogTransitionRouteScanner CATALOG_TRANSITION_ROUTE_SCANNER =
			new CatalogTransitionRouteScanner();
	private static final SimpleTeleportRouteScanner SIMPLE_TELEPORT_ROUTE_SCANNER =
			new SimpleTeleportRouteScanner();
	private static final ItemTeleportRouteScanner ITEM_TELEPORT_ROUTE_SCANNER =
			new ItemTeleportRouteScanner();
	private static final NpcTransportRouteScanner NPC_TRANSPORT_ROUTE_SCANNER =
			new NpcTransportRouteScanner();
	private static final NpcDialogueTransportRouteScanner NPC_DIALOGUE_TRANSPORT_ROUTE_SCANNER =
			new NpcDialogueTransportRouteScanner();
	private static final CharterShipRouteScanner CHARTER_SHIP_ROUTE_SCANNER =
			new CharterShipRouteScanner();
	private static final FairyRingRouteScanner FAIRY_RING_ROUTE_SCANNER =
			new FairyRingRouteScanner();
	private static final SpiritTreeRouteScanner SPIRIT_TREE_ROUTE_SCANNER =
			new SpiritTreeRouteScanner();
	private static final GnomeGliderRouteScanner GNOME_GLIDER_ROUTE_SCANNER =
			new GnomeGliderRouteScanner();
	private static final QuetzalRouteScanner QUETZAL_ROUTE_SCANNER =
			new QuetzalRouteScanner();
	private static final TeleportationLeverRouteScanner TELEPORTATION_LEVER_ROUTE_SCANNER =
			new TeleportationLeverRouteScanner();
	private static final WildernessDitchRouteScanner WILDERNESS_DITCH_ROUTE_SCANNER =
			new WildernessDitchRouteScanner();
	private static final JungleObstacleRouteScanner JUNGLE_OBSTACLE_ROUTE_SCANNER =
			new JungleObstacleRouteScanner();
	private static final CanoeRouteScanner CANOE_ROUTE_SCANNER = new CanoeRouteScanner();
	private static final MinecartRouteScanner MINECART_ROUTE_SCANNER =
			new MinecartRouteScanner();
	private static final TeleportationPortalRouteScanner TELEPORTATION_PORTAL_ROUTE_SCANNER =
			new TeleportationPortalRouteScanner();
	private static final MinigameTeleportRouteScanner MINIGAME_TELEPORT_ROUTE_SCANNER =
			new MinigameTeleportRouteScanner();
	private static final MagicMushtreeRouteScanner MAGIC_MUSHTREE_ROUTE_SCANNER =
			new MagicMushtreeRouteScanner();
	private static final HotAirBalloonRouteScanner HOT_AIR_BALLOON_ROUTE_SCANNER =
			new HotAirBalloonRouteScanner();

    /**
     * Single-edge rockfall resolution for {@code rawPath[i] -> rawPath[i+1]} via {@link MineableResolver}
     * (the former {@code handleRockfall(rawPath, i)} in the unified model). {@code NOT_APPLICABLE} when
     * {@code i} is the last index, matching the old {@code index == size-1} guard.
     */
    private static ObstacleResolution resolveRockfallOnEdge(List<WorldPoint> rawPath, int i) {
        if (rawPath == null || i < 0 || i + 1 >= rawPath.size()) {
            return ObstacleResolution.notApplicable();
        }
        return MINEABLE_RESOLVER.resolve(new PlannedEdge(rawPath.get(i), rawPath.get(i + 1)), null, null);
    }

    private static int rawAnchorIndexForPathPosition(List<WorldPoint> rawPath,
                                                     List<WorldPoint> path,
                                                     WorldPoint playerLoc) {
        int closestPathIdx = getClosestTileIndex(path, playerLoc);
        int[] smoothedToRaw = mapSmoothedToRaw(path, rawPath);
        int rawAnchorIndex = rawIndexForSmoothedIndex(closestPathIdx, smoothedToRaw, rawPath);
        return rawPathForwardAnchorIndex(rawPath, playerLoc, rawAnchorIndex);
    }

    private static boolean clickRouteBackedShortWalk(List<WorldPoint> rawPath,
                                                     WorldPoint end,
                                                     WorldPoint playerLoc,
                                                     int maxEuclidean,
                                                     int rawAnchorIndex) {
        boolean directTargetInRange = shouldAttemptDirectMinimapTarget(end, playerLoc, maxEuclidean);
        if (directTargetInRange && walkMiniMap(end)) {
            return true;
        }

        // distanceTo() is Chebyshev distance, while the minimap clip is effectively circular.
        // A diagonal endpoint can therefore pass the short-walk gate while being well outside the
        // clip. In that case select a normal forward raw-route point immediately instead of first
        // issuing a predictably rejected endpoint click and reporting the continuation as a fallback.
        WorldPoint routeTarget = findFurthestVisibleKnownRawPathPoint(
                rawPath, playerLoc, maxEuclidean, rawAnchorIndex);
        if (routeTarget != null
                && !routeTarget.equals(playerLoc)
                && !routeTarget.equals(end)
                && walkMiniMap(routeTarget)) {
            if (directTargetInRange) {
                log.debug("[Walker] Direct short-walk target {} was outside the minimap clip; continuing via route {}",
                        end, routeTarget);
            }
            return true;
        }
        return walkFastCanvasOnScreenOnly(end, true);
    }

    static boolean shouldAttemptDirectMinimapTarget(WorldPoint target,
                                                    WorldPoint playerLoc,
                                                    int maxEuclidean) {
        if (target == null || playerLoc == null || maxEuclidean < 0
                || target.getPlane() != playerLoc.getPlane()) {
            return false;
        }
        long dx = (long) target.getX() - playerLoc.getX();
        long dy = (long) target.getY() - playerLoc.getY();
        long radius = maxEuclidean;
        return dx * dx + dy * dy <= radius * radius;
    }

    private static boolean hasPendingExplicitTransportStepBeforeArrival(List<WorldPoint> path,
                                                                        WorldPoint target,
                                                                        int distance) {
        return hasPendingRouteStepBeforeArrival(path, target, distance, i -> isCatalogBackedTransportSegment(path, i));
    }

    static boolean hasPendingRouteStepBeforeArrival(List<WorldPoint> path,
                                                    WorldPoint target,
                                                    int distance,
                                                    java.util.function.IntPredicate routeStepAtIndex) {
        if (path == null || path.size() < 2 || routeStepAtIndex == null) {
            return false;
        }

        for (int i = 0; i < path.size() - 1; i++) {
            WorldPoint point = path.get(i);
            if (target != null && point != null && point.distanceTo(target) <= distance) {
                return false;
            }
            if (routeStepAtIndex.test(i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean localRouteDetoursFromComputedRoute(List<WorldPoint> rawPath,
                                                              WorldPoint end,
                                                              int directClickMaxDistance) {
        if (rawPath == null || rawPath.size() < 2 || end == null) {
            return false;
        }

        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (playerLoc == null || playerLoc.getPlane() != end.getPlane()) {
            return false;
        }

        int rawStart = getClosestTileIndex(rawPath, playerLoc);
        if (rawStart < 0 || rawStart >= rawPath.size() - 1) {
            return false;
        }

        int rawEnd = -1;
        for (int i = rawStart; i < rawPath.size(); i++) {
            WorldPoint point = rawPath.get(i);
            if (point == null || point.getPlane() != end.getPlane()) {
                break;
            }
            if (point.equals(end)) {
                rawEnd = i;
                break;
            }
        }
        if (rawEnd < 0) {
            return false;
        }

        int computedSteps = rawEnd - rawStart;
        if (computedSteps <= 0) {
            return false;
        }

        final int detourSlackTiles = 4;
        int searchDistance = Math.max(directClickMaxDistance * 3, computedSteps + detourSlackTiles + 1);
        Integer localSteps = Rs2Tile.getReachableTilesFromTile(playerLoc, searchDistance).get(end);
        return localSteps == null || localSteps > computedSteps + detourSlackTiles;
    }

    private static boolean hasPendingDoorLikeSceneObjectBeforeDirectClick(List<WorldPoint> rawPath,
                                                                          List<WorldPoint> path,
                                                                          WorldPoint playerLoc,
                                                                          int directClickMaxDistance) {
        List<WorldPoint> route = rawPath != null && rawPath.size() >= 2 ? rawPath : path;
        if (route == null || route.size() < 2 || playerLoc == null) {
            return false;
        }

        int closest = getClosestTileIndex(route, playerLoc);
        if (closest < 0 || closest >= route.size()) {
            return false;
        }

        int maxEdges = 12;
        int radius = Math.max(3, directClickMaxDistance + 2);
        int start = Math.max(0, closest - 2);
        int endExclusive = Math.min(route.size() - 1, start + maxEdges);
        for (int i = start; i < endExclusive; i++) {
            WorldPoint from = route.get(i);
            WorldPoint to = route.get(i + 1);
            if (from == null || to == null) {
                continue;
            }
            if (from.getPlane() != playerLoc.getPlane() || to.getPlane() != playerLoc.getPlane()) {
                break;
            }
            if (from.distanceTo2D(playerLoc) > radius && to.distanceTo2D(playerLoc) > radius) {
                break;
            }
            if (isCatalogBackedTransportSegment(route, i) && !isDoorLikeCatalogTransportSegment(route, i)) {
                continue;
            }
            if (hasDoorLikeSceneObjectOnSegment(from, to, playerLoc, radius)) {
                return true;
            }
        }
        return false;
    }

    // rawIndexForSmoothedIndex (pure) moved to geometry/WalkerPathGeometry (P1); wrapper supplies the lazy
    // closest-index fallback (only used when the smoothedToRaw table can't map the index).
    private static int rawIndexForSmoothedIndex(int smoothedIdx, int[] smoothedToRaw, List<WorldPoint> rawPath) {
        return WalkerPathGeometry.rawIndexForSmoothedIndex(smoothedIdx, smoothedToRaw, rawPath,
                () -> getClosestTileIndex(rawPath));
    }

    /**
     * Scene snapshot scoped to one {@link #handleNearbyRawPathSceneObjects} pass.
     * <p>
     * Door probing previously issued up to four bounded scene queries <em>per probe</em>, and a scan
     * walks ~12 raw indices x 2 offsets x several probes — hundreds of client-thread round trips.
     * Measured live at 4564ms of a 5539ms scan that resolved nothing. Taking one bounded snapshot up
     * front and matching probes against it in memory collapses that to two queries per scan.
     * <p>
     * Null outside a raw scan, so every other {@code handleDoors} caller keeps the original
     * query-per-probe behaviour.
     */
    private static volatile List<WallObject> rawScanWallSnapshot = null;
    private static volatile List<GameObject> rawScanGameObjectSnapshot = null;
    /** Immutable locations copied on the client thread for off-thread snapshot filtering. */
    private static Map<TileObject, WorldPoint> rawScanDoorLocationSnapshot = null;
    /** Object definitions and segment matches are stable for one immutable scene snapshot. */
    private static Map<TileObject, Optional<ObjectComposition>> rawScanDoorCompositionCache = null;
    private static Map<String, Optional<TileObject>> rawScanDoorSegmentCache = null;
    /** Scan-scoped memo for the segment-independent door-candidate test (see DoorProbeContext). */
    private static Map<TileObject, Boolean> rawScanDoorEligibilityCache = null;

    /** Wraps the current scan-scoped probe caches for the extracted door-probe logic. */
    private static DoorProbeContext doorProbeContext() {
        return new DoorProbeContext(rawScanWallSnapshot, rawScanGameObjectSnapshot,
                rawScanDoorLocationSnapshot, rawScanDoorCompositionCache, rawScanDoorSegmentCache,
                rawScanDoorEligibilityCache);
    }
    /** Interaction/edge-resolution wait contained inside {@link #handleDoors}; excluded from probe cost. */
    private static volatile long rawScanDoorInteractionWaitMs = 0L;
    /** Time inside the door segment probe during a raw scan (the actual geometry/snapshot work). */
    private static volatile long rawScanDoorFindMs = 0L;

    /**
     * The door segment probe, timed. "doorProbe" in the slow-scan line is a RESIDUAL — the whole
     * handleDoors call minus the interaction wait — so it silently absorbed the edge-resolution wait,
     * the menu interaction and the post-interaction verification too. Attributing the probe itself is
     * the only way to tell an expensive scan from an expensive wait, and they want opposite fixes.
     */
    private static TileObject findDoorNearSegmentTimed(WorldPoint fromWp, WorldPoint toWp, List<String> doorActions) {
        long startedAt = System.currentTimeMillis();
        try {
            return Rs2DoorProbe.findDoorNearSegment(doorProbeContext(), sessionBlacklistedDoors,
                    recentlyOpenedStationaryDoors, STATIONARY_DOOR_SUPPRESS_MS, fromWp, toWp, doorActions);
        } finally {
            if (rawScanWallSnapshot != null || rawScanGameObjectSnapshot != null) {
                rawScanDoorFindMs += System.currentTimeMillis() - startedAt;
            }
        }
    }

    /**
     * Exact-tile match first, then a one-tile adjacency fallback — the same preference order the
     * previous pair of bounded queries produced.
     */
    private static WallObject resolveProbeWallObject(WorldPoint probe) {
        List<WallObject> snapshot = rawScanWallSnapshot;
        if (snapshot != null) {
            WallObject adjacent = null;
            for (WallObject candidate : snapshot) {
                if (candidate == null) {
                    continue;
                }
                WorldPoint loc = candidate.getWorldLocation();
                if (loc == null) {
                    continue;
                }
                if (loc.equals(probe)) {
                    return candidate;
                }
                if (adjacent == null && loc.distanceTo2D(probe) <= 1) {
                    adjacent = candidate;
                }
            }
            return adjacent;
        }
        WallObject wall = Rs2GameObject.getWallObject(
                o -> probe.equals(tileObjectWorldLocation(o)), probe, 3);
        if (wall == null) {
            wall = Rs2GameObject.getWallObject(o -> {
                WorldPoint location = tileObjectWorldLocation(o);
                return location != null && location.distanceTo2D(probe) <= 1;
            }, probe, 3);
        }
        return wall;
    }

    /** @see #resolveProbeWallObject(WorldPoint) */
    private static TileObject resolveProbeGameObject(WorldPoint probe) {
        List<GameObject> snapshot = rawScanGameObjectSnapshot;
        if (snapshot != null) {
            GameObject adjacent = null;
            for (GameObject candidate : snapshot) {
                if (candidate == null) {
                    continue;
                }
                WorldPoint loc = candidate.getWorldLocation();
                if (loc == null) {
                    continue;
                }
                if (loc.equals(probe)) {
                    return candidate;
                }
                if (adjacent == null && loc.distanceTo2D(probe) <= 1) {
                    adjacent = candidate;
                }
            }
            return adjacent;
        }
        TileObject object = Rs2GameObject.getGameObject(
                o -> probe.equals(tileObjectWorldLocation(o)), probe, 3);
        if (object == null) {
            object = Rs2GameObject.getGameObject(o -> {
                WorldPoint location = tileObjectWorldLocation(o);
                return location != null && location.distanceTo2D(probe) <= 1;
            }, probe, 3);
        }
        return object;
    }

    private static boolean hasDoorCandidateOnRawSegment(List<WorldPoint> rawPath, int index) {
        if (rawPath == null || index < 0 || index >= rawPath.size() - 1) {
            return false;
        }
        if (isCatalogBackedTransportSegment(rawPath, index) && !isDoorLikeCatalogTransportSegment(rawPath, index)) {
            return false;
        }
        boolean isInstance = Microbot.getClient()
                .getTopLevelWorldView()
                .getScene()
                .isInstance();
        WorldPoint rawFrom = rawPath.get(index);
        WorldPoint rawTo = rawPath.get(index + 1);
        WorldPoint fromWp = isInstance ? Rs2WorldPoint.convertInstancedWorldPoint(rawFrom) : rawFrom;
        WorldPoint toWp = isInstance ? Rs2WorldPoint.convertInstancedWorldPoint(rawTo) : rawTo;
        if (fromWp == null || toWp == null || fromWp.getPlane() != toWp.getPlane()) {
            return false;
        }
        List<String> doorActions = List.of("pay-toll", "pick-lock", "walk-through", "go-through", "open", "pass");
        return findDoorNearSegmentTimed(fromWp, toWp, doorActions) != null;
    }

    private static void setRawScanDoorFocus(int index) {
        routeState.rawScanFocusedDoorIdx = index;
        routeState.rawScanFocusedDoorSetAtMs = System.currentTimeMillis();
        routeState.rawScanFocusedDoorAttempts = 0;
    }

    private static boolean shouldUseFocusedRawDoorIndex(List<WorldPoint> rawPath, int rawStartIdx) {
        Integer idx = routeState.rawScanFocusedDoorIdx;
        if (idx == null) {
            return false;
        }
        if (routeState.interimTargetWp != null) {
            return false;
        }
        if (System.currentTimeMillis() - routeState.rawScanFocusedDoorSetAtMs > RAW_SCAN_DOOR_FOCUS_MAX_MS) {
            return false;
        }
        if (routeState.rawScanFocusedDoorAttempts >= RAW_SCAN_DOOR_FOCUS_MAX_ATTEMPTS) {
            return false;
        }
        if (idx < 0 || idx >= rawPath.size() - 1) {
            return false;
        }
        if (rawStartIdx > idx + 1) {
            return false;
        }
        return Math.abs(rawStartIdx - idx) <= 2;
    }

    private static void clearRawScanDoorFocus(String reason) {
        if (routeState.rawScanFocusedDoorIdx != null && debug) {
            walkerDiag("clear raw door focus: %s", reason);
        }
        routeState.rawScanFocusedDoorIdx = null;
        routeState.rawScanFocusedDoorSetAtMs = 0L;
        routeState.rawScanFocusedDoorAttempts = 0;
    }


    private static boolean didCurrentTileTransportProgress(WorldPoint before, WorldPoint expectedDestination, WorldPoint target) {
        return Rs2WalkerTransportAwaits.didCurrentTileTransportProgress(before, expectedDestination, target);
    }

    // Maps each tile on the planned route at/after the player's closest index to its route position.
    // Earliest index wins (putIfAbsent) so the raw path's index space is authoritative when the same
    // tile appears in both the raw and smoothed paths (the smoothed path is a subset of the raw one).
    // Session-local set of door tiles the walker detected as quest/stat-locked after a
    // failed interact. Cleared when the client restarts. Prevents infinite retry loops
    // through the same restricted door when the restriction isn't in restrictions.tsv.
    static final Set<WorldPoint> sessionBlacklistedDoors = ConcurrentHashMap.newKeySet();
    private static final Map<WorldPoint, Long> recentlyOpenedStationaryDoors = new ConcurrentHashMap<>();
    private static final long STATIONARY_DOOR_SUPPRESS_MS = 10_000;
    private static final Map<String, Long> recentDoorAttemptByEdge = new ConcurrentHashMap<>();
    private static final long DOOR_ATTEMPT_EDGE_COOLDOWN_MS = 2_500;
    private static final long DOOR_INTERACTION_GLOBAL_COOLDOWN_MS = 1_800;

    static boolean hasQuestLockKeywords(String text) {
        if (text == null || text.isEmpty()) return false;
        String lc = text.toLowerCase();
        // Phrases that consistently appear on quest/stat-gated doors and gates.
        return lc.contains("quest") || lc.contains("you need to") || lc.contains("you must")
                || lc.contains("you have not") || lc.contains("cannot enter")
                || lc.contains("can't enter") || lc.contains("requires you");
    }

    private static boolean isQuestLockedDoorDialogue() {
        if (!Rs2Dialogue.isInDialogue()) return false;
        return hasQuestLockKeywords(Rs2Dialogue.getDialogueText());
    }

    /**
     * Rank sidestep-recovery candidate tiles by Chebyshev distance to the walk target so
     * the random pick biases toward the goal instead of wandering. Pure function — no
     * dependency on client state; safe to unit-test.
     */
    static List<WorldPoint> rankSidestepTilesToward(Collection<WorldPoint> reachable, WorldPoint target) {
        if (reachable == null || reachable.isEmpty()) return Collections.emptyList();
        return reachable.stream()
                .sorted(Comparator.comparingInt(t -> t.distanceTo(target)))
                .collect(Collectors.toList());
    }

    /**
     * Given a path and a starting index, return the index of the furthest path tile that:
     *  - is on the same plane as {@code path.get(startIdx)}
     *  - is not a transport origin (per {@code isTransportOrigin})
     *  - lies within {@code maxEuclidean} 2D Euclidean distance of {@code playerLoc}
     *
     * <p>Euclidean (not Chebyshev) because the minimap clickable area is a circle: a
     * Chebyshev-bounded cap either wastes reach on cardinal directions (where the circle
     * extends to ~{@code maxEuclidean}) or lets diagonal clicks escape the disk (where
     * Chebyshev-{@code maxEuclidean} is {@code maxEuclidean}·√2 away).
     *
     * <p>If {@code path.get(startIdx)} itself is already beyond reach — which happens
     * when the player has drifted off path and the next smoothed waypoint is out of
     * minimap range — the function scans <em>backward</em> for the latest in-range path
     * tile. Clicking that earlier tile brings the player back onto the path so forward
     * progress can resume; without this, the walker would spam off-minimap clicks
     * against {@code path.get(startIdx)} until the 10-second stall-recalc fires.
     */
    // findFurthestClickableIndex extracted to recovery/RouteRecovery (P1)

    // findFurthestForwardClickableIndex extracted to recovery/RouteRecovery (P1)

    // findForwardRecoveryIndex extracted to recovery/RouteRecovery (P1 walker decomposition)

    // interpolateClickableTarget extracted to recovery/RouteRecovery (P1)

    // clampToEuclideanRadius extracted to recovery/RouteRecovery (P1)

    private static int euclideanSq(WorldPoint a, WorldPoint b) {
        int dx = a.getX() - b.getX();
        int dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    // findReachableTransportOriginAhead extracted to recovery/RouteRecovery as a pure, unit-tested function (P1)


    private static boolean handleDoors(List<WorldPoint> path, int index) {
        return handleDoors(path, index, false);
    }

    private static boolean handleDoors(List<WorldPoint> path, int index, boolean allowSegmentProbe) {
        if (Rs2PathApi.getPathfinder() == null || index >= path.size() - 1) return false;

        // Skip any door whose tile was blacklisted after a prior quest-lock detection —
        // avoid re-triggering the same failed interact loop this session.
        WorldPoint skipFrom = path.get(index);
        WorldPoint skipTo = index + 1 < path.size() ? path.get(index + 1) : null;
        if (sessionBlacklistedDoors.contains(skipFrom)
                || (skipTo != null && sessionBlacklistedDoors.contains(skipTo))) {
            return false;
        }

        List<String> doorActions = List.of("pay-toll", "pick-lock", "walk-through", "go-through", "open", "pass");
        boolean isInstance = Microbot.getClient()
                .getTopLevelWorldView()
                .getScene()
                .isInstance();

        WorldPoint rawFrom = path.get(index);
        WorldPoint rawTo = path.get(index + 1);
        WorldPoint fromWp = isInstance
                ? Rs2WorldPoint.convertInstancedWorldPoint(rawFrom)
                : rawFrom;
        WorldPoint toWp = isInstance
                ? Rs2WorldPoint.convertInstancedWorldPoint(rawTo)
                : rawTo;

        if (isInstance && (toWp == null || fromWp == null)) {
            // Expected inside the PoH when the next tile is a teleport destination
            // (convertInstancedWorldPoint -> fromWorldInstance returns null for tiles
            // that aren't in the current instance chunk). Log path context so
            // unexpected occurrences outside that case can be diagnosed.
            log.debug("[Walker] handleDoors: POH/instance conversion returned null (rawFrom={} fromWp={} rawTo={} toWp={}) idx={}/{} — skipping door check",
                    rawFrom, fromWp, rawTo, toWp, index, path.size());
            return false;
        }

        // Cross-plane path steps are always transports (stairs, ladders, trapdoors) —
        // door probes on mismatched planes would emit wrong-plane corner coordinates
        // and the plane-guard below would reject them anyway; these are transport edges.
        if (fromWp.getPlane() != toWp.getPlane()) {
            return false;
        }

        if (isCatalogBackedTransportSegment(path, index) && !isDoorLikeCatalogTransportSegment(path, index)) {
            return false;
        }

        if (recentlyOpenedStationaryDoorOnSegment(fromWp, toWp)) {
            return false;
        }

        // A broad raw scan already owns immutable wall/game-object snapshots. Resolve the
        // segment directly from them instead of running the probe loop, which repeatedly
        // requested the same object definitions on the client thread for adjacent raw edges.
        if (allowSegmentProbe
                && (rawScanWallSnapshot != null || rawScanGameObjectSnapshot != null)) {
            TileObject snapshotDoor = findDoorNearSegmentTimed(fromWp, toWp, doorActions);
            if (snapshotDoor == null) {
                return false;
            }
            if (snapshotDoor instanceof WallObject) {
                return tryHandleDoorObject(snapshotDoor, snapshotDoor.getWorldLocation(),
                        fromWp, toWp, doorActions, true);
            }
        }

        for (int offset = 0; offset <= 1; offset++) {
            int doorIdx = index + offset;
            if (doorIdx >= path.size()) continue;

            WorldPoint rawDoorWp = path.get(doorIdx);
            WorldPoint doorWp = isInstance
                    ? Rs2WorldPoint.convertInstancedWorldPoint(rawDoorWp)
                    : rawDoorWp;

            List<WorldPoint> probes = Rs2DoorAheadResolver.buildSegmentProbes(fromWp, toWp, doorWp);

            for (WorldPoint probe : probes) {
                if (recentlyOpenedStationaryDoorOnSegment(fromWp, toWp)) {
                    return false;
                }
                boolean adjacentToPath = probe.distanceTo(fromWp) <= 1 || probe.distanceTo(toWp) <= 1;
                WorldPoint playerLoc = Rs2Player.getWorldLocation();
                if (!adjacentToPath || playerLoc == null || !Objects.equals(probe.getPlane(), playerLoc.getPlane())) continue;

                // WallObjects can report their world location as an adjacent tile depending on
                // orientation / scene representation. Use exact match first, then allow a small
                // adjacency fallback so door handling triggers reliably.
                WallObject wall = resolveProbeWallObject(probe);

                TileObject object = (wall != null) ? wall : resolveProbeGameObject(probe);
                if (object == null) continue;
                if (!Rs2DoorGeometry.isDoorInteractionWithinRange(object, probe, fromWp, toWp, playerLoc, HANDLER_RANGE)) {
                    Telemetry.recordDoorReject("door-out-of-range");
                    continue;
                }
                if (Rs2DoorProbe.isCatalogTransportObject(object) && !Rs2DoorDetection.isDoorLikeSceneObject(object)) {
                    Telemetry.recordDoorReject("catalog-transport-object");
                    continue;
                }

                ObjectComposition baseComp = Rs2GameObject.convertToObjectComposition(object);
                ObjectComposition comp = Rs2DoorDetection.resolveCompositionForDoorProbe(object);
                if (comp == null) {
                    Telemetry.recordDoorReject("composition-null");
                    continue;
                }
                if (baseComp != null && baseComp.getImpostorIds() != null
                        && !Rs2DoorClassifier.isNullOrPlaceholderObjectName(baseComp.getName())
                        && Rs2DoorClassifier.isNullOrPlaceholderObjectName(comp.getName())) {
                    Telemetry.recordDoorReject("impostor-rejected");
                    continue;
                }
                if (Rs2DoorClassifier.isNullOrPlaceholderObjectName(comp.getName())) {
                    Telemetry.recordDoorReject("name-not-door");
                    continue;
                }

                if (Rs2DoorClassifier.doorCompositionSpecifiesOnlyCloseOrShut(comp)) {
                    Telemetry.recordDoorReject("skip-close-only-open");
                    continue;
                }

                String action = Rs2DoorClassifier.pickWalkDoorAction(comp);
                if (action == null) {
                    Telemetry.recordDoorReject("no-walk-action");
                    continue;
                }
                if (Rs2DoorClassifier.doorActionPriorityIndex(action) == Integer.MAX_VALUE) {
                    Telemetry.recordDoorReject("non-standard-door-action");
                    continue;
                }

                boolean found = false;

                final String name = comp.getName();

                if (object instanceof WallObject) {
                    // Validate the door's ACTUAL blocked edge against the segment, not the probe
                    // tile. The probe can sit a tile off the wall (adjacency fallback above), and the
                    // old probe-orientation check plus the pathTouchesBothEnds shortcut opened doors
                    // merely beside the path. isDoorOnSegment walks the segment against the wall's
                    // real edge, matching the GameObject branch and findDoorNearSegment.
                    if (Rs2DoorGeometry.isDoorOnSegment(object, fromWp, toWp)) {
                        log.debug("Found WallObject door - name {} with action {} at {} - from {} to {}", name, action, probe, fromWp, toWp);
                        found = true;
                    } else {
                        Telemetry.recordDoorReject("orient-mismatch");
                    }
                } else {
                    if (Rs2DoorGeometry.isDoorOnSegment(object, fromWp, toWp)) {
                        log.debug("Found GameObject door - name {} with action {} at {} - from {} to {}", name, action, probe, fromWp, toWp);
                        found = true;
                    } else {
                        Telemetry.recordDoorReject("gameobject-segment-mismatch");
                    }
                }

                if (found) {
                    if (!handleDoorException(object, action)) {
                        if (shouldThrottleDoorAttempt(probe, fromWp, toWp)) {
                            WebWalkLog.spInfo("door_attempt_throttled | mode=segment-door probe={} from={} to={}",
                                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
                            return false;
                        }
                        if (shouldThrottleGlobalDoorInteraction()) {
                            WebWalkLog.spInfo("door_global_await | mode=segment-door probe={} from={} to={}",
                                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
                            return false;
                        }
                        if (doorInteractionDeferredForMovement(probe)) {
                            WebWalkLog.spInfo("door_interact_deferred | reason=moving mode=segment-door probe={} from={} to={}",
                                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
                            return false;
                        }
                        markDoorAttempt(probe, fromWp, toWp);
                        markGlobalDoorInteractionCooldown();
                        WorldPoint posBefore = Rs2Player.getWorldLocation();
                        boolean interacted;
                        try {
                            interacted = Rs2GameObject.interact(object, action);
                        } catch (Exception ex) {
                            WebWalkLog.spInfo("door_interact_exception | mode=segment-door probe={} from={} to={} ex={}",
                                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp), ex.getClass().getSimpleName());
                            return false;
                        }
                        if (!interacted) {
                            WebWalkLog.spInfo("door_interact_failed | mode=segment-door probe={} from={} to={}",
                                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
                            return false;
                        }
                        markDoorInteractionSettling(toWp);
                        waitForDoorInteractionProgress(fromWp, toWp);
                        WorldPoint posAfter = Rs2Player.getWorldLocation();
                        boolean traversed = didTraverseInteractedDoor(posBefore, posAfter, probe, fromWp, toWp);
                        if (!traversed && isQuestLockedDoorDialogue()) {
                            String dialogue = Rs2Dialogue.getDialogueText();
                            log.warn("[Walker] Door at {} ({} action={}) appears quest/stat-locked — dialogue=\"{}\" — blacklisting tile, refreshing restrictions, recalculating",
                                    probe, name, action, dialogue);
                            sessionBlacklistedDoors.add(probe);
                            Rs2Dialogue.clickContinue();
                            if (Rs2PathApi.getPathfinderConfig() != null) {
                                Rs2PathApi.getPathfinderConfig().refresh();
                            }
                            recalculatePath();
                            // Resolved by rerouting; return before the wrong-traversal branch so a
                            // quest/skill-locked door is never learned as a blocked edge (it unlocks when the
                            // requirement is met). Matches the tryHandleDoorObject quest-locked path.
                            return true;
                        }
                        if (!traversed) {
                            if (shouldBlacklistDoorAfterWrongTraversal(posBefore, posAfter, fromWp, toWp, Rs2Player.isMoving())) {
                                sessionBlacklistedDoors.add(probe);
                                log.warn("[Walker] Blacklisting door after wrong traversal: door={} from={} to={} before={} after={}",
                                        probe, fromWp, toWp, posBefore, posAfter);
                                // Wrong-traversal is a stable map property (one-way / mis-encoded door geometry),
                                // so persist it as a learned block that survives restarts and reroutes future paths.
                                // (Quest/skill-locked doors take the isQuestLockedDoorDialogue() branch above and are
                                // deliberately NOT learned — they unlock when the requirement is met.)
                                if (Rs2PathApi.getPathfinderConfig() != null) {
                                    Rs2PathApi.getPathfinderConfig().learnBlockedEdge(fromWp, toWp,
                                            "wrong-traversal door @ " + compactWorldPoint(probe));
                                }
                            }
                            if (doorStillHasAction(probe, fromWp, toWp, doorActions, action)) {
                                log.debug("[Walker] Door interaction did not traverse; action still present at {} ({} -> {})",
                                        probe, fromWp, toWp);
                            } else {
                                markStationaryDoorOpened(probe);
                                if (tryDoorEdgeCrossNudge(fromWp, toWp, currentTarget)) {
                                    markNearbyDoorFamilyOpened(object, probe, action, SEGMENT_DOOR_FAMILY_MARK_RADIUS);
                                    return true;
                                }
                            }
                            return false;
                        }
                        markStationaryDoorOpened(probe);
                        markNearbyDoorFamilyOpened(object, probe, action, SEGMENT_DOOR_FAMILY_MARK_RADIUS);
                    }
                    return true;
                }
            }
        }

        TileObject nearbyDoor = allowSegmentProbe ? findDoorNearSegmentTimed(fromWp, toWp, doorActions) : null;
        if (nearbyDoor != null && tryHandleDoorObject(nearbyDoor, nearbyDoor.getWorldLocation(), fromWp, toWp, doorActions, true)) {
            return true;
        }

        return false;
    }




    private static boolean tryHandleDoorObject(TileObject object, WorldPoint probe, WorldPoint fromWp, WorldPoint toWp,
                                               List<String> doorActions, boolean allowSegmentProbe) {
        if (object == null || probe == null) return false;
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (!Rs2DoorGeometry.isDoorInteractionWithinRange(object, probe, fromWp, toWp, playerLoc, HANDLER_RANGE)) {
            return false;
        }
        if (Rs2DoorProbe.isCatalogTransportObject(object) && !Rs2DoorDetection.isDoorLikeSceneObject(object)) {
            return false;
        }

        ObjectComposition comp = Rs2DoorProbe.resolveDoorComposition(doorProbeContext(), object);
        if (!Rs2DoorClassifier.isDoorComposition(comp, doorActions)) return false;

        String action = Rs2DoorClassifier.getDoorAction(comp, doorActions);
        if (action == null) return false;

        boolean found = false;
        final String name = comp.getName();

        if (object instanceof WallObject) {
            int orientation = Rs2DoorGeometry.wallOrientations((WallObject) object)[0];

            if (searchNeighborPoint(orientation, probe, fromWp)
                    || searchNeighborPoint(orientation, probe, toWp)
                    || (allowSegmentProbe && Rs2DoorGeometry.wallDoorTouchesSegment((WallObject) object, fromWp, toWp))) {
                log.debug("Found WallObject door - name {} with action {} at {} - from {} to {}", name, action, probe, fromWp, toWp);
                found = true;
            }
        } else if (name != null && name.toLowerCase().contains("door")) {
            if (Rs2DoorGeometry.isDoorOnSegment(object, fromWp, toWp)) {
                log.debug("Found GameObject door - name {} with action {} at {} - from {} to {}", name, action, probe, fromWp, toWp);
                found = true;
            }
        }

        if (!found) return false;

        if (handleDoorException(object, action)) {
            return true;
        }

        if (shouldThrottleDoorAttempt(probe, fromWp, toWp)) {
            WebWalkLog.spInfo("door_attempt_throttled | mode=segment-probe probe={} from={} to={}",
                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
            return false;
        }
        if (shouldThrottleGlobalDoorInteraction()) {
            WebWalkLog.spInfo("door_global_await | mode=segment-probe probe={} from={} to={}",
                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
            return false;
        }
        if (doorInteractionDeferredForMovement(probe)) {
            WebWalkLog.spInfo("door_interact_deferred | reason=moving mode=segment-probe probe={} from={} to={}",
                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
            return false;
        }
        markDoorAttempt(probe, fromWp, toWp);
        markGlobalDoorInteractionCooldown();
        WorldPoint posBefore = Rs2Player.getWorldLocation();
        boolean interacted;
        try {
            interacted = Rs2GameObject.interact(object, action);
        } catch (Exception ex) {
            WebWalkLog.spInfo("door_interact_exception | mode=segment-probe probe={} from={} to={} ex={}",
                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp), ex.getClass().getSimpleName());
            return false;
        }
        if (!interacted) {
            WebWalkLog.spInfo("door_interact_failed | mode=segment-probe probe={} from={} to={}",
                    compactWorldPoint(probe), compactWorldPoint(fromWp), compactWorldPoint(toWp));
            return false;
        }
        markDoorInteractionSettling(toWp);
        waitForDoorInteractionProgress(fromWp, toWp);
        WorldPoint posAfter = Rs2Player.getWorldLocation();
        boolean traversed = didTraverseInteractedDoor(posBefore, posAfter, probe, fromWp, toWp);
        if (traversed) {
            markStationaryDoorOpened(probe);
            markNearbyDoorFamilyOpened(object, probe, action, SEGMENT_DOOR_FAMILY_MARK_RADIUS);
            return true;
        }
        if (shouldBlacklistDoorAfterWrongTraversal(posBefore, posAfter, fromWp, toWp, Rs2Player.isMoving())) {
            sessionBlacklistedDoors.add(probe);
            log.warn("[Walker] Blacklisting door after wrong traversal: door={} from={} to={} before={} after={}",
                    probe, fromWp, toWp, posBefore, posAfter);
        }
        if (isQuestLockedDoorDialogue()) {
            String dialogue = Rs2Dialogue.getDialogueText();
            log.warn("[Walker] Door at {} ({} action={}) appears quest/stat-locked — dialogue=\"{}\" — blacklisting tile, refreshing restrictions, recalculating",
                    probe, name, action, dialogue);
            sessionBlacklistedDoors.add(probe);
            Rs2Dialogue.clickContinue();
            if (Rs2PathApi.getPathfinderConfig() != null) {
                Rs2PathApi.getPathfinderConfig().refresh();
            }
            recalculatePath();
            return true;
        }

        if (doorStillHasAction(probe, fromWp, toWp, doorActions, action)) {
            log.debug("[Walker] Segment door interaction did not traverse; action still present at {} ({} -> {})",
                    probe, fromWp, toWp);
        } else {
            markStationaryDoorOpened(probe);
            if (tryDoorEdgeCrossNudge(fromWp, toWp, currentTarget)) {
                markNearbyDoorFamilyOpened(object, probe, action, SEGMENT_DOOR_FAMILY_MARK_RADIUS);
                return true;
            }
        }
        return false;
    }

    private static boolean doorStillHasAction(WorldPoint probe, WorldPoint fromWp, WorldPoint toWp,
                                              List<String> doorActions, String action) {
        if (probe == null || action == null) {
            return false;
        }

        WorldPoint anchor = Rs2Player.getWorldLocation();
        if (anchor == null || anchor.getPlane() != probe.getPlane()) {
            anchor = probe;
        }

        TileObject object = Rs2GameObject.getAll(o -> doorObjectStillHasAction(o, probe, fromWp, toWp, doorActions, action),
                        anchor, Math.max(3, HANDLER_RANGE))
                .stream()
                .findFirst()
                .orElse(null);
        return object != null;
    }

    private static boolean doorObjectStillHasAction(TileObject object, WorldPoint probe, WorldPoint fromWp, WorldPoint toWp,
                                                    List<String> doorActions, String action) {
        if (object == null || object.getWorldLocation() == null || action == null) {
            return false;
        }
        if (!(object instanceof WallObject) && !(object instanceof GameObject)) {
            return false;
        }
        WorldPoint loc = object.getWorldLocation();
        if (probe != null && loc.getPlane() != probe.getPlane()) {
            return false;
        }
        if (Rs2DoorProbe.isCatalogTransportObject(object) && !Rs2DoorDetection.isDoorLikeSceneObject(object)) {
            return false;
        }
        boolean nearProbe = probe != null && loc.distanceTo2D(probe) <= 2;
        boolean onSegment = fromWp != null && toWp != null && Rs2DoorGeometry.isDoorOnSegment(object, fromWp, toWp);
        if (!nearProbe && !onSegment) {
            return false;
        }
        ObjectComposition composition = Rs2DoorDetection.resolveCompositionForDoorProbe(object);
        String currentAction = Rs2DoorClassifier.getDoorAction(composition, doorActions);
        return currentAction != null && currentAction.equalsIgnoreCase(action);
    }

    private static void markStationaryDoorOpened(WorldPoint doorTile) {
        Rs2DoorHandler.markStationaryDoorOpened(recentlyOpenedStationaryDoors, doorTile);
    }

    private static String doorAttemptKey(WorldPoint doorTile, WorldPoint fromWp, WorldPoint toWp) {
        return Rs2DoorHandler.doorAttemptKey(doorTile, fromWp, toWp);
    }

    private static boolean shouldThrottleDoorAttempt(WorldPoint doorTile, WorldPoint fromWp, WorldPoint toWp) {
        return Rs2DoorHandler.shouldThrottleDoorAttempt(
                recentDoorAttemptByEdge,
                DOOR_ATTEMPT_EDGE_COOLDOWN_MS,
                doorTile,
                fromWp,
                toWp);
    }

    private static boolean tryDoorEdgeCrossNudge(WorldPoint fromWp, WorldPoint toWp, WorldPoint target) {
        if (fromWp == null || toWp == null || fromWp.getPlane() != toWp.getPlane()) {
            return false;
        }
        WorldPoint before = Rs2Player.getWorldLocation();
        if (before == null || before.getPlane() != toWp.getPlane()) {
            return false;
        }
        if (before.equals(toWp)) {
            return true;
        }
        if (before.distanceTo2D(toWp) > POST_DOOR_EDGE_NUDGE_MAX_FROM_PLAYER) {
            return false;
        }
        if (Rs2Player.isMoving() || Rs2Player.isAnimating()) {
            return false;
        }

        boolean clicked = walkFastCanvas(toWp);
        if (!clicked) {
            clicked = walkMiniMapToward(toWp, before, POST_DOOR_FAST_CLICK_MAX_EUCLIDEAN - 1);
        }
        if (!clicked) {
            return false;
        }

        markFirstMovementClick("first_door_edge_nudge", target, before, "to=" + compactWorldPoint(toWp));
        sleepUntil(() -> {
            if (isWalkCancelled(target)) {
                return true;
            }
            WorldPoint now = Rs2Player.getWorldLocation();
            return isDoorEdgeNudgeResolved(before, now, fromWp, toWp);
        }, POST_DOOR_EDGE_NUDGE_WAIT_MS);

        WorldPoint after = Rs2Player.getWorldLocation();
        boolean progressed = isDoorEdgeNudgeResolved(before, after, fromWp, toWp);
        if (progressed) {
            WebWalkLog.tmark("door_edge_nudge", System.currentTimeMillis() - routeState.walkSessionStartedAtMs,
                    target, before, "from=" + compactWorldPoint(fromWp) + " to=" + compactWorldPoint(toWp));
            routeState.lastMovedTimeMs = System.currentTimeMillis();
            routeState.stuckCount = 0;
        } else {
            WebWalkLog.spInfo("door_edge_nudge_unresolved | from={} to={} before={} after={}",
                    compactWorldPoint(fromWp), compactWorldPoint(toWp), compactWorldPoint(before), compactWorldPoint(after));
        }
        return progressed;
    }

    static boolean isDoorEdgeNudgeResolved(WorldPoint before, WorldPoint after, WorldPoint fromWp, WorldPoint toWp) {
        if (before == null || after == null || fromWp == null || toWp == null) {
            return false;
        }
        if (before.equals(after)) {
            return false;
        }
        if (before.getPlane() != after.getPlane()
                || after.getPlane() != fromWp.getPlane()
                || after.getPlane() != toWp.getPlane()) {
            return false;
        }
        int beforeTo = before.distanceTo2D(toWp);
        int afterTo = after.distanceTo2D(toWp);
        if (after.equals(toWp) || afterTo == 0) {
            return true;
        }
        return afterTo <= 1 && afterTo < beforeTo;
    }

    private static int interimPreclickTiles() {
        try {
            return interimPreclickTiles(Rs2Player.isRunEnabled());
        } catch (Exception e) {
            return INTERIM_PRECLICK_TILES;
        }
    }

    static int interimPreclickTiles(boolean runEnabled) {
        return runEnabled ? INTERIM_RUN_PRECLICK_TILES : INTERIM_PRECLICK_TILES;
    }

    static boolean shouldClearInterimTarget(WorldPoint interim,
                                            WorldPoint playerLoc,
                                            long setAtMs,
                                            long lastProgressAtMs,
                                            long nowMs) {
        return shouldClearInterimTarget(interim, playerLoc, setAtMs, lastProgressAtMs, nowMs, Integer.MAX_VALUE);
    }

    /**
     * @param bestDistanceSeen closest the player has been to {@code interim} while holding it, or
     *                         {@link Integer#MAX_VALUE} when unknown (then the abandon check is inert).
     */
    static boolean shouldClearInterimTarget(WorldPoint interim,
                                            WorldPoint playerLoc,
                                            long setAtMs,
                                            long lastProgressAtMs,
                                            long nowMs,
                                            int bestDistanceSeen) {
        if (interim == null) {
            return false;
        }
        if (playerLoc == null || playerLoc.getPlane() != interim.getPlane()) {
            return true;
        }
        if (playerLoc.distanceTo2D(interim) <= INTERIM_CLOSE_TILES) {
            return true;
        }
        // An interim the player is walking AWAY from is dead, and nothing else here notices.
        // interimLastProgressAtMs is renewed whenever the ROUTE INDEX advances, so a player making
        // honest progress along the route — in the opposite direction to a checkpoint the route has
        // since moved past — renews the interim every pass and the stale-progress escape can never
        // fire. Measured: interim held at (2973,3350) while the player walked 2961,3349 -> 2960,3343,
        // moving=true throughout, renewed until interimAgeMs=9999 and only then "expired" — with a
        // transport dispatch waiting behind it the whole time.
        if (bestDistanceSeen != Integer.MAX_VALUE
                && playerLoc.distanceTo2D(interim) > bestDistanceSeen + INTERIM_ABANDON_MARGIN_TILES) {
            return true;
        }
        if (lastProgressAtMs > 0L && nowMs - lastProgressAtMs > INTERIM_PROGRESS_TIMEOUT_MS) {
            return true;
        }
        return setAtMs > 0L && nowMs - setAtMs > INTERIM_MAX_AGE_MS;
    }

    static int distanceToInterimOrMax(WorldPoint interim, WorldPoint playerLoc) {
        if (interim == null || playerLoc == null || interim.getPlane() != playerLoc.getPlane()) {
            return Integer.MAX_VALUE;
        }
        return playerLoc.distanceTo2D(interim);
    }

    private static void recordInterimDistanceProgress(WorldPoint interim, WorldPoint playerLoc, long nowMs) {
        int distance = distanceToInterimOrMax(interim, playerLoc);
        if (distance < routeState.interimLastDistanceToTarget) {
            routeState.interimLastDistanceToTarget = distance;
            routeState.interimLastProgressAtMs = nowMs;
        }
    }

    private static boolean shouldYieldForActiveRecoveryInterim(WorldPoint playerLoc,
                                                               List<WorldPoint> path,
                                                               long nowMs) {
        WorldPoint interim = routeState.interimTargetWp;
        if (interim == null) {
            return false;
        }
        recordInterimDistanceProgress(interim, playerLoc, nowMs);
        if (playerLoc != null && path != null && !path.isEmpty()) {
            int bestIdxNow = getClosestTileIndex(path, playerLoc);
            if (bestIdxNow > routeState.interimLastBestPathIdx) {
                routeState.interimLastBestPathIdx = bestIdxNow;
                routeState.interimLastProgressAtMs = nowMs;
            }
        }
        return shouldYieldForActiveRecoveryInterim(interim,
                playerLoc,
                routeState.interimSetAtMs,
                routeState.interimLastProgressAtMs,
                nowMs,
                routeState.lastMovedTimeMs,
                routeState.lastUnreachableRecoveryClickAtMs,
                Rs2Player.isMoving());
    }

    static boolean shouldYieldForActiveRecoveryInterim(WorldPoint interim,
                                                       WorldPoint playerLoc,
                                                       long setAtMs,
                                                       long lastProgressAtMs,
                                                       long nowMs,
                                                       long lastMovedAtMs,
                                                       long lastRecoveryClickAtMs,
                                                       boolean playerMoving) {
        if (interim == null) {
            return false;
        }
        if (shouldClearInterimTarget(interim, playerLoc, setAtMs, lastProgressAtMs, nowMs)) {
            return false;
        }
        if (shouldDeferRouteWorkForActiveInterim(interim,
                playerLoc,
                setAtMs,
                lastProgressAtMs,
                nowMs,
                lastMovedAtMs,
                playerMoving,
                INTERIM_CLOSE_TILES)) {
            return true;
        }
        return isRecentEvent(nowMs, lastRecoveryClickAtMs, RECOVERY_MOVEMENT_IN_FLIGHT_MS);
    }

    static boolean shouldDeferRouteWorkForActiveInterim(WorldPoint interim,
                                                        WorldPoint playerLoc,
                                                        long setAtMs,
                                                        long lastProgressAtMs,
                                                        long nowMs,
                                                        long lastMovedAtMs,
                                                        boolean playerMoving,
                                                        int handoffTiles) {
        if (interim == null) {
            return false;
        }
        if (shouldClearInterimTarget(interim, playerLoc, setAtMs, lastProgressAtMs, nowMs)) {
            return false;
        }
        if (playerLoc == null || playerLoc.getPlane() != interim.getPlane()) {
            return false;
        }
        if (playerLoc.distanceTo2D(interim) <= Math.max(0, handoffTiles)) {
            return false;
        }
        if (playerMoving) {
            return true;
        }
        if (isRecentEvent(nowMs, lastProgressAtMs, INTERIM_PROGRESS_TIMEOUT_MS)) {
            return true;
        }
        return isRecentEvent(nowMs, lastMovedAtMs, RECOVERY_MOVEMENT_IN_FLIGHT_MS);
    }

    private static void clearInterimTarget(String reason) {
        WorldPoint old = routeState.interimTargetWp;
        if (old != null) {
            if ("close".equals(reason)) {
                WebWalkLog.spDebug("interim_clear | reason={} interim={}", reason, compactWorldPoint(old));
            } else {
                WebWalkLog.spInfo("interim_clear | reason={} interim={}", reason, compactWorldPoint(old));
            }
        }
        routeState.interimTargetWp = null;
        routeState.interimTargetIdx = -1;
        routeState.interimSetAtMs = 0L;
        routeState.interimLastProgressAtMs = 0L;
        routeState.interimLastBestPathIdx = -1;
        routeState.interimLastDistanceToTarget = Integer.MAX_VALUE;
        routeState.interimLastRetargetAtMs = 0L;
    }

    private static boolean shouldThrottleGlobalDoorInteraction() {
        return Rs2DoorHandler.shouldThrottleGlobalDoorInteraction(routeState.nextDoorInteractionAllowedAtMs)
                || shouldDeferDoorInteractionForDialogue();
    }

    /**
     * A guarded door answers with a conversation instead of opening ("you can't go in there"). The
     * walker reads the lack of movement as "no progress, retry" and clicks again — and that click
     * CANCELS the menu the previous click just opened, destroying the only thing that can get us
     * through. Whatever answers dialogue (the questing layer) then never sees a menu that survives
     * long enough to act on, so the walk livelocks at the door.
     *
     * <p>Deferring is BOUNDED: if nothing answers within {@link #DOOR_DIALOGUE_DEFER_MAX_MS} the
     * walker resumes clicking, so a stray conversation with no handler cannot stall a plain walk
     * that has no dialogue logic behind it.
     */
    private static boolean shouldDeferDoorInteractionForDialogue() {
        if (!Rs2Dialogue.hasSelectAnOption()) {
            routeState.doorDialogueDeferSinceMs = 0L;
            return false;
        }
        long now = System.currentTimeMillis();
        if (routeState.doorDialogueDeferSinceMs == 0L) {
            routeState.doorDialogueDeferSinceMs = now;
            WebWalkLog.spInfo("door_dialogue_defer | an option menu is open — not re-clicking the door");
        }
        return doorDialogueDeferActive(routeState.doorDialogueDeferSinceMs, now, DOOR_DIALOGUE_DEFER_MAX_MS);
    }

    /**
     * Pure half of the dialogue hold-off: defer only while the menu has been up for less than
     * {@code maxDeferMs}. Split out because an unbounded version of this gate would trade a livelock
     * at a guarded door for a permanent stall at any unanswered conversation.
     */
    static boolean doorDialogueDeferActive(long deferSinceMs, long nowMs, long maxDeferMs) {
        return deferSinceMs > 0L && nowMs - deferSinceMs < maxDeferMs;
    }

    /**
     * Pure settle decision after a handled transport. Settling ends as soon as the player is confirmed
     * ARRIVED — standing at/next to the transport's planned destination, neither moving nor animating —
     * after a one-tick floor for post-action state flux; {@link #TRANSPORT_POST_INTERACT_SETTLE_MS} is
     * only the ceiling for when arrival never confirms (unknown destination, drawn-out travel). The old
     * check compared against where the player stood when the transport was MARKED handled, which after
     * landing is always true while standing still — so the settle could only ever end by timeout, a fixed
     * ~900ms freeze after every single transport.
     */
    static boolean transportSettlePending(long ageMs, WorldPoint now, WorldPoint plannedDestination,
                                          boolean moving, boolean animating) {
        if (ageMs < 0L || ageMs > TRANSPORT_POST_INTERACT_SETTLE_MS) {
            return false;
        }
        if (ageMs < POST_INTERACT_SETTLE_MIN_MS) {
            return true;
        }
        if (now == null || plannedDestination == null) {
            return ageMs <= TRANSPORT_POST_INTERACT_SETTLE_MS / 2;
        }
        boolean arrivedIdle = now.getPlane() == plannedDestination.getPlane()
                && now.distanceTo2D(plannedDestination) <= 1
                && !moving && !animating;
        return !arrivedIdle;
    }

    private static boolean isRecoveryMovementInFlight() {
        return System.currentTimeMillis() - routeState.lastUnreachableRecoveryClickAtMs < RECOVERY_MOVEMENT_IN_FLIGHT_MS;
    }

    /** Starts the door settle window, remembering the far-side tile so it can end when the edge opens. */
    private static void markDoorInteractionSettling(WorldPoint farSideWp) {
        long now = System.currentTimeMillis();
        routeState.doorInteractionSettleStartedAtMs = now;
        routeState.doorInteractionSettleUntilMs = now + DOOR_POST_INTERACT_SETTLE_MS;
        routeState.doorSettleFarSideWp = farSideWp;
    }

    private static void markGlobalDoorInteractionCooldown() {
        routeState.nextDoorInteractionAllowedAtMs = Rs2DoorHandler.markGlobalDoorInteractionCooldown(DOOR_INTERACTION_GLOBAL_COOLDOWN_MS);
    }

    private static void markDoorAttempt(WorldPoint doorTile, WorldPoint fromWp, WorldPoint toWp) {
        Rs2DoorHandler.markDoorAttempt(recentDoorAttemptByEdge, doorTile, fromWp, toWp);
        if (fromWp != null && toWp != null) {
            routeState.lastDoorAttemptFrom = fromWp;
            routeState.lastDoorAttemptTo = toWp;
            routeState.lastDoorAttemptAtMs = System.currentTimeMillis();
        }
    }



    private static boolean recentlyOpenedStationaryDoorOnSegment(WorldPoint fromWp, WorldPoint toWp) {
        return Rs2DoorHandler.recentlyOpenedStationaryDoorOnSegment(
                recentlyOpenedStationaryDoors,
                STATIONARY_DOOR_SUPPRESS_MS,
                fromWp,
                toWp);
    }

    static boolean shouldApproachPlannedTransportOrigin(boolean explicitTransportStep,
                                                        WorldPoint routeOrigin,
                                                        WorldPoint playerLoc,
                                                        int dispatchMaxDistance) {
        return explicitTransportStep
                && routeOrigin != null
                && playerLoc != null
                && routeOrigin.getPlane() == playerLoc.getPlane()
                && routeOrigin.distanceTo2D(playerLoc) > Math.max(0, dispatchMaxDistance);
    }

    /**
     * Whether this path edge is covered by a transport catalog row (same coordinates loaded from TSV into
     * {@link Rs2PathApi#getTransports()}). Includes strict origin-destination steps (including
     * cross-plane rows such as ladders) and same-plane hops where the path starts on a tile Chebyshev-adjacent
     * to the catalog origin but still targets that row's destination, keeping door probing separate.
     */
    private static boolean isCatalogBackedTransportSegment(List<WorldPoint> path, int index) {
        if (path == null || index < 0 || index >= path.size() - 1) {
            return false;
        }
        return isCatalogBackedTransportSegment(path.get(index), path.get(index + 1));
    }

    private static boolean isCatalogBackedTransportSegment(WorldPoint from, WorldPoint to) {
        if (from == null || to == null) {
            return false;
        }
        if (matchesDirectedTransportCatalogEdge(from, to)) {
            return true;
        }
        if (matchesDirectedTransportCatalogEdge(to, from)) {
            return true;
        }
        if (matchesAdjacentOriginShortTransportHop(from, to)) {
            return true;
        }
        if (matchesAdjacentOriginShortTransportHop(to, from)) {
            return true;
        }
        return false;
    }

    private static boolean isDoorLikeCatalogTransportSegment(List<WorldPoint> path, int index) {
        if (path == null || index < 0 || index >= path.size() - 1) {
            return false;
        }
        return isDoorLikeCatalogTransportSegment(path.get(index), path.get(index + 1));
    }

    private static boolean isDoorLikeCatalogTransportSegment(WorldPoint from, WorldPoint to) {
        if (from == null || to == null) {
            return false;
        }
        return hasDoorLikeDirectedCatalogTransport(from, to)
                || hasDoorLikeDirectedCatalogTransport(to, from)
                || hasDoorLikeAdjacentOriginShortTransportHop(from, to)
                || hasDoorLikeAdjacentOriginShortTransportHop(to, from);
    }

    private static boolean matchesDirectedTransportCatalogEdge(WorldPoint origin, WorldPoint dest) {
        if (origin == null || dest == null) {
            return false;
        }
        Set<Transport> transports = Rs2PathApi.getTransports().get(origin);
        if (transports == null || transports.isEmpty()) {
            return false;
        }
        return transports.stream().anyMatch(t -> Objects.equals(t.getDestination(), dest));
    }

    private static boolean hasDoorLikeDirectedCatalogTransport(WorldPoint origin, WorldPoint dest) {
        if (origin == null || dest == null) {
            return false;
        }
        Set<Transport> transports = Rs2PathApi.getTransports().get(origin);
        if (transports == null || transports.isEmpty()) {
            return false;
        }
        return transports.stream()
                .anyMatch(t -> Objects.equals(t.getDestination(), dest) && Rs2DoorProbe.isDoorLikeCatalogTransport(t));
    }

    /**
     * True when some catalog origin one step from {@code from} has a same-plane adjacent transport to {@code to}.
     * Restricted to {@link #isAdjacentSamePlaneTransport} rows so long-distance transports do not suppress doors.
     */
    private static boolean matchesAdjacentOriginShortTransportHop(WorldPoint from, WorldPoint to) {
        if (from == null || to == null || from.getPlane() != to.getPlane()) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                WorldPoint catalogOrigin = new WorldPoint(from.getX() + dx, from.getY() + dy, from.getPlane());
                Set<Transport> transports = Rs2PathApi.getTransports().get(catalogOrigin);
                if (transports == null || transports.isEmpty()) {
                    continue;
                }
                for (Transport t : transports) {
                    if (Objects.equals(t.getDestination(), to) && isAdjacentSamePlaneTransport(t)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasDoorLikeAdjacentOriginShortTransportHop(WorldPoint from, WorldPoint to) {
        if (from == null || to == null || from.getPlane() != to.getPlane()) {
            return false;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                WorldPoint catalogOrigin = new WorldPoint(from.getX() + dx, from.getY() + dy, from.getPlane());
                Set<Transport> transports = Rs2PathApi.getTransports().get(catalogOrigin);
                if (transports == null || transports.isEmpty()) {
                    continue;
                }
                for (Transport t : transports) {
                    if (Objects.equals(t.getDestination(), to)
                            && isAdjacentSamePlaneTransport(t)
                            && Rs2DoorProbe.isDoorLikeCatalogTransport(t)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }




    private static void waitForDoorInteractionProgress(WorldPoint fromWp, WorldPoint toWp) {
        long startedAt = System.currentTimeMillis();
        AwaitTicket ticket = Rs2WalkerAwaits.beginTicket();
        try {
            Rs2WalkerAwaits.awaitDoorInteractionProgress(ticket, fromWp, toWp);
        } finally {
            if (rawScanWallSnapshot != null || rawScanGameObjectSnapshot != null) {
                rawScanDoorInteractionWaitMs += System.currentTimeMillis() - startedAt;
            }
        }
    }

    private static boolean isDoorEdgeResolved(WorldPoint fromWp, WorldPoint toWp) {
        return Rs2WalkerAwaits.isDoorEdgeResolved(fromWp, toWp);
    }

    static boolean didTraverseInteractedDoor(WorldPoint start, WorldPoint end, WorldPoint objectLoc,
                                             WorldPoint fromWp, WorldPoint toWp) {
        if (start == null || end == null || objectLoc == null || toWp == null) {
            return false;
        }
        if (start.getPlane() != end.getPlane() || end.getPlane() != objectLoc.getPlane() || end.getPlane() != toWp.getPlane()) {
            return false;
        }
        if (start.equals(end)) {
            return false;
        }
        if (!movedAcrossInteractedObject(start, end, objectLoc)) {
            return false;
        }
        int beforeTo = start.distanceTo2D(toWp);
        int afterTo = end.distanceTo2D(toWp);
        if (afterTo >= beforeTo) {
            return false;
        }
        // Keep the traversal check anchored to the active segment.
        return fromWp == null || fromWp.getPlane() == end.getPlane();
    }

    static boolean shouldBlacklistDoorAfterWrongTraversal(WorldPoint start, WorldPoint end, WorldPoint fromWp, WorldPoint toWp) {
        return shouldBlacklistDoorAfterWrongTraversal(start, end, fromWp, toWp, false);
    }

    /**
     * As {@link #shouldBlacklistDoorAfterWrongTraversal(WorldPoint, WorldPoint, WorldPoint, WorldPoint)}
     * but aware of whether the {@code end} position was sampled while the player was STILL WALKING. The
     * interact walks the player to the door first and the progress wait can time out en route, so a
     * moving sample is just a point along the path — not a traversal verdict. Deciding from one poisoned
     * Wydin's shop door: before=3008,3207 (en route), after=3012,3211 (seven tiles from the edge, mid
     * walk) was blacklisted AND learn-persisted as a blocked edge. A same-plane moving sample must never
     * blacklist; a plane change is still trusted (the door acted — walking cannot change plane).
     */
    static boolean shouldBlacklistDoorAfterWrongTraversal(WorldPoint start, WorldPoint end, WorldPoint fromWp,
                                                          WorldPoint toWp, boolean sampledWhileMoving) {
        if (start == null || end == null || toWp == null) {
            return false;
        }
        if (start.equals(end)) {
            return false;
        }
        if (start.getPlane() != end.getPlane()) {
            return true;
        }
        if (sampledWhileMoving) {
            return false;
        }
        if (!startedNearDoorEdge(start, fromWp, toWp)) {
            return false;
        }
        int moved = start.distanceTo2D(end);
        if (moved < 3) {
            return false;
        }
        int startTo = start.distanceTo2D(toWp);
        int endTo = end.distanceTo2D(toWp);
        if (endTo <= startTo + 1) {
            return false;
        }
        if (fromWp == null || fromWp.getPlane() != end.getPlane()) {
            return true;
        }
        int startFrom = start.distanceTo2D(fromWp);
        int endFrom = end.distanceTo2D(fromWp);
        return endFrom >= startFrom + 2;
    }

    private static boolean startedNearDoorEdge(WorldPoint start, WorldPoint fromWp, WorldPoint toWp) {
        if (start == null) {
            return false;
        }
        final int maxDoorStartDistance = 3;
        boolean nearFrom = fromWp != null
                && fromWp.getPlane() == start.getPlane()
                && start.distanceTo2D(fromWp) <= maxDoorStartDistance;
        boolean nearTo = toWp != null
                && toWp.getPlane() == start.getPlane()
                && start.distanceTo2D(toWp) <= maxDoorStartDistance;
        return nearFrom || nearTo;
    }

    private static boolean movedAcrossInteractedObject(WorldPoint start, WorldPoint end, WorldPoint objectLoc) {
        int startRelX = Integer.compare(start.getX(), objectLoc.getX());
        int endRelX = Integer.compare(end.getX(), objectLoc.getX());
        int startRelY = Integer.compare(start.getY(), objectLoc.getY());
        int endRelY = Integer.compare(end.getY(), objectLoc.getY());
        return startRelX != endRelX || startRelY != endRelY;
    }

    private static boolean hasDoorLikeSceneObjectOnSegment(WorldPoint fromWp, WorldPoint toWp,
                                                           WorldPoint playerLoc, int radiusTiles) {
        if (fromWp == null || toWp == null || playerLoc == null || radiusTiles <= 0) {
            return false;
        }
        if (fromWp.getPlane() != toWp.getPlane() || fromWp.getPlane() != playerLoc.getPlane()) {
            return false;
        }
        if (recentlyOpenedStationaryDoorOnSegment(fromWp, toWp)) {
            return false;
        }

        for (WallObject wall : Rs2GameObject.getWallObjects(o -> true, playerLoc, radiusTiles)) {
            if (isPendingRouteDoorObject(wall, fromWp, toWp, playerLoc, radiusTiles)) {
                return true;
            }
        }
        for (GameObject object : Rs2GameObject.getGameObjects(o -> true, playerLoc, radiusTiles)) {
            if (isPendingRouteDoorObject(object, fromWp, toWp, playerLoc, radiusTiles)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnresolvedDoorLikeObjectNearRawPath(List<WorldPoint> rawPath,
                                                                  int rawEdgeStart,
                                                                  WorldPoint playerLoc,
                                                                  int backtrackEdges,
                                                                  int lookaheadEdges,
                                                                  int radiusTiles) {
        if (rawPath == null || rawPath.size() < 2 || playerLoc == null || rawEdgeStart < 0) {
            return false;
        }

        int start = Math.max(0, rawEdgeStart - Math.max(0, backtrackEdges));
        int endExclusive = Math.min(rawPath.size() - 1, rawEdgeStart + Math.max(1, lookaheadEdges));
        for (int ri = start; ri < endExclusive && ri < rawPath.size() - 1; ri++) {
            WorldPoint from = rawPath.get(ri);
            WorldPoint to = rawPath.get(ri + 1);
            if (from == null || to == null) {
                continue;
            }
            if (from.getPlane() != playerLoc.getPlane() || to.getPlane() != playerLoc.getPlane()) {
                break;
            }
            if (from.distanceTo2D(playerLoc) > radiusTiles && to.distanceTo2D(playerLoc) > radiusTiles) {
                continue;
            }
            if (isCatalogBackedTransportSegment(rawPath, ri) && !isDoorLikeCatalogTransportSegment(rawPath, ri)) {
                continue;
            }
            if (hasUnresolvedDoorLikeSceneObjectOnSegment(from, to, playerLoc, radiusTiles)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnresolvedDoorLikeSceneObjectOnSegment(WorldPoint fromWp, WorldPoint toWp,
                                                                     WorldPoint playerLoc, int radiusTiles) {
        if (fromWp == null || toWp == null || playerLoc == null || radiusTiles <= 0) {
            return false;
        }
        if (fromWp.getPlane() != toWp.getPlane() || fromWp.getPlane() != playerLoc.getPlane()) {
            return false;
        }

        for (WallObject wall : Rs2GameObject.getWallObjects(o -> true, playerLoc, radiusTiles)) {
            if (isUnresolvedRouteDoorObject(wall, fromWp, toWp, playerLoc, radiusTiles)) {
                return true;
            }
        }
        for (GameObject object : Rs2GameObject.getGameObjects(o -> true, playerLoc, radiusTiles)) {
            if (isUnresolvedRouteDoorObject(object, fromWp, toWp, playerLoc, radiusTiles)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnresolvedRouteDoorObject(TileObject object, WorldPoint fromWp, WorldPoint toWp,
                                                       WorldPoint playerLoc, int radiusTiles) {
        if (object == null || object.getWorldLocation() == null) {
            return false;
        }
        WorldPoint location = object.getWorldLocation();
        if (location.getPlane() != playerLoc.getPlane()
                || location.distanceTo2D(playerLoc) > radiusTiles
                || (Rs2DoorProbe.isCatalogTransportObject(object) && !Rs2DoorDetection.isDoorLikeSceneObject(object))
                || !Rs2DoorGeometry.isDoorOnSegment(object, fromWp, toWp)) {
            return false;
        }

        ObjectComposition comp = Rs2DoorDetection.resolveCompositionForDoorProbe(object);
        if (comp == null
                || Rs2DoorClassifier.isNullOrPlaceholderObjectName(comp.getName())
                || Rs2DoorClassifier.doorCompositionSpecifiesOnlyCloseOrShut(comp)) {
            return false;
        }
        String action = Rs2DoorClassifier.pickWalkDoorAction(comp);
        return Rs2DoorClassifier.isDoorLikeGameObjectName(comp.getName())
                || (action != null && Rs2DoorClassifier.doorActionPriorityIndex(action) < Integer.MAX_VALUE);
    }

    private static boolean isPendingRouteDoorObject(TileObject object, WorldPoint fromWp, WorldPoint toWp,
                                                    WorldPoint playerLoc, int radiusTiles) {
        if (object == null || object.getWorldLocation() == null) {
            return false;
        }
        WorldPoint location = object.getWorldLocation();
        if (location.getPlane() != playerLoc.getPlane()
                || location.distanceTo2D(playerLoc) > radiusTiles
                || sessionBlacklistedDoors.contains(location)
                || (Rs2DoorProbe.isCatalogTransportObject(object) && !Rs2DoorDetection.isDoorLikeSceneObject(object))
                || !Rs2DoorGeometry.isDoorOnSegment(object, fromWp, toWp)) {
            return false;
        }

        ObjectComposition comp = Rs2DoorDetection.resolveCompositionForDoorProbe(object);
        if (comp == null
                || Rs2DoorClassifier.isNullOrPlaceholderObjectName(comp.getName())
                || Rs2DoorClassifier.doorCompositionSpecifiesOnlyCloseOrShut(comp)) {
            return false;
        }
        String action = Rs2DoorClassifier.pickWalkDoorAction(comp);
        return Rs2DoorClassifier.isDoorLikeGameObjectName(comp.getName())
                || (action != null && Rs2DoorClassifier.doorActionPriorityIndex(action) < Integer.MAX_VALUE);
    }


	/**
	 * Door handling can include dialogue and waits; bound it so the walker cannot hang
	 * indefinitely on a bad interact. If the timeout elapses, return false so the main
	 * loop can continue (stall detection / replans).
	 */
	private static boolean handleDoorsWithTimeout(List<WorldPoint> path, int index, long timeoutMs) {
        return handleDoorsWithTimeout(path, index, timeoutMs, null);
    }

    private static boolean handleDoorsWithTimeout(List<WorldPoint> path, int index, long timeoutMs,
                                                  Map<String, WorldPoint> attemptedDoorEdgesThisPass) {
        return handleDoorsWithTimeout(path, index, timeoutMs, attemptedDoorEdgesThisPass, false);
    }

    private static boolean handleDoorsWithTimeout(List<WorldPoint> path, int index, long timeoutMs,
                                                  Map<String, WorldPoint> attemptedDoorEdgesThisPass,
                                                  boolean allowSegmentProbe) {
		long start = System.currentTimeMillis();
        WorldPoint[] segment = resolveDoorSegment(path, index);
        String edgeKey = segment != null && segment.length >= 2 && segment[0] != null && segment[1] != null
                ? doorAttemptKey(null, segment[0], segment[1])
                : null;
        WorldPoint playerBeforeAttempt = Rs2Player.getWorldLocation();
        if (!markDoorEdgeAttemptThisPass(attemptedDoorEdgesThisPass, segment, playerBeforeAttempt)) {
            routeState.lastDoorEdgePassSkipAtMs = System.currentTimeMillis();
            WebWalkLog.spInfo("door_edge_pass_skip | idx={}", index);
            return false;
        }
		boolean handled = handleDoors(path, index, allowSegmentProbe);
		if (!handled) {
            // Do not consume one-shot budget when no interaction happened; allow
            // a later resolver in the same pass to attempt this edge.
            if (attemptedDoorEdgesThisPass != null && edgeKey != null) {
                attemptedDoorEdgesThisPass.remove(edgeKey);
            }
			return false;
		}
        WebWalkLog.tmark("door_interaction_done", System.currentTimeMillis() - start, currentTarget, playerBeforeAttempt,
                "idx=" + index);
		long remaining = timeoutMs - (System.currentTimeMillis() - start);
		if (remaining <= 0) {
			return true;
		}
		WorldPoint before = Rs2Player.getWorldLocation();
		int remainingInt = (int) Math.min(Integer.MAX_VALUE, remaining);
		sleepUntil(() -> {
			WorldPoint now = Rs2Player.getWorldLocation();
			if (before != null && now != null && !before.equals(now)) return true;
			return Rs2Player.isMoving() || Rs2Dialogue.isInDialogue();
		}, remainingInt);

        if (segment != null && !isDoorEdgeResolved(segment[0], segment[1])) {
            WebWalkLog.spInfo("door_edge_post_unresolved | idx={} from={} to={}",
                    index, compactWorldPoint(segment[0]), compactWorldPoint(segment[1]));
        } else if (segment != null) {
            WebWalkLog.tmark("door_edge_resolved", System.currentTimeMillis() - start, currentTarget,
                    Rs2Player.getWorldLocation(),
                    "from=" + compactWorldPoint(segment[0]) + " to=" + compactWorldPoint(segment[1]));
        }
        return true;
	}

    private static WorldPoint[] resolveDoorSegment(List<WorldPoint> path, int index) {
        if (path == null || index < 0 || index >= path.size() - 1) {
            return null;
        }
        WorldPoint fromWp = path.get(index);
        WorldPoint toWp = path.get(index + 1);
        if (fromWp == null || toWp == null) {
            return null;
        }
        boolean isInstance = Microbot.getClient()
                .getTopLevelWorldView()
                .getScene()
                .isInstance();
        if (!isInstance) {
            return new WorldPoint[] {fromWp, toWp};
        }
        WorldPoint convertedFrom = Rs2WorldPoint.convertInstancedWorldPoint(fromWp);
        WorldPoint convertedTo = Rs2WorldPoint.convertInstancedWorldPoint(toWp);
        if (convertedFrom == null || convertedTo == null) {
            return null;
        }
        return new WorldPoint[] {convertedFrom, convertedTo};
    }

    static boolean markDoorEdgeAttemptThisPass(Map<String, WorldPoint> attemptedDoorEdgesThisPass,
                                               WorldPoint[] segment,
                                               WorldPoint playerBeforeAttempt) {
        if (attemptedDoorEdgesThisPass == null || segment == null || segment.length < 2
                || segment[0] == null || segment[1] == null) {
            return true;
        }
        String edgeKey = doorAttemptKey(null, segment[0], segment[1]);
        WorldPoint previousAttemptPos = attemptedDoorEdgesThisPass.get(edgeKey);
        if (previousAttemptPos != null && playerBeforeAttempt != null
                && previousAttemptPos.getPlane() == playerBeforeAttempt.getPlane()
                && previousAttemptPos.distanceTo2D(playerBeforeAttempt) <= 1) {
            return false;
        }
        attemptedDoorEdgesThisPass.put(edgeKey, playerBeforeAttempt);
        return true;
    }

    private static String normalizePathAdjFamilyKey(TileObject object, String action) {
        ObjectComposition comp = Rs2DoorDetection.resolveCompositionForDoorProbe(object);
        String name = comp != null && comp.getName() != null ? comp.getName().toLowerCase(Locale.ROOT).trim() : "unknown";
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        WorldPoint loc = object != null ? object.getWorldLocation() : null;
        int plane = loc != null ? loc.getPlane() : -1;
        int objectId = object != null ? object.getId() : -1;
        int idRangeLow = objectId >= 0 ? objectId - 1 : -1;
        int idRangeHigh = objectId >= 0 ? objectId + 1 : -1;
        return name + "|" + act + "|p" + plane + "|id=" + idRangeLow + "-" + idRangeHigh;
    }

    private static boolean arePathAdjFamiliesCompatible(String a, String b) {
        if (Objects.equals(a, b)) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        int aIdTag = a.indexOf("|id=");
        int bIdTag = b.indexOf("|id=");
        if (aIdTag <= 0 || bIdTag <= 0) {
            return false;
        }
        String aBase = a.substring(0, aIdTag);
        String bBase = b.substring(0, bIdTag);
        if (!Objects.equals(aBase, bBase)) {
            return false;
        }
        int[] aRange = parsePathAdjIdRange(a.substring(aIdTag + 4));
        int[] bRange = parsePathAdjIdRange(b.substring(bIdTag + 4));
        if (aRange == null || bRange == null) {
            return false;
        }
        return Math.max(aRange[0], bRange[0]) <= Math.min(aRange[1], bRange[1]);
    }

    private static int[] parsePathAdjIdRange(String range) {
        if (range == null || range.isEmpty()) {
            return null;
        }
        int sep = range.indexOf('-');
        if (sep <= 0 || sep >= range.length() - 1) {
            return null;
        }
        try {
            int low = Integer.parseInt(range.substring(0, sep));
            int high = Integer.parseInt(range.substring(sep + 1));
            if (high < low) {
                return null;
            }
            return new int[] {low, high};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void markNearbyDoorFamilyOpened(TileObject originObject, WorldPoint originLocation, String action, int radiusTiles) {
        if (originObject == null || originLocation == null || radiusTiles <= 0) {
            return;
        }
        String familyKey = normalizePathAdjFamilyKey(originObject, action);
        if (familyKey == null || familyKey.isEmpty()) {
            markStationaryDoorOpened(originLocation);
            return;
        }
        markStationaryDoorOpened(originLocation);
        for (WallObject wall : Rs2GameObject.getWallObjects(o -> true, originLocation, radiusTiles)) {
            if (wall == null || wall.getWorldLocation() == null) {
                continue;
            }
            if (wall.getWorldLocation().getPlane() != originLocation.getPlane()) {
                continue;
            }
            ObjectComposition comp = Rs2DoorDetection.resolveCompositionForDoorProbe(wall);
            String neighborFamily = normalizePathAdjFamilyKey(wall, comp == null ? null : Rs2DoorClassifier.pickWalkDoorAction(comp));
            if (arePathAdjFamiliesCompatible(familyKey, neighborFamily)) {
                markStationaryDoorOpened(wall.getWorldLocation());
            }
        }
        for (GameObject game : Rs2GameObject.getGameObjects(o -> true, originLocation, radiusTiles)) {
            if (game == null || game.getWorldLocation() == null) {
                continue;
            }
            if (game.getWorldLocation().getPlane() != originLocation.getPlane()) {
                continue;
            }
            ObjectComposition comp = Rs2DoorDetection.resolveCompositionForDoorProbe(game);
            String neighborFamily = normalizePathAdjFamilyKey(game, comp == null ? null : Rs2DoorClassifier.pickWalkDoorAction(comp));
            if (arePathAdjFamiliesCompatible(familyKey, neighborFamily)) {
                markStationaryDoorOpened(game.getWorldLocation());
            }
        }
    }

    private static final class PathAdjDoorCandidate {
        private final TileObject object;
        private final WorldPoint location;
        private final String action;
        private final int actionPriority;
        private final int edgeIdx;
        private final WorldPoint from;
        private final WorldPoint to;
        private final int edgeDist;
        private final String familyKey;

        private PathAdjDoorCandidate(TileObject object, WorldPoint location, String action, int actionPriority,
                                     int edgeIdx, WorldPoint from, WorldPoint to, int edgeDist, String familyKey) {
            this.object = object;
            this.location = location;
            this.action = action;
            this.actionPriority = actionPriority;
            this.edgeIdx = edgeIdx;
            this.from = from;
            this.to = to;
            this.edgeDist = edgeDist;
            this.familyKey = familyKey;
        }
    }

    private static final class PathAdjDoorComponent {
        private final PathAdjDoorCandidate best;
        private final int score;
        private final Set<WorldPoint> locations;

        private PathAdjDoorComponent(PathAdjDoorCandidate best, int score, Set<WorldPoint> locations) {
            this.best = best;
            this.score = score;
            this.locations = locations;
        }
    }

    private static boolean handleDoorException(TileObject object, String action) {
        if (isInStrongholdOfSecurity()) {
            return handleStrongholdOfSecurityAnswer(object, action);
        }
        return false;
    }

    private static boolean isInStrongholdOfSecurity() {
        return DoorInteractionOwnership.isStrongholdSecurityRegion(Rs2Player.getWorldLocation());
    }

    private static boolean handleStrongholdOfSecurityAnswer(TileObject object, String action) {
        Rs2GameObject.interact(object, action);
        boolean isInDialogue = Rs2Dialogue.sleepUntilInDialogue();

        // Not all the doors ask questions, so only if dialogue is shown we will attempt to get the answer
        if (!isInDialogue) return true;

        // Skip over first door dialogue & don't forget to set up two-factor warning
        if (Rs2Dialogue.getDialogueText().toLowerCase().contains("two-factor authentication options") || Rs2Dialogue.getDialogueText().toLowerCase().contains("hopefully you will learn<br>much from us.")) {
            Rs2Dialogue.sleepUntilHasContinue();
            sleepUntil(() -> !Rs2Dialogue.hasContinue() || Rs2Dialogue.getDialogueText().toLowerCase().contains("to pass you must answer me"), Rs2Dialogue::clickContinue, 5000, Rs2Random.between(600, 800));
            if (!Rs2Dialogue.isInDialogue()) return true;
        }

        String dialogueAnswer = null;
        int attempts = 0;
        final int maxAttempts = 5;

        // We attempt to find the answer multiple times in-case there is dialogue that appears before the question
        while (dialogueAnswer == null && attempts < maxAttempts) {
            if (currentTarget == null) break;
            dialogueAnswer = StrongholdAnswer.findAnswer(Rs2Dialogue.getDialogueText());
            if (dialogueAnswer == null) {
                Rs2Dialogue.clickContinue();
                Rs2Random.waitEx(800, 100);
            }
            attempts++;
        }

        if (dialogueAnswer != null) {
            Rs2Dialogue.clickContinue();
            Rs2Dialogue.sleepUntilSelectAnOption();
            Rs2Dialogue.clickOption(dialogueAnswer);
            Rs2Dialogue.sleepUntilHasContinue();
            sleepUntil(() -> !Rs2Dialogue.hasContinue(), Rs2Dialogue::clickContinue, 5000, Rs2Random.between(600, 800));
            Rs2Player.waitForAnimation(1200);
            return true;
        }

        return false;
    }

    /**
     * Determines whether a given neighbor tile lies immediately adjacent to
     * a reference tile, in the direction specified by a wall orientation code.
     *
     * @param orientation the wall orientation code:
     *                    <ul>
     *                      <li>1 = west</li>
     *                      <li>2 = north</li>
     *                      <li>4 = east</li>
     *                      <li>8 = south</li>
     *                      <li>16 = northwest</li>
     *                      <li>32 = northeast</li>
     *                      <li>64 = southeast</li>
     *                      <li>128 = southwest</li>
     *                    </ul>
     * @param point       the reference {@link WorldPoint} representing the tile at the wall’s base
     * @param neighbor    the {@link WorldPoint} to test for adjacency
     * @return {@code true} if {@code neighbor} is exactly one tile away from {@code point}
     *         in the direction indicated by {@code orientation}, {@code false} otherwise
     */
    private static boolean searchNeighborPoint(int orientation, WorldPoint point, WorldPoint neighbor) {
        int dx = neighbor.getX() - point.getX();
        int dy = neighbor.getY() - point.getY();

        switch (orientation) {
            case 1:   // west
                return dx == -1 && dy == 0;
            case 2:   // north
                return dx == 0  && dy == 1;
            case 4:   // east
                return dx == 1  && dy == 0;
            case 8:   // south
                return dx == 0  && dy == -1;
            case 16:  // northwest
                return dx == -1 && dy == 1;
            case 32:  // northeast
                return dx == 1  && dy == 1;
            case 64:  // southeast
                return dx == 1  && dy == -1;
            case 128: // southwest
                return dx == -1 && dy == -1;
            default:
                return false;
        }
    }

    /**
     * @param path list of worldpoints
     * @return closest tile index
     */
    public static int getClosestTileIndex(List<WorldPoint> path) {
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        return WalkerPathGeometry.getClosestTileIndex(path, playerLoc, getClosestIndexReachableTiles(playerLoc));
    }

    static int getClosestTileIndex(List<WorldPoint> path, WorldPoint playerLoc) {
        return WalkerPathGeometry.getClosestTileIndex(path, playerLoc, getClosestIndexReachableTiles(playerLoc));
    }

    // 3-arg getClosestTileIndex (pure) moved to geometry/WalkerPathGeometry (P1)

    /** Step budget of {@link #getClosestIndexReachableTiles}'s BFS; also the route-blocked scan gate's bound. */
    private static final int CLOSEST_INDEX_REACHABLE_STEP_BUDGET = 20;

    private static HashMap<WorldPoint, Integer> getClosestIndexReachableTiles(WorldPoint playerLoc) {
        if (playerLoc == null) {
            return new HashMap<>();
        }
        HashMap<WorldPoint, Integer> tiles = Rs2Tile.getReachableTilesFromTile(playerLoc, CLOSEST_INDEX_REACHABLE_STEP_BUDGET);

        // If an animation/shortcut puts the player on a collision-odd tile, keep route progress
        // anchored by distance instead of repeatedly recalculating an empty reachable set.
        if (tiles.isEmpty()) {
            tiles = Rs2Tile.getReachableTilesFromTileIgnoreCollision(playerLoc, CLOSEST_INDEX_REACHABLE_STEP_BUDGET);
        }
        return tiles;
    }

    static int stabilizeRouteProgressIndex(List<WorldPoint> path, int closestIdx, WorldPoint target, WorldPoint playerLoc) {
        if (path == null || path.isEmpty() || closestIdx < 0 || closestIdx >= path.size()) {
            return closestIdx;
        }

        WorldPoint pathStart = path.get(0);
        WorldPoint pathEnd = path.get(path.size() - 1);
        boolean routeChanged = routeState.routeProgressTarget == null
                || !routeState.routeProgressTarget.equals(target)
                || routeState.routeProgressPathSize != path.size()
                || !Objects.equals(routeState.routeProgressPathStart, pathStart)
                || !Objects.equals(routeState.routeProgressPathEnd, pathEnd)
                || routeState.routeProgressIdx >= path.size();
        if (routeChanged) {
            routeState.routeProgressTarget = target;
            routeState.routeProgressPathStart = pathStart;
            routeState.routeProgressPathEnd = pathEnd;
            routeState.routeProgressPathSize = path.size();
            routeState.routeProgressIdx = closestIdx;
            routeState.routeProgressAdvancedAtMs = System.currentTimeMillis();
            return closestIdx;
        }

        if (routeState.routeProgressIdx < 0 || closestIdx >= routeState.routeProgressIdx) {
            if (closestIdx > routeState.routeProgressIdx) {
                recordRouteProgressAdvanced();
            }
            routeState.routeProgressIdx = closestIdx;
            return closestIdx;
        }

        int forwardIdx = closestForwardPathIndex(path, routeState.routeProgressIdx, playerLoc);
        if (forwardIdx >= routeState.routeProgressIdx) {
            if (forwardIdx > routeState.routeProgressIdx) {
                routeState.routeProgressIdx = forwardIdx;
                recordRouteProgressAdvanced();
            }
            return routeState.routeProgressIdx;
        }
        return routeState.routeProgressIdx;
    }

    static void hintRouteProgressIndex(List<WorldPoint> path, int hintedIdx, WorldPoint target) {
        if (path == null || path.isEmpty() || hintedIdx < 0 || hintedIdx >= path.size()) {
            return;
        }

        WorldPoint pathStart = path.get(0);
        WorldPoint pathEnd = path.get(path.size() - 1);
        boolean routeChanged = routeState.routeProgressTarget == null
                || !routeState.routeProgressTarget.equals(target)
                || routeState.routeProgressPathSize != path.size()
                || !Objects.equals(routeState.routeProgressPathStart, pathStart)
                || !Objects.equals(routeState.routeProgressPathEnd, pathEnd)
                || routeState.routeProgressIdx >= path.size();
        if (routeChanged) {
            routeState.routeProgressTarget = target;
            routeState.routeProgressPathStart = pathStart;
            routeState.routeProgressPathEnd = pathEnd;
            routeState.routeProgressPathSize = path.size();
            routeState.routeProgressIdx = hintedIdx;
            recordRouteProgressAdvanced();
            return;
        }

        if (hintedIdx > routeState.routeProgressIdx) {
            routeState.routeProgressIdx = hintedIdx;
            recordRouteProgressAdvanced();
        }
    }

    static int advanceIndexPastRecentTransportEdge(List<WorldPoint> path, int index, WorldPoint playerLoc) {
        if (path == null || path.isEmpty() || index < 0 || index >= path.size()
                || !isRecentTransportEdgeWindow()) {
            return index;
        }
        WorldPoint origin = routeState.lastTransportOriginLocation;
        WorldPoint destination = routeState.lastTransportDestinationLocation;
        if (origin == null || destination == null || playerLoc == null
                || playerLoc.getPlane() != destination.getPlane()
                || playerLoc.distanceTo2D(destination) > 3) {
            return index;
        }

        int scanEndExclusive = Math.min(path.size(), index + 8);
        int lastTransportEdgeIdx = -1;
        for (int i = index; i < scanEndExclusive; i++) {
            WorldPoint point = path.get(i);
            if (isNearSamePlane(point, origin, 2) || isNearSamePlane(point, destination, 2)) {
                lastTransportEdgeIdx = i;
            }
        }
        if (lastTransportEdgeIdx >= index && lastTransportEdgeIdx + 1 < path.size()) {
            return lastTransportEdgeIdx + 1;
        }
        return index;
    }

    private static int closestForwardPathIndex(List<WorldPoint> path, int fromIdx, WorldPoint playerLoc) {
        if (path == null || path.isEmpty() || playerLoc == null || fromIdx < 0 || fromIdx >= path.size()) {
            return -1;
        }
        int bestIdx = -1;
        int bestDist = Integer.MAX_VALUE;
        int toIdxExclusive = Math.min(path.size(), fromIdx + ROUTE_PROGRESS_FORWARD_SEARCH_TILES + 1);
        for (int i = fromIdx; i < toIdxExclusive; i++) {
            WorldPoint point = path.get(i);
            if (point == null || point.getPlane() != playerLoc.getPlane()) {
                continue;
            }
            int dist = playerLoc.distanceTo2D(point);
            if (dist < bestDist) {
                bestIdx = i;
                bestDist = dist;
            }
        }
        return bestIdx;
    }

    private static void resetRouteProgress() {
        routeState.routeProgressIdx = -1;
        routeState.routeProgressTarget = null;
        routeState.routeProgressPathStart = null;
        routeState.routeProgressPathEnd = null;
        routeState.routeProgressPathSize = -1;
        routeState.routeProgressAdvancedAtMs = 0L;
    }

    private static void recordRouteProgressAdvanced() {
        long now = System.currentTimeMillis();
        routeState.routeProgressAdvancedAtMs = now;
        routeState.lastMovedTimeMs = now;
        routeState.stuckCount = 0;
    }

    private static boolean isRecentTransportEdgeWindow() {
        long handledAt = routeState.lastTransportHandledAtMs;
        if (handledAt <= 0L) {
            return false;
        }
        long ageMs = System.currentTimeMillis() - handledAt;
        return ageMs >= 0L && ageMs <= RECENT_TRANSPORT_EDGE_SUPPRESS_MS;
    }

    private static boolean isNearSamePlane(WorldPoint a, WorldPoint b, int distance) {
        return a != null
                && b != null
                && a.getPlane() == b.getPlane()
                && a.distanceTo2D(b) <= distance;
    }

    /**
     * Force the walker to recalculate path
     */
    public static void recalculatePath() {
        WorldPoint goal = currentTarget;
        if (goal == null) {
            return;
        }
        // Keep the active request installed while replacing only its route generation.
        Rs2WalkerLifecycleRuntime.applyWalkerDestination(goal, false);
    }

    private static final WalkerActions NAVIGATION_WALKER_ACTIONS = new WalkerActions() {
        private String lastActionType = "none";

		@Override
		public SpellEquipmentObservation observeEquipment() {
			return Rs2SpellEquipmentScene.observe();
		}

		@Override
		public SpellEquipmentPreparation observeSpellEquipment(RouteInteraction spell, SpellEquipmentTransaction retained) {
			return Rs2SpellEquipmentScene.prepare(spell, retained);
		}

		@Override
		public boolean interactEquipment(RouteInteraction interaction, SpellEquipmentTransaction transaction,
			java.util.function.BooleanSupplier permitted) {
			boolean issued = Rs2SpellEquipmentScene.dispatch(interaction, transaction, permitted);
			lastActionType = issued ? "spell-equipment" : "spell-equipment-rejected";
			return issued;
		}

        @Override
        public boolean clickTile(WorldPoint target) {
            if (walkMiniMap(target)) {
                lastActionType = "minimap-route-tile";
                return true;
            }
            if (walkFastCanvasOnScreenOnly(target, true)) {
                lastActionType = "canvas-route-tile";
                return true;
            }
            lastActionType = "route-tile-rejected";
            return false;
        }

		@Override
		public boolean clickTile(WorldPoint target, String selection) {
			if ("route-rejoin".equals(selection) && !Rs2Tile.isTileReachable(target)) {
				lastActionType = "route-rejoin-unreachable";
				return false;
			}
			if ("interaction-edge-crossing".equals(selection) || "route-end-approach".equals(selection)) {
				if (walkFastCanvasOnScreenOnly(target, true)) {
					lastActionType = "canvas-" + selection;
					return true;
				}
				if (walkMiniMap(target)) {
					lastActionType = "minimap-" + selection;
					return true;
				}
				lastActionType = selection + "-rejected";
				return false;
			}
			return clickTile(target);
		}

		@Override
		public boolean interact(RouteInteraction interaction) {
			if (interaction == null) {
				lastActionType = "interaction-rejected";
				return false;
			}
			if (interaction.getKind() == RouteInteraction.Kind.MINEABLE) {
				TileObject object = Rs2LiveScene.exactMineableAt(interaction.getObjectTile());
				if (object == null) {
					lastActionType = "mineable-already-cleared";
					return true;
				}
				boolean issued = Rs2GameObject.interact(object, interaction.getAction());
				lastActionType = issued ? "mineable-interaction" : "mineable-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.DOOR) {
				WorldPoint player = Rs2Player.getWorldLocation();
				OrdinaryDoor door = new Rs2DoorScene(player, INTERACTION_CHAIN_RANGE).findOrdinaryDoor(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()));
				if (door == null || !door.getTile().equals(interaction.getObjectTile())
						|| !door.getAction().equalsIgnoreCase(interaction.getAction())) {
					lastActionType = "door-already-open";
					return true;
				}
				boolean issued = Rs2GameObject.interact(door.getObject(), interaction.getAction());
				lastActionType = issued ? "door-interaction" : "door-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.ADJACENT_TRANSPORT) {
				AdjacentTransport transport = new Rs2AdjacentTransportScene().find(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()));
				if (transport == null || transport.getObjectId() != interaction.getObjectId()
						|| !transport.getAction().equalsIgnoreCase(interaction.getAction())) {
					lastActionType = "adjacent-transport-unavailable";
					return false;
				}
				boolean issued = Rs2GameObject.interact(transport.getObject(), interaction.getAction());
				lastActionType = issued ? "adjacent-transport-interaction"
					: "adjacent-transport-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.CATALOG_TRANSITION) {
				Rs2CatalogTransitionScene.DispatchResult result = Rs2CatalogTransitionScene.dispatch(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()),
					interaction.getAction(), interaction.getObjectId());
				lastActionType = result == Rs2CatalogTransitionScene.DispatchResult.ISSUED
					? "catalog-transition-interaction"
					: result == Rs2CatalogTransitionScene.DispatchResult.PREPARED
						? "catalog-transition-preparation"
						: "catalog-transition-interaction-rejected";
				// Preparation changes the next observed stage but is not the crossing command.
				// Keeping it out of the in-flight slot allows the engine to click the barrier
				// as soon as the equipment cache confirms the amulet.
				return result == Rs2CatalogTransitionScene.DispatchResult.ISSUED;
			}
			if (interaction.getKind() == RouteInteraction.Kind.ITEM_TELEPORT) {
				boolean issued = new Rs2ItemTeleportScene().dispatch(interaction);
				lastActionType = issued ? "item-teleport-interaction" : "item-teleport-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.SIMPLE_TELEPORT) {
				if (interaction.getObjectId() == TransportType.TELEPORTATION_SPELL.ordinal()) {
					boolean issued = net.runelite.client.plugins.microbot.util.walker.transport.Rs2SpellTeleportScene.dispatch(interaction);
					lastActionType = issued ? "spell-teleport-stage" : "spell-teleport-stage-rejected";
					return issued;
				}
				lastActionType = "simple-teleport-unavailable";
				return false;
			}
			if (interaction.getKind() == RouteInteraction.Kind.NPC_TRANSPORT) {
				Transport transport = TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
					interaction.getFrom(), interaction.getTo()).stream()
					.filter(NpcTransportPolicy::isEligible)
					.filter(candidate -> candidate.getObjectId() == interaction.getObjectId())
					.filter(candidate -> candidate.getAction()
						.equalsIgnoreCase(interaction.getAction()))
					.findFirst().orElse(null);
				if (transport == null) {
					lastActionType = "npc-transport-unavailable";
					return false;
				}
				boolean issued;
				Rs2NpcModel npc = Rs2NpcTransportScene.findNpc(transport);
				if (npc != null) {
					issued = Rs2Npc.interact(npc, transport.getAction());
				} else {
					Rs2TileObjectModel object = Rs2NpcTransportScene.findTravelObject(transport);
					String liveAction = Rs2NpcTransportScene.resolveLiveAction(object,
						transport.getAction());
					if (object == null || liveAction == null) {
						lastActionType = "npc-transport-unavailable";
						return false;
					}
					issued = object.click(liveAction);
				}
				lastActionType = issued ? "npc-transport-interaction"
					: "npc-transport-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT) {
				if (NpcDialogueTransportPolicy.CANCEL_UNAVAILABLE_ACTION.equals(interaction.getAction())) {
					boolean issued = Rs2NpcDialogueTransportScene.cancelUnavailableFossilDestination(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()));
					lastActionType = issued ? "npc-dialogue-transport-interaction"
						: "npc-dialogue-transport-interaction-rejected";
					return issued;
				}
				Transport transport = TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
					interaction.getFrom(), interaction.getTo()).stream()
					.filter(NpcDialogueTransportPolicy::isEligible)
					.filter(candidate -> candidate.getObjectId() == interaction.getObjectId())
					.findFirst().orElse(null);
				if (transport == null) {
					lastActionType = "npc-dialogue-transport-unavailable";
					return false;
				}
				boolean issued;
				if (NpcDialogueTransportPolicy.EQUIP_GHOSTSPEAK_ACTION.equals(
						interaction.getAction())) {
					issued = NpcDialogueTransportPolicy.isGhostCaptain(transport)
						&& Rs2NpcDialogueTransportScene.equipGhostspeakItem();
				} else if (NpcDialogueTransportPolicy.EQUIP_GOLD_HELMET_ACTION.equals(
						interaction.getAction())) {
					issued = NpcDialogueTransportPolicy.isDondakan(transport)
						&& Rs2NpcDialogueTransportScene.equipGoldHelmet();
				} else if (NpcDialogueTransportPolicy.CONTINUE_ACTION.equals(interaction.getAction())) {
					issued = Rs2Dialogue.hasContinue();
					if (issued) {
						Rs2Dialogue.clickContinue();
					}
				} else if (NpcDialogueTransportPolicy.TRAVEL_REQUEST_ACTION.equals(
						interaction.getAction())) {
					issued = NpcDialogueTransportPolicy.isCabinBoyHerbert(transport)
						&& Rs2NpcDialogueTransportScene.selectTravelRequestOption();
				} else if (NpcDialogueTransportPolicy.CONFIRM_ACTION.equals(interaction.getAction())) {
					issued = Rs2NpcDialogueTransportScene.selectConfirmOption();
				} else if (NpcDialogueTransportPolicy.isDestinationAction(interaction.getAction())) {
					issued = Rs2NpcDialogueTransportScene.selectDestinationOption(
						NpcDialogueTransportPolicy.destinationOption(transport));
				} else {
					Rs2NpcModel npc = Rs2NpcDialogueTransportScene.findActorNpc(transport);
					if (npc != null) {
						String liveAction = Rs2NpcDialogueTransportScene.resolveLiveNpcAction(
							npc, transport);
						// One ranged click; the server paths the rest. Walkability is checked
						// once at dispatch (never line-of-sight — it fails on solid geometry).
						issued = interaction.getAction().equalsIgnoreCase(transport.getAction())
							&& liveAction != null
							&& Rs2Npc.canWalkTo(npc, 20)
							&& Rs2Npc.interact(npc, liveAction);
					} else {
						Rs2TileObjectModel object =
							Rs2NpcDialogueTransportScene.findActorObject(transport);
						String liveAction = Rs2NpcDialogueTransportScene.resolveLiveObjectAction(
							object, transport.getAction());
						issued = object != null && liveAction != null && object.click(liveAction);
					}
				}
				lastActionType = issued ? "npc-dialogue-transport-interaction"
					: "npc-dialogue-transport-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.CHARTER_SHIP) {
				Transport transport = TransportEdgeMatcher.find(Rs2PathApi.getTransports(),
					interaction.getFrom(), interaction.getTo()).stream()
					.filter(CharterShipPolicy::isEligible)
					.filter(candidate -> candidate.getObjectId() == interaction.getObjectId())
					.findFirst().orElse(null);
				if (transport == null) {
					lastActionType = "charter-ship-unavailable";
					return false;
				}
				boolean issued;
				if (CharterShipPolicy.CONFIRM_ACTION.equals(interaction.getAction())) {
					issued = Rs2CharterShipScene.confirmTravel();
				} else if (CharterShipPolicy.isDestinationAction(interaction.getAction())) {
					issued = Rs2CharterShipScene.selectDestination(transport.getDisplayInfo());
				} else {
					Rs2NpcModel npc = Rs2CharterShipScene.findNpc(transport);
					issued = npc != null
						&& interaction.getAction().equalsIgnoreCase(transport.getAction())
						&& Rs2Npc.interact(npc, transport.getAction());
				}
				lastActionType = issued ? "charter-ship-interaction"
					: "charter-ship-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.FAIRY_RING) {
				boolean issued;
				if (FairyRingPolicy.isStageAction(interaction.getAction())) {
					issued = Rs2FairyRingScene.clickStage(interaction.getAction());
				} else {
					issued = Rs2FairyRingScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction());
				}
				lastActionType = issued ? "fairy-ring-interaction"
					: "fairy-ring-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.SPIRIT_TREE) {
				boolean issued;
				if (SpiritTreePolicy.isDestinationAction(interaction.getAction())) {
					issued = Rs2SpiritTreeScene.selectDestination(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction().substring(
							SpiritTreePolicy.DESTINATION_ACTION_PREFIX.length()));
				} else {
					issued = Rs2SpiritTreeScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				}
				lastActionType = issued ? "spirit-tree-interaction"
					: "spirit-tree-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.GNOME_GLIDER) {
				boolean issued;
				if (GnomeGliderPolicy.isDestinationAction(interaction.getAction())) {
					issued = Rs2GnomeGliderScene.selectDestination(
						interaction.getAction().substring(
							GnomeGliderPolicy.DESTINATION_ACTION_PREFIX.length()));
				} else {
					issued = Rs2GnomeGliderScene.interactNpc(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				}
				lastActionType = issued ? "gnome-glider-interaction"
					: "gnome-glider-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.QUETZAL) {
				boolean issued;
				if (QuetzalPolicy.isDestinationAction(interaction.getAction())) {
					issued = Rs2QuetzalScene.selectDestination(
						interaction.getAction().substring(
							QuetzalPolicy.DESTINATION_ACTION_PREFIX.length()));
				} else {
					issued = Rs2QuetzalScene.interactNpc(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction());
				}
				lastActionType = issued ? "quetzal-interaction"
					: "quetzal-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.TELEPORTATION_LEVER) {
				boolean issued;
				if (TeleportationLeverPolicy.WARNING_CONTINUE_ACTION.equals(
						interaction.getAction())) {
					issued = Rs2TeleportationLeverScene.continueWarning();
				} else if (TeleportationLeverPolicy.CONFIRM_ACTION.equals(
						interaction.getAction())) {
					issued = Rs2TeleportationLeverScene.confirmWarning();
				} else {
					issued = Rs2TeleportationLeverScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				}
				lastActionType = issued ? "teleportation-lever-interaction"
					: "teleportation-lever-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.WILDERNESS_DITCH) {
				boolean issued = WildernessDitchPolicy.WARNING_ACTION.equals(
					interaction.getAction())
					? Rs2WildernessDitchScene.confirmWarning()
					: Rs2WildernessDitchScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "wilderness-ditch-interaction"
					: "wilderness-ditch-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.JUNGLE_OBSTACLE) {
				boolean issued = Rs2JungleObstacleScene.interactObject(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()),
					interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "jungle-obstacle-interaction"
					: "jungle-obstacle-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.CANOE) {
				boolean issued = CanoePolicy.isUiStageAction(interaction.getAction())
					? Rs2CanoeScene.dispatchStage(interaction.getAction())
					: Rs2CanoeScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "canoe-interaction"
					: "canoe-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.MINECART) {
				boolean issued = Rs2MinecartScene.dispatch(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()),
					interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "minecart-interaction"
					: "minecart-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.TELEPORTATION_PORTAL) {
				boolean issued = Rs2TeleportationPortalScene.interactObject(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()),
					interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "teleportation-portal-interaction"
					: "teleportation-portal-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.MINIGAME_TELEPORT) {
				boolean issued = Rs2MinigameTeleportScene.dispatch(
					new PlannedEdge(interaction.getFrom(), interaction.getTo()),
					interaction.getAction());
				lastActionType = issued ? "minigame-teleport-interaction"
					: "minigame-teleport-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.MAGIC_MUSHTREE) {
				boolean issued = MagicMushtreePolicy.isDestinationAction(
					interaction.getAction())
					? Rs2MagicMushtreeScene.selectDestination(
						interaction.getAction().substring(
							MagicMushtreePolicy.DESTINATION_ACTION_PREFIX.length()))
					: Rs2MagicMushtreeScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "magic-mushtree-interaction"
					: "magic-mushtree-interaction-rejected";
				return issued;
			}
			if (interaction.getKind() == RouteInteraction.Kind.HOT_AIR_BALLOON) {
				boolean issued = HotAirBalloonPolicy.isDestinationAction(
					interaction.getAction())
					? Rs2HotAirBalloonScene.selectDestination(
						interaction.getAction().substring(
							HotAirBalloonPolicy.DESTINATION_ACTION_PREFIX.length()))
					: Rs2HotAirBalloonScene.interactObject(
						new PlannedEdge(interaction.getFrom(), interaction.getTo()),
						interaction.getAction(), interaction.getObjectId());
				lastActionType = issued ? "hot-air-balloon-interaction"
					: "hot-air-balloon-interaction-rejected";
				return issued;
			}
			lastActionType = "interaction-rejected";
			return false;
		}

        @Override
        public String getLastActionType() {
            return lastActionType;
        }

		@Override
		public boolean interactionPreparedOnly() {
			return "catalog-transition-preparation".equals(lastActionType);
		}
    };

    private static final NavigationWalkCoordinator.Driver NAVIGATION_WALK_DRIVER =
            new NavigationWalkCoordinator.Driver() {
                @Override
                public NavigationExecutionResult executePass(String reason) {
                    return executeNavigationPass(reason);
                }

                @Override
                public boolean isCancelled(WorldPoint target) {
                    return isWalkCancelled(target);
                }

                @Override
                public void clearTarget(String reason) {
                    setTarget(null, reason);
                }

                @Override
                public void requestReplan() {
                    recalculatePath();
                }

                @Override
                public void awaitReplan(WorldPoint target, long generation) {
                    sleepUntil(() -> isWalkCancelled(target)
                            || publishedGenerationAfter(generation), PATHFINDER_NULL_WAIT_MS);
                }

                @Override
                public void awaitProgress(WorldPoint target) {
                    WorldPoint before = Rs2Player.getWorldLocation();
                    if (Rs2Player.isMoving()) {
                        sleepUntil(() -> isWalkCancelled(target) || !Rs2Player.isMoving()
                                || navigationHandoffDue()
                                || NavigationEngineRuntime.hasUnobservedRecovery(), 1_200);
                    } else {
                        sleepUntil(() -> isWalkCancelled(target)
                                || playerMovementChanged(before)
                                || NavigationEngineRuntime.hasUnobservedRecovery(), 600);
                    }
                }
            };

	private static NavigationExecutionResult executeNavigationPass(String reason) {
        long now = System.currentTimeMillis();
        WorldPoint player = Rs2Player.getWorldLocation();
        RoutePlan plan = RoutePlannerRuntime.getPublishedPlan();
		InteractionObservations interactions = observeRouteInteractions(plan, player);
		NavigationObservation observation = NavigationObservation.route(now,
                player, plan,
                Rs2Player.isMoving(), Rs2Player.isAnimating(), Rs2Player.isInteracting(),
                false, false, false, reason)
				.withMovementDestination(currentMovementDestination())
				.withRouteInteractions(interactions.current, interactions.next);
        NavigationExecutionResult result = NavigationEngineRuntime.execute(observation,
                NAVIGATION_WALKER_ACTIONS);
        NavigationDecision decision = result.getDecision();
        NavigationSnapshot snapshot = NavigationEngineRuntime.getSnapshot();
        if (result.isEngineOwned() && decision.getType() == NavigationDecision.Type.CLICK_TILE
                && snapshot != null) {
            WebWalkLog.navigationCommand(snapshot.getRequestId(), snapshot.getGeneration(),
                    decision.getTarget(), decision.getTargetRawIndex(),
                    decision.getTargetSmoothedIndex(), decision.getTargetDistance(),
                    decision.getTargetReach(), decision.getTargetHandoffDistance(),
                    decision.getTargetSelection(), result.getActionType(), result.isCommandIssued(),
                    decision.getReason());
        }
		if (result.isEngineOwned() && decision.getType() == NavigationDecision.Type.INTERACT
				&& decision.getInteraction() != null && snapshot != null) {
			RouteInteraction interaction = decision.getInteraction();
			WebWalkLog.navigationInteractionCommand(snapshot.getRequestId(), snapshot.getGeneration(),
					interaction.getObjectTile(), interaction.getRawEdgeIndex(),
					snapshot.getPlayerLocation() == null ? -1
							: snapshot.getPlayerLocation().distanceTo2D(interaction.getObjectTile()),
					"interaction-chain-ready".equals(decision.getReason())
							? INTERACTION_CHAIN_RANGE : HANDLER_RANGE,
					"interaction-" + interaction.getKind().name().toLowerCase(),
					interaction.getAction(),
					result.getActionType(), result.isCommandIssued(), decision.getReason());
		}
        if (result.isEngineOwned() && snapshot != null
                && decision.getRecoveryCause() != RecoveryCause.NONE
                && (decision.getType() == NavigationDecision.Type.CLICK_TILE
                || decision.getType() == NavigationDecision.Type.REQUEST_REPLAN
                || decision.getType() == NavigationDecision.Type.FAIL)) {
            WebWalkLog.navigationRecovery(snapshot.getRequestId(), snapshot.getGeneration(),
                    decision.getType().name(), decision.getRecoveryCause().name(),
                    decision.getRecoveryAttempt(), decision.getRecoveryBudget(),
                    decision.getRecoveryAgeMs(), decision.getBlockedEdgeIndex(),
                    snapshot.getPlayerLocation(), snapshot.getRawProgressIndex(),
                    snapshot.getRouteDistance(), decision.getReason());
        }
        if (result.isEngineOwned() && snapshot != null
                && decision.getRecoveryCause() == RecoveryCause.COMMAND_DESTINATION_MISMATCH) {
            WebWalkLog.navigationAcknowledgementMismatch(snapshot.getRequestId(),
                    snapshot.getGeneration(), decision.getRecoveryExpectedTarget(),
                    decision.getRecoveryObservedDestination(), snapshot.getPlayerLocation(),
                    decision.getRecoveryAttempt(), decision.getRecoveryBudget(),
                    decision.getRecoveryAgeMs(), snapshot.getRawProgressIndex(),
                    snapshot.getRouteDistance(), decision.getReason());
        }
        return result;
    }

	private static InteractionObservations observeRouteInteractions(RoutePlan plan, WorldPoint player) {
		if (plan == null || player == null || plan.getRawPath().size() < 2) {
			return InteractionObservations.NONE;
		}
		NavigationSnapshot snapshot = NavigationEngineRuntime.getSnapshot();
		RouteInteraction pending = snapshot == null ? null : snapshot.getPendingInteraction();
		Rs2LiveScene mineableScene = new Rs2LiveScene(player, null);
		Rs2DoorScene doorScene = new Rs2DoorScene(player, INTERACTION_CHAIN_RANGE);
		Rs2AdjacentTransportScene transportScene = new Rs2AdjacentTransportScene();
		Rs2CatalogTransitionScene transitionScene = new Rs2CatalogTransitionScene();
		Rs2SimpleTeleportScene teleportScene = new Rs2SimpleTeleportScene();
		Rs2NpcTransportScene npcTransportScene = new Rs2NpcTransportScene();
		Rs2NpcDialogueTransportScene npcDialogueScene = new Rs2NpcDialogueTransportScene();
		Rs2CharterShipScene charterShipScene = new Rs2CharterShipScene();
		Rs2FairyRingScene fairyRingScene = new Rs2FairyRingScene();
		Rs2SpiritTreeScene spiritTreeScene = new Rs2SpiritTreeScene();
		Rs2GnomeGliderScene gnomeGliderScene = new Rs2GnomeGliderScene();
		Rs2QuetzalScene quetzalScene = new Rs2QuetzalScene();
		Rs2TeleportationLeverScene teleportationLeverScene =
			new Rs2TeleportationLeverScene();
		Rs2WildernessDitchScene wildernessDitchScene = new Rs2WildernessDitchScene();
		Rs2JungleObstacleScene jungleObstacleScene = new Rs2JungleObstacleScene();
		Rs2CanoeScene canoeScene = new Rs2CanoeScene();
		Rs2MinecartScene minecartScene = new Rs2MinecartScene();
		Rs2TeleportationPortalScene teleportationPortalScene =
			new Rs2TeleportationPortalScene();
		Rs2MinigameTeleportScene minigameTeleportScene =
			new Rs2MinigameTeleportScene();
		Rs2MagicMushtreeScene magicMushtreeScene = new Rs2MagicMushtreeScene();
		Rs2HotAirBalloonScene hotAirBalloonScene = new Rs2HotAirBalloonScene();
		long coinsHeld = Rs2Inventory.itemQuantity(ItemID.COINS);
		if (pending != null && pending.getGeneration() == plan.getGeneration()) {
			RouteInteraction current;
			if (pending.getKind() == RouteInteraction.Kind.MINEABLE) {
				current = MINEABLE_ROUTE_SCANNER.observePending(pending, mineableScene,
					hasPickaxe(), HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.DOOR) {
				current = ORDINARY_DOOR_ROUTE_SCANNER.observePending(pending, player,
					doorScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.ADJACENT_TRANSPORT) {
				current = ADJACENT_TRANSPORT_ROUTE_SCANNER.observePending(pending, player,
					transportScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.CATALOG_TRANSITION) {
				current = CATALOG_TRANSITION_ROUTE_SCANNER.observePending(pending, player,
					transitionScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.SIMPLE_TELEPORT) {
				current = SIMPLE_TELEPORT_ROUTE_SCANNER.observePending(pending, player,
					teleportScene);
			} else if (pending.getKind() == RouteInteraction.Kind.ITEM_TELEPORT) {
				current = ITEM_TELEPORT_ROUTE_SCANNER.observePending(pending, player,
					new Rs2ItemTeleportScene());
			} else if (pending.getKind() == RouteInteraction.Kind.NPC_TRANSPORT) {
				current = NPC_TRANSPORT_ROUTE_SCANNER.observePending(pending, player,
					npcTransportScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT) {
				current = NPC_DIALOGUE_TRANSPORT_ROUTE_SCANNER.observePending(pending, player,
					npcDialogueScene, HANDLER_RANGE, coinsHeld);
			} else if (pending.getKind() == RouteInteraction.Kind.CHARTER_SHIP) {
				current = CHARTER_SHIP_ROUTE_SCANNER.observePending(pending, player,
					charterShipScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.FAIRY_RING) {
				current = FAIRY_RING_ROUTE_SCANNER.observePending(pending, player,
					fairyRingScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.SPIRIT_TREE) {
				current = SPIRIT_TREE_ROUTE_SCANNER.observePending(pending, player,
					spiritTreeScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.GNOME_GLIDER) {
				current = GNOME_GLIDER_ROUTE_SCANNER.observePending(pending, player,
					gnomeGliderScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.QUETZAL) {
				current = QUETZAL_ROUTE_SCANNER.observePending(pending, player,
					quetzalScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.TELEPORTATION_LEVER) {
				current = TELEPORTATION_LEVER_ROUTE_SCANNER.observePending(pending, player,
					teleportationLeverScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.WILDERNESS_DITCH) {
				current = WILDERNESS_DITCH_ROUTE_SCANNER.observePending(pending, player,
					wildernessDitchScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.JUNGLE_OBSTACLE) {
				current = JUNGLE_OBSTACLE_ROUTE_SCANNER.observePending(pending, player,
					jungleObstacleScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.CANOE) {
				current = CANOE_ROUTE_SCANNER.observePending(pending, player, canoeScene,
					HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.MINECART) {
				current = MINECART_ROUTE_SCANNER.observePending(pending, player,
					minecartScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.TELEPORTATION_PORTAL) {
				current = TELEPORTATION_PORTAL_ROUTE_SCANNER.observePending(pending, player,
					teleportationPortalScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.MINIGAME_TELEPORT) {
				current = MINIGAME_TELEPORT_ROUTE_SCANNER.observePending(pending, player,
					minigameTeleportScene);
			} else if (pending.getKind() == RouteInteraction.Kind.MAGIC_MUSHTREE) {
				current = MAGIC_MUSHTREE_ROUTE_SCANNER.observePending(pending, player,
					magicMushtreeScene, HANDLER_RANGE);
			} else if (pending.getKind() == RouteInteraction.Kind.HOT_AIR_BALLOON) {
				current = HOT_AIR_BALLOON_ROUTE_SCANNER.observePending(pending, player,
					hotAirBalloonScene, HANDLER_RANGE);
			} else {
				return InteractionObservations.NONE;
			}
			RouteInteraction next = current != null
					&& current.getStatus() == RouteInteraction.Status.CLEARED
					? scanForwardRouteInteraction(plan, player, pending.getRawEdgeIndex() + 1,
						mineableScene, doorScene, transportScene, transitionScene, teleportScene,
						npcTransportScene, npcDialogueScene, charterShipScene, fairyRingScene,
						spiritTreeScene, gnomeGliderScene, quetzalScene,
						teleportationLeverScene, wildernessDitchScene, jungleObstacleScene,
						canoeScene,
						minecartScene,
						teleportationPortalScene, minigameTeleportScene,
						magicMushtreeScene, hotAirBalloonScene,
						INTERACTION_CHAIN_RANGE, coinsHeld)
					: null;
			return new InteractionObservations(current, next);
		}

		List<WorldPoint> rawPath = plan.getRawPath();
		int start = snapshot != null && snapshot.getGeneration() == plan.getGeneration()
				? Math.max(0, snapshot.getRawProgressIndex())
				: Math.max(0, getClosestTileIndex(rawPath, player));
		return new InteractionObservations(scanForwardRouteInteraction(plan, player, start,
			mineableScene, doorScene, transportScene, transitionScene, teleportScene,
			npcTransportScene, npcDialogueScene, charterShipScene, fairyRingScene,
			spiritTreeScene, gnomeGliderScene, quetzalScene, teleportationLeverScene,
			wildernessDitchScene, jungleObstacleScene,
			canoeScene, minecartScene, teleportationPortalScene,
			minigameTeleportScene, magicMushtreeScene, hotAirBalloonScene,
			HANDLER_RANGE, coinsHeld), null);
	}

	private static RouteInteraction scanForwardRouteInteraction(RoutePlan plan, WorldPoint player,
		int start, Rs2LiveScene mineableScene, Rs2DoorScene doorScene,
		Rs2AdjacentTransportScene transportScene, Rs2CatalogTransitionScene transitionScene,
		Rs2SimpleTeleportScene teleportScene,
		Rs2NpcTransportScene npcTransportScene,
		Rs2NpcDialogueTransportScene npcDialogueScene,
		Rs2CharterShipScene charterShipScene,
		Rs2FairyRingScene fairyRingScene,
		Rs2SpiritTreeScene spiritTreeScene,
		Rs2GnomeGliderScene gnomeGliderScene,
		Rs2QuetzalScene quetzalScene,
		Rs2TeleportationLeverScene teleportationLeverScene,
		Rs2WildernessDitchScene wildernessDitchScene,
		Rs2JungleObstacleScene jungleObstacleScene,
		Rs2CanoeScene canoeScene,
		Rs2MinecartScene minecartScene,
		Rs2TeleportationPortalScene teleportationPortalScene,
		Rs2MinigameTeleportScene minigameTeleportScene,
		Rs2MagicMushtreeScene magicMushtreeScene,
		Rs2HotAirBalloonScene hotAirBalloonScene,
		int interactionRange, long coinsHeld) {
		List<WorldPoint> rawPath = plan.getRawPath();
		start = Math.max(0, start);
		int end = Math.min(rawPath.size() - 1, start + interactionRange);
		RouteInteraction mineable = null;
		boolean inInstance = Rs2LiveScene.isInInstance();
		if (inInstance || player.getRegionID() == net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2ObstacleHandler.MOTHERLODE_MINE_REGION) {
			mineable = MINEABLE_ROUTE_SCANNER.scan(plan.getGeneration(), rawPath, start,
				end - start, mineableScene, hasPickaxe(), interactionRange);
		} else {
			boolean routeEntersMine = false;
			for (int i = start; i <= end; i++) {
				if (rawPath.get(i).getRegionID()
						== net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2ObstacleHandler.MOTHERLODE_MINE_REGION) {
					routeEntersMine = true;
					break;
				}
			}
			if (routeEntersMine) {
				mineable = MINEABLE_ROUTE_SCANNER.scan(plan.getGeneration(), rawPath, start,
					end - start, mineableScene, hasPickaxe(), interactionRange);
			}
		}
		RouteInteraction door = ORDINARY_DOOR_ROUTE_SCANNER.scan(plan, start, end - start,
			player, doorScene, interactionRange);
		RouteInteraction transport = ADJACENT_TRANSPORT_ROUTE_SCANNER.scan(plan, start,
			end - start, player, transportScene, interactionRange);
		RouteInteraction transition = CATALOG_TRANSITION_ROUTE_SCANNER.scan(plan, start,
			end - start, player, transitionScene, interactionRange);
		RouteInteraction teleport = SIMPLE_TELEPORT_ROUTE_SCANNER.scan(plan, start,
			end - start, player, teleportScene);
		RouteInteraction itemTeleport = ITEM_TELEPORT_ROUTE_SCANNER.scan(plan, start,
			end - start, new Rs2ItemTeleportScene());
		RouteInteraction npcTransport = NPC_TRANSPORT_ROUTE_SCANNER.scan(plan, start,
			end - start, player, npcTransportScene, interactionRange);
		RouteInteraction npcDialogue = NPC_DIALOGUE_TRANSPORT_ROUTE_SCANNER.scan(plan, start,
			end - start, player, npcDialogueScene, interactionRange, coinsHeld);
		RouteInteraction charterShip = CHARTER_SHIP_ROUTE_SCANNER.scan(plan, start,
			end - start, player, charterShipScene, interactionRange);
		RouteInteraction fairyRing = FAIRY_RING_ROUTE_SCANNER.scan(plan, start,
			end - start, player, fairyRingScene, interactionRange);
		RouteInteraction spiritTree = SPIRIT_TREE_ROUTE_SCANNER.scan(plan, start,
			end - start, player, spiritTreeScene, interactionRange);
		RouteInteraction gnomeGlider = GNOME_GLIDER_ROUTE_SCANNER.scan(plan, start,
			end - start, player, gnomeGliderScene, interactionRange);
		RouteInteraction quetzal = QUETZAL_ROUTE_SCANNER.scan(plan, start,
			end - start, player, quetzalScene, interactionRange);
		RouteInteraction teleportationLever = TELEPORTATION_LEVER_ROUTE_SCANNER.scan(
			plan, start, end - start, player, teleportationLeverScene, interactionRange);
		RouteInteraction wildernessDitch = WILDERNESS_DITCH_ROUTE_SCANNER.scan(
			plan, start, end - start, player, wildernessDitchScene, interactionRange);
		RouteInteraction jungleObstacle = JUNGLE_OBSTACLE_ROUTE_SCANNER.scan(
			plan, start, end - start, player, jungleObstacleScene, interactionRange);
		RouteInteraction canoe = CANOE_ROUTE_SCANNER.scan(plan, start, end - start, player,
			canoeScene, interactionRange);
		RouteInteraction minecart = MINECART_ROUTE_SCANNER.scan(plan, start,
			end - start, player, minecartScene, interactionRange);
		RouteInteraction teleportationPortal = TELEPORTATION_PORTAL_ROUTE_SCANNER.scan(
			plan, start, end - start, player, teleportationPortalScene,
			interactionRange);
		RouteInteraction minigameTeleport = MINIGAME_TELEPORT_ROUTE_SCANNER.scan(
			plan, start, end - start, player, minigameTeleportScene);
		RouteInteraction magicMushtree = MAGIC_MUSHTREE_ROUTE_SCANNER.scan(
			plan, start, end - start, player, magicMushtreeScene, interactionRange);
		RouteInteraction hotAirBalloon = HOT_AIR_BALLOON_ROUTE_SCANNER.scan(
			plan, start, end - start, player, hotAirBalloonScene, interactionRange);
		return earliestInteraction(mineable, door, transport, transition, teleport, itemTeleport,
			npcTransport, npcDialogue, charterShip, fairyRing, spiritTree, gnomeGlider,
			quetzal, teleportationLever, wildernessDitch, jungleObstacle, canoe, minecart,
			teleportationPortal,
			minigameTeleport, magicMushtree, hotAirBalloon);
	}

	private static RouteInteraction earliestInteraction(RouteInteraction... interactions) {
		return Arrays.stream(interactions).filter(Objects::nonNull)
			.min(Comparator.comparingInt(RouteInteraction::getRawEdgeIndex)).orElse(null);
	}

	private static final class InteractionObservations {
		private static final InteractionObservations NONE = new InteractionObservations(null, null);
		private final RouteInteraction current;
		private final RouteInteraction next;

		private InteractionObservations(RouteInteraction current, RouteInteraction next) {
			this.current = current;
			this.next = next;
		}
	}

	private static boolean hasPickaxe() {
		return Rs2Inventory.hasItem("pickaxe") || Rs2Equipment.isWearing("pickaxe");
	}

    private static WorldPoint currentMovementDestination() {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            if (Microbot.getClient() == null) {
                return null;
            }
            LocalPoint destination = Microbot.getClient().getLocalDestinationLocation();
            return destination == null
                    ? null
                    : WorldPoint.fromLocalInstance(Microbot.getClient(), destination);
        }).orElse(null);
    }

    private static boolean navigationHandoffDue() {
        NavigationSnapshot snapshot = NavigationEngineRuntime.getSnapshot();
        WorldPoint player = Rs2Player.getWorldLocation();
        return snapshot != null && snapshot.getCommandTarget() != null
                && snapshot.getCommandHandoffDistance() >= 0 && player != null
                && snapshot.getCommandTarget().getPlane() == player.getPlane()
                && snapshot.getRoutePlan() != null
                && snapshot.getCommandRawIndex() < snapshot.getRoutePlan().getRawPath().size() - 1
                && snapshot.getCommandTarget().distanceTo2D(player)
                <= snapshot.getCommandHandoffDistance();
    }

    private static boolean publishedGenerationAfter(long generation) {
        return RoutePlannerRuntime.getPublishedPlan() != null
                && RoutePlannerRuntime.getPublishedPlan().getGeneration() > generation;
    }

    private static boolean playerMovementChanged(WorldPoint before) {
        WorldPoint current = Rs2Player.getWorldLocation();
        return Rs2Player.isMoving() || (before != null && current != null && !before.equals(current));
    }

    /**
     * Updates world-map marker and restarts pathfinding for {@code target}. Does not assign
     * {@link #currentTarget}; callers set it when appropriate.
     */
    private static void applyWalkerDestination(WorldPoint target) {
        Rs2WalkerLifecycleRuntime.applyWalkerDestination(target, true);
    }

    /**
     * @param target destination, or {@code null} to clear (prefer {@link #clearWalkingRoute(String)} for observability)
     */
    public static void setTarget(WorldPoint target) {
        setTarget(target, null);
    }

    /**
     * @param clearReasonWhenNull logged when {@code target} is {@code null}; omit only from tests.
     *                         Clearing ({@code target == null}) runs without a {@link net.runelite.client.Client}
     *                         (teardown-safe). Non-null destinations still require a live client and login/player checks.
     */
    public static void setTarget(WorldPoint target, String clearReasonWhenNull) {
        setTarget(target, clearReasonWhenNull, null);
    }

    private static void setTarget(WorldPoint target, String clearReasonWhenNull, Integer reachedDistance) {
        if (target != null && !Microbot.isLoggedIn()) {
            log.warn("Unable to set target: not logged in");
            return;
        }
        if (target != null) {
            Client client = Microbot.getClient();
            if (client == null) {
                log.warn("Unable to set target: client unavailable");
                return;
            }
            boolean localPlayerPresent = Microbot.getClientThread().runOnClientThreadOptional(
                    () -> client.getLocalPlayer() != null).orElse(false);
            if (!Rs2PathApi.isStartPointSet() && !localPlayerPresent) {
                log.warn("Start point is not set and player is null");
                return;
            }
        }

        currentTarget = target;

        if (target == null) {
            NavigationEngineRuntime.finish(clearReasonWhenNull);
            // A completed/cancelled route owns its transport handoff context. Keeping the
            // timestamp alive made an unrelated walk started within 15 seconds inherit
            // post-transport handler suppression and misleading elapsed-time markers.
            clearRecentTransportContext();
            resetRouteProgress();
            logRouteClear(clearReasonWhenNull);
            RoutePlannerRuntime.cancel();

            WorldMapPointManager wmm = Microbot.getWorldMapPointManager();
            if (wmm != null) {
                wmm.remove(Rs2PathApi.getMarker());
            } else if (Rs2LogRateLimit.once(WORLD_MAP_REMOVE_NULL_LOGGED)) {
                log.debug("[Walker] WorldMapPointManager null during route clear — marker may linger until teardown");
            }
            Rs2PathApi.setMarker(null);
            Rs2PathApi.setStartPointSet(false);
        } else {
            if (reachedDistance == null) {
                applyWalkerDestination(target);
            } else {
                Rs2WalkerLifecycleRuntime.applyWalkerDestination(target, true, reachedDistance);
            }
        }
    }

    /**
     * @param start
     * @param end
     */
    public static boolean restartPathfinding(WorldPoint start, WorldPoint end) {
        return Rs2WalkerLifecycleRuntime.restartPathfinding(start, end);
    }

    public static boolean restartPathfinding(WorldPoint start, Set<WorldPoint> ends) {
        return Rs2WalkerLifecycleRuntime.restartPathfinding(start, ends);
    }

    /**
     * @param point
     * @return
     */
    public static Tile getTile(WorldPoint point) {
        return tileAtPoint(point);
    }

    /**
     * @param path
     * @param indexOfStartPoint
     * @return
     */


    static boolean isSettledNearAdjacentSamePlaneLanding(Transport transport,
                                                         WorldPoint playerLoc,
                                                         WorldPoint destWait,
                                                         int maxInclusive) {
        if (!isAdjacentSamePlaneTransport(transport)
                || playerLoc == null
                || destWait == null
                || playerLoc.getPlane() != destWait.getPlane()) {
            return false;
        }
        WorldPoint origin = transport.getOrigin();
        if (origin == null || playerLoc.equals(origin)) {
            return false;
        }
        int destinationDistance = playerLoc.distanceTo2D(destWait);
        if (destinationDistance <= Math.max(1, maxInclusive)
                && playerLoc.distanceTo2D(origin) > 0) {
            return true;
        }
        if (transport.getType() != TransportType.AGILITY_SHORTCUT) {
            return false;
        }

        // Some adjacent shortcut catalogues describe a multi-object animation as one-tile
        // hops. The Falador stepping stones, for example, can carry 3154 -> 3149 while the
        // selected edge says 3154 -> 3153. Accept only a tightly bounded forward, collinear
        // overshoot; sideways movement, reverse movement, and arbitrary teleports still fail.
        int edgeX = destWait.getX() - origin.getX();
        int edgeY = destWait.getY() - origin.getY();
        int movedX = playerLoc.getX() - origin.getX();
        int movedY = playerLoc.getY() - origin.getY();
        int forwardProgress = movedX * edgeX + movedY * edgeY;
        int lateralOffset = Math.abs(movedX * edgeY - movedY * edgeX);
        return forwardProgress > 0
                && forwardProgress <= 6
                && lateralOffset <= 1;
    }

    private static int[] mapSmoothedToRaw(List<WorldPoint> smoothed, List<WorldPoint> raw) {
        if (smoothed == null || raw == null || smoothed.isEmpty() || raw.isEmpty()) {
            return new int[0];
        }
        int[] mapping = new int[smoothed.size()];
        int rawIdx = 0;
        for (int si = 0; si < smoothed.size(); si++) {
            WorldPoint sp = smoothed.get(si);
            while (rawIdx < raw.size() && !raw.get(rawIdx).equals(sp)) {
                rawIdx++;
            }
            mapping[si] = Math.min(rawIdx, raw.size() - 1);
        }
        return mapping;
    }

    private static boolean doorInteractionDeferredForMovement(WorldPoint doorTile) {
        if (!Rs2Player.isMoving()) {
            return false;
        }
        if (config != null && !config.interactWithRouteObstaclesAtRange()) {
            return true;
        }
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        return playerLoc == null
                || doorTile == null
                || doorTile.getPlane() != playerLoc.getPlane()
                || doorTile.distanceTo2D(playerLoc) > DOOR_APPROACH_INTERACT_MAX_TILES;
    }

    private static boolean isAdjacentSamePlaneTransport(Transport transport) {
        return transport != null
                && transport.getOrigin() != null
                && transport.getDestination() != null
                && transport.getOrigin().getPlane() == transport.getDestination().getPlane()
                && transport.getOrigin().distanceTo(transport.getDestination()) <= 1;
    }

    public static boolean isInArea(WorldPoint... worldPoints) {
        if (worldPoints == null || worldPoints.length < 2 || worldPoints[0] == null || worldPoints[1] == null) {
            throw new IllegalArgumentException("isInArea requires two WorldPoints.");
        }
        WorldPoint a = worldPoints[0];
        WorldPoint b = worldPoints[1];
        final int aX = a.getX(), aY = a.getY();
        final int bX = b.getX(), bY = b.getY();

        final int minX = Math.min(aX, bX);
        final int maxX = Math.max(aX, bX);
        final int minY = Math.min(aY, bY);
        final int maxY = Math.max(aY, bY);

        final WorldPoint playerLocation = Rs2Player.getWorldLocation();
        final int playerX = playerLocation.getX();
        final int playerY = playerLocation.getY();

        // draws box from 2 points to check against all variations of player X,Y from said points.
        return (playerX >= minX && playerX <= maxX && playerY >= minY && playerY <= maxY);
    }

    /**
     * Checks if the player's current location is within the specified range from the given center point.
     *
     * @param centerOfArea a WorldPoint which is the center of the desired area,
     * @param range        an int of range to which the boundaries will be drawn in a square,
     * @return true if the player's current location is within the specified area, false otherwise
     */
    public static boolean isInArea(WorldPoint centerOfArea, int range) {
        WorldPoint seCorner = new WorldPoint(centerOfArea.getX() + range, centerOfArea.getY() - range, centerOfArea.getPlane());
        WorldPoint nwCorner = new WorldPoint(centerOfArea.getX() - range, centerOfArea.getY() + range, centerOfArea.getPlane());
        return isInArea(seCorner, nwCorner); // call to our sibling
    }

    public static boolean isNear() {
        final Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        if (pathfinder == null) return false; // idk are we near if we don't have a path?
        final List<WorldPoint> path = pathfinder.getPath();
        if (path == null) return false;

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            return false;
        }
        int index = IntStream.range(0, path.size())
                .filter(f -> {
                    WorldPoint wp = path.get(f);
                    return wp.getPlane() == playerLocation.getPlane()
                            && wp.distanceTo2D(playerLocation) < 3;
                })
                .findFirst().orElse(-1);
        return index >= Math.max(path.size() - 10, 0);
    }

    /**
     * @param target
     * @return
     */
    public static boolean isNear(WorldPoint target) {
        WorldPoint pl = Rs2Player.getWorldLocation();
        return pl != null && pl.equals(target);
    }

    public static boolean isNearPath() {
        final Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        if (pathfinder == null) return true;

        final List<WorldPoint> path = pathfinder.getWalkablePath();
        if (path == null || path.isEmpty()) return true;

        final WorldPoint loc = Rs2Player.getWorldLocation();
        if (loc == null) return true;

        if (config.recalculateDistance() < 0 || routeState.lastPosition.equals(routeState.lastPosition = loc)) {
            return true;
        }

        if (config.usePoh() && PohTeleports.isInHouse()) {
            //Would be nice to have access to current node here and check if the current Node is a POH transport node.
            return true;
        }

        var reachableTiles = Rs2Tile.getReachableTilesFromTile(Rs2Player.getWorldLocation(), config.recalculateDistance() - 1);
        for (WorldPoint point : path) {
            if (reachableTiles.containsKey(point)) {
                return true;
            }
        }

        return false;
    }

    static String offPathRecalcDeferralReason(boolean playerMoving,
                                              boolean playerAnimating,
                                              boolean playerInteracting,
                                              boolean movementOwned,
                                              boolean doorSettling,
                                              boolean transportSettling,
                                              boolean interimActive,
                                              long nowMs,
                                              long lastMovedAtMs,
                                              long routeProgressAtMs,
                                              long minimapClickAtMs,
                                              long interimProgressAtMs) {
        if (doorSettling) {
            return "door-settling";
        }
        if (transportSettling) {
            return "transport-settling";
        }
        // Busy state defers only while the walker owns the movement. Combat retaliation and
        // aggro pathing keep moving/animating/interacting true indefinitely, and an unbounded
        // defer here paralyzes the walker while something else drags the player off the route.
        if (movementOwned) {
            if (playerMoving) {
                return "moving";
            }
            if (playerAnimating) {
                return "animating";
            }
            if (playerInteracting) {
                return "interacting";
            }
        }
        if (isRecentEvent(nowMs, routeProgressAtMs, OFF_PATH_RECALC_ROUTE_PROGRESS_GRACE_MS)) {
            return "route-progress";
        }
        if (isRecentEvent(nowMs, minimapClickAtMs, OFF_PATH_RECALC_MINIMAP_CLICK_GRACE_MS)) {
            return "recent-click";
        }
        if (interimActive && isRecentEvent(nowMs, interimProgressAtMs, INTERIM_PROGRESS_TIMEOUT_MS)) {
            return "interim-progress";
        }
        if (movementOwned && isRecentEvent(nowMs, lastMovedAtMs, OFF_PATH_RECALC_RECENT_MOVEMENT_MS)) {
            return "recent-movement";
        }
        return null;
    }

    static int offPathRecalcDeferredWaitMs(String reason,
                                           long nowMs,
                                           long lastMovedAtMs,
                                           long routeProgressAtMs,
                                           long minimapClickAtMs,
                                           long interimProgressAtMs) {
        long remainingMs = OFF_PATH_RECALC_DEFER_WAIT_MAX_MS;
        if ("route-progress".equals(reason)) {
            remainingMs = remainingRecentEventMs(nowMs, routeProgressAtMs, OFF_PATH_RECALC_ROUTE_PROGRESS_GRACE_MS);
        } else if ("recent-click".equals(reason)) {
            remainingMs = remainingRecentEventMs(nowMs, minimapClickAtMs, OFF_PATH_RECALC_MINIMAP_CLICK_GRACE_MS);
        } else if ("interim-progress".equals(reason)) {
            remainingMs = remainingRecentEventMs(nowMs, interimProgressAtMs, INTERIM_PROGRESS_TIMEOUT_MS);
        } else if ("recent-movement".equals(reason)) {
            remainingMs = remainingRecentEventMs(nowMs, lastMovedAtMs, OFF_PATH_RECALC_RECENT_MOVEMENT_MS);
        }
        return (int) Math.max(OFF_PATH_RECALC_DEFER_WAIT_MIN_MS,
                Math.min(OFF_PATH_RECALC_DEFER_WAIT_MAX_MS, remainingMs));
    }

    private static boolean isRecentEvent(long nowMs, long eventAtMs, long graceMs) {
        return eventAtMs > 0L && nowMs >= eventAtMs && nowMs - eventAtMs < graceMs;
    }

    private static long remainingRecentEventMs(long nowMs, long eventAtMs, long graceMs) {
        if (!isRecentEvent(nowMs, eventAtMs, graceMs)) {
            return OFF_PATH_RECALC_DEFER_WAIT_MIN_MS;
        }
        return graceMs - (nowMs - eventAtMs);
    }

    private static boolean hasUpcomingNearbyTransportStep(List<WorldPoint> path,
                                                          int startIdx,
                                                          WorldPoint playerLoc,
                                                          int lookaheadEdges,
                                                          int maxDist) {
        if (path == null || path.size() < 2 || startIdx < 0 || playerLoc == null) {
            return false;
        }
        int from = Math.max(0, startIdx);
        int to = Math.min(path.size() - 2, from + Math.max(0, lookaheadEdges));
        for (int i = from; i <= to; i++) {
            if (!isCatalogBackedTransportSegment(path, i)) {
                continue;
            }
            WorldPoint segFrom = path.get(i);
            WorldPoint segTo = path.get(i + 1);
            if (segFrom == null || segTo == null || segFrom.getPlane() != playerLoc.getPlane()) {
                continue;
            }
            int d = Math.min(segFrom.distanceTo2D(playerLoc), segTo.distanceTo2D(playerLoc));
            if (d <= Math.max(1, maxDist)) {
                return true;
            }
        }
        return false;
    }

    // Base stall threshold. See stallThresholdMs() for activity-aware scaling.
    // RuneLite exposes no real-time ping, so we skip pure latency scaling and rely on
    // observable activity states that also correlate with legitimately-stuck players.
    private static final long STALL_BASE_MS = 12_000;
    private static final double STALL_COMBAT_MULTIPLIER = 2.0;
    private static final double STALL_ANIMATING_MULTIPLIER = 1.5;
    private static final double STALL_MOVING_MULTIPLIER = 1.35;
    /** While a sticky minimap interim waypoint is active, path segments can exceed base stall easily. */
    private static final double STALL_INTERIM_MINIMAP_MULTIPLIER = 1.75;
    private static final double STALL_INTERACTING_MULTIPLIER = 1.5;
    /**
     * After a successful minimap walk click, refresh the stall clock this long — blocked tiles / long
     * segments sometimes delay tile deltas without {@link Rs2Player#isMoving()} flipping immediately.
     */
    private static boolean interactingActorNearWalkablePath() {
        Pathfinder pf = Rs2PathApi.getPathfinder();
        if (pf == null) {
            return false;
        }
        List<WorldPoint> path = pf.getWalkablePath();
        if (path == null || path.isEmpty()) {
            return false;
        }
        Actor actor = Rs2Player.getInteracting();
        if (actor == null) {
            return false;
        }
        WorldPoint loc = actor.getWorldLocation();
        if (loc == null) {
            return false;
        }
        for (WorldPoint p : path) {
            if (p == null || p.getPlane() != loc.getPlane()) {
                continue;
            }
            if (p.distanceTo2D(loc) <= 2) {
                return true;
            }
        }
        return false;
    }

    private static long stallThresholdMs() {
        return Rs2WalkerStallPolicy.computeThresholdMs(
                STALL_BASE_MS,
                STALL_COMBAT_MULTIPLIER,
                STALL_ANIMATING_MULTIPLIER,
                STALL_MOVING_MULTIPLIER,
                STALL_INTERIM_MINIMAP_MULTIPLIER,
                STALL_INTERACTING_MULTIPLIER,
                Rs2Player.isInCombat(),
                Rs2Player.isAnimating(),
                Rs2Player.isMoving(),
                routeState.interimTargetWp != null,
                (Rs2Player.isMoving() || Rs2Player.isAnimating()) && interactingActorNearWalkablePath());
    }

    /**
     * @param start
     */
    public void setStart(WorldPoint start) {
        Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        if (pathfinder == null) {
            return;
        }
        Set<WorldPoint> targets = pathfinder.getTargets();
        Rs2PathApi.setStartPointSet(true);
        if (isClientThread()) {
            Microbot.getClientThread().runOnSeperateThread(() -> restartPathfinding(start, targets));
        } else {
            restartPathfinding(start, targets);
        }
    }

    /**
     * Of these candidate tiles, the one the pathfinder can actually reach most cheaply — or null when
     * none of them is reachable.
     *
     * <p>Choosing somewhere to stand by proximity is wrong whenever a wall or a closed door separates
     * the nearest tile from the player. A local reachability BFS does not rescue it either: the BFS
     * stops at the door, so the tile on the far side — often the only usable one — is invisible to it.
     * The pathfinder is the component that knows doors and transports, and it takes a whole set of
     * targets natively, so asking it once answers the question that actually matters: <em>which of
     * these can I get to?</em>
     *
     * <p>Worked case: approaching the Black Knights' Fortress ladder from (3024,3512), the tiles beside
     * it are walkable and adjacent but walled off, while the usable approach is east through a Sturdy
     * door. Proximity picks a walled tile every time; this picks the one with a route.
     *
     * @param start      where we are pathing from
     * @param candidates tiles worth standing on, in no particular order
     * @return the reachable candidate, or null if the pathfinder cannot reach any of them
     */
    public static WorldPoint nearestReachable(WorldPoint start, Collection<WorldPoint> candidates) {
        if (start == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        Set<WorldPoint> targets = new HashSet<>(candidates);
        if (targets.contains(start)) {
            return start;
        }
        Pathfinder pathfinder = new Pathfinder(Rs2PathApi.getPathfinderConfig(), start, targets);
        pathfinder.run();
        List<WorldPoint> path = pathfinder.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        // A partial path ends somewhere that is NOT a target; only trust an endpoint we asked for.
        WorldPoint endpoint = path.get(path.size() - 1);
        return targets.contains(endpoint) ? endpoint : null;
    }

    /**
     * Checks the distance between startpoint and endpoint using ShortestPath
     *
     * @param startpoint
     * @param endpoint
     * @return distance
     */
    public static int getDistanceBetween(WorldPoint startpoint, WorldPoint endpoint) {
        Set<WorldPoint> ends = Set.of(endpoint);
        Pathfinder pathfinder = new Pathfinder(Rs2PathApi.getPathfinderConfig(), startpoint, ends);
        pathfinder.run();
        return pathfinder.getPath().size();
    }

    /**
     * Forwards to {@link Rs2LeaguesTransport#recordTransportAttempt} for Leagues locked-region chat correlation.
     * Delegate records only teleport-like transports while Leagues is active (seasonal + spells/items, e.g. ectophial).
     */
    public static void recordTransportAttempt(Transport transport)
    {
        Rs2LeaguesTransport.recordTransportAttempt(transport);
    }

    public static boolean isTeleportItem(int itemId) {
        if (Rs2PathApi.getPathfinderConfig().getAllTransports().isEmpty()) {
            Rs2PathApi.getPathfinderConfig().refresh();
        }

        Set<Integer> teleportItemIds = Rs2PathApi.getPathfinderConfig().getAllTransports().values()
                .stream()
                .flatMap(Set::stream)
                .filter(t -> TransportType.isTeleport(t.getType(), t.getOrigin()))
                .map(Transport::getItemIdRequirements)
                .flatMap(Set::stream)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        // Items that are not included in transports
        teleportItemIds.add(ItemID.DRAMEN_STAFF);
        teleportItemIds.add(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF);

        return teleportItemIds.contains(itemId);
    }


    /**
     * Finds the nearest accessible target from a list of WorldPoints using pathfinding.
     * This is a generalized version of the logic used in Rs2Bank.getNearestBank().
     *
     * @param startPoint The starting location for pathfinding
     * @param targets List of target WorldPoints to evaluate
     * @param tolerance Tolerance in tiles for matching the final path point to targets (default: 2)
     * @return The index of the nearest accessible target in the list, or -1 if none are reachable
     */
    public static int findNearestAccessibleTarget(WorldPoint startPoint, List<WorldPoint> targets, boolean useBankItems, int tolerance) {
        if (targets == null || targets.isEmpty()) {
            return -1;
        }

        if (startPoint == null) {
            startPoint = Rs2Player.getWorldLocation();
        }

        if (startPoint == null) {
            log.warn("Unable to determine starting point for pathfinding");
            return -1;
        }

        // Convert list to set for pathfinder
        Set<WorldPoint> targetSet = new HashSet<>(targets);

        // Store original configuration to restore later
        boolean originalUseBankItems =  Rs2PathApi.getPathfinderConfig().isUseBankItems();
        try {
            Rs2PathApi.getPathfinderConfig().setUseBankItems(useBankItems);
            // Configure pathfinder
            Rs2PathApi.getPathfinderConfig().refresh();
            // Run pathfinder
            Pathfinder pf = new Pathfinder(Rs2PathApi.getPathfinderConfig(), startPoint, targetSet);
            pf.run();

            List<WorldPoint> path = pf.getPath();
            if (path.isEmpty()) {
                log.debug("Unable to find path to any target from starting point: " + startPoint);
                return -1;
            }

            // Find which target corresponds to the end of the path
            WorldPoint nearestTile = path.get(path.size() - 1);
            WorldArea nearestTileArea = new WorldArea(nearestTile, tolerance, tolerance);

            // Find the target that matches the final path destination
            for (int i = 0; i < targets.size(); i++) {
                WorldPoint target = targets.get(i);
                WorldArea targetArea = new WorldArea(target, tolerance, tolerance);
                if (targetArea.intersectsWith2D(nearestTileArea)) {
                    log.debug("Found nearest accessible target at index " + i + ": " + target + " (path ended at: " + nearestTile + ")");
                    return i;
                }
            }

            log.debug("Path found but no target matched the destination: " + nearestTile);
            return -1;

        } finally {
            // Always restore original configuration
            Rs2PathApi.getPathfinderConfig().setUseBankItems(originalUseBankItems);
            Rs2PathApi.getPathfinderConfig().refresh();
        }
    }

    /**
     * Finds the nearest accessible target from a list of WorldPoints using pathfinding.
     * Uses default tolerance of 2 tiles and no bank item usage.
     *
     * @param startPoint The starting location for pathfinding
     * @param targets List of target WorldPoints to evaluate
     * @return The index of the nearest accessible target in the list, or -1 if none are reachable
     */
    public static int findNearestAccessibleTarget(WorldPoint startPoint, List<WorldPoint> targets) {
        return findNearestAccessibleTarget(startPoint, targets, false, 2);
    }

    /**
     * Finds the nearest accessible target from a list of WorldPoints using pathfinding.
     * Uses the player's current location as starting point.
     *
     * @param targets List of target WorldPoints to evaluate
     * @param useBankItems Whether to enable bank item usage for transport calculations
     * @return The index of the nearest accessible target in the list, or -1 if none are reachable
     */
    public static int findNearestAccessibleTarget(List<WorldPoint> targets, boolean useBankItems) {
        return findNearestAccessibleTarget(Rs2Player.getWorldLocation(), targets, useBankItems, 2);
    }

    /**
     * Finds the nearest accessible target from a list of WorldPoints using pathfinding.
     * Uses the player's current location as starting point and no bank item usage.
     *
     * @param targets List of target WorldPoints to evaluate
     * @return The index of the nearest accessible target in the list, or -1 if none are reachable
     */
    public static int findNearestAccessibleTarget(List<WorldPoint> targets) {
        return findNearestAccessibleTarget(Rs2Player.getWorldLocation(), targets, false, 2);
    }

    /**
     * Prepares and analyzes required transport items for reaching a destination.
     * Similar but improved to Rs2Slayer.prepareItemTransports()
     *
     * @param destination The target location to reach
     * @param useBankItems Whether to consider bank items in pathfinding
     * @return List of Transport objects that are missing required items
     */
    public static List<Transport> getTransportsForDestination(WorldPoint destination, boolean useBankItems) {
        return getTransportsForDestination(destination, useBankItems, TransportType.TELEPORTATION_ITEM);
    }

    /**
     * Prepares and analyzes required transport items for reaching a destination.
     * Similar but improved to Rs2Slayer.prepareItemTransports()
     *
     * @param destination The target location to reach
     * @param useBankItems Whether to consider bank items in pathfinding
     * @param prefTransportType The preferred transport type to prioritize
     * @return List of Transport objects that are missing required items
     */
    public static List<Transport> getTransportsForDestination(WorldPoint destination, boolean useBankItems, TransportType prefTransportType) {
        return Rs2WalkerBankingPlanner.getTransportsForDestination(destination, useBankItems, prefTransportType);
    }

    /**
     * Prepares and analyzes required transport items for reaching a destination.
     * Uses bank items in calculations by default.
     *
     * @param destination The target location to reach
     * @return List of Transport objects that are missing required items
     */
    public static List<Transport> prepareTransportsForDestination(WorldPoint destination) {
        return getTransportsForDestination(destination, true);
    }

    /**
     * Checks if the player has the required items for a specific transport.
     * Similar to Rs2Slayer.hasRequiredTeleportItem() but accessible in Rs2Walker.
     *
     * @param transport The transport to check requirements for
     * @return true if the player has all required items, false otherwise
     */
    public static boolean hasRequiredTransportItems(Transport transport) {
        return Rs2WalkerBankingPlanner.hasRequiredTransportItems(transport);
    }

    /**
     * Filters a list of transports to return only those missing required items.
     * Similar to Rs2Slayer.getMissingItemTransports() but accessible in Rs2Walker.
     *
     * @param transports List of transports to check
     * @return List of transports that are missing required items
     */
    public static List<Transport> getMissingTransports(List<Transport> transports) {
        return Rs2WalkerBankingPlanner.getMissingTransports(transports);
    }

    /**
     * Extracts item IDs and their required quantities for the given transports that are missing and available in bank.
     * Enhanced version that uses Rs2Magic and Rs2Spells systems for actual rune quantities on teleportation spells.
     *
     * @param transports List of transports to check for missing items
     * @return Map where key=itemId and value=quantity needed (actual quantities for teleportation spells)
     */
    public static Map<Integer, Integer> getMissingTransportItemIdsWithQuantities(List<Transport> transports) {
        return Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(transports);
    }

    /**
     * Extracts item IDs that are missing for the given transports and available in bank.
     * Legacy method maintained for backward compatibility.
     * Similar to Rs2Slayer.getMissingItemIds() but accessible in Rs2Walker.
     *
     * @param transports List of transports to check for missing items
     * @return List of item IDs that are needed and available in bank
     */
    public static List<Integer> getMissingTransportItemIds(List<Transport> transports) {
        return Rs2WalkerBankingPlanner.getMissingTransportItemIds(transports);
    }

    /**
     * Compares the efficiency of traveling directly to a target versus going via bank first.
     * This is useful when transport items may be needed from the bank.
     *
     * @param target The target destination
     * @param startPoint Starting location (null to use current player location)
     * @return TransportRouteAnalysis containing the analysis of both routes
     */
    public static TransportRouteAnalysis compareRoutes(WorldPoint startPoint,WorldPoint target) {
        return Rs2WalkerBankingPlanner.compareRoutes(startPoint, target);
    }

    /**
     * Compares direct vs banking route using current player location as start point.
     */
    public static TransportRouteAnalysis compareRoutes(WorldPoint target) {
        return compareRoutes(null,target);
    }

    /**
     * Travels to the target through the banked setup coordinator when configured requirements are
     * missing. NavigationEngine owns both walking legs.
     *
     * @param target The destination to travel to
     * @return true if travel was successful, false otherwise
     */
    public static boolean walkWithBankedTransports(WorldPoint target) {
        return walkWithBankedTransports(target, false);
    }
    public static boolean walkWithBankedTransports(WorldPoint target, boolean forceBanking) {
        int d = reachedDistanceOrDefault();
        return walkWithBankedTransportsAndState(target, d, forceBanking) == WalkerState.ARRIVED;
    }
    public static boolean walkWithBankedTransports(WorldPoint target, int distance, boolean forceBanking){
        WalkerState state = walkWithBankedTransportsAndState(target, distance, forceBanking);
        return state == WalkerState.ARRIVED;

    }
    /**
     * Analyzes whether to go directly or via bank first for transport items. NavigationEngine owns
     * every selected walking leg.
     *
     * @param target The destination to travel to
     * @param forceBanking If true, forces banking route regardless of efficiency
     * @return true if travel was successful, false otherwise
     */
    public static WalkerState walkWithBankedTransportsAndState(WorldPoint target, int distance, boolean forceBanking) {
        if (target == null) {
            log.warn("Cannot travel to null target location");
            return WalkerState.EXIT;
        }
        if (isClientThread()) {
            log.error("Please do not call the walker from the main thread");
            return WalkerState.EXIT;
        }
        if (!walkerLock.tryLock()) {
            log.warn("[Walker] concurrent banked-transport walk detected, waiting for in-flight walk (held by {}); new target={}",
                    Thread.currentThread().getName(), target);
            try {
                walkerLock.lockInterruptibly();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return WalkerState.EXIT;
            }
        }
        try {
            return walkWithBankedTransportsAndStateLocked(target, distance, forceBanking);
        } finally {
            walkerLock.unlock();
        }
    }

    private static WalkerState walkWithBankedTransportsAndStateLocked(WorldPoint target, int distance, boolean forceBanking) {
        if (config == null || !config.walkWithBankedTransports()) {
            return walkWithStateInternal(target, distance);
        }
        WorldPoint pl = Rs2Player.getWorldLocation();
        if (pl == null) {
            // Transient snapshot; MOVING lets the caller retry on its next beat.
            return WalkerState.MOVING;
        }
        Client rlClient = Microbot.getClient();
        WorldView wv = rlClient != null ? rlClient.getTopLevelWorldView() : null;
        LocalPoint targetLocal = wv != null ? LocalPoint.fromWorld(wv, target) : null;
        boolean nearUnwalkableGoal = targetLocal != null
                && !Rs2Tile.isWalkable(targetLocal)
                && pl.distanceTo(target) <= distance;
        if (Rs2Tile.getReachableTilesFromTile(pl, distance).containsKey(target) || nearUnwalkableGoal) {
            return WalkerState.ARRIVED;
        }
        final Pathfinder pathfinder = Rs2PathApi.getPathfinder();
        if (pathfinder != null && !pathfinder.isDone())
            return WalkerState.MOVING;

        boolean bankTripWhenCacheUnavailable = config == null || config.bankTripWhenCacheUnavailable();
        if (!forceBanking && bankTripWhenCacheUnavailable && Rs2Bank.getBankLiveEpoch() <= 0
                && System.currentTimeMillis() - routeState.lastBankBootstrapMissAtMs > BANK_BOOTSTRAP_MISS_COOLDOWN_MS) {
            WalkerState bootstrapState = bootstrapBankMirrorForBankedPathing(distance);
            if (bootstrapState == WalkerState.EXIT || bootstrapState == WalkerState.UNREACHABLE) {
                return bootstrapState;
            }
        }
        int chebyshevToTarget = pl.distanceTo(target);
        if (!forceBanking && chebyshevToTarget <= 100) {
            // Straight-line proximity says nothing about the walkable route: the Shantay gate is
            // ~30 tiles away and ~700 by inventory-only path without a pass. Skipping the compare
            // here meant no missing-item check, so gold for a purchasable gate was never withdrawn
            // and the walker silently took the detour. One direct pathfind (cheap for a close,
            // reachable target) decides whether the short-circuit is safe; a partial path counts
            // as a detour too, since banking may be exactly what unlocks the blocked transport.
            List<WorldPoint> directProbePath = getWalkPath(pl, target);
            int directProbeTiles = getTotalTilesFromPath(directProbePath, target);
            int directPathCeiling = shortWalkDirectPathCeiling(chebyshevToTarget);
            if (directProbeTiles <= directPathCeiling) {
                WebWalkLog.spInfo("bank_walk | skip_compare_short_distance dist={} directTiles={} goal={}",
                        chebyshevToTarget, directProbeTiles, target);
                return walkWithStateInternal(target, distance);
            }
            WebWalkLog.spInfo("bank_walk | short_distance_detour dist={} directTiles={} ceiling={} goal={} — running bank compare",
                    chebyshevToTarget,
                    directProbeTiles == Integer.MAX_VALUE ? "partial" : String.valueOf(directProbeTiles),
                    directPathCeiling, target);
        }
        // Check what transport items are needed
        long compareStartedAt = System.currentTimeMillis();
        long compareFromWalkStart = routeState.walkSessionStartedAtMs > 0 ? compareStartedAt - routeState.walkSessionStartedAtMs : 0L;
        WebWalkLog.tmark("compare_start", compareFromWalkStart, target, pl, "bank_vs_direct");
        TransportRouteAnalysis comparison = compareRoutes(target);
        WebWalkLog.tmark("compare_done", System.currentTimeMillis() - compareStartedAt, target, pl,
                "direct=" + comparison.getDirectDistance() + " bank=" + comparison.getBankingRouteDistance());
        List<Transport> bankRouteTransports =
                getTransportsForDestination(target, true, TransportType.TELEPORTATION_SPELL);
        List<Transport> missingTransports = getMissingTransports(bankRouteTransports);

        // Plan the complete route, not only edges that fail an individual one-use check. Two
        // teleports can each look usable against the same single tablet or rune stack while the
        // route as a whole still needs a bank withdrawal.
        Map<Integer, Integer> missingItemsWithQuantities =
                getMissingTransportItemIdsWithQuantities(bankRouteTransports);
        if (!missingTransports.isEmpty()) {
            WebWalkLog.bankWalkDebug("missing_items nTrans={} to={} missingKinds={}",
                    missingTransports.size(), target, missingItemsWithQuantities.size());
        }
        // If no missing transport items, go directly
        if (missingItemsWithQuantities.isEmpty() && !forceBanking) {
            WebWalkLog.spInfo("bank_walk | direct_no_missing_items goal={}", target);
            WalkerState state = walkWithStateInternal(target, distance);
            if (state == WalkerState.ARRIVED) {
                WebWalkLog.bankWalkDebug("arrived goal={}", target);
            } else {
                WebWalkLog.bankWalkFailed(target, state);
                setTarget(null, "rs2walker:walkWithBankedTransports:direct-walk-failed");
                return state;

            }
            return state;
        } else {
            // Compare routes if we have missing items that could be obtained from bank
            // Use config for minimum bank route savings
            int minBankRouteSavings = config != null ? config.minBankRouteSavings() : 0;
            boolean preferTransportToTarget = config != null && config.preferTransportToTarget();
            int tileSavings = comparison.getTileSavings();
            boolean tieAndPreferBank = comparison.isTie() && preferTransportToTarget;
            boolean bankRouteIsBetter = (!comparison.isDirectIsFaster() && tileSavings >= minBankRouteSavings)
                    || (tieAndPreferBank && tileSavings >= minBankRouteSavings);
            // If forced banking or banking route is more efficient (with min savings), go via bank
            if (forceBanking || bankRouteIsBetter) {
                if (comparison.getNearestBank() != null) {
                    log.info("\n\tUsing banking route: \n\t\tStart: {} -> Bank: {} -> Target: {}",
                            Rs2Player.getWorldLocation(), comparison.getBankLocation(), target);
                    // The coordinator performs bank and target legs through NavigationEngine.
                    return walkWithBankingState(comparison.getBankLocation(), missingItemsWithQuantities, target, distance);
                } else {
                    log.warn("\n\tBanking route requested but no accessible bank found, trying direct route");
                    return walkWithStateInternal(target, distance);
                }
            } else {
                log.info("\n\tDirect route is more efficient despite missing items or does not meet min savings, traveling directly");
                return walkWithStateInternal(target, distance);
            }
        }


    }

    /**
     * Ceiling for how long the inventory-only path may be before a "close" target (&le;100
     * chebyshev) loses its right to skip the bank compare. 3x straight-line absorbs honest
     * wall-hugging and indoor zigzags; the 60-tile floor keeps tiny distances from tripping
     * on ordinary detours around buildings. Anything above this is a real detour — a gate the
     * player lacks the item/fare for — and the banked flow must get its chance to fetch it.
     */
    static int shortWalkDirectPathCeiling(int chebyshevDistance) {
        return Math.max(60, chebyshevDistance * 3);
    }

    /**
     * When the last bootstrap attempt found no bank it is pointless — and expensive, it runs a
     * pathfind — to retry on the very next walk. Back off instead of doing it every tick. The
     * timestamp itself lives in {@link WalkerRouteState} with the rest of the mutable route state.
     */
    private static final long BANK_BOOTSTRAP_MISS_COOLDOWN_MS = 60_000;

    private static WalkerState bootstrapBankMirrorForBankedPathing(int distance) {
        WorldPoint start = Rs2Player.getWorldLocation();
        if (start == null) {
            return WalkerState.MOVING;
        }
        BankLocation nearestBank = Rs2Bank.getNearestBank(start);
        if (nearestBank == null || nearestBank.getWorldPoint() == null) {
            // No bank we recognise from here. That is a gap in BankLocation coverage, not a reason to
            // refuse to walk: the bank mirror only unlocks transports that need banked items, and the
            // ordinary route is usually fine without it. Returning EXIT aborted the whole walk, so the
            // caller re-ran this every tick and the character never went anywhere — observed near
            // (3025,3508), where the nearest-bank search returns a point matching no BankLocation.
            routeState.lastBankBootstrapMissAtMs = System.currentTimeMillis();
            WebWalkLog.spWarn("bank_cache_bootstrap | no_nearest_bank start={} — continuing unbanked", start);
            return WalkerState.MOVING;
        }

        WorldPoint bankLocation = nearestBank.getWorldPoint();
        WebWalkLog.spInfo("bank_cache_bootstrap | epoch={} start={} bank={}",
                Rs2Bank.getBankLiveEpoch(), start, bankLocation);

        WalkerState walkToBank = walkWithStateInternal(bankLocation, distance);
        if (walkToBank != WalkerState.ARRIVED) {
            WebWalkLog.spWarn("bank_cache_bootstrap | walk_to_bank_failed state={} bank={}",
                    walkToBank, bankLocation);
            return walkToBank;
        }

        int epochBefore = Rs2Bank.getBankLiveEpoch();
        boolean wasOpen = Rs2Bank.isOpen();
        closeWorldMap();
        if (!Rs2Bank.openBank()) {
            WebWalkLog.spWarn("bank_cache_bootstrap | open_bank_failed bank={}", bankLocation);
            return WalkerState.EXIT;
        }
        boolean mirrorReady = Rs2Bank.verifyBankMirrorAfterOpen(wasOpen, epochBefore);
        WebWalkLog.spInfo("bank_cache_bootstrap_done | ready={} epochBefore={} epochAfter={} bank={}",
                mirrorReady, epochBefore, Rs2Bank.getBankLiveEpoch(), bankLocation);

        Rs2Bank.closeBank();
        sleepUntil(() -> !Rs2Bank.isOpen(), 3_000);
        return WalkerState.ARRIVED;
    }





    /**
     * Coordinates the bank transaction around two navigation-engine-owned walking legs.
     *
     * @param missingItemsWithQuantities Map of item IDs and their required quantities
     * @param finalTarget The final destination after banking
     * @return WalkerState indicating the result of the banking workflow
     */
    private static WalkerState walkWithBankingState(WorldPoint bankLocation,
                                                    Map<Integer, Integer> missingItemsWithQuantities,
                                                    WorldPoint finalTarget,int distance) {
        try {
			BankedTransportCoordinator.Result result = BankedTransportCoordinator.execute(
					bankLocation, missingItemsWithQuantities, finalTarget, distance,
					new BankedTransportCoordinator.Operations() {
						@Override
						public WalkerState walk(WorldPoint target, int reachedDistance) {
							return walkWithStateInternal(target, reachedDistance);
						}

						@Override public void beforeOpenBank() { closeWorldMap(); }
						@Override public boolean openBank() { return Rs2Bank.openBank(); }
						@Override public boolean awaitBankOpen() {
							return sleepUntil(Rs2Bank::isOpen, 8000);
						}
						@Override public int inventoryCount(int itemId) {
							// Quantities matter for stackable requirements such as runes, coins and
							// ecto-tokens. count(id) counts occupied slots and would treat any stack
							// as one item, causing unnecessary or impossible withdrawals.
							return Rs2Inventory.itemQuantity(itemId);
						}
						@Override public boolean hasBankItem(int itemId, int amount) {
							return Rs2Bank.hasBankItem(itemId, amount);
						}
						@Override public boolean withdraw(int itemId, int amount) {
							return withdrawBankedTransportRequirement(itemId, amount);
						}
						@Override public boolean awaitInventoryQuantity(int itemId, int amount) {
							return sleepUntil(() -> Rs2Inventory.itemQuantity(itemId) >= amount, 3000);
						}
						@Override public void settleWithdrawals() { sleepTickJitter(1); }
						@Override public void closeBank() { Rs2Bank.closeBank(); }
						@Override public boolean awaitBankClosed() {
							return sleepUntil(() -> !Rs2Bank.isOpen(), 3000);
						}
						@Override public void prepareFinalRoute(WorldPoint target) {
							Rs2PathApi.getPathfinderConfig().setUseBankItems(false);
							Rs2PathApi.getPathfinderConfig().refresh(target);
						}
					});
			if (result.getWalkerState() == WalkerState.EXIT) {
				log.warn("Banked transport setup failed at phase={} itemId={}",
						result.getPhase(), result.getFailedItemId());
			}
			return result.getWalkerState();

        } catch (Exception e) {
            log.error("Error in banking workflow: " + e.getMessage(), e);
            return WalkerState.EXIT;
        }
    }

    static boolean withdrawBankedTransportRequirement(int itemId, int amount) {
        return config != null && config.walkWithBankedTransports()
                && (Rs2Magic.getRs2Staff(itemId) == net.runelite.client.plugins.microbot.util.magic.Rs2Staff.NONE
                    || config.useBankedElementalStaffs())
                && amount > 0 && Rs2Bank.withdrawX(itemId, amount);
    }

    public static boolean closeWorldMap() {
        if (!Rs2Widget.isWidgetVisible(InterfaceID.Worldmap.CLOSE)) return false;
        Widget closeButton = Rs2Widget.getWidget(InterfaceID.Worldmap.CLOSE);
        if (closeButton != null) {
            Rectangle closeButtonBounds = closeButton.getBounds();
            NewMenuEntry closeEntry = new NewMenuEntry()
                    .option("Close")
                    .target("")
                    .identifier(1)
                    .type(MenuAction.CC_OP)
                    .param0(-1)
                    .param1(InterfaceID.Worldmap.CLOSE)
                    .forceLeftClick(false);

            Microbot.doInvoke(closeEntry, closeButtonBounds != null && Rs2UiHelper.isRectangleWithinCanvas(closeButtonBounds) ? closeButtonBounds : Rs2UiHelper.getDefaultRectangle());
        }
        return sleepUntil(() -> !Rs2Widget.isWidgetVisible(InterfaceID.Worldmap.CLOSE), 3000);
    }

    private static Tile tileAtPoint(WorldPoint point) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            LocalPoint localPoint;
            if (Microbot.getClient().getTopLevelWorldView().isInstance()) {
                WorldPoint instancedWorldPoint = WorldPoint.toLocalInstance(
                        Microbot.getClient().getTopLevelWorldView(), point).stream()
                        .findFirst().orElse(null);
                if (instancedWorldPoint == null) {
                    log.error("getTile instancedWorldPoint is null");
                    return null;
                }
                localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(),
                        instancedWorldPoint);
            } else {
                localPoint = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), point);
            }
            if (localPoint == null) {
                return null;
            }
            return Microbot.getClient().getTopLevelWorldView().getScene().getTiles()
                    [point.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
        }).orElse(null);
    }

    private static Collection<WorldPoint> localInstancePoints(WorldPoint point) {
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
                WorldPoint.toLocalInstance(Microbot.getClient().getTopLevelWorldView(), point))
                .orElse(Collections.emptyList());
    }

    private static WorldPoint tileObjectWorldLocation(TileObject object) {
        return object == null ? null : Microbot.getClientThread()
                .runOnClientThreadOptional(object::getWorldLocation).orElse(null);
    }

}
