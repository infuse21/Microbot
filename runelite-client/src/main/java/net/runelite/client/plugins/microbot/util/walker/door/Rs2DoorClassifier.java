package net.runelite.client.plugins.microbot.util.walker.door;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.microbot.Microbot;

/**
 * Stateless classification of door/gate objects: whether a name looks door-like, which menu
 * action to use to walk through (never a close/shut), and whether a composition is in the
 * open state (only close/shut actions). Extracted from {@code Rs2Walker} — pure string/action
 * heuristics over names and {@link ObjectComposition}, no walker state.
 */
public final class Rs2DoorClassifier {

    private static final String[] DOOR_LIKE_NAME_FRAGMENTS = {
            "door", "gate", "barrier", "stile", "portcullis", "archway", "cattlegate", "fence"
    };

    /** {@code fence} must be whole-word — substring matches {@code defence} ("fence" inside) otherwise. */
    private static final Pattern FENCE_AS_WORD = Pattern.compile("\\bfence\\b", Pattern.CASE_INSENSITIVE);

    /** Lower index = higher priority when multiple actions match (prefix, ASCII lower). */
    private static final List<String> DOOR_ACTION_PRIORITY = List.of(
            "pay-toll", "pick-lock", "walk-through", "go-through", "open", "pass", "enter",
            "push", "climb-over", "climb-through", "squeeze-through", "cross", "force", "exit"
    );

    private Rs2DoorClassifier() {
    }

    public static boolean isNullOrPlaceholderObjectName(String name) {
        if (name == null) {
            return true;
        }
        String t = name.trim();
        return t.isEmpty() || "null".equalsIgnoreCase(t);
    }

    public static int doorActionPriorityIndex(String action) {
        if (action == null) {
            return Integer.MAX_VALUE;
        }
        String al = action.toLowerCase(Locale.ROOT);
        for (int i = 0; i < DOOR_ACTION_PRIORITY.size(); i++) {
            if (al.startsWith(DOOR_ACTION_PRIORITY.get(i))) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Walker must never choose menu actions that close an open door/gate. */
    public static boolean isDoorCloseOrShutAction(String action) {
        if (action == null) {
            return false;
        }
        String al = action.toLowerCase(Locale.ROOT).trim();
        return al.startsWith("close") || al.startsWith("shut");
    }

    /** True when every non-null action is Close/Shut (typical open-door state). */
    public static boolean doorCompositionSpecifiesOnlyCloseOrShut(ObjectComposition comp) {
        CompositionSnapshot snapshot = snapshot(comp);
        return snapshot != null && doorCompositionSpecifiesOnlyCloseOrShut(snapshot.actions);
    }

    static boolean doorCompositionSpecifiesOnlyCloseOrShut(String[] actions) {
        if (actions == null) {
            return false;
        }
        boolean sawNonNull = false;
        for (String a : actions) {
            if (a == null) {
                continue;
            }
            sawNonNull = true;
            if (!isDoorCloseOrShutAction(a)) {
                return false;
            }
        }
        return sawNonNull;
    }

    /**
     * Best door action for walking through, excluding close/shut. {@code null} if none
     * (empty defs or only close/shut).
     */
    public static String pickWalkDoorAction(ObjectComposition comp) {
        CompositionSnapshot snapshot = snapshot(comp);
        return snapshot == null ? null : pickWalkDoorAction(snapshot.actions);
    }

    static String pickWalkDoorAction(String[] actions) {
        if (actions == null) {
            return null;
        }
        return Arrays.stream(actions)
                .filter(Objects::nonNull)
                .filter(a -> !isDoorCloseOrShutAction(a))
                .min(Comparator.comparingInt(Rs2DoorClassifier::doorActionPriorityIndex))
                .orElse(null);
    }

    public static boolean isDoorLikeGameObjectName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        for (String f : DOOR_LIKE_NAME_FRAGMENTS) {
            if ("fence".equals(f)) {
                if (FENCE_AS_WORD.matcher(n).find()) {
                    return true;
                }
            } else if (n.contains(f)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a (real, non-impostor) composition exposes one of {@code doorActions}. */
    public static boolean isDoorComposition(ObjectComposition comp, List<String> doorActions) {
        CompositionSnapshot snapshot = snapshot(comp);
        if (snapshot == null || snapshot.hasImpostors
                || isNullOrPlaceholderObjectName(snapshot.name) || snapshot.actions == null) {
            return false;
        }
        return getDoorAction(snapshot.actions, doorActions) != null;
    }

    /** The highest-priority matching {@code doorActions} entry the composition exposes, or null. */
    public static String getDoorAction(ObjectComposition comp, List<String> doorActions) {
        CompositionSnapshot snapshot = snapshot(comp);
        return snapshot == null ? null : getDoorAction(snapshot.actions, doorActions);
    }

    static String getDoorAction(String[] actions, List<String> doorActions) {
        if (actions == null) {
            return null;
        }
        return Arrays.stream(actions)
                .filter(Objects::nonNull)
                .filter(act -> doorActions.stream().anyMatch(dact -> act.toLowerCase().startsWith(dact.toLowerCase())))
                .min(Comparator.comparing(act -> doorActions.indexOf(doorActions.stream()
                        .filter(dact -> act.toLowerCase().startsWith(dact.toLowerCase()))
                        .findFirst()
                        .orElse(""))))
                .orElse(null);
    }

    private static CompositionSnapshot snapshot(ObjectComposition comp) {
        if (comp == null) {
            return null;
        }
        return Microbot.getClientThread().runOnClientThreadOptional(() ->
                new CompositionSnapshot(comp.getName(), comp.getActions(),
                        comp.getImpostorIds() != null)).orElse(null);
    }

    private static final class CompositionSnapshot {
        private final String name;
        private final String[] actions;
        private final boolean hasImpostors;

        private CompositionSnapshot(String name, String[] actions, boolean hasImpostors) {
            this.name = name;
            this.actions = actions;
            this.hasImpostors = hasImpostors;
        }
    }
}
