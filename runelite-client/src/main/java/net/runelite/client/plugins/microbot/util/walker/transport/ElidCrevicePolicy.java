package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.collections.ItemCollections;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/** Requirements for the rope descent into the Spirits of the Elid genie cave. */
public final class ElidCrevicePolicy
{
	private static final int ROPE_ITEM_ID = ItemID.ROPE;
	private static final Set<Integer> LIGHT_SOURCE_IDS =
		Set.copyOf(ItemCollections.LIGHT_SOURCES.getItems());
	private static final Set<String> ROUTES = Set.of(
		"3375,2904,0->3374,9305,0",
		"3375,2905,0->3374,9305,0",
		"3375,2906,0->3374,9305,0",
		"3374,2903,0->3374,9305,0",
		"3374,2902,0->3374,9305,0",
		"3372,2902,0->3374,9305,0",
		"3372,2903,0->3374,9305,0",
		"3372,2904,0->3374,9305,0",
		"3372,2905,0->3374,9305,0",
		"3372,2906,0->3374,9305,0",
		"3372,2907,0->3374,9305,0",
		"3374,2907,0->3374,9305,0");

	private ElidCrevicePolicy()
	{
	}

	public static boolean isEligible(Transport transport)
	{
		return transport != null && transport.getType() == TransportType.TRANSPORT
			&& transport.getOrigin() != null && transport.getDestination() != null
			&& transport.getObjectId() == 10416
			&& "Climb-down".equalsIgnoreCase(transport.getAction())
			&& "Crevice".equalsIgnoreCase(transport.getName())
			&& transport.isMembers() && transport.isConsumable()
			&& transport.getCurrencyAmount() == 0 && transport.getDuration() == 1
			&& transport.getItemIdRequirements().equals(Set.of(Set.of(ROPE_ITEM_ID)))
			&& transport.getQuests().equals(
				Map.of(Quest.SPIRITS_OF_THE_ELID, QuestState.IN_PROGRESS))
			&& transport.getVarbits().isEmpty() && transport.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(transport.getSkillLevels()).allMatch(level -> level == 0)
			&& ROUTES.contains(point(transport.getOrigin()) + "->"
				+ point(transport.getDestination()));
	}

	public static Set<Integer> lightSourceIds(Transport transport)
	{
		return isEligible(transport) ? LIGHT_SOURCE_IDS : Set.of();
	}

	public static boolean questStageAvailable(Transport transport)
	{
		if (!isEligible(transport) || Microbot.getRs2PlayerStateCache() == null)
		{
			return !isEligible(transport);
		}
		QuestState state = Rs2Player.getQuestState(Quest.SPIRITS_OF_THE_ELID);
		return questStageAvailable(transport, state, Microbot.getVarbitValue(VarbitID.ELIDQUEST));
	}

	static boolean questStageAvailable(Transport transport, QuestState state, int questStage)
	{
		return !isEligible(transport) || state == QuestState.FINISHED
			|| state == QuestState.IN_PROGRESS && questStage >= 40;
	}

	public static boolean requiresExactLanding(int objectId)
	{
		return objectId == 10416;
	}

	private static String point(WorldPoint point)
	{
		return point.getX() + "," + point.getY() + "," + point.getPlane();
	}
}
