# Plan: extract the questing executor from the vendored Quest Helper

Status: **proposal — nothing moved yet.**

## Why

The Quest Helper plugin is vendored upstream content: 704 Java files, ~270 quests, maintained by
a community that updates it when Jagex changes the game. That data is the expensive asset and it is
mostly *correct* — the failures fixed during the July 27–28 session were overwhelmingly in our
automation layer, not in the quest definitions.

Our automation currently lives *inside* that vendored package, which is what makes upstream syncing
painful. Separating them makes an upstream update a data refresh instead of a merge conflict.

## Current state (measured)

| Component | Files | Size | Owner |
|---|---|---|---|
| `questhelper/**` (quest data, steps, requirements, panel, managers) | ~700 | — | upstream |
| `questhelper/QuestScript.java` | 1 | 2,829 lines | ours |
| `questhelper/QuestShopCatalog.java`, `LearnedDialogue.java` | 2 | 325 lines | ours |
| `questhelper/logic/**` (per-quest custom logic + registry) | 8 | — | ours |
| `questhelper/QuestHelperPlugin.java` | 1 | modified | upstream + our edits |

**The seam is narrow.** Only one vendored file references our code (`QuestHelperPlugin`, where we
inject and start the script, and forward chat messages). Our executor consumes ~10 model types from
the vendored side, all read-only:

```
QuestHelper, QuestStep (+ subclasses), DetailedQuestStep, NpcStep, ObjectStep, DigStep,
ItemRequirement, Requirement, PanelDetails, QuestContainerManager, QuestHelperQuest
```

That is a clean dependency direction: **executor → model**. Nothing in the model needs to know the
executor exists.

## Target structure

```
plugins/microbot/questing/                 <- ours, freely refactorable
    QuestingPlugin.java                    (owns lifecycle; replaces our edits to QuestHelperPlugin)
    QuestingScript.java                    (the tick loop, slimmed)
    verbs/                                 (walk, interactNpc, interactObject, useItemOn, equip, loot, dialogue)
    strategy/                              (per-step-type handlers: NpcStepStrategy, ObjectStepStrategy, ...)
    acquisition/                           (bank -> shop -> GE chain, QuestShopCatalog)
    learning/                              (LearnedDialogue, future LearnedObjects/LearnedActions)
    quests/                                (per-quest custom logic + registry, today's logic/ package)
    diagnostics/                           (heartbeat + phase markers)

plugins/microbot/questhelper/**            <- vendored, treated as read-only
```

## What moves, what stays

**Moves** (all ours, no upstream conflict):
`QuestScript`, `QuestShopCatalog`, `LearnedDialogue`, `logic/**`.

**Stays** (upstream, untouched): everything else.

**Reduced to near-zero**: our edits to `QuestHelperPlugin`. Today we inject `QuestScript` and forward
`onChatMessage`. After extraction, `QuestingPlugin` owns its own lifecycle and subscribes to the event
bus itself, so the only thing it needs from the Quest Helper plugin is a reference to the selected
quest — obtainable via the plugin manager exactly as `QuestScript.getQuestHelperPlugin()` already does.
Target: **zero diffs** against upstream `QuestHelperPlugin`.

## Migration in safe increments

Each step compiles, runs, and is independently revertible. No step requires a rewrite of behaviour.

1. **Move files, no logic changes.** New package, fix imports, delete our edits from
   `QuestHelperPlugin` and re-home them in `QuestingPlugin`. Verify: quest still runs end to end.
2. **Introduce the verb layer.** Extract the interaction primitives currently inlined in
   `applyNpcStep` / `applyObjectStep` / `applyDetailedQuestStep` into named verbs with explicit
   pre/post conditions and bounded waits. Behaviour-preserving; the fixes from the session become
   properties of a verb rather than special cases in a 2,800-line method.
3. **Split step handling into strategies.** Replace the `if (step instanceof ...)` chain with a
   registry of per-step-type handlers. Makes new step types additive rather than invasive.
4. **Regression harness.** Headless scenario tests per step type, mirroring the existing walker test
   pattern (`Rs2WalkerUnitTest`, `ShortestPathCoreTest`). This is the piece that stops the session's
   fixes from silently regressing.
5. **Extend learning.** `LearnedObjects` (which candidate object a step means — the monument case) and
   `LearnedActions` (which menu action works — "Shout-to", "Walk-past", "Take-from"), reusing the
   dialogue learner's confirm/negative machinery.

## Upstream sync story

After step 1:

- Pull upstream Quest Helper into `questhelper/**` wholesale (no cherry-picking around our code).
- Conflicts limited to files we deliberately patched for data drift (e.g. Priest in Peril dialogue
  texts). Those should shrink over time as the learning layer absorbs drift instead of us editing data.
- Our executor is unaffected by upstream churn unless the *model* changes shape (rare, and a compile
  error rather than a silent break).

Recommended discipline: **never edit vendored quest data to fix an executor bug.** If a quest needs a
per-quest workaround, it goes in `questing/quests/` (as Eagles' Peak and Priest in Peril already do).

The inverse also holds, and matters more than it sounds: **a genuine gap in the quest data is fixed in
the quest data.** Read literally, the rule above pushes every failure into `questing/quests/`, and a
missing step there becomes a hack that papers over a model the executor still believes.

The distinguishing question: *would a human following the sidebar, and nothing else, get stuck here?*
If yes, it is a data gap — add the step. If a human sails through and only automation trips, it is
ours — fix the executor.

**Apply that test honestly — Pirate's Treasure is the cautionary example, not a supporting one.** The
back room of the Port Sarim food shop is locked until Wydin hires you, and the quest has no step for
being hired, so it looked like a textbook data gap and a `talkToWydin` step was added on that basis.
It isn't one. A human walking at the crate reaches the door, tries it, and Wydin starts the
conversation himself — upstream's model works fine, because a human approaches through the shop.

The executor didn't, and that is the actual defect: it asked the walker to get *within N tiles of the
crate's tile* and was satisfied at (3008, 3207) — one tile away, on the far side of an exterior wall.
Distance is a proxy for interactability and the proxy fails through walls. Collision was right the
whole time; it correctly reported the inside tile as unreachable.

The `talkToWydin` step is kept because it is harmless and makes the prerequisite explicit rather than
relying on an incidental door trigger. But it fixed one quest, and the goal-specification bug beneath
it affects every step that interacts with anything.

### Marking our edits

Every Microbot change inside `questhelper/**` carries a `MICROBOT` comment, so the change list is:

```
grep -rn "MICROBOT" runelite-client/src/main/java/net/runelite/client/plugins/microbot/questhelper/
```

Diffing against the sync commit (`git diff 6c78e87328..HEAD -- questhelper/`) finds post-sync edits
only. It misses the integration that predates the sync and is baked into that commit — the injector
hook in `QuestHelperPlugin`, the Microbot config section — which is why the marker is the source of
truth. Tag new edits as you make them; the point is that a future pull is a checklist, not a dig.

## Risks and non-goals

**Risks**
- *Move breaks injection.* Guice wiring and `@Subscribe` registration must be re-established in
  `QuestingPlugin`. Mitigated by doing the move as its own commit and verifying a full quest run.
- *Hidden coupling.* `QuestContainerManager` (bank/inventory snapshots) is upstream state our
  acquisition depends on. It stays where it is; we keep reading it.
- *Config split.* `QuestHelperConfig` holds our Microbot options (`startStopQuestHelper`,
  `obtainMissingItems`, `buyFromShops`). Either keep reading that config, or migrate to a
  `QuestingConfig` and accept a one-time settings reset. Recommend: keep reading it initially.

**Non-goals**
- Rewriting quest data. It is the asset, not the problem.
- Machine learning. There is no training signal and non-determinism is a liability in a game with
  irreversible choices. Deterministic rules plus persistent, self-invalidating learned overlays are
  strictly better here.
- Merging everything into one plugin. That would make us a permanent fork that cannot cheaply resync.

## Effort

| Step | Estimate |
|---|---|
| 1. Move + re-home lifecycle | half a day |
| 2. Verb layer | 1 day |
| 3. Step strategies | 1 day |
| 4. Regression harness | 1–2 days |
| 5. Learned objects/actions | 1 day |

Steps 1–2 deliver most of the value (syncable upstream, refactorable executor). Steps 3–5 are
incremental and can be scheduled independently.
