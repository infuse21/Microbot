package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HarmonyMonasteryDoorTest
{
	@Test
	public void incompleteMonasteryDoorRowsRemainSourceDisabled()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT && row.getObjectId() == 22119)
			.collect(Collectors.toList());
		assertEquals(0, rows.size());
	}
}
