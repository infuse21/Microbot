package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

/** Immutable game/legacy state sampled for one shadow-engine pass. */
public final class NavigationObservation
{
	public enum TerminalSignal
	{
		NONE,
		ARRIVED,
		UNREACHABLE,
		CANCELLED,
		FAILED
	}

	private final long observedAtMs;
	private final WorldPoint playerLocation;
	private final RoutePlan routePlan;
	private final boolean moving;
	private final boolean animating;
	private final boolean interacting;
	private final boolean interactionFrontier;
	private final boolean interactionCommandInFlight;
	private final boolean replanRequested;
	private final TerminalSignal terminalSignal;
	private final NavigationDecision.Type legacyDecisionType;
	private final String legacyReason;
	private final RecoveryCause recoveryCause;
	private final int blockedEdgeIndex;
	private final WorldPoint movementDestination;
	private final RouteInteraction routeInteraction;
	private final RouteInteraction nextRouteInteraction;

	public NavigationObservation(long observedAtMs, WorldPoint playerLocation, RoutePlan routePlan,
		boolean moving, boolean animating, boolean interacting, boolean interactionFrontier,
		boolean interactionCommandInFlight, boolean replanRequested, TerminalSignal terminalSignal,
		NavigationDecision.Type legacyDecisionType, String legacyReason)
	{
		this(observedAtMs, playerLocation, routePlan, moving, animating, interacting,
			interactionFrontier, interactionCommandInFlight, replanRequested, terminalSignal,
			legacyDecisionType, legacyReason, RecoveryCause.NONE, -1, null, null, null);
	}

	private NavigationObservation(long observedAtMs, WorldPoint playerLocation, RoutePlan routePlan,
		boolean moving, boolean animating, boolean interacting, boolean interactionFrontier,
		boolean interactionCommandInFlight, boolean replanRequested, TerminalSignal terminalSignal,
		NavigationDecision.Type legacyDecisionType, String legacyReason, RecoveryCause recoveryCause,
		int blockedEdgeIndex, WorldPoint movementDestination, RouteInteraction routeInteraction,
		RouteInteraction nextRouteInteraction)
	{
		this.observedAtMs = observedAtMs;
		this.playerLocation = playerLocation;
		this.routePlan = routePlan;
		this.moving = moving;
		this.animating = animating;
		this.interacting = interacting;
		this.interactionFrontier = interactionFrontier;
		this.interactionCommandInFlight = interactionCommandInFlight;
		this.replanRequested = replanRequested;
		this.terminalSignal = terminalSignal == null ? TerminalSignal.NONE : terminalSignal;
		this.legacyDecisionType = legacyDecisionType;
		this.legacyReason = legacyReason == null ? "" : legacyReason;
		this.recoveryCause = recoveryCause == null ? RecoveryCause.NONE : recoveryCause;
		this.blockedEdgeIndex = blockedEdgeIndex;
		this.movementDestination = movementDestination;
		this.routeInteraction = routeInteraction;
		this.nextRouteInteraction = nextRouteInteraction;
	}

	public static NavigationObservation route(long observedAtMs, WorldPoint playerLocation,
		RoutePlan routePlan, boolean moving, boolean animating, boolean interacting,
		boolean interactionFrontier, boolean interactionCommandInFlight, boolean replanRequested,
		NavigationDecision.Type legacyDecisionType, String legacyReason)
	{
		return new NavigationObservation(observedAtMs, playerLocation, routePlan, moving, animating,
			interacting, interactionFrontier, interactionCommandInFlight, replanRequested,
			TerminalSignal.NONE, legacyDecisionType, legacyReason);
	}

	public static NavigationObservation terminal(TerminalSignal signal, String reason)
	{
		return new NavigationObservation(System.currentTimeMillis(), null, null, false, false,
			false, false, false, false, signal, null, reason);
	}

	public NavigationObservation withRecovery(RecoveryCause cause, int edgeIndex)
	{
		return new NavigationObservation(observedAtMs, playerLocation, routePlan, moving, animating,
			interacting, interactionFrontier, interactionCommandInFlight, replanRequested,
			terminalSignal, legacyDecisionType, legacyReason, cause, edgeIndex,
			movementDestination, routeInteraction, nextRouteInteraction);
	}

	public NavigationObservation withMovementDestination(WorldPoint destination)
	{
		return new NavigationObservation(observedAtMs, playerLocation, routePlan, moving, animating,
			interacting, interactionFrontier, interactionCommandInFlight, replanRequested,
			terminalSignal, legacyDecisionType, legacyReason, recoveryCause, blockedEdgeIndex,
			destination, routeInteraction, nextRouteInteraction);
	}

	public NavigationObservation withRouteInteraction(RouteInteraction interaction)
	{
		return withRouteInteractions(interaction, null);
	}

	public NavigationObservation withRouteInteractions(RouteInteraction interaction,
		RouteInteraction nextInteraction)
	{
		return new NavigationObservation(observedAtMs, playerLocation, routePlan, moving, animating,
			interacting, interaction != null || interactionFrontier, interactionCommandInFlight,
			replanRequested, terminalSignal, legacyDecisionType, legacyReason, recoveryCause,
			blockedEdgeIndex, movementDestination, interaction, nextInteraction);
	}

	public long getObservedAtMs() { return observedAtMs; }
	public WorldPoint getPlayerLocation() { return playerLocation; }
	public RoutePlan getRoutePlan() { return routePlan; }
	public boolean isMoving() { return moving; }
	public boolean isAnimating() { return animating; }
	public boolean isInteracting() { return interacting; }
	public boolean isInteractionFrontier() { return interactionFrontier; }
	public boolean isInteractionCommandInFlight() { return interactionCommandInFlight; }
	public boolean isReplanRequested() { return replanRequested; }
	public TerminalSignal getTerminalSignal() { return terminalSignal; }
	public NavigationDecision.Type getLegacyDecisionType() { return legacyDecisionType; }
	public String getLegacyReason() { return legacyReason; }
	public RecoveryCause getRecoveryCause() { return recoveryCause; }
	public int getBlockedEdgeIndex() { return blockedEdgeIndex; }
	public WorldPoint getMovementDestination() { return movementDestination; }
	public RouteInteraction getRouteInteraction() { return routeInteraction; }
	public RouteInteraction getNextRouteInteraction() { return nextRouteInteraction; }
}
