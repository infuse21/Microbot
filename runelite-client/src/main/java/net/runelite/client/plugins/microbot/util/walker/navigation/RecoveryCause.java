package net.runelite.client.plugins.microbot.util.walker.navigation;

/** Distinct evidence that can cause a bounded navigation recovery action. */
public enum RecoveryCause
{
	NONE,
	NO_ACKNOWLEDGEMENT,
	COMMAND_DESTINATION_MISMATCH,
	NO_TILE_PROGRESS,
	OFF_ROUTE,
	BLOCKED_EDGE,
	INTERACTION_WAIT,
	INTERACTION_UNAVAILABLE,
	ROUTE_EXHAUSTED,
	EXTERNAL_REPLAN
}
