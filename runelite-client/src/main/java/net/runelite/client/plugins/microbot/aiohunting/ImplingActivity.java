package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Set;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

/**
 * Impling catching in Puro-Puro. Shares the chase loop but does not roam between
 * curated clusters — implings wander the whole enclosure, so the single area check is
 * enough. Puro-Puro entry/exit travel is still handled by the orchestrator for now.
 */
final class ImplingActivity extends ChaseActivity
{
	private static final int AREA_RADIUS = 50;

	ImplingActivity(AIOHuntingConfig config, AIOHuntingScript script)
	{
		super(config, script);
	}

	@Override
	int areaRadius()
	{
		return AREA_RADIUS;
	}

	@Override
	boolean hasSupplies()
	{
		return Rs2Inventory.hasItem(ItemID.HUNTING_BUTTERFLY_NET)
			&& Rs2Inventory.hasItem(ItemID.II_IMPLING_JAR);
	}

	@Override
	boolean withdraw()
	{
		return script.withdrawDeficit(ItemID.HUNTING_BUTTERFLY_NET, 1)
			&& script.withdrawDeficit(ItemID.II_IMPLING_JAR, config.implingJars());
	}

	@Override
	void collectRetained(Set<Integer> keep)
	{
		keep.add(ItemID.HUNTING_BUTTERFLY_NET);
		keep.add(ItemID.II_IMPLING_JAR);
	}

	@Override
	protected void onNoTarget()
	{
		// Implings roam the whole enclosure; no cluster roaming.
	}
}
