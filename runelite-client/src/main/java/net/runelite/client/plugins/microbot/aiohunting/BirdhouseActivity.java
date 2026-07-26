package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiohunting.enums.BirdhouseLog;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

/**
 * Self-contained Birdhouse "runner" activity. Unlike the trap/creature grinds this
 * visits a fixed set of spaces once per cycle (build → seed → later empty), banks the
 * loot, then idles until the houses are ready to harvest again.
 *
 * <p>This is the pilot module for the AIO Hunting modular refactor: birdhouse state and
 * logic live here rather than in the orchestrator, which now only asks this class the
 * orchestration questions it needs (supplies, retained items, run status).</p>
 */
final class BirdhouseActivity
{
	static final int AREA_RADIUS = 120;
	private static final int LOGS_PER_RUN = 4;
	private static final int SEEDS_PER_RUN = 40;
	private static final int SEEDS_PER_HOUSE = 10;

	private final AIOHuntingConfig config;
	private final AIOHuntingScript script;

	private int index;
	private boolean runComplete;
	private boolean needsFinalBank;

	BirdhouseActivity(AIOHuntingConfig config, AIOHuntingScript script)
	{
		this.config = config;
		this.script = script;
	}

	/** Resets the runner to the start of a fresh cycle. */
	void reset()
	{
		index = 0;
		runComplete = false;
		needsFinalBank = false;
	}

	int areaRadius()
	{
		return AREA_RADIUS;
	}

	boolean runComplete()
	{
		return runComplete;
	}

	boolean needsFinalBank()
	{
		return needsFinalBank;
	}

	void markFinalBankDone()
	{
		needsFinalBank = false;
	}

	/** Begins a new cycle once houses are ready again. */
	void beginNewRun()
	{
		reset();
	}

	void step()
	{
		if (Rs2Player.isAnimating() || Rs2Player.isMoving())
		{
			return;
		}
		if (index >= BirdhouseData.SPACES.size())
		{
			if (config.emptyBirdNests() && searchOneBirdNest())
			{
				return;
			}
			runComplete = true;
			needsFinalBank = true;
			script.setStatus("Birdhouse run complete");
			return;
		}

		BirdhouseData.Space space = BirdhouseData.SPACES.get(index);
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player == null || player.distanceTo(space.getLocation()) > 5)
		{
			script.walkUntilNotPaused(space.getLocation(), 4,
				() -> Rs2Player.distanceTo(space.getLocation()) <= 5);
			return;
		}

		int stateValue = Microbot.getVarbitPlayerValue(space.getVarpId());
		Rs2TileObjectModel birdhouse = script.findObjectAtWithActions(
			space.getLocation(), 1, "Build", "Empty", "Dismantle");
		if (birdhouse == null)
		{
			script.setStatus("Waiting for birdhouse space");
			return;
		}

		if (BirdhouseData.isSeeded(stateValue))
		{
			if (AIOHuntingScript.hasAction(birdhouse.getObjectComposition(), "Empty"))
			{
				script.setStatus("Emptying birdhouse " + (index + 1));
				if (birdhouse.click("Empty"))
				{
					script.awaitCondition(() -> BirdhouseData.isEmpty(
						Microbot.getVarbitPlayerValue(space.getVarpId())) || script.isPaused(), 10000);
				}
				return;
			}
			index++;
			return;
		}

		if (BirdhouseData.isEmpty(stateValue))
		{
			script.setStatus("Building birdhouse " + (index + 1));
			if (birdhouse.click("Build"))
			{
				script.awaitCondition(() -> !BirdhouseData.isEmpty(
					Microbot.getVarbitPlayerValue(space.getVarpId())) || script.isPaused(), 12000);
			}
			return;
		}

		Rs2ItemModel seed = findInventorySeed(SEEDS_PER_HOUSE).orElse(null);
		if (seed == null)
		{
			script.setRecoveryReason(
				"Need at least " + SEEDS_PER_HOUSE + " accepted seeds for the next birdhouse");
			return;
		}
		script.setStatus("Seeding birdhouse " + (index + 1));
		int before = seed.getQuantity();
		if (Rs2Inventory.use(seed.getId()) && script.awaitCondition(
			() -> Rs2Inventory.getSelectedItemId() == seed.getId() || script.isPaused(), 2000)
			&& birdhouse.click())
		{
			script.awaitCondition(
				() -> Rs2Inventory.itemQuantity(seed.getId()) < before || script.isPaused(), 10000);
			index++;
		}
	}

	private boolean searchOneBirdNest()
	{
		Rs2ItemModel nest = Rs2Inventory.items()
			.filter(item -> BirdhouseData.NEST_IDS.contains(item.getId()))
			.findFirst()
			.orElse(null);
		return nest != null && Rs2Inventory.interact(nest, "Search");
	}

	/** True when at least one house is holding a finished catch ready to empty. */
	boolean isReadyForNewRun()
	{
		for (BirdhouseData.Space space : BirdhouseData.SPACES)
		{
			Rs2TileObjectModel object = script.findObjectAtWithActions(
				space.getLocation(), 1, "Build", "Empty", "Dismantle");
			if (object != null
				&& AIOHuntingScript.hasAction(object.getObjectComposition(), "Empty"))
			{
				return true;
			}
		}
		return false;
	}

	boolean hasSupplies()
	{
		BirdhouseLog logType = selectedLog();
		return Rs2Inventory.hasItem(ItemID.HAMMER)
			&& Rs2Inventory.hasItem(ItemID.CHISEL)
			&& Rs2Inventory.itemQuantity(logType.getItemId()) >= LOGS_PER_RUN
			&& inventorySeedQuantity() >= SEEDS_PER_RUN;
	}

	boolean withdraw()
	{
		BirdhouseLog logType = selectedLog();
		if (!withdrawDeficit(ItemID.HAMMER, 1)
			|| !withdrawDeficit(ItemID.CHISEL, 1)
			|| !withdrawDeficit(logType.getItemId(), LOGS_PER_RUN))
		{
			return false;
		}
		int carriedSeeds = inventorySeedQuantity();
		if (carriedSeeds >= SEEDS_PER_RUN)
		{
			return true;
		}
		Rs2ItemModel bankSeed = Rs2Bank.bankItems().stream()
			.filter(this::isSeed)
			.filter(item -> item.getQuantity() >= SEEDS_PER_RUN - carriedSeeds)
			.findFirst()
			.orElse(null);
		return bankSeed != null && Rs2Bank.withdrawDeficit(bankSeed.getId(), SEEDS_PER_RUN);
	}

	void collectRetained(Set<Integer> keep)
	{
		keep.add(ItemID.HAMMER);
		keep.add(ItemID.CHISEL);
		keep.add(selectedLog().getItemId());
		findInventorySeed(1).ifPresent(seed -> keep.add(seed.getId()));
	}

	private boolean withdrawDeficit(int itemId, int amount)
	{
		return Rs2Inventory.itemQuantity(itemId) >= amount
			|| Rs2Bank.withdrawDeficit(itemId, amount);
	}

	private BirdhouseLog selectedLog()
	{
		if (!config.automaticBirdhouseLog())
		{
			return config.birdhouseLog() == null ? BirdhouseLog.LOGS : config.birdhouseLog();
		}
		return BirdhouseLog.bestFor(
			Rs2Player.getRealSkillLevel(Skill.HUNTER),
			Rs2Player.getRealSkillLevel(Skill.CRAFTING));
	}

	private Optional<Rs2ItemModel> findInventorySeed(int minimum)
	{
		return Rs2Inventory.items()
			.filter(this::isSeed)
			.filter(item -> item.getQuantity() >= minimum)
			.findFirst();
	}

	private boolean isSeed(Rs2ItemModel item)
	{
		return item != null && item.getName() != null
			&& BirdhouseData.ACCEPTED_SEEDS.contains(item.getName().toLowerCase(Locale.ROOT));
	}

	private int inventorySeedQuantity()
	{
		return findInventorySeed(1).map(Rs2ItemModel::getQuantity).orElse(0);
	}
}
