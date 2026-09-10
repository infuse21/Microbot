package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;

/** Traversal only after the corresponding quest object has been opened or cleared. */
final class QuestStatePassagePolicy
{
	static final List<Entry> ENTRIES = List.of(
		new Entry(21316, 21316, p(2385, 10264, 1), p(2385, 10260, 1), p(2385, 10263, 1),
			"Walk-across", "Rope bridge", 3312, Set.of(0), 0),
		new Entry(21317, 21317, p(2385, 10259, 1), p(2385, 10264, 1), p(2385, 10260, 1),
			"Walk-across", "Rope bridge", 0, Set.of(), 0),
		new Entry(21317, 21317, p(2385, 10260, 1), p(2385, 10264, 1), p(2385, 10260, 1),
			"Walk-across", "Rope bridge", 0, Set.of(), 0),
		new Entry(21318, 21318, p(2398, 10258, 1), p(2393, 10258, 1), p(2397, 10258, 1),
			"Walk-across", "Rope bridge", 0, Set.of(), 0),
		new Entry(21319, 21319, p(2393, 10258, 1), p(2398, 10258, 1), p(2394, 10258, 1),
			"Walk-across", "Rope bridge", 0, Set.of(), 0),
		new Entry(21584, 21584, p(2400, 3889, 0), p(2394, 10300, 1), p(2401, 3889, 0),
			"Open", "Cave", 3311, Set.of(300, 310), 0),
		new Entry(21584, 21584, p(2401, 3888, 0), p(2394, 10300, 1), p(2401, 3889, 0),
			"Open", "Cave", 3311, Set.of(300, 310), 0),
		new Entry(21584, 21584, p(2403, 3889, 0), p(2394, 10300, 1), p(2401, 3889, 0),
			"Open", "Cave", 3311, Set.of(300, 310), 0),
		new Entry(6878, 6882, p(2457, 3048, 0), p(2455, 3048, 0), p(2456, 3048, 0),
			"Climb-over", "Crushed barricade", 496, Set.of(1), 0),
		new Entry(19124, 18360, p(2700, 9688, 0), p(2323, 5104, 0), p(2701, 9688, 0),
			"Enter", "Wall opening", 2618, Set.of(1), 2),
		new Entry(18412, 18412, p(2323, 5104, 0), p(2700, 9688, 0), p(2322, 5104, 0),
			"Enter", "Passage", 0, Set.of(), 2),
		new Entry(33342, 33340, p(3105, 9308, 2), p(3105, 9306, 2), p(3105, 9307, 2),
			"Climb-over", "Pile of rubble", 1560, Set.of(50, 60, 65, 70), 0),
		new Entry(33342, 33340, p(3105, 9306, 2), p(3105, 9308, 2), p(3105, 9307, 2),
			"Climb-over", "Pile of rubble", 1560, Set.of(50, 60, 65, 70), 0));

	private QuestStatePassagePolicy() { }
	static boolean ownsObject(int id) { return ENTRIES.stream().anyMatch(entry -> entry.id == id); }

	static Entry entry(Transport row)
	{
		if (row == null || row.getType() != TransportType.TRANSPORT || !row.isMembers()
			|| row.isConsumable() || row.getCurrencyAmount() != 0 || !row.getItemIdRequirements().isEmpty()
			|| !row.getQuests().isEmpty() || !row.getVarplayers().isEmpty()
			|| java.util.Arrays.stream(row.getSkillLevels()).anyMatch(level -> level != 0)) return null;
		for (Entry entry : ENTRIES)
		{
			if (entry.id != row.getObjectId() || !entry.from.equals(row.getOrigin())
				|| !entry.to.equals(row.getDestination()) || !entry.action.equals(row.getAction())
				|| !entry.name.equals(row.getName()) || entry.duration != row.getDuration()) continue;
			if (entry.varbit == 0) return row.getVarbits().isEmpty() ? entry : null;
			return row.getVarbits().size() == 1 && row.getVarbits().stream().allMatch(bit ->
				bit.getVarbitId() == entry.varbit && bit.getOperator() == TransportVarbit.Operator.EQUAL
					&& entry.states.contains(bit.getValue())) ? entry : null;
		}
		return null;
	}

	private static WorldPoint p(int x, int y, int plane) { return new WorldPoint(x, y, plane); }

	static final class Entry
	{
		final int id;
		final int liveId;
		final WorldPoint from;
		final WorldPoint to;
		final WorldPoint anchor;
		final String action;
		final String name;
		final int varbit;
		final Set<Integer> states;
		final int duration;

		Entry(int id, int liveId, WorldPoint from, WorldPoint to, WorldPoint anchor,
			String action, String name, int varbit, Set<Integer> states, int duration)
		{
			this.id = id;
			this.liveId = liveId;
			this.from = from;
			this.to = to;
			this.anchor = anchor;
			this.action = action;
			this.name = name;
			this.varbit = varbit;
			this.states = states;
			this.duration = duration;
		}
	}
}
