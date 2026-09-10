package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import org.junit.Test;
import static org.junit.Assert.*;

public class KaruulmAccessPolicyTest
{
	@Test
	public void safeEntryEscapeAndProtectedOnwardVariantsAreExplicit()
	{
		List<Transport> rows = rows();
		assertEquals(54, rows.size());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 34359).count());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 34531).count());
		assertEquals(30, rows.stream().filter(row -> !row.getItemIdRequirements().isEmpty()).count());
		assertEquals(10, rows.stream().filter(row -> row.getItemIdRequirements().isEmpty() && !row.getVarbits().isEmpty()).count());
		for (Transport row : rows)
		{
			assertTrue(KaruulmAccessPolicy.isEligible(row));
			assertTrue(CatalogTransitionPolicy.isEligible(row));
			assertTrue(KaruulmAccessPolicy.hasLanded(row.getObjectId(), row.getOrigin(), row.getDestination(), row.getDestination()));
			assertFalse(KaruulmAccessPolicy.hasLanded(row.getObjectId(), row.getOrigin(), row.getDestination(), row.getOrigin()));
			if (!row.getItemIdRequirements().isEmpty())
			{
				assertTrue(EquippedSafetyTransitionPolicy.isEligible(row));
				int[] levels = new int[Skill.values().length];
				Arrays.fill(levels, 99);
				assertTrue(KaruulmAccessPolicy.skillsMet(row, levels));
				for (Skill skill : Skill.values())
				{
					int required = row.getSkillLevels()[skill.ordinal()];
					if (required == 0) continue;
					levels[skill.ordinal()] = required - 1;
					assertFalse(KaruulmAccessPolicy.skillsMet(row, levels));
					levels[skill.ordinal()] = 99;
				}
				row.getItemIdRequirements().clear();
				assertFalse(KaruulmAccessPolicy.isEligible(row));
			}
		}
	}

	@Test
	public void sceneUsesRealLevelsAndClaimedDiaryReward()
	{
		net.runelite.api.Client client = org.mockito.Mockito.mock(net.runelite.api.Client.class);
		net.runelite.client.callback.ClientThread thread = org.mockito.Mockito.mock(net.runelite.client.callback.ClientThread.class);
		org.mockito.Mockito.when(thread.runOnClientThreadOptional(org.mockito.ArgumentMatchers.any())).thenAnswer(call ->
			java.util.Optional.ofNullable(((java.util.concurrent.Callable<?>) call.getArgument(0)).call()));
		int[] levels = new int[Skill.values().length];
		org.mockito.Mockito.when(client.getRealSkillLevel(org.mockito.ArgumentMatchers.any(Skill.class)))
			.thenAnswer(call -> levels[((Skill) call.getArgument(0)).ordinal()]);
		try (org.mockito.MockedStatic<net.runelite.client.plugins.microbot.Microbot> microbot =
			org.mockito.Mockito.mockStatic(net.runelite.client.plugins.microbot.Microbot.class))
		{
			microbot.when(net.runelite.client.plugins.microbot.Microbot::getClient).thenReturn(client);
			microbot.when(net.runelite.client.plugins.microbot.Microbot::getClientThread).thenReturn(thread);
			for (Transport row : rows())
			{
				Arrays.fill(levels, 99);
				boolean diary = row.getItemIdRequirements().isEmpty() && !row.getVarbits().isEmpty();
				microbot.when(() -> net.runelite.client.plugins.microbot.Microbot.getVarbitValue(7932)).thenReturn(diary ? 1 : 0);
				assertTrue(Rs2CatalogTransitionScene.karuulmRequirementsReady(row));
				Arrays.fill(levels, 0);
				assertEquals(row.getItemIdRequirements().isEmpty(), Rs2CatalogTransitionScene.karuulmRequirementsReady(row));
				if (diary)
				{
					microbot.when(() -> net.runelite.client.plugins.microbot.Microbot.getVarbitValue(7932)).thenReturn(0);
					assertFalse(Rs2CatalogTransitionScene.karuulmRequirementsReady(row));
				}
			}
		}
	}

	private static List<Transport> rows() {
		return Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> KaruulmAccessPolicy.ownsObject(row.getObjectId())).collect(Collectors.toList());
	}
}
