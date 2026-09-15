package net.runelite.client.plugins.microbot.util.walker.transport.model;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;

import java.util.Objects;

/** Immutable identity of one direct item/spell teleport selected by the route. */
public final class SimpleTeleport
{
	private final WorldPoint routeOrigin;
	private final WorldPoint destination;
	private final TransportType type;
	private final String displayInfo;
	private final String command;
	private final boolean available;

	public SimpleTeleport(WorldPoint routeOrigin, WorldPoint destination,
		TransportType type, String displayInfo)
	{
		this(routeOrigin, destination, type, displayInfo, displayInfo, true);
	}

	public SimpleTeleport(WorldPoint routeOrigin, WorldPoint destination,
		TransportType type, String displayInfo, String command, boolean available)
	{
		this.routeOrigin = Objects.requireNonNull(routeOrigin, "routeOrigin");
		this.destination = Objects.requireNonNull(destination, "destination");
		this.type = Objects.requireNonNull(type, "type");
		this.displayInfo = Objects.requireNonNull(displayInfo, "displayInfo");
		this.command = Objects.requireNonNull(command, "command");
		this.available = available;
	}

	public WorldPoint getRouteOrigin() { return routeOrigin; }
	public WorldPoint getDestination() { return destination; }
	public TransportType getType() { return type; }
	public String getDisplayInfo() { return displayInfo; }
	public String getCommand() { return command; }
	public boolean isAvailable() { return available; }
}
