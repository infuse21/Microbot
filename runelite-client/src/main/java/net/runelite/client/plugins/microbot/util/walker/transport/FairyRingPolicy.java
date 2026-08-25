package net.runelite.client.plugins.microbot.util.walker.transport;

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
			&& isCode(transport.getDisplayInfo())
			&& !"DIQ".equals(normalizeCode(transport.getDisplayInfo()))
			&& (pohAnchor == null || !pohAnchor.equals(transport.getOrigin())
				&& !pohAnchor.equals(transport.getDestination()));
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
			|| isRotateAction(action) || TELEPORT_ACTION.equals(action);
	}

	public static String rotateAction(int widgetId, int observedRotation)
	{
		return ROTATE_ACTION_PREFIX + widgetId + ":" + observedRotation;
	}

	public static boolean isRotateAction(String action)
	{
		return action != null && action.startsWith(ROTATE_ACTION_PREFIX);
	}

	public static int rotationWidgetId(String action)
	{
		if (!isRotateAction(action))
		{
			return -1;
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
}
