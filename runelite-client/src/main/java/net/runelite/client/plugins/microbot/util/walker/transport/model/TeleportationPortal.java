package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;

import java.util.Objects;

/** Immutable live object for one deterministic directed teleportation portal. */
public final class TeleportationPortal
{
	private final Rs2TileObjectModel object;
	private final WorldPoint objectTile;
	private final int catalogObjectId;
	private final String action;
	private final WorldPoint origin;
	private final WorldPoint destination;

	public TeleportationPortal(Rs2TileObjectModel object, WorldPoint objectTile,
		int catalogObjectId, String action, WorldPoint origin, WorldPoint destination)
	{
		this.object = object;
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.catalogObjectId = catalogObjectId;
		this.action = Objects.requireNonNull(action, "action");
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
	}

	public Rs2TileObjectModel getObject() { return object; }
	public WorldPoint getObjectTile() { return objectTile; }
	public int getCatalogObjectId() { return catalogObjectId; }
	public String getAction() { return action; }
	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
}
