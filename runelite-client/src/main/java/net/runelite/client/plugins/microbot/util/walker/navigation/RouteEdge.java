package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/** Immutable raw-path edge classified once at route publication. */
public final class RouteEdge
{
	public enum Kind
	{
		WALK,
		SIMPLE_TELEPORT,
		ITEM_TELEPORT,
		NPC_TRANSPORT,
		NPC_DIALOGUE_TRANSPORT,
		CHARTER_SHIP,
		FAIRY_RING,
		SPIRIT_TREE,
		GNOME_GLIDER,
		QUETZAL,
		TELEPORTATION_LEVER,
		WILDERNESS_DITCH,
		JUNGLE_OBSTACLE,
		CANOE,
		MINECART,
		TELEPORTATION_PORTAL,
		MINIGAME_TELEPORT,
		MAGIC_MUSHTREE,
		HOT_AIR_BALLOON,
		ADJACENT_TRANSPORT,
		CATALOG_TRANSITION,
		TRANSPORT
	}

	private final int rawIndex;
	private final WorldPoint from;
	private final WorldPoint to;
	private final Kind kind;

	public RouteEdge(int rawIndex, WorldPoint from, WorldPoint to, Kind kind)
	{
		if (rawIndex < 0)
		{
			throw new IllegalArgumentException("rawIndex cannot be negative");
		}
		this.rawIndex = rawIndex;
		this.from = Objects.requireNonNull(from, "from");
		this.to = Objects.requireNonNull(to, "to");
		this.kind = Objects.requireNonNull(kind, "kind");
	}

	public int getRawIndex()
	{
		return rawIndex;
	}

	public WorldPoint getFrom()
	{
		return from;
	}

	public WorldPoint getTo()
	{
		return to;
	}

	public Kind getKind()
	{
		return kind;
	}
}
