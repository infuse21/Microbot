# Walker incident handoff: door ownership, oscillation, and replans

**Date:** 2026-08-15

**Observed revision:** `f19e663d28` (`PluginTesting`)

**Status:** Implemented in the working tree; focused automated validation passes. The two live
acceptance routes below still need three in-game runs each before the incident is closed.

## Implementation status

The repair now:

- gives a fresh remembered door edge a typed, exclusive decision (`approach`, `handle at edge`,
  `action in flight`, `crossed`, or `expired/invalid`) before generic recovery and route clicks;
- exempts the exact door edge and its first landing step from live-collision route replacement while
  the door claim is fresh;
- normalizes nested sealed rims in the pathfinder and permits only one effective-goal retarget while
  retaining the original requested goal in walk-session state;
- excludes partial and `SEARCH_EXHAUSTED` routes from bank-versus-direct distance comparison instead
  of comparing `Integer.MAX_VALUE`;
- verifies a false login sample over a bounded grace window before clearing an active route.

Regression coverage was added for the `1770,3589 -> 1770,3590` oscillation edge, traversal-envelope
geometry, the real `2907,3539` nested sealed target, typed route-distance comparison, session reset,
and transient logout decisions. `apiDocsCheck`, compilation, the focused walker tests, the process
architecture guard, and the client-thread guardrail pass. A broad 1,843-test run produced one
randomized `RouteClickTargetRegressionTest` failure; both cases passed immediately when that class
was rerun alone, so it is recorded as an unrelated pre-existing flaky test rather than hidden. A
later clean broad rerun reached the final test groups without an assertion failure, but its Gradle
daemon disappeared before it could write a complete summary.

## Objective

Make a planned door edge an exclusive, short-lived execution transaction:

1. approach the near side;
2. open the door once;
3. cross the edge;
4. resume the same route.

While that transaction is active, minimap fallback, local recovery, live-collision validation, and
stall recalculation must not replace it or click a destination beyond the door. Also remove the
avoidable sealed-target replan chain and do not abort a healthy walk on a single transient
`isLoggedIn()` sample.

This is an incident-driven repair, not permission to rewrite the walker or flatten the door cascade.
Read `docs/entity-guides/movement.md`, especially entries 2-5, 8, 10-16, 19, 22, 25, 28, and 29,
before changing code.

## Executive diagnosis

The log contains several independent problems which must not be treated as one generic "recalc"
bug:

1. **Door ownership is advisory rather than exclusive.** The walker identifies a walled edge as a
   scene door and says "the door pipeline owns it", but other branches can still replan or issue
   fallback/recovery clicks. When already beside the remembered edge,
   `tryWalledDoorApproachClick` deliberately declines, but the caller does not have a typed outcome
   meaning "already at door: dispatch/yield to door handler".
2. **Live route validation can replan during a door transaction.** It exempts the exact active door
   edge, but the reproduction invalidated the next adjacent/diagonal path step while the door was
   opening and replaced the active route anyway.
3. **Tail recovery loses the actual blocker.** Near the second destination the log repeatedly
   identifies edge `1770,3589 -> 1770,3590` as door-owned, yet no corresponding door interaction
   follows. Route-backed recovery and sticky interim clicks then move the player east and west around
   the same wall.
4. **Sealed-target handling chains target mutations.** The requested goal `2907,3539` retargets to
   `2907,3538`, which is probed as sealed and retargets again to `2907,3537`. This is much better than
   the old partial crawl, but it still causes cancelled searches, multiple snapshots, confusing goal
   identity, and `Integer.MAX_VALUE` route distances in banking comparison.
5. **A transient login predicate aborts a moving walk.** At 14:06:54 the walker exits as
   `not-logged-in` while the player is still moving, then the script immediately starts a successful
   retry. There is no actual logout sequence in the supplied log.

Transport refreshes, the fairy-ring execution, and the first two observed door opens work. Preserve
them. The recurring `Failed to load exit portal config` message and large `collision_conflict`
summaries are diagnostic noise unless a focused reproduction proves they change the route.

## Reproduction A: sealed goal and final door

Requested route:

- start: `2963,3378,0`
- requested goal: `2907,3539,0`
- time window: 14:06:16-14:07:35 BST

### What worked

- The route advanced normally through open terrain.
- Gate `2936,3451 -> 2935,3451` was found, opened, crossed, and followed by a forward route click.
- Door `2907,3544 -> 2907,3543` was eventually found and opened.
- The retry arrived within the configured distance at 14:07:35.

### What failed

1. Direct and bank-leg searches returned `SEARCH_EXHAUSTED` with a useful partial path but reported
   `2147483647 tiles`. The comparison therefore produced `direct_only_bank_unavailable` from a
   sentinel rather than a typed route termination.
2. Startup retargeted three goal coordinates in under one second:

   ```text
   2907,3539 (sealed) -> 2907,3538 (sealed) -> 2907,3537 (reached)
   ```

   The first retarget search was cancelled and restarted before the next retarget completed.
3. After the first successful gate, `processWalk` emitted `not-logged-in`, cleared the route, and
   forced `ShortestPathScript` auto-retry even though movement continued.
4. At the final door, click selection correctly refused a walled target and recorded:

   ```text
   route_click_walled | to=2907,3540 ...
   walled_edge_not_learned | 2907,3544 -> 2907,3543 | scene door on the edge
   recovery_target_walled ... replanning
   ```

   The player was already adjacent to `walledDoorEdgeFrom`, so `tryWalledDoorApproachClick` returned
   false and the old replan path ran. The next pass found the door, but while it was being handled the
   live validator logged:

   ```text
   route step 2907,3543 -> 2906,3542 now blocked; recalculating
   ```

   That is not the exact claimed door edge, but it is inside the same in-progress door traversal.

## Reproduction B: door ignored, then oscillation

Requested route:

- start: `2750,3477,0`
- requested goal: `1757,3595,0`
- fairy-ring handoff: `2705,3576,0` to `1826,3540,0`
- failure window: 14:09:32-14:10:16 BST

### What worked

- The direct route was selected correctly over the bank route (`211` versus `240` tiles).
- Fairy ring `AKR` was selected and arrived at the expected destination.
- Door `1777,3590 -> 1776,3590` was found, opened, and crossed.

### What failed

1. The door was first detected at range but interaction was deferred because the player was moving.
   The pass then spent about two seconds in the recovery gate before declaring the captured position
   stale. This is safe but slow and shows there is no durable pending-door transaction across passes.
2. After crossing, route clicks toward `1759,3594` and `1757,3595` were refused. The walker reported
   a different door-owned edge, `1770,3589 -> 1770,3590`, but did not open it.
3. The fallback path then visibly oscillated:

   ```text
   1770,3589 -> 1773,3588 -> 1780,3593 -> 1777,3596
   -> 1780,3595 -> 1771,3588 -> 1770,3589
   -> 1773,3588 -> 1780,3591 -> 1778,3589 ...
   ```

4. `route-backed local recovery click` targeted `1762,3593`, but the server path moved the player
   away from it. The sticky interim was later cleared as `moving-away`; subsequent route clicks again
   targeted the near side instead of resolving the door edge.
5. The user stopped the walk with Ctrl+X. Interrupt handling itself worked.

## Relevant code and current mismatch

### Door claim and walled-click recovery

- `Rs2WalkerMovement.learnWalledRouteEdge`
  - correctly refuses to learn scene-door edges as permanent walls;
  - records the edge through `rememberWalledDoorEdge`;
  - its log says the door pipeline owns the edge, but it does not enforce ownership.
- `Rs2Walker.tryWalledDoorApproachClick`
  - only returns a boolean;
  - returns false when already within one tile of the edge;
  - the caller cannot distinguish "already at the door" from stale, unreachable, moving, or click
    failure, and may replan.
- The unreachable-frontier branch in `Rs2Walker.processWalk`
  - owns the ordered door/transport/recovery cascade;
  - checks `recovery_position_stale` only after potentially blocking door and transport work;
  - can fall through from a known door-bearing walled edge into generic recovery.
- `DoorAttemptLedger`
  - already provides the natural identity and time window for an exclusive door transaction;
  - extend its semantics or add a small typed policy around it instead of creating another unrelated
    timestamp.

### Live collision recalculation

- `ShortestPathPlugin.validateRouteAgainstLiveCollision`
  - skips catalog transports and `Rs2Walker.isActiveDoorEdge(a, b)`;
  - only the exact edge matches the active `DoorAttemptLedger.Attempt`;
  - the log proves an adjacent step was invalidated during the active traversal.
- `LiveRouteValidator.firstBlockedStep`
  - is pure and already tested in `LiveCollisionTest`;
  - add a scenario that models a claimed door edge plus a temporarily blocked adjacent/diagonal
    landing step.

### Sealed target

- `Pathfinder.sealedTargetSubstitutes`
  - proves the requested tile sealed and computes up to eight rim candidates;
  - ranks candidates by distance from the search start.
- `Pathfinder.getNearestSealedRimSubstitute`
  - exposes only the first candidate even when it was not reached.
- `Rs2Walker.consumeSealedRimRetarget`
  - mutates `currentTarget` and permits up to three chained retargets;
  - conflates the caller's requested goal with the effective walkable destination.

### Login exit

- `Rs2Walker.processWalk` immediately clears the route on one `!Microbot.isLoggedIn()` read.
- `ShortestPathScript` treats that as a generic `EXIT` and auto-retries up to three times.
- Before changing this, capture the client game state and local-player presence on the same client
  thread. Do not merely add a sleep.

## Required implementation order

### P0: make door ownership a typed decision

Replace the boolean result of the remembered-walled-door recovery with an enum or small result type,
for example:

```text
NO_CLAIM
APPROACH_CLICKED
AT_EDGE_DISPATCH_REQUIRED
ACTION_IN_FLIGHT
STALE_OR_INVALID
```

The exact names do not matter; the distinction does. `AT_EDGE_DISPATCH_REQUIRED` must either invoke
the route-edge door handler immediately or end the pass with a door-specific yield that guarantees
the next pass starts at door dispatch. It must never become `REPLAN_WALLED` or a generic recovery
click for the same fresh edge.

Keep the existing door cascade ordering. Extract only the decision needed to make this behavior
headless-testable.

### P0: suppress competing actions for the active door transaction

While a fresh door claim is approaching, opening, or crossing:

- no route checkpoint beyond the claimed edge;
- no local recovery click beyond or away from the edge;
- no stall replan for lack of movement during the bounded door wait;
- no live-collision replan for the claimed edge or its traversal/landing envelope;
- no second interaction unless the existing cooldown/strike policy explicitly permits it.

Prefer a bounded ownership predicate derived from `DoorAttemptLedger` and the claimed edge geometry.
Do not globally disable live collision. A conservative traversal envelope can cover the exact door
edge plus route steps sharing either endpoint/corner while the claim is active; write the geometry as
a pure function and test exact, reverse, adjacent, diagonal, unrelated, expired, and other-plane
cases.

### P0: pin the oscillation as a pure scenario

Add a regression row representing:

- player beside `1770,3589 -> 1770,3590`;
- route goal behind the edge;
- target rejected by local reachability;
- scene door found on the first walled raw edge;
- remembered claim still fresh.

The decision must be door dispatch/yield, never `REPLAN_WALLED`, `LOCAL_RECOVERY_CLICK`, or an
ordinary route click. Add it at the narrowest pure seam (`RouteRecovery`, `FrontierDecision`, or a
new door-claim policy), then add the route-level property to `WalkerRouteCorpusTest` if it can be
represented without a live scene.

### P1: stop chained sealed retargets

Keep two identities for the walk:

- `requestedGoal`: immutable caller intent, used for result/reporting and arrival tolerance;
- `effectiveGoal`: the reachable tile the planner is currently executing.

For a proven sealed goal, choose one effective rim destination from the original rim set and plan it
with the normal budget. Do not probe each selected rim as a new caller goal and walk the rim inward
one tile per retarget. If none of the bounded candidates can be reached, return an honest typed
partial/unreachable result; do not use `Integer.MAX_VALUE` as a comparable route length.

Bank comparison must branch on route termination before comparing distance. A partial route is not a
very long complete route.

### P1: debounce false logout without hiding real logout

Confirm why `Microbot.isLoggedIn()` returned false at 14:06:54. The fix should distinguish a real
logout/hop from one inconsistent background-thread sample. Candidate behavior is to verify a stable
non-logged-in game state on the client thread (or across game ticks) before clearing the route. Real
logout, world hop, shutdown, and Ctrl+X must still stop promptly.

### P2: reduce hidden pass latency and log noise

Only after correctness:

- account for the large `pass_slow residual` rather than adding more INFO lines;
- avoid repeating `Failed to load exit portal config` on every transport refresh if absence is an
  expected configuration state;
- keep healthy `collision_conflict` summaries throttled.

## Tests to add before the live rerun

1. `RouteRecoveryTest` or a new pure policy test:
   - fresh claimed door, player far enough away -> approach;
   - fresh claimed door, player beside edge -> dispatch/yield, not replan;
   - moving toward near side -> action-in-flight;
   - expired claim -> old recovery behavior;
   - unreachable near side -> explicit invalid outcome.
2. `LiveCollisionTest`:
   - exact active door edge is skipped;
   - reverse edge is skipped;
   - traversal-envelope step sharing the landing corner is skipped while active;
   - unrelated blocked step still causes recalculation;
   - expired claim no longer suppresses recalculation.
3. Sealed-target tests:
   - `2963,3378,0 -> 2907,3539,0` selects one effective rim destination;
   - no chained `3539 -> 3538 -> 3537` mutation;
   - requested goal remains available in the result;
   - bank comparison rejects partial termination rather than comparing `Integer.MAX_VALUE`.
4. Walk-session reset tests:
   - all new door ownership and effective-goal state resets between walks.
5. Login decision test:
   - one transient false sample does not clear an otherwise active route;
   - stable logout does clear it;
   - interruption still wins immediately.

Run at minimum:

```text
./gradlew :client:runUnitTests
./gradlew :client:compileJava
```

If public signatures change, also run `./gradlew apiDocs` and verify `./gradlew apiDocsCheck`.

## Live acceptance runs

Use `./microbot-cli` and the protocol in `docs/AGENTIC_TESTING_LOOP.md`. Preserve the full client log
and record the exact revision.

### Run A

Walk from the Falador-area start to requested goal `2907,3539,0`.

Pass criteria:

- no more than one sealed-goal retarget;
- requested and effective goals are both visible in the diagnostic trail;
- no `2147483647` route distance participates in bank comparison;
- each encountered door receives at most one initial Open interaction;
- no live-collision recalc during an active door transaction;
- arrival succeeds without script auto-retry.

### Run B

Walk from the Camelot-area start through fairy ring `AKR` to `1757,3595,0`.

Pass criteria:

- fairy-ring behavior remains unchanged;
- door `1777,3590 -> 1776,3590` opens and the route continues forward;
- if edge `1770,3589 -> 1770,3590` is still on the selected route, it is dispatched as a door or
  rejected as a false door classification with a specific reason;
- no repeated east/west movement between x=`1770..1780` after the player first reaches x=`1770`;
- no repeated `route_click_walled` for the same fresh door edge;
- the walk reaches the requested tolerance without Ctrl+X.

Run each route at least three times. One success is insufficient because pathfinder tie-breaking is
deliberately randomized.

## Guardrails: do not "fix" this by

- increasing sleeps, retry counts, handler range, or stall budgets;
- globally disabling live collision or its route validator;
- treating a closed door edge as a learned permanent wall;
- allowing fallback minimap clicks past an unresolved door;
- changing `Rs2Player.isMoving()` globally;
- keying behavior only by door name (`Door`/`Gate`); edge geometry and action matter;
- deleting the sealed fast path and restoring whole-world exhaustive search;
- accepting arrival merely because one run eventually reaches the destination;
- weakening the process-walk architecture guard or API docs check.

## Definition of done

This incident is closed only when the focused tests, full unit suite, compile, and three live runs of
both routes pass. The final change should add any newly learned movement invariant to
`docs/entity-guides/movement.md` and append evidence under `docs/evidence/walker/<date>/` rather than
embedding a large live log in a high-level README.
