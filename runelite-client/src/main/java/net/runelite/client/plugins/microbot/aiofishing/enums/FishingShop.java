package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

/**
 * Shops the plugin knows how to restock fishing supplies from.
 *
 * <p>The client has no shop database ({@code Rs2ShopItem} is never populated), so stock
 * lists and coordinates are declared here. Coordinates are the wiki's map coordinates for
 * each shopkeeper; stock lists are declared here too. They are treated as a <em>hint</em>, not gospel: the script
 * always confirms with {@code Rs2Shop.hasStock} once the shop is open and stops cleanly
 * if the item isn't actually there, rather than looping.</p>
 */
@Getter
public enum FishingShop {

    /**
     * Gerrant's Fishy Business, Port Sarim. Free-to-play, and the only known source of
     * fly fishing rods. Note it does NOT stock big fishing nets - Tynan's does.
     * Stock per the OSRS wiki: feather 1000, fishing bait 1500, harpoon/lobster pot 2 each.
     */
    GERRANT("Gerrant's Fishy Business", "Gerrant", new WorldPoint(3011, 3230, 0), false,
            StageRequirement.NONE,
            "Small fishing net", "Fishing rod", "Fly fishing rod",
            "Harpoon", "Lobster pot", "Fishing bait", "Feather"),

    /**
     * Tynan's Fishing Supplies, Port Piscarilius (just east of the bank). Members only,
     * and the only known source of sandworms for anglerfish. Also stocks big fishing nets.
     */
    TYNAN("Tynan's Fishing Supplies", "Tynan", new WorldPoint(1841, 3786, 0), true,
            StageRequirement.NONE,
            "Small fishing net", "Big fishing net", "Fishing rod",
            "Harpoon", "Lobster pot", "Fishing bait", "Sandworms"),

    /** Harry's Fishing Shop, Catherby - right beside the Catherby fishing spots. */
    HARRY("Harry's Fishing Shop", "Harry", new WorldPoint(2830, 3447, 0), true,
            StageRequirement.NONE,
            "Small fishing net", "Big fishing net", "Fishing rod",
            "Harpoon", "Lobster pot", "Fishing bait"),

    /**
     * Fishing Guild Shop (Roachey), inside the guild - the most complete stock list, but
     * the guild itself needs 68 Fishing, so that gate is declared here.
     */
    ROACHEY("Fishing Guild Shop", "Roachey", new WorldPoint(2595, 3401, 0), true,
            StageRequirement.skill(Skill.FISHING, 68),
            "Small fishing net", "Big fishing net", "Fishing rod", "Fly fishing rod",
            "Harpoon", "Lobster pot", "Fishing bait", "Feather"),

    /** Fremennik Fish Monger, Rellekka. Requires The Fremennik Trials to trade. */
    FREMENNIK("Fremennik Fish Monger", "Fish monger", new WorldPoint(2647, 3676, 0), true,
            StageRequirement.quest(Quest.THE_FREMENNIK_TRIALS),
            "Small fishing net", "Big fishing net", "Fishing rod", "Fly fishing rod",
            "Harpoon", "Lobster pot", "Fishing bait", "Feather");

    private final String displayName;
    /** NPC to "Trade" with; matched loosely by Rs2Shop.openShop. */
    private final String npcName;
    private final WorldPoint location;
    private final boolean membersOnly;
    /** Quest/skill gate to actually reach and use this shop. */
    private final StageRequirement requirement;
    private final List<String> stock;

    FishingShop(String displayName, String npcName, WorldPoint location,
                boolean membersOnly, StageRequirement requirement, String... stock) {
        this.displayName = displayName;
        this.npcName = npcName;
        this.location = location;
        this.membersOnly = membersOnly;
        this.requirement = requirement;
        this.stock = Collections.unmodifiableList(Arrays.asList(stock));
    }

    /** Why this shop is unusable right now, or null when it's reachable. */
    public String lockReason(boolean membersWorld,
                             Function<Quest, QuestState> questStates,
                             Function<Skill, Integer> skillLevels) {
        if (membersOnly && !membersWorld) {
            return "P2P only";
        }
        return requirement.unmetReason(questStates, skillLevels);
    }

    public boolean sells(String itemName) {
        return stock.stream().anyMatch(s -> s.equalsIgnoreCase(itemName));
    }

    /**
     * Every shop that stocks this item AND that the player can actually reach - quest and
     * skill gates are applied here so we never set off towards a shop we can't enter.
     */
    public static List<FishingShop> findAllFor(String itemName, boolean membersWorld,
                                               Function<Quest, QuestState> questStates,
                                               Function<Skill, Integer> skillLevels) {
        if (itemName == null) {
            return Collections.emptyList();
        }
        List<FishingShop> matches = new ArrayList<>();
        for (FishingShop shop : values()) {
            if (shop.lockReason(membersWorld, questStates, skillLevels) != null) {
                continue;
            }
            if (shop.sells(itemName)) {
                matches.add(shop);
            }
        }
        return matches;
    }

    /** The first reachable shop that stocks this item, or null when there is none. */
    public static FishingShop findFor(String itemName, boolean membersWorld,
                                      Function<Quest, QuestState> questStates,
                                      Function<Skill, Integer> skillLevels) {
        List<FishingShop> matches = findAllFor(itemName, membersWorld, questStates, skillLevels);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * The quickest shop stocking this item, measured by real path distance so a member
     * fishing in Zeah isn't sent to Port Sarim for something Tynan's also sells.
     *
     * @param from      where we're travelling from
     * @param pathTiles travel-tile measure; may return Integer.MAX_VALUE when unreachable
     */
    public static FishingShop findNearest(String itemName, boolean membersWorld,
                                          Function<Quest, QuestState> questStates,
                                          Function<Skill, Integer> skillLevels,
                                          WorldPoint from,
                                          ToIntBiFunction<WorldPoint, WorldPoint> pathTiles) {
        List<FishingShop> matches = findAllFor(itemName, membersWorld, questStates, skillLevels);
        if (matches.size() <= 1 || from == null || pathTiles == null) {
            return matches.isEmpty() ? null : matches.get(0);
        }
        FishingShop best = null;
        int bestTiles = Integer.MAX_VALUE;
        for (FishingShop shop : matches) {
            int tiles = pathTiles.applyAsInt(from, shop.getLocation());
            if (tiles < bestTiles) {
                bestTiles = tiles;
                best = shop;
            }
        }
        return best == null ? matches.get(0) : best;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
