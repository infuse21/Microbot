package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;
import java.util.Set;

/** Conservative ownership contract for grouping-tab minigame teleports. */
public final class MinigameTeleportPolicy
{
	public static final String OPEN_GROUPING_TAB_ACTION = "minigame-open-grouping-tab";
	public static final String OPEN_GROUPING_ACTION = "minigame-open-grouping";
	public static final String OPEN_DROPDOWN_ACTION = "minigame-open-dropdown";
	public static final String WAIT_FOR_ACTIVITY_ACTION = "minigame-wait-for-activity";
	public static final String SELECT_ACTIVITY_PREFIX = "minigame-select-activity:";
	public static final String TELEPORT_ACTION = "minigame-teleport";
	public static final String SELECT_DESTINATION_PREFIX = "minigame-select-destination:";

	private static final Set<String> SUPPORTED_DESTINATIONS = Set.of(
		"barbarian assault", "blast furnace", "burthorpe games room", "castle wars",
		"clan wars", "fishing trawler", "giant's foundry", "guardians of the rift",
		"last man standing", "mage training arena", "nightmare zone", "pest control",
		"rat pits: ardougne", "rat pits: port sarim", "rat pits: varrock",
		"shades of mort'ton", "soul wars", "tithe farm", "trouble brewing",
		"tzhaar fight pit");

	private MinigameTeleportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.TELEPORTATION_MINIGAME
			&& transport.getOrigin() == null
			&& transport.getDestination() != null
			&& transport.getObjectId() <= 0
			&& isBlank(transport.getAction())
			&& isBlank(transport.getName())
			&& transport.getCurrencyAmount() == 0
			&& isBlank(transport.getCurrencyName())
			&& transport.getItemIdRequirements().isEmpty()
			&& SUPPORTED_DESTINATIONS.contains(normalize(transport.getDisplayInfo()));
	}

	public static String activityName(String displayInfo)
	{
		if (displayInfo == null)
		{
			return "";
		}
		int separator = displayInfo.indexOf(':');
		return (separator < 0 ? displayInfo : displayInfo.substring(0, separator)).trim();
	}

	public static String destinationOption(String displayInfo)
	{
		if (displayInfo == null)
		{
			return "";
		}
		int separator = displayInfo.indexOf(':');
		return separator < 0 ? "" : displayInfo.substring(separator + 1).trim();
	}

	public static String selectActivityAction(String activity)
	{
		return SELECT_ACTIVITY_PREFIX + activity;
	}

	public static String selectDestinationAction(String destination)
	{
		return SELECT_DESTINATION_PREFIX + destination;
	}

	public static boolean isSelectActivityAction(String action)
	{
		return action != null && action.startsWith(SELECT_ACTIVITY_PREFIX);
	}

	public static boolean isSelectDestinationAction(String action)
	{
		return action != null && action.startsWith(SELECT_DESTINATION_PREFIX);
	}

	public static boolean isTerminalAction(String action)
	{
		return TELEPORT_ACTION.equals(action) || isSelectDestinationAction(action);
	}

	public static boolean activityMatches(String expected, String observed)
	{
		return normalizeActivity(expected).equals(normalizeActivity(observed));
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}

	private static String normalizeActivity(String value)
	{
		return normalize(value).replace("'", "").replace("\u2019", "");
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
