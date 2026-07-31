package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Everything besides Mining level that gates an {@link OreStage}.
 *
 * <p>The point of this class is to stop the script from travelling to a place it can't
 * mine. A quest-locked stage that isn't unlocked is reported as unavailable *before* any
 * walking happens, so the bot never loops around a locked area.</p>
 *
 * <p>{@code partialOk} supports mining areas that become available before a quest is fully
 * completed.</p>
 */
@Getter
public final class StageRequirement {

    public static final StageRequirement NONE = new StageRequirement(null, false, Collections.emptyMap());

    /** Nullable - no quest gate when null. */
    private final Quest quest;
    /** When true, an in-progress quest is enough; when false it must be finished. */
    private final boolean partialOk;
    /** Non-Mining skill levels required, such as Sailing for island mines. */
    private final Map<Skill, Integer> skills;

    private StageRequirement(Quest quest, boolean partialOk, Map<Skill, Integer> skills) {
        this.quest = quest;
        this.partialOk = partialOk;
        this.skills = skills;
    }

    /** Quest must be fully completed. */
    public static StageRequirement quest(Quest quest) {
        return new StageRequirement(quest, false, Collections.emptyMap());
    }

    /** Quest only needs to be started. */
    public static StageRequirement questStarted(Quest quest) {
        return new StageRequirement(quest, true, Collections.emptyMap());
    }

    /** A single non-Mining skill gate, such as Sailing to reach an island mine. */
    public static StageRequirement skill(Skill skill, int level) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(skill, level);
        return new StageRequirement(null, false, Collections.unmodifiableMap(map));
    }

    /** Two skill gates with no quest requirement. */
    public static StageRequirement skills(Skill a, int levelA, Skill b, int levelB) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(a, levelA);
        map.put(b, levelB);
        return new StageRequirement(null, false, Collections.unmodifiableMap(map));
    }

    /** A quest plus two skill gates. */
    public static StageRequirement questAndSkills(Quest quest, boolean partialOk,
                                                  Skill a, int levelA, Skill b, int levelB) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(a, levelA);
        map.put(b, levelB);
        return new StageRequirement(quest, partialOk, Collections.unmodifiableMap(map));
    }

    public boolean hasQuest() {
        return quest != null;
    }

    /**
     * @param questStates resolves a quest to its state; may return null if unknown
     * @param skillLevels resolves a skill to the player's real level
     * @return null when every requirement is met, otherwise a short human-readable reason
     */
    public String unmetReason(Function<Quest, QuestState> questStates,
                              Function<Skill, Integer> skillLevels) {
        if (quest != null) {
            QuestState state = questStates == null ? null : questStates.apply(quest);
            if (state == null) {
                return partialOk ? "Start " + quest.getName() : "Needs " + quest.getName();
            }
            boolean met = partialOk ? state != QuestState.NOT_STARTED : state == QuestState.FINISHED;
            if (!met) {
                return partialOk ? "Start " + quest.getName() : "Needs " + quest.getName();
            }
        }
        for (Map.Entry<Skill, Integer> entry : skills.entrySet()) {
            Integer level = skillLevels == null ? null : skillLevels.apply(entry.getKey());
            if (level == null || level < entry.getValue()) {
                return "Needs " + entry.getValue() + " " + capitalise(entry.getKey().getName());
            }
        }
        return null;
    }

    private static String capitalise(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
