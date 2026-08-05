package net.runelite.client.plugins.microbot.util.walker.door.model;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Exact live object and action for a standard door that blocks one planned route edge. */
public final class OrdinaryDoor
{
	private final TileObject object;
	private final WorldPoint tile;
	private final String action;

	public OrdinaryDoor(TileObject object, WorldPoint tile, String action)
	{
		this.object = Objects.requireNonNull(object, "object");
		this.tile = Objects.requireNonNull(tile, "tile");
		this.action = Objects.requireNonNull(action, "action");
	}

	public TileObject getObject() { return object; }
	public WorldPoint getTile() { return tile; }
	public String getAction() { return action; }
}
