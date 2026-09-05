package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Collection;
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
	private static final int LIVE_NPC_ORIGIN_RADIUS = 15;
	private static final Set<String> ORDINARY_DIRECT_ROUTES = Set.of(
		"3680,2963,0->3786,2824,0|550|Transport|Brother Tranquility||2",
		"3786,2824,0->3680,2963,0|550|Transport|Brother Tranquility||2",
		"2484,3486,1->2649,4516,0|1445|Travel|Daero||6",
		"2649,4518,0->2894,2726,0|1446|Travel|Waydar||4",
		"2896,2727,0->2393,3465,0|1446|Travel|Waydar||4",
		"3280,3412,0->1700,3141,0|12888|Travel|Primio|Civitas illa Fortis|6",
		"1703,3140,0->3280,3412,0|12889|Travel|Primio|Varrock|6");

	private NpcTransportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (isOrdinaryDirectRoute(transport))
		{
			return true;
		}
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

	static boolean isOrdinaryDirectRoute(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() <= 0 || isBlank(transport.getAction())
			|| isBlank(transport.getName()) || transport.getCurrencyAmount() != 0
			|| !isBlank(transport.getCurrencyName()) || transport.isConsumable()
			|| !transport.getItemIdRequirements().isEmpty() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty()
			|| !ORDINARY_DIRECT_ROUTES.contains(ordinaryRouteKey(transport)))
		{
			return false;
		}
		if (transport.getObjectId() == 12888 || transport.getObjectId() == 12889)
		{
			return transport.getQuests().size() == 1
				&& transport.getQuests().get(Quest.CHILDREN_OF_THE_SUN) == QuestState.FINISHED;
		}
		return transport.getQuests().isEmpty();
	}

	static boolean isLiveNpcMatch(Transport transport, String liveName,
		Collection<String> liveActions, WorldPoint liveTile)
	{
		if (!isOrdinaryDirectRoute(transport) || liveName == null || liveActions == null
			|| liveTile == null || !liveName.equalsIgnoreCase(transport.getName())
			|| liveTile.distanceTo2D(transport.getOrigin()) > LIVE_NPC_ORIGIN_RADIUS)
		{
			return false;
		}
		return liveActions.stream().anyMatch(action -> action != null
			&& normalizeAction(action).equals(normalizeAction(transport.getAction())));
	}

	private static String ordinaryRouteKey(Transport transport)
	{
		WorldPoint origin = transport.getOrigin();
		WorldPoint destination = transport.getDestination();
		return pointKey(origin) + "->" + pointKey(destination) + "|"
			+ transport.getObjectId() + "|" + transport.getAction() + "|"
			+ transport.getName() + "|"
			+ (transport.getDisplayInfo() == null ? "" : transport.getDisplayInfo()) + "|"
			+ transport.getDuration();
	}

	private static String pointKey(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
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
