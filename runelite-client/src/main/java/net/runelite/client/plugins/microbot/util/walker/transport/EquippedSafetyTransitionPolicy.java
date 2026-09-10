package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Exact catalog contracts for transitions that require worn safety equipment. */
public final class EquippedSafetyTransitionPolicy
{
	static final String OPEN_INVENTORY = "safety-equipment:open-inventory";
	private static final String EQUIP_PREFIX = "safety-equipment:equip:";
	private static final int WAXED_SLED = 4084;
	private static final int CLIMBING_BOOTS = 3105;
	private static final Set<Integer> SMOKE_PROTECTION = Set.of(
		1506, 4164, 11864, 11865, 19639, 19641, 19643, 19645, 19647, 19649,
		21264, 21266, 21888, 21890, 23073, 23075, 24370, 24444,
		25177, 25179, 25181, 25183, 25185, 25187, 25189, 25191,
		25898, 25900, 25902, 25904, 25906, 25908, 25910, 25912, 25914,
		26674, 26675, 26676, 26677, 26678, 26679, 26680, 26681, 26682, 26683, 26684,
		29816, 29818, 29820, 29822, 33066, 33068, 33070, 33072,
		33338, 33340, 33439, 33441, 33443, 33445, 33447, 33449);
	private static final Set<String> SMOKE_DUNGEON_ROUTES = Set.of(
		"3310,2961,0->3206,9379,0|6279|climbdown|smokeywell",
		"3309,2962,0->3206,9379,0|6279|climbdown|smokeywell",
		"3311,2962,0->3206,9379,0|6279|climbdown|smokeywell",
		"3310,2963,0->3206,9379,0|6279|climbdown|smokeywell");
	private static final Set<String> TROLL_ROCK_ROUTES = Set.of(
		"2857,3611,0->2857,3613,0|3748|climb|rocks",
		"2856,3611,0->2856,3613,0|3748|climb|rocks");
	private static final Set<String> TROLLWEISS_SLOPE_ROUTES = Set.of(
		"2772,3836,0->2790,3794,0|5015|slide|slope",
		"2773,3836,0->2790,3794,0|5015|slide|slope",
		"2786,3772,0->2794,3719,0|5015|slide|slope",
		"2785,3772,0->2794,3719,0|5015|slide|slope");

	private EquippedSafetyTransitionPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport != null && KaruulmAccessPolicy.ownsObject(transport.getObjectId()))
			return KaruulmAccessPolicy.isEligible(transport) && !transport.getItemIdRequirements().isEmpty();
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| !transport.isMembers() || transport.isConsumable()
			|| transport.getCurrencyAmount() != 0 || !transport.getVarbits().isEmpty()
			|| !transport.getVarplayers().isEmpty())
		{
			return false;
		}

		String key = routeKey(transport);
		if (SMOKE_DUNGEON_ROUTES.contains(key))
		{
			return transport.getItemIdRequirements().isEmpty()
				&& transport.getQuests().equals(Map.of(Quest.DESERT_TREASURE_I,
					QuestState.IN_PROGRESS)) && noSkills(transport) && transport.getDuration() == 0;
		}
		if (TROLL_ROCK_ROUTES.contains(key))
		{
			return transport.getItemIdRequirements().equals(Set.of(Set.of(CLIMBING_BOOTS)))
				&& transport.getQuests().equals(Map.of(Quest.TROLL_STRONGHOLD,
					QuestState.IN_PROGRESS)) && onlyAgility(transport, 15)
				&& transport.getDuration() == 1;
		}
		if (TROLLWEISS_SLOPE_ROUTES.contains(key))
		{
			int duration = transport.getDestination().getY() == 3794 ? 18 : 30;
			return transport.getItemIdRequirements().equals(Set.of(Set.of(WAXED_SLED)))
				&& transport.getQuests().equals(Map.of(Quest.TROLL_ROMANCE, QuestState.FINISHED))
				&& noSkills(transport) && transport.getDuration() == duration;
		}
		return false;
	}

	public static Set<Integer> requiredEquipmentIds(Transport transport)
	{
		if (!isEligible(transport))
		{
			return Collections.emptySet();
		}
		if (transport.getObjectId() == 6279)
		{
			return SMOKE_PROTECTION;
		}
		Set<Integer> itemIds = new LinkedHashSet<>();
		transport.getItemIdRequirements().forEach(itemIds::addAll);
		return Collections.unmodifiableSet(itemIds);
	}

	static String equipAction(int itemId)
	{
		return EQUIP_PREFIX + itemId;
	}

	static int equipmentItemId(String action)
	{
		if (action == null || !action.startsWith(EQUIP_PREFIX))
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(action.substring(EQUIP_PREFIX.length()));
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}

	static boolean requiresExactLanding(int objectId,
		net.runelite.api.coords.WorldPoint origin,
		net.runelite.api.coords.WorldPoint destination)
	{
		if (origin == null || destination == null)
		{
			return false;
		}
		String prefix = point(origin) + "->" + point(destination) + "|" + objectId + "|";
		return SMOKE_DUNGEON_ROUTES.stream().anyMatch(route -> route.startsWith(prefix))
			|| TROLL_ROCK_ROUTES.stream().anyMatch(route -> route.startsWith(prefix))
			|| TROLLWEISS_SLOPE_ROUTES.stream().anyMatch(route -> route.startsWith(prefix));
	}

	private static boolean noSkills(Transport transport)
	{
		return Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0);
	}

	private static boolean onlyAgility(Transport transport, int level)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == Skill.AGILITY.ordinal() ? level : 0))
			{
				return false;
			}
		}
		return true;
	}

	private static String routeKey(Transport transport)
	{
		return point(transport.getOrigin()) + "->" + point(transport.getDestination()) + "|"
			+ transport.getObjectId() + "|" + normalize(transport.getAction()) + "|"
			+ normalize(transport.getName());
	}

	private static String point(net.runelite.api.coords.WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
			.replace("-", "").replace(" ", "");
	}
}
