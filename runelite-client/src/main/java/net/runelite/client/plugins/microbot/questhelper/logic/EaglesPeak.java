package net.runelite.client.plugins.microbot.questhelper.logic;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.questhelper.steps.QuestStep;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import static net.runelite.client.plugins.microbot.util.Global.sleep;
import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Custom logic for Eagles' Peak.
 *
 * <p>The quest data has no step for sneaking back OUT of the eagle's nest after freeing Nickolaus:
 * the "Speak to Nickolaus in his camp" phase goes straight to the human exit tunnel, which is not
 * walkable-reachable from inside the nest — the only way out is the eagle's "Walk-past" action
 * (verified live). While that phase is active and we are still in the nest, Walk-past the eagle;
 * the normal step logic (walk to the exit, then the camp) takes over once outside.
 */
public class EaglesPeak extends BaseQuest {
	@Override
	public boolean executeCustomLogic() {
		QuestStep questStep = getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep();
		if (questStep == null || questStep.getText() == null || questStep.getText().isEmpty()) {
			return true;
		}

		if (questStep.getText().get(0).startsWith("Speak to Nickolaus in his camp")) {
			if (isInNest()) {
				var eagle = Microbot.getRs2NpcCache().query()
						.withId(NpcID.EAGLEPEAK_EAGLE_GUARD).nearestOnClientThread();
				if (eagle != null) {
					Microbot.status = "Walk-past the eagle to leave the nest";
					eagle.click("Walk-past");
					// The walk-past escorts us south past the eagle; wait until we're out of the nest.
					sleepUntil(() -> !isInNest(), 15000);
				}
				return false; // consumed this tick; normal steps resume once outside
			}

			if (isInEaglePocket()) {
				// The stone feather-door auto-closes behind us, sealing the pocket between it and the
				// eagle — the pathfinder then has no route at all to the exit tunnel. It's unlocked now
				// (feathers inserted), so Open it and step through to the west side; normal pathing
				// resumes once we're out of the pocket.
				var door = Microbot.getRs2TileObjectCache().query()
						.withId(ObjectID.EAGLEPEAK_GATE_MIRROR).nearestOnClientThread();
				if (door != null) {
					Microbot.status = "Opening the stone door to leave";
					door.click("Open");
					sleep(1200, 1800);
				}
				// Step through the (now open) doorway; harmless if the click above already moved us.
				Rs2Walker.walkFastCanvas(new WorldPoint(2001, 4948, 3));
				sleepUntil(() -> !isInEaglePocket(), 8000);
				return false;
			}
		}

		return true;
	}

	/** Pocket between the stone feather-door (2003,4948) and the eagle, south of the nest. */
	private boolean isInEaglePocket() {
		WorldPoint p = Rs2Player.getWorldLocation();
		return p != null && p.getPlane() == 3
				&& p.getX() >= 2003 && p.getX() <= 2012
				&& p.getY() >= 4944 && p.getY() <= 4954;
	}

	/** North side of the eagle in the nest chamber (instance template coords, plane 3). */
	private boolean isInNest() {
		WorldPoint p = Rs2Player.getWorldLocation();
		return p != null && p.getPlane() == 3
				&& p.getX() >= 1999 && p.getX() <= 2014
				&& p.getY() >= 4955 && p.getY() <= 4968;
	}
}
