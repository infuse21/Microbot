package net.runelite.client.plugins.microbot.util.walker.obstacle;

import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Live-client implementation of {@link LiveScene} — the read side of the P2 obstacle plumbing
 * (docs/walker-p2-unification.md). Built once per resolution from a snapshot of the player tile and the
 * reachable-tiles map the walker already computes ({@code reachableTilesCache}); {@code transportsAt} and
 * {@code objectAt} read current game state and so must be used on the client thread during resolution.
 * Tests never use this — they supply in-memory {@link LiveScene} fakes, which is why resolver decision
 * logic stays headless-testable.
 */
public final class Rs2LiveScene implements LiveScene {

    private final WorldPoint player;
    private final Map<WorldPoint, Integer> reachable;

    public Rs2LiveScene(WorldPoint player, Map<WorldPoint, Integer> reachable) {
        this.player = player;
        this.reachable = reachable;
    }

	/** Reads instance state on the client thread without adding lambdas to the legacy walker. */
	public static boolean isInInstance() {
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
				Microbot.getClient().getTopLevelWorldView().isInstance()).orElse(false);
	}

    @Override
    public WorldPoint playerLocation() {
        return player;
    }

    @Override
    public boolean isReachable(WorldPoint tile) {
        return tile != null && reachable != null && reachable.containsKey(tile);
    }

    @Override
    public Set<Transport> transportsAt(WorldPoint tile) {
        final Map<WorldPoint, Set<Transport>> transports = Rs2PathApi.getTransports();
        if (transports == null) {
            return Collections.emptySet();
        }
        final Set<Transport> at = transports.get(tile);
        return at == null ? Collections.emptySet() : at;
    }

    @Override
    public TileObject objectAt(WorldPoint tile) {
		return exactGameObjectAt(tile);
    }

	/** Unlike {@code getGameObject(tile)}, this requires the object's anchor to be exactly on tile. */
	public static TileObject exactGameObjectAt(WorldPoint tile) {
		if (tile == null) {
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
				Rs2SceneLocation.sceneLocations(tile).stream()
						.map(Rs2GameObject::findGameObjectByLocation)
						.filter(java.util.Objects::nonNull)
						.findFirst().orElse(null))
				.orElse(null);
	}

	/** Returns an exact-anchor MLM rockfall, never a nearby object selected by radius search. */
	public static TileObject exactMineableAt(WorldPoint tile) {
		if (tile == null) {
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			for (WorldPoint sceneTile : Rs2SceneLocation.sceneLocations(tile)) {
				TileObject object = Rs2GameObject.findGameObjectByLocation(sceneTile);
				if (object == null) {
					continue;
				}
				int objectId = object.getId();
				if (objectId == ObjectID.MOTHERLODE_ROCKFALL_1
					|| objectId == ObjectID.MOTHERLODE_ROCKFALL_2) {
					return object;
				}
			}
			return null;
		}).orElse(null);
	}
}
