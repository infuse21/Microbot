package net.runelite.client.plugins.microbot.api.npc;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;

import java.util.List;

/**
 * Example usage of the NPC Cache & Queryable API
 *
 * IMPORTANT: Always use Microbot.getRs2NpcCache().query() to create queries.
 * Never instantiate Rs2NpcQueryable directly.
 *
 * - Rs2NpcCache: Singleton cache accessed via Microbot.getRs2NpcCache()
 * - query(): Returns a fluent queryable interface for filtering NPCs
 */
public class NpcApiExample {

    public static void examples() {
        Rs2NpcCache cache = Microbot.getRs2NpcCache();

        // Example 1: Get the nearest NPC
        Rs2NpcModel nearestNpc = cache.query().nearestOnClientThread();

        // Example 2: Get the nearest NPC within 10 tiles
        Rs2NpcModel nearestNpcWithinRange = cache.query().nearestOnClientThread(10);

        // Example 3: Find an NPC by name
        Rs2NpcModel goblin = cache.query().withName("Goblin").nearestOnClientThread();

        // Example 4: Find an NPC by multiple names
        Rs2NpcModel enemy = cache.query().withNames("Goblin", "Guard", "Dark wizard").nearestOnClientThread();

        // Example 5: Find an NPC by ID
        Rs2NpcModel npcById = cache.query().withId(1234).nearestOnClientThread();

        // Example 6: Find an NPC by multiple IDs
        Rs2NpcModel npcByIds = cache.query().withIds(1234, 5678, 9012).nearestOnClientThread();

        // Example 7: Get all NPCs with a custom filter
        Rs2NpcModel attackingNpc = cache.query()
                .where(npc -> npc.isInteractingWithPlayer())
                .firstOnClientThread();

        // Example 8: Chain multiple filters
        Rs2NpcModel lowHealthEnemy = cache.query()
                .where(npc -> npc.getName() != null && npc.getName().contains("Goblin"))
                .where(npc -> npc.getHealthPercentage() < 50)
                .nearestOnClientThread();

        // Example 9: Get all NPCs matching criteria as a list
        List<Rs2NpcModel> allGoblins = cache.query()
                .where(npc -> npc.getName() != null && npc.getName().equalsIgnoreCase("Goblin"))
                .toListOnClientThread();

        // Example 10: Complex query - Find nearest low health NPC within 15 tiles
        Rs2NpcModel target = cache.query()
                .where(npc -> npc.getHealthPercentage() > 0 && npc.getHealthPercentage() < 30)
                .where(npc -> !npc.isDead())
                .nearestOnClientThread(15);

        // Example 11: Find NPCs that are moving
        List<Rs2NpcModel> movingNpcs = cache.query()
                .where(Rs2NpcModel::isMoving)
                .toListOnClientThread();

        // Example 12: Find the first NPC with a name
        Rs2NpcModel firstNpc = cache.query()
                .where(npc -> npc.getName() != null)
                .firstOnClientThread();
    }
}
