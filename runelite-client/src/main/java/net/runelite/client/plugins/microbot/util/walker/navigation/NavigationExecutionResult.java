package net.runelite.client.plugins.microbot.util.walker.navigation;

/** Result of one engine execution pass. */
public final class NavigationExecutionResult
{
	private final NavigationDecision decision;
	private final boolean engineOwned;
	private final boolean commandIssued;
	private final String actionType;

	NavigationExecutionResult(NavigationDecision decision, boolean engineOwned, boolean commandIssued,
		String actionType)
	{
		this.decision = decision;
		this.engineOwned = engineOwned;
		this.commandIssued = commandIssued;
		this.actionType = actionType;
	}

	public NavigationDecision getDecision()
	{
		return decision;
	}

	public boolean isEngineOwned()
	{
		return engineOwned;
	}

	public boolean isCommandIssued()
	{
		return commandIssued;
	}

	public String getActionType()
	{
		return actionType;
	}
}
