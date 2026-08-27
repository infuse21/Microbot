package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Locale;

/** Conservative ownership and stage identity for non-POH spirit-tree routes. */
public final class SpiritTreePolicy
{
	public static final String DESTINATION_ACTION_PREFIX = "spirit-tree-destination:";
	static final int LOCKED_DESTINATION_TEXT_COLOR = 0x5f5f5f;

	private SpiritTreePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return isEligible(transport, PohPanel.getExitPortalTile());
	}

	static boolean isEligible(Transport transport, WorldPoint pohAnchor)
	{
		return transport != null
			&& transport.getType() == TransportType.SPIRIT_TREE
			&& transport.getOrigin() != null
			&& transport.getDestination() != null
			&& !transport.getOrigin().equals(transport.getDestination())
			&& transport.getObjectId() > 0
			&& "travel".equals(normalize(transport.getAction()))
			&& "spirit tree".equals(normalize(transport.getName()))
			&& !destinationName(transport.getDisplayInfo()).isEmpty()
			&& !"your house".equals(normalize(destinationName(transport.getDisplayInfo())))
			&& (pohAnchor == null || !pohAnchor.equals(transport.getOrigin())
				&& !pohAnchor.equals(transport.getDestination()));
	}

	public static String destinationAction(String destinationName)
	{
		return DESTINATION_ACTION_PREFIX + destinationName;
	}

	public static boolean isDestinationAction(String action)
	{
		return action != null && action.startsWith(DESTINATION_ACTION_PREFIX);
	}

	static boolean isDestinationSelectable(String widgetText, int textColor)
	{
		return textColor != LOCKED_DESTINATION_TEXT_COLOR
			&& (widgetText == null || !widgetText.toLowerCase(Locale.ROOT)
				.contains("<col=5f5f5f>"));
	}

	public static String destinationName(String displayInfo)
	{
		if (displayInfo == null)
		{
			return "";
		}
		return displayInfo.trim().replaceFirst("^[0-9A-Z]:\\s*", "").trim();
	}

	static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}
}
