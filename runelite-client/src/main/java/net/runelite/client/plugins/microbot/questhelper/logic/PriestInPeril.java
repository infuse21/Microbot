package net.runelite.client.plugins.microbot.questhelper.logic;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.questhelper.steps.QuestStep;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.Comparator;
import java.util.List;

import static net.runelite.client.plugins.microbot.util.Global.sleep;

/**
 * Custom logic for Priest in Peril.
 *
 * <p>The underground monument step's text says to "use the Golden Key on it", but the monuments'
 * actual menu actions are {@code Study} / {@code Take-from} — the key is TAKEN FROM the monument,
 * no item-on-object involved. The generic executor therefore kept re-selecting the key and stalling.
 * Take-from the monument the quest's defined point stands us in front of (nearest first, rotating on
 * repeat attempts since only one of the seven holds the key).
 */
public class PriestInPeril extends BaseQuest {
	private static final String MONUMENT_NAME = "Monument";
	private static final String TAKE_ACTION = "Take-from";

	private int rotation = 0;

	@Override
	public boolean executeCustomLogic() {
		QuestStep questStep = getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep();
		if (questStep == null || questStep.getText() == null || questStep.getText().isEmpty()) {
			return true;
		}

		String text = questStep.getText().get(0).toLowerCase();
		if (!text.contains("monument")) {
			return true;
		}

		List<Rs2TileObjectModel> monuments = Microbot.getRs2TileObjectCache().query()
				.withName(MONUMENT_NAME)
				.toList().stream()
				.sorted(Comparator.comparing(o -> o.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
				.collect(java.util.stream.Collectors.toList());

		if (monuments.isEmpty()) {
			return true; // not there yet — let the normal step walk us in
		}

		Rs2TileObjectModel monument = monuments.get(rotation++ % monuments.size());
		Microbot.status = "Take-from " + MONUMENT_NAME;
		Microbot.log("[PriestInPeril] " + TAKE_ACTION + " monument id=" + monument.getId()
				+ " at " + monument.getWorldLocation());
		monument.click(TAKE_ACTION);
		sleep(1200, 1800);
		return false; // consumed this tick
	}
}
