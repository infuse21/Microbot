package net.runelite.client.plugins.microbot.util.walker.obstacle;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/**
 * Resolves a mineable obstacle (a Motherlode Mine rockfall) sitting on a planned edge (P2;
 * docs/walker-p2-unification.md).
 * <p>
 * Classification is pure over the injected {@link LiveScene}; execution is a single non-blocking call
 * through {@link WalkerActions}. Crossing/object-clear verification belongs to the navigation session.
 */
public final class MineableResolver implements ObstacleResolver {

    @Override
    public boolean handles(PlannedEdge edge, LiveScene scene) {
        if (edge == null || scene == null) {
            return false;
        }
        return blockerTile(edge, scene) != null;
    }

    @Override
    public ObstacleResolution resolve(PlannedEdge edge, LiveScene scene, WalkerActions actions) {
        WorldPoint blocker = blockerTile(edge, scene);
        if (blocker == null || actions == null) {
            return ObstacleResolution.notApplicable();
        }
        return actions.interactAt(blocker, "mine")
                ? ObstacleResolution.interacted()
                : ObstacleResolution.waiting();
    }

    /** Returns the blocking tile, suppressing an origin object already crossed by the player. */
    public WorldPoint blockerTile(PlannedEdge edge, LiveScene scene) {
        if (edge == null || scene == null) {
            return null;
        }
        if (isRockfall(scene.objectAt(edge.to()))) {
            return edge.to();
        }
        WorldPoint player = scene.playerLocation();
        if (isRockfall(scene.objectAt(edge.from())) && (player == null || !player.equals(edge.to()))) {
            return edge.from();
        }
        return null;
    }

    private static boolean isRockfall(TileObject object) {
        return object != null
                && (object.getId() == ObjectID.MOTHERLODE_ROCKFALL_1
                || object.getId() == ObjectID.MOTHERLODE_ROCKFALL_2);
    }
}
