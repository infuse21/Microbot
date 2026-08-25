package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable live stage for one directed fairy-ring route. */
public final class FairyRing
{
	public enum Stage
	{
		EQUIP_STAFF,
		OBJECT,
		ROTATE,
		TELEPORT,
		RESTORE_INVENTORY,
		RESTORE_WEAPON
	}

	private final WorldPoint origin;
	private final WorldPoint destination;
	private final String code;
	private final WorldPoint objectTile;
	private final int objectId;
	private final String action;
	private final Stage stage;
	private final int originalWeaponId;

	public FairyRing(WorldPoint origin, WorldPoint destination, String code,
		WorldPoint objectTile, int objectId, String action, Stage stage,
		int originalWeaponId)
	{
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.code = Objects.requireNonNull(code, "code");
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.objectId = objectId;
		this.action = Objects.requireNonNull(action, "action");
		this.stage = Objects.requireNonNull(stage, "stage");
		this.originalWeaponId = originalWeaponId;
	}

	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public String getCode() { return code; }
	public WorldPoint getObjectTile() { return objectTile; }
	public int getObjectId() { return objectId; }
	public String getAction() { return action; }
	public Stage getStage() { return stage; }
	public int getOriginalWeaponId() { return originalWeaponId; }
}
