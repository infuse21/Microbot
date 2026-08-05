package net.runelite.client.plugins.microbot.util.walker.navigation;

/** Explicit lifecycle of one navigation request. */
public enum NavigationPhase
{
	NEW,
	CALCULATING,
	FOLLOWING_ROUTE,
	APPROACHING_INTERACTION,
	PERFORMING_INTERACTION,
	VERIFYING_INTERACTION,
	REPLANNING,
	ARRIVED,
	UNREACHABLE,
	CANCELLED,
	FAILED;

	public boolean isTerminal()
	{
		return this == ARRIVED || this == UNREACHABLE || this == CANCELLED || this == FAILED;
	}
}
