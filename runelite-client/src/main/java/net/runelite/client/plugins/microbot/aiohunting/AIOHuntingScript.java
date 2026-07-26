package net.runelite.client.plugins.microbot.aiohunting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiohunting.enums.AIOHuntingState;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingStyle;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.statemachine.StateMachineScript;
import net.runelite.client.plugins.microbot.statemachine.Transition;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.huntkit.Rs2HuntKit;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

@Slf4j
public class AIOHuntingScript extends StateMachineScript<AIOHuntingState>
{
	private static final int AREA_ARRIVAL_DISTANCE = 8;
	private static final int BANK_INTERACT_DISTANCE = 4;
	private static final int INVENTORY_BUFFER = 2;
	private static final int PURO_PURO_REGION = 10307;
	private static final long TRACKING_INTERACTION_DELAY_MS = 1800L;
	// A catch resets the finish varbit slightly after the noose animation; wait on the
	// varbit itself so the catching end is recorded (not misfiled as a miss).
	private static final long TRACKING_CATCH_TIMEOUT_MS = 4000L;
	// A travel block is retried rather than left to idle forever (which lets the game's
	// 20-minute idle timer log the account out).
	private static final long TRAVEL_RETRY_MS = 60000L;
	// The catchable trail end sits right next to the last clue (measured 3-6 tiles;
	// same-area decoys reach ~18). A different trail's ends are 27+ tiles away, so a
	// generous radius around the last clue keeps the search local. Falls back to the
	// whole area if nothing qualifies, so it can never get stuck.
	private static final int TRACKING_END_SEARCH_RADIUS = 20;
	private static final long ROAMING_CLUSTER_EMPTY_MS = 10000L;
	private static final long CLUSTER_MINIMUM_DWELL_MS = 20000L;
	private static final long TRAP_CLUSTER_STALL_MS = 120000L;
	private static final int ANIMATION_START_TIMEOUT_MS = 1800;
	private static final int ANIMATION_FINISH_TIMEOUT_MS = 10000;
	private static final WorldPoint PURO_PURO_ENTRANCE = new WorldPoint(2427, 4446, 0);
	private static final WorldPoint PURO_PURO_EXIT = new WorldPoint(2592, 4322, 0);
	private static final WorldPoint POLAR_PLATFORM_ACCESS = new WorldPoint(2716, 3798, 0);
	private static final int[] NOOSE_WAND_IDS = {
		ItemID.NOOSE_WAND,
		ItemID.NOOSE_WAND_WOOD,
		ItemID.NOOSE_WAND_POLAR,
		ItemID.NOOSE_WAND_DESERT,
		ItemID.NOOSE_WAND_JUNGLE,
		ItemID.NOOSE_WAND_RAZORBACK
	};
	private static final String[] MEAT_ITEM_NAMES = { "Raw bird meat", "Raw beast meat" };

	private AIOHuntingConfig config;
	private AIOHuntingPlugin plugin;

	@Getter
	private volatile HuntingMethod activeMethod = HuntingMethod.CRIMSON_SWIFT;
	@Getter
	private volatile String status = "Starting";
	@Getter
	private volatile String recoveryReason;
	@Getter
	private volatile boolean paused;

	private volatile AIOHuntingState desiredState = AIOHuntingState.PREPARING;
	private HuntingMethod progressionMethod;
	private HuntingMethod pendingMethod;
	private long lastInteractionAt;
	private int consecutiveTravelFailures;
	private HuntingMethod blockedMethod;
	private WorldPoint lastTravelPosition;
	private long blockedAt;
	private BirdhouseActivity birdhouse;
	private HerbiboarActivity herbiboar;
	private ButterflyActivity butterfly;
	private FalconryActivity falconry;
	private ImplingActivity impling;
	private TrapActivity trap;
	private int trackingFinishValue;
	private final Set<WorldPoint> attemptedTrackingEnds = new HashSet<>();
	private WorldPoint lastTrackingClue;
	private TrackingEndMemory trackingEnds;
	private int directFinishValue;
	private int directFinishMisses;
	private WorldPoint activeHuntingLocation;
	private WorldPoint pendingClusterLocation;
	private int activeClusterIndex;
	private long clusterEmptySince;
	private long lastClusterChangeAt;

	public boolean run(AIOHuntingConfig config, AIOHuntingPlugin plugin)
	{
		this.config = config;
		this.plugin = plugin;
		this.paused = false;
		this.recoveryReason = null;
		this.progressionMethod = null;
		this.pendingMethod = null;
		this.desiredState = AIOHuntingState.PREPARING;
		this.lastInteractionAt = 0L;
		this.consecutiveTravelFailures = 0;
		this.blockedMethod = null;
		this.birdhouse = new BirdhouseActivity(config, this);
		this.herbiboar = new HerbiboarActivity(config, this);
		this.butterfly = new ButterflyActivity(config, this);
		this.falconry = new FalconryActivity(config, this);
		this.impling = new ImplingActivity(config, this);
		this.trap = new TrapActivity(config, this, plugin);
		this.trackingEnds = new TrackingEndMemory(plugin.getConfigManager());
		this.trackingFinishValue = 0;
		this.attemptedTrackingEnds.clear();
		this.lastTrackingClue = null;
		this.activeHuntingLocation = null;
		this.pendingClusterLocation = null;
		this.activeClusterIndex = 0;
		this.clusterEmptySince = 0L;
		this.lastClusterChangeAt = System.currentTimeMillis();

		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			if (paused)
			{
				return;
			}
			try
			{
				step();
			}
			catch (Exception ex)
			{
				recoveryReason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
				desiredState = AIOHuntingState.RECOVERING;
				log.warn("AIO Hunting loop recovered from {}", recoveryReason);
			}
		}, 0, 600, TimeUnit.MILLISECONDS);
		return true;
	}

	public void togglePause()
	{
		paused = !paused;
		if (paused)
		{
			Rs2Walker.clearWalkingRoute("aiohunting:paused");
			status = "Paused";
		}
	}

	@Override
	protected AIOHuntingState initialState()
	{
		return AIOHuntingState.PREPARING;
	}

	@Override
	protected List<Transition<AIOHuntingState>> defineTransitions()
	{
		List<Transition<AIOHuntingState>> transitions = new ArrayList<>();
		for (AIOHuntingState from : AIOHuntingState.values())
		{
			for (AIOHuntingState to : AIOHuntingState.values())
			{
				if (from == to)
				{
					continue;
				}
				transitions.add(Transition.<AIOHuntingState>from(from)
					.when(() -> desiredState == to, "desiredState == " + to)
					.because("Situation requires " + to)
					.goTo(to));
			}
		}
		return transitions;
	}

	@Override
	protected void onState(AIOHuntingState state)
	{
		if (!Microbot.isLoggedIn())
		{
			return;
		}

		resolveActiveMethod();
		desiredState = determineDesiredState();
		if (desiredState != state)
		{
			return;
		}

		switch (state)
		{
			case PREPARING:
				status = "Checking setup";
				break;
			case WALKING_TO_BANK:
				status = "Walking to bank";
				if (openBankAsSoonAsAvailable())
				{
					consecutiveTravelFailures = 0;
				}
				else if (!paused)
				{
					recordTravelFailure("Could not reach a usable bank");
				}
				break;
			case BANKING:
				status = "Banking and resupplying";
				handleBanking();
				break;
			case WALKING_TO_AREA:
				status = "Walking to " + activeMethod.getDisplayName();
				walkToArea();
				break;
			case HUNTING:
				status = "Hunting " + activeMethod.getDisplayName();
				handleHunting();
				break;
			case RECOVERING:
				status = recoveryReason == null ? "Waiting for a safe setup" : recoveryReason;
				break;
			default:
				break;
		}
	}

	private AIOHuntingState determineDesiredState()
	{
		int level = Rs2Player.getRealSkillLevel(Skill.HUNTER);
		if (level >= config.targetLevel())
		{
			recoveryReason = "Target Hunter level reached";
			return AIOHuntingState.RECOVERING;
		}
		if (activeMethod.getLevel() > level)
		{
			recoveryReason = "Requires Hunter level " + activeMethod.getLevel();
			return AIOHuntingState.RECOVERING;
		}
		if (activeMethod.isDangerous() && !config.allowWilderness())
		{
			recoveryReason = "Enable Allow Wilderness for " + activeMethod.getDisplayName();
			return AIOHuntingState.RECOVERING;
		}
		if (blockedMethod == activeMethod)
		{
			// Don't give up permanently — retry the route periodically. This also keeps the
			// script active so the game's idle timer never logs the account out.
			if (System.currentTimeMillis() - blockedAt >= TRAVEL_RETRY_MS)
			{
				blockedMethod = null;
				consecutiveTravelFailures = 0;
				lastTravelPosition = null;
			}
			else
			{
				return AIOHuntingState.RECOVERING;
			}
		}
		if (activeMethod.getStyle() == HuntingStyle.BIRD_HOUSE && birdhouse.runComplete())
		{
			if (birdhouse.needsFinalBank() && config.useBank())
			{
				return Rs2Bank.isOpen() || isBankInteractable()
					? AIOHuntingState.BANKING
					: AIOHuntingState.WALKING_TO_BANK;
			}
			if (birdhouse.isReadyForNewRun())
			{
				birdhouse.beginNewRun();
				recoveryReason = null;
			}
			else
			{
				recoveryReason = "Birdhouse run complete; waiting for a house to become ready";
				return AIOHuntingState.RECOVERING;
			}
		}

		recoveryReason = null;
		if (pendingMethod != null)
		{
			return isAtHuntingArea()
				? AIOHuntingState.HUNTING
				: AIOHuntingState.WALKING_TO_AREA;
		}
		if ((needsInventoryCleanup() && config.useBank()) || !hasRequiredSupplies())
		{
			return Rs2Bank.isOpen() || isBankInteractable()
				? AIOHuntingState.BANKING
				: AIOHuntingState.WALKING_TO_BANK;
		}
		if (!isAtHuntingArea())
		{
			return AIOHuntingState.WALKING_TO_AREA;
		}
		return AIOHuntingState.HUNTING;
	}

	private void resolveActiveMethod()
	{
		HuntingMethod candidate;
		if (!config.autoProgress())
		{
			candidate = config.manualMethod() == null
				? HuntingMethod.CRIMSON_SWIFT
				: config.manualMethod();
		}
		else
		{
			int level = Rs2Player.getRealSkillLevel(Skill.HUNTER);
			candidate = HuntingMethod.bestFor(level);
		}

		if (progressionMethod == null)
		{
			applyMethod(candidate);
			return;
		}
		if (candidate != progressionMethod)
		{
			pendingMethod = candidate;
		}
		else
		{
			pendingMethod = null;
		}
		if (pendingMethod != null && !trap.hasOutstandingTrap())
		{
			applyMethod(pendingMethod);
			pendingMethod = null;
		}
		activeMethod = progressionMethod;
	}

	private void applyMethod(HuntingMethod method)
	{
		progressionMethod = method;
		activeMethod = method;
		blockedMethod = null;
		consecutiveTravelFailures = 0;
		birdhouse.reset();
		trackingFinishValue = 0;
		attemptedTrackingEnds.clear();
		lastTrackingClue = null;
		List<WorldPoint> locations = HuntingLocationData.locations(method);
		activeClusterIndex = HuntingLocationData.closestIndex(
			method, Rs2Player.getWorldLocation());
		activeHuntingLocation = locations.get(activeClusterIndex);
		pendingClusterLocation = null;
		clusterEmptySince = 0L;
		lastClusterChangeAt = System.currentTimeMillis();
		trap.reset();
		plugin.clearOwnedTraps();
	}

	private void walkToArea()
	{
		if (activeMethod.getStyle() == HuntingStyle.IMPLING && !isInPuroPuro())
		{
			enterPuroPuro();
			return;
		}
		if (activeMethod == HuntingMethod.POLAR_KEBBIT)
		{
			walkToPolarPlatform();
			return;
		}
		if (walkUntilNotPaused(huntingLocation(), AREA_ARRIVAL_DISTANCE,
			this::isAtHuntingArea))
		{
			consecutiveTravelFailures = 0;
		}
		else if (!paused)
		{
			recordTravelFailure("Could not route to " + activeMethod.getDisplayName());
		}
	}

	private void recordTravelFailure(String reason)
	{
		WorldPoint current = Rs2Player.getWorldLocation();
		// A long route can time out mid-walk while still making progress; only treat it as
		// a real failure once the player is genuinely frozen (unchanged since last time).
		boolean movedSinceLast = current != null && !current.equals(lastTravelPosition);
		lastTravelPosition = current;
		if (movedSinceLast)
		{
			consecutiveTravelFailures = 0;
			return;
		}
		consecutiveTravelFailures++;
		if (consecutiveTravelFailures >= 3)
		{
			blockedMethod = activeMethod;
			blockedAt = System.currentTimeMillis();
			recoveryReason = reason + "; retrying shortly";
			desiredState = AIOHuntingState.RECOVERING;
		}
	}

	private void walkToPolarPlatform()
	{
		if (isAtHuntingArea())
		{
			consecutiveTravelFailures = 0;
			return;
		}
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player == null)
		{
			return;
		}
		if (player.getPlane() != 0
			|| !walkUntilNotPaused(POLAR_PLATFORM_ACCESS, 4,
				() -> findPolarSteps(ObjectID.HUNTING_POLAR_STEPS_UPWARDS) != null))
		{
			if (!paused)
			{
				recordTravelFailure("Could not route to the Polar kebbit platform access");
			}
			return;
		}
		Rs2TileObjectModel steps = findPolarSteps(ObjectID.HUNTING_POLAR_STEPS_UPWARDS);
		status = "Ascending to Polar kebbits";
		if (steps != null && steps.click("Ascend")
			&& sleepUntil(() -> isAtHuntingArea() || paused, 10000)
			&& !paused)
		{
			consecutiveTravelFailures = 0;
			return;
		}
		if (!paused)
		{
			recordTravelFailure("Could not ascend to the Polar kebbit platform");
		}
	}

	private boolean leavePolarPlatform()
	{
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player == null || player.getPlane() != 1
			|| player.distanceTo(huntingLocation()) > activeAreaRadius())
		{
			return true;
		}
		Rs2TileObjectModel steps = findPolarSteps(ObjectID.HUNTING_POLAR_STEPS_DOWN);
		if (steps == null)
		{
			recoveryReason = "Polar kebbit platform steps are not available";
			return false;
		}
		status = "Descending from Polar kebbits";
		return steps.click("Descend")
			&& sleepUntil(() -> {
				WorldPoint location = Rs2Player.getWorldLocation();
				return paused || location != null && location.getPlane() == 0;
			}, 10000)
			&& !paused;
	}

	private Rs2TileObjectModel findPolarSteps(int objectId)
	{
		return Microbot.getRs2TileObjectCache().query()
			.withId(objectId)
			.nearestOnClientThread();
	}

	private void handleHunting()
	{
		if (Rs2Player.isAnimating() || Rs2Player.isMoving())
		{
			status = "Waiting for current action";
			return;
		}
		if (config.hopAroundPlayers() && trap.countActiveTraps() == 0
			&& Rs2Player.hopIfPlayerDetected(
				config.playersBeforeHop(), 5000, config.huntingRadius()))
		{
			status = "Hopping away from players";
			plugin.clearOwnedTraps();
			return;
		}

		if (config.dropMeat() && Rs2Inventory.hasItem(MEAT_ITEM_NAMES))
		{
			Rs2Inventory.dropAll(MEAT_ITEM_NAMES);
			return;
		}
		if (dropConfiguredItems())
		{
			return;
		}
		if (!config.useBank() && needsInventoryCleanup())
		{
			dropKnownLoot();
			return;
		}
		if (config.buryBones() && Rs2Inventory.hasItem("Bones"))
		{
			Rs2Inventory.interact("Bones", "Bury");
			return;
		}

		switch (activeMethod.getStyle())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
			case NET_TRAP:
			case DEADFALL:
				trap.step();
				break;
			case BUTTERFLY:
				butterfly.step();
				break;
			case FALCONRY:
				falconry.step();
				break;
			case BIRD_HOUSE:
				birdhouse.step();
				break;
			case HERBIBOAR:
				herbiboar.step();
				break;
			case IMPLING:
				impling.step();
				break;
			case TRACKING:
				handleTracking();
				break;
			default:
				break;
		}
	}

	private void handleTracking()
	{
		if (Rs2Player.isAnimating() || Rs2Player.isMoving())
		{
			return;
		}
		if (System.currentTimeMillis() - lastInteractionAt < TRACKING_INTERACTION_DELAY_MS)
		{
			return;
		}
		if (!Rs2Equipment.isWearing(NOOSE_WAND_IDS))
		{
			if (Rs2Inventory.hasItem(ItemID.NOOSE_WAND))
			{
				status = "Wielding noose wand";
				Rs2Inventory.interact(ItemID.NOOSE_WAND, "Wield");
			}
			return;
		}

		int finishValue = TrackingData.finishValue(activeMethod);
		if (finishValue != trackingFinishValue)
		{
			trackingFinishValue = finishValue;
			attemptedTrackingEnds.clear();
		}
		if (finishValue > 0)
		{
			handleTrackingEndpoint(finishValue);
			return;
		}

		int clueId = TrackingData.activeClueId(activeMethod);
		if (clueId != -1)
		{
			Rs2TileObjectModel clue = findTrackingObject(
				object -> object.getId() == clueId,
				"Inspect", "Search");
			if (clue != null)
			{
				WorldPoint clueLocation = clue.getWorldLocation();
				status = "Inspecting " + clue.getName();
				if (clickFirstAvailable(clue, "Inspect", "Search"))
				{
					lastTrackingClue = clueLocation;
					lastInteractionAt = System.currentTimeMillis();
				}
			}
			return;
		}

		Rs2TileObjectModel start = findTrackingObject(
			object -> TrackingData.startIds(activeMethod).contains(object.getId()),
			"Inspect", "Search");
		if (start != null)
		{
			WorldPoint startLocation = start.getWorldLocation();
			status = "Starting " + activeMethod.getDisplayName() + " trail";
			if (clickFirstAvailable(start, "Inspect", "Search"))
			{
				lastTrackingClue = startLocation;
				lastInteractionAt = System.currentTimeMillis();
			}
			return;
		}

		recoveryReason = "No active trail object is visible in the hunting area";
	}

	/**
	 * Endpoint of a tracking trail. The finish varbit is a 1-based index of which end
	 * (bush / snowdrift / rock) holds the kebbit — the same mechanism herbiboar uses. Once
	 * we know the tile for a given index we attack it directly, exactly like a player
	 * reading the footprints. The first time an index is seen we discover the tile by
	 * attacking candidates near the last clue: a wrong end is a harmless no-op, the correct
	 * one catches (and resets the finish varbit), which we then remember (persisted) so it
	 * is never probed again. Requires the noose wand to already be wielded (checked upstream).
	 */
	private void handleTrackingEndpoint(int finishValue)
	{
		WorldPoint known = trackingEnds.get(activeMethod, finishValue);
		if (known != null && isInsideArea(known))
		{
			attackKnownTrackingEnd(finishValue, known);
			return;
		}
		probeAndLearnTrackingEnd(finishValue);
	}

	/** Go straight to a known end tile and attack it; self-heal if the data is stale. */
	private void attackKnownTrackingEnd(int finishValue, WorldPoint known)
	{
		Rs2TileObjectModel end = findTrackingEndAt(known);
		if (end == null)
		{
			// The bush isn't in view yet (player too far); approach until it loads, then the
			// click below walks the final step itself — no walker call for a visible object.
			walkNear(known, 5);
			return;
		}
		status = "Catching " + activeMethod.getDisplayName();
		if (attackTrackingEndAndCaught(end, finishValue))
		{
			directFinishMisses = 0;
			return;
		}
		// The catch did not reset the trail, so the remembered tile may be stale; after a
		// couple of misses forget it so the next end stage rediscovers it by probing.
		if (finishValue == directFinishValue && ++directFinishMisses >= 2)
		{
			trackingEnds.forget(activeMethod, finishValue);
			directFinishMisses = 0;
		}
		directFinishValue = finishValue;
	}

	/** Discover which nearby end holds the kebbit by attacking candidates, and remember it. */
	private void probeAndLearnTrackingEnd(int finishValue)
	{
		// Prefer ends near the last clue (the catch is always right there); fall back to
		// the whole area if none qualify so a missing clue reference can't stall it.
		Rs2TileObjectModel end = findTrackingEnd(true);
		if (end == null)
		{
			end = findTrackingEnd(false);
		}
		if (end == null)
		{
			// Every candidate tried this pass; re-scan on the next tick.
			attemptedTrackingEnds.clear();
			return;
		}
		WorldPoint endLocation = end.getWorldLocation();
		status = "Catching " + activeMethod.getDisplayName();
		if (attackTrackingEndAndCaught(end, finishValue))
		{
			// The trail reset — this end held the kebbit. Remember it for next time.
			trackingEnds.record(activeMethod, finishValue, endLocation);
		}
		else if (!paused)
		{
			// Harmless miss (wrong end); skip it for the rest of this end phase.
			attemptedTrackingEnds.add(endLocation);
		}
	}

	/**
	 * Attack an end and report whether it caught, using the finish varbit resetting as the
	 * catch signal — the noose animation ends slightly before the varbit clears, so keying
	 * off the animation misfiles the catching end as a miss.
	 */
	private boolean attackTrackingEndAndCaught(Rs2TileObjectModel end, int finishValue)
	{
		if (!end.click("Attack"))
		{
			return false;
		}
		lastInteractionAt = System.currentTimeMillis();
		sleepUntil(() -> paused || TrackingData.finishValue(activeMethod) != finishValue,
			(int) TRACKING_CATCH_TIMEOUT_MS);
		return !paused && TrackingData.finishValue(activeMethod) != finishValue;
	}

	private Rs2TileObjectModel findTrackingEndAt(WorldPoint location)
	{
		return Microbot.getRs2TileObjectCache().query()
			.where(object -> TrackingData.endIds(activeMethod).contains(object.getId()))
			.within(location, 1)
			.nearestOnClientThread();
	}

	private Rs2TileObjectModel findTrackingEnd(boolean nearLastClue)
	{
		WorldPoint anchor = lastTrackingClue;
		return findTrackingObject(
			object -> TrackingData.endIds(activeMethod).contains(object.getId())
				&& !attemptedTrackingEnds.contains(object.getWorldLocation())
				&& (!nearLastClue || anchor == null
					|| object.getWorldLocation().distanceTo(anchor) <= TRACKING_END_SEARCH_RADIUS),
			"Attack");
	}

	private Rs2TileObjectModel findTrackingObject(
		java.util.function.Predicate<Rs2TileObjectModel> filter,
		String... actions)
	{
		return Microbot.getRs2TileObjectCache().query()
			.where(filter)
			.where(object -> isInsideArea(object.getWorldLocation()))
			.where(object -> Arrays.stream(actions)
				.anyMatch(action -> hasAction(object.getObjectComposition(), action)))
			.nearestOnClientThread();
	}

	private void enterPuroPuro()
	{
		if (!walkNear(PURO_PURO_ENTRANCE, 4))
		{
			recordTravelFailure("Could not route to the Puro-Puro crop circle");
			return;
		}
		Rs2TileObjectModel entrance = Microbot.getRs2TileObjectCache().query()
			.withId(ObjectID.II_MAGIC_WHEAT_M_ZANARIS)
			.within(PURO_PURO_ENTRANCE, 2)
			.nearestOnClientThread();
		if (entrance == null)
		{
			recordTravelFailure("Puro-Puro crop circle is not available");
			return;
		}
		status = "Entering Puro-Puro";
		if (entrance.click()
			&& sleepUntil(() -> isInPuroPuro() || paused, 10000)
			&& !paused)
		{
			consecutiveTravelFailures = 0;
		}
	}

	private boolean leavePuroPuro()
	{
		if (!isInPuroPuro())
		{
			return true;
		}
		if (!walkNear(PURO_PURO_EXIT, 4))
		{
			return false;
		}
		Rs2TileObjectModel exit = Microbot.getRs2TileObjectCache().query()
			.withId(ObjectID.II_MAGIC_WHEAT_M)
			.within(PURO_PURO_EXIT, 3)
			.nearestOnClientThread();
		if (exit == null)
		{
			recoveryReason = "Puro-Puro exit is not available";
			return false;
		}
		status = "Leaving Puro-Puro to bank";
		return exit.click()
			&& sleepUntil(() -> !isInPuroPuro() || paused, 10000)
			&& !paused;
	}

	boolean walkNear(WorldPoint target, int distance)
	{
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player != null && player.distanceTo(target) <= distance)
		{
			return true;
		}
		return walkUntilNotPaused(target, distance,
			() -> Rs2Player.distanceTo(target) <= distance);
	}

	Rs2TileObjectModel findObjectAtWithActions(
		WorldPoint point,
		int distance,
		String... actions)
	{
		return Microbot.getRs2TileObjectCache().query()
			.within(point, distance)
			.where(object -> Arrays.stream(actions)
				.anyMatch(action -> hasAction(object.getObjectComposition(), action)))
			.nearestOnClientThread();
	}

	private void handleBanking()
	{
		if (!Rs2Bank.isOpen() && !openBankAsSoonAsAvailable())
		{
			return;
		}
		if (activeMethod.getStyle() == HuntingStyle.BIRD_HOUSE
			&& birdhouse.runComplete() && birdhouse.needsFinalBank())
		{
			// Bank the run's loot but retain reusable supplies (hammer, chisel,
			// logs, seeds, kit) so the next ready-run does not re-withdraw them.
			Rs2Bank.depositAllExcept(retainedItemIds(), Collections.emptyMap());
			Rs2Bank.closeBank();
			birdhouse.markFinalBankDone();
			return;
		}

		Set<Integer> keep = retainedItemIds();
		Rs2Bank.depositAllExcept(keep, Collections.emptyMap());
		if (config.useHuntsmanKit() && Rs2Inventory.hasItem(Rs2HuntKit.KIT_ITEM_ID)
			&& !hasRequiredSupplies())
		{
			Rs2Bank.closeBank();
			Rs2HuntKit.emptyKit();
			if (!hasRequiredSupplies())
			{
				openBankAsSoonAsAvailable();
			}
		}

		if (!Rs2Bank.isOpen())
		{
			return;
		}
		if (!withdrawRequiredSupplies())
		{
			recoveryReason = "Required supplies are missing from the bank";
			desiredState = AIOHuntingState.RECOVERING;
			return;
		}
		Rs2Bank.closeBank();
	}

	private boolean withdrawRequiredSupplies()
	{
		switch (activeMethod.getStyle())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
			case NET_TRAP:
			case DEADFALL:
				return trap.withdraw();
			case BUTTERFLY:
				return butterfly.withdraw();
			case FALCONRY:
				return falconry.withdraw();
			case BIRD_HOUSE:
				return birdhouse.withdraw();
			case HERBIBOAR:
				return true;
			case IMPLING:
				return impling.withdraw();
			case TRACKING:
				return withdrawDeficit(ItemID.NOOSE_WAND, 1);
			default:
				return false;
		}
	}

	boolean withdrawDeficit(int itemId, int amount)
	{
		return Rs2Inventory.itemQuantity(itemId) >= amount
			|| Rs2Bank.withdrawDeficit(itemId, amount);
	}

	private boolean hasRequiredSupplies()
	{
		switch (activeMethod.getStyle())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
			case NET_TRAP:
			case DEADFALL:
				return trap.hasSupplies();
			case BUTTERFLY:
				return butterfly.hasSupplies();
			case FALCONRY:
				return falconry.hasSupplies();
			case BIRD_HOUSE:
				return birdhouse.hasSupplies();
			case HERBIBOAR:
				return true;
			case IMPLING:
				return impling.hasSupplies();
			case TRACKING:
				return Rs2Inventory.hasItem(NOOSE_WAND_IDS)
					|| Rs2Equipment.isWearing(NOOSE_WAND_IDS);
			default:
				return false;
		}
	}

	private boolean needsInventoryCleanup()
	{
		return Rs2Inventory.emptySlotCount() <= INVENTORY_BUFFER;
	}

	private void dropKnownLoot()
	{
		if (activeMethod.getStyle() == HuntingStyle.BOX_TRAP
			&& Rs2Inventory.hasItem("Ferret"))
		{
			Rs2Inventory.interact("Ferret", "Release");
			return;
		}
		if (!config.keepFurs())
		{
			Rs2Inventory.dropAll(item -> {
				String name = item.getName().toLowerCase();
				return name.contains("fur") || name.contains("kebbit spike");
			});
		}
		Rs2Inventory.dropAll(
			MEAT_ITEM_NAMES[0], MEAT_ITEM_NAMES[1], "Bones",
			"Green salamander", "Orange salamander",
			"Red salamander", "Black salamander", "Tecu salamander",
			"Ruby harvest", "Sapphire glacialis", "Snowy knight", "Black warlock",
			"Sunlight moth", "Moonlight moth", "Jerboa tail");
	}

	private Set<Integer> retainedItemIds()
	{
		Set<Integer> keep = new HashSet<>();
		keep.add(Rs2HuntKit.KIT_ITEM_ID);
		keep.add(ItemID.COINS);
		switch (activeMethod.getStyle())
		{
			case BIRD_SNARE:
			case BOX_TRAP:
			case NET_TRAP:
			case DEADFALL:
				trap.collectRetained(keep);
				break;
			case BUTTERFLY:
				butterfly.collectRetained(keep);
				break;
			case FALCONRY:
				falconry.collectRetained(keep);
				break;
			case BIRD_HOUSE:
				birdhouse.collectRetained(keep);
				break;
			case HERBIBOAR:
				herbiboar.collectRetained(keep);
				break;
			case IMPLING:
				impling.collectRetained(keep);
				break;
			case TRACKING:
				Arrays.stream(NOOSE_WAND_IDS).forEach(keep::add);
				break;
			default:
				break;
		}
		// User-defined keep list: protect these from banking and dropping (matched on held items).
		List<String> keepList = parseItemList(config.keepItems());
		if (!keepList.isEmpty())
		{
			Rs2Inventory.items()
				.filter(item -> item.getName() != null
					&& matchesAny(item.getName().toLowerCase(), keepList))
				.forEach(item -> keep.add(item.getId()));
		}
		return keep;
	}

	/**
	 * Drops inventory items whose name matches the user's comma-separated "Drop list", except any
	 * hunting supply or keep-list item. Lets the user prune specific low-value hunter loot without a
	 * blanket drop-all. @return true if anything was dropped this tick.
	 */
	private boolean dropConfiguredItems()
	{
		List<String> dropList = parseItemList(config.dropItems());
		if (dropList.isEmpty())
		{
			return false;
		}
		Set<Integer> protectedIds = retainedItemIds();
		List<String> keepList = parseItemList(config.keepItems());
		Predicate<Rs2ItemModel> filter = item -> {
			if (protectedIds.contains(item.getId()) || item.getName() == null)
			{
				return false;
			}
			String name = item.getName().toLowerCase();
			return !matchesAny(name, keepList) && matchesAny(name, dropList);
		};
		// Rs2Inventory.dropAll(predicate) always returns true, so gate on there actually being a
		// matching item - otherwise we would report a drop every tick and never run the rest of the loop.
		if (!Rs2Inventory.contains(filter))
		{
			return false;
		}
		Rs2Inventory.dropAll(filter);
		return true;
	}

	private static boolean matchesAny(String lowerName, List<String> terms)
	{
		for (String term : terms)
		{
			if (lowerName.contains(term))
			{
				return true;
			}
		}
		return false;
	}

	private static List<String> parseItemList(String csv)
	{
		if (csv == null || csv.trim().isEmpty())
		{
			return Collections.emptyList();
		}
		List<String> names = new ArrayList<>();
		for (String part : csv.split(","))
		{
			String trimmed = part.trim().toLowerCase();
			if (!trimmed.isEmpty())
			{
				names.add(trimmed);
			}
		}
		return names;
	}

	boolean clickFirstAvailable(Rs2TileObjectModel object, String... actions)
	{
		for (String action : actions)
		{
			if (object.click(action))
			{
				waitForHuntingAnimation();
				return true;
			}
		}
		return false;
	}

	void waitForHuntingAnimation()
	{
		boolean started = sleepUntil(
			() -> paused || Rs2Player.isAnimating(),
			ANIMATION_START_TIMEOUT_MS);
		if (!started || paused)
		{
			return;
		}
		sleepUntil(
			() -> paused || !Rs2Player.isAnimating(),
			ANIMATION_FINISH_TIMEOUT_MS);
	}

	boolean isInsideArea(WorldPoint point)
	{
		if (activeMethod.getStyle() == HuntingStyle.IMPLING)
		{
			return isInPuroPuro(point);
		}
		WorldPoint location = huntingLocation();
		return point != null && point.getPlane() == location.getPlane()
			&& point.distanceTo(location) <= activeAreaRadius();
	}

	private boolean isAtHuntingArea()
	{
		WorldPoint player = Rs2Player.getWorldLocation();
		return player != null && isInsideArea(player);
	}

	private WorldPoint huntingLocation()
	{
		return activeHuntingLocation == null
			? activeMethod.getLocation() : activeHuntingLocation;
	}

	void considerRoamingClusterMove()
	{
		List<WorldPoint> locations = HuntingLocationData.locations(activeMethod);
		if (locations.size() < 2)
		{
			return;
		}
		long now = System.currentTimeMillis();
		if (clusterEmptySince == 0L)
		{
			clusterEmptySince = now;
			return;
		}
		if (now - clusterEmptySince < ROAMING_CLUSTER_EMPTY_MS
			|| now - lastClusterChangeAt < CLUSTER_MINIMUM_DWELL_MS)
		{
			return;
		}
		activeClusterIndex = (activeClusterIndex + 1) % locations.size();
		activeHuntingLocation = locations.get(activeClusterIndex);
		clusterEmptySince = 0L;
		lastClusterChangeAt = now;
		status = "Moving to another " + activeMethod.getDisplayName() + " cluster";
	}

	boolean shouldRotateStalledTrapCluster()
	{
		if (HuntingLocationData.locations(activeMethod).size() < 2)
		{
			return false;
		}
		long latestProgress = Math.max(lastInteractionAt, lastClusterChangeAt);
		return System.currentTimeMillis() - latestProgress >= TRAP_CLUSTER_STALL_MS;
	}

	void queueNextCluster()
	{
		List<WorldPoint> locations = HuntingLocationData.locations(activeMethod);
		if (locations.size() < 2)
		{
			return;
		}
		int nextIndex = (activeClusterIndex + 1) % locations.size();
		pendingClusterLocation = locations.get(nextIndex);
		status = "Preparing to move to another "
			+ activeMethod.getDisplayName() + " cluster";
	}

	void activatePendingCluster()
	{
		List<WorldPoint> locations = HuntingLocationData.locations(activeMethod);
		int nextIndex = locations.indexOf(pendingClusterLocation);
		activeClusterIndex = nextIndex < 0 ? 0 : nextIndex;
		activeHuntingLocation = locations.get(activeClusterIndex);
		pendingClusterLocation = null;
		clusterEmptySince = 0L;
		lastInteractionAt = System.currentTimeMillis();
		lastClusterChangeAt = lastInteractionAt;
		status = "Moving to another " + activeMethod.getDisplayName() + " cluster";
	}

	private int activeAreaRadius()
	{
		switch (activeMethod.getStyle())
		{
			case BIRD_HOUSE:
				return birdhouse.areaRadius();
			case HERBIBOAR:
				return herbiboar.areaRadius();
			case BUTTERFLY:
				return butterfly.areaRadius();
			case FALCONRY:
				return falconry.areaRadius();
			case IMPLING:
				return impling.areaRadius();
			case TRACKING:
				return 55;
			default:
				return config.huntingRadius();
		}
	}

	private boolean openBankAsSoonAsAvailable()
	{
		if (isInPuroPuro() && !leavePuroPuro())
		{
			return false;
		}
		if (activeMethod == HuntingMethod.POLAR_KEBBIT && !leavePolarPlatform())
		{
			return false;
		}
		if (Rs2Bank.isOpen())
		{
			return true;
		}
		WorldPoint loadedBank = findLoadedBank();
		if (loadedBank == null)
		{
			if (Rs2Bank.getNearestBank() == null)
			{
				return false;
			}
			WorldPoint target = Rs2Bank.getNearestBank().getWorldPoint();
			if (!walkUntilNotPaused(target, BANK_INTERACT_DISTANCE,
				() -> findLoadedBank() != null))
			{
				return false;
			}
			loadedBank = findLoadedBank();
		}
		if (loadedBank != null && !isBankInteractable())
		{
			if (!walkUntilNotPaused(loadedBank, BANK_INTERACT_DISTANCE,
				this::isBankInteractable))
			{
				return false;
			}
		}
		return !paused && isBankInteractable() && Rs2Bank.openBank();
	}

	private boolean isInPuroPuro()
	{
		return isInPuroPuro(Rs2Player.getWorldLocation());
	}

	private boolean isInPuroPuro(WorldPoint point)
	{
		return point != null && point.getRegionID() == PURO_PURO_REGION
			&& point.getPlane() == 0;
	}

	boolean walkUntilNotPaused(
		WorldPoint target,
		int distance,
		BooleanSupplier completionCondition)
	{
		boolean arrived = Rs2Walker.walkUntil(target, distance,
			() -> paused || completionCondition.getAsBoolean());
		return arrived && !paused;
	}

	private boolean isBankInteractable()
	{
		WorldPoint player = Rs2Player.getWorldLocation();
		WorldPoint bank = findLoadedBank();
		return Rs2Bank.isOpen() || player != null && bank != null
			&& player.distanceTo(bank) <= BANK_INTERACT_DISTANCE;
	}

	private WorldPoint findLoadedBank()
	{
		Rs2TileObjectModel object = Microbot.getRs2TileObjectCache().query()
			.where(candidate -> hasAction(candidate.getObjectComposition(), "Bank"))
			.nearestOnClientThread();
		if (object != null)
		{
			return object.getWorldLocation();
		}
		Rs2NpcModel banker = Microbot.getRs2NpcCache().query()
			.where(candidate -> hasAction(candidate.getNpc().getTransformedComposition(), "Bank")
				|| hasAction(candidate.getNpc().getComposition(), "Bank"))
			.nearestOnClientThread();
		return banker == null ? null : banker.getWorldLocation();
	}

	static boolean hasAction(ObjectComposition composition, String action)
	{
		return composition != null && composition.getActions() != null
			&& Arrays.stream(composition.getActions())
			.anyMatch(candidate -> candidate != null && candidate.equalsIgnoreCase(action));
	}

	private static boolean hasAction(NPCComposition composition, String action)
	{
		return composition != null && composition.getActions() != null
			&& Arrays.stream(composition.getActions())
			.anyMatch(candidate -> candidate != null && candidate.equalsIgnoreCase(action));
	}

	public long getLastInteractionAt()
	{
		return lastInteractionAt;
	}

	// --- Package hooks used by extracted activity modules ---

	void setStatus(String status)
	{
		this.status = status;
	}

	void setRecoveryReason(String recoveryReason)
	{
		this.recoveryReason = recoveryReason;
	}

	boolean awaitCondition(BooleanSupplier condition, int timeoutMs)
	{
		return sleepUntil(condition, timeoutMs);
	}

	void markInteraction()
	{
		lastInteractionAt = System.currentTimeMillis();
	}

	void resetRoaming()
	{
		clusterEmptySince = 0L;
	}

	boolean hasPendingMethod()
	{
		return pendingMethod != null;
	}

	boolean hasPendingClusterMove()
	{
		return pendingClusterLocation != null;
	}

	@Override
	public void shutdown()
	{
		Rs2Walker.clearWalkingRoute("aiohunting:shutdown");
		super.shutdown();
		paused = false;
		status = "Stopped";
		desiredState = AIOHuntingState.PREPARING;
	}
}
