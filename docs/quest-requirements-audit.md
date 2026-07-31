# Auditing quest requirements against Jagex's own data

The game cache ships **Jagex's quest database** — `DBTableID.Quest`, 214 rows, updated with every
game update. The quest helper hardcodes the same information in Java. This tooling diffs the two.

## Why it's worth having

Requirements in the helper are hand-maintained, so they can silently fall out of step when Jagex
retunes a quest — and a wrong requirement either blocks a player who could start, or lets one start
who can't finish. The cache version cannot drift, because it *is* what the game enforces.

The audit is not a one-off cleanup. Run it after a game update, and especially when a new quest lands.

## What Jagex's table holds

| Column | Contents |
|---|---|
| `STARTCOORD` / `STARTNPC` / `STARTLOC` | where and who starts the quest — 213 of 214 populated |
| `REQUIREMENT_STATS` | `[skillId, level, skillId, level, …]` — 164 populated |
| `REQUIREMENT_QUESTS` | prerequisite quests, as row ids |
| `REQUIREMENT_QUESTPOINTS` / `REQUIREMENT_COMBAT` | plus `RECOMMENDED_` variants |
| `UNSTARTEDSTATE` / `ENDSTATE` | quest state boundaries |
| `QUESTPOINTS`, `MEMBERS`, `DIFFICULTY`, `LENGTH`, `SERIES`, `PARENT_QUEST` | journal metadata |

Only `REQUIREMENT_*` is used today. The start coordinates and quest-state boundaries are unexploited
and are the obvious next thing to reach for.

## Running it

```bash
./gradlew :client:auditQuestRequirements
```

It reads `~/.runelite/jagexcache/oldschool/LIVE`, which RuneLite keeps current. Point it elsewhere
with `-PcachePath=<dir>`.

The tool is [QuestRequirementAudit.java](../runelite-client/src/questAudit/java/net/runelite/client/plugins/microbot/questhelper/tools/QuestRequirementAudit.java),
in its own `questAudit` source set so its `net.runelite:cache` dependency never reaches the client's
compile or test classpath. Nothing else depends on it and it is not part of a normal build.

At runtime the same table is reachable without any of this, via `Microbot.getDBTableRows()` and
`Microbot.getDBTableField()` — `Rs2Slayer` already uses them for the slayer task table.

## Reading the output — the important caveat

**Jagex's table records only what the game gates *entry* on.** The quest helper legitimately lists
more: skills you need to *finish*, and transitive prerequisites. So the two directions of
disagreement mean very different things.

| Direction | Meaning |
|---|---|
| `only_in_jagex` | the helper is **missing** something the game enforces — investigate |
| `only_in_helper` | usually deliberate and correct — verify before "fixing" |

A worked example of why this matters: the audit flagged Forgettable Tale as requiring Cooking 22
where Jagex says 20. That is **not** a bug — the in-game gate really is 20, but 22 is needed to make
the kelda stout mid-quest, so the helper is right and Jagex's entry gate is the incomplete view.
Lowering it would have let players start a quest they could not finish. Always check the wiki's quest
page before changing a level.

## Baseline (2026-07-31)

180 quests matched by name, **158 with identical requirements**. Of the 22 that differ:

- **3** are the Shield of Arrav gang split — the helper models `Shield of Arrav - Black Arm Gang`
  separately, Jagex has one row. Not drift.
- **~18** are the helper deliberately listing more (`X Marks the Spot` across six Kourend quests,
  completion-only skills for Throne of Miscellania, Fremennik Isles, and others).
- **1** is Forgettable Tale, described above — correct as-is.

Two real problems were found and fixed on this baseline:

- **Watchtower** required Magic 15; the game requires 14. Confirmed against both the cache and the
  wiki. Fixed.
- **Rag and Bone Man II** listed neither Horror from the Deep nor The Fremennik Trials, though the
  game gates dagannoth access behind *either*. Added as a `Conditions(LogicType.OR, …)`, matching the
  file's existing pattern for partial-completion requirements. It now shows up as
  `prereq_only_in_helper: The Fremennik Trials`, because a flat table cannot express an OR.

16 helper quests have no Jagex row at all (RFD sub-quests, Balloon Transport, gang variants). Those
are modelling differences, not gaps.

## Known limits

- The audit parses Java with regexes. It resolves requirements declared inline *and* as fields
  returned from `getGeneralRequirements()`, but it does not evaluate arbitrary expressions — an
  unexpected declaration style shows up as a false "missing" requirement. Read the source before
  trusting a finding. (Fairytale II and Prying Times were both false positives for exactly this
  reason before field resolution was added.)
- Skill ids follow RuneScript order; Sailing is id 23. Verify the mapping against a known quest if a
  new skill is ever added.
- Quests are matched by display name, normalised to letters and digits.

## See also

- `docs/quest-dialogue-corpus.md` — the same idea for dialogue, where the data has to come from the
  wiki because dialogue is server-side and genuinely absent from the cache.
