package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative eligibility for direct object-backed level, cave, and gangplank transitions. */
public final class CatalogTransitionPolicy
{
	private static final Set<String> DIRECT_ACTIONS = Set.of(
		"climb-up", "climb-down", "climb", "climb up", "climb down",
		"walk-up", "walk-down", "ascend", "descend", "top-floor", "bottom-floor",
		"enter", "exit", "leave", "crawl", "climb-into", "cross");

	private CatalogTransitionPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null
			|| transport.getDestination() == null || transport.getObjectId() <= 0
			|| isBlank(transport.getAction()) || isBlank(transport.getName())
			|| transport.getType() != TransportType.TRANSPORT
			|| transport.getCurrencyAmount() > 0
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		boolean changesScene = transport.getOrigin().getPlane() != transport.getDestination().getPlane()
			|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 1;
		if (!changesScene || !DIRECT_ACTIONS.contains(normalize(transport.getAction())))
		{
			return false;
		}
		String name = normalize(transport.getName());
		return name.contains("ladder") || name.contains("stair")
			|| name.contains("trapdoor") || name.contains("cave")
			|| name.contains("gangplank");
	}

	public static boolean supportsClosedVariant(String action)
	{
		String normalized = normalize(action);
		return normalized.equals("climb-down") || normalized.equals("climb down");
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
