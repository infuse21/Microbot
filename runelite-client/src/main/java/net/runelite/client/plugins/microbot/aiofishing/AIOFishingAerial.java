package net.runelite.client.plugins.microbot.aiofishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingState;
import net.runelite.client.plugins.microbot.aiofishing.enums.AerialCatch;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * Aerial fishing at Lake Molch, driven as a self-contained activity.
 *
 * <p>Aerial shares none of the stage / tool / bank pipeline the rest of the plugin runs on -
 * there is no rod, no bank trip and no fishing stage - so it was always a branch off the side
 * of the state machine. Splitting it out of {@link AIOFishingScript} makes that separation
 * structural: the script asks {@link #determineState()} what to do and dispatches back here,
 * and nothing else in the script has to know aerial exists.</p>
 *
 * <p>{@link AerialFishing} remains the data/gear helper; this class is the loop that uses it.</p>
 */
@Slf4j
class AIOFishingAerial {

    /** How close to the Molch Island platform counts as "there". */
    private static final int SPOT_RADIUS = 12;

    private final AIOFishingScript script;

    AIOFishingAerial(AIOFishingScript script) {
        this.script = script;
    }

    /**
     * Aerial state: verify readiness once, then alternate between throwing the cormorant
     * and knifing the catch into offcuts. Readiness failures stop with a reason rather than
     * silently idling, because none of them can be fixed by waiting.
     */
    AIOFishingState determineState() {
        String unmet = AerialFishing.unmetReason(AIOFishingPlugin.SKILL_LEVELS);
        if (unmet != null) {
            script.requestStop("Aerial fishing: " + unmet);
            return AIOFishingState.STOPPED;
        }
        // Cut when there is no room left to receive another fish. One slot has to stay free
        // for the incoming catch, so cut at <=1 free rather than waiting for a full pack.
        if (AerialFishing.hasUncutCatch()
                && (Rs2Inventory.isFull() || Rs2Inventory.emptySlotCount() <= 1)) {
            return AIOFishingState.AERIAL_CUTTING;
        }
        WorldPoint player = Rs2Player.getWorldLocation();
        if (player == null || player.distanceTo(AerialFishing.MOLCH_ISLAND_SPOT) > SPOT_RADIUS) {
            return AIOFishingState.TRAVELING;
        }
        // Out of bait: gather king worms off the ground rather than banking for them. Only
        // needed to bootstrap - after the first catch the knifed offcuts are the bait.
        if (!AerialFishing.hasBait() && !AerialFishing.hasUncutCatch()) {
            return AIOFishingState.AERIAL_BAITING;
        }
        return AIOFishingState.AERIAL_FISHING;
    }

    /** Throw the cormorant at the nearest pool and wait for it to come back with a fish. */
    void handleFishing() {
        if (AerialFishing.birdIsOut() || Rs2Player.isInteracting()) {
            return; // catch already in flight
        }
        Rs2NpcModel pool = Microbot.getRs2NpcCache().query()
                .withId(NpcID.FISHING_SPOT_AERIAL)
                .nearest();
        if (pool == null) {
            return;
        }
        // The pool has to be on screen to be clicked; turn the camera if it is not. Note
        // Rs2Npc.validateInteractable takes a different Rs2NpcModel class (util.npc, not
        // api.npc.models) and also walks, so turn the camera directly instead.
        if (pool.getNpc() != null && !Rs2Camera.isTileOnScreen(pool.getNpc().getLocalLocation())) {
            Rs2Camera.turnTo(pool.getNpc());
        }
        if (pool.click()) {
            // The glove id flips while the bird is away, which is a cleaner "in progress"
            // signal than an animation check.
            sleepUntil(AerialFishing::birdIsOut, 1500);
            sleepUntil(() -> !AerialFishing.birdIsOut(), 6000);
            script.safeAntibanCooldown();
        }
    }

    /**
     * Pick king worms off the ground to start the bait cycle. Stops only if there are none
     * left to take, since without bait the cormorant cannot be sent.
     */
    void handleBaiting() {
        if (Rs2Inventory.itemQuantity(AerialFishing.GROUND_BAIT_NAME) >= script.config().wormsToPickUp()) {
            return; // enough gathered; determineState moves on next tick
        }
        var worm = Microbot.getRs2TileItemCache().query()
                .withName(AerialFishing.GROUND_BAIT_NAME)
                .nearestOnClientThread(AerialFishing.GROUND_BAIT_RANGE);
        if (worm == null) {
            if (!AerialFishing.hasBait()) {
                script.requestStop("Aerial fishing: no king worms on the ground - wait for a respawn"
                        + " or bring bait");
            }
            return;
        }
        // click() does not walk, so close the gap first (same trap as the fishing spots).
        WorldPoint wormTile = worm.getWorldLocation();
        WorldPoint player = Rs2Player.getWorldLocation();
        if (wormTile != null && player != null && player.distanceTo(wormTile) > 4) {
            Rs2Walker.walkTo(wormTile, 1);
            return;
        }
        if (worm.click("Take")) {
            Rs2Inventory.waitForInventoryChanges(1500);
        }
    }

    /** Knife the catch into stackable offcuts, which double as the next lot of bait. */
    void handleCutting() {
        Rs2ItemModel knife = Rs2Inventory.get(ItemID.KNIFE);
        if (knife == null) {
            script.requestStop("Aerial fishing: carry a knife to cut the catch");
            return;
        }
        Rs2ItemModel fish = Rs2Inventory.getRandom(AerialCatch.rawItemIds());
        if (fish == null) {
            return; // nothing left to cut
        }
        Rs2Inventory.combine(knife, fish);
        sleepUntil(() -> !AerialFishing.hasUncutCatch(), 60000);
        script.safeAntibanCooldown();
    }
}
