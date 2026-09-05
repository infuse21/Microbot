package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Conservative catalog, live-object, and staged-action policy for River Lum canoes. */
public final class CanoePolicy
{
	public static final String SHAPE_ACTION_PREFIX = "canoe-shape:";
	public static final String DESTINATION_ACTION_PREFIX = "canoe-destination:";
	public static final String WARNING_CONTINUE_ACTION = "canoe-warning-continue";
	public static final String WARNING_CONFIRM_ACTION = "canoe-warning-confirm";
	public static final String ARRIVAL_ACTION_PREFIX = "canoe-arrival:";
	public static final String WARNING_TEXT = "Warning! This canoe will take you deep into the "
		+ "Wilderness. There are no trees suitable to make a canoe there. You will have to walk back.";
	public static final String WARNING_QUESTION = "Are you sure you wish to travel";
	public static final String WARNING_OPTION = "Yes, I'm brave.";
	private static final String CANOE_STATION = "canoe station";
	private static final String PADDLE_CANOE = "paddle canoe";
	private static final int LIVE_OBJECT_RADIUS = 3;
	private static final Set<Integer> AXE_ITEM_IDS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(1349, 1351, 1353, 1355, 1357, 1359, 1361)));
	private static final Set<Integer> WOODCUTTING_LEVELS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(12, 27, 42, 57)));
	private static final Set<String> DESTINATIONS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList("Barbarian Village", "Champions Guild", "Edgeville",
			"Ferox Enclave", "Lumbridge", "Wilderness Pond")));
	private static final Set<String> ARRIVAL_TEXTS = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList(
			"You arrive at Lumbridge.",
			"You arrive at the Champions' Guild.",
			"You arrive at Barbarian Village.",
			"You arrive at Edgeville.",
			"You arrive at Ferox Enclave.",
			"You arrive at Forax Enclave.",
			"You arrive in the Wilderness. There are no trees suitable to make a canoe.",
			"Your canoe sinks into the water after the hard journey.",
			"Looks like you're walking back.")));

	private CanoePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.CANOE
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getOrigin().getPlane() != 0 || transport.getDestination().getPlane() != 0
			|| transport.getObjectId() <= 0 || !PADDLE_CANOE.equals(normalize(transport.getAction()))
			|| !CANOE_STATION.equals(normalize(transport.getName()))
			|| !DESTINATIONS.contains(transport.getDisplayInfo())
			|| transport.getCurrencyAmount() != 0 || !transport.getQuests().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getItemIdRequirements() == null
			|| !AXE_ITEM_IDS.equals(transport.getItemIdRequirements().stream()
				.flatMap(Set::stream).collect(java.util.stream.Collectors.toSet())))
		{
			return false;
		}
		int required = requiredWoodcuttingLevel(transport);
		if (!WOODCUTTING_LEVELS.contains(required))
		{
			return false;
		}
		int[] skillLevels = transport.getSkillLevels();
		for (int i = 0; i < skillLevels.length; i++)
		{
			if (i != Skill.WOODCUTTING.ordinal() && skillLevels[i] != 0)
			{
				return false;
			}
		}
		return true;
	}

	public static boolean isLiveObjectMatch(Transport transport, int objectId,
		String name, List<String> actions, WorldPoint tile)
	{
		if (!isEligible(transport) || objectId != transport.getObjectId() || tile == null
			|| tile.getPlane() != transport.getOrigin().getPlane()
			|| tile.distanceTo2D(transport.getOrigin()) > LIVE_OBJECT_RADIUS
			|| !CANOE_STATION.equals(normalize(name)) || actions == null)
		{
			return false;
		}
		return actions.stream().anyMatch(CanoePolicy::isObjectAction);
	}

	public static boolean isObjectAction(String action)
	{
		String normalized = normalize(action);
		return "chop-down".equals(normalized) || "shape-canoe".equals(normalized)
			|| "float canoe".equals(normalized) || PADDLE_CANOE.equals(normalized);
	}

	public static String shapeForLevel(int woodcuttingLevel)
	{
		if (woodcuttingLevel >= 57)
		{
			return "Waka canoe";
		}
		if (woodcuttingLevel >= 42)
		{
			return "Stable dugout canoe";
		}
		if (woodcuttingLevel >= 27)
		{
			return "Dugout canoe";
		}
		return woodcuttingLevel >= 12 ? "Log canoe" : null;
	}

	public static String shapeAction(String shape)
	{
		return SHAPE_ACTION_PREFIX + shape;
	}

	public static String destinationAction(String destination)
	{
		return DESTINATION_ACTION_PREFIX + destination;
	}

	public static String arrivalAction(String dialogueText)
	{
		if (dialogueText == null)
		{
			return null;
		}
		String trimmed = dialogueText.trim();
		return ARRIVAL_TEXTS.contains(trimmed) ? ARRIVAL_ACTION_PREFIX + trimmed : null;
	}

	public static boolean isShapeAction(String action)
	{
		return action != null && action.startsWith(SHAPE_ACTION_PREFIX);
	}

	public static boolean isDestinationAction(String action)
	{
		return action != null && action.startsWith(DESTINATION_ACTION_PREFIX);
	}

	public static boolean isArrivalAction(String action)
	{
		return action != null && action.startsWith(ARRIVAL_ACTION_PREFIX);
	}

	public static boolean isUiStageAction(String action)
	{
		return isShapeAction(action) || isDestinationAction(action) || isArrivalAction(action)
			|| WARNING_CONTINUE_ACTION.equals(action) || WARNING_CONFIRM_ACTION.equals(action);
	}

	public static int requiredWoodcuttingLevel(Transport transport)
	{
		return transport == null ? 0 : transport.getSkillLevels()[Skill.WOODCUTTING.ordinal()];
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
