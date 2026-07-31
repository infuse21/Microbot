"""
Measure GUESSER EXPOSURE: real in-game option menus for which the quest helper
declares no answer at all. Those are the menus where the bot falls through to
"pick the first non-dangerous option" -- the Wydin failure mode.

Also reports how many of those menus contain an option the wiki explicitly flags
as NOT quest-related (quest=no), i.e. a trap the guesser can walk into.
"""

import json
import re
from collections import defaultdict
from pathlib import Path

from audit_declared import extract_declared, norm

OUT = Path(__file__).parent / "build"


def folder_key(name):
    return re.sub(r"[^a-z0-9]", "", name.lower())


def main():
    menus = json.loads((OUT / "menus.json").read_text(encoding="utf-8"))
    declared = {folder_key(k): {norm(s) for s in v} for k, v in extract_declared().items()}

    by_quest = defaultdict(list)
    for m in menus:
        by_quest[folder_key(m["quest"])].append(m)

    matched_folders = set(declared) & set(by_quest)

    covered = uncovered = 0
    trap_menus = []
    per_quest = defaultdict(lambda: [0, 0])

    for q in sorted(matched_folders):
        vocab = declared[q]
        for m in by_quest[q]:
            opts = m["options"]
            if len(opts) < 2:
                continue
            if not any(o["quest_related"] for o in opts):
                continue          # pure flavour menu, bot shouldn't be there anyway

            hit = False
            for o in opts:
                n = norm(o["text"])
                if n in vocab or any(n in v or v in n for v in vocab if len(v) > 6):
                    hit = True
                    break
            if hit:
                covered += 1
                per_quest[q][0] += 1
            else:
                uncovered += 1
                per_quest[q][1] += 1
                traps = [o["text"] for o in opts if not o["quest_related"]]
                if traps:
                    trap_menus.append({
                        "quest": m["quest"], "section": m["section"],
                        "options": [o["text"] for o in opts],
                        "would_pick": opts[0]["text"],
                        "is_trap": not opts[0]["quest_related"],
                    })

    first_pick_wrong = [t for t in trap_menus if t["is_trap"]]
    worst = sorted(per_quest.items(), key=lambda kv: -kv[1][1])[:12]

    (OUT / "guesser_exposure.json").write_text(json.dumps({
        "uncovered_menus_with_trap_options": trap_menus,
    }, indent=1, ensure_ascii=False), encoding="utf-8")

    print(json.dumps({
        "quest_folders_matched_to_a_wiki_page": len(matched_folders),
        "multi_option_quest_menus": covered + uncovered,
        "menus_the_quest_data_can_answer": covered,
        "menus_with_NO_declared_answer": uncovered,
        "pct_forced_to_guess": round(100.0 * uncovered / (covered + uncovered), 1),
        "uncovered_menus_containing_a_non_quest_trap": len(trap_menus),
        "uncovered_where_first_option_IS_the_trap": len(first_pick_wrong),
    }, indent=1))

    print("\n--- worst-covered quests (covered / uncovered) ---")
    for q, (c, u) in worst:
        print(f"  {q:32s} {c:4d} / {u:4d}")

    print("\n--- menus where the guesser's first pick is a flagged non-quest option ---")
    for t in first_pick_wrong[:12]:
        print(f"[{t['quest']}] {t['section']}")
        print(f"   would pick: {t['would_pick']}")
        print(f"   options   : {' | '.join(t['options'])}")


if __name__ == "__main__":
    main()
