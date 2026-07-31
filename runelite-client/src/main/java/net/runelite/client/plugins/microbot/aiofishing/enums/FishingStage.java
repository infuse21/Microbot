package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.FishingSpot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntBiFunction;

/**
 * Every fish the plugin can catch, in ascending level order.
 *
 * <h2>Auto ladder vs manual catalogue</h2>
 * Only stages selected by {@link #isAutoStage(boolean)} take part in auto progression.
 * The ladder is deliberately small and world-aware so the bot settles into long stretches
 * instead of re-gearing and relocating every few levels. Everything remains available in
 * manual mode.
 *
 * <h2>Requirements</h2>
 * Quest and skill gates live in {@link StageRequirement}. They are checked <em>before</em>
 * travelling, so a locked stage never causes the bot to walk to an area it can't fish -
 * auto progression skips it, and manual mode refuses to start.
 *
 * <h2>World awareness</h2>
 * Two things vary by world type and are held as separate data on purpose:
 * <ul>
 *   <li>{@link #membersOnly} - whether the fish can be caught at all. Hard gate.</li>
 *   <li>{@link AutoLadder} - which auto ladder the stage sits on, so free and members
 *       accounts follow genuinely different routes rather than one list with holes in it.</li>
 * </ul>
 * Locations carry their own {@link FishingLocation#isMembersOnly()} flag, because several
 * stages are catchable on both world types from different places - lobsters and swordfish
 * are free-to-play at Musa Point and members-only everywhere else. {@link #getClosestLocation}
 * filters on it, so an F2P account can never be routed into a members area.
 */
@Getter
public enum FishingStage {

    SHRIMP("Shrimp / Anchovies", 1, FishingMethod.NET, FishingSpot.SHRIMP, false, AutoLadder.BOTH,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Draynor Village", 3084, 3228, "bank north"),
                    FishingLocation.of("Al Kharid", 3274, 3140, "bank north"),
                    FishingLocation.of("Lumbridge Swamp", 3244, 3153),
                    FishingLocation.of("Mudskipper Point", 2995, 3158),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
            },
            "Raw shrimps", "Shrimps", "Burnt shrimp", "Raw anchovies", "Anchovies", "Burnt fish"),

    SARDINE_HERRING("Sardine / Herring", 5, FishingMethod.BAIT, FishingSpot.SHRIMP, false, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Draynor Village", 3084, 3228, "bank north"),
                    FishingLocation.of("Al Kharid", 3274, 3140, "bank north"),
                    FishingLocation.of("Lumbridge Swamp", 3244, 3153),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
            },
            "Raw sardine", "Sardine", "Raw herring", "Herring", "Burnt fish"),

    /**
     * Karambwanji from the Holy Lake south of Tai Bwo Wannai. The XP is poor, but this is
     * the <em>only</em> way to obtain karambwan bait: raw karambwanji are untradeable, so
     * neither a shop trip nor the GE fallback can supply {@link #KARAMBWAN} without it.
     *
     * <p>They stack, so the inventory never fills and no banking trip is ever triggered -
     * the run ends on the configured target level.</p>
     */
    KARAMBWANJI("Karambwanji", 5, FishingMethod.NET, FishingSpot.KARAMBWANJI, true, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Holy Lake", 2806, 3014, "fairy ring CKR"),
            },
            "Raw karambwanji"),

    // Big-net spots are the same NPCs as the shark spots - they offer "Big Net" alongside
    // "Harpoon". The Fishing Guild is deliberately NOT listed here: it needs 68 Fishing and
    // this stage unlocks at 16, so nearest-location picking must never route there.
    BIG_NET("Mackerel / Cod / Bass", 16, FishingMethod.BIG_NET, FishingSpot.SHARK, true, AutoLadder.P2P,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.members("Catherby", 2853, 3423, "bank west"),
                    FishingLocation.members("Rellekka", 2649, 3708),
            },
            "Raw mackerel", "Mackerel", "Raw cod", "Cod", "Raw bass", "Bass", "Burnt fish"),

    TROUT_SALMON("Trout / Salmon", 20, FishingMethod.LURE, FishingSpot.SALMON, false, AutoLadder.F2P,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Barbarian Village", 3103, 3424),
                    FishingLocation.of("Lumbridge river", 3238, 3241),
            },
            "Raw trout", "Trout", "Raw salmon", "Salmon", "Burnt fish"),

    PIKE("Pike", 25, FishingMethod.BAIT, FishingSpot.SALMON, false, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Barbarian Village", 3103, 3424),
                    FishingLocation.of("Lumbridge river", 3238, 3241),
            },
            "Raw pike", "Pike", "Burnt fish"),

    SLIMY_EEL("Slimy eel", 28, FishingMethod.BAIT, FishingSpot.SLIMY_EEL, true, AutoLadder.NONE,
            StageRequirement.quest(Quest.NATURE_SPIRIT), // Mort Myre access
            new FishingLocation[]{
                    FishingLocation.of("Mort'ton", 3439, 3273),
                    FishingLocation.of("Mort Myre west", 3425, 3409),
            },
            "Slimy eel", "Burnt eel"),

    // Free-to-play from Musa Point on Karamja - the Catherby and Piscarilius spots are the
    // members-only alternatives, which is why this stage is world-mixed rather than P2P.
    TUNA("Tuna", 35, FishingMethod.HARPOON, FishingSpot.LOBSTER, false, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.members("Catherby", 2844, 3429, "bank west"),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
                    FishingLocation.members("Port Piscarilius", 1762, 3796, "bank close"),
            },
            "Raw tuna", "Tuna", "Burnt fish"),

    CAVE_EEL("Cave eel", 38, FishingMethod.BAIT, FishingSpot.CAVE_EEL, true, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Lumbridge caves E", 3244, 9570, "light source"),
                    FishingLocation.of("Lumbridge caves W", 3153, 9544, "light source"),
            },
            "Raw cave eel", "Cave eel", "Burnt eel"),

    /**
     * Rainbow fish. These share the ordinary river lure spots with trout and salmon - the
     * bait is what decides the catch, so no special location is involved.
     *
     * <p>Shilo Village is the other well-known spot but is deliberately left out: it sits
     * behind the Shilo Village quest, and {@link FishingLocation} carries no unlock flag, so
     * listing it would let nearest-location picking route an unqualified account there.
     * Barbarian Village and the Lumbridge river need nothing.</p>
     */
    RAINBOW_FISH("Rainbow fish", 38, FishingMethod.LURE_STRIPY, FishingSpot.SALMON, true, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Barbarian Village", 3103, 3424, "stripy feathers"),
                    FishingLocation.of("Lumbridge river", 3238, 3241, "stripy feathers"),
            },
            "Raw rainbow fish", "Rainbow fish", "Burnt fish"),

    /**
     * Lobsters are on <em>both</em> ladders. Musa Point on Karamja is free-to-play, which is
     * what carries an F2P account from trout at 20 all the way to swordfish at 50; Catherby,
     * Piscarilius and Rellekka are the members alternatives.
     */
    LOBSTER("Lobster", 40, FishingMethod.CAGE, FishingSpot.LOBSTER, false, AutoLadder.BOTH,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.members("Catherby", 2844, 3429, "bank west"),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
                    FishingLocation.members("Port Piscarilius", 1762, 3796, "bank close"),
                    FishingLocation.members("Rellekka", 2641, 3696),
            },
            "Raw lobster", "Lobster", "Burnt lobster"),

    /**
     * Barbarian fishing (Barbarian Training miniquest). One spot yields progressively better
     * fish as levels rise - leaping trout at 48/15/15, salmon at 58/30/30, sturgeon at
     * 70/45/45 - so this is modelled as a single stage gated on the lowest set.
     *
     * <p>Gated on the chapter varbit rather than the miniquest's {@link QuestState}: Otto has
     * to have taught the rod technique specifically, and completing an unrelated chapter
     * (e.g. firemaking) would otherwise make the miniquest look started while fishing is
     * still locked.</p>
     *
     * <p>Manual-only: it needs an unlock the plugin can't perform, and there is no bank
     * beside either location, so it suits power-fishing (Drop) rather than the auto ladder.</p>
     */
    BARBARIAN_FISH("Leaping fish", 48, FishingMethod.BARBARIAN_ROD,
            FishingSpot.BARB_FISH, true, AutoLadder.NONE,
            StageRequirement.varbitAndSkills(VarbitID.BRUT_FISHING_R, 1,
                    "Learn barb fishing from Otto", Skill.AGILITY, 15, Skill.STRENGTH, 15),
            new FishingLocation[]{
                    FishingLocation.of("Otto's Grotto", 2500, 3510, "no bank"),
                    FishingLocation.of("Mount Quidamortem", 1265, 3541, "no bank"),
            },
            "Leaping trout", "Leaping salmon", "Leaping sturgeon"),

    /** Free-to-play from Musa Point, and the top of the F2P ladder - nothing above it. */
    SWORDFISH("Swordfish / Tuna", 50, FishingMethod.HARPOON, FishingSpot.LOBSTER, false, AutoLadder.BOTH,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.members("Catherby", 2844, 3429, "bank west"),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
                    FishingLocation.members("Port Piscarilius", 1762, 3796, "bank close"),
                    FishingLocation.members("Rellekka", 2641, 3696),
            },
            "Raw swordfish", "Swordfish", "Burnt swordfish", "Raw tuna", "Tuna", "Burnt fish"),

    LAVA_EEL("Lava eel", 53, FishingMethod.OILY_ROD, FishingSpot.LAVA_EEL, true, AutoLadder.NONE,
            StageRequirement.questStarted(Quest.HEROES_QUEST),
            new FishingLocation[]{
                    FishingLocation.of("Taverley Dungeon", 2893, 9764),
                    FishingLocation.of("Lava Maze", 3071, 3840, "wilderness"),
            },
            "Lava eel", "Burnt fish"),

    MONKFISH("Monkfish", 62, FishingMethod.NET, FishingSpot.MONKFISH, true, AutoLadder.P2P,
            StageRequirement.questStarted(Quest.SWAN_SONG),
            new FishingLocation[]{
                    FishingLocation.members("Piscatoris", 2310, 3696, "bank close"),
            },
            "Raw monkfish", "Monkfish", "Burnt monkfish"),

    KARAMBWAN("Karambwan", 65, FishingMethod.KARAMBWAN_VESSEL, FishingSpot.KARAMBWAN, true, AutoLadder.NONE,
            StageRequirement.quest(Quest.TAI_BWO_WANNAI_TRIO),
            new FishingLocation[]{
                    FishingLocation.of("Brimhaven", 2898, 3119, "fairy ring DKP"),
            },
            "Raw karambwan", "Cooked karambwan", "Burnt karambwan"),

    SHARK("Shark", 76, FishingMethod.HARPOON, FishingSpot.SHARK, true, AutoLadder.P2P,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.members("Fishing Guild", 2605, 3417, "68 Fishing"),
                    FishingLocation.members("Catherby", 2853, 3423),
                    FishingLocation.members("Rellekka", 2649, 3708),
            },
            "Raw shark", "Shark", "Burnt shark"),

    /**
     * Infernal eels in Mor Ul Rek. Access needs a fire cape shown to the TzHaar guard and
     * the eels burn bare hands, so both the cape and ice gloves are demanded as tools by
     * {@link FishingMethod#OILY_ROD_ICE} - gearing refuses at the bank with a named reason
     * rather than walking to a city we cannot enter.
     *
     * <p>The catch is worthless until cracked with a hammer, which {@link CatchProcessing}
     * handles; the output stacks, so a full inventory clears itself without banking.</p>
     */
    INFERNAL_EEL("Infernal eel", 80, FishingMethod.OILY_ROD_ICE, FishingSpot.INFERNAL_EEL,
            true, AutoLadder.NONE, StageRequirement.NONE, CatchProcessing.CRACK,
            new FishingLocation[]{
                    FishingLocation.of("Mor Ul Rek W", 2443, 5104, "fire cape"),
                    FishingLocation.of("Mor Ul Rek S", 2476, 5077, "fire cape"),
                    FishingLocation.of("Mor Ul Rek E", 2537, 5086, "fire cape"),
            },
            "Infernal eel"),

    ANGLERFISH("Anglerfish", 82, FishingMethod.SANDWORMS, FishingSpot.ANGLERFISH, true, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Port Piscarilius", 1831, 3773, "bank close"),
            },
            "Raw anglerfish", "Anglerfish", "Burnt anglerfish"),

    DARK_CRAB("Dark crab", 85, FishingMethod.DARK_CRAB_CAGE, FishingSpot.DARK_CRAB, true, AutoLadder.NONE,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Resource Area", 3186, 3925, "wilderness"),
            },
            "Raw dark crab", "Dark crab", "Burnt dark crab"),

    /**
     * Sacred eels at Zul-Andra. Regicide is required to reach Tirannwn at all, and the
     * High Priestess has to be spoken to once before the worshippers allow fishing - the
     * plugin cannot do that itself, so it is called out in the location note.
     *
     * <p>Dissecting with a knife yields Zulrah's scales but needs 72 Cooking. That gate sits
     * on {@link CatchProcessing#DISSECT} rather than on this stage, so a lower-Cooking
     * account still fishes them and banks them whole instead of being refused outright.</p>
     */
    SACRED_EEL("Sacred eel", 87, FishingMethod.BAIT, FishingSpot.SACRED_EEL, true, AutoLadder.NONE,
            StageRequirement.quest(Quest.REGICIDE), CatchProcessing.DISSECT,
            new FishingLocation[]{
                    FishingLocation.of("Zul-Andra W", 2183, 3068, "talk to priestess first"),
                    FishingLocation.of("Zul-Andra E", 2195, 3067, "talk to priestess first"),
            },
            "Sacred eel");

    private final String displayName;
    private final int minLevel;
    private final FishingMethod method;
    private final FishingSpot spot;
    private final boolean membersOnly;
    /** Which auto-progression ladder this belongs to. Kept small to avoid churn. */
    private final AutoLadder ladder;
    private final StageRequirement requirement;
    /** In-inventory step the catch needs before it is worth anything. Never null. */
    private final CatchProcessing processing;
    private final List<FishingLocation> locations;
    private final List<String> catchItemNames;

    FishingStage(String displayName, int minLevel, FishingMethod method, FishingSpot spot,
                 boolean membersOnly, AutoLadder ladder, StageRequirement requirement,
                 FishingLocation[] locations, String... catchItemNames) {
        this(displayName, minLevel, method, spot, membersOnly, ladder, requirement,
                CatchProcessing.NONE, locations, catchItemNames);
    }

    FishingStage(String displayName, int minLevel, FishingMethod method, FishingSpot spot,
                 boolean membersOnly, AutoLadder ladder, StageRequirement requirement,
                 CatchProcessing processing, FishingLocation[] locations,
                 String... catchItemNames) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.method = method;
        this.spot = spot;
        this.membersOnly = membersOnly;
        this.ladder = ladder;
        this.requirement = requirement;
        this.processing = processing;
        this.locations = Collections.unmodifiableList(Arrays.asList(locations));
        this.catchItemNames = Arrays.asList(catchItemNames);
    }

    public int[] getSpotIds() {
        return spot.getIds();
    }

    /**
     * Locations reachable on this world type.
     *
     * <p>Several stages are catchable on both - lobsters and swordfish are free-to-play at
     * Musa Point but members-only at Catherby, Piscarilius and Rellekka. Filtering here is
     * what stops nearest-location picking from routing a free account into a members area.</p>
     *
     * <p>If filtering leaves nothing the full list is returned rather than an empty one, so
     * a caller always gets somewhere to aim at; the stage's own {@code membersOnly} gate is
     * what refuses the trip in that case.</p>
     */
    public List<FishingLocation> availableLocations(boolean membersWorld) {
        if (membersWorld) {
            return locations;
        }
        List<FishingLocation> free = new java.util.ArrayList<>();
        for (FishingLocation location : locations) {
            if (!location.isMembersOnly()) {
                free.add(location);
            }
        }
        return free.isEmpty() ? locations : free;
    }

    /** Primary (first-listed) location, used as a fallback when the player position is unknown. */
    public FishingLocation getDefaultLocation() {
        return locations.get(0);
    }

    /** Primary location reachable on this world type. */
    public FishingLocation getDefaultLocation(boolean membersWorld) {
        return availableLocations(membersWorld).get(0);
    }

    /** Look up one of this stage's locations by name, or null if it has no such location. */
    public FishingLocation findLocation(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (FishingLocation location : locations) {
            if (location.getName().equalsIgnoreCase(name)) {
                return location;
            }
        }
        return null;
    }

    /**
     * Fastest curated location by <em>actual travel distance</em>.
     *
     * <p>Straight-line distance ignores teleports and shortcuts, so it can pick a location
     * that is nearer on the map but slower to reach. Pass a path-aware measure (see
     * {@code Rs2Walker.getTotalTiles}) and this returns the genuinely quickest option.</p>
     *
     * <p>Pathfinding is expensive, so this short-circuits for single-location stages and
     * falls back to straight-line distance if nothing is reachable. Callers must invoke it
     * off the client thread and cache the result.</p>
     *
     * @param from      where we're travelling from
     * @param pathTiles measures travel tiles between two points; may return Integer.MAX_VALUE
     */
    public FishingLocation getFastestLocation(WorldPoint from, ToIntBiFunction<WorldPoint, WorldPoint> pathTiles) {
        return getFastestLocation(from, pathTiles, true);
    }

    public FishingLocation getFastestLocation(WorldPoint from,
                                              ToIntBiFunction<WorldPoint, WorldPoint> pathTiles,
                                              boolean membersWorld) {
        List<FishingLocation> usable = availableLocations(membersWorld);
        if (usable.size() == 1) {
            return usable.get(0);
        }
        if (from == null || pathTiles == null) {
            return getClosestLocation(from, membersWorld);
        }
        FishingLocation best = null;
        int bestTiles = Integer.MAX_VALUE;
        for (FishingLocation candidate : usable) {
            int tiles = pathTiles.applyAsInt(from, candidate.getPoint());
            if (tiles < bestTiles) {
                bestTiles = tiles;
                best = candidate;
            }
        }
        // Everything unreachable (or pathfinder unavailable) -> fall back to straight line.
        return best == null ? getClosestLocation(from, membersWorld) : best;
    }

    /**
     * Closest curated location by straight-line distance. Cheap; used as a fallback and
     * anywhere a path-aware measure would be too expensive (e.g. overlay rendering).
     */
    public FishingLocation getClosestLocation(WorldPoint from) {
        return getClosestLocation(from, true);
    }

    public FishingLocation getClosestLocation(WorldPoint from, boolean membersWorld) {
        if (from == null) {
            return getDefaultLocation(membersWorld);
        }
        FishingLocation closest = getDefaultLocation(membersWorld);
        int best = Integer.MAX_VALUE;
        for (FishingLocation candidate : availableLocations(membersWorld)) {
            // distanceTo returns MAX_VALUE across planes/regions; guard keeps the default.
            int distance = from.distanceTo(candidate.getPoint());
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    /**
     * Why this stage can't be trained right now, or null when it's good to go.
     * Checked before any travelling so locked areas are never walked to.
     */
    public String lockReason(int fishingLevel, boolean membersWorld,
                             Function<Quest, QuestState> questStates,
                             Function<Skill, Integer> skillLevels) {
        return lockReason(fishingLevel, membersWorld, questStates, skillLevels, null);
    }

    /**
     * @param varbitValues resolves a varbit id to its value, for stages gated on a miniquest
     *                     chapter varbit. Null means "cannot read", which locks such stages
     *                     so we fail safe rather than travelling to an area we can't use.
     */
    public String lockReason(int fishingLevel, boolean membersWorld,
                             Function<Quest, QuestState> questStates,
                             Function<Skill, Integer> skillLevels,
                             IntUnaryOperator varbitValues) {
        if (membersOnly && !membersWorld) {
            return "P2P only";
        }
        if (fishingLevel < minLevel) {
            return "Lv " + minLevel;
        }
        return requirement.unmetReason(questStates, skillLevels, varbitValues);
    }

    public boolean isAvailable(int fishingLevel, boolean membersWorld,
                               Function<Quest, QuestState> questStates,
                               Function<Skill, Integer> skillLevels) {
        return isAvailable(fishingLevel, membersWorld, questStates, skillLevels, null);
    }

    public boolean isAvailable(int fishingLevel, boolean membersWorld,
                               Function<Quest, QuestState> questStates,
                               Function<Skill, Integer> skillLevels,
                               IntUnaryOperator varbitValues) {
        return lockReason(fishingLevel, membersWorld, questStates, skillLevels, varbitValues) == null;
    }

    /**
     * Whether this stage belongs to the progression ladder for the current world type.
     *
     * <p>The two ladders are:</p>
     * <pre>
     * P2P: Shrimp 1 -&gt; Big Net 16 -&gt; Lobster 40 -&gt; Swordfish 50 -&gt; Monkfish 62 -&gt; Shark 76
     * F2P: Shrimp 1 -&gt; Trout 20   -&gt; Lobster 40 -&gt; Swordfish 50 (top)
     * </pre>
     *
     * <p>Members use Big Net from 16 until Lobster at 40. Switching to Trout at 20 would mean
     * another tool and a cross-map trip for four levels, then back to the coast at 40. F2P
     * cannot use Big Net at all, so Trout is its long-running alternative - and from 40 both
     * ladders converge on the Karamja cage and harpoon spots, which are free-to-play.</p>
     */
    public boolean isAutoStage(boolean membersWorld) {
        return ladder.appliesTo(membersWorld);
    }

    /**
     * Highest auto-ladder stage the player can actually train right now. Stages whose
     * quest/skill requirements aren't met are skipped rather than attempted.
     */
    public static FishingStage bestFor(int fishingLevel, boolean membersWorld,
                                       Function<Quest, QuestState> questStates,
                                       Function<Skill, Integer> skillLevels) {
        return bestFor(fishingLevel, membersWorld, questStates, skillLevels, null);
    }

    public static FishingStage bestFor(int fishingLevel, boolean membersWorld,
                                       Function<Quest, QuestState> questStates,
                                       Function<Skill, Integer> skillLevels,
                                       IntUnaryOperator varbitValues) {
        FishingStage best = SHRIMP;
        for (FishingStage stage : values()) {
            if (!stage.isAutoStage(membersWorld) || stage.minLevel < best.minLevel) {
                continue;
            }
            if (stage.isAvailable(fishingLevel, membersWorld, questStates, skillLevels, varbitValues)) {
                best = stage;
            }
        }
        return best;
    }

    /** The next auto-ladder rung up, or null when this is the top. */
    public FishingStage next(boolean membersWorld) {
        FishingStage best = null;
        for (FishingStage stage : values()) {
            if (!stage.isAutoStage(membersWorld) || stage.minLevel <= this.minLevel) {
                continue;
            }
            if (stage.membersOnly && !membersWorld) {
                continue;
            }
            if (best == null || stage.minLevel < best.minLevel) {
                best = stage;
            }
        }
        return best;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
