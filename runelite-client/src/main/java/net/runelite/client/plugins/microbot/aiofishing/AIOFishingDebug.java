package net.runelite.client.plugins.microbot.aiofishing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingDebugMode;
import net.runelite.client.plugins.microbot.aiofishing.enums.AIOFishingState;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;

/**
 * Forced workflows for live testing: jump straight to a bank walk, a shop resupply, a GE
 * sale and so on, without waiting for the normal loop to reach that state.
 *
 * <p>Kept apart from the state machine because it is an affordance for the developer, not
 * part of how the plugin fishes. {@link #determineState()} returns null in the normal
 * AUTOMATIC mode, so the real {@code determineState} runs untouched and none of this is in
 * the path of an ordinary session.</p>
 *
 * <p>A forced workflow that cannot start says so through {@link #getBlockReason()} rather
 * than failing silently - the overlay shows it, which is the whole point of the mode.</p>
 */
@Slf4j
class AIOFishingDebug {

    private final AIOFishingScript script;

    /** Last applied debug override, used to initialise forced workflows exactly once. */
    @Getter
    private volatile AIOFishingDebugMode activeMode = AIOFishingDebugMode.AUTOMATIC;
    /** Why a forced workflow is waiting for the user to provide a safe prerequisite. */
    @Getter
    private volatile String blockReason;

    AIOFishingDebug(AIOFishingScript script) {
        this.script = script;
    }

    void reset() {
        activeMode = AIOFishingDebugMode.AUTOMATIC;
        blockReason = null;
    }

    void clearBlockReason() {
        blockReason = null;
    }

    void setBlockReason(String reason) {
        if (!reason.equals(blockReason)) {
            log.warn(reason);
        }
        blockReason = reason;
    }

    /**
     * The state a forced workflow demands, or null to let the normal state machine decide.
     *
     * <p>Switching mode clears every scrap of in-flight work first - order book, pending
     * sale, block reason - so a workflow always starts from a clean slate rather than
     * inheriting half-finished state from the previous one.</p>
     */
    AIOFishingState determineState() {
        AIOFishingDebugMode configuredMode = script.config().debugMode();
        if (configuredMode != activeMode) {
            script.supplies().reset();
            script.grandExchange().reset();
            blockReason = null;
            activeMode = configuredMode;
            log.info("AIO Fishing debug workflow changed to {}.", configuredMode);
        }

        switch (configuredMode) {
            case SELLING_ON_GE:
                script.grandExchange().prepareDebugSale();
                return AIOFishingState.SELLING;
            case RESUPPLYING_FROM_SHOP:
                script.supplies().prepareDebugPurchase(false);
                return AIOFishingState.SHOPPING;
            case RESUPPLYING_FROM_GE:
                script.supplies().prepareDebugPurchase(true);
                return AIOFishingState.BUYING;
            case WALKING_TO_BANK:
                blockReason = null;
                return AIOFishingState.BANKING;
            case WALKING_TO_FISHING_SPOT:
                blockReason = null;
                return AIOFishingState.TRAVELING;
            default:
                blockReason = null;
                return null;
        }
    }

    /** First consumable or tool the active stage is missing, for a forced resupply. */
    String findMissingSupply() {
        for (String consumable : script.getActiveStage().getMethod().getConsumables()) {
            if (!Rs2Inventory.hasItem(consumable)) {
                return consumable;
            }
        }
        for (String tool : script.gear().requiredTools(script.getActiveStage())) {
            if (!script.gear().hasToolAvailable(tool)) {
                return tool;
            }
        }
        return null;
    }
}
