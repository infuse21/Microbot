package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuideNpcTransportPolicyTest
{
	@Test
	public void exactGuideRowsUseDirectNpcOwnership()
	{
		List<Transport> rows = guideRows();
		assertEquals(10, rows.size());
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(NpcTransportPolicy::isOrdinaryDirectRoute));
		assertTrue(rows.stream().allMatch(NpcTransportPolicy::isEligible));
	}

	@Test
	public void dorgeshuunRowsUsePostQuestNpcFormsAndActions()
	{
		List<Transport> rows = guideRows().stream()
			.filter(row -> Set.of(997, 7299, 7301).contains(row.getObjectId()))
			.collect(Collectors.toList());
		assertEquals(6, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.getQuests().equals(
			Map.of(Quest.DEATH_TO_THE_DORGESHUUN, QuestState.FINISHED))));
		assertTrue(rows.stream().allMatch(row -> row.getVarbits().isEmpty()
			&& row.getVarplayers().isEmpty()));
		assertEquals(Set.of("Cellar", "Mines"), rows.stream()
			.filter(row -> row.getName().equals("Dartog"))
			.map(Transport::getAction).collect(Collectors.toSet()));
		assertTrue(rows.stream().filter(row -> row.getName().equals("Dartog"))
			.allMatch(row -> row.getObjectId() == 997));
		assertEquals(Set.of("Cellar", "Watermill"), rows.stream()
			.filter(row -> row.getName().equals("Mistag"))
			.map(Transport::getAction).collect(Collectors.toSet()));
		assertEquals(Set.of("Mines", "Watermill"), rows.stream()
			.filter(row -> row.getName().equals("Kazgar"))
			.map(Transport::getAction).collect(Collectors.toSet()));
	}

	@Test
	public void followRowsRetainTheirExactUnlockState()
	{
		List<Transport> elkoy = guideRows().stream()
			.filter(row -> row.getObjectId() == 4968).collect(Collectors.toList());
		assertEquals(2, elkoy.size());
		assertTrue(elkoy.stream().allMatch(row -> row.getAction().equals("Follow")
			&& row.getVarbits().isEmpty() && row.getVarplayers().size() == 1
			&& row.getVarplayers().stream().allMatch(gate -> gate.getVarplayerId() == 111
				&& gate.getValue() == 0
				&& gate.getOperator() == TransportVarPlayer.Operator.GREATER_THAN)));

		List<Transport> mountain = guideRows().stream()
			.filter(row -> row.getObjectId() == 14529).collect(Collectors.toList());
		assertEquals(2, mountain.size());
		assertTrue(mountain.stream().allMatch(row -> row.getAction().equals("Follow")
			&& row.getVarplayers().isEmpty() && row.getVarbits().size() == 1
			&& row.getVarbits().stream().allMatch(gate -> gate.getVarbitId() == 17226
				&& gate.getValue() == 1
				&& gate.getOperator() == TransportVarbit.Operator.EQUAL)));
	}

	@Test
	public void nearbyOrMutatedGuideContractsStayLegacyOwned()
	{
		Transport mutated = guideRows().get(0);
		mutated.setAction("Talk-to");
		assertFalse(NpcTransportPolicy.isOrdinaryDirectRoute(mutated));
		assertFalse(NpcTransportPolicy.isEligible(mutated));
	}

	private static List<Transport> guideRows()
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> Set.of(997, 4968, 7299, 7301, 14529).contains(row.getObjectId()))
			.filter(row -> Set.of("Dartog", "Elkoy", "Kazgar", "Mistag", "Mountain Guide")
				.contains(row.getName()))
			.collect(Collectors.toList());
	}
}
