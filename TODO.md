# AIO Hunting TODO

Use this checklist for Hunter activities that could not be completed safely from
the existing RuneLite and Microbot-Hub data.

## Runtime capture commands

Run these before and after every relevant interaction:

```powershell
.\microbot-cli state
.\microbot-cli npcs --distance 30
.\microbot-cli objects --distance 30
```

For an important object, also record its surrounding walkable tiles:

```powershell
curl "http://127.0.0.1:8081/objects/neighbours?id=<OBJECT_ID>&maxDistance=30&limit=200"
```

Please record:

- Player coordinate and plane.
- NPC/object ID, name, coordinate and available actions.
- IDs before, during and after an interaction.
- Any changed varbit or varp.
- Required inventory/equipment and dialogue choices.
- A successful route's important approach, interaction and exit tiles.

## Tracking creatures

- [x] Add Polar kebbit, Common kebbit, Feldip weasel, Desert devil and
  Razor-backed kebbit from RuneLite's generated trail data.
- [x] Add noose-wand banking, equipping and loot handling.
- [x] Verify the Polar kebbit platform, two starting holes, intermediate
  objects and possible snow-drift endpoints through the live Agent Server.
- [x] Confirm that actions are not enough to select the route: every Polar
  clue exposes `Inspect` and every snow drift exposes `Search`/`Attack`.
- [x] Select active clues from RuneLite's numbered trail varbits instead of
  guessing the nearest actionable scenery object.
- [x] Live-test complete catches for all five tracking creatures, including
  endpoint selection.
- [x] Test the ring of pursuit shortcut. - not needed


## Pitfall trapping

- [ ] Add the shared pitfall state machine: tease creature, prepare pit,
  jump obstacle, wait for capture/failure, collect and rebuild.
- [ ] Port and live-test the existing Sunlight antelope Hub implementation.
- [ ] Add and live-test spined larupia, horned graahk and sabre-toothed kyatt.
- [ ] Add and live-test moonlight antelope after the shared logic is stable.
- [ ] Target NPC ID, name, available actions and hunting area.
- [ ] Empty, preparing, ready, failed and caught pit object IDs/actions.
- [ ] Exact pit footprint, preparation tile, lure tile and jump tile.
- [ ] Knife, logs, teasing stick and any creature-specific requirements.
- [ ] Loot, meat-pouch and banking behaviour.

## Deadfall trapping

- [ ] Live-test the existing shared deadfall logic before enabling any method
  for automatic progression.
- [ ] Verify empty, setting, caught and failed object IDs for wild kebbit,
  barb-tailed kebbit, prickly kebbit, sabre-toothed kebbit and pyre fox.
- [ ] Verify the correct boulder/tree anchor and safe standing tile for every
  location.
- [ ] Verify knife, axe, logs, bait, loot and bank-return behaviour.
- [ ] Add any Varlamore deadfall creature variants found during live testing.

## Maniacal monkeys

- [ ] Entrance and exit routes.
- [ ] Required greegree and equipped-state details.
- [ ] Monkey NPC IDs and interaction actions.
- [ ] Boulder coordinates plus unset, setting, ready and caught object IDs.
- [ ] Banana/bait item IDs and the exact bait interaction.
- [ ] Successful capture and reset sequence.

## Hunter rumours

- [ ] Hunter master NPC IDs and locations.
- [ ] Assignment and completion dialogue choices.
- [ ] Assignment varbit/varp, widget text or chat messages.
- [ ] Every rumour target and proof item ID.
- [ ] Task-blocking and replacement dialogue.
- [ ] Banking, resupply and return-to-master routes.

## Implings

- [x] Puro-Puro Baby through Lucky implings.
- [x] Confirm the `2592,4320` Puro-Puro anchor covers the supplied Baby
  impling spawn sample.
- [ ] Crystal impling NPC variants.
- [ ] Preferred Prifddinas hunting area and access route.
- [ ] Decide whether roaming overworld implings should be supported.

## Supplied location follow-up

- [x] Replace rough anchors for birds, tracking, butterflies, box traps,
  salamanders and Herbiboar with the supplied dense spawn locations.
- [x] Add manual Sunlight moth, Moonlight moth and Embertailed jerboa methods.
- [ ] Live-test Embertailed jerboa caught/failed box-trap object states.
- [ ] Verify the automatic route into and out of the Hunter Guild basement
  before treating Moonlight moth as route-safe.
- [x] Store wide spawn groups as curated sub-locations.
- [ ] Live-test roaming-target cluster rotation (10 seconds empty, minimum
  20 seconds in a cluster).
- [ ] Live-test stalled placed-trap relocation (2 minutes without progress);
  every owned trap must be dismantled and recovered before walking.
- [ ] Live-test portable-trap action confirmation and bounded retries,
  including the inventory-to-object cache transition and dropped-trap pickup.

## Mixed Fishing/Hunter activities

- [ ] Decide whether Drift net fishing belongs in AIO Hunting.
- [ ] Capture Drift net setup, interaction states, banking and resupply.
- [ ] Decide whether Aerial fishing belongs in AIO Hunting.
- [ ] Capture bird interaction, bait, knife and fish-processing states.

## Validation

- [ ] Test pause during every walking phase; walking must cancel safely.
- [ ] Test stopping during an interaction and while banking.
- [ ] Test automatic banking and automatic return to the hunting area.
- [ ] Test missing supplies without stopping or freezing other scripts.
- [ ] Test each activity after logout, world hop and plugin restart.
- [x] Run `.\gradlew.bat :client:compileJava`.
- [x] Run `.\gradlew.bat :client:checkstyleMain`.
