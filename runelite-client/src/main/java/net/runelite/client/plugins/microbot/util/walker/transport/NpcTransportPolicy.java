package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative eligibility for one-click NPC, ship, and boat travel actions. */
public final class NpcTransportPolicy
{
	/**
	 * Boat objects anchor at their south-west corner, which can sit several tiles from
	 * the catalog origin tile on the dock.
	 */
	public static final int LIVE_OBJECT_ORIGIN_TOLERANCE = 5;

	private static final Set<TransportType> TYPES = Set.of(
		TransportType.NPC, TransportType.SHIP, TransportType.BOAT);

	private NpcTransportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null
			|| transport.getDestination() == null || transport.getObjectId() <= 0
			|| isBlank(transport.getAction()) || isBlank(transport.getName())
			|| !TYPES.contains(transport.getType())
			|| transport.getCurrencyAmount() > 0
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		// Talk-to and destination-labelled rows require a dialogue/menu sequence. Keep
		// those legacy-owned until dialogue widgets are represented as engine state.
		return !normalize(transport.getAction()).equals("talk-to")
			&& isBlank(transport.getDisplayInfo());
	}

	/**
	 * Some eligible rows publish a tile-object id in the NPC column (rowboats, ferries,
	 * moored boats). A live object may stand in for the catalog row only with the exact
	 * catalog id, or a transformed id with the exact catalog name, and in both cases the
	 * catalog action and catalog-origin proximity.
	 */
	public static boolean isLiveObjectMatch(Transport transport, int liveId, String liveName,
		String[] liveActions, WorldPoint liveTile)
	{
		if (!isEligible(transport) || liveTile == null
			|| liveTile.getPlane() != transport.getOrigin().getPlane()
			|| liveTile.distanceTo2D(transport.getOrigin()) > LIVE_OBJECT_ORIGIN_TOLERANCE
			|| matchAction(liveActions, transport.getAction()) == null)
		{
			return false;
		}
		return liveId == transport.getObjectId()
			|| (liveName != null && liveName.equalsIgnoreCase(transport.getName()));
	}

	/**
	 * Returns the live action string whose hyphen/space-insensitive form equals the
	 * catalog action, or null. Catalog rows and live compositions disagree on separator
	 * formatting (for example {@code Climb-up} versus {@code Climb up}).
	 */
	public static String matchAction(String[] liveActions, String catalogAction)
	{
		if (liveActions == null || isBlank(catalogAction))
		{
			return null;
		}
		String expected = normalizeAction(catalogAction);
		for (String action : liveActions)
		{
			if (action != null && normalizeAction(action).equals(expected))
			{
				return action;
			}
		}
		return null;
	}

	private static String normalizeAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
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
