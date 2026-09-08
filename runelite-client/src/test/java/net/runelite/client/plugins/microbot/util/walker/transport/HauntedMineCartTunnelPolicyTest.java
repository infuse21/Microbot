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

public class HauntedMineCartTunnelPolicyTest
{
	private static final Set<Integer> CART_TUNNEL_IDS = Set.of(
		4913, 4914, 4915, 4920, 4921, 15830, 29332, 29333);

	@Test
	public void exactCartTunnelRowsUseCatalogTransitionOwnership()
	{
		List<Transport> rows = cartTunnelRows();
		assertEquals(8, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.isMembers()
			&& row.getDuration() == 1 && !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0)
			&& CatalogTransitionPolicy.isHauntedMineCartTunnel(row)
			&& CatalogTransitionPolicy.isEligible(row)));
	}

	@Test
	public void crystalShortcutRequiresCompletedHauntedMine()
	{
		List<Transport> rows = cartTunnelRows().stream()
			.filter(row -> row.getObjectId() == 29332 || row.getObjectId() == 29333)
			.collect(Collectors.toList());
		assertEquals(2, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.getQuests().equals(
			Map.of(Quest.HAUNTED_MINE, QuestState.FINISHED))));
	}

	@Test
	public void levelOneEntrancesRequireMorytaniaAccess()
	{
		List<Transport> rows = cartTunnelRows().stream()
			.filter(row -> row.getObjectId() != 29332 && row.getObjectId() != 29333)
			.collect(Collectors.toList());
		assertEquals(6, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.getQuests().equals(
			Map.of(Quest.PRIEST_IN_PERIL, QuestState.FINISHED))));
	}

	@Test
	public void mutatedCartTunnelContractsStayLegacyOwned()
	{
		Transport mutated = cartTunnelRows().get(0);
		mutated.setAction("Enter");
		assertFalse(CatalogTransitionPolicy.isHauntedMineCartTunnel(mutated));
		assertFalse(CatalogTransitionPolicy.isEligible(mutated));
	}

	private static List<Transport> cartTunnelRows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> CART_TUNNEL_IDS.contains(row.getObjectId()))
			.filter(row -> row.getName().equals("Cart tunnel"))
			.collect(Collectors.toList());
	}
}
