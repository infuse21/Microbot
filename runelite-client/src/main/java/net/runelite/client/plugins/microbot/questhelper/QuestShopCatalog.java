package net.runelite.client.plugins.microbot.questhelper;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Where to buy quest items that a bank or the Grand Exchange can't supply.
 *
 * <p>Quest data names the items a step needs but never says which shop stocks them, so this is a
 * lookup the executor consults as its last acquisition source (after bank, after GE). Deliberately a
 * plain data table: adding a shop item is one line, and an unknown item simply falls through to the
 * existing "let the quest produce it" behaviour rather than breaking anything.
 */
public final class QuestShopCatalog {
	/** A shop that stocks an item: the NPC to open it on, and where that NPC stands. */
	@Value
	public static class ShopSource {
		String npcName;
		WorldPoint location;
		/** Whether {@code npcName} must match exactly (false allows "Shop keeper" style partials). */
		boolean exactName;
	}

	private static final Map<Integer, ShopSource> SHOPS = new HashMap<>();

	private static void put(int itemId, String npc, int x, int y, int plane, boolean exact) {
		SHOPS.put(itemId, new ShopSource(npc, new WorldPoint(x, y, plane), exact));
	}

	static {
		// Varrock
		put(ItemID.PINK_SKIRT, "Thessalia", 3206, 3417, 0, true);          // Thessalia's Fine Clothes
		put(ItemID.BEER, "Bartender", 3226, 3400, 0, false);                // Blue Moon Inn
		// Lumbridge general store — the staples most early quests ask for
		put(ItemID.BUCKET_EMPTY, "Shop keeper", 3211, 3247, 0, false);
		put(ItemID.POT_EMPTY, "Shop keeper", 3211, 3247, 0, false);
		put(ItemID.ROPE, "Shop keeper", 3211, 3247, 0, false);
		put(ItemID.BALL_OF_WOOL, "Shop keeper", 3211, 3247, 0, false);
	}

	private QuestShopCatalog() {
	}

	/** The shop for the first of {@code itemIds} we know how to buy, or null. */
	public static ShopSource lookup(Collection<Integer> itemIds) {
		if (itemIds == null) {
			return null;
		}
		for (Integer id : itemIds) {
			if (id == null) {
				continue;
			}
			ShopSource source = SHOPS.get(id);
			if (source != null) {
				return source;
			}
		}
		return null;
	}

	/** The item id from {@code itemIds} that the catalog knows a shop for, or -1. */
	public static int buyableId(Collection<Integer> itemIds) {
		if (itemIds == null) {
			return -1;
		}
		for (Integer id : itemIds) {
			if (id != null && SHOPS.containsKey(id)) {
				return id;
			}
		}
		return -1;
	}
}
