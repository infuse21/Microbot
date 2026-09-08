package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class Rs2CatalogTransitionSceneTest
{
	@Test
	public void invisibleShadowLadderPublishesEquipmentStagesBeforeObjectLookup()
	{
		int checked = 0;
		for (java.util.Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (row.getObjectId() != 6560)
				{
					continue;
				}
				checked++;
				assertTrue(CatalogTransitionPolicy.isEligible(row));
				assertEquals(CatalogTransitionPolicy.VISIBILITY_RING_OPEN,
					Rs2CatalogTransitionScene.visibilityRingPreparation(row, false, false).getAction());
				assertEquals(CatalogTransitionPolicy.VISIBILITY_RING_WEAR,
					Rs2CatalogTransitionScene.visibilityRingPreparation(row, false, true).getAction());
				assertNull(Rs2CatalogTransitionScene.visibilityRingPreparation(row, true, false));
				assertNull(Rs2CatalogTransitionScene.visibilityRingPreparation(row, true, true));
				assertNull(Rs2CatalogTransitionScene.visibilityRingPreparation(row, false, true).getObject());
				assertEquals(row.getDestination(),
					Rs2CatalogTransitionScene.visibilityRingPreparation(row, false, true).getDestination());
				Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
					"", TransportType.TRANSPORT, true, "Climb-down", "Ladder", 6560);
				foreign.setItemIdRequirements(row.getItemIdRequirements());
				assertFalse(CatalogTransitionPolicy.isEligible(foreign));
				assertNull(Rs2CatalogTransitionScene.visibilityRingPreparation(foreign, false, false));
			}
		}
		assertEquals(4, checked);
	}

	@Test
	public void auditedDoorsPreserveRequirementsAndRejectForeignGeometry()
	{
		int checked = 0;
		for (java.util.Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				if (!java.util.Set.of(2000, 2010, 16774, 6919).contains(row.getObjectId()))
				{
					continue;
				}
				checked++;
				assertTrue(CatalogTransitionPolicy.isEligible(row));
				assertTrue(row.getItemIdRequirements().isEmpty());
				if (row.getObjectId() == 2010)
				{
					assertEquals(java.util.Map.of(net.runelite.api.Quest.WATERFALL_QUEST,
						net.runelite.api.QuestState.FINISHED), row.getQuests());
				}
				Transport foreign = new Transport(new WorldPoint(100, 100, 0), row.getDestination(),
					"", TransportType.TRANSPORT, true, "Open", "Door", row.getObjectId());
				assertFalse(CatalogTransitionPolicy.isEligible(foreign));
			}
		}
		assertEquals(6, checked);
	}

	@Test
	public void installedLiveActionContinuesWhileCatalogStillContainsOnlySetupVariant()
	{
		Transport setup = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(CatalogTransitionPolicy::isKalphiteRopeSetup)
			.filter(row -> row.getObjectId() == 23609)
			.findFirst().orElseThrow(AssertionError::new);
		java.util.List<Transport> staleCatalog = java.util.Collections.singletonList(setup);
		assertSame(setup, Rs2CatalogTransitionScene.findTransport(staleCatalog, "Use rope", 23609));
		assertTrue(Rs2CatalogTransitionScene.requiresRopePreparation(setup, "Use rope"));
		for (String liveAction : new String[]{"Climb-down", "Climb-down (normal)"})
		{
			String observed = Rs2CatalogTransitionScene.resolveLiveAction(new String[]{liveAction}, setup);
			assertEquals(liveAction, observed);
			assertSame(setup, Rs2CatalogTransitionScene.findTransport(staleCatalog, observed, 23609));
			assertFalse(Rs2CatalogTransitionScene.requiresRopePreparation(setup, observed));
		}
		assertNull(Rs2CatalogTransitionScene.findTransport(staleCatalog, "Use rope", 3827));
	}

	@Test
	public void catalogAndLiveClimbActionFormattingCanDiffer()
	{
		assertEquals("Climb up", Rs2CatalogTransitionScene.resolveAction(
			new String[]{"Climb up", null}, "Climb-up"));
		assertEquals("Climb-down", Rs2CatalogTransitionScene.resolveAction(
			new String[]{"Climb-down"}, "Climb down"));
	}

	@Test
	public void unrelatedActionCannotResolve()
	{
		assertNull(Rs2CatalogTransitionScene.resolveAction(
			new String[]{"Search", "Open"}, "Climb-up"));
	}

	@Test
	public void kalphiteNormalDescentCanResolveWithoutSelectingPrivateInstance()
	{
		Transport tunnel = new Transport(new WorldPoint(3508, 9498, 2),
			new WorldPoint(3508, 9493, 0), "", TransportType.TRANSPORT,
			true, "Climb-down", "Tunnel entrance", 23609);

		assertEquals("Climb-down (normal)", Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Climb-down (normal)", "Climb-down (private)", "Look-inside"}, tunnel));
		Transport unrelated = new Transport(new WorldPoint(100, 100, 0),
			new WorldPoint(100, 100, 1), "", TransportType.TRANSPORT,
			true, "Climb-down", "Tunnel entrance", 23609);
		assertNull(Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Climb-down (normal)", "Climb-down (private)"}, unrelated));
	}
}
