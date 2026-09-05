package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable observation of one grouping-tab minigame teleport stage. */
public final class MinigameTeleport
{
	public enum Stage
	{
		OPEN_GROUPING_TAB,
		OPEN_GROUPING,
		OPEN_DROPDOWN,
		WAIT_FOR_ACTIVITY,
		SELECT_ACTIVITY,
		TELEPORT,
		SELECT_DESTINATION,
		UNAVAILABLE
	}

	private final WorldPoint routeOrigin;
	private final WorldPoint destination;
	private final String displayInfo;
	private final String activityName;
	private final String destinationOption;
	private final Stage stage;

	public MinigameTeleport(WorldPoint routeOrigin, WorldPoint destination,
		String displayInfo, String activityName, String destinationOption, Stage stage)
	{
		this.routeOrigin = Objects.requireNonNull(routeOrigin, "routeOrigin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.displayInfo = Objects.requireNonNull(displayInfo, "displayInfo");
		this.activityName = Objects.requireNonNull(activityName, "activityName");
		this.destinationOption = Objects.requireNonNull(destinationOption,
			"destinationOption");
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public WorldPoint getRouteOrigin() { return routeOrigin; }
	public WorldPoint getDestination() { return destination; }
	public String getDisplayInfo() { return displayInfo; }
	public String getActivityName() { return activityName; }
	public String getDestinationOption() { return destinationOption; }
	public Stage getStage() { return stage; }
}
