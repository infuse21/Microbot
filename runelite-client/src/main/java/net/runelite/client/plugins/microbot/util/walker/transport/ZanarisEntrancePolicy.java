package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;

/** Post-quest shed travel only; neither Lost City completion nor clue solving. */
public final class ZanarisEntrancePolicy
{
	static final WorldPoint ORIGIN = new WorldPoint(3202, 3169, 0);
	static final WorldPoint DESTINATION = new WorldPoint(2452, 4473, 0);
	static final String OPEN_INVENTORY = "zanaris-open-inventory";
	static final String WIELD_STAFF = "zanaris-wield-staff";
	static final String SELECT_DESTINATION = "zanaris-select-destination";
	static final String DESTINATION_OPTION = "Let it transport you to Zanaris.";
	static final String SHED_OPTION = "Just enter the shed.";

	private ZanarisEntrancePolicy()
	{
	}

	static boolean isEligible(Transport row)
	{
		if (row == null || row.getType() != TransportType.TRANSPORT || row.getObjectId() != 2406
			|| !ORIGIN.equals(row.getOrigin()) || !DESTINATION.equals(row.getDestination())
			|| !"Open".equals(row.getAction()) || !"Door".equals(row.getName())
			|| row.getCurrencyAmount() != 0 || row.isConsumable() || row.getDuration() != 6
			|| !row.getQuests().equals(Map.of(Quest.LOST_CITY, QuestState.FINISHED))
			|| !row.getVarplayers().isEmpty() || row.getVarbits().size() != 1)
		{
			return false;
		}
		TransportVarbit diary = row.getVarbits().iterator().next();
		if (diary.getVarbitId() != VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE
			|| diary.getOperator() != TransportVarbit.Operator.EQUAL)
		{
			return false;
		}
		if (diary.getValue() == 1) return row.getItemIdRequirements().isEmpty();
		return diary.getValue() == 0 && (row.getItemIdRequirements().equals(Set.of(Set.of(772)))
			|| row.getItemIdRequirements().equals(Set.of(Set.of(9084)))
				&& row.getSkillLevels()[Skill.MAGIC.ordinal()] >= 65
				&& row.getSkillLevels()[Skill.DEFENCE.ordinal()] >= 40);
	}

	/** Require the complete two-option clue menu, not a substring or generic Yes. */
	static int destinationIndex(List<String> options)
	{
		if (options == null || options.size() != 2) return -1;
		String first = clean(options.get(0));
		String second = clean(options.get(1));
		if (DESTINATION_OPTION.equals(first) && SHED_OPTION.equals(second)) return 0;
		return SHED_OPTION.equals(first) && DESTINATION_OPTION.equals(second) ? 1 : -1;
	}

	private static String clean(String text)
	{
		return text == null ? "" : Rs2UiHelper.stripTagsToSpace(text).trim().replaceAll("\\s+", " ");
	}

	static String nextAction(boolean staffRequired, boolean staffEquipped, boolean inventoryVisible,
		boolean dialogueOpen, List<String> options, String pendingAction)
	{
		if (dialogueOpen)
		{
			return (!staffRequired || staffEquipped) && destinationIndex(options) >= 0
				? SELECT_DESTINATION : null;
		}
		if (SELECT_DESTINATION.equals(pendingAction)) return null;
		if (staffRequired && !staffEquipped)
		{
			return inventoryVisible ? WIELD_STAFF : OPEN_INVENTORY;
		}
		return "Open";
	}
}
