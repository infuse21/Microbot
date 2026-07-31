"""
Detect REAL changes to OSRS quest dialogue options by diffing the wiki's own history.

For every quest transcript we fetch the revision as it stood on a past date and compare
the extracted option-text set against today's. Options that appear/disappear are the
public record of dialogue drift -- the thing the bot currently has to guess around.

Usage: python detect_changes.py [YYYY-MM-DD]   (default: 2 years back)
"""

import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from extract_dialogue import api, list_quest_transcripts, parse_transcript, UA  # noqa: E402

OUT = Path(__file__).parent / "build"
CUTOFF = (sys.argv[1] if len(sys.argv) > 1 else "2024-07-31") + "T00:00:00Z"


def revision_at(title, ts):
    """Page content as of `ts`, plus that revision's timestamp. None if page didn't exist."""
    d = api({"action": "query", "prop": "revisions", "titles": title,
             "rvprop": "content|timestamp|ids", "rvslots": "main",
             "rvstart": ts, "rvdir": "older", "rvlimit": "1"})
    try:
        p = d["query"]["pages"][0]
        r = p["revisions"][0]
        return r["slots"]["main"]["content"], r["timestamp"]
    except (KeyError, IndexError):
        return None, None


def option_set(title, wikitext):
    """Flat set of option texts, plus a map text -> quest_related."""
    texts, flags = set(), {}
    for m in parse_transcript(title, wikitext):
        for o in m["options"]:
            texts.add(o["text"])
            flags[o["text"]] = o["quest_related"]
    return texts, flags


def norm(s):
    return re.sub(r"[^a-z0-9 ]", " ", s.lower())


def collapse(s):
    return re.sub(r"\s+", " ", norm(s)).strip()


def main():
    titles = list_quest_transcripts()
    print(f"comparing {len(titles)} transcripts against {CUTOFF}", file=sys.stderr)

    now_pages = json.loads((OUT / "menus.json").read_text(encoding="utf-8"))
    now_by_quest = {}
    for m in now_pages:
        now_by_quest.setdefault(m["quest"], set()).update(o["text"] for o in m["options"])

    report, no_history = [], []
    for i, title in enumerate(titles, 1):
        quest = title.split(":", 1)[1]
        old_text, old_ts = revision_at(title, CUTOFF)
        if old_text is None:
            no_history.append(quest)
            continue
        old, _ = option_set(title, old_text)
        new = now_by_quest.get(quest, set())
        if not old and not new:
            continue

        added, removed = new - old, old - new
        if not added and not removed:
            continue

        # pair up near-identical strings: that is a REWORD, the case that breaks matching
        rewords, add_l, rem_l = [], list(added), list(removed)
        for a in list(add_l):
            for r in list(rem_l):
                ca, cr = collapse(a), collapse(r)
                if ca == cr:
                    continue
                if ca in cr or cr in ca or (
                        len(set(ca.split()) & set(cr.split())) >= max(2, min(len(ca.split()), len(cr.split())) - 1)):
                    rewords.append({"old": r, "new": a})
                    add_l.remove(a); rem_l.remove(r)
                    break

        report.append({
            "quest": quest, "old_revision": old_ts,
            "rewordings": rewords,
            "added_only": sorted(add_l), "removed_only": sorted(rem_l),
            "counts": {"added": len(added), "removed": len(removed), "reworded": len(rewords)},
        })
        if i % 25 == 0:
            print(f"  {i}/{len(titles)}", file=sys.stderr)
        time.sleep(0.3)

    report.sort(key=lambda r: -r["counts"]["reworded"])
    (OUT / "dialogue_changes.json").write_text(
        json.dumps({"cutoff": CUTOFF, "quests": report, "no_history": no_history},
                   indent=1, ensure_ascii=False), encoding="utf-8")

    tot_rw = sum(r["counts"]["reworded"] for r in report)
    print(json.dumps({
        "cutoff": CUTOFF,
        "quests_compared": len(titles) - len(no_history),
        "quests_with_any_option_change": len(report),
        "total_rewordings_detected": tot_rw,
        "total_options_added": sum(r["counts"]["added"] for r in report),
        "total_options_removed": sum(r["counts"]["removed"] for r in report),
        "top_reworded_quests": [r["quest"] for r in report[:12]],
    }, indent=1))


if __name__ == "__main__":
    main()
