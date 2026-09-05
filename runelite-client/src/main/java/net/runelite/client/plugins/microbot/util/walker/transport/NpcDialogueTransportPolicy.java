package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Conservative eligibility and stage identity for chat-dialogue NPC/ship/boat rows: free
 * rows whose declared {@code Display info} names the destination option, and coin-paid
 * rows whose fare is confirmed through an affirmative chat option ({@code Display info}
 * optional for single-destination paid flows). The exact Shilo Village cart pair reuses this
 * lifecycle for its direct paid object click and remote landing. The exact Cabin Boy Herbert
 * rows reuse it with one explicit travel-request stage. Captain Shanks' two exact
 * destinations reuse it with a conservative maximum ticket-purchase fare. Pirate Pete's
 * quest-finished conversation and Ghost Captain's combined wearable-plus-ecto-token
 * requirement are exact allowlisted contracts. Other Talk-to conversations remain
 * legacy-owned until their full protocol is modelled.
 */
public final class NpcDialogueTransportPolicy
{
	public static final String CONTINUE_ACTION = "dialogue-continue";
	public static final String EQUIP_GHOSTSPEAK_ACTION = "dialogue-equip-ghostspeak";
	public static final String EQUIP_GOLD_HELMET_ACTION = "dialogue-equip-gold-helmet";
	public static final String CONFIRM_ACTION = "dialogue-confirm";
	public static final String TRAVEL_REQUEST_ACTION = "dialogue-travel-request";
	public static final String HERBERT_TRAVEL_REQUEST_OPTION = "Can you take me somewhere?";
	public static final String CANCEL_UNAVAILABLE_ACTION = "dialogue-cancel-unavailable";
	public static final String DESTINATION_UNAVAILABLE_ACTION = "dialogue-destination-unavailable";
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
		TransportType.NPC, TransportType.SHIP, TransportType.BOAT,
		TransportType.MAGIC_CARPET);
	private static final Set<Integer> MAGIC_CARPET_ACTOR_IDS = Set.of(17, 18, 19, 20, 22);
	private static final int BRIMHAVEN_CART_ID = 2230;
	private static final int SHILO_CART_ID = 2265;
	private static final WorldPoint BRIMHAVEN_LANDING = new WorldPoint(2776, 3214, 0);
	private static final WorldPoint SHILO_LANDING = new WorldPoint(2834, 2951, 0);
	private static final int HERBERT_PORT_SARIM_ID = 10933;
	private static final int HERBERT_PISCARILIUS_ID = 10932;
	private static final WorldPoint PORT_SARIM_ORIGIN = new WorldPoint(3055, 3245, 0);
	private static final WorldPoint PORT_SARIM_LANDING = new WorldPoint(3055, 3242, 1);
	private static final WorldPoint PISCARILIUS_ORIGIN = new WorldPoint(1824, 3691, 0);
	private static final WorldPoint PISCARILIUS_LANDING = new WorldPoint(1824, 3695, 1);
	private static final WorldPoint LANDS_END_LANDING = new WorldPoint(1504, 3395, 1);
	private static final int CAPTAIN_SHANKS_ID = 5364;
	private static final WorldPoint CAPTAIN_SHANKS_ORIGIN = new WorldPoint(2763, 2960, 1);
	private static final WorldPoint PORT_KHAZARD_LANDING = new WorldPoint(2680, 3150, 0);
	private static final WorldPoint PORT_SARIM_MOORING = new WorldPoint(3050, 3192, 0);
	private static final int CAPTAIN_SHANKS_MAX_FARE = 50;
	private static final int PIRATE_PETE_PORT_ID = 601;
	private static final int PIRATE_PETE_ISLAND_ID = 602;
	private static final WorldPoint PIRATE_PETE_PORT_ORIGIN = new WorldPoint(3680, 3536, 0);
	private static final WorldPoint PIRATE_PETE_ISLAND_ORIGIN = new WorldPoint(2162, 5114, 1);
	private static final String PIRATE_PETE_CONFIRM_OPTION = "Okay!";
	private static final int GHOST_CAPTAIN_ID = 3005;
	private static final WorldPoint GHOST_CAPTAIN_ORIGIN = new WorldPoint(3703, 3487, 0);
	private static final WorldPoint DRAGONTOOTH_LANDING = new WorldPoint(3792, 3560, 0);
	private static final Set<Integer> GHOSTSPEAK_ITEM_IDS = Set.of(552, 4250, 13113, 13114, 13115);
	private static final int GOLD_HELMET_ID = 4567;
	private static final int DONDAKAN_ID = 4891;
	private static final WorldPoint DONDAKAN_ORIGIN = new WorldPoint(2824, 10169, 0);
	private static final WorldPoint DONDAKAN_MINE = new WorldPoint(2615, 4966, 0);
	private static final String DONDAKAN_OPTION = "Can you shoot me into the rock again?";
	private static final int MOUNTAIN_GUIDE_ID = 7600;
	private static final int MOUNTAIN_GUIDE_UNLOCK_VARBIT = 5421;
	private static final Set<String> MOUNTAIN_GUIDE_ROUTES = Set.of(
		"1401,3536->1275,3559|1: Mount Quidamortem.",
		"1401,3536->1272,3475|2: South of Quidamortem.",
		"1275,3559->1401,3536|1: The Shayzien Outpost.",
		"1275,3559->1272,3475|2: South of Quidamortem.",
		"1272,3475->1401,3536|1: The Shayzien Outpost.",
		"1272,3475->1275,3559|2: Mount Quidamortem.");
	public static final WorldPoint FOSSIL_CAMP = new WorldPoint(3724, 3807, 0);
	private static final WorldPoint FOSSIL_NORTH = new WorldPoint(3734, 3893, 0);
	private static final WorldPoint FOSSIL_SEA = new WorldPoint(3763, 3899, 0);

	private NpcDialogueTransportPolicy()
	{
	}

	/** Null means unrecognised/incomplete menu, not evidence that a destination is locked. */
	public static Set<WorldPoint> missingFossilCampDestinations(WorldPoint player, List<String> options)
	{
		if (player == null || player.distanceTo(FOSSIL_CAMP) > LIVE_ACTOR_ORIGIN_TOLERANCE
			|| options == null)
		{
			return null;
		}
		Set<String> text = new HashSet<>();
		for (String option : options)
		{
			text.add(normalize(Rs2UiHelper.stripTagsToSpace(option == null ? "" : option)));
		}
		if (!text.contains("row to the barge.")
			|| !text.contains("row to the barge and travel to the digsite.")
			|| !text.contains("cancel."))
		{
			return null;
		}
		Set<WorldPoint> missing = new HashSet<>();
		if (Rs2NpcDialogueTransportScene.matchOptionIndex(options, "North") < 0)
		{
			missing.add(FOSSIL_NORTH);
		}
		if (Rs2NpcDialogueTransportScene.matchOptionIndex(options, "Sea") < 0)
		{
			missing.add(FOSSIL_SEA);
		}
		return Set.copyOf(missing);
	}

	/** These six legacy labels contain menu ordinals, which change when destinations unlock. */
	public static String destinationOption(Transport transport)
	{
		if (isPiratePete(transport))
		{
			return PIRATE_PETE_CONFIRM_OPTION;
		}
		if (isDondakan(transport))
		{
			return DONDAKAN_OPTION;
		}
		if (transport.getType() == TransportType.BOAT
			&& ((transport.getObjectId() == 30914 && FOSSIL_CAMP.equals(transport.getOrigin()))
				|| (transport.getObjectId() == 30915 && FOSSIL_NORTH.equals(transport.getOrigin()))
				|| (transport.getObjectId() == 30919 && FOSSIL_SEA.equals(transport.getOrigin()))))
		{
			if (FOSSIL_NORTH.equals(transport.getDestination())) return "North";
			if (FOSSIL_SEA.equals(transport.getDestination())) return "Sea";
			if (FOSSIL_CAMP.equals(transport.getDestination())) return "Camp";
		}
		if (isMountainGuideShadow(transport))
		{
			return transport.getDisplayInfo().replaceFirst("^\\d+:\\s*", "");
		}
		return transport.getDisplayInfo();
	}

	public static boolean isEligible(Transport transport)
	{
		if (isTravelCart(transport) || isCabinBoyHerbert(transport)
			|| isCaptainShanks(transport) || isPiratePete(transport)
			|| isGhostCaptain(transport) || isMountainGuideShadow(transport)
			|| isDondakan(transport))
		{
			return true;
		}
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
		if (transport.getType() == TransportType.MAGIC_CARPET)
		{
			return MAGIC_CARPET_ACTOR_IDS.contains(transport.getObjectId())
				&& normalize(transport.getAction()).equals("travel")
				&& normalize(transport.getName()).equals("rug merchant")
				&& !isBlank(transport.getDisplayInfo())
				&& transport.getCurrencyAmount() > 0
				&& normalize(transport.getCurrencyName()).equals("coins");
		}
		if (transport.getCurrencyAmount() > 0)
		{
			return normalize(transport.getCurrencyName()).equals("coins");
		}
		return !isBlank(transport.getDisplayInfo());
	}

	public static boolean isDondakan(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getObjectId() == DONDAKAN_ID
			&& DONDAKAN_ORIGIN.equals(transport.getOrigin())
			&& DONDAKAN_MINE.equals(transport.getDestination())
			&& normalize(transport.getAction()).equals("talk-to")
			&& normalize(transport.getName()).equals("dondakan the dwarf")
			&& isBlank(transport.getDisplayInfo()) && transport.getCurrencyAmount() == 0
			&& isBlank(transport.getCurrencyName()) && !transport.isConsumable()
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(GOLD_HELMET_ID)))
			&& transport.getQuests().size() == 1
			&& transport.getQuests().get(Quest.BETWEEN_A_ROCK) == QuestState.FINISHED
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& transport.getDuration() == 4;
	}

	static boolean isMountainGuideShadow(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getObjectId() != MOUNTAIN_GUIDE_ID
			|| !normalize(transport.getAction()).equals("travel")
			|| !normalize(transport.getName()).equals("mountain guide")
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| transport.getOrigin().getPlane() != 0 || transport.getDestination().getPlane() != 0
			|| transport.getCurrencyAmount() != 0 || !isBlank(transport.getCurrencyName())
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getQuests().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 7 || transport.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit unlock = transport.getVarbits().iterator().next();
		return unlock.getVarbitId() == MOUNTAIN_GUIDE_UNLOCK_VARBIT
			&& unlock.getValue() == 1 && unlock.getOperator() == TransportVarbit.Operator.EQUAL
			&& MOUNTAIN_GUIDE_ROUTES.contains(mountainGuideKey(transport));
	}

	private static String mountainGuideKey(Transport transport)
	{
		WorldPoint origin = transport.getOrigin();
		WorldPoint destination = transport.getDestination();
		return origin.getX() + "," + origin.getY() + "->"
			+ destination.getX() + "," + destination.getY() + "|"
			+ transport.getDisplayInfo();
	}

	public static boolean isPiratePete(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.BOAT
			|| !normalize(transport.getAction()).equals("talk-to")
			|| !normalize(transport.getName()).equals("pirate pete")
			|| transport.getCurrencyAmount() != 0 || !isBlank(transport.getCurrencyName())
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 9 || transport.getQuests().size() != 1
			|| transport.getQuests().get(Quest.RUM_DEAL) != QuestState.FINISHED)
		{
			return false;
		}
		return (transport.getObjectId() == PIRATE_PETE_PORT_ID
			&& PIRATE_PETE_PORT_ORIGIN.equals(transport.getOrigin())
			&& PIRATE_PETE_ISLAND_ORIGIN.equals(transport.getDestination()))
			|| (transport.getObjectId() == PIRATE_PETE_ISLAND_ID
			&& PIRATE_PETE_ISLAND_ORIGIN.equals(transport.getOrigin())
			&& PIRATE_PETE_PORT_ORIGIN.equals(transport.getDestination()));
	}

	public static boolean isGhostCaptain(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.BOAT
			&& transport.getObjectId() == GHOST_CAPTAIN_ID
			&& GHOST_CAPTAIN_ORIGIN.equals(transport.getOrigin())
			&& DRAGONTOOTH_LANDING.equals(transport.getDestination())
			&& normalize(transport.getAction()).equals("travel")
			&& normalize(transport.getName()).equals("ghost captain")
			&& transport.getCurrencyAmount() == 25
			&& normalize(transport.getCurrencyName()).equals("ecto-token")
			&& !transport.isConsumable() && transport.getItemIdRequirements().size() == 1
			&& transport.getItemIdRequirements().iterator().next().equals(GHOSTSPEAK_ITEM_IDS)
			&& transport.getQuests().isEmpty() && transport.getVarbits().isEmpty()
			&& transport.getVarplayers().isEmpty() && transport.getDuration() == 6;
	}

	public static Set<Integer> ghostspeakItemIds()
	{
		return GHOSTSPEAK_ITEM_IDS;
	}

	public static int goldHelmetId()
	{
		return GOLD_HELMET_ID;
	}

	public static boolean isCaptainShanks(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.SHIP
			|| transport.getObjectId() != CAPTAIN_SHANKS_ID
			|| !CAPTAIN_SHANKS_ORIGIN.equals(transport.getOrigin())
			|| !normalize(transport.getAction()).equals("talk-to")
			|| !normalize(transport.getName()).equals("captain shanks")
			|| transport.getCurrencyAmount() != CAPTAIN_SHANKS_MAX_FARE
			|| !normalize(transport.getCurrencyName()).equals("coins")
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 10 || transport.getQuests().size() != 1
			|| transport.getQuests().get(Quest.SHILO_VILLAGE) != QuestState.FINISHED)
		{
			return false;
		}
		return (PORT_KHAZARD_LANDING.equals(transport.getDestination())
			&& "Khazard Port".equals(transport.getDisplayInfo()))
			|| (PORT_SARIM_MOORING.equals(transport.getDestination())
				&& "Port Sarim".equals(transport.getDisplayInfo()));
	}

	public static boolean isCabinBoyHerbert(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.SHIP
			|| !normalize(transport.getAction()).equals("talk-to")
			|| !normalize(transport.getName()).equals("cabin boy herbert")
			|| transport.getCurrencyAmount() != 0 || !isBlank(transport.getCurrencyName())
			|| transport.isConsumable() || !transport.getItemIdRequirements().isEmpty()
			|| !transport.getVarbits().isEmpty() || !transport.getVarplayers().isEmpty()
			|| transport.getDuration() != 6 || transport.getQuests().size() != 1
			|| transport.getQuests().get(Quest.A_KINGDOM_DIVIDED) != QuestState.FINISHED)
		{
			return false;
		}
		WorldPoint origin = transport.getOrigin();
		WorldPoint destination = transport.getDestination();
		String display = transport.getDisplayInfo();
		if (transport.getObjectId() == HERBERT_PORT_SARIM_ID
			&& PORT_SARIM_ORIGIN.equals(origin))
		{
			return (PISCARILIUS_LANDING.equals(destination)
				&& "Port Piscarilius".equals(display))
				|| (LANDS_END_LANDING.equals(destination) && "Land's End".equals(display));
		}
		return transport.getObjectId() == HERBERT_PISCARILIUS_ID
			&& PISCARILIUS_ORIGIN.equals(origin)
			&& ((PORT_SARIM_LANDING.equals(destination) && "Port Sarim".equals(display))
				|| (LANDS_END_LANDING.equals(destination) && "Land's End".equals(display)));
	}

	static boolean isTravelCart(Transport transport)
	{
		if (transport == null || transport.getType() != TransportType.TRANSPORT
			|| transport.getOrigin() == null || transport.getDestination() == null
			|| !normalize(transport.getAction()).equals("pay-fare")
			|| !normalize(transport.getName()).equals("travel cart")
			|| transport.getCurrencyAmount() != 200
			|| !normalize(transport.getCurrencyName()).equals("coins")
			|| transport.getDuration() != 5 || transport.isConsumable()
			|| !transport.getItemIdRequirements().isEmpty()
			|| !transport.getQuests().isEmpty() || !transport.getVarbits().isEmpty()
			|| transport.getVarplayers().size() != 1)
		{
			return false;
		}
		TransportVarPlayer questState = transport.getVarplayers().iterator().next();
		if (questState.getVarplayerId() != VarPlayerID.ZOMBIEQUEEN
			|| questState.getValue() != 14
			|| questState.getOperator() != TransportVarPlayer.Operator.GREATER_THAN)
		{
			return false;
		}
		WorldPoint origin = transport.getOrigin();
		if (transport.getObjectId() == BRIMHAVEN_CART_ID)
		{
			return SHILO_LANDING.equals(transport.getDestination())
				&& origin.getPlane() == 0 && origin.getX() >= 2776 && origin.getX() <= 2779
				&& origin.getY() >= 3210 && origin.getY() <= 3214;
		}
		return transport.getObjectId() == SHILO_CART_ID
			&& BRIMHAVEN_LANDING.equals(transport.getDestination())
			&& origin.getPlane() == 0 && origin.getX() >= 2830 && origin.getX() <= 2834
			&& origin.getY() >= 2951 && origin.getY() <= 2954;
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
			|| TRAVEL_REQUEST_ACTION.equals(action)
			|| CANCEL_UNAVAILABLE_ACTION.equals(action)
			|| DESTINATION_UNAVAILABLE_ACTION.equals(action)
			|| isDestinationAction(action);
	}

	/** A selected destination/continue can be in transit while its old menu is closing. */
	public static boolean isVoyageStageAction(String action)
	{
		return isDestinationAction(action) || CONTINUE_ACTION.equals(action);
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
