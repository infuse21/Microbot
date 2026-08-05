package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

/** The only Phase 3 boundary permitted to send movement input. */
public interface WalkerActions
{
	/** @return true only when a minimap or canvas walk command was actually issued. */
	boolean clickTile(WorldPoint target);

	/** Selection-aware movement hook; adapters may specialize short interaction crossings. */
	default boolean clickTile(WorldPoint target, String selection)
	{
		return clickTile(target);
	}

	/** @return true only when the route interaction was actually issued. */
	default boolean interact(RouteInteraction interaction)
	{
		return false;
	}

	/** Diagnostic label for the most recent click attempt. Lambdas retain a neutral default. */
	default String getLastActionType()
	{
		return "adapter";
	}
}
