package net.runelite.client.plugins.microbot.questing;

import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.Map;

/**
 * Where to stand to interact with objects that geometry alone can't work out.
 *
 * <p>Some objects sit one tile from somewhere you can stand but are separated by a wall — the Port
 * Sarim food shop's back-room crate being the case that motivated this. Deriving that automatically
 * proved unreliable: the collision map's edge flags conflate a wall with the object's own blocking
 * (the tile outside the wall and the correct tile inside both read as blocked), so a spot that is
 * plainly right to a human keeps being missed.
 *
 * <p>This is the escape hatch: one line per awkward object, stating the tile to stand on. The executor
 * walks there first and only interacts from that tile. Objects not listed here are unaffected and keep
 * using the normal reachability logic.
 */
public final class QuestApproachCatalog {
	private static final Map<Integer, WorldPoint> APPROACH_TILES = new HashMap<>();

	static {
		// Pirate's Treasure: crate in the back room of Wydin's food shop, Port Sarim. Standing west of
		// it puts the shop wall between you and the crate.
		APPROACH_TILES.put(2071, new WorldPoint(3010, 3207, 0));
	}

	private QuestApproachCatalog() {
	}

	/** The tile to stand on to interact with this object, or null when no override is needed. */
	public static WorldPoint lookup(int objectId) {
		return APPROACH_TILES.get(objectId);
	}
}
