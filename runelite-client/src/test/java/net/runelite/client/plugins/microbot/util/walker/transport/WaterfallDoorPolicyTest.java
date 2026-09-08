package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WaterfallDoorPolicyTest
{
	private static List<Transport> doors()
	{
		return Transport.loadAllFromResources().values().stream().flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT && row.getObjectId() == 2002)
			.collect(Collectors.toList());
	}

	@Test
	public void allInternalDoorsRequireOneReusableBankableKey()
	{
		List<Transport> rows = doors();
		assertEquals(4, rows.size());
		for (Transport row : rows)
		{
			assertEquals(Set.of(Set.of(298)), row.getItemIdRequirements());
			assertTrue(Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			assertFalse(row.isConsumable());
			boolean remote = row.getOrigin().distanceTo2D(row.getDestination()) == 38;
			assertEquals(remote, CatalogTransitionPolicy.isEligible(row));
			assertEquals(!remote, AdjacentTransportPolicy.isEligible(row));
			assertEquals(remote ? Map.of(Quest.WATERFALL_QUEST, QuestState.FINISHED) : Map.of(),
				row.getQuests());
		}
		assertEquals(Map.of(298, 1), Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(rows));
	}

	@Test
	public void remoteDoorsWaitForTheOtherRoomNotDoorDisappearance()
	{
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		for (Transport row : doors())
		{
			if (row.getOrigin().distanceTo2D(row.getDestination()) != 38) continue;
			WorldPoint from = row.getOrigin();
			WorldPoint to = row.getDestination();
			assertEquals(Set.of(new WorldPoint(2566, 9901, 0), new WorldPoint(2604, 9901, 0)),
				Set.of(from, to));
			RouteInteraction pending = new RouteInteraction(1, 0, from, to, from,
				RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
				"Open", true, 2002, from, to);
			CatalogTransition door = new CatalogTransition(null, from, 2002, "Open", "Open", from, to);
			assertEquals(RouteInteraction.Status.AVAILABLE,
				scanner.observePending(pending, from, edge -> door, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, from, edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.UNAVAILABLE,
				scanner.observePending(pending, new WorldPoint(2585, 9901, 0), edge -> null, 13).getStatus());
			assertEquals(RouteInteraction.Status.CLEARED,
				scanner.observePending(pending, to, edge -> null, 13).getStatus());
		}
	}

	@Test
	public void incompleteQuestMissingKeyAndForeignGeometryAreNotRemoteContracts()
	{
		for (int variant = 0; variant < 4; variant++)
		{
			Transport row = new Transport(new WorldPoint(variant == 3 ? 2565 : 2566, 9901, 0),
				new WorldPoint(2604, 9901, 0), "", TransportType.TRANSPORT, true, "Open", "Door", 2002);
			if (variant != 0) row.getQuests().put(Quest.WATERFALL_QUEST, QuestState.FINISHED);
			if (variant != 1) row.getItemIdRequirements().add(Set.of(variant == 2 ? 4446 : 298));
			assertFalse(CatalogTransitionPolicy.isEligible(row));
		}
	}
}
