package net.runelite.client.plugins.microbot.questing;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Where quest items spawn in the world, for the ones no bank, shop or Grand Exchange run can be
 * relied on to supply.
 *
 * <p>The gap this fills: Plague City wants dwellberries. They are technically tradeable, but the GE
 * rarely has stock, so the buy offer sits unfilled for 60 seconds and the quester stops — while the
 * berries respawn endlessly on the ground in McGrubor's Wood, minutes away. Same shape as
 * {@link QuestShopCatalog}: a plain data table the executor consults as an acquisition source, where
 * adding an item is one line and an unknown item falls through to the existing behaviour.
 *
 * <p>A spawn entry is a tile to walk to and a loot radius around it. The exact spawn tile doesn't
 * matter — the ground-item cache is queried by item id once the area is loaded — so the anchor only
 * has to be near enough for the scene to contain the spawn. Ground spawns respawn on a timer
 * (typically ~30s), so the collector waits briefly for one to appear before giving up.
 */
public final class QuestGroundSpawnCatalog {
	/** A world spawn: where to stand, and how far around that tile to look for the item. */
	@Value
	public static class SpawnSource {
		WorldPoint location;
		int lootRadius;
	}

	private static final Map<Integer, SpawnSource> SPAWNS = new HashMap<>();

	private static void put(int itemId, int x, int y, int plane, int radius) {
		SPAWNS.put(itemId, new SpawnSource(new WorldPoint(x, y, plane), radius));
	}

	static {
		// McGrubor's Wood, near the red vines (Plague City's dwellberries). Anchor is the vine area
		// the Fishing Contest data also uses; the spawns sit in the western half of the wood and the
		// loot radius covers them from there.
		put(ItemID.DWELLBERRIES, 2631, 3497, 0, 25);
	}

	private QuestGroundSpawnCatalog() {
	}

	/** The spawn for the first of {@code itemIds} we know a world source for, or null. */
	public static SpawnSource lookup(Collection<Integer> itemIds) {
		if (itemIds == null) {
			return null;
		}
		for (Integer id : itemIds) {
			if (id == null) {
				continue;
			}
			SpawnSource source = SPAWNS.get(id);
			if (source != null) {
				return source;
			}
		}
		return null;
	}

	/** The item id from {@code itemIds} that the catalog knows a spawn for, or -1. */
	public static int spawnableId(Collection<Integer> itemIds) {
		if (itemIds == null) {
			return -1;
		}
		for (Integer id : itemIds) {
			if (id != null && SPAWNS.containsKey(id)) {
				return id;
			}
		}
		return -1;
	}
}
