package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Arrays;

/**
 * Pickaxes, worst to best. The script uses the best one the player owns and can wield - a
 * better pickaxe mines faster but is never required, so it degrades to whatever is available.
 *
 * <p>Note the level shown is the <em>Mining</em> requirement to use it. Several also carry an
 * Attack requirement to <em>equip</em>, which is why an unwieldable pickaxe still works from
 * the inventory; the script therefore accepts either.</p>
 */
@Getter
public enum PickaxeType {
    BRONZE("Bronze pickaxe", ItemID.BRONZE_PICKAXE, 1),
    IRON("Iron pickaxe", ItemID.IRON_PICKAXE, 1),
    STEEL("Steel pickaxe", ItemID.STEEL_PICKAXE, 6),
    BLACK("Black pickaxe", ItemID.BLACK_PICKAXE, 11),
    MITHRIL("Mithril pickaxe", ItemID.MITHRIL_PICKAXE, 21),
    ADAMANT("Adamant pickaxe", ItemID.ADAMANT_PICKAXE, 31),
    RUNE("Rune pickaxe", ItemID.RUNE_PICKAXE, 41),
    DRAGON("Dragon pickaxe", ItemID.DRAGON_PICKAXE, 61),
    INFERNAL("Infernal pickaxe", ItemID.INFERNAL_PICKAXE, 61),
    CRYSTAL("Crystal pickaxe", ItemID.CRYSTAL_PICKAXE, 71);

    private final String itemName;
    private final int itemId;
    /** Mining level needed to use it. */
    private final int levelRequired;

    PickaxeType(String itemName, int itemId, int levelRequired) {
        this.itemName = itemName;
        this.itemId = itemId;
        this.levelRequired = levelRequired;
    }

    /** Held in the inventory or worn - a pickaxe mines from either. */
    public boolean isHeld() {
        return Rs2Equipment.isWearing(itemId) || Rs2Inventory.hasItem(itemId);
    }

    public boolean meetsLevel() {
        return Rs2Player.getRealSkillLevel(Skill.MINING) >= levelRequired;
    }

    /** Best pickaxe already held that the player can actually mine with, or null. */
    public static PickaxeType bestHeld() {
        return Arrays.stream(values())
                .filter(PickaxeType::meetsLevel)
                .filter(PickaxeType::isHeld)
                .reduce((a, b) -> b) // values() is worst-to-best, so the last match wins
                .orElse(null);
    }

    @Override
    public String toString() {
        return itemName;
    }
}
