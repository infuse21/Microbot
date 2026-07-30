package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.ItemID;

/**
 * The Angler's outfit, one entry per equipment slot.
 *
 * <p>Each piece grants Fishing experience (hat 0.4%, top 0.8%, waders 0.6%, boots 0.2%) with
 * a further 0.5% for the complete set, so 2.5% in total. It is obtained from Fishing Trawler
 * or bought from Alry for 100 Molch pearls per piece - which is why the aerial page and this
 * are worth having in the same plugin.</p>
 *
 * <p>The upgraded <em>spirit</em> angler pieces are accepted as equivalents. They are matched
 * by id rather than by name because only some of the names line up: "Spirit angler hat"
 * contains "Angler hat", but the spirit legs are called "Spirit angler legs" while the base
 * item is "Angler waders", so substring matching would miss it.</p>
 */
@Getter
public enum AnglerGear {
    HAT("Angler hat", EquipmentInventorySlot.HEAD, 0.4,
            ItemID.TRAWLER_REWARD_HAT, ItemID.SPIRIT_ANGLER_HAT),
    TOP("Angler top", EquipmentInventorySlot.BODY, 0.8,
            ItemID.TRAWLER_REWARD_TOP, ItemID.SPIRIT_ANGLER_TOP),
    WADERS("Angler waders", EquipmentInventorySlot.LEGS, 0.6,
            ItemID.TRAWLER_REWARD_LEGS, ItemID.SPIRIT_ANGLER_LEGS),
    BOOTS("Angler boots", EquipmentInventorySlot.BOOTS, 0.2,
            ItemID.TRAWLER_REWARD_BOOTS, ItemID.SPIRIT_ANGLER_BOOTS);

    /** Bonus for wearing all four pieces, on top of the individual bonuses. */
    public static final double SET_BONUS_PERCENT = 0.5;

    private final String displayName;
    private final EquipmentInventorySlot slot;
    private final double xpBonusPercent;
    /** Base item id, then any accepted upgrades (spirit angler). */
    private final int[] itemIds;

    AnglerGear(String displayName, EquipmentInventorySlot slot, double xpBonusPercent, int... itemIds) {
        this.displayName = displayName;
        this.slot = slot;
        this.xpBonusPercent = xpBonusPercent;
        this.itemIds = itemIds;
    }

    /**
     * Total bonus for the full set, including the set bonus.
     *
     * <p>Rounded to one decimal place: summing the per-piece doubles drifts
     * (0.4 + 0.8 + 0.6 + 0.2 + 0.5 lands on 2.5000000000000004), which would surface as a
     * nonsense figure anywhere this is displayed.</p>
     */
    public static double fullSetBonusPercent() {
        double total = SET_BONUS_PERCENT;
        for (AnglerGear piece : values()) {
            total += piece.xpBonusPercent;
        }
        return Math.round(total * 10.0) / 10.0;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
