package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Arrays;
import java.util.Locale;

/** Conservative ownership and stage identity for directed gnome-glider routes. */
public final class GnomeGliderPolicy
{
	public static final String DESTINATION_ACTION_PREFIX = "gnome-glider-destination:";

	private GnomeGliderPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.GNOME_GLIDER
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& !transport.getOrigin().equals(transport.getDestination())
			&& transport.getObjectId() > 0
			&& "glider".equals(normalize(transport.getAction()))
			&& !normalize(transport.getName()).isEmpty()
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

	static boolean isDestinationSelectable(boolean hidden)
	{
		return !hidden;
	}

	static boolean isLiveNpcMatch(Transport transport, int liveId, String liveName,
		String[] liveActions, WorldPoint liveTile)
	{
		return isEligible(transport)
			&& liveTile != null
			&& liveTile.getPlane() == transport.getOrigin().getPlane()
			&& liveTile.distanceTo2D(transport.getOrigin()) <= 6
			&& normalize(liveName).equals(normalize(transport.getName()))
			&& liveActions != null && Arrays.stream(liveActions)
				.filter(java.util.Objects::nonNull)
				.anyMatch(action -> action.equalsIgnoreCase(transport.getAction()))
			&& liveId > 0;
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}
}
