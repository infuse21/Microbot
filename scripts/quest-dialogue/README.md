# Quest dialogue corpus tooling

**Optional maintenance tooling. Not part of the build.** Nothing in Gradle or CI references these
scripts, and you never need Python to build or run the client — the generated artifact
(`quest-dialogue.tsv`) is committed. You only need these to regenerate it after a game update
rewords dialogue.

Requires Python 3 (standard library only, no packages to install).

## Why this is Python and the requirement audit is not

The sibling quest-requirements audit reads the game cache, so it is Java and lives behind
`./gradlew :client:auditQuestRequirements`. This one is HTTP plus wikitext regex parsing against the
OSRS Wiki — Java would be substantially more code for a script that runs a few times a year.

## Regenerating

```bash
python scripts/quest-dialogue/extract_dialogue.py && python scripts/quest-dialogue/build_resource.py
```

`extract_dialogue.py` scrapes `Category:Quest transcript` via the MediaWiki API (~200 pages, 50 per
request, about a minute) into `build/menus.json`. `build_resource.py` turns that into
`runelite-client/src/main/resources/net/runelite/client/plugins/microbot/questing/quest-dialogue.tsv`.

Then confirm nothing regressed:

```bash
./gradlew :client:runUnitTests --tests "net.runelite.client.plugins.microbot.questing.*"
```

## The other scripts

| Script | Purpose |
|---|---|
| `guesser_exposure.py` | How many real menus the quest data can answer vs. how many force a guess |
| `audit_declared.py` | Every `addDialogStep` string that no longer matches a real option, with the closest match |
| `detect_changes.py` | Diffs transcripts against an earlier date to find real dialogue rewordings |

Intermediates land in a gitignored `build/`.

Full background, data format, and the licence note: [docs/quest-dialogue-corpus.md](../../docs/quest-dialogue-corpus.md).
