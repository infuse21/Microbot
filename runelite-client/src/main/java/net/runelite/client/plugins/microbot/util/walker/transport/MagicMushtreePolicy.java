package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative ownership and stage identity for Fossil Island Magic Mushtrees. */
public final class MagicMushtreePolicy
{
	public static final String DESTINATION_ACTION_PREFIX = "magic-mushtree-destination:";
	private static final Set<Integer> OBJECT_IDS = Set.of(30920, 30924);
	private static final Set<String> DESTINATIONS = Set.of(
		"house on the hill", "verdant valley", "sticky swamp", "mushroom meadow");

	private MagicMushtreePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.MAGIC_MUSHTREE
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& !transport.getOrigin().equals(transport.getDestination())
			&& OBJECT_IDS.contains(transport.getObjectId())
			&& "use".equals(normalize(transport.getAction()))
			&& "magic mushtree".equals(normalize(transport.getName()))
			&& DESTINATIONS.contains(normalize(destinationName(transport.getDisplayInfo())))
			&& transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty();
	}

	public static String destinationAction(String destinationName)
	{
		return DESTINATION_ACTION_PREFIX + destinationName;
	}

	public static boolean isDestinationAction(String action)
	{
		return action != null && action.startsWith(DESTINATION_ACTION_PREFIX);
	}

	public static String destinationName(String displayInfo)
	{
		return stripDisplayPrefix(displayInfo);
	}

	private static String stripDisplayPrefix(String value)
	{
		return value == null ? "" : value.trim().replaceFirst("^[1-4]\\.\\s*", "").trim();
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}
}
