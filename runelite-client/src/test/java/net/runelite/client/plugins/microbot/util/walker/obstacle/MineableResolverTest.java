package net.runelite.client.plugins.microbot.util.walker.obstacle;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Headless tests for mineable classification and non-blocking action dispatch.
 */
public class MineableResolverTest {

    private final MineableResolver resolver = new MineableResolver();

    private static WorldPoint wp(int x, int y) {
        return new WorldPoint(x, y, 0);
    }

    private static LiveScene sceneWith(Map<WorldPoint, TileObject> objects) {
		return sceneWith(objects, wp(0, 0));
	}

	private static LiveScene sceneWith(Map<WorldPoint, TileObject> objects, WorldPoint player) {
        return new LiveScene() {
            public WorldPoint playerLocation() {
				return player;
            }

            public boolean isReachable(WorldPoint tile) {
                return true;
            }

            public Set<Transport> transportsAt(WorldPoint tile) {
                return Collections.emptySet();
            }

            public TileObject objectAt(WorldPoint tile) {
                return objects.get(tile);
            }
        };
    }

    private static TileObject objectWithId(int id) {
        TileObject o = mock(TileObject.class);
        when(o.getId()).thenReturn(id);
        return o;
    }

    @Test
    public void handlesEdgeWithRockfallOnDestination() {
        Map<WorldPoint, TileObject> objs = new HashMap<>();
        objs.put(wp(3727, 5683), objectWithId(ObjectID.MOTHERLODE_ROCKFALL_1));
        assertTrue(resolver.handles(new PlannedEdge(wp(3728, 5683), wp(3727, 5683)), sceneWith(objs)));
    }

    @Test
    public void handlesEdgeWithRockfallOnOrigin() {
        Map<WorldPoint, TileObject> objs = new HashMap<>();
        objs.put(wp(3728, 5683), objectWithId(ObjectID.MOTHERLODE_ROCKFALL_2));
        assertTrue(resolver.handles(new PlannedEdge(wp(3728, 5683), wp(3727, 5683)), sceneWith(objs)));
    }

    @Test
    public void ignoresNonRockfallObject() {
        Map<WorldPoint, TileObject> objs = new HashMap<>();
        objs.put(wp(3727, 5683), objectWithId(12345));
        assertFalse(resolver.handles(new PlannedEdge(wp(3728, 5683), wp(3727, 5683)), sceneWith(objs)));
    }

    @Test
    public void ignoresEdgeWithNoObject() {
        assertFalse(resolver.handles(new PlannedEdge(wp(3728, 5683), wp(3727, 5683)), sceneWith(new HashMap<>())));
    }

	@Test
	public void ignoresOriginRockfallAfterEdgeWasCrossed() {
		WorldPoint from = wp(3728, 5683);
		WorldPoint to = wp(3727, 5683);
		Map<WorldPoint, TileObject> objects = new HashMap<>();
		objects.put(from, objectWithId(ObjectID.MOTHERLODE_ROCKFALL_1));

		assertFalse(resolver.handles(new PlannedEdge(from, to), sceneWith(objects, to)));
	}

	@Test
	public void resolveIssuesOneNonBlockingMineAction() {
		WorldPoint from = wp(3728, 5683);
		WorldPoint to = wp(3727, 5683);
		Map<WorldPoint, TileObject> objects = new HashMap<>();
		objects.put(to, objectWithId(ObjectID.MOTHERLODE_ROCKFALL_1));
		final WorldPoint[] interactedTile = new WorldPoint[1];
		final String[] interactedAction = new String[1];
		WalkerActions actions = new WalkerActions() {
			@Override
			public boolean interactAt(WorldPoint tile, String action) {
				interactedTile[0] = tile;
				interactedAction[0] = action;
				return true;
			}

			@Override
			public boolean walkToward(WorldPoint target) { return false; }

			@Override
			public boolean waitUntilObjectGone(WorldPoint tile) { return false; }
		};

		ObstacleResolution result = resolver.resolve(new PlannedEdge(from, to),
			sceneWith(objects), actions);

		assertEquals(ObstacleResolution.Kind.INTERACTED, result.kind());
		assertEquals(to, interactedTile[0]);
		assertEquals("mine", interactedAction[0]);
	}
}
