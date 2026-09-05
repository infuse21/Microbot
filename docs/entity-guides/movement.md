# Movement Gotchas

> **How to read these.** Each entry records a failure mode observed while working on the walker.
> Two caveats for reviewers:
>
> - **"Pattern to follow" blocks are illustrative.** They may use shortened or pseudo-code names
>   (`atTransportDestination`, `isRouteActive`, `clickOptimisticRecoveryTarget`, …) that do not exist
>   verbatim in the source. The **"Where this applies"** line is the authoritative list of real symbols.
> - **Some entries record approaches that were tried and reverted** (#19, #20). They are kept
>   deliberately so the reverted idea is not reintroduced from an older note. Where an entry cites a
>   measured log line, that measurement is the evidence; where it does not, treat it as a hypothesis
>   that happened to hold at the time of writing.

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

`Rs2GameObject.getGameObject(WorldPoint)` treats the point as a search anchor and returns a nearby
game object; it is not an exact-tile lookup. Route-edge classification and interaction verification
must additionally require the object's world location (or footprint, for genuinely multi-tile
objects) to occupy the planned edge tile. Otherwise the walker opens or mines unrelated objects one
tile beside the route.

Once an exact route-blocking object is loaded and within a conservative handler range, its object
interaction may supersede an active ground-movement command. The server can complete the approach;
forcing a separate walk to the object's edge adds redundant clicks and prevents natural ranged
interaction. Keep the range bounded and retain post-interaction clearance/crossing verification.

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

If a previous minimap click is still moving the player, do not attribute that movement to a new gate interaction. A valid gate can be blacklisted when the walker clicks an interim waypoint, notices the gate while movement is still in flight, then compares the pre/post positions from the old minimap movement against the gate edge. Wrong-traversal blacklisting should only run when the player started near the attempted door edge.

## 5. Suppress the inverse adjacent transport after crossing a same-plane door

Some doors are represented in `transports.tsv` as two adjacent same-plane transports, one for each direction. After the walker clicks one side and arrives on the other, immediately accepting the inverse transport can bounce the player back through the same door instead of letting the next minimap step continue away from it. Mark both tiles of a successful adjacent same-plane transport as recently handled for a short window. **This is necessary but not sufficient — see #22:** suppression must also run when the landing check *fails*, because an adjacent transport requires landing on the exact destination tile and a crossing can succeed while that check reports failure.

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

## 12. Do not let local recovery override unresolved door blockers

Local-reachability recovery is useful for outdoor false negatives, but in tight rooms it can fight the door resolver. If a route edge still has a door-like scene object on or adjacent to the raw path, suppress broad minimap recovery and let the door scanners retry after their normal cooldowns. Do not permanently blacklist a path-adjacent fallback door just because one attempt traversed the wrong way; in small door clusters the same object may be the correct blocker again once the player has moved to the other side.

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

**Defensive check:** Reproduce a route through a small room with three nearby doors and a POH portal. The walker should retry the route-door blocker and avoid repeated `route-backed local recovery click` loops around the room; it should not need the user to manually open the final door.

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

For long-route minimap walking, let the next checkpoint selection happen before the current minimap target is fully consumed. Waiting until the player is only a few tiles from the interim makes the walker visibly stop before issuing the next click; handing off at a moderate remaining distance keeps movement continuous without rapid re-clicking. If an interim clears as close and no nearby route door/transport is pending, issue the next route-aligned continuation click immediately instead of waiting for idle-nudge recovery. Stale-progress checks must also count distance progress toward the actual interim checkpoint, not only smoothed path index progress; sparse smoothed paths can otherwise clear valid raw-path checkpoints as stale while the player is still walking toward them.

Before the first movement click, avoid timeout-backed speculative segment scans for doors, blockers, rockfalls, or transports. Let the first minimap click move the player toward the route; actual visible doors can still be handled by the dedicated pre-click route-door guard, and steady-state handling can resolve later obstacles once movement has started. Spending startup time scanning every nearby raw segment creates a visible cold start.

Continuation clicks that keep an active route moving should be tail-exempt like `interim-in-flight`; otherwise very long routes can exhaust `MAX_PROCESS_WALK_TAIL_ITERATIONS` while still making progress and trigger an unnecessary auto-retry.

Sticky interim targets should also clear when route-index progress goes stale. If the player keeps moving but the closest path index does not advance for `INTERIM_PROGRESS_TIMEOUT_MS`, treat the checkpoint as stale and select a fresh route-aligned target instead of waiting for max-age expiry.

When a route-following minimap click is outside the minimap clip, fallback clicks must stay on the raw path. This includes the "clicked but no movement yet" retry after a route click and route-backed direct short-walks near the final destination. A generic "reachable tile closer to target" fallback can select a tile far away from the route in open areas, especially near fences and the final destination. Raw fallback must be anchored from the stabilized route index, not a fresh nearest-raw-tile lookup, or the walker can snap backward to an already-travelled branch.

For adjacent same-plane shortcuts, do not treat any movement away from the origin as success. Some shortcuts, such as stepping stones, can fail and place the player on a fallback tile; once the player is settled away from the expected destination, stop the landing wait and replan from the actual tile. If the player is settled within one tile of the expected destination and is no longer on the origin, accept the landing instead of waiting for an exact tile that server pathing may not choose.

## 14. Keep local recovery clicks route-backed and paced

Local-reachability recovery is a fallback for bounded reachability false negatives, not a license to pick an arbitrary minimap point. Recovery clicks should use the same route-backed raw-path fallback as normal route movement, then store the actual clicked target as the sticky interim checkpoint. After issuing the click, wait only for movement to start or the checkpoint/goal to be reached; do not wait for a full idle cycle before returning to the walk loop.

**Why this matters:** Bounded local reachability can report a valid route tile as unreachable even though the server pathfinder can still walk toward it. Arbitrary or repeated recovery clicks can pull the player away from the planned route and create visible stop-start movement.

**Pattern to follow:**

```java
int reach = STALL_RECOVERY_MINIMAP_REACH_EUCLIDEAN;
WorldPoint clickTarget = clampToEuclideanRadius(playerLoc, recoverTarget, reach - 1);
WorldPoint clickedTarget = clickMiniMapOrFallback(rawPath, clickTarget, playerLoc, reach - 1, false, rawAnchorIndex);
if (clickedTarget != null) {
    interimTargetWp = clickedTarget;
    waitForMovementStartAfterRecovery(target, playerLoc, clickedTarget, target, finishThreshold);
}
```

**Where this applies:** `Rs2Walker.processWalk` local-reachability recovery, route-backed direct short-walks, and any fallback that runs after local reachability says a route tile is not currently reachable.

**Defensive check:** Exercise a long outdoor route and a route with gates or transports. Recovery logs should select route-backed points, and there should be no long idle pause between `route-backed local recovery click` and the next route-aligned movement.

After recovery sets a sticky interim target, do not issue another optimistic recovery while that interim is still active and movement/progress is fresh. Yield as `interim-in-flight` or another tail-exempt movement state until the checkpoint is close, stale, or expired. Otherwise the walker can loop through several recovery clicks while the player is already moving, especially when local reachability reports the next smoothed tile as unreachable.

## 15. Keep primary route clicks on the raw route

When a shortest-path raw route exists, choose the next minimap target from that raw route before applying smoothed-waypoint wall-distance nudges, bounded reachability shortcuts, or generic directional fallbacks. Sideways reachable tiles can be valid server walk targets while still being the wrong side of a fence, gate, or corridor corner. Generic fallback is only appropriate when no route-backed raw point is available.

**Why this matters:** Tight routes near gates and fences can fail when the walker clicks a tile left or right of the drawn route line. The game then chooses its own path around the obstacle, which fights the planned door/transport handlers and produces stop-start recovery.

**Pattern to follow:**

```java
WorldPoint rawTarget = findFurthestRawPathPointMatching(rawPath, playerLoc, reach, rawAnchor,
        Rs2Walker::isKnownWalkableOrUnloaded);
WorldPoint clickTarget = rawTarget != null ? rawTarget : getPointWithWallDistance(smoothedTarget);
```

**Where this applies:** `Rs2Walker.processWalk`, recovery clicks after false unreachable reports, route continuation clicks, direct short-walk fallbacks, and any minimap click helper used while a shortest-path raw route exists.

**Defensive check:** Unit-test raw-route target selection with a route that doubles back near an earlier branch; the selected tile must stay at or ahead of the anchored raw route index.

## 16. Yield before scans while an interim route click is in flight

Sticky interim targets only reduce click thrash if they are checked before broad route scans. When a fresh interim checkpoint is still far away and the player is moving or has very recent checkpoint progress, yield before raw-scene scans, current-tile transport scans, direct short-walk attempts, and path segment scans. Resume normal route work once the checkpoint is close, stale, expired, on another plane, or movement has genuinely stopped.

**Why this matters:** Long post-transport routes can otherwise run several no-op scans and select several small follow-up clicks while the previous minimap flag is still carrying the player. That looks like stop-start walking and burns tail iterations even though the route is making progress.

**Pattern to follow:**

```java
if (shouldYieldForActiveRouteInterim(playerLoc, path, nowMs)) {
    exitReason = "interim-in-flight";
    continue;
}
```

**Where this applies:** `Rs2Walker.processWalk` before broad raw-scene handling, current-tile transport handling, direct short-walk handling, and the main path loop.

**Defensive check:** Start a long route immediately after a transport. While the player is still moving toward the active minimap checkpoint, the walker should log a tail-exempt `interim-in-flight` yield instead of repeated `post_transport_path_selected` clicks for nearby path indices.

## 17. Dispatch immediate planned transports before idle nudges

When the current route index is an explicit shortest-path transport edge, let the segment transport handler run even during startup, before route idle nudges or stall recovery clicks. Keep startup door/rockfall probes skipped, but do not postpone teleports, ships, ladders, or other catalog-backed route transports that start at the player's current segment.

**Why this matters:** Originless spell/item teleports can be the first edge in a route. If startup logic suppresses the segment handler and idle recovery runs first, the walker can sit on the origin, spam no-op route nudges, trigger stall recovery, and only then cast the teleport.

**Pattern to follow:**

```java
boolean immediateTransport = hasImmediatePlannedTransportStep(path, routeStartIdx, playerLoc);
if (shouldRunActiveRouteIdleNudge(idleNudgeDue, immediateTransport)) {
    tryIssueRouteRecoveryClick(rawPath, path, target, "active route idle nudge");
}
```

**Where this applies:** `Rs2Walker.processWalk` startup handling, active route idle nudges, stall recovery clicks, and segment transport dispatch.

**Defensive check:** Start a route whose first planned edge is Home Teleport. Logs should show `transport_handoff_enter` for `TELEPORTATION_SPELL` without several `active route idle nudge: clicked=false` entries first.

## 18. Model conditional paid gates as transports plus blocked edges

When a gate can be crossed by either a quest unlock or a currency payment, do not model the gate tiles as quest-only restrictions. A tile restriction cannot express "quest complete OR pay coins", and it can prevent the walker from reaching the paid transport origin. Model each crossing as explicit paid/free transports and add a blocked edge override so normal walking cannot bypass the unavailable transport.

**Why this matters:** The Lumbridge-to-Al Kharid toll gate can be opened freely after Prince Ali Rescue or paid through with 10 coins. A quest-only restriction blocks the paid route for accounts without the quest, while no edge block can let the pathfinder walk through the gate without paying.

**Pattern to follow:**

```java
// Wrong: restrict both sides of the gate to one quest state.
3267 3227 0        Prince Ali Rescue

// Right: transport data decides availability; blocked_edges prevents free walking.
3267 3227 0    3268 3227 0    Pay-toll(10gp);Gate;2786    ...    10 Coins
3267 3227 0    3268 3227 0    Open;Gate;2786              ...    Prince Ali Rescue
```

**Where this applies:** `transports.tsv`, `blocked_edges.tsv`, `restrictions.tsv`, `PathfinderConfig`, and `Rs2Walker.handleTransports`.

**Defensive check:** Add resource tests that assert both paid and quest-free transports load, the gate tiles are not quest-only restricted, and the gate edge is blocked without a usable transport.

## 19. Reachability-gate the FALLBACK click paths, and allow bidirectional rejoin

A tile being *walkable* (`Rs2Tile.isWalkable`, i.e. not `BLOCK_MOVEMENT_FULL`) is not the same as being *reachable from the player*. Next to a wall the two diverge hard: a tile flush on the far side of a castle wall is walkable and Euclidean-close, but only reachable via a long detour. This matters for click targets that are **invented** rather than taken from the route:

- `getPointWithWallDistance` picks a neighbour reachable *from the target*, from an unordered set, so its nudge can land on the far side of a wall or inside a building. Pass the player position so it prefers a neighbour reachable from and nearest to the player, and verify the result with `Rs2Tile.isTileReachable` before clicking it.
- The scaled-radius directional fallback in `walkMiniMapToward` invents a geometric point toward an off-clip target. Only click it if it is actually reachable, otherwise a fresh teleport produces clicks far off the route.

Forward-only anchoring (introduced to stop the walker snapping back to already-travelled branches) overcorrects when the player is pushed *off* the path: if nothing ahead is reachable, forward-only recovery returns nothing and the walker stalls. Recovery must therefore be **bidirectional**: scan a bounded index window on both sides of the anchor and rejoin via the nearest reachable raw point, preferring the furthest-forward one so normal progress is never sacrificed. This is a rejoin path for the off-path case only; steady-state progress stays forward-only.

**Do NOT extend this to primary selection.** An earlier revision selected the primary click from the player's bounded reachable set. That was reverted: a BFS bounded by click radius can exclude legitimate around-a-corner points and shorten clicks for no benefit, because a point taken from the raw route is safe by construction regardless of how the server walks to it (see #20). Reachability gating belongs only on targets that are *not* route points.

**Why this matters:** Walking past the Varrock West Bank, the wall-distance nudge and a stale-anchor fallback produced clicks that were off the planned route, so the game improvised a detour. The route-membership rule in #20 fixes the primary path; these guards fix the invented-target paths.

**Pattern to follow:**

```java
// Primary: an on-route point (see #20) — no reachability gate needed.
WorldPoint rawRouteTarget = selectRouteClickTarget(rawPath, playerLoc, reach, rawAnchorIndex);
if (rawRouteTarget != null) {
    clickTarget = rawRouteTarget;
} else {
    // Invented target: now reachability matters.
    clickTarget = getPointWithWallDistance(targetWp, playerLoc);
    if (!Rs2Tile.isTileReachable(clickTarget)) {
        WorldPoint rejoin = findReachableRejoinRawPathPoint(rawPath, playerLoc, reach, rawAnchorIndex);
        if (rejoin != null) clickTarget = rejoin; // step back onto the line if needed
    }
}
if (!reachable.contains(scaledFallback)) continue; // directional guesses too
```

**Where this applies:** `Rs2Walker.findReachableRejoinRawPathPoint`, `Rs2Walker.walkMiniMapToward`, `getPointWithWallDistance` consumers. **Not** `selectRouteClickTarget`.

**Defensive check:** Unit-test the rejoin core with an injected reachability predicate: (a) player pushed one tile off-path with nothing ahead reachable must return a point *behind* the anchor; (b) with points ahead reachable it must return the furthest-forward one inside the reach circle; (c) nothing reachable must return null so stall/recalc can take over.

## 20. Keep minimap click targets on the raw route — do NOT clamp them to line-of-sight

A minimap click is resolved by the **game's own pathing**, so line of sight is irrelevant to walking: a player clicks past a corner, through a doorway, or around a building and the server routes them there. The invariant that matters is that the click target sits **on the raw route** — then wherever the server chooses to walk, we arrive on the planned path. Do not select the click by clamping a far smoothed waypoint to a Euclidean radius either; that is what produced an off-route target in the first place.

**Why this matters:** Walking past the Varrock West Bank, selection returned null on a stale anchor (see #24), so the caller clamped a far smoothed waypoint to a ~10 tile radius and clicked `(3176,3428)` — a tile that is *not* on the raw route. The game pathed there its own way, deviating wide and backtracking.

**Do not "fix" this with line-of-sight.** An earlier revision clamped clicks to a straight walkable line. It removed the symptom but made the walker advance corner-to-corner — stopping at every corner to re-aim, because it would only click as far as it could see in a straight line. That is a visible behavioural tell and it bought no correctness: an on-route target is safe regardless of how the server walks to it. It was reverted.

**Pattern to follow:**

```java
// Furthest forward point ON THE RAW ROUTE within minimap reach. No LOS requirement.
WorldPoint forward = findFurthestRawPathPointMatching(rawPath, playerLoc, maxEuclidean,
        rawAnchorIndex, Rs2Walker::isKnownWalkableOrUnloaded);
```

**Where this applies:** `Rs2Walker.selectRouteClickTarget`, `findFurthestVisibleKnownRawPathPoint`, and any minimap click selection while a raw route exists. Pending doors/gates are handled by `handlePendingDoorBeforeRouteClick` — not by shortening the click.

**Defensive check:** `RouteClickTargetRegressionTest` asserts the historic `(3176,3428)` is not on the raw route, and that the route offers several on-route candidates within minimap reach so a click is never artificially shortened to the nearest corner.

## 21. Route continuation is one-shot — keep the idle-nudge window short

`tryIssueRouteContinuationClick` only runs on the single pass where `clearInterimTargetIfReachedOrExpired` returns `true` (the transition where the interim checkpoint is cleared). If any guard blocks it on that exact pass — `isInteracting`, `isAnimating`, door/transport settling, or its own unresolved-door / upcoming-transport checks — the opportunity is lost and **nothing retries it**. The route then only resumes via the active-route idle nudge, so that stationary window is the visible dead stop between hops. Keep it to a few ticks; do not treat it as a rare recovery path, because in practice it is what continues normal routes.

**Why this matters:** A 41-tile walk from `(3183,3435)` to `(3173,3399)` clicked correctly, walked to its checkpoint, then sat fully stopped for the whole `2500ms` idle window before issuing the next click — roughly a third of the total walk time spent standing still, once per hop.

**Pattern to follow:**

```java
// One-shot: only fires on the pass that clears the interim checkpoint.
if (clearedInterimTarget && !Rs2Player.isInteracting() && !Rs2Player.isAnimating()
        && tryIssueRouteContinuationClick(rawPath, path, target)) { ... }

// So the nudge window is the real continuation path — keep it a few ticks, not seconds.
private static final long ACTIVE_ROUTE_IDLE_NUDGE_MS = 1_200L;
```

**Where this applies:** `Rs2Walker.tryIssueRouteContinuationClick`, `clearInterimTargetIfReachedOrExpired`, `shouldIssueActiveRouteIdleNudge`, and `ACTIVE_ROUTE_IDLE_NUDGE_MS` / `ACTIVE_ROUTE_IDLE_NUDGE_COOLDOWN_MS`.

**Defensive check:** On a multi-hop route, the gap between `first_minimap_click` / route-fallback clicks and the next `active route idle nudge` should be roughly the walk time plus a few ticks — not walk time plus a full idle window. A repeating ~2.5s stationary gap per hop means continuation is being missed and only the nudge is driving the route.

## 22. Suppress the inverse adjacent transport even when the landing check fails

Gotcha #5 suppresses the inverse of an adjacent same-plane transport after a crossing, but only on the success path. Adjacent same-plane transports require landing on the *exact* destination tile, and agility shortcuts routinely deposit the player a tile off it — so a crossing can physically succeed while the landing check reports failure. On that path nothing was suppressed, leaving the inverse entry immediately eligible. Record the suppression whenever the player is no longer on the origin, regardless of the landing verdict; the landing result itself should stay unchanged so the route still replans.

**Why this matters:** Near `(3150..3151, 3363)` the walker crossed an `AGILITY_SHORTCUT`, landed a tile off the catalogued destination, logged `post-handleObject landing unresolved`, then immediately took the opposing entry back across and stranded itself — roughly 19s of a 2m26s walk spent bouncing and recovering.

**Pattern to follow:**

```java
boolean landed = waitForPostHandleObjectLanding(transport, destWait, maxInclusive);
if (!landed) {
    WorldPoint after = Rs2Player.getWorldLocation();
    // Off the origin => we crossed, even though the strict landing check failed.
    if (isAdjacentSamePlaneTransport(transport) && after != null && !after.equals(transport.getOrigin())) {
        markAdjacentSamePlaneTransportHandled(transport, object);
    }
}
```

**Where this applies:** `Rs2Walker.handleTransports` object interactions, `waitForPostHandleObjectLanding` callers, `markAdjacentSamePlaneTransportHandled`, and any adjacent same-plane transport regardless of `TransportType` — suppression is deliberately type-agnostic, so shortcuts and doors are covered alike.

**Defensive check:** A crossed shortcut should produce at most one `post-handleObject landing unresolved` followed by route progress away from the shortcut — not a second interaction with the opposing entry at an adjacent destination tile (`dest=3150,3363` then `dest=3151,3363`).

## 23. Never hash a raw cooldown timer into the transport cache key

The transport refresh verification hash must contain values that change only when a transport's *usability* changes. A `COOLDOWN_MINUTES` varplayer compares against wall-clock minutes, so its raw value advances continuously — hashing it guarantees cache misses forever. Worse, casting Home Teleport writes `LAST_HOME_TELEPORT`, which is one of the hashed varplayers, so **the teleport invalidated the transport cache simply by being used**, forcing a full re-evaluation of every transport (~2.6s) immediately after the teleport it had just performed. Hash the condition *verdict* (`TransportVarPlayer.matches`) plus the condition identity instead of the raw value: the verdict flips exactly once, when the cooldown expires and the transport actually becomes usable.

This is the same rule as #19's skill fix: only inputs that can flip a transport's usability may participate. Timers, regenerating stats, and other monotonically-drifting values must not.

**Why this matters:** A Camelot -> Draynor walk beginning with a Home Teleport logged
`transport_handoff ... ms=16562` with `refresh_transports cache_miss reason=verify ... changed=varplayers` firing inside that window — about 2.9s of a 16.5s teleport was the teleport invalidating its own cache.

**Pattern to follow:**

```java
// Wrong: raw value ticks every minute, and casting the spell writes it.
h = 31 * h + Microbot.getVarbitPlayerValue(id);

// Right: only a usability flip participates.
boolean satisfied = new TransportVarPlayer(id, value, operator).matches(actualValue);
h = 31 * h + id; h = 31 * h + operatorOrdinal; h = 31 * h + value;
h = 31 * h + (satisfied ? 1 : 0);
```

**Where this applies:** `PathfinderConfig.hashVarplayerConditionVerdicts`, `PathfinderConfig.hashVarbitConditionVerdicts`, `computeTransportRefreshVerificationHash`, the transport refresh snapshot, and any future cache key covering varbits/varplayers/skills. Varbits get the identical treatment: a raw varbit moving without flipping a condition verdict must not invalidate, which is what produced the ~2.4s dead time between pressing go and the first action.

**Defensive check:** `refresh_transports cache_miss reason=verify ... changed=varplayers` should not appear immediately after a teleport. Unit-test that two different raw values inside the same cooldown window hash identically, and that crossing the threshold does not.

## 24. Vary click reach, not click direction

Route selection otherwise always returns the furthest candidate inside a fixed radius, so every click covers the same tile span — a deterministic signature. Jitter the reach per click instead (`routeClickReach`), which only changes how far **along** the route the target sits. Do **not** offset the target sideways: a lateral tile offset leaves the planned route, which is what produces clicks on the wrong side of a fence or inside a building (#15), and route membership is the property that keeps the server's own pathing on our route (#20). Lateral randomness belongs *inside* the tile, in click-point jitter, where it changes the pixel without changing the destination.

Two bounds are load-bearing. The floor must stay clear of `INTERIM_CLOSE_TILES`, or a short click lands inside the interim-close threshold, clears the checkpoint immediately and produces click thrash with visible stop-start movement. The ceiling is the caller's reach, already tuned to the minimap clip; above it clicks simply fall outside the clip and take the fallback path. A shortened reach must also never be the reason selection fails — retry at full reach before falling through, or the click drops onto the off-route wall-nudge clamp (#19).

**Pattern to follow:**

```java
int jitteredReach = routeClickReach(maxEuclidean);
WorldPoint selected = selectRouteClickTargetAnchored(rawPath, playerLoc, jitteredReach, rawAnchorIndex);
if (selected == null && jitteredReach < maxEuclidean) {
    selected = selectRouteClickTargetAnchored(rawPath, playerLoc, maxEuclidean, rawAnchorIndex);
}
```

**Where this applies:** `Rs2Walker.routeClickReach`, `Rs2Walker.selectRouteClickTarget`, and any future click-target randomisation.

**Defensive check:** Unit-test that the jittered reach never exceeds the caller's reach, never drops to the interim-close threshold, actually varies across calls, and passes a degenerate reach through unchanged.

## 25. Never charge a retry budget for an iteration that made progress

A "give up after N attempts" counter must count *failures to advance*, not loop iterations. Two things break it otherwise.

First, **a partial path is a persistent condition, not an event.** `partialPath` is recomputed every iteration as "the path end is further than `distance` from the goal", so any goal the pathfinder cannot fully reach — a tile short of an underground destination, a gated area, a dynamic-region target — makes it true from the first snapshot to the last. The budget is then armed for the entire walk rather than for a stuck stretch.

Second, **the path loop exits for good reasons too.** Opening a door, taking a transport, clearing a rockfall, or having movement already in flight all end the iteration and land in the same branch as "we failed to move". Charging those spends the budget while the walker is doing exactly what it should.

Together they are quietly fatal on long multi-leg routes. Observed: a walk to an underground goal whose path end sat 31 tiles short burned retry 1 at the end of the first segment, walked ~100 tiles over 46 seconds, then spent retries 2 and 3 within one second of opening a single door — the second without the player moving at all. It reported `UNREACHABLE` standing at the entrance, having never reached the ladder. Nothing in the log said "stuck", because it never was.

So: refill the budget when route progress has advanced since the last charge, and exempt exit reasons that denote work done. Keep the exemption **narrow** — reasons meaning the walker failed to advance (`not-near-path`, `click-failed-off-minimap`, waiting/retry states) must still consume it, or a genuinely unreachable goal never terminates and instead spins until the outer tail cap trips.

**Where this applies:** `Rs2Walker.processWalk` partial-retry branch, `Rs2Walker.isRouteProgressExit`, and any future attempt-capped recovery loop.

**Defensive check:** Unit-test both directions of the classifier — every progress reason exempt, every stuck reason still charged, `null` treated as not-progress.

## 26. Telemetry must report measurements, not placeholders

`recordUnreachable` was called with the player's location as `pathEndpoint` and a literal `0` as `pathSize`. The emitted line read as *"the pathfinder returned an empty path ending at your feet"* — a completely different and much scarier failure than the real one, and it sent diagnosis after a phantom pathfinder bug.

It also corrupted the derived field. `endpointToTarget` is computed from `pathEndpoint`, so instead of the true 31-tile shortfall it printed `6346`: the raw y-delta between a surface tile and an underground one. Both are plane 0 — underground is the same plane at `y + 6400` — so `WorldPoint.distanceTo` does not bail out, it just returns a meaningless number. Any distance compared across that boundary is nonsense, and any threshold against it can never be met.

Pass the real endpoint and the real size. If a value is genuinely unavailable, log it as absent rather than substituting a plausible-looking stand-in.

**Where this applies:** `Rs2Walker.Telemetry.*`, `WebWalkLog`, and any diagnostic that derives a field from an argument.

**Defensive check:** When a log line drives a diagnosis, confirm at the call site that each value is measured rather than hardcoded before trusting it.

## 27. Gate direct minimap clicks by Euclidean distance

`WorldPoint.distanceTo` / `distanceTo2D` use Chebyshev distance, but the usable minimap area is
approximately circular. A diagonally offset endpoint can therefore pass a short-walk distance check
while lying outside the minimap clip. Before trying a direct endpoint click, apply an explicit
Euclidean-radius check. When a raw route exists and the endpoint is outside that radius, select a
forward route tile immediately; this is normal continuation, not an exceptional fallback.

**Why this matters:** Near the end of a long route, the walker tried an endpoint that was diagonally
within 13 Chebyshev tiles, predictably failed the circular minimap clip, and then logged a forward
raw-route tile as a seemingly random fallback.

**Pattern to follow:**

```java
if (shouldAttemptDirectMinimapTarget(end, player, maxEuclidean) && walkMiniMap(end)) {
    return true;
}
WorldPoint continuation = findFurthestVisibleKnownRawPathPoint(rawPath, player, maxEuclidean, anchor);
return continuation != null && walkMiniMap(continuation);
```

**Where this applies:** `Rs2Walker.tryDirectShortWalk`, route-backed final clicks, and any helper
that converts a Chebyshev proximity check into a minimap click.

**Defensive check:** Test a diagonal delta such as `(11, 11)` against a 12-tile minimap radius; it
must be rejected even though its Chebyshev distance is only 11.

## 28. Distinguish spatial proximity from route proximity

A route that detours around a mountain, fence, or building can pass close to itself. A future
smoothed waypoint may then be only a few world tiles from the player while remaining hundreds of raw
route steps ahead. Do not treat that waypoint as the immediate locally unreachable blocker and do
not anchor recovery to it. Bound local recovery candidates by forward raw-route distance; when the
candidate belongs to a distant route fold, continue from the stabilized current route frontier.

**Why this matters:** South of White Wolf Mountain, the route from Catherby toward Taverley first
travels around the mountain and later returns near `(2867,3442)`. Local recovery selected that
spatially close future branch from around `(2855,3440)`, causing every minimap click to fight the
mountain collision and route the player back west indefinitely.

**Pattern to follow:**

```java
if (!isLocalRecoveryCandidateOnForwardRoute(rawPath, smoothedToRaw,
        routeStartIdx, candidateIdx, maxRawSteps)) {
    issueRouteContinuationFromCurrentFrontier();
    return;
}
recoverToward(candidate);
```

**Where this applies:** `Rs2Walker.processWalk` local-reachability recovery, route progress
stabilization, and any fallback that chooses a waypoint using world distance on a self-near route.

**Defensive check:** Construct a raw route whose final branch returns near its beginning. A
spatially close waypoint beyond the raw-step budget must be rejected while an ordinary nearby
forward waypoint remains eligible.

## 29. Let the owning walker thread evaluate early-completion conditions

When a script can interact before reaching the coordinate destination, pass a read-only
completion condition to `Rs2Walker.walkUntil`. Do not run `walkTo` in one executor task while
another task polls entities and calls `clearWalkingRoute` or interrupts the walk. The walker
already owns its route lock and cancellation checkpoints; evaluating the condition there lets it
stop issuing movement clicks and release its state before the script performs the interaction.

**Why this matters:** A companion polling task that cancelled a walk during plugin shutdown could
contend with the walker and client-thread calls. The plugin stopped, then unrelated scripts began
timing out on the client thread and the client appeared frozen.

**Pattern to follow:**

```java
boolean ready = Rs2Walker.walkUntil(destination, interactionDistance,
        () -> isTargetInteractable());
if (ready && isTargetInteractable()) {
    interactWithTarget();
}
```

The condition must only inspect state. It must not click, walk, sleep, or otherwise mutate game
state. Re-query the entity before interacting because cache results are snapshots.

**Where this applies:** `Rs2Walker.walkUntil`, scripts approaching moving NPCs, fishing spots,
banks, shops, or any destination whose interactable entity can become ready before coordinate
arrival.

**Defensive check:** Existing `walkTo` and `walkWithState` overloads must not install or evaluate a
completion condition. Test immediate condition completion without walker setup, and test that an
exception from the condition falls back to ordinary distance-based walking instead of retaining
the walker lock.

## 30. Do not let distance arrival overtake a pending route interaction

A player can enter the configured finish radius while a door, gate, or mineable edge is still
pending. Resolve that interaction lifecycle first: verify clearance, cross the exact route edge,
and retire the pending interaction before returning `ARRIVED`. The finish radius remains a caller
policy and must not be silently reduced by the engine.

**Why this matters:** A ranged door command placed the player within the sidebar's five-tile finish
distance. Because arrival was checked before interaction verification, the walk completed beside
the opened door without crossing toward the selected target.

**Pattern to follow:**

```java
if (hasArrived(player) && !hasUnresolvedRouteInteraction(observation)) {
    return COMPLETE;
}
return handleRouteInteraction(observation);
```

**Where this applies:** `NavigationEngine`, `WalkSession.pendingInteraction`, and any future
distance-based terminal predicate added to the unified walker.

**Defensive check:** Start inside the requested finish radius with a door interaction on the next
raw edge. The engine must interact, verify, cross, and retire the edge before it may complete.

## 31. Chain exact route interactions before issuing a crossing nudge

After an interaction clears, a second exact route interaction may already be loaded and within the
bounded dispatch range. Dispatch that successor directly and let the server path through the
cleared edge. Keep the cleared edge recorded until raw-route progress proves it was crossed. Use the
one-edge minimap crossing only when no safe successor is ready.

**Why this matters:** A corridor containing several ordinary doors produced an alternating sequence
of door clicks and one-tile minimap clicks. Each subsequent door click would already have carried
the player through the previously opened door, making those ground clicks redundant and visibly
mechanical.

**Pattern to follow:**

```java
if (current.isCleared() && next != null && next.isReady()) {
    rememberClearedEdgeUntilCrossed(current);
    return interact(next);
}
return crossClearedEdge(current);
```

**Where this applies:** All `RouteInteraction` families, including ordinary doors, gates,
mineables, and later migrated shortcuts or transports that use the common interaction lifecycle.

**Defensive check:** Present two consecutive ready interactions. Clearing the first must issue the
second interaction with no intervening tile click, while arrival remains blocked until every saved
cleared edge has been crossed.

## 32. Do not let live collision invalidate an engine-owned interaction edge

Dynamic doors, gates, and blockers can disagree with the static collision map while their common
interaction lifecycle is opening or crossing them. Suppress live-collision recovery temporarily
for each pending or cleared-but-uncrossed raw edge and its immediate index neighbour. Continue to
report unrelated blocked edges normally, and remove the protection as soon as route progress proves
the interaction edge was crossed.

**Why this matters:** A successful multi-door chain was interrupted twice by `BLOCKED_EDGE`
replans. The validator reported indices one step after the active door edges while the server was
still resolving the object commands, replacing otherwise valid route generations.

**Pattern to follow:**

```java
if (snapshot.isInteractionCollisionProtected(blockedEdgeIndex)) {
    consumeCollisionObservation();
    return;
}
reportBlockedEdge(blockedEdgeIndex);
```

**Where this applies:** `NavigationEngineRuntime`, `NavigationSnapshot`, live route collision
validation, and future interaction families using dynamic collision.

**Defensive check:** A collision on a pending interaction edge or its adjacent anchor must not queue
recovery; a collision several edges ahead must still queue `BLOCKED_EDGE` recovery.

## 33. Classify interaction ownership before treating a door action as ordinary

A door-like name and an `Open`, `Pass`, or `Walk-through` action do not prove that an interaction is
an ordinary one-step door. Region-specific doors can open a question dialogue, while `Pick-lock`
and `Pay-toll` edges are catalog transports with requirements or confirmation UI. Decide ownership
before the ordinary route scanner or engine can issue the interaction.

**Why this matters:** Stronghold of Security question doors look ordinary to the scene classifier.
Letting the engine claim them would bypass the only handler that can answer the question, leaving a
pending door that repeatedly appears available. Conversely, treating toll and lock-pick rows as
ordinary doors would bypass transport eligibility and post-interaction handling.

**Pattern to follow:** Keep specialised regions legacy-owned until their dialogue flow is modeled as
non-blocking session state; reject their objects in the ordinary scene adapter as a second guard.
Keep catalog-backed toll and lock-pick edges transport-owned and migrate them with their transport
family.

**Defensive check:** A route starting or ending in a Stronghold of Security region must not enable
ordinary engine execution. A normal surface route must remain eligible, and the ordinary scene must
never return a Stronghold question door.

## 34. Publish transport-family eligibility with the route

Do not let a runtime object scan silently decide that an otherwise legacy transport route is safe
for engine execution. Classify each explicit transport edge when the immutable route is published,
using the enabled catalog rows that actually connect its directed endpoints. Engine ownership is
allowed only when every edge belongs to a migrated family.

**Why this matters:** Once adjacent gates and shortcuts share the common interaction lifecycle, the
old boolean distinction between an "ordinary" route and "any transport" is too coarse. Conversely,
making every adjacent transport engine-owned would accidentally include toll gates, lock-picking,
dialogue, item preparation, and other flows the engine cannot yet complete.

**Pattern to follow:** Give migrated families explicit `RouteEdge.Kind` values. Apply a conservative
eligibility policy at path publication, repeat that policy while resolving the live object, and keep
unsupported rows `LEGACY_LOCKED` before any input is issued. A pending persistent shortcut whose
object disappears is unavailable; only actions known to transform/open their object may treat
disappearance as clearance.

**Defensive check:** A one-tile object-backed direct action must produce an engine-supported edge and
one interaction command. A toll, lock-pick, dialogue, cross-plane, non-adjacent, or specialised row
must keep the complete request legacy-owned.

## 35. Arrival tolerance cannot skip a published interaction edge

Being within the caller's destination tolerance is not sufficient for arrival when the selected
route still contains an uncrossed engine-owned interaction edge. The destination can be only a few
tiles beyond a closed door or gate, so a distance-only arrival check can finish on the wrong side
before the live scanner gets a chance to publish the interaction.

**Pattern to follow:** Gate distance-based arrival on route topology as well as pending live state.
Every published engine-owned interaction edge at or beyond raw progress must be crossed first. Do
not rely only on finding the live object: an absent `Open` object may mean the door is already open,
in which case the route must still walk through the edge before completing.

**Defensive check:** Start inside the arrival radius but before an adjacent transport edge. The
engine must issue movement or interaction rather than `COMPLETE`; after raw progress crosses the
edge, the same destination tolerance may complete normally.

## 36. Catalog ownership outranks door-like appearance

A transport-catalog object can be named `Door` or `Gate` and expose an `Open` action without being
an ordinary door. Toll gates commonly publish mutually exclusive paid and quest-completed rows for
the same physical object. Classifying from its scene name alone can bypass payment, dialogue, or
quest handling.

**Pattern to follow:** Once an enabled catalog row owns an object or crossed boundary, exclude it
from ordinary-door classification. Match route crossings by directed boundary, not only exact
endpoint equality: a diagonal raw step across either lane of a two-tile gate still crosses that
catalog transport. Apply migrated-family eligibility only after this ownership match.

**Defensive check:** An Al Kharid toll-gate route for an account that must pay remains
`LEGACY_LOCKED` and never emits `selection=interaction-door`. A free/open eligible row may become an
adjacent transport, but it must emit `selection=interaction-adjacent_transport` instead.

When a route plan already labels an edge as a transport, do not allow an object-level ordinary
scanner to compete for that edge. Scene object ids can change through impostors or object state;
use the route kind as the primary ownership boundary. A transformed-id fallback may use the
catalog boundary plus exact object name and action, but proximity alone is insufficient.

## 37. Do not use a one-tile minimap nudge as normal post-interaction continuation

After a door, gate, or mineable clears, clicking only the immediately adjacent route tile can land
inside the minimap's centre dead zone. The adapter may report an issued click even though the
client never registers a new movement destination. Recovery then replans from the interaction
boundary and can reopen or repeat the same interaction.

**Pattern to follow:** If the forward interaction scan found no next blocker, select the next
bounded raw-route checkpoint beyond the cleared edge and let that command carry the player through.
If another interaction prevents forward lookahead, retain the exact one-edge crossing as a fallback
but prefer a canvas/local-tile click before minimap input.

**Defensive check:** Opening the Al Kharid gate in either direction must be followed by one forward
route command and progress, without `NO_ACKNOWLEDGEMENT`, generation churn, or redispatching the
same gate interaction.

The interaction-command deadline must include server approach distance. A fixed timeout suitable
for an adjacent click can expire while a valid ranged click is still pathing to the object, causing
an unnecessary second interaction before the first command has had time to execute.

## 38. Persistent transport objects cannot acknowledge their own crossing

Some gates and shortcuts remain present and interactable from both sides after a successful
interaction. Object disappearance and raw-route progress are therefore insufficient acknowledgements:
a ranged interaction can put the player on the destination side while the route anchor still points
at the blocking edge. Redispatching the same object from there can pull the player back across it.

**Pattern to follow:** Preserve the enabled catalog row's directed origin and destination in the
pending interaction. Retire the interaction as soon as the player is on or just beyond that
destination boundary, even if the live object still resolves. Use the catalog boundary rather than
the raw path step because a parallel double gate may be represented by a neighbouring diagonal edge.

**Defensive check:** A ranged click on either direction of the Al Kharid double gate must produce one
interaction. Reaching the destination-side catalog tile must clear it without a second gate click,
generation change, or return to the origin side; remaining on the origin side must not clear it.

## 39. Scene transitions complete at their catalog landing, not at object transformation

Stairs, ladders, caves, and trapdoors often jump to another plane or a distant coordinate. Their
object may remain visible, disappear immediately, or transform before the player actually travels.
None of those states proves that the transition completed.

**Pattern to follow:** Carry the catalog destination in the pending interaction and acknowledge the
command only when the player reaches the destination plane within a small coordinate tolerance.
Keep direct object clicks separate from dialogue, currency, and item-use transports. A closed
trapdoor is a two-stage interaction: `Open` advances the live object stage, then the catalog action
such as `Climb-down` must be dispatched; it is not clearance by itself.

**Defensive check:** A direct ladder/stair/cave interaction remains pending on the origin side and
clears after its catalog landing. When a closed trapdoor changes from `Open` to `Climb-down`, the
second action dispatches without waiting for the first action's acknowledgement deadline.

## 40. Originless teleports still need exact route-edge ownership

Item and spell teleports have no fixed catalog origin. During a search the pathfinder attaches each
currently usable teleport to the node where it may be invoked, so looking up only the transport's
declared origin cannot recover the row selected by the route.

**Pattern to follow:** Resolve the teleport from the enabled transport snapshot at the published raw
edge's `from` tile and require its destination to equal the edge's `to` tile. Preserve the transport
type and display identity through dispatch, then acknowledge only from arrival near the catalog
destination. Keep multi-stage interfaces (destination submenus, confirmations, scroll books, and
long home teleports) outside the direct-teleport family until their UI states are modelled. A generic
item action such as `Break` or `Teleport` must return whether that input was issued; landing remains
a separate observation and a successful click must not be reported as command rejection. Once that
command is accepted, consumption of the last item/runes may remove the row from the refreshed usable
catalog before landing; preserve the pending interaction until landing or its acknowledgement
deadline instead of treating this expected disappearance as immediate unavailability.

**Defensive check:** A direct spell/tablet edge is engine-owned, issues exactly one interaction, and
remains pending until the destination landing. A colon-delimited destination menu or Lumbridge Home
Teleport remains `LEGACY_LOCKED` and receives no unified-engine input.

## 41. NPC travel ownership must be narrower than the transport type

`SHIP`, `BOAT`, and `NPC` catalog rows do not describe one interaction protocol. Some expose a
direct destination action, while others start a conversation, charge currency, display a
destination menu, or run named quest dialogue. Treating the enum alone as engine eligibility can
issue the first click and then strand the engine in an unmodelled interface.

**Pattern to follow:** Initially migrate only exact NPC-id rows with a direct non-`Talk-to` action,
no currency or item requirement, and no separate display/destination choice. Resolve the enabled
directed catalog edge and exact NPC name/id before dispatch. Keep the catalog destination as the
acknowledgement predicate because the source NPC commonly unloads as the voyage begins; its
disappearance is not interaction failure. A valid voyage can also pass through a temporary ship
deck or departure scene far away from both route endpoints. While its bounded command window is
active, the pending transport owns that intermediate position and ordinary off-route recovery must
not replan. Raw progress alone must not retire an NPC voyage: an intermediate scene can share the
destination plane and therefore look like the destination raw index despite being hundreds of
tiles away. Require the catalog landing observation. Size the family deadline for boarding plus
travel animation, not ordinary object-click latency.

**Defensive check:** A direct `Travel`/`Follow` ship, boat, or NPC edge is engine-owned and issues
one NPC interaction, then clears only near its catalog landing. A `Talk-to`, paid, or
destination-selection row remains `LEGACY_LOCKED` and receives no unified-engine input.

## 42. A voyage landing may still be aboard the destination ship

An NPC-backed ship edge can finish on a ship deck rather than on ordinary land. For example, the
Void Knights-to-Port Sarim catalog lands on plane 1 and then uses a separate one-tile-origin
`Cross;Gangplank` transport to reach plane 0. Migrating the NPC voyage alone therefore leaves the
whole route legacy-locked even though the outward route can appear accepted when its requested
endpoint is the ship landing itself.

**Pattern to follow:** Treat an exact direct `Cross` action on a catalogued `Gangplank` as a scene
transition, resolve its exact object/action through the tile-object cache, and retain the common
interaction-chain lifecycle between the voyage landing and disembark edge. Do not generalise this
eligibility to arbitrary bridges or unrelated `Cross` actions.

**Defensive check:** A reverse ship route containing `NPC_TRANSPORT`, an optional ordinary deck
step, and `CATALOG_TRANSITION` remains engine-supported and dispatches the gangplank only after the
catalogued voyage landing is observed.

## 43. Paid menu transports are multi-stage commands

A charter ship is not one transport click. The catalog row first opens the charter interface from
an exact Trader Crewmember, the interface selects one named destination, a dialogue confirms the
fare, and only then does the remote voyage begin. Treating any successful stage as clearance can
resume ground walking aboard the source ship or redispatch the NPC while a widget is open.

**Pattern to follow:** Publish paid coin-only charter rows as their own route family. Preserve the
directed catalog edge and advance one pending interaction through exact NPC, destination-widget,
and `Yes` confirmation stages. After confirmation, keep the interaction pending while the NPC and
widgets unload, and acknowledge only near the catalog landing. Do not use handler-owned sleeps or
infer success merely because the fare item or enabled row disappears.

**Defensive check:** A charter edge issues exactly one input at each UI stage, waits without input
during the voyage, and clears only at its directed landing. Unpaid rows, non-coin requirements, and
other ship protocols remain outside this family.

## 44. Catalog NPC ids may transform in the live scene

Transport data can correctly identify an NPC definition while the loaded actor exposes another id
after a varbit-driven transform. Port Sarim's charter row names Trader Crewmember `9342`, while a
live accepted state exposed the nearby charter actor as `9377`. An exact-id-only lookup therefore
made an otherwise valid engine-owned route end at the charter origin without publishing its next
interaction.

**Pattern to follow:** Keep the catalog id as the preferred route identity, but permit a transformed
live id only when the actor has the exact catalog name, exact action, correct plane, and lies within
a small radius of the directed catalog origin. Rank the exact catalog id first when both variants
are present. Do not fall back to name alone where multiple same-named NPCs share a scene.

**Defensive check:** A transformed charter NPC beside the origin resolves, while the same name with
the wrong action or an actor beyond the origin tolerance remains unavailable.

## 45. Treat fairy-ring travel as observed stages, not one blocking helper

A fairy-ring route can require staff equipment, object interaction, three dial rotations, travel
confirmation, an unloaded source scene, and restoration of the displaced weapon. None of those
intermediate commands proves that the directed catalog edge crossed.

**Pattern to follow:** Publish only directed three-letter non-POH rows as `FAIRY_RING`. Preserve the
original weapon id in the pending interaction and issue at most one stage command per engine pass.
Capture widget rotations and tile-object composition on the client thread, retain the edge while
the interface is absent during travel, and clear the teleport only at its directed landing. For
restoration, force the inventory tab open as its own command, wait until the inventory container is
visible, then use the normal inventory equip path.

**Defensive check:** Do not equate `issued=true` with restoration. Keep the interaction pending
until the equipment cache confirms the remembered weapon is worn. Live testing showed that direct
dispatch to a hidden inventory container could report issuance while leaving the Dramen staff
equipped. Restoration must also use the immutable pending edge rather than rediscovering the live
transport row: moving the required staff from inventory to equipment can remove that row from the
usable transport snapshot at the landing refresh. The original weapon can also briefly appear in
neither inventory nor equipment while those caches settle after teleporting; treat that absence as
a wait state, not successful restoration. Keep `DIQ` and configured POH-anchor edges
legacy-owned until POH instance mapping is migrated.

**Engine ownership check:** Add every migrated remote transport family to both navigation-engine
remote-edge guards. Raw progress normally jumps beyond the transport edge as soon as the player
lands; generic crossed-edge retirement must not delete a still-available staged interaction such as
fairy-ring weapon restoration.

## 46. Spirit-tree routes are object, destination, and directed landing stages

The non-POH spirit-tree catalogue combines an origin object row (`Travel;Spirit Tree;<id>`) with a
numbered destination row such as `4: Grand Exchange`. A successful object click only opens interface
`187`; it does not prove travel.

**Pattern to follow:** Resolve the exact catalogue object through the shared tile-object cache near
the directed origin, publish one `Travel` command, then locate the destination text after removing its
number/letter prefix and publish one destination command. Keep the pending interaction while both the
source object and interface are unloaded, and clear it only at the directed catalogue landing.

**Defensive check:** Spirit-tree travel advances raw progress beyond the network edge at landing, so
it must participate in the navigation engine's remote-edge retention and unresolved-engine-edge
guards. Exclude synthesized POH rows until POH instance mapping owns their real object coordinate.

Spirit-tree destinations moved from the legacy adventure-log interface `187:3` to the reusable menu
component `InterfaceID.MenuNew.TEXT` (`947:9`). Resolve the current component first with the existing
client-thread-safe `Rs2Widget.findWidget` traversal and retain `187:3` only as a compatibility
fallback. Live Agent Server inspection showed the destination entries as dynamic children of
`947:9`, including `2: Gnome Stronghold`; hard-coding only the old interface leaves the interaction
stuck after `Travel`.

Being listed in the spirit-tree menu does not mean a destination is unlocked. Selectable entries use
text colour `0xff981f`; locked entries render as `0x5f5f5f` and still have normal text widgets. The
row's base `getTextColor()` remains `0xff981f` for both states; a locked destination instead carries
markup such as `<col=5f5f5f>Port Sarim</col>` in `getText()`. Inspect both representations on the
client thread, never click the locked entry, session-disable both directions of its destination in
`PathfinderConfig`, invalidate the transport memo, and publish `UNAVAILABLE` so the navigation
engine replans immediately.

Farmable spirit trees at Etceteria, Brimhaven, Port Sarim, Hosidius, and the Farming Guild are not
present merely because the account satisfies the network quests. Keep these destination toggles
opt-in. If a toggle is stale, confirm the missing `Travel` object only after the player is within
four tiles of the catalog origin, session-disable both directions through the same unavailable-tree
set, and publish `UNAVAILABLE` for an immediate replan. Do not infer a distant patch's state from the
region-local farming transmit varbits.

Transport route comparison must score reconstructed edges with the same duration model as the
pathfinder. Counting `path.size()` makes every long-distance transport look like a one-tick hop even
when `Transport.duration` influenced path selection. Use `TransportCostModel` for both search edges
and direct-vs-bank scoring. Its measured floors currently reflect successful live traces: 24 ticks
for staged fairy-ring travel and 12 ticks for spirit-tree menu travel; a larger catalog duration wins.

## 47. Gnome gliders expose transformed captains and generated map buttons

The NPC id stored in `gnome_gliders.tsv` is not always the id exposed by the loaded actor. At White
Wolf Mountain the catalog identifies Captain Bleemadge as `10459`, while the live cache exposed
`10461`. Exact-id-only lookup therefore cannot open an otherwise valid glider route.

**Pattern to follow:** Prefer the catalog NPC id, but accept a transformed id only when the actor has
the exact catalog name, the exact `Glider` action, the correct plane, and lies within a small radius
of the directed origin. Resolve destinations through `InterfaceID.Glidermap` rather than decimal
packed ids: the generated button constants correspond to the live destination actions. Treat a
missing/hidden button on a visible map as unavailable, session-disable both directions, and replan.
After selection, retain the remote edge until the directed catalog landing is observed.

**Defensive check:** A transformed captain with the wrong action, name, plane, or origin distance
must not resolve. Opening the map is only stage progress; it is not proof that the flight landed.
The measured eight-tick catalog cost remains the conservative route floor for this family.

## 48. Short portal doors may span two catalog tiles

Same-plane object transports are usually adjacent, but the Gnome Stronghold `Tree Door` rows cross
from one side of the doorway to the other with a catalog distance of two. Treating every distance
greater than one as unsupported forces an otherwise migrated glider route back to the legacy walker
after it lands.

**Pattern to follow:** Keep ordinary adjacent eligibility unchanged and admit only the exact
object-backed `Open;Tree Door` transport family at a maximum distance of two. Preserve the directed
catalog origin and destination so clearance is acknowledged only on the destination side. Do not
generalize this exception to arbitrary two-tile doors, entrances, gates, or puzzle objects.

**Defensive check:** Both straight and diagonal Stronghold rows publish as adjacent transports;
an unrelated `Open;Door`, the wrong transport type, or a three-tile tree-door row remains legacy-owned.

## 49. Quetzal routes have no stable catalog NPC id

The `quetzals.tsv` network is assembled from origin-only and destination-only rows. Its directed
edges intentionally carry no NPC id, name, or action, while the live mount can expose several Renu
definitions according to quest and appearance state. Requiring one hard-coded green Renu id, or
persisting the first observed live id as route identity, makes a valid route unavailable when the
NPC transforms.

**Pattern to follow:** Own only directed `QUETZAL` rows with a named destination. Resolve the live
actor through the shared NPC cache using exact `Renu` name, exact `Travel` action, correct plane,
and bounded distance from the catalog origin. Treat the live id as telemetry rather than identity.
Model `Travel`, exact `InterfaceID.QuetzalMenu` destination selection, and directed landing as
separate observations. Keep originless quetzal-whistle item transports outside this family.

**Defensive check:** Every known Renu variant with the correct name/action/origin remains eligible;
the wrong name, missing action, wrong plane, or actor beyond the origin tolerance does not. A map
click remains pending until the directed catalog landing and cannot be retired by raw-index progress.

## 50. POH routing coordinates are logical, not loaded-scene coordinates

Inside a player-owned house, `WorldPoint.fromLocalInstance` exposes the reusable house-template
coordinate while cached objects can expose the loaded instance coordinate. The configured POH exit
tile is therefore a logical pathfinder anchor and cannot be used as a normal proximity query around
the loaded exit-portal object. The transport graph must also publish both directions: an outside
`Home` portal edge does not imply an inside exit edge.

**Pattern to follow:** Store the exit portal's template coordinate as the route origin/destination,
but resolve the exact `POH_EXIT_PORTAL` object from the shared tile-object cache without a logical
coordinate radius. Publish exact `Portal/Home` and `Portal/Enter` rows as `TransportType.POH`
directed catalog
transitions, retain them through the scene unload, and acknowledge only at the directed logical or
outside landing. This keeps both physical portal directions behind the master `Use POH` toggle;
classifying them as `TELEPORTATION_PORTAL` incorrectly couples them to the unrelated generic-portal
setting. Never use the loaded instance object's world coordinate as a persistent route key.

**Defensive check:** The POH portal graph contains outside-to-inside and inside-to-outside edges.
Both are engine-owned, while an unrelated teleportation portal action or object name remains
unsupported. The inside resolver may click the loaded object, but readiness is measured against the
configured logical exit tile. Detection saves facilities and the logical tile but does not enable
`Use POH`; verify that master toggle before treating a missing route as an instance-mapping failure.

## 51. Seasonal transport data is not an execution contract

Seasonal TSV rows identify a destination, required item, cost, and League varbit, but they do not
identify the interface protocol needed after the item is activated. Enabling every row solely from
`useSeasonalTransports` can therefore publish a route the walker cannot execute and repeatedly select
the same zero-origin edge.

**Pattern to follow:** Require a registered executor for the exact seasonal row shape before adding
it to the usable transport graph. Clue compass rows use their destination as the inventory action or
submenu action. Map of Alacrity rows parse `Region - Destination`, open widget `187:3`, select the
region and then the destination, treat `<str>` rows as locked, and prefer the displayed hotkey so
off-screen entries remain usable. A discovered lock must invalidate the transport memo as well as
the session availability set; otherwise a cached snapshot republishes the rejected edge.

**Defensive check:** Every packaged seasonal row maps to one executor, malformed/unknown rows remain
unpublished, punctuation and colour markup do not break Map destination matching, and locked rows
never receive a click. Keep live acceptance deferred when the relevant League world and item are not
available rather than claiming UI proof from headless parsing tests.

## 52. Bank-route eligibility and withdrawal accounting are one contract

A pathfinder can select a route using an item known to be in the bank while a separate route filter
silently omits that edge from withdrawal planning. The same split can underfund repeated paid edges,
over-withdraw reusable equipment once per edge, or begin the final walk after a failed withdrawal.
Mutating shared currency transports into ordinary item requirements is especially unsafe: later
checks may mistake one coin for a complete fare and reusable-item aggregation may replace fare sums.

**Pattern to follow:** Use one predicate for route-analysis and withdrawal eligibility. Determine
originless teleports through `TransportType.isTeleport(type, origin)`, not a hand-written list of
teleport families. Sum consumables, runes, and currency per use; take the maximum for reusable items;
then subtract what the inventory already carries at withdrawal time. Keep pure fare metadata intact.
An explicit coordinator should own the bank leg, open/withdraw/close transaction, final inventory-only
replan, and target leg, while delegating each walking leg to the navigation engine. A missing bank item,
failed withdrawal, or unconfirmed inventory update is terminal and must not start the target leg.

**Defensive check:** Exercise a repeated reusable-item route, a repeated coin-fare route, a partial
inventory shortfall, and an unavailable bank item. The first withdraws one item, the second sums all
fares, the third withdraws only the difference, and the fourth never issues the final walk.

## 53. One portal action does not always identify one directed landing

A remote transport catalog can contain two edges with the same origin, object id, object name, and
action but different destinations. Treating both as deterministic engine-owned edges makes route
selection promise a landing that the input itself cannot select. The Castle Wars Guthix portal is
the concrete case: six identical object inputs each declare both alliance waiting-room landings.

**Pattern to follow:** Before migrating a direct-action remote family, group rows by the complete
live input identity and require one destination per group. Resolve the exact object through
`Microbot.getRs2TileObjectCache()`, issue only the catalog action, and retain the interaction until
the directed landing is observed. Leave ambiguous groups legacy-owned unless the executor explicitly
models every valid outcome and replans from the observed landing.

**Defensive check:** Of the 100 packaged generic portal rows, the 88 deterministic rows publish as
`TELEPORTATION_PORTAL`; all 12 object-`4408` Guthix rows remain `TRANSPORT` / `LEGACY_LOCKED`.
Unknown object/action/name/display shapes also remain unsupported.

## 54. Staged widget transports must model their parent tab and loading state

A transport widget can be unavailable because its parent tab is closed, visible but still loading,
or populated without the requested entry. Treating all three states as the same click-ready state can
toggle the interface closed on every retry or make a valid row look permanently unavailable. The
grouping/minigame interface is the concrete case: opening the grouping tab, opening the grouping
panel, opening the activity dropdown, waiting for its dynamic children, selecting the activity, and
teleporting are separate observable stages.

**Pattern to follow:** Open the parent tab through a non-blocking client-thread action, then advance
one immutable stage observation at a time. Use the dropdown arrow sprite to distinguish open from
closed; an open dropdown with no dynamic children is a pending `WAIT` state, while a populated list
without the requested activity is unavailable. Match catalog and widget labels with normalized case,
whitespace, and apostrophes so `Giant's Foundry` and `Giants' Foundry` identify the same activity.
After the teleport input, retain the exact route edge through cooldown-driven catalog disappearance
until its directed landing is observed.

**Defensive check:** All 20 active packaged minigame rows classify as engine-owned, while the
commented broken Keldagrim Rat Pits row is excluded. The three active Rat Pits rows model their
destination option after teleport input. A loading dropdown never receives a second toggle click,
and cooldown removal of the source row cannot return ownership to legacy orchestration before the
landing.

## 55. Permutation transport catalogs need runtime-edge and unlock-state checks

A compact network catalog can store several origin rows and destination-only rows, then expand them
into a larger directed graph at load time. Testing only source-line count misses duplicate, omitted,
or reversed runtime edges. Magic Mushtrees are the concrete case: two origin tiles at each of four
locations plus four destination-only rows produce 24 directed runtime edges, not 12 executable
routes.

**Pattern to follow:** Assert the exact expanded edge count and classify every generated directed
edge. Resolve the live object through `Microbot.getRs2TileObjectCache()` with exact catalog id,
name, action, plane, and origin proximity. Treat opening the group-scoped interface and selecting a
normalized destination label as separate immutable stages. Strip only the displayed numeric prefix;
do not search unrelated widgets outside the known interface group. Once destination input is issued,
retain the edge while the source and menu unload and acknowledge only the directed catalog landing.

**Defensive check:** A visible network menu that labels the requested slot `Not yet found` is
unavailable, not pending or click-ready. It receives no destination input, session-disables every
directed edge into or out of that network node (including exact legacy shadow rows), invalidates the
transport snapshot, and requests a bounded replan. Test both that locked-node replan and terminal
source/menu disappearance, because neither interface closure nor raw-route progress proves remote
arrival.

## 56. Permutation transport costs belong to the destination half

A compact network can place the live object interaction on origin rows and its per-trip item cost
on destination-only rows. If the destination column uses an unrecognized resource header, the
loader still expands a plausible-looking graph but every generated edge silently loses its item
requirement. Hot-air balloons exposed this failure: the loader recognizes `Item IDs`, not `Items`,
and each destination consumes a specific log.

**Pattern to follow:** Encode destination requirements under the parser's exact header and mark
single-use costs as consumable. Include the transport family in the same shared bank-planning
predicate used by both route analysis and withdrawal collection. Generated origin-to-destination
edges then inherit the destination log, repeated trips sum that log, and a banked route cannot start
its final leg without the required inventory update.

**Defensive check:** Assert all 225 generated balloon edges carry exactly one of the six destination
log ids, are consumable, and participate in bank planning. A two-use route to the same destination
must withdraw two logs, while malformed or requirement-free balloon rows remain engine-unsupported.

## 57. A direct boundary object can still have an optional warning stage

An object action that normally animates directly across a boundary may instead open a warning for
accounts that have not disabled it. Wilderness ditches are the concrete case: the catalog exposes
an exact `Cross;Wilderness Ditch;23271` input, while warning widget `475:11` can appear before the
crossing. A generic direct-transition handler bypasses the legacy process-loop warning branch and
can redispatch the object or stall without ever confirming the interface.

**Pattern to follow:** Give warning-capable boundaries a dedicated route kind. Publish immutable
object and warning observations, issue at most one input per stage, retain the directed edge while
the object or interface disappears, and clear only at the catalog landing. Match the complete
catalog identity and geometry instead of broadening every `Cross` action.

**Defensive check:** Every packaged ditch row classifies into the dedicated kind, an unrelated
bridge remains legacy-owned, warning appearance advances the pending action exactly once, and both
warning and warning-free flows wait for the directed landing without legacy handoff.

## 58. Exact direct-action families still need a complete identity boundary

An action/name pair can look like a generic one-click scene transition while still belonging to a
specific obstacle protocol. Dense forest crossings are the concrete case: all current rows use
`Enter;Dense forest`, but the catalog contains five object IDs and unrelated `Enter` objects have
different dialogue, requirement, or landing behaviour.

**Pattern to follow:** Before reusing the catalog-transition lifecycle, audit every generated row
for item and fare requirements, directed-input ambiguity, geometry, and failure behaviour. Pin the
policy to the exact normalized action, name, and complete current object-ID set. Resolve through
`Microbot.getRs2TileObjectCache()`, let the navigation engine own the bounded command, and acknowledge
only the directed catalog landing.

**Defensive check:** All 46 packaged dense-forest rows and only object IDs `3937`, `3938`, `3939`,
`3998`, and `3999` publish as `CATALOG_TRANSITION`. A neighboring ID, an unrelated `Enter` target,
or an item/fare requirement stays `TRANSPORT` / `LEGACY_LOCKED`; the production input audit must
also remain free of ambiguous destinations.

## 59. Transport requirements can depend on completed quest state

A catalog action can encode a fare that is not present in the structured currency column, and the
same interaction can become free after a quest. Port Phasmatys energy barriers are the concrete
case: `Pay-toll(2-Ecto)` costs two ecto-tokens before `Ghosts Ahoy` and consumes none afterward.
Treating every matching row as always paid over-withdraws after the quest; treating the blank
currency column as free lets pre-quest routes strand at the barrier.

**Pattern to follow:** Resolve one effective currency name and amount from the exact transport
identity plus the cached quest state, failing closed when the state is unavailable. Use that same
effective requirement in pathfinder eligibility, transport-cache relevance, bank-planning
inclusion, inventory checks, and withdrawal aggregation. Do not mutate the shared `Transport` row,
because its structured fare remains the catalog source and other consumers may distinguish encoded
from conditional requirements.

**Defensive check:** Before and during `Ghosts Ahoy`, every exact object-`16105`
`Pay-toll(2-Ecto);Energy Barrier` row resolves to two ecto-tokens and participates in bank planning.
After quest completion it resolves to zero. Unrelated encoded coin and ecto-token fares retain their
original name and amount, and ambiguous barrier inputs remain legacy-owned until their directed
landing protocol is explicit.

## 60. Reuse a direct lifecycle only after proving the complete input is deterministic

A paid action does not automatically imply a dialogue or confirmation stage. Port Phasmatys energy
barriers expose the payment itself as the object action, `Pay-toll(2-Ecto)`, so adding an inferred
dialogue step would make the engine wait for an interface that does not belong to the protocol.
Conversely, identical object input at one origin can still map to multiple catalog destinations and
cannot safely promise either landing.

**Pattern to follow:** Establish the live menu action from client metadata and game guidance, then
group the catalog by origin, object id, normalized action, and name. Reuse the bounded direct
catalog-transition lifecycle only for groups with one directed destination. Keep fare resolution in
the shared requirement policy rather than the interaction executor, and retain ambiguous groups as
legacy-owned until their landing model is explicit.

**Defensive check:** The 16 exact object-`16105` energy-barrier rows split into 12 deterministic
`CATALOG_TRANSITION` rows and four `TRANSPORT` rows from two ambiguous origins. The rebuilt
production policy probe must report that same `12/4` split; live crossing remains deferred until an
account is safely positioned with the required quest/fare state.

## 61. Legacy ownership does not validate gameplay prerequisites

Keeping a transport legacy-owned prevents premature interaction migration, but does not stop the
pathfinder selecting an impossible edge. The three Dorgesh-Kaan `Open;Door;6919` entrance rows had
no quest metadata, so production eligibility admitted them before `Death to the Dorgeshuun` was
completed.

**Pattern to follow:** Audit catalog requirements separately from execution ownership. Encode a
verified unconditional quest-completion gate in the transport resource and test the parsed quest
state as well as the route classification. Do not infer an entrance gate for a one-way exit or
flatten a conditional staff, diary, key, or quest-stage protocol into the same static requirement.

**Defensive check:** All three ID 6919 entrance rows require `Death to the Dorgeshuun = FINISHED`
and remain `TRANSPORT` / legacy-owned. A rebuilt-client availability probe on an account without
that completion must reject all three; this negative check is not live crossing acceptance.

## 62. A shared transformed object ID does not imply a shared unlock

Different location-specific object definitions can transform into the same interactable object.
The three surface Catacombs entrances all expose `Hole/Enter` ID `28915` after unlocking, but
their base objects use independent varbits: `28919 -> 5088`, `28920 -> 5089`, `28921 -> 5090`.
Copying one transformed row's unlock to the other locations can publish invisible entrances or
hide an entrance that is actually unlocked.

**Pattern to follow:** Read the base object's varbit and transform array on the client thread.
Associate catalog requirements with the physical location and directed landing, not only the
shared transformed ID. Check every approach row, including older base-ID rows, against that
mapping; preserve exact name/action and bounded-origin matching in the live cache-backed resolver.

**Defensive check:** All four approach rows at each Catacombs entrance require its own unlock
value `1`. Independently enable each of the three unlocks in a regression and verify that only
the corresponding destination's rows pass; a read-only runtime audit must find zero discrepancies
between catalog requirements and base-object definitions.

## 63. Do not require an unlock on the action that creates it

An entrance and its return exit can have different prerequisites. Catacombs surface holes are
unavailable until their underground vines have been climbed, but the vines themselves must permit
first use. Copying the incoming hole's unlock condition onto the outgoing vine creates a circular
dependency and suppresses a valid exit; older rows also carried unrelated varbits.

**Pattern to follow:** Audit both directions independently. Preserve unlock conditions on incoming
entrances, omit them from unconditional unlocking exits, and keep the directed landing as the
transition acknowledgement. Reuse an existing engine lifecycle only for the exact verified
object/action family; a successful metadata check is not proof that a physical crossing or unlock
write occurred.

**Defensive check:** All 20 Catacombs `Climb-up;Vine` rows have no item, fare, quest, varbit, or
varplayer requirement. Their 12 surface `Enter;Hole` counterparts retain the location-specific
unlock tests, including rejection when only another entrance has been unlocked.

## 64. Check that landing tolerance excludes the starting side

A direct action and deterministic destination do not alone prove that an existing transition
scanner is suitable. Its arrival tolerance may already include the source position. The shared
catalog scanner previously accepted positions within two tiles of the destination, so a two-tile
same-plane Steps edge could look landed before any crossing.

**Pattern to follow:** Check directed geometry against the acknowledgement predicate before
migrating a family. Reject short same-plane rows when the existing predicate cannot distinguish
the sides; do not globally shrink caller arrival distances or treat object disappearance as proof.
A proper short-link migration needs a side-aware or progress-aware acknowledgement with tests at
the source, during approach, and beyond the destination.

**Defensive check:** The audited Steps policy rejects same-plane endpoints within two tiles even
for an allowed object ID, while accepting its verified cross-plane and distant-exit rows. Kurask
and Wyvern two-tile steps remain legacy-owned; eligibility is separate from landing correctness.

The shared scanner now also requires a position on or beyond the destination's perpendicular
boundary for same-plane links up to four tiles, where the two endpoint tolerance areas overlap.
It uses the full directed catalogue vector, not either axis alone or the raw-route/object tile.
Longer links and plane changes retain their two-tile landing tolerance. Catalogue interactions must
also remain pending in `NavigationEngine` until the scanner confirms landing: a nearest-route index
can advance while a three-tile link is still being approached. Test origin, midpoint, diagonal
source-side offsets, landing, bounded overshoot, source disappearance, and raw-index advancement.

## 65. Capture the caller's arrival radius in the navigation request

The sidebar's default finish distance is not the distance passed to `walkWithState(target, distance)`.
The lifecycle adapter previously built every engine request from the sidebar value: a live radius-zero
request captured five and returned `ARRIVED` four tiles from its goal. Transport landing tolerance
is a separate predicate and must not replace the caller's final arrival contract either.

**Pattern to follow:** Pass the explicit radius through destination setup and capture it before
queuing asynchronous planning. Preserve the immutable request's radius for same-destination replans,
including cave-route comparisons. Reuse a cached route only when its active request matches both
destination and radius; a completed, cancelled, or differently sized request is not reusable.
Leave the HTTP endpoint's strict distance check intact rather than widening it to hide early arrival.

**Where this applies:** `Rs2Walker.walkWithStateInternal`, `Rs2WalkerLifecycleRuntime`, and
`NavigationRequest`. Default-only callers retain the configured default when starting a fresh request.

**Defensive check:** With sidebar distance five, explicit distances zero, three, and six must be
captured unchanged. Changing the sidebar during a replan must not change the active request, and an
exact-tile live request must complete only with zero remaining distance after all interactions retire.

## 66. A final raw index is not proof of exact endpoint arrival

After a teleport lands one tile off its catalogue coordinate, nearest-route progress already points
to the final raw index. Treating that index as route exhaustion immediately replans and can select
the same charged teleport again instead of walking the remaining tile.

**Pattern to follow:** After retiring the transport, allow a bounded approach to the existing raw
endpoint when that endpoint satisfies the caller's arrival contract and is on the player's plane
within normal click reach. Prefer the canvas input for this exact final approach, retain normal
command acknowledgement/recovery budgets, and acknowledge arrival from position rather than index.
If the endpoint itself falls outside the requested radius, retain partial-route replan behaviour.

**Where this applies:** `NavigationEngine` route-end handling and `Rs2Walker`'s engine action adapter.

**Defensive check:** An off-centre teleport landing followed by a radius-zero goal issues one ground
click and completes at the target without requesting another teleport. A nearby partial endpoint
outside the goal radius still requests a replan rather than receiving an endpoint-approach click.

## 67. Apply transport switches to ordinary rows of the same network

A transport's TSV type is not always its logical network. The catalogue includes twelve
`TRANSPORT` rows for Magic Mushtrees alongside the generated `MAGIC_MUSHTREE` rows. Checking
only the enum left six usable ordinary rows published on an account with mushtrees disabled.
Two ordinary `Use;Fairy ring` rows have the same bypass for `useFairyRings`.

**Pattern to follow:** Resolve these exact object/name/action identities to their logical feature
type before the shared feature-switch check. Keep their original route type and geometry intact.
Apply the same filter to the catalogue used by planning and live scene lookup, including after a
config refresh; do not create a separate executor-specific toggle or broadly gate unrelated objects.

**Where this applies:** `PathfinderConfig.isFeatureEnabled` and transport refresh publication.

**Defensive check:** Exercise all 22 boolean family switches in both states, plus all twelve ordinary
mushtree and two ordinary fairy-ring rows. With mushtrees disabled, the rebuilt live catalogue must
contain zero mushtree rows, including after a fresh walk and a cached transport refresh.

## 68. A Travel action is not proof of one-click arrival

The Fossil Island dock rowboat (`30914`, object tile `3723,3805,0`) opens a menu after `Travel`.
Its route to the Digsite must select `Row to the barge and travel to the Digsite.`, not the shorter
`Row to the barge.` option. A blank `Display info` caused the row to enter the direct NPC/object
executor, which had no dialogue stages and waited for a landing that could not happen.

**Pattern to follow:** Encode the observed full destination option in the route resource, so the
existing dialogue executor owns actor -> option -> continue -> directed landing. Verify the exact
option against competing prefixes and test the return direction independently; the Barge guard's
`Quick-Travel` return is still direct and should not be changed into a menu sequence.

**Where this applies:** `ships.tsv`, `NpcTransportPolicy`, `NpcDialogueTransportPolicy` and scenes.

**Defensive check:** The dock-to-Digsite row is dialogue-owned, never direct-owned; the two-option
menu selects index 1. Rebuilt live tests must finish at both exact endpoints without legacy input.

## 69. Missing rowboat destinations are unlock evidence, not fixed option numbers

The Fossil Island camp rowboat initially offers only the barge, barge-and-Digsite, and Cancel.
North-island and sea destinations are absent until unlocked through discovery trips. The legacy
handler presses the first character of labels such as `2. North of Island`; with the locked menu,
that number instead selects the Digsite. No verified rowboat unlock varbit was found in this audit.

**Pattern to follow:** Observe a complete camp menu on the client thread, anchored by both known
barge options and Cancel and proximity to the camp boat. Publish an immutable set of missing
destinations, invalidate transport snapshots when it changes, and exclude only camp's two outgoing
BOAT rows. Preserve the north/sea discovery directions and Digsite SHIP row. When a planned
destination is absent, explicitly select Cancel from that verified menu, then publish a distinct
unavailable stage so the engine drops the original voyage acknowledgement deadline and replans
immediately. Match the six island routes by unique destination text rather than catalogue ordinals.
Later observed menus replace the negative evidence, and the login screen clears it so it cannot
leak between accounts. Before the first observation, state is unknown: this is a session-learned
gate, not a pre-arrival unlock-varbit check.

**Where this applies:** `PathfinderConfig`, `ShortestPathPlugin.onGameTick`, and NPC dialogue scenes.

**Defensive check:** A locked live menu removes both outgoing camp BOAT rows from publication while
the Digsite route remains usable. Foreign/incomplete/remote menus cannot alter the gate; synthetic
later-unlocked menus restore destinations. A pending `Travel` followed by the definitive Cancel and
unavailable stages must request a replan within the next observations rather than after the 60-second
voyage deadline. Verify actual unlocked travel separately.

## 70. Model intermediate Talk-to choices as explicit stages

A destination label in a transport row does not mean the first dialogue menu contains that
destination. Cabin Boy Herbert first asks whether the player wants travel at all; only after selecting
`Can you take me somewhere?` does the `Travel to <destination>.` menu appear. Treating every
`Talk-to` route as continue-then-destination either stalls on the first menu or invites broad matching
that can choose unrelated conversation branches.

**Pattern to follow:** Keep the general `Talk-to` exclusion. Admit only an exact audited actor/route
contract, represent each intermediate intent option as its own engine stage, and match that prompt
exactly and uniquely before input. Continue frames may be optional, but a foreign or ambiguous menu
must receive no input and remain under the bounded interaction deadline.

**Where this applies:** `NpcDialogueTransportPolicy`, `Rs2NpcDialogueTransportScene`, its route
scanner, and any later specialised NPC/boat conversation migration.

**Defensive check:** All four packaged Herbert rows publish as dialogue-owned; malformed Herbert and
other `Talk-to` rows remain legacy-owned. The exact request advances to the destination stage, while
partial or duplicate request labels are rejected.

## 71. Replan when a failed short transition moves behind its command position

Fallible short crossings can send the player backward while their next object remains visible and
clickable. Retaining that pending edge makes ranged dispatch retry an obstacle that the player can no
longer reach without traversing earlier edges again.

**Pattern to follow:** Record the player position when issuing a short same-plane catalog transition.
While its command is pending, project both that position and the current player position onto the
directed crossing vector. If the player moved materially farther backward, clear the pending command
and replan immediately from the observed landing; do not wait for the interaction deadline or click
the stale edge again. Compare against the command position rather than requiring dispatch at the
catalog origin, because valid ranged interaction can begin several tiles before the obstacle.

**Where this applies:** `NavigationEngine` command acknowledgement for fallible
`CATALOG_TRANSITION` edges such as the Fremennik basalt causeway.

**Defensive check:** Model a command issued at `2522,3597` for the
`2522,3600 -> 2522,3602` edge followed by a failed landing at `2522,3595`. The next decision must be
`REQUEST_REPLAN` with `interaction-displaced-behind-origin`, and the interaction adapter must have
received exactly one command.
