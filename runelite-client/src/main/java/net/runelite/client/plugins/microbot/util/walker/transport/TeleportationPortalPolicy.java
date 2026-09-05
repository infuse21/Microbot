package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/** Conservative identity contract for deterministic direct-action teleportation portals. */
public final class TeleportationPortalPolicy
{
	public static final int LIVE_OBJECT_ORIGIN_TOLERANCE = 4;

	private static final Set<String> SUPPORTED_SHAPES = Set.of(
		shape(30386, "Enter", "Castle Wars portal", ""),
		shape(4387, "Enter", "Saradomin Portal", ""),
		shape(4388, "Enter", "Zamorak Portal", ""),
		shape(4389, "Exit", "Portal", ""),
		shape(4390, "Exit", "Portal", ""),
		shape(26645, "Enter", "Free-for-all portal", ""),
		shape(2156, "Enter", "Magic Portal", ""),
		shape(2157, "Enter", "Magic Portal", ""),
		shape(2158, "Enter", "Magic Portal", ""),
		shape(40476, "Ferox Enclave", "Portal", "Soul Wars: Ferox Enclave"),
		shape(40476, "Edgeville", "Portal", "Soul Wars: Edgeville"),
		shape(6282, "Enter", "Portal", ""),
		shape(11356, "Enter", "Portal Home", ""),
		shape(40474, "Enter", "Soul Wars Portal", "Soul Wars"),
		shape(40475, "Enter", "Soul Wars Portal", "Soul Wars"),
		shape(26646, "Exit", "Portal", ""),
		shape(27094, "Use", "Clan Cup portal", ""),
		shape(27095, "Use", "Clan Cup portal", ""),
		shape(19005, "Use", "Portal", ""),
		shape(20786, "Use", "Portal", ""),
		shape(23707, "Use", "Portal", ""),
		shape(23922, "Use", "Portal", ""),
		shape(6550, "Use", "Portal", ""),
		shape(41724, "Enter-member", "Clan hall portal", ""),
		shape(41617, "Leave", "Portal", ""));

	private TeleportationPortalPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.TELEPORTATION_PORTAL
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& !transport.getOrigin().equals(transport.getDestination())
			&& transport.getObjectId() > 0
			&& transport.getCurrencyAmount() == 0
			&& normalize(transport.getCurrencyName()).isEmpty()
			&& transport.getItemIdRequirements().isEmpty()
			&& SUPPORTED_SHAPES.contains(shape(transport.getObjectId(),
				transport.getAction(), transport.getName(), transport.getDisplayInfo()));
	}

	public static boolean isLiveObjectMatch(Transport transport, int liveId,
		String liveName, Collection<String> liveActions, WorldPoint liveTile)
	{
		return isEligible(transport) && liveId == transport.getObjectId()
			&& normalize(liveName).equals(normalize(transport.getName()))
			&& exactAction(liveActions, transport.getAction()) != null
			&& liveTile != null
			&& liveTile.getPlane() == transport.getOrigin().getPlane()
			&& liveTile.distanceTo2D(transport.getOrigin())
				<= LIVE_OBJECT_ORIGIN_TOLERANCE;
	}

	public static String exactAction(Collection<String> actions, String expected)
	{
		if (actions == null || expected == null)
		{
			return null;
		}
		return actions.stream().filter(java.util.Objects::nonNull)
			.filter(action -> action.equalsIgnoreCase(expected)).findFirst().orElse(null);
	}

	private static String shape(int objectId, String action, String name, String displayInfo)
	{
		return objectId + "\u0000" + normalize(action) + "\u0000" + normalize(name)
			+ "\u0000" + normalize(displayInfo);
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
