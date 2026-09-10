package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;

/** The Resource Area's directed, diary-priced gate contract. */
public final class ResourceAreaGatePolicy
{
	public static final int GATE = 26760;
	static final WorldPoint INSIDE = new WorldPoint(3184, 3944, 0);
	static final WorldPoint OUTSIDE = new WorldPoint(3184, 3945, 0);
	static final String CONFIRM = "resource-area-confirm-payment";

	private ResourceAreaGatePolicy() { }

	static boolean isEligible(Transport row)
	{
		if (row == null || row.getObjectId() != GATE || row.getType() != TransportType.TRANSPORT
			|| !row.isMembers() || !"Open".equals(row.getAction()) || !"Gate".equals(row.getName())
			|| row.isConsumable() || !row.getItemIdRequirements().isEmpty()
			|| !row.getQuests().isEmpty() || !row.getVarplayers().isEmpty() || row.getDuration() != 2)
		{
			return false;
		}
		for (int level : row.getSkillLevels()) if (level != 0) return false;
		if (INSIDE.equals(row.getOrigin()) && OUTSIDE.equals(row.getDestination()))
		{
			return row.getCurrencyAmount() == 0 && row.getVarbits().isEmpty();
		}
		if (!OUTSIDE.equals(row.getOrigin()) || !INSIDE.equals(row.getDestination())) return false;
		if (row.getVarbits().stream().anyMatch(bit -> bit.getOperator() != TransportVarbit.Operator.EQUAL))
		{
			return false;
		}
		Map<Integer, Integer> bits = row.getVarbits().stream().collect(Collectors.toMap(
			TransportVarbit::getVarbitId, TransportVarbit::getValue, (first, second) -> -1));
		int fare = row.getCurrencyAmount();
		if (fare == 0) return bits.equals(Map.of(4469, 1));
		if (!"Coins".equals(row.getCurrencyName())) return false;
		return fare == 7500 && bits.equals(Map.of(4469, 0, 4468, 0, 4467, 0))
			|| fare == 6000 && bits.equals(Map.of(4469, 0, 4468, 0, 4467, 1))
			|| fare == 3750 && bits.equals(Map.of(4469, 0, 4468, 1));
	}

	static String prompt(int fare)
	{
		return "Pay " + fare + " coins to enter?";
	}

	static int confirmationIndex(int fare, String title, List<String> options)
	{
		if (fare <= 0 || !prompt(fare).equals(clean(title)) || options == null || options.size() != 2)
		{
			return -1;
		}
		if ("Yes".equals(clean(options.get(0))) && "No".equals(clean(options.get(1)))) return 0;
		if ("No".equals(clean(options.get(0))) && "Yes".equals(clean(options.get(1)))) return 1;
		return -1;
	}

	static String nextAction(int fare, int carried, boolean requirementsMet, boolean dialogueOpen,
		String title, List<String> options, String pendingAction)
	{
		if (CONFIRM.equals(pendingAction)) return null;
		if (!requirementsMet || carried < fare) return null;
		if (dialogueOpen)
		{
			return "Open".equals(pendingAction) && confirmationIndex(fare, title, options) >= 0
				? CONFIRM : null;
		}
		return "Open";
	}

	private static String clean(String value)
	{
		return value == null ? "" : Rs2UiHelper.stripTagsToSpace(value).trim();
	}
}
