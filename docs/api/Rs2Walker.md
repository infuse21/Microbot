# Rs2Walker compatibility API

## [Back](development.md)

## Ownership

`Rs2Walker` is the plugin-facing compatibility facade for movement. Blocking route requests
enter through `walkTo`, `walkWithState`, or the banked variants, then delegate to the single
`NavigationEngine` request/session/planner/cancellation lifecycle through
`NavigationWalkCoordinator`; there is no legacy executor fallback.

The facade still supplies live-client observations and input adapters used by engine decisions.
Transport, door, recovery, and banking components report outcomes to the engine and do not own or
replace the active route. `BankedTransportCoordinator` owns the optional bank setup transaction,
while `Rs2WalkerBankingPlanner` owns requirement analysis.

## Blocking route entry points

- `walkTo(...)` returns `true` only when the engine-owned walk arrives.
- `walkWithState(...)`, `walkWithStateUntil(...)`, and `walkWithStateTry(...)` expose the terminal
  `WalkerState` while retaining the established plugin signatures.
- `walkWithBankedTransports(...)` and `walkWithBankedTransportsAndState(...)` run bank setup only
  when the caller opts into banked transports. Both the bank leg and final target leg remain
  NavigationEngine-owned.
- `walkUntil(...)` preserves the caller completion predicate without creating a second executor.

Replacing a target, Ctrl+X cancellation, plugin shutdown, and route failure all invalidate the
same generation-owned session. Unsupported routes fail explicitly; they are never handed to a
retired walker loop.

## Direct movement conveniences

- `walkFastCanvas(WorldPoint)` and `walkCanvas(WorldPoint)` issue a bounded canvas movement click.
- `walkMiniMap(WorldPoint)` issues a minimap click.
- `walkFastLocal(LocalPoint)` is intended for caller-owned local movement in instanced activities.
- `walkStep(WorldPoint, int)` performs a single caller-owned step.

These helpers are intentionally not complete pathfinding sessions: they do not calculate a route,
bank, traverse a transport chain, or take ownership from an active NavigationEngine request.

## Route queries

- `canReach(WorldPoint)` checks local reachability.
- `getWalkPath(WorldPoint)` returns the current calculated walk path.
- `getTotalTiles(WorldPoint)` estimates route length and returns `Integer.MAX_VALUE` when no route
  can be produced.
- `setTarget(WorldPoint)` remains a compatibility control surface; normal callers should prefer a
  walking entry point so request generation and cancellation remain atomic.

## Threading contract

Blocking compatibility calls must run on a script/worker thread. Client-thread work is limited to
short snapshots or input dispatch; all game-state waits use bounded `sleepUntil` polling from the
worker and must never block or sleep the client thread.
