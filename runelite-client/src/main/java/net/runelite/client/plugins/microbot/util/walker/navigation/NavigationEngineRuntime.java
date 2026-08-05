package net.runelite.client.plugins.microbot.util.walker.navigation;

import lombok.extern.slf4j.Slf4j;

/** Process bridge for shadow comparison and opt-in ordinary-route execution. */
@Slf4j
public final class NavigationEngineRuntime
{
	private static final Object MUTEX = new Object();
	private static NavigationEngine engine;
	private static volatile NavigationSnapshot snapshot;
	private static PendingBlockedEdge pendingBlockedEdge;

	private NavigationEngineRuntime()
	{
	}

	public static void ensureRequest(NavigationRequest request)
	{
		synchronized (MUTEX)
		{
			if (snapshot != null && snapshot.getRequestId() == request.getRequestId()
				&& !snapshot.isTerminal())
			{
				return;
			}
			if (engine == null)
			{
				engine = new NavigationEngine();
			}
			snapshot = engine.start(request);
			pendingBlockedEdge = null;
		}
	}

	public static NavigationDecision observe(NavigationObservation observation)
	{
			synchronized (MUTEX)
		{
			if (engine == null || snapshot == null)
			{
				return NavigationDecision.of(NavigationDecision.Type.NO_ACTION,
					"shadow-session-not-started");
			}
			NavigationDecision decision = engine.observe(observation);
			snapshot = engine.snapshot();
			if (log.isDebugEnabled() && decision.getType() != NavigationDecision.Type.NO_ACTION)
			{
				log.debug("[NavShadow] req={} gen={} phase={} decision={} reason={} comparison={}",
					snapshot.getRequestId(), snapshot.getGeneration(), snapshot.getPhase(),
					decision.getType(), decision.getReason(), snapshot.getComparison());
			}
			return decision;
		}
	}

	/** Executes at most one command, and only for a request locked to ENGINE_SUPPORTED. */
	public static NavigationExecutionResult execute(NavigationObservation observation,
		WalkerActions actions)
	{
		if (actions == null)
		{
			throw new NullPointerException("actions");
		}
		synchronized (MUTEX)
		{
			if (engine == null || snapshot == null)
			{
				return new NavigationExecutionResult(NavigationDecision.of(
					NavigationDecision.Type.NO_ACTION, "engine-session-not-started"), false, false,
					"none");
			}
			NavigationObservation effectiveObservation = applyPendingRecovery(observation);
			NavigationDecision decision = engine.observe(effectiveObservation);
			clearHandledRecovery(effectiveObservation, decision);
			snapshot = engine.snapshot();
			boolean engineOwned = snapshot.getExecutionMode() == NavigationExecutionMode.ENGINE_SUPPORTED;
			boolean issued = false;
			if (engineOwned && decision.getType() == NavigationDecision.Type.CLICK_TILE)
			{
				issued = actions.clickTile(decision.getTarget(), decision.getTargetSelection());
				engine.recordCommandResult(decision, issued, effectiveObservation.getObservedAtMs());
				snapshot = engine.snapshot();
			}
			else if (engineOwned && decision.getType() == NavigationDecision.Type.INTERACT
				&& decision.getInteraction() != null)
			{
				issued = actions.interact(decision.getInteraction());
				engine.recordCommandResult(decision, issued, effectiveObservation.getObservedAtMs());
				snapshot = engine.snapshot();
			}
			return new NavigationExecutionResult(decision, engineOwned, issued,
				decision.getType() == NavigationDecision.Type.CLICK_TILE
					|| decision.getType() == NavigationDecision.Type.INTERACT
					? actions.getLastActionType() : "none");
		}
	}

	/** Queues one live-collision contradiction for the engine-owned route. */
	public static boolean reportBlockedEdge(long routeGeneration, int rawEdgeIndex)
	{
		synchronized (MUTEX)
		{
			if (rawEdgeIndex < 0 || snapshot == null || snapshot.isTerminal()
				|| snapshot.getExecutionMode() != NavigationExecutionMode.ENGINE_SUPPORTED
				|| snapshot.getRoutePlan() == null)
			{
				return false;
			}
			if (snapshot.getGeneration() != routeGeneration)
			{
				return true;
			}
			if (snapshot.isInteractionCollisionProtected(rawEdgeIndex))
			{
				pendingBlockedEdge = null;
				return true;
			}
			pendingBlockedEdge = new PendingBlockedEdge(snapshot.getGeneration(), rawEdgeIndex);
			return true;
		}
	}

	public static boolean isOrdinaryExecutionActive()
	{
		NavigationSnapshot current = snapshot;
		return current != null && !current.isTerminal()
			&& current.getExecutionMode() == NavigationExecutionMode.ENGINE_SUPPORTED;
	}

	public static boolean hasUnobservedRecovery()
	{
		synchronized (MUTEX)
		{
			return pendingBlockedEdge != null && !pendingBlockedEdge.delivered;
		}
	}

	public static void finishFromLegacy(String reason)
	{
		NavigationObservation.TerminalSignal signal = terminalSignal(reason);
		observe(NavigationObservation.terminal(signal, reason == null ? "legacy-clear" : reason));
		synchronized (MUTEX)
		{
			pendingBlockedEdge = null;
		}
	}

	public static void cancel(String reason)
	{
		synchronized (MUTEX)
		{
			if (engine != null)
			{
				snapshot = engine.cancel(reason);
			}
			pendingBlockedEdge = null;
		}
	}

	public static NavigationSnapshot getSnapshot()
	{
		return snapshot;
	}

	static void resetForTesting()
	{
		synchronized (MUTEX)
		{
			engine = null;
			snapshot = null;
			pendingBlockedEdge = null;
		}
	}

	private static NavigationObservation applyPendingRecovery(NavigationObservation observation)
	{
		if (pendingBlockedEdge == null || observation.getRoutePlan() == null)
		{
			return observation;
		}
		if (pendingBlockedEdge.generation != observation.getRoutePlan().getGeneration())
		{
			pendingBlockedEdge = null;
			return observation;
		}
		if (isInteractionCollisionProtected(pendingBlockedEdge.edgeIndex, observation))
		{
			pendingBlockedEdge = null;
			return observation;
		}
		pendingBlockedEdge.delivered = true;
		return observation.withRecovery(RecoveryCause.BLOCKED_EDGE,
			pendingBlockedEdge.edgeIndex);
	}

	private static boolean isInteractionCollisionProtected(int rawEdgeIndex,
		NavigationObservation observation)
	{
		if (snapshot != null && snapshot.isInteractionCollisionProtected(rawEdgeIndex))
		{
			return true;
		}
		return isSameInteractionEdge(rawEdgeIndex, observation.getRouteInteraction())
			|| isSameInteractionEdge(rawEdgeIndex, observation.getNextRouteInteraction());
	}

	private static boolean isSameInteractionEdge(int rawEdgeIndex, RouteInteraction interaction)
	{
		return interaction != null
			&& Math.abs(interaction.getRawEdgeIndex() - rawEdgeIndex) <= 1;
	}

	private static void clearHandledRecovery(NavigationObservation observation,
		NavigationDecision decision)
	{
		if (observation.getRecoveryCause() != RecoveryCause.BLOCKED_EDGE)
		{
			return;
		}
		String reason = decision.getReason();
		if (decision.getType() == NavigationDecision.Type.REQUEST_REPLAN
			|| decision.getType() == NavigationDecision.Type.FAIL
			|| reason.equals("blocked-edge") || reason.equals("blocked-edge-budget-exhausted")
			|| reason.equals("blocked-edge-already-handled-this-generation"))
		{
			pendingBlockedEdge = null;
		}
	}

	private static final class PendingBlockedEdge
	{
		private final long generation;
		private final int edgeIndex;
		private boolean delivered;

		private PendingBlockedEdge(long generation, int edgeIndex)
		{
			this.generation = generation;
			this.edgeIndex = edgeIndex;
		}
	}

	private static NavigationObservation.TerminalSignal terminalSignal(String reason)
	{
		String normalized = reason == null ? "" : reason.toLowerCase();
		if (normalized.contains("arrived") || normalized.contains("reached-path-endpoint")
			|| normalized.contains("within-distance"))
		{
			return NavigationObservation.TerminalSignal.ARRIVED;
		}
		if (normalized.contains("unreachable") || normalized.contains("no-walkable-path")
			|| normalized.contains("partial-retries-exhausted")
			|| normalized.contains("target-not-walkable"))
		{
			return NavigationObservation.TerminalSignal.UNREACHABLE;
		}
		if (normalized.contains("exception") || normalized.contains("timeout"))
		{
			return NavigationObservation.TerminalSignal.FAILED;
		}
		return NavigationObservation.TerminalSignal.CANCELLED;
	}
}
