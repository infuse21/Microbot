package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MortMyreTreeBridgeAndTrollweissSlopePolicyTest
{
	@Test
	public void exactMortMyreTreeBridgeRowsUseCatalogOwnership()
	{
		List<Transport> rows = loadedRows(5003, "Tree");
		assertEquals(4, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.getAction().equals("Cross-bridge")
			&& row.isMembers() && row.getDuration() == 1
			&& row.getQuests().equals(Map.of(Quest.PRIEST_IN_PERIL, QuestState.FINISHED))
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0)
			&& CatalogTransitionPolicy.isMortMyreTreeBridge(row)
			&& CatalogTransitionPolicy.isEligible(row)));
	}

	@Test
	public void mutatedTreeBridgeContractStaysLegacyOwned()
	{
		Transport mutated = loadedRows(5003, "Tree").get(0);
		mutated.setAction("Cross");
		assertFalse(CatalogTransitionPolicy.isMortMyreTreeBridge(mutated));
		assertFalse(CatalogTransitionPolicy.isEligible(mutated));
	}

	@Test
	public void equippedSledSlopesAreLoadedAfterPreparationIsSupported()
	{
		List<Transport> slopes = loadedRows(5015, "Slope");
		assertEquals(4, slopes.size());
		assertTrue(slopes.stream().allMatch(EquippedSafetyTransitionPolicy::isEligible));
	}

	private static List<Transport> loadedRows(int objectId, String name)
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> row.getObjectId() == objectId && row.getName().equals(name))
			.collect(Collectors.toList());
	}
}
