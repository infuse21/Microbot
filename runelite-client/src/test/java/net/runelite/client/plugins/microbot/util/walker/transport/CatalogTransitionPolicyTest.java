package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CatalogTransitionPolicyTest
{
	private static final WorldPoint SURFACE = new WorldPoint(100, 100, 0);
	private static final WorldPoint UPSTAIRS = new WorldPoint(100, 100, 1);

	@Test
	public void completedGhostsAhoyBarrierAcceptsTheModernPassAction()
	{
		Transport barrier = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(candidate -> new WorldPoint(3651, 3486, 0).equals(candidate.getOrigin()))
			.filter(candidate -> candidate.getDestination().equals(new WorldPoint(3654, 3486, 0)))
			.findFirst().orElseThrow(() -> new AssertionError("completed barrier row missing"));

		assertEquals("Pass", Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Pass"}, barrier, true));
		assertNull(Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Pass"}, barrier, false));
	}

	@Test
	public void acceptsDirectStairsLaddersTrapdoorsCavesAndGangplanks()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Climb-up", "Ladder")));
		assertTrue(CatalogTransitionPolicy.isEligible(
			transport(UPSTAIRS, SURFACE, "Climb-down", "Trapdoor")));
		assertTrue(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(200, 200, 0), "Enter", "Cave entrance")));
		assertTrue(CatalogTransitionPolicy.isEligible(transport(
			new WorldPoint(3041, 3199, 1), new WorldPoint(3041, 3202, 0),
			"Cross", "Gangplank")));
	}

	@Test
	public void acceptsOnlyDirectItemFreeSceneChangingAgilityShortcuts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(105, 100, 0), "Walk-across", "Log balance")));
		assertTrue(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			UPSTAIRS, "Jump-up", "Weathered wall")));
		assertFalse(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(101, 100, 0), "Jump-onto", "Stepping stone")));
		assertFalse(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(105, 100, 0), "Grapple", "Rocks")));
		assertFalse(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(102, 100, 0), "Use", "Rope -> Boulder")));
	}

	@Test
	public void acceptsOnlyExactItemFreeOrdinaryDirectContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb-over", "Stile")));
		Transport rocks = transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb", "Rocks");
		assertTrue(CatalogTransitionPolicy.isEligible(rocks));
		rocks.setItemIdRequirements(Set.of(Set.of(3105)));
		assertFalse(CatalogTransitionPolicy.isEligible(rocks));
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb-over", "Wall")));
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Cross", "Stile")));
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb", "Climbing rocks")));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 32153)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 32152)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3937)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3999)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3997)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Forest", 3937)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			UPSTAIRS, "test", TransportType.TRANSPORT,
			false, "Enter", "Lift", 30258)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(UPSTAIRS,
			SURFACE, "test", TransportType.TRANSPORT,
			false, "Enter", "Lift", 30259)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			UPSTAIRS, "test", TransportType.TRANSPORT,
			false, "Enter", "Lift", 30257)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			UPSTAIRS, "test", TransportType.TRANSPORT,
			false, "Use", "Lift", 30258)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump on", "Rubber cap mushroom", 30606)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump on", "Rubber cap mushroom", 30605)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump", "Rubber cap mushroom", 30606)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump on", "Mushroom", 30606)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Magical Barrier", 31616)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(98, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Magical Barrier", 31617)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Magical Barrier", 31615)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Magical Barrier", 31616)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 31616)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "City Gate", 36518)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Exit", "City Gate", 36523)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "City Gate", 36517)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "City Gate", 36518)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Gate", 36518)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 20482)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 15771)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 20540)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 7258)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Passageway", 20482)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Tunnel", 2141)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Tunnel", 30174)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Exit", "Tunnel", 2141)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3220, 10088, 0), new WorldPoint(3220, 10084, 0),
			"test", TransportType.TRANSPORT, false, "Jump-to", "Pillar", 31561)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3200, 10196, 0), new WorldPoint(3204, 10196, 0),
			"test", TransportType.TRANSPORT, false, "Jump-to", "Pillar", 31561)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3220, 10088, 0), new WorldPoint(3220, 10084, 0),
			"test", TransportType.TRANSPORT, false, "Jump", "Pillar", 31561)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(104, 100, 0), "test", TransportType.TRANSPORT,
			false, "Jump-to", "Pillar", 20540)));
	}

	@Test
	public void acceptsOnlyExactBasaltCausewayContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3595, 0), new WorldPoint(2522, 3597, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump-across", "Basalt rock", 4551)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3597, 0), new WorldPoint(2522, 3595, 0),
			"test", TransportType.TRANSPORT, false, "Jump-to", "Beach", 4550)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2514, 3617, 0), new WorldPoint(2514, 3619, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump-to", "Rocky shore", 4559)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3595, 0), new WorldPoint(2522, 3597, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump-across", "Basalt rock", 4549)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3595, 0), new WorldPoint(2522, 3597, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump", "Basalt rock", 4551)));
	}

	@Test
	public void acceptsOnlyExactCatalogRaftContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2510, 3494, 0), new WorldPoint(2512, 3481, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Log raft", 1987)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(1761, 5362, 0), new WorldPoint(2531, 3446, 0),
			"test", TransportType.TRANSPORT, false, "Ride", "Aged log", 25216)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2567, 9680, 0), new WorldPoint(2606, 9692, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Raft", 2849)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2606, 9692, 0), new WorldPoint(2567, 9680, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Raft", 2849)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2567, 9680, 0), new WorldPoint(2607, 9692, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Raft", 2849)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2510, 3494, 0), new WorldPoint(2512, 3481, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Log raft", 1986)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2510, 3494, 0), new WorldPoint(2513, 3481, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Log raft", 1987)));
	}

	@Test
	public void acceptsOnlyExactGuardiansOfTheRiftBarrierContracts()
	{
		for (int x = 3613; x <= 3617; x++)
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
				new WorldPoint(x, 9482, 0), new WorldPoint(x, 9484, 0),
				"test", TransportType.TRANSPORT, false,
				"Quick-pass", "Barrier", 43700)));
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
				new WorldPoint(x, 9484, 0), new WorldPoint(x, 9482, 0),
				"test", TransportType.TRANSPORT, false,
				"Quick-pass", "Barrier", 43700)));
		}
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3612, 9482, 0), new WorldPoint(3612, 9484, 0),
			"test", TransportType.TRANSPORT, false,
			"Quick-pass", "Barrier", 43700)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3613, 9482, 0), new WorldPoint(3613, 9484, 0),
			"test", TransportType.TRANSPORT, false,
			"Pass", "Barrier", 43700)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3613, 9482, 0), new WorldPoint(3613, 9484, 0),
			"test", TransportType.TRANSPORT, false,
			"Quick-pass", "Barrier", 43699)));
	}

	@Test
	public void acceptsOnlyExactTempleOfTheEyePortalContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3104, 9573, 0), new WorldPoint(3615, 9470, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43841)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3615, 9470, 0), new WorldPoint(3104, 9573, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43692)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3104, 9573, 0), new WorldPoint(3615, 9470, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43840)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3104, 9573, 0), new WorldPoint(3615, 9471, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43841)));
	}

	@Test
	public void acceptsOnlyAuditedStepsOutsideSamePlaneLandingTolerance()
	{
		for (int id : new int[]{30189, 30190, 8966, 33261})
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
				new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
				new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Steps", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Rope", id)));
		}
		for (int id : new int[]{34530, 34531, 37417, 29993, 8729, 30191})
		{
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
		}
	}

	@Test
	public void acceptsOnlyExactCatacombsExitVineIdentities()
	{
		for (int id : new int[]{28895, 28896, 28897, 28898, 42350})
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb-up", "Vine", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb-down", "Vine", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb-up", "Root", id)));
		}
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
			"test", TransportType.TRANSPORT, false, "Climb-up", "Vine", 28899)));
	}

	@Test
	public void acceptsOnlyTheExactEnakhrasTempleSandPileExitContract()
	{
		WorldPoint temple = new WorldPoint(3124, 9328, 1);
		WorldPoint surface = new WorldPoint(3194, 2926, 0);
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb", "Sand pile", 10950)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb-up", "Sand pile", 10950)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb", "Sand pile", 10949)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb", "Sand heap", 10950)));
	}

	@Test
	public void acceptsOnlyFrozenDirectedManifestRows()
	{
		WorldPoint origin = new WorldPoint(1425, 2933, 0);
		WorldPoint destination = new WorldPoint(1427, 2933, 0);
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Pass-through", "Entryway", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin,
			new WorldPoint(1428, 2933, 0), "test", TransportType.TRANSPORT, false,
			"Pass-through", "Entryway", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Enter", "Entryway", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Pass-through", "Entrance", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Pass-through", "Entryway", 54708)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(1424, 2933, 0), destination, "test", TransportType.TRANSPORT,
			false, "Pass-through", "Entryway", 54707)));
	}

	@Test
	public void acceptsOnlyAuditedDirectHoleIdentities()
	{
		for (int id : new int[]{31791, 28915, 28919, 28920, 28921})
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Hole", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Hole", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Unknown", id)));
		}
		for (int id : new int[]{25154, 12656, 28918, 31790})
		{
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Hole", id)));
		}
	}

	@Test
	public void acceptsOnlyExactDirectedPohPortalRows()
	{
		WorldPoint outside = new WorldPoint(2953, 3224, 0);
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(outside, SURFACE,
			"RIMMINGTON -> PoH", TransportType.POH,
			true, "Home", "Portal", 15478)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"PoH -> RIMMINGTON", TransportType.POH,
			true, "Enter", "Portal", 4525)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"other", TransportType.POH,
			true, "Teleport", "Portal", 4525)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"other", TransportType.POH,
			true, "Enter", "Magic portal", 4525)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"other", TransportType.TELEPORTATION_PORTAL,
			true, "Enter", "Portal", 4525)));
	}

	@Test
	public void rejectsAdjacentDialogueOpenOnlyAndUnrelatedObjects()
	{
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(101, 100, 0), "Climb-up", "Ladder")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Pay-toll", "Stairs")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Open", "Trapdoor")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Climb-up", "Tree")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Cross", "Bridge")));
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String action, String name)
	{
		return new Transport(origin, destination, "test", TransportType.TRANSPORT,
			false, action, name, 123);
	}

	private static Transport agilityTransport(WorldPoint origin, WorldPoint destination,
		String action, String name)
	{
		return new Transport(origin, destination, "test", TransportType.AGILITY_SHORTCUT,
			false, action, name, 123);
	}
}
