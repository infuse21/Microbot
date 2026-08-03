# Rs2Walker API Inventory

_Snapshot: 2026-08-04. Phase 0 input for `walker-unification-plan.md`._

## Summary

`Rs2Walker` currently exposes 79 top-level public static method declarations and is referenced
from 66 production source files. The surface combines five different responsibilities. The rewrite
must preserve caller compatibility for navigation entry points while moving planning, diagnostics,
banking, and geometry to narrower owners.

The inventory below records migration intent, not immediate removals. Existing methods remain until
their production callers have moved or a compatibility delegate exists.

## Supported compatibility surface

These are the methods the final thin `Rs2Walker` facade should retain, unless call-site migration
shows that an overload has no users:

| Capability | Current methods | Destination |
|---|---|---|
| Blocking walk | `walkTo(...)`, `walkUntil(...)`, `walkWithState(...)`, `walkWithStateUntil(...)`, `walkWithStateTry(...)` | Delegate to `NavigationEngine` request/result API |
| Non-blocking walk | `walkStep(...)` | Delegate to one engine advancement/request API |
| Bank-aware walk | `walkWithBankedTransports(...)`, `walkWithBankedTransportsAndState(...)` | Facade over navigation request plus route-setup policy |
| Cancellation | `clearWalkingRoute(reason)` | Delegate to engine cancellation with a stable reason |
| Read-only active state | `getCurrentTarget()`, `getLastRouteClearReason()`, `getLastRouteClearAtMs()` | Read from immutable `NavigationSnapshot` |

`setTarget(...)` is currently used as both a start/recalculate mechanism and a cancellation API.
It remains during migration, but is not part of the desired final caller contract. Callers that mean
cancel should move to `clearWalkingRoute(reason)`; callers that mean walk should submit a request.

## Move behind engine/planner ownership

These methods expose lifecycle or planning internals and should disappear from the public facade
after Phase 7:

- `setTarget(...)`
- `recalculatePath()`
- `restartPathfinding(...)`
- `runWithWalkerLockReleased(...)`
- `setSeasonalTransportHandlers(...)`
- `getSeasonalTransportHandlers()`
- `recordTransportAttempt(...)`
- `isNearPath()` and parameterless `isNear()`

Direct mutable fields `config` and `disableTeleports` are also part of this category. They should
become immutable request/route options rather than process-wide switches.

## Move to RoutePlanner/query services

These are useful APIs but are not execution responsibilities:

- `isWalkableInCollisionMap(...)`
- `getTotalTiles(...)`
- `getTotalTilesFromPath(...)`
- `canReach(...)`
- `getWalkPath(...)`
- `getTransportsForPath(...)`
- `getDistanceBetween(...)`
- `nearestReachable(...)`
- `findNearestAccessibleTarget(...)`
- `isTeleportItem(...)`

During migration, `Rs2Walker` may delegate these calls to `RoutePlanner`/`RouteQueryService` to
avoid a flag-day change across callers.

## Move to interaction/geometry utilities

These methods perform low-level movement or geometry and should not live on the final navigation
facade:

- `walkMiniMap(...)`
- `walkFastLocal(...)`
- `walkFastCanvas(...)`
- `walkCanvas(...)`
- `getPointWithWallDistance(...)`
- `getClosestTileIndex(...)`
- `getTile(...)`
- `walkNextTo(...)`
- `walkNextToInstance(...)`
- `isInArea(...)`
- `isCloseToRegion(...)`
- `distanceToRegion(...)`
- `isNear(WorldPoint)`

The engine should reach these through `WalkerActions` and pure geometry services. Existing external
callers can migrate independently after the engine cutover.

## Move to banking/route preparation

These methods already delegate partly to `Rs2WalkerBankingPlanner` and should move completely:

- `getTransportsForDestination(...)`
- `prepareTransportsForDestination(...)`
- `hasRequiredTransportItems(...)`
- `getMissingTransports(...)`
- `getMissingTransportItemIdsWithQuantities(...)`
- `getMissingTransportItemIds(...)`
- `compareRoutes(...)`

`closeWorldMap()` is UI utility behaviour and belongs outside the core walker.

## Diagnostics

The public nested `Telemetry` methods are implementation diagnostics, not caller APIs. Replace them
with engine counters and immutable snapshots. Preserve stable terminal reason codes used by the CLI
and harness, but do not preserve the nested static class as an architectural dependency.

## Current production-call concentration

The most common calls outside `Rs2Walker` at the snapshot date are:

| Call | Occurrences |
|---|---:|
| `walkTo` | 47 |
| `walkFastCanvas` | 22 |
| `clearWalkingRoute` | 16 |
| `getTransportsForPath` | 9 |
| `canReach` | 6 |
| `setTarget` | 6 |
| `walkWithState` | 4 |
| `getWalkPath` | 4 |
| `getTotalTiles` | 4 |

This concentration supports preserving a small compatibility facade: most callers use ordinary
walking, canvas movement, or cancellation, while the large remainder of the 79-method surface is
planning and utility functionality with relatively few consumers.

## Phase 0 enforcement

`WalkerArchitectureGuardTest` prevents:

1. new mutable `ShortestPathPlugin` access outside the plugin package and `Rs2PathApi` facade;
2. door/obstacle/transport handlers from taking route lifecycle ownership;
3. the legacy `processWalk` method from growing while the new engine is built.

The size ceiling is deliberately one-way. It may be lowered as phases delete legacy branches; it
must not be raised merely to accommodate another patch.
