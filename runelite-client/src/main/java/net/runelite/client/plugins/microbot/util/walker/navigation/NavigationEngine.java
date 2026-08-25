package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.List;

/**
 * Pure navigation state machine introduced in Phase 2 and used for the Phase 3 ordinary-walking cutover.
 *
 * <p>The engine remains input-free: each observation produces one {@link NavigationDecision},
 * while {@link NavigationEngineRuntime} is the sole adapter allowed to execute a decision through
 * {@link WalkerActions} for an opted-in ordinary route.</p>
 */
public final class NavigationEngine
{
	private static final long COMMAND_ACK_TIMEOUT_MS = 1_200L;
	private static final long NO_TILE_PROGRESS_TIMEOUT_MS = 2_400L;
	private static final int MAX_NO_ACKNOWLEDGEMENT_ATTEMPTS = 2;
	private static final int MAX_COMMAND_DESTINATION_MISMATCH_ATTEMPTS = 2;
	private static final int MAX_NO_TILE_PROGRESS_ATTEMPTS = 2;
	private static final int MAX_OFF_ROUTE_ATTEMPTS = 3;
	private static final int MAX_BLOCKED_EDGE_ATTEMPTS = 3;
	private static final int MAX_INTERACTION_UNAVAILABLE_ATTEMPTS = 1;
	private static final long INTERACTION_COMMAND_BASE_TIMEOUT_MS = 5_000L;
	private static final long INTERACTION_COMMAND_TIMEOUT_PER_TILE_MS = 600L;
	private static final long NPC_TRANSPORT_COMMAND_TIMEOUT_MS = 30_000L;
	private static final int MAX_INTERACTION_COMMAND_DISTANCE = 13;
	private static final int MAX_ROUTE_EXHAUSTED_ATTEMPTS = 3;
	private static final int MAX_EXTERNAL_REPLAN_ATTEMPTS = 3;
	private static final int MAX_PROGRESS_ADVANCE_PER_OBSERVATION = 32;
	private static final int MIN_ROUTE_CLICK_REACH = 7;
	private static final int MAX_ROUTE_CLICK_REACH = 10;
	private static final int MIN_COMMAND_HANDOFF_DISTANCE = 2;
	private static final int MAX_COMMAND_HANDOFF_DISTANCE = 4;
	private static final int COMMAND_DESTINATION_TOLERANCE = 2;
	private static final int DESTINATION_ROUTE_TOLERANCE = 3;
	private WalkSession session;

	public synchronized NavigationSnapshot start(NavigationRequest request)
	{
		if (session != null && !session.phase.isTerminal())
		{
			cancel("replaced-by-request-" + request.getRequestId());
		}
		session = new WalkSession(request);
		return snapshot();
	}

	public synchronized NavigationDecision observe(NavigationObservation observation)
	{
		if (session == null)
		{
			throw new IllegalStateException("No active navigation request");
		}
		if (session.phase.isTerminal())
		{
			return publish(NavigationDecision.of(NavigationDecision.Type.NO_ACTION,
				"terminal-" + session.phase.name().toLowerCase()), observation);
		}

		if (observation.getTerminalSignal() != NavigationObservation.TerminalSignal.NONE)
		{
			return terminal(observation);
		}
		if (session.request.getCancellationToken().isCancelled())
		{
			session.transitionTo(NavigationPhase.CANCELLED, "request-token-cancelled");
			return publish(NavigationDecision.of(NavigationDecision.Type.NO_ACTION,
				"request-token-cancelled"), observation);
		}

		session.lastObservedAtMs = observation.getObservedAtMs();
		session.lastObservedPlayer = observation.getPlayerLocation();
		session.lastObservedDestination = observation.getMovementDestination();
		RoutePlan observedPlan = observation.getRoutePlan();
		if (observedPlan == null)
		{
			session.transitionTo(NavigationPhase.CALCULATING, "awaiting-route-plan");
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"awaiting-route-plan"), observation);
		}
		if (observedPlan.getRequestId() != session.request.getRequestId())
		{
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"stale-route-request"), observation);
		}
		if (session.routePlan != null && observedPlan.getGeneration() < session.generation)
		{
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"stale-route-generation"), observation);
		}
		if (session.routePlan == null || observedPlan.getGeneration() > session.generation)
		{
			session.install(observedPlan);
			if (session.executionMode == NavigationExecutionMode.ENGINE_SUPPORTED
				&& !observedPlan.isEngineSupported())
			{
				session.transitionTo(NavigationPhase.FAILED,
					"engine-route-became-non-ordinary");
				return publish(NavigationDecision.of(NavigationDecision.Type.FAIL,
					"engine-route-became-non-ordinary"), observation);
			}
			session.transitionTo(NavigationPhase.FOLLOWING_ROUTE, "route-generation-installed");
		}

		updateProgress(observation.getPlayerLocation(), observation.getObservedAtMs());
		retireCrossedChainedInteractions();
		if (hasArrived(observation.getPlayerLocation())
			&& !hasUncrossedEngineInteractionEdge()
			&& !hasUnresolvedRouteInteraction(observation))
		{
			session.transitionTo(NavigationPhase.ARRIVED, "destination-within-reached-distance");
			return publish(NavigationDecision.of(NavigationDecision.Type.COMPLETE,
				"destination-within-reached-distance"), observation);
		}
		if (observedPlan.getRawPath().isEmpty())
		{
			session.transitionTo(NavigationPhase.UNREACHABLE, "route-plan-empty");
			return publish(NavigationDecision.of(NavigationDecision.Type.FAIL,
				"route-plan-empty"), observation);
		}
		if (session.commandRejected)
		{
			session.commandRejected = false;
			clearCommandTarget();
			return requestReplan(RecoveryCause.NO_ACKNOWLEDGEMENT,
				"movement-command-rejected", observation);
		}
		if (session.commandPending)
		{
			WorldPoint movementDestination = observation.getMovementDestination();
			boolean destinationChanged = movementDestination != null
				&& !movementDestination.equals(session.commandDestinationAtIssue);
			if (destinationChanged && isDivergentCommandDestination(movementDestination))
			{
				session.commandPending = false;
				return requestDestinationMismatch(movementDestination, observation);
			}
			boolean routeProgressed = session.commandOriginRawIndex >= 0
				&& session.rawProgressIndex > session.commandOriginRawIndex;
			if (destinationChanged || routeProgressed)
			{
				session.commandPending = false;
			}
			else if (observation.getObservedAtMs() - session.lastCommandAtMs < COMMAND_ACK_TIMEOUT_MS)
			{
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"movement-command-awaiting-ack"), observation);
			}
			else
			{
				session.commandPending = false;
				clearCommandTarget();
				return requestReplan(RecoveryCause.NO_ACKNOWLEDGEMENT,
					"movement-command-not-acknowledged", observation);
			}
		}
		// Long-distance interactions can move through a temporary ship deck, cutscene,
		// or loading scene that is intentionally nowhere near the published raw route.
		// While the interaction acknowledgement window owns the command, let its
		// destination predicate observe that state before ordinary off-route recovery.
		if (session.interactionCommandPending)
		{
			NavigationDecision interactionInFlight = handleRouteInteraction(observation);
			if (interactionInFlight != null)
			{
				return interactionInFlight;
			}
		}
		NavigationDecision observedRecovery = recoverFromObservation(observation);
		if (observedRecovery != null)
		{
			return observedRecovery;
		}
		int recalculateDistance = session.request.getRouteOptions().getRecalculateDistance();
		if (recalculateDistance >= 0 && session.routeDistance > recalculateDistance)
		{
			if (observation.isMoving() || observation.isAnimating() || observation.isInteracting())
			{
				session.transitionTo(NavigationPhase.FOLLOWING_ROUTE,
					"off-route-movement-in-flight");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"off-route-movement-in-flight"), observation);
			}
			return requestReplan(RecoveryCause.OFF_ROUTE, "off-route", observation);
		}

		NavigationDecision interactionDecision = handleRouteInteraction(observation);
		if (interactionDecision != null)
		{
			return interactionDecision;
		}

		if (observation.isReplanRequested())
		{
			if (observation.isMoving() || observation.isAnimating() || observation.isInteracting())
			{
				session.transitionTo(NavigationPhase.FOLLOWING_ROUTE,
					"replan-deferred-command-in-flight");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"replan-deferred-command-in-flight"), observation);
			}
			return requestReplan(RecoveryCause.EXTERNAL_REPLAN,
				"legacy-requested-replan", observation);
		}

		boolean proximityHandoff = isProximityHandoff(observation);
		if (observation.isAnimating() || observation.isInteracting()
			|| (observation.isMoving() && !proximityHandoff))
		{
			session.transitionTo(NavigationPhase.FOLLOWING_ROUTE, "movement-in-flight");
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"movement-in-flight"), observation);
		}
		if (isAwaitingCommandProgress(observation))
		{
			session.transitionTo(NavigationPhase.FOLLOWING_ROUTE,
				"movement-command-progress-window");
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"movement-command-progress-window"), observation);
		}
		NavigationDecision noProgress = recoverFromNoTileProgress(observation);
		if (noProgress != null)
		{
			return noProgress;
		}

		List<WorldPoint> rawPath = session.routePlan.getRawPath();
		if (session.rawProgressIndex >= rawPath.size() - 1)
		{
			return requestReplan(RecoveryCause.ROUTE_EXHAUSTED,
				"route-end-before-arrival", observation);
		}
		int reach = routeClickReach();
		int selectionAnchor = proximityHandoff
			? Math.max(session.rawProgressIndex, session.commandRawIndex)
			: session.rawProgressIndex;
		RouteClickSelection selection = RouteClickSelector.select(session.routePlan,
			observation.getPlayerLocation(), selectionAnchor, reach, MAX_ROUTE_CLICK_REACH);
		if (selection == null)
		{
			if (proximityHandoff)
			{
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"proximity-handoff-awaiting-forward-target"), observation);
			}
			return requestReplan(RecoveryCause.ROUTE_EXHAUSTED,
				"no-forward-raw-route-target", observation);
		}
		if (proximityHandoff && (selection.getRawIndex() <= session.commandRawIndex
			|| selection.getTarget().equals(session.commandTarget)))
		{
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"proximity-handoff-not-forward"), observation);
		}
		String reason = proximityHandoff ? "proximity-route-handoff" : "next-raw-route-tile";
		session.transitionTo(NavigationPhase.FOLLOWING_ROUTE, reason);
		return publish(NavigationDecision.click(selection,
			routeHandoffDistance(selection.getRawIndex()), reason), observation);
	}

	public synchronized void recordCommandResult(NavigationDecision decision, boolean issued,
		long commandAtMs)
	{
		if (session == null || session.phase.isTerminal() || !decision.issuesInput())
		{
			return;
		}
		session.lastCommandAtMs = commandAtMs;
		if (decision.getType() == NavigationDecision.Type.INTERACT)
		{
			session.interactionCommandPending = issued;
			int distance = interactionCommandDistance(decision.getInteraction());
			long timeout = decision.getInteraction() != null
				&& (decision.getInteraction().getKind() == RouteInteraction.Kind.NPC_TRANSPORT
					|| decision.getInteraction().getKind()
						== RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.CHARTER_SHIP
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.FAIRY_RING
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.SPIRIT_TREE)
				? NPC_TRANSPORT_COMMAND_TIMEOUT_MS
				: INTERACTION_COMMAND_BASE_TIMEOUT_MS
					+ Math.min(MAX_INTERACTION_COMMAND_DISTANCE, distance)
						* INTERACTION_COMMAND_TIMEOUT_PER_TILE_MS;
			session.interactionCommandDeadlineMs = issued ? commandAtMs + timeout : 0L;
			session.commandRejected = !issued;
			return;
		}
		session.commandOrigin = session.lastObservedPlayer;
		session.commandDestinationAtIssue = issued ? session.lastObservedDestination : null;
		session.commandPending = issued;
		session.commandRejected = !issued;
		session.commandTarget = issued ? decision.getTarget() : null;
		session.commandRawIndex = issued ? decision.getTargetRawIndex() : -1;
		session.commandHandoffDistance = issued ? decision.getTargetHandoffDistance() : -1;
		session.commandOriginRawIndex = issued ? session.rawProgressIndex : -1;
	}

	private NavigationDecision requestDestinationMismatch(WorldPoint movementDestination,
		NavigationObservation observation)
	{
		WorldPoint expectedTarget = session.commandTarget;
		long ageMs = Math.max(0L, observation.getObservedAtMs() - session.lastCommandAtMs);
		clearCommandTarget();
		int attempts = session.incrementRecovery(RecoveryCause.COMMAND_DESTINATION_MISMATCH);
		if (session.executionMode == NavigationExecutionMode.ENGINE_SUPPORTED
			&& attempts > MAX_COMMAND_DESTINATION_MISMATCH_ATTEMPTS)
		{
			String reason = "command-destination-mismatch-budget-exhausted";
			session.transitionTo(NavigationPhase.UNREACHABLE, reason);
			return publish(NavigationDecision.destinationMismatch(NavigationDecision.Type.FAIL,
				attempts, MAX_COMMAND_DESTINATION_MISMATCH_ATTEMPTS, ageMs, expectedTarget,
				movementDestination, reason), observation);
		}
		String reason = "movement-destination-off-route";
		session.transitionTo(NavigationPhase.REPLANNING, reason);
		return publish(NavigationDecision.destinationMismatch(
			NavigationDecision.Type.REQUEST_REPLAN, attempts,
			MAX_COMMAND_DESTINATION_MISMATCH_ATTEMPTS, ageMs, expectedTarget,
			movementDestination, reason), observation);
	}

	private NavigationDecision requestReplan(RecoveryCause cause, String reason,
		NavigationObservation observation)
	{
		clearCommandTarget();
		int attempts = session.incrementRecovery(cause);
		int budget = recoveryBudget(cause);
		long ageMs = recoveryAgeMs(cause, observation);
		int blockedEdgeIndex = cause == RecoveryCause.BLOCKED_EDGE
			? observation.getBlockedEdgeIndex() : -1;
		if (session.executionMode == NavigationExecutionMode.ENGINE_SUPPORTED
			&& attempts > budget)
		{
			String terminalReason = recoveryReason(cause) + "-budget-exhausted";
			session.transitionTo(NavigationPhase.UNREACHABLE, terminalReason);
			return publish(NavigationDecision.recovery(NavigationDecision.Type.FAIL, cause,
				attempts, budget, ageMs, blockedEdgeIndex, terminalReason), observation);
		}
		session.transitionTo(NavigationPhase.REPLANNING, reason);
		return publish(NavigationDecision.recovery(NavigationDecision.Type.REQUEST_REPLAN,
			cause, attempts, budget, ageMs, blockedEdgeIndex, reason), observation);
	}

	private NavigationDecision recoverFromObservation(NavigationObservation observation)
	{
		RecoveryCause cause = observation.getRecoveryCause();
		if (cause == RecoveryCause.NONE)
		{
			return null;
		}
		if (cause == RecoveryCause.INTERACTION_WAIT || observation.isMoving()
			|| observation.isAnimating() || observation.isInteracting()
			|| hasRecentValidCommand(observation.getObservedAtMs()))
		{
			session.transitionTo(NavigationPhase.FOLLOWING_ROUTE,
				recoveryReason(cause) + "-deferred");
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				recoveryReason(cause) + "-deferred"), observation);
		}
		if (cause == RecoveryCause.BLOCKED_EDGE)
		{
			int edgeIndex = observation.getBlockedEdgeIndex();
			if (edgeIndex < 0 || !session.blockedEdgesReplanned.add(edgeIndex))
			{
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"blocked-edge-already-handled-this-generation"), observation);
			}
		}
		return requestReplan(cause, recoveryReason(cause), observation);
	}

	private NavigationDecision recoverFromNoTileProgress(NavigationObservation observation)
	{
		long progressAge = observation.getObservedAtMs()
			- Math.max(session.lastCommandAtMs, session.lastProgressAtMs);
		if (session.commandTarget == null || session.commandOriginRawIndex < 0
			|| session.rawProgressIndex > session.commandOriginRawIndex
			|| progressAge < NO_TILE_PROGRESS_TIMEOUT_MS)
		{
			return null;
		}
		int attempts = session.incrementRecovery(RecoveryCause.NO_TILE_PROGRESS);
		if (session.executionMode == NavigationExecutionMode.ENGINE_SUPPORTED
			&& attempts > MAX_NO_TILE_PROGRESS_ATTEMPTS)
		{
			clearCommandTarget();
			String reason = "no-tile-progress-budget-exhausted";
			session.transitionTo(NavigationPhase.UNREACHABLE, reason);
			return publish(NavigationDecision.recovery(NavigationDecision.Type.FAIL,
				RecoveryCause.NO_TILE_PROGRESS, attempts, MAX_NO_TILE_PROGRESS_ATTEMPTS,
				progressAge, -1, reason), observation);
		}
		if (attempts == MAX_NO_TILE_PROGRESS_ATTEMPTS)
		{
			return replanNoTileProgress(attempts, progressAge, "no-tile-progress",
				observation);
		}
		RouteClickSelection selection = RouteClickSelector.select(session.routePlan,
			observation.getPlayerLocation(), session.rawProgressIndex, 4, 6);
		if (selection == null)
		{
			return replanNoTileProgress(attempts, progressAge,
				"no-tile-progress-no-rejoin-target", observation);
		}
		session.transitionTo(NavigationPhase.FOLLOWING_ROUTE,
			"no-tile-progress-rejoin-click");
		return publish(NavigationDecision.recoveryClick(selection,
			routeHandoffDistance(selection.getRawIndex()), RecoveryCause.NO_TILE_PROGRESS,
			attempts, MAX_NO_TILE_PROGRESS_ATTEMPTS, progressAge,
			"no-tile-progress-rejoin-click"), observation);
	}

	private NavigationDecision replanNoTileProgress(int attempts, long progressAge,
		String reason, NavigationObservation observation)
	{
		clearCommandTarget();
		session.transitionTo(NavigationPhase.REPLANNING, reason);
		return publish(NavigationDecision.recovery(NavigationDecision.Type.REQUEST_REPLAN,
			RecoveryCause.NO_TILE_PROGRESS, attempts, MAX_NO_TILE_PROGRESS_ATTEMPTS,
			progressAge, -1, reason), observation);
	}

	private boolean isAwaitingCommandProgress(NavigationObservation observation)
	{
		long progressAge = observation.getObservedAtMs()
			- Math.max(session.lastCommandAtMs, session.lastProgressAtMs);
		return !observation.isMoving() && session.commandTarget != null
			&& session.commandOriginRawIndex >= 0
			&& session.rawProgressIndex <= session.commandOriginRawIndex
			&& progressAge < NO_TILE_PROGRESS_TIMEOUT_MS;
	}

	private boolean hasRecentValidCommand(long observedAtMs)
	{
		return session.commandTarget != null
			&& observedAtMs - session.lastCommandAtMs < COMMAND_ACK_TIMEOUT_MS;
	}

	private static int recoveryBudget(RecoveryCause cause)
	{
			switch (cause)
		{
			case NO_ACKNOWLEDGEMENT: return MAX_NO_ACKNOWLEDGEMENT_ATTEMPTS;
			case COMMAND_DESTINATION_MISMATCH:
				return MAX_COMMAND_DESTINATION_MISMATCH_ATTEMPTS;
			case NO_TILE_PROGRESS: return MAX_NO_TILE_PROGRESS_ATTEMPTS;
			case OFF_ROUTE: return MAX_OFF_ROUTE_ATTEMPTS;
			case BLOCKED_EDGE: return MAX_BLOCKED_EDGE_ATTEMPTS;
			case INTERACTION_UNAVAILABLE: return MAX_INTERACTION_UNAVAILABLE_ATTEMPTS;
			case ROUTE_EXHAUSTED: return MAX_ROUTE_EXHAUSTED_ATTEMPTS;
			case EXTERNAL_REPLAN: return MAX_EXTERNAL_REPLAN_ATTEMPTS;
			default: return 0;
		}
	}

	private NavigationDecision handleRouteInteraction(NavigationObservation observation)
	{
		RouteInteraction observed = observation.getRouteInteraction();
		if (observed != null && observed.getGeneration() == session.generation)
		{
			if (interactionStageAdvanced(session.pendingInteraction, observed))
			{
				session.interactionCommandPending = false;
				session.interactionCommandDeadlineMs = 0L;
			}
			session.pendingInteraction = observed;
		}
		RouteInteraction pending = session.pendingInteraction;
		if (pending == null)
		{
			// Retain the Phase 2 boolean scaffold for shadow-corpus compatibility.
			if (!observation.isInteractionFrontier())
			{
				return null;
			}
			if (observation.isInteractionCommandInFlight() || observation.isInteracting())
			{
				session.transitionTo(NavigationPhase.VERIFYING_INTERACTION,
					"interaction-command-in-flight");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"interaction-command-in-flight"), observation);
			}
			if (observation.isMoving())
			{
				session.transitionTo(NavigationPhase.APPROACHING_INTERACTION,
					"approaching-interaction-frontier");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"approaching-interaction-frontier"), observation);
			}
			session.transitionTo(NavigationPhase.PERFORMING_INTERACTION,
				"interaction-frontier-ready");
			return publish(NavigationDecision.of(NavigationDecision.Type.INTERACT,
				"interaction-frontier-ready"), observation);
		}

		boolean remoteLandingRequired = (pending.getKind() == RouteInteraction.Kind.NPC_TRANSPORT
			|| pending.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
			|| pending.getKind() == RouteInteraction.Kind.CHARTER_SHIP
			|| pending.getKind() == RouteInteraction.Kind.FAIRY_RING
			|| pending.getKind() == RouteInteraction.Kind.SPIRIT_TREE)
			&& pending.getStatus() != RouteInteraction.Status.CLEARED;
		if (session.rawProgressIndex > pending.getRawEdgeIndex() && !remoteLandingRequired)
		{
			clearPendingInteraction();
			// Yield one observation after retiring the blocker. This lets the live scanner publish
			// the next nearby interaction before ordinary lookahead can jump beyond it.
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
				"interaction-edge-crossed"), observation);
		}
		if (pending.getStatus() == RouteInteraction.Status.UNAVAILABLE)
		{
			// A command in flight owns the interaction until its acknowledgement deadline.
			// Mid-crossing the scene can transiently fail to re-resolve the object (the
			// Land's End gangplank read as unavailable two seconds after a successful
			// Cross); replanning then abandons a crossing that is actually happening.
			if (session.interactionCommandPending
				&& observation.getObservedAtMs() < session.interactionCommandDeadlineMs)
			{
				session.transitionTo(NavigationPhase.VERIFYING_INTERACTION,
					"interaction-unavailable-command-in-flight");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"interaction-unavailable-command-in-flight"), observation);
			}
			return requestReplan(RecoveryCause.INTERACTION_UNAVAILABLE,
				"interaction-unavailable", observation);
		}
		if (pending.getStatus() == RouteInteraction.Status.CLEARED)
		{
			session.interactionCommandPending = false;
			session.interactionCommandDeadlineMs = 0L;
			RouteInteraction next = observation.getNextRouteInteraction();
			if (canChainInteraction(pending, next))
			{
				session.clearedInteractionsAwaitingCrossing.add(pending);
				session.pendingInteraction = next;
				session.interactionClearedObserved = false;
				session.transitionTo(NavigationPhase.PERFORMING_INTERACTION,
					"interaction-chain-ready");
				return publish(NavigationDecision.interact(next,
					"interaction-chain-ready"), observation);
			}
			if (!session.interactionClearedObserved)
			{
				session.interactionClearedObserved = true;
				session.transitionTo(NavigationPhase.VERIFYING_INTERACTION,
					"interaction-cleared-awaiting-crossing");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"interaction-cleared-awaiting-crossing"), observation);
			}
			if (observation.isMoving())
			{
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"crossing-cleared-interaction-edge"), observation);
			}
			RouteClickSelection crossing = next == null
				? RouteClickSelector.select(session.routePlan, observation.getPlayerLocation(),
					pending.getRawEdgeIndex(), routeClickReach(), MAX_ROUTE_CLICK_REACH)
				: null;
			if (crossing == null)
			{
				int distance = observation.getPlayerLocation() == null ? -1
					: observation.getPlayerLocation().distanceTo2D(pending.getTo());
				crossing = new RouteClickSelection(pending.getTo(),
					pending.getRawEdgeIndex() + 1, -1, distance, Math.max(0, distance),
					"interaction-edge-crossing");
			}
			session.transitionTo(NavigationPhase.FOLLOWING_ROUTE,
				"cross-cleared-interaction-edge");
			return publish(NavigationDecision.click(crossing,
				"interaction-edge-crossing".equals(crossing.getSelection()) ? 1
					: routeHandoffDistance(crossing.getRawIndex()),
				"cross-cleared-interaction-edge"), observation);
		}

		session.interactionClearedObserved = false;
		if (session.interactionCommandPending || observation.isInteracting())
		{
			if (session.interactionCommandPending
				&& !observation.isMoving() && !observation.isInteracting()
				&& observation.getObservedAtMs() >= session.interactionCommandDeadlineMs)
			{
				session.interactionCommandPending = false;
				session.interactionCommandDeadlineMs = 0L;
			}
			else
			{
				session.transitionTo(NavigationPhase.VERIFYING_INTERACTION,
					"interaction-command-in-flight");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"interaction-command-in-flight"), observation);
			}
		}
		if (!pending.isReady())
		{
			if (observation.isMoving())
			{
				session.transitionTo(NavigationPhase.APPROACHING_INTERACTION,
					"approaching-interaction-frontier");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"approaching-interaction-frontier"), observation);
			}
			int distance = observation.getPlayerLocation() == null ? -1
				: observation.getPlayerLocation().distanceTo2D(pending.getFrom());
			RouteClickSelection approach = new RouteClickSelection(pending.getFrom(),
				pending.getRawEdgeIndex(), -1, distance, Math.max(0, distance),
				"interaction-approach");
			session.transitionTo(NavigationPhase.APPROACHING_INTERACTION,
				"approach-interaction-origin");
			return publish(NavigationDecision.click(approach, 2,
				"approach-interaction-origin"), observation);
		}
		// An object command supersedes the current ground movement and lets the server path to the
		// exact interaction point. Do not wait for a preceding minimap command to finish once the
		// blocker is loaded and within the resolver's bounded dispatch range.
		session.transitionTo(NavigationPhase.PERFORMING_INTERACTION,
			"interaction-frontier-ready");
		return publish(NavigationDecision.interact(pending,
			"interaction-frontier-ready"), observation);
	}

	private void clearPendingInteraction()
	{
		session.pendingInteraction = null;
		session.interactionCommandPending = false;
		session.interactionCommandDeadlineMs = 0L;
		session.interactionClearedObserved = false;
	}

	private int interactionCommandDistance(RouteInteraction interaction)
	{
		if (interaction == null || interaction.getObjectTile() == null
			|| session.lastObservedPlayer == null
			|| interaction.getObjectTile().getPlane() != session.lastObservedPlayer.getPlane())
		{
			return 0;
		}
		return session.lastObservedPlayer.distanceTo2D(interaction.getObjectTile());
	}

	private boolean canChainInteraction(RouteInteraction cleared, RouteInteraction next)
	{
		return next != null
			&& next.getGeneration() == session.generation
			&& next.getRawEdgeIndex() > cleared.getRawEdgeIndex()
			&& next.getStatus() == RouteInteraction.Status.AVAILABLE
			&& next.isReady();
	}

	private void retireCrossedChainedInteractions()
	{
		session.clearedInteractionsAwaitingCrossing.removeIf(
			interaction -> session.rawProgressIndex > interaction.getRawEdgeIndex());
	}

	private static String recoveryReason(RecoveryCause cause)
	{
		return cause.name().toLowerCase().replace('_', '-');
	}

	private long recoveryAgeMs(RecoveryCause cause, NavigationObservation observation)
	{
		if (cause == RecoveryCause.NO_ACKNOWLEDGEMENT
			|| cause == RecoveryCause.COMMAND_DESTINATION_MISMATCH)
		{
			return Math.max(0L, observation.getObservedAtMs() - session.lastCommandAtMs);
		}
		if (cause == RecoveryCause.NO_TILE_PROGRESS)
		{
			return Math.max(0L, observation.getObservedAtMs()
				- Math.max(session.lastCommandAtMs, session.lastProgressAtMs));
		}
		return -1L;
	}

	public synchronized NavigationSnapshot cancel(String reason)
	{
		if (session == null)
		{
			return null;
		}
		if (!session.phase.isTerminal())
		{
			session.request.cancel();
			session.transitionTo(NavigationPhase.CANCELLED, reason);
			session.lastDecision = NavigationDecision.of(NavigationDecision.Type.NO_ACTION, reason);
		}
		return snapshot();
	}

	public synchronized NavigationSnapshot snapshot()
	{
		return session == null ? null : new NavigationSnapshot(session);
	}

	private NavigationDecision terminal(NavigationObservation observation)
	{
		NavigationPhase phase;
		NavigationDecision.Type decisionType;
		switch (observation.getTerminalSignal())
		{
			case ARRIVED:
				phase = NavigationPhase.ARRIVED;
				decisionType = NavigationDecision.Type.COMPLETE;
				break;
			case UNREACHABLE:
				phase = NavigationPhase.UNREACHABLE;
				decisionType = NavigationDecision.Type.FAIL;
				break;
			case FAILED:
				phase = NavigationPhase.FAILED;
				decisionType = NavigationDecision.Type.FAIL;
				break;
			case CANCELLED:
			default:
				phase = NavigationPhase.CANCELLED;
				decisionType = NavigationDecision.Type.NO_ACTION;
				break;
		}
		String reason = observation.getLegacyReason().isEmpty()
			? "legacy-terminal-" + phase.name().toLowerCase()
			: observation.getLegacyReason();
		session.transitionTo(phase, reason);
		return publish(NavigationDecision.of(decisionType, reason), observation);
	}

	private NavigationDecision publish(NavigationDecision decision, NavigationObservation observation)
	{
		session.lastDecision = decision;
		if (decision.issuesInput())
		{
			session.lastCommandAtMs = observation.getObservedAtMs();
		}
		session.comparison = compare(decision.getType(), observation.getLegacyDecisionType());
		return decision;
	}

	private static NavigationComparison compare(NavigationDecision.Type shadow,
		NavigationDecision.Type legacy)
	{
		if (legacy == null)
		{
			return NavigationComparison.NOT_OBSERVED;
		}
		if (shadow == legacy)
		{
			return NavigationComparison.MATCH;
		}
		boolean shadowPassive = shadow == NavigationDecision.Type.NO_ACTION
			|| shadow == NavigationDecision.Type.WAIT;
		boolean legacyPassive = legacy == NavigationDecision.Type.NO_ACTION
			|| legacy == NavigationDecision.Type.WAIT;
		if (!shadowPassive && legacyPassive)
		{
			return NavigationComparison.SHADOW_ONLY;
		}
		if (shadowPassive && !legacyPassive)
		{
			return NavigationComparison.LEGACY_ONLY;
		}
		return NavigationComparison.DIVERGED;
	}

	private void updateProgress(WorldPoint player, long observedAtMs)
	{
		if (player == null || session.routePlan == null)
		{
			return;
		}
		session.routeDistance = closestDistance(session.routePlan.getRawPath(), player);
		session.rawProgressIndex = closestForwardIndex(session.routePlan.getRawPath(), player,
			session.rawProgressIndex);
		session.smoothedProgressIndex = closestForwardIndex(session.routePlan.getSmoothedPath(), player,
			session.smoothedProgressIndex);
		if (session.lastProgressAtMs == 0L
			|| session.rawProgressIndex > session.lastProgressRawIndex)
		{
			session.lastProgressAtMs = observedAtMs;
			session.lastProgressRawIndex = session.rawProgressIndex;
		}
	}

	private boolean hasArrived(WorldPoint player)
	{
		if (player == null)
		{
			return false;
		}
		for (WorldPoint destination : session.request.getDestinations())
		{
			if (player.getPlane() == destination.getPlane()
				&& player.distanceTo2D(destination) <= session.request.getReachedDistance())
			{
				return true;
			}
		}
		return false;
	}

	private boolean hasUnresolvedRouteInteraction(NavigationObservation observation)
	{
		RouteInteraction observed = observation.getRouteInteraction();
		return session.pendingInteraction != null
			|| session.interactionCommandPending
			|| !session.clearedInteractionsAwaitingCrossing.isEmpty()
			|| observed != null && observed.getGeneration() == session.generation;
	}

	private boolean hasUncrossedEngineInteractionEdge()
	{
		if (session.routePlan == null)
		{
			return false;
		}
		return session.routePlan.getRouteEdges().stream()
			.anyMatch(edge -> (edge.getKind() == RouteEdge.Kind.SIMPLE_TELEPORT
				|| edge.getKind() == RouteEdge.Kind.NPC_TRANSPORT
				|| edge.getKind() == RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT
				|| edge.getKind() == RouteEdge.Kind.CHARTER_SHIP
				|| edge.getKind() == RouteEdge.Kind.FAIRY_RING
				|| edge.getKind() == RouteEdge.Kind.SPIRIT_TREE
				|| edge.getKind() == RouteEdge.Kind.ADJACENT_TRANSPORT
				|| edge.getKind() == RouteEdge.Kind.CATALOG_TRANSITION)
				&& session.rawProgressIndex <= edge.getRawIndex());
	}

	private static boolean interactionStageAdvanced(RouteInteraction previous,
		RouteInteraction observed)
	{
		return previous != null && previous.getGeneration() == observed.getGeneration()
			&& previous.getRawEdgeIndex() == observed.getRawEdgeIndex()
			&& previous.getKind() == observed.getKind()
			&& previous.getStatus() == RouteInteraction.Status.AVAILABLE
			&& observed.getStatus() == RouteInteraction.Status.AVAILABLE
			&& !previous.getAction().equalsIgnoreCase(observed.getAction());
	}

	private static int closestForwardIndex(List<WorldPoint> path, WorldPoint player, int currentIndex)
	{
		int start = Math.max(0, currentIndex);
		int end = currentIndex < 0 ? path.size() : Math.min(path.size(),
			currentIndex + MAX_PROGRESS_ADVANCE_PER_OBSERVATION + 1);
		int bestIndex = currentIndex;
		int bestDistance = Integer.MAX_VALUE;
		for (int i = start; i < end; i++)
		{
			WorldPoint tile = path.get(i);
			if (tile.getPlane() != player.getPlane())
			{
				continue;
			}
			int distance = tile.distanceTo2D(player);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	private static int closestDistance(List<WorldPoint> path, WorldPoint player)
	{
		int bestDistance = Integer.MAX_VALUE;
		for (WorldPoint tile : path)
		{
			if (tile.getPlane() == player.getPlane())
			{
				bestDistance = Math.min(bestDistance, tile.distanceTo2D(player));
			}
		}
		return bestDistance;
	}

	private int routeClickReach()
	{
		int span = MAX_ROUTE_CLICK_REACH - MIN_ROUTE_CLICK_REACH + 1;
		long seed = session.request.getRequestId() * 31L + session.generation * 17L
			+ Math.max(0, session.rawProgressIndex) * 13L;
		return MIN_ROUTE_CLICK_REACH + Math.floorMod((int) (seed ^ (seed >>> 32)), span);
	}

	private int routeHandoffDistance(int targetRawIndex)
	{
		int span = MAX_COMMAND_HANDOFF_DISTANCE - MIN_COMMAND_HANDOFF_DISTANCE + 1;
		long seed = session.request.getRequestId() * 19L + session.generation * 23L
			+ Math.max(0, targetRawIndex) * 29L;
		return MIN_COMMAND_HANDOFF_DISTANCE
			+ Math.floorMod((int) (seed ^ (seed >>> 32)), span);
	}

	private boolean isProximityHandoff(NavigationObservation observation)
	{
		if (!observation.isMoving() || session.commandTarget == null
			|| session.commandHandoffDistance < 0 || observation.getPlayerLocation() == null
			|| session.commandTarget.getPlane() != observation.getPlayerLocation().getPlane()
			|| session.routePlan == null
			|| session.commandRawIndex >= session.routePlan.getRawPath().size() - 1)
		{
			return false;
		}
		return session.commandTarget.distanceTo2D(observation.getPlayerLocation())
			<= session.commandHandoffDistance;
	}

	private boolean isDivergentCommandDestination(WorldPoint destination)
	{
		if (destination == null || session.commandTarget == null)
		{
			return false;
		}
		if (destination.getPlane() != session.commandTarget.getPlane())
		{
			return true;
		}
		if (destination.distanceTo2D(session.commandTarget) <= COMMAND_DESTINATION_TOLERANCE)
		{
			return false;
		}
		return session.routePlan == null
			|| closestDistance(session.routePlan.getRawPath(), destination)
			> DESTINATION_ROUTE_TOLERANCE;
	}

	private void clearCommandTarget()
	{
		session.commandDestinationAtIssue = null;
		session.commandTarget = null;
		session.commandRawIndex = -1;
		session.commandHandoffDistance = -1;
		session.commandOriginRawIndex = -1;
	}
}
