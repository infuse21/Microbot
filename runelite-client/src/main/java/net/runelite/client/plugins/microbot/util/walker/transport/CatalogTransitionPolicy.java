package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;

import java.util.Locale;
import java.util.Map;
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
	private static final Set<String> NEYPOTZLI_ENTRANCE_ROUTE_KEYS = Set.of(
		"1374,9667,0->1347,9590,0|51377|passthrough|entrance",
		"1347,9590,0->1374,9667,0|51375|passthrough|entrance",
		"1418,9632,0->1387,9591,0|51377|passthrough|entrance",
		"1387,9591,0->1418,9632,0|51375|passthrough|entrance",
		"1513,9563,0->1355,9538,0|51377|passthrough|entrance",
		"1355,9538,0->1513,9563,0|51375|passthrough|entrance",
		"1526,9671,0->1513,9596,0|51377|passthrough|entrance",
		"1513,9596,0->1526,9671,0|51377|passthrough|entrance",
		"1522,9719,0->1390,9676,0|51378|passthrough|entrance",
		"1390,9676,0->1522,9719,0|51378|passthrough|entrance",
		"1403,9717,0->1422,9649,1|51375|passthrough|entrance",
		"1422,9649,1->1403,9717,0|51376|passthrough|entrance",
		"1388,9575,0->1423,9615,1|51375|passthrough|entrance",
		"1423,9615,1->1388,9575,0|51376|passthrough|entrance",
		"1480,9669,0->1457,9649,1|51375|passthrough|entrance",
		"1457,9649,1->1480,9669,0|51376|passthrough|entrance",
		"1510,9675,0->1461,9632,0|51375|passthrough|entrance",
		"1461,9632,0->1510,9675,0|51377|passthrough|entrance",
		"1440,9653,0->1403,9704,0|51377|passthrough|entrance",
		"1403,9704,0->1440,9653,0|51377|passthrough|entrance",
		"1440,9615,1->1439,9599,1|51377|passthrough|entrance",
		"1439,9599,1->1440,9615,1|51375|passthrough|entrance",
		"1435,3128,0->1439,9509,1|51375|passthrough|entrance",
		"1436,3128,0->1439,9509,1|51375|passthrough|entrance",
		"1439,9509,1->1435,3128,0|51375|passthrough|entrance");
	private static final Set<String> DIRECT_CLIMB_UP_ROPE_ROUTE_KEYS = Set.of(
		"3297,9824,0->3312,3450,0|13999|climbup|rope",
		"3298,9823,0->3312,3450,0|13999|climbup|rope",
		"3296,9823,0->3312,3450,0|13999|climbup|rope",
		"3297,9822,0->3312,3450,0|13999|climbup|rope",
		"3483,9510,2->3226,3108,0|3829|climbup|rope",
		"3484,9510,2->3226,3108,0|3829|climbup|rope",
		"3483,9509,2->3226,3108,0|3829|climbup|rope",
		"3508,9493,0->3508,9497,2|3832|climbup|rope",
		"3507,9494,0->3508,9497,2|3832|climbup|rope",
		"3508,9494,0->3508,9497,2|3832|climbup|rope",
		"3206,9379,0->3310,2961,0|6439|climbup|rope",
		"3205,9380,0->3310,2961,0|6439|climbup|rope",
		"3204,9379,0->3310,2961,0|6439|climbup|rope",
		"3205,9378,0->3310,2961,0|6439|climbup|rope",
		"2914,5300,1->2912,5299,2|26371|climbup|rope",
		"2920,5274,0->2919,5276,1|26375|climbup|rope",
		"2919,5274,0->2919,5276,1|26375|climbup|rope",
		"2915,5300,1->2912,5299,2|26371|climbup|rope",
		"1435,10077,3->1435,3671,0|30234|climbup|rope",
		"2128,5647,0->2026,5611,0|28687|climbup|rope");
	private static final Set<String> DIRECT_CLIMB_DOWN_HOLE_ROUTE_KEYS = Set.of(
		"2620,3864,0->2619,10265,0|15203|climbdown|hole",
		"2619,3865,0->2619,10265,0|15203|climbdown|hole",
		"2621,3865,0->2619,10265,0|15203|climbdown|hole",
		"2916,3748,0->2882,5311,2|26419|climbdown|hole",
		"2916,3745,0->2882,5311,2|26419|climbdown|hole",
		"2916,3746,0->2882,5311,2|26419|climbdown|hole",
		"2917,3744,0->2882,5311,2|26419|climbdown|hole",
		"2918,3744,0->2882,5311,2|26419|climbdown|hole",
		"2919,3744,0->2882,5311,2|26419|climbdown|hole",
		"2920,3744,0->2882,5311,2|26419|climbdown|hole",
		"2916,3747,0->2882,5311,2|26419|climbdown|hole",
		"2917,3749,0->2882,5311,2|26419|climbdown|hole",
		"2918,3749,0->2882,5311,2|26419|climbdown|hole",
		"2919,3749,0->2882,5311,2|26419|climbdown|hole",
		"2920,3749,0->2882,5311,2|26419|climbdown|hole",
		"2921,3748,0->2882,5311,2|26419|climbdown|hole",
		"2921,3747,0->2882,5311,2|26419|climbdown|hole",
		"2921,3746,0->2882,5311,2|26419|climbdown|hole");
	static final int ROPE_ITEM_ID = 954;
	static final String ATTACH_ROPE_ACTION = "Use rope";
	private static final Set<String> KALPHITE_ROPE_DESCENT_ROUTE_KEYS = Set.of(
		"3226,3108,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3226,3109,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3227,3110,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3228,3110,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3227,3107,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3228,3107,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3229,3109,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3229,3108,0->3483,9510,2|3827|climbdown|tunnel entrance",
		"3508,9498,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3508,9497,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3509,9496,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3510,9496,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3509,9499,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3510,9499,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3511,9497,2->3508,9493,0|23609|climbdown|tunnel entrance",
		"3511,9498,2->3508,9493,0|23609|climbdown|tunnel entrance");
	private static final Set<String> CRABCLAW_CAVES_DESCENT_ROUTE_KEYS = Set.of(
		"1673,9800,0->1677,9747,0|31692|climbdown|tunnel entrance",
		"1677,9747,0->1673,9800,0|31692|climbdown|tunnel entrance");
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
			|| transport.getCurrencyAmount() > 0)
		{
			return false;
		}
		if (isKalphiteRopeDescentRoute(transport))
		{
			return isKalphiteRopeSetup(transport) || isKalphiteInstalledDescent(transport);
		}
		if (!transport.getItemIdRequirements().isEmpty())
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
			|| NEYPOTZLI_ENTRANCE_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| DIRECT_CLIMB_UP_ROPE_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| DIRECT_CLIMB_DOWN_HOLE_ROUTE_KEYS.contains(routeKey(transport, action, name))
			|| isCrabclawCavesDescent(transport, action, name)
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

	static boolean isKalphiteRopeSetup(Transport transport)
	{
		return isKalphiteRopeDescentRoute(transport)
			&& transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(ROPE_ITEM_ID)))
			&& hasOnlyVarbit(transport, transport.getObjectId() == 3827 ? 4586 : 11705,
				0, TransportVarbit.Operator.EQUAL);
	}

	private static boolean isKalphiteInstalledDescent(Transport transport)
	{
		if (!isKalphiteRopeDescentRoute(transport) || transport.isConsumable()
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		return transport.getObjectId() == 3827
			? hasOnlyVarbit(transport, 4586, 1, TransportVarbit.Operator.EQUAL)
			: hasOnlyVarbit(transport, 11705, 0, TransportVarbit.Operator.GREATER_THAN);
	}

	static boolean isKalphiteRopeDescentRoute(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null)
		{
			return false;
		}
		return KALPHITE_ROPE_DESCENT_ROUTE_KEYS.contains(routeKey(transport,
			normalizeDirectAction(transport.getAction()), normalize(transport.getName())));
	}

	private static boolean isCrabclawCavesDescent(Transport transport, String action, String name)
	{
		return CRABCLAW_CAVES_DESCENT_ROUTE_KEYS.contains(routeKey(transport, action, name))
			&& !transport.isConsumable() && transport.getVarbits().isEmpty()
			&& transport.getQuests().equals(Map.of(Quest.THE_DEPTHS_OF_DESPAIR,
				QuestState.IN_PROGRESS));
	}

	private static boolean hasOnlyVarbit(Transport transport, int id, int value,
		TransportVarbit.Operator operator)
	{
		if (transport.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit requirement = transport.getVarbits().iterator().next();
		return requirement.getVarbitId() == id && requirement.getValue() == value
			&& requirement.getOperator() == operator;
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
