# Walker agility coverage

Scope expanded by the user on 2026-09-07: all agility shortcuts and training-course obstacles
should become NavigationEngine-owned traversals. This includes obstacles absent from the current
transport catalogue, not only rows still classified legacy. Automatic lap scheduling, rewards,
mark collection and training strategy are not implemented by adding traversal edges.
The user clarified that this means ways up/across/down while navigating to a destination,
not prescribed training routes or complete laps. Course inventories identify available obstacles;
they do not mandate traversing every obstacle in course order.

## Acceptance contract

Each directed obstacle needs verified identity/action, approach and landing, current-level and
quest/item/unlock requirements, config filtering, and failed-obstacle recovery. A click, animation,
object disappearance or proximity to a different platform is not successful traversal. Preserve
NavigationEngine ownership; do not introduce per-course walking/retry loops or quest solving.
For instanced/timed hazards, static coordinates alone are insufficient.

Headless support and live acceptance are separate. The existing headless-first instruction remains
in force: do not restart or drive the client without a requested live-testing stage. Track physical
verification in [walker-live-testing-backlog.md](walker-live-testing-backlog.md).

## Rooftops: missing route catalogue, not just missing ownership

The nine local quest-helper course classes define 74 ObjectStep obstacles. Resolving their
ObjectID constants and checking the shortestpath TSV action/object fields found zero matches
for those IDs on 2026-09-07. This is an ID-coverage audit, not a proof of every transformed variant.
ObjectStep positions and Zone bounds provide useful source evidence but are not verified landing
tiles. Do not copy object positions into destination fields or turn whole roof zones into arrivals.

| Course | Local obstacle steps | Status |
| --- | ---: | --- |
| Draynor Village | 7 | Route endpoints/actions/level gates to verify and add |
| Al Kharid | 8 | Pending catalogue and traversal coverage |
| Varrock | 9 | Pending catalogue and traversal coverage |
| Canifis | 8 | Pending catalogue and traversal coverage |
| Falador | 13 | Pending catalogue and traversal coverage |
| Seers' Village | 6 | Pending catalogue and traversal coverage |
| Pollnivneach | 9 | Pending catalogue and traversal coverage |
| Rellekka | 7 | Pending catalogue and traversal coverage |
| Ardougne | 7 | Pending catalogue and traversal coverage |

Source definitions: `runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/helpers/skills/agility/`.
The [wiki rooftop list](https://oldschool.runescape.wiki/w/Rooftop_Agility_Courses) corroborates
the nine course obstacle counts. Verify current requirements rather than assuming old ten-level
increments (the current wiki lists Draynor from level 1).

## Other courses and obstacle networks

All are in scope for investigation; none of the following is claimed course-complete by this list:

- Gnome Stronghold, Barbarian Outpost and Wilderness courses.
- Agility Pyramid (approach rocks are not the pyramid circuit).
- Ape Atoll, Werewolf, Penguin and Dorgesh-Kaan courses.
- Shayzien basic/advanced, Prifddinas and Colossal Wyrm routes/variants.
- Brimhaven Agility Arena, Hallowed Sepulchre and Rogues' Den: keep timing, instances, tools,
  reset/failure states and activity-specific mechanisms explicit. Static Open/Jump eligibility
  does not implement these protocols.
- Quest/dungeon networks: remaining Meiyerditch mechanisms, Tarn's traps, Isafdar traps and
  other obstacle chains. An unlocked crossing is distinct from quest or puzzle setup.
- Remaining ordinary and explicitly typed agility/grapple shortcuts, including duplicate resource
  variants with missing or conflicting skill/item requirements and equipment-free variants.

Use the [Agility training overview](https://oldschool.runescape.wiki/w/Agility_training) as one
discovery source, not a complete machine-readable catalogue. Reconcile additional/new activities
and route variants as discovered; the non-rooftop inventory is still open.

## Completed headless slices

- Meiyerditch/Ver Sinhaza: 66 rows are headless-owned: 18 floorboards, six walk-across floors, 20
  rubble/crawl-wall/rock/shelf/washing-line links, seven persistent prepared-floor climbs, five
  tunnel/barricade rows and ten post-quest access rows. The course/tunnel slice requires 26 Agility
  and partial Darkness of Hallowvale; prepared floors also require knockdown varbit 2589 equal to
  1. The final ten require the quest finished. Exact landing applies throughout, and agility-style
  ordinary rows honour the toggle. Quest-time preparation, discovery and quest solving remain
  incomplete even though no row in this audited corpus remains legacy-owned.

- Nature Grotto, Agility Pyramid entrance, Rellekka Hunter area and GWD/Wilderness: 28 loaded
  ordinary rows across four families now use strict catalogue ownership and exact landing. The
  enforced requirements are respectively 1 Agility; level-free/30 Agility for rock IDs
  11948/11949; 35 Agility; and 60 Agility plus partial Troll Stronghold. All honour the agility
  toggle. One GWD source row is an exact duplicate and the GWD traversal is a hazardous one-way
  descent; neither fact is hidden by the loaded-row count. The local generic shortcut inventory
  lists the Rellekka handholds at level 1, conflicting with the current wiki's level 35; the more
  conservative current requirement is retained pending a physical level-boundary check.
  Sources: [Nature Grotto bridge](https://oldschool.runescape.wiki/w/Bridge_(Nature_Grotto)),
  [Pyramid rocks](https://oldschool.runescape.wiki/w/Climbing_rocks_(Agility_Pyramid_entrance)),
  [Rellekka handholds](https://oldschool.runescape.wiki/w/Rocky_handholds_(Rellekka_Hunter_area)),
  and [GWD handholds](https://oldschool.runescape.wiki/w/Rocky_handholds_(God_Wars_Dungeon,_Wilderness)).

- Fremennik surface rope bridges: ten directed crossings; the two mine-shortcut rows now have
  40 Agility and feature-toggle filtering. Strict support identity and exact landing preserve
  crossing ownership. Five underground rows remain pending encounter/access review, including
  the Ice Troll King boundary; bridge repair is not implemented by walking.

- Isafdar: six log balances and eight tripwires now catalogue-owned, with exact landing and
  strict object identity. Logs require 45 Agility and partial Regicide and honour the agility
  toggle. Four leaf-pit rows remain pending climb-out recovery, not headless-complete.
  Sources: [logs](https://oldschool.runescape.wiki/w/Log_balance_(Tirannwn)),
  [tripwires](https://oldschool.runescape.wiki/w/Tripwire),
  [leaf pits](https://oldschool.runescape.wiki/w/Leaves_(trap)).

- Varrock garden trellis and Shaman Caves jagged wall: four exact one-tile crossings now use
  catalogue landing acknowledgement, retaining level 35/Garden of Tranquillity and level
  50/Legends' Quest completion respectively. Quest-in-progress jagged-wall use has not been
  added; the existing conservative completion gate is unchanged.
- Revenant pillars: removed eight obsolete ordinary rows in favour of the existing ten typed
  agility rows with 65/75/89 requirements and the agility toggle. Six obsolete rows were ungated;
  two also used a different object/diagonal endpoint from the canonical typed family. This is
  duplicate-data removal, not six new successful physical traversals. Freeze/bind handling and
  live endpoint checks remain open.

- Meiyerditch/Ver Sinhaza: 66 exact traversals; setup/unlock quest mechanisms remain incomplete.
- Tarn's Lair: 46 exact jumps (35 pillar and 11 ledge rows), with exact destination-tile
  acknowledgement and strict object identity. Neighbouring pillars, mid-jump tiles and lower-plane
  falls cannot acknowledge landing. Trap disarming, trap avoidance and whole-dungeon safety are
  **not implemented** by this slice. The [dungeon guide](https://oldschool.runescape.wiki/w/Tarn%27s_Lair)
  documents log traps knocking players down during pillar jumps.

## Next batches

1. Verify Draynor's seven directed transitions, then use its tested traversal/failed-landing
   contract to add the remaining eight rooftop courses in coherent batches.
2. Close the explicitly typed shortcut gaps and reconcile duplicate Revenant Cave rows.
3. Add other static course networks with exact requirements and entry/exit/fall routes.
4. Implement stateful/timed obstacle protocols; do not silently call these live-only deferrals.

Current post-Tarn legacy baseline: 1,365 total, 889 ordinary TRANSPORT. These counts do not include
new course routes absent from the transport resources and therefore are not an agility completion
percentage. Nothing in this scope expansion closes Phase 6 live gates or authorizes Phase 7 deletion.

Latest Meiyerditch/Ver Sinhaza baseline: 1,247 legacy / 775 ordinary TRANSPORT.

## Stale Champions' Guild stone chain

The eight level-31 stone rows (16533) still describe four intermediate stones. The
[object wiki](https://oldschool.runescape.wiki/w/Stepping_stone_(Champions%27_Guild)) records a
2026-04-01 change reducing the crossing to one stone. Do not migrate the old intermediate
endpoints or invent a new landing from them: verify the running-client object and both banks,
then replace the chain. These eight rows remain legacy and are not claimed repaired.

Other sources for this batch: [Varrock trellis](https://oldschool.runescape.wiki/w/Trellis_(Varrock)),
[jagged wall](https://oldschool.runescape.wiki/w/Jagged_wall), and
[Revenant pillars](https://oldschool.runescape.wiki/w/Pillar_(Revenant_Caves)).
