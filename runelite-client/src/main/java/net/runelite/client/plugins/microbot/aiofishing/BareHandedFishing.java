package net.runelite.client.plugins.microbot.aiofishing;

import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;

import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Bare-handed fishing, the harpoon half of the Barbarian Training miniquest.
 *
 * <p>Otto teaches you to catch harpoon fish with your hands, which needs 20 more Fishing
 * levels than the harpoon equivalent but requires <em>no equipment at all</em>. That last
 * point is the useful part here: when it is active, harpoon stages stop needing a harpoon,
 * which also frees the weapon slot.</p>
 *
 * <p>Gated on {@link VarbitID#BRUT_FISHING_S} - the <em>harpoon</em> chapter - not the rod
 * chapter used by barbarian rod fishing. They are separate lessons from the same NPC, so
 * having learned one says nothing about the other.</p>
 */
public final class BareHandedFishing {

    /** Otto has taught the technique once this chapter varbit has started. */
    private static final int CHAPTER_STARTED = 1;

    /** Entry requirement: tuna and harpoonfish. */
    public static final int MIN_FISHING = 55;
    public static final int MIN_STRENGTH = 35;

    private BareHandedFishing() {
    }

    /**
     * Why bare-handed fishing is unavailable, or null when it can be used.
     *
     * @param varbitValues resolves a varbit id; null is treated as locked so we fail safe
     */
    public static String unmetReason(Function<Skill, Integer> skillLevels,
                                     IntUnaryOperator varbitValues) {
        if (varbitValues == null) {
            return "Learn bare-handed fishing from Otto";
        }
        int chapter;
        try {
            chapter = varbitValues.applyAsInt(VarbitID.BRUT_FISHING_S);
        } catch (Exception e) {
            return "Learn bare-handed fishing from Otto";
        }
        if (chapter < CHAPTER_STARTED) {
            return "Learn bare-handed fishing from Otto";
        }
        Integer fishing = skillLevels == null ? null : skillLevels.apply(Skill.FISHING);
        if (fishing == null || fishing < MIN_FISHING) {
            return "Needs " + MIN_FISHING + " Fishing";
        }
        Integer strength = skillLevels == null ? null : skillLevels.apply(Skill.STRENGTH);
        if (strength == null || strength < MIN_STRENGTH) {
            return "Needs " + MIN_STRENGTH + " Strength";
        }
        return null;
    }

    public static boolean isAvailable(Function<Skill, Integer> skillLevels,
                                      IntUnaryOperator varbitValues) {
        return unmetReason(skillLevels, varbitValues) == null;
    }
}
