package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Matches directed raw-route edges to enabled catalog transports, including parallel gate lanes. */
public final class TransportEdgeMatcher
{
	private TransportEdgeMatcher()
	{
	}

	public static Set<Transport> find(Map<WorldPoint, Set<Transport>> transports,
		WorldPoint from, WorldPoint to)
	{
		if (transports == null || transports.isEmpty() || from == null || to == null)
		{
			return Collections.emptySet();
		}
		Set<Transport> matches = new LinkedHashSet<>();
		Set<Transport> exact = transports.get(from);
		if (exact != null)
		{
			exact.stream().filter(transport -> transport != null
				&& to.equals(transport.getDestination())).forEach(matches::add);
		}
		if (from.getPlane() != to.getPlane())
		{
			return matches;
		}
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				Set<Transport> candidates = transports.get(new WorldPoint(from.getX() + dx,
					from.getY() + dy, from.getPlane()));
				if (candidates == null)
				{
					continue;
				}
				for (Transport candidate : candidates)
				{
					if (crosses(candidate, from, to))
					{
						matches.add(candidate);
					}
				}
			}
		}
		return matches;
	}

	private static boolean crosses(Transport transport, WorldPoint from, WorldPoint to)
	{
		if (transport == null || transport.getOrigin() == null || transport.getDestination() == null)
		{
			return false;
		}
		WorldPoint origin = transport.getOrigin();
		WorldPoint destination = transport.getDestination();
		if (origin.equals(from) && destination.equals(to))
		{
			return true;
		}
		if (from.distanceTo2D(to) > 1 || origin.getPlane() != from.getPlane()
			|| destination.getPlane() != to.getPlane() || origin.distanceTo2D(destination) != 1)
		{
			return false;
		}
		int transportDx = destination.getX() - origin.getX();
		int transportDy = destination.getY() - origin.getY();
		if (transportDx != 0)
		{
			return from.getX() == origin.getX() && to.getX() == destination.getX()
				&& Math.abs(from.getY() - origin.getY()) <= 1
				&& Math.abs(to.getY() - destination.getY()) <= 1;
		}
		return transportDy != 0 && from.getY() == origin.getY()
			&& to.getY() == destination.getY()
			&& Math.abs(from.getX() - origin.getX()) <= 1
			&& Math.abs(to.getX() - destination.getX()) <= 1;
	}
}
