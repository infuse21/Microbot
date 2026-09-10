package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentObservation;
import net.runelite.client.plugins.microbot.util.walker.transport.LeafPitPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.NpcDialogueTransportPolicy;
import net.runelite.client.plugins.microbot.util.walker.transport.TeleportationPortalPolicy;

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
	private static final long HOME_TELEPORT_COMMAND_TIMEOUT_MS = 35_000L;
	private static final long DIALOGUE_CONTINUE_COMMAND_TIMEOUT_MS = 2_000L;
	private static final long NPC_DIALOGUE_TRANSPORT_COMMAND_TIMEOUT_MS = 60_000L;
	private static final long JUNGLE_OBSTACLE_COMMAND_TIMEOUT_MS = 60_000L;
	private static final int MAX_STOCHASTIC_TRANSITION_ATTEMPTS = 5;
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
		SpellEquipmentTransaction retained = session == null ? null : session.equipmentTransaction;
		if (session != null && !session.phase.isTerminal())
		{
			cancel("replaced-by-request-" + request.getRequestId());
		}
		session = new WalkSession(request);
		session.equipmentTransaction = retained;
		session.equipmentRestorationRequired = retained != null;
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
			session.lastObservedDestination = observation.getMovementDestination();
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

		if (session.equipmentRestorationRequired) return awaitEquipmentRestoration(observation);
		updateProgress(observation.getPlayerLocation(), observation.getObservedAtMs());
		retireCrossedChainedInteractions();
		if (hasArrived(observation.getPlayerLocation())
			&& !hasUncrossedEngineInteractionEdge()
			&& !hasUnresolvedRouteInteraction(observation))
		{
			if (session.equipmentTransaction != null) return awaitEquipmentRestoration(observation);
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
			if (LeafPitPolicy.owns(session.pendingInteraction)
				&& LeafPitPolicy.inPit(observation.getPlayerLocation()))
			{
				return failLeafRecovery("leaf-pit-recovery-rejected", observation);
			}
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
		WorldPoint movementDestination = observation.getMovementDestination();
		if (!session.commandPending && movementDestination != null && session.commandTarget != null
			&& isDivergentCommandDestination(movementDestination))
		{
			return requestDestinationMismatch(movementDestination, observation);
		}
		// Long-distance interactions can move through a temporary ship deck, cutscene,
		// or loading scene that is intentionally nowhere near the published raw route.
		// While the interaction acknowledgement window owns the command, let its
		// destination predicate observe that state before ordinary off-route recovery.
		if (session.interactionCommandPending || LeafPitPolicy.owns(session.pendingInteraction))
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
			if (isWalkerMovementInFlight(observation))
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
			if (isWalkerMovementInFlight(observation))
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
		if (observation.isMoving() && session.commandTarget == null && movementDestination != null)
		{
			NavigationDecision rejoin = tryRouteRejoin(observation, 0, 0L);
			return rejoin != null ? rejoin : requestReplan(RecoveryCause.OFF_ROUTE,
				"unowned-movement-cannot-rejoin", observation);
		}
		if (observation.isMoving() && !proximityHandoff)
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
			WorldPoint endpoint = rawPath.get(rawPath.size() - 1);
			WorldPoint player = observation.getPlayerLocation();
			// Nearest-raw progress can reach the last index before the caller's radius is satisfied.
			if (player != null && player.getPlane() == endpoint.getPlane() && hasArrived(endpoint))
			{
				int distance = (int) Math.ceil(Math.hypot(endpoint.getX() - player.getX(),
					endpoint.getY() - player.getY()));
				if (distance > 0 && distance <= MAX_ROUTE_CLICK_REACH)
				{
					RouteClickSelection finish = new RouteClickSelection(endpoint, rawPath.size() - 1,
						session.routePlan.getSmoothedPath().size() - 1, distance, MAX_ROUTE_CLICK_REACH,
						"route-end-approach");
					session.transitionTo(NavigationPhase.FOLLOWING_ROUTE, "route-end-approach");
					return publish(NavigationDecision.click(finish, 1, "route-end-approach"), observation);
				}
			}
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
			if (decision.getInteraction() != null
				&& decision.getInteraction().getKind() == RouteInteraction.Kind.SPELL_EQUIPMENT)
			{
				recordEquipmentCommand(decision.getInteraction());
				return;
			}
			session.interactionCommandPending = issued;
			session.interactionCommandOrigin = issued ? session.lastObservedPlayer : null;
			int distance = interactionCommandDistance(decision.getInteraction());
			long timeout = isLongHomeTeleport(decision.getInteraction())
				? HOME_TELEPORT_COMMAND_TIMEOUT_MS
				: decision.getInteraction() != null
				&& decision.getInteraction().getKind()
					== RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
				&& NpcDialogueTransportPolicy.CONTINUE_ACTION.equals(
					decision.getInteraction().getAction())
				? DIALOGUE_CONTINUE_COMMAND_TIMEOUT_MS
				: decision.getInteraction() != null
				&& decision.getInteraction().getKind()
					== RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
				? NPC_DIALOGUE_TRANSPORT_COMMAND_TIMEOUT_MS
				: decision.getInteraction() != null
					&& decision.getInteraction().getKind()
						== RouteInteraction.Kind.JUNGLE_OBSTACLE
				? JUNGLE_OBSTACLE_COMMAND_TIMEOUT_MS
				: decision.getInteraction() != null
				&& (decision.getInteraction().getKind() == RouteInteraction.Kind.NPC_TRANSPORT
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.ITEM_TELEPORT
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.CHARTER_SHIP
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.FAIRY_RING
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.SPIRIT_TREE
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.GNOME_GLIDER
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.QUETZAL
					|| decision.getInteraction().getKind()
						== RouteInteraction.Kind.TELEPORTATION_LEVER
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.CANOE
					|| decision.getInteraction().getKind() == RouteInteraction.Kind.MINECART
					|| decision.getInteraction().getKind()
						== RouteInteraction.Kind.TELEPORTATION_PORTAL
					|| decision.getInteraction().getKind()
						== RouteInteraction.Kind.MINIGAME_TELEPORT
					|| decision.getInteraction().getKind()
						== RouteInteraction.Kind.MAGIC_MUSHTREE
					|| decision.getInteraction().getKind()
						== RouteInteraction.Kind.HOT_AIR_BALLOON)
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
		if (attempts == 1)
		{
			NavigationDecision rejoin = tryRouteRejoin(observation, attempts, ageMs);
			if (rejoin != null) return rejoin;
		}
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

	private NavigationDecision tryRouteRejoin(NavigationObservation observation, int attempts, long ageMs)
	{
		if (hasUnresolvedRouteInteraction(observation) || observation.isInteractionFrontier()
			|| observation.isInteractionCommandInFlight()) return null;
		RouteClickSelection selected = RouteClickSelector.select(session.routePlan,
			observation.getPlayerLocation(), session.rawProgressIndex, 6, MAX_ROUTE_CLICK_REACH);
		if (selected == null) return null;
		for (RouteEdge edge : session.routePlan.getRouteEdges())
		{
			if (edge.getRawIndex() >= session.rawProgressIndex
				&& edge.getRawIndex() < selected.getRawIndex() && edge.getKind() != RouteEdge.Kind.WALK)
			{
				return null;
			}
		}
		RouteClickSelection correction = new RouteClickSelection(selected.getTarget(), selected.getRawIndex(),
			selected.getSmoothedIndex(), selected.getDistance(), selected.getReach(), "route-rejoin");
		session.transitionTo(NavigationPhase.FOLLOWING_ROUTE, "correcting-unowned-movement");
		return publish(NavigationDecision.recoveryClick(correction, 1,
			RecoveryCause.COMMAND_DESTINATION_MISMATCH, attempts,
			MAX_COMMAND_DESTINATION_MISMATCH_ATTEMPTS, ageMs, "correcting-unowned-movement"), observation);
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
		if (cause == RecoveryCause.INTERACTION_WAIT || isWalkerMovementInFlight(observation)
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
			RouteInteraction previous = session.pendingInteraction;
			boolean leafStageAdvanced = LeafPitPolicy.owns(previous) && LeafPitPolicy.owns(observed)
				&& previous.getRawEdgeIndex() == observed.getRawEdgeIndex()
				&& observed.getStatus() == RouteInteraction.Status.AVAILABLE
				&& !previous.getAction().equals(observed.getAction());
			if (leafStageAdvanced && LeafPitPolicy.RECOVER.equals(observed.getAction()))
			{
				// Count the failed jump once, but always climb out before exhausting its budget.
				session.stochasticTransitionAttempts++;
			}
			if (interactionStageAdvanced(previous, observed) || leafStageAdvanced)
			{
				session.interactionCommandPending = false;
				session.interactionCommandOrigin = null;
				session.interactionCommandDeadlineMs = 0L;
			}
			else if (dialogueVoyageStarted(previous, observed)
				&& session.interactionCommandPending)
			{
				// Intermediate Continue frames remain visible and use the short retry window.
				// Once the dialogue disappears, the same click may own a real voyage through
				// a temporary ship scene, so preserve it until the bounded landing deadline.
				session.interactionCommandDeadlineMs = Math.max(
					session.interactionCommandDeadlineMs,
					observation.getObservedAtMs() + NPC_DIALOGUE_TRANSPORT_COMMAND_TIMEOUT_MS);
			}
			session.pendingInteraction = observed;
		}
		RouteInteraction pending = session.pendingInteraction;
		if (LeafPitPolicy.owns(pending) && "Jump".equals(pending.getAction())
			&& pending.getCrossingFrom().equals(observation.getPlayerLocation())
			&& session.stochasticTransitionAttempts >= MAX_STOCHASTIC_TRANSITION_ATTEMPTS)
		{
			return requestReplan(RecoveryCause.NO_ACKNOWLEDGEMENT,
				"stochastic-transition-attempts-exhausted", observation);
		}
		if (pending == null)
		{
			// Retain the Phase 2 boolean scaffold for shadow-corpus compatibility.
			if (!observation.isInteractionFrontier())
			{
				return null;
			}
			if (observation.isInteractionCommandInFlight())
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
		if (failedShortTransitionMovedBehindOrigin(pending,
			observation.getPlayerLocation()))
		{
			session.interactionCommandPending = false;
			session.interactionCommandOrigin = null;
			session.interactionCommandDeadlineMs = 0L;
			return requestReplan(RecoveryCause.INTERACTION_UNAVAILABLE,
				"interaction-displaced-behind-origin", observation);
		}

		if (pending.getKind() == RouteInteraction.Kind.SIMPLE_TELEPORT
			&& pending.getStatus() == RouteInteraction.Status.CLEARED
			&& session.equipmentTransaction != null) return awaitEquipmentRestoration(observation);
		boolean remoteLandingRequired = (pending.getKind() == RouteInteraction.Kind.SIMPLE_TELEPORT
			|| pending.getKind() == RouteInteraction.Kind.NPC_TRANSPORT
			|| pending.getKind() == RouteInteraction.Kind.ITEM_TELEPORT
			|| pending.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
			|| pending.getKind() == RouteInteraction.Kind.CHARTER_SHIP
			|| pending.getKind() == RouteInteraction.Kind.FAIRY_RING
			|| pending.getKind() == RouteInteraction.Kind.SPIRIT_TREE
			|| pending.getKind() == RouteInteraction.Kind.GNOME_GLIDER
			|| pending.getKind() == RouteInteraction.Kind.QUETZAL
			|| pending.getKind() == RouteInteraction.Kind.TELEPORTATION_LEVER
			|| pending.getKind() == RouteInteraction.Kind.WILDERNESS_DITCH
			|| pending.getKind() == RouteInteraction.Kind.JUNGLE_OBSTACLE
			|| pending.getKind() == RouteInteraction.Kind.CANOE
			|| pending.getKind() == RouteInteraction.Kind.MINECART
			|| pending.getKind() == RouteInteraction.Kind.TELEPORTATION_PORTAL
			|| pending.getKind() == RouteInteraction.Kind.MINIGAME_TELEPORT
			|| pending.getKind() == RouteInteraction.Kind.MAGIC_MUSHTREE
			|| pending.getKind() == RouteInteraction.Kind.HOT_AIR_BALLOON
			|| pending.getKind() == RouteInteraction.Kind.CATALOG_TRANSITION)
			&& pending.getStatus() != RouteInteraction.Status.CLEARED;
		if (pending.getKind() == RouteInteraction.Kind.ADJACENT_TRANSPORT
			&& (pending.getObjectId() == 190 || pending.getObjectId() == 12723 || pending.getObjectId() == 12725
				|| pending.getObjectId() == 8738 || pending.getObjectId() == 8739))
		{
			remoteLandingRequired = !net.runelite.client.plugins.microbot.util.walker.transport
				.AdjacentTransportRouteScanner.hasCrossedCatalogBoundary(pending, observation.getPlayerLocation());
		}
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
			if (LeafPitPolicy.owns(pending) && LeafPitPolicy.inPit(observation.getPlayerLocation()))
			{
				return failLeafRecovery("leaf-pit-recovery-unavailable", observation);
			}
			return requestReplan(RecoveryCause.INTERACTION_UNAVAILABLE,
				"interaction-unavailable", observation);
		}
		if (pending.getStatus() == RouteInteraction.Status.CLEARED)
		{
			session.interactionCommandPending = false;
			session.interactionCommandOrigin = null;
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
		// An actor interaction may be combat, not a command issued by this walker.
		if (session.interactionCommandPending)
		{
			if (LeafPitPolicy.owns(pending) && LeafPitPolicy.RECOVER.equals(pending.getAction())
				&& observation.getObservedAtMs() >= session.interactionCommandDeadlineMs)
			{
				return failLeafRecovery("leaf-pit-recovery-not-acknowledged", observation);
			}
			if (!observation.isMoving()
				&& observation.getObservedAtMs() >= session.interactionCommandDeadlineMs)
			{
				session.interactionCommandPending = false;
				session.interactionCommandOrigin = null;
				session.interactionCommandDeadlineMs = 0L;
				if (isStochasticCatalogTransition(pending)
					&& ++session.stochasticTransitionAttempts
					>= MAX_STOCHASTIC_TRANSITION_ATTEMPTS)
				{
					return requestReplan(RecoveryCause.NO_ACKNOWLEDGEMENT,
						"stochastic-transition-attempts-exhausted", observation);
				}
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
			if (LeafPitPolicy.owns(pending) && LeafPitPolicy.inPit(observation.getPlayerLocation()))
			{
				return failLeafRecovery("leaf-pit-recovery-out-of-range", observation);
			}
			if (observation.isMoving())
			{
				session.transitionTo(NavigationPhase.APPROACHING_INTERACTION,
					"approaching-interaction-frontier");
				return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
					"approaching-interaction-frontier"), observation);
			}
			WorldPoint approachTile = pending.getKind() == RouteInteraction.Kind.TELEPORTATION_PORTAL
				&& TeleportationPortalPolicy.isDirectPohObjectId(pending.getObjectId())
				? pending.getObjectTile() : pending.getFrom();
			int distance = observation.getPlayerLocation() == null ? -1
				: observation.getPlayerLocation().distanceTo2D(approachTile);
			RouteClickSelection approach = new RouteClickSelection(approachTile,
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
		session.interactionCommandOrigin = null;
		session.interactionCommandDeadlineMs = 0L;
		session.interactionClearedObserved = false;
		session.stochasticTransitionAttempts = 0;
	}

	private NavigationDecision failLeafRecovery(String reason, NavigationObservation observation)
	{
		session.transitionTo(NavigationPhase.FAILED, reason);
		return publish(NavigationDecision.of(NavigationDecision.Type.FAIL, reason), observation);
	}

	private static boolean isStochasticCatalogTransition(RouteInteraction interaction)
	{
		return interaction != null
			&& interaction.getKind() == RouteInteraction.Kind.CATALOG_TRANSITION
			&& (interaction.getObjectId() == 2234 || interaction.getObjectId() == 2236
				|| interaction.getObjectId() == 3922 || interaction.getObjectId() == 3925
				|| interaction.getObjectId() == 16544);
	}

	private static boolean isLongHomeTeleport(RouteInteraction interaction)
	{
		return interaction != null
			&& interaction.getKind() == RouteInteraction.Kind.SIMPLE_TELEPORT
			&& "Lumbridge Home Teleport".equalsIgnoreCase(interaction.getAction());
	}

	public synchronized void recordInteractionPreparation(NavigationDecision decision,
		long commandAtMs)
	{
		if (session == null || session.phase.isTerminal() || decision == null
			|| decision.getType() != NavigationDecision.Type.INTERACT)
		{
			return;
		}
		session.lastCommandAtMs = commandAtMs;
		if (decision.getInteraction() != null
			&& decision.getInteraction().getKind() == RouteInteraction.Kind.SPELL_EQUIPMENT)
		{
			recordEquipmentCommand(decision.getInteraction());
			return;
		}
		session.interactionCommandPending = false;
		session.interactionCommandOrigin = null;
		session.interactionCommandDeadlineMs = 0L;
		session.commandRejected = false;
	}

	private boolean isWalkerMovementInFlight(NavigationObservation observation)
	{
		WorldPoint movementDestination = observation.getMovementDestination();
		return observation.isMoving() && session.commandTarget != null
			&& movementDestination != null
			&& !isDivergentCommandDestination(movementDestination);
	}

	/**
	 * A failed short crossing can relocate the player behind the edge that was actually
	 * attempted. Retaining that later pending edge makes ranged dispatch skip the earlier
	 * crossings needed to reach it again; force the next plan to start at the real landing.
	 */
	private boolean failedShortTransitionMovedBehindOrigin(RouteInteraction pending,
		WorldPoint player)
	{
		if (LeafPitPolicy.owns(pending)
			&& (LeafPitPolicy.inPit(player) || LeafPitPolicy.RECOVER.equals(pending.getAction())))
		{
			return false;
		}
		if (!session.interactionCommandPending
			|| pending.getKind() != RouteInteraction.Kind.CATALOG_TRANSITION
			|| session.interactionCommandOrigin == null || player == null)
		{
			return false;
		}
		WorldPoint origin = pending.getCrossingFrom();
		WorldPoint destination = pending.getCrossingTo();
		if (player.distanceTo2D(session.interactionCommandOrigin) > 1
			&& net.runelite.client.plugins.microbot.util.walker.transport.NorthernQuestShortcutPolicy
				.fellToEarlierStage(pending, player)) return true;
		if (origin.getPlane() != destination.getPlane()
			|| player.getPlane() != origin.getPlane()
			|| origin.distanceTo2D(destination) > 4
			|| session.interactionCommandOrigin.getPlane() != origin.getPlane()
			|| player.distanceTo2D(session.interactionCommandOrigin) <= 1)
		{
			return false;
		}
		int dx = destination.getX() - origin.getX();
		int dy = destination.getY() - origin.getY();
		int commandProjection = (session.interactionCommandOrigin.getX() - origin.getX()) * dx
			+ (session.interactionCommandOrigin.getY() - origin.getY()) * dy;
		int playerProjection = (player.getX() - origin.getX()) * dx
			+ (player.getY() - origin.getY()) * dy;
		return (dx != 0 || dy != 0)
			&& playerProjection < commandProjection;
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

	public synchronized boolean retainEquipmentTransaction(long requestId, long generation,
		SpellEquipmentTransaction transaction)
	{
		if (session == null || transaction == null || session.phase.isTerminal()
			|| session.request.getCancellationToken().isCancelled()
			|| session.request.getRequestId() != requestId || session.generation != generation
			|| session.executionMode != NavigationExecutionMode.ENGINE_SUPPORTED
			|| session.equipmentRestorationRequired) return false;
		if (session.equipmentTransaction != null) return session.equipmentTransaction == transaction;
		session.equipmentTransaction = transaction;
		session.equipmentRestorationStartedAtMs = -1L;
		session.equipmentCommandAttempted = false;
		session.equipmentTabCommandAttempted = false;
		return true;
	}

	/** Caller must supply a confirmed equipment snapshot, not an unavailable cache result. */
	public synchronized boolean acknowledgeEquipmentRestored(long requestId, long generation,
		SpellEquipmentTransaction transaction, int weaponId, int offhandId)
	{
		if (session == null || transaction == null || session.equipmentTransaction != transaction
			|| session.request.getRequestId() != requestId || session.generation != generation
			|| transaction.restore(weaponId, offhandId, java.util.Set.of(), 0, false)
				!= SpellEquipmentTransaction.Action.RESTORED) return false;
		session.equipmentTransaction = null;
		session.equipmentRestorationRequired = false;
		session.equipmentRestorationStartedAtMs = -1L;
		session.equipmentCommandAttempted = false;
		session.equipmentTabCommandAttempted = false;
		return true;
	}

	/** Returns null when the normal route-observation path should run instead. */
	public synchronized NavigationDecision observeEquipmentRestoration(long requestId, long generation,
		SpellEquipmentTransaction transaction, SpellEquipmentObservation equipment,
		NavigationObservation observation)
	{
		if (session == null || session.phase.isTerminal() || session.request.getCancellationToken().isCancelled()
			|| observation.getTerminalSignal() != NavigationObservation.TerminalSignal.NONE
			|| session.executionMode != NavigationExecutionMode.ENGINE_SUPPORTED
			|| !session.equipmentRestorationRequired || transaction == null
			|| transaction != session.equipmentTransaction || requestId != session.request.getRequestId()
			|| generation != session.generation) return null;
		RoutePlan observedPlan = observation.getRoutePlan();
		if (observedPlan == null || observedPlan.getRequestId() != requestId
			|| observedPlan.getGeneration() != generation) return null;
		long now = observation.getObservedAtMs();
		if (session.equipmentRestorationStartedAtMs < 0) session.equipmentRestorationStartedAtMs = now;
		SpellEquipmentTransaction.Action action = equipment == null ? SpellEquipmentTransaction.Action.WAIT
			: equipment.restorationAction(transaction);
		if (action == SpellEquipmentTransaction.Action.RESTORED)
		{
			acknowledgeEquipmentRestored(requestId, generation, transaction,
				equipment.getWeaponId(), equipment.getOffhandId());
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT, "equipment-restored"), observation);
		}
		if (action == SpellEquipmentTransaction.Action.CONFLICT)
		{
			// A confirmed different loadout is no longer ours to overwrite.
			session.equipmentTransaction = null;
			session.equipmentRestorationRequired = false;
			session.transitionTo(NavigationPhase.FAILED, "equipment-ownership-lost");
			return publish(NavigationDecision.of(NavigationDecision.Type.FAIL, "equipment-ownership-lost"), observation);
		}
		if (now - session.equipmentRestorationStartedAtMs >= 5_000L)
		{
			session.transitionTo(NavigationPhase.FAILED, "equipment-restoration-timeout");
			return publish(NavigationDecision.of(NavigationDecision.Type.FAIL, "equipment-restoration-timeout"), observation);
		}
		WorldPoint player = observation.getPlayerLocation();
		boolean openingTab = action == SpellEquipmentTransaction.Action.OPEN_INVENTORY
			|| action == SpellEquipmentTransaction.Action.OPEN_EQUIPMENT;
		if (session.equipmentCommandAttempted || player == null
			|| openingTab && session.equipmentTabCommandAttempted
			|| (action != SpellEquipmentTransaction.Action.RESTORE_WEAPON
				&& action != SpellEquipmentTransaction.Action.REMOVE_STAFF && !openingTab)) return awaitEquipmentRestoration(observation);
		int itemId = action == SpellEquipmentTransaction.Action.RESTORE_WEAPON
			|| action == SpellEquipmentTransaction.Action.OPEN_INVENTORY
			? transaction.getOriginalWeaponId() : transaction.getStaff().getItemID();
		RouteInteraction interaction = new RouteInteraction(generation, -1, player, player, player,
			RouteInteraction.Kind.SPELL_EQUIPMENT, RouteInteraction.Status.AVAILABLE, action.name(), true, itemId);
		return publish(NavigationDecision.interact(interaction, "equipment-restoration-command"), observation);
	}

	private void recordEquipmentCommand(RouteInteraction interaction)
	{
		if (!session.equipmentRestorationRequired)
		{
			if (SpellEquipmentTransaction.Action.OPEN_INVENTORY.name().equals(interaction.getAction()))
				session.staffTabAttempted = true;
			else session.staffEquipAttempted = true;
			return;
		}
		if (SpellEquipmentTransaction.Action.OPEN_INVENTORY.name().equals(interaction.getAction())
			|| SpellEquipmentTransaction.Action.OPEN_EQUIPMENT.name().equals(interaction.getAction()))
			session.equipmentTabCommandAttempted = true;
		else session.equipmentCommandAttempted = true;
	}

	/** Replaces only the current engine-owned spell command; it cannot initiate an unrelated swap. */
	public synchronized NavigationDecision prepareSpellEquipment(NavigationDecision cast,
		SpellEquipmentTransaction transaction, SpellEquipmentObservation equipment, boolean equipable,
		NavigationObservation observation)
	{
		if (session == null || session.phase.isTerminal() || session.request.getCancellationToken().isCancelled()
			|| session.equipmentRestorationRequired || session.executionMode != NavigationExecutionMode.ENGINE_SUPPORTED
			|| cast == null || cast != session.lastDecision || cast.getInteraction() == null
			|| cast.getInteraction() != session.pendingInteraction
			|| cast.getInteraction().getKind() != RouteInteraction.Kind.SIMPLE_TELEPORT
			|| cast.getInteraction().getObjectId() != net.runelite.client.plugins.microbot.shortestpath.TransportType
				.TELEPORTATION_SPELL.ordinal() || transaction == null) return cast;
		if (session.equipmentTransaction != null && session.equipmentTransaction != transaction)
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT, "spell-equipment-owner-mismatch"), observation);
		SpellEquipmentTransaction.Action action = equipment == null ? SpellEquipmentTransaction.Action.WAIT
			: equipment.preparationAction(transaction, equipable);
		if (action == SpellEquipmentTransaction.Action.READY_TO_CAST) return cast;
		if (session.equipmentTransaction == null)
		{
			retainEquipmentTransaction(session.request.getRequestId(), session.generation, transaction);
			session.staffEquipAttempted = false;
			session.staffTabAttempted = false;
			session.staffPreparationStartedAtMs = observation.getObservedAtMs();
		}
		if (action == SpellEquipmentTransaction.Action.CONFLICT
			|| observation.getObservedAtMs() - session.staffPreparationStartedAtMs >= 5_000L)
		{
			session.transitionTo(NavigationPhase.FAILED, "spell-equipment-preparation-failed");
			return publish(NavigationDecision.of(NavigationDecision.Type.FAIL, "spell-equipment-preparation-failed"), observation);
		}
		boolean open = action == SpellEquipmentTransaction.Action.OPEN_INVENTORY;
		if (session.staffEquipAttempted || open && session.staffTabAttempted
			|| (!open && action != SpellEquipmentTransaction.Action.EQUIP_STAFF))
			return publish(NavigationDecision.of(NavigationDecision.Type.WAIT, "spell-equipment-preparation-pending"), observation);
		RouteInteraction spell = cast.getInteraction();
		RouteInteraction command = new RouteInteraction(session.generation, spell.getRawEdgeIndex(),
			spell.getFrom(), spell.getTo(), spell.getObjectTile(), RouteInteraction.Kind.SPELL_EQUIPMENT,
			RouteInteraction.Status.AVAILABLE, action.name(), true, transaction.getStaff().getItemID());
		return publish(NavigationDecision.interact(command, "spell-equipment-preparation"), observation);
	}

	private NavigationDecision awaitEquipmentRestoration(NavigationObservation observation)
	{
		session.equipmentRestorationRequired = true;
		session.transitionTo(NavigationPhase.VERIFYING_INTERACTION, "equipment-restoration-required");
		return publish(NavigationDecision.of(NavigationDecision.Type.WAIT,
			"equipment-restoration-required"), observation);
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
		if (observation.getTerminalSignal() == NavigationObservation.TerminalSignal.ARRIVED
			&& session.equipmentTransaction != null) return awaitEquipmentRestoration(observation);
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
				|| edge.getKind() == RouteEdge.Kind.ITEM_TELEPORT
				|| edge.getKind() == RouteEdge.Kind.NPC_TRANSPORT
				|| edge.getKind() == RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT
				|| edge.getKind() == RouteEdge.Kind.CHARTER_SHIP
				|| edge.getKind() == RouteEdge.Kind.FAIRY_RING
				|| edge.getKind() == RouteEdge.Kind.SPIRIT_TREE
				|| edge.getKind() == RouteEdge.Kind.GNOME_GLIDER
				|| edge.getKind() == RouteEdge.Kind.QUETZAL
				|| edge.getKind() == RouteEdge.Kind.TELEPORTATION_LEVER
				|| edge.getKind() == RouteEdge.Kind.WILDERNESS_DITCH
				|| edge.getKind() == RouteEdge.Kind.JUNGLE_OBSTACLE
				|| edge.getKind() == RouteEdge.Kind.CANOE
				|| edge.getKind() == RouteEdge.Kind.MINECART
				|| edge.getKind() == RouteEdge.Kind.TELEPORTATION_PORTAL
				|| edge.getKind() == RouteEdge.Kind.MINIGAME_TELEPORT
				|| edge.getKind() == RouteEdge.Kind.MAGIC_MUSHTREE
				|| edge.getKind() == RouteEdge.Kind.HOT_AIR_BALLOON
				|| edge.getKind() == RouteEdge.Kind.ADJACENT_TRANSPORT
				|| edge.getKind() == RouteEdge.Kind.CATALOG_TRANSITION)
				&& session.rawProgressIndex <= edge.getRawIndex());
	}

	private static boolean interactionStageAdvanced(RouteInteraction previous,
		RouteInteraction observed)
	{
		boolean dialogueFrameReappeared = previous != null
			&& previous.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
			&& NpcDialogueTransportPolicy.CONTINUE_ACTION.equals(previous.getAction())
			&& previous.getAction().equalsIgnoreCase(observed.getAction())
			&& !previous.isReady() && observed.isReady();
		return previous != null && previous.getGeneration() == observed.getGeneration()
			&& previous.getRawEdgeIndex() == observed.getRawEdgeIndex()
			&& previous.getKind() == observed.getKind()
			&& previous.getStatus() == RouteInteraction.Status.AVAILABLE
			&& (observed.getStatus() == RouteInteraction.Status.AVAILABLE
				|| observed.getStatus() == RouteInteraction.Status.UNAVAILABLE)
			&& (!previous.getAction().equalsIgnoreCase(observed.getAction())
				|| dialogueFrameReappeared);
	}

	private static boolean dialogueVoyageStarted(RouteInteraction previous,
		RouteInteraction observed)
	{
		return previous != null
			&& previous.getGeneration() == observed.getGeneration()
			&& previous.getRawEdgeIndex() == observed.getRawEdgeIndex()
			&& previous.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
			&& observed.getKind() == RouteInteraction.Kind.NPC_DIALOGUE_TRANSPORT
			&& previous.getStatus() == RouteInteraction.Status.AVAILABLE
			&& observed.getStatus() == RouteInteraction.Status.AVAILABLE
			&& NpcDialogueTransportPolicy.CONTINUE_ACTION.equals(previous.getAction())
			&& previous.getAction().equalsIgnoreCase(observed.getAction())
			&& previous.isReady() && !observed.isReady();
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
			|| closestDistance(session.routePlan.getRawPath().subList(
				Math.min(session.routePlan.getRawPath().size(), Math.max(0, session.rawProgressIndex)),
				session.routePlan.getRawPath().size()), destination)
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
