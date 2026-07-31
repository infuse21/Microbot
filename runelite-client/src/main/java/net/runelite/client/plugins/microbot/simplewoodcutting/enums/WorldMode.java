package net.runelite.client.plugins.microbot.simplewoodcutting.enums;

import lombok.Getter;

import java.util.function.BooleanSupplier;

/**
 * Which world type the progression ladder and location picking should assume.
 *
 * <p>{@link #AUTO} reads the world actually logged into, so hopping between a free and a
 * members world switches ladder on its own - which beats a manual tickbox that silently goes
 * stale the moment you hop and then routes you somewhere you can't reach.</p>
 */
@Getter
public enum WorldMode {

    AUTO("Auto (detect world)"),
    MEMBERS("Members"),
    FREE("Free-to-play");

    private final String displayName;

    WorldMode(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Resolve to a plain "are we on a members world" answer.
     *
     * @param detector reads the live world type; only consulted for {@link #AUTO}
     * @return whether members content should be considered available
     */
    public boolean isMembersWorld(BooleanSupplier detector) {
        switch (this) {
            case MEMBERS:
                return true;
            case FREE:
                return false;
            default:
                // An unreadable world resolves to free-to-play on purpose. Guessing
                // "members" would let nearest-location picking send a free account to
                // Catherby; guessing "free" only costs a members account a slower ladder.
                try {
                    return detector != null && detector.getAsBoolean();
                } catch (Exception e) {
                    return false;
                }
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
