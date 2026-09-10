package net.runelite.client.plugins.microbot.util.walker.navigation;

import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.magic.Rs2Staff;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentTransaction;
import net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentObservation;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NavigationEquipmentRetentionTest
{
	private static final WorldPoint START = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint TARGET = new WorldPoint(3203, 3200, 0);

	@Test
	public void replanRetainsObligationAndRejectsOldGenerationAcknowledgement()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = transaction();
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		NavigationSnapshot before = engine.snapshot();
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(1, 2, START)).getType());
		assertSame(transaction, engine.snapshot().getEquipmentTransaction());
		assertTrue(engine.snapshot().isEquipmentRestorationRequired());
		assertFalse(before.isEquipmentRestorationRequired());
		assertFalse(engine.acknowledgeEquipmentRestored(1, 1, transaction, 4151, -1));
		assertTrue(engine.acknowledgeEquipmentRestored(1, 2, transaction, 4151, -1));
		assertNull(engine.snapshot().getEquipmentTransaction());
		assertSame(transaction, before.getEquipmentTransaction());
		assertTrue(engine.observe(observation(1, 2, START)).issuesInput());
	}

	@Test
	public void cancellationStopsInputAndReplacementRequestInheritsRestoration()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = transaction();
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		assertSame(transaction, engine.cancel("test-stop").getEquipmentTransaction());
		assertFalse(engine.observe(observation(1, 1, START)).issuesInput());
		assertFalse(engine.retainEquipmentTransaction(1, 1, transaction));
		engine.start(request(2, true));
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(2, 1, START)).getType());
		assertSame(transaction, engine.snapshot().getEquipmentTransaction());
		assertFalse(engine.acknowledgeEquipmentRestored(1, 1, transaction, 4151, -1));
		assertTrue(engine.acknowledgeEquipmentRestored(2, 1, transaction, 4151, -1));
		assertTrue(engine.observe(observation(2, 1, START)).issuesInput());
	}

	@Test
	public void neitherSpatialNorExplicitArrivalCanSkipRestoration()
	{
		for (boolean explicit : new boolean[]{false, true})
		{
			NavigationEngine engine = engine();
			SpellEquipmentTransaction transaction = transaction();
			assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
			NavigationObservation arrival = explicit ? NavigationObservation.terminal(
				NavigationObservation.TerminalSignal.ARRIVED, "test-arrival") : observation(1, 1, TARGET);
			assertEquals(NavigationDecision.Type.WAIT, engine.observe(arrival).getType());
			assertFalse(engine.snapshot().isTerminal());
			assertTrue(engine.acknowledgeEquipmentRestored(1, 1, transaction, 4151, -1));
			assertEquals(NavigationDecision.Type.COMPLETE, engine.observe(arrival).getType());
		}
	}

	@Test
	public void wrongEquipmentOrTransactionIdentityCannotClearTheObligation()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = transaction();
		assertFalse(engine.retainEquipmentTransaction(2, 1, transaction));
		assertFalse(engine.retainEquipmentTransaction(1, 2, transaction));
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		assertFalse(engine.retainEquipmentTransaction(1, 1, transaction()));
		assertFalse(engine.acknowledgeEquipmentRestored(1, 1, transaction(), 4151, -1));
		assertFalse(engine.acknowledgeEquipmentRestored(1, 1, transaction, Rs2Staff.STAFF_OF_AIR.getItemID(), -1));
		assertFalse(engine.acknowledgeEquipmentRestored(1, 1, transaction, 4151, 1540));
		assertSame(transaction, engine.snapshot().getEquipmentTransaction());
	}

	@Test
	public void shadowSessionCannotAcquireAnEquipmentObligation()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(request(1, false));
		engine.observe(observation(1, 1, START));
		assertFalse(engine.retainEquipmentTransaction(1, 1, transaction()));
	}

	@Test
	public void restorationDispatchIsOnceOnlyUntilEquipmentAcknowledgesIt()
	{
		for (boolean issued : new boolean[]{true, false})
		{
			NavigationEngine engine = engine();
			SpellEquipmentTransaction transaction = transaction();
			assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
			engine.observe(observation(1, 2, START));
			SpellEquipmentObservation wearingStaff = new SpellEquipmentObservation(
				Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 0);
			NavigationDecision command = engine.observeEquipmentRestoration(1, 2, transaction,
				wearingStaff, observation(1, 2, START));
			assertEquals(NavigationDecision.Type.INTERACT, command.getType());
			assertEquals(RouteInteraction.Kind.SPELL_EQUIPMENT, command.getInteraction().getKind());
			engine.recordCommandResult(command, issued, 1);
			assertEquals(NavigationDecision.Type.WAIT, engine.observeEquipmentRestoration(
				1, 2, transaction, wearingStaff, observation(1, 2, START)).getType());
			engine.observeEquipmentRestoration(1, 2, transaction,
				new SpellEquipmentObservation(4151, -1, Set.of(), 0), observation(1, 2, START));
			assertNull(engine.snapshot().getEquipmentTransaction());
		}
	}

	@Test
	public void unavailableEquipmentNeverMeansAnEmptyWeaponOrPermissionToClick()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = transaction();
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		engine.observe(observation(1, 2, START));
		assertEquals(NavigationDecision.Type.WAIT, engine.observeEquipmentRestoration(
			1, 2, transaction, null, observation(1, 2, START)).getType());
		assertSame(transaction, engine.snapshot().getEquipmentTransaction());
		assertNull(engine.observeEquipmentRestoration(1, 1, transaction, null, observation(1, 2, START)));
		engine.cancel("test-stop");
		assertNull(engine.observeEquipmentRestoration(1, 2, transaction,
			new SpellEquipmentObservation(Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 0),
			observation(1, 2, START)));
	}

	@Test
	public void newRouteGenerationMustInstallBeforeRestorationCanDispatch()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = transaction();
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		engine.observe(observation(1, 2, START));
		SpellEquipmentObservation wearingStaff = new SpellEquipmentObservation(
			Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 0);
		assertNull(engine.observeEquipmentRestoration(1, 2, transaction,
			wearingStaff, observation(1, 3, START)));
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(1, 3, START)).getType());
		assertEquals(3, engine.snapshot().getGeneration());
		assertEquals(NavigationDecision.Type.INTERACT, engine.observeEquipmentRestoration(
			1, 3, transaction, wearingStaff, observation(1, 3, START)).getType());
	}

	@Test
	public void restorationTimeoutRetainsObligationForTheNextRequest()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = transaction();
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		engine.observe(observation(1, 2, START));
		assertEquals(NavigationDecision.Type.WAIT, engine.observeEquipmentRestoration(
			1, 2, transaction, null, observation(1, 2, START)).getType());
		NavigationObservation later = NavigationObservation.route(5001, START,
			observation(1, 2, START).getRoutePlan(), false, false, false,
			false, false, false, null, "equipment-timeout");
		assertEquals(NavigationDecision.Type.FAIL, engine.observeEquipmentRestoration(
			1, 2, transaction, null, later).getType());
		assertSame(transaction, engine.snapshot().getEquipmentTransaction());
		engine.start(request(2, true));
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(observation(2, 1, START)).getType());
		assertEquals(NavigationDecision.Type.INTERACT, engine.observeEquipmentRestoration(
			2, 1, transaction, new SpellEquipmentObservation(Rs2Staff.STAFF_OF_AIR.getItemID(),
				-1, Set.of(4151), 0), observation(2, 1, START)).getType());
	}

	@Test
	public void emptyOriginalSlotRequiresSpaceAndManualChangesAreNotOverwritten()
	{
		NavigationEngine engine = engine();
		SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, -1, -1);
		assertTrue(engine.retainEquipmentTransaction(1, 1, transaction));
		engine.observe(observation(1, 2, START));
		assertEquals(NavigationDecision.Type.WAIT, engine.observeEquipmentRestoration(1, 2, transaction,
			new SpellEquipmentObservation(Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(), 0),
			observation(1, 2, START)).getType());
		NavigationDecision removal = engine.observeEquipmentRestoration(1, 2, transaction,
			new SpellEquipmentObservation(Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(), 1),
			observation(1, 2, START));
		assertEquals("REMOVE_STAFF", removal.getInteraction().getAction());
		assertEquals(NavigationDecision.Type.FAIL, engine.observeEquipmentRestoration(1, 2, transaction,
			new SpellEquipmentObservation(4151, -1, Set.of(), 1), observation(1, 2, START)).getType());
		assertNull(engine.snapshot().getEquipmentTransaction());
	}

	@Test
	public void tabPreparationWaitsForVisibilityBeforeIssuingTheEquipmentCommand()
	{
		for (int original : new int[]{4151, -1})
		{
			NavigationEngine engine = engine();
			SpellEquipmentTransaction transaction = new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, original, -1);
			engine.retainEquipmentTransaction(1, 1, transaction);
			engine.observe(observation(1, 2, START));
			SpellEquipmentObservation closed = new SpellEquipmentObservation(
				Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 1, false, false);
			NavigationDecision tab = engine.observeEquipmentRestoration(1, 2, transaction, closed, observation(1, 2, START));
			assertEquals(original > 0 ? "OPEN_INVENTORY" : "OPEN_EQUIPMENT", tab.getInteraction().getAction());
			engine.recordCommandResult(tab, true, 1);
			assertEquals(NavigationDecision.Type.WAIT, engine.observeEquipmentRestoration(
				1, 2, transaction, closed, observation(1, 2, START)).getType());
			NavigationDecision equip = engine.observeEquipmentRestoration(1, 2, transaction,
				new SpellEquipmentObservation(Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 1, true, true),
				observation(1, 2, START));
			assertEquals(original > 0 ? "RESTORE_WEAPON" : "REMOVE_STAFF", equip.getInteraction().getAction());
			engine.recordCommandResult(equip, true, 1);
			assertEquals(NavigationDecision.Type.WAIT, engine.observeEquipmentRestoration(
				1, 2, transaction, closed, observation(1, 2, START)).getType());
		}
	}

	@Test(timeout = 5000)
	public void runtimeEquipmentDispatchDoesNotHoldMutexAndCancellationRevokesPermission() throws Exception
	{
		NavigationEngineRuntime.resetForTesting();
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
		try
		{
			NavigationEngineRuntime.ensureRequest(request(1, true));
			NavigationEngineRuntime.observe(observation(1, 1, START));
			SpellEquipmentTransaction transaction = transaction();
			assertTrue(NavigationEngineRuntime.retainEquipmentTransaction(1, 1, transaction));
			NavigationEngineRuntime.observe(observation(1, 2, START));
			WalkerActions actions = new WalkerActions()
			{
				@Override
				public boolean clickTile(WorldPoint target) { throw new AssertionError("Unexpected movement"); }
				@Override
				public SpellEquipmentObservation observeEquipment()
				{
					return new SpellEquipmentObservation(Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 0);
				}
				@Override
				public boolean interactEquipment(RouteInteraction interaction, SpellEquipmentTransaction retained,
					java.util.function.BooleanSupplier permitted)
				{
					assertSame(transaction, retained);
					assertTrue(permitted.getAsBoolean());
					try
					{
						executor.submit(() -> {
							assertEquals("equipment-dispatch-in-flight", NavigationEngineRuntime.execute(
								observation(1, 2, START), this).getDecision().getReason());
							NavigationEngineRuntime.cancel("concurrent-stop");
						}).get(2, java.util.concurrent.TimeUnit.SECONDS);
					}
					catch (Exception failure) { throw new AssertionError(failure); }
					assertFalse(permitted.getAsBoolean());
					return false;
				}
			};
			assertFalse(NavigationEngineRuntime.execute(observation(1, 2, START), actions).isCommandIssued());
			assertSame(transaction, NavigationEngineRuntime.getSnapshot().getEquipmentTransaction());
			NavigationEngineRuntime.ensureRequest(request(2, true));
			assertEquals(NavigationDecision.Type.WAIT, NavigationEngineRuntime.execute(
				observation(2, 1, START), actions).getDecision().getType());
		}
		finally
		{
			executor.shutdownNow();
			NavigationEngineRuntime.resetForTesting();
		}
	}

	@Test
	public void spellPreparationOwnsEquipCastAndRestorationThroughLanding()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(request(1, true));
		RoutePlan plan = new RoutePlan(1, 1, START, Set.of(TARGET), List.of(START, TARGET),
			List.of(START, TARGET), true, List.of(new RouteEdge(0, START, TARGET, RouteEdge.Kind.SIMPLE_TELEPORT)));
		RouteInteraction spell = new RouteInteraction(1, 0, START, TARGET, START,
			RouteInteraction.Kind.SIMPLE_TELEPORT, RouteInteraction.Status.AVAILABLE, "Varrock Teleport", true,
			net.runelite.client.plugins.microbot.shortestpath.TransportType.TELEPORTATION_SPELL.ordinal());
		NavigationObservation before = NavigationObservation.route(1, START, plan, false, false,
			false, false, false, false, null, "spell-staging").withRouteInteraction(spell);
		SpellEquipmentTransaction transaction = transaction();
		SpellEquipmentObservation inventoryClosed = new SpellEquipmentObservation(4151, -1,
			Set.of(Rs2Staff.STAFF_OF_AIR.getItemID()), 0, false, false);
		NavigationDecision open = engine.prepareSpellEquipment(engine.observe(before), transaction,
			inventoryClosed, true, before);
		assertEquals("OPEN_INVENTORY", open.getInteraction().getAction());
		assertSame(transaction, engine.snapshot().getEquipmentTransaction());
		engine.recordCommandResult(open, true, 1);
		assertEquals(NavigationDecision.Type.WAIT, engine.prepareSpellEquipment(engine.observe(before),
			transaction, inventoryClosed, true, before).getType());
		NavigationDecision equip = engine.prepareSpellEquipment(engine.observe(before), transaction,
			new SpellEquipmentObservation(4151, -1, Set.of(Rs2Staff.STAFF_OF_AIR.getItemID()), 0), true, before);
		assertEquals("EQUIP_STAFF", equip.getInteraction().getAction());
		engine.recordCommandResult(equip, true, 1);
		SpellEquipmentObservation staffWorn = new SpellEquipmentObservation(
			Rs2Staff.STAFF_OF_AIR.getItemID(), -1, Set.of(4151), 0);
		NavigationDecision cast = engine.prepareSpellEquipment(engine.observe(before), transaction, staffWorn, true, before);
		assertEquals(RouteInteraction.Kind.SIMPLE_TELEPORT, cast.getInteraction().getKind());
		engine.recordCommandResult(cast, true, 1);
		NavigationObservation landed = NavigationObservation.route(2, TARGET, plan, false, false,
			false, false, false, false, null, "spell-landing")
			.withRouteInteraction(spell.withStatus(RouteInteraction.Status.CLEARED, false));
		assertEquals(NavigationDecision.Type.WAIT, engine.observe(landed).getType());
		assertTrue(engine.snapshot().isEquipmentRestorationRequired());
		NavigationDecision restore = engine.observeEquipmentRestoration(1, 1, transaction, staffWorn, landed);
		assertEquals("RESTORE_WEAPON", restore.getInteraction().getAction());
		engine.recordCommandResult(restore, true, 2);
		engine.observeEquipmentRestoration(1, 1, transaction,
			new SpellEquipmentObservation(4151, -1, Set.of(), 0), landed);
		engine.observe(landed);
		assertEquals(NavigationDecision.Type.COMPLETE, engine.observe(landed.withRouteInteraction(null)).getType());
	}

	@Test
	public void runtimePreparesStaffBeforeDispatchingTheCast()
	{
		NavigationEngineRuntime.resetForTesting();
		try
		{
			NavigationEngineRuntime.ensureRequest(request(1, true));
			RoutePlan plan = new RoutePlan(1, 1, START, Set.of(TARGET), List.of(START, TARGET),
				List.of(START, TARGET), true, List.of(new RouteEdge(0, START, TARGET, RouteEdge.Kind.SIMPLE_TELEPORT)));
			RouteInteraction spell = new RouteInteraction(1, 0, START, TARGET, START,
				RouteInteraction.Kind.SIMPLE_TELEPORT, RouteInteraction.Status.AVAILABLE, "Varrock Teleport", true,
				net.runelite.client.plugins.microbot.shortestpath.TransportType.TELEPORTATION_SPELL.ordinal());
			NavigationObservation before = NavigationObservation.route(1, START, plan, false, false,
				false, false, false, false, null, "spell-runtime").withRouteInteraction(spell);
			SpellEquipmentTransaction transaction = transaction();
			java.util.concurrent.atomic.AtomicInteger equips = new java.util.concurrent.atomic.AtomicInteger();
			java.util.concurrent.atomic.AtomicInteger casts = new java.util.concurrent.atomic.AtomicInteger();
			java.util.concurrent.atomic.AtomicBoolean worn = new java.util.concurrent.atomic.AtomicBoolean();
			java.util.concurrent.atomic.AtomicBoolean available = new java.util.concurrent.atomic.AtomicBoolean(true);
			WalkerActions actions = new WalkerActions()
			{
				@Override
				public boolean clickTile(WorldPoint target) { throw new AssertionError("Unexpected movement"); }
				@Override
				public net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentPreparation observeSpellEquipment(
					RouteInteraction observed, SpellEquipmentTransaction retained)
				{
					assertTrue(retained == null || retained == transaction);
					if (!available.get()) return null;
					return new net.runelite.client.plugins.microbot.util.walker.banking.SpellEquipmentPreparation(
						observed, transaction, new SpellEquipmentObservation(worn.get()
						? Rs2Staff.STAFF_OF_AIR.getItemID() : 4151, -1,
						Set.of(4151, Rs2Staff.STAFF_OF_AIR.getItemID()), 0), true);
				}
				@Override
				public boolean interactEquipment(RouteInteraction interaction, SpellEquipmentTransaction retained,
					java.util.function.BooleanSupplier permitted)
				{
					assertTrue(permitted.getAsBoolean());
					assertEquals("EQUIP_STAFF", interaction.getAction());
					equips.incrementAndGet();
					return true;
				}
				@Override
				public boolean interact(RouteInteraction interaction)
				{
					assertTrue(worn.get());
					assertEquals(RouteInteraction.Kind.SIMPLE_TELEPORT, interaction.getKind());
					casts.incrementAndGet();
					return true;
				}
			};
			assertTrue(NavigationEngineRuntime.execute(before, actions).isCommandIssued());
			assertEquals(1, equips.get());
			assertEquals(0, casts.get());
			available.set(false);
			assertEquals(NavigationDecision.Type.WAIT, NavigationEngineRuntime.execute(before, actions).getDecision().getType());
			assertEquals(0, casts.get());
			assertSame(transaction, NavigationEngineRuntime.getSnapshot().getEquipmentTransaction());
			available.set(true);
			worn.set(true);
			assertTrue(NavigationEngineRuntime.execute(before, actions).isCommandIssued());
			assertEquals(1, equips.get());
			assertEquals(1, casts.get());
		}
		finally { NavigationEngineRuntime.resetForTesting(); }
	}

	private static SpellEquipmentTransaction transaction()
	{
		return new SpellEquipmentTransaction(Rs2Staff.STAFF_OF_AIR, 4151, -1);
	}

	private static NavigationEngine engine()
	{
		NavigationEngine engine = new NavigationEngine();
		engine.start(request(1, true));
		engine.observe(observation(1, 1, START));
		return engine;
	}

	private static NavigationRequest request(long requestId, boolean enabled)
	{
		return new NavigationRequest(requestId, Set.of(TARGET), 0,
			new NavigationRouteOptions(true, true, true, enabled), "equipment-test");
	}

	private static NavigationObservation observation(long requestId, long generation, WorldPoint player)
	{
		List<WorldPoint> path = List.of(START, new WorldPoint(3201, 3200, 0),
			new WorldPoint(3202, 3200, 0), TARGET);
		RoutePlan plan = new RoutePlan(requestId, generation, START, Set.of(TARGET), path, path, true);
		return NavigationObservation.route(1, player, plan, false, false, false,
			false, false, false, null, "equipment-test");
	}
}
