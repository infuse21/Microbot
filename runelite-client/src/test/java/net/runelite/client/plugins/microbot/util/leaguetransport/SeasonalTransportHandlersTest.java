package net.runelite.client.plugins.microbot.util.leaguetransport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SeasonalTransportHandlersTest
{
	@Test
	public void defaultHandlerList_orderAndSize()
	{
		var handlers = SeasonalTransportHandlers.defaultHandlerList();
		assertTrue(handlers.size() >= 3);
		assertSame(SeasonalTransportHandlers.LEAGUES_AREA, handlers.get(0));
		assertSame(SeasonalTransportHandlers.MAP_OF_ALACRITY, handlers.get(1));
		assertSame(SeasonalTransportHandlers.CLUE_COMPASS, handlers.get(2));
	}

	@Test
	public void onlyKnownWellFormedRowsAreExecutable()
	{
		assertTrue(SeasonalTransportHandlers.canHandle(seasonal(
			"Map of Alacrity: Asgarnia - Falador wall")));
		assertTrue(SeasonalTransportHandlers.canHandle(seasonal(
			"Clue compass: B. Barbarian Village")));
		assertFalse(SeasonalTransportHandlers.canHandle(seasonal("Map of Alacrity: malformed")));
		assertFalse(SeasonalTransportHandlers.canHandle(seasonal("Unknown relic: Somewhere")));
	}

	@Test
	public void everyPackagedSeasonalRowHasAnExecutor()
	{
		List<Transport> seasonal = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(transport -> transport.getType() == TransportType.SEASONAL_TRANSPORT)
			.collect(Collectors.toList());

		assertEquals(169, seasonal.size());
		assertTrue(seasonal.stream().allMatch(SeasonalTransportHandlers::canHandle));
	}

	private static Transport seasonal(String display)
	{
		return new Transport(new WorldPoint(3200, 3200, 0), display,
			TransportType.SEASONAL_TRANSPORT, false, 20,
			Set.of(Collections.singleton(Rs2MapOfAlacrityTransport.ITEM_ID)));
	}
}
