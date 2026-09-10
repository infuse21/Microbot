package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.shortestpath.TransportVarbit;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CatalogTransitionPolicyTest
{
	private static final WorldPoint SURFACE = new WorldPoint(100, 100, 0);
	private static final WorldPoint UPSTAIRS = new WorldPoint(100, 100, 1);

	@Test
	public void completedGhostsAhoyBarrierAcceptsTheModernPassAction()
	{
		Transport barrier = Transport.loadAllFromResources().values().stream()
			.flatMap(Set::stream)
			.filter(candidate -> new WorldPoint(3651, 3486, 0).equals(candidate.getOrigin()))
			.filter(candidate -> candidate.getDestination().equals(new WorldPoint(3654, 3486, 0)))
			.findFirst().orElseThrow(() -> new AssertionError("completed barrier row missing"));

		assertEquals("Pass", Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Pass"}, barrier, true));
		assertNull(Rs2CatalogTransitionScene.resolveLiveAction(
			new String[]{"Pass"}, barrier, false));
	}

	@Test
	public void acceptsDirectStairsLaddersTrapdoorsCavesAndGangplanks()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Climb-up", "Ladder")));
		assertTrue(CatalogTransitionPolicy.isEligible(
			transport(UPSTAIRS, SURFACE, "Climb-down", "Trapdoor")));
		assertTrue(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(200, 200, 0), "Enter", "Cave entrance")));
		assertTrue(CatalogTransitionPolicy.isEligible(transport(
			new WorldPoint(3041, 3199, 1), new WorldPoint(3041, 3202, 0),
			"Cross", "Gangplank")));
	}

	@Test
	public void acceptsOnlyDirectItemFreeSceneChangingAgilityShortcuts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(105, 100, 0), "Walk-across", "Log balance")));
		assertTrue(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			UPSTAIRS, "Jump-up", "Weathered wall")));
		assertFalse(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(101, 100, 0), "Jump-onto", "Stepping stone")));
		assertFalse(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(105, 100, 0), "Grapple", "Rocks")));
		assertFalse(CatalogTransitionPolicy.isEligible(agilityTransport(SURFACE,
			new WorldPoint(102, 100, 0), "Use", "Rope -> Boulder")));
	}

	@Test
	public void acceptsOnlyFrozenEquippedGrappleRowsAndRequirements()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(CatalogTransitionPolicy::isEquippedGrappleShortcut)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(12, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 17062).count());

		Transport catherby = rows.stream().filter(row -> row.getObjectId() == 17042)
			.findFirst().orElseThrow(AssertionError::new);
		catherby.getSkillLevels()[Skill.AGILITY.ordinal()] = 33;
		assertFalse(CatalogTransitionPolicy.isEquippedGrappleShortcut(catherby));
		catherby.getSkillLevels()[Skill.AGILITY.ordinal()] = 32;
		catherby.setItemIdRequirements(Set.of());
		assertFalse(CatalogTransitionPolicy.isEquippedGrappleShortcut(catherby));
		catherby.setItemIdRequirements(Set.of(Set.of(9419)));
		catherby.setAction("Swim-to");
		assertFalse(CatalogTransitionPolicy.isEquippedGrappleShortcut(catherby));
		catherby.setAction("Grapple");
		assertTrue(CatalogTransitionPolicy.isEquippedGrappleShortcut(catherby));

		Transport shifted = new Transport(new WorldPoint(2865, 3428, 0),
			new WorldPoint(2869, 3428, 0), "test", TransportType.GRAPPLE_SHORTCUT,
			false, "Grapple", "Rocks", 17042);
		shifted.getSkillLevels()[Skill.AGILITY.ordinal()] = 32;
		shifted.getSkillLevels()[Skill.RANGED.ordinal()] = 35;
		shifted.getSkillLevels()[Skill.STRENGTH.ordinal()] = 35;
		shifted.setItemIdRequirements(Set.of(Set.of(9419)));
		assertFalse(CatalogTransitionPolicy.isEquippedGrappleShortcut(shifted));
	}

	@Test
	public void acceptsOnlyExactBarehandWaterObeliskVariant()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(CatalogTransitionPolicy::isBarehandGrappleShortcut)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(1, rows.size());
		Transport barehand = rows.get(0);
		assertEquals(17062, barehand.getObjectId());
		assertEquals(72, barehand.getSkillLevels()[Skill.AGILITY.ordinal()]);
		assertTrue(barehand.getItemIdRequirements().isEmpty());
		assertTrue(CatalogTransitionPolicy.isEligible(barehand));

		barehand.getSkillLevels()[Skill.AGILITY.ordinal()] = 71;
		assertFalse(CatalogTransitionPolicy.isBarehandGrappleShortcut(barehand));
		barehand.getSkillLevels()[Skill.AGILITY.ordinal()] = 72;
		barehand.setItemIdRequirements(Set.of(Set.of(9419)));
		assertFalse(CatalogTransitionPolicy.isBarehandGrappleShortcut(barehand));
	}

	@Test
	public void acceptsOnlyExactCapeGatedMorUlRekHotVentDoors()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 30266)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(11, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isMorUlRekHotVentDoor));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(Transport::isMembers));
		assertTrue(rows.stream().allMatch(row -> row.getItemIdRequirements().equals(
			Set.of(Set.of(6570, 13329, 24134, 24223)))));
	}

	@Test
	public void acceptsAllExactKaramjaVolcanoEntryAndReturnRows()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 11441 || row.getObjectId() == 18969)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(12, rows.size());
		assertEquals(8, rows.stream().filter(row -> row.getObjectId() == 11441).count());
		assertEquals(4, rows.stream().filter(row -> row.getObjectId() == 18969).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isKaramjaVolcanoTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> !row.isMembers()
			&& !row.isConsumable() && row.getItemIdRequirements().isEmpty()
			&& row.getCurrencyAmount() == 0 && !row.isQuestLocked()
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()));

		Transport entrance = rows.stream().filter(row -> row.getObjectId() == 11441)
			.findFirst().orElseThrow(AssertionError::new);
		entrance.setItemIdRequirements(Set.of(Set.of(954)));
		assertFalse(CatalogTransitionPolicy.isKaramjaVolcanoTransition(entrance));
	}

	@Test
	public void acceptsAllExactMembersOnlyOpeningExitRows()
	{
		Set<Integer> objectIds = Set.of(39648, 40389, 40391, 40737);
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> objectIds.contains(row.getObjectId()))
			.filter(row -> "Exit".equals(row.getAction()) && "Opening".equals(row.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(11, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isOpeningExitTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& !row.isQuestLocked() && row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()));

		Transport exit = rows.get(0);
		Transport freeWorldCopy = new Transport(exit.getOrigin(), exit.getDestination(),
			exit.getDisplayInfo(), exit.getType(), false, exit.getAction(), exit.getName(), exit.getObjectId());
		assertFalse(CatalogTransitionPolicy.isOpeningExitTransition(freeWorldCopy));
	}

	@Test
	public void acceptsAllExactMembersOnlyClimbUpExitRows()
	{
		Set<Integer> objectIds = Set.of(15193, 18354, 30844);
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> objectIds.contains(row.getObjectId()))
			.filter(row -> "Climb-up".equals(row.getAction()) && "Exit".equals(row.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(7, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isClimbUpExitTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& !row.isQuestLocked() && row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()));

		Transport exit = rows.get(0);
		Transport freeWorldCopy = new Transport(exit.getOrigin(), exit.getDestination(),
			exit.getDisplayInfo(), exit.getType(), false, exit.getAction(), exit.getName(), exit.getObjectId());
		assertFalse(CatalogTransitionPolicy.isClimbUpExitTransition(freeWorldCopy));
	}

	@Test
	public void acceptsOnlyTheFifteenExactOutwardRopeExits()
	{
		Set<Integer> objectIds = Set.of(5946, 10434, 12230, 25213, 26370);
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> objectIds.contains(row.getObjectId()))
			.filter(row -> "Climb".equals(row.getAction()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(15, rows.size());
		assertEquals(4, rows.stream().filter(row -> !row.isMembers()).count());
		assertEquals(11, rows.stream().filter(Transport::isMembers).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isRopeExitTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& !row.isQuestLocked() && row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()));

		Transport exit = rows.get(0);
		exit.setItemIdRequirements(Set.of(Set.of(954)));
		assertFalse(CatalogTransitionPolicy.isRopeExitTransition(exit));
	}

	@Test
	public void acceptsOnlyTheFiftyExactCompletedQuestEntrances()
	{
		Set<Integer> objectIds = Set.of(5008, 5009, 5011, 5012, 5013, 5014, 6310, 6621);
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> objectIds.contains(row.getObjectId()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(50, rows.size());
		assertEquals(26, rows.stream().filter(row -> row.getObjectId() >= 5008
			&& row.getObjectId() <= 5014).count());
		assertEquals(12, rows.stream().filter(row -> row.getObjectId() == 6310).count());
		assertEquals(12, rows.stream().filter(row -> row.getObjectId() == 6621).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isQuestGatedEntranceTransition));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& row.getQuests().equals(Map.of(expectedQuest(row.getObjectId()), QuestState.FINISHED))));

		Transport missingQuest = rows.get(0);
		missingQuest.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isQuestGatedEntranceTransition(missingQuest));
	}

	@Test
	public void acceptsExactPostQuestCrandorAndShiloEntrances()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 25154 || row.getObjectId() == 2216)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(22, rows.size());
		assertEquals(12, rows.stream().filter(CatalogTransitionPolicy::isCrandorHole).count());
		assertEquals(10, rows.stream().filter(CatalogTransitionPolicy::isShiloBrokenCart).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));

		Transport missingQuest = rows.get(0);
		missingQuest.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isEligible(missingQuest));
	}

	@Test
	public void unresolvedRevenantFeeCrevicesAreNotLoaded()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.noneMatch(row -> row.getObjectId() == 40386));
	}

	@Test
	public void acceptsExactStrongholdEscapesAndWintertodtDoors()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 29322 || Set.of(23703, 23705, 23706, 23732)
				.contains(row.getObjectId()))
			.filter(row -> !row.getOrigin().equals(new WorldPoint(1570, 5061, 0)))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(30, rows.size());
		assertEquals(14, rows.stream().filter(CatalogTransitionPolicy::isStrongholdEscape).count());
		assertEquals(16, rows.stream().filter(CatalogTransitionPolicy::isWintertodtDoor).count());
		assertEquals(7, rows.stream().filter(row -> row.getObjectId() == 29322)
			.filter(row -> row.getSkillLevels()[net.runelite.api.Skill.FIREMAKING.ordinal()] == 50).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));

		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 23703)
			.noneMatch(row -> row.getOrigin().equals(new WorldPoint(1570, 5061, 0))));
	}

	@Test
	public void acceptsOnlyPostQuestLithkrenBrokenDoors()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> Set.of(32117, 32132).contains(row.getObjectId()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(9, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isLithkrenBrokenDoor));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers()
			&& row.getQuests().equals(Map.of(Quest.DRAGON_SLAYER_II, QuestState.FINISHED))));

		rows.get(0).getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isLithkrenBrokenDoor(rows.get(0)));
	}

	@Test
	public void acceptsOnlyTheSixExactDirectOutwardExits()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(CatalogTransitionPolicy::isDirectOutwardExit)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(6, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && !row.isQuestLocked()
			&& !row.isConsumable() && row.getItemIdRequirements().isEmpty()
			&& row.getCurrencyAmount() == 0 && row.getVarbits().isEmpty()
			&& row.getVarplayers().isEmpty()));

		Transport exit = rows.get(0);
		Transport freeWorldCopy = new Transport(exit.getOrigin(), exit.getDestination(),
			exit.getDisplayInfo(), exit.getType(), false, exit.getAction(), exit.getName(),
			exit.getObjectId());
		assertFalse(CatalogTransitionPolicy.isDirectOutwardExit(freeWorldCopy));
	}

	@Test
	public void acceptsOnlyTheExactCompletedCamdozaalBoundary()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(CatalogTransitionPolicy::isCamdozaalRoute)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(2, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> !row.isMembers()
			&& row.getQuests().equals(Map.of(Quest.BELOW_ICE_MOUNTAIN, QuestState.FINISHED))));

		rows.get(0).getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isCamdozaalRoute(rows.get(0)));
	}

	@Test
	public void acceptsOnlyTheSixExactUnlockedPassages()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> Set.of(11355, 28918, 42249).contains(row.getObjectId()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(6, rows.size());
		assertEquals(2, rows.stream().filter(row -> row.getObjectId() == 11355).count());
		assertEquals(3, rows.stream().filter(row -> row.getObjectId() == 28918).count());
		assertEquals(1, rows.stream().filter(row -> row.getObjectId() == 42249).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isUnlockedPassage));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(Transport::isMembers));

		Transport killerwatt = rows.stream().filter(row -> row.getObjectId() == 11355)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(Map.of(Quest.ERNEST_THE_CHICKEN, QuestState.FINISHED),
			killerwatt.getQuests());
		killerwatt.getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isUnlockedPassage(killerwatt));

		Transport forthos = rows.stream().filter(row -> row.getObjectId() == 28918)
			.findFirst().orElseThrow(AssertionError::new);
		forthos.getVarbits().clear();
		assertFalse(CatalogTransitionPolicy.isUnlockedPassage(forthos));

		Transport foreignRoute = new Transport(new WorldPoint(1804, 9968, 0),
			new WorldPoint(1727, 9993, 0), "test", TransportType.TRANSPORT, true,
			"Enter", "Strange passage", 28918);
		foreignRoute.getVarbits().add(new TransportVarbit(
			5087, 1, TransportVarbit.Operator.EQUAL));
		assertFalse(CatalogTransitionPolicy.isUnlockedPassage(foreignRoute));
	}

	@Test
	public void acceptsOnlyTheElevenUnlockedIcePathGateApproaches()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 5043 || row.getObjectId() == 5044)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(11, rows.size());
		assertEquals(5, rows.stream().filter(row -> row.getOrigin().getX() == 2837).count());
		assertEquals(6, rows.stream().filter(row -> row.getOrigin().getX() == 2839).count());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isIcePathGate));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && row.getDuration() == 0
			&& row.getItemIdRequirements().isEmpty()));

		Transport entry = rows.stream().filter(row -> row.getOrigin().getX() == 2837)
			.findFirst().orElseThrow(AssertionError::new);
		assertEquals(Map.of(Quest.DESERT_TREASURE_I, QuestState.IN_PROGRESS),
			entry.getQuests());
		assertEquals(1, entry.getVarbits().size());
		entry.getVarbits().clear();
		assertFalse(CatalogTransitionPolicy.isIcePathGate(entry));

		Transport exit = rows.stream().filter(row -> row.getOrigin().getX() == 2839)
			.findFirst().orElseThrow(AssertionError::new);
		assertTrue(exit.getQuests().isEmpty());
		assertTrue(exit.getVarbits().isEmpty());
		exit.getQuests().put(Quest.DESERT_TREASURE_I, QuestState.IN_PROGRESS);
		assertFalse(CatalogTransitionPolicy.isIcePathGate(exit));

		Transport foreignRoute = new Transport(new WorldPoint(2837, 3735, 0),
			new WorldPoint(2839, 3739, 0), "", TransportType.TRANSPORT, true,
			"Go-through", "Ice gate", 5044);
		foreignRoute.getQuests().put(Quest.DESERT_TREASURE_I, QuestState.IN_PROGRESS);
		foreignRoute.getVarbits().add(new TransportVarbit(
			382, 1, TransportVarbit.Operator.GREATER_THAN));
		assertFalse(CatalogTransitionPolicy.isIcePathGate(foreignRoute));
	}

	@Test
	public void acceptsOnlyTheSixCanonicalHeroesRockSlides()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getObjectId() == 2634)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(6, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isHeroesRockSlide));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && row.getDuration() == 10
			&& row.getSkillLevels()[Skill.MINING.ordinal()] == 50
			&& row.getQuests().equals(Map.of(Quest.HEROES_QUEST, QuestState.FINISHED))));

		rows.get(0).getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isHeroesRockSlide(rows.get(0)));
	}

	@Test
	public void acceptsOnlyTheExactStrongholdSlayerTunnelApproaches()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getType() == TransportType.TRANSPORT)
			.filter(candidate -> candidate.getObjectId() == 30174)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(2, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isStrongholdSlayerTunnel));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers() && !row.isQuestLocked()
			&& row.getSkillLevels()[Skill.AGILITY.ordinal()] == 72));
	}

	@Test
	public void acceptsOnlyTheFiveCompletedWeissHoleApproaches()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(candidate -> candidate.getObjectId() == 33227)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(5, rows.size());
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isWeissHole));
		assertTrue(rows.stream().allMatch(CatalogTransitionPolicy::isEligible));
		assertTrue(rows.stream().allMatch(row -> row.isMembers()
			&& row.getQuests().equals(Map.of(
				Quest.MAKING_FRIENDS_WITH_MY_ARM, QuestState.FINISHED))));

		rows.get(0).getQuests().clear();
		assertFalse(CatalogTransitionPolicy.isWeissHole(rows.get(0)));
	}

	@Test
	public void karuulmAccessUsesOnlyExplicitSafeOrProtectedContracts()
	{
		assertTrue(Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> KaruulmAccessPolicy.ownsObject(row.getObjectId()))
			.allMatch(KaruulmAccessPolicy::isEligible));
	}

	@Test
	public void acceptsOnlyExactItemFreeOrdinaryDirectContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb-over", "Stile")));
		Transport rocks = transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb", "Rocks");
		assertTrue(CatalogTransitionPolicy.isEligible(rocks));
		rocks.setItemIdRequirements(Set.of(Set.of(3105)));
		assertFalse(CatalogTransitionPolicy.isEligible(rocks));
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb-over", "Wall")));
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Cross", "Stile")));
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(103, 100, 0), "Climb", "Climbing rocks")));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 32153)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 32152)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3937)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3999)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Dense forest", 3997)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(103, 100, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Forest", 3937)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			UPSTAIRS, "test", TransportType.TRANSPORT,
			false, "Enter", "Lift", 30258)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(UPSTAIRS,
			SURFACE, "test", TransportType.TRANSPORT,
			false, "Enter", "Lift", 30259)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			UPSTAIRS, "test", TransportType.TRANSPORT,
			false, "Enter", "Lift", 30257)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			UPSTAIRS, "test", TransportType.TRANSPORT,
			false, "Use", "Lift", 30258)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump on", "Rubber cap mushroom", 30606)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump on", "Rubber cap mushroom", 30605)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump", "Rubber cap mushroom", 30606)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Jump on", "Mushroom", 30606)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Magical Barrier", 31616)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(98, 100, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Magical Barrier", 31617)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Magical Barrier", 31615)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Magical Barrier", 31616)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(100, 102, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Barrier", 31616)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "City Gate", 36518)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Exit", "City Gate", 36523)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "City Gate", 36517)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "City Gate", 36518)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Gate", 36518)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 20482)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 15771)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 20540)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Passageway", 7258)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Pass", "Passageway", 20482)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Tunnel", 2141)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Enter", "Tunnel", 30174)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT,
			false, "Exit", "Tunnel", 2141)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3220, 10088, 0), new WorldPoint(3220, 10084, 0),
			"test", TransportType.TRANSPORT, false, "Jump-to", "Pillar", 31561)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3200, 10196, 0), new WorldPoint(3204, 10196, 0),
			"test", TransportType.TRANSPORT, false, "Jump-to", "Pillar", 31561)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3220, 10088, 0), new WorldPoint(3220, 10084, 0),
			"test", TransportType.TRANSPORT, false, "Jump", "Pillar", 31561)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
			new WorldPoint(104, 100, 0), "test", TransportType.TRANSPORT,
			false, "Jump-to", "Pillar", 20540)));
	}

	@Test
	public void acceptsOnlyExactBasaltCausewayContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3595, 0), new WorldPoint(2522, 3597, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump-across", "Basalt rock", 4551)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3597, 0), new WorldPoint(2522, 3595, 0),
			"test", TransportType.TRANSPORT, false, "Jump-to", "Beach", 4550)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2514, 3617, 0), new WorldPoint(2514, 3619, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump-to", "Rocky shore", 4559)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3595, 0), new WorldPoint(2522, 3597, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump-across", "Basalt rock", 4549)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2522, 3595, 0), new WorldPoint(2522, 3597, 0),
			"test", TransportType.TRANSPORT, false,
			"Jump", "Basalt rock", 4551)));
	}

	@Test
	public void acceptsOnlyExactCatalogRaftContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2510, 3494, 0), new WorldPoint(2512, 3481, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Log raft", 1987)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(1761, 5362, 0), new WorldPoint(2531, 3446, 0),
			"test", TransportType.TRANSPORT, false, "Ride", "Aged log", 25216)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2567, 9680, 0), new WorldPoint(2606, 9692, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Raft", 2849)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2606, 9692, 0), new WorldPoint(2567, 9680, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Raft", 2849)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2567, 9680, 0), new WorldPoint(2607, 9692, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Raft", 2849)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2510, 3494, 0), new WorldPoint(2512, 3481, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Log raft", 1986)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(2510, 3494, 0), new WorldPoint(2513, 3481, 0),
			"test", TransportType.TRANSPORT, false, "Board", "Log raft", 1987)));
	}

	@Test
	public void acceptsOnlyExactGuardiansOfTheRiftBarrierContracts()
	{
		for (int x = 3613; x <= 3617; x++)
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
				new WorldPoint(x, 9482, 0), new WorldPoint(x, 9484, 0),
				"test", TransportType.TRANSPORT, false,
				"Quick-pass", "Barrier", 43700)));
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
				new WorldPoint(x, 9484, 0), new WorldPoint(x, 9482, 0),
				"test", TransportType.TRANSPORT, false,
				"Quick-pass", "Barrier", 43700)));
		}
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3612, 9482, 0), new WorldPoint(3612, 9484, 0),
			"test", TransportType.TRANSPORT, false,
			"Quick-pass", "Barrier", 43700)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3613, 9482, 0), new WorldPoint(3613, 9484, 0),
			"test", TransportType.TRANSPORT, false,
			"Pass", "Barrier", 43700)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3613, 9482, 0), new WorldPoint(3613, 9484, 0),
			"test", TransportType.TRANSPORT, false,
			"Quick-pass", "Barrier", 43699)));
	}

	@Test
	public void acceptsOnlyExactTempleOfTheEyePortalContracts()
	{
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3104, 9573, 0), new WorldPoint(3615, 9470, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43841)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3615, 9470, 0), new WorldPoint(3104, 9573, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43692)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3104, 9573, 0), new WorldPoint(3615, 9470, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43840)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(3104, 9573, 0), new WorldPoint(3615, 9471, 0),
			"test", TransportType.TRANSPORT, false,
			"Enter", "Portal", 43841)));
	}

	@Test
	public void acceptsOnlyAuditedStepsOutsideSamePlaneLandingTolerance()
	{
		for (int id : new int[]{30189, 30190, 8966, 33261})
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
				new WorldPoint(200, 200, 0), "test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE,
				new WorldPoint(102, 100, 0), "test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Steps", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Rope", id)));
		}
		for (int id : new int[]{34530, 34531, 37417, 29993, 8729, 30191})
		{
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Steps", id)));
		}
	}

	@Test
	public void acceptsOnlyExactCatacombsExitVineIdentities()
	{
		for (int id : new int[]{28895, 28896, 28897, 28898, 42350})
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb-up", "Vine", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb-down", "Vine", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb-up", "Root", id)));
		}
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
			"test", TransportType.TRANSPORT, false, "Climb-up", "Vine", 28899)));
	}

	@Test
	public void acceptsOnlyTheSixPostQuestEnakhraMagicBarriers()
	{
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> row.getObjectId() == 11005)
			.collect(java.util.stream.Collectors.toList());
		assertEquals(6, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.isMembers()
			&& row.getDuration() == 1 && !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& row.getQuests().equals(Map.of(Quest.ENAKHRAS_LAMENT, QuestState.FINISHED))
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0)
			&& CatalogTransitionPolicy.isEnakhraMagicBarrier(row)
			&& CatalogTransitionPolicy.isEligible(row)));

		WorldPoint north = new WorldPoint(3103, 9318, 1);
		WorldPoint south = new WorldPoint(3103, 9320, 1);
		Transport exact = new Transport(north, south, "test", TransportType.TRANSPORT,
			true, "Pass-through", "Magic barrier", 11005);
		exact.getQuests().put(Quest.ENAKHRAS_LAMENT, QuestState.FINISHED);
		assertTrue(CatalogTransitionPolicy.isEnakhraMagicBarrier(exact));

		Transport missingQuest = new Transport(north, south, "test", TransportType.TRANSPORT,
			true, "Pass-through", "Magic barrier", 11005);
		assertFalse(CatalogTransitionPolicy.isEnakhraMagicBarrier(missingQuest));
		Transport nonMember = new Transport(north, south, "test", TransportType.TRANSPORT,
			false, "Pass-through", "Magic barrier", 11005);
		nonMember.getQuests().put(Quest.ENAKHRAS_LAMENT, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isEnakhraMagicBarrier(nonMember));

		for (Transport malformed : java.util.List.of(
			new Transport(north, new WorldPoint(3103, 9321, 1), "test", TransportType.TRANSPORT,
				true, "Pass-through", "Magic barrier", 11005),
			new Transport(north, south, "test", TransportType.TRANSPORT,
				true, "Pass", "Magic barrier", 11005),
			new Transport(north, south, "test", TransportType.TRANSPORT,
				true, "Pass-through", "Magical barrier", 11005),
			new Transport(north, south, "test", TransportType.TRANSPORT,
				true, "Pass-through", "Magic barrier", 11006)))
		{
			malformed.getQuests().put(Quest.ENAKHRAS_LAMENT, QuestState.FINISHED);
			assertFalse(CatalogTransitionPolicy.isEnakhraMagicBarrier(malformed));
		}
	}

	@Test
	public void acceptsOnlyTheSevenCompletedQuestTunnels()
	{
		Map<Integer, Quest> questsByObject = Map.of(
			6481, Quest.DESERT_TREASURE_I,
			15188, Quest.ROYAL_TROUBLE,
			15189, Quest.ROYAL_TROUBLE,
			6898, Quest.THE_LOST_TRIBE,
			6899, Quest.THE_LOST_TRIBE,
			6912, Quest.THE_LOST_TRIBE);
		java.util.List<Transport> rows = Transport.loadAllFromResources().values().stream()
			.flatMap(java.util.Collection::stream)
			.filter(row -> questsByObject.containsKey(row.getObjectId()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(7, rows.size());
		assertTrue(rows.stream().allMatch(row -> row.isMembers()
			&& row.getDuration() == 1 && !row.isConsumable()
			&& row.getItemIdRequirements().isEmpty() && row.getCurrencyAmount() == 0
			&& row.getQuests().equals(Map.of(questsByObject.get(row.getObjectId()),
				QuestState.FINISHED))
			&& row.getVarbits().isEmpty() && row.getVarplayers().isEmpty()
			&& java.util.Arrays.stream(row.getSkillLevels()).allMatch(level -> level == 0)
			&& CatalogTransitionPolicy.isCompletedQuestTunnel(row)
			&& CatalogTransitionPolicy.isEligible(row)));

		WorldPoint surface = new WorldPoint(3233, 2887, 0);
		WorldPoint pyramid = new WorldPoint(3233, 9313, 0);
		Transport exact = new Transport(surface, pyramid, "test", TransportType.TRANSPORT,
			true, "Enter", "Tunnel", 6481);
		exact.getQuests().put(Quest.DESERT_TREASURE_I, QuestState.FINISHED);
		assertTrue(CatalogTransitionPolicy.isCompletedQuestTunnel(exact));

		Transport unfinished = new Transport(surface, pyramid, "test", TransportType.TRANSPORT,
			true, "Enter", "Tunnel", 6481);
		unfinished.getQuests().put(Quest.DESERT_TREASURE_I, QuestState.IN_PROGRESS);
		assertFalse(CatalogTransitionPolicy.isCompletedQuestTunnel(unfinished));
		Transport nonMember = new Transport(surface, pyramid, "test", TransportType.TRANSPORT,
			false, "Enter", "Tunnel", 6481);
		nonMember.getQuests().put(Quest.DESERT_TREASURE_I, QuestState.FINISHED);
		assertFalse(CatalogTransitionPolicy.isCompletedQuestTunnel(nonMember));

		for (Transport malformed : java.util.List.of(
			new Transport(surface, new WorldPoint(3233, 9314, 0), "test", TransportType.TRANSPORT,
				true, "Enter", "Tunnel", 6481),
			new Transport(surface, pyramid, "test", TransportType.TRANSPORT,
				true, "Climb-down", "Tunnel", 6481),
			new Transport(surface, pyramid, "test", TransportType.TRANSPORT,
				true, "Enter", "Cave", 6481),
			new Transport(surface, pyramid, "test", TransportType.TRANSPORT,
				true, "Enter", "Tunnel", 6482)))
		{
			malformed.getQuests().put(Quest.DESERT_TREASURE_I, QuestState.FINISHED);
			assertFalse(CatalogTransitionPolicy.isCompletedQuestTunnel(malformed));
		}
	}

	@Test
	public void acceptsOnlyTheExactEnakhrasTempleSandPileExitContract()
	{
		WorldPoint temple = new WorldPoint(3124, 9328, 1);
		WorldPoint surface = new WorldPoint(3194, 2926, 0);
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb", "Sand pile", 10950)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb-up", "Sand pile", 10950)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb", "Sand pile", 10949)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(temple, surface,
			"test", TransportType.TRANSPORT, false, "Climb", "Sand heap", 10950)));
	}

	@Test
	public void acceptsOnlyFrozenDirectedManifestRows()
	{
		WorldPoint origin = new WorldPoint(1425, 2933, 0);
		WorldPoint destination = new WorldPoint(1427, 2933, 0);
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Pass-through", "Entryway", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin,
			new WorldPoint(1428, 2933, 0), "test", TransportType.TRANSPORT, false,
			"Pass-through", "Entryway", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Enter", "Entryway", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Pass-through", "Entrance", 54707)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(origin, destination,
			"test", TransportType.TRANSPORT, false, "Pass-through", "Entryway", 54708)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(
			new WorldPoint(1424, 2933, 0), destination, "test", TransportType.TRANSPORT,
			false, "Pass-through", "Entryway", 54707)));
	}

	@Test
	public void acceptsOnlyAuditedDirectHoleIdentities()
	{
		for (int id : new int[]{31791, 28915, 28919, 28920, 28921})
		{
			assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Hole", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Climb", "Hole", id)));
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Unknown", id)));
		}
		for (int id : new int[]{25154, 12656, 28918, 31790})
		{
			assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, UPSTAIRS,
				"test", TransportType.TRANSPORT, false, "Enter", "Hole", id)));
		}
	}

	@Test
	public void acceptsOnlyExactDirectedPohPortalRows()
	{
		WorldPoint outside = new WorldPoint(2953, 3224, 0);
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(outside, SURFACE,
			"RIMMINGTON -> PoH", TransportType.POH,
			true, "Home", "Portal", 15478)));
		assertTrue(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"PoH -> RIMMINGTON", TransportType.POH,
			true, "Enter", "Portal", 4525)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"other", TransportType.POH,
			true, "Teleport", "Portal", 4525)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"other", TransportType.POH,
			true, "Enter", "Magic portal", 4525)));
		assertFalse(CatalogTransitionPolicy.isEligible(new Transport(SURFACE, outside,
			"other", TransportType.TELEPORTATION_PORTAL,
			true, "Enter", "Portal", 4525)));
	}

	@Test
	public void rejectsAdjacentDialogueOpenOnlyAndUnrelatedObjects()
	{
		assertFalse(CatalogTransitionPolicy.isEligible(transport(SURFACE,
			new WorldPoint(101, 100, 0), "Climb-up", "Ladder")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Pay-toll", "Stairs")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Open", "Trapdoor")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Climb-up", "Tree")));
		assertFalse(CatalogTransitionPolicy.isEligible(
			transport(SURFACE, UPSTAIRS, "Cross", "Bridge")));
	}

	private static Transport transport(WorldPoint origin, WorldPoint destination,
		String action, String name)
	{
		return new Transport(origin, destination, "test", TransportType.TRANSPORT,
			false, action, name, 123);
	}

	private static Transport agilityTransport(WorldPoint origin, WorldPoint destination,
		String action, String name)
	{
		return new Transport(origin, destination, "test", TransportType.AGILITY_SHORTCUT,
			false, action, name, 123);
	}

	private static Quest expectedQuest(int objectId)
	{
		switch (objectId)
		{
			case 5008:
			case 5009:
			case 5011:
			case 5012:
			case 5013:
			case 5014:
				return Quest.TROLL_ROMANCE;
			case 6310:
				return Quest.THE_GOLEM;
			case 6621:
				return Quest.ICTHLARINS_LITTLE_HELPER;
			default:
				throw new AssertionError(objectId);
		}
	}
}
