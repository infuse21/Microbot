# Walker Fix Plan — 2026-08-10

_Companion to [`walker-audit-2026-08-10.md`](walker-audit-2026-08-10.md). Fixes the five defects it
found, and closes the loop that keeps producing them._

## Goal

Stop the walker producing new failure modes every time an old one is fixed.

The audit's five defects are worth fixing on their own, but the reason they existed is that
`processWalk` has no test seam, so each fix is a guard added blind and verified by walking around
in-game. This plan fixes the defects **in an order that builds the seam as a side effect**, so the
sixth defect is caught by a test instead of by the user.

## Non-goals

- **No from-scratch rewrite.** 13.7k lines of load-bearing walker; the previous audit already
  rejected big-bang and it was right.
- **No door-cascade unification.** Assessed twice as net-negative (`walker-migration-harness.md`):
  the cascade's complexity is essential scenario diversity, not accident. Leave it.
- **No more leaf-helper extraction.** That well is dry — the decision layer is already decomposed and
  tested. The missing seam is for the *loop*.
- **No new guards inside `processWalk`.** If a fix needs one, it belongs behind a phase-A/B seam.

## Rules this plan follows

Earned the hard way in this repo; each has a scar behind it.

1. **Refactor and fix are separate commits.** A refactor must be provably inert before the behaviour
   change lands on top of it.
2. **Throttle actions, never gate correctness checks on a throttle.** (The Port Sarim wall-click bug.)
3. **Every walker incident gets a corpus row** in `WalkerRouteCorpusTest`.
4. **Preserve log wire strings.** Live debugging here is log-driven; renaming an exit reason blinds
   the only diagnostic that works.
5. **Run the FULL suite, not a filtered one** — filtered walker runs miss ordering interactions.
6. **Regenerate the client-thread guardrail baseline deliberately**, diffing +/- with lambda indices
   stripped, never blind.

---

## Phase A — Type the control flow, fix termination

Findings #1 (`isRouteProgressExit` under-covers) and #4 (tail budget is not a bound).
**All headless-verifiable. No live walk needed for A1.**

### A1. `WalkExit` enum — provably inert

Replace `String exitReason` with an enum in `util/walker/state/WalkExit.java`.

```
enum WalkExit {
    END_OF_PATH("end-of-path", …),
    INTERIM_IN_FLIGHT("interim-in-flight", …),
    …
    ;
    String wireName();        // EXACT existing string — logs must not change
    boolean isProgress();     // today's isRouteProgressExit
    boolean isTailExempt();   // today's :3401-3405 list
    boolean isDoorLike();     // today's shouldCanvasNudgeAfterDoorLikeExit
}
```

- **42 constants**, from the 47 assignment sites at `:2156`–`:3299`. Note `door-edge-waiting-retry`
  (`:2568`) is produced inside a ternary and does **not** appear in a grep for `exitReason = "…"` —
  enumerate from the audit's list, not from a fresh grep, or you will miss it.
- `off-path-deferred:<reason>` becomes `OFF_PATH_DEFERRED` plus a separate local `String
  offPathDeferDetail`; `wireName()` for logging is `"off-path-deferred:" + detail`.
- Keep the three legacy `String` predicates **unchanged and package-private** for one commit.

**Verification (this is the point of the step):** `WalkExitTest` asserts, for every constant, that
`isProgress()/isTailExempt()/isDoorLike()` equal the legacy predicates evaluated on `wireName()`.
Green = the refactor changed nothing. This is a characterization test, and it is what makes A2 safe.

**Risk:** near zero. **Rollback:** single commit revert.

### A2. Fix the classification

Now a one-line change per constant, visible in review, each with a comment saying why.

Reclassify as progress (`isProgress() == true`): `transport-handled-local-reachability`,
`frontier-obstacle-handled`, `local-recovery-click`, `door-suppressed-approach-click`,
`recent-door-edge-nudge`, `door-edge-resolved-fast-click`, `door-edge-resolved-after-wait`,
`door-edge-resolved-after-nearby-wait`, `route-move-in-flight`, `route-fold-continuation-pending`,
and the three in-flight yields `door-settling-yield`, `door-traversal-pending-yield`,
`transport-settling-yield`.

Update `WalkExitTest` deliberately — the diff to that test **is** the record of what behaviour
changed, which is exactly what the old `String` version could never give you.

Then delete the legacy `String` predicates and the `startsWith("door-handled")` prefix rule. That
prefix is the trap that hid half of these; it must not survive.

**Verification:** headless. Plus one live walk on a known partial route (Tempoross cove is already a
corpus pin) confirming it no longer reports `UNREACHABLE` while advancing.

**Effort:** A1+A2 ≈ half a day. **This is the highest value-per-risk work in the plan — do it first.**

### A3. Extract the epilogue as a pure decision

Lines `:3236`–`:3415` (partial-retry accounting, off-path wait sizing, tail exemption) become
`TailDecision.decide(WalkExit exit, TailState) -> TailAction {CONTINUE, CONTINUE_EXEMPT, REPLAN_RETRY,
UNREACHABLE, ARRIVED, WAIT_OFF_PATH}` in `util/walker/recovery/`. Pure, fully injected, no statics.

Fold the **wall-clock budget** (finding #4) in here, since this is the only place that decides
whether the loop goes round again:

- add `walkStartedAtMs` + `WALK_WALL_CLOCK_BUDGET_MS` (generous — 5 min — this catches livelocks,
  not slow walks) and a **separate cap on consecutive exempt iterations** (~24), so
  `processWalkTail--` can no longer produce an unbounded loop;
- **ship it WARN-only for one iteration of live testing** (log `walk_budget_exceeded`, don't act),
  then enforce. A budget that aborts a working long walk is worse than the livelock.

**Verification:** `TailDecisionTest`, decision table. Pin as rows: the partial-route door case from
A2, a permanently-exempt interim loop, and a genuine unreachable.

---

## Phase B — One consistent world per iteration

Findings #2 (stale reachability drives recovery) and #5 (post-transport leak). **Needs live walks.**

### B1. Stop the post-transport window leaking across walks

Small and independent — land it early.

- `markWalkSessionStart` (`:395-397`): call `clearRecentTransportContext()` instead of nulling only
  the three location fields. The timestamp `lastTransportHandledAtMs` is what every window check
  actually reads.
- Clear it on the exits that currently don't: exception (`:3436`), tail-exceeded (`:3449`), and the
  `walkCancelledDiag` returns.
- Delete `lastTransportHandledAtLocation` (finding #6, write-only).

**Verification:** unit test on the session-start/exit paths asserting the timestamp is zero.
Live: take a staircase, interrupt the walk, immediately start a second walk — the second must not
log `post_transport_segment_handler_skip` / `post_transport_raw_scene_scan_skip`.

**Risk:** low, but it *re-enables* handlers that were being skipped, so the second walk now does more
work than before. That is the intent; watch for door double-handling in the first live pass.

> **B2 slice 1 DONE** — `PluginTesting` b25e436043, `walker-fix` 154569dec9. This is the *behaviour*
> half: the reachable set is now recaptured whenever the player is no longer standing where it was
> built, by reading the `reachableTilesCacheOrigin` that had been declared, assigned twice and read
> never. Two bugs fell out of the recapture that already existed — it used a **smaller** radius than
> the original capture (18 vs 39), so it could manufacture "unreachable" for a tile the wider map had
> already reached; and it was gated on tile *proximity* rather than on anything having *changed*, so
> it rebuilt when nothing had moved and stayed stale when everything had. Exactly inverted.
>
> **The plan's other suggestion here was wrong** and is not done: moving `recovery_position_stale` to
> the top of the branch would defeat it. It does not duplicate the recapture — it covers a different
> window, the seconds the door cascade itself spends between the verdict and the recovery click.
>
> **The structural half below — one `WalkTick` threaded through the segment, frontier and
> click-selection reads — remains open.**

### B2. `WalkTick` — capture the world once

`WalkLoopSnapshot` (`:466-482`) already exists and already captures `playerLoc` +
`closestReachableTiles`. Grow it rather than inventing a new type:

```
WalkTick {  // captured once per tail iteration
  WorldPoint playerLoc; int plane;
  Map<WorldPoint,Integer> reachable;     // was reachableTilesCache
  List<WorldPoint> path, rawPath; int[] smoothedToRaw; int indexOfStartPoint;
  boolean nearPath, moving, doorSettling, transportSettling, recoveryInFlight,
          postTransportWindow, partialPath, inInstance;
  long capturedAtMs;
}
```

Migrate in **slices**, one commit each, so a regression bisects to a small diff:

1. the segment-loop gating reads (`:2337`–`:2471`),
2. the unreachable-frontier reads (`:2474`–`:2882`),
3. the click-selection reads (`:2896`–`:3210`).

Two behaviour changes ride along, and they are the finding-#2 fix:

- **Invalidate on player-tile change.** `reachableTilesCacheOrigin` is already assigned at `:2252`
  and `:2481` and never read — wire it: when the live player tile differs from the tick's origin, the
  tick is stale. Re-capture rather than deciding on it.
- **Move the `recovery_position_stale` check to the TOP** of the unreachable branch (`:2489`),
  before the door cascade at `:2562`–`:2710`. Today it sits at `:2733`, downstream of everything it
  is supposed to protect.

**Verification:** headless for the tick construction; **live walk required** for each slice — a door
route, a multi-transport route, and an MLM rockfall (the standing three).

**Risk:** highest in the plan. Mitigation: slice it, one live walk per slice, revert per slice.

---

## Phase C — Fix the movement sensor

Finding #3. **Needs live walks.**

> **C1 DONE** — `PluginTesting` 29c93dfafb, `walker-fix` a1659793d4. Implemented as a tile-change
> *recency* window rather than the per-sample position diff sketched below: a walking step is ~600ms
> and the check samples faster, so demanding a delta every sample would declare every healthy walk
> stalled. The pose flag is now credited only when a real tile change happened within 2.5s — several
> steps of slack, no help at all to a player who is only rotating. Tracked on its own
> `lastTileChangeAtMs` because `lastMovedTimeMs` is deliberately refreshed elsewhere to buy grace and
> therefore cannot answer "is the player really covering ground". **C2 remains open** and should wait
> for live logs showing C1 behaving.

### C1. Walker-local `isPlayerAdvancing()`

**Do not change `Rs2Player.isMoving()`** — 65 walker call sites and every other plugin depend on
today's pose semantics, and changing it globally is an unrelated blast radius.

Add, in the walker only, a position-diff sensor: player tile changed within the last N ms, OR pose
says moving AND the tile changed at some point in this window. Use it **only in stall accounting**
first (`checkIfStuck` `:11838-11847`), where the pose reading is actively wrong — a player wedged at
a door who keeps turning currently reads as "moving near path" and resets the stall clock forever.

> **C2 DONE** — `PluginTesting` b735b615b3, `walker-fix` 983ce8aaa2. The 12s post-click grace was
> **deleted** rather than shortened: after C1 it is pure residue, because `checkIfStuck` already
> refreshes the clock on every real tile change, so the blanket only ever bound the case where the
> player was *not* moving — precisely what the clock exists to measure. The interim multiplier is the
> same mistake in smaller print (walking toward an interim refreshes the clock; the
> stationary-with-interim case is rescued by the idle nudge in ~1–2s) and goes 1.75 → 1.25.
> **The 12s base stays**: the longest *legitimate* motionless stretch measured across four live runs
> is ~7.1s waiting out a transport handoff, and cutting the base buys a walker that interrupts its own
> ships. Budget is now **12s plain / 15s with an interim live / 24s worst case**, down from 36s, and
> pinned in wall-clock seconds so it cannot drift silently. `processWalk` 1630 → **1623**.

### C2. Re-tune the stall budget

Once C1 lands, the 12 s `MINIMAP_CLICK_STALL_GRACE_MS` blanket refresh at `:1998-2002` exists to
paper over the bad sensor. Reduce it, and re-check `stallThresholdMs()` multipliers. Target: worst
case from **~36 s down to ~15 s** before recovery engages.

**Verification:** live. Deliberately wedge the player (stand behind a closed door mid-route) and
confirm recovery engages inside the new budget. Add a corpus/telemetry row.

**Risk:** moderate — a too-aggressive stall clock causes premature replans, which is its own
pathology. Change one number at a time; C2 is the item most likely to need a second pass.

---

## Phase D — The seam that ends the whack-a-mole

Only worth doing after A–C prove the pattern. This is where the audit's P1 lands.

- ~~**D1. Segment gating policy.**~~ **DONE** — `PluginTesting` afa96cca60, `walker-fix` 819866a317.
  `segment/SegmentGate` owns the decision as one enum-returning function with a 12-case table. The two
  skip reasons and their precedence are pinned, the log strings live on the constants instead of a
  ternary, and `mayDispatchDoorAtRange` is named for the invariant it protects — a *skipped* segment
  was never examined, so it withdraws the right to click a door at range, which is exactly the
  coupling that produced the Falador U-turn out of two booleans that never appeared in the same
  expression. Behaviour-preserving. `processWalk` 1641 → **1630**.
- **D2. Frontier cascade.** `:2489`–`:2882` → pure `FrontierDecision`, interactions stay in
  `Rs2Walker` (the proven functional-core/imperative-shell split from
  `RouteRecovery.decideRecoveryClick`). ~10 ordered branches → one decision table. Seed it with
  **every pinned incident** already written up in `walker-migration-harness.md`: Clock Tower
  backtrack, Port Sarim cooldown wall-click, Wydin door poisoning, Falador U-turn, stepping stones.

After D, `processWalk` should be materially under the guard ceiling.

## Phase E — Make it stick

- Ratchet `MAX_LEGACY_PROCESS_WALK_LINES` **down** as lines leave, and treat it as a hard gate. It
  was raised 1628→1647 to accommodate growth; that must not happen again.
- Keep the corpus rule: every walker incident gets a `WalkerRouteCorpusTest` row.

---

## Sequencing and effort

| # | Item | Verify | Live walk? | Effort |
|---|---|---|---|---|
| ✅ A1 | `WalkExit` enum, inert | characterization test | no | done |
| ✅ A2 | Fix classification | headless ✓ / partial route pending | **yes — pending** | done |
| ✅ B1 | Transport-context clear | headless ✓ / interrupted walk pending | **yes — pending** | done |
| ✅ A3 | `TailDecision` + budget (observe-only) | decision table ✓ | observe logs | done |
| C1 | `isPlayerAdvancing()` in stall | live wedge test | yes (1) | 2h |
| C2 | Re-tune stall budget | live | yes (1) | 2h |
| B2 | `WalkTick`, 3 slices | headless + live per slice | yes (3) | 2d |
| D1 | Segment policy pure | decision table | yes (1) | 1d |
| D2 | Frontier cascade pure | decision table | yes (2) | 2–3d |
| E | Guard ratchet down | CI | no | — |

**A1 → A2 → B1 is the first slice**: half a day, kills the `UNREACHABLE`-while-walking bug and the
cross-walk suppression leak, and leaves a characterization test that makes everything after it safer.

### Progress log

- **A1 landed** — `PluginTesting` 48e1df11a0, `walker-fix` 85211d9622. Provably inert: the
  characterization test passes on both branches. The architecture guard caught the three lines it
  added and was ratcheted **down** 1647 → 1646 (that guard does not exist on `walker-fix`, so it was
  dropped from that cherry-pick rather than resurrected).
- **A2 landed** — `PluginTesting` 31dd7f7f37, `walker-fix` 4a5b49f562. Fourteen reasons
  reclassified; divergence from the old classification pinned in both directions.
  **Still owed: one live walk on a partial route** to confirm the `UNREACHABLE`-while-advancing
  report is gone.
- **B1 landed** — `PluginTesting` 52e3f9b73e, `walker-fix` 8c4f63fa63. Clearing at walk-session
  start turned out to be sufficient on its own: `walkWithStateInternal` is the only caller of
  `markWalkSessionStart` and the only route into `processWalk` (banked walks included), so the
  walk-ending paths did not each need their own clear and `processWalk` was not touched at all.
  Also deleted the write-only `lastTransportHandledAtLocation`, which removes a
  `Rs2Player.getWorldLocation()` client-thread hop from the transport handoff.
  **Still owed: one live walk** — take a staircase, interrupt the walk, start a second walk
  immediately, and confirm no `post_transport_segment_handler_skip` /
  `post_transport_raw_scene_scan_skip` in the new walk's log.
- **A3 landed** — `PluginTesting` 970e6ab320, `walker-fix` 7595dcac67. The epilogue's partial-retry
  accounting and tail exemption moved into a pure `TailDecision` with a 13-case decision table. The
  wall-clock budget and the consecutive-exempt-iteration cap ship **observe-only** (they log, they
  do not abort) — decide enforcement from live logs. `processWalk` 1646 → **1641**; guard ratcheted
  down again.

**Phase A is complete.** The full walker/route/door/pathfinder/collision suite is green on both
branches — the first clean full run of this effort.

### First live log (2026-08-10 farm run) — two corrections, one new finding

`PluginTesting` a4a6addfdb / `walker-fix` f8b90cf3eb. The run succeeded end to end; a walk arriving
is not evidence that it was right.

- **The walled-route net was learning blocked edges from the BFS frontier.** Its proximity guard is
  Chebyshev while the BFS budget counts *steps*, so a tile thirteen tiles away as the crow flies but
  thirty steps away around a building reads as walled. At the Port Sarim / Land's End docks a click
  to (2760,3238) was refused and the edge (2759,3230)→(2759,3231) learned — and nine seconds later
  the walker was standing on (2760,3238). Refusing the click is conservative and has fallbacks;
  writing it into the learned-blocked-edge store poisons routing for the session. An edge is now only
  convicted when its near end is strictly *inside* the frontier.
- **`MAX_CONSECUTIVE_EXEMPT_ITERATIONS = 24` was wrong.** A healthy Catherby→Ardougne leg yielded
  `interim-in-flight` **28 times in a row** while steadily covering ground — that is just what
  travelling between minimap clicks looks like. A bound on yields is a bound on walking. The run now
  resets whenever the player tile changes, so it bounds yielding *while stationary*. Shipping this
  observe-only is what made the mistake cost a log line rather than an aborted walk.

**Still-unexplained, worth watching:** ~4s of total log silence during walk startup after a
walled-edge replan, *including* the 1/second heartbeat. Per the heartbeat's own contract that means
the thread was blocked inside a wait, not spinning. The startup tmarks (`pf_wait_retry`, `pf_ready`,
`path_snapshot`) are deduped once-per-walk, so a walk that replans during startup goes blind exactly
when it is slowest. Removing the false convictions removes most occurrences; the blind spot remains.

**Not exercised by this log:** every route came back `TARGET_REACHED`, so no partial path and no A2
coverage; and no walk began inside a previous walk's 15s post-transport window, so no B1 coverage
either. Both still owe a live test.

### Second live log (2026-08-10 18:32, Ardougne) — the fixes hold, and a stile costs 20s

`PluginTesting` b9b22b370d / `walker-fix` f26c1496c5.

**Confirmed working:** not one `walled_edge_learned` line in the whole run, against three in the
earlier log — the frontier fix and the catalog-transport guard are both holding. And
`early_exit r=frontier-obstacle-handled` appears, so the A2 reclassification is live and firing.

**New defect: a moves-you obstacle was owned by the door cascade.** `"stile"` is a door-name
fragment, so a catalog transport at (2637,3350) with action `Climb-over` classified as door-like on
its NAME and went to the door handler — whose completion contract is "the blocked edge became
passable", which is unsatisfiable for something you climb over. It logged
`door_edge_post_unresolved` and the walk then spent **twenty seconds**: six refused route clicks, a
recovery click onto the far side of the fence, a stall, a replan and an idle nudge, before the
transport handler got the same object and crossed it in a single action.

The action now wins over the name: a catalog row whose action moves the player *across*
(Climb-over / Climb-through / Squeeze-through / Cross) is not door-like, so
`shouldDeferDoorHandlingToTransport` gives it to the handler that can complete it. Opening actions
are untouched.

This is the third obstacle of this class to need correcting — the Varrock museum guard barrier and
the Port Sarim back-room door were both fixed as individual data rows. Deciding on the action turns
a growing list of coordinates into a rule.

### Watch the logs for these two

Both are new and deliberately inert. If either appears on a healthy walk, the threshold is wrong and
should be raised before anyone considers enforcing it:

- `walk exceeded its 300000ms budget … probable livelock`
- `N consecutive tail-exempt iterations (exit=…) … yielding without advancing`

If one appears on a walk that really is stuck, that is the livelock finding #4 predicted, and the
`exit=` value names which yield is spinning.

### Found in passing: a false alarm, now defused

`RouteClickTargetRegressionTest > theHistoricDeviatingClickIsNotOnTheRawRoute` went red during this
work and was **wrongly reported here as a PluginTesting-specific route-data regression**. It is not.
It is a **starvation false positive**, and chasing it cost a round of investigation.

`calculationCutoffMillis` is a *no-progress* guard. Under CPU contention — a full-suite run, or the
client running alongside the build, which is the normal state of this machine — the search gets
starved and returns a best-effort **partial** path. A partial path wanders through tiles the test
requires to be absent, so it fails in exactly the shape of a real routing change. The apparent
"green on `walker-fix`, red on `PluginTesting`" split was an artifact: the clean-worktree runs were
isolated, the main-tree runs were not. A clean worktree at the *same* PluginTesting commit passes.

Fixed rather than documented-around: the cutoff goes 10s → 30s, and the route is now verified to
actually reach the goal before any content is asserted, with one retry and then an explicit
`pathfinder starved — INCONCLUSIVE, not a route regression` failure. A starved run can no longer
masquerade as a routing change.

The deeper flakiness — the pathfinder's per-node random tiebreaker varying equal-cost routes — is
untouched here; a hermetic rework of this test exists on another branch and should not be duplicated.

### D2 landed (2026-08-12) — `PluginTesting` 89c46b22e7, `walker-fix` 50945afac8

Five slices, each compiled and suite-green before the next. `FrontierDecision` now owns: the earliest
blocked route index, the frontier edge, what a door wait *means* once it returns (a six-value outcome
enum that carries its own exit and whether it ends the pass), the yield taken before any door action,
the recovery-index clamp, the step-back out of a hazard, the three-way target precedence, the exit for
a recovery click, and whether a tail scene click is worth trying. 41 rows, seeded from named incidents
— the Clock Tower rewind, the stepping-stone origin precedence, the fall-through wait, the hazard
asymmetry. `processWalk` 1623 → **1598**; the guard was ratcheted down after each slice.

Four things surfaced that reading the cascade had not: two `!gateDoorInteraction` guards that could
never be false at their call sites, a precedence that only worked because the raw-gated target
happened to be checked for hazards while the shortcut origin was not, an off-by-one in the clamp's
lower bound when the frontier sat at the route position, and a door-wait path with no exit assigned.

**What is still stateful and therefore still in the shell:** `findForwardReachableRecoveryIndex`, the
interim/sticky bookkeeping, and rejoin. Those read and write route state across iterations, so they
want B2's `WalkTick` snapshot first — extracting them ahead of it would just move the mutation.

### The two regressions, reverted (2026-08-12)

The short-walk fast path and the zoom-aware minimap stride are both gone from both branches. The fast
path broke `distance = 0` — the walker stopped wanting to end *on* the goal tile, which the user
caught in a live run before any test did; the ceiling for "short" is not the problem, the assumption
that a short walk needs no arrival check is. The zoom stride is reverted alongside it because it landed
in the same pair and its benefit was never measured. All five call sites are back on
`NORMAL_MINIMAP_REACH_EUCLIDEAN`.

### The Tithe Farm battery (2026-08-12 evening) — 8b49bdba02 and the follow-up

An agent-server test session produced four fixes in one commit (door strike-out, route stagnation
bound, tail dither, minecart menu 947), then two post-restart live walks validated and corrected them:

**Lovakengj → Varrock (healthy long walk).** Minecart selected its destination through the walker for
the first time ("via minecart menu", handoff expected), ship + gangplank + three ranged doors chained
clean, 2:22 end to end. It also exposed a margin problem in the new stagnation bound: the smoothed
progress index held ONE value (the final segment) through ~50s of honest walking against a 60s budget
— the whole west approach lives inside it, and it loops away from the path end before coming back, so
"closer to the next point" is not a fix either. The signal is now the player's furthest-yet RAW path
index (`stabilizeRouteProgressWithRawWatermark`), which advances tile by tile on that exact walk and
still refuses to advance for a two-tile ping-pong. Pinned in `RouteProgressWatermarkTest`.

**Varrock → Tithe lobby (the strike-out's first real encounter).** Three concluded attempts →
`door_strike_out` → honest sealed-goal answer in ~25s instead of the previous 4+ minutes. Two
interaction defects surfaced and were fixed:
1. **Withdrawal ordering.** The walk-scoped unlearn ran inside `markWalkSessionStart`, which follows
   `setTarget` — so a retry's plan ran against the previous walk's blocks, collapsed to a 1-tile path
   and burned the retry. The withdrawal now runs at the top of `walkWithStateInternal`, before any
   planning.
2. **The walled-net re-blocked the same edge for the SESSION.** `route_click_walled` learned the door
   edge one second after the strike-out had deliberately walk-scoped its own block — the museum
   lesson through the side door: the Tithe plugin's later seeded walk-in would find the door
   unroutable until restart. `learnWalledRouteEdge` now skips edges hosting a scene door (the same
   rule its catalog-transport guard already encoded: a shut door is not a wall).

### Sync note — hand-apply, do not cherry-pick

The scripted cherry-pick of these slices onto `walker-fix` produced a commit that *built* while having
silently dropped slice 2's test additions. The branches' `Rs2Walker` copies have diverged enough
(~400 lines, different section ordering, mixed line endings) that patch application succeeds against
the wrong context. The surgical route — content-search boundaries, file-by-file, compile and full suite
on the target branch — is the only one that is honest here. Guardrail baselines are per-branch and were
regenerated on `walker-fix`, not copied; the delta was verified as pure lambda renumbering, 0 non-lambda
lines, before accepting it.

## Phase D3 — the door-attempt lifecycle (planned 2026-08-12, late)

> "currently you change 1 thing to fix something, you rip the patch off something else." — the
> user, after watching the Stronghold corridor expose four serial pass-consumers in one evening.
> That is what implicit contracts between eleven independent door-state holders guarantee, and what
> one owner with an explicit lifecycle makes structurally impossible.


The audit called `Rs2Walker` a god-class where the stalls live; the Stronghold of Security's chained
gates spent one evening proving it empirically. Four pass-consumers were found and fixed serially —
the raw scan's backtrack window (crossed-face guard, 160a5fbe4f), the recovery anchor
(8aabe7cceb), the recent-attempt nudge's victory lap over a conquered door, and the fallback click
arming dead interims (e192da4a46) — and every one was a DISAGREEMENT between the door subsystem's
scattered state stores. There are TEN independent ones: `recentDoorAttemptByEdge`,
`recentlyOpenedStationaryDoors`, `sessionBlacklistedDoors`, `doorCrossFailuresByEdge`,
`walkScopedDoorBlocks`, `routeState.lastDoorAttempt*`, the door settle window, the global
interaction cooldown, `rawScanFocusedDoorIdx`, and `doorEdgesAttemptedThisTail`. Any two of them
can hold contradictory beliefs about one door, and the corridor is dense enough to manifest each
contradiction as a stall.

The cure is one owner: a **door-attempt ledger** — per-edge records with a lifecycle
(DETECTED → ATTEMPTED → CROSSED | REFUSED | EXPIRED), transitions driven by the geometric truths
that ended tonight's bugs (`playerBeyondWallFace`, `crossedDoorAxis`, the conclusive-sample rule),
strike counting and walk-scoped blocks folded in, and a pure `DoorLifecycle.decide(...)` table
answering the one question every entry path currently answers privately: *may I act on this door,
and if not, why not.* The three entry paths (segment handler, segment probe, raw scan) and the
recovery consumers become reporters and readers of the ledger instead of keepers of private maps.

> **D3 slice 1 DONE** — `PluginTesting` 2e4df33d14. `DoorAttemptLedger` owns ATTEMPTED:
> `recentDoorAttemptByEdge` and `routeState.lastDoorAttempt*` were the same fact stored twice with
> different lifetimes (the victory-lap disagreement), now two facets of one record set — per-edge
> cooldowns survive walk boundaries, the latest claim is withdrawn at walk start and on an observed
> crossing. Characterization table in `DoorAttemptLedgerTest` (direction-blind cooldown vs
> direction-aware same-edge check, withdraw-claim-keeps-cooldown). Deleted both Rs2DoorHandler
> map-shufflers and the WalkerRouteState triple. Eight stores remain. Live gate PASSED 2026-08-13
> 12:45: twelve gates, one attempt each, 101s — identical signature to the pre-fold run.

> **D3 slice 2 DONE** — `PluginTesting` 6730e68e2f. The ledger owns REFUSED:
> `doorCrossFailuresByEdge` + `walkScopedDoorBlocks` folded in as strike counting and
> once-only-draining walk-scoped blocks; Rs2DoorHandler's pass-the-map statics and DoorStrike enum
> deleted. Strike table migrated with two new rows (direction-blind strike accumulation,
> drain-exactly-once). Planner learn/unlearn stays in the shell. Six stores remain:
> `recentlyOpenedStationaryDoors`, `sessionBlacklistedDoors`, the door settle window, the global
> interaction cooldown, `rawScanFocusedDoorIdx`, `doorEdgesAttemptedThisTail`. Live gate: shares the
> next corridor run with whatever slice follows (refactor-only, same wire behaviour).

> **D3 slice 3 DONE** — `PluginTesting` 58f16db0d9. The ledger owns the tile facets:
> `recentlyOpenedStationaryDoors` (suppress-reclick window; locality, expiry and expire-on-read
> pinned) and `sessionBlacklistedDoors` (session-permanent quest locks; plane-identity pins kept).
> Rs2DoorProbe consults the ledger instead of carrying a Set+Map through its signature;
> Rs2DoorHandler is down to the key builder and the global-cooldown pair. Guardrail baseline: one
> pure rename. Six stores folded, four remain: door settle window, global interaction cooldown,
> `rawScanFocusedDoorIdx`, `doorEdgesAttemptedThisTail`.

> **D3 requirement #1 LANDED early** — `PluginTesting` b12f9e9944. The goal-object rule
> (`goalTileObjectIsNotAnObstacle`, wire line `door_skip_goal_object`) shipped as a pure guard ahead
> of the ledger's decide table after the Gift of Peace chest cost ~9s on three consecutive corridor
> runs. Narrow by design: wall doors on the goal edge stay handleable, distance-0 walks still open
> honestly, and the skip requires the walk be allowed to finish from the near side (same
> tightFinishThreshold as arrival). The rule folds INTO DoorLifecycle.decide when that table exists.
> Requirement #1 LIVE-VERIFIED 2026-08-13 14:20 — door_skip_goal_object fired at the goal chest,
> walk finished within-distance immediately; the ~9s tax is gone. Same run surfaced requirement #3
> (NEW): the segment-door site classifies ANY Open-actioned GameObject as a route door — a second
> Gift of Peace chest EN ROUTE (not on the goal) was clicked for 7s. The probe site requires a
> door-ish name; the segment site doesn't (large gates are sometimes GameObjects named "Gate", so a
> naive name filter is wrong). Belongs to the ledger's decide table / DoorLifecycle classification,
> not another point patch — strike-out contains repeats meanwhile.
> Requirement #2 LANDED — 5e18923361: walled-net learning defers to ACTIONED doors ADJACENT to
> the edge (double-gate slave-wing lesson, both live shapes as decision rows; suppression errs safe). Live gate: next corridor run should
> show door_skip_goal_object at the chest and an arrival ~9s sooner.

> **Fold stall FIXED, LIVE-VERIFIED 2026-08-13 17:42** — `PluginTesting` 716ca779dd. Branch tiles
> now log once and the same pass handles the forward gate (observed twice, including a two-tile
> skip); zero pending exits, zero idle-nudge rescues. Same run: the double-gate wing guard fired on
> the exact 14:00 edge (walled_edge_not_learned), and a mid-walk network logout was recovered by the
> script's auto-retry from mid-corridor without walker pathology. Two missing truths: the pass must not END at a
> behind/branch tile (continue scanning; the next gate gets handled the same pass), and a wall door
> whose face the player is beyond is RESOLVED in both route-door classifiers (conquered moves-you
> gates keep their Open action forever and were vetoing the continuation click from the backtrack
> window). Live gate: route-fold-continuation-pending should stop repeating; no idle-nudge rescues
> at gate deposits; corridor drops by the stall cost (~4-26s/run). The CROSSED-event formalization
> still belongs to the ledger's decide table; this fix uses the geometric truth directly.

> **D3 slice 4 DONE — ALL TEN STORES FOLDED** — `PluginTesting` b145854b0a. The walk-runtime
> quartet (per-tail pass budget, settle window, global cooldown, raw-scan focus) joins the ledger;
> WalkerRouteState loses seven fields and every door-handling signature loses its threaded Map
> parameter (the budgeted/unbudgeted split survives as an explicit boolean). The ledger is now the
> single owner of door state. Remaining D3 work: the DoorLifecycle.decide table (requirement #3's
> home — chest-as-door classification) and pointing the three entry paths at it. Live gate PASSED
> 2026-08-13 19:41: twelve gates, 115s, identical behaviour; the wing guard fired twice more (once
> on the gate's OWN edge that the exact-edge check missed on snapshot timing — the adjacency net is
> defense in depth). Same run: the slow-login refresh_transports instrumentation finally fired —
> total=833ms with key=658ms, so the cost is the CACHE-KEY computation, not the filtering (task #13's
> diagnosis, banked).

> **Requirement #3 LANDED** — `PluginTesting` d555583c19. `Rs2DoorClassifier.isRouteDoorObject` is
> the decide table's first column: walls open by action (unchanged), GAME objects need a door-like
> name or a traversal-proof verb — bare Open on a non-door name is scenery. The walker previously
> held FOUR different answers to this question across ten sites; all ten now call the one rule.
> Live gate: chest-adjacent walks log gameobject-not-a-door rejects instead of Open-clicks.

Sequencing: after B2's remaining live checks settle. Same slice discipline — one store folded into
the ledger per slice, characterization first, the Stronghold corridor as the live gate for every
slice. The file is 14,003 lines as of tonight (GROWN ~2k since the audit measured 12k, even as
processWalk shrank under its guard): D3 is the first phase whose success metric is the file getting
SMALLER, because each folded store deletes its scattered call sites.

## Phase E — transport-handler extraction

> **E1 DONE** — `PluginTesting` a8ee6893e4. Rs2Walker 14,119 -> 11,245 lines (-20%);
> ~90 methods / 3,090 lines into `Rs2WalkerTransports` (same package, static-import sharing,
> package-private dispatcher). The compiler corrected the static closure: seven methods moved back
> (callers behind multi-line signatures), one restored to the nested Telemetry class. Baseline delta
> verified an exact multiset re-home (61 out = 61 in, zero new/vanished violations). Full suite
> green. Corridor gate 2026-08-13 22:55: NO REGRESSION (12 door legs, 120s, normal signature) —
> but the walk started inside the corridor, so the moved dispatcher itself was not exercised; that
> half of the gate rides the next walk that takes any transport. Also noted: the spawn-side first
> gate logs did-not-traverse then crosses on continuation EVERY run (4/4, ~5s each; conclusive-gate
> correctly refuses the strike — cosmetic cost, minor open item). E2/E3 subsumed — the whole
> component moved in one verified step.

## Phase E — original scope (superseded by E1-complete above)

The line-count phase. ~2,400 lines of self-contained transport executors live inside Rs2Walker:
`handleSelectedTransport` (639), `handleObjectExceptions` (177), `handleCanoe` (114),
`handleObject` (104), `handleMinigameTeleport` (82), `handleInventoryTeleports` (80),
`handleFairyRing` (77), `handleSeasonalTransport` (68), `handleGlider` (63),
`interactWithAdventureLog` (59, + the minecart-947 machinery), `handleSpiritTree` (52),
`handleAlKharidTollGate` (35), plus their private helpers.

Slice discipline, one executor family per slice, biggest first: (E1) `handleSelectedTransport` +
`handleObject`/`handleObjectExceptions` into `util/walker/transport/Rs2TransportExecutor`; (E2) the
widget-flow teleports (fairy ring, glider, minecart/adventure log, spirit tree, minigame,
inventory); (E3) canoe + seasonal + toll + Stronghold answer. Dependencies to thread:
`expectedTransportDestinations`, route-state stamps, `WebWalkLog` tmarks, `currentTarget`. Each
slice: characterization where a pure core exists, full suite, corridor unchanged, guardrail
baseline regenerated deliberately (lambda renumbering will be extensive). DO THIS IN A FRESH
SESSION — it is mechanical but chimera-prone, and it is the phase whose success metric is
Rs2Walker finally getting SMALLER (14.1k today).

## Branch policy — settled 2026-08-12

**`PluginTesting` is authoritative.** All walker work lands there first; it is the branch actually
run day to day, so it is the branch that produces the live evidence every fix here depends on.

**`walker-fix` receives batched merges of walker and API-layer changes only — never plugins.** That
is the whole of its remit: `util/walker/**`, `util/pathfinder` / `shortestpath/**`, the walker's data
files, and the shared API/util layer the walker sits on. Plugin work (farming, questing, kudos,
thieving, hunting, …) stays on `PluginTesting` and does not travel, even when it is in the same
commit range. Batch the merge so this costs one sync per group of fixes rather than one per fix.

Two mechanical rules that have already been paid for once each:

- **Run `git rev-parse --abbrev-ref HEAD` immediately before every commit.** A cherry-pick has landed
  on the wrong walker branch once.
- **Hand-apply; do not cherry-pick.** See the sync note above — the copies have diverged enough that
  a patch can apply against the wrong context and drop changes silently while still building.

Do **not** sync to `Fix-The-Walker` or `WalkerRewrite`; those hold the rewrite, not the fixes.
