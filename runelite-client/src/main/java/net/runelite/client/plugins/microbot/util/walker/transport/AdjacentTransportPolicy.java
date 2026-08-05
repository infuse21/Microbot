package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative eligibility policy for the first engine-owned transport family. */
public final class AdjacentTransportPolicy
{
	private static final Set<String> DIRECT_ACTIONS = Set.of(
		"open", "pass", "walk-through", "go-through", "climb-over", "climb-through",
		"squeeze-through", "cross", "vault");
	private AdjacentTransportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() <= 0 || isBlank(transport.getAction())
			|| transport.getOrigin().getPlane() != transport.getDestination().getPlane()
			|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 1
			|| transport.getCurrencyAmount() > 0)
		{
			return false;
		}
		TransportType type = transport.getType();
		if (type != TransportType.TRANSPORT && type != TransportType.AGILITY_SHORTCUT
			&& type != TransportType.GRAPPLE_SHORTCUT)
		{
			return false;
		}
		String action = transport.getAction().toLowerCase(Locale.ROOT);
		return DIRECT_ACTIONS.contains(action);
	}

	public static boolean actionClearsObject(String action)
	{
		if (action == null)
		{
			return false;
		}
		String normalized = action.toLowerCase(Locale.ROOT);
		return normalized.equals("open") || normalized.equals("pass")
			|| normalized.equals("walk-through") || normalized.equals("go-through");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
