package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative eligibility for direct object-backed scene transitions. */
public final class CatalogTransitionPolicy
{
	private static final Set<String> DIRECT_ACTIONS = Set.of(
		"climb-up", "climb-down", "climb", "climb up", "climb down",
		"walk-up", "walk-down", "ascend", "descend", "top-floor", "bottom-floor",
		"enter", "exit", "leave", "crawl", "climb-into", "cross");
	private static final Set<String> DIRECT_AGILITY_ACTIONS = Set.of(
		"climb", "squeezethrough", "cross", "enter", "walkacross", "climbinto",
		"climbdown", "jumpover", "jumpto", "climbover", "jump", "climbup",
		"jumpacross", "pass", "squeezepast", "swingacross", "jumpdown", "jumpup",
		"climbthrough", "climbunder", "open", "teethgrip");
	private static final Set<Integer> DENSE_FOREST_IDS = Set.of(
		3937, 3938, 3939, 3998, 3999);
	private static final Set<Integer> DIRECT_HOLE_IDS = Set.of(31791, 28915, 28919, 28920, 28921);
	private static final Set<Integer> CATACOMBS_EXIT_VINE_IDS = Set.of(28895, 28896, 28897, 28898, 42350);
	private static final Set<Integer> DIRECT_STEPS_IDS = Set.of(30189, 30190, 8966, 33261);
	private static final int ENAKHRAS_TEMPLE_SAND_PILE_ID = 10950;
	private static final Set<Integer> CHASM_OF_FIRE_LIFT_IDS = Set.of(30258, 30259);
	private static final Set<Integer> MYTHS_GUILD_MAGICAL_BARRIER_IDS = Set.of(31616, 31617);
	private static final Set<Integer> PRIFDDINAS_CITY_GATE_ENTER_IDS = Set.of(36518, 36519);
	private static final Set<Integer> PRIFDDINAS_CITY_GATE_EXIT_IDS = Set.of(36522, 36523);
	private static final Set<Integer> BASALT_CAUSEWAY_IDS = Set.of(
		4550, 4551, 4552, 4553, 4554, 4555, 4556, 4557, 4558, 4559);
	private static final Set<String> DIRECT_RAFT_ROUTES = Set.of(
		"2510,3494,0->2512,3481,0|1987|board|log raft",
		"2510,3493,0->2512,3481,0|1987|board|log raft",
		"1742,5352,0->2531,3446,0|25216|ride|aged log",
		"1761,5362,0->2531,3446,0|25216|ride|aged log",
		"2567,9680,0->2606,9692,0|2849|board|raft",
		"2606,9692,0->2567,9680,0|2849|board|raft");
	private static final Set<String> TEMPLE_OF_THE_EYE_PORTAL_ROUTES = Set.of(
		"3104,9573,0->3615,9470,0|43841|enter|portal",
		"3615,9470,0->3104,9573,0|43692|enter|portal");
	private static final int GUARDIANS_OF_THE_RIFT_BARRIER_ID = 43700;
	private static final int RUBBER_CAP_MUSHROOM_ID = 30606;
	/** Frozen item-free direct routes audited as one-click scene transitions. */
	private static final Set<String> AUDITED_DIRECT_ROUTE_KEYS = Set.of(
		"3439,3337,0->3442,9734,1|3516|enter|grotto",
		"3440,3337,0->3442,9734,1|3516|enter|grotto",
		"3441,3337,0->3442,9734,1|3516|enter|grotto",
		"3442,9734,1->3440,3337,0|3526|exit|grotto",
		"2386,3333,0->2386,3335,0|3944|enter|huge gate",
		"2386,3335,0->2386,3333,0|3944|enter|huge gate",
		"2385,3333,0->2385,3335,0|3945|enter|huge gate",
		"2385,3335,0->2385,3333,0|3945|enter|huge gate",
		"2304,3194,0->2306,3195,0|8742|pass|tree",
		"2304,3195,0->2306,3195,0|8742|pass|tree",
		"2306,3194,0->2304,3194,0|8742|pass|tree",
		"2306,3195,0->2304,3195,0|8742|pass|tree",
		"3363,3298,0->3363,3300,0|10721|enter|doorway",
		"3363,3300,0->3363,3298,0|10721|enter|doorway",
		"2715,3798,0->2715,3802,1|19690|ascend|steps",
		"2716,3798,0->2716,3802,1|19690|ascend|steps",
		"2726,3801,0->2726,3805,1|19690|ascend|steps",
		"2727,3801,0->2727,3805,1|19690|ascend|steps",
		"2715,3802,1->2715,3798,0|19691|descend|steps",
		"2716,3802,1->2716,3798,0|19691|descend|steps",
		"2726,3805,1->2726,3801,0|19691|descend|steps",
		"2727,3805,1->2727,3801,0|19691|descend|steps",
		"1556,3046,2->1559,3046,0|51644|climbdown|rope",
		"1559,3046,0->1556,3046,2|51647|climbup|rope",
		"1425,2933,0->1427,2933,0|54707|passthrough|entryway",
		"1427,2933,0->1425,2933,0|54707|passthrough|entryway",
		"1259,3430,0->1271,3436,0|57219|passthrough|cave",
		"1271,3436,0->1259,3430,0|57220|passthrough|cave");

	private CatalogTransitionPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (EnergyBarrierPolicy.isEligible(transport))
		{
			return true;
		}
		if (transport == null || transport.getOrigin() == null
			|| transport.getDestination() == null || transport.getObjectId() <= 0
			|| isBlank(transport.getAction()) || isBlank(transport.getName())
			|| transport.getCurrencyAmount() > 0
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		boolean changesScene = transport.getOrigin().getPlane() != transport.getDestination().getPlane()
			|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 1;
		if (!changesScene)
		{
			return false;
		}
		if (isPohPortal(transport))
		{
			return true;
		}
		if (transport.getType() == TransportType.AGILITY_SHORTCUT)
		{
			return DIRECT_AGILITY_ACTIONS.contains(normalizeDirectAction(
				transport.getAction()));
		}
		if (isOrdinaryDirectTransition(transport))
		{
			return true;
		}
		if (transport.getType() != TransportType.TRANSPORT
			|| !DIRECT_ACTIONS.contains(normalize(transport.getAction())))
		{
			return false;
		}
		String name = normalize(transport.getName());
		return name.contains("ladder") || name.contains("stair")
			|| name.contains("trapdoor") || name.contains("cave")
			|| name.contains("gangplank");
	}

	private static boolean isOrdinaryDirectTransition(Transport transport)
	{
		if (transport.getType() != TransportType.TRANSPORT)
		{
			return false;
		}
		String action = normalizeDirectAction(transport.getAction());
		String name = normalize(transport.getName());
		return AUDITED_DIRECT_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| "climbover".equals(action) && "stile".equals(name)
			|| "climb".equals(action) && "rocks".equals(name)
			|| isBasaltCausewayTransition(transport, action, name)
			|| DIRECT_RAFT_ROUTES.contains(routeKey(transport, action, name))
			|| TEMPLE_OF_THE_EYE_PORTAL_ROUTES.contains(routeKey(transport, action, name))
			|| isGuardiansOfTheRiftBarrier(transport, action, name)
			|| "pass".equals(action) && "barrier".equals(name)
				&& transport.getObjectId() == 32153
			|| "enter".equals(action) && "dense forest".equals(name)
				&& DENSE_FOREST_IDS.contains(transport.getObjectId())
			|| "enter".equals(action) && "lift".equals(name)
				&& CHASM_OF_FIRE_LIFT_IDS.contains(transport.getObjectId())
			|| "jumpon".equals(action) && "rubber cap mushroom".equals(name)
				&& transport.getObjectId() == RUBBER_CAP_MUSHROOM_ID
			|| "pass".equals(action) && "magical barrier".equals(name)
				&& MYTHS_GUILD_MAGICAL_BARRIER_IDS.contains(transport.getObjectId())
			|| "city gate".equals(name)
				&& ("enter".equals(action)
					&& PRIFDDINAS_CITY_GATE_ENTER_IDS.contains(transport.getObjectId())
					|| "exit".equals(action)
					&& PRIFDDINAS_CITY_GATE_EXIT_IDS.contains(transport.getObjectId()))
			|| "enter".equals(action) && "passageway".equals(name)
				&& (transport.getObjectId() == 7258
					|| isTarnsLairPassagewayId(transport.getObjectId()))
			|| "enter".equals(action) && "tunnel".equals(name)
				&& transport.getObjectId() == 2141
			|| "enter".equals(action) && "hole".equals(name)
				&& DIRECT_HOLE_IDS.contains(transport.getObjectId())
			|| "climbup".equals(action) && "vine".equals(name)
				&& CATACOMBS_EXIT_VINE_IDS.contains(transport.getObjectId())
			|| "climb".equals(action) && "sand pile".equals(name)
				&& transport.getObjectId() == ENAKHRAS_TEMPLE_SAND_PILE_ID
			|| "climb".equals(action) && "steps".equals(name)
				&& DIRECT_STEPS_IDS.contains(transport.getObjectId())
				&& (transport.getOrigin().getPlane() != transport.getDestination().getPlane()
					|| transport.getOrigin().distanceTo2D(transport.getDestination()) > 2)
			|| "jumpto".equals(action) && "pillar".equals(name)
				&& isEasyRevenantCavesPillar(transport);
	}

	private static boolean isGuardiansOfTheRiftBarrier(Transport transport, String action,
		String name)
	{
		if (transport.getObjectId() != GUARDIANS_OF_THE_RIFT_BARRIER_ID
			|| !"quickpass".equals(action) || !"barrier".equals(name))
		{
			return false;
		}
		int originX = transport.getOrigin().getX();
		int originY = transport.getOrigin().getY();
		int destinationX = transport.getDestination().getX();
		int destinationY = transport.getDestination().getY();
		return originX == destinationX && originX >= 3613 && originX <= 3617
			&& transport.getOrigin().getPlane() == 0
			&& transport.getDestination().getPlane() == 0
			&& (originY == 9482 && destinationY == 9484
				|| originY == 9484 && destinationY == 9482);
	}

	private static String routeKey(Transport transport, String action, String name)
	{
		return pointKey(transport.getOrigin()) + "->" + pointKey(transport.getDestination())
			+ "|" + transport.getObjectId() + "|" + action + "|" + name;
	}

	private static String pointKey(net.runelite.api.coords.WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}

	private static boolean isBasaltCausewayTransition(Transport transport, String action,
		String name)
	{
		if (!BASALT_CAUSEWAY_IDS.contains(transport.getObjectId()))
		{
			return false;
		}
		return "jumpacross".equals(action) && "basalt rock".equals(name)
			|| "jumpto".equals(action)
				&& ("beach".equals(name) || "rocky shore".equals(name));
	}

	private static boolean isEasyRevenantCavesPillar(Transport transport)
	{
		if (transport.getObjectId() != 31561)
		{
			return false;
		}
		return hasDirectedEndpoints(transport, 3220, 10088, 3220, 10084)
			|| hasDirectedEndpoints(transport, 3220, 10084, 3220, 10088);
	}

	private static boolean hasDirectedEndpoints(Transport transport, int originX, int originY,
		int destinationX, int destinationY)
	{
		return transport.getOrigin().getX() == originX && transport.getOrigin().getY() == originY
			&& transport.getOrigin().getPlane() == 0
			&& transport.getDestination().getX() == destinationX
			&& transport.getDestination().getY() == destinationY
			&& transport.getDestination().getPlane() == 0;
	}

	private static boolean isTarnsLairPassagewayId(int objectId)
	{
		return objectId == 15771 || objectId == 16132 || objectId == 18308
			|| objectId == 19029 || objectId == 20482 || objectId == 20539
			|| isBetween(objectId, 20489, 20492)
			|| isBetween(objectId, 20497, 20506)
			|| isBetween(objectId, 20509, 20532)
			|| isBetween(objectId, 20535, 20536);
	}

	private static boolean isBetween(int value, int minimum, int maximum)
	{
		return value >= minimum && value <= maximum;
	}

	static boolean isPohPortal(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.POH
			|| !"portal".equals(normalize(transport.getName())))
		{
			return false;
		}
		String action = normalize(transport.getAction());
		return "home".equals(action) || "enter".equals(action);
	}

	public static boolean supportsClosedVariant(String action)
	{
		String normalized = normalize(action);
		return normalized.equals("climb-down") || normalized.equals("climb down");
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalizeDirectAction(String value)
	{
		return normalize(value).replace("-", "").replace(" ", "");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
