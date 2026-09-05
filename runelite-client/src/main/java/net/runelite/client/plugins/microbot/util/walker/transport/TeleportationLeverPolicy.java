package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.List;
import java.util.Locale;

/** Conservative catalog and live-object identity for teleportation levers. */
public final class TeleportationLeverPolicy
{
	public static final String WARNING_TEXT =
		"Warning! The lever will teleport you deep into the Wilderness.";
	public static final String CONFIRM_QUESTION = "Are you sure you wish to pull it?";
	public static final String CONFIRM_OPTION = "Yes, I'm brave.";
	public static final String WARNING_CONTINUE_ACTION = "lever-warning-continue";
	public static final String CONFIRM_ACTION = "lever-warning-confirm";
	private static final int LIVE_OBJECT_RADIUS = 2;

	private TeleportationLeverPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.TELEPORTATION_LEVER
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& transport.getObjectId() > 0
			&& "pull".equals(normalize(transport.getAction()))
			&& "lever".equals(normalize(transport.getName()))
			&& transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty();
	}

	public static boolean isLiveObjectMatch(Transport transport, int objectId,
		String name, List<String> actions, WorldPoint tile)
	{
		if (!isEligible(transport) || objectId != transport.getObjectId()
			|| tile == null || tile.getPlane() != transport.getOrigin().getPlane()
			|| tile.distanceTo2D(transport.getOrigin()) > LIVE_OBJECT_RADIUS
			|| !normalize(transport.getName()).equals(normalize(name)) || actions == null)
		{
			return false;
		}
		String expected = normalize(transport.getAction());
		return actions.stream().anyMatch(action -> expected.equals(normalize(action)));
	}

	public static boolean isStageAction(String action)
	{
		return WARNING_CONTINUE_ACTION.equals(action) || CONFIRM_ACTION.equals(action);
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
