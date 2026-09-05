package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;

import java.util.Objects;

/** Immutable live stage for one directed minecart route. */
public final class MinecartTransport
{
	public enum Stage
	{
		UNEQUIP_WEAPON,
		UNEQUIP_SHIELD,
		OBJECT,
		DESTINATION,
		DESTINATION_UNAVAILABLE,
		RESTORE_INVENTORY,
		RESTORE_WEAPON,
		RESTORE_SHIELD,
		UNAVAILABLE
	}

	private final Rs2TileObjectModel object;
	private final WorldPoint objectTile;
	private final int catalogObjectId;
	private final String action;
	private final WorldPoint origin;
	private final WorldPoint destination;
	private final Stage stage;
	private final int originalWeaponId;
	private final int originalShieldId;
	private final boolean ready;

	public MinecartTransport(Rs2TileObjectModel object, WorldPoint objectTile,
		int catalogObjectId, String action, WorldPoint origin,
		WorldPoint destination, Stage stage, int originalWeaponId,
		int originalShieldId, boolean ready)
	{
		this.object = object;
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.catalogObjectId = catalogObjectId;
		this.action = Objects.requireNonNull(action, "action");
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.stage = Objects.requireNonNull(stage, "stage");
		this.originalWeaponId = originalWeaponId;
		this.originalShieldId = originalShieldId;
		this.ready = ready;
	}

	public Rs2TileObjectModel getObject() { return object; }
	public WorldPoint getObjectTile() { return objectTile; }
	public int getCatalogObjectId() { return catalogObjectId; }
	public String getAction() { return action; }
	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public Stage getStage() { return stage; }
	public int getOriginalWeaponId() { return originalWeaponId; }
	public int getOriginalShieldId() { return originalShieldId; }
	public boolean isReady() { return ready; }
}
