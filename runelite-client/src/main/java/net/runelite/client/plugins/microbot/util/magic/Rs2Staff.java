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
public enum Rs2Staff {

    NONE(0, Collections.emptyList(), 0, 0, false),
    STAFF_OF_AIR(ItemID.STAFF_OF_AIR, List.of(Runes.AIR), 0, 0, false),
    STAFF_OF_WATER(ItemID.STAFF_OF_WATER, List.of(Runes.WATER), 0, 0, false),
    STAFF_OF_EARTH(ItemID.STAFF_OF_EARTH, List.of(Runes.EARTH), 0, 0, false),
    STAFF_OF_FIRE(ItemID.STAFF_OF_FIRE, List.of(Runes.FIRE), 0, 0, false),
    AIR_BATTLESTAFF(ItemID.AIR_BATTLESTAFF, List.of(Runes.AIR), 30, 30, true),
    WATER_BATTLESTAFF(ItemID.WATER_BATTLESTAFF, List.of(Runes.WATER), 30, 30, true),
    EARTH_BATTLESTAFF(ItemID.EARTH_BATTLESTAFF, List.of(Runes.EARTH), 30, 30, true),
    FIRE_BATTLESTAFF(ItemID.FIRE_BATTLESTAFF, List.of(Runes.FIRE), 30, 30, true),
    DUST_BATTLESTAFF(ItemID.DUST_BATTLESTAFF, List.of(Runes.AIR, Runes.EARTH), 30, 30, true),
    LAVA_BATTLESTAFF(ItemID.LAVA_BATTLESTAFF, List.of(Runes.FIRE, Runes.EARTH), 30, 30, true),
    MIST_BATTLESTAFF(ItemID.MIST_BATTLESTAFF, List.of(Runes.AIR, Runes.WATER), 30, 30, true),
    MUD_BATTLESTAFF(ItemID.MUD_BATTLESTAFF, List.of(Runes.WATER, Runes.EARTH), 30, 30, true),
    SMOKE_BATTLESTAFF(ItemID.SMOKE_BATTLESTAFF, List.of(Runes.AIR, Runes.FIRE), 30, 30, true),
    STEAM_BATTLESTAFF(ItemID.STEAM_BATTLESTAFF, List.of(Runes.WATER, Runes.FIRE), 30, 30, true),
    MYSTIC_AIR_STAFF(ItemID.MYSTIC_AIR_STAFF, List.of(Runes.AIR), 40, 40, true),
    MYSTIC_WATER_STAFF(ItemID.MYSTIC_WATER_STAFF, List.of(Runes.WATER), 40, 40, true),
    MYSTIC_EARTH_STAFF(ItemID.MYSTIC_EARTH_STAFF, List.of(Runes.EARTH), 40, 40, true),
    MYSTIC_FIRE_STAFF(ItemID.MYSTIC_FIRE_STAFF, List.of(Runes.FIRE), 40, 40, true),
    MYSTIC_DUST_STAFF(ItemID.MYSTIC_DUST_BATTLESTAFF, List.of(Runes.AIR, Runes.EARTH), 40, 40, true),
    MYSTIC_LAVA_STAFF(ItemID.MYSTIC_LAVA_STAFF, List.of(Runes.FIRE, Runes.EARTH), 40, 40, true),
    MYSTIC_MIST_STAFF(ItemID.MYSTIC_MIST_BATTLESTAFF, List.of(Runes.AIR, Runes.WATER), 40, 40, true),
    MYSTIC_MUD_STAFF(ItemID.MYSTIC_MUD_STAFF, List.of(Runes.WATER, Runes.EARTH), 40, 40, true),
    MYSTIC_SMOKE_STAFF(ItemID.MYSTIC_SMOKE_BATTLESTAFF, List.of(Runes.AIR, Runes.FIRE), 40, 40, true),
    MYSTIC_STEAM_STAFF(ItemID.MYSTIC_STEAM_BATTLESTAFF, List.of(Runes.WATER, Runes.FIRE), 40, 40, true),
    TWINFLAME_STAFF(ItemID.TWINFLAME_STAFF, List.of(Runes.FIRE, Runes.WATER), 0, 60, true);

    private final int itemID;
    private final List<Runes> runes;
    private final int requiredAttackLevel;
    private final int requiredMagicLevel;
    private final boolean membersOnly;

    public boolean canEquip(int realAttackLevel, int realMagicLevel, boolean membersWorld) {
        return this != NONE && (!membersOnly || membersWorld)
                && realAttackLevel >= requiredAttackLevel && realMagicLevel >= requiredMagicLevel;
    }

    private static final Map<Integer, Rs2Staff> BY_ITEM_ID = Arrays.stream(values())
            .filter(s -> s != NONE)
            .collect(Collectors.toMap(Rs2Staff::getItemID, Function.identity()));

    static Rs2Staff byItemId(int itemID) {
        return BY_ITEM_ID.getOrDefault(itemID, NONE);
    }
}
