package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable live stage for one exact paid charter-ship route. */
public final class CharterShip
{
	public enum Stage
	{
		NPC,
		DESTINATION,
		CONFIRM
	}

	private final WorldPoint origin;
	private final WorldPoint destination;
	private final int npcId;
	private final String npcName;
	private final String npcAction;
	private final String destinationName;
	private final WorldPoint npcTile;
	private final Stage stage;

	public CharterShip(WorldPoint origin, WorldPoint destination, int npcId,
		String npcName, String npcAction, String destinationName,
		WorldPoint npcTile, Stage stage)
	{
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.npcId = npcId;
		this.npcName = Objects.requireNonNull(npcName, "npcName");
		this.npcAction = Objects.requireNonNull(npcAction, "npcAction");
		this.destinationName = Objects.requireNonNull(destinationName, "destinationName");
		this.npcTile = Objects.requireNonNull(npcTile, "npcTile");
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public int getNpcId() { return npcId; }
	public String getNpcName() { return npcName; }
	public String getNpcAction() { return npcAction; }
	public String getDestinationName() { return destinationName; }
	public WorldPoint getNpcTile() { return npcTile; }
	public Stage getStage() { return stage; }
}
