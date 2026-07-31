"""
Extract real OSRS quest dialogue OPTION MENUS from the OSRS Wiki transcript namespace.

The wiki's transcript style guide mandates machine-parseable templates:
  {{tselect|<prompt>}}          -- "Select an Option" header, starts a menu
  {{topt|<text>}}               -- one selectable option
  {{topt|quest=no|<text>}}      -- option that is NOT part of the quest (shop, chat, flavour)
  {{topt|cond=If ...|<text>}}   -- option only present under a condition

We reconstruct each MENU (the full set of options offered at one decision point),
because the bot keys learned answers on the option SET, not on a single string.

Output: menus.json + stats, written next to this script.
"""

import json
import re
import sys
import time
import urllib.parse
import urllib.request
from collections import defaultdict
from pathlib import Path

API = "https://oldschool.runescape.wiki/api.php"
UA = "MicrobotQuestDialogueResearch/0.1 (feasibility study; contact: local)"
OUT = Path(__file__).parent / "build"
OUT.mkdir(exist_ok=True)


def api(params):
    params = dict(params, format="json", formatversion="2")
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return json.loads(r.read().decode("utf-8"))
        except Exception as e:
            if attempt == 3:
                raise
            print(f"  retry {attempt+1} after {e}", file=sys.stderr)
            time.sleep(2 * (attempt + 1))


def list_quest_transcripts():
    titles, cont = [], {}
    while True:
        d = api({"action": "query", "list": "categorymembers",
                 "cmtitle": "Category:Quest transcript", "cmlimit": "500", **cont})
        titles += [m["title"] for m in d["query"]["categorymembers"] if m["ns"] == 120]
        if "continue" not in d:
            return titles
        cont = d["continue"]


def fetch_wikitext(titles):
    """Batch-fetch page content, 50 titles per request."""
    out = {}
    for i in range(0, len(titles), 50):
        batch = titles[i:i + 50]
        d = api({"action": "query", "prop": "revisions", "rvprop": "content",
                 "rvslots": "main", "titles": "|".join(batch)})
        for p in d["query"]["pages"]:
            try:
                out[p["title"]] = p["revisions"][0]["slots"]["main"]["content"]
            except (KeyError, IndexError):
                pass
        print(f"  fetched {min(i+50, len(titles))}/{len(titles)}", file=sys.stderr)
        time.sleep(0.4)
    return out


TOPT = re.compile(r"\{\{\s*topt\s*\|(.*?)\}\}", re.S)
TSELECT = re.compile(r"\{\{\s*tselect\s*\|(.*?)\}\}", re.S)
HEADING = re.compile(r"^\s*(={2,5})\s*(.*?)\s*\1\s*$")


def clean(text):
    """Strip wiki markup down to the string the game actually shows."""
    text = re.sub(r"\{\{[Cc]hatmenu\|.*?\}\}", "", text)
    text = re.sub(r"\[\[([^\]|]*)\|([^\]]*)\]\]", r"\2", text)   # [[link|label]]
    text = re.sub(r"\[\[([^\]]*)\]\]", r"\1", text)              # [[link]]
    text = re.sub(r"'{2,}", "", text)                            # bold/italic
    text = re.sub(r"<[^>]+>", "", text)                          # html tags
    text = re.sub(r"\{\{.*?\}\}", "", text)                      # leftover templates
    return re.sub(r"\s+", " ", text).strip()


def parse_option(body):
    """Split a {{topt|...}} body into named params + the visible option text."""
    parts, depth, cur = [], 0, ""
    for ch in body:                      # split on | not nested in {{ }} or [[ ]]
        if ch == "|" and depth == 0:
            parts.append(cur); cur = ""; continue
        if ch in "{[":
            depth += 1
        elif ch in "}]":
            depth -= 1
        cur += ch
    parts.append(cur)

    named, positional = {}, []
    for p in parts:
        m = re.match(r"^\s*(quest|cond|name|hidden)\s*=\s*(.*)$", p, re.S)
        if m:
            named[m.group(1)] = clean(m.group(2))
        else:
            positional.append(p)
    text = clean(positional[-1]) if positional else ""
    return text, named


def depth_of(line):
    m = re.match(r"^(\*+)", line.strip())
    return len(m.group(1)) if m else 0


def parse_transcript(title, wikitext):
    """Reconstruct option menus. A menu = consecutive {{topt}} at one bullet depth."""
    quest = title.split(":", 1)[1]
    menus, section = [], ""
    open_menus = {}   # depth -> menu being accumulated

    def close(d):
        m = open_menus.pop(d, None)
        if m and len(m["options"]) >= 1:
            menus.append(m)

    for raw in wikitext.split("\n"):
        h = HEADING.match(raw)
        if h:
            section = clean(h.group(2))
            for d in sorted(open_menus, reverse=True):
                close(d)
            continue

        d = depth_of(raw)
        opts = TOPT.findall(raw)
        sel = TSELECT.search(raw)

        # any content at a shallower depth ends deeper menus
        for od in sorted([x for x in open_menus if x > d], reverse=True):
            close(od)

        if sel and not opts:
            close(d)
            open_menus[d] = {"quest": quest, "section": section,
                             "prompt": clean(sel.group(1)), "depth": d, "options": []}
            continue

        if opts:
            if d not in open_menus:
                open_menus[d] = {"quest": quest, "section": section,
                                 "prompt": "", "depth": d, "options": []}
            for body in opts:
                text, named = parse_option(body)
                if not text:
                    continue
                open_menus[d]["options"].append({
                    "text": text,
                    "quest_related": named.get("quest", "").lower() != "no",
                    "cond": named.get("cond", ""),
                })
            continue

        # a plain dialogue line at the same depth does NOT close the menu
        # (options are interleaved with their outcomes), but a *shallower* one did above.

    for d in sorted(open_menus, reverse=True):
        close(d)

    # de-duplicate identical menus within a quest
    seen, uniq = set(), []
    for m in menus:
        key = (m["section"], tuple(o["text"] for o in m["options"]))
        if key in seen:
            continue
        seen.add(key)
        uniq.append(m)
    return uniq


def main():
    print("listing quest transcripts...", file=sys.stderr)
    titles = list_quest_transcripts()
    print(f"  {len(titles)} transcript pages", file=sys.stderr)

    print("fetching wikitext...", file=sys.stderr)
    pages = fetch_wikitext(titles)
    print(f"  {len(pages)} pages retrieved", file=sys.stderr)

    all_menus, per_quest = [], defaultdict(int)
    for title, text in sorted(pages.items()):
        ms = parse_transcript(title, text)
        all_menus += ms
        per_quest[title] = len(ms)

    opts = [o for m in all_menus for o in m["options"]]
    non_quest = [o for o in opts if not o["quest_related"]]
    multi = [m for m in all_menus if len(m["options"]) > 1]

    (OUT / "menus.json").write_text(
        json.dumps(all_menus, indent=1, ensure_ascii=False), encoding="utf-8")

    stats = {
        "transcript_pages": len(titles),
        "pages_retrieved": len(pages),
        "pages_with_menus": sum(1 for v in per_quest.values() if v),
        "menus_total": len(all_menus),
        "menus_multi_option": len(multi),
        "options_total": len(opts),
        "options_quest_related": len(opts) - len(non_quest),
        "options_flagged_not_quest": len(non_quest),
        "options_conditional": sum(1 for o in opts if o["cond"]),
        "empty_pages": [t for t, v in per_quest.items() if v == 0],
    }
    (OUT / "stats.json").write_text(json.dumps(stats, indent=1), encoding="utf-8")
    print(json.dumps(stats, indent=1))


if __name__ == "__main__":
    main()
