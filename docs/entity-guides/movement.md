# Movement Gotchas

## 1. Do not recurse on failed minimap clicks without changing the click target

`Rs2Walker.processWalk` holds the walker lock while processing a path. If a minimap click is rejected because the calculated point is outside the minimap clip, immediately recursing with the same target can spin forever while still holding the lock. Shrink the click target toward the player or otherwise change the condition before retrying.

**Why this matters:** Quest steps that walk to a nearby object can repeatedly calculate a valid path but never move, starving other walk requests because the walker lock is never released.

**Pattern to follow:**

```java
WorldPoint clickTarget = getPointWithWallDistance(targetWp);
boolean clicked = Rs2Walker.walkMiniMap(clickTarget);
if (!clicked)
{
	clicked = walkMiniMapToward(clickTarget, playerLoc, MINIMAP_REACH_EUCLIDEAN - 1);
}
```

**Where this applies:** `Rs2Walker`, `Rs2MiniMap`, and shortest-path walking loops.

**Defensive check:** When debugging stalls, compare pathfinder logs with `./microbot-cli state`. A repeating valid path with an unchanged player position usually means the click layer failed after pathing succeeded.

## 2. Probe raw path obstacles before declaring the walker stuck

Path smoothing can collapse many adjacent raw path tiles into one minimap waypoint. Some doors and gates are not represented as blocking collision in the pathfinder map, so the smoothed segment may legally cross them while hiding the exact tile the object handler needs to inspect. Run nearby raw-path door/object checks as soon as the raw path is longer than the smoothed path and the obstacle is in scene range; do not wait for `stuckCount` to increment first.

**Why this matters:** A walk from Varrock castle's upper floors toward Varrock fountain can descend correctly, then stall at the plane-1 castle door because the smoothed waypoint skips over the door tile and the normal per-segment door check never sees it.

**Pattern to follow:**

```java
if (rawPath != null && path != null && rawPath.size() > path.size()
        && handleNearbyRawPathSceneObjects(rawPath, HANDLER_RANGE)) {
    doorOrTransportResult = true;
}
```

**Where this applies:** `Rs2Walker`, `PathSmoother`, and shortest-path obstacle handling.

**Defensive check:** When a path stalls beside a visible door while the pathfinder reports a complete route, compare raw and smoothed path lengths; if the raw path is longer, verify nearby raw-path obstacle probing happens before stall recovery.

## 3. Match wall doors by crossed edge, not nearby tile

Wall-object doors block the edge between the wall object's tile and the neighboring tile indicated by its orientation. Raw-path segment probes must only treat a wall door as relevant when the path segment actually transitions across that edge. Do not match a wall door merely because the path starts on, ends on, or passes near one side of the door.

**Why this matters:** At Draynor Manor's east/back door, the player can stand on the south-side door tile and need to walk southwest into the room. A broad "door near segment" match repeatedly re-opens the back door instead of allowing the next minimap walk step to run.

**Pattern to follow:**

```java
WorldPoint doorTile = wall.getWorldLocation();
WorldPoint blockedNeighbor = getWallDoorNeighborPoint(wall.getOrientationA(), doorTile);
return isDoorEdgeTransition(previousPathTile, nextPathTile, doorTile, blockedNeighbor);
```

**Where this applies:** `Rs2Walker.handleNearbyRawPathSceneObjects`, `Rs2Walker.findDoorNearSegment`, and any wall-door probe that uses `WallObject.getOrientationA()`.

**Defensive check:** Add a unit test for a path starting on the door's blocked-neighbor tile and moving away from the door; it must return false.

## 4. Do not raw-probe doors while the player is already moving

Raw-path scene-object probing is a recovery aid for smoothed paths that hide nearby obstacles. Once a door interaction has started movement, let that movement settle or reach the door edge before probing again. Re-running raw probes while the player is still moving can repeatedly interact with the same door and prevent the normal minimap/path step from taking over.

**Why this matters:** When leaving Draynor Manor through the east/back door, the walker can click the door, start moving toward it, then immediately re-enter raw-path probing and click the same door again instead of continuing through the path outside.

**Pattern to follow:**

```java
if (Rs2Player.isMoving()) {
    return false;
}
waitForDoorInteractionProgress(fromWp, toWp);
```

**Where this applies:** `Rs2Walker.handleNearbyRawPathSceneObjects`, door handlers that call `Rs2GameObject.interact`, and any recovery logic that recurses into `processWalk`.

**Defensive check:** In live testing, a door should produce one interaction followed by movement/path progress, not repeated `Raw path door handler resolved obstacle` messages every tick while the player is moving.

## 5. Suppress the inverse adjacent transport after crossing a same-plane door

Some doors are represented in `transports.tsv` as two adjacent same-plane transports, one for each direction. After the walker clicks one side and arrives on the other, immediately accepting the inverse transport can bounce the player back through the same door instead of letting the next minimap step continue away from it. Mark both tiles of a successful adjacent same-plane transport as recently handled for a short window.

**Why this matters:** Leaving Draynor Manor through the east/back door can alternate between `3123,3360,0` and `3123,3361,0`, repeatedly logging raw-path/current-tile transport handling and burning the route timeout before walking back to Draynor.

**Pattern to follow:**

```java
boolean reachedDestination = sleepUntil(() -> atTransportDestination(transport), 5000);
if (reachedDestination && isAdjacentSamePlaneTransport(transport)) {
    markStationaryDoorOpened(transport.getOrigin());
    markStationaryDoorOpened(transport.getDestination());
}
```

**Where this applies:** `Rs2Walker.handleTransports`, current-tile transport recovery, raw-path transport probing, and bidirectional same-plane door/gate transports.

**Defensive check:** A successful adjacent same-plane transport should be followed by a minimap/path step away from the doorway, not by alternating `Raw path transport handler` and `Current-tile transport handler` logs for the same two tiles.

## 6. Recalculate after long-distance object transports

Not every large map transition changes plane or uses a teleport type. Some object transports, such as the Varrock Sewers ladder, remain on plane 0 while jumping between coordinate bands. After a successful object interaction reaches one of these destinations, run the normal transport finalizer so the shortest path is rebuilt from the new location.

**Why this matters:** A route from Varrock Sewers back to a surface origin can climb the ladder successfully, then continue using a path that was calculated from the underground coordinate band. The walker may drift off path or exit during setup even though the transport itself worked.

**Pattern to follow:**

```java
if (reachedDestination) {
    markAdjacentSamePlaneTransportHandled(transport, object);
    return finishHandledTransport(transport);
}
```

**Where this applies:** `Rs2Walker.handleTransports` object interactions and any object-transport handler that waits for the destination tile directly.

**Defensive check:** Same-plane object transports with a large `distanceTo2D` delta should produce a fresh pathfinder start near the post-transport player location before the next minimap step.

## 7. Model missing collision edges before tuning walker retries

Some static collision gaps are specific edges, not whole tiles. If the pathfinder repeatedly routes through a visible fence/wall and the live client keeps clicking fallback tiles near that boundary, add an explicit blocked edge to pathfinding and smoothing instead of trying to solve it with longer timeouts or broader minimap fallback.

**Why this matters:** The Varrock Palace garden south fence can be missing from the bundled collision map near `3229..3241,3472 -> 3471`. A no-agility F2P route to the Varrock Sewers manhole can walk around the trellis correctly, then stall against that garden boundary because the path says the south edge is traversable.

**Pattern to follow:**

```java
if (config.isBlockedTransportEdge(node.packedPosition, neighborPacked)) {
    continue;
}
```

**Where this applies:** `CollisionMap.getNeighbors`, `PathSmoother.lineOfSight`, and any path data correction where only one edge between adjacent tiles is invalid.

**Defensive check:** Add a core pathfinder regression from the observed stuck tile; assert neither the raw path nor smoothed path crosses the blocked edge, and that the route still reaches the original destination.

## 8. Do not click a visible endpoint before honoring pending route interactions

An endpoint being visible on the minimap does not mean it is the next correct click. If the computed shortest path reaches that endpoint through an intermediate door, gate, transport, shortcut, ladder, or other route object, the walker must process the first route interaction before issuing a direct endpoint click.

**Why this matters:** From Varrock Palace, a destination such as `3229,3473,0` can be visible on the minimap while the shorter route requires opening the palace doors first. Clicking the endpoint lets the game choose a longer collision-valid detour and bypasses the webwalker's route.

**Pattern to follow:**

```java
if (handleNearbyRawPathSceneObjects(rawPath, HANDLER_RANGE)) {
    return true;
}
if (!hasPendingExplicitTransportStepBeforeArrival(rawPath, target, distance)
        && !localRouteDetoursFromComputedRoute(rawPath, end, DIRECT_CLICK_MAX_DISTANCE)) {
    walkMiniMap(end);
}
```

**Where this applies:** `Rs2Walker.walkWithStateInternal`, short local walk kick-starts, final/minimap endpoint clicks, and any future fast-path that bypasses normal path iteration.

**Defensive check:** Reproduce with closed Varrock Palace doors toward `3229,3473,0`; the first action should target the door or route waypoint, not the final endpoint tile.

## 9. Preserve interrupts so walker cancellation stops waits immediately

Ctrl+X and script shutdown cancel the active walk task with `Future.cancel(true)` and clear the walker target. Shared sleep/poll helpers must preserve the interrupted flag and stop polling when interruption is observed; otherwise the walker can continue through several timeout cycles before noticing the cleared target.

**Why this matters:** A user pressing Ctrl+X expects the webwalker to stop issuing route actions immediately. If `InterruptedException` is swallowed, long waits in object, transport, dialogue, or animation handling can keep cycling until their normal timeout elapses.

**Pattern to follow:**

```java
try {
    Thread.sleep(delayMs);
} catch (InterruptedException ignored) {
    Thread.currentThread().interrupt();
}
while (!Thread.currentThread().isInterrupted() && !condition.getAsBoolean()) {
    sleep(pollMs);
}
```

**Where this applies:** `Global.sleep*`, `Global.sleepUntil*`, `Rs2Walker.setTarget(null)`, and any walker helper that waits after clicking a door, shortcut, transport, or minimap tile.

**Defensive check:** Start a long webwalk, press Ctrl+X during movement or a route-object wait, and verify no additional path recalculations or route-object interactions occur after the cancel log.

## 10. Do not treat reachable endpoint tiles as proof that a gate edge is open

Local reachability answers whether individual tiles can be reached within the sampled area; it does not prove that the computed path edge between two reachable tiles can be crossed without opening a gate, door, stile, or similar route object. Before skipping door handling, issuing a direct short minimap/checkpoint click, or yielding to an in-flight interim minimap target, scan the nearby remaining route for door-like scene objects that sit on the route segment. Include one or two raw edges before the closest path index; when the player is slightly off-path near a gate, the closest raw tile can already be on the far side of the gate edge. For diagonal hops beside small gates, also check the two cardinal sub-steps of the diagonal; the gate edge may sit on one of those sub-steps even when the direct diagonal segment does not equal the wall edge. Do not skip door probing just because the edge or object is catalogued as a transport when it is an `Open Gate` / door-like object transport. A raw-route scan may notice a future gate early, but the actual object interaction must still be range-gated against that gate edge before treating it as handled.

**Why this matters:** A short route near the Lumbridge farm allotment can correctly choose the gate as the shortest path, but the walker may see both sides as valid reachable tiles and click the minimap endpoint. The game then routes around the fence instead of opening the gate.

**Pattern to follow:**

```java
int scanStart = Math.max(0, closestRawIndex - 2);
if (bothEndpointTilesReachable
        && !hasDoorLikeSceneObjectOnSegment(from, to, playerLoc, HANDLER_RANGE)) {
    continue;
}
if (doorOpenedButPlayerDidNotTraverse) {
    // Only count this as success if the nudge actually reaches/crosses the door edge.
    tryDoorEdgeCrossNudge(from, to, currentTarget);
}
if (hasPendingDoorLikeSceneObjectBeforeDirectClick(rawPath, path, playerLoc, DIRECT_CLICK_MAX_DISTANCE)
        || handlePendingDoorBeforeRouteClick(rawPath, path, i, targetIdx, smoothedToRaw, timeoutMs,
        attemptedDoorEdgesThisPass, playerLoc)
        || handlePendingDoorNearRawPath(rawPath, timeoutMs, attemptedDoorEdgesThisPass, playerLoc, 2, 14)
        || handlePendingDoorDuringInterim(rawPath, timeoutMs, attemptedDoorEdgesThisPass, playerLoc)) {
    return WalkerState.MOVING;
}
```

**Where this applies:** `Rs2Walker.tryDirectShortWalk`, route checkpoint/minimap click selection, unreachable-smoothed-tile recovery, interim minimap movement waits, post-open door-edge nudges, `Rs2Walker.handleDoorsInRawSegment`, and any future optimization that skips route-object probing because tiles look locally reachable.

**Defensive check:** Reproduce from the Lumbridge farm road toward a target southwest of the allotment with both gates closed; the route should open each gate as it enters handler range, not stay in `interim-in-flight` until the server path has already routed around the field.

## 11. Clear sticky minimap interim targets outside the click branch

Sticky interim targets prevent click thrash while the player is moving toward a minimap checkpoint, but they are only useful while the checkpoint is still ahead. Clear them at the start of each walk pass when the player is already within the close threshold, the target is on another plane, or the checkpoint has aged out. Also clear them when stall-recalc fires, otherwise the replan can inherit the same stale checkpoint and spin without issuing a new movement command.

**Why this matters:** Long post-transport routes through cluttered areas can stop one tile from the sticky interim target. If the next pass does not enter the checkpoint-click branch, the stale interim remains in diagnostics and each stall-recalc repeats the same state until the tail iteration limit exits.

**Pattern to follow:**

```java
if (shouldClearInterimTarget(interimTargetWp, Rs2Player.getWorldLocation(), interimSetAtMs,
        interimLastProgressAtMs, nowMs)) {
    clearInterimTarget("close-or-expired");
}
if (isStuckTooLong()) {
    clearInterimTarget("stall-recalc");
    recalculatePath();
}
```

**Where this applies:** `Rs2Walker.processWalk`, recovery minimap clicks, post-transport walking, and any future logic that stores a sticky route checkpoint across loop iterations.

**Defensive check:** Reproduce a long route after the Falador crumbling-wall shortcut toward Ardougne through the dead-tree field; if the player reaches one tile from the interim checkpoint, the next pass should log `interim_clear` and select a fresh movement target instead of repeating `STALL_RECALC` until `tail_max`.

For long open routes, retarget before the player fully stops when they are already close to the interim checkpoint, and keep normal minimap clicks slightly inside the observed minimap edge. This reduces visible stop/start pauses and outside-clip fallback clicks without reintroducing rapid click thrash.

## 12. Do not let optimistic recovery override unresolved door blockers

Unreachable-tile recovery is useful for outdoor false negatives, but in tight rooms it can fight the door resolver. If a route edge still has a door-like scene object on or adjacent to the raw path, suppress broad minimap recovery and let the door scanners retry after their normal cooldowns. Do not permanently blacklist a path-adjacent fallback door just because one attempt traversed the wrong way; in small door clusters the same object may be the correct blocker again once the player has moved to the other side.

**Why this matters:** In POH-style tight rooms with several doors close together, a fallback door click can move the player away from the intended route. If that door tile is session-blacklisted and optimistic recovery keeps clicking route tiles beyond the blocker, the walker loops around the room until a user manually opens the final door.

**Pattern to follow:**

```java
if (tryResolvePathAdjacentBlocker(...)) {
    return MOVING;
}
if (hasUnresolvedDoorLikeObjectNearRawPath(...)) {
    return MOVING; // retry door handling next pass; do not broad-click recovery
}
clickOptimisticRecoveryTarget();
```

**Where this applies:** `Rs2Walker.processWalk` unreachable-tile handling, `tryResolvePathAdjacentBlocker`, and any fallback that issues minimap recovery clicks after door/path-adjacent scans fail.

**Defensive check:** Reproduce a route through a small room with three nearby doors and a POH portal. The walker should retry the route-door blocker and avoid repeated `unreachable optimistic recovery` loops around the room; it should not need the user to manually open the final door.

## 13. Stall recalculation must also issue fresh movement

Recalculating a path after a stationary stall is not enough by itself. If the player is idle and the next loop still cannot enter a normal click branch, repeated `STALL_RECALC` logs can continue forever until a user manually nudges the player. After clearing stale interim state and refreshing the route, issue a conservative minimap click along the reachable raw route so the server pathfinder gets a new movement command immediately. On active long routes, also nudge after a short stationary idle window, around a few ticks, instead of waiting for the full stall threshold.

**Why this matters:** Long routes can stop on a tile with no combat, animation, or interaction. Repeated stall recalcs refresh pathfinding state but leave the character standing still, so the route only resumes after manual movement changes the local path context.

**Pattern to follow:**

```java
if (isRouteActive() && playerIsIdleForShortWindow()) {
    tryIssueRouteRecoveryClick(rawPath, path, target);
    continue;
}
if (isStuckTooLong()) {
    clearInterimTarget("stall-recalc");
    setTarget(target);
    if (playerIsIdle()) {
        tryIssueRouteRecoveryClick(rawPath, path, target);
    }
    continue;
}
```

**Where this applies:** `Rs2Walker.processWalk` stall-recalc handling and any future stale-state recovery that clears route state while the player is idle.

**Defensive check:** Start a long route and observe a stationary pause. A short idle pause should log `active route idle nudge`; if it reaches full stall recalc, the next log sequence should include `stall recovery click` and a position delta, not another idle-only `STALL_RECALC` loop at the same tile.

After a handled transport, avoid expensive path-adjacent or raw transport scans on ordinary open-ground segments unless a nearby planned transport or recent door attempt exists. Those scans are recovery tools, and on long outdoor routes a no-op scan can add several seconds before the next minimap click.

For long-route minimap walking, let the next checkpoint selection happen before the current minimap target is fully consumed. Waiting until the player is only a few tiles from the interim makes the walker visibly stop before issuing the next click; handing off at a moderate remaining distance keeps movement continuous without rapid re-clicking. If an interim clears as close and no nearby route door/transport is pending, issue the next route-aligned continuation click immediately instead of waiting for idle-nudge recovery.

Continuation clicks that keep an active route moving should be tail-exempt like `interim-in-flight`; otherwise very long routes can exhaust `MAX_PROCESS_WALK_TAIL_ITERATIONS` while still making progress and trigger an unnecessary auto-retry.

Sticky interim targets should also clear when route-index progress goes stale. If the player keeps moving but the closest path index does not advance for `INTERIM_PROGRESS_TIMEOUT_MS`, treat the checkpoint as stale and select a fresh route-aligned target instead of waiting for max-age expiry.

When a route-following minimap click is outside the minimap clip, fallback clicks must stay on the raw path. A generic "reachable tile closer to target" fallback can select a tile far away from the route in open areas, especially near the final destination.

For adjacent same-plane shortcuts, do not treat any movement away from the origin as success. Some shortcuts, such as stepping stones, can fail and place the player on a fallback tile; once the player is settled away from the expected destination, stop the landing wait and replan from the actual tile.

## 14. A scene door on a refused route edge owns recovery

When route-click validation refuses an edge because a door or gate is on or immediately adjacent
to it, remember that edge as a short-lived door claim. Being beside the claim means “dispatch the
door handler now”; it does not mean the approach helper failed and generic recovery may replan or
click another waypoint. While the claim is fresh, suppress competing wall nudges, local-recovery
clicks, and live-collision recalculation for the door edge plus its first landing step.

**Why this matters:** At `(1770,3589)->(1770,3590)` the walker correctly logged that the door
pipeline owned the edge, then treated an adjacent player as a false return from a boolean approach
helper. It repeatedly replanned and clicked waypoints on both sides of the door instead of opening
it. A second incident recalculated the route on the diagonal landing step immediately after opening
the door at `(2907,3544)->(2907,3543)`.

**Pattern to follow:** Classify remembered claims as expired, crossed, action-in-flight,
handle-at-edge, approach, or invalid. Only expired/invalid claims release ownership to generic
recovery. Bound the claim by time and exact route geometry so unrelated nearby obstacles remain
visible.

**Defensive check:** Test exact/reverse door edges, the first cardinal and diagonal landing steps,
an unrelated nearby edge, another plane, and an expired claim. An adjacent fresh claim must select
the door handler—not replan.

## 15. Door ownership begins at detection and lasts through crossing

A route door must claim its exact attempted edge as soon as the handler detects it, including when
interaction is deferred because the player is still moving. Until the edge is crossed, expires, or
strikes out, suppress idle nudges, stall replans, generic recovery clicks, and live-collision
revalidation. Keep a refused/adjacent route edge as the affected envelope, but prefer the handler's
exact edge once it is known.

**Why this matters:** At Port Piscarilius, a failed door traversal left the player beside
`1761,3631 -> 1761,3630`; early idle recovery repeatedly clicked the near-side tile for 13 seconds
before stall recalculation retried the door. At a nearby double gate, the refused horizontal edge
was adjacent to the actual north/south locked-door edge, so one edge alone could not describe both
the route impact and the interaction.

**Where this applies:** Door detection/attempt ledgers, active-route idle nudges, stall recovery,
live-collision route validation, and walled-route recovery.

**Defensive check:** After a `Found ... door` or `door_interact_deferred` log, no idle nudge, generic
recovery click, or stall replan may occur before a door retry, crossing, strike-out, or expiry.

## 16. Spend an opened door claim on traversal instead of passive yields

When an exact scene-door claim is at the edge and live collision reports that exact edge passable,
let the door transaction issue its controlled route-aware cross nudge. Check both before retrying
the door interaction and immediately after an interaction returns without observing traversal.
Do not grant this behavior to a merely refused/walled route envelope, and do not click while
movement or animation is already in flight.

**Why this matters:** At the gates around `1761,3636` and `2936,3451`, the door opened but the
scene-object snapshot still exposed an `Open` action. Exclusive ownership prevented competing
recovery correctly, but then emitted `door-traversal-pending-yield` for 6-10 seconds until the
claim expired. The ordinary route click crossed immediately afterward, proving the delay belonged
to ownership handoff rather than pathfinding.

**Where this applies:** Active door-claim recovery, post-interaction traversal, live collision edge
checks, and route-aware door-edge nudges.

**Defensive check:** Once `Rs2Tile.isEdgePassable(from,to)` becomes true for an exact idle claim,
the next owned action should be a door-edge traversal click; repeated passive pending-yields must
not continue until claim expiry.

## 17. Approach object-occupied transport origins instead of replanning them as walls

When local recovery reaches the first planned transport edge, target the transport origin if that
tile is reachable. If collision correctly excludes the origin because the scene object occupies it,
target the last reachable raw-route tile before the origin. Stop at that first transport edge; never
scan through it for a later shortcut.

**Why this matters:** On the DIS fairy-ring route, the player stopped at `(1364,2936)` while the
planned origin `(1359,2941)` was absent from the local BFS. Generic wall recovery launched two
six-second path recalculations from the same tile. A manual step to `(1359,2940)` immediately let
the normal fairy-ring handler dispatch, proving the origin was an interaction target rather than a
walkable recovery tile.

**Where this applies:** Local unreachable-frontier recovery for fairy rings and other object-,
dialog-, or widget-driven transports whose catalog origin cannot itself be stood on.

**Defensive check:** Model a planned transport whose origin is absent from the reachable set but
whose preceding raw-route tile is reachable. Recovery must choose that preceding tile and preserve
the planned transport; it must not return `REPLAN_WALLED` or select a tile beyond the transport.

## 18. Catalog object transports own their scene objects, not just their edges

When a route interaction has a catalog `OBJECT` executor, exclude its scene object from every
generic door scan. Exact-edge suppression is insufficient: door geometry can associate the same
physical object with a neighbouring diagonal or cardinal route segment after the intended
transport crossing. The selected transport owns the click, server approach, and landing; generic
door detection is reserved for doors that have no object-executor route row.

**Why this matters:** In the Draynor basement, the catalog transport correctly crossed puzzle door
137 from `(3106,9765)` to `(3104,9765)`. The generic recovery scan then matched that same object to
`(3104,9765)->(3103,9764)`, opened it, and moved the player back to `(3106,9765)`. Recovery dispatched
the intended transport again, producing an endless two-tile ping-pong.

**Where this applies:** Segment-door candidates, raw-scene scans, pending/unresolved-door checks,
local recovery, route-ahead door searches, and ordinary catalogued `Open` gates.

**Defensive check:** Both a puzzle-door row and an ordinary catalogued `Open` gate with an `OBJECT`
executor must remain transport-owned. An uncatalogued scene door must remain eligible for the
generic door cascade.

## 19. Ranged object dispatch must not require a reachable catalog origin

Once route ordering has proved an object transport is the next unresolved interaction, click the
object and let the server choose its approach tile. Do not require the transport origin to appear
in local reachability first; closed doors often make that synthetic origin unreachable until the
interaction opens the edge, recreating the one-tile pre-approach that ranged dispatch exists to
avoid. Retain the reachability requirement for legacy near-origin dispatch and as the fallback after
a ranged click makes no progress.

**Why this matters:** Stairs usually expose a reachable origin, while closed doors often do not, so
the same ranged-dispatch setting appeared to work only for stairs and behaved inconsistently for
doors.

**Where this applies:** Route-ordered object transports, raw-path ranged scans, and transport object
execution. It does not authorize firing a later interaction past an earlier unresolved route edge.

**Defensive check:** The ranged executor must accept an unreachable origin after the route-order
gate; its non-ranged overload must continue requiring a reachable origin.

## 20. A ranged door click still requires a reachable near side

Before clicking a generic door from range, require the raw-route tile immediately before that door
to be present in the current local reachable set. A broad route-ahead scan must stop at the first
unreachable near side instead of continuing through it to another visible door. This preserves
server-side approach without allowing the scan to skip an earlier unresolved blocker.

**Why this matters:** In Falador Castle, after descending to `(2995,3341,1)`, recovery scanned ahead
from `(2991,3341)` and clicked the later door at `(2990,3337)` from five tiles away. The route to
that door was still blocked by the first door, so the server could not complete the intended
approach and the walker entered door suppression and wall-replan recovery.

**Where this applies:** Pending-door lookahead, exact raw-segment door handling, local unreachable-
frontier recovery, and every generic ranged door path. Catalog object transports retain their own
route-order gate.

**Defensive check:** With two route-ordered doors, mark only the first door's near-side tile locally
reachable. The first door may dispatch from range; the second must not be considered until its own
near side becomes reachable after the first crossing.

## 21. An actioned double-gate wing owns the first blocked edge

Derive the first raw-route edge that leaves the player-origin reachable set before running any
route-ahead door scan. If an actionable door or gate wing is adjacent to either endpoint, give that
object exclusive ownership even when its wall orientation does not geometrically intersect the raw
edge. Click it from range and end the pass; do not let an exact-segment door farther ahead act.

**Why this matters:** In Falador Castle the first blocker was the double-gate edge
`(2991,3341)->(2991,3340)`, but the Open action lived on the neighbouring wing. Exact segment
matching rejected that wing with `orient-mismatch`, then lookahead clicked the later door at
`(2990,3337)` from five tiles away. A per-door reachable-near-side check could not fix this because
the first blocker was never classified as a door edge in the first place.

**Where this applies:** Local unreachable-frontier recovery, double doors/gates with actionless
slave wings, broad pending-door lookahead, and walled-route ownership.

**Defensive check:** With an actionable wing adjacent to the first reachable-to-unreachable raw
edge and a geometrically exact door later on the route, only the adjacent wing may be claimed or
clicked during that pass.

## 22. Stage puzzle-dungeon entry before solving sealed interior rooms

When a destination is inside a stateful dungeon puzzle and the player is outside, first walk to the
surface side of the dungeon's real entrance and execute that transport into a known-safe room. Only
then solve the state needed to reach the requested room. Do not pathfind directly toward a currently
sealed interior room and accept the closest partial component. If every interior target invokes the
puzzle hook, using an interior staging target can recursively trigger its own entry guard.

**Why this matters:** Draynor Manor's oil room is numerically close to the Dwarven Mine in the
shared underground coordinate band. With the lever door sealed, a direct search selected the
Dwarven Mine/MLM cave as its closest partial endpoint, producing a long walk followed by a
recalculation on nearly every step.

**Pattern to follow:**

```java
if (isPuzzleTarget(target) && !isInsidePuzzle(player)) {
    walkTo(SURFACE_ENTRANCE, 1);
    useEntranceTransport();
    awaitInside(SAFE_ENTRANCE_ROOM);
}
solvePuzzleToward(target);
```

**Where this applies:** `DraynorBasementSolver`, dungeon-specific solvers, and walker handling of
`SEARCH_EXHAUSTED` partial routes across disconnected underground components.

**Defensive check:** From a surface tile, a walk to Draynor's oil room must use the Manor ladder at
`(3092,3361)` and must never use the Dwarven Mine stairs at `(3061,3376)`.
