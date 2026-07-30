package net.runelite.client.plugins.microbot.aiofishing;

import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.aiofishing.enums.AerialCatch;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

import java.util.function.Function;

/**
 * Facts and readiness checks for aerial fishing at Lake Molch.
 *
 * <p>Kept separate from the script so the level gate stays pure and testable, and so the
 * "can I actually do this right now" question has one obvious home.</p>
 *
 * <p>Bait needs no bank trip and is not part of {@link #gearReason()}: king worms lie on the
 * ground beside Alry, and from the first catch onwards the knifed offcuts/chunks <em>are</em>
 * the bait, so the supply sustains itself. The glove still has to be fetched from Alry by
 * hand - that is dialogue the plugin does not attempt - so it is verified and reported.</p>
 */
public final class AerialFishing {

    /** Shore tile the cormorant is thrown from, on Molch Island. */
    public static final WorldPoint MOLCH_ISLAND_SPOT = new WorldPoint(1376, 3629, 0);

    /**
     * Anything that works as bait. Offcuts and chunks come from knifing your own catch,
     * which is what makes the activity self-sustaining once it is running.
     */
    public static final String[] BAIT_NAMES = {"King worm", "Fish offcuts", "Fish chunks"};

    private AerialFishing() {
    }

    /** True while the cormorant is out - the worn glove swaps id when the bird leaves. */
    public static boolean birdIsOut() {
        return Rs2Equipment.isWearing(ItemID.AERIAL_FISHING_GLOVES_BIRD);
    }

    public static boolean hasGloveEquipped() {
        return Rs2Equipment.isWearing(ItemID.AERIAL_FISHING_GLOVES_NO_BIRD)
                || Rs2Equipment.isWearing(ItemID.AERIAL_FISHING_GLOVES_BIRD);
    }

    public static boolean hasBait() {
        return Rs2Inventory.hasItem(BAIT_NAMES);
    }

    public static boolean hasKnife() {
        return Rs2Inventory.hasItem(ItemID.KNIFE);
    }

    /** Ground item that bootstraps the bait cycle, spawning beside Alry. */
    public static final String GROUND_BAIT_NAME = "King worm";
    /** How far from the shore tile to look for worms on the ground. */
    public static final int GROUND_BAIT_RANGE = 20;

    /** Raw catch still needing to be knifed into offcuts. */
    public static boolean hasUncutCatch() {
        return Rs2Inventory.hasItem(AerialCatch.rawItemIds());
    }

    /**
     * Level gate only - pure, so it can be exercised without a client.
     *
     * @return null when the entry levels are met, otherwise a short reason
     */
    public static String levelReason(Function<Skill, Integer> skillLevels) {
        AerialCatch entry = AerialCatch.entry();
        Integer fishing = skillLevels == null ? null : skillLevels.apply(Skill.FISHING);
        Integer hunter = skillLevels == null ? null : skillLevels.apply(Skill.HUNTER);
        if (fishing == null || fishing < entry.getFishingLevel()) {
            return "Needs " + entry.getFishingLevel() + " Fishing";
        }
        if (hunter == null || hunter < entry.getHunterLevel()) {
            return "Needs " + entry.getHunterLevel() + " Hunter";
        }
        return null;
    }

    /**
     * Equipment/inventory gate.
     *
     * <p>"Both hands free" does <em>not</em> mean an empty weapon slot: the cormorant's glove
     * is itself a weapon-slot item. So the glove occupies the weapon slot, and what must be
     * clear is the shield and the gloves slot. Testing the weapon slot for emptiness would
     * reject the very item that makes the activity possible.</p>
     *
     * @return null when ready, otherwise a short actionable reason
     */
    public static String gearReason() {
        if (!hasGloveEquipped()) {
            return "Equip a cormorant's glove";
        }
        if (Rs2Equipment.isWearing(EquipmentInventorySlot.SHIELD)) {
            return "Unequip your shield (hands must be free)";
        }
        if (Rs2Equipment.isWearing(EquipmentInventorySlot.GLOVES)) {
            return "Remove your gloves (hands must be free)";
        }
        if (!hasKnife()) {
            return "Carry a knife to cut the catch";
        }
        return null;
    }

    /** Combined readiness check: levels first, then gear. Null when good to go. */
    public static String unmetReason(Function<Skill, Integer> skillLevels) {
        String level = levelReason(skillLevels);
        return level != null ? level : gearReason();
    }
}
