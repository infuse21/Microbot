package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Arrays;
import java.util.Locale;

/** Conservative ownership and live-NPC identity for directed quetzal routes. */
public final class QuetzalPolicy
{
	public static final String NPC_NAME = "Renu";
	public static final String NPC_ACTION = "Travel";
	public static final String DESTINATION_ACTION_PREFIX = "quetzal-destination:";
	private static final int NPC_ORIGIN_TOLERANCE = 8;

	private QuetzalPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.QUETZAL
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& !transport.getOrigin().equals(transport.getDestination())
			&& !destinationName(transport).isEmpty();
	}

	public static String destinationAction(String destinationName)
	{
		return DESTINATION_ACTION_PREFIX + destinationName;
	}

	public static boolean isDestinationAction(String action)
	{
		return action != null && action.startsWith(DESTINATION_ACTION_PREFIX);
	}

	public static String destinationName(Transport transport)
	{
		return transport == null || transport.getDisplayInfo() == null
			? "" : transport.getDisplayInfo().trim();
	}

	static boolean isLiveNpcMatch(Transport transport, String liveName,
		String[] liveActions, WorldPoint liveTile)
	{
		return isEligible(transport)
			&& liveTile != null
			&& liveTile.getPlane() == transport.getOrigin().getPlane()
			&& liveTile.distanceTo2D(transport.getOrigin()) <= NPC_ORIGIN_TOLERANCE
			&& normalize(liveName).equals(normalize(NPC_NAME))
			&& liveActions != null && Arrays.stream(liveActions)
				.filter(java.util.Objects::nonNull)
				.anyMatch(action -> action.equalsIgnoreCase(NPC_ACTION));
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}
}
