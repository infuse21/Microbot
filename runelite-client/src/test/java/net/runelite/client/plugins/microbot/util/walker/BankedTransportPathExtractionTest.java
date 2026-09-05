package net.runelite.client.plugins.microbot.util.walker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BankedTransportPathExtractionTest
{
	@Test
	public void remoteCatalogOriginAlreadyInPathDoesNotDependOnLoadedScene()
	{
		WorldPoint origin = new WorldPoint(3703, 3487, 0);
		Transport transport = new Transport(origin, new WorldPoint(3792, 3560, 0),
			"Ghost captain", TransportType.BOAT, false, 6);
		AtomicBoolean attemptedSceneMapping = new AtomicBoolean();

		Collection<WorldPoint> origins = Rs2Walker.resolvePathTransportOrigins(
			transport,
			Map.of(origin, 235),
			() -> {
				attemptedSceneMapping.set(true);
				return java.util.List.of();
			});

		assertEquals(java.util.List.of(origin), java.util.List.copyOf(origins));
		assertFalse("a remote overworld route must not be filtered through the loaded scene",
			attemptedSceneMapping.get());
	}
}
