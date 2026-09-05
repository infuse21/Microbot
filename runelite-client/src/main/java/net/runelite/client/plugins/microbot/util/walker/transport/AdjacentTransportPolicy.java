package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative eligibility policy for short object-backed same-plane transports. */
public final class AdjacentTransportPolicy
{
	private static final int ADJACENT_DISTANCE = 1;
	private static final int SHORT_PORTAL_DISTANCE = 2;
	private static final int FEROX_BARRIER = 39652;
	private static final int FEROX_BARRIER_MIRRORED = 39653;
	private static final int SLASHABLE_WEB = 733;
	private static final String STRONGHOLD_TREE_DOOR = "tree door";
	private static final Set<Integer> MOLCH_MYSTICAL_BARRIERS = Set.of(
		34643, 34644, 34645, 34646);
	private static final Set<String> AL_KHARID_TOLL_ROUTES = Set.of(
		"3267,3227,0->3268,3227,0|2786",
		"3267,3228,0->3268,3228,0|2787",
		"3268,3227,0->3267,3227,0|2788",
		"3268,3228,0->3267,3228,0|2789");
	private static final Set<Integer> WILDERNESS_SWORDS = Set.of(
		ItemID.WILDERNESS_SWORD_EASY, ItemID.WILDERNESS_SWORD_MEDIUM,
		ItemID.WILDERNESS_SWORD_HARD, ItemID.WILDERNESS_SWORD_ELITE);
	private static final Set<String> DIRECT_ACTIONS = Set.of(
		"open", "pass", "walk-through", "go-through", "climb-over", "climb-through",
		"squeeze-through", "cross", "vault");
	private AdjacentTransportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getObjectId() <= 0 || isBlank(transport.getAction())
			|| transport.getOrigin().getPlane() != transport.getDestination().getPlane())
		{
			return false;
		}
		TransportType type = transport.getType();
		if (type != TransportType.TRANSPORT && type != TransportType.AGILITY_SHORTCUT
			&& type != TransportType.GRAPPLE_SHORTCUT)
		{
			return false;
		}
		String action = transport.getAction().toLowerCase(Locale.ROOT);
		boolean directRocks = type == TransportType.TRANSPORT
			&& "climb".equals(action) && "rocks".equals(normalize(transport.getName()))
			&& transport.getItemIdRequirements().isEmpty();
		boolean feroxBarrier = isFeroxBarrier(transport, action);
		boolean slashableWeb = isSlashableWeb(transport, action);
		boolean molchMysticalBarrier = isMolchMysticalBarrier(transport, action);
		if (isAlKharidTollGate(transport))
		{
			return true;
		}
		if (!DIRECT_ACTIONS.contains(action) && !directRocks && !feroxBarrier && !slashableWeb)
		{
			return false;
		}
		if (transport.getCurrencyAmount() > 0)
		{
			return false;
		}
		int distance = transport.getOrigin().distanceTo2D(transport.getDestination());
		return distance <= ADJACENT_DISTANCE || isStrongholdTreeDoor(transport, action, distance)
			|| slashableWeb && distance <= SHORT_PORTAL_DISTANCE
			|| molchMysticalBarrier && distance <= SHORT_PORTAL_DISTANCE;
	}

	private static boolean isAlKharidTollGate(Transport transport)
	{
		return transport.getType() == TransportType.TRANSPORT
			&& "pay-toll(10gp)".equals(normalize(transport.getAction()))
			&& "gate".equals(normalize(transport.getName()))
			&& transport.getCurrencyAmount() == 10
			&& "coins".equals(normalize(transport.getCurrencyName()))
			&& !transport.isConsumable() && transport.getItemIdRequirements().isEmpty()
			&& transport.getQuests().isEmpty() && transport.getVarbits().isEmpty()
			&& transport.getVarplayers().isEmpty() && transport.getDuration() == 2
			&& AL_KHARID_TOLL_ROUTES.contains(pointKey(transport.getOrigin()) + "->"
				+ pointKey(transport.getDestination()) + "|" + transport.getObjectId());
	}

	private static String pointKey(net.runelite.api.coords.WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}

	private static boolean isMolchMysticalBarrier(Transport transport, String action)
	{
		return transport.getType() == TransportType.TRANSPORT
			&& "pass".equals(action)
			&& "mystical barrier".equals(normalize(transport.getName()))
			&& MOLCH_MYSTICAL_BARRIERS.contains(transport.getObjectId())
			&& !transport.isConsumable()
			&& transport.getItemIdRequirements().isEmpty();
	}

	private static boolean isSlashableWeb(Transport transport, String action)
	{
		return transport.getType() == TransportType.TRANSPORT
			&& "slash".equals(action)
			&& "web".equals(normalize(transport.getName()))
			&& transport.getObjectId() == SLASHABLE_WEB
			&& !transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(WILDERNESS_SWORDS));
	}

	private static boolean isFeroxBarrier(Transport transport, String action)
	{
		int objectId = transport.getObjectId();
		return transport.getType() == TransportType.TRANSPORT
			&& "pass-through".equals(action)
			&& "barrier".equals(normalize(transport.getName()))
			&& (objectId == FEROX_BARRIER || objectId == FEROX_BARRIER_MIRRORED)
			&& transport.getItemIdRequirements().isEmpty();
	}

	private static boolean isStrongholdTreeDoor(Transport transport, String action, int distance)
	{
		return distance <= SHORT_PORTAL_DISTANCE
			&& transport.getType() == TransportType.TRANSPORT
			&& action.equals("open")
			&& STRONGHOLD_TREE_DOOR.equals(normalize(transport.getName()));
	}

	public static boolean actionClearsObject(String action)
	{
		if (action == null)
		{
			return false;
		}
		String normalized = action.toLowerCase(Locale.ROOT);
		return normalized.equals("open") || normalized.equals("pass")
			|| normalized.equals("walk-through") || normalized.equals("go-through")
			|| normalized.equals("slash");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
