package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Everything besides Fishing level that gates a {@link FishingStage}.
 *
 * <p>The point of this class is to stop the script from travelling to a place it can't
 * fish. A quest-locked stage that isn't unlocked is reported as unavailable *before* any
 * walking happens, so the bot never loops around a locked area.</p>
 *
 * <p>Some fish only need the quest <em>started</em> rather than finished (Swan Song for
 * monkfish, Heroes' Quest for lava eels), which is what {@code partialOk} expresses.</p>
 */
@Getter
public final class StageRequirement {

    /** Sentinel for "no varbit gate". */
    private static final int NO_VARBIT = -1;

    public static final StageRequirement NONE =
            new StageRequirement(null, false, Collections.emptyMap(), NO_VARBIT, 0, null);

    /** Nullable - no quest gate when null. */
    private final Quest quest;
    /** When true, an in-progress quest is enough; when false it must be finished. */
    private final boolean partialOk;
    /** Non-Fishing skill levels required (e.g. Agility/Strength for barbarian fishing). */
    private final Map<Skill, Integer> skills;
    /**
     * Varbit that must reach {@link #varbitMin}, or {@link #NO_VARBIT} for no gate.
     *
     * <p>Needed where a miniquest chapter has its own progress varbit. The whole-miniquest
     * {@link QuestState} is too coarse: finishing the Barbarian Training <em>firemaking</em>
     * chapter flips the miniquest to IN_PROGRESS while barbarian fishing is still locked,
     * so gating on the quest alone would wrongly pass and send us to Otto's for nothing.</p>
     */
    private final int varbitId;
    private final int varbitMin;
    /** Human-readable, actionable reason shown when the varbit gate is unmet. */
    private final String varbitLabel;

    private StageRequirement(Quest quest, boolean partialOk, Map<Skill, Integer> skills,
                             int varbitId, int varbitMin, String varbitLabel) {
        this.quest = quest;
        this.partialOk = partialOk;
        this.skills = skills;
        this.varbitId = varbitId;
        this.varbitMin = varbitMin;
        this.varbitLabel = varbitLabel;
    }

    /** Quest must be fully completed. */
    public static StageRequirement quest(Quest quest) {
        return new StageRequirement(quest, false, Collections.emptyMap(), NO_VARBIT, 0, null);
    }

    /** Quest only needs to be started. */
    public static StageRequirement questStarted(Quest quest) {
        return new StageRequirement(quest, true, Collections.emptyMap(), NO_VARBIT, 0, null);
    }

    /** A single skill gate, e.g. 68 Fishing to enter the Fishing Guild. */
    public static StageRequirement skill(Skill skill, int level) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(skill, level);
        return new StageRequirement(null, false, Collections.unmodifiableMap(map), NO_VARBIT, 0, null);
    }

    /** Skill gates only. */
    public static StageRequirement skills(Skill a, int levelA, Skill b, int levelB) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(a, levelA);
        map.put(b, levelB);
        return new StageRequirement(null, false, Collections.unmodifiableMap(map), NO_VARBIT, 0, null);
    }

    /** Quest plus skill gates. */
    public static StageRequirement questAndSkills(Quest quest, boolean partialOk,
                                                  Skill a, int levelA, Skill b, int levelB) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(a, levelA);
        map.put(b, levelB);
        return new StageRequirement(quest, partialOk, Collections.unmodifiableMap(map), NO_VARBIT, 0, null);
    }

    /**
     * A chapter-progress varbit plus two skill gates - used by barbarian fishing, which
     * needs Otto to have taught the technique (BRUT_FISHING_R >= 1) plus Agility/Strength.
     *
     * @param label actionable text shown when the gate is unmet, e.g. "Learn barb fishing from Otto"
     */
    public static StageRequirement varbitAndSkills(int varbitId, int minValue, String label,
                                                   Skill a, int levelA, Skill b, int levelB) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(a, levelA);
        map.put(b, levelB);
        return new StageRequirement(null, false, Collections.unmodifiableMap(map),
                varbitId, minValue, label);
    }

    public boolean hasQuest() {
        return quest != null;
    }

    public boolean hasVarbit() {
        return varbitId != NO_VARBIT;
    }

    /** True when this gate needs an unlock the plugin cannot perform itself (quest/miniquest). */
    public boolean hasUnlockGate() {
        return hasQuest() || hasVarbit();
    }

    /**
     * @param questStates resolves a quest to its state; may return null if unknown
     * @param skillLevels resolves a skill to the player's real level
     * @return null when every requirement is met, otherwise a short human-readable reason
     */
    public String unmetReason(Function<Quest, QuestState> questStates,
                              Function<Skill, Integer> skillLevels) {
        return unmetReason(questStates, skillLevels, null);
    }

    /**
     * @param questStates  resolves a quest to its state; may return null if unknown
     * @param skillLevels  resolves a skill to the player's real level
     * @param varbitValues resolves a varbit id to its value; null means "cannot read", which
     *                     is treated as unmet so we fail safe and never travel to a locked area
     * @return null when every requirement is met, otherwise a short human-readable reason
     */
    public String unmetReason(Function<Quest, QuestState> questStates,
                              Function<Skill, Integer> skillLevels,
                              IntUnaryOperator varbitValues) {
        if (hasVarbit()) {
            if (varbitValues == null) {
                return varbitLabel;
            }
            int value;
            try {
                value = varbitValues.applyAsInt(varbitId);
            } catch (Exception e) {
                return varbitLabel; // unreadable -> treat as locked
            }
            if (value < varbitMin) {
                return varbitLabel;
            }
        }
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
