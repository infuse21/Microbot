package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Arrays;

/**
 * Woodcutting axes, ordered worst to best. The script wields the best axe the player
 * both owns and meets the Woodcutting level for - a better axe chops faster but is never
 * required, so we degrade gracefully to whatever is available.
 */
@Getter
public enum AxeType {
    BRONZE("Bronze axe", ItemID.BRONZE_AXE, 1),
    IRON("Iron axe", ItemID.IRON_AXE, 1),
    STEEL("Steel axe", ItemID.STEEL_AXE, 6),
    BLACK("Black axe", ItemID.BLACK_AXE, 11),
    MITHRIL("Mithril axe", ItemID.MITHRIL_AXE, 21),
    ADAMANT("Adamant axe", ItemID.ADAMANT_AXE, 31),
    RUNE("Rune axe", ItemID.RUNE_AXE, 41),
    DRAGON("Dragon axe", ItemID.DRAGON_AXE, 61),
    INFERNAL("Infernal axe", ItemID.INFERNAL_AXE, 61),
    CRYSTAL("Crystal axe", ItemID.CRYSTAL_AXE, 71);

    private final String itemName;
    private final int itemId;
    /** Woodcutting level needed to chop with this axe. */
    private final int levelRequired;

    AxeType(String itemName, int itemId, int levelRequired) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.levelRequired = levelRequired;
    }

    public boolean isHeld() {
        return Rs2Equipment.isWearing(itemName) || Rs2Inventory.hasItem(itemName);
    }

    public boolean meetsLevel() {
        return Rs2Player.getRealSkillLevel(Skill.WOODCUTTING) >= levelRequired;
    }

    /** Best axe already equipped or in the inventory that we can chop with, or null. */
    public static AxeType bestHeld() {
        return Arrays.stream(values())
                .filter(AxeType::meetsLevel)
                .filter(AxeType::isHeld)
                .reduce((a, b) -> b) // last match = best, since values() is worst-to-best
                .orElse(null);
    }

    @Override
    public String toString() {
        return itemName;
    }
}
