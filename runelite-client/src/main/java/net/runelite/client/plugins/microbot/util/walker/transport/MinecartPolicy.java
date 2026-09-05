package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/** Conservative catalog identity and encoded stages for minecart travel. */
public final class MinecartPolicy
{
	public static final String UNEQUIP_WEAPON_ACTION_PREFIX =
		"minecart-unequip-weapon:";
	public static final String UNEQUIP_SHIELD_ACTION_PREFIX =
		"minecart-unequip-shield:";
	public static final String OBJECT_ACTION_PREFIX = "minecart-object:";
	public static final String DESTINATION_ACTION_PREFIX = "minecart-destination:";
	public static final String RESTORE_OPEN_ACTION_PREFIX = "minecart-restore-open:";
	public static final String RESTORE_WEAPON_ACTION_PREFIX =
		"minecart-restore-weapon:";
	public static final String RESTORE_SHIELD_ACTION_PREFIX =
		"minecart-restore-shield:";
	public static final String UNAVAILABLE_ACTION = "minecart-unavailable";
	public static final int LIVE_OBJECT_ORIGIN_TOLERANCE = 4;

	private static final int GE_TRAPDOOR = 16168;
	private static final int KELDAGRIM_CART = 7028;
	private static final int ICE_MOUNTAIN_CART = 7029;
	private static final int WHITE_WOLF_CART = 7030;
	private static final int KOUREND_CART = 28835;
	private static final Set<String> KOUREND_DESTINATIONS = Set.of(
		"1: Arceuus", "2: Farming Guild", "3: Hosidius South",
		"4: Hosidius West", "5: Kingstown", "6: Kourend Woodland",
		"7: Lovakengj", "8: Mount Quidamortem", "9: Northern Tundras",
		"A: Port Piscarilius", "B: Shayzien East", "C: Shayzien West");

	private MinecartPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.MINECART
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getOrigin().equals(transport.getDestination())
			|| transport.getOrigin().getPlane() != 0
			|| transport.getDestination().getPlane() != 0
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		switch (transport.getObjectId())
		{
			case GE_TRAPDOOR:
				return row(transport, "Travel", "Trapdoor", "", 0);
			case KELDAGRIM_CART:
				return row(transport, "Ride", "Train cart", "", 0)
					|| row(transport, "Ride", "Train cart", "", 100)
					|| row(transport, "Ride", "Train cart", "", 150);
			case ICE_MOUNTAIN_CART:
				return row(transport, "Ride", "Train cart", "", 150);
			case WHITE_WOLF_CART:
				return row(transport, "Ride", "Train cart", "", 100);
			case KOUREND_CART:
				return "Travel".equals(transport.getAction())
					&& "Minecart".equals(transport.getName())
					&& KOUREND_DESTINATIONS.contains(transport.getDisplayInfo())
					&& currencyMatches(transport, 20);
			default:
				return false;
		}
	}

	public static boolean isTrainCart(Transport transport)
	{
		return isEligible(transport)
			&& "Train cart".equalsIgnoreCase(transport.getName());
	}

	public static boolean hasDestinationMenu(Transport transport)
	{
		return isEligible(transport) && transport.getObjectId() == KOUREND_CART;
	}

	public static boolean isLiveObjectMatch(Transport transport, int liveId,
		String liveName, Collection<String> liveActions, WorldPoint liveTile)
	{
		return isEligible(transport) && liveId == transport.getObjectId()
			&& liveName != null && liveName.equalsIgnoreCase(transport.getName())
			&& exactAction(liveActions, transport.getAction()) != null
			&& liveTile != null
			&& liveTile.getPlane() == transport.getOrigin().getPlane()
			&& liveTile.distanceTo2D(transport.getOrigin())
				<= LIVE_OBJECT_ORIGIN_TOLERANCE;
	}

	public static String exactAction(Collection<String> actions, String expected)
	{
		if (actions == null || expected == null)
		{
			return null;
		}
		return actions.stream().filter(java.util.Objects::nonNull)
			.filter(action -> action.equalsIgnoreCase(expected)).findFirst().orElse(null);
	}

	public static String unequipWeaponAction(int weaponId, int shieldId)
	{
		return equipmentAction(UNEQUIP_WEAPON_ACTION_PREFIX, weaponId, shieldId);
	}

	public static String unequipShieldAction(int weaponId, int shieldId)
	{
		return equipmentAction(UNEQUIP_SHIELD_ACTION_PREFIX, weaponId, shieldId);
	}

	public static String objectAction(String liveAction, int weaponId, int shieldId)
	{
		return payloadAction(OBJECT_ACTION_PREFIX, liveAction, weaponId, shieldId);
	}

	public static String destinationAction(String displayInfo, int weaponId, int shieldId)
	{
		return payloadAction(DESTINATION_ACTION_PREFIX, displayInfo, weaponId, shieldId);
	}

	public static String restoreOpenAction(int weaponId, int shieldId)
	{
		return equipmentAction(RESTORE_OPEN_ACTION_PREFIX, weaponId, shieldId);
	}

	public static String restoreWeaponAction(int weaponId, int shieldId)
	{
		return equipmentAction(RESTORE_WEAPON_ACTION_PREFIX, weaponId, shieldId);
	}

	public static String restoreShieldAction(int weaponId, int shieldId)
	{
		return equipmentAction(RESTORE_SHIELD_ACTION_PREFIX, weaponId, shieldId);
	}

	public static boolean isUnequipWeaponAction(String action)
	{
		return hasPrefix(action, UNEQUIP_WEAPON_ACTION_PREFIX);
	}

	public static boolean isUnequipShieldAction(String action)
	{
		return hasPrefix(action, UNEQUIP_SHIELD_ACTION_PREFIX);
	}

	public static boolean isObjectAction(String action)
	{
		return hasPrefix(action, OBJECT_ACTION_PREFIX);
	}

	public static boolean isDestinationAction(String action)
	{
		return hasPrefix(action, DESTINATION_ACTION_PREFIX);
	}

	public static boolean isRestoreOpenAction(String action)
	{
		return hasPrefix(action, RESTORE_OPEN_ACTION_PREFIX);
	}

	public static boolean isRestoreWeaponAction(String action)
	{
		return hasPrefix(action, RESTORE_WEAPON_ACTION_PREFIX);
	}

	public static boolean isRestoreShieldAction(String action)
	{
		return hasPrefix(action, RESTORE_SHIELD_ACTION_PREFIX);
	}

	public static boolean hasEncodedEquipment(String action)
	{
		return parse(action) != null;
	}

	public static int originalWeaponId(String action)
	{
		ParsedAction parsed = parse(action);
		return parsed == null ? -1 : parsed.weaponId;
	}

	public static int originalShieldId(String action)
	{
		ParsedAction parsed = parse(action);
		return parsed == null ? -1 : parsed.shieldId;
	}

	public static String objectLiveAction(String action)
	{
		ParsedAction parsed = parse(action);
		return isObjectAction(action) && parsed != null ? parsed.payload : null;
	}

	public static String destinationDisplayInfo(String action)
	{
		ParsedAction parsed = parse(action);
		return isDestinationAction(action) && parsed != null ? parsed.payload : null;
	}

	public static String destinationLabel(String displayInfo)
	{
		return displayInfo == null ? "" : displayInfo.replaceFirst(
			"^[0-9A-C]:\\s*", "").trim();
	}

	private static boolean row(Transport transport, String action, String name,
		String displayInfo, int currency)
	{
		return action.equals(transport.getAction()) && name.equals(transport.getName())
			&& (displayInfo.isEmpty()
				? normalize(transport.getDisplayInfo()).isEmpty()
				: displayInfo.equals(transport.getDisplayInfo()))
			&& currencyMatches(transport, currency);
	}

	private static boolean currencyMatches(Transport transport, int amount)
	{
		if (transport.getCurrencyAmount() != amount)
		{
			return false;
		}
		String currency = normalize(transport.getCurrencyName());
		return amount == 0 ? currency.isEmpty() : "coins".equals(currency);
	}

	private static String equipmentAction(String prefix, int weaponId, int shieldId)
	{
		return prefix + weaponId + ":" + shieldId;
	}

	private static String payloadAction(String prefix, String payload,
		int weaponId, int shieldId)
	{
		return prefix + payload + ":" + weaponId + ":" + shieldId;
	}

	private static boolean hasPrefix(String action, String prefix)
	{
		return action != null && action.startsWith(prefix) && parse(action) != null;
	}

	private static ParsedAction parse(String action)
	{
		String prefix = prefix(action);
		if (prefix == null)
		{
			return null;
		}
		int shieldSeparator = action.lastIndexOf(':');
		int weaponSeparator = action.lastIndexOf(':', shieldSeparator - 1);
		if (weaponSeparator < prefix.length() - 1 || shieldSeparator <= weaponSeparator)
		{
			return null;
		}
		try
		{
			int weaponId = Integer.parseInt(action.substring(
				weaponSeparator + 1, shieldSeparator));
			int shieldId = Integer.parseInt(action.substring(shieldSeparator + 1));
			String payload = weaponSeparator < prefix.length() ? ""
				: action.substring(prefix.length(), weaponSeparator);
			return new ParsedAction(payload,
				weaponId, shieldId);
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}

	private static String prefix(String action)
	{
		if (action == null)
		{
			return null;
		}
		String[] prefixes = {
			UNEQUIP_WEAPON_ACTION_PREFIX, UNEQUIP_SHIELD_ACTION_PREFIX,
			OBJECT_ACTION_PREFIX, DESTINATION_ACTION_PREFIX,
			RESTORE_OPEN_ACTION_PREFIX, RESTORE_WEAPON_ACTION_PREFIX,
			RESTORE_SHIELD_ACTION_PREFIX
		};
		for (String prefix : prefixes)
		{
			if (action.startsWith(prefix))
			{
				return prefix;
			}
		}
		return null;
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	private static final class ParsedAction
	{
		private final String payload;
		private final int weaponId;
		private final int shieldId;

		private ParsedAction(String payload, int weaponId, int shieldId)
		{
			this.payload = payload;
			this.weaponId = weaponId;
			this.shieldId = shieldId;
		}
	}
}
