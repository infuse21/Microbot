package net.runelite.client.plugins.microbot.util.walker.navigation;

import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationEngineExecutionTest
{
	private static final WorldPoint A = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint B = new WorldPoint(3201, 3200, 0);
	private static final WorldPoint C = new WorldPoint(3202, 3200, 0);
	private static final WorldPoint UP = new WorldPoint(3200, 3200, 1);
	private static final WorldPoint UP_TARGET = new WorldPoint(3201, 3200, 1);

	@After
	public void resetRuntime()
	{
		NavigationEngineRuntime.resetForTesting();
	}

	@Test
	public void ordinaryRequestIssuesOnlyOneCommandWhileAwaitingAcknowledgement()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();

		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, A, ordinaryPlan(1), false, false), target -> {
				commands.incrementAndGet();
				return true;
			});
		NavigationExecutionResult second = NavigationEngineRuntime.execute(
			observation(100, A, ordinaryPlan(1), false, false), target -> {
				commands.incrementAndGet();
				return true;
			});

		assertTrue(first.isEngineOwned());
		assertTrue(first.isCommandIssued());
		assertEquals(NavigationDecision.Type.WAIT, second.getDecision().getType());
		assertEquals(1, commands.get());
	}

	@Test
	public void ordinaryRequestDispatchesInteractionThroughAdapterOnce()
	{
		startEngineRequest();
		AtomicInteger interactions = new AtomicInteger();
		RouteInteraction mineable = new RouteInteraction(1, 0, A, B, B,
			RouteInteraction.Kind.MINEABLE, RouteInteraction.Status.AVAILABLE, "mine", true);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				interactions.incrementAndGet();
				return true;
			}
		};

		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, A, ordinaryPlan(1), false, false).withRouteInteraction(mineable), actions);
		NavigationExecutionResult second = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(1), false, false).withRouteInteraction(mineable), actions);

		assertEquals(NavigationDecision.Type.INTERACT, first.getDecision().getType());
		assertTrue(first.isCommandIssued());
		assertEquals(NavigationDecision.Type.WAIT, second.getDecision().getType());
		assertEquals(1, interactions.get());
	}

	@Test
	public void reachedDistanceCannotCompleteBeforePendingDoorEdgeIsCrossed()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 2, options, "door-arrival-test"));
		RouteInteraction availableDoor = new RouteInteraction(1, 0, A, B, B,
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.AVAILABLE, "open", true);
		RouteInteraction clearedDoor = new RouteInteraction(1, 0, A, B, B,
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.CLEARED, "open", true);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				return true;
			}
		};

		NavigationExecutionResult interact = NavigationEngineRuntime.execute(
			observation(1, A, ordinaryPlan(1), false, false)
				.withRouteInteraction(availableDoor), actions);
		NavigationExecutionResult observeCleared = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(1), false, false)
				.withRouteInteraction(clearedDoor), actions);
		NavigationExecutionResult cross = NavigationEngineRuntime.execute(
			observation(3, A, ordinaryPlan(1), false, false)
				.withRouteInteraction(clearedDoor), actions);
		NavigationExecutionResult retire = NavigationEngineRuntime.execute(
			observation(4, B, ordinaryPlan(1), false, false)
				.withRouteInteraction(clearedDoor), actions);
		NavigationExecutionResult arrived = NavigationEngineRuntime.execute(
			observation(5, B, ordinaryPlan(1), false, false), actions);

		assertEquals(NavigationDecision.Type.INTERACT, interact.getDecision().getType());
		assertEquals(NavigationDecision.Type.WAIT, observeCleared.getDecision().getType());
		assertEquals(NavigationDecision.Type.CLICK_TILE, cross.getDecision().getType());
		assertEquals(C, cross.getDecision().getTarget());
		assertEquals("raw-route-lookahead", cross.getDecision().getTargetSelection());
		assertEquals(NavigationDecision.Type.WAIT, retire.getDecision().getType());
		assertEquals(NavigationDecision.Type.COMPLETE, arrived.getDecision().getType());
	}

	@Test
	public void clearedInteractionChainsDirectlyToNextReadyInteraction()
	{
		startEngineRequest();
		RouteInteraction firstDoor = new RouteInteraction(1, 0, A, B, B,
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.AVAILABLE, "open", true);
		RouteInteraction clearedFirstDoor = firstDoor.withStatus(
			RouteInteraction.Status.CLEARED, true);
		RouteInteraction secondDoor = new RouteInteraction(1, 1, B, C, C,
			RouteInteraction.Kind.DOOR, RouteInteraction.Status.AVAILABLE, "open", true);
		AtomicInteger clicks = new AtomicInteger();
		AtomicInteger interactions = new AtomicInteger();
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				clicks.incrementAndGet();
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				interactions.incrementAndGet();
				return true;
			}
		};

		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, A, ordinaryPlan(1), false, false)
				.withRouteInteraction(firstDoor), actions);
		NavigationExecutionResult chained = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(1), false, false)
				.withRouteInteractions(clearedFirstDoor, secondDoor), actions);

		assertEquals(NavigationDecision.Type.INTERACT, first.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, chained.getDecision().getType());
		assertEquals("interaction-chain-ready", chained.getDecision().getReason());
		assertEquals(secondDoor, chained.getDecision().getInteraction());
		assertEquals(0, clicks.get());
		assertEquals(2, interactions.get());
	}

	@Test
	public void rejectedClickRequestsReplanWithoutSecondInput()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		WalkerActions rejecting = target -> {
			commands.incrementAndGet();
			return false;
		};

		NavigationEngineRuntime.execute(observation(1, A, ordinaryPlan(1), false, false), rejecting);
		NavigationExecutionResult next = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(1), false, false), rejecting);

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, next.getDecision().getType());
		assertFalse(next.isCommandIssued());
		assertEquals(1, commands.get());
	}

	@Test
	public void registeredOffRouteDestinationTriggersImmediateCategorizedReplan()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, A, ordinaryPlan(1), false, false), target -> {
				commands.incrementAndGet();
				return true;
			});
		WorldPoint offRouteDestination = new WorldPoint(3182, 3490, 0);

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(1), false, false)
				.withMovementDestination(offRouteDestination), target -> {
					commands.incrementAndGet();
					return true;
				});

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, result.getDecision().getType());
		assertEquals(RecoveryCause.COMMAND_DESTINATION_MISMATCH,
			result.getDecision().getRecoveryCause());
		assertEquals(first.getDecision().getTarget(),
			result.getDecision().getRecoveryExpectedTarget());
		assertEquals(offRouteDestination,
			result.getDecision().getRecoveryObservedDestination());
		assertEquals(1, result.getDecision().getRecoveryAttempt());
		assertEquals(2, result.getDecision().getRecoveryBudget());
		assertEquals(1, commands.get());
	}

	@Test
	public void routeBackedRegisteredDestinationAcknowledgesCommand()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, A, ordinaryPlan(1), false, false), target -> {
				commands.incrementAndGet();
				return true;
			});

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(1), false, false)
				.withMovementDestination(first.getDecision().getTarget()), target -> {
					commands.incrementAndGet();
					return true;
				});

		assertEquals(NavigationDecision.Type.WAIT, result.getDecision().getType());
		assertEquals("movement-command-progress-window", result.getDecision().getReason());
		assertEquals(0, NavigationEngineRuntime.getSnapshot().getRecoveryAttempts(
			RecoveryCause.COMMAND_DESTINATION_MISMATCH));
		assertEquals(1, commands.get());
	}

	@Test
	public void movingRouteHandsOffOnlyInsideCommandProximityWindow()
	{
		List<WorldPoint> raw = new ArrayList<>();
		for (int i = 0; i <= 25; i++)
		{
			raw.add(new WorldPoint(3200 + i, 3200, 0));
		}
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(raw.get(25)), 0, options, "handoff-test"));
		RoutePlan plan = new RoutePlan(21, 1, raw.get(0), Collections.singleton(raw.get(25)),
			raw, Arrays.asList(raw.get(0), raw.get(25)), true);
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};

		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, raw.get(0), plan, false, false), actions);
		int targetIndex = first.getDecision().getTargetRawIndex();
		int handoff = first.getDecision().getTargetHandoffDistance();
		NavigationExecutionResult early = NavigationEngineRuntime.execute(
			observation(2, raw.get(targetIndex - handoff - 1), plan, true, false), actions);
		NavigationExecutionResult near = NavigationEngineRuntime.execute(
			observation(3, raw.get(targetIndex - handoff), plan, true, false), actions);

		assertTrue(handoff >= 2 && handoff <= 4);
		assertEquals(NavigationDecision.Type.WAIT, early.getDecision().getType());
		assertEquals(NavigationDecision.Type.CLICK_TILE, near.getDecision().getType());
		assertEquals("proximity-route-handoff", near.getDecision().getReason());
		assertEquals(2, commands.get());
	}

	@Test
	public void proximityHandoffStrictlyAdvancesAndCannotRepeatItsTarget()
	{
		List<WorldPoint> raw = new ArrayList<>();
		for (int i = 0; i <= 30; i++)
		{
			raw.add(new WorldPoint(3200 + i, 3200, 0));
		}
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(raw.get(30)), 0, options, "handoff-forward-test"));
		RoutePlan plan = new RoutePlan(21, 1, raw.get(0), Collections.singleton(raw.get(30)),
			raw, Arrays.asList(raw.get(0), raw.get(30)), true);
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};

		NavigationExecutionResult first = NavigationEngineRuntime.execute(
			observation(1, raw.get(0), plan, false, false), actions);
		int firstTarget = first.getDecision().getTargetRawIndex();
		int handoff = first.getDecision().getTargetHandoffDistance();
		WorldPoint nearFirstTarget = raw.get(firstTarget - handoff);
		NavigationExecutionResult second = NavigationEngineRuntime.execute(
			observation(2, nearFirstTarget, plan, true, false), actions);
		NavigationExecutionResult repeated = NavigationEngineRuntime.execute(
			observation(3, nearFirstTarget, plan, true, false), actions);

		assertEquals(NavigationDecision.Type.CLICK_TILE, second.getDecision().getType());
		assertTrue(second.getDecision().getTargetRawIndex() > firstTarget);
		assertEquals(NavigationDecision.Type.WAIT, repeated.getDecision().getType());
		assertEquals(2, commands.get());
	}

	@Test
	public void finalRouteCommandDoesNotRetargetWhileMoving()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};

		NavigationEngineRuntime.execute(observation(1, A, ordinaryPlan(1), false, false), actions);
		NavigationExecutionResult moving = NavigationEngineRuntime.execute(
			observation(2, B, ordinaryPlan(1), true, false), actions);

		assertEquals(NavigationDecision.Type.WAIT, moving.getDecision().getType());
		assertEquals(1, commands.get());
	}

	@Test
	public void transportRouteLocksRequestToLegacyWithoutInput()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(1, A, transportPlan(1), false, false), target -> {
				commands.incrementAndGet();
				return true;
			});

		assertFalse(result.isEngineOwned());
		assertEquals(NavigationExecutionMode.LEGACY_LOCKED,
			NavigationEngineRuntime.getSnapshot().getExecutionMode());
		assertEquals(0, commands.get());
	}

	@Test
	public void migratedAdjacentTransportIsEngineOwnedAndInteracted()
	{
		startEngineRequest();
		AtomicInteger interactions = new AtomicInteger();
		RouteInteraction transport = new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.ADJACENT_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Climb-over", true, 123);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return false;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				interactions.incrementAndGet();
				return true;
			}
		};

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(1, A, adjacentTransportPlan(1), false, false)
				.withRouteInteraction(transport), actions);

		assertTrue(result.isEngineOwned());
		assertEquals(NavigationExecutionMode.ENGINE_SUPPORTED,
			NavigationEngineRuntime.getSnapshot().getExecutionMode());
		assertEquals(NavigationDecision.Type.INTERACT, result.getDecision().getType());
		assertEquals(1, interactions.get());
	}

	@Test
	public void reachedDistanceCannotCompleteBeforePublishedAdjacentTransportIsCrossed()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 2, options, "adjacent-transport-arrival-test"));
		AtomicInteger commands = new AtomicInteger();

		NavigationExecutionResult beforeEdge = NavigationEngineRuntime.execute(
			observation(1, A, adjacentTransportPlan(1), false, false), target -> {
				commands.incrementAndGet();
				return true;
			});

		assertTrue(beforeEdge.isEngineOwned());
		assertEquals(NavigationDecision.Type.CLICK_TILE, beforeEdge.getDecision().getType());
		assertEquals(1, commands.get());
		assertFalse(NavigationEngineRuntime.getSnapshot().getPhase().isTerminal());

		NavigationExecutionResult afterEdge = NavigationEngineRuntime.execute(
			observation(2, B, adjacentTransportPlan(1), false, false)
				.withMovementDestination(C), target -> true);

		assertEquals(NavigationDecision.Type.COMPLETE, afterEdge.getDecision().getType());
	}

	@Test
	public void catalogTransitionIsEngineOwnedAndCompletesOnlyAfterLanding()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(UP_TARGET), 0, options, "catalog-transition-test"));
		AtomicInteger interactions = new AtomicInteger();
		RouteInteraction transition = new RouteInteraction(1, 0, A, UP, A,
			RouteInteraction.Kind.CATALOG_TRANSITION, RouteInteraction.Status.AVAILABLE,
			"Climb-up", true, 123, A, UP);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				interactions.incrementAndGet();
				return true;
			}
		};

		NavigationExecutionResult interact = NavigationEngineRuntime.execute(
			observation(1, A, catalogTransitionPlan(1), false, false)
				.withRouteInteraction(transition), actions);
		NavigationExecutionResult landed = NavigationEngineRuntime.execute(
			observation(2, UP, catalogTransitionPlan(1), false, false)
				.withRouteInteraction(transition.withStatus(
					RouteInteraction.Status.CLEARED, false)), actions);
		NavigationExecutionResult arrived = NavigationEngineRuntime.execute(
			observation(3, UP_TARGET, catalogTransitionPlan(1), false, false), actions);

		assertTrue(interact.isEngineOwned());
		assertEquals(NavigationDecision.Type.INTERACT, interact.getDecision().getType());
		assertEquals(1, interactions.get());
		assertEquals("interaction-edge-crossed", landed.getDecision().getReason());
		assertEquals(NavigationDecision.Type.COMPLETE, arrived.getDecision().getType());
	}

	@Test
	public void simpleTeleportIsEngineOwnedAndAcknowledgedAtLanding()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 0, options, "simple-teleport-test"));
		AtomicInteger interactions = new AtomicInteger();
		RouteInteraction teleport = new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.SIMPLE_TELEPORT, RouteInteraction.Status.AVAILABLE,
			"Varrock Teleport", true, 1, A, B);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				interactions.incrementAndGet();
				return true;
			}
		};

		NavigationExecutionResult interact = NavigationEngineRuntime.execute(
			observation(1, A, simpleTeleportPlan(1), false, false)
				.withRouteInteraction(teleport), actions);
		NavigationExecutionResult landed = NavigationEngineRuntime.execute(
			observation(2, B, simpleTeleportPlan(1), false, false)
				.withRouteInteraction(teleport.withStatus(
					RouteInteraction.Status.CLEARED, false)), actions);

		assertTrue(interact.isEngineOwned());
		assertEquals(NavigationDecision.Type.INTERACT, interact.getDecision().getType());
		assertEquals(1, interactions.get());
		assertTrue(landed.isEngineOwned());
		assertFalse(NavigationEngineRuntime.getSnapshot().getPhase().isTerminal());
	}

	@Test
	public void npcTransportIsEngineOwnedAndAcknowledgedAtLanding()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 0, options, "npc-transport-test"));
		AtomicInteger interactions = new AtomicInteger();
		RouteInteraction transport = new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.NPC_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Follow", true, 4968, A, B);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				interactions.incrementAndGet();
				return true;
			}
		};

		NavigationExecutionResult interact = NavigationEngineRuntime.execute(
			observation(1, A, npcTransportPlan(1), false, false)
				.withRouteInteraction(transport), actions);
		NavigationExecutionResult landed = NavigationEngineRuntime.execute(
			observation(2, B, npcTransportPlan(1), false, false)
				.withRouteInteraction(transport.withStatus(
					RouteInteraction.Status.CLEARED, false)), actions);

		assertTrue(interact.isEngineOwned());
		assertEquals(NavigationDecision.Type.INTERACT, interact.getDecision().getType());
		assertEquals(1, interactions.get());
		assertTrue(landed.isEngineOwned());
		assertFalse(NavigationEngineRuntime.getSnapshot().getPhase().isTerminal());
	}

	@Test
	public void charterShipAdvancesThroughUiStagesAndWaitsForLanding()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 0, options, "charter-ship-test"));
		java.util.List<String> actionsIssued = new java.util.ArrayList<>();
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				actionsIssued.add(interaction.getAction());
				return true;
			}
		};
		RouteInteraction npc = charterInteraction("Charter", true);
		RouteInteraction destination = charterInteraction("charter-destination:Catherby", true);
		RouteInteraction confirm = charterInteraction("charter-confirm", true);

		NavigationExecutionResult npcResult = NavigationEngineRuntime.execute(
			observation(1, A, charterShipPlan(1), false, false)
				.withRouteInteraction(npc), actions);
		NavigationExecutionResult destinationResult = NavigationEngineRuntime.execute(
			observation(2, A, charterShipPlan(1), false, false)
				.withRouteInteraction(destination), actions);
		NavigationExecutionResult confirmResult = NavigationEngineRuntime.execute(
			observation(3, A, charterShipPlan(1), false, false)
				.withRouteInteraction(confirm), actions);
		NavigationExecutionResult voyage = NavigationEngineRuntime.execute(
			observation(4, new WorldPoint(500, 500, 1), charterShipPlan(1), false, false)
				.withRouteInteraction(charterInteraction("charter-confirm", false)), actions);
		NavigationExecutionResult landed = NavigationEngineRuntime.execute(
			observation(5, B, charterShipPlan(1), false, false)
				.withRouteInteraction(confirm.withStatus(
					RouteInteraction.Status.CLEARED, false)), actions);

		assertEquals(NavigationDecision.Type.INTERACT, npcResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, destinationResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, confirmResult.getDecision().getType());
		assertEquals(Arrays.asList("Charter", "charter-destination:Catherby",
			"charter-confirm"), actionsIssued);
		assertEquals(NavigationDecision.Type.WAIT, voyage.getDecision().getType());
		assertTrue(landed.isEngineOwned());
	}

	@Test
	public void fairyRingAdvancesThroughStagesAndWaitsForExactLanding()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 0, options, "fairy-ring-test"));
		java.util.List<String> actionsIssued = new java.util.ArrayList<>();
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				actionsIssued.add(interaction.getAction());
				return true;
			}
		};
		RouteInteraction equip = fairyRingInteraction("fairy-ring-equip:772", true);
		RouteInteraction configure = fairyRingInteraction("Configure", true);
		RouteInteraction rotate = fairyRingInteraction("fairy-ring-rotate:26083347:512", true);
		RouteInteraction teleport = fairyRingInteraction("fairy-ring-teleport", true);
		RouteInteraction restoreOpen = fairyRingInteraction(
			"fairy-ring-restore-open:6563", true);
		RouteInteraction restore = fairyRingInteraction("fairy-ring-restore:6563", true);

		NavigationExecutionResult equipResult = NavigationEngineRuntime.execute(
			observation(1, A, fairyRingPlan(1), false, false)
				.withRouteInteraction(equip), actions);
		NavigationExecutionResult configureResult = NavigationEngineRuntime.execute(
			observation(2, A, fairyRingPlan(1), false, false)
				.withRouteInteraction(configure), actions);
		NavigationExecutionResult rotateResult = NavigationEngineRuntime.execute(
			observation(3, A, fairyRingPlan(1), false, false)
				.withRouteInteraction(rotate), actions);
		NavigationExecutionResult teleportResult = NavigationEngineRuntime.execute(
			observation(4, A, fairyRingPlan(1), false, false)
				.withRouteInteraction(teleport), actions);
		NavigationExecutionResult voyage = NavigationEngineRuntime.execute(
			observation(5, new WorldPoint(500, 500, 1), fairyRingPlan(1), false, false)
				.withRouteInteraction(fairyRingInteraction("fairy-ring-teleport", false)), actions);
		NavigationExecutionResult landed = NavigationEngineRuntime.execute(
			observation(6, B, fairyRingPlan(1), false, false)
				.withRouteInteraction(restoreOpen), actions);
		NavigationExecutionResult restoreResult = NavigationEngineRuntime.execute(
			observation(7, B, fairyRingPlan(1), false, false)
				.withRouteInteraction(restore), actions);
		NavigationExecutionResult restored = NavigationEngineRuntime.execute(
			observation(8, B, fairyRingPlan(1), false, false)
				.withRouteInteraction(restore.withStatus(
					RouteInteraction.Status.CLEARED, false)), actions);

		assertEquals(NavigationDecision.Type.INTERACT, equipResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, configureResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, rotateResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, teleportResult.getDecision().getType());
		assertEquals(Arrays.asList("fairy-ring-equip:772", "Configure",
			"fairy-ring-rotate:26083347:512", "fairy-ring-teleport",
			"fairy-ring-restore-open:6563", "fairy-ring-restore:6563"), actionsIssued);
		assertEquals(NavigationDecision.Type.WAIT, voyage.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, landed.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT, restoreResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.WAIT, restored.getDecision().getType());
	}

	@Test
	public void spiritTreeAdvancesThroughDestinationAndWaitsForLanding()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(22,
			Collections.singleton(C), 0, options, "spirit-tree-test"));
		List<String> actionsIssued = new ArrayList<>();
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				actionsIssued.add(interaction.getAction());
				return true;
			}
		};
		RouteEdge treeEdge = new RouteEdge(0, A, B, RouteEdge.Kind.SPIRIT_TREE);
		RouteEdge onward = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		RoutePlan plan = new RoutePlan(22, 1, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(treeEdge, onward));
		RouteInteraction object = new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.SPIRIT_TREE, RouteInteraction.Status.AVAILABLE,
			"Travel", true, 1295, A, B);
		RouteInteraction destination = new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.SPIRIT_TREE, RouteInteraction.Status.AVAILABLE,
			"spirit-tree-destination:Gnome Stronghold", true, 1295, A, B);

		NavigationExecutionResult objectResult = NavigationEngineRuntime.execute(
			observation(1, A, plan, false, false).withRouteInteraction(object), actions);
		NavigationExecutionResult destinationResult = NavigationEngineRuntime.execute(
			observation(2, A, plan, false, false).withRouteInteraction(destination), actions);
		NavigationExecutionResult transit = NavigationEngineRuntime.execute(
			observation(3, new WorldPoint(500, 500, 1), plan, false, false)
				.withRouteInteraction(destination.withStatus(
					RouteInteraction.Status.AVAILABLE, false)), actions);
		NavigationExecutionResult landed = NavigationEngineRuntime.execute(
			observation(4, B, plan, false, false).withRouteInteraction(
				destination.withStatus(RouteInteraction.Status.CLEARED, false)), actions);

		assertEquals(NavigationDecision.Type.INTERACT, objectResult.getDecision().getType());
		assertEquals(NavigationDecision.Type.INTERACT,
			destinationResult.getDecision().getType());
		assertEquals(Arrays.asList("Travel",
			"spirit-tree-destination:Gnome Stronghold"), actionsIssued);
		assertEquals(NavigationDecision.Type.WAIT, transit.getDecision().getType());
		assertEquals(NavigationDecision.Type.WAIT, landed.getDecision().getType());
	}

	@Test
	public void npcTransportIntermediateSceneDoesNotTriggerOffRouteRecovery()
	{
		WorldPoint landing = new WorldPoint(2662, 2677, 1);
		WorldPoint afterLanding = new WorldPoint(2663, 2677, 1);
		RouteEdge voyage = new RouteEdge(0, A, landing, RouteEdge.Kind.NPC_TRANSPORT);
		RouteEdge onward = new RouteEdge(1, landing, afterLanding, RouteEdge.Kind.WALK);
		RoutePlan voyagePlan = new RoutePlan(21, 1, A,
			Collections.singleton(afterLanding), Arrays.asList(A, landing, afterLanding),
			Arrays.asList(A, landing, afterLanding), true, Arrays.asList(voyage, onward));
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(afterLanding), 0, options, "npc-intermediate-scene-test"));
		RouteInteraction transport = new RouteInteraction(1, 0, A, landing, A,
			RouteInteraction.Kind.NPC_TRANSPORT, RouteInteraction.Status.AVAILABLE,
			"Travel", true, 1770, A, landing);
		WalkerActions actions = new WalkerActions()
		{
			@Override
			public boolean clickTile(WorldPoint target)
			{
				return true;
			}

			@Override
			public boolean interact(RouteInteraction interaction)
			{
				return true;
			}
		};
		NavigationEngineRuntime.execute(observation(1, A, voyagePlan,
			false, false).withRouteInteraction(transport), actions);
		WorldPoint shipDeck = new WorldPoint(3064, 3208, 1);

		NavigationExecutionResult intermediate = NavigationEngineRuntime.execute(
			observation(2, shipDeck, voyagePlan, false, false)
				.withRouteInteraction(transport), actions);

		assertEquals(NavigationDecision.Type.WAIT, intermediate.getDecision().getType());
		assertEquals("interaction-command-in-flight", intermediate.getDecision().getReason());
		assertEquals(0, NavigationEngineRuntime.getSnapshot()
			.getRecoveryAttempts(RecoveryCause.OFF_ROUTE));
	}

	@Test
	public void executorChoiceCannotSwitchFromLegacyOnLaterGeneration()
	{
		startEngineRequest();
		NavigationEngineRuntime.execute(observation(1, A, transportPlan(1), false, false),
			target -> true);

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(2), false, false), target -> true);

		assertFalse(result.isEngineOwned());
		assertEquals(NavigationExecutionMode.LEGACY_LOCKED,
			NavigationEngineRuntime.getSnapshot().getExecutionMode());
	}

	@Test
	public void engineRouteCannotBecomeTransportRouteMidRequest()
	{
		startEngineRequest();
		NavigationEngineRuntime.execute(observation(1, A, ordinaryPlan(1), false, false),
			target -> true);

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(2, B, transportPlan(2), false, false), target -> true);

		assertEquals(NavigationDecision.Type.FAIL, result.getDecision().getType());
		assertEquals(NavigationPhase.FAILED, NavigationEngineRuntime.getSnapshot().getPhase());
	}

	@Test
	public void routeReplacementWhileClickIsInFlightUsesOnlyNewGeneration()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};
		NavigationEngineRuntime.execute(observation(1, A, ordinaryPlan(1), false, false), actions);

		NavigationExecutionResult replacement = NavigationEngineRuntime.execute(
			observation(2, A, ordinaryPlan(2), false, false), actions);

		assertEquals(2, NavigationEngineRuntime.getSnapshot().getGeneration());
		assertEquals(NavigationDecision.Type.CLICK_TILE, replacement.getDecision().getType());
		assertEquals(2, commands.get());
	}

	@Test
	public void cancellationDuringMovementPreventsFurtherCommands()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		WalkerActions actions = target -> {
			commands.incrementAndGet();
			return true;
		};
		NavigationEngineRuntime.execute(observation(1, A, ordinaryPlan(1), false, false), actions);

		NavigationEngineRuntime.cancel("test-cancel-during-movement");
		NavigationExecutionResult after = NavigationEngineRuntime.execute(
			observation(2, B, ordinaryPlan(1), true, false), actions);

		assertEquals(NavigationDecision.Type.NO_ACTION, after.getDecision().getType());
		assertEquals(NavigationPhase.CANCELLED, NavigationEngineRuntime.getSnapshot().getPhase());
		assertEquals(1, commands.get());
	}

	@Test
	public void displacementRequestsReplanWithoutInput()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(1, new WorldPoint(3300, 3300, 0), ordinaryPlan(1), false, false),
			target -> {
				commands.incrementAndGet();
				return true;
			});

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, result.getDecision().getType());
		assertEquals(0, commands.get());
	}

	@Test
	public void partialRouteReplansAndEmptyRouteFailsWithoutInput()
	{
		startEngineRequest();
		AtomicInteger commands = new AtomicInteger();
		RoutePlan partial = new RoutePlan(21, 1, A, Collections.singleton(C),
			Arrays.asList(A, B), Arrays.asList(A, B), false);
		NavigationEngineRuntime.execute(observation(1, B, partial, false, false),
			target -> {
				commands.incrementAndGet();
				return true;
			});
		NavigationExecutionResult partialResult = NavigationEngineRuntime.execute(
			observation(2, B, partial, false, false), target -> true);

		assertEquals(NavigationDecision.Type.REQUEST_REPLAN,
			partialResult.getDecision().getType());
		assertEquals(0, commands.get());

		NavigationEngineRuntime.resetForTesting();
		startEngineRequest();
		RoutePlan empty = new RoutePlan(21, 1, A, Collections.singleton(C),
			Collections.emptyList(), Collections.emptyList(), false);
		NavigationExecutionResult emptyResult = NavigationEngineRuntime.execute(
			observation(3, A, empty, false, false), target -> true);
		assertTrue(emptyResult.isEngineOwned());
		assertEquals(NavigationDecision.Type.FAIL, emptyResult.getDecision().getType());
		assertEquals(NavigationPhase.UNREACHABLE, NavigationEngineRuntime.getSnapshot().getPhase());
	}

	@Test
	public void straightDiagonalCorneredAndDoubledBackRoutesAreEligible()
	{
		WorldPoint d = new WorldPoint(3201, 3201, 0);
		WorldPoint e = new WorldPoint(3200, 3201, 0);
		assertTrue(new RoutePlan(1, 1, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, C), true).isOrdinaryWalkOnly());
		assertTrue(new RoutePlan(1, 1, A, Collections.singleton(d),
			Arrays.asList(A, d), Arrays.asList(A, d), true).isOrdinaryWalkOnly());
		assertTrue(new RoutePlan(1, 1, A, Collections.singleton(e),
			Arrays.asList(A, B, d, e), Arrays.asList(A, B, d, e), true).isOrdinaryWalkOnly());
		assertTrue(new RoutePlan(1, 1, A, Collections.singleton(B),
			Arrays.asList(A, B, d, e, A, B), Arrays.asList(A, d, A, B), true)
			.isOrdinaryWalkOnly());
	}

	@Test
	public void doubledBackRouteStartsFromEarliestMatchingAnchor()
	{
		WorldPoint d = new WorldPoint(3201, 3201, 0);
		WorldPoint e = new WorldPoint(3200, 3201, 0);
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(B), 0, options, "folded-route-test"));
		RoutePlan folded = new RoutePlan(21, 1, A, Collections.singleton(B),
			Arrays.asList(A, B, d, e, A, B), Arrays.asList(A, d, A, B), true);

		NavigationExecutionResult result = NavigationEngineRuntime.execute(
			observation(1, A, folded, false, false), target -> true);

		assertEquals(NavigationDecision.Type.CLICK_TILE, result.getDecision().getType());
		assertEquals(e, result.getDecision().getTarget());
		assertEquals(3, result.getDecision().getTargetRawIndex());
		assertEquals("raw-route-lookahead", result.getDecision().getTargetSelection());
		assertEquals(0, NavigationEngineRuntime.getSnapshot().getRawProgressIndex());
	}

	@Test
	public void combatDisplacementWaitsInFlightThenReplansWithoutRecoveryClick()
	{
		startEngineRequest();
		WorldPoint displaced = new WorldPoint(3300, 3300, 0);
		NavigationObservation inCombat = NavigationObservation.route(1, displaced, ordinaryPlan(1),
			false, true, true, false, false, false, null, "combat-displacement");

		NavigationExecutionResult waiting = NavigationEngineRuntime.execute(inCombat, target -> true);
		int attemptsWhileInFlight = NavigationEngineRuntime.getSnapshot().getRecoveryAttempts();
		NavigationExecutionResult settled = NavigationEngineRuntime.execute(
			observation(2, displaced, ordinaryPlan(1), false, false), target -> true);

		assertEquals(NavigationDecision.Type.WAIT, waiting.getDecision().getType());
		assertEquals(0, attemptsWhileInFlight);
		assertEquals(NavigationDecision.Type.REQUEST_REPLAN, settled.getDecision().getType());
	}

	private static void startEngineRequest()
	{
		NavigationRouteOptions options = new NavigationRouteOptions(true, true, false, true);
		NavigationEngineRuntime.ensureRequest(new NavigationRequest(21,
			Collections.singleton(C), 0, options, "phase-3-test"));
	}

	private static NavigationObservation observation(long time, WorldPoint player, RoutePlan plan,
		boolean moving, boolean replan)
	{
		return NavigationObservation.route(time, player, plan, moving, false, false,
			false, false, replan, null, "phase-3-test");
	}

	private static RoutePlan ordinaryPlan(long generation)
	{
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, C), true);
	}

	private static RoutePlan transportPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, B, RouteEdge.Kind.TRANSPORT);
		RouteEdge second = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(first, second));
	}

	private static RoutePlan adjacentTransportPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, B, RouteEdge.Kind.ADJACENT_TRANSPORT);
		RouteEdge second = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(first, second));
	}

	private static RoutePlan catalogTransitionPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, UP, RouteEdge.Kind.CATALOG_TRANSITION);
		RouteEdge second = new RouteEdge(1, UP, UP_TARGET, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(UP_TARGET),
			Arrays.asList(A, UP, UP_TARGET), Arrays.asList(A, UP, UP_TARGET), true,
			Arrays.asList(first, second));
	}

	private static RoutePlan simpleTeleportPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, B, RouteEdge.Kind.SIMPLE_TELEPORT);
		RouteEdge second = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(first, second));
	}

	private static RoutePlan npcTransportPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, B, RouteEdge.Kind.NPC_TRANSPORT);
		RouteEdge second = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(first, second));
	}

	private static RoutePlan charterShipPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, B, RouteEdge.Kind.CHARTER_SHIP);
		RouteEdge second = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(first, second));
	}

	private static RouteInteraction charterInteraction(String action, boolean ready)
	{
		return new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.CHARTER_SHIP, RouteInteraction.Status.AVAILABLE,
			action, ready, 9318, A, B);
	}

	private static RoutePlan fairyRingPlan(long generation)
	{
		RouteEdge first = new RouteEdge(0, A, B, RouteEdge.Kind.FAIRY_RING);
		RouteEdge second = new RouteEdge(1, B, C, RouteEdge.Kind.WALK);
		return new RoutePlan(21, generation, A, Collections.singleton(C),
			Arrays.asList(A, B, C), Arrays.asList(A, B, C), true,
			Arrays.asList(first, second));
	}

	private static RouteInteraction fairyRingInteraction(String action, boolean ready)
	{
		return new RouteInteraction(1, 0, A, B, A,
			RouteInteraction.Kind.FAIRY_RING, RouteInteraction.Status.AVAILABLE,
			action, ready, 6563, A, B);
	}
}
