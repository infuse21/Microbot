package net.runelite.client.plugins.microbot.motherloadmine;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Perspective;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.player.Rs2PlayerCache;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.motherloadmine.enums.MLMMiningSpot;
import net.runelite.client.plugins.microbot.motherloadmine.enums.MLMStatus;
import net.runelite.client.plugins.microbot.motherloadmine.enums.Pickaxe;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.antiban.AntibanPlugin;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.depositbox.Rs2DepositBox;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Gembag;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MotherloadMineScript extends Script
{

    private static final WorldArea WEST_UPPER_AREA = new WorldArea(3748, 5676, 7, 9, 0);
    private static final WorldArea EAST_UPPER_AREA = new WorldArea(3756, 5667, 8, 8, 0);
    // Static areas for lower floor to avoid getting stuck behind rockfall
    private static final WorldArea WEST_LOWER_AREA = new WorldArea(3729, 5653, 10, 22, 0);
    private static final WorldArea SOUTH_LOWER_AREA = new WorldArea(3740, 5640, 20, 20, 0);

	private static final WorldPoint HOPPER_DEPOSIT_DOWN = new WorldPoint(3748, 5672, 0);
	private static final WorldPoint HOPPER_DEPOSIT_UP = new WorldPoint(3755, 5677, 0);

	private static final WorldArea CRATE_AREA = new WorldArea(new WorldPoint(3750, 5659, 0), 10, 16);

	private static final WorldPoint[] CRATE_WALKPOINTS = new WorldPoint[]
	{
		new WorldPoint(3755, 5671, 0),
		new WorldPoint(3756, 5662, 0),
		new WorldPoint(3751, 5662, 0),
	};

    private static final int UPPER_FLOOR_HEIGHT = -490;
    private static final int SACK_LARGE_SIZE = 189;
    private static final int SACK_SIZE = 108;
    /** Ids of an ore vein that still has pay-dirt in it. */
    private static final Set<Integer> ORE_VEIN_IDS = Set.of(26661, 26662, 26663, 26664);
    /** A click that landed has the player moving or swinging within a couple of ticks. */
    private static final int MINE_CLICK_ACK_MS = 2_000;
    private static final int MINE_START_TIMEOUT_MS = 10_000;
    public static MLMStatus status = MLMStatus.IDLE;
    public static Rs2TileObjectModel oreVein;
    public static MLMMiningSpot miningSpot = MLMMiningSpot.IDLE;
    private int maxSackSize;
	private List<String> itemsToKeep;

	private final MotherloadMinePlugin plugin;
    private final MotherloadMineConfig config;
    private final Rs2TileObjectCache rs2TileObjectCache;
    private final Rs2PlayerCache rs2PlayerCache;


	private boolean shouldEmptySack = false;
	private boolean shouldRepairWaterwheel = false;
	private boolean emptySackWorkflowActive = false;
	private long idleSince = 0;
	private int idleThreshold = 0;
	private boolean pickedUpHammer = false;
    private MLMStatus lastLoggedStatus = null;

	@Inject
	public MotherloadMineScript(MotherloadMinePlugin plugin, MotherloadMineConfig config, Rs2TileObjectCache rs2TileObjectCache, Rs2PlayerCache rs2PlayerCache)
	{
		this.plugin = plugin;
		this.config = config;
        this.rs2TileObjectCache = rs2TileObjectCache;
        this.rs2PlayerCache = rs2PlayerCache;
    }

    public boolean run()
    {
        log.info("Starting MotherloadMine script");
        initialize();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this::executeTaskSafely, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }

    private void initialize()
    {
        log.debug("Initializing MLM runtime state");
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();
        miningSpot = MLMMiningSpot.IDLE;
        status = MLMStatus.IDLE;
        lastLoggedStatus = null;
        shouldEmptySack = false;
		shouldRepairWaterwheel = false;
		emptySackWorkflowActive = false;
    }

    private void executeTaskSafely()
    {
        try
        {
            executeTask();
        }
        catch (Exception ex)
        {
            if (isShutdownInterrupt(ex))
            {
                // shutdown() cancels this task with an interrupt, so whichever call the loop
                // was parked in throws on its way out. That is a normal stop, and the state
                // reset below is pointless once the script is already going away.
                Thread.currentThread().interrupt();
                log.debug("MLM main loop interrupted; stopping");
                return;
            }
            log.error("Unhandled error in MLM main loop; resetting runtime state", ex);
            abortCurrentWorkflow();
        }
    }

    /**
     * Whether this throwable is only the loop being cancelled. {@code ClientThread.invoke}
     * wraps the InterruptedException in a RuntimeException, so the cause chain has to be
     * walked; helpers that swallow it instead leave just the thread's interrupt flag behind.
     */
    private static boolean isShutdownInterrupt(Throwable ex)
    {
        for (Throwable cause = ex; cause != null; cause = cause.getCause())
        {
            if (cause instanceof InterruptedException)
            {
                return true;
            }
        }
        return Thread.currentThread().isInterrupted();
    }

    private void executeTask()
    {
        if (!super.run() || !isWorkflowRunnable())
        {
            abortCurrentWorkflow();
            return;
        }

        determineStatusFromInventory();
        logStatusTransitionIfChanged();

        switch (status)
        {
            case IDLE:
                break;
            case MINING:
                Rs2Antiban.setActivityIntensity(Rs2Antiban.getActivity().getActivityIntensity());
                handleMining();
                break;
            case EMPTY_SACK:
                if (Rs2Player.isAnimating()) return;
                Rs2Antiban.setActivityIntensity(ActivityIntensity.EXTREME);
                emptySack();
                break;
            case FIXING_WATERWHEEL:
                if (Rs2Player.isAnimating()) return;
                fixWaterwheel();
                break;
            case DEPOSIT_HOPPER:
                if (Rs2Player.isAnimating()) return;
                depositHopper();
                break;
            case DROP_GEMS:
                if (Rs2Player.isAnimating()) return;
                dropGems();
                break;
        }
    }
    private String[] SPEC_PICKAXES = {"dragon pickaxe", "crystal pickaxe", "infernal pickaxe"};

    private void handlePickaxeSpec() {
        if (Rs2Equipment.isWearing(SPEC_PICKAXES)) {
            Rs2Combat.setSpecState(true, 1000);
        }
    }

    private void determineStatusFromInventory()
    {
        updateSackSize();
        if (!hasRequiredTools())
        {
            log.info("Missing required tools, running inventory setup");
            setupInventory();
            return;
        }

        if (shouldRepairWaterwheel && getBrokenStrutCount() > 1) {
            status = MLMStatus.FIXING_WATERWHEEL;
            return;
        }

        if (config.dropGems() && hasGemsInInventory()) {
            status = MLMStatus.DROP_GEMS;
            return;
        }

        int payDirtCount = payDirtCount();
        if (payDirtCount > 0 && Rs2Inventory.isFull()) {
            resetMiningState();
            status = MLMStatus.DEPOSIT_HOPPER;
            return;
        }

        if (currentSackCount() >= maxSackSize || hasOreInInventory() || (shouldEmptySack && !Rs2Inventory.contains(ItemID.PAYDIRT))) {
            resetMiningState();
            status = MLMStatus.EMPTY_SACK;
            return;
        }
        status = MLMStatus.MINING;
    }

    private boolean hasRequiredTools()
    {
		return Pickaxe.hasItem();
    }

    private void updateSackSize()
    {
        boolean sackUpgraded = Microbot.getVarbitValue(VarbitID.MOTHERLODE_BIGGERSACK) == 1;
        maxSackSize = sackUpgraded ? SACK_LARGE_SIZE : SACK_SIZE;
    }

	private void handleMining()
	{
		if (Rs2Player.getAnimation() != net.runelite.api.AnimationID.IDLE || Rs2Player.isMoving()) {
			idleSince = 0;
			return;
		}
		if (idleSince == 0) {
			idleSince = System.currentTimeMillis();
			idleThreshold = Math.max(2000, Rs2Random.randomGaussian(3000, 600));
			return;
		}
		if (System.currentTimeMillis() - idleSince < idleThreshold) return;
		idleSince = 0;

		if (Rs2Gembag.isUnknown()) {
			Rs2Gembag.checkGemBag();
		}

		shouldRepairWaterwheel = false;

		if (miningSpot == MLMMiningSpot.IDLE)
		{
			selectMiningSpotFromConfig();
		}

		// Click the nearest valid vein whatever the distance: the click walks us there, and
		// working outwards from where we stand is what produces the steady sweep along a wall.
		// Falling back to the curated spot point instead sends us back across the cavern, and
		// that point is reshuffled on every reselect. One sweep per pass - it used to be run
		// again inside the mine attempt and a third time after walking, for the same answer.
		if (isOnSelectedMiningFloor())
		{
			Rs2TileObjectModel vein = findClosestVein();
			if (vein != null && mineVein(vein))
			{
				return;
			}
		}

		if (!walkToMiningSpot()) return;

		mineVein(findClosestVein());
	}

	private boolean isOnSelectedMiningFloor()
	{
		if (miningSpot.isUpstairs()) return isUpperFloor();
		if (miningSpot.isDownstairs()) return !isUpperFloor();
		return true;
	}


    private void emptySack()
	{
		if (!emptySackWorkflowActive)
		{
			emptySackWorkflowActive = true;
			log.info("Emptying sack workflow started");
		}

		if (!isWorkflowRunnable())
		{
			abortCurrentWorkflow();
			return;
		}

		ensureLowerFloor();
		if (!isWorkflowRunnable())
		{
			abortCurrentWorkflow();
			return;
		}

		if (Microbot.getVarbitValue(VarbitID.MOTHERLODE_SACK_TRANSMIT) <= 0 && !hasOreInInventory())
		{
			completeEmptySackWorkflow();
			return;
		}

		if (hasOreInInventory())
		{
			useDepositBox();
			return;
		}

        if (canDropPayDirt())
        {
            depositHopper();
            return;
        }

        rs2TileObjectCache.query().interact(ObjectID.MOTHERLODE_SACK);
		sleepUntil(() -> !isWorkflowRunnable() || hasOreInInventory(), 10_000);
	}

	private void completeEmptySackWorkflow()
	{
		shouldEmptySack = false;
		shouldRepairWaterwheel = false;
		emptySackWorkflowActive = false;
		Rs2Antiban.takeMicroBreakByChance();
		status = MLMStatus.IDLE;
        log.info("Emptying sack workflow complete");
	}

	private boolean isWorkflowRunnable()
	{
		if (!Microbot.isLoggedIn() || Microbot.pauseAllScripts.get() || Thread.currentThread().isInterrupted())
		{
			return false;
		}

		try
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() -> {
				var player = Microbot.getClient().getLocalPlayer();
				return player != null && player.getWorldView() != null;
			}).orElse(false);
		}
		catch (RuntimeException ex)
		{
			log.debug("Player state unavailable during MLM lifecycle transition", ex);
			return false;
		}
	}

	private void abortCurrentWorkflow()
	{
		resetMiningState(true);
		status = MLMStatus.IDLE;
		idleSince = 0;
		shouldEmptySack = false;
		shouldRepairWaterwheel = false;
		emptySackWorkflowActive = false;
		pickedUpHammer = false;
	}

    private boolean hasOreInInventory()
    {
        return Rs2Inventory.contains(
                ItemID.RUNITE_ORE, ItemID.ADAMANTITE_ORE, ItemID.MITHRIL_ORE,
                ItemID.GOLD_ORE, ItemID.COAL
        );
    }

    private boolean hasGemsInInventory() {
        return Rs2Inventory.contains(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND);
    }
    
    private void dropGems() {
        if (hasGemsInInventory()) {
            Rs2Inventory.dropAll(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND);
        }
    }

    private int payDirtCount() {
        return Rs2Inventory.count(ItemID.PAYDIRT);
    }

    private boolean canDropPayDirt() {
        return payDirtCount() > 0 && currentSackCount() < maxSackSize;
    }

    private int currentSackCount() {
        return Microbot.getVarbitValue(VarbitID.MOTHERLODE_SACK_TRANSMIT);
    }

    private void fixWaterwheel() {
        log.info("Fixing waterwheel workflow started");
        ensureLowerFloor();

		if (!hasHammer()) {
			if (!obtainHammer()) return;
		}

		if (rs2TileObjectCache.query().interact(ObjectID.MOTHERLODE_WHEEL_STRUT_BROKEN))
		{
			// We use a modified version of waitForXpDrop to ensure we break out of the sleep if the strut is repaired
			final int skillExp = Microbot.getClientThread().invoke(() -> Microbot.getClient().getSkillExperience(Skill.SMITHING));
			sleepUntilTrue(() -> skillExp != Microbot.getClientThread().invoke(() -> Microbot.getClient().getSkillExperience(Skill.SMITHING)) || getBrokenStrutCount() <= 1, 250, 20_000);

			dropHammerIfNeeded();
			shouldRepairWaterwheel = false;
            log.info("Waterwheel repair complete");
		}
    }

    private void depositHopper()
    {
        // if using a gem bag, fill the gem bag and return to mining if the inventory is no longer full
        if (Rs2Inventory.isFull() && (Rs2Gembag.hasGemBag() && !Rs2Gembag.isGemBagOpen()))
        {
			Rs2Inventory.interact("gem bag", "open");
			sleepUntil(Rs2Gembag::isGemBagOpen);
            Rs2Inventory.interact("gem bag", "fill");
            if (!Rs2Inventory.isFull())
            {
                return;
            }
        }

        WorldPoint hopperDeposit = (isUpperFloor() && config.upstairsHopperUnlocked()) ? HOPPER_DEPOSIT_UP : HOPPER_DEPOSIT_DOWN;
        Rs2TileObjectModel hopper = rs2TileObjectCache.query().where(x -> x.getWorldLocation().equals(hopperDeposit)).withId(ObjectID.MOTHERLODE_HOPPER).first();

        if(isUpperFloor() && !config.upstairsHopperUnlocked())
        {
            ensureLowerFloor();
        }

        final int paydirtToDeposit = payDirtCount();

        if (hopper != null && hopper.click()) {
            log.debug("Depositing pay-dirt into hopper");
            sleepUntil(() -> payDirtCount() != paydirtToDeposit && !Rs2Player.isAnimating(), 10_000);

			shouldRepairWaterwheel = true;

            // Calculate the effective sack size after deposit as VarbitID.MOTHERLODE_SACK_TRANSMIT takes time to update
            final int currentSackAmount = currentSackCount();
            final int effectiveSackAmount = Math.max(currentSackAmount, Math.min(maxSackSize, currentSackAmount + paydirtToDeposit));

			shouldEmptySack = effectiveSackAmount >= (maxSackSize - 28);
            log.debug("Hopper deposit complete: paydirtDeposited={}, effectiveSackAmount={}, shouldEmptySack={}",
                    paydirtToDeposit, effectiveSackAmount, shouldEmptySack);
        }
        else
        {
            log.debug("Hopper unavailable, walking closer to deposit point");
            Rs2Walker.walkTo(hopperDeposit, 15);
        }
    }

    private void useDepositBox()
    {
        if (Rs2DepositBox.openDepositBox())
        {
            sleepUntil(Rs2DepositBox::isOpen);

            // if using the gem sack, empty its contents directly into the bank
            if (Rs2Gembag.hasGemBag() && Rs2Gembag.getGemBagContents().stream().anyMatch(s -> s.getQuantity() > 30))
            {
				Rs2Bank.emptyGemBag();
				sleep(100, 300);
            }

			if (config.useDepositAll()) {
				Rs2DepositBox.depositAll();
			} else {
				Rs2DepositBox.depositAllExcept(getItemsToKeep(), false);
				Rs2Inventory.waitForInventoryChanges(5000);
			}

			Rectangle gameObjectBounds = getMotherloadSackBounds();
			Rectangle depositBoxBounds = Rs2DepositBox.getDepositBoxBounds();
			if (depositBoxBounds != null && (!Rs2UiHelper.isRectangleWithinViewport(gameObjectBounds) || depositBoxBounds.intersects(gameObjectBounds))) {
				Rs2DepositBox.closeDepositBox();
			}
        }
    }

	private void setupInventory() {
        log.info("Running MLM inventory setup (useInventorySetup={})", config.useInventorySetup());
		if (!config.useInventorySetup()) {
			Rs2ItemModel pickaxe = Pickaxe.getBestPickaxe();

			if (pickaxe == null) {
				Rs2Bank.openBank();
				sleepUntil(Rs2Bank::isOpen);

				pickaxe = Pickaxe.getBestPickaxeFromBank();
				if (pickaxe == null) {
					Microbot.showMessage("No pickaxe found in bank or inventory. Please bank a pickaxe.");
                    log.warn("No pickaxe found in bank or inventory, stopping plugin");
					Microbot.stopPlugin(plugin);
					return;
				}

				if (Rs2Inventory.isFull()) {
					Rs2Bank.depositAll();
				}

				// Only equip if it has attack requirements, otherwise keep in inventory
				if (Pickaxe.hasAttackLevelRequirement(pickaxe.getId())) {
					final Rs2ItemModel currentWeapon = Rs2Equipment.get(EquipmentInventorySlot.WEAPON);
					final Rs2ItemModel _pickaxe = pickaxe;
					Rs2Bank.withdrawAndEquip(_pickaxe.getId());
					sleepUntil(() -> Rs2Equipment.isWearing(_pickaxe.getId()));
					if (currentWeapon != null) {
						Rs2Bank.depositOne(currentWeapon.getId());
						Rs2Inventory.waitForInventoryChanges(5000);
					}
				} else {
					Rs2Bank.withdrawOne(pickaxe.getId());
					Rs2Inventory.waitForInventoryChanges(5000);
				}

				// Get gem bag and hammer
				final int[] gemBagIDs = {ItemID.GEM_BAG, ItemID.GEM_BAG_OPEN};
				for (int gemBagID : gemBagIDs) {
					if (!isRunning()) break;
					if (Rs2Bank.withdrawOne(gemBagID)) {
						Rs2Inventory.waitForInventoryChanges(5000);
						break;
					}
				}

				if (Rs2Random.dicePercentage(10) && !hasHammer()) {
					if (Rs2Bank.withdrawOne("hammer")) {
						Rs2Inventory.waitForInventoryChanges(5000);
					}
				}

				Rs2Bank.toggleItemLock("pickaxe", false);
				Rs2Bank.toggleItemLock("hammer", false);
				Rs2Bank.toggleItemLock("gem bag", false);
			}

		} else {
			Rs2InventorySetup mlmInventorySetup = new Rs2InventorySetup(config.getInventorySetup(), mainScheduledFuture);
			boolean doesEquipmentMatch = true;
			boolean doesInventoryMatch = true;

			if (!mlmInventorySetup.doesEquipmentMatch()) {
				doesEquipmentMatch = mlmInventorySetup.loadEquipment();
			}

			if (!mlmInventorySetup.doesInventoryMatch()) {
				doesInventoryMatch = mlmInventorySetup.loadInventory();
			}

			if (!doesEquipmentMatch || !doesInventoryMatch) {
				Microbot.showMessage("Failed to load inventory setup. Please check your settings.");
                log.warn("Inventory setup failed (equipmentMatch={}, inventoryMatch={}), stopping plugin",
                        doesEquipmentMatch, doesInventoryMatch);
				Microbot.stopPlugin(plugin);
				return;
			}
		}

		Rs2Bank.closeBank();
		sleepUntil(() -> !Rs2Bank.isOpen());
        log.info("Inventory setup complete");
	}

    private void selectMiningSpotFromConfig() {
        MLMMiningSpot selected = MLMMiningSpot.valueOf(config.miningArea().name());

        if (selected == MLMMiningSpot.ANY) {
            if (config.mineUpstairs()) {
                miningSpot = Rs2Random.between(0, 1) == 0 ? MLMMiningSpot.WEST_UPPER : MLMMiningSpot.EAST_UPPER;
            }
            else {
				MLMMiningSpot[] filteredSpots = Arrays.stream(MLMMiningSpot.values())
					.filter(s -> s.getWorldPoint() != null && s.isDownstairs())
					.toArray(MLMMiningSpot[]::new);

				int size = filteredSpots.length;
				if (size == 0) return;

				int randomIndex = Rs2Random.randomGaussian(size / 2.0, size / 6.0);
				randomIndex = Math.max(0, Math.min(size - 1, randomIndex));

				miningSpot = filteredSpots[randomIndex];
            }
        } else {
            switch (selected) {
                case EAST_UPPER:
                case WEST_UPPER:
                case WEST_LOWER:
                case WEST_MID:
                case SOUTH_WEST:
                case SOUTH_EAST:
                    miningSpot = selected;
                    break;
                default:
                    Microbot.showMessage("Invalid mining area selected.");
                    log.warn("Invalid mining area selected: {}", selected);
                    Microbot.stopPlugin(plugin);
                    return;
            }
        }

        // Shuffle order of veins within the selected area
        if (miningSpot.getWorldPoint() != null) {
            Collections.shuffle(miningSpot.getWorldPoint());
        }
        log.info("Selected mining spot: {}", miningSpot);
    }

    private boolean walkToMiningSpot()
    {
        WorldPoint target = miningSpot.getWorldPoint().get(0);

        // Navigates to correct floor based on selected mining area
        if (miningSpot.isUpstairs() && !isUpperFloor())
        {
            goUp();
            return false; // Wait until we've gone up
        }

        if (miningSpot.isDownstairs() && isUpperFloor()) {
            goDown();
            return false; // Wait until we've gone down
        }

        // Walk to actual mining target tile
        return Rs2Walker.walkTo(target, 10);
    }

	private boolean mineVein(Rs2TileObjectModel vein) {
		if (vein == null) {
			repositionCameraAndMove();
			return false;
		}

		handlePickaxeSpec();

		if (!vein.click()) return false;
		oreVein = vein;

		WorldPoint veinLocation = vein.getWorldLocation();

		// handleMining only runs while the player is still, so a click that landed shows up as
		// movement or a swing almost immediately. Waiting out the full mine-start timeout on a
		// click that never landed is what made a cold start crawl.
		if (!sleepUntil(() -> Rs2Player.isMoving() || AntibanPlugin.isMining(), MINE_CLICK_ACK_MS)) {
			return false;
		}

		// Deliberately cheap: this is polled every tick or so, and the old version ran a
		// reachability search plus a client-thread hop on every single poll.
		return sleepUntil(() -> {
			Rs2TileObjectModel current = rs2TileObjectCache.query()
					.where(o -> Objects.equals(o.getWorldLocation(), veinLocation))
					.first();
			if (current == null || !ORE_VEIN_IDS.contains(current.getId())) return false;
			WorldPoint playerLoc = Rs2Player.getWorldLocation();
			return AntibanPlugin.isMining() && playerLoc != null
					&& veinLocation.distanceTo(playerLoc) <= 2;
		}, MINE_START_TIMEOUT_MS);
	}

    /**
     * Nearest vein worth mining.
     *
     * <p>Order matters here. The id and area tests are pure data, but
     * {@link #hasWalkableTilesAround} and {@code isReachable} each cost a client-thread round
     * trip per candidate, and MLM's walls are made of veins - running the scene through them
     * meant well over a hundred round trips behind a client thread that is simultaneously
     * doing live-collision capture, which froze one loop pass for 28 seconds. So: filter
     * cheaply, sort by distance, and pay for the expensive checks only until one passes -
     * normally the first candidate.</p>
     */
    private Rs2TileObjectModel findClosestVein()
    {
        WorldPoint player = Rs2Player.getWorldLocation();
        if (player == null)
        {
            return null;
        }

        List<WorldPoint> others = antiCrashEnabled() ? nearbyPlayerLocations() : Collections.emptyList();
        List<Rs2TileObjectModel> candidates = rs2TileObjectCache.query()
                .where(vein -> isVeinInSelectedArea(vein, others))
                .toList();
        candidates.sort(Comparator.comparingInt(vein -> vein.getWorldLocation().distanceTo(player)));

        for (Rs2TileObjectModel candidate : candidates)
        {
            if (hasWalkableTilesAround(candidate) && candidate.isReachable())
            {
                return candidate;
            }
        }
        return null;
    }

    private boolean antiCrashEnabled()
    {
        return !config.mineUpstairs() && config.useAntiCrash();
    }

    /**
     * Every player position in one client-thread hop.
     *
     * <p>{@code Rs2PlayerModel.getWorldLocation()} is itself a round trip to the client thread,
     * so asking it per player per vein cost players x veins round trips - jstack caught the
     * loop parked in exactly that lambda. Nested invokes run inline once we are on the client
     * thread, so one hop for the whole list makes the per-vein test pure arithmetic.</p>
     */
    private List<WorldPoint> nearbyPlayerLocations()
    {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            List<WorldPoint> locations = new ArrayList<>();
            for (var other : rs2PlayerCache.query().toList())
            {
                if (other == null) continue;
                WorldPoint location = other.getWorldLocation();
                if (location != null) locations.add(location);
            }
            return locations;
        }).orElse(Collections.emptyList());
    }

    /** Cheap half of vein selection: right rock, right area, nobody standing on it. */
    private boolean isVeinInSelectedArea(Rs2TileObjectModel wallObject, List<WorldPoint> otherPlayers)
    {
        if (!ORE_VEIN_IDS.contains(wallObject.getId())) return false;

        WorldPoint location = wallObject.getWorldLocation();

		for (WorldPoint other : otherPlayers)
		{
			if (other.distanceTo(location) <= 2) return false;
		}

		if (config.mineUpstairs())
		{
            return (miningSpot == MLMMiningSpot.WEST_UPPER && WEST_UPPER_AREA.contains(location))
                    || (miningSpot == MLMMiningSpot.EAST_UPPER && EAST_UPPER_AREA.contains(location));
        }
        return (miningSpot == MLMMiningSpot.WEST_LOWER && WEST_LOWER_AREA.contains(location))
                || (miningSpot == MLMMiningSpot.WEST_MID && WEST_LOWER_AREA.contains(location))
                || (miningSpot == MLMMiningSpot.SOUTH_WEST && SOUTH_LOWER_AREA.contains(location))
                || (miningSpot == MLMMiningSpot.SOUTH_EAST && SOUTH_LOWER_AREA.contains(location));
    }

    private boolean hasWalkableTilesAround(Rs2TileObjectModel wallObject)
    {
        return Rs2Tile.areSurroundingTilesWalkable(wallObject.getWorldLocation(), 1, 1);
    }

    private void repositionCameraAndMove()
    {
        Rs2Camera.resetPitch();
        Rs2Camera.resetZoom();
		LocalPoint localTarget = Microbot.getClientThread().invoke(() ->
			LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), miningSpot.getWorldPoint().get(0))
		);
		if (localTarget != null) {
        	Rs2Camera.turnTo(localTarget);
		}
        Rs2Walker.walkFastCanvas(miningSpot.getWorldPoint().get(0));
    }

    private void goUp()
    {
        if (isUpperFloor()) return;
        log.debug("Transitioning to upper floor");

		Rs2TileObjectModel ladder = rs2TileObjectCache.query().withId(ObjectID.MOTHERLODE_LADDER_BOTTOM).nearestReachable();
		if (ladder == null) {
			Rs2Walker.walkTo(miningSpot.getWorldPoint().get(0), 6);
			return;
		}

		if (!ladder.click()) return;

		sleepUntil(() -> Rs2Player.isMoving() || Rs2Player.isAnimating(), 1_500);
		sleepUntil(this::isUpperFloor, 8_000);
    }

    private void goDown()
    {
        if (!isUpperFloor()) return;
        log.debug("Transitioning to lower floor");

		Rs2TileObjectModel ladder = rs2TileObjectCache.query().withId(ObjectID.MOTHERLODE_LADDER_TOP).nearestReachable();
		if (ladder == null) {
			Rs2Walker.walkTo(HOPPER_DEPOSIT_DOWN, 6);
			return;
		}

		if (!ladder.click()) return;

		sleepUntil(() -> Rs2Player.isMoving() || Rs2Player.isAnimating(), 1_500);
        sleepUntil(() -> !isUpperFloor(), 8_000);
    }

    private void ensureLowerFloor()
    {
        if (isUpperFloor()) goDown();
    }

    private boolean isUpperFloor()
    {
		Integer height = Microbot.getClientThread().invoke(() -> {
			if (Microbot.getClient() == null || Microbot.getClient().getLocalPlayer() == null) return null;
			return Perspective.getTileHeight(
				Microbot.getClient(),
				Microbot.getClient().getLocalPlayer().getLocalLocation(),
				0
			);
		});
		return height != null && height < UPPER_FLOOR_HEIGHT;
    }

    private void resetMiningState(boolean force)
    {
        oreVein = null;
        miningSpot = (ThreadLocalRandom.current().nextBoolean() || force) ? MLMMiningSpot.IDLE : miningSpot;
    }

	private void resetMiningState()
	{
		resetMiningState(false);
	}

	private boolean hasHammer() {
		return Rs2Equipment.isWearing("hammer") || Rs2Inventory.hasItem("hammer");
	}

	private boolean obtainHammer() {
		/*

			Typically, the hammer is located near the hopper on the lower floor OR near the sack,
			so we should be close enough to directly interact with it.

			WorldPoint nearestCratePoint = Arrays.stream(CRATE_WALKPOINTS)
				.min(WorldPoint::distanceTo)
				.orElse(CRATE_WALKPOINTS[0]);
			if (!Rs2Walker.walkTo(nearestCratePoint)) return false;
		 */

        if (Rs2Inventory.isFull()) {
            if (Rs2Inventory.interact("pay-dirt", "drop")) {
                sleepUntil(() -> !Rs2Inventory.isFull());
            } else {
                return false;
            }
        }

        while (!Rs2Inventory.hasItem("hammer") && isRunning()) {
            //The crate at this point ALWAYS gives the player a hammer
            rs2TileObjectCache.query().where(obj -> obj.getWorldLocation().equals(new WorldPoint(3752, 5674, 0))).interact("Search");
            Rs2Inventory.waitForInventoryChanges(5_000);
            if (Rs2Inventory.hasItem("hammer")) {
                pickedUpHammer = true;
                log.info("Hammer obtained from crate");
                break;
            }

			sleep(50, 100);
		}

		return pickedUpHammer;
	}

	private void dropHammerIfNeeded() {
		if (pickedUpHammer || (!Rs2Equipment.isWearing("hammer") && Rs2Inventory.hasItem("hammer"))) {
			Rs2Inventory.drop("hammer");
			sleepUntil(() -> !Rs2Inventory.hasItem("hammer"));
			pickedUpHammer = false;
		}
	}

    private void logStatusTransitionIfChanged()
    {
        if (status == lastLoggedStatus)
        {
            return;
        }

        log.info("MLM status transition: {} -> {}", lastLoggedStatus, status);
        lastLoggedStatus = status;
    }

	private Rectangle getMotherloadSackBounds() {
		TileObject sack = rs2TileObjectCache.query().where(o -> o.getId() == ObjectID.MOTHERLODE_SACK).first();
		return Rs2UiHelper.getObjectClickbox(sack);
	}

	private int getBrokenStrutCount() {
		List<Rs2TileObjectModel> brokenStruts = rs2TileObjectCache.query().where(o -> o.getId() == ObjectID.MOTHERLODE_WHEEL_STRUT_BROKEN).toList();
		return brokenStruts.isEmpty() ? 0 : brokenStruts.size();
	}

	private List<String> getItemsToKeep() {
		if (itemsToKeep == null) {
			List<String> _itemsToKeep = new ArrayList<>();
			if (Rs2Inventory.hasItem("hammer")) {
				_itemsToKeep.add("hammer");
			}
			if (Rs2Inventory.hasItem("pickaxe")) {
				_itemsToKeep.add("pickaxe");
			}
			if (Rs2Gembag.hasGemBag()) {
				_itemsToKeep.add("gem bag");
			}
			itemsToKeep = _itemsToKeep;
		}
		return itemsToKeep;
	}

    @Override
    public void shutdown()
    {
        log.info("Starting MLM script shutdown");
        Rs2Antiban.resetAntibanSettings();
        // Named clear rather than setTarget(null): the walker logs an unexplained clear at WARN.
        Rs2Walker.clearWalkingRoute("mlm:shutdown");
		itemsToKeep = null;
        super.shutdown();
        log.info("MLM script shutdown complete");
    }
}
