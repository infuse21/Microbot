package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared transport-time estimates used by path search and route comparison. */
public final class TransportCostModel
{
	/** Observed equipped-staff fairy-ring trips take roughly 22-25 game ticks. */
	static final int FAIRY_RING_MINIMUM_TICKS = 24;
	/** Observed spirit-tree menu and landing sequences take roughly 10-13 game ticks. */
	static final int SPIRIT_TREE_MINIMUM_TICKS = 12;
	/** Live glider menu travel confirms the catalog's eight-tick estimate is conservative. */
	static final int GNOME_GLIDER_MINIMUM_TICKS = 8;
	/** Live quetzal destination selection and landing take roughly 10-12 game ticks. */
	static final int QUETZAL_MINIMUM_TICKS = 10;
	/** Live POH physical-portal transitions take roughly 4-7 game ticks. */
	static final int POH_MINIMUM_TICKS = 6;
	/** Live canoe construction, selection, and landing take roughly 53-57 game ticks. */
	static final int CANOE_MINIMUM_TICKS = 54;
	/** Live Keldagrim train-cart travel takes roughly 26-29 game ticks. */
	static final int KELDAGRIM_TRAIN_CART_MINIMUM_TICKS = 28;
	/** Kourend's object, destination menu, and landing need more than its five-tick row. */
	static final int KOUREND_MINECART_MINIMUM_TICKS = 8;
	/** Live Soul Wars portal interactions and landing take roughly two to three game ticks. */
	static final int TELEPORTATION_PORTAL_MINIMUM_TICKS = 3;

	private TransportCostModel()
	{
	}

	/**
	 * Returns the configured duration with a measured floor for staged transports.
	 * Every interaction costs at least one tick, even when its data row omits Duration.
	 */
	public static int travelTicks(Transport transport)
	{
		if (transport == null)
		{
			return 1;
		}
		int minimum;
		switch (transport.getType())
		{
			case FAIRY_RING:
				minimum = FAIRY_RING_MINIMUM_TICKS;
				break;
			case SPIRIT_TREE:
				minimum = SPIRIT_TREE_MINIMUM_TICKS;
				break;
			case GNOME_GLIDER:
				minimum = GNOME_GLIDER_MINIMUM_TICKS;
				break;
			case QUETZAL:
				minimum = QUETZAL_MINIMUM_TICKS;
				break;
			case POH:
				minimum = POH_MINIMUM_TICKS;
				break;
			case CANOE:
				minimum = CANOE_MINIMUM_TICKS;
				break;
			case MINECART:
				minimum = transport.getObjectId() == 28835
					? KOUREND_MINECART_MINIMUM_TICKS
					: transport.getObjectId() == 16168 ? 21
						: KELDAGRIM_TRAIN_CART_MINIMUM_TICKS;
				break;
			case TELEPORTATION_PORTAL:
				minimum = TELEPORTATION_PORTAL_MINIMUM_TICKS;
				break;
			default:
				minimum = 1;
				break;
		}
		return Math.max(minimum, transport.getDuration());
	}

	/** Scores a reconstructed path in the same tick-equivalent units as the pathfinder. */
	public static int pathTicks(List<WorldPoint> path,
		Map<WorldPoint, Set<Transport>> transports)
	{
		if (path == null || path.isEmpty())
		{
			return Integer.MAX_VALUE;
		}
		long total = 0;
		for (int i = 1; i < path.size(); i++)
		{
			int edgeIndex = i;
			WorldPoint from = path.get(i - 1);
			WorldPoint to = path.get(i);
			int edgeTicks = TransportEdgeMatcher.find(transports, from, to).stream()
				.mapToInt(TransportCostModel::travelTicks)
				.min()
				.orElseGet(() -> originlessTicks(transports, edgeIndex, from, to));
			if (edgeTicks < 0)
			{
				edgeTicks = WorldPointUtil.distanceBetween(from, to);
			}
			if (edgeTicks == Integer.MAX_VALUE || total + edgeTicks >= Integer.MAX_VALUE)
			{
				return Integer.MAX_VALUE;
			}
			total += edgeTicks;
		}
		return (int) total;
	}

	private static int originlessTicks(Map<WorldPoint, Set<Transport>> transports,
		int edgeIndex, WorldPoint origin, WorldPoint destination)
	{
		if (edgeIndex != 1 || transports == null || origin == null || destination == null)
		{
			return -1;
		}
		Set<Transport> originless = transports.get(origin);
		if (originless == null)
		{
			return -1;
		}
		return originless.stream().filter(transport -> transport != null
			&& transport.getOrigin() == null
			&& destination.equals(transport.getDestination()))
			.mapToInt(TransportCostModel::travelTicks).min().orElse(-1);
	}
}
