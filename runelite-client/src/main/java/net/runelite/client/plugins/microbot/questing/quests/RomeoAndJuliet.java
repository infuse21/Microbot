package net.runelite.client.plugins.microbot.questing.quests;

import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.microbot.questhelper.steps.QuestStep;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Romeo and Juliet quest custom logic
 */
public class RomeoAndJuliet extends BaseQuest {
    private static final WorldPoint JULIET_STAIR_ORIGIN = new WorldPoint(3159, 3436, 0);
    private static final WorldPoint JULIET_TOP_STAIR = new WorldPoint(3156, 3435, 1);
    private static final WorldPoint ROMEO_LOCATION = new WorldPoint(3211, 3422, 0);

    @Override
    public boolean executeCustomLogic() {
        QuestStep questStep = getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep();
        if (getQuestHelperPlugin().getSelectedQuest().getQuest().getId() == Quest.ROMEO__JULIET.getId()) {
            if (Rs2Dialogue.hasDialogueOptionTitle("Start the Romeo & Juliet quest?")) {
                Rs2Dialogue.keyPressForDialogueOption("Yes.");
                return false;
            }
            if (questStep.getText().contains("Bring the cadava berries to the Apothecary in south east Varrock.")) {
                boolean hasCadavaBerries = fetchCadavaBerries();
                if (!hasCadavaBerries) {
                    return false;
                }
            }
            if (questStep.getText().contains("Bring the potion to Juliet in the house west of Varrock.")) {
                return climbToJulietWithPotion();
            }
            if (questStep.getText().contains("Talk to Romeo in Varrock Square to finish the quest.")) {
                return leaveJulietRoomForRomeo();
            }
        }
        return true;
    }

    private boolean climbToJulietWithPotion() {
        if (!Rs2Inventory.hasItem(ItemID.CADAVA)) {
            return true;
        }

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null || playerLocation.getPlane() != 0) {
            return true;
        }

        if (playerLocation.distanceTo2D(JULIET_STAIR_ORIGIN) > 0) {
            Rs2Walker.walkFastCanvas(JULIET_STAIR_ORIGIN);
            return false;
        }

        Rs2GameObject.interact(ObjectID.FAI_VARROCK_STAIRS_TALLER, "Climb-up");
        Rs2Player.waitForWalking();
        return false;
    }

    private boolean leaveJulietRoomForRomeo() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) {
            return true;
        }

        if (playerLocation.getPlane() == 0) {
            return walkToRomeo();
        }

        if (playerLocation.getPlane() != 1) {
            return true;
        }

        if (playerLocation.distanceTo2D(JULIET_TOP_STAIR) > 4) {
            Rs2Walker.walkFastCanvas(JULIET_TOP_STAIR);
            return false;
        }

        Rs2GameObject.interact(ObjectID.FAI_VARROCK_STAIRS_TOP, "Climb-down");
        Rs2Player.waitForWalking();
        return false;
    }

    private boolean walkToRomeo() {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null || playerLocation.distanceTo2D(ROMEO_LOCATION) <= 12) {
            return true;
        }
        if (playerLocation.distanceTo2D(ROMEO_LOCATION) > 120) {
            return true;
        }

        return Rs2Walker.walkTo(ROMEO_LOCATION, 12);
    }

    private boolean fetchCadavaBerries() {
        if (Rs2Inventory.hasItem(ItemID.CADAVABERRIES)) {
            return true;
        }
        if (Rs2Walker.walkTo(3266, 3374, 0, 10)) {
            Rs2GameObject.interact(new int[] {ObjectID.FAI_VARROCK_CADAVABUSH_2, ObjectID.FAI_VARROCK_CADAVABUSH_1, ObjectID.FAI_VARROCK_CADAVABUSH_0}, "take");
            Rs2Player.waitForWalking();
            Rs2Inventory.waitForInventoryChanges(2000);
        }

        return Rs2Inventory.hasItem(ItemID.CADAVABERRIES);
    }
}
