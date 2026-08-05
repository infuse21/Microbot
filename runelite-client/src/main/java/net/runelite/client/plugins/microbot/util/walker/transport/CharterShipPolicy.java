package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Collection;
import java.util.Locale;

/** Conservative eligibility and stage identity for paid charter-ship routes. */
public final class CharterShipPolicy
{
	private static final int LIVE_NPC_ORIGIN_TOLERANCE = 3;
	public static final String DESTINATION_ACTION_PREFIX = "charter-destination:";
	public static final String CONFIRM_ACTION = "charter-confirm";

	private CharterShipPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.CHARTER_SHIP
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& transport.getObjectId() > 0
			&& !isBlank(transport.getAction())
			&& !isBlank(transport.getName())
			&& !isBlank(transport.getDisplayInfo())
			&& transport.getCurrencyAmount() > 0
			&& normalize(transport.getCurrencyName()).equals("coins")
			&& transport.getItemIdRequirements().isEmpty();
	}

	public static String destinationAction(String destination)
	{
		return DESTINATION_ACTION_PREFIX + destination;
	}

	public static boolean isDestinationAction(String action)
	{
		return action != null && action.startsWith(DESTINATION_ACTION_PREFIX);
	}

	public static boolean isLiveNpcMatch(Transport transport, String liveName,
		Collection<String> liveActions, WorldPoint liveTile)
	{
		return isEligible(transport) && liveName != null
			&& liveName.equalsIgnoreCase(transport.getName())
			&& liveActions != null && liveActions.stream()
				.anyMatch(action -> action != null
					&& action.equalsIgnoreCase(transport.getAction()))
			&& liveTile != null
			&& liveTile.getPlane() == transport.getOrigin().getPlane()
			&& liveTile.distanceTo2D(transport.getOrigin()) <= LIVE_NPC_ORIGIN_TOLERANCE;
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
