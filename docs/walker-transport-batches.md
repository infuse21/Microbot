# Phase 6 behaviour-based batches

Latest residual metadata audit: 2026-09-09; older sections below retain their dated snapshots.
This is the execution backlog for
[walker unification](walker-unification-plan.md), not a new ownership policy.

## September 9 bulk residual metadata audit

September 10 mounted-menu cutover: the three MountedDigsite and four MountedXerics
destinations now share explicit NavigationEngine portal stages. A base or non-selected
decorative variant opens Teleport menu, the next observation selects the exact visible and
unlocked destination, and a destination-specific transformed variant uses its direct action.
The resolver scans only the current house/world view and approaches the real room object;
missing or struck-through entries become UNAVAILABLE instead of falling into
`PohTeleport.execute()`. The fail-first ownership test reported the old zero object identity.
Compilation, focused mounted/chamber scene and ownership tests, client-thread/queryable
guardrails, pathfinder benchmark and both Checkstyles pass. The guardrail baseline remains
903/0 and no exemption was added. Physical testing is deferred; nexus, jewellery, POH
ring/tree integration and graph generation remain open.

September 10 POH network-generator correction: permutation-expanded transports now retain
their source and destination endpoint components. `createTransportsToPoh` enumerates every
unique external origin and destination, creates one inbound and outbound house connection
per endpoint, and combines only the applicable endpoint requirements. This replaces the
`findFirst()` behavior and avoids both missing inbound origins and requirement contamination
from an unrelated destination/source. A fail-first two-origin/two-destination test pins all
four house connections, item isolation and outbound de-duplication; focused POH,
transport-cost and route-calculation tests pass. Runtime POH fairy-ring/spirit-tree ownership
is still separate implementation work.

Direct mounted facilities: four MountedGlory routes (13523) and one MountedMythical route
(31986) now share the direct house portal lifecycle and resolved-room approach. Their exact
cache actions are Edgeville/Karamja/Draynor Village/Al Kharid and Teleport respectively.
This brings the direct typed POH slice to 45 destinations. Mounted digsite and Xeric's were
subsequently admitted by the staged September 10 cutover above. Focused engine, scene, ownership,
portal, guardrail and benchmark checks plus compilation and both Checkstyles pass (1m30s).

Chamber ownership follow-up: all 40 typed chamber destinations now publish the engine's
TELEPORTATION_PORTAL lifecycle. Resolution checks house presence, current world view, base
or active object identity and exact destination action. The engine approaches the actual
room portal rather than the synthetic house anchor. All variant and rejection cases, source
unloading and selected landing have headless coverage. A 2,212-test full suite passed before
the final approach correction; the final 90-test engine/portal/guardrail/benchmark selection,
compilation and both Checkstyles passed afterward. Physical house-layout/diary/destination
checks remain in the live ledger; other POH facility families and graph generation remain open.

POH chamber prerequisite correction: `PohPortal.execute()` no longer hard-codes Enter for
every destination. Varrock, Grand Exchange, Camelot and Watchtower expose named destination
actions; `getAction()` now supplies those exact names, with Enter for the other chamber entries.
The offline cache audit resolved all 149 referenced constants across ObjectID's inherited
source files and inspected 160 definitions including transforms. Varrock bases transform on
4585 and Camelot bases on 4560; their active variants offer destination actions, Toggle and
Remove, not Enter. Watchtower variants likewise offer Watchtower/Yanille actions.
`PohChamberActionTest` exercises dispatch for all 40 enum entries with mocked object helpers.
This fixes shared action metadata only: chamber route ownership, instance-local approach,
transformed-ID resolution and directed-landing acknowledgement still require migration.
Compilation, both focused POH tests and both Checkstyles pass (1m6s). No physical test or
client restart was performed for this headless prerequisite correction.

Historical POH generation audit (fixed 2026-09-10): `PohPanel.createTransportsToPoh` selected only the first matching
origin group with `findFirst()`. `Transport(origin, destination)` retains `origin.origin`,
so every generated inbound house edge is still from that single selected source; iterating
its destination rows does not create inbound edges from those destinations. The map keys
match those edges, but other network origins receive no inbound connection. Simply iterating
all groups is insufficient as a complete fix: already-expanded rows merge both endpoint
requirements, and Transport uses identity equality, so naive rebuilding also duplicates
house-outbound edges and could retain an unrelated source's requirements. The endpoint-retention
correction above now has multi-origin and source-only/destination-only requirement coverage.
This remains independent of whether a test account owns a house ring/tree.

POH facility dispatch is also separate: `PohTransport.execute()` delegates directly to the
facility's `PohTeleport.execute()` through the legacy `Rs2Walker.handlePohTransport` path.
The first bounded runtime migration should cover deterministic chamber portals, whose
`PohPortal` enum already identifies destination-specific object IDs; nexus and jewellery
interfaces need their own staged protocols rather than reclassifying the whole POH type.

Acceptance scope now follows the user's September 9 amendment in the unification plan:
unavailable live tests remain deferred and do not block Phase 6 implementation/headless closure.

Post-Ice-Troll historical inventory: 49 exact identity matches (requirements not compared),
36 nearby-ID candidates, six same-direction candidates and 64 rows with no local match.
These are diagnostic row counts, not remaining implementation counts: duplicate approaches,
malformed identities and replacement geometry still require individual contract decisions.
The full-suite bridge regression now covers all 15 loaded Fremennik crossings (ten surface,
five underground), restricts the level-40 assertion to mine-shortcut IDs 21314/21315, and
checks incomplete positions using each route's actual direction and plane.

Implemented batch: six Tears-cave approaches (6659 inward, 6658 outward). The
[Chasm of Tears](https://oldschool.runescape.wiki/w/Chasm_of_Tears) explicitly needs no light
source and is accessible before Tears of Guthix completion; the adjacent
[Lumbridge Swamp Caves](https://oldschool.runescape.wiki/w/Lumbridge_Swamp_Caves) need light
unless their eternal fire is lit. Do not conflate access to Juna with the quest-gated minigame,
games-necklace destination or light-creature ride. Player/plugin owns protective loadouts;
neither tunnel inherently consumes a travel item. The missing members metadata is corrected.

Offline cache probe `TearsPlacement.java` in the existing temporary cache-probe directory finds
6659 at (3225,9539,0), bridge flags 8 (not a bridge-plane adjustment), and 6658 at (3218,9533,2),
flags 0; both expose Enter. The inward anchor is three tiles from the old Y=9542 approaches,
beyond the generic two-tile resolver radius. The scene now uses an exact per-direction anchor rather than a
broad Tunnel-name search. Quest helpers independently target these same objects at nearby
footprint points (3226,9540,0) and (3219,9534,2). Source durations disagree (central 0, side 2);
physical timing and landings remain unverified. The six rows retain their individual historical
durations and source comments; no invented canonical timing is substituted. No quest gate,
automatic light-source banking or quest-solving interaction is added.

Bulk cache definitions also identify swapped Shayzien source IDs: 31967 is Rope/Climb
(`shayzienquest_lab_exit`), whereas 32507 is Magic Gate/Pass (`shayzienquest_cave_door`).
The disabled rows use the opposite identities, and the rope origin (9363,1050,0) is not a
verified template coordinate. Correct placement/unlock contracts before restoring either.

Placement follow-up: `ShayzienPlacement.java` (temporary offline cache probe) confirms the
climbable rope 31967 at (1168,9973,0), with an actionless rope prop 31968 directly above on
plane 1. Tile flags are 8, not the bridge-plane bit. Magic Gate 32507 is at (1171,9946,0)
with Pass; the historical supposed gate origin (1169,9973,0) is actually beside the exit rope.
Surface crevice base 32502 is at (1214,3558,0), with Inspect/Enter variants 31965/31966.
The old rope destination (1214,3557,0) is adjacent to that surface placement, but adjacency
does not establish the server's exact landing. Do not simply swap IDs and activate both rows.
The local TaleOfTheRighteous quest helper independently targets exit rope 31967 at the same
template anchor. Its barrier step and the
[wiki walkthrough](https://oldschool.runescape.wiki/w/Tale_of_the_Righteous#Another_expedition)
identify the corrupt-lizardman encounter separately. Remaining implementation decisions are
the outward rope's landing contract and the gate's post-unlock crossing contract, not automatic
combat or quest solving. Neither is marked implemented or live-passed by this metadata audit.

Fremennik cave 21584 is now supported for three exact approaches at quest varbit 3311 values
300 and 310 (six variants), using the existing quest-state passage dispatcher and cache anchor
(2401,3889,0). QuestHelper routes entry at those pre-king-death stages. Stage 320 and later
are not assigned the old landing: the [quest guide](https://oldschool.runescape.wiki/w/The_Fremennik_Isles)
describes direct return to the corpse, and the [caves](https://oldschool.runescape.wiki/w/Ice_Troll_Caves)
are western-entrance-only after completion. No combat or equipment preparation is added.

The five underground bridge rows now use exact quest-state passage ownership.
Cache anchors (plane 1, tile flags 1) are 21316=(2385,10263), 21317=(2385,10260),
21318=(2397,10258), 21319=(2394,10258). QuestHelper's boss-entry step uses 21316 and
requires FRIS_TASK/3312=0 after ten trolls; only that southbound boss-entry row carries this
requirement. The return and east/west passage rows carry no item or surface bridge-repair gate.
The quest guide explicitly describes leaving via the east bridge or returning the way entered
after the fight. Static metadata does not prove pre-victory retreat or post-quest traversal;
those physical states remain in the live ledger. No combat or automatic quest progression occurs.

Museum evidence correction: the [Old passageway](https://oldschool.runescape.wiki/w/Old_passageway)
Wiki page distinguishes upstairs object 31892 (Leave to the Falador Party Room) from downstairs
object 47316 (decorative, no Leave). The earlier source comment calling 31892 an unusable prop
was not supported. Offline cache inspection places the exits at (3013,9951,1) and (3066,9951,1),
both with bridge flag 2, giving effective plane 0 beside the four historical approaches.
Those four directed exits are now restored under exact catalog ownership; item, fare, quest,
skill and membership requirements remain absent. Landing acknowledgement requires the selected
historical surface tile. Source comments retain the original four rows as audit evidence.
The OSRS MCP loctype index independently distinguishes `31892 osb5_passageway_leave` from
`47316 osb5_passageway_noop`; it does not provide placement or landing verification.
The separate offline cache probe confirms Leave versus no actions and the placements above.
Its path is `%TEMP%/microbot-debug-probes/leaf-pit-cache-20260909/MuseumPlacement.java`.
Actual server landing and a rebuilt-client journey remain live gates.

Karuulm implementation follow-up supersedes the research deferral below: the 24 directed
elevator/rock/stair contracts now have 54 exact variants, with free safe-entry/escape directions
and equipment/claimed-diary gating for onward heat travel. Real equip levels are rechecked in
the scene adapter before input; the existing non-blocking equipment stages own boot preparation.
The original ungated elevator/stair comments remain audit evidence. Compilation, 47 selected
tests including the benchmark and guardrails, and both Checkstyle tasks passed. Loader count:
6,139 origins. The live ledger retains all unproved physical boundary, banking and landing cases.

Karuulm boundary research: the [dungeon](https://oldschool.runescape.wiki/w/Karuulm_Slayer_Dungeon)
and [mountain](https://oldschool.runescape.wiki/w/Mount_Karuulm) pages explicitly distinguish the
safe entrance chamber from the damaging floor beyond it. The elevator's four disabled approach/
landing variants are therefore not inherently heat-gated; an implementation must not require
boots just to visit the Tasakaal. The bundled Ascent of Arceuus helper anchors its Activate
object at `(1311,3807,0)`. Eight disabled upper-floor stair rows remain a separate hot-floor
contract. The resource already has twelve object-34544 rock crossings around the entrance;
audit their directed geometry, warning handling and protective requirements together before
making deeper routes newly reachable through the elevator. The shared equipment adapter chooses
the first carried alternative without checking equip levels, so reusing it unchanged would not
establish readiness: [boots of stone](https://oldschool.runescape.wiki/w/Boots_of_stone) require
44 Slayer; [granite boots](https://oldschool.runescape.wiki/w/Granite_boots) require 50 Defence/
Strength; [Boots of Brimstone](https://oldschool.runescape.wiki/w/Boots_of_Brimstone) require
44 Slayer and 70 Defence/Magic/Ranged.
Elite diary *reward* immunity must be distinguished from task completion. No elevator, stair or
rock contract was changed by this research, and no physical boundary traversal was performed.

Mushtree candidate audit: all 12 historical rows have nearby generated `MAGIC_MUSHTREE`
edges with engine ownership, but the attempted transport-free connectivity proof failed.
All four historical source tiles expose no cardinal exits in the bundled collision map;
none of the 12 old-origin-to-generated-origin checks succeeded. This is not evidence that the
current generated routes fail, nor proof that the old tiles are valid player standing tiles.
The audit therefore records, rather than assumes, connectivity in
`runelite-client/build/reports/walker-mushtree-connectivity.tsv`. The historical ordinary rows
remain disabled; do not count this audit as 12 new migrations or re-enable them to bypass the
network configuration/unlock handler. The 20 selected inventory, mushtree-policy/scanner and
feature-toggle tests plus Checkstyle passed in 23s. Physical/scene confirmation of the old
anchors remains separate from the already implemented generated-network protocol.

Loaded-graph follow-up: `DisabledTransportInventoryTest` now emits
`runelite-client/build/reports/walker-disabled-transports.tsv` from the real resource loader,
including generated family edges. Of 155 historical comments, 19 have exact directed identity
matches (requirements are not compared), six have same-direction/different-identity candidates,
36 have same-object candidates within three tiles at both ends, and 94 have no such local match.
The nearby group includes 12 mushtrees, 16 jungle rows, four rock slides, two vines and two portals.
Those candidates require geometry/protocol review; neither 94 nor 130 is a remaining-migration
count. The audit asserts all exact aliases still classify as engine-owned and pins both Canifis
approaches. The test and Checkstyle passed in 18s. It does not enable any historical row or prove
account availability, requirements equivalence or live travel.

Post-Canifis cross-resource recount: 155 commented numeric rows remain; 19 match an active TSV
origin/destination/action/name/object-ID signature after normalizing case, spaces and hyphens.
Twenty-five match an active direction regardless of identity. These are candidate aliases, not
proof of equivalent requirements. This scan excludes dynamically generated families, so its
130 unmatched directions are not a remaining-work count either. In particular, historical
mushtree and jungle rows must be checked against their specialized resource loaders. Keep the
74-contract historical tally explicitly non-authoritative until a loaded-graph audit reconciles
all generated families, quest variants and malformed entries.

Canifis follow-up: the two 5055/Open approaches now have post-Myreque quest-gated catalog
ownership with dispatch-time quest checks and exact cellar landing. The historical ungated
comments stay disabled. Loader count is 6,129 origins. Earlier quest-stage access and physical
acceptance remain open; the older 74-source-contract tally below has not been re-audited into
a count of fully resolved routes. See the live ledger for source evidence and remaining gates.

Eastern Brimhaven stones audit: the four commented ordinary rows duplicate four already-active
`AGILITY_SHORTCUT` rows, all of which incorrectly required 56 Agility. A transport-free flood
fill of the collision map proves 2690,9547 and 2695,9533 share the island, disconnected from
2682,9548 and 2697,9525. The [wiki](https://oldschool.runescape.wiki/w/Stepping_stones_(Eastern_Brimhaven_Dungeon))
requires 56 only towards the island. The canonical agility rows now encode that asymmetry,
retain their four-tick cost and feature switch, resolve the exact source-side stone and require
the exact destination. No duplicate ordinary rows were enabled.

Copied-cache source anchors, in directed-origin order: 2682,9548 -> 2684,9548;
2690,9547 -> 2688,9547; 2697,9525 -> 2696,9527; 2695,9533 -> 2695,9531.
Only 19040 has Cross; 19041 is actionless scenery between the active endpoint stones.
Compilation, all 36 focused tests, benchmark and both Checkstyle tasks passed in 1m 48s.
The first run exposed a guardrail attribution issue on the skill ordinal lookup; making its
client-thread snapshot explicit resolved it without exemptions. This corrects the earlier backlog count: its 78 “genuine disabled
traversals” included these four active duplicates. The remaining **74 unresolved source
contracts are not a verified count of distinct missing runtime routes**; further deduplication
and protocol audits are required before using that number as a Phase 6 completion measure.

Quest-state passage batch: five directed contracts publish eleven disjoint variants. Zogre's
already-crushed barricade requires 496=1 (6878 -> 6882); Slug Menace's already-open wall requires
2618=1 (19124 -> 18360), with the static reverse passage 18412 ungated for escape. Enakhra rubble
33342 exposes Climb-over 33340 only at quest states 50, 60, 65 and 70; both directions encode those
four exact values rather than a broad >=50 gate admitting absent transforms. Cache placements and
the bundled quest helpers establish the exact anchors. The walker does not crush the barricade,
push the quest wall, build the temple or fight anything. Compilation, all 25 focused tests,
benchmark, unchanged guardrail and both Checkstyle tasks passed in 1m 42s. The latest totals
are 6,127 loaded origins / 78 genuine disabled traversals; physical acceptance remains open.

The placement audit also disproved a tempting Grand Tree substitution: varp-150 wrapper 2444
(which changes to Climb-down 26243 at 130-160) is at Glough's house, 2487,3464,2. The central
2463,3497,0 entrance is static 2446/Open, so that wrapper cannot establish its access protocol.
Likewise 18342 is Slug Menace lair-wall scenery, not Enakhra bones; those two malformed bone
rows remain disabled. Probe: `%TEMP%/microbot-debug-probes/leaf-pit-cache-20260909/QuestAccessCache.java`.

Quest-trapdoor protocol audit (after the Molch batch):

| Entry | Verified evidence | Required before cutover |
| --- | --- | --- |
| Basement of Doom | Cache 12267 is Trapdoor/Open; 12268 is Open trapdoor/Go-down/Close. Both bundled Evil Dave helpers explicitly pair these IDs at 3077,3493,0. The wiki requires completed Shadow of the Storm. | Publish the actual Go-down traversal with exact closed-state preparation, retain its quest gate, and test that Open alone never acknowledges descent. Confirm underground landing. |
| Grand Tree | Cache 2446 is GRANDTREE_TRAPDOORUNDER with Open, no transform array. The wiki allows the tunnels during and after the quest, but the northern mine specifically after completion. Nearby constants 2444/2445 are separate objects, not demonstrated transforms of 2446. | Verify the exact central entrance protocol and quest stage; do not assume opening always exposes a second object or equate all tunnel access with completed-quest mine access. |
| Canifis tavern | Cache 5055 has Open and no transform array; 5054 is the underground Climb-up ladder. The Myreque helper exits via 5054 at 3477,9846,0. | Establish the surface Open outcome and entry unlock before choosing direct versus staged handling; an actionless transform was not found by this audit. |

Sources: [Basement of Doom](https://oldschool.runescape.wiki/w/Basement_of_Doom),
[Grand Tree Tunnels](https://oldschool.runescape.wiki/w/Grand_Tree_Tunnels), copied-cache object
definitions, and the bundled RFDEvilDave/MakeEvilStew/InSearchOfTheMyreque helpers.
Implementation follow-up: Evil Dave's corrected 12268/Go-down row is now active under the exact
quest-gated catalog handler; its old 12267/Open row remains commented historical evidence.
Compilation, 110 focused tests, benchmark, unchanged guardrail and both Checkstyle tasks passed
in 1m 45s. The loader reports 6,122 origins; resolving this historical entry leaves 83 genuine
disabled traversals. The three Grand Tree/Canifis approaches remain implementation work.
The shared handler must distinguish a preparatory Open
from a direct Open-triggered traversal using evidence, rather than installing a universal second
click for every object named trapdoor.

Molch batch: all 20 directed crossings now use the catalog handler, including four restored
barrier-34642 rows previously mislabelled as Karuulm object 34542. The copied cache identifies
34542 as lava scenery and 34642-34646 as five independent wrappers using varbits 7942-7946.
States 0-2/3-5/6-10 expose green/orange/red objects 34432/34433/34434; unknown states are refused.
Cache placements have the bridge flag, yielding the existing effective plane-0 coordinates.
The [Lizardman Temple documentation](https://oldschool.runescape.wiki/w/Lizardman_Temple) describes
increasing crossing damage and decay, not Karuulm heat. Dispatch budgets a possible next-colour
increase and requires HP above that budget. Exact geometry, membership, live colour/action and
opposite-side acknowledgement replace the old generic adjacent shortcut. Compilation, 89 focused
tests (zero failures/errors/skips), benchmark, unchanged guardrail and both Checkstyle tasks passed
in 1m 10s. Loaded origins are now **6,121**, with **84 genuine disabled traversals** after resolving
the four mistyped entries; their commented aliases remain historical evidence, not open routes.
Physical colour/damage/counter acceptance remains in the live ledger. Older counts below describe
the earlier checkpoints.

Revenant payment audit: the [cave documentation](https://oldschool.runescape.wiki/w/Revenant_Caves)
confirms a 100,000-coin fee that the server can deduct directly from the bank. It persists until
any death inside the caves or a PvP death elsewhere in the Wilderness; ordinary death elsewhere
is not the documented invalidation rule. Do not implement this as an unconditional withdrawal
or a fee for every traversal. Wiki MCP varbit/varp searches and the generated gameval catalog
did not establish a Revenant paid-state variable. `6506` is `wilderness_cave_mid_warning`, not
payment; `14693` is `wbr_entrance_fee_paid`, not evidence for the Revenant contract. The 16
object-40386 approaches remain disabled, and the 88-traversal remainder is unchanged. Required
evidence before cutover: exact unpaid/paid confirmation frames, bank-only deduction behavior,
and a confirmed paid-state signal or an explicit protocol that handles unknown state without
excluding already-paid access or charging again. No client input or payment was issued.

Northern shortcut batch: four Ungael side passages and all ten Weiss cliff directions now use
exact NavigationEngine catalog ownership. The copied cache confirms the three Ungael base/transform
pairs and anchors; `DragonSlayerII` identifies `DS2_FREM >= 30` as defeated quest Vorkath, matching
their actionless-to-actionable transform boundary. The Weiss rope transforms activate at quest
stage 45, also matched by `MakingFriendsWithMyArm`'s scaling-the-mountain steps. Static rockslides
and ledge do not gain an invented completed-quest gate. No rope is withdrawn or installed.

The [rockslide](https://oldschool.runescape.wiki/w/Rockslide_(Weiss)),
[tree](https://oldschool.runescape.wiki/w/Tree_(Weiss)) and
[ledge](https://oldschool.runescape.wiki/w/Ledge_(Weiss)) articles distinguish 68-Agility ascents
from unrestricted descents; the [quest guide](https://oldschool.runescape.wiki/w/Making_Friends_with_My_Arm)
documents up to 15 damage on rockslides. The engine now recognizes an exact earlier Weiss stage
after an issued crossing, including zig-zag falls missed by a simple direction projection, and
requests a replan rather than reissuing the later crossing. All 14 directions require the exact
source tile before dispatch and exact destination before acknowledgement. The live ledger retains
actual fall positions, damage, crossings and rebuilt-client acceptance as unproved.
Validation passed in one combined run: compilation, 142 tests, unchanged guardrail baseline and
both Checkstyle tasks, 1m 35s. The benchmark's total best-route times were 1,343ms. The latest
production totals are **6,117 loaded origins / 88 genuine disabled traversals**; physical acceptance
remains deferred and no client restart or commit was performed.

Brimhaven payment contract follow-up: copied cache varbit definitions place 5628 at varp 393
bit 0 (single-entry fee), 5629 at bits 1-2 (southern backdoor), and 8123 at bits 0-3 (door data).
`KARAM_DUNGEON_PERMANENTACCESS` is 8122; the bundled Rag and Bone Man II helper independently
records door data changing 0 -> 8 on permanent purchase. Thus southern unlock bits must not
affect the entry fare. The new pure `BrimhavenEntrancePolicy` covers all 16 valid door states,
coin shortfalls, the Yes/No and million-coin-holder menus, scoped Saniboch receipt continuation,
and refusal to pay again after confirmation. It never selects permanent purchase. Dialogue
evidence: [Saniboch transcript](https://oldschool.runescape.wiki/w/Transcript:Saniboch).
**Integrated follow-up:** scene dispatch and graph requirements now cover 21 canonical variants
across the seven approaches, replacing 14 disabled aliases. Paid, already-paid and permanent
access use disjoint predicates; exact underground landing clears the NavigationEngine leg.
The conflicting exit landing remains unresolved and its rows remain disabled. The Brimhaven checkpoint
totals were 6,106 loaded origins and 102 genuine disabled traversals. Physical acceptance is still open.
Validation: compilation, all 12 focused Brimhaven tests and both Checkstyle tasks passed
in 1m 25s. No client restart, gameplay input, full-suite run or commit was performed.
Integrated validation: the initial 87-test batch passed production classification and the
pathfinder benchmark (2,656ms total best-route times), but exposed an explicit widget-wrapper
guardrail issue and an incomplete scene fixture. After correcting the wrapper and fixture
(including its Supplier overload), compilation, all 14 Brimhaven/guardrail checks and both
Checkstyle tasks passed in 59s. The guardrail remains at 903 with no new exemptions. The scene
test exercises all 21 variants, payment/receipt/entry, refreshed access without coins, object
identity/anchor rejection and no repeated confirmation. No live crossing is claimed.

The copied game cache was queried in one pass for every object ID extracted from commented
numeric transport rows, plus its direct transforms. This includes historical duplicates and
malformed rows, so it is not a new count of genuine disabled traversals. Current implementation
counts at that metadata-only audit were 6,099 loaded origins and 116 genuine disabled traversals;
the integrated Brimhaven follow-up above supersedes those counts.
Probe: `%TEMP%/microbot-debug-probes/leaf-pit-cache-20260909/DeferredTransportCache.java`.

| Shared batch | Verified cache evidence | Remaining implementation evidence |
| --- | --- | --- |
| Paid Brimhaven entrance | Base 20877 uses varbit 8123: values 0/2/4/6 resolve to Pay 34713; 1/3/5/7 and 8-15 resolve to Enter 20876; fallback absent. Exit 20878 has Leave. | Decode temporary/permanent access, payment outcome and conflicting exit landings; 20876/20877 are not stale IDs. |
| Quest-transformed passages | Ungael bases 29868/29869/29870 use 6108: values 0-29 are actionless, fallback transforms 31994/31996/31998 have Climb-over/Climb-down/Climb-up. Weiss 33327/33328 use 6528: 0-44 actionless, fallback 33187/33188 has Climb. | Verify quest semantics and placements before defining access bounds; fallback action alone does not prove a safe route or failure recovery. |
| Other quest transforms | Barricade 6878 uses 496; wall 19124 uses 2618; Harmony door 22119 uses 3393; statue 22355 uses 3401; rubble 33342 uses 1560. | Freeze eligible quest states and exact landings together, reusing the existing catalog scene snapshot. |
| Non-transforming obstacles | Crevice 40386 has Jump-Down; bridges 21316-21319 have Walk-across; elevator 34359 has Activate; steps 34530/34531 have Climb. | Their payment, repair, equipment or destination contracts still need evidence; having an action does not establish eligibility. |
| Malformed/obsolete identity rows | 31967 is Rope/Climb while 32507 is Magic Gate/Pass; 17900, 18342 and 34542 have no actions. | Do not simply uncomment or swap IDs: geometry and actual route identity must be resolved first. |

Wiki MCP varbit lookup independently confirms the names `karam_dungeon_doordata` (8123),
`ds2_frem` (6108), and `my2arm_status` (6528), matching `VarbitID`. This is metadata evidence,
not live crossing acceptance. Source deferral comments for Brimhaven and Ungael are corrected.
Implement related contracts in batches with one focused validation run; reserve full-suite runs
for major checkpoints. Existing live-only entries remain open in the acceptance ledger.

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
