package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative eligibility policy for short object-backed same-plane transports. */
public final class AdjacentTransportPolicy
{
	private static final int ADJACENT_DISTANCE = 1;
	private static final int SHORT_PORTAL_DISTANCE = 2;
	private static final String STRONGHOLD_TREE_DOOR = "tree door";
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
		if (!DIRECT_ACTIONS.contains(action))
		{
			return false;
		}
		int distance = transport.getOrigin().distanceTo2D(transport.getDestination());
		return distance <= ADJACENT_DISTANCE || isStrongholdTreeDoor(transport, action, distance);
	}

	private static boolean isStrongholdTreeDoor(Transport transport, String action, int distance)
	{
		return distance <= SHORT_PORTAL_DISTANCE
			&& transport.getType() == TransportType.TRANSPORT
			&& action.equals("open")
			&& STRONGHOLD_TREE_DOOR.equals(normalize(transport.getName()));
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

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
