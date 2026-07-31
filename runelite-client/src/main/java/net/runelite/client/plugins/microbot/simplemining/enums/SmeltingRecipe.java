package net.runelite.client.plugins.microbot.simplemining.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public enum SmeltingRecipe {
    NONE("Off", null, 0, null, 0),
    BRONZE("Bronze (1 copper : 1 tin)", OreStage.COPPER, 1, OreStage.TIN, 1),
    STEEL("Steel (1 iron : 2 coal)", OreStage.IRON, 1, OreStage.COAL, 2),
    MITHRIL("Mithril (1 mithril : 4 coal)", OreStage.MITHRIL, 1, OreStage.COAL, 4),
    ADAMANTITE("Adamantite (1 adamantite : 6 coal)",
            OreStage.ADAMANTITE, 1, OreStage.COAL, 6),
    RUNITE("Runite (1 runite : 8 coal)", OreStage.RUNITE, 1, OreStage.COAL, 8);

    private final String displayName;
    private final OreStage primaryOre;
    private final int primaryRatio;
    private final OreStage secondaryOre;
    private final int secondaryRatio;

    SmeltingRecipe(String displayName, OreStage primaryOre, int primaryRatio,
                   OreStage secondaryOre, int secondaryRatio) {
        this.displayName = displayName;
        this.primaryOre = primaryOre;
        this.primaryRatio = primaryRatio;
        this.secondaryOre = secondaryOre;
        this.secondaryRatio = secondaryRatio;
    }

    public boolean isEnabled() {
        return this != NONE;
    }

    public List<OreStage> getOres() {
        return isEnabled()
                ? Collections.unmodifiableList(Arrays.asList(primaryOre, secondaryOre))
                : Collections.emptyList();
    }

    public boolean contains(OreStage stage) {
        return stage == primaryOre || stage == secondaryOre;
    }

    public int targetFor(OreStage stage, int barsPerCycle) {
        if (stage == primaryOre) {
            return primaryRatio * barsPerCycle;
        }
        if (stage == secondaryOre) {
            return secondaryRatio * barsPerCycle;
        }
        return 0;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
