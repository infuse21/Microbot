package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CharterShipPolicyTest
{
	@Test
	public void acceptsRealPaidCharterCatalogRow()
	{
		Transport charter = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.CHARTER_SHIP)
			.filter(candidate -> new WorldPoint(2760, 3238, 0).equals(candidate.getOrigin()))
			.filter(candidate -> "Catherby".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertNotNull(charter);
		assertTrue(CharterShipPolicy.isEligible(charter));
	}

	@Test
	public void rejectsUnpaidAndUnrelatedRows()
	{
		WorldPoint origin = new WorldPoint(2760, 3238, 0);
		WorldPoint destination = new WorldPoint(2792, 3417, 1);
		assertFalse(CharterShipPolicy.isEligible(new Transport(origin, destination,
			"Catherby", TransportType.CHARTER_SHIP, true,
			"Charter", "Trader Crewmember", 9318)));
		assertFalse(CharterShipPolicy.isEligible(new Transport(origin, destination,
			"Catherby", TransportType.SHIP, true,
			"Charter", "Trader Crewmember", 9318)));
	}

	@Test
	public void transformedNpcIdUsesExactActionNameAndCatalogOrigin()
	{
		Transport charter = realCharter();
		WorldPoint nearOrigin = charter.getOrigin().dx(1);

		assertTrue(CharterShipPolicy.isLiveNpcMatch(charter, charter.getName(),
			java.util.Arrays.asList("Talk-to", charter.getAction()), nearOrigin));
		assertFalse(CharterShipPolicy.isLiveNpcMatch(charter, "Sailor",
			Collections.singletonList(charter.getAction()), nearOrigin));
		assertFalse(CharterShipPolicy.isLiveNpcMatch(charter, charter.getName(),
			Collections.singletonList("Trade"), nearOrigin));
		assertFalse(CharterShipPolicy.isLiveNpcMatch(charter, charter.getName(),
			Collections.singletonList(charter.getAction()), charter.getOrigin().dx(4)));
	}

	private static Transport realCharter()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.CHARTER_SHIP)
			.findFirst().orElse(null);
	}
}
