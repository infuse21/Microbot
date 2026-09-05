package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Conservative ownership and destination-log contract for unlocked balloon travel. */
public final class HotAirBalloonPolicy
{
	public static final String DESTINATION_ACTION_PREFIX = "hot-air-balloon-destination:";
	public static final int LIVE_OBJECT_ORIGIN_TOLERANCE = 4;
	private static final int[] BASKET_OBJECT_IDS = {
		ObjectID.ZEP_BASKET_ENTRANA,
		ObjectID.ZEP_BASKET,
		ObjectID.ZEP_MULTI_BASKET_ENTRANA,
		ObjectID.ZEP_MULTI_BASKET_TAV,
		ObjectID.ZEP_MULTI_BASKET_CAST,
		ObjectID.ZEP_MULTI_BASKET_GNO,
		ObjectID.ZEP_MULTI_BASKET_CRAFT,
		ObjectID.ZEP_MULTI_BASKET_VARR
	};
	private static final Set<Integer> OBJECT_IDS = Set.of(
		ObjectID.ZEP_BASKET_ENTRANA,
		ObjectID.ZEP_BASKET,
		ObjectID.ZEP_MULTI_BASKET_ENTRANA,
		ObjectID.ZEP_MULTI_BASKET_TAV,
		ObjectID.ZEP_MULTI_BASKET_CAST,
		ObjectID.ZEP_MULTI_BASKET_GNO,
		ObjectID.ZEP_MULTI_BASKET_CRAFT,
		ObjectID.ZEP_MULTI_BASKET_VARR);
	private static final Map<String, Integer> DESTINATION_LOGS = Map.of(
		"entrana", ItemID.LOGS,
		"taverley", ItemID.LOGS,
		"crafting guild", ItemID.OAK_LOGS,
		"varrock", ItemID.WILLOW_LOGS,
		"castle wars", ItemID.YEW_LOGS,
		"grand tree", ItemID.MAGIC_LOGS);

	private HotAirBalloonPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.HOT_AIR_BALLOON
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getOrigin().equals(transport.getDestination())
			|| !OBJECT_IDS.contains(transport.getObjectId())
			|| !"use".equals(normalize(transport.getAction()))
			|| !"basket".equals(normalize(transport.getName()))
			|| transport.getCurrencyAmount() != 0 || !transport.isConsumable())
		{
			return false;
		}
		Integer requiredLog = requiredLogId(transport.getDisplayInfo());
		return requiredLog != null && transport.getItemIdRequirements().size() == 1
			&& transport.getItemIdRequirements().contains(Set.of(requiredLog));
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
		return displayInfo == null ? "" : displayInfo.trim();
	}

	public static Integer requiredLogId(String destinationName)
	{
		return DESTINATION_LOGS.get(normalize(destinationName));
	}

	static int[] basketObjectIds()
	{
		return BASKET_OBJECT_IDS.clone();
	}

	static boolean isBasketObjectId(int objectId)
	{
		return OBJECT_IDS.contains(objectId);
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}
}
