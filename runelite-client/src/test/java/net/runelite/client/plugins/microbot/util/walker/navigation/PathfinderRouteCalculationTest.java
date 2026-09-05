package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.WorldPointUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PathfinderRouteCalculationTest
{
	@Test
	public void auditedItemBatchPublishesNinetyNineRowsAndDefersTwentyNine()
	{
		int candidates = 0;
		int migrated = 0;
		int legacy = 0;
		for (Set<Transport> group : Transport.loadAllFromResources().values())
		{
			for (Transport row : group)
			{
				RouteEdge.Kind kind = PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(row));
				if (kind == RouteEdge.Kind.TRANSPORT)
				{
					legacy++;
				}
				if (row.getType() != TransportType.TELEPORTATION_ITEM || row.getDisplayInfo() == null
					|| !row.getDisplayInfo().contains(":"))
				{
					continue;
				}
				String family = row.getDisplayInfo().split(":", 2)[0].toLowerCase(java.util.Locale.ROOT);
				if (!family.matches(".*(?:cape$|ring|necklace|amulet|bracelet|pendant|talisman).*"))
				{
					continue;
				}
				candidates++;
				boolean deferred = family.equals("max cape") || family.equals("mythical cape")
					|| family.equals("camulet") || family.equals("burning amulet")
					|| row.getDisplayInfo().equals("Hunter cape: Black chinchompa");
				assertEquals(deferred ? RouteEdge.Kind.TRANSPORT : RouteEdge.Kind.ITEM_TELEPORT, kind);
				if (!deferred)
				{
					migrated++;
				}
			}
		}
		assertEquals(128, candidates);
		assertEquals(99, migrated);
		assertEquals(1559, legacy);
	}

	private static SplitFlagMap collisionMap;

	@BeforeClass
	public static void loadCollisionMap()
	{
		collisionMap = SplitFlagMap.fromResources();
	}

	@Test
	public void completedPathfinderIsSnapshottedWithPlannerIdentity()
	{
		WorldPoint start = new WorldPoint(3222, 3218, 0);
		WorldPoint target = new WorldPoint(3223, 3219, 0);
		Pathfinder pathfinder = new Pathfinder(config(), start, target);

		RoutePlan plan = new PathfinderRouteCalculation(pathfinder).calculate(41, 3);

		assertEquals(41, plan.getRequestId());
		assertEquals(3, plan.getGeneration());
		assertEquals(start, plan.getStart());
		assertEquals(target, plan.getEndpoint());
		assertTrue(plan.isComplete());
		assertFalse(plan.getSmoothedPath().isEmpty());
		assertTrue(plan.getSmoothedPath().size() <= plan.getRawPath().size());
	}

	@Test
	public void cancellationBeforeStartCannotPublishAPlan()
	{
		Pathfinder pathfinder = new Pathfinder(config(),
			new WorldPoint(3222, 3218, 0), new WorldPoint(3232, 3228, 0));
		PathfinderRouteCalculation calculation = new PathfinderRouteCalculation(pathfinder);
		calculation.cancel();

		try
		{
			calculation.calculate(1, 1);
			fail("cancelled calculation must not return a route plan");
		}
		catch (CancellationException expected)
		{
			// expected
		}
	}

	@Test
	public void directSpellTeleportEdgeIsPublishedAsEngineOwned()
	{
		WorldPoint start = new WorldPoint(3222, 3218, 0);
		WorldPoint target = new WorldPoint(3213, 3424, 0);
		PathfinderConfig config = config();
		Transport teleport = new Transport(target, "Varrock Teleport",
			TransportType.TELEPORTATION_SPELL, false, 19,
			Collections.singletonMap(net.runelite.api.Skill.MAGIC, 1));
		Set<Transport> transports = Collections.singleton(teleport);
		config.getTransports().put(start, transports);
		config.getTransportsPacked().put(WorldPointUtil.packWorldPoint(start), transports);

		RoutePlan plan = new PathfinderRouteCalculation(
			new Pathfinder(config, start, target)).calculate(42, 1);

		assertTrue(plan.isComplete());
		assertEquals(RouteEdge.Kind.SIMPLE_TELEPORT,
			plan.getRouteEdges().get(0).getKind());
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void seasonalCatalogTeleportEdgeIsPublishedAsEngineOwned()
	{
		WorldPoint target = new WorldPoint(1641, 3809, 0);
		Transport seasonal = new Transport(target, "Clue compass: 4. Arceuus Library",
			TransportType.SEASONAL_TRANSPORT, false, 20,
			Set.of(Collections.singleton(30363)));

		assertEquals(RouteEdge.Kind.SIMPLE_TELEPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(seasonal)));
	}

	@Test
	public void directNpcTransportEdgeIsPublishedAsEngineOwned()
	{
		WorldPoint start = new WorldPoint(2503, 3193, 0);
		WorldPoint target = new WorldPoint(2515, 3159, 0);
		PathfinderConfig config = config();
		Transport transport = new Transport(start, target, null, TransportType.NPC,
			false, "Follow", "Elkoy", 4968);
		Set<Transport> transports = Collections.singleton(transport);
		config.getTransports().put(start, transports);
		config.getTransportsPacked().put(WorldPointUtil.packWorldPoint(start), transports);

		RoutePlan plan = new PathfinderRouteCalculation(
			new Pathfinder(config, start, target)).calculate(43, 1);

		assertTrue(plan.isComplete());
		assertEquals(RouteEdge.Kind.NPC_TRANSPORT,
			plan.getRouteEdges().get(0).getKind());
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void npcVoyageAndGangplankChainIsEngineOwned()
	{
		WorldPoint start = new WorldPoint(2659, 2676, 0);
		WorldPoint shipLanding = new WorldPoint(3042, 3199, 1);
		WorldPoint gangplankOrigin = new WorldPoint(3041, 3199, 1);
		WorldPoint dock = new WorldPoint(3041, 3202, 0);
		Transport voyage = new Transport(start, shipLanding, null, TransportType.SHIP,
			false, "Travel", "Squire", 1769);
		Transport gangplank = new Transport(gangplankOrigin, dock, null, TransportType.TRANSPORT,
			false, "Cross", "Gangplank", 14305);
		Set<Transport> voyages = Collections.singleton(voyage);
		Set<Transport> gangplanks = Collections.singleton(gangplank);
		assertEquals(RouteEdge.Kind.NPC_TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(voyages));
		assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
			PathfinderRouteCalculation.classifyTransportEdge(gangplanks));
		RoutePlan plan = new RoutePlan(44, 1, start, Collections.singleton(dock),
			java.util.Arrays.asList(start, shipLanding, gangplankOrigin, dock),
			java.util.Arrays.asList(start, shipLanding, dock), true,
			java.util.Arrays.asList(
				new RouteEdge(0, start, shipLanding, RouteEdge.Kind.NPC_TRANSPORT),
				new RouteEdge(1, shipLanding, gangplankOrigin, RouteEdge.Kind.WALK),
				new RouteEdge(2, gangplankOrigin, dock,
					RouteEdge.Kind.CATALOG_TRANSITION)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void paidCharterCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport charter = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.CHARTER_SHIP)
			.findFirst().orElse(null);

		assertTrue(charter != null);
		assertEquals(RouteEdge.Kind.CHARTER_SHIP,
			PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(charter)));
	}

	@Test
	public void dialogueMenuCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport boaty = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> new net.runelite.api.coords.WorldPoint(1342, 3645, 0)
				.equals(candidate.getOrigin()))
			.filter(candidate -> "Shayzien".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertTrue(boaty != null);
		assertEquals(RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(boaty)));
		RoutePlan plan = new RoutePlan(45, 1, boaty.getOrigin(),
			Collections.singleton(boaty.getDestination()),
			java.util.Arrays.asList(boaty.getOrigin(), boaty.getDestination()),
			java.util.Arrays.asList(boaty.getOrigin(), boaty.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, boaty.getOrigin(),
				boaty.getDestination(), RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void fairyRingCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport ring = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.FAIRY_RING)
			.filter(candidate -> "AKR".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertTrue(ring != null);
		assertEquals(RouteEdge.Kind.FAIRY_RING,
			PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(ring)));
	}

	@Test
	public void spiritTreeCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport tree = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.SPIRIT_TREE)
			.filter(candidate -> "4: Grand Exchange".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertTrue(tree != null);
		assertEquals(RouteEdge.Kind.SPIRIT_TREE,
			PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(tree)));
	}

	@Test
	public void gnomeGliderCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport glider = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.GNOME_GLIDER)
			.filter(candidate -> "Kar-Hewo".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertTrue(glider != null);
		assertEquals(RouteEdge.Kind.GNOME_GLIDER,
			PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(glider)));
	}

	@Test
	public void quetzalCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport quetzal = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.QUETZAL)
			.filter(candidate -> "The Teomat".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertTrue(quetzal != null);
		assertEquals(RouteEdge.Kind.QUETZAL,
			PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(quetzal)));
	}

	@Test
	public void teleportationLeverCatalogEdgeIsPublishedAsEngineOwned()
	{
		Transport lever = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TELEPORTATION_LEVER)
			.findFirst().orElse(null);

		assertTrue(lever != null);
		assertEquals(RouteEdge.Kind.TELEPORTATION_LEVER,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(lever)));
		RoutePlan plan = new RoutePlan(46, 1, lever.getOrigin(),
			Collections.singleton(lever.getDestination()),
			java.util.Arrays.asList(lever.getOrigin(), lever.getDestination()),
			java.util.Arrays.asList(lever.getOrigin(), lever.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, lever.getOrigin(),
				lever.getDestination(), RouteEdge.Kind.TELEPORTATION_LEVER)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void exactTalkToShipRowsPublishWithoutAdmittingOtherTalkToShips()
	{
		java.util.List<Transport> herbert = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Cabin Boy Herbert".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(4, herbert.size());
		assertTrue(herbert.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT));
		java.util.List<Transport> shanks = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> "Captain Shanks".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(2, shanks.size());
		assertTrue(shanks.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT));

		java.util.List<Transport> dedicatedLegacy = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.BOAT
				|| candidate.getType() == TransportType.SHIP)
			.filter(candidate -> PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(candidate)) == RouteEdge.Kind.TRANSPORT)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(0, dedicatedLegacy.size());
		assertEquals(0, dedicatedLegacy.stream()
			.filter(candidate -> "Captain Shanks".equals(candidate.getName())).count());
		assertEquals(0, dedicatedLegacy.stream()
			.filter(candidate -> "Pirate Pete".equals(candidate.getName())).count());
		assertEquals(0, dedicatedLegacy.stream()
			.filter(candidate -> "Ghost captain".equals(candidate.getName())).count());
	}

	@Test
	public void completeCanoeCatalogIsPublishedAsEngineOwned()
	{
		java.util.List<Transport> canoes = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.CANOE)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(25, canoes.size());
		assertTrue(canoes.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CANOE));
		Transport canoe = canoes.get(0);
		RoutePlan plan = new RoutePlan(47, 1, canoe.getOrigin(),
			Collections.singleton(canoe.getDestination()),
			java.util.Arrays.asList(canoe.getOrigin(), canoe.getDestination()),
			java.util.Arrays.asList(canoe.getOrigin(), canoe.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, canoe.getOrigin(),
				canoe.getDestination(), RouteEdge.Kind.CANOE)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void completeMinecartCatalogIsPublishedAsEngineOwned()
	{
		java.util.List<Transport> minecarts = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.MINECART)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(254, minecarts.size());
		assertTrue(minecarts.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.MINECART));
		Transport minecart = minecarts.get(0);
		RoutePlan plan = new RoutePlan(48, 1, minecart.getOrigin(),
			Collections.singleton(minecart.getDestination()),
			java.util.Arrays.asList(minecart.getOrigin(), minecart.getDestination()),
			java.util.Arrays.asList(minecart.getOrigin(), minecart.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, minecart.getOrigin(),
				minecart.getDestination(), RouteEdge.Kind.MINECART)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void deterministicPortalCatalogIsPublishedAndAmbiguousRowsStayLocked()
	{
		java.util.List<Transport> portals = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate ->
				candidate.getType() == TransportType.TELEPORTATION_PORTAL)
			.collect(java.util.stream.Collectors.toList());
		java.util.List<Transport> deterministic = portals.stream()
			.filter(net.runelite.client.plugins.microbot.util.walker.transport
				.TeleportationPortalPolicy::isEligible)
			.collect(java.util.stream.Collectors.toList());
		java.util.List<Transport> ambiguous = portals.stream()
			.filter(candidate -> !net.runelite.client.plugins.microbot.util.walker.transport
				.TeleportationPortalPolicy.isEligible(candidate))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(100, portals.size());
		assertEquals(88, deterministic.size());
		assertEquals(12, ambiguous.size());
		assertTrue(deterministic.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TELEPORTATION_PORTAL));
		assertTrue(ambiguous.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT));
		Transport portal = deterministic.get(0);
		RoutePlan plan = new RoutePlan(49, 1, portal.getOrigin(),
			Collections.singleton(portal.getDestination()),
			java.util.Arrays.asList(portal.getOrigin(), portal.getDestination()),
			java.util.Arrays.asList(portal.getOrigin(), portal.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, portal.getOrigin(),
				portal.getDestination(), RouteEdge.Kind.TELEPORTATION_PORTAL)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void completeActiveMinigameCatalogIsPublishedAsEngineOwned()
	{
		java.util.List<Transport> minigames = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate ->
				candidate.getType() == TransportType.TELEPORTATION_MINIGAME)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(20, minigames.size());
		assertTrue(minigames.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.MINIGAME_TELEPORT));
		Transport minigame = minigames.get(0);
		net.runelite.api.coords.WorldPoint routeOrigin =
			new net.runelite.api.coords.WorldPoint(3081, 3475, 0);
		RoutePlan plan = new RoutePlan(50, 1, routeOrigin,
			Collections.singleton(minigame.getDestination()),
			java.util.Arrays.asList(routeOrigin, minigame.getDestination()),
			java.util.Arrays.asList(routeOrigin, minigame.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, routeOrigin,
				minigame.getDestination(), RouteEdge.Kind.MINIGAME_TELEPORT)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void completeMagicMushtreeCatalogIsPublishedAsEngineOwned()
	{
		java.util.List<Transport> mushtrees = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.MAGIC_MUSHTREE)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(24, mushtrees.size());
		assertTrue(mushtrees.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.MAGIC_MUSHTREE));
		Transport mushtree = mushtrees.get(0);
		RoutePlan plan = new RoutePlan(51, 1, mushtree.getOrigin(),
			Collections.singleton(mushtree.getDestination()),
			java.util.Arrays.asList(mushtree.getOrigin(), mushtree.getDestination()),
			java.util.Arrays.asList(mushtree.getOrigin(), mushtree.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, mushtree.getOrigin(),
				mushtree.getDestination(), RouteEdge.Kind.MAGIC_MUSHTREE)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void completeHotAirBalloonCatalogIsPublishedAsEngineOwned()
	{
		java.util.List<Transport> balloons = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.HOT_AIR_BALLOON)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(225, balloons.size());
		assertTrue(balloons.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.HOT_AIR_BALLOON));
		Transport balloon = balloons.get(0);
		RoutePlan plan = new RoutePlan(52, 1, balloon.getOrigin(),
			Collections.singleton(balloon.getDestination()),
			java.util.Arrays.asList(balloon.getOrigin(), balloon.getDestination()),
			java.util.Arrays.asList(balloon.getOrigin(), balloon.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, balloon.getOrigin(),
				balloon.getDestination(), RouteEdge.Kind.HOT_AIR_BALLOON)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void deterministicAgilityCatalogIsPublishedAndUnsafeRowsStayLocked()
	{
		java.util.List<Transport> shortcuts = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.AGILITY_SHORTCUT)
			.collect(java.util.stream.Collectors.toList());
		long adjacent = shortcuts.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT).count();
		long transitions = shortcuts.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION).count();
		java.util.List<Transport> locked = shortcuts.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(274, shortcuts.size());
		assertEquals(12, adjacent);
		assertEquals(248, transitions);
		assertEquals(14, locked.size());
		assertEquals(8, locked.stream()
			.filter(candidate -> "Jump-onto".equals(candidate.getAction())).count());
		assertTrue(locked.stream().anyMatch(candidate ->
			!candidate.getItemIdRequirements().isEmpty()));
		assertTrue(locked.stream().anyMatch(candidate ->
			"Grapple".equals(candidate.getAction())));
	}

	@Test
	public void ordinaryStileCatalogIsPublishedAndOtherOrdinaryRowsStayLocked()
	{
		java.util.List<Transport> ordinary = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.collect(java.util.stream.Collectors.toList());
		java.util.List<Transport> stiles = ordinary.stream()
			.filter(candidate -> "Climb-over".equals(candidate.getAction()))
			.filter(candidate -> "Stile".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(54, stiles.size());
		assertEquals(4, stiles.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT).count());
		assertEquals(50, stiles.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION).count());
		assertEquals(1083, ordinary.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT).count());
		java.util.Set<Integer> directManifestIds = new java.util.HashSet<>(java.util.Arrays.asList(
			3516, 3526, 3944, 3945, 8742, 10721, 19690, 19691, 51644, 51647,
			54707, 57219, 57220));
		java.util.List<Transport> directManifestRows = ordinary.stream()
			.filter(candidate -> directManifestIds.contains(candidate.getObjectId()))
			.filter(candidate -> candidate.getCurrencyAmount() == 0)
			.filter(candidate -> candidate.getItemIdRequirements().isEmpty())
			.filter(candidate -> candidate.getQuests().isEmpty())
			.filter(candidate -> candidate.getVarbits().isEmpty())
			.filter(candidate -> candidate.getVarplayers().isEmpty())
			.collect(java.util.stream.Collectors.toList());
		assertEquals(30, directManifestRows.size());
		assertTrue(directManifestRows.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		java.util.List<Transport> sandPileExits = ordinary.stream()
			.filter(candidate -> candidate.getObjectId() == 10950)
			.filter(candidate -> "Climb".equals(candidate.getAction()))
			.filter(candidate -> "Sand pile".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(16, sandPileExits.size());
		assertTrue(sandPileExits.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		java.util.List<Transport> rubberCapMushrooms = ordinary.stream()
			.filter(candidate -> "Jump on".equals(candidate.getAction()))
			.filter(candidate -> "Rubber cap mushroom".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(5, rubberCapMushrooms.size());
		assertTrue(rubberCapMushrooms.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void exactAlKharidTollRowsUsePaidAdjacentOwnership()
	{
		java.util.List<Transport> tolls = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Pay-toll(10gp)".equals(candidate.getAction()))
			.filter(candidate -> "Gate".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(4, tolls.size());
		assertTrue(tolls.stream().allMatch(candidate -> candidate.getCurrencyAmount() == 10
			&& "Coins".equals(candidate.getCurrencyName())));
		assertTrue(tolls.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT));
	}

	@Test
	public void paidEnergyBarrierRowsUseDirectEngineOwnership()
	{
		java.util.List<Transport> unresolved = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 16105
				&& (candidate.getOrigin().equals(new WorldPoint(3651, 3485, 0))
					|| candidate.getOrigin().equals(new WorldPoint(3651, 3486, 0))))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(4, unresolved.stream().filter(candidate ->
			candidate.getObjectId() == 16105).count());
		assertTrue(unresolved.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void guardiansOfTheRiftBarrierRowsAreQuestGatedAndEngineOwned()
	{
		java.util.List<Transport> barriers = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 43700)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(10, barriers.size());
		assertTrue(barriers.stream().allMatch(candidate ->
			candidate.getQuests().equals(Collections.singletonMap(
				Quest.TEMPLE_OF_THE_EYE, QuestState.FINISHED))));
		assertEquals(5, barriers.stream().filter(candidate ->
			candidate.getOrigin().getY() == 9482
				&& candidate.getVarbits().stream().anyMatch(requirement ->
					requirement.getVarbitId() == 13691 && requirement.getValue() == 0)).count());
		assertTrue(barriers.stream().filter(candidate -> candidate.getOrigin().getY() == 9484)
			.allMatch(candidate -> candidate.getVarbits().isEmpty()));
		assertTrue(barriers.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void templeOfTheEyePortalPairIsEngineOwned()
	{
		java.util.List<Transport> portals = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 43841
				|| candidate.getObjectId() == 43692)
			.filter(candidate -> "Enter".equals(candidate.getAction()))
			.filter(candidate -> "Portal".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(2, portals.size());
		assertTrue(portals.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void deterministicRaftExitsUseDirectEngineOwnership()
	{
		java.util.List<Transport> raftExits = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 1987
				|| candidate.getObjectId() == 25216)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(4, raftExits.size());
		assertTrue(raftExits.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		java.util.List<Transport> hazeelRafts = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 2849)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(2, hazeelRafts.size());
		assertTrue(hazeelRafts.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void fremennikBoatCatalogHasOnlyCanonicalQuestGatedRows()
	{
		java.util.List<Transport> boats = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> "Fremennik Boat".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 37408
				|| candidate.getObjectId() == 37432)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(7, boats.size());
		assertTrue(boats.stream().allMatch(candidate ->
			candidate.getType() == TransportType.BOAT
				&& candidate.getQuests().get(Quest.THE_FREMENNIK_EXILES)
					== net.runelite.api.QuestState.FINISHED));
		assertTrue(boats.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.NPC_TRANSPORT));
	}

	@Test
	public void completeBasaltCausewayUsesDirectEngineOwnership()
	{
		java.util.List<Transport> causeway = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() >= 4550
				&& candidate.getObjectId() <= 4559)
			.filter(candidate -> "Basalt rock".equals(candidate.getName())
				|| "Beach".equals(candidate.getName())
				|| "Rocky shore".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(10, causeway.size());
		assertTrue(causeway.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void exactMythsGuildMagicalBarriersUseDirectEngineOwnership()
	{
		java.util.List<Transport> barriers = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Pass".equals(candidate.getAction()))
			.filter(candidate -> "Magical Barrier".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 31616
				|| candidate.getObjectId() == 31617)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(10, barriers.size());
		assertTrue(barriers.stream().allMatch(candidate ->
			candidate.getQuests().containsKey(Quest.DRAGON_SLAYER_II)));
		assertTrue(barriers.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void exactPrifddinasCityGatesUseDirectEngineOwnership()
	{
		java.util.List<Transport> gates = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "City Gate".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 36518
				|| candidate.getObjectId() == 36519
				|| candidate.getObjectId() == 36522
				|| candidate.getObjectId() == 36523)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(32, gates.size());
		assertEquals(16, gates.stream().filter(candidate ->
			"Enter".equals(candidate.getAction())).count());
		assertEquals(16, gates.stream().filter(candidate ->
			"Exit".equals(candidate.getAction())).count());
		assertTrue(gates.stream().allMatch(candidate ->
			candidate.getQuests().containsKey(Quest.SONG_OF_THE_ELVES)));
		assertTrue(gates.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void exactTarnsLairAndRoguesDenPassagewaysUseDirectEngineOwnership()
	{
		java.util.List<Transport> passageways = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Enter".equals(candidate.getAction()))
			.filter(candidate -> "Passageway".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(53, passageways.size());
		assertTrue(passageways.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void exactFremennikSlayerDungeonExitTunnelsUseDirectEngineOwnership()
	{
		java.util.List<Transport> tunnels = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Enter".equals(candidate.getAction()))
			.filter(candidate -> "Tunnel".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(42, tunnels.size());
		assertEquals(3, tunnels.stream().filter(candidate -> candidate.getObjectId() == 2141)
			.filter(candidate -> PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(candidate)) == RouteEdge.Kind.CATALOG_TRANSITION).count());
		assertEquals(39, tunnels.stream().filter(candidate -> candidate.getObjectId() != 2141)
			.filter(candidate -> PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(candidate)) == RouteEdge.Kind.TRANSPORT).count());
	}

	@Test
	public void exactEasyRevenantCavesPillarsUseDirectEngineOwnership()
	{
		java.util.List<Transport> pillars = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Jump-to".equals(candidate.getAction()))
			.filter(candidate -> "Pillar".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(43, pillars.size());
		assertEquals(2, pillars.stream().filter(candidate ->
			candidate.getSkillLevels()[Skill.AGILITY.ordinal()] == 65).count());
		assertEquals(2, pillars.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION).count());
		assertEquals(41, pillars.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT).count());
	}

	@Test
	public void dorgeshKaanEntryDoorsRequireQuestCompletionAndRemainLegacyOwned()
	{
		java.util.List<Transport> doors = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 6919)
			.filter(candidate -> "Open".equals(candidate.getAction()))
			.filter(candidate -> "Door".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(3, doors.size());
		for (Transport door : doors)
		{
			assertEquals(Collections.singletonMap(Quest.DEATH_TO_THE_DORGESHUUN,
				QuestState.FINISHED), door.getQuests());
			assertEquals(RouteEdge.Kind.TRANSPORT,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(door)));
		}
	}

	@Test
	public void auditedStepsPublishWithoutAdmittingUnverifiedOrShortLinks()
	{
		Set<Integer> supportedIds = Set.of(30189, 30190, 8966, 33261);
		java.util.List<Transport> steps = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> "Climb".equals(row.getAction()) && "Steps".equals(row.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(22, steps.size());
		int supported = 0;
		java.util.Map<String, WorldPoint> inputs = new HashMap<>();
		for (Transport step : steps)
		{
			boolean eligible = supportedIds.contains(step.getObjectId());
			assertEquals(eligible ? RouteEdge.Kind.CATALOG_TRANSITION : RouteEdge.Kind.TRANSPORT,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(step)));
			if (eligible)
			{
				supported++;
				assertTrue(step.getItemIdRequirements().isEmpty());
				assertEquals(0, step.getCurrencyAmount());
				assertTrue(step.getOrigin().getPlane() != step.getDestination().getPlane()
					|| step.getOrigin().distanceTo2D(step.getDestination()) > 2);
				WorldPoint previous = inputs.putIfAbsent(step.getOrigin() + ":" + step.getObjectId(),
					step.getDestination());
				assertTrue(previous == null || previous.equals(step.getDestination()));
			}
		}
		assertEquals(8, supported);
	}

	@Test
	public void catacombsExitVinesNeverRequireTheirOwnUnlock()
	{
		java.util.List<Transport> vines = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> "Climb-up".equals(row.getAction()) && "Vine".equals(row.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(20, vines.size());
		for (Transport vine : vines)
		{
			assertTrue(vine.getVarbits().isEmpty());
			assertTrue(vine.getVarplayers().isEmpty());
			assertTrue(vine.getQuests().isEmpty());
			assertTrue(vine.getItemIdRequirements().isEmpty());
			assertEquals(0, vine.getCurrencyAmount());
		}
	}

	@Test
	public void exactCatacombsExitVinesPublishAsDirectedEngineTransitions()
	{
		Set<Integer> expectedIds = Set.of(28895, 28896, 28897, 28898, 42350);
		java.util.List<Transport> vines = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> "Climb-up".equals(row.getAction()) && "Vine".equals(row.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(20, vines.size());
		assertEquals(expectedIds, vines.stream().map(Transport::getObjectId)
			.collect(java.util.stream.Collectors.toSet()));
		java.util.Map<WorldPoint, Long> destinations = vines.stream().collect(
			java.util.stream.Collectors.groupingBy(Transport::getDestination,
				java.util.stream.Collectors.counting()));
		assertEquals(5, destinations.size());
		assertTrue(destinations.values().stream().allMatch(count -> count == 4));
		java.util.Map<String, WorldPoint> inputs = new HashMap<>();
		for (Transport vine : vines)
		{
			assertEquals(RouteEdge.Kind.CATALOG_TRANSITION,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(vine)));
			WorldPoint previous = inputs.putIfAbsent(vine.getOrigin() + ":" + vine.getObjectId(),
				vine.getDestination());
			assertTrue(previous == null || previous.equals(vine.getDestination()));
		}
	}

	@Test
	public void catacombsHolesRequireTheirOwnLocationUnlock()
	{
		java.util.Map<WorldPoint, Integer> unlocks = java.util.Map.of(
			new WorldPoint(1650, 9987, 0), 5088,
			new WorldPoint(1719, 10101, 0), 5089,
			new WorldPoint(1617, 10101, 0), 5090);
		java.util.List<Transport> holes = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> "Enter".equals(row.getAction()) && "Hole".equals(row.getName()))
			.filter(row -> unlocks.containsKey(row.getDestination()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(12, holes.size());
		for (java.util.Map.Entry<WorldPoint, Integer> unlock : unlocks.entrySet())
		{
			assertEquals(4, holes.stream().filter(row -> unlock.getKey().equals(row.getDestination())).count());
		}
		for (Transport hole : holes)
		{
			assertEquals(1, hole.getVarbits().size());
			net.runelite.client.plugins.microbot.shortestpath.TransportVarbit requirement =
				hole.getVarbits().iterator().next();
			assertEquals(unlocks.get(hole.getDestination()).intValue(), requirement.getVarbitId());
			assertFalse(requirement.matches(0));
			assertTrue(requirement.matches(1));
			for (int unlockedLocation : unlocks.values())
			{
				boolean eligible = hole.getVarbits().stream().allMatch(condition ->
					condition.matches(condition.getVarbitId() == unlockedLocation ? 1 : 0));
				assertEquals(unlocks.get(hole.getDestination()) == unlockedLocation, eligible);
			}
		}
	}

	@Test
	public void exactDirectHolesPublishWithoutMigratingQuestHoles()
	{
		Set<Integer> supportedIds = Set.of(31791, 28915, 28919, 28920, 28921);
		java.util.List<Transport> holes = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> "Enter".equals(row.getAction()) && "Hole".equals(row.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(27, holes.size());
		int supported = 0;
		java.util.Map<String, WorldPoint> directedInputs = new HashMap<>();
		for (Transport hole : holes)
		{
			boolean eligible = supportedIds.contains(hole.getObjectId());
			assertEquals(eligible ? RouteEdge.Kind.CATALOG_TRANSITION : RouteEdge.Kind.TRANSPORT,
				PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(hole)));
			if (eligible)
			{
				supported++;
				assertTrue(hole.getItemIdRequirements().isEmpty());
				assertEquals(0, hole.getCurrencyAmount());
				String input = hole.getOrigin() + ":" + hole.getObjectId();
				WorldPoint previous = directedInputs.putIfAbsent(input, hole.getDestination());
				assertTrue(previous == null || previous.equals(hole.getDestination()));
			}
		}
		assertEquals(13, supported);
	}

	@Test
	public void exactWildernessSwordWebCatalogPublishesAsAdjacentClearance()
	{
		java.util.List<Transport> webs = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Slash".equals(candidate.getAction()))
			.filter(candidate -> "Web".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 733)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(28, webs.size());
		assertTrue(webs.stream().allMatch(candidate ->
			candidate.getItemIdRequirements().equals(Set.of(Set.of(13108, 13109, 13110, 13111)))));
		assertTrue(webs.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT));
	}

	@Test
	public void molchMysticalBarriersPublishAndKaruulmHazardStaysLocked()
	{
		java.util.List<Transport> barriers = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Pass".equals(candidate.getAction()))
			.filter(candidate -> "Mystical barrier".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(20, barriers.size());
		assertEquals(16, barriers.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT).count());
		assertEquals(4, barriers.stream().filter(candidate -> candidate.getObjectId() == 34542)
			.filter(candidate -> PathfinderRouteCalculation.classifyTransportEdge(
				Collections.singleton(candidate)) == RouteEdge.Kind.TRANSPORT).count());
	}

	@Test
	public void exactDenseForestCatalogPublishesWithoutBroadeningEnterActions()
	{
		java.util.List<Transport> denseForest = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Enter".equals(candidate.getAction()))
			.filter(candidate -> "Dense forest".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(46, denseForest.size());
		assertTrue(denseForest.stream().allMatch(candidate ->
			candidate.getObjectId() == 3937 || candidate.getObjectId() == 3938
				|| candidate.getObjectId() == 3939 || candidate.getObjectId() == 3998
				|| candidate.getObjectId() == 3999));
		assertTrue(denseForest.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		Transport unrelated = new Transport(new WorldPoint(100, 100, 0),
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3997);
		assertEquals(RouteEdge.Kind.TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(unrelated)));
	}

	@Test
	public void itemFreeOrdinaryClimbRocksPublishAndBootGatedRowsStayLocked()
	{
		java.util.List<Transport> rocks = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Climb".equals(candidate.getAction()))
			.filter(candidate -> "Rocks".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(55, rocks.size());
		assertEquals(4, rocks.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT).count());
		assertEquals(49, rocks.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION).count());
		java.util.List<Transport> locked = rocks.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT).collect(java.util.stream.Collectors.toList());
		assertEquals(2, locked.size());
		assertTrue(locked.stream().allMatch(candidate ->
			candidate.getItemIdRequirements().stream()
				.anyMatch(group -> group.contains(3105))));
	}

	@Test
	public void exactLithkrenVaultBarriersPublishWithoutBroadeningOtherBarriers()
	{
		java.util.List<Transport> barriers = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Pass".equals(candidate.getAction()))
			.filter(candidate -> "Barrier".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(48, barriers.size());
		assertTrue(barriers.stream().allMatch(candidate -> candidate.getObjectId() == 32153));
		assertTrue(barriers.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
		Transport unrelated = new Transport(new WorldPoint(100, 100, 0),
			new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 32152);
		assertEquals(RouteEdge.Kind.TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(unrelated)));
	}

	@Test
	public void exactFeroxEntryBarriersPublishWithoutBroadeningPassThrough()
	{
		java.util.List<Transport> barriers = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Pass-Through".equals(candidate.getAction()))
			.filter(candidate -> "Barrier".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(16, barriers.size());
		assertTrue(barriers.stream().allMatch(candidate ->
			candidate.getObjectId() == 39652 || candidate.getObjectId() == 39653));
		assertTrue(barriers.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.ADJACENT_TRANSPORT));
		Transport unrelated = new Transport(new WorldPoint(100, 100, 0),
			new WorldPoint(101, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass-Through", "Barrier", 39651);
		assertEquals(RouteEdge.Kind.TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(unrelated)));
	}

	@Test
	public void completeWildernessDitchCatalogUsesStagedEngineOwnership()
	{
		java.util.List<Transport> ditches = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Cross".equals(candidate.getAction()))
			.filter(candidate -> "Wilderness Ditch".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(670, ditches.size());
		assertTrue(ditches.stream().allMatch(candidate -> candidate.getObjectId() == 23271));
		assertTrue(ditches.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.WILDERNESS_DITCH));
		Transport unrelated = new Transport(new WorldPoint(100, 100, 0),
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Cross", "Bridge", 23271);
		assertEquals(RouteEdge.Kind.TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(unrelated)));
	}

	@Test
	public void deterministicEnergyBarriersUseDirectEngineOwnership()
	{
		java.util.List<Transport> barriers = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 16105)
			.filter(candidate -> "Pay-toll(2-Ecto)".equals(candidate.getAction()))
			.filter(candidate -> "Energy Barrier".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());
		long supported = barriers.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION).count();
		long locked = barriers.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT).count();

		assertEquals(16, barriers.size());
		assertEquals(16, supported);
		assertEquals(0, locked);
	}

	@Test
	public void deterministicJungleObstaclesUseItemGatedEngineOwnership()
	{
		java.util.List<Transport> jungle = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Chop-down".equals(candidate.getAction()))
			.filter(candidate -> "Jungle Bush".equals(candidate.getName())
				|| "Jungle tree".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 2889
				|| candidate.getObjectId() == 2890 || candidate.getObjectId() == 2892
				|| candidate.getObjectId() == 2893)
			.collect(java.util.stream.Collectors.toList());
		long supported = jungle.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.JUNGLE_OBSTACLE).count();
		java.util.List<Transport> locked = jungle.stream().filter(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(92, jungle.size());
		assertEquals(76, supported);
		assertEquals(16, locked.size());
		assertEquals(8, locked.stream().map(candidate -> candidate.getOrigin().toString()
			+ ":" + candidate.getObjectId()).distinct().count());
		assertTrue(jungle.stream().allMatch(candidate ->
			!candidate.getItemIdRequirements().isEmpty()));
	}

	@Test
	public void brimhavenVinesUseItemGatedEngineOwnership()
	{
		java.util.List<Transport> vines = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Chop-down".equals(candidate.getAction()))
			.filter(candidate -> "Vines".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() >= 21731
				&& candidate.getObjectId() <= 21735)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(10, vines.size());
		assertTrue(vines.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.JUNGLE_OBSTACLE));
		assertTrue(vines.stream().allMatch(candidate ->
			!candidate.getItemIdRequirements().isEmpty()));
	}

	@Test
	public void everyChasmOfFireLiftUsesDirectEngineOwnership()
	{
		java.util.List<Transport> lifts = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Enter".equals(candidate.getAction()))
			.filter(candidate -> "Lift".equals(candidate.getName()))
			.filter(candidate -> candidate.getObjectId() == 30258
				|| candidate.getObjectId() == 30259)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(16, lifts.size());
		assertTrue(lifts.stream().allMatch(candidate ->
			candidate.getOrigin().getPlane() != candidate.getDestination().getPlane()));
		assertTrue(lifts.stream().allMatch(candidate ->
			candidate.getItemIdRequirements().isEmpty()
				&& candidate.getCurrencyAmount() == 0));
		assertTrue(lifts.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.CATALOG_TRANSITION));
	}

	@Test
	public void everyShiloVillageTravelCartUsesPaidEngineOwnership()
	{
		java.util.List<Transport> carts = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> "Pay-fare".equals(candidate.getAction()))
			.filter(candidate -> "Travel cart".equals(candidate.getName()))
			.collect(java.util.stream.Collectors.toList());

		assertEquals(20, carts.size());
		assertTrue(carts.stream().allMatch(candidate ->
			candidate.getCurrencyAmount() == 200
				&& "Coins".equals(candidate.getCurrencyName())));
		assertTrue(carts.stream().allMatch(candidate ->
			candidate.getVarplayers().size() == 1
				&& candidate.getVarplayers().iterator().next().getVarplayerId() == 116
				&& candidate.getVarplayers().iterator().next().getValue() == 14));
		assertTrue(carts.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT));
	}

	@Test
	public void hotAirBalloonEdgeWinsOverTheSurfaceRoute()
	{
		Transport balloon = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.HOT_AIR_BALLOON)
			.filter(candidate -> "Varrock".equals(candidate.getDisplayInfo()))
			.filter(candidate -> candidate.getOrigin().getX() == 2939)
			.filter(candidate -> candidate.getOrigin().getY() == 3423)
			.findFirst().orElse(null);

		assertTrue(balloon != null);
		WorldPoint start = new WorldPoint(2945, 3424, 0);
		RoutePlan approach = new PathfinderRouteCalculation(new Pathfinder(config(), start,
			balloon.getOrigin())).calculate(53, 1);
		assertTrue("balloon basket must be reachable from its approach", approach.isComplete());
		PathfinderConfig config = config();
		Set<Transport> transports = Collections.singleton(balloon);
		config.getTransports().put(balloon.getOrigin(), transports);
		config.getTransportsPacked().put(
			WorldPointUtil.packWorldPoint(balloon.getOrigin()), transports);

		RoutePlan plan = new PathfinderRouteCalculation(new Pathfinder(config,
			start, balloon.getDestination())).calculate(53, 1);

		assertTrue(plan.isComplete());
		assertTrue("route did not publish the selected balloon edge",
			plan.getRouteEdges().stream().anyMatch(edge ->
				edge.getKind() == RouteEdge.Kind.HOT_AIR_BALLOON));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void randomWildernessObeliskCatalogRemainsLegacyLocked()
	{
		java.util.List<Transport> obelisks = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.WILDERNESS_OBELISK)
			.collect(java.util.stream.Collectors.toList());
		java.util.List<Transport> remoteObelisks = obelisks.stream()
			.filter(candidate -> candidate.getOrigin().distanceTo2D(
				candidate.getDestination()) > 2)
			.collect(java.util.stream.Collectors.toList());
		java.util.Map<WorldPoint, Set<WorldPoint>> destinationsByInput = remoteObelisks.stream()
			.collect(java.util.stream.Collectors.groupingBy(Transport::getOrigin,
				java.util.stream.Collectors.mapping(Transport::getDestination,
					java.util.stream.Collectors.toSet())));

		assertEquals(318, obelisks.size());
		assertEquals(270, remoteObelisks.size());
		assertEquals(48, obelisks.size() - remoteObelisks.size());
		assertEquals(54, destinationsByInput.size());
		assertTrue(destinationsByInput.values().stream().allMatch(destinations ->
			destinations.size() == 5));
		assertTrue(obelisks.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.TRANSPORT));
		Transport obelisk = obelisks.get(0);
		RoutePlan plan = new RoutePlan(54, 1, obelisk.getOrigin(),
			Collections.singleton(obelisk.getDestination()),
			java.util.Arrays.asList(obelisk.getOrigin(), obelisk.getDestination()),
			java.util.Arrays.asList(obelisk.getOrigin(), obelisk.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, obelisk.getOrigin(),
				obelisk.getDestination(), RouteEdge.Kind.TRANSPORT)));
		assertFalse(plan.isEngineSupported());
	}

	@Test
	public void completeMagicCarpetCatalogUsesEngineOwnedDialogueLifecycle()
	{
		java.util.List<Transport> carpets = Transport.loadAllFromResources().values()
			.stream().flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.MAGIC_CARPET)
			.collect(java.util.stream.Collectors.toList());

		assertEquals(12, carpets.size());
		assertTrue(carpets.stream().allMatch(candidate ->
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(candidate))
				== RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT));
		Transport carpet = carpets.get(0);
		RoutePlan plan = new RoutePlan(52, 1, carpet.getOrigin(),
			Collections.singleton(carpet.getDestination()),
			java.util.Arrays.asList(carpet.getOrigin(), carpet.getDestination()),
			java.util.Arrays.asList(carpet.getOrigin(), carpet.getDestination()), true,
			Collections.singletonList(new RouteEdge(0, carpet.getOrigin(),
				carpet.getDestination(), RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT)));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void magicCarpetEdgeWinsOverTheSurfaceRoute()
	{
		Transport carpet = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.MAGIC_CARPET)
			.filter(candidate -> "Bedabin Camp".equals(candidate.getDisplayInfo()))
			.findFirst().orElse(null);

		assertTrue(carpet != null);
		WorldPoint start = new WorldPoint(3308, 3108, 0);
		RoutePlan approach = new PathfinderRouteCalculation(new Pathfinder(config(), start,
			carpet.getOrigin())).calculate(53, 1);
		assertTrue("carpet origin must be reachable from its live approach", approach.isComplete());
		PathfinderConfig config = config();
		Set<Transport> transports = Collections.singleton(carpet);
		config.getTransports().put(carpet.getOrigin(), transports);
		config.getTransportsPacked().put(
			WorldPointUtil.packWorldPoint(carpet.getOrigin()), transports);

		RoutePlan plan = new PathfinderRouteCalculation(new Pathfinder(config,
			start, new WorldPoint(3178, 3042, 0)))
			.calculate(53, 1);

		assertTrue(plan.isComplete());
		assertTrue("route did not publish the selected carpet edge",
			plan.getRouteEdges().stream().anyMatch(edge ->
				edge.getKind() == RouteEdge.Kind.NPC_DIALOGUE_TRANSPORT));
		assertTrue(plan.isEngineSupported());
	}

	@Test
	public void strongholdTreeDoorCatalogEdgeIsPublishedAsEngineOwned()
	{
		WorldPoint origin = new WorldPoint(2466, 3493, 0);
		WorldPoint destination = new WorldPoint(2466, 3491, 0);
		Transport door = Transport.loadAllFromResources().get(origin).stream()
			.filter(candidate -> destination.equals(candidate.getDestination()))
			.filter(candidate -> "Tree Door".equals(candidate.getName()))
			.findFirst().orElse(null);

		assertTrue(door != null);
		assertEquals(RouteEdge.Kind.ADJACENT_TRANSPORT,
			PathfinderRouteCalculation.classifyTransportEdge(Collections.singleton(door)));
	}

	private static PathfinderConfig config()
	{
		PathfinderConfig config = new PathfinderConfig(collisionMap, new HashMap<>(),
			Collections.emptyList(), null, null);
		try
		{
			// The default calculation cutoff flakes under parallel-suite load; use the
			// same generous budget as the route corpus harness.
			java.lang.reflect.Field cutoff =
				PathfinderConfig.class.getDeclaredField("calculationCutoffMillis");
			cutoff.setAccessible(true);
			cutoff.setLong(config, 10_000);
		}
		catch (Exception e)
		{
			throw new RuntimeException("cutoff injection failed", e);
		}
		return config;
	}
}
