package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MagicMushtreePolicyTest
{
	@Test
	public void acceptsAllTwentyFourGeneratedCatalogEdges()
	{
		List<Transport> mushtrees = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(transport -> transport.getType() == TransportType.MAGIC_MUSHTREE)
			.collect(Collectors.toList());

		assertEquals(24, mushtrees.size());
		assertTrue(mushtrees.stream().allMatch(MagicMushtreePolicy::isEligible));
		assertTrue(mushtrees.stream().allMatch(transport ->
			!transport.getOrigin().equals(transport.getDestination())));
	}

	@Test
	public void parsesDestinationAndRejectsUnknownShapes()
	{
		WorldPoint origin = new WorldPoint(3764, 3879, 1);
		WorldPoint destination = new WorldPoint(3676, 3755, 0);
		Transport valid = new Transport(origin, destination, "3. Sticky Swamp",
			TransportType.MAGIC_MUSHTREE, true, "Use", "Magic Mushtree", 30920);

		assertTrue(MagicMushtreePolicy.isEligible(valid));
		assertEquals("Sticky Swamp",
			MagicMushtreePolicy.destinationName(valid.getDisplayInfo()));
		assertEquals("magic-mushtree-destination:Sticky Swamp",
			MagicMushtreePolicy.destinationAction("Sticky Swamp"));
		assertFalse(MagicMushtreePolicy.isEligible(new Transport(origin, destination,
			"5. Unknown", TransportType.MAGIC_MUSHTREE, true,
			"Use", "Magic Mushtree", 30920)));
		assertFalse(MagicMushtreePolicy.isEligible(new Transport(origin, destination,
			"3. Sticky Swamp", TransportType.MAGIC_MUSHTREE, true,
			"Travel", "Magic Mushtree", 30920)));
	}
}
