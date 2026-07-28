package net.runelite.client.plugins.microbot.questhelper.logic;

import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.questhelper.steps.QuestStep;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Custom logic for Priest in Peril.
 *
 * <p>Underground monument step: the golden key must be USED ON the correct monument (only one of the
 * seven accepts it). The generic use-item-on-object path picks the nearest actionable object to the
 * step's defined point, which in that room is the central well — so the key went to the well and the
 * step never advanced. Restrict the target to actual Monuments and rotate through them.
 */
public class PriestInPeril extends BaseQuest {
	private static final String MONUMENT_NAME = "Monument";
	/** The monument that accepts the golden key (3418,9894) — verified in game. */
	private static final int CORRECT_MONUMENT_ID = 3499;

	private int rotation = 0;

	@Override
	public boolean executeCustomLogic() {
		QuestStep questStep = getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep();
		if (questStep == null || questStep.getText() == null || questStep.getText().isEmpty()) {
			return true;
		}

		String text = questStep.getText().get(0).toLowerCase();
		if (!text.contains("monument") || !Rs2Inventory.contains(ItemID.PIPKEY_GOLD)) {
			return true;
		}

		List<Rs2TileObjectModel> monuments = Microbot.getRs2TileObjectCache().query()
				.withName(MONUMENT_NAME)
				.toList().stream()
				.sorted(Comparator.comparing(o -> o.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
				.collect(Collectors.toList());

		if (monuments.isEmpty()) {
			return true; // not in the room yet — let the normal step walk us in
		}

		// Monument 3499 (3418,9894) is always the one that accepts the key — try it first, and only
		// fall back to rotating the others if it somehow isn't in the scene.
		Rs2TileObjectModel monument = monuments.stream()
				.filter(o -> o.getId() == CORRECT_MONUMENT_ID)
				.findFirst()
				.orElseGet(() -> monuments.get(rotation++ % monuments.size()));

		Rs2Inventory.use(ItemID.PIPKEY_GOLD);
		if (!sleepUntil(() -> Microbot.getClient().isWidgetSelected(), 2000)) {
			return false; // selection didn't land; retry next tick
		}

		Microbot.status = "Using golden key on " + MONUMENT_NAME;
		Microbot.log("[PriestInPeril] golden key -> monument id=" + monument.getId()
				+ " at " + monument.getWorldLocation());
		monument.click("");
		sleep(1500, 2200);
		return false; // consumed this tick
	}
}
