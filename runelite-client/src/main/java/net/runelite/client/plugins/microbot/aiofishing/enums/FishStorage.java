package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Optional container that holds caught fish beyond the inventory.
 *
 * <p>Both barrels occupy a single inventory slot and hold 28 raw fish, so a trip lasts
 * roughly twice as long.</p>
 *
 * <p>The barrel only collects while it is <em>open</em>, and it does so automatically: caught
 * fish go straight in while there is room. That has a useful consequence - the script never
 * has to count the contents. While the barrel has space the inventory does not fill up, and
 * once the barrel is full the catch starts landing in the inventory again, so an ordinary
 * "inventory is full" test already means "and the barrel too". All the script must do is make
 * sure the barrel is open.</p>
 *
 * <p>Note the barrel refuses harpoonfish, stackable fish and trophy fish, and can only be
 * emptied at a bank.</p>
 */
@Getter
public enum FishStorage {
    NONE("None", 0, -1, -1),
    FISH_BARREL("Fish barrel", 28, ItemID.FISH_BARREL_CLOSED, ItemID.FISH_BARREL_OPEN),
    FISH_SACK_BARREL("Fish sack barrel", 28,
            ItemID.FISH_SACK_BARREL_CLOSED, ItemID.FISH_SACK_BARREL_OPEN);

    private final String displayName;
    /** Informational: 28 raw fish. Nothing needs to count them - see the class note. */
    private final int capacity;
    /** Id while shut - in this state it collects nothing. */
    private final int closedId;
    /** Id while open - the state that actually auto-collects. */
    private final int openId;

    FishStorage(String displayName, int capacity, int closedId, int openId) {
        this.displayName = displayName;
        this.capacity = capacity;
        this.closedId = closedId;
        this.openId = openId;
    }

    /** Both ids, for "do I have one at all" lookups. */
    public int[] getItemIds() {
        return new int[]{closedId, openId};
    }

    public boolean isEnabled() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
