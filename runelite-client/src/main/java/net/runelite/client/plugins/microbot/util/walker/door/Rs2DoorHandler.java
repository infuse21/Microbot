package net.runelite.client.plugins.microbot.util.walker.door;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.walker.door.model.DoorEdge;

import java.util.Map;

public final class Rs2DoorHandler {
    private Rs2DoorHandler() {
    }

    public static String doorAttemptKey(WorldPoint doorTile, WorldPoint fromWp, WorldPoint toWp) {
        if (fromWp != null && toWp != null) {
            return new DoorEdge(fromWp, toWp).normalizedKey();
        }
        return compactWorldPoint(doorTile) + "|" + compactWorldPoint(fromWp) + "->" + compactWorldPoint(toWp);
    }

    public static boolean shouldThrottleDoorAttempt(Map<String, Long> recentDoorAttemptByEdge,
                                                    long cooldownMs,
                                                    WorldPoint doorTile,
                                                    WorldPoint fromWp,
                                                    WorldPoint toWp) {
        String key = doorAttemptKey(doorTile, fromWp, toWp);
        long now = System.currentTimeMillis();
        recentDoorAttemptByEdge.entrySet().removeIf(entry -> now - entry.getValue() > cooldownMs);
        Long last = recentDoorAttemptByEdge.get(key);
        return last != null && now - last < cooldownMs;
    }

    public static void markDoorAttempt(Map<String, Long> recentDoorAttemptByEdge,
                                       WorldPoint doorTile,
                                       WorldPoint fromWp,
                                       WorldPoint toWp) {
        recentDoorAttemptByEdge.put(doorAttemptKey(doorTile, fromWp, toWp), System.currentTimeMillis());
    }

    public static void markStationaryDoorOpened(Map<WorldPoint, Long> recentlyOpenedStationaryDoors, WorldPoint doorTile) {
        if (doorTile != null) {
            recentlyOpenedStationaryDoors.put(doorTile, System.currentTimeMillis());
        }
    }

    public static boolean recentlyOpenedStationaryDoorOnSegment(Map<WorldPoint, Long> recentlyOpenedStationaryDoors,
                                                                long suppressMs,
                                                                WorldPoint fromWp,
                                                                WorldPoint toWp) {
        if (fromWp == null || toWp == null) {
            return false;
        }
        final int segmentDoorSuppressDist = 2;
        long now = System.currentTimeMillis();
        recentlyOpenedStationaryDoors.entrySet().removeIf(entry -> now - entry.getValue() > suppressMs);
        return recentlyOpenedStationaryDoors.keySet().stream()
                .anyMatch(door -> door != null
                        && door.getPlane() == fromWp.getPlane()
                        && (door.distanceTo2D(fromWp) <= segmentDoorSuppressDist || door.distanceTo2D(toWp) <= segmentDoorSuppressDist));
    }

    public static boolean shouldThrottleGlobalDoorInteraction(long nextDoorInteractionAllowedAtMs) {
        return System.currentTimeMillis() < nextDoorInteractionAllowedAtMs;
    }

    /**
     * Edge-scoped variant. The full window is anti-hammer for ONE door — re-clicking the same edge
     * before the world has caught up. A DIFFERENT door immediately after a successful open is not
     * hammering, it is chaining, and holding it for the full window serialised every pair of nearby
     * doors. A different edge owes only the cross-edge floor (one game tick): enough that two clicks
     * cannot land inside the same tick, no more.
     *
     * @param fullCooldownMs      the window {@code nextAllowedAtMs} was stamped with
     * @param crossEdgeCooldownMs the floor a different edge still owes
     */
    public static boolean shouldThrottleGlobalDoorInteraction(long nowMs, long nextAllowedAtMs,
                                                              boolean sameEdgeAsLastAttempt,
                                                              long fullCooldownMs, long crossEdgeCooldownMs) {
        if (sameEdgeAsLastAttempt) {
            return nowMs < nextAllowedAtMs;
        }
        return nowMs < nextAllowedAtMs - (fullCooldownMs - crossEdgeCooldownMs);
    }

    public static long markGlobalDoorInteractionCooldown(long cooldownMs) {
        return System.currentTimeMillis() + cooldownMs;
    }

    private static String compactWorldPoint(WorldPoint wp) {
        if (wp == null) {
            return "?";
        }
        return wp.getX() + "," + wp.getY() + ",p" + wp.getPlane();
    }
}
