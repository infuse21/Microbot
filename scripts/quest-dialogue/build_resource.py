"""
Turn the scraped wiki corpus into the TSV resource the plugin ships.

Record types
  Q <quest> <text>              option that IS part of the quest
  N <quest> <text>              option the wiki flags quest=no (never pick during a quest)
  M <quest> <sorted-norm-set> <answer>   whole menu with exactly ONE quest option -> that answer

`/Historical` pages are dropped: they document how quests used to read and would actively
poison the corpus with dialogue that no longer exists.
"""

import json
import re
from collections import defaultdict
from pathlib import Path

OUT = Path(__file__).parent / "build"
REPO = Path(__file__).resolve().parents[2]
DEST = (REPO / "runelite-client/src/main/resources/net/runelite/client/plugins"
               "/microbot/questing/quest-dialogue.tsv")

# wiki title -> the name the plugin uses at runtime
ALIASES = {
    "Recipe for Disaster/Freeing the Goblin generals": "Recipe for Disaster - Wartface & Bentnoze",
}
RFD = re.compile(r"^Recipe for Disaster/Freeing (?:the )?(.*)$")

HEADER = """\
# Real OSRS quest dialogue options, scraped from the Old School RuneScape Wiki transcripts.
#
# Source:  https://oldschool.runescape.wiki/  (Category:Quest transcript, namespace 120)
# Licence: CC BY-NC-SA 3.0 - attribution, non-commercial, share-alike.
#          RuneScape and Old School RuneScape are trademarks of Jagex Limited.
#
# Regenerate with the scripts described in docs/quest-dialogue-corpus.md
#
# Records (tab separated):
#   Q<TAB>quest<TAB>option          option that IS part of the quest
#   N<TAB>quest<TAB>option          option the wiki marks as NOT quest related - never auto-pick
#   M<TAB>quest<TAB>key<TAB>answer  a menu with exactly one quest option; key is the normalised,
#                                   sorted option set joined by '|'
#
# '/Historical' transcript pages are deliberately excluded - they describe superseded dialogue.
"""


def norm(s):
    s = re.sub(r"<[^>]*>", " ", s).lower()
    s = re.sub(r"[^a-z0-9 ]", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def runtime_name(wiki_title):
    if wiki_title in ALIASES:
        return ALIASES[wiki_title]
    m = RFD.match(wiki_title)
    if m:
        return "Recipe for Disaster - " + m.group(1)
    return wiki_title


def main():
    menus = json.loads((OUT / "menus.json").read_text(encoding="utf-8"))
    menus = [m for m in menus if not m["quest"].endswith("/Historical")]

    quest_opts = defaultdict(dict)     # quest -> norm text -> (raw, is_quest)
    menu_rows = []

    for m in menus:
        quest = runtime_name(m["quest"])
        opts = [o for o in m["options"] if norm(o["text"])]
        if not opts:
            continue

        for o in opts:
            n = norm(o["text"])
            prev = quest_opts[quest].get(n)
            # if an option is quest-related anywhere in the quest, never blacklist it
            is_q = o["quest_related"] or (prev[1] if prev else False)
            quest_opts[quest][n] = (o["text"], is_q)

        if len(opts) < 2:
            continue
        qrel = [o for o in opts if o["quest_related"]]
        if len(qrel) != 1:
            continue                    # 0 = nothing to do, >1 = a real branch, don't decide it
        key = "|".join(sorted({norm(o["text"]) for o in opts}))
        menu_rows.append((quest, key, qrel[0]["text"]))

    seen, uniq_menus = set(), []
    for row in menu_rows:
        if (row[0], row[1]) in seen:
            continue
        seen.add((row[0], row[1]))
        uniq_menus.append(row)

    lines = [HEADER.rstrip()]
    nq = nn = 0
    for quest in sorted(quest_opts):
        for n in sorted(quest_opts[quest]):
            raw, is_q = quest_opts[quest][n]
            raw = raw.replace("\t", " ").strip()
            if not raw:
                continue
            lines.append(f"{'Q' if is_q else 'N'}\t{quest}\t{raw}")
            nq += is_q
            nn += not is_q
    for quest, key, answer in sorted(uniq_menus):
        lines.append(f"M\t{quest}\t{key}\t{answer.replace(chr(9), ' ').strip()}")

    DEST.parent.mkdir(parents=True, exist_ok=True)
    DEST.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(json.dumps({
        "quests": len(quest_opts),
        "Q_records": nq,
        "N_records": nn,
        "M_records": len(uniq_menus),
        "bytes": DEST.stat().st_size,
        "path": str(DEST),
    }, indent=1))


if __name__ == "__main__":
    main()
