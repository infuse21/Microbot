package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinigameTeleport;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MinigameTeleportRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(3081, 3475, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2442, 3093, 0);

	@Test
	public void advancesObservedUiStagesAndRetainsTerminalLanding()
	{
		MinigameTeleportRouteScanner scanner = new MinigameTeleportRouteScanner();
		MutableScene scene = new MutableScene(stage(MinigameTeleport.Stage.OPEN_GROUPING_TAB,
			"Castle Wars", ""));
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.MINIGAME_TELEPORT),
			0, 1, ORIGIN, scene);

		assertEquals(RouteInteraction.Kind.MINIGAME_TELEPORT, pending.getKind());
		assertEquals(MinigameTeleportPolicy.OPEN_GROUPING_TAB_ACTION, pending.getAction());
		assertTrue(pending.isReady());

		scene.current = stage(MinigameTeleport.Stage.OPEN_GROUPING, "Castle Wars", "");
		pending = scanner.observePending(pending, ORIGIN, scene);
		assertEquals(MinigameTeleportPolicy.OPEN_GROUPING_ACTION, pending.getAction());
		assertTrue(pending.isReady());

		scene.current = stage(MinigameTeleport.Stage.OPEN_DROPDOWN, "Castle Wars", "");
		pending = scanner.observePending(pending, ORIGIN, scene);
		assertEquals(MinigameTeleportPolicy.OPEN_DROPDOWN_ACTION, pending.getAction());
		assertTrue(pending.isReady());

		scene.current = stage(MinigameTeleport.Stage.WAIT_FOR_ACTIVITY,
			"Castle Wars", "");
		pending = scanner.observePending(pending, ORIGIN, scene);
		assertEquals(MinigameTeleportPolicy.WAIT_FOR_ACTIVITY_ACTION,
			pending.getAction());
		assertFalse(pending.isReady());

		scene.current = stage(MinigameTeleport.Stage.SELECT_ACTIVITY, "Castle Wars", "");
		pending = scanner.observePending(pending, ORIGIN, scene);
		assertEquals(MinigameTeleportPolicy.selectActivityAction("Castle Wars"),
			pending.getAction());

		scene.current = stage(MinigameTeleport.Stage.TELEPORT, "Castle Wars", "");
		pending = scanner.observePending(pending, ORIGIN, scene);
		assertEquals(MinigameTeleportPolicy.TELEPORT_ACTION, pending.getAction());

		scene.current = null;
		RouteInteraction travelling = scanner.observePending(pending, ORIGIN, scene);
		assertEquals(RouteInteraction.Status.AVAILABLE, travelling.getStatus());
		assertEquals(MinigameTeleportPolicy.TELEPORT_ACTION, travelling.getAction());
		assertFalse(travelling.isReady());

		RouteInteraction landed = scanner.observePending(travelling, DESTINATION, scene);
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void ratPitsDestinationAndUnavailableStatesAreExplicit()
	{
		MinigameTeleportRouteScanner scanner = new MinigameTeleportRouteScanner();
		MutableScene scene = new MutableScene(stage(
			MinigameTeleport.Stage.SELECT_DESTINATION, "Rat Pits", "Varrock"));
		RouteInteraction option = scanner.scan(plan(RouteEdge.Kind.MINIGAME_TELEPORT),
			0, 1, ORIGIN, scene);

		assertEquals(MinigameTeleportPolicy.selectDestinationAction("Varrock"),
			option.getAction());

		scene.current = stage(MinigameTeleport.Stage.UNAVAILABLE, "Rat Pits", "Varrock");
		RouteInteraction unavailable = scanner.observePending(option, ORIGIN, scene);
		assertEquals(RouteInteraction.Status.UNAVAILABLE, unavailable.getStatus());
		assertFalse(unavailable.isReady());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new MinigameTeleportRouteScanner().scan(
			plan(RouteEdge.Kind.TRANSPORT), 0, 1, ORIGIN,
			new MutableScene(stage(MinigameTeleport.Stage.TELEPORT,
				"Castle Wars", ""))));
	}

	private static MinigameTeleport stage(MinigameTeleport.Stage stage,
		String activity, String destinationOption)
	{
		String display = destinationOption.isEmpty() ? activity
			: activity + ": " + destinationOption;
		return new MinigameTeleport(ORIGIN, DESTINATION, display, activity,
			destinationOption, stage);
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements MinigameTeleportScene
	{
		private MinigameTeleport current;

		private MutableScene(MinigameTeleport current)
		{
			this.current = current;
		}

		@Override
		public MinigameTeleport find(PlannedEdge edge)
		{
			return current;
		}

		@Override
		public MinigameTeleport observe(PlannedEdge edge, String pendingAction)
		{
			return current;
		}
	}
}
