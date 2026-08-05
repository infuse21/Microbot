package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Live object and immutable catalog fields for one eligible adjacent transport edge. */
public final class AdjacentTransport
{
	private final TileObject object;
	private final WorldPoint objectTile;
	private final int objectId;
	private final String action;
	private final WorldPoint origin;
	private final WorldPoint destination;

	public AdjacentTransport(TileObject object, WorldPoint objectTile, int objectId, String action)
	{
		this(object, objectTile, objectId, action, null, null);
	}

	public AdjacentTransport(TileObject object, WorldPoint objectTile, int objectId, String action,
		WorldPoint origin, WorldPoint destination)
	{
		this.object = object;
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.objectId = objectId;
		this.action = Objects.requireNonNull(action, "action");
		this.origin = origin;
		this.destination = destination;
	}

	public TileObject getObject() { return object; }
	public WorldPoint getObjectTile() { return objectTile; }
	public int getObjectId() { return objectId; }
	public String getAction() { return action; }
	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
}
