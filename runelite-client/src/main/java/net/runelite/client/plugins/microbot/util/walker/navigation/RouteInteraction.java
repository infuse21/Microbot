package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable live-scene observation for one interaction that blocks a raw route edge. */
public final class RouteInteraction
{
	public enum Kind
	{
		MINEABLE,
		DOOR,
		SIMPLE_TELEPORT,
		NPC_TRANSPORT,
		NPC_DIALOGUE_TRANSPORT,
		CHARTER_SHIP,
		ADJACENT_TRANSPORT,
		CATALOG_TRANSITION
	}

	public enum Status
	{
		AVAILABLE,
		CLEARED,
		UNAVAILABLE
	}

	private final long generation;
	private final int rawEdgeIndex;
	private final WorldPoint from;
	private final WorldPoint to;
	private final WorldPoint objectTile;
	private final Kind kind;
	private final Status status;
	private final String action;
	private final boolean ready;
	private final int objectId;
	private final WorldPoint crossingFrom;
	private final WorldPoint crossingTo;

	public RouteInteraction(long generation, int rawEdgeIndex, WorldPoint from, WorldPoint to,
		WorldPoint objectTile, Kind kind, Status status, String action, boolean ready)
	{
		this(generation, rawEdgeIndex, from, to, objectTile, kind, status, action, ready, -1);
	}

	public RouteInteraction(long generation, int rawEdgeIndex, WorldPoint from, WorldPoint to,
		WorldPoint objectTile, Kind kind, Status status, String action, boolean ready, int objectId)
	{
		this(generation, rawEdgeIndex, from, to, objectTile, kind, status, action, ready,
			objectId, from, to);
	}

	public RouteInteraction(long generation, int rawEdgeIndex, WorldPoint from, WorldPoint to,
		WorldPoint objectTile, Kind kind, Status status, String action, boolean ready, int objectId,
		WorldPoint crossingFrom, WorldPoint crossingTo)
	{
		this.generation = generation;
		this.rawEdgeIndex = rawEdgeIndex;
		this.from = Objects.requireNonNull(from, "from");
		this.to = Objects.requireNonNull(to, "to");
		this.objectTile = Objects.requireNonNull(objectTile, "objectTile");
		this.kind = Objects.requireNonNull(kind, "kind");
		this.status = Objects.requireNonNull(status, "status");
		this.action = Objects.requireNonNull(action, "action");
		this.ready = ready;
		this.objectId = objectId;
		this.crossingFrom = Objects.requireNonNull(crossingFrom, "crossingFrom");
		this.crossingTo = Objects.requireNonNull(crossingTo, "crossingTo");
	}

	public RouteInteraction withStatus(Status nextStatus, boolean nextReady)
	{
		return new RouteInteraction(generation, rawEdgeIndex, from, to, objectTile, kind,
			nextStatus, action, nextReady, objectId, crossingFrom, crossingTo);
	}

	public long getGeneration() { return generation; }
	public int getRawEdgeIndex() { return rawEdgeIndex; }
	public WorldPoint getFrom() { return from; }
	public WorldPoint getTo() { return to; }
	public WorldPoint getObjectTile() { return objectTile; }
	public Kind getKind() { return kind; }
	public Status getStatus() { return status; }
	public String getAction() { return action; }
	public boolean isReady() { return ready; }
	public int getObjectId() { return objectId; }
	public WorldPoint getCrossingFrom() { return crossingFrom; }
	public WorldPoint getCrossingTo() { return crossingTo; }
}
