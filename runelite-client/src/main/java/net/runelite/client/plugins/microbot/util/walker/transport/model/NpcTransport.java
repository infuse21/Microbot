package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import java.util.Objects;

/** Immutable live identity for a one-click NPC, ship, or boat route transition. */
public final class NpcTransport
{
	private final WorldPoint origin;
	private final WorldPoint destination;
	private final TransportType type;
	private final int npcId;
	private final String name;
	private final String action;
	private final WorldPoint npcTile;

	public NpcTransport(WorldPoint origin, WorldPoint destination, TransportType type,
		int npcId, String name, String action, WorldPoint npcTile)
	{
		this.origin = Objects.requireNonNull(origin, "origin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.type = Objects.requireNonNull(type, "type");
		this.npcId = npcId;
		this.name = Objects.requireNonNull(name, "name");
		this.action = Objects.requireNonNull(action, "action");
		this.npcTile = Objects.requireNonNull(npcTile, "npcTile");
	}

	public WorldPoint getOrigin() { return origin; }
	public WorldPoint getDestination() { return destination; }
	public TransportType getType() { return type; }
	public int getNpcId() { return npcId; }
	public String getName() { return name; }
	public String getAction() { return action; }
	public WorldPoint getNpcTile() { return npcTile; }
}
