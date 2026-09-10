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

public class AuditedMiscAccessTransportTest
{
	private static final Set<Integer> MIGRATED_IDS = Set.of(
		30236, 31626, 33262, 38574, 39170, 44003);

	@Test
	public void exactRowsHaveAuditedMetadataAndEngineOwnership()
	{
		List<Transport> rows = ordinaryRows(MIGRATED_IDS);

		assertEquals(9, rows.size());
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isAuditedMiscAccess));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 44003)
			.filter(row -> row.getDuration() == 4)
			.filter(row -> row.getQuests().equals(Map.of(Quest.BENEATH_CURSED_SANDS,
				QuestState.IN_PROGRESS))).count());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 38574)
			.filter(row -> onlyAgility(row, 52))
			.filter(row -> row.getQuests().equals(Map.of(Quest.SINS_OF_THE_FATHER,
				QuestState.FINISHED))).count());
	}

	@Test
	public void incompleteOrMutatedContractsRemainLegacyOwned()
	{
		Transport missingQuest = transport(new WorldPoint(3627, 3328, 0),
			new WorldPoint(3627, 3332, 0), "Enter", "Cracked wall", 39170);
		assertFalse(CatalogTransitionPolicy.isEligible(missingQuest));

		Transport missingAgility = transport(new WorldPoint(3654, 3384, 0),
			new WorldPoint(2400, 5969, 0), "Enter", "Mausoleum Door", 38574);
		missingAgility.getQuests().put(Quest.SINS_OF_THE_FATHER, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isEligible(missingAgility));

		Transport wrongLanding = transport(new WorldPoint(2457, 2849, 0),
			new WorldPoint(1937, 9009, 1), "Enter", "Mythic Statue", 31626);
		wrongLanding.getQuests().put(Quest.DRAGON_SLAYER_II, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isEligible(wrongLanding));
	}

	@Test
	public void exactLandingIsRequiredForCompletion()
	{
		WorldPoint origin = new WorldPoint(1435, 3671, 0);
		WorldPoint destination = new WorldPoint(1435, 10077, 3);
		RouteInteraction pending = new RouteInteraction(1, 0, origin, destination, origin,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Enter", true, 30236, origin, destination);
		CatalogTransition stage = new CatalogTransition(null, origin, 30236,
			"Enter", "Enter", origin, destination);
		CatalogTransitionRouteScanner scanner = new CatalogTransitionRouteScanner();

		assertEquals(RouteInteraction.Status.AVAILABLE, scanner.observePending(pending,
			new WorldPoint(1435, 10076, 3), edge -> stage, 13).getStatus());
		assertEquals(RouteInteraction.Status.CLEARED, scanner.observePending(pending,
			destination, edge -> stage, 13).getStatus());
	}

	@Test
	public void unsafeRowsRemainOnlyAsCommentedSourceEvidence() throws IOException
	{
		Set<Integer> disabledIds = Set.of(
			26880, 26882, 32403, 32507);
		assertEquals(0, ordinaryRows(disabledIds).size());

		String source = new String(getClass().getResourceAsStream(
			"/net/runelite/client/plugins/microbot/shortestpath/transports.tsv").readAllBytes(),
			StandardCharsets.UTF_8);
		assertTrue(source.contains("# 2673 9583 0\t2670 9583 2\tClimb;Vine;26880"));
		assertTrue(source.contains("# 1483 3549 0\t1483 9951 3\tEnter;Crypt Entrance;32403"));
		assertTrue(source.contains("# 9363 1050 0\t1214 3557 0\tClimb;Rope;32507"));
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
			if (levels[i] != (i == Skill.AGILITY.ordinal() ? agility : 0))
			{
				return false;
			}
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
