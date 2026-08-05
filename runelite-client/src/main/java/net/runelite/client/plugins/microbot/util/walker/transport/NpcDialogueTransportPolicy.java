package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Conservative eligibility and stage identity for chat-dialogue NPC/ship/boat rows: free
 * rows whose declared {@code Display info} names the destination option, and coin-paid
 * rows whose fare is confirmed through an affirmative chat option ({@code Display info}
 * optional for single-destination paid flows). Talk-to conversations, non-coin currencies,
 * and item-gated rows remain legacy-owned.
 */
public final class NpcDialogueTransportPolicy
{
	public static final String CONTINUE_ACTION = "dialogue-continue";
	public static final String CONFIRM_ACTION = "dialogue-confirm";
	public static final String DESTINATION_ACTION_PREFIX = "dialogue-destination:";
	/** Boat/actor objects anchor at their south-west corner, tiles away from the dock tile. */
	public static final int LIVE_ACTOR_ORIGIN_TOLERANCE = 5;
	/**
	 * The catalog origin is the walkable route anchor, not the actor's tile: Captain
	 * Barnaby stands eight tiles along the Ardougne pier from his row origin, and deck
	 * actors can stand a plane above it. Exact name plus action keeps identity strong,
	 * so proximity only guards against a same-name actor elsewhere in the scene.
	 */
	private static final int LIVE_NPC_ORIGIN_RADIUS = 15;

	private static final Set<TransportType> TYPES = Set.of(
		TransportType.NPC, TransportType.SHIP, TransportType.BOAT);

	private NpcDialogueTransportPolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		if (transport == null
			|| !TYPES.contains(transport.getType())
			|| transport.getOrigin() == null
			|| transport.getDestination() == null
			|| transport.getObjectId() <= 0
			|| isBlank(transport.getAction())
			|| isBlank(transport.getName())
			|| normalize(transport.getAction()).equals("talk-to")
			|| !transport.getItemIdRequirements().isEmpty())
		{
			return false;
		}
		if (transport.getCurrencyAmount() > 0)
		{
			return normalize(transport.getCurrencyName()).equals("coins");
		}
		return !isBlank(transport.getDisplayInfo());
	}

	public static String destinationAction(String destination)
	{
		return DESTINATION_ACTION_PREFIX + destination;
	}

	public static boolean isDestinationAction(String action)
	{
		return action != null && action.startsWith(DESTINATION_ACTION_PREFIX);
	}

	/** Whether the action names a dialogue stage rather than the initial actor click. */
	public static boolean isStageAction(String action)
	{
		return CONTINUE_ACTION.equals(action) || CONFIRM_ACTION.equals(action)
			|| isDestinationAction(action);
	}

	public static boolean isLiveNpcMatch(Transport transport, String liveName,
		Collection<String> liveActions, WorldPoint liveTile)
	{
		return isEligible(transport) && liveName != null
			&& liveName.equalsIgnoreCase(transport.getName())
			&& matchLiveNpcAction(liveActions, transport.getAction(),
				transport.getDisplayInfo()) != null
			&& liveTile != null
			&& liveTile.distanceTo2D(transport.getOrigin()) <= LIVE_NPC_ORIGIN_RADIUS;
	}

	/**
	 * Returns the live action to click. Quest-state NPC variants swap the travel op for a
	 * destination-named op (Captain Tobias {@code _1op} exposes {@code Travel} while
	 * {@code _2op} exposes {@code Musa Point}), so the declared destination is accepted as
	 * a direct action when the catalog action is absent.
	 */
	public static String matchLiveNpcAction(Collection<String> liveActions,
		String catalogAction, String displayInfo)
	{
		if (liveActions == null)
		{
			return null;
		}
		for (String action : liveActions)
		{
			if (action != null && action.equalsIgnoreCase(catalogAction))
			{
				return action;
			}
		}
		if (isBlank(displayInfo))
		{
			return null;
		}
		for (String action : liveActions)
		{
			if (action != null && action.equalsIgnoreCase(displayInfo))
			{
				return action;
			}
		}
		return null;
	}

	/** Same acceptance rules as the direct NPC family's object-backed rows. */
	public static boolean isLiveObjectMatch(Transport transport, int liveId, String liveName,
		String[] liveActions, WorldPoint liveTile)
	{
		if (!isEligible(transport) || liveTile == null
			|| liveTile.getPlane() != transport.getOrigin().getPlane()
			|| liveTile.distanceTo2D(transport.getOrigin()) > LIVE_ACTOR_ORIGIN_TOLERANCE
			|| NpcTransportPolicy.matchAction(liveActions, transport.getAction()) == null)
		{
			return false;
		}
		return liveId == transport.getObjectId()
			|| (liveName != null && liveName.equalsIgnoreCase(transport.getName()));
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
