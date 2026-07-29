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

Pirate's Treasure is the worked example. The back room of the Port Sarim food shop is locked until
Wydin hires you, and the quest had no step for being hired — upstream folded the answer into the crate
step's dialogue and left a human to infer the rest. Two attempts to patch it in `questing/quests/`
failed, because the executor was being asked to reach a crate the step tree insisted was already
reachable. Adding `talkToWydin` as a real step fixed it in one go, and is upstreamable besides.

The distinguishing question: *would a human following the sidebar, and nothing else, get stuck here?*
If yes, it is a data gap — add the step. If a human sails through and only automation trips, it is
ours — fix the executor.

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
