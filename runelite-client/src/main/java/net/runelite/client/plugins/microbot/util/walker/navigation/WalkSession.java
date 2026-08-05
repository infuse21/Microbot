package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mutable state owned exclusively by one {@link NavigationEngine}. */
final class WalkSession
{
	final NavigationRequest request;
	NavigationPhase phase = NavigationPhase.NEW;
	RoutePlan routePlan;
	long generation;
	int rawProgressIndex = -1;
	int smoothedProgressIndex = -1;
	WorldPoint lastObservedPlayer;
	long lastObservedAtMs;
	NavigationDecision lastDecision = NavigationDecision.of(
		NavigationDecision.Type.NO_ACTION, "session-created");
	long lastCommandAtMs;
	final Map<RecoveryCause, Integer> recoveryAttempts = new EnumMap<>(RecoveryCause.class);
	final Set<Integer> blockedEdgesReplanned = new HashSet<>();
	String terminalReason = "";
	String transitionReason = "session-created";
	NavigationComparison comparison = NavigationComparison.NOT_OBSERVED;
	NavigationExecutionMode executionMode = NavigationExecutionMode.SHADOW;
	boolean executionModeSelected;
	boolean commandPending;
	boolean commandRejected;
	WorldPoint commandOrigin;
	WorldPoint lastObservedDestination;
	WorldPoint commandDestinationAtIssue;
	WorldPoint commandTarget;
	int commandRawIndex = -1;
	int commandHandoffDistance = -1;
	int commandOriginRawIndex = -1;
	long lastProgressAtMs;
	int lastProgressRawIndex = -1;
	int routeDistance = Integer.MAX_VALUE;
	RouteInteraction pendingInteraction;
	final List<RouteInteraction> clearedInteractionsAwaitingCrossing = new ArrayList<>();
	boolean interactionCommandPending;
	long interactionCommandDeadlineMs;
	boolean interactionClearedObserved;

	WalkSession(NavigationRequest request)
	{
		this.request = request;
	}

	void transitionTo(NavigationPhase next, String reason)
	{
		if (phase.isTerminal())
		{
			return;
		}
		phase = next;
		transitionReason = reason;
		if (next.isTerminal())
		{
			terminalReason = reason;
		}
	}

	void install(RoutePlan plan)
	{
		routePlan = plan;
		generation = plan.getGeneration();
		rawProgressIndex = -1;
		smoothedProgressIndex = -1;
		commandPending = false;
		commandRejected = false;
		commandOrigin = null;
		lastObservedDestination = null;
		commandDestinationAtIssue = null;
		commandTarget = null;
		commandRawIndex = -1;
		commandHandoffDistance = -1;
		commandOriginRawIndex = -1;
		lastProgressAtMs = 0L;
		lastProgressRawIndex = -1;
		blockedEdgesReplanned.clear();
		pendingInteraction = null;
		clearedInteractionsAwaitingCrossing.clear();
		interactionCommandPending = false;
		interactionCommandDeadlineMs = 0L;
		interactionClearedObserved = false;
		routeDistance = Integer.MAX_VALUE;
		if (!executionModeSelected)
		{
			executionMode = request.getRouteOptions().isOrdinaryEngineEnabled()
				? (plan.isEngineSupported()
					? NavigationExecutionMode.ENGINE_SUPPORTED
					: NavigationExecutionMode.LEGACY_LOCKED)
				: NavigationExecutionMode.SHADOW;
			executionModeSelected = true;
		}
	}

	int incrementRecovery(RecoveryCause cause)
	{
		int attempts = recoveryAttempts.getOrDefault(cause, 0) + 1;
		recoveryAttempts.put(cause, attempts);
		return attempts;
	}

	int recoveryAttempts(RecoveryCause cause)
	{
		return recoveryAttempts.getOrDefault(cause, 0);
	}

	int totalRecoveryAttempts()
	{
		int total = 0;
		for (int attempts : recoveryAttempts.values())
		{
			total += attempts;
		}
		return total;
	}
}
