package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Set;

/** Exact staged contract for the two Shantay-pass gate families. */
public final class ShantayPassPolicy
{
	public enum Stage
	{
		UNAVAILABLE,
		BUY_PASS,
		GO_THROUGH
	}

	public static final int PASS_ITEM_ID = 1854;
	public static final int MAIN_GATE_ID = 4031;
	public static final int UNKAH_GATE_ID = 41326;
	public static final String BUY_PASS_ACTION = "Buy-pass";
	public static final String GO_THROUGH_ACTION = "Go-through";

	private static final Set<String> MAIN_PAID_ROUTES = Set.of(
		"3303,3117,0->3304,3115,0|4031",
		"3304,3117,0->3304,3115,0|4031");
	private static final Set<String> MAIN_FREE_ROUTES = Set.of(
		"3304,3115,0->3304,3117,0|4031",
		"3303,3115,0->3304,3117,0|4031");
	private static final Set<String> UNKAH_PAID_ROUTES = Set.of(
		"3193,2843,0->3196,2843,0|41326",
		"3193,2842,0->3196,2842,0|41326",
		"3167,2819,0->3167,2816,0|41326",
		"3168,2819,0->3168,2816,0|41326");
	private static final Set<String> UNKAH_FREE_ROUTES = Set.of(
		"3196,2843,0->3193,2843,0|41326",
		"3196,2842,0->3193,2842,0|41326",
		"3167,2816,0->3167,2819,0|41326",
		"3168,2816,0->3168,2819,0|41326");

	private ShantayPassPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| !GO_THROUGH_ACTION.equalsIgnoreCase(transport.getAction())
			|| !"Shantay pass".equalsIgnoreCase(transport.getName()))
		{
			return false;
		}
		String key = routeKey(transport);
		if (MAIN_PAID_ROUTES.contains(key))
		{
			return requiresPass(transport) || isCoinTwin(transport);
		}
		if (UNKAH_PAID_ROUTES.contains(key))
		{
			return requiresPass(transport);
		}
		return (MAIN_FREE_ROUTES.contains(key) || UNKAH_FREE_ROUTES.contains(key))
			&& isFreeReturn(transport);
	}

	public static boolean requiresPass(Transport transport)
	{
		return transport != null && transport.isConsumable()
			&& transport.getCurrencyAmount() == 0
			&& Set.of(Set.of(PASS_ITEM_ID)).equals(transport.getItemIdRequirements());
	}

	public static boolean isCoinTwin(Transport transport)
	{
		return transport != null && !transport.isConsumable()
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 5
			&& "Coins".equalsIgnoreCase(transport.getCurrencyName());
	}

	public static boolean isFreeReturn(Transport transport)
	{
		return transport != null && !transport.isConsumable()
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getCurrencyAmount() == 0;
	}

	public static Stage nextStage(Transport transport, boolean passCarried, long coinsHeld,
		boolean eliteDiary, boolean vendorAvailable, String pendingAction)
	{
		if (!isEligible(transport))
		{
			return Stage.UNAVAILABLE;
		}
		if (GO_THROUGH_ACTION.equalsIgnoreCase(pendingAction) || isFreeReturn(transport)
			|| eliteDiary || passCarried)
		{
			return Stage.GO_THROUGH;
		}
		if (transport.getObjectId() == MAIN_GATE_ID && vendorAvailable
			&& (BUY_PASS_ACTION.equalsIgnoreCase(pendingAction) || coinsHeld >= 5))
		{
			return Stage.BUY_PASS;
		}
		return Stage.UNAVAILABLE;
	}

	public static boolean hasCrossed(int objectId, WorldPoint origin,
		WorldPoint destination, WorldPoint player)
	{
		if ((objectId != MAIN_GATE_ID && objectId != UNKAH_GATE_ID)
			|| origin == null || destination == null || player == null
			|| player.getPlane() != destination.getPlane()
			|| player.distanceTo2D(destination) > 2)
		{
			return false;
		}
		int dx = destination.getX() - origin.getX();
		if (Math.abs(dx) >= Math.abs(destination.getY() - origin.getY()))
		{
			return dx > 0 ? player.getX() >= destination.getX()
				: player.getX() <= destination.getX();
		}
		int dy = destination.getY() - origin.getY();
		return dy > 0 ? player.getY() >= destination.getY()
			: player.getY() <= destination.getY();
	}

	private static String routeKey(Transport transport)
	{
		return point(transport.getOrigin()) + "->" + point(transport.getDestination())
			+ "|" + transport.getObjectId();
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}
}
