package net.runelite.client.plugins.microbot.aiofishing;

import net.runelite.client.plugins.microbot.aiofishing.enums.FishingStage;
import net.runelite.client.plugins.skillcalculator.skills.FishingAction;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns Fishing XP into a catch count.
 *
 * <h2>Why XP rather than counting fish</h2>
 * Counting the fish themselves is surprisingly unreliable. Watching the inventory misses
 * everything when a fish barrel is open, because the catch is absorbed straight into the
 * barrel and the inventory never changes. Watching the chat line works, but the catch
 * message is spam-filtered and a player who has that filter off records nothing. XP is
 * always observable, so the count is derived from it instead.
 *
 * <h2>Which XP value</h2>
 * XP per catch comes from {@link FishingAction} - the same table the skill calculator uses -
 * matched to a stage by name, so there is no second copy of the numbers to drift.
 *
 * <p>Stages that yield more than one fish (shrimp <em>and</em> anchovies, trout
 * <em>and</em> salmon) have no single answer, so the <b>lowest-level</b> catch is used: that
 * is the one you land most often, which keeps the estimate close. It is an estimate, and
 * mixed stages will read slightly high once the rarer, higher-XP fish start appearing.</p>
 */
final class CatchEstimate {

    /** Cached per stage - the lookup scans an enum and never changes at runtime. */
    private static final Map<FishingStage, Float> XP_PER_CATCH = new EnumMap<>(FishingStage.class);

    /**
     * Catches {@link FishingAction} does not carry, in constant-name form.
     *
     * <p>Only lava eels so far: they sit behind Heroes' Quest, which the skill calculator
     * does not model, so the table has no row for them. Value taken from the wiki (53
     * Fishing, 60 xp). A stage landing here would otherwise report no catches at all, which
     * is how this gap was found.</p>
     */
    private static final Map<String, Float> UNLISTED_XP = Map.of("LAVA_EEL", 60f);

    private CatchEstimate() {
    }

    /**
     * XP awarded by the fish this stage lands most often, or 0 when nothing matches - in
     * which case callers should show no estimate rather than divide by zero.
     */
    static synchronized float xpPerCatch(FishingStage stage) {
        return XP_PER_CATCH.computeIfAbsent(stage, CatchEstimate::resolve);
    }

    /** Catches implied by an XP delta. Zero when the stage has no usable XP value. */
    static int fromXp(FishingStage stage, int xpGained) {
        float xpPer = xpPerCatch(stage);
        if (xpPer <= 0f || xpGained <= 0) {
            return 0;
        }
        return Math.round(xpGained / xpPer);
    }

    private static float resolve(FishingStage stage) {
        FishingAction best = null;
        for (String catchName : stage.getCatchItemNames()) {
            FishingAction action = findAction(catchName);
            if (action != null && (best == null || action.getLevel() < best.getLevel())) {
                best = action;
            }
        }
        if (best != null) {
            return best.getXp();
        }
        // Nothing in the shared table - fall back to the values we carry ourselves.
        for (String catchName : stage.getCatchItemNames()) {
            Float unlisted = UNLISTED_XP.get(constantName(catchName));
            if (unlisted != null) {
                return unlisted;
            }
        }
        return 0f;
    }

    /** "Raw shrimps" -&gt; "RAW_SHRIMPS", so names can be matched against enum constants. */
    private static String constantName(String catchName) {
        return catchName == null ? "" : catchName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    /**
     * Match a catch name to its XP row.
     *
     * <p>Matched on the enum constant rather than the item id so no ItemManager (and so no
     * client thread) is involved. The stage names the item as the game does ("Raw shrimps",
     * "Slimy eel"); the enum is that name in constant form, sometimes with a {@code RAW_}
     * prefix the stage name omits - so both spellings are tried.</p>
     */
    private static FishingAction findAction(String catchName) {
        if (catchName == null || catchName.isEmpty()) {
            return null;
        }
        String key = constantName(catchName);
        for (FishingAction action : FishingAction.values()) {
            String name = action.name();
            if (name.equals(key) || name.equals("RAW_" + key)) {
                return action;
            }
        }
        return null;
    }
}
