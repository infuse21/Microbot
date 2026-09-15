package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.magic.Runes;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.util.walker.banking.BankedSpellEquipmentPlanner;
import net.runelite.client.plugins.microbot.util.walker.banking.BankedTransportCoordinator;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentObservation;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentPreparation;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankedStaffJourneyTest
{
	private static final WorldPoint BANK = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TARGET = new WorldPoint(3212, 3424, 0);
	private static final List<Map<Runes, Integer>> CASTS = List.of(Map.of(Runes.AIR, 3, Runes.FIRE, 1, Runes.LAW, 1));

	@Test
	public void bankOnlyStaffFeedsFinalReplanCastAndRestoration()
	{
		NavigationEngineRuntime.resetForTesting();
		try
		{
			int staffId = Rs2Staff.STAFF_OF_AIR.getItemID();
			BankedSpellEquipmentPlanner.Plan selection = BankedSpellEquipmentPlanner.choose(CASTS,
				Map.of(), Map.of(Runes.FIRE, 1, Runes.LAW, 1), Set.of(staffId), Rs2Staff.NONE, 1, 25, false);
			assertNotNull(selection);
			assertEquals(Rs2Staff.STAFF_OF_AIR, selection.getStaff());
			Map<Integer, Integer> required = new HashMap<>(selection.getRuneWithdrawals());
			required.put(staffId, 1);
			Map<Integer, Integer> inventory = new HashMap<>();
			BankedTransportCoordinator.Operations bank = mock(BankedTransportCoordinator.Operations.class);
			when(bank.walk(BANK, 0)).thenAnswer(call ->
			{
				NavigationEngineRuntime.ensureRequest(request(1, BANK));
				RoutePlan plan = new RoutePlan(1, 1, BANK, Set.of(BANK), List.of(BANK), List.of(BANK), true);
				assertEquals(NavigationDecision.Type.COMPLETE, NavigationEngineRuntime.execute(
					observation(BANK, plan), mock(WalkerActions.class)).getDecision().getType());
				return WalkerState.ARRIVED;
			});
			when(bank.openBank()).thenReturn(true);
			when(bank.awaitBankOpen()).thenReturn(true);
			when(bank.awaitBankClosed()).thenReturn(true);
			when(bank.hasBankItem(anyInt(), anyInt())).thenAnswer(call ->
				required.getOrDefault(call.getArgument(0), 0) >= (int) call.getArgument(1));
			when(bank.withdraw(anyInt(), anyInt())).thenAnswer(call ->
			{
				inventory.merge(call.getArgument(0), call.getArgument(1), Integer::sum);
				return true;
			});
			when(bank.awaitInventoryQuantity(anyInt(), anyInt())).thenAnswer(call ->
				inventory.getOrDefault(call.getArgument(0), 0) >= (int) call.getArgument(1));
			when(bank.walk(TARGET, 0)).thenAnswer(call ->
			{
				verify(bank).closeBank();
				verify(bank).prepareFinalRoute(TARGET);
				assertEquals(required, inventory);
				return executeFinalLeg(inventory);
			});
			BankedTransportCoordinator.Result result = BankedTransportCoordinator.execute(BANK, required, TARGET, 0, bank);
			assertEquals(BankedTransportCoordinator.Phase.COMPLETE, result.getPhase());
			assertEquals(Map.of(staffId, 1), inventory);
			verify(bank).withdraw(staffId, 1);
			verify(bank, times(3)).withdraw(anyInt(), anyInt());
		}
		finally { NavigationEngineRuntime.resetForTesting(); }
	}

	private static WalkerState executeFinalLeg(Map<Integer, Integer> inventory)
	{
		BankedSpellEquipmentPlanner.Plan carried = BankedSpellEquipmentPlanner.choose(CASTS,
			Map.of(Runes.FIRE, inventory.get(Runes.FIRE.getItemId()), Runes.LAW, inventory.get(Runes.LAW.getItemId())),
			Map.of(), inventory.keySet(), Rs2Staff.NONE, 1, 25, false);
		assertNotNull(carried);
		assertTrue(carried.getRuneWithdrawals().isEmpty());
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(carried.getStaff(), 4151, -1);
		int staffId = carried.getStaff().getItemID();
		java.util.concurrent.atomic.AtomicInteger weapon = new java.util.concurrent.atomic.AtomicInteger(4151);
		java.util.concurrent.atomic.AtomicInteger commands = new java.util.concurrent.atomic.AtomicInteger();
		WalkerActions actions = new WalkerActions()
		{
			@Override public boolean clickTile(WorldPoint target) { throw new AssertionError("Unexpected movement"); }
			@Override public SpellEquipmentObservation observeEquipment() {
				return new SpellEquipmentObservation(weapon.get(), -1, inventory.keySet(), 20);
			}
			@Override public SpellEquipmentPreparation observeSpellEquipment(RouteInteraction spell, SpellEquipmentTransaction retained) {
				assertTrue(retained == null || retained == transaction);
				return new SpellEquipmentPreparation(spell, transaction, observeEquipment(), true);
			}
			@Override public boolean interactEquipment(RouteInteraction interaction, SpellEquipmentTransaction retained,
				java.util.function.BooleanSupplier permitted) {
				assertTrue(permitted.getAsBoolean());
				assertSame(transaction, retained);
				assertEquals(commands.get() == 0 ? "EQUIP_STAFF" : "RESTORE_WEAPON", interaction.getAction());
				assertNotNull(inventory.remove(interaction.getObjectId()));
				inventory.put(weapon.getAndSet(interaction.getObjectId()), 1);
				commands.incrementAndGet();
				return true;
			}
			@Override public boolean interact(RouteInteraction interaction) {
				assertEquals(RouteInteraction.Kind.SIMPLE_TELEPORT, interaction.getKind());
				assertEquals(staffId, weapon.get());
				assertEquals(Integer.valueOf(1), inventory.remove(Runes.LAW.getItemId()));
				assertEquals(Integer.valueOf(1), inventory.remove(Runes.FIRE.getItemId()));
				commands.incrementAndGet();
				return true;
			}
		};
		NavigationEngineRuntime.ensureRequest(request(2, TARGET));
		RoutePlan plan = new RoutePlan(2, 1, BANK, Set.of(TARGET), List.of(BANK, TARGET), List.of(BANK, TARGET), true,
			List.of(new RouteEdge(0, BANK, TARGET, RouteEdge.Kind.SIMPLE_TELEPORT)));
		RouteInteraction spell = new RouteInteraction(1, 0, BANK, TARGET, BANK, RouteInteraction.Kind.SIMPLE_TELEPORT,
			RouteInteraction.Status.AVAILABLE, "Varrock Teleport", true, TransportType.TELEPORTATION_SPELL.ordinal());
		NavigationObservation before = observation(BANK, plan).withRouteInteraction(spell);
		assertTrue(NavigationEngineRuntime.execute(before, actions).isCommandIssued());
		assertTrue(NavigationEngineRuntime.execute(before, actions).isCommandIssued());
		NavigationObservation landed = observation(TARGET, plan).withRouteInteraction(spell.withStatus(RouteInteraction.Status.CLEARED, false));
		assertEquals(NavigationDecision.Type.WAIT, NavigationEngineRuntime.execute(landed, actions).getDecision().getType());
		assertTrue(NavigationEngineRuntime.execute(landed, actions).isCommandIssued());
		assertFalse(NavigationEngineRuntime.execute(landed, actions).isCommandIssued());
		NavigationEngineRuntime.execute(landed, actions);
		assertEquals(NavigationDecision.Type.COMPLETE,
			NavigationEngineRuntime.execute(landed.withRouteInteraction(null), actions).getDecision().getType());
		assertEquals(3, commands.get());
		assertEquals(4151, weapon.get());
		return WalkerState.ARRIVED;
	}

	private static NavigationRequest request(long id, WorldPoint target) {
		return new NavigationRequest(id, Set.of(target), 0, new NavigationRouteOptions(true, true, true), "banked-staff-test");
	}

	private static NavigationObservation observation(WorldPoint player, RoutePlan plan) {
		return NavigationObservation.route(1, player, plan, false, false, false, false, false, false, "banked-staff-test");
	}
}
