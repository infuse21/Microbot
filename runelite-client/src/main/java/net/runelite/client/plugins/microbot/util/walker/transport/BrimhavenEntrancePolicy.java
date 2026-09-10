package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;

/** One-visit payment decisions; permanent access is observed, never purchased. */
final class BrimhavenEntrancePolicy
{
	static final int FARE = 875;
	static final int ENTRANCE = 20877;
	static final WorldPoint ANCHOR = new WorldPoint(2743, 3153, 0);
	static final WorldPoint LANDING = new WorldPoint(2713, 9564, 0);
	private static final Set<WorldPoint> APPROACHES = Set.of(
		new WorldPoint(2744, 3154, 0), new WorldPoint(2744, 3153, 0),
		new WorldPoint(2745, 3154, 0), new WorldPoint(2743, 3154, 0),
		new WorldPoint(2744, 3152, 0), new WorldPoint(2745, 3152, 0), new WorldPoint(2746, 3152, 0));
	static final String CONFIRM = "brimhaven-confirm-visit";
	static final String CONTINUE = "brimhaven-payment-continue";
	private static final String VISIT = "Pay 875 coins to enter once";
	private static final String PERMANENT = "Pay 1,000,000 coins for permanent access";
	private static final String RECEIPT =
		"Many thanks. You may now pass the door. May your death be a glorious one!";

	private BrimhavenEntrancePolicy() { }

	static boolean isEligible(Transport row)
	{
		if (row == null || row.getObjectId() != ENTRANCE || row.getType() != TransportType.TRANSPORT
			|| !row.isMembers() || !APPROACHES.contains(row.getOrigin()) || !LANDING.equals(row.getDestination())
			|| !"Enter".equals(row.getAction()) || !"Dungeon entrance".equals(row.getName())
			|| row.getDuration() != 2 || row.isConsumable() || !row.getItemIdRequirements().isEmpty()
			|| !row.getQuests().isEmpty() || !row.getVarplayers().isEmpty()) return false;
		for (int level : row.getSkillLevels()) if (level != 0) return false;
		if (row.getVarbits().stream().anyMatch(bit -> bit.getOperator() != TransportVarbit.Operator.EQUAL))
			return false;
		Map<Integer, Integer> gates = row.getVarbits().stream().collect(Collectors.toMap(
			TransportVarbit::getVarbitId, TransportVarbit::getValue, (first, second) -> -1));
		if (row.getCurrencyAmount() == FARE && "Coins".equals(row.getCurrencyName()))
			return gates.equals(Map.of(5628, 0, 8122, 0));
		return row.getCurrencyAmount() == 0
			&& (gates.equals(Map.of(5628, 1, 8122, 0)) || gates.equals(Map.of(8122, 1)));
	}

	static boolean isOpen(int doorState)
	{
		return doorState >= 0 && doorState < 16 && (doorState & 9) != 0;
	}

	static int fare(int doorState)
	{
		return doorState < 0 || doorState >= 16 ? -1 : isOpen(doorState) ? 0 : FARE;
	}

	static int confirmationIndex(String title, List<String> options)
	{
		if (options == null || options.stream().anyMatch(Objects::isNull)) return -1;
		List<String> labels = options.stream().map(BrimhavenEntrancePolicy::clean)
			.collect(Collectors.toList());
		if ("Pay 875 coins to enter?".equals(clean(title)) && labels.size() == 2
			&& labels.contains("Yes") && labels.contains("No")) return labels.indexOf("Yes");
		if ("Select an Option".equalsIgnoreCase(clean(title)) && labels.size() == 3
			&& labels.contains(VISIT) && labels.contains(PERMANENT) && labels.contains("Cancel"))
		{
			return labels.indexOf(VISIT);
		}
		return -1;
	}

	static String nextAction(int doorState, int coins, boolean dialogueOpen, String title,
		List<String> options, String speaker, String speech, String pendingAction)
	{
		int fare = fare(doorState);
		if (fare < 0) return null;
		if (dialogueOpen)
		{
			if ("Pay".equals(pendingAction) && fare == FARE && coins >= FARE
				&& confirmationIndex(title, options) >= 0) return CONFIRM;
			if (CONFIRM.equals(pendingAction) && isOpen(doorState)
				&& "Saniboch".equals(clean(speaker)) && RECEIPT.equals(clean(speech))) return CONTINUE;
			return null;
		}
		if (isOpen(doorState)) return "Enter";
		// Never issue a second payment after confirmation, even if the receipt disappears.
		if (CONFIRM.equals(pendingAction) || CONTINUE.equals(pendingAction)
			|| "Enter".equals(pendingAction)) return null;
		return coins >= FARE ? "Pay" : null;
	}

	private static String clean(String value)
	{
		return value == null ? "" : Rs2UiHelper.stripTagsToSpace(value).trim();
	}
}
