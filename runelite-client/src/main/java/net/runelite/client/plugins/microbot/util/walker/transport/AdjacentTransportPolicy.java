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
	private static final Set<String> WIDE_GATE_ROUTES = Set.of(
		"2461,3385,0->2461,3382,0|190", "2461,3382,0->2461,3385,0|190",
		"2462,3385,0->2461,3382,0|190", "2460,3382,0->2461,3385,0|190",
		"2460,3385,0->2461,3382,0|190", "2462,3382,0->2461,3385,0|190",
		"2343,3661,0->2343,3663,0|12723", "2343,3663,0->2343,3661,0|12723",
		"2344,3661,0->2344,3663,0|12725", "2344,3663,0->2344,3661,0|12725",
		"2559,3300,0->2556,3300,0|8739", "2556,3300,0->2559,3300,0|8738",
		"2559,3299,0->2556,3299,0|8738", "2556,3299,0->2559,3299,0|8739");
	private static final Set<String> DRAYNOR_DOOR_CONTRACTS = Set.of(
		"3108,9757,0->3108,9759,0|144|1788=1;1789=1;1790=0;1793=0",
		"3108,9759,0->3108,9757,0|144|1788=1;1789=1;1790=0;1793=0",
		"3104,9760,0->3106,9760,0|139|1788=1;1790=0;1792=0;1793=0",
		"3106,9760,0->3104,9760,0|139|1788=1;1790=0;1792=0;1793=0",
		"3102,9757,0->3102,9759,0|145|1791=1;1792=0",
		"3102,9759,0->3102,9757,0|145|1791=1;1792=0",
		"3099,9760,0->3101,9760,0|140|1791=1;1792=0",
		"3101,9760,0->3099,9760,0|140|1791=1;1792=0",
		"3097,9762,0->3097,9764,0|143|1789=0;1791=1;1793=0",
		"3097,9764,0->3097,9762,0|143|1789=0;1791=1;1793=0",
		"3099,9765,0->3101,9765,0|138|1789=0;1791=1;1793=1",
		"3101,9765,0->3099,9765,0|138|1789=0;1791=1;1793=1",
		"3104,9765,0->3106,9765,0|137|1788=0;1792=1;1793=1",
		"3106,9765,0->3104,9765,0|137|1788=0;1792=1;1793=1",
		"3102,9762,0->3102,9764,0|142|1792=0;1793=1",
		"3102,9764,0->3102,9762,0|142|1792=0;1793=1",
		"3099,9755,0->3101,9755,0|141|1790=1;1791=1;1793=1",
		"3101,9755,0->3099,9755,0|141|1790=1;1791=1;1793=1");
	private static final Set<String> DRAYNOR_BOOKCASE_ROUTES = Set.of(
		"3098,3359,0->3096,3359,0|156",
		"3098,3358,0->3096,3358,0|155",
		"3097,3360,0->3096,3359,0|156",
		"3097,3357,0->3096,3358,0|155");
	private static final Set<String> EAST_ARDOUGNE_PICKLOCK_ROUTES = Set.of(
		"2674,3304,0->2674,3303,0|11720",
		"2674,3305,0->2674,3306,0|11719");
	private static final Set<String> HAUNTED_MINE_CART_ROUTES = Set.of(
		"3446,3236,0->3444,3236,0|4918",
		"3444,3236,0->3446,3236,0|4918");
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
	private static final Set<String> EDGEVILLE_ODD_WALL_ROUTES = Set.of(
		"3094,9895,0->3093,9895,0|1736", "3093,9895,0->3094,9895,0|1736",
		"3094,9896,0->3093,9896,0|1734", "3093,9896,0->3094,9896,0|1734");
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
		if (transport.getObjectId() >= 137 && transport.getObjectId() <= 145)
		{
			return isDraynorBasementDoor(transport);
		}
		String action = transport.getAction().toLowerCase(Locale.ROOT);
		if (isEdgevilleOddWall(transport)) return true;
		if (isDraynorBookcase(transport)) return true;
		if (transport.getObjectId() == 4918) return isHauntedMineCart(transport);
		if ("pick-lock".equals(action))
		{
			return isYanillePickLockDoor(transport) || isEastArdougnePickLockDoor(transport);
		}
		if (isWideGate(transport)) return true;
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

	static boolean isEdgevilleOddWall(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null)
		{
			return false;
		}
		return transport.getType() == TransportType.TRANSPORT
			&& "push".equals(normalize(transport.getAction()))
			&& "odd looking wall".equals(normalize(transport.getName()))
			&& !transport.isMembers() && !transport.isConsumable()
			&& transport.getDuration() == 0 && transport.getCurrencyAmount() == 0
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getQuests().isEmpty() && transport.getVarbits().isEmpty()
			&& transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& EDGEVILLE_ODD_WALL_ROUTES.contains(pointKey(transport.getOrigin()) + "->"
				+ pointKey(transport.getDestination()) + "|" + transport.getObjectId());
	}

	static boolean isWideGate(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getType() != TransportType.TRANSPORT || !"open".equals(normalize(transport.getAction()))
			|| transport.isConsumable() || transport.getCurrencyAmount() != 0
			|| !transport.getItemIdRequirements().isEmpty() || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty()) return false;
		boolean stronghold = transport.getObjectId() == 190;
		boolean ardougne = transport.getObjectId() == 8738 || transport.getObjectId() == 8739;
		return (stronghold ? "gate" : ardougne ? "ardougne wall door" : "colony gate")
			.equals(normalize(transport.getName()))
			&& transport.getQuests().equals(stronghold ? java.util.Collections.emptyMap()
				: java.util.Map.of(ardougne ? net.runelite.api.Quest.BIOHAZARD : net.runelite.api.Quest.SWAN_SONG,
					net.runelite.api.QuestState.FINISHED))
			&& WIDE_GATE_ROUTES.contains(pointKey(transport.getOrigin()) + "->"
				+ pointKey(transport.getDestination()) + "|" + transport.getObjectId());
	}

	static boolean isYanillePickLockDoor(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null)
		{
			return false;
		}
		return transport.getType() == TransportType.TRANSPORT && transport.getObjectId() == 11728
			&& "pick-lock".equals(normalize(transport.getAction()))
			&& "door".equals(normalize(transport.getName())) && !transport.isConsumable()
			&& transport.getCurrencyAmount() == 0 && transport.getDuration() == 10
			&& transport.getSkillLevels()[net.runelite.api.Skill.THIEVING.ordinal()] == 82
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(1523)))
			&& transport.getQuests().isEmpty() && transport.getVarbits().isEmpty()
			&& transport.getVarplayers().isEmpty()
			&& Set.of("2601,9481,0->2601,9482,0", "2601,9482,0->2601,9481,0")
				.contains(pointKey(transport.getOrigin()) + "->" + pointKey(transport.getDestination()));
	}

	static boolean hasRequiredYanillePickLockItemsAndLevel(Transport transport, int thieving, boolean lockpick)
	{
		return isYanillePickLockDoor(transport) && thieving >= 82 && lockpick;
	}

	static boolean isEastArdougnePickLockDoor(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getType() != TransportType.TRANSPORT || !transport.isMembers()
			|| !"pick-lock".equals(normalize(transport.getAction()))
			|| !"door".equals(normalize(transport.getName())) || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || transport.getDuration() != 2
			|| !transport.getItemIdRequirements().isEmpty() || !transport.getQuests().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty())
		{
			return false;
		}
		int required = transport.getObjectId() == 11720 ? 16 : 0;
		return transport.getSkillLevels()[net.runelite.api.Skill.THIEVING.ordinal()] == required
			&& java.util.stream.IntStream.range(0, transport.getSkillLevels().length)
				.filter(index -> index != net.runelite.api.Skill.THIEVING.ordinal())
				.allMatch(index -> transport.getSkillLevels()[index] == 0)
			&& EAST_ARDOUGNE_PICKLOCK_ROUTES.contains(pointKey(transport.getOrigin()) + "->"
				+ pointKey(transport.getDestination()) + "|" + transport.getObjectId());
	}

	static boolean hasRequiredEastArdougneThieving(Transport transport, int thieving)
	{
		return isEastArdougnePickLockDoor(transport)
			&& thieving >= transport.getSkillLevels()[net.runelite.api.Skill.THIEVING.ordinal()];
	}

	static boolean isHauntedMineCart(Transport transport)
	{
		return transport != null && transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getType() == TransportType.TRANSPORT && transport.isMembers()
			&& !transport.isConsumable() && transport.getCurrencyAmount() == 0
			&& transport.getDuration() == 1 && transport.getObjectId() == 4918
			&& "climb-over".equals(normalize(transport.getAction()))
			&& "mine cart".equals(normalize(transport.getName()))
			&& transport.getItemIdRequirements().isEmpty()
			&& transport.getQuests().equals(java.util.Map.of(
				net.runelite.api.Quest.PRIEST_IN_PERIL, net.runelite.api.QuestState.FINISHED))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& transport.getSkillLevels()[net.runelite.api.Skill.AGILITY.ordinal()] == 15
			&& java.util.stream.IntStream.range(0, transport.getSkillLevels().length)
				.filter(index -> index != net.runelite.api.Skill.AGILITY.ordinal())
				.allMatch(index -> transport.getSkillLevels()[index] == 0)
			&& HAUNTED_MINE_CART_ROUTES.contains(pointKey(transport.getOrigin()) + "->"
				+ pointKey(transport.getDestination()) + "|" + transport.getObjectId());
	}

	static boolean hasRequiredHauntedMineCartAgility(Transport transport, int agility)
	{
		return isHauntedMineCart(transport) && agility >= 15;
	}

	static boolean isDraynorBookcase(Transport transport)
	{
		return transport != null && transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getType() == TransportType.TRANSPORT && !transport.isMembers()
			&& !transport.isConsumable() && transport.getCurrencyAmount() == 0
			&& transport.getDuration() == 3 && "search".equals(normalize(transport.getAction()))
			&& "bookcase".equals(normalize(transport.getName()))
			&& transport.getItemIdRequirements().isEmpty() && transport.getQuests().isEmpty()
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& DRAYNOR_BOOKCASE_ROUTES.contains(pointKey(transport.getOrigin()) + "->"
				+ pointKey(transport.getDestination()) + "|" + transport.getObjectId());
	}

	static boolean isDraynorBasementDoor(Transport transport)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getType() != TransportType.TRANSPORT || !"open".equals(normalize(transport.getAction()))
			|| !"door".equals(normalize(transport.getName())) || transport.getCurrencyAmount() != 0
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getQuests().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 1
			|| !"Draynor basement puzzle door".equals(transport.getDisplayInfo())
			|| transport.getVarbits().stream().anyMatch(gate -> gate.getOperator()
				!= net.runelite.client.plugins.microbot.shortestpath.TransportVarbit.Operator.EQUAL))
		{
			return false;
		}
		String gates = transport.getVarbits().stream()
			.map(gate -> gate.getVarbitId() + "=" + gate.getValue()).sorted()
			.collect(java.util.stream.Collectors.joining(";"));
		return DRAYNOR_DOOR_CONTRACTS.contains(pointKey(transport.getOrigin()) + "->"
			+ pointKey(transport.getDestination()) + "|" + transport.getObjectId() + "|" + gates);
	}

	static boolean hasRequiredDraynorLevers(Transport transport, java.util.function.IntUnaryOperator values)
	{
		return isDraynorBasementDoor(transport) && transport.getVarbits().stream()
			.allMatch(gate -> gate.matches(values.applyAsInt(gate.getVarbitId())));
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
			|| normalized.equals("slash") || normalized.equals("pick-lock")
			|| normalized.equals("search");
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
