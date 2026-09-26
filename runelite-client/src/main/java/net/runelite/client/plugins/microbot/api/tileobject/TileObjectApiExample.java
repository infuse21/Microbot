package net.runelite.client.plugins.microbot.api.tileobject;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;

import java.util.List;

/**
 * Example usage of the Tile Object Cache & Queryable API
 *
 * IMPORTANT: Always use Microbot.getRs2TileObjectCache().query() to create queries.
 * Never instantiate Rs2TileObjectQueryable directly.
 *
 * - Rs2TileObjectCache: Singleton cache accessed via Microbot.getRs2TileObjectCache()
 * - query(): Returns a fluent queryable interface for filtering tile objects
 */
public class TileObjectApiExample {

    public static void examples() {
        Rs2TileObjectCache cache = Microbot.getRs2TileObjectCache();

        // Example 1: Get the nearest tile object
        Rs2TileObjectModel nearestObject = cache.query().nearestOnClientThread();

        // Example 2: Get the nearest tile object within 10 tiles
        Rs2TileObjectModel nearestObjectWithinRange = cache.query().nearestOnClientThread(10);

        // Example 3: Find a tile object by name
        Rs2TileObjectModel tree = cache.query().withName("Oak tree").nearestOnClientThread();

        // Example 4: Find a tile object by multiple names
        Rs2TileObjectModel bankObj = cache.query().withNames("Bank booth", "Bank chest", "Bank").nearestOnClientThread();

        // Example 5: Find a tile object by ID
        Rs2TileObjectModel objectById = cache.query().withId(1234).nearestOnClientThread();

        // Example 6: Find a tile object by multiple IDs
        Rs2TileObjectModel objectByIds = cache.query().withIds(1234, 5678, 9012).nearestOnClientThread();

        // Example 7: Get all tile objects matching a partial name
        List<Rs2TileObjectModel> doorObjects = cache.query()
                .where(obj -> obj.getName() != null && obj.getName().toLowerCase().contains("door"))
                .toListOnClientThread();

        // Example 8: Find the nearest reachable tile object
        Rs2TileObjectModel reachableObject = cache.query()
                .withName("Ladder")
                .nearestReachableOnClientThread();

        // Example 9: Find the nearest reachable tile object within 15 tiles
        Rs2TileObjectModel nearbyReachable = cache.query()
                .withName("Furnace")
                .nearestReachableOnClientThread(15);

        // Example 10: Get all tile objects as a list
        List<Rs2TileObjectModel> allObjects = cache.query().toListOnClientThread();

        // Example 11: Count tile objects matching criteria
        int treeCount = cache.query()
                .where(obj -> obj.getName() != null && obj.getName().contains("tree"))
                .countOnClientThread();

        // Example 12: Find the nearest bank and interact with it
        Rs2TileObjectModel bank = cache.query()
                .withNames("Bank booth", "Bank chest", "Bank")
                .nearestReachableOnClientThread();
        boolean clickedBank = bank != null && bank.click("Bank");

        // Example 13: Find the nearest object by name and interact with a specific action
        Rs2TileObjectModel door = cache.query()
                .withName("Door")
                .nearestReachableOnClientThread();
        boolean clickedOpen = door != null && door.click("Open");

        // Example 14: Find objects within a specific distance and interact
        Rs2TileObjectModel chopTarget = cache.query()
                .withNames("Tree", "Oak tree", "Willow tree")
                .nearestReachableOnClientThread(5);
        boolean clickedChop = chopTarget != null && chopTarget.click("Chop down");

        // Example 15: Find the first object with a name
        Rs2TileObjectModel firstObject = cache.query()
                .where(obj -> obj.getName() != null)
                .firstOnClientThread();
    }
}
