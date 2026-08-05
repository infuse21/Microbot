package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Immutable diagnostics/overlay view of the active shadow session. */
public final class NavigationSnapshot
{
	private final NavigationRequest request;
	private final NavigationPhase phase;
	private final RoutePlan routePlan;
	private final int rawProgressIndex;
	private final int smoothedProgressIndex;
	private final WorldPoint playerLocation;
	private final NavigationDecision decision;
	private final int recoveryAttempts;
	private final Map<RecoveryCause, Integer> recoveryAttemptsByCause;
	private final String transitionReason;
	private final String terminalReason;
	private final NavigationComparison comparison;
	private final NavigationExecutionMode executionMode;
	private final int routeDistance;
	private final WorldPoint commandTarget;
	private final int commandRawIndex;
	private final int commandHandoffDistance;
	private final RouteInteraction pendingInteraction;
	private final Set<Integer> interactionEdgesAwaitingResolution;

	NavigationSnapshot(WalkSession session)
	{
		request = session.request;
		phase = session.phase;
		routePlan = session.routePlan;
		rawProgressIndex = session.rawProgressIndex;
		smoothedProgressIndex = session.smoothedProgressIndex;
		playerLocation = session.lastObservedPlayer;
		decision = session.lastDecision;
		recoveryAttempts = session.totalRecoveryAttempts();
		EnumMap<RecoveryCause, Integer> attempts = new EnumMap<>(RecoveryCause.class);
		attempts.putAll(session.recoveryAttempts);
		recoveryAttemptsByCause = Collections.unmodifiableMap(attempts);
		transitionReason = session.transitionReason;
		terminalReason = session.terminalReason;
		comparison = session.comparison;
		executionMode = session.executionMode;
		routeDistance = session.routeDistance;
		commandTarget = session.commandTarget;
		commandRawIndex = session.commandRawIndex;
		commandHandoffDistance = session.commandHandoffDistance;
		pendingInteraction = session.pendingInteraction;
		Set<Integer> interactionEdges = new HashSet<>();
		if (pendingInteraction != null)
		{
			interactionEdges.add(pendingInteraction.getRawEdgeIndex());
		}
		for (RouteInteraction interaction : session.clearedInteractionsAwaitingCrossing)
		{
			interactionEdges.add(interaction.getRawEdgeIndex());
		}
		interactionEdgesAwaitingResolution = Collections.unmodifiableSet(interactionEdges);
	}

	public NavigationRequest getRequest() { return request; }
	public long getRequestId() { return request.getRequestId(); }
	public NavigationPhase getPhase() { return phase; }
	public RoutePlan getRoutePlan() { return routePlan; }
	public long getGeneration() { return routePlan == null ? 0 : routePlan.getGeneration(); }
	public int getRawProgressIndex() { return rawProgressIndex; }
	public int getSmoothedProgressIndex() { return smoothedProgressIndex; }
	public WorldPoint getPlayerLocation() { return playerLocation; }
	public NavigationDecision getDecision() { return decision; }
	public int getRecoveryAttempts() { return recoveryAttempts; }
	public int getRecoveryAttempts(RecoveryCause cause)
	{
		return recoveryAttemptsByCause.getOrDefault(cause, 0);
	}
	public String getTransitionReason() { return transitionReason; }
	public String getTerminalReason() { return terminalReason; }
	public NavigationComparison getComparison() { return comparison; }
	public NavigationExecutionMode getExecutionMode() { return executionMode; }
	public int getRouteDistance() { return routeDistance; }
	public WorldPoint getCommandTarget() { return commandTarget; }
	public int getCommandRawIndex() { return commandRawIndex; }
	public int getCommandHandoffDistance() { return commandHandoffDistance; }
	public RouteInteraction getPendingInteraction() { return pendingInteraction; }
	public boolean isInteractionCollisionProtected(int rawEdgeIndex)
	{
		for (int interactionEdge : interactionEdgesAwaitingResolution)
		{
			if (Math.abs(interactionEdge - rawEdgeIndex) <= 1)
			{
				return true;
			}
		}
		return false;
	}
	public boolean isTerminal() { return phase.isTerminal(); }
}
