package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.AdjacentTransport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WideGateTransportTest
{
	private static List<Transport> gates()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> Set.of(190, 12723, 12725, 8738, 8739).contains(row.getObjectId())).collect(Collectors.toList());
	}

	@Test
	public void fourteenExactGateRowsAreOwnedAndRetainQuestGates()
	{
		List<Transport> rows = gates();
		assertEquals(14, rows.size());
		for (Transport row : rows)
		{
			assertTrue(AdjacentTransportPolicy.isEligible(row));
			assertTrue(AdjacentTransportPolicy.isWideGate(row));
			if (row.getObjectId() != 190)
			{
				Quest quest = row.getObjectId() == 8738 || row.getObjectId() == 8739
					? Quest.BIOHAZARD : Quest.SWAN_SONG;
				assertEquals(QuestState.FINISHED, row.getQuests().get(quest));
				row.getQuests().put(quest, QuestState.IN_PROGRESS);
				assertFalse(AdjacentTransportPolicy.isEligible(row));
				row.getQuests().clear();
				assertFalse(AdjacentTransportPolicy.isEligible(row));
			}
		}
	}

	@Test
	public void persistentGateRequiresCrossingNotLateralAlignmentOrMiddleTile()
	{
		AdjacentTransportRouteScanner scanner = new AdjacentTransportRouteScanner();
		for (Transport row : gates())
		{
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
				"Open", true, row.getObjectId(), from, to);
			AdjacentTransport gate = new AdjacentTransport(null, from, row.getObjectId(), "Open", from, to);
			int direction = Integer.compare(to.getY(), from.getY());
			WorldPoint nearSide = direction != 0 ? new WorldPoint(to.getX(), to.getY() - direction, 0)
				: new WorldPoint(to.getX() - Integer.compare(to.getX(), from.getX()), to.getY(), 0);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> gate, 13).getStatus());
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, nearSide, edge -> gate, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> gate, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
		}
	}

	@Test
	public void unrelatedWideGateAndForeignStrongholdGeometryStayUnsupported()
	{
		for (int id : new int[]{190, 123456})
		{
			Transport row = new Transport(new WorldPoint(2461, 3381, 0),
				new WorldPoint(2461, 3384, 0), "", TransportType.TRANSPORT, true, "Open", "Gate", id);
			assertFalse(AdjacentTransportPolicy.isEligible(row));
		}
	}
}
