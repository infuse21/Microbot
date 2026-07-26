package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

/** Butterfly-net catching (Ruby harvest, Sapphire glacialis, moths, ...). */
final class ButterflyActivity extends ChaseActivity
{
	private static final int JARS = 20;

	ButterflyActivity(AIOHuntingConfig config, AIOHuntingScript script)
	{
		super(config, script);
	}

	@Override
	int areaRadius()
	{
		return config.huntingRadius();
	}

	@Override
	boolean hasSupplies()
	{
		return Rs2Inventory.hasItem(ItemID.HUNTING_BUTTERFLY_NET)
			&& Rs2Inventory.hasItem(ItemID.BUTTERFLY_JAR);
	}

	@Override
	boolean withdraw()
	{
		return script.withdrawDeficit(ItemID.HUNTING_BUTTERFLY_NET, 1)
			&& script.withdrawDeficit(ItemID.BUTTERFLY_JAR, JARS);
	}

	@Override
	void collectRetained(Set<Integer> keep)
	{
		keep.add(ItemID.HUNTING_BUTTERFLY_NET);
		keep.add(ItemID.BUTTERFLY_JAR);
	}
}
