package net.runelite.client.plugins.microbot.simplewoodcutting.forestry;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ObjectComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.simplewoodcutting.SimpleWoodcuttingPlugin;
import net.runelite.client.plugins.microbot.simplewoodcutting.SimpleWoodcuttingScript;
import net.runelite.client.plugins.microbot.simplewoodcutting.enums.ForestryEvents;

import java.util.Arrays;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;
@Slf4j
public class RootEvent implements BlockingEvent {

    private final SimpleWoodcuttingPlugin plugin;
    public RootEvent(SimpleWoodcuttingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean validate() {
        try{
            if (plugin == null || !Microbot.isPluginEnabled(plugin) || !plugin.isScriptRunning() || plugin.isScriptPaused()) return false;
            if (Microbot.getClient() == null || !Microbot.isLoggedIn()) return false;
            var root = Microbot.getRs2TileObjectCache().query()
                    .where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS)
                    .nearest(SimpleWoodcuttingScript.FORESTRY_DISTANCE);
            var specialRoot = Microbot.getRs2TileObjectCache().query()
                    .where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS_SPECIAL)
                    .nearest(SimpleWoodcuttingScript.FORESTRY_DISTANCE);

            // Is the hasAction Check needed?
            // If special root is present
            if (specialRoot != null)
                return hasAction(specialRoot.getObjectComposition(), "Chop down");
            // If regular root is present
            if (root != null)
                return hasAction(root.getObjectComposition(), "Chop down");

            return false; // No roots found
        } catch (Exception e) {
            log.error("RootEvent: Exception in validate method", e);
            return false;
        }

    }

    @Override
    public boolean execute() {
        Microbot.log("RootEvent: Executing Root event");
        plugin.currentForestryEvent = ForestryEvents.TREE_ROOT;
        while (this.validate()) {
            var root = Microbot.getRs2TileObjectCache().query()
                    .where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS)
                    .nearest(SimpleWoodcuttingScript.FORESTRY_DISTANCE);
            var specialRoot = Microbot.getRs2TileObjectCache().query()
                    .where(x -> x.getId() == ObjectID.GATHERING_EVENT_RISING_ROOTS_SPECIAL)
                    .nearest(SimpleWoodcuttingScript.FORESTRY_DISTANCE);

            // Use special attack if available
            if ( Rs2Equipment.isWearing(ItemID.DRAGON_AXE) || Rs2Equipment.isWearing(ItemID.DRAGON_AXE_2H) || Rs2Equipment.isWearing(ItemID.CRYSTAL_AXE) ||
                    Rs2Equipment.isWearing(ItemID.CRYSTAL_AXE_2H) || Rs2Equipment.isWearing(ItemID.INFERNAL_AXE) ||
                    Rs2Equipment.isWearing(ItemID.TRAILBLAZER_AXE))
                Rs2Combat.setSpecState(true, 1000);

            // If special root is present
            if (specialRoot != null) {

                // Check if the player is already interacting with the special root
                if (Rs2Player.isInteracting() && Rs2Player.getInteracting() != null) {
                    Actor interactingNpc = Rs2Player.getInteracting();
                    if (interactingNpc.getWorldLocation().equals(specialRoot.getWorldLocation())) {
                        continue;
                    }
                }
                // Interact with the special root
                Microbot.log("RootEvent: Interacting with special root at " + specialRoot.getWorldLocation());
                specialRoot.click("Chop down");
                Rs2Player.waitForAnimation(5000);
                sleepUntil(() -> plugin.isScriptPaused() || !Rs2Player.isInteracting(), 40000);
            }
            // If regular root is present
            else if (root != null) {

                // Check if the player is already interacting with the root
                if (Rs2Player.isInteracting() && Rs2Player.getInteracting() != null) {
                    Actor interactingNpc = Rs2Player.getInteracting();
                    if (interactingNpc.getWorldLocation().equals(root.getWorldLocation())) {
                        continue;
                    }
                }
                // Interact with the regular root
                Microbot.log("RootEvent: Interacting with regular root at " + root.getWorldLocation());
                root.click("Chop down");
                Rs2Player.waitForAnimation(5000);
                sleepUntil(() -> plugin.isScriptPaused() || !Rs2Player.isInteracting(), 40000);
            }
        }
        plugin.incrementForestryEventCompleted();
        return true;
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.NORMAL;
    }

    private static boolean hasAction(ObjectComposition composition, String wanted) {
        return composition != null && composition.getActions() != null
                && Arrays.stream(composition.getActions())
                        .anyMatch(action -> action != null && action.equalsIgnoreCase(wanted));
    }
}
