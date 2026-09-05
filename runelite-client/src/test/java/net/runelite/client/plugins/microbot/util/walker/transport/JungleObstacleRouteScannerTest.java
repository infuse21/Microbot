package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.JungleObstacle;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JungleObstacleRouteScannerTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(2800, 2940, 0);
	private static final WorldPoint DESTINATION = new WorldPoint(2800, 2938, 0);
	private static final int OBJECT_ID = 2892;

	@Test
	public void observesObjectLossUnderCommandAndClearsOnlyAtDirectedLanding()
	{
		JungleObstacleRouteScanner scanner = new JungleObstacleRouteScanner();
		MutableScene scene = new MutableScene();
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.JUNGLE_OBSTACLE),
			0, 1, ORIGIN, scene, 13);

		assertEquals(RouteInteraction.Kind.JUNGLE_OBSTACLE, pending.getKind());
		assertEquals("Chop-down", pending.getAction());
		assertTrue(pending.isReady());

		scene.visible = false;
		RouteInteraction transformed = scanner.observePending(pending, ORIGIN, scene, 13);
		RouteInteraction landed = scanner.observePending(pending, DESTINATION, scene, 13);
		assertEquals(RouteInteraction.Status.UNAVAILABLE, transformed.getStatus());
		assertFalse(transformed.isReady());
		assertEquals(RouteInteraction.Status.CLEARED, landed.getStatus());
	}

	@Test
	public void changedCatalogObjectIdentityIsUnavailable()
	{
		JungleObstacleRouteScanner scanner = new JungleObstacleRouteScanner();
		MutableScene scene = new MutableScene();
		RouteInteraction pending = scanner.scan(plan(RouteEdge.Kind.JUNGLE_OBSTACLE),
			0, 1, ORIGIN, scene, 13);
		scene.objectId = OBJECT_ID + 1;

		assertEquals(RouteInteraction.Status.UNAVAILABLE,
			scanner.observePending(pending, ORIGIN, scene, 13).getStatus());
	}

	@Test
	public void unsupportedRouteEdgeIsIgnored()
	{
		assertNull(new JungleObstacleRouteScanner().scan(plan(RouteEdge.Kind.TRANSPORT),
			0, 1, ORIGIN, new MutableScene(), 13));
	}

	private static RoutePlan plan(RouteEdge.Kind kind)
	{
		return new RoutePlan(1, 1, ORIGIN, Collections.singleton(DESTINATION),
			Arrays.asList(ORIGIN, DESTINATION), Arrays.asList(ORIGIN, DESTINATION), true,
			Collections.singletonList(new RouteEdge(0, ORIGIN, DESTINATION, kind)));
	}

	private static final class MutableScene implements JungleObstacleScene
	{
		private boolean visible = true;
		private int objectId = OBJECT_ID;

		@Override
		public JungleObstacle find(PlannedEdge edge)
		{
			return visible ? new JungleObstacle(null, ORIGIN, objectId, "Chop-down",
				ORIGIN, DESTINATION) : null;
		}
	}
}
