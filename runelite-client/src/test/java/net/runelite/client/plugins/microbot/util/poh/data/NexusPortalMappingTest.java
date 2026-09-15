package net.runelite.client.plugins.microbot.util.poh.data;

import java.util.Collections;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;

public class NexusPortalMappingTest
{
    @Test
    public void alternativesRequireTheirInstalledBaseAndDiary()
    {
        int[] baseValues = {1, 4, 6};
        int[] diaries = {4480, 4477, 4460};
        NexusPortal[] alternatives = {NexusPortal.VARROCK_GE, NexusPortal.SEERS_VILLAGE, NexusPortal.YANILLE};
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class))
        {
            for (int i = 0; i < baseValues.length; i++)
            {
                microbot.reset();
                microbot.when(() -> Microbot.getVarbitValue(anyInt())).thenReturn(0);
                microbot.when(() -> Microbot.getVarbitValue(NexusPortal.VARBITS[0])).thenReturn(baseValues[i]);
                org.junit.Assert.assertFalse(NexusPortal.getAvailableTeleports().contains(alternatives[i]));
                int diary = diaries[i];
                microbot.when(() -> Microbot.getVarbitValue(diary)).thenReturn(1);
                org.junit.Assert.assertTrue(NexusPortal.getAvailableTeleports().contains(alternatives[i]));
                assertEquals(2, NexusPortal.getAvailableTeleports().size());
                microbot.when(() -> Microbot.getVarbitValue(NexusPortal.VARBITS[0])).thenReturn(0);
                org.junit.Assert.assertTrue(NexusPortal.getAvailableTeleports().isEmpty());
            }
        }
    }

    @Test
    public void savedSlotsMatchLiveCacheEnum1377RatherThanJavaOrdinal()
    {
        String[] names = {"Varrock", "Lumbridge", "Falador", "Camelot", "Ardougne",
            "Watchtower", "Senntisten", "Marim", "Kharyrll", "Lunar Isle", "Kourend Castle",
            "Waterbirth Island", "Fishing Guild", "Annakarl", "Troll Stronghold", "Catherby",
            "Ghorrock", "Carrallanger", "Weiss", "Arceuus Library", "Draynor Manor", "Battlefront",
            "Mind Altar", "Salve Graveyard", "Fenken' Castle", "West Ardougne", "Harmony Island",
            "Cemetery", "Barrows", "Ape Atoll Dungeon", "Civitas illa Fortis", "Trollheim",
            "Paddewwa", "Lassar", "Dareeyak", "Ourania", "Barbarian Outpost", "Port Khazard", "Ice Plateau"};
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class))
        {
            microbot.when(() -> Microbot.getVarbitValue(anyInt())).thenReturn(0);
            for (int value = 1; value <= names.length; value++)
            {
                microbot.when(() -> Microbot.getVarbitValue(NexusPortal.VARBITS[0])).thenReturn(value);
                var decoded = NexusPortal.getAvailableTeleports();
                assertEquals(1, decoded.size());
                assertEquals(names[value - 1], decoded.get(0).getText());
                assertEquals(value, decoded.get(0).varbitValue());
            }
            microbot.when(() -> Microbot.getVarbitValue(NexusPortal.VARBITS[0])).thenReturn(Integer.MAX_VALUE);
            assertEquals(Collections.emptyList(), NexusPortal.getAvailableTeleports());
        }
    }
}
