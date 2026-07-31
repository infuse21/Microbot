package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntBiFunction;

/**
 * Every ore the plugin can mine, in ascending level order.
 *
 * <p>Rocks are matched by object <em>name</em> ("iron rocks", "coal rocks"), which is how the
 * existing, live-tested mining plugin finds them - unlike most scenery they are individually
 * named rather than all being a generic "Rocks".</p>
 *
 * <h2>Auto ladder vs manual catalogue</h2>
 * Only stages flagged {@link #inAutoLadder} take part in auto progression, kept small so the
 * bot settles into long stretches rather than relocating every few levels. Everything stays
 * available in manual mode.
 *
 * <p>Free-to-play stages list only free-to-play mines, so nearest-location picking can never
 * send an F2P account somewhere it cannot go.</p>
 */
@Getter
public enum OreStage {

    COPPER("Copper", "copper rocks", "Copper ore", 1, false, true,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Lumbridge Swamp Mine", 3229, 3146),
                    MiningLocation.of("Varrock South East", 3286, 3364),
                    MiningLocation.of("Al Kharid Mine", 3297, 3315, "bank close"),
            }),

    TIN("Tin", "tin rocks", "Tin ore", 1, false, false,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Lumbridge Swamp Mine", 3224, 3147),
                    MiningLocation.of("Varrock South West", 3181, 3376),
                    MiningLocation.of("Al Kharid Mine", 3302, 3316, "bank close"),
            }),

    IRON("Iron", "iron rocks", "Iron ore", 15, false, true,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Al Kharid Mine", 3295, 3310, "bank close"),
                    MiningLocation.of("Varrock South East", 3286, 3369),
                    MiningLocation.of("Dwarven Mine", 3033, 9826),
            }),

    SILVER("Silver", "silver rocks", "Silver ore", 20, false, false,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Varrock South West", 3177, 3366),
                    MiningLocation.of("Al Kharid Mine", 3294, 3301, "bank close"),
            }),

    VOLCANIC_ASH("Volcanic ash", "ash pile", "Volcanic ash", 22, true, false,
            StageRequirement.quest(Quest.BONE_VOYAGE),
            new MiningLocation[]{
                    MiningLocation.members("Fossil Island Volcano", 3800, 3785, "members; Bone Voyage"),
            }),

    LEAD("Lead", "lead rocks", "Lead ore", 25, true, false,
            StageRequirement.skill(Skill.SAILING, 1),
            new MiningLocation[]{
                    MiningLocation.members("The Pandemonium Mine", 3057, 9371, "members; 1 Sailing"),
            }),

    COAL("Coal", "coal rocks", "Coal", 30, false, true,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Dwarven Mine", 3038, 9799),
                    MiningLocation.of("Barbarian Village", 3082, 3421),
            }),

    GOLD("Gold", "gold rocks", "Gold ore", 40, false, false,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Dwarven Mine", 3049, 9760),
                    MiningLocation.of("Al Kharid Mine", 3294, 3287, "bank close"),
                    MiningLocation.members("Crafting Guild", 2939, 3278, "members"),
            }),

    GEM_ROCKS("Gem rocks", "gem rocks",
            new String[]{"Uncut opal", "Uncut jade", "Uncut red topaz", "Uncut sapphire",
                    "Uncut emerald", "Uncut ruby", "Uncut diamond"},
            40, true, false, StageRequirement.quest(Quest.SHILO_VILLAGE),
            new MiningLocation[]{
                    MiningLocation.members("Shilo Village Gem Mine", 2831, 9388,
                            "members; Shilo Village"),
            }),

    RUBIUM_SPLINTERS("Rubium splinters", "rubium rocks", "Rubium splinters", 48,
            true, false, StageRequirement.skill(Skill.SAILING, 60),
            new MiningLocation[]{
                    MiningLocation.members("Charred Dungeon", 2682, 8849, "members; 60 Sailing"),
            }),

    MITHRIL("Mithril", "mithril rocks", "Mithril ore", 55, false, true,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Al Kharid Mine", 3304, 3305, "bank close"),
                    MiningLocation.of("Dwarven Mine", 3036, 9772),
                    MiningLocation.of("Lumbridge Swamp West", 3144, 3146),
            }),

    LOVAKITE("Lovakite", "lovakite rocks", "Lovakite ore", 65, true, false,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.members("Lovakengj Mine", 1437, 3849, "members"),
            }),

    RUBIUM_GEODES("Rubium geodes", "rubium deposit", "Rubium geode", 68,
            true, false, StageRequirement.skill(Skill.SAILING, 60),
            new MiningLocation[]{
                    MiningLocation.members("Charred Dungeon", 2679, 8852, "members; 60 Sailing"),
            }),

    SOFT_CLAY("Soft clay", "soft clay rocks", "Soft clay", 70, true, false,
            StageRequirement.quest(Quest.SONG_OF_THE_ELVES),
            new MiningLocation[]{
                    MiningLocation.members("Trahaearn Mine", 3293, 12447,
                            "members; Song of the Elves"),
            }),

    ADAMANTITE("Adamantite", "adamantite rocks", "Adamantite ore", 70, false, true,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.of("Al Kharid Mine", 3299, 3318, "bank close"),
                    MiningLocation.of("Lumbridge Swamp West", 3148, 3147),
                    MiningLocation.of("Dwarven Mine", 3042, 9772),
            }),

    BASALT("Basalt", "basalt rocks", "Basalt", 72, true, false,
            StageRequirement.quest(Quest.MAKING_FRIENDS_WITH_MY_ARM),
            new MiningLocation[]{
                    MiningLocation.members("Weiss Salt Mine", 2834, 10334,
                            "members; Making Friends with My Arm"),
            }),

    TE_SALT("Te salt", "te salt rocks", "Te salt", 72, true, false,
            StageRequirement.quest(Quest.MAKING_FRIENDS_WITH_MY_ARM),
            new MiningLocation[]{
                    MiningLocation.members("Weiss Salt Mine", 2837, 10329,
                            "members; Making Friends with My Arm"),
            }),

    EFH_SALT("Efh salt", "efh salt rocks", "Efh salt", 72, true, false,
            StageRequirement.quest(Quest.MAKING_FRIENDS_WITH_MY_ARM),
            new MiningLocation[]{
                    MiningLocation.members("Weiss Salt Mine", 2837, 10334,
                            "members; Making Friends with My Arm"),
            }),

    URT_SALT("Urt salt", "urt salt rocks", "Urt salt", 72, true, false,
            StageRequirement.quest(Quest.MAKING_FRIENDS_WITH_MY_ARM),
            new MiningLocation[]{
                    MiningLocation.members("Weiss Salt Mine", 2833, 10334,
                            "members; Making Friends with My Arm"),
            }),

    NICKEL("Nickel", "nickel rocks", "Nickel ore", 74, true, false,
            StageRequirement.skill(Skill.SAILING, 67),
            new MiningLocation[]{
                    MiningLocation.members("Deepfin Point", 1932, 2794, "members; 67 Sailing"),
            }),

    INFERNAL_SHALE("Infernal shale", "infernal shale deposit",
            new String[]{"Infernal shale", "Crushed infernal shale"}, 78,
            true, false, StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.members("Chasm of Fire", 1469, 10090, "members"),
            }),

    /**
     * Runite. The wilderness mine is the only free-to-play option and is a PvP area, so it is
     * listed last and flagged - the members mines are the sane default.
     */
    RUNITE("Runite", "runite rocks", "Runite ore", 85, false, true,
            StageRequirement.NONE,
            new MiningLocation[]{
                    MiningLocation.members("Heroes' Guild Mine", 2940, 9884, "members; Heroes' Quest"),
                    MiningLocation.members("Fremennik Isles", 2375, 3850, "members; Fremennik Isles"),
                    MiningLocation.of("Lava Maze", 3058, 3884, "WILDERNESS"),
            }),

    /** Amethyst is mined from crystals in the members-only Mining Guild. */
    AMETHYST("Amethyst", "amethyst crystals", "Amethyst", 92, true, false,
            StageRequirement.skill(Skill.MINING, 92),
            new MiningLocation[]{
                    MiningLocation.members("Mining Guild", 3006, 9720, "members"),
            });

    private final String displayName;
    /** In-game object name to match when searching for a rock to mine. */
    private final String rockObjectName;
    /** Item name of the ore produced, used for banking, dropping and selling. */
    private final String oreName;
    /** Every possible inventory product; gem rocks can produce several different gems. */
    private final List<String> productNames;
    private final int minLevel;
    private final boolean membersOnly;
    /** Whether auto progression considers this stage. */
    private final boolean inAutoLadder;
    private final StageRequirement requirement;
    private final List<MiningLocation> locations;

    OreStage(String displayName, String rockObjectName, String oreName, int minLevel,
             boolean membersOnly, boolean inAutoLadder, StageRequirement requirement,
             MiningLocation[] locations) {
        this.displayName = displayName;
        this.rockObjectName = rockObjectName;
        this.oreName = oreName;
        this.productNames = Collections.singletonList(oreName);
        this.minLevel = minLevel;
        this.membersOnly = membersOnly;
        this.inAutoLadder = inAutoLadder;
        this.requirement = requirement;
        this.locations = Collections.unmodifiableList(Arrays.asList(locations));
    }

    OreStage(String displayName, String rockObjectName, String[] productNames, int minLevel,
             boolean membersOnly, boolean inAutoLadder, StageRequirement requirement,
             MiningLocation[] locations) {
        this.displayName = displayName;
        this.rockObjectName = rockObjectName;
        this.productNames = Collections.unmodifiableList(Arrays.asList(productNames));
        this.oreName = productNames[0];
        this.minLevel = minLevel;
        this.membersOnly = membersOnly;
        this.inAutoLadder = inAutoLadder;
        this.requirement = requirement;
        this.locations = Collections.unmodifiableList(Arrays.asList(locations));
    }

    /**
     * Mines usable on this world type. Free-to-play never sees a members mine, so neither
     * the default nor nearest-picking can route an F2P account somewhere it cannot go.
     */
    public List<MiningLocation> availableLocations(boolean membersWorld) {
        if (membersWorld) {
            return locations;
        }
        List<MiningLocation> free = new java.util.ArrayList<>();
        for (MiningLocation location : locations) {
            if (!location.isMembersOnly()) {
                free.add(location);
            }
        }
        return free.isEmpty() ? locations : free;
    }

    public MiningLocation getDefaultLocation() {
        return locations.get(0);
    }

    public MiningLocation getDefaultLocation(boolean membersWorld) {
        return availableLocations(membersWorld).get(0);
    }

    public MiningLocation findLocation(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (MiningLocation location : locations) {
            if (location.getName().equalsIgnoreCase(name)) {
                return location;
            }
        }
        return null;
    }

    /** Closest curated mine by straight-line distance; cheap fallback. */
    public MiningLocation getClosestLocation(WorldPoint from) {
        return getClosestLocation(from, true);
    }

    public MiningLocation getClosestLocation(WorldPoint from, boolean membersWorld) {
        if (from == null) {
            return getDefaultLocation(membersWorld);
        }
        MiningLocation closest = getDefaultLocation(membersWorld);
        int best = Integer.MAX_VALUE;
        for (MiningLocation candidate : availableLocations(membersWorld)) {
            int distance = from.distanceTo(candidate.getPoint());
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    /**
     * Fastest curated mine by real travel distance, so teleports and shortcuts count.
     * Pathfinding is expensive: single-location stages short-circuit, and callers must run
     * this off the client thread and cache the result.
     */
    public MiningLocation getFastestLocation(WorldPoint from,
                                             ToIntBiFunction<WorldPoint, WorldPoint> pathTiles) {
        return getFastestLocation(from, pathTiles, true);
    }

    public MiningLocation getFastestLocation(WorldPoint from,
                                             ToIntBiFunction<WorldPoint, WorldPoint> pathTiles,
                                             boolean membersWorld) {
        List<MiningLocation> usable = availableLocations(membersWorld);
        if (usable.size() == 1) {
            return usable.get(0);
        }
        if (from == null || pathTiles == null) {
            return getClosestLocation(from, membersWorld);
        }
        MiningLocation best = null;
        int bestTiles = Integer.MAX_VALUE;
        for (MiningLocation candidate : usable) {
            int tiles = pathTiles.applyAsInt(from, candidate.getPoint());
            if (tiles < bestTiles) {
                bestTiles = tiles;
                best = candidate;
            }
        }
        return best == null ? getClosestLocation(from, membersWorld) : best;
    }

    /** Why this ore cannot be mined right now, or null when it is available. */
    public String lockReason(int miningLevel, boolean membersWorld,
                             Function<Quest, QuestState> questStates,
                             Function<Skill, Integer> skillLevels) {
        return lockReason(miningLevel, membersWorld, questStates, skillLevels, null);
    }

    public String lockReason(int miningLevel, boolean membersWorld,
                             Function<Quest, QuestState> questStates,
                             Function<Skill, Integer> skillLevels,
                             IntUnaryOperator varbitValues) {
        if (membersOnly && !membersWorld) {
            return "P2P only";
        }
        if (miningLevel < minLevel) {
            return "Lv " + minLevel;
        }
        return requirement.unmetReason(questStates, skillLevels);
    }

    public boolean isAvailable(int miningLevel, boolean membersWorld,
                               Function<Quest, QuestState> questStates,
                               Function<Skill, Integer> skillLevels) {
        return lockReason(miningLevel, membersWorld, questStates, skillLevels) == null;
    }

    /** Highest auto-ladder stage the player can actually mine right now. */
    public static OreStage bestFor(int miningLevel, boolean membersWorld,
                                   Function<Quest, QuestState> questStates,
                                   Function<Skill, Integer> skillLevels) {
        OreStage best = COPPER;
        for (OreStage stage : values()) {
            if (!stage.inAutoLadder || stage.minLevel < best.minLevel) {
                continue;
            }
            if (stage.isAvailable(miningLevel, membersWorld, questStates, skillLevels)) {
                best = stage;
            }
        }
        return best;
    }

    /** The next auto-ladder rung up, or null when this is the top. */
    public OreStage next(boolean membersWorld) {
        OreStage best = null;
        for (OreStage stage : values()) {
            if (!stage.inAutoLadder || stage.minLevel <= this.minLevel) {
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
