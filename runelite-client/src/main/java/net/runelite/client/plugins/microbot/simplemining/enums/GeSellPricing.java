package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;

/**
 * How to price a Grand Exchange sell offer for the catch.
 *
 * <p>{@link #ADAPTIVE} is the default because a flat market-price offer frequently sits
 * unfilled, which ties up a GE slot and leaves the stack unsold indefinitely. Adaptive
 * starts at the live wiki price and only undercuts when an offer fails to fill, so it
 * doesn't give away value up front but still clears eventually.</p>
 */
@Getter
public enum GeSellPricing {
    ADAPTIVE("Market, undercut if unsold"),
    MARKET("Market price only"),
    CUSTOM("Fixed price");

    private final String displayName;

    GeSellPricing(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
