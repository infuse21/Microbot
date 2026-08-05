package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;

/** Conservative ownership boundary for single-command, originless item and spell teleports. */
public final class SimpleTeleportPolicy
{
	private SimpleTeleportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getOrigin() != null
			|| transport.getDestination() == null || isBlank(transport.getDisplayInfo()))
		{
			return false;
		}
		String display = normalize(transport.getDisplayInfo());
		if (display.contains(":") || display.contains("master scroll book"))
		{
			return false;
		}
		if (transport.getType() == TransportType.TELEPORTATION_SPELL)
		{
			return !display.contains("home teleport");
		}
		return transport.getType() == TransportType.TELEPORTATION_ITEM
			&& transport.getItemIdRequirements() != null
			&& !transport.getItemIdRequirements().isEmpty();
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
