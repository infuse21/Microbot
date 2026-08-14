package net.runelite.client.plugins.microbot.util.magic;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Getter
@RequiredArgsConstructor
public enum Rs2Tome {

    NONE(0, Collections.emptyList()),
    TOME_OF_FIRE(ItemID.TOME_OF_FIRE, List.of(Runes.FIRE)),
    TOME_OF_WATER(ItemID.TOME_OF_WATER, List.of(Runes.WATER)),
    TOME_OF_EARTH(ItemID.TOME_OF_EARTH, List.of(Runes.EARTH));


    private final int itemID;
    private final List<Runes> runes;

    private static final Map<Integer, Rs2Tome> BY_ITEM_ID = Arrays.stream(values())
            .filter(t -> t != NONE)
            .collect(Collectors.toMap(Rs2Tome::getItemID, Function.identity()));

    static Rs2Tome byItemId(int itemID) {
        return BY_ITEM_ID.getOrDefault(itemID, NONE);
    }
}
