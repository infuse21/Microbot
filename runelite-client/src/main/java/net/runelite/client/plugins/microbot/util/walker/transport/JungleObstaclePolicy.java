package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Exact catalog and live-object boundary for deterministic axe- and machete-gated crossings. */
public final class JungleObstaclePolicy
{
	private static final Set<Integer> BUSH_IDS = Set.of(2892, 2893);
	private static final Set<Integer> TREE_IDS = Set.of(2889, 2890);
	private static final Set<Integer> BRIMHAVEN_VINE_IDS = Set.of(
		21731, 21732, 21733, 21734, 21735);
	private static final Set<Integer> MACHETE_IDS = Set.of(6313, 6315, 6317, 975);
	private static final Set<Integer> AXE_IDS = Set.of(
		1361, 6739, 1349, 1351, 1353, 23673, 13241, 1355, 20011, 1357, 1359, 23279);
	private static final Set<Integer> BRIMHAVEN_AXE_IDS = Set.of(
		1351, 1349, 1353, 1361, 1355, 1357, 1359, 6739, 20011, 13241, 23673,
		25066, 28222);
	private static final Set<String> AMBIGUOUS_INPUTS = Set.of(
		"2866,2936,0:2892", "2819,2938,0:2893", "2791,2939,0:2893",
		"2933,2939,0:2893", "2868,2936,0:2889", "2793,2939,0:2893",
		"2799,2939,0:2893", "2762,2936,0:2892");
	private static final int CROSSING_DISTANCE = 2;
	private static final int LIVE_OBJECT_RADIUS = 2;

	private JungleObstaclePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| !"chopdown".equals(normalizeAction(transport.getAction()))
			|| transport.getOrigin().getPlane() != transport.getDestination().getPlane()
			|| transport.getOrigin().distanceTo2D(transport.getDestination())
				!= CROSSING_DISTANCE
			|| transport.getCurrencyAmount() != 0 || transport.isConsumable()
			|| AMBIGUOUS_INPUTS.contains(inputKey(transport)))
		{
			return false;
		}
		String name = normalize(transport.getName());
		Set<Integer> expectedTools;
		if ("jungle bush".equals(name) && BUSH_IDS.contains(transport.getObjectId()))
		{
			expectedTools = MACHETE_IDS;
		}
		else if ("jungle tree".equals(name) && TREE_IDS.contains(transport.getObjectId()))
		{
			expectedTools = AXE_IDS;
		}
		else if ("vines".equals(name)
			&& BRIMHAVEN_VINE_IDS.contains(transport.getObjectId()))
		{
			expectedTools = BRIMHAVEN_AXE_IDS;
		}
		else
		{
			return false;
		}
		return transport.getItemIdRequirements().stream()
			.flatMap(Set::stream).collect(Collectors.toSet()).equals(expectedTools);
	}

	public static boolean isLiveObjectMatch(Transport transport, int objectId,
		String name, List<String> actions, WorldPoint tile)
	{
		if (!isEligible(transport) || objectId != transport.getObjectId() || tile == null
			|| tile.getPlane() != transport.getOrigin().getPlane()
			|| tile.distanceTo2D(transport.getOrigin()) > LIVE_OBJECT_RADIUS
			|| !normalize(transport.getName()).equals(normalize(name)) || actions == null)
		{
			return false;
		}
		String expected = normalizeAction(transport.getAction());
		return actions.stream().anyMatch(action -> expected.equals(normalizeAction(action)));
	}

	static boolean isAmbiguousInput(Transport transport)
	{
		return transport != null && transport.getOrigin() != null
			&& AMBIGUOUS_INPUTS.contains(inputKey(transport));
	}

	private static String inputKey(Transport transport)
	{
		WorldPoint origin = transport.getOrigin();
		return origin.getX() + "," + origin.getY() + "," + origin.getPlane()
			+ ":" + transport.getObjectId();
	}

	private static String normalizeAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
