package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable explanation of one route-backed movement target. */
public final class RouteClickSelection
{
	private final WorldPoint target;
	private final int rawIndex;
	private final int smoothedIndex;
	private final int distance;
	private final int reach;
	private final String selection;

	RouteClickSelection(WorldPoint target, int rawIndex, int smoothedIndex, int distance,
		int reach, String selection)
	{
		this.target = Objects.requireNonNull(target, "target");
		this.rawIndex = rawIndex;
		this.smoothedIndex = smoothedIndex;
		this.distance = distance;
		this.reach = reach;
		this.selection = Objects.requireNonNull(selection, "selection");
	}

	public WorldPoint getTarget() { return target; }
	public int getRawIndex() { return rawIndex; }
	public int getSmoothedIndex() { return smoothedIndex; }
	public int getDistance() { return distance; }
	public int getReach() { return reach; }
	public String getSelection() { return selection; }
}
