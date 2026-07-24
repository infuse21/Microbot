package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.FishingSpot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
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
 * <p>Free-to-play stages deliberately list only free-to-play locations, so that
 * {@link #getClosestLocation} can never send an F2P account to a members area.</p>
 */
@Getter
public enum FishingStage {

    SHRIMP("Shrimp / Anchovies", 1, FishingMethod.NET, FishingSpot.SHRIMP, false, true,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Draynor Village", 3084, 3228, "bank north"),
                    FishingLocation.of("Al Kharid", 3274, 3140, "bank north"),
                    FishingLocation.of("Lumbridge Swamp", 3244, 3153),
                    FishingLocation.of("Mudskipper Point", 2995, 3158),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
            },
            "Raw shrimps", "Shrimps", "Burnt shrimp", "Raw anchovies", "Anchovies", "Burnt fish"),

    SARDINE_HERRING("Sardine / Herring", 5, FishingMethod.BAIT, FishingSpot.SHRIMP, false, false,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Draynor Village", 3084, 3228, "bank north"),
                    FishingLocation.of("Al Kharid", 3274, 3140, "bank north"),
                    FishingLocation.of("Lumbridge Swamp", 3244, 3153),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
            },
            "Raw sardine", "Sardine", "Raw herring", "Herring", "Burnt fish"),

    // Big-net spots are the same NPCs as the shark spots - they offer "Big Net" alongside
    // "Harpoon". The Fishing Guild is deliberately NOT listed here: it needs 68 Fishing and
    // this stage unlocks at 16, so nearest-location picking must never route there.
    BIG_NET("Big net: Mackerel / Cod / Bass", 16, FishingMethod.BIG_NET, FishingSpot.SHARK, true, true,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Catherby", 2853, 3423, "bank west"),
                    FishingLocation.of("Rellekka", 2649, 3708),
            },
            "Raw mackerel", "Mackerel", "Raw cod", "Cod", "Raw bass", "Bass", "Burnt fish"),

    TROUT_SALMON("Trout / Salmon", 20, FishingMethod.LURE, FishingSpot.SALMON, false, true,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Barbarian Village", 3103, 3424),
                    FishingLocation.of("Lumbridge river", 3238, 3241),
            },
            "Raw trout", "Trout", "Raw salmon", "Salmon", "Burnt fish"),

    PIKE("Pike", 25, FishingMethod.BAIT, FishingSpot.SALMON, false, false,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Barbarian Village", 3103, 3424),
                    FishingLocation.of("Lumbridge river", 3238, 3241),
            },
            "Raw pike", "Pike", "Burnt fish"),

    SLIMY_EEL("Slimy eel", 28, FishingMethod.BAIT, FishingSpot.SLIMY_EEL, true, false,
            StageRequirement.quest(Quest.NATURE_SPIRIT), // Mort Myre access
            new FishingLocation[]{
                    FishingLocation.of("Mort'ton", 3439, 3273),
                    FishingLocation.of("Mort Myre west", 3425, 3409),
            },
            "Slimy eel", "Burnt eel"),

    TUNA("Tuna", 35, FishingMethod.HARPOON, FishingSpot.LOBSTER, true, false,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Catherby", 2844, 3429, "bank west"),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
                    FishingLocation.of("Port Piscarilius", 1762, 3796, "bank close"),
            },
            "Raw tuna", "Tuna", "Burnt fish"),

    CAVE_EEL("Cave eel", 38, FishingMethod.BAIT, FishingSpot.CAVE_EEL, true, false,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Lumbridge caves E", 3244, 9570, "light source"),
                    FishingLocation.of("Lumbridge caves W", 3153, 9544, "light source"),
            },
            "Raw cave eel", "Cave eel", "Burnt eel"),

    LOBSTER("Lobster", 40, FishingMethod.CAGE, FishingSpot.LOBSTER, true, true,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Catherby", 2844, 3429, "bank west"),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
                    FishingLocation.of("Port Piscarilius", 1762, 3796, "bank close"),
                    FishingLocation.of("Rellekka", 2641, 3696),
            },
            "Raw lobster", "Lobster", "Burnt lobster"),

    SWORDFISH("Swordfish / Tuna", 50, FishingMethod.HARPOON, FishingSpot.LOBSTER, true, true,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Catherby", 2844, 3429, "bank west"),
                    FishingLocation.of("Musa Point", 2925, 3179, "bank close"),
                    FishingLocation.of("Port Piscarilius", 1762, 3796, "bank close"),
                    FishingLocation.of("Rellekka", 2641, 3696),
            },
            "Raw swordfish", "Swordfish", "Burnt swordfish", "Raw tuna", "Tuna", "Burnt fish"),

    LAVA_EEL("Lava eel", 53, FishingMethod.OILY_ROD, FishingSpot.LAVA_EEL, true, false,
            StageRequirement.questStarted(Quest.HEROES_QUEST),
            new FishingLocation[]{
                    FishingLocation.of("Taverley Dungeon", 2893, 9764),
                    FishingLocation.of("Lava Maze", 3071, 3840, "wilderness"),
            },
            "Lava eel", "Burnt fish"),

    MONKFISH("Monkfish", 62, FishingMethod.NET, FishingSpot.MONKFISH, true, true,
            StageRequirement.questStarted(Quest.SWAN_SONG),
            new FishingLocation[]{
                    FishingLocation.of("Piscatoris", 2310, 3696, "bank close"),
            },
            "Raw monkfish", "Monkfish", "Burnt monkfish"),

    KARAMBWAN("Karambwan", 65, FishingMethod.KARAMBWAN_VESSEL, FishingSpot.KARAMBWAN, true, false,
            StageRequirement.quest(Quest.TAI_BWO_WANNAI_TRIO),
            new FishingLocation[]{
                    FishingLocation.of("Brimhaven", 2898, 3119, "fairy ring DKP"),
            },
            "Raw karambwan", "Cooked karambwan", "Burnt karambwan"),

    SHARK("Shark", 76, FishingMethod.HARPOON, FishingSpot.SHARK, true, true,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Fishing Guild", 2605, 3417, "68 Fishing"),
                    FishingLocation.of("Catherby", 2853, 3423),
                    FishingLocation.of("Rellekka", 2649, 3708),
            },
            "Raw shark", "Shark", "Burnt shark"),

    ANGLERFISH("Anglerfish", 82, FishingMethod.SANDWORMS, FishingSpot.ANGLERFISH, true, false,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Port Piscarilius", 1831, 3773, "bank close"),
            },
            "Raw anglerfish", "Anglerfish", "Burnt anglerfish"),

    DARK_CRAB("Dark crab", 85, FishingMethod.DARK_CRAB_CAGE, FishingSpot.DARK_CRAB, true, false,
            StageRequirement.NONE,
            new FishingLocation[]{
                    FishingLocation.of("Resource Area", 3186, 3925, "wilderness"),
            },
            "Raw dark crab", "Dark crab", "Burnt dark crab");

    private final String displayName;
    private final int minLevel;
    private final FishingMethod method;
    private final FishingSpot spot;
    private final boolean membersOnly;
    /** Whether auto progression considers this stage. Kept small to avoid churn. */
    private final boolean inAutoLadder;
    private final StageRequirement requirement;
    private final List<FishingLocation> locations;
    private final List<String> catchItemNames;

    FishingStage(String displayName, int minLevel, FishingMethod method, FishingSpot spot,
                 boolean membersOnly, boolean inAutoLadder, StageRequirement requirement,
                 FishingLocation[] locations, String... catchItemNames) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.method = method;
        this.spot = spot;
        this.membersOnly = membersOnly;
        this.inAutoLadder = inAutoLadder;
        this.requirement = requirement;
        this.locations = Collections.unmodifiableList(Arrays.asList(locations));
        this.catchItemNames = Arrays.asList(catchItemNames);
    }

    public int[] getSpotIds() {
        return spot.getIds();
    }

    /** Primary (first-listed) location, used as a fallback when the player position is unknown. */
    public FishingLocation getDefaultLocation() {
        return locations.get(0);
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
        if (locations.size() == 1) {
            return getDefaultLocation();
        }
        if (from == null || pathTiles == null) {
            return getClosestLocation(from);
        }
        FishingLocation best = null;
        int bestTiles = Integer.MAX_VALUE;
        for (FishingLocation candidate : locations) {
            int tiles = pathTiles.applyAsInt(from, candidate.getPoint());
            if (tiles < bestTiles) {
                bestTiles = tiles;
                best = candidate;
            }
        }
        // Everything unreachable (or pathfinder unavailable) -> fall back to straight line.
        return best == null ? getClosestLocation(from) : best;
    }

    /**
     * Closest curated location by straight-line distance. Cheap; used as a fallback and
     * anywhere a path-aware measure would be too expensive (e.g. overlay rendering).
     */
    public FishingLocation getClosestLocation(WorldPoint from) {
        if (from == null) {
            return getDefaultLocation();
        }
        FishingLocation closest = getDefaultLocation();
        int best = Integer.MAX_VALUE;
        for (FishingLocation candidate : locations) {
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
        if (membersOnly && !membersWorld) {
            return "P2P only";
        }
        if (fishingLevel < minLevel) {
            return "Lv " + minLevel;
        }
        return requirement.unmetReason(questStates, skillLevels);
    }

    public boolean isAvailable(int fishingLevel, boolean membersWorld,
                               Function<Quest, QuestState> questStates,
                               Function<Skill, Integer> skillLevels) {
        return lockReason(fishingLevel, membersWorld, questStates, skillLevels) == null;
    }

    /**
     * Whether this stage belongs to the progression ladder for the current world type.
     *
     * <p>Members use Big Net from 16 until Lobster at 40. Switching to Trout at 20 would
     * require another tool and cross-map trip after only four levels, then send the player
     * back to the coast at 40. F2P cannot use Big Net, so Trout remains its long-running
     * alternative.</p>
     */
    public boolean isAutoStage(boolean membersWorld) {
        if (this == BIG_NET) {
            return membersWorld;
        }
        if (this == TROUT_SALMON) {
            return !membersWorld;
        }
        return inAutoLadder;
    }

    /**
     * Highest auto-ladder stage the player can actually train right now. Stages whose
     * quest/skill requirements aren't met are skipped rather than attempted.
     */
    public static FishingStage bestFor(int fishingLevel, boolean membersWorld,
                                       Function<Quest, QuestState> questStates,
                                       Function<Skill, Integer> skillLevels) {
        FishingStage best = SHRIMP;
        for (FishingStage stage : values()) {
            if (!stage.isAutoStage(membersWorld) || stage.minLevel < best.minLevel) {
                continue;
            }
            if (stage.isAvailable(fishingLevel, membersWorld, questStates, skillLevels)) {
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
