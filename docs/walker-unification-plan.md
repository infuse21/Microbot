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

## Handover snapshot - 2026-08-05

This workspace contains the uncommitted incremental migration; preserve the dirty worktree and do
not reset unrelated or earlier phase changes.

**Branch move (2026-08-05):** the uncommitted walker changeset was moved by stash from
`WalkerRewrite` onto `Fix-The-Walker`, a fresh branch from the 1.12.35 release. Work committed
only on `WalkerRewrite` did not come along; the pieces the walker changeset depended on were
re-applied here by hand: the `Rs2PathApi` facade swaps in `Script`, `QuestScript`, `Rs2Bank`,
`Rs2DepositBox`, `Rs2LeaguesTransport`, `Rs2NpcManager`, and `Rs2Slayer`; a stale
`setPathfinderFuture` line in the lifecycle runtime; a regenerated client-thread guardrail
baseline (lambda indices shift whenever `Rs2Walker` gains lambdas); and a direct-capture fallback
in `Rs2DoorGeometry.wallOrientations` so an empty client-thread hop cannot read as "no blocking
orientation". The `Rs2Dialogue` caching/content-gating layer (phantom-dialogue fixes,
`invalidateDialogueState`) remains only in `WalkerRewrite` commits — cherry-pick it from there if
that behavior is wanted on this branch. The full client unit suite passes on this branch.

**Current position:** Phase 6 is active. Phases 0, 1, 2, and 4 are complete. Phase 3's unified
ordinary executor is live and accepted, but its final legacy deletion/default-cutover gate remains.
Phase 5 mineables and ordinary doors/gates have completed headless and live acceptance, while their
legacy deletion gate intentionally waits for the remaining route families. Phase 6 families already
accepted in both directions are adjacent same-plane shortcuts, stairs/ladders/trapdoors/caves,
simple item teleport, direct NPC/ship travel plus gangplanks, paid coin-only charter ships, and
free dialogue-menu NPC/ship/boat travel (`NPC_DIALOGUE_TRANSPORT`, object and NPC actors both
live-proven) including its coin-paid step 2 (Port Sarim/Musa Point and Ardougne/Brimhaven
ferries live-accepted both ways; `CONFIRM` stage headless-proven, no post-Sailing coin row
prompts live). Directed three-letter non-POH fairy rings are now live-accepted in both directions,
including displaced-weapon restoration proven by equipment state. POH/DIQ fairy-ring edges remain
legacy-owned for the later POH slice. Non-POH spirit trees are also live-accepted in both directions,
including session filtering and replanning for grey locked destinations. Gnome gliders are now
live-accepted in both directions, including the three Stronghold descents and its two-tile
`Open;Tree Door` boundary after the reverse landing. Quetzals are headless-complete and awaiting
live NPC/map/landing acceptance. Next: finish that quetzal gate, then
POH, and seasonal transports, with banked-transport route setup last.

**Charter slice closed (2026-08-05):** reverse Catherby-to-Port-Sarim acceptance passed on
request 2/generation 1. The engine walked from `2809,3441,0` to the charter origin `2792,3414,0`,
issued exactly one NPC-stage and one destination-stage command (this interface presented no
separate `Yes` prompt, so the confirm stage correctly issued nothing), paid the fare (observed as
the inventory-driven transport refresh), landed within tolerance of `3038,3189,1`, chained the
Port Sarim gangplank as one `CATALOG_TRANSITION`, resumed ordinary walking, and arrived. The
whole run stayed on one request/generation with no replan, no recovery, no duplicate charter
input, and no legacy markers. Together with the forward Port Sarim-to-Catherby pass, the paid
coin-only charter family has completed both live acceptance directions.

**Fairy-ring slice closed (2026-08-24):** directed
three-letter non-POH edges publish as `FAIRY_RING`. One pending interaction advances through
optional Dramen/Lunar staff equipment, exact last-destination or configure action, individual dial
rotations, confirmation, remote landing, and original-weapon restoration. The source UI may unload
during travel; only the directed catalog landing acknowledges the teleport. Restoration forces the
inventory tab open as a separate `fairy-ring-restore-open:<id>` stage, waits for the item container,
equips through the normal inventory path, and clears only when
`Rs2Equipment.isWearing(originalWeaponId)` is true. Live acceptance passed from `3129,3496` to
`2705,3576` and back on request generations that emitted both restore stages, resumed ordinary
walking only afterward, and arrived normally. `DIQ` and configured POH-anchor edges stay
legacy-owned until POH migration. Spirit trees and gliders are closed; quetzals are the active family.

**Spirit-tree slice closed (2026-08-25):** directed non-POH
`SPIRIT_TREE` rows are migrated as an object stage (`Travel`), exact adventure-log destination stage,
and directed remote landing. The current destination interface is `947:9`; grey embedded destination
text marks configured-but-locked planted trees, which are not clicked and are removed from both
directions of the session graph before an immediate replan. Grand Exchange/Gnome Stronghold passed
in both directions. Synthesized POH rows remain legacy-owned. Policy, scanner, pathfinder publication,
navigation lifecycle, remote-edge retention, Checkstyle, and thread/query guardrails pass.

**Transport-cost calibration (2026-08-25):** path search and direct-vs-bank comparison now share
`TransportCostModel` instead of letting reconstructed `path.size()` reduce every transport to one
tick. Successful live traces establish conservative floors of 24 ticks for staged fairy-ring travel
and 12 ticks for spirit-tree menu travel; explicit larger catalog durations still win, and every
other interaction costs at least one tick. Live glider inspection confirmed its existing eight-tick
catalog duration is already conservative, so the shared model records an explicit eight-tick glider
floor. Transport debug output reports both catalog `duration` and effective `routeTicks`. Add a
measured floor for each specialised family as it is migrated.

**Gnome-glider slice closed (2026-08-27):** directed `GNOME_GLIDER` rows publish as an NPC
`Glider` stage, exact generated `InterfaceID.Glidermap` destination-button stage, and directed remote
landing. Hidden map buttons are never clicked; the destination is session-disabled in both
directions and the engine replans. Live inspection at White Wolf Mountain verified group `138`, all
seven generated button constants/actions, the `2465,3501,3` Ta Quir Priw landing, and a transformed
Captain Bleemadge actor (`10461` live versus catalog `10459`). The cache-backed resolver therefore
prefers the catalog id but admits a transformed actor only with exact name/action, plane, and origin
proximity. The forward Stronghold-to-Kar-Hewo engine route live-passed with exactly one actor stage,
one destination stage, directed landing, ordinary walking, and arrival. The reverse flight also
landed at the Stronghold top level, but the later plane-zero `Open;Tree Door` edge made the request
`LEGACY_LOCKED`. Those exact distance-two rows now publish through the existing adjacent-transport
state machine without broadening other two-tile doors. The final reverse Kar-Hewo-to-Stronghold run
remained on request 3/generation 1, issued one actor command and one exact `Ta Quir Priw` command,
observed the directed top-floor landing, chained three `Climb-down` catalog transitions, issued one
engine-owned `Open` command for the Tree Door, crossed the cleared edge, and arrived at the plane-zero
target. No `LEGACY_LOCKED`, replan, recovery, or duplicate interaction appeared. The family is closed.

**Quetzal slice active (2026-08-27):** directed `QUETZAL` rows now publish as a live Renu `Travel`
stage, exact `InterfaceID.QuetzalMenu` destination stage, and directed remote landing. The network
catalog intentionally has no stable NPC id, so the cache-backed resolver accepts transforming Renu
definitions only with exact name/action, correct plane, and bounded origin proximity; live ids are
telemetry and may change between observations. Quetzal-whistle item teleports remain legacy-owned.
The catalog's six-tick duration is retained as the initial explicit route-cost floor pending live
calibration. Compile, Checkstyle, policy/scanner, publication, ownership, remote-retention, and
navigation lifecycle tests pass. Live acceptance requires one route between two unlocked landing
sites in both directions, including inspection of the current map widget text/action contract.

**Landing catalog-disappearance finding (2026-08-24):** the next two-direction run still emitted no
restore stage. Request 2 captured and equipped Dramen staff `772`, but the landing inventory refresh
could remove the fairy row from the usable transport snapshot once that required staff was no longer
in inventory. `Rs2FairyRingScene.restore` then failed its unnecessary transport rediscovery and
returned `null`, clearing the pending edge. Restoration now uses the immutable pending origin,
destination, and original weapon id; it no longer depends on the mutable eligibility catalog.

**Landing equipment-cache gap (2026-08-24):** live testing after that change still skipped the restore
command. Immediately after the teleport/staff swap, the original weapon can briefly be visible in
neither inventory nor equipment. That absence is now an unsettled wait state; restoration completes
only when the saved weapon ID is observed equipped. A later trace proved this defensive condition was
not the cause of the skipped command.

**Remote-edge retirement finding (2026-08-24):** the navigation engine's remote-landing guard covered
NPC transports and charter ships but omitted `FAIRY_RING`. Landing advanced raw progress beyond the
fairy edge, so generic crossed-edge retirement deleted the pending restore interaction before dispatch.
Fairy rings now participate in both remote-edge guards, and the engine test reproduces the landing
progress jump while requiring both restore commands before the edge can retire.

**Completed dialogue/menu NPC implementation record:** the family was scoped (2026-08-05) in
three steps. Step 0 fixes a latent hole found in the accepted direct NPC/ship slice:
`NpcTransportPolicy` admits object-backed rows (Swamp Boaty `Board`, Tempoross `Ferry`, Fremennik
`Boat`, the DS2/Lithkren `Rowboat` rows, Lunar Isle `Go-inside;House`), but `Rs2NpcTransportScene`
resolves actors only through the NPC cache, so an engine-owned route through one of those edges
can never publish its interaction and stalls to bounded terminal failure at the frontier. Step 0
is implemented (2026-08-05): `Rs2NpcTransportScene` now falls back to a tile object near the
catalog origin — the exact catalog id, or a transformed id with the exact catalog name, in both
cases requiring the catalog action under hyphen/space-insensitive formatting — the pending
interaction keeps the catalog id, and dispatch clicks the resolved live action string through the
shared tile-object path. Policy, broad walker, and Checkstyle tests pass. Its live gate is one
object-backed route in both directions: the free, ungated Al Kharid Tempoross Ferry
(`Board;Ferry;41311`, Al Kharid bank side `3271,3144,0` to Ruins of Unkah `3148,2843,0`).
Step 0 is accepted (2026-08-05): forward and reverse Tempoross Ferry runs each issued exactly one
object-backed `npc-transport-interaction`, held one request/generation through the crossing,
acknowledged the catalog landing, and finished — the forward run arrived within the finish
tolerance of the landing, the reverse run resumed ordinary walking from the landing to the goal —
with no recovery, replan, duplicate input, or legacy markers. Step 1 is
the conservative dialogue slice behind a new `NPC_DIALOGUE_TRANSPORT` route kind: NPC/SHIP/BOAT
rows with a direct non-`Talk-to` action, non-blank `Display info`, no currency, and no item
requirements (Mountain Guide, Achilka, Primio, Cabin Boy Colin, Veos/Cabin Boy Herbert/Captain
Magoro, Molch Boaty, the Fossil Island underwater Rowboat menu, the Burgh de Rott/Meiyerditch
Boat). Reuse the charter staged-interaction pattern with stages observed from live dialogue state,
never assumed: actor click, zero-or-more bounded `Continue` clicks (each is one command in one
pass), an optional select-an-option stage matched against normalized `Display info` (numbered
options such as `1: Meiyerditch.` require tag/whitespace-normalized contains-matching), then
voyage retention with off-route recovery suppressed and clearing only near the directed catalog
landing — rows whose action is itself the destination may sail with no dialogue at all, exactly
like charter's optional confirmation stage. Step 1's code and headless tests are complete
(2026-08-05): the policy, staged scene over live chat-dialogue state, route scanner,
classification, engine landing/timeout ownership, and dispatch all mirror the charter pattern.
The actor stage resolves an NPC (catalog id preferred, transformed id with exact name/action and
origin proximity) or a tile object via the step-0 rules; destination options are matched exactly,
never by contains, so prefixed pairs such as Molch and Molch Island cannot be confused; a menu
without the declared option is treated as foreign and never receives input. Policy, scanner,
classification, mutual-exclusivity, broad walker, and Checkstyle suites pass. Lake Molch Boaty
live acceptance passed in both directions (2026-08-05): each run issued one object-actor command
and two dialogue-stage commands (Boaty presents a continue frame plus its destination menu — a
same-stage duplicate is impossible because a pending interaction command only yields to an
observed stage whose action string differs), held one request/generation, acknowledged the
catalog landing, resumed ordinary walking, and arrived without recovery or legacy markers. The
Shayzien-to-Molch run also proved exact option matching by selecting `Molch` while `Molch Island`
was offered. `nav_cmd` interaction lines now include `stage=` so later staged families read
directly from the log. Before the slice closes, one NPC-backed representative should pass both
live directions, because Molch exercised only the object-actor path: Captain Magoro at Land's
End (`1504,3399,0`, action `Port Sarim`, ungated) is the suggested route and chains a Port Sarim
gangplank landing. Live observation confirmed his shape: the destination-named right-click
action sails immediately with no dialogue, while left-click Talk-to opens a conversation whose
menu phrases each destination as a sentence (`Can you take me to Port Sarim please?`). The first
attempt showed the legacy executor stuck talking to him through that Talk-to path, so destination
matching now prefers an exact option and otherwise accepts only a single option containing the
destination text — ambiguous containment is still refused, preserving the Molch/Molch Island
guard — which also lets an engine run recover by menu selection and `CONTINUE` frames if an actor
click ever lands on Talk-to instead of the travel action. That attempt also exposed that a route
which locks to the legacy executor did so silently: a `nav_mode` line now names the unsupported
edges and their catalog rows once per request. That line identified the live lock:
`PathfinderConfig.updateActionBasedOnQuestState` rewrote the Veos and Captain Magoro rows to
`Talk-to` whenever Client of Kourend was not `FINISHED`. Live observation on an account without
that quest showed the right-click travel option works regardless — the gate modelled a game
behaviour that no longer exists, and its one-way mutation additionally poisoned the shared row
for a whole session on any early quest-state misread. The mutation is deleted; rows keep their
catalog actions, and no runtime code rewrites transport actions anymore. `nav_mode` remains the
tool for diagnosing any future legacy lock. With the gate gone, both NPC-backed live directions
then passed (2026-08-05): Land's End-to-Port-Sarim issued exactly one Magoro actor command
(`stage=Port Sarim`, exact catalog id, immediate sail with no dialogue stages), chained the ship
gangplank as one `CATALOG_TRANSITION`, and arrived on one request/generation with no recovery;
Port-Sarim-to-Land's-End issued one Veos actor command (`stage=Land's End`, exact id 10724),
sailed, and landed cleanly. The dialogue-slice live gates — object actor both directions at
Molch, NPC actor both directions here — are complete. The reverse run's chained gangplank then
exposed a general race in the already-accepted catalog-transition family: two seconds after a
successful `Cross` command, the scene transiently failed to re-resolve the object mid-crossing
and the pending interaction read `INTERACTION_UNAVAILABLE`, forcing a needless replan (the walk
still arrived). An in-flight interaction command now owns its interaction until the
acknowledgement deadline: an `UNAVAILABLE` observation inside that window yields `WAIT`, and only
persistent unavailability past the deadline replans. A headless regression pins both halves; the
fix should be observed live in passing on any future slow crossing. Step 2, only after step 1
closes, adds the coin-paid confirmation variants (Captain Tobias, Captain Barnaby, Antonia, Swamp
Boaty `Quick-board`, the 10,000-coin Slepe `Row boat`, Dwarven Ferryman) with fare evidence
reported as bounded `INTERACTION_UNAVAILABLE` and a `Yes` confirmation stage; non-coin currencies
(Ghost captain's Ecto-tokens) wait for that sub-slice to prove the pattern first. Step 2's code
and headless tests are complete (2026-08-05): eligibility now also accepts coin-paid rows
(`Display info` optional for single-destination confirm-only flows such as Swamp Boaty
`Quick-board`), the model carries a `CONFIRM` stage and the fare, and the scene recognises the
payment prompt as the single option starting with an affirmative word — checked only after
destination matching and only for paid rows, so free rows keep their exact step-1 semantics and
ambiguity is still refused. The scanner fare-gates only the actor stage against the live coin
count, reporting a missing fare as bounded `INTERACTION_UNAVAILABLE`; an open dialogue is never
fare-gated. The staged shapes were verified against Captain Tobias's transcript: pre-Pandemonium
is continue-then-confirm (`Yes please.` vs `No, thank you.`), post-Pandemonium adds a
destination menu whose sentence options the existing unique-containment matcher selects. Policy,
scene, scanner, mutual-exclusivity, broad walker, shortestpath, and Checkstyle suites pass. The
remaining gate is live acceptance: Port Sarim to Musa Point and back (Captain Tobias at
`3029,3217,0` out, Customs officer at `2956,3146,0` back, 30 Coins each way, each landing on a
ship deck chaining a gangplank `CATALOG_TRANSITION`), with the usual criteria and `stage=`
showing `dialogue-confirm` for each payment click. The first live attempt was engine-owned but
exhausted its route at the dock without proposing the interaction: the Sailing rework
(2025-11-19) replaced these NPCs, so the catalog ids were defunct (`3644`/`3648`) and the
declared `Musa Point`/`Port Sarim` actions no longer exist — the live NPCs are
`captain_tobias_1op` 14978 / `customs_officer_1op` 14984 with a `Travel` action, and their
`_2op` post-Pandemonium variants expose destination-named actions instead. The two ships.tsv
rows are corrected (`Travel`, new default-variant ids, `Display info` retained), and
`matchLiveNpcAction` now accepts the declared destination as a direct action when the catalog
action is absent, so one row covers both quest-state variants. Bounded `ROUTE_EXHAUSTED` was the
correct failure shape for an unresolvable published interaction. The rerun with corrected
identity then proved the new `Travel` op pays and sails directly with no dialogue — one actor
command (`stage=Travel`), fare deducted, voyage — so the confirm stage stayed correctly unused
and its live exercise waits for a family member that actually presents a prompt. That run also
exposed the third staleness axis: the live boat lands directly on the Musa Point dock
(`2956,3146,0`) rather than the catalog's ship deck (`2956,3143,1`), so the pending interaction
could not clear until its 30-second window expired and a bounded replan finished the walk. Both
rows' destinations are now the observed dockside landings (`2956,3146,0` /
`3029,3217,0`), eliminating the phantom gangplank edge. Post-Sailing staleness therefore has
three axes for the queued ship/boat audit: NPC id, action name, and landing destination. With
identity, action, and landing all corrected, both live directions then passed cleanly
(2026-08-05): each run issued exactly one `stage=Travel` actor command, satisfied the fare gate
with coins held, paid, sailed, cleared the pending interaction immediately at the dockside
landing, and arrived on one request/generation with no recovery, replan, dialogue stage, or
legacy marker. The Tobias/Customs officer pair is accepted as the paid slice's first
representative. Captain Barnaby then exposed the actor-anchor gap: his catalog origin is a route
anchor, and he wanders the Ardougne pier up to eight tiles from it (live id exactly 9250), so
the 3-tile same-plane matcher refused him and the engine-owned route exhausted at the frontier —
diagnosed and reproduced end-to-end over the agent server. Actor matching now accepts the exact
name/action within fifteen 2D tiles of the origin on any plane, readiness fires from the actor's
vicinity or the route frontier, and NPC dispatch gates on one dispatch-time walkability check —
per the recorded ranged-dispatch invariant (one click on any loaded, reachable target; never
walk-adjacent-then-click; never line-of-sight or per-candidate reachability sweeps). The
agent-driven Ardougne-to-Brimhaven retest then passed with the whole journey as exactly two
commands — one ranged `stage=Brimhaven` click on the wandered actor and one ship-deck gangplank
`Cross` — no ordinary movement clicks, no recovery, one generation. Barnaby also pays directly,
so with the Brimhaven-to-Ardougne direction already clean, every live-tested paid row pays
without a prompt. Step 2 is closed (2026-08-05) with the `CONFIRM` stage headless-proven only:
post-Sailing, ships that converse before sailing are the charter family, which is already
migrated with its own staged confirmation. The dialogue-stage confirm is conservative by
construction — an unmatched prompt stalls bounded rather than misclicking — and will validate in
passing if a prompting coin row ever appears live. The dialogue-menu family (free and paid) is
therefore complete; only the `Talk-to` conversation slice (Cabin Boy Herbert, Captain Shanks,
Pirate Pete) remains deliberately legacy-owned for a later specialised slice. Full `Talk-to`
conversation rows (Captain Shanks, Pirate Pete) remain legacy-owned — Shanks declares no
`Display info` for three destinations from one origin, so it needs a data fix before any executor
work. After this family: fairy rings, spirit trees, gliders, quetzals, POH, seasonal transports,
and other specialised transports, with banked-transport route setup last within Phase 6. Do not
classify an entire `TransportType` as engine-owned unless its complete UI/dialogue/landing
lifecycle is modelled and tested. Phases 7 and 8 (thin `ShortestPathPlugin`, collapse facade,
delete legacy orchestration) have not started.

**Post-Sailing ship/boat catalog audit (2026-08-05):** the queued audit of every remaining
ships.tsv and boats.tsv NPC row is complete (canoes and minecarts were inspected and are
object-only, so unaffected). Sources: post-Sailing wiki infoboxes (Options row plus advanced-data
ids), the vendored 1.12.35 `gameval.NpcID` cache constants, and the upstream shortest-path
project's post-Sailing data as a field-tested cross-check. The central finding is that the
Sailing rework's dock-NPC renumbering block (`14978`–`14985`) covers only the Port
Sarim–Musa Point dock — Captain Tobias, Seaman Lorris, Seaman Thresnor, Customs officer — so no
other ferry NPC in either file was renumbered by Sailing; the wiki still lists every other row's
id as live. Note the cache retains defunct definitions (`CAPTAIN_TOBIAS = 3644` still exists
beside `CAPTAIN_TOBIAS_1OP = 14978`), so id existence in `NpcID` proves nothing about
placement — the wiki infobox is the liveness source. Verified correct as-is: Captain Barnaby
(`9250`/`8764`/`8763`, destination-named actions), Cabin Boy Colin `7967`, Barge guard `8012`
`Quick-Travel`, Captain Shanks `5364` (`Talk-to`, legacy; its missing-`Display info` data gap
stays open), Bill Teach `4016`, Sailor `3936`/`3680`, Captain Bentley `6650`, Captain Magoro
`7471`, Monk of Entrana `1165`/`1168` (`Take-boat` confirmed by the wiki Options row; upstream's
`Travel-boat` divergence was checked and rejected), Squire `1770`/`1769`, Antonia
`13984`/`13985`, Holgart `7789`/`5070`, Pirate Pete `601`/`602`, Ghost captain `3005` at both
ends, Mord/Maria Gunnars `1900`/`1940`/`1883`/`1882`, Lokar Searunner `3855`/`9306`, Lumdo
`1454`, Sandicrahb `7483`/`7484`, and all object-backed rows. Four defects were fixed, all
pre-Sailing staleness rather than Sailing casualties: (1) Jarvald's Rellekka-side id `6535` no
longer spawns — the wiki lists `5937`/`7205`, `7205` being the one-op pre-quest variant, so the
row now uses `5937`; (2) the River Kelda rows carried nonsense ids (`13825`/`13826`/`13829` are
Death on the Isle NPCs, `17768` a POH fireplace) and now use Dwarven Boatman `7726`
(mines, post-quest) / `2433` (city) and Dwarven Ferryman `4896` (entrance) / `4897` (mines);
(3) Veos reused `10724` for every origin and now carries the per-origin live ids `8630` (Port
Sarim) / `10726` (Piscarilius), keeping destination-named actions; (4) Cabin Boy Herbert's rows
declared destination-named actions that do not exist — the live NPC exposes only `Talk-to` — so
the rows now declare `Talk-to` with per-origin ids `10933` (Sarim) / `10932` (Piscarilius) and
retain `Display info`. That makes Herbert legacy-owned by the talk-to exclusion, and the legacy
Veos `Talk-to` dialogue branch in `Rs2Walker` now also accepts Cabin Boy Herbert — his
transcript has the identical shape (`Can you take me somewhere?` then `Travel to <dest>.`
options matched by `Display info` containment). Since live matching is name-plus-action in both
executors, the action fixes are the load-bearing part; id fixes restore honest catalog identity.
The audit covered the id and action axes; the third axis (landing destinations) is only proven
for Tobias/Customs and remains a live-run concern per family. For the still-unproven `CONFIRM`
stage, the paid rows most likely to present a payment prompt are Captain Barnaby (30 Coins,
destination ops) and the Dwarven Ferryman (2 Coins); Shanks (50 Coins) is a `Talk-to`
conversation and stays legacy.

**Latest defect fixed:** the Port Sarim TSV row correctly names Trader Crewmember id `9342`, but the
loaded actor transformed to live id `9377`. Charter resolution now prefers the catalog id and permits
a transformed id only with exact name, exact `Charter` action, correct plane, and proximity to the
directed catalog origin. The initial forward run's pre-charter
`COMMAND_DESTINATION_MISMATCH` was the already-isolated physical-mouse/natural-mouse input race, not
a charter failure.

**Validation at handover:** `:client:compileJava`, `:client:compileTestJava`, focused charter tests,
the broad `net.runelite.client.plugins.microbot.util.walker.*` unit suite, `checkstyleMain`,
`checkstyleTest`, and `git diff --check` all pass. The central implementation is in
`navigation/NavigationEngine.java`, `navigation/PathfinderRouteCalculation.java`, and
`Rs2Walker.java`; charter-specific policy, scanner, live scene, and model classes are under
`util/walker/transport/`. Movement gotchas 39-44 document the accepted transport assumptions.

## Execution status

- **Phase 0 complete (2026-08-04):** full headless unit baseline green; public API inventory
  recorded in `walker-api-inventory.md`; architecture guardrails enforce the facade boundary,
  prohibit lifecycle ownership in interaction handlers, and prevent legacy `processWalk` growth.
- **Phase 1 complete (2026-08-04):** `RoutePlannerRuntime` is the single production owner of
  pathfinder tasks. Request/preparation/generation identities reject stale work; completed routes
  publish immutable `RoutePlan` snapshots; the old plugin executor/Future/mutex and all lifecycle
  setters are deleted. `getPathfinder()` remains temporarily read-only for the legacy executor and
  overlays until Phase 2 consumes snapshots.
- **Phase 2 complete (2026-08-04):** `NavigationEngine` exclusively owns an instance-scoped
  `WalkSession` and publishes immutable `NavigationSnapshot` diagnostics. Real walker heartbeats,
  replans, interaction/exit classifications, route generations, and terminal clears feed the
  engine in shadow mode; its decisions are logged/compared but cannot issue input. Headless shadow
  traces cover transitions, monotonic progress, one-command-per-pass, cancellation, interaction
  frontiers, and recovery-budget deferral. An architecture guard prohibits input dependencies.
- **Phase 3 in progress (2026-08-04):** immutable plans now contain explicit raw edges and a
  one-time smoothed-to-raw mapping. Requests snapshot an opt-in ordinary-engine flag and lock to
  `ENGINE_SUPPORTED` or `LEGACY_LOCKED` when their first plan arrives; that choice cannot change on
  later generations. The new loop sends at most one movement command through `WalkerActions`,
  tracks acknowledgement, replans rejected/off-route movement, and handles arrival, replacement,
  partial/empty routes, and cancellation. Transport or non-adjacent edges are conservatively
  ineligible. Initial live acceptance exposed LOS-shaped click placement; selection now advances
  7-10 bounded steps on the forward raw route, can cross smoothed/LOS corners, stops before repeated
  folded-route coordinates, and never geometrically clamps a rejected route click off the route.
  Each issued checkpoint also carries a 2-4 tile proximity handoff window, allowing the next raw
  checkpoint to be selected while movement is still in flight without retargeting a final route
  command. `nav_cmd` diagnostics report raw/smoothed indices, Euclidean distance, reach, handoff,
  selection tier, adapter action, and issuance result. The developer flag remains off by default
  pending further live acceptance routes; the legacy ordinary-click branches remain until that
  gate passes.
- **Phase 4 complete (2026-08-04):** engine-owned ordinary walks now keep recovery evidence and
  per-cause budgets in `WalkSession`, distinguishing rejected/unacknowledged commands, no tile
  progress, off-route movement, live blocked edges, interaction waits, route exhaustion, and
  external replans. A settled no-progress command gets one shorter raw-route rejoin click before a
  bounded replan; recent or in-flight valid commands defer recovery without spending its budget.
  Live-collision validation queues a generation-scoped blocked-edge observation for the engine
  instead of independently recalculating, and a repeated edge can request only one replan per
  generation. `nav_cmd` now includes the engine decision reason, while recovery clicks, replans,
  and terminal failures emit one `nav_recovery` record containing cause, attempt/budget, evidence
  age, blocked-edge index, raw progress, and route distance. Live acceptance caught repeated
  proximity commands to the same raw index; handoffs now select strictly beyond the active command,
  and an acknowledged command with no raw progress waits for the recovery evidence window instead
  of resetting that window with another ordinary click. Movement acknowledgement now samples the
  destination registered by the client: a newly registered route-backed destination or actual
  forward raw-route progress acknowledges a dispatched command, while a clearly divergent
  destination immediately consumes its own bounded recovery category and emits a mismatch-only
  `nav_ack` diagnostic. A no-progress recovery without a valid short rejoin target remains
  classified as `NO_TILE_PROGRESS` and replans without spending the route-exhaustion budget.
  Live acceptance covered uninterrupted long routes, manual displacement, physical-mouse input
  contention, and successful recovery through arrival. Engine-owned requests return before the
  legacy `processWalk` recovery block, so legacy `stuckCount` and interaction recovery are retained
  only for `LEGACY_LOCKED` routes until their Phase 5/6 migrations; they cannot compete for an
  ordinary-engine request. The ordinary-engine flag remains off by default pending the Phase 3
  deletion/cutover decision.
- **Phase 5 in progress (2026-08-04):** the first dynamic-obstacle slice migrates Motherlode
  rockfalls. `RouteInteraction` now carries the blocking raw edge, object tile, action,
  availability, and approach readiness into the engine. `WalkSession` retains the interaction
  across passes while the engine separately approaches, issues one non-blocking mine command,
  verifies object clearance, and keeps the interaction pending until the route edge is crossed.
  Missing-pickaxe evidence is reported as bounded `INTERACTION_UNAVAILABLE` recovery instead of
  letting the handler replan or clear the walk target. Live object classification remains behind
  `LiveScene`/`MineableResolver`; the old blocking `handleRockfall` scans remain reachable only on
  `LEGACY_LOCKED` routes. The first live Motherlode
  route successfully mined and arrived, but also exposed that `getGameObject(WorldPoint)` treats
  its point as a search anchor and can select an adjacent object. Classification and dispatch now
  require an exact object anchor. The repeat route passed; it also showed that waiting beside a
  blocker before interacting retained legacy-style approach clicks. Loaded mineables within the
  bounded 13-tile handler frontier now dispatch while ordinary ground movement is still active,
  allowing the server to finish the approach. Post-clear movement is limited to crossing that raw
  edge, followed by a scanner-only observation, so ordinary lookahead cannot skip a second nearby
  blocker. Forward and reverse live routes now pass this sequence, completing mineable acceptance
  for engine-owned routes. The ordinary-door slice now snapshots standard door/gate candidates,
  matches wall objects to the exact crossed edge and single-tile game objects to a route endpoint,
  and feeds the earliest door-or-mineable interaction into the same lifecycle. Only `Open`,
  `Walk-through`, `Go-through`, and `Pass` are initially eligible; special/dialogue, catalog-backed,
  cross-plane, and instanced cases remain legacy-owned pending later slices. Reverse multi-door
  live acceptance chained three successive doors, including a 17-tile server-pathing handoff, and
  arrived. Ordinary doors/gates are accepted for engine-owned routes; the same chaining behavior is
  now required for each later transport and shortcut family.
- **Phase 6 in progress (2026-08-04):** route publication now distinguishes migrated adjacent
  same-plane catalog transports from unsupported transports. Only one-tile, object-backed,
  direct-action rows (`Open`, `Pass`, `Walk-through`, `Go-through`, `Climb-over`,
  `Climb-through`, `Squeeze-through`, `Cross`, and `Vault`) are initially eligible. Toll,
  lock-pick, NPC/dialogue, arbitrary `Use`, destructive-object actions, non-adjacent, cross-plane,
  and specialised transport families remain legacy-owned. Eligible catalog edges enter the same
  pending interaction, exact-edge crossing, collision ownership, arrival guard, and chaining
  lifecycle already accepted for ordinary doors and mineables. The execution-mode name is now
  `ENGINE_SUPPORTED` because engine ownership is no longer limited to transport-free routes.
  Initial live acceptance began within the configured destination tolerance on the opposite side
  of a catalog door and exposed premature arrival before the edge was crossed. Distance-based
  arrival is now gated by every uncrossed engine-owned interaction edge in the published route;
  this remains true when an `Open` object is absent because the door is already open. A subsequent
  Al Kharid toll-gate route exposed two older ownership leaks: door-like catalog objects were
  deliberately exempted from the catalog guard, and a diagonal raw step could cross one lane of a
  double gate without exactly matching either TSV edge. Catalog ownership now wins regardless of
  door-like naming/action, and directed edge matching recognises those adjacent parallel lanes.
  Toll and quest-gated rows therefore remain legacy-owned rather than entering the ordinary-door
  lifecycle. Repeat acceptance then showed that a transformed live gate id could still let the
  ordinary scanner tie with the catalog scanner, with the earlier ordinary result winning.
  Ordinary scanning now considers only published `WALK` edges, while the adjacent-transport scene
  resolves a transformed id only when its exact boundary, object name, and action match the
  catalog row. Reverse acceptance then opened the gate but exposed the old one-tile post-clear
  minimap nudge: the adjacent click was not acknowledged, each replan rediscovered the gate, and
  the walk looped until a later attempt crossed. When no next interaction is published, cleared
  interactions now continue to the next bounded forward raw-route checkpoint. The one-edge nudge
  remains only as a guarded fallback and prefers a canvas click to avoid the minimap centre dead
  zone. Interaction acknowledgement is also distance-aware, so a ranged object click does not
  expire and redispatch while the server is still walking the player to the object. A further
  forward run proved that time alone is not sufficient: the first ranged click crossed the gate,
  but the persistent gate object was redispatched from its destination side and pulled the player
  back. Pending adjacent transports now retain the catalog row's real directed boundary and clear
  as soon as the player reaches or passes its destination side, even when the raw route used the
  neighbouring lane of a double gate and the object remains interactable. Final westbound and
  eastbound Al Kharid acceptance each issued exactly one adjacent-transport interaction followed
  by one forward route command, stayed on generation 1, and arrived without recovery. The
  adjacent same-plane transport slice is live-accepted.
  The second Phase 6 slice is now implemented behind a separate `CATALOG_TRANSITION` route kind.
  It covers direct object-backed stairs, ladders, trapdoors, and caves whose action is a bounded
  climb/walk/enter/exit operation, while currency, item-use, dialogue, `Open`-only, and unrelated
  object rows remain legacy-owned. Exact cross-plane catalog edges are now preserved by transport
  matching. The live resolver uses the shared tile-object cache, verifies landing against the
  catalog destination plane and coordinates, and models a closed trapdoor as `Open` followed by
  its actual climb action rather than treating object transformation as arrival. That slice and
  the direct NPC/ship plus gangplank slice have completed headless and live acceptance. Paid
  coin-only charter ships model the exact Trader Crewmember click, destination widget, `Yes`
  confirmation, and remote landing as one staged interaction; forward and reverse live acceptance
  both passed, closing the charter slice. Headless and broad walker tests pass.

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

- Select clicks from a bounded forward window on the raw route; smoothing may inform progress and
  diagnostics but must not impose a line-of-sight click frontier.
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

Deletion gate: satisfied for engine-owned ordinary routes—the engine returns before legacy
`stuckCount` and recovery-click control flow. Those paths remain isolated behind `LEGACY_LOCKED`
for dynamic obstacles/transports and are deleted family-by-family in Phases 5 and 6.

### Phase 5 - Migrate dynamic obstacles

Status: in progress. Mineable blockers have completed the code/test cutover for engine-owned
ordinary routes. The first live Motherlode route mined its blockers and arrived; one repeat route
confirmed the exact-anchor fix no longer interacts with adjacent rockfalls. Ranged interaction
and bounded edge-crossing then passed a forward/reverse live route with consecutive rockfalls.
The mineable slice is accepted for engine-owned routes; its old handler remains isolated to
`LEGACY_LOCKED` requests. The common ordinary door/gate code and headless tests are in progress;
the first live ordinary-door route dispatched the exact door successfully. That run exposed an
arrival-ordering defect when the target was inside the configured finish radius: completion could
overtake the still-pending door verification. Arrival is now deferred until the interaction edge
has been cleared and crossed. Repeat live acceptance crossed and retired the door correctly. A
multi-door route then exposed redundant one-tile crossing clicks between already-loaded
interactions; the common lifecycle now chains directly to the next exact, ready route interaction
and retains the one-edge minimap crossing only as a fallback. Reverse multi-door live acceptance
chained three successive doors, including a 17-tile server-pathing handoff, and arrived. Ordinary
doors/gates are accepted for engine-owned routes. The same interaction-chain contract is a required
acceptance condition for each Phase 6 transport and shortcut family. That acceptance log also
showed live-collision validation reporting the active door edge (with a one-index anchor skew) as a
generic block. Pending and cleared-but-uncrossed interaction edges now temporarily own their edge
and immediate neighbour for collision recovery; unrelated collision contradictions still replan.
The repeat corridor stayed on one route generation, chained all four doors, emitted no
`BLOCKED_EDGE` recovery, used the one-edge crossing only after the final door, and arrived. The
ordinary door/gate slice and its collision-ownership acceptance gate are complete.

The unusual-door inventory now has an explicit ownership boundary. `Pick-lock` doors and
`Pay-toll` gates are transport-catalog rows with requirements or dialogue, so they remain
transport-owned for Phase 6 instead of entering the ordinary-door scanner. Stronghold of Security
question doors expose ordinary-looking actions but require a multi-step dialogue; routes starting
or ending in its regions now remain legacy-owned, and the live ordinary-door scene rejects those
objects as a second guard. The legacy Stronghold handler remains the only owner until its sleeps and
dialogue loop are replaced by a non-blocking dialogue interaction state. This closes the unsafe
classification gap without disguising legacy dialogue execution as a migrated engine interaction.

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

Status: in progress. The adjacent same-plane slice has completed its headless and live acceptance
gates.
The immutable route snapshot classifies only conservatively eligible catalog edges into migrated
route kinds; a route containing any other transport remains `LEGACY_LOCKED` before input.
The live adapter resolves the exact enabled directed row and its object id near the catalog origin,
then dispatches through `RouteInteraction.Kind.ADJACENT_TRANSPORT`. Persistent shortcut objects are
verified against the catalog row's directed destination boundary rather than only raw-edge progress;
transforming `Open`/`Pass` door-family objects may also prove clearance by disappearing. Missing
persistent shortcut objects are unavailable rather than assumed clear. Live forward/reverse
acceptance passed at the Al Kharid double gate without duplicate interaction, replan, or generation
churn. The stairs, ladders, trapdoors, and caves slice now has a conservative route kind, live
cache-backed resolver, staged closed-trapdoor handling, and catalog-destination landing
acknowledgement. Live acceptance passed on the three-floor Falador Castle staircase chain: the
engine dispatched all three directed catalog transitions without a minimap nudge between floors,
then continued through an ordinary door and arrived without replan or generation churn. Its
headless and live acceptance gates are complete.

The simple item/spell teleport slice now classifies direct originless casts and single-action items
as `SIMPLE_TELEPORT`, resolves the exact enabled row at the published raw edge, reuses the existing
item/spell command implementations, and acknowledges only at the catalog landing. Destination
submenus, wilderness confirmations, Master Scroll Book flows, multi-option spell variants, and the
long Lumbridge Home Teleport remain legacy-owned. Planner, scanner, execution, and route-ownership
tests pass. Live item acceptance passed with a consumable tablet: exactly one unified interaction
was issued, the post-consumption transport refresh did not invalidate the pending command, landing
was acknowledged, and ordinary walking resumed on the same generation. A later ground-click
destination mismatch caused a normal replan; it occurred after teleport acknowledgement and is
tracked as input-adapter behavior rather than teleport-family failure. Direct-spell live acceptance
remains unavailable on the current test account; its command and landing behavior share the tested
simple-teleport lifecycle.

The dialogue/NPC/boat family has completed a conservative direct-action slice. Exact `SHIP`,
`BOAT`, and `NPC` rows are published as `NPC_TRANSPORT` only when they have a one-click non-`Talk-to`
NPC action, no currency/item requirement, and no separate destination display choice. Dispatch
re-resolves the enabled directed row plus exact NPC id/name, issues one NPC interaction, tolerates
the source NPC unloading during travel, and acknowledges only near the catalog landing. Planner,
scanner, execution, ownership, broad walker, and Checkstyle tests pass. Conversation choices,
paid confirmations, named special dialogue, and destination menus remain legacy-owned. The first
Port Sarim run issued the correct unified
Squire interaction, then exposed a legitimate intermediate ship scene far from the raw route before
the final landing. Pending voyage commands now suppress ordinary off-route recovery through that
scene and use a bounded 30-second boarding/landing window. A repeat proved that the intermediate
scene's shared destination plane also made nearest-route progress jump to the destination raw index
from 531 tiles away; `NPC_TRANSPORT` retirement now requires the catalog landing rather than raw
index advancement. Live Port Sarim-to-Void-Knights acceptance then passed after restart: the engine
issued one `NPC_TRANSPORT` command, retained the same request and generation through the voyage,
reached the catalog landing without off-route recovery or legacy handoff, and terminated as
arrived. Reverse acceptance remains outstanding before the direct-action NPC/boat slice is closed.
The first reverse attempt was correctly legacy-locked because the voyage lands on the Port Sarim
ship and the remaining `Cross;Gangplank` edge was outside the initial stairs/ladders/caves catalog
transition policy. Direct `Cross` on an exact catalogued `Gangplank` is now conservatively eligible
as `CATALOG_TRANSITION`, allowing the reverse Squire voyage and disembark edge to remain one
engine-owned interaction chain. Repeat live acceptance passed in both directions with plane-0
land destinations: each route issued exactly one `NPC_TRANSPORT` command followed by exactly one
gangplank `CATALOG_TRANSITION`, retained its request and generation through the voyage, and then
either arrived or resumed ordinary walking without legacy markers, replan, or duplicate input. The
direct-action NPC/ship slice has completed its headless and live acceptance gates; dialogue/menu
and paid variants remain legacy-owned for their later specialised slices. Scoping the dialogue
family then exposed that this accepted slice admits object-backed rows (Swamp Boaty, the
Tempoross Ferry, the Fremennik Boats, several quest Rowboats, Lunar Isle's `Go-inside;House`)
whose actor is a tile object the NPC-only scene could never resolve, so an engine-owned route
through one stalled to bounded terminal failure without wrong input. The live scene now falls
back to a tile object near the catalog origin — exact catalog id, or a transformed id with the
exact catalog name, always requiring the catalog action under hyphen/space-insensitive
formatting — the pending interaction retains the catalog id, and dispatch clicks the resolved
live action string. Forward and reverse Al Kharid Tempoross Ferry routes then passed with one
object-backed command each, stable request/generation, acknowledged landings, and no recovery or
legacy markers; the object-actor repair is accepted.

The paid charter-ship slice is now implemented behind a separate `CHARTER_SHIP` route kind. Only
enabled charter rows with an exact Trader Crewmember id/name/action, a named destination, and a
positive Coins fare are eligible; item requirements and other paid or dialogue protocols remain
legacy-owned. One pending interaction advances through the NPC click, exact destination widget,
and `Yes` confirmation, then remains active while the source UI unloads and the voyage runs. It
clears only near the directed catalog landing, so neither raw-route progress nor disappearance of
the refreshed usable row can retire it early. Route-publication, policy, scanner, three-stage
execution, broad walker, and Checkstyle tests pass. The first live attempt reached the Port Sarim
charter origin but published no charter interaction because the catalog NPC id `9342` was
transformed to live id `9377`. Charter NPC resolution now prefers the catalog id while safely
accepting a transformed id only with exact name/action and catalog-origin proximity; the focused
regression suite and Checkstyle pass. Repeat Port Sarim-to-Catherby acceptance then passed: the
engine issued the charter NPC and destination-stage inputs, retained request 1/generation 2
through the remote landing, chained the Catherby gangplank as one `CATALOG_TRANSITION`, resumed
ordinary walking, and arrived without legacy handoff or transport recovery. That charter interface
did not present a separate confirmation prompt, so the optional confirmation stage correctly
issued no command. The generation-1 destination mismatch occurred before the charter frontier and
matches the already isolated physical-mouse input race rather than charter execution. Reverse
Catherby-to-Port-Sarim acceptance then passed on request 2/generation 1: one NPC-stage and one
destination-stage command at the Catherby origin, landing acknowledged at the directed catalog
destination, one gangplank `CATALOG_TRANSITION`, and ordinary walking resumed to arrival with no
replan, recovery, duplicate input, or legacy markers. The paid coin-only charter-ship family has
completed its headless and live acceptance gates; item-fare, dialogue, and other paid protocols
remain legacy-owned for later specialised slices.

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

Reuse the common interaction-chain lifecycle for every family: when the current edge clears and
the next exact interaction is loaded and ready, dispatch it directly and retain the cleared edge
until route progress confirms crossing. One-edge ground movement is only the fallback when no safe
successor exists. This applies to stairs, ladders, gates, agility shortcuts, and transport objects;
family handlers may classify and execute their interaction but may not add an independent
approach/crossing loop.

Required transport invariants:

- when the target actor or object is loaded and within the handler frontier, dispatch one
  ranged click and let the server path the approach — never stage extra movement clicks to
  stand adjacent first. Gate NPC dispatch on a single dispatch-time walkability check; never
  gate any family on line-of-sight (fails on solid geometry, deadlocks) or on per-candidate
  reachability sweeps inside scan loops (documented multi-second client-thread stalls);
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
