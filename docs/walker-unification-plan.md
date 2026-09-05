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
prompts live). Captain Shanks' ticket-purchase conversation and Port Khazard voyage are also
live-accepted; the corrected Port Sarim option is headless-proven. Directed three-letter non-POH
fairy rings are now live-accepted in both directions,
including displaced-weapon restoration proven by equipment state. POH/DIQ fairy-ring edges remain
legacy-owned for the later POH slice. Non-POH spirit trees are also live-accepted in both directions,
including session filtering and replanning for grey locked destinations. Gnome gliders are now
live-accepted in both directions, including the three Stronghold descents and its two-tile
`Open;Tree Door` boundary after the reverse landing. Quetzals are also live-accepted in both
directions, including varbit-gated exclusion of a grey locked destination. The POH physical portal
foundation is live-accepted in both directions; configured high-level POH facilities remain deferred
until an equipped house is available. Seasonal transports now have a headless cutover: all 169
packaged Clue compass and Map of Alacrity rows have explicit executors and publish as engine-owned
remote teleport interactions, while unknown row shapes are rejected before routing. Live acceptance
is deferred until the relevant League server/items are available. Banked-transport route setup is
now headless- and live-complete: one explicit coordinator owns the bank leg, bank transaction,
final replan, and target leg while the navigation engine continues to own each walk. The remaining
Phase 6 work is the audited unsupported/specialised-family backlog below. All ten exact Guardians
of the Rift barrier rows are now engine-owned and rebuilt-client accepted in both directions, with
the completed-quest and player-participation varbit gates encoded directionally. The two adjacent
Temple of the Eye portal rows are also exact-contract engine-owned and live-complete in both
directions on rebuilt post-cutover bytecode. The two Hazeel Cult raft rows are exact-contract
engine-owned headlessly, preserving legacy's direct `Board` behaviour while retaining directed
landing validation for incorrect sewer-valve states. All 16 Port Phasmatys Energy Barrier rows
are exact-contract engine-owned headlessly: paid pre-quest rows require two ecto-tokens and an
equipped ghostspeak item, while completed-quest rows use the modern free `Pass` action. The paid
west-to-east barrier contract is also rebuilt-client accepted with banked requirements; this is
representative acceptance, not a claim that every quest-state/direction row was run live. All seven teleportation
lever rows are now headless- and live-complete behind an engine-owned staged interaction: rebuilt-
client acceptance passed for both the Wilderness-warning and direct-return directions without a
legacy ownership marker. All 25 River Lum canoe rows now have a dedicated engine-owned staged
interaction. Ferox Enclave/Edgeville acceptance passed in both directions; the one-way Wilderness
Pond warning remains headless-proven rather than live-claimed. All 47 packaged minecart source rows
(254 directed transports after loader expansion) now publish through a dedicated engine-owned
interaction. The Grand Exchange/Keldagrim free pair passed rebuilt-client acceptance in both
directions; paid Ice Mountain/White Wolf and Kourend destination-menu travel, plus train-cart
weapon/shield restoration, remain headless-complete rather than live-claimed. Eighty-eight
deterministic generic teleportation-portal rows now publish as engine-owned, with the representative
Edgeville/Soul Wars pair live-accepted in both directions. Twelve Castle Wars Guthix rows remain
legacy-owned because identical object inputs declare two possible directed landings. Phase 7 has not
started. All 20 active grouping-tab minigame rows now publish through one staged engine interaction;
the direct-activity path is representative-live-complete, while the three Rat Pits destination-option
rows remain headless-complete rather than live-claimed. The commented broken Keldagrim Rat Pits row
is not an active catalog row. All 12 packaged Magic Mushtree source rows now expand to 24 directed
runtime edges and publish through one staged engine interaction. The Mushroom Meadow/Verdant Valley
pair is representative-live-complete; undiscovered menu destinations are rejected without input,
session-disabled, and replanned without surrendering movement ownership.
All 12 magic-carpet rows are headless- and representative-live-complete, including the observed
200-coin fare and the bounded 60-second flight lifecycle. Hot-air balloons are headless- and
representative-live-complete: 51 resource rows expand to 225 directed engine-owned edges with
destination-log banking, and an empty-equipment Entrana-to-Taverley run consumed one normal log,
retained engine ownership through both interface stages, and arrived normally. Wilderness
obelisks remain deliberately legacy-locked: the
audit proves 270 genuine remote rows share random `Activate` inputs, plus 48 unsafe self-pad loader
artifacts; this account has not completed Wilderness Hard and no deterministic live probe was made.
The deterministic axe- and machete-gated chopping subset is now headless-complete. Seventy-six
unique `Chop-down;Jungle Bush/Jungle tree` Kharazi inputs and all ten paired
`Chop-down;Vines` Brimhaven Dungeon rows publish as `JUNGLE_OBSTACLE`; the 16 Kharazi rows
belonging to eight inputs that each declare two directed destinations remain legacy-locked. All
102 rows retain bank planning for one reusable machete or axe alternative. Representative live
acceptance remains deferred because these crossings are in hazardous jungle/dungeon areas and no
character movement was attempted.
All 16 exact Chasm of Fire `Enter;Lift` rows now share the direct catalog-transition lifecycle;
actual lift use remains live-deferred because every landing is inside the demon-filled dungeon.
All ten directed Fremennik basalt-causeway rows now share that lifecycle and are live-accepted in
both directions. Internal jump failure recovery is pinned by the observed wash-back geometry and a
deterministic engine regression; the rebuilt follow-up runs did not randomly fail, so that recovery
branch remains live-in-passing rather than claimed from a post-fix failure trace.
All 20 generated Shilo Village `Pay-fare;Travel cart` rows now reuse the paid dialogue-transport
lifecycle for their exact direct object action and remote landing; rebuilt-client object-definition
evidence confirms both catalog IDs expose `Travel cart` with `Board` and `Pay-fare`. Live travel
remains deferred until the account is logged in at a safe cart endpoint.
All five exact Fossil Island `Jump on;Rubber cap mushroom;30606` rows now reuse the direct
catalog-transition lifecycle. Their five distinct origins declare one directed remote landing and
carry no item, fare, quest, or var-state requirement. Exact-ID/action/name policy and production
corpus tests reduce the ordinary legacy count by five without admitting neighboring mushrooms or
other jump actions. A read-only probe against the rebuilt client confirmed `rows=1327 groups=260`
for the remaining ordinary legacy inventory and was undeployed afterward. Live acceptance remains
deferred because the account remained at the login screen and no character movement was attempted.
All ten exact Myths' Guild `Pass;Magical Barrier` rows for object IDs 31616 and 31617 now reuse the
same direct catalog-transition lifecycle. The six bridge rows for ID 31616 were corrected to require
`Dragon Slayer II`, matching the four dungeon rows and preventing an inaccessible route from being
offered before guild access. Exact identity, quest-gating, classifier, regression, benchmark, and
Checkstyle coverage pass. A read-only probe against the rebuilt client confirmed
`rows=1317 groups=259` and was undeployed afterward. Live crossing remains deferred because the
account remained at the login screen and no character movement was attempted.
All 32 exact Prifddinas `Enter/Exit;City Gate` rows for object IDs 36518, 36519, 36522, and 36523
now reuse that lifecycle as two direction-specific contracts. Every row already carries the
`Song of the Elves` completion gate; `Enter` admits only IDs 36518/36519 and `Exit` only IDs
36522/36523, so swapped actions and neighboring gates remain legacy-locked. Exact identity,
quest-gating, classifier, regression, benchmark, and Checkstyle coverage pass. A read-only probe
against the rebuilt client confirmed `rows=1285 groups=257` and was undeployed afterward. Live
crossing remains deferred because the account remained at the login screen and no character
movement was attempted.
All 53 exact `Enter;Passageway` rows now reuse the direct catalog-transition lifecycle: 52 Tarn's
Lair room transitions and the Rogues' Den lobby exit to the Toad and Chicken. OSRS Wiki MCP evidence
identifies both families as single-action passageways, while cache metadata pins the Tarn production
IDs to `lotr_ruins_door` variants and ID 7258 to `roguesden_passage_to_pub`. Tarn's trapped pillar,
ledge, and floorboard obstacles remain legacy-owned. Exact identity, classifier, regression,
benchmark, and Checkstyle coverage pass. Read-only probes against rebuilt clients confirmed the
intermediate `rows=1233 groups=257` Tarn-only result and final `rows=1232 groups=256` complete-family
result; both probes were undeployed afterward. Live passage remains deferred because Tarn's Lair is
hazardous, the account remained at the login screen, and no character movement was attempted.

The 42-row `Enter;Tunnel` family has been split by exact object protocol rather than migrated by
display name. The three object-ID 2141 rows are the quest-free, one-way Fremennik Slayer Dungeon
exit; OSRS Wiki MCP documents `Enter` as its sole action and the dungeon entrance as its landing, so
those rows now use the direct catalog-transition lifecycle. The other 39 rows remain legacy-owned:
the Stronghold Slayer Cave pair needs 72 Agility, the Miscellania pair needs 40 Agility and Royal
Trouble, the Kalphite/Lumbridge cave links have light-source or access-state hazards, the Ancient
Pyramid back door is quest-unlocked, and the Troll Romance tunnel groups are not requirement-complete
in the transport resource. Exact-ID policy and resource regressions keep those contracts separated.
The broad regression, benchmark, and Checkstyle suite passes, and a read-only probe against the
rebuilt client confirmed `rows=1229 groups=256` with 39 `Enter;Tunnel` rows still legacy-owned; the
probe was undeployed afterward. Live crossing of ID 2141 remains deferred until the account is
safely positioned inside the dungeon.

The 43-row `Jump-to;Pillar` family is also split by exact location and requirement shape. OSRS Wiki
MCP identifies the Revenant Caves pillars as direct `Jump-to` shortcuts with easy/medium/hard
requirements of 65/75/89 Agility; failure deals 4-11 damage but still crosses. Only the two directed
easy-shortcut rows at `(3220,10088,0) <-> (3220,10084,0)` carry their documented 65 Agility gate,
so those exact object-ID 31561 rows now use `CATALOG_TRANSITION`. Six medium/hard Revenant rows lack
their documented skill requirements and remain legacy-owned, as do all 35 Tarn's Lair pillar rows:
Tarn's hanging-log trap can knock the player off and deal 15-20 damage. The policy is pinned to the
exact easy-shortcut coordinates and object identity, and the resource regression asserts the 2/41
ownership split. The broad regression, benchmark, and Checkstyle suite passes, and a read-only probe
against the rebuilt client confirmed `rows=1227 groups=256`, 41 remaining pillar rows, and 39
remaining tunnel rows; the probe was undeployed afterward. No live crossing is planned while the
account is at the login screen; Revenant Caves are PvP Wilderness and are not an appropriate
unattended acceptance location.

**Remaining `Open;Door` audit (2026-09-02):** all 29 remaining rows stay legacy-owned; an
`Open` action alone does not prove a direct scene-transition lifecycle. OSRS Wiki MCP object
metadata and location guidance separate this family as follows:

| Rows | Object / location | Outstanding requirement or interaction evidence |
| --- | --- | --- |
| 18 | IDs 137-145, Draynor basement | Lever puzzle and varbit-gated doors need explicit non-blocking puzzle ownership. |
| 1 | ID 16774, Baba Yaga house exit | Exact action and landing need live confirmation. |
| 1 | ID 2000, Waterfall dungeon exit | Exact action and landing need live confirmation; do not infer entrance requirements for an exit. |
| 1 | ID 2010, Waterfall entrance | Amulet requirement changes after quest completion; current static requirements need a separate audit. |
| 2 | ID 2002, long Waterfall internal links | Key requirement and 38-tile catalog span need investigation; the two already-migrated adjacent rows are outside this count. |
| 1 | ID 2406, Zanaris shed | Equipped staff, quest/diary state, and clue-dependent dialogue prevent a generic direct transition. |
| 3 | ID 6919, Dorgesh-Kaan entry | Missing quest gate corrected; physical crossing remains unaccepted. |
| 2 | ID 22119, Harmony monastery | Quest-stage explosive-door and gas/gear state need explicit handling. |

[Dorgesh-Kaan](https://oldschool.runescape.wiki/w/Dorgesh-Kaan) requires completion of
`Death to the Dorgeshuun`. A read-only probe on an account without that completion reproduced
`PathfinderConfig.useTransport` returning `usable=true gated=false` for all three ID 6919 rows.
The resource rows now require that quest to be `FINISHED`, and a fail-first regression pins both
the requirement and continued legacy ownership. This fixes route eligibility, not interaction
ownership: ordinary legacy rows remain **1,227** across **256** groups, and the generated total
legacy floor remains **1,875**. Phase 6 is still open.

Rebuilt-client verification at **2026-09-02 22:19:34 BST** repeated the unchanged read-only
probe with `canTestDorgesh=false`: origins `(3318,9602,0)`, `(3317,9602,0)`, and `(3317,9603,0)`
all reported `usable=false gated=true`, and the legacy door count remained `29`. No movement or
physical crossing was attempted. Compilation, all **247** selected route-classification,
transport, core, and pathfinder-benchmark tests, and main/test Checkstyle passed. The temporary
probe was undeployed after verification; the successful negative eligibility check does not close
these doors' live crossing gate.

**`Enter;Hole` audit (2026-09-02):** the 27-row family splits into 12 Crandor entrances,
12 Catacombs of Kourend entrances, one Corsair Cove Dungeon entrance, and two Piscatoris links.
The Catacombs and Corsair rows now reuse the existing engine-owned `CATALOG_TRANSITION` lifecycle,
pinned to `Enter;Hole` and object IDs `28915`, `28919`, `28920`, `28921`, and `31791`.
All 13 inputs have deterministic directed destinations and no item/fare requirements; unlock
conditions still filter the enabled graph before classification. Unknown hole IDs stay legacy-owned.

The audit also reproduced **eight incorrect unlock conditions** in the live resource catalog.
At **22:51:05 BST**, the read-only client probe reported `unlockRows=12 mismatches=8`.
Live object definitions prove `28919` uses varbit `5088`, `28920` uses `5089`, and `28921`
uses `5090`; all three transform to the common `Hole/Enter` object `28915` only at value `1`.
Two base-object rows incorrectly referenced `5080` (Slayer helmet unlock) and `5070` (Monkey
Madness II counter), while six transformed-object rows incorrectly reused the Shayzien unlock.
The resource now applies the location-specific condition to all four approach rows per entrance:

| Entrance | Directed landing | Unlock |
| --- | --- | --- |
| Shayzien | `(1650,9987,0)` | `5088=1` |
| Dark Altar | `(1719,10101,0)` | `5089=1` |
| Lovakengj | `(1617,10101,0)` | `5090=1` |

The fail-first regression checks all 12 rows and independently enables each location to ensure
one unlock cannot admit another entrance. A separate classification regression pins the **13/14**
engine/legacy split and rejects ambiguous directed inputs. The 12 Crandor rows remain legacy-owned
because [Dragon Slayer I entry can trigger a cutscene](https://oldschool.runescape.wiki/w/Dragon_Slayer_I/Quick_guide);
the two Piscatoris rows still need exact interaction evidence. The
[Catacombs holes](https://oldschool.runescape.wiki/w/Hole_(Catacombs_of_Kourend)) unlock by climbing
their respective vines from below, whereas the [Corsair entrance](https://oldschool.runescape.wiki/w/Hole_(Corsair_Cove_Dungeon))
is a direct descent into the eastern dungeon. Physical crossing acceptance remains deferred;
the Catacombs entrances can land beside dangerous monsters and no character movement was used
for this metadata audit.

Rebuilt-client verification at **2026-09-02 23:00:54 BST** repeated the unchanged probe and
reported `rows=27 supported=13 legacy=14` and `unlockRows=12 mismatches=0`. Compilation,
all **250** selected navigation/transport/core/benchmark tests, and main/test Checkstyle passed.
The existing ordinary-legacy count regression now confirms **1,214** rows (13 fewer); the
generated total legacy floor is **1,862**. No new relevant log errors appeared, and the probe
was undeployed after verification. This is runtime metadata/classification proof, not live
crossing acceptance; Phase 6 remains open.

**Catacombs exit-vine slice (2026-09-03):** all 20 exact `Climb-up;Vine` rows now reuse
`CATALOG_TRANSITION`, restricted to IDs `28895`, `28896`, `28897`, `28898`, and `42350`.
They provide four approach rows for each of five destinations: Lovakengj, Shayzien, Dark Altar,
Forthos Dungeon, and Giants' Den. The already-supported central `Vine ladder` is outside this
20-row count. The input audit found no ambiguous directed destinations or item/fare requirements.

The [vine lifecycle](https://oldschool.runescape.wiki/w/Vine_ladder_(Catacombs_of_Kourend)) is
asymmetric: climbing out creates the corresponding entrance unlock, so an exit must not require
that unlock beforehand. A read-only production probe at **2026-09-03 00:19:20 BST** reproduced
`rows=20 supported=0 firstUseBlocked=4` and `usable=16 total=20`. All five live object definitions
exposed `Vine/Climb-up`, with no transform or varbit gate. Four older exit rows incorrectly carried
conditions `5090=1`, `5080=1`, `5070=1`, and `5087=1`; those conditions have been removed rather
than copied from the incoming holes. The incoming holes retain their independent unlock gates.
The original Shayzien row's object-ID discrepancy is not changed by this slice; the existing
bounded exact-name/action fallback remains in place pending scene-level identity verification.

Regressions pin all 20 exit rows' empty prerequisites, five directed destinations, exact policy
identities, and retained incoming-hole locks. Physical crossing and first-use unlock creation remain
live-deferred: the probe reads metadata and eligibility only, without moving into dangerous areas.

Rebuilt-client verification at **2026-09-03 00:27:23 BST** repeated the unchanged probe:
`rows=20 supported=20 firstUseBlocked=0` and `usable=20 total=20`. Compilation, all **253**
selected navigation/transport/core/benchmark tests, and main/test Checkstyle passed; the first
validation attempt could not clean stale resource outputs, and retrying after stopping the client
resolved that build issue without deleting worktree files. No new relevant client-log errors
appeared. The probe was undeployed, the client remained logged in, and no physical crossing was
attempted. The ordinary-legacy regression now confirms **1,194** rows; the total legacy floor is
**1,842**. Phase 6 remains open.

**`Climb;Steps` slice (2026-09-03):** the 22-row family has been audited by exact object
identity and location. Eight rows now reuse the engine-owned `CATALOG_TRANSITION` lifecycle:
four Taverley upper-level links (`30189`/`30190`), one main Waterbirth exit (`8966`), and three
Weiss salt-mine exit approaches (`33261`). All expose `Steps/Climb` in the running client,
have no item/fare requirements, and have one directed destination per complete input. The
pre-change probe at **00:42:39 BST** reported `rows=22 supported=0 legacy=22`.

The remaining 14 rows stay legacy-owned: eight Karuulm links (`34530`/`34531`) pending review of
heat protection/warning behaviour, two Island of Stone links (`37417`) pending exact interaction
evidence, and four two-tile Kurask/Wyvern links (`29993`/`8729`) pending tighter same-plane landing
verification. The current shared scanner's two-tile landing tolerance includes the origin of
those short links; merely treating the name `Steps` as another stair family would acknowledge a
crossing before it happened. The new Steps policy therefore also rejects same-plane endpoints
within that tolerance. This does not fix the shared scanner for previously migrated families;
that is a separate priority follow-up before admitting more short links.

The [Taverley upper-level restriction](https://oldschool.runescape.wiki/w/Taverley_Dungeon) is
described as a restriction on killing dragons, not a prerequisite for climbing the steps; no
unproven Slayer-task gate was introduced. Similarly, the Kurask/Wyvern attack restrictions must not
be copied into travel requirements without evidence. [Karuulm's floor](https://oldschool.runescape.wiki/w/Karuulm_Slayer_Dungeon)
can rapidly damage players without suitable boots or the diary reward, so those rows are not
included in this direct-action slice. No character movement or crossing was attempted.

Verification: after rebuilding and restarting the client, the unchanged read-only probe at
**00:50:44 BST** reported `rows=22 supported=8 legacy=14`. All nine inspected object definitions
still exposed `Steps/Climb` without transforms. Compilation, **255 focused tests** (including the
pathfinder benchmark), and both main/test Checkstyle checks passed. No new relevant errors appeared
in the bounded client-log check. The probe was undeployed and the client remained logged in.
This verifies runtime classification and metadata, not physical crossing acceptance. The
ordinary-legacy regression now confirms **1,186** rows; the total legacy floor is **1,834**.
Phase 6 remains open, with short-link landing acknowledgement the next priority investigation.

**Shared short-link landing fix (2026-09-03):** the read-only production probe at **01:01:19 BST**
examined all 348 engine-classified same-plane catalogue links of up to four tiles. **177** incorrectly
reported `CLEARED` at their own origin; all 348 accepted their exact destination. A two-tile example
also incorrectly cleared its midpoint. `CatalogTransitionRouteScanner` now requires the destination
side of the directed catalogue vector when endpoint tolerance areas overlap, while retaining the
two-tile tolerance for longer links and plane changes. This changes acknowledgement, not eligibility.

A second isolated-engine probe at **01:06:41 BST** reproduced `interaction-edge-crossed` before the
three-tile destination was reached, even when the pending observation remained `AVAILABLE`.
`NavigationEngine` now retains catalogue interactions until landing is explicitly `CLEARED`, rather
than retiring them from nearest-raw-index progress alone. Both missing-source and persistent-source
paths have regression coverage. The scanner's three fail-first regressions and the engine's
fail-first retirement test reproduced their respective conditions before correction; compilation,
**352** navigation/transport/core/benchmark tests, and both Checkstyle tasks passed afterward.

Rebuilt-client verification at **01:14:08 BST** repeated the unchanged probe:
`shortRows=348 sourceCleared=0 destinationRejected=0`. The two-tile origin and midpoint remain
`AVAILABLE`, while the destination and bounded overshoot are `CLEARED`. The isolated engine now
reports `approach=interaction-command-in-flight` and retires only at landing. No new relevant log
errors appeared; an unrelated world-hopper ping `ConnectException` warning was observed. The probe
was undeployed, and no character movement or physical crossing was attempted. This is runtime
verification of the acknowledgement and ownership predicates, not live acceptance of every family.
Eligibility remained unchanged at 1,834 legacy entries before the following item batch.

**Staged jewellery/cape batch (2026-09-03):** the complete 128-row candidate set was audited
together. The unchanged read-only probe reproduced `rows=128 supported=0 legacy=128 definitions=112`
before cutover and reported `rows=128 supported=99 legacy=29 definitions=112` against the rebuilt
client at **01:41:41 BST**. Ninety-nine rows across 27 item families now publish as `ITEM_TELEPORT`.
One engine-owned lifecycle opens the correct tab, dispatches the exact inventory/equipment
destination, and retains the pending interaction through charge replacement/disappearance until
the directed landing is observed. There is no legacy Rub/dialogue orchestration fallback.

All accepted item alternatives are checked against a committed fixture of 112 public live item
definitions. Exact action aliases cover NPC-named diary cape destinations, Hunter/Strength capes,
Digsite pendant, Slayer ring, and inventory/equipment Ring of shadows differences. All 99 rows
continue to participate in the existing shared banking planner. Compilation, **653** selected
walker/banking/transport/navigation/seasonal/core tests including the pathfinder benchmark, and
both Checkstyle tasks passed. The production-classifier corpus test pins **1,735** legacy entries,
including **119** item rows. See the [batch backlog](walker-transport-batches.md) for the 29 exact
jewellery/cape deferrals; this is not live acceptance of high-level or account-locked destinations.

The client was restarted with `:client:run` (the previously tested `runDebug` waits for debugger
attachment), and the unchanged temporary probe was rerun. The welcome overlay initially blocked
the bank approach. After it was dismissed, the user completed Wintertodt's lossy exit confirmation;
that legacy Doors of Dinh preparation is not an item-executor acceptance trace.

**Representative jewellery acceptance:** one banked Ring of dueling(2) was manually withdrawn.
The initial route selected a minigame teleport, so a temporary setup probe saved the original
item/minigame settings, enabled carried-item teleports, and disabled minigame teleports for the
following tests. These are executor tests, not another banked-coordinator acceptance claim.

- **01:51:15-01:51:21 BST:** `item-use:inventory:Emir's Arena`; item 2564 became 2566 (2 -> 1
  charge), followed by `rs2walker:navigation-engine:arrived`. The strict HTTP destination-radius
  check returned `success=false` despite `state=ARRIVED` (landing four tiles from the requested
  offset target with radius three); this first request is not claimed as a clean HTTP pass.
- **01:53:43-01:53:48 BST:** `item-use:inventory:Castle Wars`; the final charge removed item
  2566, inventory became empty, landing was `(2438,3091,0)`, and the request returned
  `success=true state=ARRIVED` with no positional-fallback completion. The target was the exact
  catalogue destination `(2440,3090,0)` with radius six to include the observed landing spread.
- **01:54:09-01:54:16 BST:** `item-open:equipment:Emir's Arena` then exactly one
  `item-use:equipment:Emir's Arena`; the already-equipped ring changed 2552 -> 2554 (8 -> 7
  charges). Landing `(3313,3233,0)` returned `success=true state=ARRIVED`, again without positional
  fallback, for catalogue target `(3315,3235,0)` with radius six. No equipment swap was needed.

All three item runs retained NavigationEngine ownership; the bounded item-test log interval had
no legacy markers or new relevant item/navigation errors. Both probes were removed, the setup
probe restored the original route settings, and the character was left idle at Emir's Arena.
Direct-action capes, high-level/account-locked destinations, and the 29 rejected contracts remain
headless/deferred as documented; representative ring acceptance does not make every destination
live-complete. Phase 6 remains open and Phase 7 has not started.

Temporary probe: `%TEMP%/microbot-debug-probes/phase6-item-batch-20260903/ItemBatchProbePlugin.java`.
Temporary settings/equipment probe: `%TEMP%/microbot-debug-probes/phase6-item-live-20260903/ItemLiveSetupPlugin.java`.

**Caller-radius and final-endpoint correction (2026-09-03):** the jewellery acceptance's strict
radius mismatch was traced to `Rs2WalkerLifecycleRuntime`, which constructed the immutable engine
request from the sidebar default instead of the explicit `walkWithState` distance. A live short-walk
probe at **03:09:44 BST** reproduced `requested=0 configured=5 captured=5 result=ARRIVED remaining=4`.
Destination setup now carries the explicit radius across asynchronous planning; same-destination
replans and cave-route comparison retain it. Cached-route reuse requires an active request with the
same destination and radius. Default-only callers still use the configured radius for fresh requests.

After rebuild and dismissal of the welcome overlay, the unchanged probe at **03:16:42 BST** reported
`requested=0 configured=5 captured=0 result=ARRIVED remaining=0`. A radius-zero HTTP return walk also
returned `success=true state=ARRIVED distanceToDestination=0`, without positional fallback. An earlier
post-restart attempt correctly captured zero but exhausted input acknowledgement while the welcome
overlay was still visible; it is not counted as acceptance.

The subsequent radius-zero Castle Wars teleport test exposed a second previously hidden failure:
nearest-raw progress already pointed to the final index after an off-centre landing, causing
`ROUTE_EXHAUSTED` replans and repeated selection of the same charged teleport. That bounded failed
run issued **four** teleport inputs and consumed **four charges** (equipped ring 2554 -> 2562,
7 -> 3 charges), before ending `UNREACHABLE` one tile away. Test overrides were immediately removed;
no further charged travel was attempted in this correction.

`NavigationEngine` now approaches its existing raw endpoint without a replan when that endpoint
satisfies the caller's radius, is on the same plane, and is within normal click reach. This exact
final approach prefers canvas input; normal acknowledgement/recovery limits remain intact. A partial
endpoint outside the goal radius still replans. The regression first failed with
`expected CLICK_TILE but was REQUEST_REPLAN`, then passed after correction.

A non-consuming live probe simulated only the completed teleport in an isolated engine and exercised
the real ground-click adapter. Before correction it reported `first=REQUEST_REPLAN issued=false
remaining=1`; after the final rebuild at **03:24:40 BST** it reported `selection=route-end-approach
first=CLICK_TILE issued=true remaining=0 result=COMPLETE`. This verifies the actual final ground
input without claiming a second full charged-teleport run. **659** selected regressions, the
pathfinder benchmark, compilation, and both Checkstyle tasks passed. No new relevant errors appeared
in the final bounded log check. Probes were removed, settings restored, and the character left idle
at `(2440,3090,0)` with the three-charge ring still equipped. No files were staged or committed.
Ownership counts remain **1,735** legacy entries / **119** item entries; Phase 6 remains open.

Temporary probes:
`%TEMP%/microbot-debug-probes/phase6-radius-20260903/RadiusProbePlugin.java` and
`%TEMP%/microbot-debug-probes/phase6-radius-end-20260903/RadiusEndProbePlugin.java`.

**Second staged-item batch (2026-09-03):** all 64 remaining other-item candidates were reviewed
together against the live client definitions. **44 rows across 16 families** now use the existing
`ITEM_TELEPORT` scanner/scene and NavigationEngine lifecycle: diary cloak/gloves/legs/blessing,
memoirs and Book of the dead, lyres, teleport crystals, selected medallion/sceptre destinations,
GE/Yanille tablets, and basalts. Inventory-only items explicitly return no equipment action.
The action fixture now contains **166** public item definitions; every accepted alternative is
checked for an exact inventory action and, where supported, an exact equipment action.
All 143 migrated item rows still participate in shared bank planning.

Three fail-first regressions demonstrated absent ownership, missing action aliases, and missing
basalt quest requirements. The patch reuses the existing executor and adds no movement owner.
Ardougne cloak/legs have distinct worn actions; lyres require the client's `Jatiszo` spelling.
Stony basalt now selects `Troll Stronghold entrance` or `Troll Stronghold roof` from the
directed coordinate, never its toggle-dependent default. Both stony rows and the icy row now
require Making Friends with My Arm; the roof retains its existing Agility/diary gates.
These requirements and explicit actions were cross-checked with the
[stony basalt](https://oldschool.runescape.wiki/w/Stony_basalt) and
[icy basalt](https://oldschool.runescape.wiki/w/Icy_basalt) wiki pages through the wiki MCP.

The rebuilt-client, unchanged metadata probe at **13:57:13 BST** reported
`rows=218 supported=143 legacy=75 definitions=166`, versus `supported=99 legacy=119`
before the patch. All 166 definitions matched the test fixture, with zero mismatches.
Compilation, **663** walker/banking/transport/seasonal/shortest-path regressions including
PathfinderBenchmarkTest, and both Checkstyle tasks passed. No new relevant errors appeared in
the rebuilt-client output. This is runtime classification/metadata verification, **not physical
live acceptance of the 44 added routes**. Available memoirs had no charges/page unlock, and
the inspected Grand Exchange tablet destination was locked, so neither was used.

The remaining 20 other-item rows are 14 Quetzal whistles; two consumable rows containing inert
IDs (Calcified moth and Mokhaiotl waystone); and Ardougne Farm, Chronicle, Slepe, and Jaltevas.
Those four need daily-use, planner-owned charge-input, Slepey-tablet unlock, or obelisk-attunement
work respectively. Keeping them legacy-owned does not validate their existing prerequisites.
Master Scroll Book (18), POH tablets (8), and the prior jewellery/cape deferrals (29) remain.
Current whole-catalogue ownership: **1,691 legacy rows**, including **75 item rows**.
Phase 6 remains open; no Phase 7 or legacy orchestration deletion is authorized by these counts.

Temporary probe:
`%TEMP%/microbot-debug-probes/phase6-item-other-20260903/ItemOtherProbePlugin.java`.
It was undeployed. No route settings, inventory items, charges or movement inputs were changed
by the test; only the bank was opened/read/closed before restart and the welcome screen dismissed
after configured login. The reported position changed across restart and was moving at final
cleanup, so no idle/safe-position assertion or further in-game action was made. No files committed.

**Follow-up prerequisite audit (2026-09-03):** the 14 whistle rows remain deferred: their map
is interface 949, not Renu's map, and saved setting varbit 19681 can redirect `Signal` straight
to Hunter Guild. Numeric mode semantics and per-whistle charges still require controlled
acceptance. All six destination-build gates are already encoded. The detailed checklist is in
[walker-transport-batches.md](walker-transport-batches.md#quetzal-whistle-audit-and-consumable-prerequisite-corrections-2026-09-03).

The review also reproduced a real bank-planning data bug: two calcified-moth uses requested
only one item because `Consumable=Y` is not recognized by `Transport`. Corrected that field
to `T`, the moth Wilderness ceiling from 29 to 20, and removed inert item-ID alternatives from
both moth and waystone rows. All three new tests failed first; all **666** selected regressions,
compilation, benchmark and both Checkstyle tasks then passed. The unchanged read-only live probe
confirmed real IDs only and two items for two uses. It was undeployed; no game inputs were sent.
No production Java bytecode changed: the probe re-read built resources without restarting, but
an already-cached active route catalogue still needs its normal reload/restart. No physical
teleport acceptance is claimed. Moth landing/partial-quest semantics remain deferred, and neither
row was migrated by this data fix. Counts remain **1,691 legacy / 75 item legacy / 143 migrated
item rows**. No Phase 6 gate was marked complete by this audit.

**Config bypass and Fossil Island rowboat correction (2026-09-03):** the live read-only probe
reproduced `useMagicMushtrees=false` with twelve ordinary rows passing the feature check and
six still published. Dedicated mushtree rows were correctly excluded; ordinary shadow rows
were classified only as `TRANSPORT` by the feature filter. The same flaw affected two ordinary
fairy-ring rows. `PathfinderConfig.isFeatureEnabled` now resolves those exact identities to their
network feature type before using the existing switches; original route classification stays intact.

The pictured dock rowboat (`30914` at `3723,3805,0`) was also reproduced live: `Travel` opens
`Row to the barge.`, `Row to the barge and travel to the Digsite.`, and `Cancel.`. Its blank display
field incorrectly made it direct-owned. `ships.tsv` now supplies the full Digsite option, reusing
the existing `NPC_DIALOGUE_TRANSPORT` lifecycle with no new movement or dialogue owner.

Three regressions failed first (both shadow families and the missing option); all **670** selected
regressions, compilation, PathfinderBenchmarkTest, and both Checkstyle tasks passed after the fix.
The switch tests cover all **22 boolean family switches**, each enabled and disabled, plus all
twelve ordinary mushtree and both ordinary fairy-ring rows. The client was rebuilt and restarted
with `:client:run` (`runDebug` suspends awaiting a debugger); configured login succeeded.

The unchanged probe then reported:
```text
live.useMagicMushtrees=false
rowboat.direct=false dialogue=true option=Row to the barge and travel to the Digsite.
feature.allowed ordinary=0 dedicated=0 published=0
```
Both normal live walks passed without config overrides or item use:

- **14:40:56–14:41:01 BST**, dock -> `(3362,3445,0)`: one `Travel` actor input, the exact Digsite
  option, one `dialogue-continue`, then `navigation-engine:arrived`; HTTP `ARRIVED`, distance zero.
- **14:41:39–14:41:44 BST**, Digsite -> `(3724,3807,0)`: direct Barge guard `Quick-Travel`, one
  exact endpoint ground step, then `navigation-engine:arrived`; HTTP `ARRIVED`, distance zero.

No legacy movement ownership was used in either trace. The post-walk probe still reported zero
published mushtree routes, including after cached refresh. No new relevant walker/pathfinder/
dialogue errors appeared in the rebuilt-client log. Settings were unchanged; the character was
left idle at `(3724,3807,0)` and the probe was undeployed. Counts remain **1,691 legacy / 75 item
legacy / 143 migrated item rows**; this corrects an existing route's protocol, not Phase 6 closure.

Probe: `%TEMP%/microbot-debug-probes/walker-config-dialogue-20260903/WalkerConfigDialogueProbePlugin.java`.
Trace: `%TEMP%/walker-config-client-20260903.out.log`. No files were staged or committed.

**Fossil Island rowboat unlock gate (2026-09-03):** the camp menu was live-reproduced with only
barge, barge-and-Digsite, and Cancel, while both camp-to-north and camp-to-sea BOAT rows were
`allowed=true published=true`. The [rowboat wiki](https://oldschool.runescape.wiki/w/Rowboat_(Fossil_Island))
confirms these destinations require discovery trips. No verified unlock varbit was found in the
MCP catalogue. The committed legacy `handleObjectExceptions` has no such guard: it presses the
first character of the numbered display label and waits, which can select a different destination.

The client now observes complete camp menus on ticks, publishes immutable negative availability,
and invalidates transport memo snapshots when that evidence changes. Only the two outgoing camp
BOAT rows are excluded; Digsite and discovery directions remain available. Later menus can restore
destinations, and login-screen reset prevents cross-account carry-over. This remains session-learned
availability, not a pre-arrival varbit gate. Existing pending edges produce `UNAVAILABLE` for the
engine's bounded replan even after their rows disappear from the published catalogue. The six
island BOAT rows use unique destination text rather than fixed menu ordinals in the engine adapter;
the legacy catalogue labels and handler were not rewritten.

Compilation, both Checkstyle tasks, and **330** selected transport/navigation/feature-gate tests
including PathfinderBenchmarkTest passed (zero failures/errors/skips). The new gate API tests
initially failed compilation before implementation; the behavioral failure was established by
the live probe above. Rebuilt client (`:client:run`; `runDebug` awaits debugger attach) replayed
the unchanged probe with the same menu:

```text
camp -> north: BOAT allowed=false published=false
camp -> sea:   BOAT allowed=false published=false
camp -> Digsite: SHIP allowed=true published=true
```

Normal travel still passed: **15:10:28–15:10:35 BST** camp-to-Digsite (`Travel`, exact Digsite
option, continue, engine arrival), and **15:11:23–15:11:28 BST** return (`Quick-Travel`, engine
arrival). Both HTTP results were `ARRIVED`, distance zero. No legacy movement ownership was used.
Unlocked north/sea travel is **headless-only**, not live-accepted. No unlock trip was attempted.
Two startup blocking-event timeouts occurred in WelcomeScreenEvent/BankTutorialEvent during
login transport refresh; no further errors appeared during rowboat verification.

Probe: `%TEMP%/microbot-debug-probes/fossil-rowboat-gate-20260903/FossilRowboatGateProbePlugin.java`.
Trace: `%TEMP%/fossil-rowboat-client-20260903.out.log`. No settings or items changed; character
returned idle to `(3724,3807,0)`. The probe was undeployed and all changes remain uncommitted.
This is a bug fix, not a Phase 6 closure or migration-count change.

**Locked-rowboat recovery follow-up (2026-09-03):** the first true end-to-end locked route exposed
that filtering alone was insufficient. `Travel` was issued at **16:25:05 BST**, but the definitive
missing destination remained behind the generic dialogue-voyage acknowledgement deadline until
**16:26:02 BST**. The rowboat menu therefore sat open for about **57 seconds** before a replan; the
user supplied no input. The replan then walked overland to the north pier and arrived. This was not
acceptable immediate recovery, so the earlier live-acceptance wording above is narrowed accordingly.

A verified complete camp menu that omits the planned destination now produces an engine-owned
`dialogue-cancel-unavailable` stage. After Cancel closes the menu, a distinct unavailable action
supersedes the in-flight Cancel and requests the replan on the next observations rather than after
60 seconds. The generic long voyage timeout is unchanged. The adapter revalidates the exact camp,
menu anchors and missing destination before issuing Cancel, so foreign or changing menus receive no
input. A destination/continue action already in flight suppresses both lock learning and Cancel;
this was required after live testing caught a closing-menu snapshot trying to cancel an available
camp-to-sea voyage after `dialogue-destination:Sea` had already been issued.

All **333** selected transport/navigation/feature-gate tests passed with zero failures/errors/skips,
including the two-stage Cancel/immediate-replan regression and PathfinderBenchmarkTest; compilation
and both Checkstyle tasks passed. The final rebuilt-client return from sea passed cleanly at
**16:43:13–16:43:19 BST**: one `Travel`, `dialogue-destination:Camp`, final endpoint approach and
engine arrival, with no false Cancel or recovery. The camp menu then showed both north and sea, so
this account is now fully unlocked and cannot repeat the locked-menu live gate. Immediate Cancel of
a genuinely locked menu remains captured-live/headless verified, not live-accepted after the fix.
The character was left idle at camp, no items/settings changed, and nothing was committed.

**Cabin Boy Herbert dialogue cutover (2026-09-03):** all ten remaining dedicated `BOAT`/`SHIP`
rows were audited as complete protocols. The four exact Herbert rows now publish as
`NPC_DIALOGUE_TRANSPORT`; NavigationEngine owns `Talk-to`, optional Continue frames, the exact
`Can you take me somewhere?` request, the unique `Travel to <destination>.` option, and the
directed landing. Eligibility is restricted to the two packaged NPC IDs/origins, four exact
destinations, zero fare/items/var-state, duration six, and completed `A Kingdom Divided`.

At that point six dedicated rows remained legacy-owned: three Captain Shanks rows, two
variable-dialogue Pirate Pete rows, and Ghost Captain's outbound row whose true ecto-token
requirement can be 25, 10 with Ring of Charos(a), or free after a permanent unlock. The free Ghost
Captain return was already engine-owned. This Herbert cutover reduced the legacy floor from
**1,691 to 1,687** and SHIP legacy rows from seven to three; BOAT remained three.
All **337** selected regressions, compilation, Pathfinder benchmark, and both Checkstyle tasks passed.
A read-only probe against the existing client observed `A Kingdom Divided=NOT_STARTED`, four packaged
Herbert rows and zero active-account published Herbert rows. That verifies the unchanged quest gate
and account prerequisite, not the new executor bytecode; the active client needs a normal restart
to load this patch. Physical Herbert travel remains live-deferred until an eligible account is
available. No gameplay input was issued and the temporary probe was undeployed.

**Captain Shanks dialogue cutover (2026-09-04):** the stale three-row resource contract is now the
two destinations exposed by the live transcript: Khazard Port `(2680,3150,0)` and Port Sarim
`(3050,3192,0)`. Both rows name their destination, require completed `Shilo Village`, and publish
only through the exact Captain Shanks `NPC_DIALOGUE_TRANSPORT` policy. The false Entrana-area
`(3047,3235,0)` destination was deleted. Route planning retains a conservative 50-coin maximum;
live dialogue offered a random price in the documented 20-50 range, bought item `621` on site, and
consumed that ship ticket when sailing.

The rebuilt-client run at **16:19:29-16:19:52 BST** issued `Talk-to`, consecutive Continue frames,
the unique affirmative fare option, further Continue frames, `dialogue-destination:Khazard Port`,
and the final sailing Continue without external dialogue input. It paid 42 coins, crossed the
temporary instanced ship scene under the original NavigationEngine interaction, landed exactly at
`(2680,3150,0)`, and cleared as `rs2walker:navigation-engine:arrived` with no recovery or relevant
error. The live run exposed two generic acknowledgement defects: identical consecutive Continue
pages had inherited the 60-second voyage timeout, while shortening every Continue also abandoned
ownership during the real voyage. Visible Continue pages now retry after two seconds; disappearance
promotes the same command to the existing 60-second remote-landing window, and a reappearing page
advances immediately. Arrival Continue cleanup now uses the already-owned edge rather than
re-resolving a post-payment row that may have left the affordable catalogue. The voyage lifecycle is
live-accepted; the final arrival-page cleanup is focused-test complete and awaits its normal rebuilt-
client observation.

**Dedicated boat dialogue closure (2026-09-04, headless):** all 166 generated `BOAT`/`SHIP`
rows now have an explicit engine classification; no dedicated row remains legacy-owned. Pirate
Pete's two quest-finished rows use the wiki-verified `Talk-to` protocol: zero-or-more bounded
Continue pages, the exact unique `Okay!` option, the random post-selection Continue sequence, and
the directed landing. Ghost Captain's outbound row uses `Travel`, equips one carried ghostspeak
alternative (ghostspeak amulet, enchanted ghostspeak amulet, or Morytania legs 2-4), selects the
single affirmative ecto-token payment option, and retains the landing under NavigationEngine.
Its resource row conservatively requires 25 ecto-tokens; the newer 500-token permanent-free unlock
and Ring of Charos(a) discount remain deliberately unoptimised until their persistent state and
live variants are observed. Banking now aggregates currency and reusable-item requirements on the
same edge, so this route requests both the 25-token fare and one wearable instead of silently
choosing one requirement class. Focused dialogue, route-scanner, classifier, requirement-policy,
and banking tests pass. These three rows are not live-accepted because the current account has not
completed `Rum Deal` or `Ghosts Ahoy`; do not mark their live gate complete.

**Mountain Guide shadow-row cutover (2026-09-04, headless):** the six ordinary `TRANSPORT`
duplicates of the already-migrated Mount Quidamortem network now publish through the same
`NPC_DIALOGUE_TRANSPORT` lifecycle. Eligibility pins NPC ID `7600`, the six exact directed
origin/destination/display tuples, `Travel`, duration seven, and unlock varbit `5421=1`; it does
not widen generic ordinary-transport eligibility. NavigationEngine owns the actor click, exact
destination selection, and remote landing. The stale resource labels' `1:`/`2:` prefixes are
removed before matching the current three-destination menu documented by the wiki transcript.
This reduces the whole-catalogue legacy floor from **1,671 to 1,665**. Physical acceptance remains
deferred until the account is near the network; the equivalent NPC-table protocol was already
engine-owned, but that is not claimed as live acceptance for these shadow coordinates.

**One-click ordinary NPC transport cutover (2026-09-04, headless):** seven more ordinary
`TRANSPORT` rows now use the existing direct `NPC_TRANSPORT` lifecycle: Brother Tranquility's two
`Transport` directions, Daero's one `Travel` route, Waydar's two `Travel` routes, and Primio's two
`Travel` routes. The wiki NPC definitions expose those actions directly; unlike Mountain Guide and
Dondakan, they do not require destination-menu ownership. Eligibility pins all seven complete
route identities and preserves Primio's completed `Children of the Sun` gate. Exact-ID resolution
remains preferred, with a bounded exact-name/action/origin fallback for transformed Brother
Tranquility variants. NavigationEngine owns the single actor command and directed landing. This
reduces the whole-catalogue legacy floor from **1,665 to 1,658**. Physical acceptance remains
deferred until a rebuilt client is positioned at an eligible endpoint.

**Dondakan equipment/dialogue cutover (2026-09-04, headless):** the single ordinary
`Talk-to;Dondakan the Dwarf` route now publishes as `NPC_DIALOGUE_TRANSPORT`. The exact contract
requires completed `Between a Rock...` and one reusable gold helmet (`4567`), so the shared bank
planner can obtain it when available. NavigationEngine owns helmet equipment, the actor click,
arbitrary Continue pages, the exact `Can you shoot me into the rock again?` option, and arrival in
Dondakan's mine. The helmet is deliberately left equipped because it is the mine's traversal gear.
No other ordinary Talk-to row is admitted. This reduces the whole-catalogue legacy floor from
**1,658 to 1,657**. Physical acceptance remains deferred until a rebuilt client is positioned at
the quest-gated endpoint with the required helmet; it is not claimed from headless coverage.

The same runtime audit found a separate set of **45 ordinary `TRANSPORT` candidates** whose action
or actor name looks travel-related: Brother Tranquility (2), Raft (2), Fremennik Boat (7),
Dondakan (1), Mountain Guide (6), Al Kharid toll gates (4), Log raft (2), Waydar (2), Primio (2),
Daero (1), Aged log (2), quick-pass barriers (10), and four Energy Barrier variants. This is an
audit queue, not a dialogue allowlist: several are direct object crossings, while Talk-to and
generic Travel rows require exact per-family option and unlock evidence before migration. The six
Mountain Guide shadows, seven one-click NPC rows, and Dondakan above are now closed, leaving 31
candidates to reconcile against their actual object protocols and existing exact policies; this
number is not a claim that all 31 remain legacy-owned. The final four Energy Barrier variants are
now reconciled below, closing this travel-looking candidate queue.

**Deterministic raft-exit reconciliation (2026-09-04, headless):** the two Waterfall `Board;Log
raft;1987` rows and two Ancient Cavern `Ride;Aged log;25216` rows now reuse the engine-owned
`CATALOG_TRANSITION` lifecycle. Eligibility is pinned to all four complete directed row identities;
NavigationEngine owns the single object command and remote landing. The two Hazeel Cult raft rows
remain legacy-owned because the same `Board` action can return to an intermediate island or fail
according to quest-stage sewer-valve state that the catalogue does not encode. Seven stale ordinary
Fremennik Boat duplicates were removed: the identical canonical `BOAT` rows already use
`NPC_TRANSPORT`, carry the required completed `The Fremennik Exiles` gate, and use the audited
object-backed scene resolver. This reduces the whole-catalogue legacy floor from **1,657 to 1,646**
(four migrations and seven removed unsafe duplicates). Physical acceptance of the raft exits remains
deferred until a rebuilt client can be positioned safely at those endpoints.

**Al Kharid toll-gate cutover (2026-09-04, headless):** all four exact pre-quest
`Pay-toll(10gp);Gate` rows now use the adjacent engine lifecycle. The allowlist pins both lanes,
both directions, object IDs `2786`-`2789`, the ten-coin requirement, and the otherwise empty
requirement shape; it does not admit other paid or adjacent objects. The shared requirement and
banking policies still own coin availability and withdrawal, while NavigationEngine owns the
single gate action and directed one-tile landing. The completed-`Prince Ali Rescue` `Open` rows
remain the already-supported free alternative. This reduces the whole-catalogue legacy floor from
**1,646 to 1,642**. A rebuilt live crossing remains required before claiming physical acceptance.

**Guardians of the Rift barrier cutover (2026-09-04, live accepted):** all ten exact
`Quick-pass;Barrier;43700` rows now use `CATALOG_TRANSITION`. The resource rows require completed
`Temple of the Eye`; the five outside-to-inside rows additionally require `GOTR_IS_PLAYING`
varbit `13691` to equal zero, matching the observed outside/player-not-participating state. The five
inside-to-outside rows deliberately remain independent of that state so NavigationEngine can always
own an exit. Eligibility is pinned to object `43700`, the exact action/name, x coordinates
`3613`-`3617`, y coordinates `9482`/`9484`, plane zero, and the directed two-tile geometry. This
reduces the whole-catalogue legacy floor from **1,642 to 1,632**. Focused parsing, ownership, and
negative-contract tests pass. Rebuilt-client acceptance passed in both directions at x `3615`:
outside-to-inside issued one `selection=interaction-catalog_transition stage=Quick-pass` command,
arrived at y `9484`, and changed varbit `13691` from `0` to `1`; inside-to-outside published while
the varbit was `1`, issued the same engine-owned interaction, arrived at y `9482`, and returned the
varbit to `0`. Neither crossing opened dialogue or emitted legacy ownership for the barrier leg.

**Temple of the Eye portal cutover (2026-09-04, live accepted):** the two exact portal
rows connecting `(3104,9573,0)` and `(3615,9470,0)` now use `CATALOG_TRANSITION`. A mixed long-route
probe exposed these rows as the reason the entire request reported `mode=LEGACY_LOCKED`; both fixed
`Enter;Portal` interactions then completed live without dialogue and landed at their declared remote
endpoint. Eligibility is pinned to the complete directed identities, including object `43841` from
the temple and object `43692` from the rift lobby. This reduces the whole-catalogue legacy floor from
**1,632 to 1,630** and prevents this pair from forcing otherwise migrated GOTR routes into legacy
ownership. Rebuilt-client acceptance passed in both directions: `(3615,9470,0)` to
`(3104,9573,0)` issued one `selection=interaction-catalog_transition stage=Enter` command at
23:35:24 and arrived at the exact endpoint at 23:35:26; the reverse issued the same engine-owned
command at 23:35:45 and arrived exactly at 23:35:47. Both requests ended with
`rs2walker:navigation-engine:arrived` and emitted no legacy ownership marker.

**Hazeel Cult raft cutover (2026-09-04, headless):** both exact `Board;Raft;2849` rows between
`(2567,9680,0)` and `(2606,9692,0)` now reuse `CATALOG_TRANSITION`. This preserves the legacy
contract, which issues the object action and waits for the declared landing; it does not configure
the five Hazeel Cult sewer valves. Correct valve state reaches the directed endpoint, while an
incorrect-island landing cannot acknowledge the edge and therefore replans. Exact origin,
destination, object, action, and name checks reject neighboring raft contracts. Focused policy and
production-corpus tests pass, reducing ordinary legacy rows from **1,135 to 1,133** and the complete
generated legacy floor from **1,630 to 1,628**. Physical acceptance remains deferred until the
account is safely positioned at the quest raft with the valves already configured.

**Port Phasmatys Energy Barrier completion (2026-09-04, headless):** all 16 exact
`Pay-toll(2-Ecto);Energy Barrier;16105` rows now reuse `CATALOG_TRANSITION`. The four formerly
ambiguous west-gate rows are mutually exclusive by quest state: duration-two rows describe the
paid pre-`Ghosts Ahoy` landing, while the three-tile quest-gated rows describe the free completed-
quest landing. The shared requirement policy supplies two ecto-tokens and one reusable ghostspeak
alternative before completion, and that same effective requirement feeds path eligibility, cache
invalidation, route analysis, and bank withdrawal. Dispatch equips the ghostspeak item as a
non-blocking preparatory command, then issues the exact barrier action on the same owned edge. On
completed accounts the resolver accepts the modern transformed barrier's `Pass` action while
retaining the catalog identity and directed landing. Repeated-route tests prove two paid crossings
sum to four ecto-tokens but one reusable amulet. Focused policy, banking, classifier, scanner, and
Checkstyle tests pass, reducing ordinary legacy rows from **1,133 to 1,129** and the complete
generated legacy floor from **1,628 to 1,624**. Physical acceptance remains required; no live
Energy Barrier crossing is claimed here.

**External movement and combat recovery correction (2026-09-05, headless):** a live report and
historical `off-route-movement-in-flight` traces exposed that generic moving/animating/interacting
flags could defer recovery until the player stopped. `NavigationEngine` now retains the wait only
when the current movement destination still matches an engine-issued route command. A later manual
click to a divergent destination invalidates that ownership and requests an immediate categorized
replan even after the original command was acknowledged; off-route combat displacement and external
replan requests likewise no longer wait for combat animation or interaction state to clear. Engine-
owned movement and staged transport acknowledgement remain protected. Focused navigation tests,
the navigation shadow corpus, both Checkstyle tasks, and the broader navigation/transport suite
pass after updating the old wait contract. Rebuilt-client live acceptance is still required.

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

**Farmable spirit-tree availability correction (2026-09-03):** a live Catherby-to-Ardougne request
for `2573,3322,0` chartered to Brimhaven and stopped at `2800,3204,0` because the profile explicitly
enabled the Brimhaven player-grown tree even though no `Travel` object existed. The five optional
farmable-tree toggles now default off. A stale opt-in is also detected conservatively within four
tiles of its directed origin, removes both directions from the session graph, and replans instead of
repeating the dead route. With `spiritTreeBrimhaven=false`, the same request live-passed from the
Brimhaven dock: NavigationEngine selected Captain Barnaby's `Ardougne` dialogue transport, crossed
the landing gangplank, retained ordinary movement ownership, and arrived exactly at `2573,3322,0`.

**Transport-cost calibration (2026-08-25):** path search and direct-vs-bank comparison now share
`TransportCostModel` instead of letting reconstructed `path.size()` reduce every transport to one
tick. Successful live traces establish conservative floors of 24 ticks for staged fairy-ring travel
and 12 ticks for spirit-tree menu travel; explicit larger catalog durations still win, and every
other interaction costs at least one tick. Live glider inspection confirmed its existing eight-tick
catalog duration is already conservative, so the shared model records an explicit eight-tick glider
floor. Transport debug output reports both catalog `duration` and effective `routeTicks`. Add a
measured floor for each specialised family as it is migrated. The accepted Quetzacalli
Gorge/Civitas illa Fortis traces establish a ten-tick Quetzal floor from destination selection
through directed landing.

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

**Quetzal slice closed (2026-08-30):** directed `QUETZAL` rows publish as a live Renu `Travel`
stage, exact `InterfaceID.QuetzalMenu` destination stage, and directed remote landing. The network
catalog intentionally has no stable NPC id, so the cache-backed resolver accepts transforming Renu
definitions only with exact name/action, correct plane, and bounded origin proximity; live ids are
telemetry and may change between observations. Quetzal-whistle item teleports remain legacy-owned.
Live timing raises the catalog's six-tick duration to a ten-tick route-cost floor. Compile,
Checkstyle, policy/scanner, publication, ownership, remote-retention, and
navigation lifecycle tests pass. Forward live acceptance passed from Quetzacalli Gorge to Civitas
illa Fortis on request 2/generation 1: the engine issued exactly one Renu `Travel` command and one
exact `Civitas illa Fortis` destination command, acknowledged the directed landing, and arrived
without duplicate input, replan, recovery, or legacy fallback. The reverse request selected
`Quetzacalli Gorge` exactly once, acknowledged its landing, resumed ordinary walking, and arrived
on request 3/generation 1 with the same clean lifecycle. A third request targeted grey locked Cam
Torum: the transport refresh observed the changed unlock varbit, excluded that Quetzal edge before
interaction, and completed via ordinary walking without issuing any Quetzal command. Both live
directions and locked-destination eligibility are accepted; the family is closed.

**POH physical-portal foundation live-accepted (2026-08-30):** the saved exit-portal tile remains a
logical house-template coordinate while the exact live `POH_EXIT_PORTAL` is resolved globally from
the shared tile-object cache at its loaded instance coordinate. The graph now publishes separate
`TransportType.POH` edges for outside `Portal/Home` and inside `Portal/Enter`; the inside edge belongs
to the outgoing-POH set so it remains present when world-to-POH teleports are intentionally omitted.
Both directions are navigation-owned catalog transitions. With the master `Use POH` toggle enabled,
live Rimmington acceptance issued exactly one `Enter` stage from logical `1859,7052` and landed
outside, then exactly one `Home` stage from `2954,3224` and arrived back at the saved logical tile.
The shared route-cost model applies a conservative six-tick POH floor from these measured loads.
The empty level-48 test house has no portal chamber, nexus, jewellery box, mounted teleports, fairy
ring, or spirit tree, so those configured facility executors remain headless/live-deferred rather
than being claimed complete.

**Seasonal transport slice headless-complete, live-deferred (2026-08-30):** the packaged seasonal
catalog contains 47 Clue compass rows and 122 Map of Alacrity rows. Clue compass destinations reuse
the proven direct inventory/submenu action contract. Map of Alacrity restores the historical
two-stage `187:3` picker contract: select region, then destination, skip `<str>` locked rows, and use
the menu hotkeys for entries below the scroll fold. Both families classify through the existing
engine-owned remote teleport lifecycle and retain their interaction until directed landing. The
transport refresh now requires an explicit registered executor before publishing any seasonal row,
so an unknown or malformed future seasonal definition cannot enter a retry loop. A newly observed
locked Map region/destination is session-disabled and invalidates the transport memo before replan.
All 169 packaged rows, parsing/normalisation/hotkey behavior, engine classification, compile, and
focused unit tests pass. Live acceptance remains intentionally deferred because these items and
interfaces only exist on the corresponding League server.

**Banked-transport setup live-accepted (2026-08-30):** route analysis and
withdrawal planning now share one eligibility predicate, including item-gated ordinary transports
and all registered seasonal item teleports. Originless route extraction uses
`TransportType.isTeleport`, so Clue compass and Map of Alacrity edges are no longer omitted from the
bank plan. Reusable requirements use the maximum needed by any edge; consumables, runes, and fares
sum across uses; and withdrawals subtract the quantity already carried. Pure currency rows remain
currency rows instead of mutating the shared transport catalog. `BankedTransportCoordinator` owns
the explicit `WALK_TO_BANK -> OPEN_BANK -> WITHDRAW_ITEMS -> CLOSE_BANK -> PREPARE_FINAL_ROUTE ->
WALK_TO_TARGET` transaction, aborts before the target leg if a requirement cannot be satisfied, and
delegates both walking legs to the navigation engine. Coordinator, requirement-planning, and compile
checks pass. Live acceptance used a Draynor manor teleport tablet (`19615`) held only in Draynor
Bank, with zero carried. The rebuilt client compared 112 direct ticks with a six-tick banked route,
published one bank-leg item transport, and emitted the complete transaction in order:
`WALK_TO_BANK -> OPEN_BANK -> WITHDRAW_ITEMS` (one tablet, carried zero) `-> CLOSE_BANK ->
PREPARE_FINAL_ROUTE -> WALK_TO_TARGET -> COMPLETE`. The final leg stayed engine-owned, issued one
`interaction-simple_teleport` command for the Draynor Manor tablet, consumed the tablet, observed
the directed landing, and cleared as `rs2walker:navigation-engine:arrived`; no
`LEGACY_LOCKED` marker or legacy movement handoff appeared.

The live pass exposed and fixed two test-only/production-boundary defects before acceptance.
`TransportCostModel` had queried originless transports through a null key, which works with the
old test `HashMap` but throws for production's `ConcurrentHashMap`; originless costs now read
the transport set published at the route origin, and the regression test uses the production map
shape. Separately, Agent Server's waiting wrapper used to cancel the walker future immediately when
its independent position poll saw the teleport landing. It now gives the owning walker ten seconds
to complete normally before position becomes a bounded fallback, so live automation cannot pre-empt
the coordinator's final acknowledgement.

**Banked-requirement completeness hardening (2026-08-31, headless):** withdrawal planning now
aggregates the complete selected route rather than only transports that fail an isolated one-edge
availability check. Repeated tablets, charged-jewellery uses, spell casts, and fares therefore cannot
all consume the same single carried item/rune stack unnoticed. Inventory accounting uses stack
quantity (not occupied slots), so coins, ecto-tokens, runes, and stacked consumables subtract
correctly. Item IDs encoded for charged jewellery, trimmed/untrimmed capes, and similar variants are
treated as OR alternatives consistently with the pathfinder; a requirement can be fulfilled across
multiple banked charge variants. Reusable capes, tools, fairy-ring staves, and seasonal relics require
one item, while consumables and currencies aggregate conservatively across route uses. Spell planning
aggregates raw rune costs, honors inventory/rune-pouch/already-equipped staff supply, and can select
banked combination runes that cover both elemental requirements. Focused catalog-driven tests cover
Draynor Manor tablets, ring of dueling variants, Crafting capes, repeated Varrock spell casts,
combination runes, coins, ecto-tokens, seasonal rows, and ordinary item-gated transports.

The expansion as a whole remains headless-complete only. The existing live Draynor-tablet trace
still proves the coordinator transaction and engine ownership, but does not by itself live-accept
every newly covered requirement category. A rebuilt-client spot-check additionally proved charged-
jewellery variant selection and withdrawal: a banked Necklace of passage(5), item `21146`, was
withdrawn with none carried and became Necklace of passage(4) after use; a banked Ring of dueling was
also selected and consumed one charge. Both runs emitted the ordered bank-setup phases, withdrawal,
final replan, and arrival. Those destination-submenu item-teleport routes still classified as
`LEGACY_LOCKED`, so this is live evidence for banking coverage and inventory accounting only, not
acceptance of migrated interaction ownership. Withdrawing a banked elemental staff is also
intentionally not enabled yet: that needs an explicit equip, post-teleport restore, and ownership
contract rather than treating a staff as ordinary inventory. Charged-jewellery capacity is not
modeled per item ID yet, so repeated uses conservatively plan one banked jewellery item per edge even
when one high-charge item could cover several uses.

**Banked currency/equipment follow-up live acceptance (2026-09-05):** a rebuilt-client forced-bank
route from Canifis Bank to the paid Port Phasmatys Energy Barrier selected exactly two ecto-tokens
(`4278`) and one ghostspeak amulet (`552`) from the bank with an empty inventory. The coordinator
emitted `WALK_TO_BANK -> OPEN_BANK -> WITHDRAW_ITEMS` (two tokens, then one amulet)
`-> CLOSE_BANK -> PREPARE_FINAL_ROUTE -> WALK_TO_TARGET -> COMPLETE`; the final leg remained
engine-owned, equipped the amulet as an explicit preparation-only action, issued the barrier
`Pay-toll(2-Ecto)` interaction on the following pass, consumed both tokens, and returned `ARRIVED`
at `3655,3485,0` without manual movement or a legacy ownership marker. The controlled run disabled
competing spell teleports, charter ships, and magic carpets, and restored all three settings after
acceptance.

This pass also closed three defects exposed by the first live ecto-token route. Static remote
transport origins already present in a calculated path are now matched directly instead of being
dropped when they cannot be converted through the currently loaded scene. Withdrawal confirmation
now checks inventory quantity rather than occupied slots, so a stack of 27 ecto-tokens confirms as
27 instead of one. Finally, catalog-transition preparation is distinct from issuing the crossing:
equipping a required amulet clears the pending command immediately and lets the engine retry the
actual interaction instead of waiting for the crossing deadline. Focused regressions cover remote
path extraction, stacked-currency confirmation, and immediate retry after preparation.

**Teleportation-lever slice headless- and live-complete (2026-08-31):** all seven packaged
`TELEPORTATION_LEVER` rows now publish through a dedicated engine route/interaction kind. The live
resolver uses only `Microbot.getRs2TileObjectCache()`, requires the exact catalog object id plus
`Lever`/`Pull` identity within two tiles of the directed origin, and never treats disappearance as
arrival. A single pending interaction covers the object click, the optional exact Wilderness warning
continue, the exact `Yes, I'm brave.` confirmation, and the remote teleport. It clears only within
three tiles of the directed catalog destination; source-object or dialogue disappearance keeps the
landing pending under the engine's bounded 30-second remote-interaction deadline. No stage blocks or
sleeps on the client thread. Catalog-count/identity, classification, scanner stages, direct landing,
warning landing, disappearance, wrong-id unavailability, engine ownership, and bounded-timeout tests
pass. The legacy warning handler remains reachable only on a route already locked to the legacy
executor because another unsupported edge exists. Rebuilt-client acceptance passed in both
directions. Request 7/generation 1 used object `1814` at `2561,3311,0`, issued the engine-owned
`Pull`, `lever-warning-continue`, and `lever-warning-confirm` stages, then arrived at
`3154,3924,0`. Request 8/generation 1 used object `1815` at `3153,3923,0`, issued only the
engine-owned `Pull` stage, then arrived at `2562,3311,0`. Neither request emitted a legacy ownership
marker. The controlled test disabled competing item teleports and temporarily allowed Wilderness
routing; the original banked-transport, item-teleport, lever, and Wilderness-avoidance settings were
restored afterward.

**Canoe slice headless-complete and representative-live-complete (2026-08-31):** all 25 packaged
`CANOE` rows now publish through a dedicated engine route/interaction kind. The live resolver uses
only `Microbot.getRs2TileObjectCache()`, requires the exact catalog object id plus `Canoe Station`
identity and a recognized canoe action within three tiles of the directed origin, and never treats
object or interface disappearance as arrival. One pending interaction owns `Chop-down ->
Shape-Canoe -> highest available shape -> Float Canoe -> Paddle Canoe -> exact destination`,
the optional Wilderness warning/confirmation, exact known arrival dialogue, and directed landing.
The scanner keeps a remote edge pending until the catalog destination is reached, under the
engine's bounded 30-second post-destination remote deadline; no stage sleeps or blocks the client
thread. Catalog identity/count, level-to-shape policy, all UI stages, warning and arrival dialogue,
missing-widget/object rejection, classification, ownership, landing retention, and timeout tests
pass. The shared cost model now applies a conservative 54-tick canoe floor from three measured
53-57 tick construction/travel sequences rather than the catalog's 12-tick estimate.

Rebuilt-client acceptance first passed Ferox Enclave to Edgeville on request 3/generation 1 and
Edgeville to Ferox Enclave on request 4/generation 1. Each direction issued exactly one
`Chop-down`, `Shape-Canoe`, `canoe-shape:Waka canoe`, `Float Canoe`, `Paddle Canoe`, and exact
destination command, retained one request/generation through the remote landing, and arrived
without `LEGACY_LOCKED`, replan, recovery, or duplicate input. The canoe travel interface exposed
an independent RuneLite renderer defect: `OverlayRenderer` dereferenced a nullable
`Graphics2D.getClip()` while restoring overlay state, producing per-frame exceptions during both
travel animations even though the walker completed. The comparison is now null-safe. A newly
compiled request 1/generation 1 repeated Ferox-to-Edgeville with the same six engine-owned commands
and normal arrival, with zero new overlay-render exceptions across destination selection and
landing. The ordinary canoe family is live-accepted in both directions. The one-way Wilderness
Pond warning/confirmation remains headless-complete and is not claimed as a live pass because the
controlled account retained its default Wilderness-avoidance safety setting.

**Minecart slice headless-complete and representative-live-complete (2026-08-31):** all 47 packaged
`MINECART` source rows, expanding to 254 directed transports, now publish through dedicated engine
route and interaction kinds. Eligibility is restricted to the exact packaged object/action/name,
currency, and display shapes. The scene resolves only through `Microbot.getRs2TileObjectCache()`;
missing or changed objects and missing menu destinations are unavailable rather than guessed. Grand
Exchange trapdoor travel is a directed one-stage interaction. Keldagrim train carts additionally
stage both occupied hands into inventory, select the exact front cart for the directed row, retain
the interaction through landing, and restore the originally equipped weapon and shield only after
the equipment mirror confirms each item. Kourend minecarts stage the exact object and exact
destination menu entry. No stage sleeps or blocks the client thread. Catalog identity/count,
classification, ownership, staged equipment restoration, direct landing, Kourend menus, changed
object rejection, landing retention, timeout, and conservative cost-floor tests pass.

Rebuilt-client acceptance passed Grand Exchange to Keldagrim and Keldagrim to Grand Exchange. The
forward trace issued `selection=interaction-minecart stage=minecart-object:Travel:-1:-1
action=minecart-interaction issued=true` at `17:11:23`, landed at `2909,10173,0`, and arrived at
`17:11:34`. The reverse route approached the exact front cart at `2923,10171,0`, issued
`selection=interaction-minecart stage=minecart-object:Ride:-1:-1
action=minecart-interaction issued=true` at `17:12:00`, landed at the Grand Exchange, and arrived at
`3141,3504,0` at `17:12:13`. Both runs retained engine ownership with no `LEGACY_LOCKED` or
`processWalk` marker. Paid Ice Mountain/White Wolf and Kourend destination-menu travel remain
headless-complete/live-deferred. Weapon/shield restoration also remains headless-complete/live-
deferred because the controlled account had neither hand occupied during the representative run.

**Generic teleportation-portal slice partially closed (2026-08-31):** the packaged catalog contains
100 rows. Eighty-eight deterministic rows now publish through dedicated engine route and interaction
kinds, restricted to the exact known object id, action, name, display, zero-fare, and no-item shapes.
The live adapter resolves only through `Microbot.getRs2TileObjectCache()`, requires the exact catalog
identity within four tiles of the directed origin, issues one object action, retains ownership while
the source scene unloads, and clears only at the directed landing. Missing or changed objects receive
no guessed input, and the navigation engine supplies the same bounded remote-interaction deadline as
the other migrated teleport families. The shared cost model applies a measured three-tick floor to
one-tick rows while preserving longer configured durations.

The remaining 12 rows are all Castle Wars object `4408`, action `Enter`, name `Guthix Portal`. Six
identical origin/object/action inputs each declare both `2376,9489,0` and `2416,9524,0`; the catalog
therefore cannot promise which directed edge the same click will take. They intentionally remain
`TRANSPORT` / `LEGACY_LOCKED` until the route data or executor can model the team-dependent outcome.
Catalog-count, supported/unsupported classification, exact live identity, source disappearance,
changed identity, directed landing, bounded deadline, route ownership, and cost-floor tests pass.

The pre-cutover Edgeville-to-Soul-Wars baseline emitted
`mode=LEGACY_LOCKED ... rows=TELEPORTATION_PORTAL:Soul Wars Portal:Enter:Soul Wars` and completed
through `rs2walker:processWalk`; the reverse route did the same for the destination-labelled
`Edgeville` action. On the rebuilt client, request 1/generation 1 issued
`selection=interaction-teleportation_portal stage=Enter
action=teleportation-portal-interaction issued=true` at `17:46:57` and arrived at
`2206,2858,0` through `rs2walker:navigation-engine:arrived` at `17:47:01`. Request 2/generation 1
issued the corresponding `stage=Edgeville` command at `17:47:24` and arrived at `3081,3475,0` at
`17:47:26`. Neither rebuilt run emitted `LEGACY_LOCKED` or `processWalk`. The original portal,
item-teleport, and banked-transport settings were restored afterward.

**Grouping-tab minigame-teleport slice closed (2026-08-31):** all 20 active packaged
`TELEPORTATION_MINIGAME` rows now publish through dedicated engine route and interaction kinds. The
executor models opening the parent grouping tab, opening the grouping panel and dropdown, waiting
for dynamic dropdown children, selecting the exact activity, teleporting, and (for the three active
Rat Pits rows) selecting the exact destination option. An open dropdown with no children is pending
rather than click-ready, so a slow interface cannot be toggled closed on the next observation.
Activity matching normalizes case, whitespace, and straight/curly apostrophes, including the
catalogue's `Giant's Foundry` versus the live interface's `Giants' Foundry`. The terminal edge is
retained when cooldown varplayer 888 removes the source row and clears only at the directed landing.
The commented `Rat Pits: Keldagrim` line is marked broken in the resource and is not included in the
active count. All 20 active rows, staged widget observations, Rat Pits destination handling, loading
and unavailable states, terminal catalog disappearance, bounded ownership, and route classification
are headless-tested; the Rat Pits destination branch remains live-deferred.

Before cutover, the Ferox-to-Castle-Wars route emitted `mode=LEGACY_LOCKED` for
`TELEPORTATION_MINIGAME` and completed through `rs2walker:processWalk`. On the rebuilt client, the
fully hands-off request 1/generation 1 started at `3151,3635,0` and issued
`minigame-open-grouping-tab` at `19:22:17`, `minigame-open-dropdown` at `19:22:18`, exact
`minigame-select-activity:Castle Wars` at `19:22:19`, and `minigame-teleport` at `19:22:20`.
Cooldown-driven transport refresh then removed the row, but the same engine request retained the
landing and ended through `rs2walker:navigation-engine:arrived` at `19:22:38`, one tile from the
catalog coordinate and within the engine's directed landing tolerance. The CLI's stricter
distance-zero wrapper therefore reported `success=false` while its walker state was `ARRIVED`; no
`LEGACY_LOCKED`, `processWalk`, recovery, manual input, or ownership handoff occurred. The isolated
minigame, item-teleport, spell, portal, and banked-transport settings were restored afterward.

**Magic Mushtree slice closed (2026-08-31):** the 12 packaged source rows consist of two origin
tiles at each of four locations plus four destination-only rows; loader expansion produces exactly
24 directed runtime edges. Eligible rows require the exact `MAGIC_MUSHTREE` type, known object ids
`30920`/`30924`, `Magic Mushtree` / `Use` identity, and one of the four catalog destinations. The
cache-backed live adapter resolves the object only through `Microbot.getRs2TileObjectCache()`, opens
the Mycelium Transportation System, normalizes the displayed numeric destination prefix, and
publishes the exact destination as a second immutable stage. A visible menu containing `Not yet
found` instead of the requested destination is unavailable and receives no click. After destination
selection, the engine retains the remote edge across source/interface disappearance and clears only
within six tiles of the directed catalog landing.

The first rebuilt-client attempt from Verdant Valley correctly issued the engine-owned `Use` stage,
then bounded out as `INTERACTION_UNAVAILABLE` because the live menu showed Mushroom Meadow as
`4. Not yet found`; that attempt exposed the need to invalidate unavailable network nodes instead of
only exhausting the edge. After discovering Mushroom Meadow overland, the same widget read
`4. Mushroom Meadow`. The fully hands-off return on request 12/generation 1 then issued
`selection=interaction-magic_mushtree stage=Use` at `20:13:59`, issued the exact
`stage=magic-mushtree-destination:Verdant Valley` at `20:14:00`, and ended through
`rs2walker:navigation-engine:arrived` at the exact `3760,3758,0` target at `20:14:04`. The accepted
request emitted no `LEGACY_LOCKED`, `processWalk`, replan, recovery, duplicate input, or ownership
handoff.

The locked-node follow-up on request 2 proved the hardened path. The already-open group-scoped menu
showed House on the Hill as unavailable, so at `20:49:02` the adapter emitted
`magic_mushtree_unavailable destination=3764,3879,1`, issued no destination click, and requested
generation 2. The invalidation removed both the dedicated `MAGIC_MUSHTREE` rows and their exact
generic `TRANSPORT:Magic Mushtree:Use` shadows into or out of that node. The replacement route used
the known Mushroom Meadow destination at `20:49:03`, then the same navigation engine owned the
overland leg and three catalog transitions before `rs2walker:navigation-engine:arrived` at
`20:50:13`, three tiles from the catalog target and inside the engine's landing tolerance. The CLI's
stricter distance-zero wrapper therefore reported `success=false` while its walker state was
`ARRIVED`. No `LEGACY_LOCKED`, `processWalk`, duplicate unavailable-node input, or ownership handoff
occurred. A separate Sticky Swamp fallback correctly remained unsupported because its shortest route
contained the then-unmigrated Rubber cap mushroom shortcut; that historical result was a Phase 6
backlog boundary, not a Magic Mushtree ownership failure. The shortcut has since been migrated as
recorded below. Catalog count/identity, classification, unavailable-menu behavior,
terminal source/menu disappearance, directed landing, broad walker regression, Checkstyle, and the
pathfinder benchmark pass. The original mushtree, item-teleport, spell, ship, and banked-transport
settings were restored.

**Magic-carpet slice headless- and representative-live-complete (2026-08-31):** all 12 packaged
`MAGIC_CARPET` rows now enter the existing engine-owned NPC-dialogue lifecycle only when they retain
the exact `Travel` / `Rug Merchant` / known actor-id / coin-fare shape. A route regression from the
observed Shantay approach proves the three-tick carpet edge beats the approximately 130-tile surface
route. That regression exposed an A* heuristic gap: carpets were absent from the network landmarks,
so a useful rug-merchant origin slightly off the direct walking line was not explored before the
surface goal was reached. Adding carpets to the heuristic-only landmark set changes no graph edge or
interaction ownership. Catalog classification, malformed-row rejection, policy/scanner tests,
compile, and Checkstyle pass. The first rebuilt-client Shantay-to-Bedabin run selected `Travel`, chose
`Bedabin Camp`, and consumed the correct 200-coin fare, but its roughly 40-second flight exceeded the
generic 30-second remote-interaction deadline and caused an engine recovery/surface replan before
landing. Dialogue transports now have their own bounded 60-second command deadline. After rebuilding,
the reverse Bedabin-to-Shantay run selected `Travel` at `23:35:01`, selected `Shantay Pass` at
`23:35:02`, retained navigation-engine ownership throughout the flight, and completed normally at
`23:35:44`. The 200-coin fare was consumed; no recovery, surface replan, or legacy ownership marker
occurred during that accepted request. The temporary transport settings were restored, and the
character was returned to the Shantay-bank side at full Hitpoints.

**Hot-air-balloon slice headless- and representative-live-complete (2026-09-01):** the 51
resource rows (45 basket origins plus six destination-only rows) expand into 225 directed runtime
edges. Every accepted row now retains the exact `Use` / `Basket` / known object-id shape, maps the
requested destination to its exact group-469 button, and carries the destination's one-log
consumable requirement. The resource previously used an unrecognized `Items` header, so the graph
silently dropped every log requirement; the corrected `Item IDs` data and shared bank-planning
predicate now make balloon logs participate in withdrawal planning and sum across repeated trips.
The navigation engine owns the basket, destination-menu, in-flight, and directed-landing stages,
and the pathfinder publishes balloon landmarks so a useful basket off the direct surface line is
considered. Catalog count/classification, malformed-row rejection, staged scanner behavior,
bank-planning, route selection, bounded engine ownership, compile, focused tests, Checkstyle, and
the pathfinder benchmark cover the headless gate. The current account has only Entrana and Taverley
unlocked. A temporary read-only loadout probe confirmed zero equipped items before the Entrana
journey, and the character carried one normal log (`1511`). The first rebuilt-client attempt reached
the Entrana origin but exposed a live-scene mismatch: the catalog uses pre-unlock basket IDs
`19128`/`19129`, while the unlocked Entrana station renders `ZEP_MULTI_BASKET_ENTRANA` (`19133`).
The cache-backed scene now accepts only the official legacy and unlocked station basket variants
(`19128`, `19129`, and odd IDs `19133` through `19143`) near the planned origin and publishes the
actual live object ID. After rebuilding, the accepted `00:47:52` request from
`2807,3357,0` to Taverley `2941,3420,0` published one `HOT_AIR_BALLOON` edge. It issued `Use` at
`00:47:58`, selected `hot-air-balloon-destination:Taverley` at `00:47:59`, consumed the normal log,
and cleared as `rs2walker:navigation-engine:arrived` at `00:48:04`. No recovery, surface replan,
`LEGACY_LOCKED`, `processWalk`, or legacy ownership marker occurred in the accepted trace. This is
representative acceptance for the unlocked pair only; the four higher-tier destinations remain
headless-complete rather than live-claimed.

**Remaining Phase 6 transport-classification audit (updated 2026-09-01):** the immutable route classifier
still has no dedicated engine route kind for the following complete catalog families:
`WILDERNESS_OBELISK` (60 source rows). Routes using those rows therefore remain `TRANSPORT` /
`LEGACY_LOCKED`. Hot-air balloons have completed the representative live gate above. The
generic portal catalog is partially migrated as described above:
its 12 ambiguous Guthix rows remain legacy-owned. The 286-edge agility/grapple catalog is now
split by an exact production-classifier audit. Of 274 `AGILITY_SHORTCUT` edges, 12 conservative
same-plane direct actions already publish through `ADJACENT_TRANSPORT`, and 248 deterministic,
item-free, scene-changing object interactions publish through `CATALOG_TRANSITION`. Those 248
reuse the established exact catalog object/action resolver, bounded engine command lifecycle, and
directed landing observation; the action allowlist is pinned to the current catalog rather than
admitting future shortcut protocols implicitly. Fourteen agility edges remain `TRANSPORT` /
`LEGACY_LOCKED`: eight `Jump-onto` stepping-stone edges (two inputs each declare two possible
destinations), four adjacent actions outside the existing direct-action contract, one item-gated
`Use;Rope -> Boulder` row, and one anomalous `Grapple;Rocks` row that is not typed as a grapple
shortcut. All 12 `GRAPPLE_SHORTCUT` edges also remain legacy-owned until equipment/tool setup and
restoration have their own explicit contract. Corpus classification, policy/scanner tests, the
broad transport suite, Checkstyle, and the pathfinder benchmark pass. Representative rebuilt-client
live acceptance of the 248-edge catalog-transition slice also passed on the level-5 Falador
crumbling wall. The first approach attempt never issued movement or shortcut input because a
full-screen game interface had removed the minimap widget; a read-only client-thread probe showed
the canvas remained `1889x1009`, camera focus equalled the player, but the minimap widget and both
click projections were unavailable. One Escape restored the normal minimap and on-screen
projections, so that attempt is not counted as an interaction failure. On the accepted reverse run,
request 7/generation 1 started inside Falador, published the directed `2934,3355,0` to
`2936,3355,0` wall edge, and at `01:35:32` issued exactly one
`selection=interaction-catalog_transition stage=Climb-over
action=catalog-transition-interaction issued=true` command. It observed the directed outside
landing and cleared through `rs2walker:navigation-engine:arrived` at `01:35:35`. The CLI's stricter
distance-two wrapper reported `success=false` because the normal engine finish tolerance stopped
five tiles from the requested follow-on target; its walker state was `ARRIVED` at the exact catalog
landing. No recovery, replan, `LEGACY_LOCKED`, `processWalk`, duplicate interaction, manual movement,
or ownership handoff occurred. The original disabled agility-shortcut setting was restored.

The first ordinary-transport protocol group is now closed as well. The catalog contains 54 exact
`Climb-over;Stile` rows: four were already engine-owned adjacent transitions, while the remaining
50 item-free, non-adjacent, single-destination rows now use `CATALOG_TRANSITION`. The eligibility
boundary requires the exact normalized `Climb-over` action and exact `Stile` name; it does not
admit other ordinary climb-over objects. Rebuilt-client acceptance passed in both directions on
stile `993` at `2917,3438`. Request 3/generation 1 selected
`interaction-catalog_transition`, issued one `Climb-over` at `02:17:08`, observed the directed
west landing at `2916,3438`, and cleared as `rs2walker:navigation-engine:arrived` at `02:17:13`.
Request 4/generation 1 issued the symmetric engine-owned command at `02:17:32`, observed the east
landing at `2919,3438`, and arrived at `02:17:35`. Neither accepted trace contained a replan,
recovery, `LEGACY_LOCKED`, `processWalk`, duplicate interaction, manual movement, or ownership
handoff. The CLI wrapper reported `success=false` only because its requested distance-one threshold
is stricter than the engine's normal arrival tolerance; both walker states were `ARRIVED` at the
exact directed catalog landing. After the rebuilt client opened, one initial long approach command
was rejected before movement because the minimap was again hidden by a full-screen interface; one
Escape restored input projection and no transport input had been issued.

The exact ordinary `Climb;Rocks` protocol is now headless-complete. A read-only production-
classifier probe found 55 rows across 55 distinct inputs with no multi-destination ambiguity:
51 non-adjacent and four adjacent. Forty-nine item-free non-adjacent rows now publish through
`CATALOG_TRANSITION`, and the four item-free one-tile rows publish through
`ADJACENT_TRANSPORT`. The policies require the exact normalized `Climb` action and exact `Rocks`
name, so `Climbing rocks` and other climb objects are not admitted implicitly. Two level-15
Agility rows requiring climbing boots (`3105`) remain `TRANSPORT` / `LEGACY_LOCKED`; equipment
setup and restoration are not part of either direct-object lifecycle. The corpus also contains 12
skill-gated and eight quest-gated rows, whose existing route eligibility filtering is unchanged.
The rebuilt production classifier confirms the exact `49 / 4 / 2` kind split. Representative live
acceptance is deliberately deferred because the available clusters are in Troll Country, hazardous
jungle, or dungeons; no character movement or interaction was attempted for this headless slice.

The exact ordinary `Pass;Barrier;32153` protocol is now headless-complete. A read-only production-
classifier probe found 48 rows across 48 distinct inputs, all item-free, same-plane, two-tile
transitions with no multi-destination ambiguity. Local game-value metadata identifies object 32153
as `DS2_LITHKREN_VAULT_BARRIER`, so the policy accepts only the exact normalized `Pass` action,
exact `Barrier` name, and exact object ID 32153; a synthetic neighboring ID remains legacy-locked.
The packaged rows omit a quest requirement and route eligibility is unchanged. After a rebuilt-client
restart, the production classifier confirmed all 48 rows publish through `CATALOG_TRANSITION`.
Representative live acceptance is deliberately deferred because the Dragon Slayer II Lithkren vault
is unavailable to the test account; no character movement or interaction was attempted for this
headless slice.

The exact Ferox Enclave `Pass-Through;Barrier` protocol is now headless-complete. The remaining-
ordinary inventory and a focused production probe found 16 item-free, fare-free, one-tile rows
across 16 distinct inputs with no ambiguity. Game-value metadata identifies IDs 39652 and 39653 as
the normal and mirrored wilderness-hub entry barriers. `AdjacentTransportPolicy` accepts only the
exact normalized action, exact `Barrier` name, those two IDs, and an empty item requirement; wrong
IDs, names, actions, and two-tile shapes remain locked. After a rebuilt-client restart, the
production classifier confirmed all 16 rows publish through `ADJACENT_TRANSPORT`, split evenly
between the two IDs. Live acceptance is deferred because this family sits on the wilderness-facing
Ferox boundary; no character movement or interaction was attempted for this headless slice.

The same production inventory grouped all 2,242 then-remaining ordinary legacy rows before the
Ferox cutover. It confirmed that the large families included 670 wilderness-ditch rows, 92
item-gated jungle-chopping rows, 46 dense-forest rows, and smaller webs, paid ecto barriers,
dialogue/fare routes, and ambiguous multi-destination inputs. Jungle chopping, webs, paid ecto
barriers, and the smaller mixed contracts were still legacy-owned at that audit boundary. Jungle
chopping, deterministic ecto barriers, and exact Wilderness-sword web rows were subsequently split
by the contracts below; the remaining families stay locked until each required item,
warning/dialogue, transformed-object, and landing protocol is isolated.

The exact ordinary `Cross;Wilderness Ditch;23271` family is now headless-complete behind its own
staged engine interaction. A read-only production probe found 670 rows across 670 distinct directed
inputs, all item-free and fare-free three-tile crossings with no ambiguity. The engine observes the
exact cache-backed object, issues one `Cross`, handles optional warning widget `475:11` as a separate
command, retains ownership while either stage disappears, and acknowledges only at the directed
catalog landing. It does not broaden generic `Cross` rows or rely on the legacy process-loop warning
branch. Policy, scanner, engine-stage, generated-route, broad transport, benchmark, and Checkstyle
tests pass. After a rebuilt-client restart, the production classifier confirmed all 670 rows publish
as `WILDERNESS_DITCH`. Live acceptance is deliberately deferred because entering this family crosses
the Wilderness boundary; no login, character movement, or interaction was attempted for this
headless slice.

The exact ordinary `Enter;Dense forest` protocol is now headless-complete through the existing
`CATALOG_TRANSITION` lifecycle. A rebuilt-client production probe found 46 directed rows across 46
distinct inputs, all item-free, fare-free, same-plane crossings with no ambiguous destinations.
Eligibility requires the exact normalized action and name plus one of the five catalog object IDs
`3937`, `3938`, `3939`, `3998`, or `3999`; unrelated `Enter` objects and neighboring IDs remain
legacy-owned. The cache-backed scene resolves the exact catalog identity, the navigation engine
issues the single interaction, retains ownership while the obstacle animation advances, and clears
only at the directed landing. The rebuilt production classifier confirmed all 46 rows publish as
`CATALOG_TRANSITION`. Focused policy/scanner/corpus tests, the broad transport suite, Checkstyle,
and the pathfinder benchmark pass. Live acceptance is deliberately deferred because the available
obstacles are in hazardous Tirannwn and the account is not positioned for a safe representative
run; no character movement or interaction was attempted for this headless slice.

The exact item-gated Kharazi `Chop-down` protocol is now headless-complete behind a dedicated
`JUNGLE_OBSTACLE` route kind. A rebuilt-client production probe found 92 rows across 84 exact
inputs. Seventy-six inputs each have one directed destination and now use engine ownership; eight
inputs each declare two destinations, so their 16 rows remain `TRANSPORT` / `LEGACY_LOCKED` rather
than guessing a landing. Eligibility is pinned to ordinary `TRANSPORT`, exact normalized
`Chop-down`, exact `Jungle Bush` IDs `2892`/`2893` with the machete alternatives
`6313`/`6315`/`6317`/`975`, or exact `Jungle tree` IDs `2889`/`2890` with the catalog's twelve axe
alternatives, a fare-free reusable requirement, and the exact same-plane two-tile geometry. The
cache-backed scene uses `Microbot.getRs2TileObjectCache()`, dispatches one object command without
waiting, retains the pending interaction while the source transforms, and clears only at the
directed landing. The navigation engine owns a bounded 60-second chopping window before requesting
a replan, accommodating repeated Woodcutting attempts without introducing sleeps or a separate
control loop. All 92 rows still participate in `Rs2WalkerBankingPlanner`: inventory and equipment
count toward the requirement, and a bank route withdraws only one available reusable machete or axe
alternative. Focused policy/scanner/corpus/engine tests, the full transport and banking regression
groups, and Checkstyle pass. The rebuilt classifier confirmed the exact
`76 JUNGLE_OBSTACLE / 16 TRANSPORT` split. Live acceptance is deliberately deferred because the
available crossings are in hazardous Kharazi jungle; following the earlier near-death incident, no
character movement or interaction was attempted for this headless slice.

The five exact Brimhaven Dungeon vine boundaries are now included in that same staged lifecycle.
Their ten paired `Chop-down;Vines` rows use dedicated object ids `21731`-`21735`, are same-plane
two-tile crossings, carry no fare or varbit gate, and declare the exact 13 supported axe ids. The
resource represents those ids as singleton groups, while availability and banking deliberately
flatten all encoded ids into one OR-alternative set; the exact policy now compares that normalized
set rather than requiring one particular nested representation. Repeated vine edges therefore plan
one reusable axe, not one axe per edge (and not all 13 variants). A rebuilt production-classifier
probe moved the split from `76 JUNGLE_OBSTACLE / 26 TRANSPORT` to
`86 JUNGLE_OBSTACLE / 16 TRANSPORT`; the remaining 16 are precisely the already-audited ambiguous
Kharazi inputs. Navigation retains the command through temporary object disappearance and clears
only at the directed landing. Live chopping remains deferred: the documented obstacles span
Woodcutting requirements and lie inside a combat dungeon, so no character movement was attempted.

The exact Chasm of Fire lift network is now headless-complete through `CATALOG_TRANSITION`. All 16
rows use object IDs `30258`/`30259`, action/name `Enter;Lift`, no item, fare, quest, or varbit
requirement, and a same-coordinate plane change. The existing cache-backed transition resolver
therefore owns the object interaction and retains it until the directed plane landing; no new
controller or legacy handoff is introduced. A rebuilt production inventory recorded all 16 as
legacy before the cutover and none afterward, reducing the ordinary legacy floor by exactly 16.
The Chasm houses lesser, greater, and black demons, so actual lift interaction remains live-deferred
and no character movement was attempted.

The exact `Slash;Web;733` family is now headless-complete through the adjacent-clearance lifecycle.
All 28 rows have unique directed inputs and destinations: 18 are adjacent and 10 cross a two-tile
web boundary. Each row now declares one reusable OR requirement containing Wilderness swords I-IV
(`13108`-`13111`). Those swords have guaranteed web-cutting success from inventory, so the safe
contract needs neither an arbitrary combat-weapon withdrawal nor an equipment replacement and
restoration cycle. `AdjacentTransportPolicy` accepts only ordinary, fare-free, non-consumable rows
with the exact action, name, object ID, requirement group, same-plane geometry, and distance at most
two; generic positive-slash weapons and Aranea boots are deliberately not inferred into this
contract. The existing cache-backed adjacent scene issues one `Slash`, treats observed object loss
as clearance, and leaves the navigation engine to walk across the now-open local edge. All 28 rows
participate in `Rs2WalkerBankingPlanner`; repeated route edges require one available reusable sword
alternative. Focused policy, classifier, and banking tests plus the broad walker regression suite,
pathfinder benchmark, compilation, and both Checkstyle tasks pass. After a rebuilt-client restart,
the read-only production audit reported
`rows=28 itemGated=28 supported=28 bankPlanned=28`; the probe was undeployed afterward. Most rows
are in hazardous Wilderness or similarly unsafe areas, so no character movement or interaction was
attempted and actual web cutting remains live-deferred.

The Molch Temple subset of `Pass;Mystical barrier` is now headless-complete through the same
adjacent-clearance lifecycle. The production inventory found 20 rows under that action/name, but
their object identity separates two different protocols. Sixteen deterministic two-tile rows use
Molch Temple IDs `34643`-`34646`, carry no item, fare, quest, or destination ambiguity, and now
publish as `ADJACENT_TRANSPORT`. The remaining four ID-`34542` Karuulm rows stay legacy-owned:
crossing farther into the volcanic dungeon without an approved heat-protection boot or the elite
Kourend & Kebos diary can open a warning and then deal rapid repeated damage, which is not encoded
in those catalog rows. The exact policy therefore does not generalize the shared action/name across
that hazard boundary. The cache-backed adjacent scene issues one `Pass`, retains the directed
boundary while the persistent object remains, and acknowledges only from the destination side.
Actual Molch crossing remains live-deferred because the barriers enter level-150 Lizardman shaman
enclosures and passing itself deals one damage; no character movement or interaction was attempted.

The next apparent direct boundary, 11 exact `Pass;Hot vent door;30266` rows in Mor Ul Rek, remains
legacy-owned after audit. The resource records five bidirectional pairs plus one one-way row but no
item, varbit, quest, or other persistent-unlock requirement. Inner-city access requires the account
to have shown a fire cape to the guarding TzHaar, so the action/name/geometry alone is not a complete
eligibility contract. This family must first identify and publish the persistent access state (and
explain the asymmetric row) rather than being admitted by another generic two-tile `Pass` exception.

The 16 exact `Pay-toll(2-Ecto);Energy Barrier;16105` rows were initially split by directed-input
identity: twelve deterministic rows reused `CATALOG_TRANSITION`, while four west-gate rows remained
legacy-owned because the same inputs declared two landings. The 2026-09-04 completion above replaces
that interim boundary with quest-state-exclusive paid/free variants, shared ghostspeak and token
requirements, and compatibility with the modern post-quest `Pass` object. The earlier read-only
`rows=16 supported=12 locked=4 ambiguous=4` result remains historical; the current headless boundary
is `rows=16 supported=16 locked=0`, with physical crossing still live-deferred.

After the staged-item and Cabin Boy Herbert cutovers, the generated-edge legacy floor is 1,687 rows:
`AGILITY_SHORTCUT` 14, `BOAT` 3, `FAIRY_RING` 53, `GRAPPLE_SHORTCUT` 12, `SHIP` 3,
`TELEPORTATION_ITEM` 75, `TELEPORTATION_PORTAL` 12, `TELEPORTATION_SPELL` 11,
ordinary `TRANSPORT` 1,186, and `WILDERNESS_OBELISK` 318. These are production-classifier
results over all 12,680 loaded generated entries, not source-line estimates. The count is an audit
boundary, not a claim that every remaining row needs a new family: the 1,186 ordinary rows still
need protocol grouping into safe direct actions, item/tool setup, dialogue, random or multi-stage
interactions, and malformed/duplicate artifacts before any additional ownership cutover.

The exact Fremennik basalt causeway is now headless- and bidirectional-live-complete. Its ten rows
are the five directed pairs using `Basalt rock` IDs `4551`-`4558`, plus the Beach entry ID `4550`
and Rocky shore exit ID `4559`; only their exact `Jump-across`/`Jump-to` identities publish through
`CATALOG_TRANSITION`. This reduces the generated legacy floor from **1,687 to 1,677** and ordinary
legacy rows from **1,186 to 1,176**. Rebuilt-client requests at `19:10:39` and `19:11:17` traversed
the entire chain in opposite directions, emitted engine-owned `interaction-catalog_transition`
commands for every causeway edge, and cleared as `rs2walker:navigation-engine:arrived` with no
legacy ownership marker or relevant client-log error.

The Captain Shanks correction then removes three more legacy entries: two exact `SHIP` rows migrate
to `NPC_DIALOGUE_TRANSPORT`, and the nonexistent third destination is removed from the generated
catalogue. The current generated-edge legacy floor is therefore **1,674**: `AGILITY_SHORTCUT` 14,
`BOAT` 3, `FAIRY_RING` 53, `GRAPPLE_SHORTCUT` 12, `SHIP` 0, `TELEPORTATION_ITEM` 75,
`TELEPORTATION_PORTAL` 12, `TELEPORTATION_SPELL` 11, ordinary `TRANSPORT` 1,176, and
`WILDERNESS_OBELISK` 318.

A live pre-rebuild failure also reproduced the stale-edge bug precisely: after the command for the
`2522,3600 -> 2522,3602` jump was issued from `2522,3597`, failure washed the player back to
`2522,3595`, while the pending command kept clicking the later edge. `NavigationEngine` now records
the player position when it issues a short catalog-transition command and requests an immediate
`interaction-displaced-behind-origin` replan when a later observation has moved farther backward
along that edge's directed vector. The regression uses those exact live coordinates and proves that
only one interaction is issued before replanning. After rebuilding, three bounded crossings passed
normally in both directions without damage; RNG did not produce another failure, so the
failure-specific live trace remains intentionally unclaimed. Focused navigation, catalog-policy,
catalog-scanner, production-classification, compilation, and both Checkstyle tasks pass.

**Batching change (2026-09-03):** use the [behaviour-based batch backlog](walker-transport-batches.md)
instead of continuing small object-name slices. The first 128-row jewellery/cape review migrated
99 rows; the second 64-row other-item review migrated 44. The 14 Quetzal-whistle map contracts,
remaining item exceptions, and complete ordinary scene-protocol groups are next.
Candidate counts are not acceptance claims; exact requirements, input
ambiguity, staged warnings, and representative live gates remain mandatory.

The wilderness-obelisk count is a source-file count, not a deterministic edge count: 54 activation
tiles (six 3-by-3 pads) combine with six destination-only rows into 318 runtime transports. Forty-
eight of those generated rows are self-pad artifacts from a perimeter tile to its own centre, leaving
270 genuine remote rows. The legacy handler activates an obelisk and waits for the route's promised
destination, but ordinary obelisk travel is random unless a supported destination-selection
protocol is available. That
nondeterministic/permutation contract must be resolved before the family can safely receive a
directed engine route kind. The audit pins all 318 generated rows as `TRANSPORT` / `LEGACY_LOCKED`:
each of the 54 exact `Activate` inputs declares five genuine remote destinations, so none can prove
the selected edge. The 48 self-pad artifacts are equally unsafe because arrival at the pad centre
does not cancel the delayed random teleport. Wilderness Hard unlocks a separate set-destination/favourite protocol, but the
catalog does not encode that staged input and this account's `WILDERNESS_DIARY_HARD_COMPLETE`
varbit (`4468`) is `0`. No wilderness live probe was attempted. A future deterministic migration
must require the diary unlock, model destination setup plus pad activation/waiting as distinct
stages, and observe the selected landing; random activation cannot be accepted as success.

The already sliced mixed catalogs also intentionally retain unsupported row shapes: item/spell
teleports needing destination submenus, wilderness confirmation, Master Scroll Book, multi-option
spell flows; `BOAT`/`SHIP`/`NPC` rows using `Talk-to`, non-coin
currency, item gates, or named bespoke conversations; charter rows outside the coin-only contract;
and ordinary `TRANSPORT` rows outside the accepted adjacent and ladder/stair/trapdoor/cave/
gangplank policies. POH
physical Home/Enter is accepted, but portal chambers, nexus, jewellery box,
mounted teleports, POH fairy ring, and POH spirit tree remain explicitly live-deferred. Seasonal
rows remain headless-complete/live-deferred on League availability. This audit is the Phase 6
backlog boundary; it does not authorize deleting `processWalk` or starting Phase 7.

**Rebuilt-client Phase 6 remainder audit (2026-09-05):** a read-only production-classifier probe
loaded 12,672 generated edges and found 1,624 still classified as `TRANSPORT` before the home-
teleport cutover: ordinary `TRANSPORT` 1,129, `WILDERNESS_OBELISK` 318,
`TELEPORTATION_ITEM` 75, multi-code `FAIRY_RING` 53, `AGILITY_SHORTCUT` 14,
`GRAPPLE_SHORTCUT` 12, ambiguous `TELEPORTATION_PORTAL` 12, and
`TELEPORTATION_SPELL` 11. The ordinary remainder contains 51 item-gated rows, 12 fare rows,
51 quest-gated rows, 58 var-state-gated rows, 196 remote/plane-changing rows, 270 adjacent rows,
and 52 executable-input groups with multiple declared destinations. These dimensions overlap and
are audit evidence, not safe-migration counts. The probe was undeployed without game input.

The same audit reproduced the day's legacy fallback: a route from Port Phasmatys selected the exact
originless `Lumbridge Home Teleport` edge, and that single unsupported edge locked the whole request
to `processWalk`. That exact spell is now admitted as `SIMPLE_TELEPORT`, receives a bounded
35-second cast/landing deadline, and remains pending until its directed Lumbridge landing rather
than being retired by projected raw-route progress. The other ten spell rows are all destination-
override contracts (eight Teleport to House locations, Watchtower-to-Yanille, and Varrock-to-GE)
and remain legacy-owned. Policy, delayed-cast retry, remote-retention, production-count, and focused
navigation tests pass, reducing the headless legacy floor to 1,623 and spell remainder to ten.
Rebuilt-client acceptance passed at **2026-09-05 01:49 BST** after the strict 30-minute cooldown
gate became usable. With competing transport networks temporarily disabled, the route from
`(1884,3481,0)` to `(3221,3218,0)` published exactly one
`selection=interaction-simple_teleport stage=Lumbridge Home Teleport` command at `01:49:06`,
retained NavigationEngine ownership throughout the approximately 16-second cast, landed at
`(3222,3219,0)`, issued the final one-tile `route-end-approach`, and cleared as
`rs2walker:navigation-engine:arrived` at the exact target. No `LEGACY_LOCKED` or `processWalk`
ownership appeared. All temporary transport settings were restored after the run. Phase 6 remains
open for the audited remainder and Phase 7 has not started.

**Enakhra's Temple sand-pile exits (2026-09-05, headless):** all 16 exact
`Climb;Sand pile;10950` rows now reuse `CATALOG_TRANSITION`. The allowlist pins the object ID,
action, and name, while the production-corpus regression confirms all 16 directed rows publish
through NavigationEngine and no broader sand object is admitted. Cache metadata identifies `10950`
as the temple sand pile, and the OSRS Wiki documents climbing the sand pile as the direct way out;
the separate unlock restriction applies to entering from the surface after first leaving through
an exit, so this cutover deliberately owns only the already-published underground-to-surface rows.
Focused policy/classifier tests and main/test Checkstyle pass. This reduces ordinary legacy rows
from **1,129 to 1,113** and the generated legacy floor from **1,623 to 1,607**. Physical acceptance
remains deferred until an eligible account is safely inside the quest temple.

**Frozen direct-transition manifest (2026-09-05, headless):** 30 generated rows across 13 exact
object IDs now reuse `CATALOG_TRANSITION`. The tranche covers Nature Grotto enter/exit, the paired
Tree Gnome Village huge gates, Elf Village tree-gate crossings, Magic Training Arena doorway
crossings, Polar hunting-area steps, the Hunter Guild rope, the exact `DOTI_BARRIER` entryway, and
the paired Custodia Pass cave. Each manifest entry pins its directed origin and destination, object
ID, normalized action, and exact object name. It therefore cannot admit a new catalog row merely
because that row later gains a familiar verb or name. The shared scene adapter still issues one
exact object interaction and NavigationEngine retains ownership until the directed landing clears.

The broad structural candidate was deliberately narrowed before acceptance. Fremennik/Troll tunnel
rows previously audited as requirement-incomplete, damaging Slayer Tower spikey chains, Darkmeyer
agility walls, Karuulm heat-floor links, Wilderness/God Wars routes, paid or warning entrances, and
all item/fare/quest/var-gated candidates remain legacy-owned. A fail-first classifier run exposed
the tunnel overreach and passed after removal. Exact endpoint/action/name/ID mutation regressions,
the existing 39-row tunnel lock regression, catalog landing/scanner tests, NavigationEngine tests,
compilation, Pathfinder benchmark, and both Checkstyle tasks pass. This reduces ordinary legacy
rows from **1,113 to 1,083** and the generated legacy floor from **1,607 to 1,577**. Physical route
acceptance passed on the rebuilt client for the Magic Training Arena doorway in both directions.
At **02:42 BST**, requests `(3363,3298,0) -> (3363,3300,0)` and the exact reverse each issued one
`selection=interaction-catalog_transition stage=Enter` command, landed on the directed endpoint,
and cleared as `rs2walker:navigation-engine:arrived`; neither trace contained a legacy ownership
marker, replan, or duplicate interaction. Other manifest families remain physical-live-deferred.
The optional dynamic-probe deployment endpoint returned `404`, so no runtime probe was left
installed and no metadata-only result is claimed.

**Master Scroll Book cutover (2026-09-05, headless):** all 18 exact book destinations now publish
through the engine-owned `ITEM_TELEPORT` lifecycle. The interaction advances from the inventory tab
to the book's exact `Open` action, then to the generated `InterfaceID.Bookofscrolls` destination
component, and retains the directed edge until the declared landing. Revenant Cave adds the exact
`Yes, teleport me now` confirmation as a separate stage and remains subject to the existing
wilderness and transport configuration gates. The catalog's per-destination varbit still owns
stored-scroll availability. Banking treats item `21389` as one reusable container even though the
row is marked consumable for its stored charge, so repeated edges withdraw one book rather than
inventing multiple physical books. Focused policy, staged-lifecycle, banking, and classifier tests
cover all 18 rows. This reduces item-teleport legacy rows from **75 to 57** and the generated legacy
floor from **1,577 to 1,559**. Physical acceptance remains pending because all 18 stored-scroll
varbits are zero on the active account; any Revenant Cave acceptance must use an empty inventory
and equipment loadout.

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
therefore complete. The later specialised `Talk-to` slices migrated Cabin Boy Herbert and the two
corrected Captain Shanks destinations, Pirate Pete's two quest-finished conversation rows, and Ghost
Captain's outbound combined-item-and-currency row. The dedicated `BOAT`/`SHIP` family is now
headless-complete, with the latter three quest-gated routes still awaiting live acceptance. After this
family: fairy rings, spirit trees, gliders, quetzals, POH, seasonal transports,
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
`Quick-Travel`, Captain Shanks `5364` (`Talk-to`; the corrected Khazard Port and Port Sarim rows are
engine-owned), Bill Teach `4016`, Sailor `3936`/`3680`, Captain Bentley `6650`, Captain Magoro
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
submenus, wilderness confirmations, Master Scroll Book flows, and multi-option spell variants remain
legacy-owned. The exact Lumbridge Home Teleport now uses this lifecycle with a longer bounded cast
window and directed-landing retention. Planner, scanner, execution, and route-ownership
tests pass. Live item acceptance passed with a consumable tablet: exactly one unified interaction
was issued, the post-consumption transport refresh did not invalidate the pending command, landing
was acknowledged, and ordinary walking resumed on the same generation. A later ground-click
destination mismatch caused a normal replan; it occurred after teleport acknowledgement and is
tracked as input-adapter behavior rather than teleport-family failure. Direct-spell live acceptance
now also passes for Lumbridge Home Teleport: one engine-owned command survived the full cast,
acknowledged the directed landing, completed the final one-tile approach, and arrived without a
legacy handoff.

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
6. banked-transport route setup (headless and live complete).

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
