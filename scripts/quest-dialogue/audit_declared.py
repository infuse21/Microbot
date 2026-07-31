"""
Cross-check every dialogue string the vendored quest helper DECLARES against the
real option corpus scraped from the wiki. Anything declared but absent from the
corpus is a candidate stale/misworded answer -- the exact condition that drops the
bot into its guesser.
"""

import difflib
import json
import re
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
ROOT = (REPO / "runelite-client/src/main/java/net/runelite/client/plugins"
               "/microbot/questhelper/helpers/quests")
OUT = Path(__file__).parent / "build"

CALL = re.compile(r"addDialogStep(?:s)?\s*\(", re.S)
STRING = re.compile(r'"((?:[^"\\]|\\.)*)"')


def norm(s):
    s = re.sub(r"<[^>]*>", " ", s)
    s = re.sub(r"[^a-z0-9 ]", " ", s.lower())
    return re.sub(r"\s+", " ", s).strip()


def extract_declared():
    """Pull the string literals out of every addDialogStep/addDialogSteps call."""
    out = defaultdict(set)
    for f in ROOT.rglob("*.java"):
        src = f.read_text(encoding="utf-8", errors="replace")
        for m in CALL.finditer(src):
            # walk to the matching close paren
            i, depth = m.end(), 1
            while i < len(src) and depth:
                if src[i] == "(":
                    depth += 1
                elif src[i] == ")":
                    depth -= 1
                i += 1
            args = src[m.end():i - 1]
            for s in STRING.findall(args):
                s = s.replace('\\"', '"').strip()
                if len(s) >= 3 and not s.isdigit():
                    out[f.parent.name].add(s)
    return out


def main():
    menus = json.loads((OUT / "menus.json").read_text(encoding="utf-8"))

    corpus, by_quest = {}, defaultdict(set)
    for m in menus:
        for o in m["options"]:
            n = norm(o["text"])
            if n:
                corpus.setdefault(n, o["text"])
                by_quest[norm(m["quest"]).replace(" ", "")].add(n)

    declared = extract_declared()
    tot = miss = 0
    findings = []

    for folder, strings in sorted(declared.items()):
        quest_opts = by_quest.get(folder, set())
        for s in sorted(strings):
            n = norm(s)
            if not n:
                continue
            tot += 1
            if n in corpus:
                continue
            # substring containment either way = probably the same option
            if any(n in c or c in n for c in corpus):
                continue
            miss += 1
            pool = quest_opts if quest_opts else corpus.keys()
            near = difflib.get_close_matches(n, list(pool), n=1, cutoff=0.72)
            findings.append({
                "folder": folder,
                "declared": s,
                "closest_real_option": corpus.get(near[0], "") if near else "",
                "matched_quest_page": bool(quest_opts),
            })

    exact_near = [f for f in findings if f["closest_real_option"]]
    findings.sort(key=lambda f: (not f["closest_real_option"], f["folder"]))
    (OUT / "declared_audit.json").write_text(
        json.dumps(findings, indent=1, ensure_ascii=False), encoding="utf-8")

    print(json.dumps({
        "quest_folders_scanned": len(declared),
        "declared_dialogue_strings": tot,
        "not_found_in_wiki_corpus": miss,
        "of_those_with_a_close_real_match": len(exact_near),
        "pct_unmatched": round(100.0 * miss / tot, 1) if tot else 0,
    }, indent=1))

    print("\n--- sample: declared string vs closest REAL option ---")
    for f in exact_near[:25]:
        print(f"[{f['folder']}]")
        print(f"   declared: {f['declared']}")
        print(f"   real    : {f['closest_real_option']}")


if __name__ == "__main__":
    main()
