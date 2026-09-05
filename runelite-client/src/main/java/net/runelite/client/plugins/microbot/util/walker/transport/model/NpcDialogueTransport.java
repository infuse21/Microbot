package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable live stage for one free dialogue-menu NPC/ship/boat route. */
public final class NpcDialogueTransport
{
	public enum Stage
	{
		EQUIP_REQUIREMENT,
		ACTOR,
		CONTINUE,
		TRAVEL_REQUEST,
		CONFIRM,
		DESTINATION,
		DESTINATION_CANCEL,
		DESTINATION_UNAVAILABLE
	}

	private final WorldPoint origin;
	private final WorldPoint destination;
	private final int actorId;
	private final String actorName;
	private final String actorAction;
	private final String destinationOption;
	private final int fareCoins;
	private final WorldPoint actorTile;
	private final Stage stage;

	public NpcDialogueTransport(WorldPoint origin, WorldPoint destination, int actorId,
		String actorName, String actorAction, String destinationOption, int fareCoins,
		WorldPoint actorTile, Stage stage)
	{
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.actorId = actorId;
		this.actorName = Objects.requireNonNull(actorName, "actorName");
		this.actorAction = Objects.requireNonNull(actorAction, "actorAction");
		this.destinationOption = destinationOption == null ? "" : destinationOption;
		this.fareCoins = Math.max(0, fareCoins);
		this.actorTile = Objects.requireNonNull(actorTile, "actorTile");
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public int getActorId() { return actorId; }
	public String getActorName() { return actorName; }
	public String getActorAction() { return actorAction; }
	public String getDestinationOption() { return destinationOption; }
	public int getFareCoins() { return fareCoins; }
	public WorldPoint getActorTile() { return actorTile; }
	public Stage getStage() { return stage; }
}
