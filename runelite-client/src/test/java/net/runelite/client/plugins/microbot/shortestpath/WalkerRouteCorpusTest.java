package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Route-regression corpus: real end-to-end routes computed with the real pathfinder, collision map and
 * transport catalog, asserting the PROPERTIES that past live incidents depended on (uses the ferry, crosses
 * the gate, goes through the door, honest partial when unreachable) rather than exact tile sequences.
 * <p>
 * Every journey here was once a live failure or a live fix: pin it, and a data regression (a filtered
 * ferry, a lost transport row, a collision-map hole) fails the build instead of stranding a walk mid-quest.
 * Scenario "player state" (holding a Shantay ticket, holding coins) is expressed by filtering which
 * transports are usable, mirroring what PathfinderConfig's live refresh does with real inventory/quest
 * state — the pathfinder itself is exercised unmodified.
 */
public class WalkerRouteCorpusTest {

    private static SplitFlagMap collisionMap;
    private static HashMap<WorldPoint, Set<Transport>> allTransports;

    @BeforeClass
    public static void load() {
        collisionMap = SplitFlagMap.fromResources();
        allTransports = Transport.loadAllFromResources();
    }

    // ---- harness -----------------------------------------------------------------------------------

    /** A transport with no requirements at all — usable by any account in any state. */
    private static boolean unrestricted(Transport t) {
        return (t.getQuests() == null || t.getQuests().isEmpty())
                && (t.getItemIdRequirements() == null || t.getItemIdRequirements().isEmpty())
                && (t.getVarbits() == null || t.getVarbits().isEmpty())
                && (t.getVarplayers() == null || t.getVarplayers().isEmpty())
                && t.getCurrencyAmount() <= 0
                && Arrays.stream(t.getSkillLevels()).allMatch(l -> l <= 1);
    }

    /**
     * Config whose usable-transport set is {@code allTransports} filtered by {@code allow}, injected via
     * the same reflection seam the rest of this suite already uses for {@code calculationCutoffMillis} —
     * the live population path (refreshTransports) needs a client.
     */
    @SuppressWarnings("unchecked")
    private static PathfinderConfig configWith(Predicate<Transport> allow) {
        PathfinderConfig config = new PathfinderConfig(
                collisionMap, new HashMap<>(), Collections.emptyList(), null, null);
        try {
            Field cutoff = PathfinderConfig.class.getDeclaredField("calculationCutoffMillis");
            cutoff.setAccessible(true);
            cutoff.setLong(config, 10000);

            Field transportsField = PathfinderConfig.class.getDeclaredField("transports");
            transportsField.setAccessible(true);
            Map<WorldPoint, Set<Transport>> transports =
                    (Map<WorldPoint, Set<Transport>>) transportsField.get(config);

            Field packedField = PathfinderConfig.class.getDeclaredField("transportsPacked");
            packedField.setAccessible(true);
            PrimitiveIntHashMap<Set<Transport>> packed =
                    (PrimitiveIntHashMap<Set<Transport>>) packedField.get(config);

            for (Map.Entry<WorldPoint, Set<Transport>> e : allTransports.entrySet()) {
                if (e.getKey() == null) {
                    continue; // teleports (null origin) live in usableTeleports; not corpus scope
                }
                Set<Transport> allowed = e.getValue().stream().filter(allow).collect(Collectors.toSet());
                if (allowed.isEmpty()) {
                    continue;
                }
                transports.put(e.getKey(), allowed);
                packed.put(WorldPointUtil.packWorldPoint(e.getKey()), allowed);
            }
        } catch (Exception ex) {
            throw new RuntimeException("corpus transport injection failed", ex);
        }
        return config;
    }

    private static List<WorldPoint> route(PathfinderConfig config, WorldPoint from, WorldPoint to) {
        Pathfinder pf = new Pathfinder(config, from, to);
        pf.run();
        assertTrue("pathfinder did not complete for " + from + " -> " + to, pf.isDone());
        return pf.getPath();
    }

    private static boolean arrives(List<WorldPoint> path, WorldPoint goal, int tolerance) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        WorldPoint end = path.get(path.size() - 1);
        return end.getPlane() == goal.getPlane() && end.distanceTo2D(goal) <= tolerance;
    }

    private static boolean visits(List<WorldPoint> path, WorldPoint tile, int radius) {
        return path != null && path.stream().anyMatch(p ->
                p.getPlane() == tile.getPlane() && p.distanceTo2D(tile) <= radius);
    }

    private static final WorldPoint LUMBRIDGE = new WorldPoint(3222, 3218, 0);

    // ---- baseline ----------------------------------------------------------------------------------

    @Test
    public void lumbridgeToGrandExchange_plainWalkArrives() {
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted),
                LUMBRIDGE, new WorldPoint(3164, 3485, 0));
        assertTrue("baseline overland route must arrive", arrives(path, new WorldPoint(3164, 3485, 0), 5));
    }

    // ---- Tempoross / Ruins of Unkah (the route-flapping incident) ----------------------------------

    @Test
    public void ruinsOfUnkah_reachedViaTheFerry() {
        // The Unkah peninsula is ferry-only (probe-verified: no walkable approach exists). If the ferry
        // rows in boats.tsv are ever lost or gain a bogus requirement, this fails instead of the walker
        // bouncing between partial routes at the coast.
        WorldPoint unkahBank = new WorldPoint(3156, 2835, 0);
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted), LUMBRIDGE, unkahBank);
        assertTrue("route to Ruins of Unkah must arrive", arrives(path, unkahBank, 5));
        assertTrue("route to Ruins of Unkah must use the ferry landing",
                visits(path, new WorldPoint(3148, 2843, 0), 3));
    }

    @Test
    public void temporossCove_staysAnHonestPartial() {
        // The arena template west of Unkah is unreachable from everywhere. The planner must say so via a
        // partial (endpoint far from the goal), never fabricate an arrival.
        WorldPoint cove = new WorldPoint(3044, 2870, 0);
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted), LUMBRIDGE, cove);
        assertFalse("the cove must not be reachable", arrives(path, cove, 40));
    }

    // ---- Shantay Pass (the buy-at-gate fix) --------------------------------------------------------

    private static final WorldPoint NORTH_OF_GATE = new WorldPoint(3304, 3125, 0);
    private static final WorldPoint SOUTH_OF_GATE = new WorldPoint(3304, 3105, 0);
    private static final WorldPoint GATE = new WorldPoint(3304, 3116, 0);

    @Test
    public void shantaySouthbound_withTicket_crossesTheGate() {
        List<WorldPoint> path = route(configWith(t -> unrestricted(t)
                        || (t.getObjectId() == 4031 && t.getItemIdRequirements() != null
                        && !t.getItemIdRequirements().isEmpty())),
                NORTH_OF_GATE, SOUTH_OF_GATE);
        assertTrue("ticket holder must arrive south of the gate", arrives(path, SOUTH_OF_GATE, 3));
        assertTrue("ticket holder must cross AT the gate", visits(path, GATE, 2));
    }

    @Test
    public void shantaySouthbound_withCoinsOnly_crossesTheGate() {
        // Pins the buy-at-gate fix: 5 coins (no ticket) must keep the gate plannable, or the planner
        // detours several hundred tiles ("it starts then retargets").
        List<WorldPoint> path = route(configWith(t -> unrestricted(t)
                        || (t.getObjectId() == 4031 && t.getCurrencyAmount() == 5)),
                NORTH_OF_GATE, SOUTH_OF_GATE);
        assertTrue("coin holder must arrive south of the gate", arrives(path, SOUTH_OF_GATE, 3));
        assertTrue("coin holder must cross AT the gate", visits(path, GATE, 2));
    }

    @Test
    public void shantaySouthbound_withNothing_neverCrossesTheGate() {
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted),
                NORTH_OF_GATE, SOUTH_OF_GATE);
        boolean crossedGate = visits(path, new WorldPoint(3304, 3117, 0), 0)
                && visits(path, new WorldPoint(3304, 3115, 0), 0);
        assertFalse("without a ticket or coins the route must not cross the gate", crossedGate);
    }

    // ---- Port Sarim, Wydin's shop (the door-poisoning incident) ------------------------------------

    @Test
    public void wydinsShop_isEnterableWithoutCircling() {
        // The door-poisoning incident: a mis-learned blocked edge turned entering Wydin's shop into an
        // endless circling loop. Pin that the shop interior stays reachable and short from the west side.
        // (The planner may pick either entrance; WHICH door is a cost choice, not a correctness property.)
        WorldPoint outside = new WorldPoint(3008, 3207, 0);
        WorldPoint inside = new WorldPoint(3014, 3206, 0);
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted), outside, inside);
        assertTrue("must arrive inside the shop", arrives(path, inside, 2));
        assertTrue("route must be the short way in, not a loop (" + path.size() + " tiles)",
                path.size() < 40);
    }

    @Test
    public void wydinsShopDoorTransportRow_staysInTheCatalog() {
        // The west door edge (3012,3204 <-> 3011,3204) exists as a transport row because the collision
        // map holds that wall closed; the row is what makes the west-side approach plannable at all.
        Set<Transport> atDoor = allTransports.get(new WorldPoint(3012, 3204, 0));
        assertTrue("the Wydin shop door transport row must exist",
                atDoor != null && atDoor.stream().anyMatch(t ->
                        t.getDestination() != null
                                && t.getDestination().equals(new WorldPoint(3011, 3204, 0))));
    }
}
