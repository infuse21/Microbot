package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class ZanarisEntrancePolicyTest
{
	@Test
	public void shedHasQuestGatedStaffAndStaffFreeDiaryVariants()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT && row.getObjectId() == 2406)
			.collect(Collectors.toList());
		assertEquals(3, rows.size());
		for (Transport row : rows)
		{
			assertEquals(QuestState.FINISHED, row.getQuests().get(Quest.LOST_CITY));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertEquals(1, row.getVarbits().size());
			assertFalse(row.isConsumable());
			boolean staffRequired = !row.getItemIdRequirements().isEmpty();
			assertEquals(staffRequired, Rs2WalkerBankingPlanner.requiresBankPlanning(row));
			assertEquals(staffRequired ? 0 : 1, row.getVarbits().iterator().next().getValue());
			assertTrue(row.getVarbits().iterator().next().matches(staffRequired ? 0 : 1));
			assertFalse(row.getVarbits().iterator().next().matches(staffRequired ? 1 : 0));
			java.util.Map<Integer, Integer> items =
				Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(List.of(row, row));
			assertEquals(staffRequired ? 1 : 0,
				items.values().stream().mapToInt(Integer::intValue).sum());
			if (row.getItemIdRequirements().equals(Set.of(Set.of(9084))))
			{
				assertEquals(65, row.getSkillLevels()[Skill.MAGIC.ordinal()]);
				assertEquals(40, row.getSkillLevels()[Skill.DEFENCE.ordinal()]);
				row.getSkillLevels()[Skill.MAGIC.ordinal()] = 0;
				assertFalse(ZanarisEntrancePolicy.isEligible(row));
			}
		}
		assertEquals(Set.of(Set.of(), Set.of(Set.of(772)), Set.of(Set.of(9084))),
			rows.stream().map(Transport::getItemIdRequirements).collect(Collectors.toSet()));
	}

	@Test
	public void onlyTheCompleteUniqueClueMenuCanSelectZanaris()
	{
		String go = ZanarisEntrancePolicy.DESTINATION_OPTION;
		String shed = ZanarisEntrancePolicy.SHED_OPTION;
		assertEquals(0, ZanarisEntrancePolicy.destinationIndex(List.of(go, shed)));
		assertEquals(1, ZanarisEntrancePolicy.destinationIndex(List.of(shed, "<col=ffffff>" + go + "</col>")));
		for (List<String> options : List.of(List.of(go), List.of(go, go),
			List.of("Do not " + go, shed), List.of(go, "Yes"), List.of(go, shed, "Cancel")))
		{
			assertEquals(-1, ZanarisEntrancePolicy.destinationIndex(options));
		}
		assertEquals(-1, ZanarisEntrancePolicy.destinationIndex(null));
	}

	@Test
	public void equipmentAndDialogueAreStagesNotArrival()
	{
		List<String> empty = List.of();
		assertEquals(ZanarisEntrancePolicy.OPEN_INVENTORY,
			ZanarisEntrancePolicy.nextAction(true, false, false, false, empty, null));
		assertEquals(ZanarisEntrancePolicy.WIELD_STAFF,
			ZanarisEntrancePolicy.nextAction(true, false, true, false, empty,
				ZanarisEntrancePolicy.OPEN_INVENTORY));
		assertEquals("Open", ZanarisEntrancePolicy.nextAction(true, true, true, false,
			empty, ZanarisEntrancePolicy.WIELD_STAFF));
		assertEquals("Open", ZanarisEntrancePolicy.nextAction(false, false, false, false, empty, null));
		List<String> menu = List.of(ZanarisEntrancePolicy.DESTINATION_OPTION, ZanarisEntrancePolicy.SHED_OPTION);
		assertEquals(ZanarisEntrancePolicy.SELECT_DESTINATION,
			ZanarisEntrancePolicy.nextAction(true, true, true, true, menu, "Open"));
		assertNull(ZanarisEntrancePolicy.nextAction(true, false, true, true, menu, "Open"));
		assertNull(ZanarisEntrancePolicy.nextAction(false, false, true, true, List.of("Yes", "No"), "Open"));
		assertNull(ZanarisEntrancePolicy.nextAction(false, false, true, false, empty,
			ZanarisEntrancePolicy.SELECT_DESTINATION));
		assertNull(ZanarisEntrancePolicy.nextAction(false, false, true, true, empty, "Open"));
	}

	@Test
	public void unrelatedShedGeometryAndMissingAccessMetadataRemainUnsupported()
	{
		Transport row = new Transport(ZanarisEntrancePolicy.ORIGIN, ZanarisEntrancePolicy.DESTINATION,
			"", TransportType.TRANSPORT, true, "Open", "Door", 2406);
		assertFalse(CatalogTransitionPolicy.isEligible(row));
		Transport foreign = new Transport(new WorldPoint(3202, 3170, 0), ZanarisEntrancePolicy.DESTINATION,
			"", TransportType.TRANSPORT, true, "Open", "Door", 2406);
		assertFalse(CatalogTransitionPolicy.isEligible(foreign));
	}

	@Test
	public void pendingMenuSelectionCannotRedispatchDoorAndOnlyLandingClears()
	{
		CatalogTransitionScene scene = new CatalogTransitionScene()
		{
			@Override
			public CatalogTransition find(PlannedEdge edge)
			{
				throw new AssertionError("pending observation must carry the previous stage");
			}

			@Override
			public CatalogTransition observe(PlannedEdge edge, String action)
			{
				assertEquals(ZanarisEntrancePolicy.SELECT_DESTINATION, action);
				assertNull(ZanarisEntrancePolicy.nextAction(false, false, true, false, List.of(), action));
				return null;
			}
		};
		RouteInteraction pending = new RouteInteraction(1, 0, ZanarisEntrancePolicy.ORIGIN,
			ZanarisEntrancePolicy.DESTINATION, ZanarisEntrancePolicy.ORIGIN,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			ZanarisEntrancePolicy.SELECT_DESTINATION, true, 2406,
			ZanarisEntrancePolicy.ORIGIN, ZanarisEntrancePolicy.DESTINATION);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();
		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ZanarisEntrancePolicy.ORIGIN, scene, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED,
			scanner.observePending(pending, ZanarisEntrancePolicy.DESTINATION, scene, 13).getStatus());
	}
}
