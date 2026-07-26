package net.runelite.client.plugins.microbot.aiohunting;

import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

/**
 * Falconry (Spotted/Dark/Dashing kebbits). Rents a gyr falcon from Matthias, directs it
 * at kebbits and retrieves it (following the hint arrow) to collect the drop.
 */
final class FalconryActivity extends ChaseActivity
{
	private static final int FALCONRY_COINS = 500;
	private static final Set<Integer> FALCON_IDS = Set.of(1342, 1343, 1344, 1345, 1346);

	FalconryActivity(AIOHuntingConfig config, AIOHuntingScript script)
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
		return Rs2Inventory.hasItem(ItemID.FALCON_ON_GLOVES)
			|| Rs2Equipment.isWearing(ItemID.FALCON_ON_GLOVES)
			|| Rs2Inventory.itemQuantity(ItemID.COINS) >= FALCONRY_COINS;
	}

	@Override
	boolean withdraw()
	{
		return script.withdrawDeficit(ItemID.COINS, FALCONRY_COINS);
	}

	@Override
	void collectRetained(Set<Integer> keep)
	{
		keep.add(ItemID.FALCON_GLOVES);
		keep.add(ItemID.FALCON_ON_GLOVES);
	}

	@Override
	protected boolean preCatch()
	{
		if (!ensureFalcon())
		{
			return true;
		}
		NPC hintNpc = Microbot.getClientThread().runOnClientThreadOptional(
			() -> Microbot.getClient().getHintArrowNpc()).orElse(null);
		if (hintNpc != null && FALCON_IDS.contains(hintNpc.getId()))
		{
			Rs2NpcModel falcon = Microbot.getRs2NpcCache().query()
				.where(npc -> npc.getNpc() == hintNpc)
				.nearestOnClientThread();
			if (falcon != null && falcon.click("Retrieve"))
			{
				script.markInteraction();
				script.waitForHuntingAnimation();
			}
			return true;
		}
		return false;
	}

	private boolean ensureFalcon()
	{
		if (Rs2Inventory.hasItem(ItemID.FALCON_ON_GLOVES)
			|| Rs2Equipment.isWearing(ItemID.FALCON_ON_GLOVES))
		{
			return true;
		}
		if (Rs2Dialogue.hasContinue())
		{
			Rs2Dialogue.clickContinue();
			return false;
		}
		if (Rs2Dialogue.hasSelectAnOption())
		{
			if (!Rs2Dialogue.clickOption("Yes", "I'd like", "Okay"))
			{
				script.setRecoveryReason("Choose the falconry rental option");
			}
			return false;
		}

		Rs2NpcModel matthias = Microbot.getRs2NpcCache().query()
			.withName("Matthias")
			.where(npc -> script.isInsideArea(npc.getWorldLocation()))
			.nearestOnClientThread();
		if (matthias == null)
		{
			script.setRecoveryReason("Matthias is not available at the Falconry area");
			return false;
		}
		script.setStatus("Renting a falcon");
		matthias.click("Talk-to");
		return false;
	}
}
