package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CatalogTransition;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuditedShortcutTransportTest
{
	private static final Set<Integer> MIGRATED_IDS = Set.of(
		15186, 15187, 15194, 15195, 21727, 23568, 23569, 26382);

	@Test
	public void exactAuditedRowsHaveCompleteRequirementsAndEngineOwnership()
	{
		List<Transport> rows = ordinaryRows(MIGRATED_IDS);

		assertEquals(10, rows.size());
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isAuditedShortcutTraversal));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertEquals(4, rows.stream()
			.filter(row -> row.getQuests().equals(Map.of(Quest.ROYAL_TROUBLE,
				QuestState.FINISHED)))
			.filter(row -> onlyAgility(row, 40)).count());
		assertEquals(2, rows.stream()
			.filter(row -> row.getQuests().equals(Map.of(Quest.TROLL_STRONGHOLD,
				QuestState.IN_PROGRESS)))
			.filter(row -> onlyAgility(row, 60)).count());
		assertEquals(2, rows.stream().filter(row -> Set.of(23568, 23569)
			.contains(row.getObjectId())).filter(row -> onlyAgility(row, 10)).count());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 21727)
			.filter(row -> onlyAgility(row, 34)).count());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 21727)
			.filter(row -> onlyAgility(row, 0)).count());
	}

	@Test
	public void incompleteOrMutatedContractsRemainLegacyOwned()
	{
		Transport missingQuest = transport(new WorldPoint(2585, 10259, 0),
			new WorldPoint(2585, 10262, 0), "Squeeze-through", "Crevice", 15194);
		missingQuest.getSkillLevels()[Skill.AGILITY.ordinal()] = 40;
		assertFalse(CatalogTransitionPolicy.isEligible(missingQuest));

		Transport wrongLanding = transport(new WorldPoint(2899, 3713, 0),
			new WorldPoint(2903, 3720, 0), "Crawl-through", "Little crack", 26382);
		wrongLanding.getSkillLevels()[Skill.AGILITY.ordinal()] = 60;
		wrongLanding.getQuests().put(Quest.TROLL_STRONGHOLD, QuestState.IN_PROGRESS);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongLanding));

		Transport wrongAction = transport(new WorldPoint(2709, 3209, 0),
			new WorldPoint(2704, 3209, 0), "Swing", "Ropeswing", 23568);
		wrongAction.getSkillLevels()[Skill.AGILITY.ordinal()] = 10;
		assertFalse(CatalogTransitionPolicy.isEligible(wrongAction));
	}

	@Test
	public void auditedShortcutsRequireExactCatalogLanding()
	{
		WorldPoint origin = new WorldPoint(2899, 3713, 0);
		WorldPoint destination = new WorldPoint(2904, 3720, 0);
		RouteInteraction pending = new RouteInteraction(1, 0, origin, destination, origin,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Crawl-through", true, 26382, origin, destination);
		CatalogTransition stage = new CatalogTransition(null, origin, 26382,
			"Crawl-through", "Crawl-through", origin, destination);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();

		assertEquals(RouteInteraction.Status.AVAILABLE, scanner.observePending(pending,
			new WorldPoint(2903, 3719, 0), edge -> stage, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED, scanner.observePending(pending,
			destination, edge -> stage, 13).getStatus());
	}

	@Test
	public void incompleteAndShadowRowsRemainOnlyAsSourceEvidence() throws IOException
	{
		Set<Integer> disabledIds = Set.of(15216, 15252, 21316, 21317, 21318, 21319,
			21728, 53259);
		assertEquals(0, ordinaryRows(disabledIds).size());

		String source = new String(getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv").readAllBytes(),
			StandardCharsets.UTF_8);
		assertTrue(source.contains("# 2655 9573 0\t2655 9566 0\tSqueeze-through;Pipe;21728"));
		assertTrue(source.contains("# 2539 10299 0\t2543 10299 0\tSwing-on;Ropeswing;15252"));
		assertTrue(source.contains("# 2385 10264 1\t2385 10260 1\tWalk-across;Rope bridge;21316"));
		assertTrue(source.contains("# 3434 10090 0\t3434 10095 0\tSqueeze-through;Crevice;53259"));
	}

	private static List<Transport> ordinaryRows(Set<Integer> ids)
	{
		return Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getType() == TransportType.TRANSPORT)
			.filter(row -> ids.contains(row.getObjectId()))
			.collect(Collectors.toList());
	}

	private static boolean onlyAgility(Transport transport, int agility)
	{
		int[] levels = transport.getSkillLevels();
		for (int i = 0; i < levels.length; i++)
		{
			if (levels[i] != (i == Skill.AGILITY.ordinal() ? agility : 0)) return false;
		}
		return true;
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination, String action,
		String name, int objectId)
	{
		return new Transport(origin, destination, "", TransportType.TRANSPORT,
			true, action, name, objectId);
	}
}
