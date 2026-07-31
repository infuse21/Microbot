package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.Skill;

import java.util.function.Function;

/**
 * An in-inventory step that turns a raw catch into the thing you actually wanted.
 *
 * <p>Two fish are worthless as caught: sacred eels have to be dissected with a knife for
 * Zulrah's scales, and infernal eels cracked open with a hammer for tokkul, lava scale
 * shards and onyx bolt tips. Both yield stackable output, so processing a full inventory
 * frees the slots again and fishing resumes without a bank trip.</p>
 *
 * <h2>Why the skill gate lives here and not in StageRequirement</h2>
 * Dissecting a sacred eel needs 72 Cooking, but <em>catching</em> one does not. A gate in
 * {@link StageRequirement} would lock the whole stage and refuse to travel; here it only
 * downgrades processing to "skip", so a sub-72 account still fishes the eels and banks
 * them whole.
 */
@Getter
public enum CatchProcessing {

    /** No processing - the catch is banked or dropped as-is. */
    NONE(null, null, null, null, 0),

    /** Knife on a sacred eel -&gt; Zulrah's scales. Needs 72 Cooking. */
    DISSECT("Knife", "Sacred eel", "Dissecting eels", Skill.COOKING, 72),

    /** Hammer on an infernal eel -&gt; tokkul, lava scale shards, onyx bolt tips. */
    CRACK("Hammer", "Infernal eel", "Cracking eels", null, 0);

    /** Item used on the catch; withdrawn during gearing while this step is active. Nullable. */
    private final String toolName;
    /** The raw catch this step consumes. Nullable. */
    private final String rawItemName;
    /** Status text shown while processing. Nullable. */
    private final String label;
    /** Skill gate, or null when there isn't one. */
    private final Skill gateSkill;
    private final int gateLevel;

    CatchProcessing(String toolName, String rawItemName, String label,
                    Skill gateSkill, int gateLevel) {
        this.toolName = toolName;
        this.rawItemName = rawItemName;
        this.label = label;
        this.gateSkill = gateSkill;
        this.gateLevel = gateLevel;
    }

    public boolean isEnabled() {
        return toolName != null;
    }

    /**
     * Why this step can't run right now, or null when it can.
     *
     * <p>An unreadable skill level is treated as unmet, which skips processing and banks the
     * catch whole - the harmless direction, unlike attempting to use a tool we may not need.</p>
     */
    public String unmetReason(Function<Skill, Integer> skillLevels) {
        if (!isEnabled() || gateSkill == null) {
            return null;
        }
        Integer level;
        try {
            level = skillLevels == null ? null : skillLevels.apply(gateSkill);
        } catch (Exception e) {
            level = null;
        }
        if (level == null || level < gateLevel) {
            return "Needs " + gateLevel + " " + capitalise(gateSkill.getName());
        }
        return null;
    }

    /** True when this step exists and the player meets its gate. */
    public boolean isUsable(Function<Skill, Integer> skillLevels) {
        return isEnabled() && unmetReason(skillLevels) == null;
    }

    private static String capitalise(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
