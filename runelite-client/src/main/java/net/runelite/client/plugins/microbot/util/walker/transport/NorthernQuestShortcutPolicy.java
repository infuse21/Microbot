package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;

/** Exact Ungael passages and the directed Weiss cliff chain. */
public final class NorthernQuestShortcutPolicy
{
	private static final List<WorldPoint> WEISS = List.of(point(2852, 3966), point(2852, 3964),
		point(2855, 3964), point(2853, 3961), point(2857, 3961), point(2859, 3962), point(2859, 3960));
	static final List<Entry> ENTRIES = List.of(
		new Entry(29868, 31994, point(2266, 4039), point(2264, 4039), point(2265, 4039), "Climb-over", "Ice chunks", 6108, 29, false),
		new Entry(29868, 31994, point(2264, 4039), point(2266, 4039), point(2265, 4039), "Climb-over", "Ice chunks", 6108, 29, false),
		new Entry(29870, 31998, point(2263, 4040), point(2263, 4048), point(2263, 4041), "Climb-up", "Ice chunks", 6108, 29, false),
		new Entry(29869, 31996, point(2263, 4048), point(2263, 4040), point(2263, 4047), "Climb-down", "Ice chunks", 6108, 29, false),
		weiss(33184, 33184, 0, 1, 2852, 3965, "Climb", "Rockslide", true),
		weiss(33184, 33184, 1, 0, 2852, 3965, "Climb", "Rockslide", false),
		weiss(33185, 33185, 1, 2, 2853, 3964, "Climb", "Rockslide", true),
		weiss(33185, 33185, 2, 1, 2854, 3964, "Climb", "Rockslide", false),
		weiss(33328, 33188, 2, 3, 2854, 3962, "Climb", "Rope", true),
		weiss(33327, 33187, 3, 2, 2853, 3962, "Climb", "Roped tree", false),
		weiss(33190, 33190, 3, 4, 2854, 3961, "Cross", "Ledge", true),
		weiss(33190, 33190, 4, 3, 2854, 3961, "Cross", "Ledge", false),
		weiss(33191, 33191, 5, 6, 2859, 3961, "Climb", "Rockslide", true),
		weiss(33191, 33191, 6, 5, 2859, 3961, "Climb", "Rockslide", false));

	private NorthernQuestShortcutPolicy() { }

	static boolean ownsObject(int id)
	{
		return ENTRIES.stream().anyMatch(entry -> entry.id == id);
	}

	public static boolean isWeiss(Transport row)
	{
		Entry entry = entry(row);
		return entry != null && WEISS.contains(entry.from);
	}

	static Entry entry(Transport row)
	{
		if (row == null || row.getType() != TransportType.TRANSPORT || !row.isMembers()
			|| row.getDuration() != 0 || row.getCurrencyAmount() != 0 || row.isConsumable()
			|| !row.getItemIdRequirements().isEmpty() || !row.getQuests().isEmpty()
			|| !row.getVarplayers().isEmpty()) return null;
		for (Entry entry : ENTRIES)
		{
			if (entry.id != row.getObjectId() || !entry.from.equals(row.getOrigin())
				|| !entry.to.equals(row.getDestination()) || !entry.action.equals(row.getAction())
				|| !entry.name.equals(row.getName())) continue;
			for (int i = 0; i < row.getSkillLevels().length; i++)
			{
				if (row.getSkillLevels()[i] != (entry.ascending && i == Skill.AGILITY.ordinal() ? 68 : 0))
					return null;
			}
			if (entry.varbit == 0) return row.getVarbits().isEmpty() ? entry : null;
			return row.getVarbits().size() == 1 && row.getVarbits().stream().allMatch(bit ->
				bit.getVarbitId() == entry.varbit && bit.getValue() == entry.threshold
					&& bit.getOperator() == TransportVarbit.Operator.GREATER_THAN) ? entry : null;
		}
		return null;
	}

	public static boolean fellToEarlierStage(RouteInteraction pending, WorldPoint player)
	{
		if (pending == null || pending.getKind() != RouteInteraction.Kind.CATALOG_TRANSITION) return false;
		for (Entry entry : ENTRIES)
		{
			if (!entry.ascending || entry.id != pending.getObjectId()
				|| !entry.from.equals(pending.getCrossingFrom()) || !entry.to.equals(pending.getCrossingTo())) continue;
			int index = WEISS.indexOf(player);
			return index >= 0 && index < WEISS.indexOf(entry.from);
		}
		return false;
	}

	private static Entry weiss(int id, int live, int from, int to, int x, int y,
		String action, String name, boolean ascending)
	{
		boolean rope = id == 33327 || id == 33328;
		return new Entry(id, live, WEISS.get(from), WEISS.get(to), point(x, y), action, name,
			rope ? 6528 : 0, rope ? 44 : 0, ascending);
	}

	private static WorldPoint point(int x, int y) { return new WorldPoint(x, y, 0); }

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
		final int threshold;
		final boolean ascending;

		Entry(int id, int liveId, WorldPoint from, WorldPoint to, WorldPoint anchor,
			String action, String name, int varbit, int threshold, boolean ascending)
		{
			this.id = id;
			this.liveId = liveId;
			this.from = from;
			this.to = to;
			this.anchor = anchor;
			this.action = action;
			this.name = name;
			this.varbit = varbit;
			this.threshold = threshold;
			this.ascending = ascending;
		}
	}
}
