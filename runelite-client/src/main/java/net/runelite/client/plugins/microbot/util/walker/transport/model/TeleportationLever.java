package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;

import java.util.Objects;

/** One live stage of a catalog-backed teleportation-lever route. */
public final class TeleportationLever
{
	public enum Stage
	{
		OBJECT,
		WARNING,
		CONFIRM
	}

	private final Rs2TileObjectModel object;
	private final WorldPoint objectTile;
	private final int catalogObjectId;
	private final String action;
	private final WorldPoint origin;
	private final WorldPoint destination;
	private final Stage stage;

	public TeleportationLever(Rs2TileObjectModel object, WorldPoint objectTile,
		int catalogObjectId, String action, WorldPoint origin, WorldPoint destination,
		Stage stage)
	{
		this.object = object;
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.catalogObjectId = catalogObjectId;
		this.action = Objects.requireNonNull(action, "action");
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.stage = Objects.requireNonNull(stage, "stage");
	}

	public Rs2TileObjectModel getObject() { return object; }
	public WorldPoint getObjectTile() { return objectTile; }
	public int getCatalogObjectId() { return catalogObjectId; }
	public String getAction() { return action; }
	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public Stage getStage() { return stage; }
}
