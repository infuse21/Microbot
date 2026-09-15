package net.runelite.client.plugins.microbot.util.poh.data;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;

public class JewelleryBoxLandingTest
{
    @Test
    public void farmingLandingUsesCurrentLevelAndTransportKeepsItsPlannedSnapshot()
    {
        try (MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class))
        {
            for (int level : new int[]{1, 44, 45, 99})
            {
                player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.FARMING)).thenReturn(level);
                WorldPoint expected = new WorldPoint(1248, level >= 45 ? 3725 : 3719, 0);
                assertEquals(expected, JewelleryBox.FARMING_GUILD.getDestination());
                PohTransport row = new PohTransport(new WorldPoint(1859, 7051, 0), JewelleryBox.FARMING_GUILD);
                player.when(() -> Rs2Player.getBoostedSkillLevel(Skill.FARMING)).thenReturn(1);
                assertEquals(expected, row.getDestination());
            }
        }
    }
}
