package net.runelite.client.plugins.microbot.util.walker.navigation;

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
