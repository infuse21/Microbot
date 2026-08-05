package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransportEdgeMatcherTest
{
	@Test
	public void matchesExactDirectedEdge()
	{
		Transport transport = transport(point(3267, 3227), point(3268, 3227));
		assertEquals(Collections.singleton(transport), TransportEdgeMatcher.find(
			map(transport), point(3267, 3227), point(3268, 3227)));
	}

	@Test
	public void matchesDiagonalCrossingOfParallelGateLane()
	{
		Transport transport = transport(point(3267, 3227), point(3268, 3227));
		assertEquals(Collections.singleton(transport), TransportEdgeMatcher.find(
			map(transport), point(3267, 3228), point(3268, 3227)));
	}

	@Test
	public void matchesExactCrossPlaneTransition()
	{
		WorldPoint origin = new WorldPoint(3200, 3200, 0);
		WorldPoint destination = new WorldPoint(3200, 3200, 1);
		Transport transport = transport(origin, destination);

		assertEquals(Collections.singleton(transport),
			TransportEdgeMatcher.find(map(transport), origin, destination));
	}

	@Test
	public void doesNotMatchMovementAlongGateBoundaryOrReverseDirection()
	{
		Transport transport = transport(point(3267, 3227), point(3268, 3227));
		Map<WorldPoint, Set<Transport>> transports = map(transport);
		assertTrue(TransportEdgeMatcher.find(transports,
			point(3267, 3227), point(3267, 3228)).isEmpty());
		assertTrue(TransportEdgeMatcher.find(transports,
			point(3268, 3227), point(3267, 3227)).isEmpty());
	}

	private static Map<WorldPoint, Set<Transport>> map(Transport transport)
	{
		Map<WorldPoint, Set<Transport>> transports = new HashMap<>();
		transports.put(transport.getOrigin(), Collections.singleton(transport));
		return transports;
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination)
	{
		return new Transport(origin, destination, "test", TransportType.TRANSPORT,
			false, "Open", "Gate", 2786);
	}

	private static WorldPoint point(int x, int y)
	{
		return new WorldPoint(x, y, 0);
	}
}
