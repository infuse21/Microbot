package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** One shadow-engine outcome. A pass can contain at most one input-capable decision. */
public final class NavigationDecision
{
	public enum Type
	{
		NO_ACTION,
		CLICK_TILE,
		INTERACT,
		WAIT,
		REQUEST_REPLAN,
		COMPLETE,
		FAIL
	}

	private final Type type;
	private final WorldPoint target;
	private final String reason;
	private final int targetRawIndex;
	private final int targetSmoothedIndex;
	private final int targetDistance;
	private final int targetReach;
	private final int targetHandoffDistance;
	private final String targetSelection;
	private final RecoveryCause recoveryCause;
	private final int recoveryAttempt;
	private final int recoveryBudget;
	private final long recoveryAgeMs;
	private final int blockedEdgeIndex;
	private final WorldPoint recoveryExpectedTarget;
	private final WorldPoint recoveryObservedDestination;
	private final RouteInteraction interaction;

	private NavigationDecision(Type type, WorldPoint target, String reason, int targetRawIndex,
		int targetSmoothedIndex, int targetDistance, int targetReach, int targetHandoffDistance,
		String targetSelection, RecoveryCause recoveryCause, int recoveryAttempt,
		int recoveryBudget, long recoveryAgeMs, int blockedEdgeIndex,
		WorldPoint recoveryExpectedTarget, WorldPoint recoveryObservedDestination,
		RouteInteraction interaction)
	{
		this.type = Objects.requireNonNull(type, "type");
		this.reason = Objects.requireNonNull(reason, "reason");
		if (type == Type.CLICK_TILE && target == null)
		{
			throw new IllegalArgumentException("CLICK_TILE requires a target");
		}
		if (type != Type.CLICK_TILE && target != null)
		{
			throw new IllegalArgumentException(type + " cannot contain a tile command");
		}
		this.target = target;
		this.targetRawIndex = targetRawIndex;
		this.targetSmoothedIndex = targetSmoothedIndex;
		this.targetDistance = targetDistance;
		this.targetReach = targetReach;
		this.targetHandoffDistance = targetHandoffDistance;
		this.targetSelection = Objects.requireNonNull(targetSelection, "targetSelection");
		this.recoveryCause = Objects.requireNonNull(recoveryCause, "recoveryCause");
		this.recoveryAttempt = recoveryAttempt;
		this.recoveryBudget = recoveryBudget;
		this.recoveryAgeMs = recoveryAgeMs;
		this.blockedEdgeIndex = blockedEdgeIndex;
		this.recoveryExpectedTarget = recoveryExpectedTarget;
		this.recoveryObservedDestination = recoveryObservedDestination;
		this.interaction = interaction;
	}

	public static NavigationDecision of(Type type, String reason)
	{
		return new NavigationDecision(type, null, reason, -1, -1, -1, -1, -1, "-",
			RecoveryCause.NONE, 0, 0, -1L, -1, null, null, null);
	}

	public static NavigationDecision click(WorldPoint target, String reason)
	{
		return new NavigationDecision(Type.CLICK_TILE, target, reason, -1, -1, -1, -1, -1, "-",
			RecoveryCause.NONE, 0, 0, -1L, -1, null, null, null);
	}

	public static NavigationDecision click(RouteClickSelection selection, int handoffDistance,
		String reason)
	{
		Objects.requireNonNull(selection, "selection");
		return new NavigationDecision(Type.CLICK_TILE, selection.getTarget(), reason,
			selection.getRawIndex(), selection.getSmoothedIndex(), selection.getDistance(),
			selection.getReach(), handoffDistance, selection.getSelection(), RecoveryCause.NONE,
			0, 0, -1L, -1, null, null, null);
	}

	public static NavigationDecision interact(RouteInteraction interaction, String reason)
	{
		return new NavigationDecision(Type.INTERACT, null, reason, -1, -1, -1, -1, -1, "-",
			RecoveryCause.NONE, 0, 0, -1L, interaction.getRawEdgeIndex(), null, null,
			Objects.requireNonNull(interaction, "interaction"));
	}

	public static NavigationDecision recovery(Type type, RecoveryCause cause, int attempt,
		int budget, long ageMs, int blockedEdgeIndex, String reason)
	{
		return new NavigationDecision(type, null, reason, -1, -1, -1, -1, -1, "-", cause,
			attempt, budget, ageMs, blockedEdgeIndex, null, null, null);
	}

	public static NavigationDecision destinationMismatch(Type type, int attempt, int budget,
		long ageMs, WorldPoint expectedTarget, WorldPoint observedDestination, String reason)
	{
		return new NavigationDecision(type, null, reason, -1, -1, -1, -1, -1, "-",
			RecoveryCause.COMMAND_DESTINATION_MISMATCH, attempt, budget, ageMs, -1,
			expectedTarget, observedDestination, null);
	}

	public static NavigationDecision recoveryClick(RouteClickSelection selection,
		int handoffDistance, RecoveryCause cause, int attempt, int budget, long ageMs,
		String reason)
	{
		Objects.requireNonNull(selection, "selection");
		return new NavigationDecision(Type.CLICK_TILE, selection.getTarget(), reason,
			selection.getRawIndex(), selection.getSmoothedIndex(), selection.getDistance(),
			selection.getReach(), handoffDistance, selection.getSelection(), cause, attempt,
			budget, ageMs, -1, null, null, null);
	}

	public Type getType()
	{
		return type;
	}

	public WorldPoint getTarget()
	{
		return target;
	}

	public String getReason()
	{
		return reason;
	}

	public int getTargetRawIndex() { return targetRawIndex; }
	public int getTargetSmoothedIndex() { return targetSmoothedIndex; }
	public int getTargetDistance() { return targetDistance; }
	public int getTargetReach() { return targetReach; }
	public int getTargetHandoffDistance() { return targetHandoffDistance; }
	public String getTargetSelection() { return targetSelection; }
	public RecoveryCause getRecoveryCause() { return recoveryCause; }
	public int getRecoveryAttempt() { return recoveryAttempt; }
	public int getRecoveryBudget() { return recoveryBudget; }
	public long getRecoveryAgeMs() { return recoveryAgeMs; }
	public int getBlockedEdgeIndex() { return blockedEdgeIndex; }
	public WorldPoint getRecoveryExpectedTarget() { return recoveryExpectedTarget; }
	public WorldPoint getRecoveryObservedDestination() { return recoveryObservedDestination; }
	public RouteInteraction getInteraction() { return interaction; }

	public boolean issuesInput()
	{
		return type == Type.CLICK_TILE || type == Type.INTERACT;
	}
}
