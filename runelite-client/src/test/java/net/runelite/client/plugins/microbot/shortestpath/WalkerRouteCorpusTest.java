package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.microbot.shortestpath.pathfinder.SplitFlagMap;
import net.runelite.client.plugins.microbot.util.walker.navigation.PathfinderRouteCalculation;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
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

    @Test
    public void faladorCastleStairChain_isOwnedByUnifiedEngine() {
        WorldPoint start = new WorldPoint(2962, 3338, 0);
        WorldPoint goal = new WorldPoint(2959, 3338, 3);
        Pathfinder pathfinder = new Pathfinder(configWith(WalkerRouteCorpusTest::unrestricted), start, goal);

        RoutePlan plan = new PathfinderRouteCalculation(pathfinder).calculate(1, 1);
        List<RouteEdge> unsupported = plan.getRouteEdges().stream()
                .filter(edge -> edge.getKind() == RouteEdge.Kind.TRANSPORT)
                .collect(Collectors.toList());

        assertTrue("Falador stair route did not arrive: " + plan.getEndpoint(), plan.isComplete());
        assertTrue("unified engine rejected stair edges: " + unsupported, plan.isEngineSupported());
    }

    // ---- baseline ----------------------------------------------------------------------------------

    @Test
    public void lumbridgeToGrandExchange_plainWalkArrives() {
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted),
                LUMBRIDGE, new WorldPoint(3164, 3485, 0));
        assertTrue("baseline overland route must arrive", arrives(path, new WorldPoint(3164, 3485, 0), 5));
    }

    // ---- Falador-area farm (the walk-1 "hairpin" report — resolved: no bug) ------------------------

    @Test
    public void farmFieldGoal_arrivesViaTheHonestTour() {
        // The reported 67-tile "out-and-back" was two parallel rows on OPPOSITE sides of the field
        // fence: the goal tile sits inside the enclosure, whose only mapped opening is the far west
        // corner, so the tour is the honest shortest path. Cold reproduction settled static-vs-live
        // in one run; live, the walk correctly ended within-distance on the road without walking the
        // loop. Pinned so a collision regression around the fence turns this into a loud failure.
        WorldPoint faladorCentre = new WorldPoint(2964, 3378, 0);
        WorldPoint fieldGoal = new WorldPoint(2930, 3449, 0);
        List<WorldPoint> path = route(configWith(WalkerRouteCorpusTest::unrestricted), faladorCentre, fieldGoal);
        assertTrue("goal inside the fenced field must remain reachable", arrives(path, fieldGoal, 5));
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
        assertTrue("the cove route must return an honest partial path", path != null && !path.isEmpty());
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
        // Same predicate as the positive tests. The old form required BOTH tiles either side of the
        // gate at radius 0, so a diagonal step across the gate satisfied neither and the assertion
        // passed while the route did cross.
        assertFalse("without a ticket or coins the route must not cross the gate",
                visits(path, GATE, 2));
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

    @Test
    public void sturdyDoorTransportRows_stayInTheCatalog() {
        // A door that MOVES you through rather than swinging open (the Al Kharid toll-gate pattern):
        // measured live via the agent server on plane 1 — standing at (3026,3511) and opening put the
        // player on (3025,3511), and the reverse held. Collision cannot reveal this one: static and
        // resolved both report every edge passable there, so without these rows the walker plans
        // straight through and then has nothing to open. Both directions are pinned because the
        // measurement covered both.
        assertTransportEdge(new WorldPoint(3026, 3511, 1), new WorldPoint(3025, 3511, 1));
        assertTransportEdge(new WorldPoint(3025, 3511, 1), new WorldPoint(3026, 3511, 1));
    }

    /**
     * The Black Knights' Fortress ladder (object 17148 at 3025,3513,p1) sits behind the Sturdy door.
     * From inside the west room the ladder's own tile is NOT walkable and cannot be reached — the
     * quester stood one tile south of it, got "can't reach", then asked for another equally walled
     * tile. The usable approach is east of the ladder, and reaching it requires the door transport.
     * <p>
     * This pins the walker half of that incident: the route through the door exists. The remaining
     * half is the quester choosing an approach tile by proximity rather than by reachability.
     */
    @Test
    public void bkfLadderApproachIsReachableOnlyThroughTheSturdyDoor() {
        WorldPoint insideWestRoom = new WorldPoint(3024, 3512, 1);
        PathfinderConfig cfg = configWith(WalkerRouteCorpusTest::unrestricted);

        WorldPoint ladderTile = new WorldPoint(3025, 3513, 1);
        assertFalse("the ladder's own tile is an object tile and must stay unreachable",
                arrives(route(cfg, insideWestRoom, ladderTile), ladderTile, 0));

        WorldPoint eastOfLadder = new WorldPoint(3026, 3513, 1);
        assertTrue("the usable approach east of the ladder must be reachable through the door",
                arrives(route(cfg, insideWestRoom, eastOfLadder), eastOfLadder, 0));

        WorldPoint doorFarSide = new WorldPoint(3026, 3511, 1);
        assertTrue("the door's far side must be reachable, i.e. the transport row is doing its job",
                arrives(route(cfg, insideWestRoom, doorFarSide), doorFarSide, 0));
    }

    private static void assertTransportEdge(WorldPoint origin, WorldPoint destination) {
        Set<Transport> at = allTransports.get(origin);
        assertTrue("transport row " + origin + " -> " + destination + " must exist",
                at != null && at.stream().anyMatch(t ->
                        destination.equals(t.getDestination())));
    }

    // ---- White Wolf Mountain tunnel (Fishing Contest reward) ---------------------------------------

    /** Burthorpe/Taverley-side entrance and the Catherby-side exit of the under-mountain tunnel. */
    private static final WorldPoint TUNNEL_EAST_SURFACE = new WorldPoint(2877, 3482, 0);
    private static final WorldPoint TUNNEL_EAST_UNDER = new WorldPoint(2876, 9878, 0);
    private static final WorldPoint TUNNEL_WEST_UNDER = new WorldPoint(2820, 9882, 0);
    private static final WorldPoint TUNNEL_WEST_SURFACE = new WorldPoint(2820, 3486, 0);

    /** Tunnel stair object ids from transports.tsv lines 1255-1260. */
    private static boolean isWhiteWolfTunnel(Transport t) {
        int id = t.getObjectId();
        return id == 54 || id == 55 || id == 56 || id == 57;
    }

    /**
     * Fishing Contest unlocks the tunnel under White Wolf Mountain. The rows, the collision map and the
     * pathfinder were all correct — measured here: the cave is walkable end to end and a surface
     * crossing does route under the mountain once the transport is allowed. What failed live was the
     * runtime quest gate, because Fishing Contest is tracked by a VARPLAYER and the player-state cache
     * only refreshed varbit-tracked quests, so completing it mid-session never opened the gate.
     * <p>
     * Pinned so a lost row or a collision regression in the cave fails here instead of quietly sending
     * every Catherby-bound walk back over the mountain.
     */
    @Test
    public void whiteWolfTunnel_isRoutableWhenTheQuestGateOpens() {
        List<WorldPoint> underground = route(configWith(WalkerRouteCorpusTest::unrestricted),
                TUNNEL_EAST_UNDER, TUNNEL_WEST_UNDER);
        assertTrue("the tunnel cave must be walkable end to end",
                arrives(underground, TUNNEL_WEST_UNDER, 3));

        List<WorldPoint> surface = route(configWith(t -> unrestricted(t) || isWhiteWolfTunnel(t)),
                TUNNEL_EAST_SURFACE, TUNNEL_WEST_SURFACE);
        assertTrue("crossing must arrive on the Catherby side", arrives(surface, TUNNEL_WEST_SURFACE, 3));
        assertTrue("with the tunnel usable the route must go UNDER the mountain",
                visits(surface, TUNNEL_EAST_UNDER, 5));
    }

    /** Without the quest-gated tunnel the crossing must NOT dive into the cave. */
    @Test
    public void whiteWolfTunnel_notUsedWithoutTheQuest() {
        List<WorldPoint> surface = route(configWith(WalkerRouteCorpusTest::unrestricted),
                TUNNEL_EAST_SURFACE, TUNNEL_WEST_SURFACE);
        assertFalse("a player without Fishing Contest must not be routed through the tunnel",
                visits(surface, TUNNEL_EAST_UNDER, 5));
    }
}
