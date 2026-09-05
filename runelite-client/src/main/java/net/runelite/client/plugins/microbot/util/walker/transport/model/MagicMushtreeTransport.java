package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable live stage for one directed Magic Mushtree route. */
public final class MagicMushtreeTransport
{
	public enum Stage
	{
		OBJECT,
		DESTINATION,
		DESTINATION_UNAVAILABLE
	}

	private final WorldPoint origin;
	private final WorldPoint destination;
	private final int objectId;
	private final String objectAction;
	private final String destinationName;
	private final WorldPoint objectTile;
	private final Stage stage;

	public MagicMushtreeTransport(WorldPoint origin, WorldPoint destination, int objectId,
		String objectAction, String destinationName, WorldPoint objectTile, Stage stage)
	{
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.objectId = objectId;
		this.objectAction = Objects.requireNonNull(objectAction, "objectAction");
		this.destinationName = Objects.requireNonNull(destinationName, "destinationName");
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public int getObjectId() { return objectId; }
	public String getObjectAction() { return objectAction; }
	public String getDestinationName() { return destinationName; }
	public WorldPoint getObjectTile() { return objectTile; }
	public Stage getStage() { return stage; }
}
