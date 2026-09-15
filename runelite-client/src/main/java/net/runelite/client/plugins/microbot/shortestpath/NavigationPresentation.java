package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationSnapshot;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;

/** Selects immutable engine state which is safe for overlays to render. */
final class NavigationPresentation
{
	private NavigationPresentation()
	{
	}

	static RoutePlan activePlan(NavigationSnapshot snapshot, RoutePlan published)
	{
		if (snapshot == null || snapshot.isTerminal())
		{
			return null;
		}
		if (snapshot.getRoutePlan() != null)
		{
			return snapshot.getRoutePlan();
		}
		return published != null && published.getRequestId() == snapshot.getRequestId()
			? published
			: null;
	}

	static WorldPoint target(NavigationSnapshot snapshot)
	{
		if (snapshot == null || snapshot.isTerminal()
			|| snapshot.getRequest().getDestinations().size() != 1)
		{
			return null;
		}
		return snapshot.getRequest().getDestinations().iterator().next();
	}

	static boolean invalidatesSession(GameState gameState)
	{
		return gameState == GameState.LOGIN_SCREEN
			|| gameState == GameState.HOPPING
			|| gameState == GameState.CONNECTION_LOST;
	}
}
