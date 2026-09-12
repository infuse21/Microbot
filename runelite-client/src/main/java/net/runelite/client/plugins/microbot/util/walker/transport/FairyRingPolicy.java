package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;

/** Conservative ownership and stage identity for the three-dial fairy-ring interface. */
public final class FairyRingPolicy
{
	public static final String EQUIP_ACTION_PREFIX = "fairy-ring-equip:";
	public static final String RESTORE_OPEN_ACTION_PREFIX = "fairy-ring-restore-open:";
	public static final String RESTORE_ACTION_PREFIX = "fairy-ring-restore:";
	public static final String ROTATE_ACTION_PREFIX = "fairy-ring-rotate:";
	public static final String TELEPORT_ACTION = "fairy-ring-teleport";
	public static final String HIDEOUT_SEQUENCE = "AIR DLR DJQ AJS";
	public static final WorldPoint HIDEOUT_DESTINATION = new WorldPoint(2328, 4426, 0);
	private static final String SEQUENCE_OBJECT_PREFIX = "fairy-ring-sequence-object:";
	private static final String SEQUENCE_ROTATE_PREFIX = "fairy-ring-sequence-rotate:";
	private static final String SEQUENCE_TELEPORT_PREFIX = "fairy-ring-sequence-teleport:";
	private static final String[] HIDEOUT_CODES = {"AIR", "DLR", "DJQ", "AJS"};
	private static final WorldPoint[] HIDEOUT_LANDINGS = {
		new WorldPoint(2700, 3247, 0),
		new WorldPoint(2213, 3099, 0),
		new WorldPoint(2213, 3099, 0),
		HIDEOUT_DESTINATION
	};

	private FairyRingPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return isEligible(transport, PohPanel.getExitPortalTile());
	}

	static boolean isEligible(Transport transport, WorldPoint pohAnchor)
	{
		return transport != null
			&& transport.getType() == TransportType.FAIRY_RING
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& ((isCode(transport.getDisplayInfo())
				&& !"DIQ".equals(normalizeCode(transport.getDisplayInfo())))
				|| isHideoutSequence(transport))
			&& (pohAnchor == null || !pohAnchor.equals(transport.getDestination()));
	}

	public static boolean isHideoutSequence(Transport transport)
	{
		return transport != null
			&& HIDEOUT_SEQUENCE.equals(normalizeCode(transport.getDisplayInfo()))
			&& HIDEOUT_DESTINATION.equals(transport.getDestination())
			&& QuestState.FINISHED.equals(transport.getQuests().get(
				Quest.FAIRYTALE_II__CURE_A_QUEEN));
	}

	public static boolean isCode(String value)
	{
		String code = normalizeCode(value);
		return code.length() == 3
			&& "ABCD".indexOf(code.charAt(0)) >= 0
			&& "IJKL".indexOf(code.charAt(1)) >= 0
			&& "PQRS".indexOf(code.charAt(2)) >= 0;
	}

	public static String equipAction(int itemId)
	{
		return EQUIP_ACTION_PREFIX + itemId;
	}

	public static boolean isEquipAction(String action)
	{
		return action != null && action.startsWith(EQUIP_ACTION_PREFIX);
	}

	public static int equipItemId(String action)
	{
		return parseTrailingInt(action, EQUIP_ACTION_PREFIX);
	}

	public static String restoreAction(int itemId)
	{
		return RESTORE_ACTION_PREFIX + itemId;
	}

	public static String restoreOpenAction(int itemId)
	{
		return RESTORE_OPEN_ACTION_PREFIX + itemId;
	}

	public static boolean isRestoreOpenAction(String action)
	{
		return action != null && action.startsWith(RESTORE_OPEN_ACTION_PREFIX);
	}

	public static int restoreOpenItemId(String action)
	{
		return parseTrailingInt(action, RESTORE_OPEN_ACTION_PREFIX);
	}

	public static boolean isRestoreAction(String action)
	{
		return action != null && action.startsWith(RESTORE_ACTION_PREFIX);
	}

	public static int restoreItemId(String action)
	{
		return parseTrailingInt(action, RESTORE_ACTION_PREFIX);
	}

	public static boolean isStageAction(String action)
	{
		return isEquipAction(action) || isRestoreOpenAction(action) || isRestoreAction(action)
			|| isRotateAction(action) || TELEPORT_ACTION.equals(action)
			|| isSequenceTeleportAction(action);
	}

	public static String rotateAction(int widgetId, int observedRotation)
	{
		return ROTATE_ACTION_PREFIX + widgetId + ":" + observedRotation;
	}

	public static boolean isRotateAction(String action)
	{
		return action != null && (action.startsWith(ROTATE_ACTION_PREFIX)
			|| action.startsWith(SEQUENCE_ROTATE_PREFIX));
	}

	public static int rotationWidgetId(String action)
	{
		if (!isRotateAction(action))
		{
			return -1;
		}
		if (action.startsWith(SEQUENCE_ROTATE_PREFIX))
		{
			String[] parts = action.substring(SEQUENCE_ROTATE_PREFIX.length()).split(":", 3);
			return parts.length == 3 ? parseInt(parts[1]) : -1;
		}
		int separator = action.indexOf(':', ROTATE_ACTION_PREFIX.length());
		if (separator < 0)
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(action.substring(ROTATE_ACTION_PREFIX.length(), separator));
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}

	public static String sequenceObjectAction(int step, String liveAction)
	{
		return SEQUENCE_OBJECT_PREFIX + step + ":" + liveAction;
	}

	public static String sequenceObjectLiveAction(String action)
	{
		if (action == null || !action.startsWith(SEQUENCE_OBJECT_PREFIX))
		{
			return null;
		}
		int separator = action.indexOf(':', SEQUENCE_OBJECT_PREFIX.length());
		return separator < 0 ? null : action.substring(separator + 1);
	}

	public static String sequenceRotateAction(int step, int widgetId, int observedRotation)
	{
		return SEQUENCE_ROTATE_PREFIX + step + ":" + widgetId + ":" + observedRotation;
	}

	public static String sequenceTeleportAction(int step)
	{
		return SEQUENCE_TELEPORT_PREFIX + step;
	}

	public static boolean isSequenceTeleportAction(String action)
	{
		int step = sequenceStep(action);
		return action != null && action.startsWith(SEQUENCE_TELEPORT_PREFIX)
			&& step >= 0 && step < HIDEOUT_CODES.length;
	}

	public static int sequenceStep(String action)
	{
		if (action == null)
		{
			return -1;
		}
		for (String prefix : new String[]{SEQUENCE_OBJECT_PREFIX,
			SEQUENCE_ROTATE_PREFIX, SEQUENCE_TELEPORT_PREFIX})
		{
			if (!action.startsWith(prefix))
			{
				continue;
			}
			String suffix = action.substring(prefix.length());
			int separator = suffix.indexOf(':');
			return parseInt(separator < 0 ? suffix : suffix.substring(0, separator));
		}
		return -1;
	}

	public static String sequenceCode(int step)
	{
		return step >= 0 && step < HIDEOUT_CODES.length ? HIDEOUT_CODES[step] : "";
	}

	public static WorldPoint sequenceLanding(int step)
	{
		return step >= 0 && step < HIDEOUT_LANDINGS.length ? HIDEOUT_LANDINGS[step] : null;
	}

	public static WorldPoint sequenceRingAnchor(WorldPoint origin, int step)
	{
		if (step <= 0)
		{
			return origin;
		}
		return step == 1 ? HIDEOUT_LANDINGS[0] : HIDEOUT_LANDINGS[1];
	}

	public static boolean sequenceTeleportCompleted(int step, WorldPoint player,
		boolean interfaceVisible)
	{
		WorldPoint landing = sequenceLanding(step);
		return step >= 0 && step < HIDEOUT_CODES.length - 1 && !interfaceVisible
			&& player != null && landing != null && player.getPlane() == landing.getPlane()
			&& player.distanceTo2D(landing) <= 3;
	}

	public static int desiredRotation(char letter)
	{
		switch (Character.toUpperCase(letter))
		{
			case 'A':
			case 'I':
			case 'P':
				return 0;
			case 'B':
			case 'J':
			case 'Q':
				return 512;
			case 'C':
			case 'K':
			case 'R':
				return 1024;
			case 'D':
			case 'L':
			case 'S':
				return 1536;
			default:
				return -1;
		}
	}

	public static String normalizeCode(String value)
	{
		return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
	}

	private static int parseTrailingInt(String action, String prefix)
	{
		if (action == null || !action.startsWith(prefix))
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(action.substring(prefix.length()));
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}

	private static int parseInt(String value)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ignored)
		{
			return -1;
		}
	}
}
