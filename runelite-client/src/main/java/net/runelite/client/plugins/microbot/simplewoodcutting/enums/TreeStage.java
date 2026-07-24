package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Every tree the plugin can chop, in ascending level order.
 *
 * <h2>Auto ladder vs manual catalogue</h2>
 * Only stages flagged {@link #inAutoLadder} take part in auto progression. That set is
 * kept deliberately small so the bot settles into long stretches instead of relocating
 * every few levels. Everything else is available in manual mode.
 *
 * <h2>Requirements</h2>
 * Quest and skill gates live in {@link StageRequirement}, checked before travelling so a
 * locked stage never sends the bot to an area it can't chop in.
 *
 * <p>Free-to-play stages deliberately list only free-to-play locations, so nearest-
 * location picking can never send an F2P account to a members area.</p>
 */
@Getter
public enum TreeStage {

    TREE("Tree", "tree", "Logs", 1, "Chop down", false, true,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Lumbridge", 3193, 3226, "bank walk"),
                    TreeLocation.of("Draynor Village", 3086, 3235, "bank close"),
                    TreeLocation.of("Varrock West", 3162, 3411),
            }),

    OAK("Oak", "oak tree", "Oak logs", 15, "Chop down", false, true,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Draynor Village", 3103, 3279, "bank close"),
                    TreeLocation.of("Lumbridge", 3190, 3247),
                    TreeLocation.of("Varrock West", 3085, 3481),
            }),

    WILLOW("Willow", "willow tree", "Willow logs", 30, "Chop down", false, true,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Draynor Village", 3088, 3235, "bank close"),
                    TreeLocation.of("Port Sarim", 3059, 3253),
                    TreeLocation.of("Barbarian Outpost", 2532, 3565),
            }),

    TEAK("Teak", "teak tree", "Teak logs", 35, "Chop down", true, false,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Fossil Island", 3714, 3835, "Bone Voyage"),
                    TreeLocation.of("Ape Atoll", 2765, 2704, "members"),
            }),

    MAPLE("Maple", "maple tree", "Maple logs", 45, "Chop down", true, true,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Seers' Village", 2725, 3480, "bank close"),
                    TreeLocation.of("Woodcutting Guild", 1591, 3474, "60% Hosidius"),
            }),

    MAHOGANY("Mahogany", "mahogany tree", "Mahogany logs", 50, "Chop down", true, false,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Fossil Island", 3714, 3835, "Bone Voyage"),
                    TreeLocation.of("Ape Atoll", 2765, 2704, "members"),
            }),

    YEW("Yew", "yew tree", "Yew logs", 60, "Chop down", false, true,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Varrock Castle", 3225, 3459),
                    TreeLocation.of("Falador", 3052, 3272),
                    TreeLocation.of("Woodcutting Guild", 1591, 3483, "60% Hosidius"),
                    TreeLocation.of("Lumbridge", 3165, 3220),
            }),

    BLISTERWOOD("Blisterwood", "blisterwood tree", "Blisterwood logs", 62, "Chop", true, false,
            StageRequirement.quest(Quest.SINS_OF_THE_FATHER),
            new TreeLocation[]{
                    TreeLocation.of("Darkmeyer", 3634, 3362, "Sins of the Father"),
            }),

    SULLIUSCEP("Sulliuscep", "sulliuscep", "Mushroom", 65, "Chop", true, false,
            StageRequirement.quest(Quest.BONE_VOYAGE),
            new TreeLocation[]{
                    TreeLocation.of("Fossil Island", 3688, 3883, "Bone Voyage"),
            }),

    MAGIC("Magic", "magic tree", "Magic logs", 75, "Chop down", true, true,
            StageRequirement.NONE,
            new TreeLocation[]{
                    TreeLocation.of("Woodcutting Guild", 1610, 3443, "60% Hosidius"),
                    TreeLocation.of("Sorcerer's Tower", 2704, 3397, "members"),
            }),

    REDWOOD("Redwood", "redwood tree", "Redwood logs", 90, "Cut", true, true,
            StageRequirement.NONE, // level 90 gate already comes from minLevel
            new TreeLocation[]{
                    TreeLocation.of("Woodcutting Guild", 1569, 3493, "60% Hosidius"),
            });

    private final String displayName;
    /** In-game object name to match when searching for a tree to chop. */
    private final String treeObjectName;
    /** Item name of the log this tree yields (used for banking/dropping/selling). */
    private final String logName;
    private final int minLevel;
    /** Menu action to chop this tree (varies: "Chop down", "Chop", "Cut"). */
    private final String action;
    private final boolean membersOnly;
    /** Whether auto progression considers this stage. Kept small to avoid churn. */
    private final boolean inAutoLadder;
    private final StageRequirement requirement;
    private final List<TreeLocation> locations;

    TreeStage(String displayName, String treeObjectName, String logName, int minLevel, String action,
              boolean membersOnly, boolean inAutoLadder, StageRequirement requirement,
              TreeLocation[] locations) {
        this.displayName = displayName;
        this.treeObjectName = treeObjectName;
        this.logName = logName;
        this.minLevel = minLevel;
        this.action = action;
        this.membersOnly = membersOnly;
        this.inAutoLadder = inAutoLadder;
        this.requirement = requirement;
        this.locations = Collections.unmodifiableList(Arrays.asList(locations));
    }

    public TreeLocation getDefaultLocation() {
        return locations.get(0);
    }

    public TreeLocation findLocation(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (TreeLocation location : locations) {
            if (location.getName().equalsIgnoreCase(name)) {
                return location;
            }
        }
        return null;
    }

    /** Closest curated location by straight-line distance; cheap fallback. */
    public TreeLocation getClosestLocation(WorldPoint from) {
        if (from == null) {
            return getDefaultLocation();
        }
        TreeLocation closest = getDefaultLocation();
        int best = Integer.MAX_VALUE;
        for (TreeLocation candidate : locations) {
            int distance = from.distanceTo(candidate.getPoint());
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    /**
     * Fastest curated location by real travel distance (accounts for teleports/shortcuts).
     * Pathfinding is expensive, so single-location stages short-circuit and callers must
     * run this off the client thread and cache the result.
     */
    public TreeLocation getFastestLocation(WorldPoint from,
                                           java.util.function.ToIntBiFunction<WorldPoint, WorldPoint> pathTiles) {
        if (locations.size() == 1) {
            return getDefaultLocation();
        }
        if (from == null || pathTiles == null) {
            return getClosestLocation(from);
        }
        TreeLocation best = null;
        int bestTiles = Integer.MAX_VALUE;
        for (TreeLocation candidate : locations) {
            int tiles = pathTiles.applyAsInt(from, candidate.getPoint());
            if (tiles < bestTiles) {
                bestTiles = tiles;
                best = candidate;
            }
        }
        return best == null ? getClosestLocation(from) : best;
    }

    /** Why this stage can't be trained now, or null when it's good to go. */
    public String lockReason(int wcLevel, boolean membersWorld,
                             Function<Quest, QuestState> questStates,
                             Function<Skill, Integer> skillLevels) {
        if (membersOnly && !membersWorld) {
            return "P2P only";
        }
        if (wcLevel < minLevel) {
            return "Lv " + minLevel;
        }
        return requirement.unmetReason(questStates, skillLevels);
    }

    public boolean isAvailable(int wcLevel, boolean membersWorld,
                               Function<Quest, QuestState> questStates,
                               Function<Skill, Integer> skillLevels) {
        return lockReason(wcLevel, membersWorld, questStates, skillLevels) == null;
    }

    /** Highest auto-ladder stage the player can actually train right now. */
    public static TreeStage bestFor(int wcLevel, boolean membersWorld,
                                    Function<Quest, QuestState> questStates,
                                    Function<Skill, Integer> skillLevels) {
        TreeStage best = TREE;
        for (TreeStage stage : values()) {
            if (!stage.inAutoLadder || stage.minLevel < best.minLevel) {
                continue;
            }
            if (stage.isAvailable(wcLevel, membersWorld, questStates, skillLevels)) {
                best = stage;
            }
        }
        return best;
    }

    /** The next auto-ladder rung up, or null when this is the top. */
    public TreeStage next(boolean membersWorld) {
        TreeStage best = null;
        for (TreeStage stage : values()) {
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
