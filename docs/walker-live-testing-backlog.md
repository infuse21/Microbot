# Walker live-testing backlog

Updated: 2026-09-09. Scope: the incremental NavigationEngine migration on `Fix-The-Walker`.
This is the live-acceptance ledger; [walker-unification-plan.md](walker-unification-plan.md)
retains detailed historical traces, and [walker-transport-batches.md](walker-transport-batches.md)
tracks implementation batches. Later dated evidence supersedes earlier audit counts and deferrals.

## How to interpret this ledger

- **Headless:** policy, resource, banking, scanner or engine tests passed; no physical crossing proved.
- **Runtime metadata:** a running client verified definitions, classification or availability only.
  This is not a successful journey, even if an older note calls it rebuilt-client acceptance.
- **Representative live:** the named route/protocol passed. Other destinations, directions,
  warnings, locks and equipment variants are not automatically accepted.
- **Pending implementation:** not headless-complete; do not place it in a live-only queue.

The user explicitly revised Phase 6 acceptance on 2026-09-09: implementation and headless
verification may close Phase 6 without live tests requiring unavailable items, stats, unlocks
or endgame equipment. Those tests remain deferred here, never labelled live-passed. Do not
restart or issue gameplay input merely to close these entries. This supersedes older wording
that made every deferred physical gate a Phase 6 blocker; unresolved implementation defects
and unsupported-route decisions still block closure. No Phase 7 or legacy deletion starts
before the implementation/headless completion audit passes. Counts below are scoped to their named historical batches, not
additive totals; source rows, generated edges and duplicated approaches must not be mixed.

## Latest work versus earlier headless batches

### Mounted Digsite and Xeric's menus - 2026-09-10

All three mounted Digsite pendant and four mounted Xeric's talisman destinations are
headless-complete under NavigationEngine ownership. Tests cover base/non-selected objects
opening Teleport menu, exact unlocked selection, struck-through/unavailable destinations,
destination-specific transformed-object actions, actual room-object approach and directed
landing retention. Compilation, focused POH tests, both thread-safety guardrails, benchmark
and both Checkstyles pass with unchanged 903/0 guardrail baselines. No rebuilt client was
available, so physical room layouts and all seven destination landings remain deferred,
not live-passed. Nexus, jewellery box and POH fairy ring/spirit tree interaction integration
are still pending implementation; the graph-generator correction is recorded below.

### POH fairy-ring/spirit-tree graph generation - 2026-09-10

The shared house network generator is headless-complete. It now connects every unique external
origin to the house and the house to every unique destination while retaining the original
permutation endpoints, so source-only and destination-only requirements cannot contaminate the
opposite leg. Tests pin two origins, two destinations, all four house edges, exact item
requirements and outbound de-duplication. Actual POH fairy-ring and spirit-tree interaction
ownership remains pending implementation; no physical house network test is claimed.

### Mounted glory and mythical cape - 2026-09-09

Four mounted glory destinations and the mounted mythical cape now use the same direct POH
portal lifecycle as chamber portals. Cache definitions confirm 13523's Edgeville, Karamja,
Draynor Village and Al Kharid actions, and 31986's Teleport action. These decorative objects
resolve only in the current house/world view; the engine approaches their actual room tile.
Headless coverage includes all five classifications, exact actions/IDs, missing or wrong
object/action/world rejection, and engine approach. The shared scene test now covers 45
direct POH destinations including the earlier chambers. Mounted digsite/Xeric's menu flows
were subsequently migrated above; nexus, jewellery box and house network generation remain
implementation work. Physical
crossings and layout-specific approaches remain deferred, not live-passed.

### POH chamber portals - 2026-09-09

Typed `PohTransport` chamber destinations now publish `TELEPORTATION_PORTAL` ownership,
using the existing engine portal lifecycle rather than `PohTransport.execute()` dispatch.
The 40 enum destinations retain their configured endpoints and expose stable object/action
identity. Resolution requires being inside a house and the player's current world view;
it checks base or active transformed IDs and the destination's exact action, never Toggle
or Remove. Grand Exchange also resolves through a Varrock-preferred base portal. The
object's template tile, rather than the configured exit-anchor tile, drives approach readiness.
Source unloading is not arrival; the common portal scanner waits for the selected landing.
Physical approach across room layouts/floors, diary variants, wilderness destinations and
arrival timing remain deferred, not live-passed. This does not implement nexus, jewellery
box, mounted facilities, house rings/trees or their graph-generation correction.
Validation: 2,212-test full-suite checkpoint (zero failures, four skips), followed by the final
approach correction's 90-test engine/portal/guardrail/benchmark run (zero failures/skips).
Compilation and both Checkstyles passed; the guardrail baseline remains unchanged.

### Ice Troll underground bridges - 2026-09-09

All five directed rows now use the shared quest-state passage lifecycle with exact cache
anchors. The north-to-boss bridge 21316 checks FRIS_TASK/3312=0 before input; four return or
east/west passage rows have no copied entrance/task/item gate. This supersedes the earlier
five-row implementation deferral. Pending physical states: first boss-entry cutscene, return
crossings before/after victory, east/west passage, post-quest accessibility and precise landing.
No fighting or quest solving is implemented, and physical cases remain deferred, not live-passed.
Validation: selected quest-state scene/policy, audited-shortcut, route, disabled-row audit and
benchmark tests passed with compilation and both Checkstyles (1m58s).

### Eastern Ice Troll quest-time entrance - 2026-09-09

Three approaches have six exact variants for quest varbit 3311=300 or 310, handled through
the existing quest-state passage lifecycle. Exact anchor (2401,3889,0), Open/Cave/21584 and
the selected underground landing are retained; requirements are rechecked before dispatch.
No gear banking, troll combat or quest solving is performed. Deferred live: initial cutscene
and repeat entry at these stages. Later corpse-return stages are not represented by this
landing, and the five underground bridge directions still require implementation decisions.
Validation: selected route/source-audit/benchmark and quest-state scene/policy tests passed,
including all 17 variants of the shared quest-state family; compilation and both Checkstyles
passed (2m12s). Those 17 include earlier families, not 17 new Ice Troll routes.

### Tears-cave tunnels - 2026-09-09

Six member-only approaches use catalog ownership and exact cache anchors for Enter. No travel
item or quest completion is required. Caller/plugin remains responsible for light in the swamp
caves outside the Chasm; the Chasm itself needs none. The original per-row durations (0/2) are
preserved rather than treated as measured timings. Headless tests exercise every approach,
reject wrong-position objects, retain source-side pending interactions and require the exact
remote destination. Deferred live checks: actual landing/timing in both directions and onward
navigation. These physical checks do not block Phase 6 under the revised acceptance scope.
Validation: 79 selected route/source/scene/audit/benchmark tests, compilation and both
Checkstyle tasks passed (1m9s); all six actual scene-dispatch approaches were exercised headlessly.

### Old School Museum upstairs exits - 2026-09-09

Four historical Leave approaches now use exact catalog ownership. Offline cache metadata places
31892 at (3013,9951,1) and (3066,9951,1), bridge flag 2/effective plane 0; downstairs 47316 is
actionless and is not a substitute. No item withdrawal or quest solve is required. Tests cover
all four approaches, exact cache anchors, decorative-ID rejection and destination acknowledgement.
Pending live: each exit's actual surface landing and normal onward navigation on a rebuilt client.
Cache geometry and mocked dispatch do not prove the historical destination tiles are exact.
Validation: 78 selected tests passed, including source/scene, route classification, disabled-row
audit and pathfinder benchmark; compilation and both Checkstyle tasks passed (1m42s).

### Karuulm safe entry and protected onward travel - 2026-09-09

- **Classifier follow-up:** a failing loaded-graph test found four northern one-tile free/diary
  rock variants selecting the generic adjacent handler. They now use catalog ownership, retaining
  Karuulm-specific requirement checks. Focused route/scene/policy/benchmark tests, compilation and
  both Checkstyle tasks pass; physical testing still requires a rebuilt client.

- **Scene integration follow-up:** `KaruulmAccessSceneTest` drives all 54 loaded variants through
  the actual scene resolver/dispatcher with mocked client state. Boot routes open the inventory,
  issue one equip command, and issue no object click before worn equipment is observed. Removing
  that equipment before a subsequent traversal dispatch rejects the stale action; revoking the
  diary reward does likewise. Every boot variant participates in banking requirement analysis.
  Three Karuulm policy/scene tests and Checkstyle passed in 21s. These are simulated object/widget
  observations, not a live equipment or boundary trace.

- **Headless implementation:** 24 directed contracts now publish 54 disjoint resource variants:
  four safe elevator entries, six rock returns and four descending stairs are equipment-free;
  six outward rock crossings and four ascending stairs each have stone/Brimstone/granite boot
  variants or the claimed elite-diary reward exemption (`7932>0`). Boots stay worn for onward
  travel; the walker never confirms an unprotected heat warning. Existing cave exits are unchanged.
- **Ownership:** exact catalog policy, shared engine-owned equipment preparation, dispatch-time
  diary/real-skill recheck and bounded directed landing. Elevator/stair acknowledgement accepts
  their explicitly paired catalog landing tiles; rock crossings retain the exact opposite tile.
  Ordinary item planning sees each boot variant; normal bank configuration remains authoritative.
- **Evidence:** the [dungeon](https://oldschool.runescape.wiki/w/Karuulm_Slayer_Dungeon) separates
  the safe chamber from heat outside it; the bundled quest helper supplies the elevator anchor.
  Equipment levels and the directed source contracts are recorded in the batch inventory.
- **Validation:** fail-first resource regression found only 12 active rock rows instead of the
  planned 54 variants. Compilation, 47 selected policy/equipment/readiness/benchmark/guardrail
  tests and both Checkstyle tasks passed (39s). The skill-read loop remains directly inside its
  client-thread callback; no new guardrail exemptions. Loader count is 6,139 origins.
- **Live pending:** all distinct elevator/stair/rock protocols, actual protected/unprotected
  boundary behavior, diary reward exemption, bank-only boot preparation, retained footwear and
  landing bounds. Headless source geometry is not a physical heat-boundary or arrival trace.

### Canifis tavern trapdoor - 2026-09-09

- The two object-5055 surface approaches now publish post-`In Search of the Myreque=FINISHED`
  catalog transitions. Dispatch rechecks the quest and uses the existing exact-object `Open`
  resolver; no synthetic second trapdoor action or legacy movement owner is introduced.
- Pending interaction clears only at `(3477,9845,0)`, not when the surface object disappears.
  The existing ungated ladder exits are unchanged. Historical ungated entry comments stay disabled.
- Evidence: [trapdoor](https://oldschool.runescape.wiki/w/Trapdoor_(Canifis)) identifies 5055/Open
  as cellar access; the [quest guide](https://oldschool.runescape.wiki/w/In_Search_of_the_Myreque)
  describes unlocking the return route by leaving through it before the final conversation.
- Physical Open/landing acceptance remains open. The precise earlier quest-stage unlock remains
  unverified; this post-quest slice is not proof of full mid-quest support.
- Compilation, six selected Canifis/unsupported-source/Evil-Dave/benchmark tests and both
  Checkstyle tasks passed (27s). The new resource test first failed with zero active entries.

### Latest full-suite result

The post-Canifis/staff-option full run executed 2,197 tests: one failure, four skips (1m 38s).
`UdsHttpServerTest.authTokenAccepted` exceeded its 15-second deadline while reading the socket;
all other tests passed, including the walker tests. The six-test UDS class passed in isolation
immediately afterward (3s), without source changes. This does not erase the full-suite failure or
establish its cause. Client-thread/queryable guardrails reported 903/0 known violations and no
regressions. Do not describe this worktree's latest full run as green.

### Earlier passing full-suite checkpoint

Full-suite checkpoint after the Brimhaven/rune batches, staff integration and withdrawal-config
guard: 2,193 tests, zero failures/errors, four skips. XML reports were inspected and Gradle
confirmed runUnitTests, compileJava and both Checkstyle tasks successful/up-to-date on
2026-09-09. Physical-live entries below remain open.

### Multi-cast combination-rune planning audit - 2026-09-09

- **Reproduced and corrected headlessly:** `getMissingTransportItemIdsWithQuantities`
  previously merged all spell costs before `planRuneWithdrawals` chose combination runes. This lost
  cast boundaries: Varrock needs one fire rune and Watchtower needs two earth runes; with
  air/law supplied separately, their journey consumes three lava runes, not the two obtained
  by covering the merged fire=1/earth=2 deficits. Combination runes supply both elements
  within one cast, not across separate casts
  ([Wiki](https://oldschool.runescape.wiki/w/Combination_rune)).
- **Implementation:** ordered cast requirements and a private physical-rune reservation map
  replace merged elemental deficits. The collector disables combination expansion while retaining
  inventory, pouch and equipped supplies. All potentially withdrawn runes are considered before
  the first cast, since combination runes fetched for a later cast can be consumed earlier.
  Equipped infinite base-element supplies remain non-consuming. Only the physical shortfall is
  converted to coordinator inventory targets; pouch counts are not added to those targets.
- **Regression scope:** unlike/repeated spells, one-cast dual-element benefit, finite bank stock,
  inventory/pouch shortfall, staff supply and bank closure without a target leg on insufficient stock.
- **Validation:** compileJava, all 56 banking tests and both Checkstyle tasks passed, with zero
  failures/errors/skips; the final combined run took 39s. No client restart or gameplay performed.
- **Still open:** rebuilt-client cast consumption, especially inventory/pouch priority when multiple
  different combination-rune types overlap. The planner prioritizes combination runes, then coverage
  and stock; this tie-breaking is not yet verified against every server consumption ordering.
  Banked-staff equipment/restoration is a separate unimplemented gate.

### Banked elemental-staff integration boundary - 2026-09-09

- **User-selected default:** `Use banked elemental staffs` is now an opt-in option immediately
  below `Walk with banked transports`. It defaults off: spell banking requests runes rather than
  an extra elemental staff. Bank-only staff eligibility and collection require both options;
  each actual elemental-staff withdrawal rechecks the opt-in. Already-carried equipment and
  restoration of an acquired transaction remain available. Enabling the option deliberately
  leaves the reusable staff in inventory after restoring the original weapon.

- **Unavailable preparation regression:** the runtime attempted the spell command after an
  acquired staff transaction's next preparation observation returned null. A fail-first runtime
  test reached the cast callback with the original weapon still observed. The runtime now feeds
  missing/mismatched preparation into the retained transaction as unavailable equipment, retaining
  the bounded wait instead of bypassing preparation. The test also verifies casting resumes after
  a valid equipped-staff observation. This is headless evidence, not a live timeout reproduction.
  Compilation, all 89 selected banking/equipment/journey/config tests and both Checkstyle tasks
  passed in 1m 5s, with zero failures/errors/skips. The full-suite checkpoint predates this fix.

- **Connected headless journey:** `BankedStaffJourneyTest` now runs the pure staff selector,
  bank coordinator and NavigationEngine runtime in one scenario. Starting with no carried
  requirements, it withdraws one air staff plus one fire and law rune, closes the bank before
  final-route setup, equips once, casts once, observes landing, restores the original weapon
  once and completes. Final inventory retains the reusable staff and no spent runes.
  The regression and test Checkstyle passed (39s; compilation up-to-date). This adds coverage
  beyond the 2,193-test full-suite checkpoint above, not a new full-suite result. Bank operations,
  route publication and game observations are simulated: production eligibility/collector/scene
  integration, actual bank approach, inventory-event timing and physical travel remain unproved
  by this test. Do not close the bank-only-staff live gate from this result.

- **Pending implementation:** `PathfinderConfig.isTeleportationSpellUsable` checks bank runes,
  but `Rs2Magic.addBankRunes` does not provide banked-staff elements. A withdrawal-only change
  therefore cannot make a staff-dependent route eligible. Selection must be shared with banking
  and honor actual equip requirements. `Rs2Staff` now has immutable equip-level/membership metadata
  and a pure `canEquip` predicate for all 25 elemental staffs: four basic (no level/member gate),
  ten battlestaffs (30 Attack/Magic), ten mystic staffs (40 Attack/Magic), and Twinflame
  (60 Magic, no Attack requirement). `NONE` and unknown/plain battlestaff IDs are not candidates.
  Sources: [elemental staffs](https://oldschool.runescape.wiki/w/Elemental_staff) and
  [Twinflame](https://oldschool.runescape.wiki/w/Twinflame_staff). This is prerequisite metadata,
  not enabled banked-staff routing or completed equipment execution.
  Compilation, all 60 selected staff/banking/guardrail tests and both Checkstyle tasks passed
  in 1m 26s, with zero failures/errors/skips and unchanged guardrail baselines (903/0).
- **Selection core implemented, not yet wired:** `BankedSpellEquipmentPlanner` prefers the
  current weapon/rune plan whenever affordable, then considers only owned, equipable staff IDs.
  Each candidate must cover the whole cast sequence with the available physical rune/bank stock;
  missing law or other catalytic runes still rejects the plan. Inputs separate the current weapon's
  rune contribution from independent equipment supplies, preventing a new staff from inheriting
  the replaced staff's infinite elements. Results contain immutable staff/rune-withdrawal choices.
  The caller still needs coherent snapshots, capacity/restoration checks and engine-owned stages;
  no production route eligibility or equipment interaction uses this core yet.
  Compilation, 67 selected banking/staff/guardrail tests and both Checkstyle tasks passed in
  1m 16s, with zero failures/errors/skips and unchanged 903/0 guardrail baselines.
- **Equipment decision core implemented, not yet wired:** `SpellEquipmentTransaction` retains
  the selected staff and original weapon/offhand independently of a route generation. Confirmed
  snapshots distinguish equip, ready-to-cast, restore-weapon, remove-staff, restored, wait, conflict
  and stop. Empty original weapon slots require removal plus inventory space; manual equipment
  changes are not overwritten. Cancellation returns stop without destroying the immutable obligation.
  The runtime must distinguish an observed empty slot from an unavailable cache snapshot.
  The decision model alone does not dispatch equipment actions.
  Compilation, all 71 selected banking/staff tests and both Checkstyle tasks passed in 1m 15s
  with zero failures/errors/skips. No client input or restart was performed.
- **Engine retention added, runtime still unwired:** an engine-supported session can retain one
  immutable equipment transaction independently of `pendingInteraction`. Route-generation changes
  keep it and pause navigation for restoration; both spatial and explicit arrival refuse completion
  while it remains outstanding. Cancellation issues no cleanup input, and a replacement request
  inherits the obligation before it can walk. Restoration acknowledgement requires the current
  request/generation, the same transaction instance and confirmed original weapon/offhand values.
  Snapshot views retain immutable historical values. No production staff stage acquires this
  obligation yet, so this does not enable banked-staff routing or claim physical restoration.
  Compilation, 151 selected engine/banking/guardrail tests and both Checkstyle tasks passed in
  1m 29s; zero failures/errors/skips and unchanged 903/0 guardrail baselines.
- **Restoration command regression covered:** the unfinished engine command path initially
  recorded attempted equipment input only in `recordInteractionPreparation`, allowing normal
  command results to request repeated restoration clicks. A fail-first test reproduced
  `INTERACT` instead of `WAIT`; `recordCommandResult` now records the equipment attempt without
  contaminating route-interaction acknowledgement state. Tests cover accepted/rejected dispatch,
  confirmed original equipment, unavailable snapshots, stale generations and cancellation.
  All 151 selected engine/banking tests passed with zero failures/errors/skips. This is headless
  command-path coverage only: the production equipment adapter, cast stages and bank-aware
  route eligibility remain unfinished, and no live restoration is claimed.
- **Route-generation follow-up:** a second fail-first regression showed restoration could
  issue input using the retained generation while the observation supplied a newer plan.
  Restoration now yields to normal route validation/installation whenever the observed plan
  is missing or has a different request/generation. Additional tests prove bounded timeout
  retains the obligation for a fresh request, an originally empty weapon slot needs removal
  capacity, and confirmed manual loadout changes relinquish ownership without overwrite.
  All 154 selected engine/banking tests passed with zero failures/errors/skips.
  Adapter integration must stage tab preparation separately: the existing inventory helper
  can switch tabs and issue item input in one invocation. No adapter or live gate is closed here.
- **Restoration adapter and tab stages implemented, not connected:** `Rs2SpellEquipmentScene`
  reads both item containers in one client-thread observation, distinguishes missing state from
  empty slots, rechecks the loadout before dispatch and resolves an actual equip action rather
  than assuming Wield. Engine restoration now separates inventory/equipment-tab preparation from
  item input, with independent once-only attempts and the existing bounded acknowledgement wait.
  Final validation: compilation, 84 selected banking/retention/guardrail tests and both Checkstyle
  tasks passed in 30s, with zero failures/errors/skips and no baseline expansion. The runtime
  dispatch boundary still needs review before wiring
  this adapter: it currently holds its mutex while calling interaction helpers. No production
  staff obligation is acquired yet; cast staging, eligibility and live acceptance remain open.
- **Restoration runtime connected:** the walker now supplies the equipment observation and
  dispatch adapter through `WalkerActions`. The runtime reserves an equipment attempt under its
  mutex, then dispatches outside it with a permission predicate covering request identity,
  cancellation, generation and transaction identity. A reserved attempt prevents concurrent passes
  from issuing another command; cleanup releases the reservation even on exceptions. Equipment
  tab callbacks check permission on the client thread, and item helpers check before invocation.
  A bounded headless concurrency test proves another thread can enter the runtime and cancel
  during adapter dispatch, revoking permission without losing the restoration obligation.
  The prior unconnected-adapter note is superseded; acquisition/equip/cast staging and bank-aware
  spell eligibility are still unfinished. No staff-dependent route or live acceptance is enabled.
- **Runtime selection snapshot added:** `Rs2SpellEquipmentScene.plan` feeds the pure selector
  with physical inventory/pouch runes (no equipment or combo expansion), independent worn-tome
  supply, owned inventory/bank staff IDs, real equip levels and world membership. Bank lookup is
  conditional on the supplied bank-planning flag; zero-quantity/noted bank entries are excluded.
  The old weapon's infinite supply is added only by the evaluated candidate, not inherited by
  its replacement. A headless scene test checks the bank toggle, bank-only staff, catalytic-rune
  withdrawal and depleted bank staff. Compilation, 86 selected banking/retention/guardrail tests
  and both Checkstyle tasks passed in 30s with zero failures/errors/skips; baselines remain 903/0.
  This snapshot bridge is not yet called by production spell eligibility or withdrawal collection;
  enable those together with engine-owned acquisition/equip/cast stages, not independently.
- **Engine preparation sequence implemented, production caller still pending:**
  `prepareSpellEquipment` can replace only the current engine-owned spell interaction with
  tab/equip commands. It acquires the immutable original-loadout obligation, limits preparation
  attempts and permits casting only after the supplied equipment observation reports the staff.
  A cleared spell landing now requests restoration before retiring the interaction. A headless
  sequence covers open, equip, observed staff, cast, landing, original-weapon restoration and
  completion. Its initial final assertion incorrectly kept republishing the retired cleared
  interaction; the corrected test follows the existing scanner contract and removes it after
  retirement. Compilation, 162 selected engine/banking/guardrail tests and both Checkstyle tasks
  passed in 23s with zero failures/errors/skips and unchanged 903/0 baselines. The runtime must
  still feed the preparation candidate and dispatch equip-stage actions before bank-aware staff
  routing is enabled. This is not a live cast or a closed banked-staff gate.
- **Production preparation dispatch connected:** `SpellEquipmentPreparation` binds the selected
  transaction and observed loadout to the spell's generation, raw edge, coordinates, type and
  display action. The walker supplies this candidate outside the runtime mutex; the runtime
  applies it only to the matching engine decision. The scene selects carried staff/runes only,
  preserves a retained transaction after equipment changes, and dispatches tab/equip stages with
  current equip-level and permission checks. A runtime test proves one equip command, no cast
  while the old weapon remains observed, then one cast after staff acknowledgement. Compilation,
  163 selected engine/banking/guardrail tests and both Checkstyle tasks passed in 23s, zero
  failures/errors/skips and unchanged 903/0 baselines. Earlier pending-production-caller notes
  are superseded. Bank-aware eligibility and coordinated staff withdrawal remain disconnected;
  no bank-only-staff journey or physical acceptance is claimed.
- **Bank integration connected, physical acceptance still open:** migrated spells may use the
  shared staff selector when unified navigation is enabled. The withdrawal collector selects
  against the complete cast sequence and requests one missing reusable staff plus the selected
  rune shortfall; carried/worn staffs are not withdrawn again. Legacy-engine configuration keeps
  the rune-only path. Initial focused compilation/tests/Checkstyle passed; broader and physical
  bank-only-staff journey validation remains open.
- **Bank-setting authority clarified by the user:** actual walker withdrawals require the current
  `walkWithBankedTransports` setting. Direct banked-walk entry falls back to ordinary walking
  when it is off, including `forceBanking` calls (that flag overrides efficiency, not permission).
  The production withdrawal callback rechecks the setting for every item, so switching it off
  during a transaction prevents further withdrawals. The unified-engine toggle is not banking
  authorization. Read-only requirement planning and use of already-carried equipment are separate.
- The earlier direct-cast-only implementation boundary is superseded by the engine-owned staff
  preparation/restoration integration above. Whole-journey acceptance must still verify retained
  identity while equipment changes the enabled transport snapshot; isolated equipment tests are
  not proof of a complete bank-only-staff journey.
- Required checks include already-worn staff, bank-only staff, insufficient equip levels, drained
  levels, full inventory, previous two-handed weapon/offhand, initially empty weapon slot, spell
  failure, cancellation and replan. Do not publish banked-staff availability before these execution
  stages exist, or switch equipment in the banking coordinator outside NavigationEngine ownership.

### Brimhaven entrance pipe - 2026-09-09

- **Implementation:** existing object-21728 northbound row no longer requires 22 Agility;
  southbound toward moss giants still does. No new ordinary duplicate or ownership handoff.
- **Evidence:** Wiki asymmetric requirement and transport-free collision connectivity;
  fail-first resource regression detected the original return-direction level requirement.
- **Headless validation:** compilation, all 19 selected Brimhaven/benchmark tests and both
  Checkstyle tasks passed in 1m 25s; zero failures, errors or skips.
- **Live pending:** both directions, below-22 return and below-22 inward exclusion, plus
  interaction/landing acknowledgement. No client restart or gameplay performed for this change.

### Eastern Brimhaven stepping stones - 2026-09-09

- **Headless:** corrected the four existing agility rows: 56 Agility
  toward the island, none away. Exact source-side 19040 anchors, current-level recheck and
  exact landing replace generic identity/proximity handling. Ordinary duplicates stay disabled.
  Compilation, 36 focused tests, benchmark, unchanged guardrail and both Checkstyle tasks passed.
- **Evidence:** transport-free collision-map connectivity establishes the island endpoints;
  wiki and cache definitions establish asymmetry and active versus scenery IDs.
- **Live pending:** both crossing pairs in both directions, below-56 departure from the island,
  boosted-level drain before inward dispatch, continued walking and any observed failed crossing.
  Unknown damage/failure behavior is not claimed as accepted.

### Full-suite checkpoint - 2026-09-09

**Latest rerun passed:** 2,143 tests, zero failures/errors, four skips, both Checkstyle tasks,
3m 27s. UDS smoke tests now have a test-only 15-second deadline and all six passed in isolation.
The production server is unchanged; the intermittent socket stall's cause remains unproved.
This supersedes the incomplete checkpoint described below. Live gates are unchanged.

First run: 2,143 tests, two stale Weiss source-test failures, four skips. The source expectations
were corrected to enforce the migrated contracts, and all 50 focused Weiss/northern/engine tests
plus Checkstyle passed. The full rerun stalled in the Agent Server UDS test's socket read and its
verified Gradle worker was stopped. A clean full-suite rerun is still required; no client restart,
gameplay, production-server change or commit was made during this checkpoint.

### Opened quest-state passages - 2026-09-09

- **Headless:** Zogre's crushed barricade, Slug Menace's wall/passage pair,
  and both Enakhra rubble directions use exact object/anchor/state and landing checks. Five
  directed contracts have eleven state variants. No quest setup actions are automated.
  Compilation, 25 focused tests, benchmark, unchanged guardrail and both Checkstyle tasks passed
  in 1m 42s, with zero failures/errors/skips.
- **Live pending:** cross in each supported direction/state; confirm locked/absent transforms
  are rejected, the reverse Slug passage remains usable for escape, and a quest-state refresh
  before dispatch prevents stale input. Exercise continued walking and cancellation.
- These are travel contracts, not combat protection: Jiggig contains disease-inflicting enemies
  and the Slug tunnel contains aggressive lobsters. The separate imposing door is not migrated
  by this entry. Malformed bone rows and the central Grand Tree trapdoor remain implementation work.

### Quest trapdoor follow-up - 2026-09-09

- **Headless:** Basement of Doom entry now publishes 12268/Go-down with
  completed Shadow of the Storm, membership and the original directed landing. The exact
  12267/Open object is preparation, not arrival; the same catalog interaction advances to the
  open object. Quest state, identity, action and anchor are rechecked before dispatch.
  Compilation, 110 focused tests, benchmark, unchanged guardrail and both Checkstyle tasks passed
  in 1m 45s, with no failures/errors/skips.
- **Live pending:** closed and already-open entry, quest-locked refusal, cancellation between
  opening and descending, actual underground landing and continued walking after descent.
- **Still implementation work:** Grand Tree and Canifis surface Open outcomes/unlocks. The
  cache does not establish that those static Open objects use Evil Dave's two-object protocol.
  Do not enable them by broadly treating every Open trapdoor as either arrival or preparation.

### Molch coloured barriers - 2026-09-09

- **Headless:** all 20 directions across five barriers use exact catalog
  ownership, colour-specific object identity and a current-HP check before dispatch. Four
  previously disabled rows used the mistyped lava-scenery ID 34542; the actual Molch ID is 34642.
  The other 16 directions no longer bypass damage checks through the adjacent-transport handler.
  Compilation, all 89 combined focused tests, benchmark, unchanged client-thread guardrail and
  both Checkstyle tasks passed in 1m 10s. The scene test covers 20 directions across 11 states.
- **Live pending:** rebuilt-client crossings in both directions and both lanes of each barrier,
  green/orange/red states, colour-boundary crossings, counter decay and low-HP refusal. The
  next-crossing budgets (0/10/20) conservatively include a possible colour increase; exact
  server damage timing remains unproved. Confirm source-side/mid-barrier positions cannot
  acknowledge crossing and that adjacent barriers sharing transformed IDs are never selected.
- Karuulm stairs/elevator remain separate implementation work. The safe entrance chamber does
  not justify gating every elevator visit on boots, while equipping only at a later stair is too
  late to protect a route that already crossed the hot floor. No gameplay was issued for this batch.

### Revenant fee discovery - 2026-09-09

- **Pending implementation, not live-only:** all 16 deferred crevice-40386 approaches. The fee is
  100,000 coins and can be deducted directly from the bank. Capture exact initial warning/payment
  frames with no carried coins and sufficient bank coins, then already-paid entry with neither
  carried nor bank coins required. Establish how paid status is exposed; warning varbit 6506
  must not be treated as a receipt. Invalidation must distinguish cave deaths / Wilderness PvP
  deaths from ordinary deaths elsewhere. Do not deliberately cause a death merely to inspect this
  flag; use an independently observed transition or authoritative protocol evidence.
- No coins were withdrawn, spent or exposed to Wilderness travel by this audit. It does not
  authorize a live run under the current headless-first instruction.

### Ungael side passages and Weiss cliff chain - 2026-09-09

- **Headless:** four Ungael side passages require `6108>29`, matching the
  cache transforms unlocked after quest Vorkath is defeated. These are not crater-entry objects
  31990/31822. All ten Weiss directions have exact anchors and landings; ascent requires 68 Agility
  and current HP above 15, while descent has no Agility/HP gate. The installed rope requires
  `6528>44`. All Weiss directions respect the agility-shortcut switch.
  Compilation, 142 focused tests (including scene, lower-stage recovery, config, classification,
  benchmark and guardrail) and both Checkstyle tasks passed in 1m 35s; no guardrail exemptions added.
- **Live pending:** cross all Ungael directions after the unlock; verify locked refusal before it.
  Exercise the complete Weiss ascent/descent, one rope fall to below the first rockslide, one ledge
  fall to below the rope, and a source-side failure. Confirm replans start at the lower stage, never
  repeat the later obstacle, and do not acknowledge a fall as arrival. Verify low-HP refusal and
  installed/uninstalled rope behavior. The 15-HP margin comes from the documented rockslide damage;
  it does not promise protection from unrelated hazards or replace physical failure observations.
- **Not implemented:** installing the rope or solving either quest; those are quest-helper actions.

### Main Brimhaven entrance - 2026-09-09

- **Implemented:** seven approaches, each with disjoint paid, already-paid and permanent-access
  variants; exact base/live-object identity, single-visit-only menu selection and scoped receipt
  continuation under NavigationEngine ownership. Paid variants use shared bank planning.
- **Live pending:** freshly rebuilt client, a banked 875-coin visit, already-paid entry without coins,
  permanent access without coins, and the three-option payment menu when carrying at least one
  million coins. Confirm only 875 coins are deducted, no permanent purchase is selected, the receipt
  advances at most once and actual underground arrival clears the leg. Check all approach geometry,
  particularly the easternmost approach three tiles from the object's anchor.
- **Still implementation work:** the two conflicting main-exit rows; neither destination is asserted
  as a successful crossing. First-time permanent purchase is not part of this transport protocol.

### Active graph, instance scene boundary and large agility objects - 2026-09-09

- **Headless:** all 6,117 currently loaded transport origins now classify into an explicit
  NavigationEngine-owned family; no active edge remains generic `TRANSPORT`. This does not include
  the 88 genuine disabled traversals that still need complete protocols. Shared two-way instance
  normalization now covers ordinary doors, catalog transitions, adjacent transports and exact
  mineable/object probes; synthetic chunk mappings test live-to-template and template-to-live.
- **Live pending:** in a rebuilt client, repeat a GOTR/instanced layout where a closed ordinary door
  precedes stairs and verify the door is issued first. Exercise one instanced catalog object and one
  exact obstacle probe, confirming interaction tiles and arrival acknowledgements remain in template
  space. Repeat the Draynor underwall route through
  object `19032` from a northern approach and verify `Climb-into`, directed landing and continued
  walking without a legacy marker. Cancel a catalog scan with Ctrl+X and verify it exits without a
  client-thread timeout exception.
- **Regression context:** the earlier underwall trace had agility enabled and routed to
  `(3065,3260,0)`, but its two-tile object search could not see the large object anchored three tiles
  away. The new five-tile allowance applies only to agility transitions and retains exact catalog
  identity checks.
- **Not complete:** the active classification boundary is not Phase 6 closure. Disabled equipment,
  safety, quest-state, payment, stochastic and staged interactions remain implementation work, and
  all previously listed representative live gates remain deferred unless explicitly accepted.

- **New equipment batch:** four Smoke Dungeon well approaches, two Troll Stronghold uphill rocks
  and four Trollweiss sled slopes are headless-complete with exact quest/skill/equipment contracts,
  shared bank planning, staged equipment and directed landing. Live-test each only in a prepared
  account state; the walker enters/crosses and does not solve the associated quests.
- **New Ghosts Ahoy rock batch:** ten members-only ship-rock directions require 25 Agility, wait
  nonblockingly for 5% run energy, dispatch the exact `Jump-To` object and accept only the exact
  opposite tile. Live-test the full five-jump chain at sufficient energy, then one below-5% wait;
  confirm a damaging failure still advances only after the opposite landing.
- **New Ice Path gate batch:** five inward gate rows require Desert Treasure I progress and unlock
  varbit `382>1`; six outward rows are unconditionally usable from inside. Live-test one direction
  each and verify exact landing; the walker does not claim route-wide protection from extreme cold.
- **New Royal Trouble plank batch:** eight post-quest stepping-stone directions require one
  reusable bankable plank and stage `select plank -> Use on rocks` under NavigationEngine ownership.
  Live-test the four-rock chain in both directions, including a banked-plank route, and verify a
  click is never acknowledged before the exact opposite tile.
- **New Royal Trouble ropeswings:** both post-quest directions require 40 Agility and permanent
  installation varbit `2147=1`, and complete only at their exact directed landing. Live-test each
  direction; first-time rope installation is quest-helper work and is not claimed by the walker.
- **New failure-retry batch:** both Lighthouse broken-bridge and Karamja wooden-log directions
  remain pending after a source-side failure and clear only at the exact directed landing. Six
  Regicide stick directions additionally require current HP above their 8-damage maximum before
  every attempt. Live-test one failure and success for each restored family in prepared conditions.
- **New leaf-pit recovery batch (2026-09-09):** four directed leaf jumps are restored with exact
  near-side object selection, Regicide/Agility requirements and current HP above 18 before jumping.
  The [Wiki](https://oldschool.runescape.wiki/w/Leaves_(trap)) documents damage and same-side return;
  an offline copy of the local game cache identifies `Climb;Protruding rocks;3927` in four two-by-two
  pits starting at `(2313,9656)`, `(2336,9656)`, `(2354,9656)` and `(2354,9643)`, all plane zero.
  NavigationEngine retains the jump while resolving only that pit's rocks, climbing out and verifying
  the original surface tile before retry. Recovery does not depend on the damaged player's jump row
  remaining available. Missing/rejected/timed-out recovery stops; five failed jumps exhaust the budget
  only after escape. Exact opposite landing, not recovery or object disappearance, clears the route.
  - **Headless coverage:** scene resolution/dispatch, foreign pit/object/action rejection, HP boundary,
    both directions, transient missing scene, fall/escape/retry, five-failure limit, failed climb,
    cancellation and requirement loss after escape. No new thread-safety exemptions.
  - **Live pending:** confirm the actual pit position and original-side surface return for both
    directions of each physical trap, success followed by route continuation, below-19-HP refusal
    after a damaging fall, and cancellation during recovery. Cache inspection is not a live crossing.
- **New Darkmeyer wall batch:** six unique directions require 63 Agility, Sins of the Father
  completion and both installed-rope varbits. The walker matches only the corresponding installed
  live wall transforms, never withdraws long rope, and requires exact landing. Live-test the two
  wall segments and at least one alternate eastern approach after permanent setup.
- **New Spirits of the Elid crevice batch:** twelve approaches require quest stage 40 (or quest
  completion), one consumable rope and one reusable lit light source. Bank planning independently
  sums ropes and selects one carried, worn or banked light source, while NavigationEngine owns the
  exact `Climb-down` and underground landing. Live-test one in-progress and one post-quest descent,
  including a banked rope/light setup; the walker enters the cave and does not solve the quest.
- **New Cerberus winch batch:** five members-only approaches require currently boosted 91 Slayer,
  a positive assignment count and an exact Hellhounds or Cerberus task. The refresh cache tracks
  both the task target and boss subtype, the scene snapshot rechecks access immediately before
  `Turn`, and only the exact lair landing clears the edge. Live-test a normal Hellhounds assignment
  and a boosted-level boundary; the walker enters the lair and does not own combat or survival.
- **New Lumbridge Swamp entrance batch:** four permanently roped dark-hole approaches require
  installed-state varbit `279=1`, one reusable gas-safe light and the exact underground landing.
  The mutually exclusive `6533=1` variant removes the light requirement after the Lumbridge Fire of
  Eternal Light is built. When `279=0`, the engine stages and consumes one bankable rope before the
  same descent. Live-test one banked rope/light setup, one installed entry, one permanent-fire entry,
  and confirm open flames are never selected.
- **New Grim Tales manhole batch:** four approaches publish only with permanent unlock varbit
  `3718=1`, dispatch exact `Enter;Manhole;24842`, and clear only at `(2901,9867,0)`. Live-test one
  unlocked entry and the existing staircase exit; the walker does not solve the quest or own combat.
- **New southern Brimhaven backdoor batch:** four rope approaches and one crevice exit require
  members access and varbit `5629` in the cache's enabled range 1-3. Exact object/name/action checks
  support wrapper 66 and rope transform 30200; static exit 30201 is also unlock-gated. Scene discovery
  and dispatch both check current state, and only the exact directed landing completes the edge.
  Live-test locked refusal, unlocked entry and exit, plus continued walking after each landing.
  The one-time 5,000-trading-stick Banisoch purchase is not implemented or charged per traversal.
- **New Brimhaven metal-dragon passage correction:** both crevice-30198 directions are direct,
  members-only catalogue transitions with exact landing. Hieve's documented Slayer-task restriction
  applies to attacking dragons, not ordinary passage. Test both directions without a task in a
  rebuilt client and verify continued walking; this does not grant or automate dragon combat.

### Steps, access routes and audited shortcut cleanup - 2026-09-08

- **Headless:** exact Steps/climb obstacles, access doors/exits, quest gates and four Wiki-audited
  boundary families now remain NavigationEngine-owned until their directed landing is observed.
  The production classifier floor is 97 ordinary plus 23 item rows, 120 total.
- **Not live-pending:** unsafe task-only, failure-prone, setup-dependent, quest-time and duplicate
  shadow rows were removed from the runtime graph. They need explicit protocols before restoration.
- **Live pending:** representative crossings for each distinct migrated protocol, including the
  one-tile Myreque fence, quest-gated crevices/gates and exact-ID Draynor bookcase transformation.

### Audited boundary, boss-exit and unsafe-access cleanup - 2026-09-08

- **Headless:** 25 exact tree/root/mud/fence/opening/odd-wall and outward boss-exit transitions are
  engine-owned with exact identity, requirement and directed-landing contracts.
- **Historical implementation remainder:** four equipped-sled slopes, five Resource Area payment
  rows, one malformed remote odd wall and four ungated southern Brimhaven ropes are runtime-disabled.
  The slopes and Resource Area rows have since been restored with explicit protocols; the earlier
  claim that the Resource Area fares were reversed was incorrect. Remaining rows need unlock protocols.
  Current classifier floor: 181 ordinary plus 23 item rows, 204 total.
- **Live pending:** traverse each migrated family in both available directions where applicable;
  for Cerberus and Scorpia, test only prepared outward exits and verify exact landing with no legacy
  handoff. These are hazardous locations and this batch adds no combat or death protection.

### Haunted Mine, Slayer Tower and unsafe-environment cleanup - 2026-09-08

- **Headless:** five glowing-fungus Haunted Mine stairs, five post-quest lifts and eight level-61
  Slayer Tower chains are exact engine-owned transitions. The chain contract treats its documented
  damage failure as a successful plane transition only after the exact destination is observed.
- **Not live-pending:** 32 source-disabled rows omit failure recovery, asymmetric skill gates,
  light/rope/protective-equipment state, quest state, heat safety, or describe an unusable museum
  prop. Restore them only with those protocols encoded. Current classifier floor: 218 ordinary
  plus 23 item rows, 241 total.
- **Live pending:** traverse the fungus stairs and lift in both directions, then a medium Slayer
  Tower chain including a damage failure, verifying exact landing and no legacy handoff.

### Access doors, cart tunnels and hazard-route pruning - 2026-09-08

- **Headless:** eight access doors, eight Haunted Mine cart tunnels and two Fortis Colosseum lobby
  entrances are exact engine-owned transitions with their members, skill, quest and unlock gates.
  Ten unsafe door/state rows and eight light/fire-hazard cave tunnels are runtime-disabled with
  Wiki/cache MCP evidence and exact source-shape tests.
- **Live pending:** traverse each migrated family, including Ranging Guild entry/exit, the Dorgesh
  and Forthos doors, each Haunted Mine entrance family and a Colosseum lobby entrance. Verify exact
  landing and no legacy handoff. Disabled state/hazard rows are pending implementation, not live
  coverage. Current classifier floor: 268 ordinary plus 23 item rows, 291 total.

### Weiss post-quest routes, guide NPCs and stale portal duplicates - 2026-09-08

- **Headless:** seven safe post-quest Weiss routes and ten deterministic Dorgesh/Elkoy/Auburn guide
  routes are exact engine-owned transitions. Five wrong-action Runecrafting portal duplicates and
  four unsafe Weiss ledge/rope rows are source-disabled; canonical Runecrafting `Use` exits remain.
- **Live pending:** traverse one cleared Weiss cave plus one fallen-tree/little-boulder route, each
  Dorgesh guide destination family, Elkoy in both directions and the Auburn guide in both directions.
  Verify the final landing is acknowledged once with no legacy handoff.
- **Not live-pending:** wrong-action portal duplicates and unsafe Weiss rows are not migrated
  coverage. Current classifier floor: 304 ordinary plus 23 item rows, 327 total.

### Unsafe access gates, Weiss rockslides and remaining item audit - 2026-09-08

- **Headless:** five God Wars boulder, five Cerberus winch and six Weiss rockslide rows are
  runtime-disabled with Wiki/cache MCP evidence and exact zero-load tests. Their source contracts
  omitted mandatory skills/tasks or asymmetric failure and damage recovery.
- **Implementation pending:** the 23 remaining item rows are 20 Max cape, two Camulet and one
  Hunter cape Black-chinchompa route. Capture and implement their grouped menus, settings, charges,
  shared daily counter and scoped Wilderness confirmation before live testing them.
- **Not live-pending:** the 16 disabled ordinary rows are unsupported protocols, not migrated
  coverage. Current classifier floor: 327 ordinary plus 23 item rows, 350 total.

### Unsafe incomplete shortcut cleanup - 2026-09-08

- **Headless:** OSRS Wiki MCP evidence and source-shape tests cover the runtime removal of ten
  Ghosts Ahoy rock jumps, eight Darkmeyer wall rows, eight Royal Trouble plank rows and six
  Regicide stick rows. Their source entries omit required skills/unlocks/items or damage and
  failure recovery, so they cannot safely participate in routing.
- **Not live-pending:** these 32 rows are unsupported protocols rather than migrated coverage.
  Restore them only after the required setup, gating, interaction staging and recovery semantics
  are implemented and headlessly verified.

### Post-quest tunnels/caves and stale entrance cleanup - 2026-09-08

- **Headless:** seven completed-quest tunnels, four western Ice Troll Cave entrances and four
  completed-Myreque wooden doors are exact engine-owned members transitions. Three invalid eastern
  Ice Troll entrances, two ungated Shade Catacombs doors and 14 obsolete Brimhaven payment/state
  rows are runtime-disabled with source evidence.
- **Live pending:** traverse each migrated quest family once after completion and verify exact
  landing with no legacy handoff. Modern Brimhaven payment/permanent access and shade-key use are
  pending implementation, not live-only tests; removed stale rows are not migrated coverage.

### Wintertodt gaps, Enakhra barriers, and ambiguous Kharazi cleanup - 2026-09-08

- **Headless:** six level-60 Wintertodt pillar gaps and six completed-Enakhra magic barriers are
  exact engine-owned transitions. Sixteen ambiguous Kharazi rows are runtime-disabled because each
  source click claimed two different destinations; 76 deterministic jungle inputs remain loaded.
- **Live pending:** cross the Wintertodt gap in both directions including one failed jump, and cross
  an Enakhra barrier after quest completion, verifying exact landing and no legacy handoff.
  Ambiguous Kharazi rows are removed coverage, not live-pending transitions.

### Ice Queen rock slides, Stronghold tunnel, and Weiss exits - 2026-09-08

- **Headless:** six canonical Ice Queen rock slides are engine-owned with completed Heroes' Quest,
  50 Mining and reusable-pickaxe banking; four malformed shadows are runtime-disabled. Both
  level-72 Stronghold Slayer Cave tunnel approaches and five completed-quest Weiss hole approaches
  are engine-owned, with the tunnel also respecting the agility-shortcut toggle.
- **Live pending:** cross each family once with exact requirements, verify the selected landing,
  normal recovery through the rock-slide delay, toggle rejection for the tunnel, and no legacy
  handoff. The four malformed rock-slide rows are removed coverage, not pending live tests.

### Killerwatt and unlocked Catacombs passages - 2026-09-08

- **Headless:** two completed-Ernest Killerwatt rifts, three varbit-unlocked Forthos passages and
  one varbit-unlocked Giants' Den passage are exact engine-owned members routes.
- **Live pending:** cross one Killerwatt rift and each passage family in both available states,
  verifying locked routes are absent, exact landings complete, and no legacy handoff occurs.

### Direct outward exits, Camdozaal, and Ice Path safety - 2026-09-08

- **Headless:** six exact members-only outward dungeon exits and both F2P Camdozaal boundary rows
  are engine-owned; Camdozaal requires completed Below Ice Mountain. Eleven Ice Path gates are
  runtime-disabled because their extreme-cold/stat-drain route needs a route-wide safety contract.
- **Live pending:** cross representative outward exits and both Camdozaal directions, verifying the
  exact interaction and landing with no legacy handoff. Ice Path is pending implementation, not a
  live-only test and not migrated coverage.

### Outward rope-exit transitions - 2026-09-08

- **Headless:** 15 exact fixed-landing rope exits are engine-owned across Lumbridge Swamp Caves,
  Crandor, Water Ravine/Elid, Giant Mole and God Wars Dungeon. Eleven members flags were corrected;
  the four Crandor exits remain free-to-play. Reverse rope installation and entrance protocols are
  excluded.
- **Live pending:** exit one representative route in each area and verify one exact interaction,
  the selected surface landing, interruption recovery and no legacy handoff. Use minimal equipment
  in aggressive areas; this cutover does not claim any inward rope/light/quest setup.

### Witchaven, Wyvern Cave and Miscellania climb-up exits - 2026-09-08

- **Headless:** all seven exact members-only `Climb-up;Exit` rows are engine-owned: one Witchaven
  Dungeon row, two Fossil Island Wyvern Cave rows and four Miscellania/Etceteria dungeon rows.
  Entrance-only quest, task and unlock gates are deliberately not imposed on players leaving.
- **Live pending:** exit each dungeon family once and verify one exact interaction, the expected
  remote surface landing, interruption recovery and no legacy handoff. The Witchaven dungeon can
  contain aggressive monsters; use an intentionally minimal loadout.

### Ferox, Wilderness Slayer and Isle of Souls opening exits - 2026-09-08

- **Headless:** all 11 exact members-only `Exit;Opening` rows are engine-owned: three Ferox
  Enclave dungeon rows, seven Wilderness Slayer Cave rows and one Isle of Souls Dungeon row. Nine
  missing members flags were corrected, and exact directed keys retain each remote surface landing.
- **Live pending:** exit each dungeon family once and verify one exact interaction, the expected
  surface landing, recovery from combat/external movement where applicable, and no legacy handoff.
  Use an intentionally minimal loadout for the Wilderness Slayer Cave test.

### Karamja Volcano entrance and return transitions - 2026-09-08

- **Headless:** all eight exact surface `Climb-down;Rocks;11441` rows and all four dungeon
  `Climb;Climbing rope;18969` rows are engine-owned. The immutable route keys retain the selected
  cross-plane landing and reject unrelated objects or newly added requirements.
- **Live pending:** traverse one surface approach and one dungeon return, verify exact landing,
  normal recovery if combat or external movement interrupts the dungeon side, and no legacy
  handoff. This tests only the public volcano boundary, not the deeper Dragon Slayer/Crandor route.

### Chronicle and Jaltevas charged-container teleports - 2026-09-08

- **Headless:** Chronicle now has exact inventory/worn `Teleport`, a corrected Wilderness ceiling
  of 0, read-only saved-charge planning, bank-only availability when the saved charge count is
  positive, and one reusable-container withdrawal. All Pharaoh's sceptre destinations accept both
  charged IDs 26948/26950; Jaltevas additionally requires cache-confirmed necropolis unlock varbit
  13839, and repeated edges require only one charged sceptre.
- **Live pending:** test Chronicle from inventory, equipment and bank with positive, zero and unknown
  saved charges; verify it is never selected in the Wilderness. Test both charged sceptre IDs,
  Jaltevas locked/unlocked states, depleted-charge rejection, exact landings and no legacy handoff.

### Calcified moth and Slepe medallion teleports - 2026-09-08

- **Headless:** Calcified moth 29090 now uses exact inventory `Crush`, remains consumable, requires
  completed Perilous Moons in the conservative catalog row, and cannot route above level 20
  Wilderness. The reusable Slepe medallion row uses exact `Slepe` inventory/equipment actions and
  requires cache-confirmed `slepe_teleport_unlocked` varbit 12416 to equal 1. Banking sums moths per
  use and requires only one medallion across repeated edges.
- **Live pending:** test a bank-only moth to Cam Torum and inventory, equipped and bank-only Slepe
  medallions; verify consumption/reuse, exact landings and no legacy handoff. The valid partial-
  Perilous-Moons moth branch remains unavailable until its precise stage predicate is encoded.

### Mor Ul Rek hot-vent doors - 2026-09-08

- **Headless:** all 11 exact Inner Mor Ul Rek object-30266 `Pass` rows are engine-owned and
  members-only. Normal/trouver Fire cape and Fire max cape IDs form one reusable alternative group,
  participate in bank planning, and are rechecked at dispatch. Fight Pits hot-vent IDs are excluded.
- **Live pending:** cross representative directions with a carried, worn and bank-only cape; verify
  one reusable withdrawal across repeated barriers, exact landing and no legacy handoff. The Wiki
  says the cape becomes unnecessary after the first successful entry, but no reliable local unlock
  variable is known, so unlocked cape-free routing remains deliberately unavailable.

### Water Obelisk grapple and barehand shortcut - 2026-09-08

- **Headless:** the exact one-way object-17062 route is engine-owned with its corrected live
  `Grapple` action. One variant requires the documented 36 Agility / 39 Ranged / 22 Strength plus
  an equipped mith grapple and compatible crossbow; a separate variant requires 72 Agility and no
  tools. Reverse travel is absent rather than inferred.
- **Live pending:** cross from the Water Obelisk toward Catherby once with equipped tools and once
  barehanded; verify exact landing, no reverse edge, equipment-loss rejection before input, normal
  completion if the grapple breaks during a successful crossing, and no legacy handoff.

### Already-equipped grapple shortcuts - 2026-09-08

- **Headless:** 11 exact `Grapple` routes are engine-owned only while mith grapple 9419 is in the
  ammo slot and a compatible crossbow is equipped. Route publication and dispatch both recheck the
  equipment; inventory/bank-only tools, lost equipment and excluded weapon classes cannot enable
  the route. Wiki MCP confirms both equipped items and the grapple break chance.
- **Live pending:** cross representative raft, wall, rock and Strong Tree links with an intentionally
  minimal loadout; verify both directions where present, exact landings, 1-in-25 break recovery and
  no legacy handoff. This slice does not equip/restore tools or claim barehand variants; the exact
  Water Obelisk variants are covered separately above.

### Unlimited Ardougne Farm cape teleport - 2026-09-08

- **Headless:** the exact Farm row now accepts only unlimited item IDs 13124/20760, with inventory
  `Farm Teleport`, worn `Ardougne Farm`, reusable banking and directed landing ownership. Wiki MCP
  confirms both variants provide unlimited farm-patch teleports.
- **Live pending:** test inventory, worn and bank-only forms; verify one reusable withdrawal,
  exact farm landing and no legacy handoff. Limited cloaks 2/3 remain deliberately unavailable
  until their daily-use state can be observed reliably.

### Abyss guaranteed passages - 2026-09-08

- **Headless:** all 12 exact object-26250 Passage configurations are engine-owned with a frozen
  directed manifest, no fabricated requirements and exact acknowledgement of their three possible
  inner-ring landings. Wiki MCP confirms Passage is guaranteed, tool-free and present opposite the
  initial blockage in each of the 12 random configurations.
- **Live pending:** with an intentionally minimal loadout, traverse representative configurations
  for each distinct landing and verify exact object/action identity, uninterrupted movement,
  directed arrival and no legacy handoff. The outer Abyss is aggressive multicombat territory;
  headless support is not a safety claim or a request for immediate physical testing.

### Fairy Resistance Hideout sequence - 2026-09-08

- **Headless:** all 53 generated `AIR DLR DJQ AJS` fan-in rows are engine-owned after Fairytale II
  completion. Exact intermediate AIR/DLR landings, DJQ's intentional no-movement transition, final
  AJS landing, one retained interaction and final-only weapon restoration are covered.
- **Live pending:** run the full four-code chain from a non-POH ring after quest completion; verify
  each interface transition and landing, no direct-last-destination shortcut, no command advance
  before acknowledgement, final weapon restoration and no legacy handoff. Quest-time certificate
  access is deliberately not implemented by this slice.

### Max-cape duplicate cleanup - 2026-09-08

- **Headless:** one exact duplicate Black-chinchompa route was removed; 21 unique Max-cape rows
  remain legacy-owned. Reachability is unchanged and no menu behavior is claimed.
- **Pending implementation:** variant-specific inventory/equipment nested menus, POH portal submenu,
  exact label aliases and Wilderness confirmation still need a dedicated Max-cape protocol.

### Mythical cape - 2026-09-08

- **Headless:** usable cape IDs 22114/24855 now use exact inventory and worn `Teleport` actions.
  Inert POH trophy ID 21913 was removed from the banking requirement and remains non-executable.
- **Live pending:** test one inventory and one worn cape, verify exact guild landing, reusable item
  retention and no legacy handoff. This does not cover a mounted cape inside a POH.

### Mokhaiotl waystone - 2026-09-08

- **Headless:** the sole usable item-31099 `Channel` row is engine-owned only with completed Final
  Dawn and its exact consumable/member/Wilderness/destination contract. Inert 31101+ display items
  and equipment actions are rejected; repeated banking still consumes one real waystone per use.
- **Live pending:** use one waystone from inventory on an eligible account, confirm one `Channel`
  input, exact underground landing, item consumption and no legacy handoff.

### Burning amulet Wilderness teleports - 2026-09-08

- **Headless:** Chaos Temple, Bandit Camp and Lava Maze are exact engine-owned item teleports. The
  captured definitions prove all five charged variants expose the exact inventory subactions and
  worn actions. The pending edge owns the scoped `Okay, teleport to level` warning and clears only
  at its directed landing; final-charge disappearance cannot acknowledge success.
- **Live pending:** with a deliberately minimal risk-free loadout, test one inventory and one worn
  use, including a final-charge transition if practical. Capture the actual warning text, one
  destination input, one affirmative input, exact landing and no legacy marker. Do not treat the
  headless result as permission to carry valuable gear into the Wilderness.

### Alternate-destination teleport spells - 2026-09-08

- **Headless:** all ten exact alternate rows are engine-owned: eight House `Outside`, Varrock GE and
  Watchtower Yanille. Exact requirement shapes and option/identifier mappings are pinned; malformed
  colon rows are rejected and repeated House casts aggregate Air/Earth/Law bank requirements.
- **Live pending:** perform one House Outside and one GE or Yanille cast on a rebuilt client, confirm
  one alternate input, exact landing, engine-owned retention and no legacy handoff. This does not
  cover Inside/Group house actions or advanced POH facilities.

### Molch/Lizardman Temple one-way links - 2026-09-08

- **Headless:** all 16 exact dwelling/exit approaches are engine-owned, including strict destination
  pairing for shared dwelling ID 34403. No unlisted sibling ID or reverse route is inferred.
- **Live pending:** enter each of the three represented dwellings, use the southwest Strange hole
  exit, confirm directed landings and no legacy handoff. Treat the nearby shaman barriers as their
  separate already-migrated damaging protocol.

### Castle Wars random Guthix portal removal - 2026-09-08

- **Headless:** all 12 object-4408 two-destination rows are removed because one server-selected
  random-team input cannot satisfy a directed landing promise. Deterministic 4387/4388 portals still
  reach both waiting rooms; the generic portal corpus is now 88/88 engine-owned.
- **Live:** no Guthix click is required for removed routing data. Existing deterministic portal
  representative acceptance remains the relevant protocol evidence. Empty head/cape eligibility is
  a separate correctness follow-up, not inferred from this removal.

### Swan Song island hole - 2026-09-08

- **Headless:** both exact directions for object 12656
  retain the completed-Swan-Song gate and NavigationEngine ownership through the directed landing.
  The 12 Dragon Slayer quest-in-progress ruin-hole aliases are deliberately not included. An
  isolated client build passed all 84 focused classifier/policy/scanner/scene tests.
- **Live pending:** cross both directions, confirm exact object/action resolution, five-tile landing,
  no premature retirement and no legacy handoff.

### Enakhra temple secret entrances - 2026-09-08

- **Headless:** 16 exact exterior approaches across directional boulder IDs 11045-11048 are
  NavigationEngine-owned only after Enakhra's Lament is finished. The existing interior sand-pile
  exits stay unrestricted; no quest-time opening action is claimed.
- **Live pending:** enter through each directional boulder, confirm the exact live object/action,
  directed temple landing and no legacy handoff, then verify an interior sand-pile exit remains
  usable. Partial-quest access needs a proven stage/unlock predicate before it can be supported.

### POH Outside teleport tablets - 2026-09-08

- **Headless:** all eight item-8013 `Outside` rows are engine-owned only when house-location varbit
  2187 selects their exact exterior. The captured item definition proves the direct inventory action
  and absence of an equipment action; consumption, wrong-exterior rejection and repeated banked
  quantity are covered. The remaining item-teleport legacy count is 49.
- **Live pending:** with a house location set, place all tablets in the bank, force/select the banked
  route, verify one withdrawal and exact `Outside` input, then confirm exterior landing and no legacy
  handoff. Do not interpret this as inside-house or facility acceptance.

### Runecrafting altar exit portals - 2026-09-08

- **Headless:** all 16 fixed `Use;Portal` exits for the Mind, Water, Earth, Fire, Body, Cosmic,
  Nature, Chaos and Blood temples are exact-manifest NavigationEngine transitions. Tests reject
  foreign geometry, missing source objects and cross-acknowledgement between the three distinct
  Chaos portal landings.
- **Live pending:** use representative ordinary and quest-accessed altar exits, including each
  distinct Chaos origin, and confirm exact live object/action identity, surface landing, one command,
  and no legacy handoff. Entry to an altar is not implemented or implied by this exit-only slice.

### Meiyerditch course traversals - 2026-09-08

- **Headless:** 66 exact directed links are NavigationEngine-owned: 18 floorboards, six floors, 20
  rubble/crawl-wall/rock/shelf/washing-line rows, seven persistent prepared-floor climbs, five
  tunnel/barricade rows and ten post-quest access rows. The first 56 require 26 Agility and partial
  Darkness of Hallowvale; prepared floors additionally require knockdown varbit 2589 equal to 1.
  The post-quest ten require the quest finished and include six Ver Sinhaza entrances, two push-wall
  directions and two western-wall climbs. Intermediate positions, failed obstacles and missing
  objects cannot retire an edge.
- **Live pending:** traverse representative pairs in both directions and chain through each of the
  five newly added object families. Include an obstacle failure and toggle-off route exclusion, and
  confirm no following interaction is issued before the exact catalogue destination.
- **Pending implementation:** no row from this audited 66-row corpus remains legacy-owned. Quest-time
  prepared-floor knockdown, initial knife-wall opening, barricade discovery/search and other quest
  solving remain outside the traversal contract. The western-wall IDs in this slice are distinct
  from the modern rope-built Darkmeyer shortcut and have no supported level-86/rope gate. Do not
  treat traversal after access as quest or whole-course completion.

### Abyss exit rifts - 2026-09-08

- **Headless:** eleven exact inner-ring rifts are engine-owned with directed altar landings and
  their existing Cosmic/Death/Blood quest gates. Foreign geometry remains unsupported and an
  absent rift cannot acknowledge travel from the origin.
- **Live pending:** verify representative unrestricted and quest-gated rifts from the inner ring,
  exact live names/actions, actual altar landing spread and no next-edge command before arrival.
  Arrange any Wilderness approach and outer-ring traversal separately; this batch starts at the
  safe inner ring and adds no combat protection.
- **Pending implementation:** Law remains locked until Entrana prohibited-equipment handling is
  encoded and tested. Soul remains locked until its dark-essence item-on-rift protocol and
  consumption/banking semantics are represented; neither is a live-only deferral.

### Audited agility traversals - 2026-09-08

- **Headless:** all 28 loaded rows across the Nature Grotto bridge, Agility Pyramid entrance
  rocks, Rellekka Hunter-area handholds and GWD/Wilderness handholds are NavigationEngine-owned.
  Tests cover exact identity/geometry, requirements, toggle filtering, missing objects, foreign
  origins, near-destination positions and exact landing. The GWD family contains one exact duplicate.
- **Live pending:** test both directions of the Nature Grotto bridge, including a failed jump that
  lands on the opposite bank; both Pyramid rock sets below/at their level boundaries; both
  Rellekka handhold object variants; agility-toggle exclusion; and the GWD one-way descent only
  with deliberate Wilderness/GWD preparation. Confirm no next edge is issued before exact landing.
- **Safety boundary:** no chill mitigation, combat escape, quest solving or whole-area safety was
  added. The level-35 Rellekka gate follows the current wiki despite a stale local level-1 shortcut
  entry and should be checked against the rebuilt client before being called live-complete.

### Misclick recovery reuses the forward route - 2026-09-07

Headless implementation: the first destination mismatch attempts a nearby forward raw-route
rejoin instead of immediately recalculating. The runtime collision reachability check rejects
unreachable correction targets; rejected/unacknowledged commands retain the replan fallback.
Pending observed interactions and non-walking edges in the correction segment prevent shortcutting
the route. A fresh plan with known unowned movement can issue a correction while still moving.
Destination matching now uses the forward route suffix, not already traversed route sections.
Live pending after rebuilt-client restart: sideways/backward misclick, blocked rejoin, and misclick
near a door/transport; verify correction without waiting for the mistaken walk to finish, no
skipped interaction, and fallback only when needed. Unknown destination signals still wait
conservatively; this does not override genuine transport movement or implement manual cancellation.

Validation: 422 focused navigation/transport tests passed, including changed expectations for
immediate correction, rejected correction fallback, preservation of pending obstacles and the
stale-destination acknowledgement window. The latter exposed plan installation clearing the
observed destination; retaining that observation fixed the reproduced immediate second correction.

### Combat must not own route-interaction waits - 2026-09-07

Three headless regressions reproduced WAIT instead of INTERACT when an unrelated actor interaction
was active before dispatch or past the command deadline. NavigationEngine now waits on its own
command state, not the player's generic interacting flag; existing movement settling and transport
acknowledgement windows remain intact. All 418 selected navigation/transport tests passed.
Live pending after rebuilt-client restart: attack/auto-retaliation near a route object must not
prevent dispatch or extend the expired stationary command wait; also verify genuine dialogue and
transport waits. Auto-retaliate settings and ordinary-ground movement timing were not changed.

### Fremennik surface bridges - 2026-09-07

- **Headless (423 focused regressions passed):** ten directed surface bridges, strict support identity and exact
  landing. The two mine-shortcut rows require 40 Agility and the agility toggle; corrected action
  is Walk-across. Physical checks must verify both directions, landing coordinates, before/after
  repair action availability and no next-edge click while still crossing.
- **Live pending:** mine shortcut below/at level 40 and toggle-off exclusion; ordinary surface
  bridges must remain available without the agility shortcut toggle. Account for hostile trolls
  when arranging a later safe test; this batch does not add combat protection or repair actions.
- **Pending implementation:** five underground bridge rows, including the Ice Troll King boundary,
  require encounter/access review. They are not included in the ten migrated surface rows.

### Isafdar logs and tripwires - 2026-09-07

- **Headless:** six log and eight tripwire rows are catalogue-owned with strict object identity
  and exact landing. Logs enforce 45 Agility, partial Regicide access and the agility toggle.
- **Live pending:** both directions and actual landing coordinates; denied log requirements and
  toggle-off exclusion; failed log crossing and tripwire damage/poison must not acknowledge
  arrival without reaching the destination. Verify bounded recovery without a premature next edge.
  Arrange hazard-safe test conditions explicitly; this batch adds no poison treatment.
- **Superseded:** the four leaf-pit crossings now have headless recovery ownership; see the
  2026-09-09 leaf-pit batch above for the outstanding physical acceptance cases.

### Short agility crossings and Revenant cleanup - 2026-09-07

- **Headless:** four Varrock trellis/jagged-wall crossings with exact landing and unchanged
  level/quest gates. Live checks: both directions, denied requirements, failed wall jump and
  no premature next-edge click. Legends' Quest in-progress access is not newly enabled.
- **Headless data cleanup:** ten typed Revenant pillar rows remain after removing eight older
  ordinary variants. Verify toggle-off exclusion, level boundaries, destinations and bind/freeze
  rejection later; no Wilderness travel performed in this batch.
- **Pending implementation:** replace the obsolete Champions' Guild four-stone chain after
  verifying the newer single-stone object and landing coordinates in the rebuilt client.

### Tarn jumps and full agility scope - 2026-09-07

- **Headless:** 46 exact pillar/ledge jumps, strict object identity and exact landing. Live checks
  need representative pillar-to-pillar and pillar-to-ledge jumps, both planes, and a trap-induced
  fall that must not issue the next jump. Trap disarming/avoidance remains pending implementation;
  do not perform hazardous testing merely because direct traversal is headless-supported.
- **Pending implementation:** full shortcut/course coverage, including the 74 rooftop obstacles
  absent from transport resources. See [walker-agility-coverage.md](walker-agility-coverage.md).

### Meiyerditch floorboards and Ranging Guild - 2026-09-07

- **Headless:** all 18 floorboard Jump-to routes migrated with exact directed identities and
  landing/failure-position tests. Representative live checks still need both directions, the
  three-tile and plane-3 variants, actual object location/action, and recovery without issuing the
  next jump after a fall. Nearby quest mechanisms and whole-course access are not included.
- **Headless requirement correction only:** Ranging Guild entry requires 40 Ranged and exit
  remains free. Verify denied entry below 40 and entry at 40, plus exit after a level drain.
  Guild-door traversal itself remains legacy; do not call this a migrated gate.
- **Pending implementation:** the Ice Gate unlock, hot-vent access and ambiguous Wintertodt
  routes identified in the September 7 batch audit remain unresolved.

### Equipment-free grapple variants - wiki audit, not implemented

Checked 2026-09-06. These are documented alternate access requirements, not proof that the
old catalog Grapple action is usable without equipment. Verify the loaded client's actions,
object transformations, direction and landing before enabling separate variants. The user's
Broken Raft failure at 65 Agility remains decisive evidence against blindly enabling Grapple.

| Shortcut | Equipment-free Agility | Source |
| --- | --- | --- |
| Broken Raft / River Lum | 48 | [Broken Raft](https://oldschool.runescape.wiki/w/Broken_Raft) |
| Falador rough wall | 52 | [Rough wall](https://oldschool.runescape.wiki/w/Rough_wall_(Falador)) |
| Catherby rocks | 68 | [Rocks](https://oldschool.runescape.wiki/w/Rocks_(Catherby)) |
| Yanille south wall | 69 | [Wall](https://oldschool.runescape.wiki/w/Wall_(Yanille)) |
| Water Obelisk to Catherby (one-way) | 72 | [Crossbow Tree](https://oldschool.runescape.wiki/w/Crossbow_Tree) |
| Karamja Strong Tree | 78 | [Strong Tree](https://oldschool.runescape.wiki/w/Strong_Tree_(Karamja)) |

[Observatory rocks](https://oldschool.runescape.wiki/w/Rocks_(Observatory)) are a separate
installed-rope case: Observatory Quest, 23 Agility, 28 Strength and 24 Ranged for the documented
shortcut; first installation needs the equipment and consumes the grapple, then the rope remains.
Do not apply a generic permanent equipment requirement to its installed Climb variant. The wiki
contains inconsistent older infobox/action details for some variants; runtime protocol verification
is outstanding. Equipment-free crossings do not satisfy grapple-specific diary tasks.

### Implementation batches

| Batch / date | Implemented and tested | Physical checks still needed |
| --- | --- | --- |
| Ardougne wall doors, 2026-09-07 (headless) | Four exact 8738/8739 crossings now engine-owned with Biohazard FINISHED gate; east/west boundary retirement protected. | Post-quest Open/cross all approaches, transformed IDs and no inverse bounce. **Unresolved earliest unlock:** specific object wiki says Biohazard, city wiki says Plague City; verify before relaxing the conservative gate. No earlier quest access implementation claimed. Legacy totals 1,429 / ordinary 953. |
| Stronghold / colony wide gates, 2026-09-07 (headless) | Six Stronghold and four Swan Song-gated Piscatoris approaches; exact contracts, north/south crossing for offset approaches and engine retirement guarded against near-side progress. | Normal Open/cross both directions, transformed object lookup and no inverse bounce. **Pending implementation:** Femi first-entry dialogue and Grand Tree smuggling/restricted-entry branches; these are not claimed as supported merely because the ordinary gate rows classify. Legacy totals now 1,433 / ordinary 957. |
| Broken Raft / desert gate requirements, 2026-09-06; Shantay ownership 2026-09-08 | Raft routing and pre-dispatch require both equipped tools; paid Al Kharid action filtered after Prince Ali Rescue. All 14 exact Shantay rows are now headless engine-owned: six consumable-ticket entries, two main-gate 5-coin twins with exact NPC `Buy-pass` staging, and six free returns; Desert Elite entry is free. Unkah coin-only purchase stays gated because no local vendor record is verified. | Rebuilt-client missing-tool reroute, equipment removal/breakage memo invalidation, equipped raft crossing, pre/post-quest toll action, repeated banked tickets/fares, main-gate purchase/ticket/free-return, and representative Unkah ticket/elite crossings. No barehanded raft protocol or Unkah vendor purchase is claimed. |
| Yanille pick-lock door, 2026-09-06 (headless only) | Two exact directed 11728 rows; 82 Thieving plus one reusable bank-planned lockpick (1523). Failed picks retain the edge; lost level/item is unavailable, opened-door clearance retains engine-owned forward crossing. | Cross both directions; verify exact object identity before/after opening, failed attempts and bounded recovery, boosted-level drain, bank-only lockpick withdrawal, missing/wrong lockpick refusal and no inverse bounce. Dungeon hazards apply. Ardougne 11719/11720 are still pending implementation, not live-only. |
| Draynor basement unlocked crossings, 2026-09-06 (headless only) | 18 directed two-tile doors with exact existing lever gates; 64-combination truth-table checks, side-aware crossing and relock protection. | Actual unlocked Open/cross in both directions, exact scene identity, lever change/reset invalidation, no inverse bounce. **Only crossing migrated:** the pre-existing legacy lever solver and full puzzle orchestration remain implementation work. |
| Harmony monastery audit, 2026-09-06 (gate correction only) | Both entrance directions require destroyed-door varbit `3393>4`; earlier setup states rejected headlessly. No new engine ownership. | **Still implementation work:** transformed actions/collision, post-quest normal crossing, in-progress gas protection and instance handling. Later live tests must distinguish these states; do not treat the demolition gate as complete safety or traversal support. |
| Waterfall internal doors, 2026-09-06 (headless only) | Four key-298 requirements; one reusable bank-planned key. Two post-quest remote throne-room links now catalog-owned; two adjacent links retain adjacent ownership. Distant room coordinates preserved using bundled quest-helper evidence. | Verify exact live door IDs/actions and normal Open-with-key protocol, then cross both doors in both directions; bank-only key withdrawal; missing/wrong key rejection; actual remote room landing without legacy input. Fire giants make this hazardous. No crate search or quest puzzle solving. |
| Equipment-gated entrances, 2026-09-06 | Four Shadow Dungeon ladder approaches; Waterfall entrance/exit; Baba Yaga exit; three Dorgesh-Kaan entries (ten rows). Runtime probe confirmed all ten classify correctly. | Ring equip then visible ladder then landing; direct doors in each supported direction. Preserve quest gates. Test both visibility-ring families if available. |
| Zanaris shed, 2026-09-06 (headless only) | Three post-Lost City variants: bank-planned reusable Dramen staff, level-gated Lunar staff, staff-free elite diary; explicit equipment/menu stages. Not loaded into the running client yet. | Staff in bank/inventory/already worn; elite-diary staff-free entry; exact clue menu; foreign menu refusal; directed landing. No Lost City quest completion or automatic weapon restoration is claimed. |
| Kalphite / Crabclaw tunnel entrances, 2026-09-06 | 18 directed links, 34 setup/installed/resource variants; normal-instance action and banked consumable rope. Locked/itemless states verified through runtime metadata. | Outer and inner rope installation, already-installed descent, setup-row retained during refresh, normal not private instance, Crabclaw quest-access crossing. Hazardous. |
| Climb-down holes, 2026-09-06 | Three Royal Trouble and 15 God Wars approaches; locked states rejected in running client. | Positive Royal Trouble quest-stage descent and installed God Wars descent. God Wars first-time installation is not implemented by this slice. |
| Climb-up ropes, 2026-09-06 | 21 classified rows; four Saradomin God Wars approaches retain installed-rope gates. | Tolna, Kalphite exits, Smoke Dungeon, Saradomin ropes, Chasm, Crash Site and the prior Hunter Guild route; first-use and return states as applicable. |
| Neypotzli, 2026-09-05/06 | 25 canonical quest-gated surface/internal entrances; stale duplicate landings removed. | Surface and internal passage after Perilous Moons access is granted; correct plane and landing. |
| Frozen direct manifest, 2026-09-05 | 30 rows, exact identities. Magic Training Arena doorway passed both ways. | Nature Grotto, Tree Gnome Village huge gates, Elf Village tree gates, Polar hunting steps, Hunter Guild rope, DOTI entryway, Custodia Pass cave. Verify access prerequisites independently. |
| Enakhra exits, 2026-09-05 | 16 sand-pile exit approaches. | Climb from inside temple to the directed surface landing; do not require the unlock created by exiting. |
| Master Scroll Book, 2026-09-05 | 18 destinations; stored-charge gate, reusable banked book, exact widgets, Revenant warning. | Charged book normal destination, depletion/catalog disappearance, bank-only book, Revenant warning with a deliberately minimal loadout. |
| Ordinary NPCs, 2026-09-04 | Brother Tranquility (2), Daero (1), Waydar (2), Primio (2). | Actual direct action and landing at each distinct protocol; transformed actors and applicable quest access. |
| Mountain Guide shadows, 2026-09-04 | Six exact ordinary duplicates of the existing network. | Menu selection and landing using these coordinates, both directions, retained unlock gate. |
| Dondakan, 2026-09-04 | One gold-helmet / Talk-to / exact request / landing contract. | Banked helmet, equipment acknowledgement, conversation, mine landing. Helmet intentionally stays worn. |
| Raft exits and Hazeel, 2026-09-04 | Two Waterfall log rafts, two Ancient Cavern aged logs, two Hazeel rafts. | Each exit protocol; Hazeel correct valve state and incorrect-island bounded recovery. The walker does not configure valves. |
| Paid Al Kharid gate, 2026-09-04 | Four ten-coin pre-quest lanes/directions. | Actual paid crossing, fare consumption and no inverse bounce. Earlier free-gate acceptance does not prove payment. |
| Staged items, 2026-09-03 | First batch 99 jewellery/cape rows; second batch 44 other-item rows; public action fixtures verified in runtime. Dueling ring is representative-live. | See item matrix below; all 44 second-batch additions and non-ring first-batch families still need physical coverage. |
| Steps, 2026-09-03 | Eight Taverley upper-level, main Waterbirth-exit and Weiss-exit rows. | Each distinct steps protocol/plane change. Fourteen other Steps rows are still implementation work, not accepted by this batch. |
| Catacombs vines, 2026-09-03 | 20 exits; incorrect incoming-unlock requirements removed. | Five exit destinations and first-use unlock writes, especially Shayzien identity fallback; then corresponding surface entrance. |
| Catacombs/Corsair holes, 2026-09-02 | 13 entrances; three independent Catacombs unlock varbits corrected. | Each unlocked entrance and rejection of still-locked other locations; Corsair descent. |
| Revenant easy pillars, 2026-09-02 | Two exact 65-Agility rows. | Both directions and damage-with-success landing, only under an explicit safe Wilderness test plan. |
| Fremennik Slayer exit, 2026-09-02 | Three object-2141 approaches. | Direct one-way exit. Other 13 Enter/Tunnel rows are not covered after the expanded Trollweiss cutover. |
| Passageways, 2026-09-02 | 52 Tarn room links and Rogues' Den lobby exit. | Rogues' Den exit; representative Tarn transitions with hazard preparation. Tarn traps/pillars are not included. |
| Prifddinas gates, 2026-09-02 | 32 Enter/Exit rows with Song of the Elves completion. | Both gate actions/directions on an eligible account. |
| Myths' Guild barriers, 2026-09-02 | Ten bridge/dungeon rows with Dragon Slayer II gate. | Each distinct bridge/dungeon crossing and landing. |
| Rubber cap mushroom, 2026-09-02 | Five Fossil Island jump approaches. | Jump and landing; useful mixed route with mushtrees enabled and disabled. |
| Chasm of Fire lifts, earlier batch | 16 plane-changing rows. | Up/down lift protocols inside demon-filled dungeon. |
| Molch mystical barriers, earlier batch | 16 directed boundaries. | Both directions, one-damage crossing and persistent-object acknowledgement; shamans make this hazardous. |
| Wilderness-sword webs, earlier batch | 28 rows; one reusable banked sword alternative. | Slash from inventory, clearance then crossing, repeated boundaries. Other slash weapons/Aranea boots are not covered. |
| Kharazi and Brimhaven chopping, earlier batch | 76 deterministic Kharazi plus ten vine rows; reusable axe/machete banking. | Both tool families, repeated attempts, object transform, directed landing and bank-only tool; hazardous areas. Sixteen ambiguous Kharazi rows remain unsupported. |
| Dense forest, earlier batch | 46 Tirannwn crossings. | Representative directions, animation and landing with hazardous-route preparation. |
| Ferox barriers, earlier batch | 16 adjacent entry-barrier rows. | Both barrier IDs/directions; do not confuse with accepted Wilderness Ditch warning tests. |
| Lithkren vault barrier, earlier batch | 48 exact object-32153 rows. | Quest-access audit and representative physical crossing; original rows omit quest requirements, so classification alone does not validate access. |
| Ordinary Climb/Rocks, earlier batch | 51 catalog + four adjacent rows; the two climbing-boot routes are now headless-complete with staged equipment. | Distinct rock protocols and skill/quest variants, including one banked climbing-boots pass. |
| Quest-gated Golem/Sophanem/Trollweiss entrances, 2026-09-08 | 50 exact completed-quest rows are headless-complete. | Representative Golem portal, eastern Sophanem rock and distinct Troll Romance tunnel crossings; quest-time intermediate states remain unsupported. |
| Post-quest Crandor/Shilo entrances, 2026-09-08 | 12 Crandor hole and ten Shilo broken-cart rows are headless-complete. | Representative completed-quest crossing; pre-quest boat/dialogue/setup states remain quest-helper work. |
| Stronghold escapes/Wintertodt doors, 2026-09-08 | 14 Stronghold vine/chain rows and 16 direction-gated Wintertodt door rows are headless-complete. | Representative vertical exit and Firemaking-qualified inbound/outbound Wintertodt crossings. |
| Lithkren broken doors, 2026-09-08 | Nine exact post-Dragon Slayer II rows are headless-complete. | Representative crossing; quest-time dragon-key setup remains quest-helper work. |
| Seasonal, 2026-08-30 | 47 Clue Compass + 122 Map of Alacrity rows. | League world/items required; direct compass action, two-level map, off-screen hotkeys, locked entries, banked requirement and landing. |

## Partially live-accepted families: remaining branches

Resource Area gate 26760 (2026-09-09): all five directed fare/diary variants are headless-owned.
The user's screenshot and manual crossing confirm the 7,500-coin `Open -> exact prompt -> Yes`
flow, not rebuilt-engine acceptance. Test engine-driven ordinary paid, discounted and elite/free
entry plus free exit, insufficient coins and bank-funded entry. Only the exact fare prompt is
accepted; discounted prompt wording and direct free-entry/exit flows remain live-unverified.

| Family | Evidence already completed | Outstanding live cases |
| --- | --- | --- |
| Banking | Draynor tablet coordinator; legacy-era necklace/dueling variant withdrawal; later engine-owned two ecto-tokens + ghostspeak amulet transaction. | Repeated requirements, inventory shortfall, missing/failed withdrawal with bank close and no target leg; capes/tools, spell runes, combination runes, rune pouch and already-equipped elemental staff. Banked elemental-staff equip/restore is NOT implemented. |
| Item teleports | Dueling ring inventory Castle Wars final-charge consumption and worn Emir's Arena activation. | Direct cape aliases (including diary/Hunter/Strength), pendant/ring destination variants; second-batch cloak/gloves/legs/blessing, memoirs/Book of the dead, lyres, crystals, medallion/sceptre, GE/Yanille tablets and basalt entrance/roof. Respect charges, unlocks, daily limits and inventory-only items. |
| Minecarts | GE/Keldagrim free pair both ways. | Paid Ice Mountain/White Wolf routes, Kourend menu, weapon/shield restoration. |
| Canoes | Ferox/Edgeville pair both ways. | One-way Wilderness Pond warning; other shaping requirements/stations as representative regression coverage. |
| Balloons | Empty-equipment Entrana -> Taverley consumed one log and arrived. | Reverse Entrana restrictions, four higher-tier unlocked destinations and their log types; bank-funded logs. |
| Generic portals | Edgeville/Soul Wars both ways. | Other distinct deterministic portal contracts among the 88 migrated rows. Twelve Guthix rows are still unsupported. |
| Grouping teleports | Castle Wars activity path retained engine ownership; old HTTP radius discrepancy documented. | Three active Rat Pits destination menus; fresh strict-radius end-to-end smoke. Broken commented Keldagrim row is excluded. |
| Ships/dialogue | Squire + gangplanks, Tempoross object ferry, Molch menu, Veos/Magoro, Tobias/Customs and Barnaby representative directions passed. | Herbert four quest-gated routes; Pirate Pete two routes; Ghost Captain outbound wearable + 25-token payment; Shanks Port Sarim destination and final arrival-page cleanup. Optional coin-confirm dialogue remains unobserved; Shanks Khazard purchase/voyage did pass. |
| Shilo travel carts | Exact Pay-fare object definitions and 20 generated classifications verified. | Actual paid Brimhaven/Shilo journeys and fares in both directions. |
| Charters | Port Sarim/Catherby both ways without separate confirmation. | Optional confirmation if a live interface presents it; other distinct destination/actor variants. |
| Energy Barrier | Bank-funded paid west-to-east crossing, equip then two-token payment then arrival. | Reverse and other boundary contracts; free post-Ghosts Ahoy modern Pass; no amulet/token withdrawal after completion. |
| Mushtrees | Meadow/Verdant journey, locked-node invalidation/replan and config-off publication checks. | Other distinct destinations as available; no fabricated pre-arrival unlock varbit. |
| Fossil rowboats | Camp/Digsite return pair; unlocked sea -> camp. | Unlocked camp -> north/sea and other directions; immediate Cancel/replan with genuinely locked menu (current account now unlocked). Earlier 57-second automatic recovery is not acceptance of the faster fix. |
| Spirit trees | GE/Stronghold both ways, grey locked-menu recovery; corrected Brimhaven-disabled route reached Ardougne. | Planted-tree variants and absent-object stale opt-in automatic recovery; do not assume remote farming transmit state proves a planted tree. |
| Fairy rings | Non-POH directed travel both ways with original-weapon restoration. | Full post-quest `AIR DLR DJQ AJS` hideout chain; distinct staff-free/account-locked configurations; POH/DIQ is a separate unsupported contract. |
| Gliders/Quetzals | Stronghold chain/tree door and transformed captain; Quetzacalli/Civitas pair and locked Cam Torum filtering. | Other endpoint variants as regression coverage; the separate Quetzal-whistle destination map is headless-complete but still needs its own live pass. |
| Agility/stiles | Falador crumbling wall and Taverley stile representatives; basalt causeway all ten edges both ways. | Other distinct protocols in the 248 scene-changing agility batch; post-fix random basalt failed-jump recovery (recorded failure reproduced headlessly, subsequent crossings did not fail). |
| Core recovery/arrival | Ordinary recovery, manual displacement/input contention, exact caller-radius walk, non-consuming off-centre final approach. | 2026-09-05 moving-mouse/combat correction end-to-end; charged teleport -> exact final tile without repeat use; short-link origin/midpoint/overshoot regressions across representative physical boundaries. |

## Live gates already recorded (do not reopen as wholly untested)

The main plan records representative acceptance for ordinary walks/recovery; Motherlode rockfalls;
chained ordinary doors; free Al Kharid adjacent gates; Falador stair chains; Squire/Tempoross;
free/paid ship representatives; charters; non-POH fairy rings, spirit trees, gliders and Quetzals;
Rimmington physical POH Home/Enter; tablet and Home Teleport; levers with warning/direct return;
canoe/free minecart/portal/grouping representatives; mushtrees; magic carpet Bedabin -> Shantay;
Entrana -> Taverley balloon; Falador agility/Taverley stile; both Wilderness Ditch directions with
outbound warning; GOTR barrier and Temple of the Eye portals both ways; paid Energy Barrier;
full basalt causeway both ways; Magic Training Arena doorway both ways; Dwarven Mine trapdoor
and return ladder (2026-09-06). These results do not cover every row in those families.

## Still needs implementation or prerequisite research

Do not label these as "just waiting for live testing":

- Advanced POH facilities: chamber/nexus/jewellery box/mounted teleports/POH rings/trees and house
  destination/instance flows. The level-48 test house is empty; only physical Home/Enter passed.
- POH/DIQ fairy-ring facilities; quest-time certificate form of the Fairy Resistance Hideout;
  Random Wilderness obelisks are runtime-disabled: the retained resource contains 270 random remote
  permutations and 48 unsafe self-pad artifacts. A future deterministic hard-diary selection protocol
  is separate implementation work. The ten unsafe agility rows were removed: eight stale River Lum
  segments and malformed Mountain Camp/Observatory setup rows need separately verified replacements.
  All grapple rows and 53 post-quest multi-code rows are
  headless-complete above, and the 12 redundant random Guthix rows were removed.
- The former item-classification remainder is now headless-owned, including the Quetzal-whistle
  map, audited Max/Hunter cape destinations and both Camulet routes. Their distinct charge, daily
  limit, map-mode and Wilderness-confirmation branches remain in the live queue; this does not
  cover advanced POH facilities or banked elemental-staff equipment/restoration.
- The ten alternate spell overrides are headless-complete above; representative live acceptance is
  still pending.
- Ordinary specialised remainder: Draynor lever-solver ownership (unlocked door crossing is migrated),
  other puzzle/Stronghold question doors, Harmony gas/explosive door,
  pre-completion Waterfall chamber variants, Karuulm heat/warnings, Mor Ul Rek access, unverified tunnels/holes,
  ambiguous Kharazi/stepping stones, damaging Tarn traps and other unsupported route contracts.
- Banked elemental staff equipment/restoration; efficient per-jewellery-item charge capacity;
  Ghost Captain discount/permanent-free-unlock optimisation; quest solving and one-time unlocks.

The production classifier and exact disabled-source tests, not this prose list, are authoritative.
Pre-Zanaris checkpoint: 1,466 legacy edges, including 990 ordinary TRANSPORT edges; after the
headless Zanaris cutover: 1,465 and 989; after Waterfall internal doors: 1,463 and 987;
after Draynor unlocked crossings: 1,445 and 969 respectively; after Yanille pick-lock:
1,443 and 967 respectively. Those are historical checkpoints. The current active graph has no
generic legacy `TRANSPORT` classification: the latest verified loader has 6,127 origins with engine
ownership. After removing the active Brimhaven-stone duplicates from the disabled backlog,
74 source contracts remain unresolved; this is not a verified count of distinct missing routes. These counts are
not percentages of live acceptance or estimates of implementation effort.

## Acceptance record to append for each future run

Record date/build, exact row/protocol and endpoints, account prerequisites (without identifiers),
starting item quantities/equipment, relevant config switches, planned owner, issued stages, landing,
normal terminal result, consumption/restoration, and any human intervention. Keep a bounded redacted
trace or link to its main-plan entry. Check no legacy handoff, duplicate command, unexplained replan
or relevant error. Test positive travel and meaningful negative/locked/warning cases separately.
One representative per distinct protocol is useful; repeated approach tiles do not each need a run.
Never count a manually assisted crossing, definition lookup or returned `issued=true` as a pass.
