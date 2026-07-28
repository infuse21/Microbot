package net.runelite.client.plugins.microbot.questhelper.logic;

import net.runelite.api.Quest;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all quests
 */
public class QuestRegistry {
    private static final Map<Integer, IQuest> QUEST_MAP = new HashMap<>();

    static {
        QUEST_MAP.put(Quest.ROMEO__JULIET.getId(), new RomeoAndJuliet());
        QUEST_MAP.put(Quest.RUNE_MYSTERIES.getId(), new RuneMysteries());
        QUEST_MAP.put(Quest.PIRATES_TREASURE.getId(), new PiratesTreasure());
        QUEST_MAP.put(Quest.EAGLES_PEAK.getId(), new EaglesPeak());
        QUEST_MAP.put(Quest.PRIEST_IN_PERIL.getId(), new PriestInPeril());

    }

    /**
     * Get the quest implementation for the given quest id
     * @param questId
     * @return
     */
    public static IQuest getQuest(int questId) {
        IQuest quest = QUEST_MAP.getOrDefault(questId, null);
        return quest;
    }
}
