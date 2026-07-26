package net.runelite.client.plugins.microbot.util.walker.state;

import net.runelite.api.coords.WorldPoint;

/**
 * Consolidated mutable route state for the walker, extracted from {@code Rs2Walker}'s scattered static
 * fields. This is the enabling step for P1 of the walker audit: once the state the {@code processWalk}
 * loop, recovery, and transport handling all share lives in one place, those pieces can be lifted into
 * their own classes ({@code WalkExecutor} / {@code RouteRecovery} / {@code TransportService}) without
 * threading a dozen parameters through every call.
 * <p>
 * Fields are migrated in cohesive clusters, one increment at a time, each verified by compilation (which
 * catches any missed reference) and the walker test suite. Access stays {@code volatile} to preserve the
 * cross-thread visibility the original static fields had (the script thread writes, overlays/other reads).
 * Fields are public for now to keep the migration a pure mechanical move; encapsulation can follow once
 * all clusters are in.
 */
public final class WalkerRouteState {

    // ---- transport handoff: set when a transport (stairs, ladder, shortcut, teleport) is taken, read by
    // the post-transport settling/window logic in processWalk. ----

    /** Wall-clock ms when the last transport was handled; 0 when none this session. */
    public volatile long lastTransportHandledAtMs = 0L;
    /** Player tile immediately after the last transport handoff. */
    public volatile WorldPoint lastTransportHandledAtLocation = null;
    /** Origin tile of the last handled transport. */
    public volatile WorldPoint lastTransportOriginLocation = null;
    /** Destination tile of the last handled transport. */
    public volatile WorldPoint lastTransportDestinationLocation = null;

    // ---- route progress: tracks how far along the current route the player has advanced, used to detect
    // real forward progress (vs thrashing) and to decide when to reset on a new/changed route. ----

    /** Furthest path index reached on the current route; -1 when none. */
    public volatile int routeProgressIdx = -1;
    /** Target the current progress tracking is for. */
    public volatile WorldPoint routeProgressTarget = null;
    /** Start tile of the path the current progress tracking is for. */
    public volatile WorldPoint routeProgressPathStart = null;
    /** End tile of the path the current progress tracking is for. */
    public volatile WorldPoint routeProgressPathEnd = null;
    /** Size of the path the current progress tracking is for; -1 when none. */
    public volatile int routeProgressPathSize = -1;
    /** Wall-clock ms when route progress last advanced. */
    public volatile long routeProgressAdvancedAtMs = 0L;
}
