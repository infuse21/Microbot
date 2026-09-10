package net.runelite.client.plugins.microbot.util.walker.navigation;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentObservation;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentPreparation;

/** Process bridge for shadow comparison and opt-in ordinary-route execution. */
@Slf4j
public final class NavigationEngineRuntime
{
	private static final Object MUTEX = new Object();
	private static NavigationEngine engine;
	private static volatile NavigationSnapshot snapshot;
	private static PendingBlockedEdge pendingBlockedEdge;
	private static volatile NavigationDecision equipmentDispatch;

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

	public static boolean retainEquipmentTransaction(long requestId, long generation,
		SpellEquipmentTransaction transaction)
	{
		synchronized (MUTEX)
		{
			if (engine == null) return false;
			boolean retained = engine.retainEquipmentTransaction(requestId, generation, transaction);
			snapshot = engine.snapshot();
			return retained;
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
		NavigationSnapshot equipmentOwner = snapshot;
		SpellEquipmentObservation equipment = equipmentOwner != null && !equipmentOwner.isTerminal()
			&& equipmentOwner.isEquipmentRestorationRequired() ? actions.observeEquipment() : null;
		NavigationDecision deferredCommand;
		RouteInteraction observedSpell = observation.getRouteInteraction() != null
			? observation.getRouteInteraction() : equipmentOwner == null ? null : equipmentOwner.getPendingInteraction();
		SpellEquipmentPreparation preparation = equipmentOwner != null && !equipmentOwner.isTerminal()
			&& !equipmentOwner.isEquipmentRestorationRequired() && observedSpell != null
			&& observedSpell.getKind() == RouteInteraction.Kind.SIMPLE_TELEPORT
			? actions.observeSpellEquipment(observedSpell, equipmentOwner.getEquipmentTransaction()) : null;
		NavigationSnapshot deferredOwner;
		synchronized (MUTEX)
		{
			if (engine == null || snapshot == null)
			{
				return new NavigationExecutionResult(NavigationDecision.of(
					NavigationDecision.Type.NO_ACTION, "engine-session-not-started"), false, false,
					"none");
			}
			if (equipmentDispatch != null)
				return new NavigationExecutionResult(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"equipment-dispatch-in-flight"), snapshot.getExecutionMode()
					== NavigationExecutionMode.ENGINE_SUPPORTED, false, "none");
			NavigationObservation effectiveObservation = applyPendingRecovery(observation);
			NavigationDecision decision = equipmentOwner == null ? null : engine.observeEquipmentRestoration(
				equipmentOwner.getRequestId(), equipmentOwner.getGeneration(),
				equipmentOwner.getEquipmentTransaction(), equipment, effectiveObservation);
			if (decision == null) decision = engine.observe(effectiveObservation);
			if (preparation != null && snapshot.getRequest() == equipmentOwner.getRequest()
				&& preparation.matches(decision.getInteraction()))
				decision = engine.prepareSpellEquipment(decision, preparation.getTransaction(),
					preparation.getEquipment(), preparation.isEquipable(), effectiveObservation);
			else if (snapshot.getEquipmentTransaction() != null)
				// Unavailable or stale observations cannot bypass an acquired equipment obligation.
				decision = engine.prepareSpellEquipment(decision, snapshot.getEquipmentTransaction(),
					null, false, effectiveObservation);
			clearHandledRecovery(effectiveObservation, decision);
			snapshot = engine.snapshot();
			boolean engineOwned = snapshot.getExecutionMode() == NavigationExecutionMode.ENGINE_SUPPORTED;
			boolean issued = false;
			if (engineOwned && decision.getInteraction() != null
				&& decision.getInteraction().getKind() == RouteInteraction.Kind.SPELL_EQUIPMENT)
			{
				equipmentDispatch = decision;
				deferredCommand = decision;
				deferredOwner = snapshot;
				// Reserve the attempt before releasing ownership; observations acknowledge completion.
				engine.recordCommandResult(decision, false, effectiveObservation.getObservedAtMs());
				snapshot = engine.snapshot();
			}
			else
			{
				deferredCommand = null;
				deferredOwner = null;
			}
			if (engineOwned && decision.getType() == NavigationDecision.Type.CLICK_TILE)
			{
				issued = actions.clickTile(decision.getTarget(), decision.getTargetSelection());
				engine.recordCommandResult(decision, issued, effectiveObservation.getObservedAtMs());
				snapshot = engine.snapshot();
			}
			else if (engineOwned && decision.getType() == NavigationDecision.Type.INTERACT
				&& decision.getInteraction() != null && deferredCommand == null)
			{
				issued = actions.interact(decision.getInteraction());
				if (actions.interactionPreparedOnly())
				{
					engine.recordInteractionPreparation(decision,
						effectiveObservation.getObservedAtMs());
				}
				else
				{
					engine.recordCommandResult(decision, issued,
						effectiveObservation.getObservedAtMs());
				}
				snapshot = engine.snapshot();
			}
			if (deferredCommand == null) return new NavigationExecutionResult(decision, engineOwned, issued,
				decision.getType() == NavigationDecision.Type.CLICK_TILE
					|| decision.getType() == NavigationDecision.Type.INTERACT
					? actions.getLastActionType() : "none");
		}
		try
		{
			boolean issued = equipmentPermission(deferredOwner, deferredCommand)
				&& actions.interactEquipment(deferredCommand.getInteraction(), deferredOwner.getEquipmentTransaction(),
					() -> equipmentPermission(deferredOwner, deferredCommand));
			return new NavigationExecutionResult(deferredCommand, true, issued, actions.getLastActionType());
		}
		finally
		{
			synchronized (MUTEX)
			{
				if (equipmentDispatch == deferredCommand) equipmentDispatch = null;
			}
		}
	}

	private static boolean equipmentPermission(NavigationSnapshot owner, NavigationDecision command)
	{
		NavigationSnapshot current = snapshot;
		return equipmentDispatch == command && current != null && !current.isTerminal()
			&& current.getRequest() == owner.getRequest()
			&& !owner.getRequest().getCancellationToken().isCancelled()
			&& current.getGeneration() == owner.getGeneration()
			&& current.getEquipmentTransaction() == owner.getEquipmentTransaction()
			&& current.isEquipmentRestorationRequired() == owner.isEquipmentRestorationRequired();
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
			equipmentDispatch = null;
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
