# Quest dialogue corpus

Real OSRS quest dialogue options, scraped from the Old School RuneScape Wiki, shipped so the questing
plugin stops guessing at dialogue menus.

## Why

`QuestingScript#handleUnmatchedDialogueOptions` answers a menu by looking for an option the quest data
declares. When nothing matches it used to fall through to *"click the first option that doesn't look
dangerous"*. On a shopkeeper the first option is usually the one that opens the shop — which is
exactly what happened to Wydin in Pirate's Treasure: the bot talked to him, matched nothing, opened
the shop, and cycled.

Measured against the real corpus (see [Re-measuring](#re-measuring)):

| | |
|---|---|
| Declared strings that no longer match any real option | **56 / 2128 (2.6%)** |
| Real multi-option quest menus with **no** declared answer | **1257 / 3610 (34.8%)** |
| …of those, menus whose **first** option is flagged not-quest-related | **142** |

So dialogue *drift* was never the main problem — **coverage** was. Roughly one menu in three was a
guess, and the guesser's fallback was structurally biased toward the wrong answer.

The Wydin case turned out not to be drift at all. Pirate's Treasure has two different menus: talking
to him offers `Can I get a job here?`, while being blocked at the back door offers
`Well, can I get a job here?`. Both are live game text. The quest data declared the door wording.

## The data source

OSRS dialogue is server-side RuneScript, so it is **not in the client cache** — the wiki is the only
bulk source. Its [transcript style guide](https://oldschool.runescape.wiki/w/RuneScape:Style_guide/Transcripts)
mandates parseable templates, so this is structured data rather than scraped prose:

| Template | Meaning |
|---|---|
| `{{tselect\|…}}` | starts an option menu |
| `{{topt\|…}}` | one selectable option |
| `{{topt\|quest=no\|…}}` | option that is **not** part of the quest (shop, flavour, small talk) |
| `{{topt\|cond=If …\|…}}` | option only present under a condition |

That `quest=no` flag is the valuable half: it is a ready-made list of options the bot must never pick
while questing.

## The shipped resource

`runelite-client/src/main/resources/net/runelite/client/plugins/microbot/questing/quest-dialogue.tsv`
— ~440 KB, 190 quests. Tab separated, human readable, three record types:

```
Q	<quest>	<option>              option that IS part of the quest
N	<quest>	<option>              flagged not quest related — never auto-picked
M	<quest>	<key>	<answer>      a menu with exactly ONE quest option, mapped to that answer
```

`key` is the menu's normalised, de-duplicated, sorted option set joined by `|`, so option order and
colour tags don't matter.

**Menus with two or more quest-related options are deliberately omitted.** Those are real branches —
which gang, which reward — and must never be auto-answered. This is why the corpus is safe to consult
even in quests where a wrong choice is permanent: a gang-choice menu simply has no `M` record.

Counts: 6421 `Q`, 877 `N`, 566 `M`.

## How it is consumed

`QuestDialogueCorpus` is a hint source, never an authority. In `handleUnmatchedDialogueOptions` the
order is:

1. A **learned** answer (one that demonstrably advanced the quest before).
2. The quest's own declared answer, when exactly one option matches.
3. **This corpus**, when the exact menu is documented with a single quest option.
4. The quest's declared answer when several matched.
5. An informed guess: an option the corpus marks as belonging to this quest, never one it marks `N`.

If everything is either known-wrong or documented as unrelated, it now **stops and says so** rather
than clicking blindly.

It never bypasses `LearnedDialogue.isDangerousOption`, and quests in `GUESS_BLOCKED_QUEST_IDS` still
refuse to act on an unrecognised menu exactly as before.

## Regenerating

After a game update reworded dialogue, or to pick up wiki fixes:

```bash
python scripts/quest-dialogue/extract_dialogue.py && python scripts/quest-dialogue/build_resource.py
```

The first call scrapes `Category:Quest transcript` via the MediaWiki API (~200 pages, 50 per request,
about a minute) into `scripts/quest-dialogue/build/menus.json`. The second turns it into the TSV.
Then run the tests:

```bash
./gradlew :client:runUnitTests --tests "net.runelite.client.plugins.microbot.questing.*"
```

`/Historical` transcript pages are excluded automatically — they document how quests *used* to read
and would poison the corpus.

## Re-measuring

```bash
python scripts/quest-dialogue/guesser_exposure.py
```

Reports how many real menus the quest data can answer versus how many force a guess, and lists menus
where the old first-option fallback would have hit a flagged non-quest option.

```bash
python scripts/quest-dialogue/audit_declared.py
```

Cross-checks every `addDialogStep` string in the vendored helper against the corpus and reports the
closest real option for anything that no longer matches — the drift list.

## Tracking dialogue changes over time

Wiki revision history is the only systematic before/after record of dialogue changes. Jagex patch
notes rarely enumerate strings, and the wiki's *Diversity & Inclusion updates* page records *that*
text changed, seldom the old wording.

```bash
python scripts/quest-dialogue/detect_changes.py 2024-07-31
```

Fetches each transcript as it stood on that date and diffs the option sets. Over 2024-07-31 →
2026-07-31: 140 quests comparable, 60 with option changes, 32 rewordings. Real game changes cluster
visibly — the Ironman degendering pass, `Ok` → `Okay`, and a batch of quests where long decline
options collapsed to a plain `No.`. That last cluster hit at least seven quests in one update, which
is why fixing dialogue quest-by-quest is whack-a-mole.

For ongoing monitoring, `list=recentchanges&rcnamespace=120` is a live feed of transcript edits.

## Licence

Wiki text is **CC BY-NC-SA 3.0** — attribution, non-commercial, share-alike. The attribution header is
in the TSV itself; keep it. RuneScape and Old School RuneScape are trademarks of Jagex Limited.
