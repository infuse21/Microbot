# F2P Web Walker Harness

This harness runs live in-game webwalker regression routes for a fresh F2P account after Tutorial Island. It uses `Rs2Walker.walkWithState(...)` for both setup movement and the route under test.

The Microbot CLI may be used while investigating a failure to read state, nearby objects, nearby NPCs, screenshots, or logs. Do not use `./microbot-cli walk` or any other manual movement command to place the player at the destination.

## Run

Full fail-fast suite:

```bash
scripts/run-f2p-webwalker-harness.sh
```

Specific route:

```bash
scripts/run-f2p-webwalker-harness.sh F2P-15
```

The script compiles the client, starts RuneLite in test mode with AutoLogin enabled by `TestRunnerPlugin`, runs the hidden `F2P Web Walker Harness` plugin, and writes:

```text
~/.runelite/test-results/f2p-webwalker/result.json
```

Override the timeout or output directory:

```bash
MICROBOT_WEBWALKER_TIMEOUT_MS=2400000 \
MICROBOT_WEBWALKER_LEG_TIMEOUT_MS=300000 \
MICROBOT_WEBWALKER_OUTPUT_DIR=/tmp/f2p-webwalker \
scripts/run-f2p-webwalker-harness.sh F2P-15
```

The runner forwards route settings through `microbot.test.webwalker.*` system properties because the Gradle `runTest` task only propagates `microbot.test.*` properties into the launched client JVM.

## Agent Loop

1. Run the full suite.
2. If a route fails, inspect `result.json`, `~/.runelite/logs/client.log`, and optional observational CLI output such as `./microbot-cli state`, `objects`, `npcs`, or screenshots.
3. Understand whether the failure was setup movement or the route itself.
4. Patch the walker or supporting path data.
5. Rebuild and rerun only the failed route, for example `scripts/run-f2p-webwalker-harness.sh F2P-15`.
6. Once the failed route passes, rerun the full suite.

## Required Routes

| ID | From | To | Coverage |
|---|---:|---:|---|
| F2P-01 | `3222,3218,0` | `3208,3220,2` | Lumbridge castle stairs and plane change |
| F2P-02 | `3208,3220,2` | `3253,3266,0` | Castle exit to local outdoor area |
| F2P-03 | `3253,3266,0` | `3092,3245,0` | Lumbridge/Draynor open-world routing |
| F2P-04 | `3092,3245,0` | `3109,3168,0` | Draynor to Wizards' Tower bridge |
| F2P-05 | `3109,3168,0` | `3029,3217,0` | Bridge to Port Sarim docks |
| F2P-06 | `3029,3217,0` | `2957,3214,0` | Port Sarim to Rimmington |
| F2P-07 | `2957,3214,0` | `2946,3368,0` | Rimmington to Falador |
| F2P-08 | `2946,3368,0` | `3082,3420,0` | Falador to Barbarian Village |
| F2P-09 | `3082,3420,0` | `3093,3493,0` | Barbarian Village to Edgeville |
| F2P-10 | `3093,3493,0` | `3164,3486,0` | Edgeville to Grand Exchange |
| F2P-11 | `3164,3486,0` | `3185,3441,0` | Grand Exchange to Varrock west bank |
| F2P-12 | `3185,3441,0` | `3253,3420,0` | Varrock west-to-east city routing |
| F2P-13 | `3253,3420,0` | `3222,3218,0` | Varrock to Lumbridge long return |
| F2P-14 | `3092,3245,0` | `3109,3341,0` | Draynor Manor approach |
| F2P-15 | `3109,3341,0` | `3106,3363,0` | Draynor Manor door/object handling |
| F2P-16 | `3106,3363,0` | `3092,3245,0` | Reverse manor exit behavior |
| F2P-17 | current live player tile | `3237,9858,0` | Captures current origin, then walks to Varrock Sewers 5 times on a F2P world with agility shortcuts and teleports disabled |
