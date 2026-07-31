package net.runelite.client.plugins.microbot.aiofishing.enums;

/**
 * Which auto-progression ladder a {@link FishingStage} belongs to.
 *
 * <p>Free and members accounts want genuinely different routes, not the same list with the
 * members entries removed. A members account goes Shrimp -&gt; Big Net at 16, because big net
 * carries it all the way to lobsters at 40. Free-to-play cannot use big net at all, so it
 * fly-fishes trout instead - and a members account should <em>not</em> fly-fish, because
 * that would mean re-gearing and crossing the map for four levels before going back to the
 * coast.</p>
 *
 * <p>This replaces what used to be a plain "in the ladder" boolean plus two hardcoded
 * exceptions in {@code isAutoStage}. Encoding it as data means adding a stage to one ladder
 * is a single field rather than another special case.</p>
 */
public enum AutoLadder {

    /** Manual catalogue only - never auto-selected. */
    NONE,
    /** On both ladders: the fish trains well on either world type. */
    BOTH,
    /** Members ladder only. */
    P2P,
    /** Free-to-play ladder only - members have something better at this level. */
    F2P;

    /** Whether this rung is part of the ladder for the given world type. */
    public boolean appliesTo(boolean membersWorld) {
        switch (this) {
            case BOTH:
                return true;
            case P2P:
                return membersWorld;
            case F2P:
                return !membersWorld;
            default:
                return false;
        }
    }
}
