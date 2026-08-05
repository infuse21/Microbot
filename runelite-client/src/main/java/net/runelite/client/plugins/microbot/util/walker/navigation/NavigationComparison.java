package net.runelite.client.plugins.microbot.util.walker.navigation;

/** Classification of a shadow decision against the legacy action sampled in the same pass. */
public enum NavigationComparison
{
	NOT_OBSERVED,
	MATCH,
	SHADOW_ONLY,
	LEGACY_ONLY,
	DIVERGED
}
