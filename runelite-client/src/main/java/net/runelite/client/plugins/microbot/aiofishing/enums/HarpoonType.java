package net.runelite.client.plugins.microbot.aiofishing.enums;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Optional special-attack harpoon used on harpoon stages to occasionally catch an
 * extra fish. {@link #NONE} disables spec usage entirely.
 */
@Getter
public enum HarpoonType {
    NONE("None", -1),
    DRAGON_HARPOON("Dragon harpoon", ItemID.DRAGON_HARPOON),
    INFERNAL_HARPOON("Infernal harpoon", ItemID.INFERNAL_HARPOON),
    CRYSTAL_HARPOON("Crystal harpoon", ItemID.CRYSTAL_HARPOON);

    private final String itemName;
    private final int itemId;

    HarpoonType(String itemName, int itemId) {
        this.itemName = itemName;
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return itemName;
    }
}
