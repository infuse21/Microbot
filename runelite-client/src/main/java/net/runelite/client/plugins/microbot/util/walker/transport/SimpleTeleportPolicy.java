package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.leaguetransport.SeasonalTransportHandlers;

import java.util.Locale;
import java.util.Map;

/** Conservative ownership boundary for single-command, originless item and spell teleports. */
public final class SimpleTeleportPolicy
{
	private static final String GRAND_EXCHANGE = "varrock teleport: grand exchange";
	private static final String HOUSE_OUTSIDE = "teleport to house: outside";
	private static final String YANILLE = "watchtower teleport: yanille";
	private static final Map<WorldPoint, Integer> HOUSE_EXTERIORS = Map.of(
		new WorldPoint(2952, 3224, 0), 1,
		new WorldPoint(2892, 3465, 0), 2,
		new WorldPoint(3339, 3001, 0), 3,
		new WorldPoint(2669, 3629, 0), 4,
		new WorldPoint(2756, 3176, 0), 5,
		new WorldPoint(2545, 3097, 0), 6,
		new WorldPoint(3239, 6077, 0), 7,
		new WorldPoint(1740, 3517, 0), 8);

	private SimpleTeleportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getOrigin() != null
			|| transport.getDestination() == null || isBlank(transport.getDisplayInfo()))
		{
			return false;
		}
		String display = normalize(transport.getDisplayInfo());
		if (transport.getType() == TransportType.SEASONAL_TRANSPORT)
		{
			return SeasonalTransportHandlers.isAvailable(transport);
		}
		if (display.contains("master scroll book"))
		{
			return false;
		}
		if (transport.getType() == TransportType.TELEPORTATION_SPELL)
		{
			if (display.contains(":"))
			{
				return isAlternateDestinationSpell(transport);
			}
			return !display.contains("home teleport")
				|| isLumbridgeHomeTeleport(transport);
		}
		if (display.contains(":"))
		{
			return false;
		}
		return transport.getType() == TransportType.TELEPORTATION_ITEM
			&& transport.getItemIdRequirements() != null
			&& !transport.getItemIdRequirements().isEmpty();
	}

	public static boolean isLumbridgeHomeTeleport(Transport transport)
	{
		return transport != null
			&& transport.getType() == TransportType.TELEPORTATION_SPELL
			&& "lumbridge home teleport".equals(normalize(transport.getDisplayInfo()));
	}

	public static boolean isAlternateDestinationSpell(Transport transport)
	{
		if (!hasBaseAlternateSpellShape(transport))
		{
			return false;
		}
		String display = normalize(transport.getDisplayInfo());
		if (GRAND_EXCHANGE.equals(display))
		{
			return transport.getDestination().equals(new WorldPoint(3164, 3478, 0))
				&& !transport.isMembers() && transport.getDuration() == 4
				&& exactMagicLevel(transport, 25) && transport.getQuests().isEmpty()
				&& transport.getVarplayers().isEmpty()
				&& exactVarbits(transport, new int[][]{{4070, 0}, {4480, 1}, {4585, 1}});
		}
		if (HOUSE_OUTSIDE.equals(display))
		{
			Integer location = HOUSE_EXTERIORS.get(transport.getDestination());
			return location != null && transport.isMembers() && transport.getDuration() == 4
				&& exactMagicLevel(transport, 40) && transport.getQuests().isEmpty()
				&& transport.getVarplayers().isEmpty()
				&& exactVarbits(transport, new int[][]{{4070, 0}, {2187, location}});
		}
		return YANILLE.equals(display)
			&& transport.getDestination().equals(new WorldPoint(2584, 3097, 0))
			&& transport.isMembers() && transport.getDuration() == 10
			&& exactMagicLevel(transport, 58)
			&& transport.getQuests().equals(Map.of(Quest.WATCHTOWER, QuestState.FINISHED))
			&& exactVarbits(transport, new int[][]{{4070, 0}, {4460, 1}, {4548, 1}})
			&& exactVarplayers(transport, new int[][]{{212, 14}});
	}

	public static String spellName(Transport transport)
	{
		if (transport == null || transport.getDisplayInfo() == null)
		{
			return null;
		}
		String display = normalize(transport.getDisplayInfo());
		if (GRAND_EXCHANGE.equals(display)) return "Varrock Teleport";
		if (HOUSE_OUTSIDE.equals(display)) return "Teleport to House";
		if (YANILLE.equals(display)) return "Watchtower Teleport";
		return transport.getDisplayInfo().trim();
	}

	public static String spellOption(Transport transport)
	{
		String display = normalize(transport == null ? null : transport.getDisplayInfo());
		if (GRAND_EXCHANGE.equals(display)) return "grand exchange";
		if (HOUSE_OUTSIDE.equals(display)) return "outside";
		if (YANILLE.equals(display)) return "yanille";
		return "cast";
	}

	public static int spellIdentifier(Transport transport)
	{
		return isAlternateDestinationSpell(transport) ? 2 : 1;
	}

	private static boolean hasBaseAlternateSpellShape(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TELEPORTATION_SPELL
			&& transport.getOrigin() == null && transport.getDestination() != null
			&& transport.getMaxWildernessLevel() == 19 && !transport.isConsumable()
			&& transport.getCurrencyAmount() == 0 && transport.getItemIdRequirements().isEmpty();
	}

	private static boolean exactMagicLevel(Transport transport, int expected)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == Skill.MAGIC.ordinal() ? expected : 0))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean exactVarbits(Transport transport, int[][] expected)
	{
		if (transport.getVarbits().size() != expected.length) return false;
		for (int[] requirement : expected)
		{
			if (transport.getVarbits().stream().noneMatch(varbit ->
				varbit.getVarbitId() == requirement[0] && varbit.getValue() == requirement[1]
					&& varbit.getOperator() == TransportVarbit.Operator.EQUAL)) return false;
		}
		return true;
	}

	private static boolean exactVarplayers(Transport transport, int[][] expected)
	{
		if (transport.getVarplayers().size() != expected.length) return false;
		for (int[] requirement : expected)
		{
			if (transport.getVarplayers().stream().noneMatch(varplayer ->
				varplayer.getVarplayerId() == requirement[0] && varplayer.getValue() == requirement[1]
					&& varplayer.getOperator() == TransportVarPlayer.Operator.EQUAL)) return false;
		}
		return true;
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean isBlank(String value)
	{
		return value == null || value.trim().isEmpty();
	}
}
