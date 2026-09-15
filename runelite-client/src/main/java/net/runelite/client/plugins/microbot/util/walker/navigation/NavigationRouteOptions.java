package net.runelite.client.plugins.microbot.util.walker.navigation;

/** Immutable route-option subset captured when a navigation request begins. */
public final class NavigationRouteOptions
{
	private final boolean teleportsAllowed;
	private final boolean agilityShortcutsAllowed;
	private final boolean bankedTransportsAllowed;
	private final int recalculateDistance;

	public NavigationRouteOptions(boolean teleportsAllowed, boolean agilityShortcutsAllowed,
		boolean bankedTransportsAllowed)
	{
		this(teleportsAllowed, agilityShortcutsAllowed, bankedTransportsAllowed, 10);
	}

	public NavigationRouteOptions(boolean teleportsAllowed, boolean agilityShortcutsAllowed,
		boolean bankedTransportsAllowed, int recalculateDistance)
	{
		this.teleportsAllowed = teleportsAllowed;
		this.agilityShortcutsAllowed = agilityShortcutsAllowed;
		this.bankedTransportsAllowed = bankedTransportsAllowed;
		this.recalculateDistance = recalculateDistance;
	}

	public static NavigationRouteOptions defaults()
	{
		return new NavigationRouteOptions(true, true, false);
	}

	public boolean isTeleportsAllowed()
	{
		return teleportsAllowed;
	}

	public boolean isAgilityShortcutsAllowed()
	{
		return agilityShortcutsAllowed;
	}

	public boolean isBankedTransportsAllowed()
	{
		return bankedTransportsAllowed;
	}

	public int getRecalculateDistance()
	{
		return recalculateDistance;
	}
}
