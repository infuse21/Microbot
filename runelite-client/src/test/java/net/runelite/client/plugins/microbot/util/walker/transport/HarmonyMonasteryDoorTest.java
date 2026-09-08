package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HarmonyMonasteryDoorTest
{
	@Test
	public void monasteryDoorsRequireCompletedDemolitionButRemainUnmigrated()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT && row.getObjectId() == 22119)
			.collect(Collectors.toList());
		assertEquals(2, rows.size());
		for (Transport row : rows)
		{
			assertEquals(1, row.getVarbits().size());
			TransportVarbit gate = row.getVarbits().iterator().next();
			assertEquals(VarbitID.BRAIN_BARREL_SETUP, gate.getVarbitId());
			for (int state = 0; state < 5; state++) assertFalse(gate.matches(state));
			assertTrue(gate.matches(5));
			assertTrue(gate.matches(6));
			assertTrue(row.getItemIdRequirements().isEmpty());
			assertTrue(row.getQuests().isEmpty());
			assertFalse(AdjacentTransportPolicy.isEligible(row));
			assertFalse(CatalogTransitionPolicy.isEligible(row));
			assertEquals(Set.of(3804, 3806), Set.of(row.getOrigin().getX(), row.getDestination().getX()));
			assertEquals(2844, row.getOrigin().getY());
			assertEquals(2844, row.getDestination().getY());
		}
	}
}
