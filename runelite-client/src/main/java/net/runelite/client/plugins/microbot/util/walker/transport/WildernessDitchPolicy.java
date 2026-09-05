package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.List;
import java.util.Locale;

/** Exact catalog and live-object boundary for wilderness ditch crossings. */
public final class WildernessDitchPolicy
{
	public static final String WARNING_ACTION = "wilderness-ditch-confirm";
	private static final int OBJECT_ID = 23271;
	private static final int CROSSING_DISTANCE = 3;
	private static final int LIVE_OBJECT_RADIUS = 2;

	private WildernessDitchPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& transport.getObjectId() == OBJECT_ID
			&& "cross".equals(normalize(transport.getAction()))
			&& "wilderness ditch".equals(normalize(transport.getName()))
			&& transport.getOrigin().getPlane() == transport.getDestination().getPlane()
			&& transport.getOrigin().distanceTo2D(transport.getDestination())
				== CROSSING_DISTANCE
			&& transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty();
	}

	public static boolean isLiveObjectMatch(Transport transport, int objectId,
		String name, List<String> actions, WorldPoint tile)
	{
		if (!isEligible(transport) || objectId != OBJECT_ID || tile == null
			|| tile.getPlane() != transport.getOrigin().getPlane()
			|| tile.distanceTo2D(transport.getOrigin()) > LIVE_OBJECT_RADIUS
			|| !normalize(transport.getName()).equals(normalize(name)) || actions == null)
		{
			return false;
		}
		String expected = normalize(transport.getAction());
		return actions.stream().anyMatch(action -> expected.equals(normalize(action)));
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
