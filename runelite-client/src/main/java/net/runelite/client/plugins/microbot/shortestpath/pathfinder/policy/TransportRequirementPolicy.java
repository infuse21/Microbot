package net.runelite.client.plugins.microbot.shortestpath.pathfinder.policy;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class TransportRequirementPolicy {
    private static final String ECTO_TOKEN = "ecto-token";
    private static final Set<Integer> GHOSTSPEAK_ITEMS = Set.of(
            552, 4250, 13113, 13114, 13115);

    private TransportRequirementPolicy() {
    }

    public static boolean completedQuests(Transport transport, List<QuestState> questStateOrder) {
        return transport.getQuests().entrySet().stream()
                .allMatch(entry -> {
                    QuestState playerState = Rs2Player.getQuestState(entry.getKey());
                    QuestState requiredState = entry.getValue();
                    int playerIndex = questStateOrder.indexOf(playerState);
                    int requiredIndex = questStateOrder.indexOf(requiredState);
                    if (requiredIndex < 0 || playerIndex < 0) {
                        return false;
                    }
                    return playerIndex >= requiredIndex;
                });
    }

    public static boolean varbitChecks(Transport transport) {
        return transport.getVarbits().isEmpty()
                || transport.getVarbits().stream()
                .allMatch(varbitCheck -> varbitCheck.matches(Microbot.getVarbitValue(varbitCheck.getVarbitId())));
    }

    public static boolean varplayerChecks(Transport transport) {
        return transport.getVarplayers().isEmpty()
                || transport.getVarplayers().stream()
                .allMatch(varplayerCheck -> varplayerCheck.matches(Microbot.getVarbitPlayerValue(varplayerCheck.getVarplayerId())));
    }

    /**
     * Returns the currency amount that applies to the player's current quest state.
     *
     * <p>The Port Phasmatys barrier rows encode their pre-quest two-token toll only in the
     * interaction action. After Ghosts Ahoy the same barrier is free. Unknown quest state fails
     * closed to the toll so routing never assumes a free crossing from a stale player snapshot.
     */
    public static int currencyAmount(Transport transport) {
        if (!isEctoBarrier(transport)) {
            return transport == null ? 0 : transport.getCurrencyAmount();
        }
        QuestState questState = Microbot.getRs2PlayerStateCache() == null
                ? null : Rs2Player.getQuestState(Quest.GHOSTS_AHOY);
        return currencyAmount(transport, questState);
    }

    static int currencyAmount(Transport transport, QuestState ghostsAhoyState) {
        if (isEctoBarrier(transport)) {
            return ghostsAhoyState == QuestState.FINISHED ? 0 : 2;
        }
        return transport == null ? 0 : transport.getCurrencyAmount();
    }

    public static String currencyName(Transport transport) {
        return isEctoBarrier(transport) ? ECTO_TOKEN
                : transport == null ? "" : transport.getCurrencyName();
    }

    /**
     * Returns item alternatives that apply to the player's current quest state.
     *
     * <p>The pre-quest Port Phasmatys toll also requires an equipped ghostspeak item, but the
     * packaged rows encode only the object action. Keep that conditional requirement outside the
     * shared transport object so post-quest copies of the same row remain free and itemless.
     */
    public static Set<Set<Integer>> itemIdRequirements(Transport transport) {
        QuestState questState = Microbot.getRs2PlayerStateCache() == null
                ? null : Rs2Player.getQuestState(Quest.GHOSTS_AHOY);
        return itemIdRequirements(transport, questState);
    }

    static Set<Set<Integer>> itemIdRequirements(Transport transport, QuestState ghostsAhoyState) {
        if (isEctoBarrier(transport) && ghostsAhoyState != QuestState.FINISHED) {
            return Set.of(GHOSTSPEAK_ITEMS);
        }
        return transport == null || transport.getItemIdRequirements() == null
                ? Collections.emptySet() : transport.getItemIdRequirements();
    }

    /** The duration-two barrier rows describe the paid pre-quest landing only. */
    public static boolean questVariantAvailable(Transport transport) {
        QuestState questState = Microbot.getRs2PlayerStateCache() == null
                ? null : Rs2Player.getQuestState(Quest.GHOSTS_AHOY);
        return questVariantAvailable(transport, questState);
    }

    static boolean questVariantAvailable(Transport transport, QuestState ghostsAhoyState) {
        return !isEctoBarrier(transport) || transport.isQuestLocked()
                || transport.getDuration() != 2 || ghostsAhoyState != QuestState.FINISHED;
    }

    public static Set<Integer> ghostspeakItemIds() {
        return GHOSTSPEAK_ITEMS;
    }

    private static boolean isEctoBarrier(Transport transport) {
        return transport != null
                && transport.getObjectId() == 16105
                && "Pay-toll(2-Ecto)".equalsIgnoreCase(transport.getAction())
                && "Energy Barrier".equalsIgnoreCase(transport.getName());
    }
}
