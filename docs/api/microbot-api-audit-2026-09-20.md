# Microbot scripting API audit

Date: 20 September 2026. Source baseline: `a8a907eefd` plus the existing working tree.

## Scope and evidence

This audit covers Microbot's Java scripting API: entity caches and queryables, entity models, reachability, scheduling/waits, the Script base class, and selected inventory, banking, widget and walking entry points. RuneLite's API is not being audited; dependency contracts were consulted only to check Microbot's use of coordinates. Agent Server HTTP endpoints, telemetry, every walker transport, and individual automation plugins are outside this report's coverage. This is a deep review of the shared scripting foundation, not a claim that every Microbot method was exhaustively verified.

Fourteen findings below are supported by source inspection. Their failure scenarios and proposed regression tests have not been executed against a live game. P1 means a high-priority correctness or concurrency issue; P2 means a narrower correctness or contract issue. No P0 finding was established.

The strongest systemic concern is that a convenient facade does not consistently provide the thread safety, entity lifetime or success semantics its callers need. Several small logic defects are also independently actionable.

## Findings

### 1. P1 — Reachability starts at the target, making results depend on query order

**Evidence:** [Rs2Reachable.isReachable](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/reachable/Rs2Reachable.java:22), [getReachableTiles](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/reachable/Rs2Reachable.java:32), [IEntity default](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/IEntity.java:16).

`isReachable(target)` calls `getReachableTiles(target)`. The flood fill unconditionally marks its starting tile visited, and the caller then tests whether that same tile belongs to the result. On a fresh cache, an in-scene target on the current plane therefore reports reachable even when the player is in a disconnected area. The tick-only cache then reuses that target's connected region for subsequent targets. An overlay that first requests a fill from the player can mask the bug.

This affects the default entity reachability path used by NPC/player/ground-item queries, including `nearestReachable()` and query interaction helpers. Tile objects have a separate override and should not be assumed to share precisely this path.

**Repair:** Start player-reachability searches at the player's location. If arbitrary origins remain supported, key the cache by origin, world-view/scene identity, plane and tick. Keep the returned set read-only and publish it safely; the current API exposes the mutable set.

**Regression test:** Put player and target in disconnected collision regions. Query both target orders in one tick, with and without an earlier player-origin lookup, and assert identical answers.

### 2. P1 — NPC, player and ground-item cache refreshes run on the caller's thread

**Evidence:** [NPC cache](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/npc/Rs2NpcCache.java:41), [player cache](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/player/Rs2PlayerCache.java:40), [ground-item cache](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/tileitem/Rs2TileItemCache.java:49), [query construction](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/AbstractEntityQueryable.java:24).

These caches receive a `ClientThread` but do not use it when refreshing. They enumerate live actors or scene tiles and replace ordinary list/tick fields without confinement or safe publication. A script thread can enter directly through `query()`, whose constructor immediately calls `initialSource()`.

Calling `firstOnClientThread()` or `nearestOnClientThread()` afterward does not fix that earlier collection phase. Concurrent readers can race cache refreshes; scene transitions can race live traversal.

**Repair:** Confine refresh, cache-version checks and snapshot copying to one client-thread operation, following the existing tile-object cache pattern. Publish immutable values if reads should remain entirely off-thread. Simply making the list reference volatile does not make live scene traversal safe.

**Regression test:** Invoke each cache from an executor while mock scene accessors require client-thread execution. Exercise concurrent first reads and a scene transition.

### 3. P1 — Microbot models and widget helpers expose live state outside the client thread

**Evidence:** [Rs2ActorModel](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/actor/Rs2ActorModel.java:83), [widget bounds access](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/widget/Rs2Widget.java:60), [widget text access](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/widget/Rs2Widget.java:130), [inventory widget access](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/inventory/Rs2Inventory.java:1932).

Entity models are wrappers around live references, not immutable state snapshots. Some getters marshal to the client thread, while adjacent getters such as `getLocalLocation()` and `getAnimation()` delegate directly. Setters such as `setAnimation()` also directly mutate the wrapped actor. Copying a list of these models only snapshots membership.

Likewise, `Rs2Widget.getWidget()` obtains a widget on the client thread, but helpers subsequently call `getText()`, `getChild()` or `getBounds()` on the caller's thread. This is a defect in Microbot's facade boundary, not a proposed change to RuneLite's API.

**Repair:** Capture each operation's required values together on the client thread, then perform waits and mouse work outside it. Explicitly distinguish live wrappers from immutable snapshots. Revalidate entity identity/slot/world-view immediately before dispatch.

**Regression test:** Use models/widgets whose accessors reject off-thread access and exercise public helpers from a script executor. Include despawn and widget replacement between lookup and interaction.

### 4. P1 — Await scheduling shares one cancellation handle across all callers

**Evidence:** [Global.awaitExecutionUntil](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/Global.java:28).

Every call overwrites the same static `scheduledFuture`. If wait A becomes ready after wait B is registered, A cancels B, clears the shared field and invokes A's callback. A can remain scheduled and later encounter a null handle. With zero initial delay, execution can also begin before its own future has been assigned. Cancelling with interruption before running the callback can leave the callback executing under interruption.

No production call site was found in the searched Microbot tree, so this is an exposed API defect rather than a demonstrated current script failure.

**Repair:** Give each request independent completion and cancellation ownership, safe even if the condition is initially true. Use a small request-local task or completion future and exactly-once completion; avoid a shared future field.

**Regression test:** Register two independently controlled waits, complete them in either order, cancel one, and test an immediately true condition.

### 5. P1 — Tutorial handling bypasses pause and cancellation checks

**Evidence:** [Script.run](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/Script.java:77).

For a logged-in player who has not completed Tutorial Island, `run()` returns true before checking `pauseAllScripts` or thread interruption. A script using the normal `if (!super.run()) return` guard can therefore proceed despite being paused or interrupted in that state.

**Repair:** Check cancellation and the global pause flag before the tutorial-specific branch. Preserve the intended ability of tutorial scripts to run; do not simply invert the tutorial return value.

**Regression test:** With tutorial incomplete, independently enable pause and interrupt the calling thread; both must cause the guard to reject execution.

### 6. P2 — Boat cache is not keyed by the requested player

**Evidence:** [Rs2BoatCache.getLocalBoat](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/boat/Rs2BoatCache.java:31), [getBoat](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/boat/Rs2BoatCache.java:62).

Both methods share `boat` and `lastCheckedOnBoat`. Within the cache window, asking for player B after player A returns A's boat. A local-player lookup can similarly receive a remote player's result, or null from a different player on land. The ordinary cache fields are also accessed across threads, and the timestamp is changed before the result is published.

**Repair:** Keep a cache specifically for the local player and resolve arbitrary players independently, or use a correctly keyed cache only if profiling justifies it. Confine the entire update to the client thread.

**Regression test:** Query two players in different views and a third on land, then query the local player, all within one cache window.

### 7. P2 — NPC distance helpers confuse local-coordinate units with tiles

**Evidence:** [isWithinDistanceFromPlayer](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/npc/models/Rs2NpcModel.java:57), [getDistanceFromPlayer](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/npc/models/Rs2NpcModel.java:70).

The documented tile-distance helpers return/compare `LocalPoint.distanceTo` directly. Adjacent tile centres differ by 128 local units along one axis, so a caller using a range of 5 tiles can reject an NPC only one tile away. The world-point overload uses a different unit and distance metric. Local coordinates from different world views are also not directly comparable.

**Repair:** Specify one tile-distance metric and use world points in a consistent coordinate space, with explicit plane/world-view handling. Preserve a separately named local-unit operation only if it is needed.

**Regression test:** Adjacent centres, diagonals, the exact range boundary, another plane and another world view.

### 8. P2 — Ground items always report the top-level world view

**Evidence:** [Rs2TileItemModel.getWorldView](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/tileitem/models/Rs2TileItemModel.java:118), [cache traverses every view](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/tileitem/Rs2TileItemCache.java:59).

The cache collects items across world views, but every model returns `getTopLevelWorldView()`. Consequently `fromWorldView()` can discard valid items when the player is in a subview, or incorrectly include subview items when the player is in the main world. Click dispatch already derives a world-view ID from the local point, so query identity and interaction identity disagree.

**Repair:** Preserve the originating world view during model construction and define how coordinates are projected for cross-view comparisons. Review ground-item reachability with the same identity.

**Regression test:** Items in top-level and child views with overlapping coordinates; filtering must select only the intended view and dispatch must use that same view.

### 9. P2 — Ground-item financial helpers have incorrect semantics and overflow risk

**Evidence:** [isProfitableToHighAlch](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/tileitem/models/Rs2TileItemModel.java:137), [getTotalGeValue](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/tileitem/models/Rs2TileItemModel.java:163), [getTotalValue](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/tileitem/models/Rs2TileItemModel.java:185).

`isProfitableToHighAlch()` returns true when market price exceeds alchemy proceeds: the comparison is reversed for profitability. It also lacks a definition of rune costs. `getTotalGeValue()` uses the item definition's base price, while the more generic `getTotalValue()` uses the market-price service. Both multiply two ints, allowing a sufficiently valuable stack to overflow and corrupt value-based filtering.

**Repair:** Name base, market and alchemy values explicitly; define whether profitability includes consumables. Perform multiplication in long arithmetic. Add long-returning methods and deprecate ambiguous int methods without silently breaking existing callers.

**Regression test:** Base price differs from market price; profitable, break-even and loss-making alchemy; a stack value above `Integer.MAX_VALUE`.

### 10. P2 — NPC partial-name matching mishandles capitalization

**Evidence:** [Rs2NpcModel.matches](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/npc/models/Rs2NpcModel.java:126).

The NPC name is lowercased only when `exact` is true. The partial branch lowercases the search string but uses the original NPC name. Thus searching for `goblin` in `Goblin` fails in this helper while the general queryable's case-insensitive contains filter succeeds.

**Repair:** Normalize both operands in the partial branch with `Locale.ROOT`, with an explicit null policy. Align helper semantics with queryable name filters.

**Regression test:** Mixed-case exact and partial matches, null inputs and a non-English default locale.

### 11. P2 — Missing inventory widget slots throw instead of falling back

**Evidence:** [Rs2Inventory.invokeMenu](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/inventory/Rs2Inventory.java:1941).

`findFirst().orElseGet(null)` passes a null supplier. When a stale item slot is absent from the widget children, the Optional invokes that supplier and throws. The following code explicitly allows a null widget and has a fallback to inventory actions, but that fallback is unreachable on this path.

**Repair:** Use a valid null fallback and revalidate slot/item identity. An absent or changed slot should return a failed dispatch, not select another item.

**Regression test:** A nonempty widget-child array with no child matching the cached slot must neither throw nor issue an interaction with an unrelated item.

### 12. P2 — Inventory interaction reports success when nothing was dispatched

**Evidence:** [Rs2Inventory.interact](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/inventory/Rs2Inventory.java:1201), [invokeMenu](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/inventory/Rs2Inventory.java:1898).

For any non-null item, `interact` returns true after calling a void helper. That helper can return early when inventory widgets are unavailable, so the result does not reliably mean even that an action was dispatched. An unrecognized action can also leave identifier `-1` and continue toward dispatch.

This is distinct from the normal difference between dispatch and game completion: here success can be reported without dispatch at all. Some banking methods do wait for state confirmation, so a single boolean convention cannot safely be inferred across utilities.

**Repair:** Propagate dispatch failure through the existing boolean API. Reject unresolved actions before dispatch. Document which methods merely submit an action and which confirm completion; introduce richer outcomes only where callers need them.

**Regression test:** Missing interface, unknown action, changed slot, successful dispatch and a dispatched action whose game-state confirmation times out.

### 13. P2 — Nearest queries treat invalid distance sentinels as valid candidates

**Evidence:** [AbstractEntityQueryable.nearest](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/AbstractEntityQueryable.java:220), [default maximum](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/AbstractEntityQueryable.java:168).

Null locations are mapped to `Integer.MAX_VALUE`, and candidates are accepted when distance is less than or equal to the maximum. The unbounded default is itself `Integer.MAX_VALUE`. An entity without a location can therefore win if no valid candidate exists. Different-plane world-point distances use the same sentinel and can also pass an unbounded query.

Meanwhile `within()` dereferences locations directly, giving different null behavior from `nearest()`.

**Repair:** Exclude missing locations and incompatible planes/world views before distance comparison. Reserve maximum distance for an actual bound rather than using it as both an invalid sentinel and a valid distance. Standardize null and negative-distance behavior.

**Regression test:** A source containing only null-location entities, only another plane, and mixed valid/invalid entities; default and bounded queries should agree on validity.

### 14. P2 — Client-thread wait helper continuously resubmits without a polling interval

**Evidence:** [Global.sleepUntilOnClientThread](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/util/Global.java:224).

While a condition is false, the helper repeatedly invokes the client-thread bridge until the deadline, with no delay or tick wait between checks. Actual frequency depends on bridge servicing, but the helper itself imposes no bound on submission rate. Multiple scripts can generate avoidable client-thread traffic. The void return also prevents callers from distinguishing completion from timeout.

**Repair:** Add bounded off-thread polling or tick-based waiting, use a monotonic deadline, and expose a boolean-returning variant. Keep the client-thread callback short and never sleep inside it.

**Regression test:** An always-false condition must time out with bounded invocation count; interruption must terminate promptly. Measure the count without relying on a live game.

## API improvements beyond individual defects

### Make the threading contract complete

Treat the public script API as safe for script-thread callers by default, with explicitly named/documented exceptions. Batch collection and cheap filtering in one client-thread operation; do expensive pure computation on copied values off-thread. Do not fix this by wrapping whole interaction routines in client-thread callbacks: they may walk, wait or move the mouse.

The existing tile-object collection boundary and inventory's volatile unmodifiable membership list are useful local patterns. Neither makes the referenced entity objects immutable.

### Define query ownership and lifetime

[AbstractEntityQueryable](C:/Users/keenx/IdeaProjects/Microbot/runelite-client/src/main/java/net/runelite/client/plugins/microbot/api/AbstractEntityQueryable.java:22) stores and mutates a single Java Stream. A terminal operation consumes it; `count()` followed by `first()` on the same query fails, and branches built from the same query share pipeline mutation. This can be a legitimate one-shot design, but the public contract should say so explicitly.

Document queries as confined, short-lived, single-use objects and show recreating a query for each poll. Only introduce reusable query descriptions if actual callers require them. The constructor also calls an overridable source method before subclass initialization; pass the source explicitly during a future focused cleanup.

### Make world-view and entity lifetime part of the contract

Specify whether a query covers the player's view or all views, whether world coordinates are projected, and whether an entity remains valid after a tick. Cached entity references should carry enough identity to reject stale actions. View identity must be preserved through query, distance, reachability and dispatch, rather than repaired independently in each layer.

### Invalidate caches on lifecycle changes

The entity caches key primarily on tick counts and do not subscribe to logout, scene rebuild or world-view replacement. They check the tick before checking whether a player still exists. Inventory also retains its static nonempty list until another container update. These are lifecycle risks requiring targeted transition tests; this audit did not prove a specific live transition fails.

Reset on the relevant lifecycle events, distinguish uninitialized from valid-empty state, and consider a scene/session generation for entity references. Publish related fields together; individual volatile fields do not guarantee a coherent multi-field snapshot. Apply that principle to the local-position/world-view fields in `Rs2PlayerStateCache` as well.

### Normalize waits and action outcomes incrementally

Use monotonic elapsed time consistently; Global currently mixes wall-clock and monotonic deadlines. State what timeout, cancellation, missing state and client-thread misuse return. Avoid making null/false/zero simultaneously stand for legitimate state and failed access where callers need to distinguish them.

Preserve current APIs as compatibility wrappers. A small result enum or an additional confirming method can help selected multi-step operations; a new service framework is unnecessary. The walker's existing state-returning methods are an example of offering detail without forcing it on all callers.

### Tighten lifecycle ownership

`Script.shutdown()` cancels two known futures but does not itself stop the per-instance executor or every task a subclass might submit. It also resets shared helpers, so simultaneous script ownership needs an explicit policy. This is an API lifecycle concern, not proof every existing subclass leaks tasks. Define restart behavior and which component owns each executor/task before changing shutdown semantics.

### Align documentation and guardrails with the supported API

The long query guide frequently demonstrates name-filter queries ending in ordinary `nearest()`/`toList()`, while the terminal guardrail recommends batching these evaluations. `IEntityQueryable` exposes no `countOnClientThread()` or `nearestReachableOnClientThread()` equivalents. Guide examples should compile and demonstrate the intended threading/lifetime contract.

The existing query guardrail deliberately excludes `api/` and `util/` and focuses on name-filter terminals. Passing it does not establish that cache construction, model access or widgets are safe. Add focused boundary tests rather than treating the scanner as a complete concurrency proof.

`Rs2PlayerModel` inherits query interaction methods but its `click` methods always return false. Mark this limitation prominently at the query API surface; avoid implying player interactions are supported merely because a generic method is present.

### Optimize after correctness

Ordinary name/distance queries may bridge to the client thread per entity. Scene-wide ground-item/object caches poll every tile when invalidated, and tile-object reads copy the list even when current. These costs merit measurement, but are not proof of a current performance regression.

First fix the unsafe boundaries, then measure query latency, bridge calls and allocation rate. Prefer batching, immutable snapshot reuse and filtering by cheap IDs before expensive live properties. Avoid speculative spatial indices or a broad query-framework rewrite.

## Suggested repair order

1. Add regression coverage and fix reachability, cache thread confinement and pause/cancellation ordering.
2. Correct inventory dispatch failures, missing-widget handling and request-local wait ownership.
3. Correct boat cache keys, distance units, world-view identity and value/name predicates.
4. Add lifecycle-transition and stale-entity tests; finish the public threading and result contracts.
5. Update guide examples and broaden boundary guardrails, then profile before optimizing.

Keep patches small and independently verifiable. Do not combine these repairs with a rewrite of the utility layer.

## Validation performed

Ran with JDK 17:

```text
./gradlew.bat :client:runUnitTests --tests '*AbstractEntityQueryableInterruptionTest' --tests '*Rs2PlayerStateCacheTest' --tests '*QueryableTerminalGuardrailTest' --console=plain
```

Result: **BUILD SUCCESSFUL**, 16 tests passed: 5 query interruption tests, 10 player-state cache tests and 1 queryable guardrail. The client compile task was UP-TO-DATE; test sources compiled successfully with existing deprecation/Lombok warnings. The guardrail reported zero matching violations. The first sandboxed attempt could not download Gradle; the approved retry succeeded.

These existing tests do not reproduce the new findings; the regression cases listed above are proposed work. No live interactions, gameplay changes or runtime-log validation were performed. No production or test source was edited; only this report was added. Existing user changes were preserved.

During final review, additional changes appeared in build configuration and RuneLite files outside this audit. They were left untouched. Test results describe the completed run, not a fresh validation of those subsequent changes. The report passed its whitespace check; the whole working tree has unrelated whitespace findings in PluginDescriptor.java.
