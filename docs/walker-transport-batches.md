# Phase 6 behaviour-based batches

Latest classifier audit: 2026-09-08; older sections below retain their dated snapshots.
This is the execution backlog for
[walker unification](walker-unification-plan.md), not a new ownership policy.

## September 8 Max-cape duplicate cleanup

One exact duplicate Black-chinchompa row is removed, leaving 21 unique Max-cape contracts. No Max
menu is migrated by this cleanup. Baseline is **1,161 legacy / 43 item legacy**.

## September 8 Mythical cape correction

The inert POH trophy ID 21913 is removed from the route requirement. Usable capes 22114/24855 now
publish the exact inventory/worn `Teleport` action through the reusable item lifecycle. Baseline is
**1,162 legacy / 44 item legacy**; mounted-POH behavior and physical acceptance remain outside this
cutover.

## September 8 Mokhaiotl waystone follow-up

The exact item-31099 `Channel` row now uses the normal consumable item-teleport lifecycle with its
completed-Final-Dawn and Wilderness metadata frozen. Inert neighboring IDs remain excluded. The
current baseline is **1,163 legacy**, including **45 item-teleport rows**; physical use is deferred.

## September 8 Burning amulet follow-up

All three exact Burning amulet destinations now advance from the inventory/worn destination action
to one scoped Wilderness-warning confirmation, then remain NavigationEngine-owned until the directed
landing. No broad `Yes` handler is used. The current baseline is **1,164 legacy**, including **46
item-teleport rows**. Banking conservatively requests one charged amulet per selected route use;
physical Wilderness acceptance remains deferred.

## September 8 alternate-spell follow-up

The last ten spell rows—eight House `Outside`, Varrock GE and Watchtower Yanille—now reuse the simple
teleport executor through an exact requirement/action allowlist. The bank planner sums their base
spell runes, and no generic colon-labelled spell is admitted. Current baseline is **1,167 legacy**;
the `TELEPORTATION_SPELL` remainder is zero. Representative alternate-input live tests remain open.

## September 8 Molch/Lizardman Temple follow-up

Sixteen exact one-way rows—eight Lizard dwelling entrances and eight Strange hole exits—now use the
catalogue transition lifecycle. Shared ID 34403 is pinned by full route key because it reaches two
different interiors; unlisted dwelling 34404 is excluded. Current baseline is **1,177 legacy / 725
ordinary**. Physical acceptance remains deferred in the combat-adjacent temple area.

## September 8 Castle Wars random-portal cleanup

The 12 Guthix object-4408 rows are removed: six identical live inputs each declared both team-room
landings, while the server may select either team. Deterministic 4387/4388 portals preserve both
destinations, so the portal corpus is now 88/88 engine-owned without inventing a directed outcome.
The current baseline is **1,193 legacy / 741 ordinary**.

## September 8 Swan Song hole follow-up

The two exact `Enter;Hole;12656` directions are implemented as completed-quest catalogue
transitions; the other 12 remaining `Enter;Hole` rows are unrelated Dragon Slayer quest-in-progress
aliases and stay legacy-owned. Its post-cutover baseline was **1,205 legacy / 741 ordinary** before
the random-portal cleanup. An isolated client build passed the 84 focused tests without stopping the
running client; physical bidirectional acceptance is still outstanding.

## September 8 Enakhra secret-entrance follow-up

Sixteen `Climb-down;Secret entrance` approaches for boulder IDs 11045-11048 are headless-migrated
through an exact manifest. Entry now conservatively requires completed Enakhra's Lament because
the resource was ungated and no reliable partial-quest unlock predicate was found; reverse
sand-pile exits remain free. Pre-Swan baseline was **1,207 legacy / 743 ordinary**. Quest-time
boulder opening and physical side-by-side acceptance are not claimed.

## September 8 POH Outside-tablet follow-up

The eight item-8013 `Outside` rows now use the existing item-teleport lifecycle under an exact
house-location-varbit-to-exterior map. They remain inventory-only consumables, so repeated routes
sum one tablet per use in bank planning; a disappearing consumed tablet does not surrender pending
ownership before landing. No inside-house or advanced-facility support is claimed. The current
classifier floor is **1,223 legacy / 759 ordinary**, with **49 item** rows still legacy-owned.

## September 8 runecrafting exit-portal follow-up

Sixteen fixed Rune Temple `Use;Portal` exits are headless-migrated through an exact directed
manifest. The nine object IDs are the vendored Mind, Water, Earth, Fire, Body, Cosmic, Nature,
Chaos and Blood exit portals; no generic portal action was enabled. Chaos ID 34757 retains three
different origin/destination contracts, and tests prevent one landing from acknowledging another.
Current baseline after this cutover is **1,231 legacy / 759 ordinary**; physical exits remain in
the live ledger.

## September 8 Meiyerditch course follow-up

Sixty-six Meiyerditch/Ver Sinhaza rows are headless-owned with exact identities and landing: 18
floorboards, six floors, 20 rubble/crawl-wall/rock/shelf/washing-line links, seven prepared-floor
climbs, five tunnel/barricade rows and ten post-quest access rows. The course/tunnel slice enforces
26 Agility and partial Darkness of Hallowvale; prepared floors also require persistent knockdown
varbit 2589. The final ten require the quest finished. No row from this audited corpus remains
legacy-owned, but one-time setup, discovery and quest-flow behavior are not implemented. The
knife is not a recurring push-wall requirement, and these western-wall IDs are not the modern
rope-built level-86 shortcut. Current baseline is **1,247 legacy / 775 ordinary**.

## September 8 Abyss rift follow-up

Eleven deterministic inner-Abyss rifts are headless-migrated with exact directed identities and
retained quest gates. Law is excluded because the source row omits Entrana equipment restrictions;
Soul is excluded because current access requires dark essence rather than an unrestricted click.
Current baseline is **1,292 legacy / 820 ordinary**. Compilation, 435 focused tests, both
Checkstyle tasks and the whitespace check passed; physical altar arrivals remain deferred.

## September 8 audited agility follow-up

Twenty-eight loaded rows across four static agility families are headless-migrated: Nature Grotto
bridge, Agility Pyramid entrance rocks, Rellekka Hunter-area handholds and GWD/Wilderness
handholds. The exact directed manifest has 27 keys because one GWD row is duplicated. Current
baseline is **1,303 legacy / 831 ordinary**. Requirements, agility-toggle filtering, exact identity
and exact landing are covered by regressions; physical crossings and hazard behavior remain in
the live backlog. Compilation, 433 focused tests and both Checkstyle tasks passed.

## September 7 remaining-family audit

Latest Fremennik follow-up: ten surface rope-bridge crossings are headless-migrated with strict
identity/exact landing; the two mine-shortcut rows now enforce 40 Agility and the agility toggle.
Current baseline is **1,331 legacy / 859 ordinary**. Five cave bridge rows remain pending
encounter/access review. Compilation, 423 focused tests and both checkstyle tasks passed.

Latest Isafdar follow-up: six log balances and eight tripwires migrated headlessly; current
baseline is **1,341 legacy / 869 ordinary**. Four leaf-pit rows remain pending a verified
fall/climb-out protocol. Logs now carry 45 Agility/partial Regicide gates and honour the
agility toggle. All fourteen require exact landing; physical acceptance remains deferred.

Latest short-agility follow-up: four trellis/jagged-wall crossings migrated and eight obsolete
ordinary Revenant variants removed in favour of ten typed, skill-gated shortcuts. Current legacy
baseline is **1,355 total / 883 ordinary**. Champions' Guild's eight old stone-chain rows remain
unmigrated pending verification of the newer single-stone geometry.

Latest follow-up: 46 Tarn pillar/ledge rows are headless-migrated with exact landing; baseline is
**1,365 legacy / 889 ordinary**. The expanded all-shortcuts/course inventory is owned by
[walker-agility-coverage.md](walker-agility-coverage.md), including missing rooftop route data.

Follow-up: the 18 exact Meiyerditch floorboard rows are now headless-migrated and Ranging Guild
entry metadata has been corrected to 40 Ranged (exit remains free). Current baseline is **1,411
legacy / 935 ordinary**; counts in the original audit below are the pre-batch snapshot. The 41
pillar rows remain candidates, not migrated or live-complete.

The production singleton-row classifier still reports **1,429 legacy entries**, including
**953 ordinary TRANSPORT entries**. These are generated route entries, not unique interactions
or account-accessible routes. No runtime transport eligibility changed in this audit.
`LegacyTransportInventoryTest.reportRemainingBehaviourGroups` regenerates the remaining groups
from the resource loader and production classifier; do not count already migrated TSV rows again.
For example, the 48 Lithkren barrier rows are already catalogue-owned.

### More missing or conflicting gate metadata

| Family | Remaining rows | Finding and required follow-up |
| --- | ---: | --- |
| Ranging Guild door 11665 | 4 | Source rows have no skill gate. Wiki requires 40 Ranged. Add the directional requirement and denial tests before migrating the offset crossing. |
| Ice gate 5043/5044 | 11 | Source rows have no quest/unlock gate. Entry requires starting Desert Treasure and speaking to the troll child; exit is unrestricted. Resolve the actual unlock state, not merely quest completion. Both halves route through the northern half, so object disappearance or incidental lateral movement cannot confirm arrival. |
| Hot vent door 30266 | 11 | Source rows have no access metadata. Inner Mor Ul Rek requires showing a fire cape/fire max cape; verify persistent unlock versus carried-item handling and unrestricted exit before migration. |
| Doors of Dinh 29322 | 16 | Fourteen newer rows include seven 50-Firemaking entries and seven ungated opposite destinations from the same origins; two older rows are also ungated. Resolve direction/endpoint ambiguity and the entry requirement before treating Enter as deterministic. |

Sources: [Ranging Guild door](https://oldschool.runescape.wiki/w/Guild_door_(Ranging_Guild)),
[Ice gate](https://oldschool.runescape.wiki/w/Ice_gate),
[Hot vent door](https://oldschool.runescape.wiki/w/Hot_vent_door), and
[Doors of Dinh](https://oldschool.runescape.wiki/w/Doors_of_Dinh).
These are audit findings, **not repaired or live-accepted gates**.

### Larger batch candidates

| Candidate protocol | Remaining rows found | Review boundary |
| --- | ---: | --- |
| Directed jumps: Jump-to Pillar / Floorboards | 41 / 18 | Best next shared-protocol review: exact landing, failed-jump position, retry/replan, and skill/quest metadata. Pillars include different areas and must not share eligibility merely by name. All 59 are candidates, not an approved migration count. |
| Direct entrances: Enter Tunnel / Door | 39 / 18 | Split direct clicks from dialogue, quest progress, warnings and multiple destinations; freeze an exact reviewed manifest. |
| Vertical transitions: Secret entrance / Climbing rocks / Rope / Steps | 16 / 15 / 14 / 14 | Reuse the existing staged executor only after checking tools, setup state, direction and landing. Counts are review sizes, not blanket-safe additions. |
| Wilderness obelisks | 318 | Large but separate random-destination protocol; do not count these as deterministic direct crossings. |

Recommended order: repair verified access metadata, then audit and migrate the directed-jump
protocol as one headless batch, with a representative live-test matrix added to the live backlog.
Do not bulk-enable every Open, Enter or Jump-to action. This audit did not operate the live client,
advance quests, or satisfy any outstanding live gate.

Validation: all 65 inventory and route-classification tests passed headlessly, along with
`checkstyleTest` and `git diff --check`. Corpus assertions retain the 1,429 total / 953 ordinary
legacy baseline.

## Working method

Migrate interaction protocols, not a handful of object IDs per rebuild. Audit all generated rows
for a candidate protocol together, implement its shared staged lifecycle once, and validate the
whole accepted set with positive/negative catalogue tests. Keep exact identity and requirement
boundaries; a matching action verb is not permission to enable a row.

Use one rebuild per coherent batch and representative live checks for each distinct protocol,
including its warning, lock, failure, and return variants where available. Do not require a physical
crossing for every duplicated approach row. Keep hazardous, account-locked, POH-facility, and League
cases explicitly headless/deferred. No batch bypasses NavigationEngine ownership or authorizes
Phase 7 / deletion of `processWalk` before the remaining gates are satisfied.

## Priority 1: staged item teleports

**Current:** 143 of the original 218 item rows are engine-owned; **75** remain legacy-owned.
The whole-catalogue legacy floor is **1,687** of 12,680 generated entries. The second batch adds
44 rows across 16 families using the existing executor, not another orchestration layer.

Before this batch, the production classifier left **218** `TELEPORTATION_ITEM` entries legacy-owned. The current
`SimpleTeleportPolicy` rejects colon-labelled destinations and Master Scroll Book rows. That is an
execution-protocol gap shared across many items, not 218 unrelated walkers to write.

| Review bucket | Rows | Intended approach / exceptions |
| --- | ---: | --- |
| Jewellery, talismans, rings, necklaces, amulets, bracelets, pendants | 74 | Shared inventory/equipment activation, observed destination selection, optional warning, directed landing. Charged variants and depleted-item disappearance need coverage. |
| Capes | 54 | Reuse the staged item lifecycle; distinguish direct destination actions from nested menus and equipment-only actions. Max/achievement/construction capes are not assumed to share one menu. |
| Other item protocols | 64 | Separate direct colon-labelled actions from books, diary rewards, crystals and special menus; includes 14 Quetzal-whistle rows that can potentially reuse the migrated destination-map protocol. |
| Master Scroll Book | 18 | Book interface, stored-scroll availability, exact destination, and optional Wilderness warning. Do not model stored charges as loose bank inventory. |
| POH tablets | 8 | House destination / instance contract; retain the existing advanced-facility live deferrals. |

**First batch implemented (2026-09-03):** all 128 jewellery/cape candidates were reviewed together.
**99 rows across 27 item families** now publish as `ITEM_TELEPORT`, leaving **119** item rows legacy-owned.
The shared lifecycle opens the inventory/equipment tab, selects the exact direct/submenu destination,
and retains ownership until landing, including when the last charge removes the source item.
It does not call a legacy Rub/dialogue orchestrator. A fixture captured from 112 live item definitions
checks every accepted inventory/equipment alternative; all 99 rows retain shared bank planning.

The **29 deferred candidates** are 22 Max cape rows (variant-specific/nested destinations), three
Burning amulet rows and Hunter cape's Black chinchompa row (Wilderness confirmations), two Camulet
rows (inventory/equipment activation differs), and one Mythical cape row (its alternatives include
inert item 21913). These require separate contracts or resource correction, not wider name matching.
The table above records original review sizes, not the remaining backlog. High-level/account-locked cape
destinations are headless-covered only unless a representative live result is explicitly recorded.

**Representative live result:** rebuilt-client dueling-ring tests passed inventory activation,
final-charge disappearance, and the equipment tab/open/use sequence. Castle Wars and Emir's Arena
returned normal `ARRIVED` without positional fallback or legacy ownership markers. The initial
offset-target request exposed a separate strict HTTP radius mismatch, retained in the main plan's
trace rather than hidden by the subsequent exact-catalogue-target passes. Direct-action cape use
and account-locked destinations remain headless-covered only. Temporary route settings were restored.
The caller-radius handoff and off-centre final-endpoint replan bugs found by stricter acceptance
are now corrected; the main plan records the failed charged run, non-consuming final-step live
verification, and 659 passing regressions. This correctness work does not change the row counts.

Representative checks should include a charged jewellery destination menu and a direct item action,
plus an available cape menu if this account has one. Unsupported cape/diary states remain headless.
Banking must use the existing shared requirement planner, subtract carried quantities, aggregate
consumables/charges appropriately, and refuse the final leg after failed preparation. A banked item
does not imply that its destination is unlocked or that a charged variant is usable.

**Second batch implemented (2026-09-03):** reviewed all 64 other-item rows and migrated 44:
Ardougne Monastery cloak (1), Karamja gloves (2), Morytania legs (4), Rada's blessing (5),
Kharedst's memoirs / Book of the dead (10), charged / infinite lyres (8), charged / eternal
teleport crystals (4), Drakan's medallion excluding Slepe (2), Pharaoh's sceptre excluding
Jaltevas (3), explicit Grand Exchange / Yanille tablets (2), and icy / stony basalt (3).
Inventory-only items never receive equipment commands. Exact aliases include the game's
`Jatiszo` spelling and the different worn cloak/legs actions. Stony basalt selects the
explicit entrance or roof command from the directed destination, independent of its saved toggle;
both basalts now require Making Friends with My Arm in the resource.

The 20 other-item deferrals are Quetzal whistles (14; Signal/map/locks), Calcified moth and
Mokhaiotl waystone (2; see the requirement corrections and remaining landing/cutover work below), and four direct-looking contracts:
Ardougne Farm (daily-use gating), Chronicle (charge discovery still issues input from planning),
Slepe (missing unlock gate), and Jaltevas (missing obelisk-attunement gate).
Together with the original 29 jewellery/cape deferrals, 18 Master Scroll Book rows and eight
POH tablet rows, these account for all **75** remaining item rows.

**Verification:** 663 selected regressions, compilation, benchmark and both Checkstyle tasks passed.
The unchanged read-only rebuilt-client probe reported `rows=218 supported=143 legacy=75 definitions=166`;
all 166 public definitions match the fixture. The 44 new routes remain physical-live-deferred:
the inspected memoirs book had no usable charges/page unlock and the GE tablet destination was locked.
No teleport, withdrawal or movement command was issued for this batch; no item charges were consumed.
The probe was undeployed. Next review: the 14 Quetzal-whistle map contracts, then the remaining
warning/charge protocols and the ordinary scene batches.

### Master Scroll Book executor (2026-09-05)

All 18 rows are now engine-owned with explicit stages for opening the inventory tab, opening item
`21389`, selecting the exact generated book widget, and retaining the route edge through landing.
Revenant Cave alone adds its exact affirmative warning option. Stored-scroll varbits remain the
availability contract, while bank planning withdraws one reusable book for repeated edges instead
of treating each consumed stored charge as another physical book. Headless policy, lifecycle,
banking, and production-classifier coverage passes; live acceptance remains pending on an account
with a charged book. Counts are now **1,559 legacy / 57 item legacy / 161 migrated item rows**.

### Quetzal-whistle audit and consumable prerequisite corrections (2026-09-03)

All 14 whistle rows were reviewed against the wiki MCP and public client definitions. Do not reuse
Renu's NPC executor directly: whistle `Signal` opens interface **949** (`QuetzalwhistleMenu`),
not `QuetzalMenu`, and the saved `SETTINGS_QUETZALWHISTLE_DEFAULT_TP` varbit **19681** can instead
make that input teleport directly to the Hunter Guild. The observed value was zero, but its
numeric mode semantics were not physically verified. Preserve the user's setting; do not infer
that every `Signal` input opens a map. `Last-destination` is likewise not a directed route command.

The six destination-build gates are already present: Cam Torum Entrance `9955`, Colossal Wyrm
Remains `9956`, Outer Fortis `9957`, Fortis Colosseum `9958`, Salvager Overlook `11379`, Kastori
`17757`. The other eight sites are built by default. Basic/enhanced/perfected IDs `29271`,
`29273`, `29275` do not encode their remaining charges; possession alone does not prove usability.
Whistles can be used without completing Renu's Twilight's Promise unlock, so do not copy that NPC
quest gate into their rows. These 14 routes remain deferred, not migrated or live-accepted.

Before cutover, cover inventory-tab -> Signal -> exact unlocked map destination -> landing as
NavigationEngine-owned stages, including delayed/missing map, saved direct mode, empty whistle,
last charge, cancellation, and a locked destination. Use a charged whistle for the representative
live check; none was carried during this audit and no game inputs were issued.

The same review corrected two existing resource contracts: remove inert IDs from moth/waystone
requirements, encode the moth as consumable (`T`, not unrecognized `Y`), and reduce its incorrect
Wilderness ceiling from 29 to 20. Three regressions failed before the patch. The unchanged live
read-only resource/quantity probe changed moth `repeated={29090=1}` to `{29090=2}`; waystones
remain `{31099=2}` and only those two usable IDs remain. **666** regressions, compilation,
benchmark, and both Checkstyle tasks passed. No production Java bytecode changed and the client
was not restarted; the probe reloaded built resources, not the active pathfinder catalogue.
A normal restart/catalogue reload is still needed for an already-cached route catalogue.

Both rows remain legacy-classified. Moth landing needs verification: the resource is `(1439,9564,0)`
but the wiki destination map shows `(1439,9550,1)`; the conservative completed-quest gate is also
stricter than the documented partial-quest unlock. Neither was guessed or relaxed. Waystone's
single `Channel` contract is ready for a later grouped cutover. Counts remain **1,691 legacy /
75 item legacy / 143 migrated item rows**; this pass fixes prerequisites, not ownership counts.

Sources: [basic whistle](https://oldschool.runescape.wiki/w/Basic_quetzal_whistle),
[Quetzal network](https://oldschool.runescape.wiki/w/Quetzal_Transport_System),
[calcified moth](https://oldschool.runescape.wiki/w/Calcified_moth),
[waystone](https://oldschool.runescape.wiki/w/Mokhaiotl_waystone).
Temporary probe: `%TEMP%/microbot-debug-probes/phase6-consumable-contract-20260903/ConsumableContractProbePlugin.java`;
it was undeployed after verification. No items, charges, settings or movement were changed.

### Dedicated boat/ship conversation audit (2026-09-03)

All ten remaining dedicated `BOAT`/`SHIP` rows were reviewed as complete protocols. The four exact
Cabin Boy Herbert rows now publish as `NPC_DIALOGUE_TRANSPORT`: after the quest-finished gate, the
engine owns `Talk-to`, optional Continue frames, the exact `Can you take me somewhere?` request,
the unique `Travel to <destination>.` option, and the directed landing. The policy admits only IDs
`10933`/`10932` at their exact Port Sarim/Piscarilius origins, their four packaged destinations,
zero fare/items/var-state, the single completed `A Kingdom Divided` requirement, and duration six.
The general `Talk-to` exclusion remains intact.

The subsequent Captain Shanks audit corrected the stale three-row contract to the two transcript and
live destinations, Khazard Port and Port Sarim. Both exact rows now publish as
`NPC_DIALOGUE_TRANSPORT`; the nonexistent Entrana-area third destination was removed. The engine owns
the ticket-purchase conversation, repeated Continue frames, affirmative fare choice, destination,
temporary voyage scene, and exact landing. Planning keeps the documented 50-coin upper bound while
the live NPC charges a random 20-50 coins and consumes the purchased ship ticket.

Three dedicated rows remain legacy-owned. Pirate Pete contributes two quest-finished routes; both
use an `Okay!` selection followed by variable Continue dialogue and need their exact landing/
completion lifecycle pinned before cutover. Ghost Captain contributes only the paid outbound row;
its fixed 25 ecto-token declaration does not model the equipped Ring of Charos(a) discount or the
permanent free-travel unlock. The free return row is already engine-owned by the direct-action family.

The Herbert cutover reduced the production-classifier floor from **1,691 to 1,687** and dedicated
SHIP legacy rows from seven to three. Shanks then reduced the current floor from **1,677 to 1,674**:
dedicated SHIP is zero and BOAT remains three. Focused policy/scanner tests and corpus tests
pin all four Herbert rows, both exact Shanks rows, the three-row remainder, and the new global
count. All **337** selected transport/navigation/feature-gate tests, compilation, Pathfinder benchmark,
and both Checkstyle tasks passed. A read-only probe against the existing client found
`A Kingdom Divided=NOT_STARTED`, all four resource rows, and zero Herbert rows in the active
account-filtered catalog. This confirms the unchanged quest gate and account prerequisite, not the
new executor bytecode. Physical Herbert travel remains live-deferred until an eligible account is
available; the active client still needs a normal restart to load this patch.

Probe: `%TEMP%/microbot-debug-probes/herbert-audit-20260903/HerbertAuditProbePlugin.java`; it was
undeployed after capture. No movement, dialogue, item, or setting input was issued.
The same probe found `Rum Deal=NOT_STARTED`, `Ghosts Ahoy=NOT_STARTED`, and `Shilo Village=FINISHED`.
Captain Shanks was subsequently live-accepted to Khazard Port on 2026-09-04. Consecutive visible
Continue pages now use a short retry, dialogue disappearance promotes the command to the remote-
landing timeout, and arrival Continue cleanup no longer depends on the fare row remaining affordable.

Sources: [Cabin Boy Herbert transcript](https://oldschool.runescape.wiki/w/Transcript:Cabin_Boy_Herbert),
[Captain Shanks transcript](https://oldschool.runescape.wiki/w/Transcript:Captain_Shanks),
[Pirate Pete transcript](https://oldschool.runescape.wiki/w/Transcript:Pirate_Pete), and
[Ghost captain](https://oldschool.runescape.wiki/w/Ghost_captain).

## Priority 2: ordinary scene protocols

There are **1,186** ordinary `TRANSPORT` entries. The following disjoint action-normalized buckets
sum to that count; they are review sizes, not safe-migration counts.

| Review bucket | Rows | Main gates before enabling a protocol |
| --- | ---: | --- |
| Entrances/exits | 328 | Direct action versus dialogue/cutscene; entrance and return requirements checked independently. |
| Vertical/climbing | 304 | Reuse catalogue transitions where deterministic; ropes, tools, heat/light protection, and failure landings remain explicit. |
| Traversal obstacles | 251 | Directed side-aware landing, skill gates, failure outcomes, and no accidental classification of traps as simple jumps. |
| Gates/puzzles/fares | 161 | Separate free direct boundaries, exact paid actions, lock-picking, and stateful puzzles. |
| Tools/menus/special | 142 | Reuse migrated executors where the complete protocol matches; otherwise explicit stages or defer. |

At least **72** of these ordinary legacy rows belong to inputs with multiple declared destinations,
even when grouping by origin, object ID, action, name, and display. That count is over the legacy
subset only. Before migration, repeat the ambiguity check over the **complete** generated catalogue
using the actual executable input identity; display text alone must not disambiguate an object click.
Check shadow rows against migrated network edges rather than assuming each shadow needs a new handler.

Audit whole vertical/entrance protocol sets together after the item batch. Reuse the fixed landing
scanner and existing stage lifecycle; retain explicit exceptions for known puzzle/cutscene/hazard
protocols. Existing exact Steps exclusions are unchanged by the landing fix.

## Deliberately separate contracts

The remaining **426** non-item/non-ordinary entries are: Wilderness obelisks 318, multi-code fairy
rings 53, agility shortcuts 14, grapple shortcuts 12, ambiguous generic portals 12, spell teleports
11, ships 3, and boats 3. Obelisks alone include 270 random-destination remote rows and 48 self-pad
artifacts; do not inflate progress by treating random travel as deterministic arrival. These contracts
need explicit handling or an agreed deferral, not broad action-name eligibility.

## Inventory provenance

The read-only rebuilt-client probe loads `Transport.loadAllFromResources()` and applies production
`PathfinderRouteCalculation.classifyTransportEdge` to each singleton row. The snapshot contains
**12,680 generated entries**. The pre-item-batch inventory had **1,834** legacy entries; the 99-row
first cutover reduced that to **1,735**; the additional 44-row batch reduces it to **1,691**,
and the four Cabin Boy Herbert rows reduce it to **1,687**, confirmed by production-classifier
corpus regressions. These
are not source-line counts or the current account's unlocked-route count.

Temporary probe: `%TEMP%/microbot-debug-probes/phase6-landing-batches-20260903/LandingBatchProbePlugin.java`.
Its generated inventory is `%TEMP%/phase6-legacy-batches-20260903.tsv`; no live account data is exported.

For repeatability, ordinary action grouping strips spaces/hyphens and ignores case. Vertical actions
are `climbup/climbdown/climb/ascend/descend/walkdown/goup/godown/climbinto/climbthrough/crawldown`;
entrances are `enter/exit/leave/exitthrough/gothrough/passthrough/crawlthrough/getin/jumpin/jumpinto`;
traversal is `jumpto/jump/jumpdown/jumpacross/jumpover/jumpfrom/jumponto/walkacross/cross/crossbridge/
climbover/squeezethrough/stepover/swingon/crawlunder/slide`; gate buckets start with
`open/pass/quickpass/push/move/unlock/picklock/paytoll`; everything else is special. Item grouping
uses the family before `:`, reserving Master Scroll Book and POH tablets first, then `cape` suffixes,
then jewellery-name terms; these are inventory labels, never production eligibility predicates.
