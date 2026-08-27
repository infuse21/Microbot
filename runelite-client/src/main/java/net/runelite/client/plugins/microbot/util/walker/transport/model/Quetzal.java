package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable live stage for one directed quetzal route. */
public final class Quetzal
{
	public enum Stage
	{
		NPC,
		DESTINATION
	}

	private final WorldPoint origin;
	private final WorldPoint destination;
	private final int npcId;
	private final String destinationName;
	private final WorldPoint npcTile;
	private final Stage stage;

	public Quetzal(WorldPoint origin, WorldPoint destination, int npcId,
		String destinationName, WorldPoint npcTile, Stage stage)
	{
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.npcId = npcId;
		this.destinationName = Objects.requireNonNull(destinationName, "destinationName");
		this.npcTile = Objects.requireNonNull(npcTile, "npcTile");
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public int getNpcId() { return npcId; }
	public String getDestinationName() { return destinationName; }
	public WorldPoint getNpcTile() { return npcTile; }
	public Stage getStage() { return stage; }
}
