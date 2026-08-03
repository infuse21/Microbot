# Walker Unification Plan

_Created: 2026-08-03. Scope: `util/walker/` runtime execution and the automation-facing
parts of `shortestpath/`. This supersedes the architectural direction in
`walker-audit.md`; `walker-p2-unification.md` remains useful historical context._

## Decision

Rewrite the walker's orchestration and state ownership incrementally. Preserve the mature
pathfinding, collision, transport-data, and interaction knowledge. Do not replace the
working system in one cutover, and do not continue adding independent recovery paths to
`Rs2Walker.processWalk`.

The migration is complete only when the old orchestration is deleted. Adapters that leave
both systems permanently active are not completion.

## Why this work is needed

The current system has several overlapping owners:

- `Rs2Walker` owns the scripted destination, movement loop, recovery, interaction dispatch,
  cancellation, and much of route lifecycle.
- `ShortestPathPlugin` owns mutable pathfinder state, target markers, live-collision route
  validation, and another pathfinder lifecycle.
- `ShortestPathScript` owns a walk task plus an additional terminal-state retry loop.
- Static state and futures connect those owners indirectly through `Rs2PathApi`.

This makes a walk the product of multiple implicit state machines. Door, transport,
off-path, stall, live-collision, and retry branches can react to the same observation with
different actions. The result is hard-to-reproduce cancellation, duplicate clicks,
recalculation churn, and fixes that alter a distant recovery path.

## Goals

1. Give one component exclusive ownership of an active walk.
2. Make every tick/pass produce at most one movement or interaction command.
3. Represent route progress, pending interactions, recovery, and cancellation explicitly.
4. Reject stale asynchronous pathfinder results deterministically.
5. Preserve the existing public `Rs2Walker` entry points during migration.
6. Keep specialised transport behaviour without giving transports separate control loops.
7. Make orchestration headlessly testable; reserve live tests for client integration.
8. Delete the legacy branches as their replacements become proven.

## Non-goals

- Rewriting `Pathfinder`, `CollisionMap`, `PathSmoother`, or the transport datasets.
- Making every door, boat, teleport, shortcut, and ladder use identical internal logic.
- Changing plugin callers to understand pathfinder internals.
- Replacing all shortest-path UI, panels, or overlays during the executor migration.
- Solving missing or incorrect transport data as part of the architecture cutover.

## Target architecture

```text
Callers / ShortestPath UI
          |
          v
  Rs2Walker facade
          |
          v
  NavigationEngine --------------> NavigationSnapshot (read-only)
          |
          +-- owns one WalkSession
          +-- asks RoutePlanner for immutable RoutePlan
          +-- asks InteractionCoordinator for one intent
          +-- sends one command through WalkerActions
          |
          +--> RoutePlanner ------> Pathfinder + collision/transport data
          |
          +--> InteractionCoordinator
                 +-- DoorHandler
                 +-- TransportHandler(s)
                 +-- MineableHandler
                 +-- future obstacle handlers

ShortestPathPlugin
  - turns UI input into navigation requests
  - renders NavigationSnapshot
  - captures live collision
  - does not own automation lifecycle
```

### Ownership rules

- `NavigationEngine` is the only writer of active-walk state.
- `RoutePlanner` owns pathfinder task creation/cancellation. Neither the UI plugin nor an
  interaction handler manipulates its futures.
- `ShortestPathPlugin` may request, cancel, and observe a walk, but may not recalculate one
  directly.
- Interaction handlers propose or advance an interaction. They do not replan, clear the
  target, or issue an unrelated fallback click.
- Live-collision capture publishes an observation/event. The engine decides whether it
  invalidates the current route.
- UI and overlays consume immutable snapshots and never read mutable session internals.

## Core model

### NavigationRequest

An immutable request containing:

- request ID
- destination set and reached distance
- route options snapshot
- whether banked transports are allowed
- caller/source label for diagnostics
- cancellation token

### RoutePlan

An immutable result containing:

- request ID and route generation
- raw adjacent path
- smoothed click path
- explicit route edges
- transport steps and their raw-path anchors
- start, reachable endpoint, and requested destinations
- complete/partial status and calculation diagnostics

Raw and smoothed paths must be mapped once when the plan is created. Runtime code must not
reconstruct that relationship with repeated nearest-tile guesses.

### WalkSession

All mutable state for one request:

- current phase
- current route plan and generation
- raw/smoothed progress indices
- last observed player position
- last issued command and command timestamp
- pending interaction and its attempt state
- progress/stall evidence
- recovery attempts and budgets
- learned/temporarily blocked edges
- terminal result and reason

The session is instance state. It must not be shared through public static fields.

### Phases

```text
NEW
CALCULATING
FOLLOWING_ROUTE
APPROACHING_INTERACTION
PERFORMING_INTERACTION
VERIFYING_INTERACTION
REPLANNING
ARRIVED
UNREACHABLE
CANCELLED
FAILED
```

Transitions must be named and logged. A pass through the engine returns one decision and
does not recurse into another pass.

### Engine decisions

```text
NO_ACTION
CLICK_TILE
INTERACT
WAIT
REQUEST_REPLAN
COMPLETE
FAIL
```

Only `CLICK_TILE` and `INTERACT` may send input. Enforce at most one such decision per
engine pass.

### Interaction contract

Unify the protocol, not the domain logic:

```java
InteractionProposal inspect(RouteContext context);
InteractionProgress advance(InteractionProposal proposal, WalkerActions actions);
```

`InteractionProgress` should distinguish:

- `NOT_APPLICABLE`
- `APPROACH_REQUIRED`
- `COMMAND_ISSUED`
- `WAITING_FOR_CONFIRMATION`
- `CROSSED`
- `RETRYABLE_FAILURE`
- `UNAVAILABLE`

Handlers may keep specialised internal sequences. They may not clear the route, recalculate,
or fall through to a second handler after issuing a command.

## Concurrency and cancellation

Every request receives a monotonically increasing request ID. Every calculation within a
request receives a route generation.

When a calculation completes, publish it only when both values still match the active
session. A cancelled or superseded calculation may finish, but its result is discarded.

Use one pathfinding executor owner. Cancellation is:

1. mark the session cancelled;
2. invalidate its request ID/generation;
3. cancel the planner task;
4. publish a terminal snapshot;
5. remove UI marker state through the UI adapter.

Do not signal cancellation by temporarily setting a shared target to `null` and later
restoring it.

## Migration phases

Each phase must compile, pass its listed tests, and leave the branch shippable.

### Phase 0 - Baseline and invariants

No behaviour changes.

- Record the current route corpus and walker test results.
- Add architecture tests/grep checks for forbidden new dependencies:
  - no new direct `ShortestPathPlugin` mutable-state access outside `Rs2PathApi`;
  - no new recovery or interaction branches in `processWalk` without an explicit migration
    exception;
  - no interaction handler may call `setTarget`, `recalculatePath`, or planner lifecycle APIs.
- Define a compact decision trace format with request ID, generation, phase, observation,
  decision, and reason.
- Catalogue the existing public `Rs2Walker` methods and identify supported compatibility
  entry points versus unrelated helpers to move later.

Exit gate: baseline results and API inventory are committed and reproducible.

### Phase 1 - Extract planner ownership

No movement behaviour changes.

- Introduce `RoutePlanner` and a production adapter over the existing `Pathfinder`.
- Move executor/future/mutex ownership out of `ShortestPathPlugin` and
  `Rs2WalkerLifecycleRuntime` into that adapter.
- Add request ID and route generation checks.
- Return immutable `RoutePlan` objects instead of exposing the current mutable pathfinder as
  runtime state.
- Keep `Rs2PathApi` as a temporary compatibility bridge for unmigrated consumers.

Tests:

- stale calculation cannot replace a newer route;
- cancellation before and after calculation completion;
- simultaneous recalculate requests coalesce or deterministically supersede;
- partial and complete path results retain current semantics.

Deletion gate: remove duplicate pathfinder start/cancel implementations. There must be one
production owner of the pathfinding executor and future.

### Phase 2 - Introduce NavigationEngine in shadow mode

- Add `NavigationEngine`, `WalkSession`, phases, observations, and decisions.
- Feed it recorded/headless observations from existing walks.
- In shadow mode it emits decisions to diagnostics but sends no input.
- Compare its progress index, arrival, interaction-frontier, and replan decisions with the
  legacy walker.
- Publish `NavigationSnapshot` for diagnostics and future overlays.

Tests:

- phase transition table;
- one-command-per-pass invariant;
- cancellation is terminal;
- progress cannot move backward without a new route generation;
- benign movement-in-flight does not consume recovery budget.

Exit gate: corpus scenarios produce stable shadow decisions with every divergence classified.

### Phase 3 - Cut over ordinary walking

Scope only routes containing adjacent walk edges and no explicit transports or recognised
dynamic obstacles.

- Implement route-following click selection using raw/smoothed mapping from `RoutePlan`.
- Move interim-target ownership, movement acknowledgement, arrival checks, and ordinary
  off-path replanning into the session.
- Keep existing minimap/canvas action utilities behind `WalkerActions`.
- Select legacy versus new execution once, when the request begins. Never let both executors
  act on one request.

Tests:

- straight, diagonal, cornered, doubled-back, partial, and unreachable routes;
- rejected minimap/canvas clicks;
- external movement and combat displacement;
- route cancellation during movement;
- route replacement while a click is in flight.

Deletion gate: remove the equivalent ordinary-click, interim, and basic off-path branches
from `processWalk`.

### Phase 4 - Centralise recovery

- Move stall evidence and recovery budgets into `WalkSession`.
- Recovery produces `WAIT`, `CLICK_TILE`, or `REQUEST_REPLAN`; it never directly recurses.
- Live-collision contradictions become route invalidation observations.
- Preserve bounded sidestep/rejoin logic from `RouteRecovery` where tests show value.
- Distinguish no acknowledgement, no tile progress, off-route movement, blocked edge, and
  interaction wait instead of treating all of them as generic stuck state.

Tests:

- each failure category has an independent budget and terminal reason;
- recovery cannot overwrite a recent valid command;
- repeated blocked edge is learned/replanned once per generation;
- recovery never issues more than one command per pass.

Deletion gate: remove legacy `stuckCount` control flow and competing recovery click paths.

### Phase 5 - Migrate dynamic obstacles

Order: mineable blockers, then ordinary doors/gates, then unusual door/dialogue cases.

- Adapt the existing mineable resolver and door classifier/probe/geometry code to the
  interaction contract.
- Store the pending interaction in the session so it survives across passes.
- Separate approach, interaction, and crossing verification.
- On `UNAVAILABLE`, report the edge to the engine; the handler does not replan itself.
- Keep live scene reads behind a snapshot/adapter so classification remains headlessly
  testable.

Tests:

- obstacle ahead versus merely nearby;
- diagonal/cardinal door edges;
- opened without crossing;
- crossing while the exact landing predicate fails;
- dialogue/quest-locked door;
- blocker despawns while approaching;
- inverse adjacent interaction suppression.

Deletion gate: remove migrated door/rockfall scans and their attempt/cooldown fields from
`Rs2Walker`.

### Phase 6 - Migrate transports by family

Do not rewrite all transports together. Suggested order:

1. adjacent same-plane shortcuts;
2. stairs, ladders, trapdoors, and caves;
3. simple item/spell teleports;
4. dialogue/NPC/boat transports;
5. charter, fairy ring, spirit tree, glider, quetzal, POH, seasonal, and other specialised
   families;
6. banked-transport route setup.

For each family:

- preserve its existing interaction implementation initially;
- adapt its progress/result to the common interaction contract;
- pin headless decision tests and a small live route set;
- cut over that family;
- delete its old dispatch and bookkeeping before starting the next family.

Required transport invariants:

- approach origin before interacting;
- confirm crossing from location/plane/interface evidence appropriate to the family;
- suppress immediate inverse traversal;
- recalculate from the actual landing location when required;
- never let a transport handler issue a normal route click as an undocumented fallback.

Deletion gate: `handleTransports` no longer owns a control loop; only family handlers remain.

### Phase 7 - Make ShortestPathPlugin an adapter

- Route map-click/hotkey requests through `NavigationEngine`.
- Render the path, target marker, ETA, and debug information from `NavigationSnapshot` and
  immutable `RoutePlan`.
- Replace `ShortestPathScript` terminal retry logic with an engine retry policy or remove it
  when the engine has explicit failure categories.
- Publish live-collision updates to the engine rather than invoking walker recalculation.
- Remove static mutable pathfinder, target, future, and executor state from the plugin.

Tests:

- UI target set/replace/clear;
- Ctrl+X cancellation;
- plugin shutdown with calculation or interaction in flight;
- login/world-hop refresh;
- overlays tolerate no active session and terminal sessions.

Deletion gate: `shortestpath` UI code no longer imports `Rs2Walker` implementation details;
the engine has no dependency on plugin UI classes.

### Phase 8 - Collapse the facade and remove legacy orchestration

- Make supported static `Rs2Walker` methods thin delegates to the injected/service-owned
  engine boundary.
- Move banking, route-analysis, location-enum, puzzle, and unrelated convenience APIs out of
  the core executor class where practical.
- Delete dead obstacle abstractions, unused adapters, duplicate awaits, legacy state fields,
  and compatibility methods with no callers.
- Split `PathfinderConfig` only after executor migration, separating immutable route options,
  transport repository/availability, and collision provider.
- Update `docs/api/Rs2Walker.md`, entity movement gotchas, and architecture docs.

Exit gate:

- no legacy `processWalk` orchestration remains;
- one owner exists for request, session, calculation, and cancellation state;
- old and new executor flags are gone;
- the compatibility facade contains no route logic;
- full route corpus and live acceptance suite pass.

## Verification strategy

### Required on every phase

- `./gradlew :client:compileJava`
- focused headless tests for changed components
- existing walker/shortest-path unit tests
- no unexpected changes to route-corpus results
- review decision traces for duplicate input in the same pass

### Required before a behaviour cutover

- recorded/harness reproduction for the behaviour being moved;
- test that fails against the new component before the fix/migration;
- comparison of legacy and new terminal outcomes;
- cancellation test during the behaviour's longest wait.

### Required before deleting a legacy path

- production call sites point exclusively at the replacement;
- relevant route corpus and live scenarios pass;
- no fallback can silently invoke the deleted executor;
- state fields used only by the deleted path are removed in the same phase.

## Observability

Use one compact record per meaningful decision, behind the existing verbose toggle:

```text
[Nav] req=42 gen=3 phase=VERIFYING_INTERACTION
      at=3201,3218,0 edge=3201,3218,0->3202,3218,0
      decision=WAIT reason=door-command-awaiting-crossing ageMs=600
```

Always log terminal transitions at an appropriate normal level with a stable reason code.
Do not log player/session identifiers. Counters should include calculations, stale results,
replans by reason, commands by type, interaction attempts, recovery attempts, and terminal
outcomes.

## Safety and rollback

- Behaviour cutovers are family/scenario flags, not one flag per internal branch.
- A request selects its executor at creation; changing configuration cannot swap executors
  mid-session.
- Keep the legacy executor available only until the current phase's acceptance routes pass.
- Roll back by selecting legacy for new requests, never by handing an active session from one
  executor to the other.
- Remove each flag when its legacy path is deleted to prevent a permanent dual architecture.

## Pull request slicing

Prefer small vertical PRs with a deletion or enforceable boundary:

1. models and architecture tests;
2. planner ownership plus stale-result protection;
3. shadow engine;
4. ordinary walking cutover and deletion;
5. central recovery cutover and deletion;
6. one obstacle/transport family per PR;
7. plugin adapter cutover;
8. facade cleanup and final legacy deletion.

Avoid PRs that only move hundreds of lines from `Rs2Walker` into a new static helper while
leaving ownership and control flow unchanged.

## Completion criteria

The rewrite is complete when all of the following are true:

- one `NavigationEngine` owns each active `WalkSession`;
- one planner owner manages asynchronous pathfinding;
- stale route results cannot be published;
- one engine pass can issue at most one input command;
- interactions persist as explicit state across passes;
- UI, overlays, and callers observe immutable snapshots;
- `Rs2Walker` is a compatibility facade rather than an executor god-class;
- `ShortestPathPlugin` is a UI/live-data adapter rather than an automation state owner;
- old orchestration, duplicated lifecycle methods, retry loops, and migrated state fields are
  deleted;
- compile, unit, corpus, cancellation, and live acceptance checks pass.
