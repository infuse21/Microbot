package net.runelite.client.plugins.microbot.questhelper;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.api.tileobject.models.TileObjectType;
import net.runelite.client.plugins.microbot.questhelper.logic.PiratesTreasure;
import net.runelite.client.plugins.microbot.questhelper.logic.QuestRegistry;
import net.runelite.client.plugins.microbot.questhelper.questinfo.QuestHelperQuest;
import net.runelite.client.plugins.microbot.questhelper.managers.QuestContainerManager;
import net.runelite.client.plugins.microbot.questhelper.questhelpers.QuestHelper;
import net.runelite.client.plugins.microbot.questhelper.requirements.Requirement;
import net.runelite.client.plugins.microbot.questhelper.requirements.item.ItemRequirement;
import net.runelite.client.plugins.microbot.questhelper.steps.*;
import net.runelite.client.plugins.microbot.questhelper.steps.widget.WidgetHighlight;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.grandexchange.models.WikiPrice;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.shop.Rs2Shop;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.walker.WalkerState;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectQueryable;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.api.tileitem.Rs2TileItemQueryable;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.slf4j.event.Level;
import net.runelite.api.coords.WorldArea;

public class QuestScript extends Script {
    public static double version = 0.3;

    private static final long MISSING_REQUIREMENT_NOTIFY_INTERVAL_MS = 10_000L;
    private static final Map<Integer, Long> lastMissingRequirementNotice = new HashMap<>();

    public static List<ItemRequirement> itemRequirements = new ArrayList<>();

    public static List<ItemRequirement> itemsMissing = new ArrayList<>();
    public static List<ItemRequirement> grandExchangeItems = new ArrayList<>();

    private static final AtomicBoolean valeTotemsPromptInFlight = new AtomicBoolean(false);
    private static volatile QuestHelperConfig.ValeTotemsWoodType valeTotemsSessionWoodType;

    private static final AtomicBoolean obtainItemsPromptInFlight = new AtomicBoolean(false);
    private static volatile Boolean obtainItemsSessionChoice;

    boolean unreachableTarget = false;
    int unreachableTargetCheckDist = 1;

    private QuestHelperConfig config;
    private QuestHelperPlugin mQuestPlugin;
    private static Set<Integer> npcsHandled = new HashSet<>();
    private static Set<Long> objectsHandeled = new HashSet<>();

    private int heldTrackingQuestId = -1;
    private final Set<Integer> everHeldItemRequirementIds = new HashSet<>();

    QuestStep dialogueStartedStep = null;

    /**
     * Epoch millis at which the post-dialogue cooldown expires. While
     * {@code System.currentTimeMillis() < dialogueCooldownEndsAt}, the main tick
     * returns early to avoid re-clicking the quest NPC and interrupting scripted
     * animations or cutscenes that play between dialogue exchanges. Set on the
     * transition from in-dialogue to not-in-dialogue; zero means no cooldown.
     */
    private long dialogueCooldownEndsAt = 0;

    /** Throttle for the in-dialogue diagnostic log (see the space-branch in the main tick). */
    private long lastDialogueDiagLog = 0;
    /** Throttle for the tick-phase diagnostic (used to locate where the loop hangs). */
    private long lastPhaseLog = 0;
    private long lastApplyStepMark = 0;

    /**
     * Safety valve against {@link Rs2Dialogue#isInDialogue()} false positives. If the tick loop sits in
     * the in-dialogue space-branch on the same step, pressing space with no genuine dialogue present and
     * no player progress for {@link #DIALOGUE_SPACE_STUCK_MS}, that step is flagged here as a phantom
     * dialogue so the loop stops returning early and lets the step (and the walker) run. Cleared when the
     * active step changes or a real interactive dialogue appears.
     */
    private QuestStep phantomDialogueStep = null;
    private long dialogueSpaceStuckSince = 0;
    private QuestStep dialogueSpaceStuckStep = null;
    private static final long DIALOGUE_SPACE_STUCK_MS = 5000L;



    public boolean run(QuestHelperConfig config, QuestHelperPlugin mQuestPlugin) {
        this.config = config;
        this.mQuestPlugin = mQuestPlugin;


        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!config.startStopQuestHelper()) return;
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (getQuestHelperPlugin().getSelectedQuest() == null) return;
                if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() == null) return;

                if (Rs2Player.isAnimating())
                    Rs2Player.waitForAnimation(3000); // bounded: waitForAnimation() has an unbounded inner sleepUntil

                QuestStep questStep = getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep();
                if (System.currentTimeMillis() - lastPhaseLog > 1500) {
                    lastPhaseLog = System.currentTimeMillis();
                    Microbot.log("[QuestHelper] tick phase=reached-step-eval step=" + (questStep == null ? "null" : questStep.getClass().getSimpleName()), Level.WARN);
                }

                // Drop the phantom-dialogue flag once the quest moves on to a different step.
                if (phantomDialogueStep != null && phantomDialogueStep != questStep)
                    phantomDialogueStep = null;

                if (Rs2Dialogue.isInDialogue() && dialogueStartedStep == null && questStep != phantomDialogueStep)
                    dialogueStartedStep = questStep;

                if (questStep != null && Rs2Widget.isWidgetVisible(ComponentID.DIALOG_OPTION_OPTIONS)) {
                    var dialogOptions = Rs2Widget.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
                    var dialogChoices = dialogOptions != null ? dialogOptions.getDynamicChildren() : null;

                    if (dialogChoices != null) {
                        for (var choice : questStep.getChoices().getChoices()) {
                            if (choice.getExpectedPreviousLine() != null)
                                continue; // TODO

                            if (choice.getExcludedStrings() != null && choice.getExcludedStrings().stream().anyMatch(Rs2Widget::hasWidget))
                                continue;

                            for (var dialogChoice : dialogChoices) {
                                if (dialogChoice == null || dialogChoice.getText() == null
                                        || !dialogChoice.getText().endsWith(choice.getChoice()))
                                    continue;

                                Object[] keyListener = dialogChoice.getOnKeyListener();
                                if (keyListener == null || keyListener.length <= 7 || keyListener[7] == null)
                                    continue;

                                String keyToken = keyListener[7].toString();
                                if (keyToken.isEmpty())
                                    continue;

                                Rs2Keyboard.keyPress(keyToken.charAt(0));
                                return;
                            }
                        }
                    }
                }

                if (questStep != null && !questStep.getWidgetsToHighlight().isEmpty()) {
                    var widgetHighlight = questStep.getWidgetsToHighlight().stream()
                            .filter(x -> x instanceof WidgetHighlight)
                            .map(x -> (WidgetHighlight) x)
                            .filter(x -> Rs2Widget.isWidgetVisible(x.getInterfaceID()))
                            .findFirst().orElse(null);

                    if (widgetHighlight != null) {
                        var widget = Rs2Widget.getWidget(widgetHighlight.getInterfaceID());
                        if (widget != null) {
                            if (widgetHighlight.getChildChildId() != -1) {
                                var childWidget = widget.getChildren()[widgetHighlight.getChildChildId()];
                                if (childWidget != null) {
                                    Rs2Widget.clickWidget(childWidget.getId());
                                    return;
                                }
                            } else {
                                if (widgetHighlight.getNameToCheckFor() != null && !widgetHighlight.getNameToCheckFor().isEmpty()) {
                                    Rs2Widget.clickWidget(widgetHighlight.getNameToCheckFor());
                                } else {
                                    Rs2Widget.clickWidget(widget.getId());
                                    if (Rs2Shop.isOpen() && getQuestHelperPlugin().getSelectedQuest().getQuest().getId() == Quest.PIRATES_TREASURE.getId()) {
                                        Rs2Shop.buyItemOptimally("karamjan rum", 1);
                                    }
                                }
                                return;
                            }
                        }
                    }
                }

                /**
                 * Execute custom logic for the quest
                 */
                var questLogic = QuestRegistry.getQuest(getQuestHelperPlugin().getSelectedQuest().getQuest().getId());
                if (questLogic instanceof PiratesTreasure) {
                    ((PiratesTreasure) questLogic).setMQuestPlugin(mQuestPlugin);
                }
                if (questLogic != null) {
                    if (!questLogic.executeCustomLogic()) {
                        return;
                    }
                }

                if (getQuestHelperPlugin().getSelectedQuest() != null && !Microbot.getClientThread().runOnClientThreadOptional(() ->
                        getQuestHelperPlugin().getSelectedQuest().isCompleted()).orElse(false)) {
                    if (Rs2Widget.isWidgetVisible(ComponentID.DIALOG_OPTION_OPTIONS) && getQuestHelperPlugin().getSelectedQuest().getQuest().getId() != Quest.COOKS_ASSISTANT.getId() && !Rs2Bank.isOpen()) {
                        boolean hasOption = Rs2Dialogue.handleQuestOptionDialogueSelection();
                        //if there is no quest option in the dialogue, just click player location to remove
                        // the dialogue to avoid getting stuck in an infinite loop of dialogues
                        if (!hasOption) {
                            if (Rs2Dialogue.acceptQuestStartDialogue()) {
                                return;
                            }
                            if (getQuestHelperPlugin().getSelectedQuest() != null &&
                                    getQuestHelperPlugin().getSelectedQuest().getQuest().getId() == Quest.IMP_CATCHER.getId()
                                    && Microbot.getClient().getTopLevelWorldView().getPlane() == 1) {
                                Rs2Dialogue.keyPressForDialogueOption(1); // presses option 1
                                sleep(1200,1800);
                            }
                            Rs2Walker.walkFastCanvas(Rs2Player.getWorldLocation());
                        }
                        return;
                    }

                    if (getQuestHelperPlugin().getSelectedQuest() != null &&
                            getQuestHelperPlugin().getSelectedQuest().getQuest().getId() == Quest.COOKS_ASSISTANT.getId() &&
                            Rs2Dialogue.isInDialogue()) {
                        dialogueStartedStep = questStep;  // Force this to be true for Cook's Assistant
                    }

                    if (getQuestHelperPlugin().getSelectedQuest() != null &&
                            getQuestHelperPlugin().getSelectedQuest().getQuest().getId() == Quest.PIRATES_TREASURE.getId() &&
                            Rs2Dialogue.isInDialogue()) {
                        dialogueStartedStep = questStep;
                    }

                    if (Rs2Dialogue.isInDialogue() && dialogueStartedStep == questStep) {
                        // A genuine NPC/player continue or an options list: advance it with space as before.
                        if (Rs2Dialogue.hasInteractiveDialogue()) {
                            dialogueSpaceStuckStep = null;
                            Rs2Walker.clearWalkingRoute("quest-helper:dialogue-space-step");
                            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                            return;
                        }

                        // Suspected phantom (isInDialogue() true with no interactive dialogue). Log the
                        // widget breakdown, throttled, so a recurring false positive can be traced to its
                        // exact source without spamming during real conversations.
                        if (System.currentTimeMillis() - lastDialogueDiagLog > 1500) {
                            lastDialogueDiagLog = System.currentTimeMillis();
                            Microbot.log("[QuestHelper] phantom-dialogue suspected — " + Rs2Dialogue.describeState(), Level.WARN);
                        }

                        // No real dialogue on screen — isInDialogue() is true only on a phantom widget.
                        // Give space a few seconds to clear it (in case it's a slow sprite/item prompt);
                        // if it's still stuck with the player idle, flag the step as a phantom dialogue and
                        // fall through so the step and walker actually run instead of deadlocking here.
                        if (dialogueSpaceStuckStep != questStep) {
                            dialogueSpaceStuckStep = questStep;
                            dialogueSpaceStuckSince = System.currentTimeMillis();
                        }
                        boolean stuckTooLong = System.currentTimeMillis() - dialogueSpaceStuckSince > DIALOGUE_SPACE_STUCK_MS;
                        boolean idle = !Rs2Player.isMoving() && !Rs2Player.isAnimating();
                        if (stuckTooLong && idle) {
                            Microbot.log("[QuestHelper] isInDialogue() stuck true with no interactive dialogue for >"
                                    + DIALOGUE_SPACE_STUCK_MS + "ms — treating step as phantom-dialogue and resuming. "
                                    + Rs2Dialogue.describeState(), Level.WARN);
                            phantomDialogueStep = questStep;
                            dialogueSpaceStuckStep = null;
                            dialogueStartedStep = null;
                            dialogueCooldownEndsAt = 0; // don't let a stale post-dialogue cooldown re-block the step
                            // fall through to the step/walker logic below (do NOT return here)
                        } else {
                            Rs2Walker.clearWalkingRoute("quest-helper:dialogue-space-step");
                            Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                            return;
                        }
                    } else {
                        dialogueSpaceStuckStep = null;
                        if (dialogueStartedStep != null) {
                            dialogueCooldownEndsAt = System.currentTimeMillis() + Rs2Random.between(4000, 7000);
                        }
                        dialogueStartedStep = null;
                    }

                    if (System.currentTimeMillis() < dialogueCooldownEndsAt) {
                        return;
                    }

                    boolean isInCutscene = Microbot.getVarbitValue(4606) > 0;
                    if (isInCutscene) {
                        if (Rs2PathApi.getMarker() != null)
                            Rs2PathApi.exit();
                        return;
                    }

					if (questStep instanceof DetailedQuestStep && handleRequirements((DetailedQuestStep) questStep)) {
						sleep(500, 1000);
						return;
					}

					// Only pre-acquire items for pure item-gathering steps. Object/NPC/Dig steps handle their
					// own interaction (and often PRODUCE the required item — e.g. picking feathers from a
					// pile), so running the acquire flow there just chases later, unobtainable quest items
					// (fakeBeak, disguise feathers) with a blocking walk and freezes the loop.
					if (questStep instanceof DetailedQuestStep
							&& !(questStep instanceof NpcStep || questStep instanceof ObjectStep || questStep instanceof DigStep)
							&& shouldObtainMissingItems() && handleMissingItemRequirements((DetailedQuestStep) questStep)) {
						return;
					}

					/**
					 * This portion is needed when using item on another item in your inventory.
					 * If we do not prioritize this, the script will think we are missing items
					 */
					if (questStep instanceof DetailedQuestStep && !(questStep instanceof NpcStep || questStep instanceof ObjectStep || questStep instanceof DigStep)) {
                        boolean result = applyDetailedQuestStep((DetailedQuestStep) getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep());
                        if (result) {
                            sleepUntil(() -> Rs2Player.isInteracting() || Rs2Player.isMoving() || Rs2Player.isAnimating() || Rs2Dialogue.isInDialogue(), 500);
                            sleepUntil(() -> !Rs2Player.isInteracting() && !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 5000);
                            return;
                        }
                    }

                    if (System.currentTimeMillis() - lastApplyStepMark > 1500) {
                        lastApplyStepMark = System.currentTimeMillis();
                        Microbot.log("[QuestHelper] tick phase=pre-apply-step", Level.WARN);
                    }

                    if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() instanceof ConditionalStep) {
                        QuestStep conditionalStep = getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep();
                        applyStep(conditionalStep);
                    } else if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() instanceof NpcStep) {
                        applyNpcStep((NpcStep) getQuestHelperPlugin().getSelectedQuest().getCurrentStep());
                    } else if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() instanceof ObjectStep) {
                        applyObjectStep((ObjectStep) getQuestHelperPlugin().getSelectedQuest().getCurrentStep());
                    } else if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() instanceof DigStep) {
                        applyDigStep((DigStep) getQuestHelperPlugin().getSelectedQuest().getCurrentStep());
                    } else if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() instanceof PuzzleStep) {
                        applyPuzzleStep((PuzzleStep) getQuestHelperPlugin().getSelectedQuest().getCurrentStep());
                    }

                    sleepUntil(() -> Rs2Player.isInteracting() || Rs2Player.isMoving() || Rs2Player.isAnimating() || Rs2Dialogue.isInDialogue(), 500);
                    sleepUntil(() -> !Rs2Player.isInteracting() && !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 5000);
                }

            } catch (Exception ex) {
                Microbot.log("Quest helper tick error: " + ex.getMessage(), Level.ERROR, ex);
            }
        }, 0, Rs2Random.between(400, 1000), TimeUnit.MILLISECONDS);
        return true;
    }

	private boolean handleRequirements(DetailedQuestStep questStep) {
		var requirements = questStep.getRequirements();

		for (var requirement : requirements) {
			if (requirement instanceof ItemRequirement) {
				var itemRequirement = (ItemRequirement) requirement;

				if (itemRequirement.mustBeEquipped()) {
					if (!hasItemRequirementOnPlayer(itemRequirement)) {
						notifyMissingRequirement(itemRequirement);
						continue;
					}

					if (itemRequirement.getAllIds().stream().noneMatch(Rs2Equipment::isWearing)) {
						Rs2Inventory.wear(itemRequirement.getAllIds().stream().filter(Rs2Inventory::contains).findFirst().orElse(-1));
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean handleMissingItemRequirements(DetailedQuestStep questStep) {
		List<ItemRequirement> missing = new ArrayList<>();
		List<ItemRequirement> needsUnnoting = new ArrayList<>();

		for (Requirement requirement : collectAllItemRequirements(questStep)) {
			if (!(requirement instanceof ItemRequirement)) {
				continue;
			}

			ItemRequirement itemRequirement = (ItemRequirement) requirement;

			if (itemRequirement.mustBeEquipped()
					&& Rs2Inventory.contains(itemRequirement.getAllIds().stream().mapToInt(i -> i).toArray())
					&& itemRequirement.getAllIds().stream().noneMatch(Rs2Equipment::isWearing)) {
				Rs2Inventory.wear(itemRequirement.getAllIds().stream().filter(Rs2Inventory::contains).findFirst().orElse(-1));
				return true;
			}

			if (hasItemRequirementOnPlayer(itemRequirement)) {
				continue;
			}

			if (hasNotedVersionInInventory(itemRequirement)) {
				needsUnnoting.add(itemRequirement);
				continue;
			}

			missing.add(itemRequirement);
		}

		if (!needsUnnoting.isEmpty()) {
			return unnoteItemsViaBank(needsUnnoting);
		}

		if (missing.isEmpty()) {
			return false;
		}

		ItemRequirement nonTradable = missing.stream()
				.filter(ir -> !isItemRequirementTradable(ir))
				.findFirst()
				.orElse(null);

		if (nonTradable != null) {
			return attemptToAcquireRequirementItem(questStep, nonTradable);
		}

		return acquireMissingTradableItems(missing);
	}

	private boolean acquireMissingTradableItems(List<ItemRequirement> missing) {
		notifyMissingRequirement(missing.get(0));

		List<ItemRequirement> actionable = new ArrayList<>();
		for (ItemRequirement ir : missing) {
			if (!hasMatchingGrandExchangeOffer(ir)) {
				actionable.add(ir);
			}
		}

		if (actionable.isEmpty()) {
			if (!Rs2GrandExchange.isOpen()) {
				Microbot.status = "Quest helper: heading to Grand Exchange for in-progress offers";
				Rs2GrandExchange.walkToGrandExchange();
				Rs2GrandExchange.openExchange();
				sleepUntil(Rs2GrandExchange::isOpen, 15_000);
				if (!Rs2GrandExchange.isOpen()) {
					return true;
				}
			}

			if (!Rs2GrandExchange.hasBoughtOffer()) {
				Microbot.status = "Waiting for Grand Exchange offers to fill";
				if (!sleepUntil(Rs2GrandExchange::hasBoughtOffer, 60_000)) {
					stopQuesterWithReason(
							"Grand Exchange offers for missing quest items did not fill within 60 seconds. "
									+ "They may be underpriced or low supply — cancel them manually and retry.");
					return true;
				}
			}

			collectPurchasedItemsViaBank(missing);
			return true;
		}

		if (!Rs2Bank.isOpen()) {
			Microbot.status = "Quest helper: walking to bank for missing items";
			Rs2Bank.walkToBankAndUseBank();
			sleepUntil(Rs2Bank::isOpen, 15_000);
			if (!Rs2Bank.isOpen()) {
				return true;
			}
		}

		List<ItemRequirement> fromBank = new ArrayList<>();
		Map<ItemRequirement, Integer> bankWithdrawId = new HashMap<>();
		List<ItemRequirement> toBuy = new ArrayList<>();

		for (ItemRequirement ir : actionable) {
			int needed = remainingQuantityNeeded(ir);
			if (needed <= 0) {
				continue;
			}

			int bestBankId = -1;
			int bestBankCount = 0;
			for (Integer id : ir.getAllIds()) {
				if (id == null || id <= 0) {
					continue;
				}
				int count = Rs2Bank.count(id);
				if (count > bestBankCount) {
					bestBankCount = count;
					bestBankId = id;
				}
			}

			if (bestBankCount >= needed) {
				fromBank.add(ir);
				bankWithdrawId.put(ir, bestBankId);
			} else {
				toBuy.add(ir);
			}
		}

		long totalBuyCost = 0L;
		Map<ItemRequirement, Integer> offerPrices = new HashMap<>();
		Map<ItemRequirement, Integer> buyQuantities = new HashMap<>();
		Map<ItemRequirement, Integer> buyPrimaryIds = new HashMap<>();

		for (ItemRequirement ir : toBuy) {
			int primaryId = tradablePrimaryId(ir);
			if (primaryId == -1) {
				Rs2Bank.closeBank();
				stopQuesterWithReason("Quest item is not tradable on the Grand Exchange: " + ir.getName());
				return true;
			}

			int basePrice = fetchInstabuyReferencePrice(primaryId);
			if (basePrice <= 0) {
				Rs2Bank.closeBank();
				stopQuesterWithReason("Failed to fetch Grand Exchange price for: " + ir.getName());
				return true;
			}

			int offerPrice = Math.max(1, (int) Math.ceil(basePrice * 1.2));
			int qty = remainingQuantityNeeded(ir);
			buyPrimaryIds.put(ir, primaryId);
			offerPrices.put(ir, offerPrice);
			buyQuantities.put(ir, qty);
			totalBuyCost += (long) offerPrice * qty;
		}

		long invCoins = Rs2Inventory.itemQuantity(ItemID.COINS_995);
		long bankCoins = Rs2Bank.count(ItemID.COINS_995);
		long availableGp = invCoins + bankCoins;

		if (availableGp < totalBuyCost) {
			Rs2Bank.closeBank();
			stopQuesterWithReason(String.format(
					"Not enough gp to buy missing quest items (need %,d gp, have %,d gp)",
					totalBuyCost, availableGp));
			return true;
		}

		if (!toBuy.isEmpty()) {
			int freeSlots = Rs2GrandExchange.getAvailableSlotsCount();
			if (freeSlots < toBuy.size()) {
				Rs2Bank.closeBank();
				stopQuesterWithReason(String.format(
						"Not enough free Grand Exchange slots for missing quest items (need %d, have %d)",
						toBuy.size(), freeSlots));
				return true;
			}
		}

		if (!Rs2Bank.setWithdrawAsItem()) {
			Rs2Bank.closeBank();
			stopQuesterWithReason("Failed to set bank withdraw mode to Item. Toggle it manually and restart.");
			return true;
		}

		for (ItemRequirement ir : fromBank) {
			Integer idToWithdraw = bankWithdrawId.get(ir);
			if (idToWithdraw == null || idToWithdraw <= 0) {
				continue;
			}
			int qty = remainingQuantityNeeded(ir);
			if (qty <= 0) {
				continue;
			}
			Microbot.status = "Withdrawing " + ir.getName() + " x" + qty;
			Rs2Bank.withdrawX(idToWithdraw, qty);
			if (!sleepUntil(() -> hasItemRequirementOnPlayer(ir), 2_000)) {
				Microbot.log("Quest helper: bank withdrawal for " + ir.getName() + " did not land in time",
						Level.WARN);
			}
		}

		if (!toBuy.isEmpty()) {
			final long requiredCoins = totalBuyCost;
			long currentInvCoins = Rs2Inventory.itemQuantity(ItemID.COINS_995);
			if (requiredCoins > currentInvCoins) {
				long coinsStillNeeded = requiredCoins - currentInvCoins;
				int coinsToWithdraw = (int) Math.min(Integer.MAX_VALUE, coinsStillNeeded);
				Microbot.status = "Withdrawing " + coinsToWithdraw + " gp for Grand Exchange";
				Rs2Bank.withdrawX(ItemID.COINS_995, coinsToWithdraw);
				sleepUntil(() -> Rs2Inventory.itemQuantity(ItemID.COINS_995) >= requiredCoins, 3_000);
			}

			long invCoinsAfter = Rs2Inventory.itemQuantity(ItemID.COINS_995);
			if (invCoinsAfter < requiredCoins) {
				Rs2Bank.closeBank();
				stopQuesterWithReason(String.format(
						"Bank withdrawal failed to supply enough coins (need %,d gp, have %,d gp in inventory)",
						requiredCoins, invCoinsAfter));
				return true;
			}
		}

		Rs2Bank.closeBank();
		sleepUntil(() -> !Rs2Bank.isOpen(), 3_000);

		if (toBuy.isEmpty()) {
			return true;
		}

		if (!Rs2GrandExchange.isOpen()) {
			Microbot.status = "Quest helper: walking to Grand Exchange";
			Rs2GrandExchange.walkToGrandExchange();
			Rs2GrandExchange.openExchange();
			sleepUntil(Rs2GrandExchange::isOpen, 15_000);
			if (!Rs2GrandExchange.isOpen()) {
				return true;
			}
		}

		int placedOffers = 0;
		for (ItemRequirement ir : toBuy) {
			int offerPrice = offerPrices.getOrDefault(ir, 0);
			int qty = buyQuantities.getOrDefault(ir, 0);
			int primaryId = buyPrimaryIds.getOrDefault(ir, -1);
			if (offerPrice <= 0 || qty <= 0 || primaryId == -1) {
				continue;
			}

			String canonicalName = canonicalItemName(primaryId);
			if (canonicalName == null || canonicalName.isEmpty() || "null".equalsIgnoreCase(canonicalName)) {
				stopQuesterWithReason("Unable to resolve in-game name for: " + ir.getName());
				return true;
			}

			Microbot.status = "Buying " + canonicalName + " x" + qty;
			if (Rs2GrandExchange.buyItem(canonicalName, offerPrice, qty)) {
				placedOffers++;
			}
			sleep(800, 1500);
		}

		if (placedOffers == 0) {
			stopQuesterWithReason("Failed to place any Grand Exchange offers for missing quest items");
			return true;
		}

		final int maxBuyAttempts = 5;
		final int perAttemptWaitMs = 15_000;

		for (int attempt = 1; attempt <= maxBuyAttempts; attempt++) {
			Microbot.status = String.format(
					"Waiting for Grand Exchange offers to fill (attempt %d/%d)",
					attempt, maxBuyAttempts);

			sleepUntil(() -> itemsStillBuying(toBuy, buyPrimaryIds).isEmpty(), perAttemptWaitMs);

			List<ItemRequirement> stillPending = itemsStillBuying(toBuy, buyPrimaryIds);
			if (stillPending.isEmpty()) {
				break;
			}

			if (attempt >= maxBuyAttempts) {
				Rs2GrandExchange.abortAllOffers(false);
				stopQuesterWithReason(String.format(
						"Grand Exchange offers did not fill after %d attempts with doubled prices. "
								+ "Aborted all offers — check your inventory for any partially-filled items "
								+ "and add more gp before restarting.",
						maxBuyAttempts));
				return true;
			}

			for (ItemRequirement ir : stillPending) {
				int primaryId = buyPrimaryIds.getOrDefault(ir, -1);
				if (primaryId == -1) {
					continue;
				}
				String canonicalName = canonicalItemName(primaryId);
				if (canonicalName == null || canonicalName.isEmpty()) {
					continue;
				}

				long preAbortCoins = Rs2Inventory.itemQuantity(ItemID.COINS_995);

				Microbot.status = "Cancelling unfilled " + canonicalName;
				if (!Rs2GrandExchange.abortOffer(canonicalName, false)) {
					Microbot.log("Quest helper: failed to abort offer for " + canonicalName
							+ "; skipping retry for this item this round", Level.WARN);
					continue;
				}

				sleepUntil(() -> Rs2Inventory.itemQuantity(ItemID.COINS_995) > preAbortCoins, 3_000);

				int alreadyOnHand = inventoryQuantityIncludingNoted(ir);
				int qtyNeeded = Math.max(0, ir.getQuantity() - alreadyOnHand);
				if (qtyNeeded <= 0) {
					Microbot.log("Quest helper: " + canonicalName
							+ " already obtained after abort (have " + alreadyOnHand
							+ "); skipping retry buy", Level.INFO);
					buyQuantities.put(ir, 0);
					continue;
				}
				buyQuantities.put(ir, qtyNeeded);

				int currentPrice = offerPrices.getOrDefault(ir, 0);
				long doubled = (long) currentPrice * 2L;
				int newPrice = doubled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) doubled;
				offerPrices.put(ir, newPrice);

				if (newPrice <= 0) {
					continue;
				}

				long retryInvCoins = Rs2Inventory.itemQuantity(ItemID.COINS_995);
				long costNeeded = (long) newPrice * (long) qtyNeeded;
				if (retryInvCoins < costNeeded) {
					Rs2GrandExchange.abortAllOffers(false);
					stopQuesterWithReason(String.format(
							"Not enough gp to retry %s at %,d gp each (need %,d, have %,d)",
							canonicalName, newPrice, costNeeded, retryInvCoins));
					return true;
				}

				Microbot.status = String.format(
						"Retry %d/%d: buying %s x%d at %,d gp",
						attempt + 1, maxBuyAttempts, canonicalName, qtyNeeded, newPrice);
				if (!Rs2GrandExchange.buyItem(canonicalName, newPrice, qtyNeeded)) {
					Microbot.log("Quest helper: retry buy for " + canonicalName + " failed to place",
							Level.WARN);
				}
				sleep(800, 1500);
			}
		}

		collectPurchasedItemsViaBank(toBuy);
		return true;
	}

	private List<ItemRequirement> itemsStillBuying(List<ItemRequirement> toBuy,
			Map<ItemRequirement, Integer> primaryIds) {
		List<ItemRequirement> pending = new ArrayList<>();
		GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
		if (offers == null) {
			return pending;
		}

		for (ItemRequirement ir : toBuy) {
			int primaryId = primaryIds.getOrDefault(ir, -1);
			if (primaryId == -1) {
				continue;
			}

			for (GrandExchangeOffer offer : offers) {
				if (offer == null) {
					continue;
				}
				if (offer.getItemId() == primaryId
						&& offer.getState() == GrandExchangeOfferState.BUYING) {
					pending.add(ir);
					break;
				}
			}
		}
		return pending;
	}

	private void collectPurchasedItemsViaBank(List<ItemRequirement> items) {
		Microbot.status = "Collecting purchased quest items to bank";
		Rs2GrandExchange.collectAllToBank();
		sleep(800, 1200);

		if (Rs2GrandExchange.isOpen()) {
			Rs2GrandExchange.closeExchange();
			sleepUntil(() -> !Rs2GrandExchange.isOpen(), 2_000);
		}

		if (!Rs2Bank.isOpen()) {
			Microbot.status = "Opening bank to retrieve purchased quest items";
			Rs2Bank.walkToBankAndUseBank();
			sleepUntil(Rs2Bank::isOpen, 15_000);
			if (!Rs2Bank.isOpen()) {
				Microbot.log("Quest helper: failed to open bank after Grand Exchange collection", Level.WARN);
				return;
			}
		}

		if (!Rs2Bank.setWithdrawAsItem()) {
			Rs2Bank.closeBank();
			stopQuesterWithReason("Failed to set bank withdraw mode to Item after Grand Exchange collection. Toggle it manually and restart.");
			return;
		}

		for (ItemRequirement ir : items) {
			int qty = remainingQuantityNeeded(ir);
			if (qty <= 0) {
				continue;
			}

			int idToWithdraw = -1;
			for (Integer id : ir.getAllIds()) {
				if (id == null || id <= 0) {
					continue;
				}
				if (Rs2Bank.count(id) > 0) {
					idToWithdraw = id;
					break;
				}
			}

			if (idToWithdraw == -1) {
				continue;
			}

			Microbot.status = "Withdrawing " + ir.getName() + " x" + qty;
			Rs2Bank.withdrawX(idToWithdraw, qty);
			if (!sleepUntil(() -> hasItemRequirementOnPlayer(ir), 2_000)) {
				Microbot.log("Quest helper: purchased item " + ir.getName() + " did not land in inventory after bank withdrawal",
						Level.WARN);
			}
		}

		Rs2Bank.closeBank();
		sleepUntil(() -> !Rs2Bank.isOpen(), 3_000);
	}

	private boolean isItemRequirementTradable(ItemRequirement itemRequirement) {
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			for (Integer id : itemRequirement.getAllIds()) {
				if (id == null || id <= 0) {
					continue;
				}
				ItemComposition def = Microbot.getClient().getItemDefinition(id);
				if (def != null && def.isTradeable()) {
					return true;
				}
			}
			return false;
		}).orElse(false);
	}

	private int tradablePrimaryId(ItemRequirement itemRequirement) {
		List<Integer> tradableIds = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			List<Integer> ids = new ArrayList<>();
			for (Integer id : itemRequirement.getAllIds()) {
				if (id == null || id <= 0) {
					continue;
				}
				ItemComposition def = Microbot.getClient().getItemDefinition(id);
				if (def != null && def.isTradeable()) {
					ids.add(id);
				}
			}
			return ids;
		}).orElse(new ArrayList<>());

		if (tradableIds.isEmpty()) {
			return -1;
		}

		// Pick the cheapest tradable variant to avoid league/cosmetic items priced at MAX_VALUE
		int bestId = tradableIds.get(0);
		int bestPrice = Integer.MAX_VALUE;
		for (int id : tradableIds) {
			int price = fetchInstabuyReferencePrice(id);
			if (price > 0 && price < bestPrice) {
				bestPrice = price;
				bestId = id;
			}
		}
		return bestId;
	}

	private int remainingQuantityNeeded(ItemRequirement itemRequirement) {
		int onPlayer = itemRequirement.checkTotalMatchesInContainers(
				QuestContainerManager.getEquippedData(),
				QuestContainerManager.getInventoryData());
		return Math.max(0, itemRequirement.getQuantity() - onPlayer);
	}

	private List<Requirement> collectAllItemRequirements(DetailedQuestStep questStep) {
		List<Requirement> combined = new ArrayList<>(questStep.getRequirements());

		Set<Integer> seenIds = new HashSet<>();
		for (Requirement req : questStep.getRequirements()) {
			if (req instanceof ItemRequirement) {
				seenIds.add(((ItemRequirement) req).getId());
			}
		}

		QuestHelper selectedQuest = getQuestHelperPlugin().getSelectedQuest();
		if (selectedQuest != null) {
			updateEverHeldItemTracking(selectedQuest);

			List<ItemRequirement> questLevel = selectedQuest.getItemRequirements();
			if (questLevel != null) {
				for (ItemRequirement ir : questLevel) {
					if (ir == null) {
						continue;
					}
					if (!seenIds.add(ir.getId())) {
						continue;
					}
					if (everHeldItemRequirementIds.contains(ir.getId())) {
						continue;
					}
					combined.add(ir);
				}
			}
		}

		if (selectedQuest != null
				&& selectedQuest.getQuest() != null
				&& selectedQuest.getQuest().getId() == Quest.VALE_TOTEMS.getId()) {
			combined = applyValeTotemsWoodType(combined);
		}

		return combined;
	}

	private void updateEverHeldItemTracking(QuestHelper selectedQuest) {
		if (selectedQuest == null || selectedQuest.getQuest() == null) {
			return;
		}

		int questId = selectedQuest.getQuest().getId();
		if (questId != heldTrackingQuestId) {
			heldTrackingQuestId = questId;
			everHeldItemRequirementIds.clear();

			QuestState state = null;
			try {
				state = selectedQuest.getQuest().getState(Microbot.getClient());
			} catch (Exception ignored) {
			}

			if (state == QuestState.IN_PROGRESS) {
				List<ItemRequirement> questLevel = selectedQuest.getItemRequirements();
				if (questLevel != null) {
					for (ItemRequirement ir : questLevel) {
						if (ir != null) {
							everHeldItemRequirementIds.add(ir.getId());
						}
					}
				}
			}
		}

		List<ItemRequirement> questLevel = selectedQuest.getItemRequirements();
		if (questLevel == null) {
			return;
		}

		for (ItemRequirement ir : questLevel) {
			if (ir == null) {
				continue;
			}
			if (everHeldItemRequirementIds.contains(ir.getId())) {
				continue;
			}
			if (hasItemRequirementOnPlayer(ir)) {
				everHeldItemRequirementIds.add(ir.getId());
			}
		}
	}

	private boolean shouldObtainMissingItems() {
		if (Rs2Player.isIronman()) {
			return false;
		}
		QuestHelperConfig.ObtainMissingItemsOption option = config.obtainMissingItems();
		if (option == QuestHelperConfig.ObtainMissingItemsOption.YES) {
			return true;
		}
		if (option == QuestHelperConfig.ObtainMissingItemsOption.NO) {
			return false;
		}
		if (obtainItemsSessionChoice != null) {
			return obtainItemsSessionChoice;
		}
		promptObtainMissingItems();
		return false;
	}

	private void promptObtainMissingItems() {
		if (!obtainItemsPromptInFlight.compareAndSet(false, true)) {
			return;
		}

		SwingUtilities.invokeLater(() -> {
			try {
				int choice = JOptionPane.showConfirmDialog(
						null,
						"The quest helper has detected missing items.\n\n" +
								"Would you like the quest helper to automatically obtain\n" +
								"missing items from the bank and Grand Exchange?",
						"Obtain Missing Items",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);

				obtainItemsSessionChoice = (choice == JOptionPane.YES_OPTION);
			} finally {
				obtainItemsPromptInFlight.set(false);
			}
		});
	}

	private void promptValeTotemsWoodType() {
		if (!valeTotemsPromptInFlight.compareAndSet(false, true)) {
			return;
		}

		stopQuesterWithReason("Vale Totems: pick a wood type in the dialog to continue.");

		SwingUtilities.invokeLater(() -> {
			try {
				QuestHelperConfig.ValeTotemsWoodType[] values = QuestHelperConfig.ValeTotemsWoodType.values();
				List<QuestHelperConfig.ValeTotemsWoodType> selectable = new ArrayList<>();
				for (QuestHelperConfig.ValeTotemsWoodType value : values) {
					if (value != QuestHelperConfig.ValeTotemsWoodType.ASK) {
						selectable.add(value);
					}
				}

				Object[] options = selectable.stream().map(Object::toString).toArray();

				int choice = JOptionPane.showOptionDialog(
						null,
						"Which wood type are you using for Vale Totems?\n\n" +
								"This must match the logs you used to build the totem.\n" +
								"The quester will source the matching logs and decorative items (shields/longbows/shortbows).",
						"Vale Totems - Wood Type",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						options,
						options[0]);

				if (choice >= 0 && choice < selectable.size()) {
					QuestHelperConfig.ValeTotemsWoodType selected = selectable.get(choice);
					valeTotemsSessionWoodType = selected;
					Microbot.getConfigManager().setConfiguration(
							QuestHelperConfig.QUEST_HELPER_GROUP, "TurnOn", true);
					Microbot.status = "Vale Totems: using " + selected + " (this session)";
					Microbot.log("Quest helper: Vale Totems wood type set to " + selected
							+ " for this session (config stays on 'Ask me')", Level.INFO);
				}
			} finally {
				valeTotemsPromptInFlight.set(false);
			}
		});
	}

	private List<Requirement> applyValeTotemsWoodType(List<Requirement> requirements) {
		QuestHelperConfig.ValeTotemsWoodType configured = config.valeTotemsWoodType();
		QuestHelperConfig.ValeTotemsWoodType woodType;

		if (configured != null && configured != QuestHelperConfig.ValeTotemsWoodType.ASK) {
			woodType = configured;
		} else {
			woodType = valeTotemsSessionWoodType;
			if (woodType == null) {
				promptValeTotemsWoodType();
				return requirements;
			}
		}

		if (woodType == QuestHelperConfig.ValeTotemsWoodType.OAK) {
			return requirements;
		}

		List<Requirement> transformed = new ArrayList<>(requirements.size());
		for (Requirement req : requirements) {
			if (!(req instanceof ItemRequirement)) {
				transformed.add(req);
				continue;
			}

			ItemRequirement ir = (ItemRequirement) req;
			List<Integer> allIds = ir.getAllIds();

			if (allIds.contains(ItemID.OAK_LOGS)) {
				transformed.add(new ItemRequirement(
						woodType + " log", woodType.getLogId(), ir.getQuantity()));
			} else if (allIds.contains(ItemID.OAK_SHIELD)
					|| allIds.contains(ItemID.OAK_LONGBOW)
					|| allIds.contains(ItemID.OAK_SHORTBOW)
					|| allIds.contains(ItemID.OAK_LONGBOW_U)
					|| allIds.contains(ItemID.OAK_SHORTBOW_U)) {
				transformed.add(new ItemRequirement(
						woodType + " shield/longbow/shortbow",
						woodType.getDecorativeIds(),
						ir.getQuantity()));
			} else {
				transformed.add(req);
			}
		}
		return transformed;
	}

	private String canonicalItemName(int itemId) {
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			ItemComposition def = Microbot.getClient().getItemDefinition(itemId);
			return def != null ? def.getName() : null;
		}).orElse(null);
	}

	private int fetchInstabuyReferencePrice(int itemId) {
		WikiPrice priceData = Rs2GrandExchange.getRealTimePrices(itemId);
		if (priceData != null && priceData.buyPrice > 0 && priceData.buyPrice < Integer.MAX_VALUE) {
			return priceData.buyPrice;
		}
		int price = Rs2GrandExchange.getPrice(itemId);
		return (price > 0 && price < Integer.MAX_VALUE) ? price : -1;
	}

	private int notedVariantId(int unnotedId) {
		return Microbot.getClientThread().runOnClientThreadOptional(() -> {
			ItemComposition def = Microbot.getClient().getItemDefinition(unnotedId);
			if (def == null) {
				return -1;
			}
			if (def.getNote() == 799) {
				return -1;
			}
			int linked = def.getLinkedNoteId();
			return linked > 0 ? linked : -1;
		}).orElse(-1);
	}

	private boolean hasNotedVersionInInventory(ItemRequirement itemRequirement) {
		if (remainingQuantityNeeded(itemRequirement) <= 0) {
			return false;
		}
		for (Integer id : itemRequirement.getAllIds()) {
			if (id == null || id <= 0) {
				continue;
			}
			int notedId = notedVariantId(id);
			if (notedId > 0 && Rs2Inventory.itemQuantity(notedId) > 0) {
				return true;
			}
		}
		return false;
	}

	private int inventoryQuantityIncludingNoted(ItemRequirement itemRequirement) {
		int total = 0;
		for (Integer id : itemRequirement.getAllIds()) {
			if (id == null || id <= 0) {
				continue;
			}
			total += Rs2Inventory.itemQuantity(id);
			int notedId = notedVariantId(id);
			if (notedId > 0) {
				total += Rs2Inventory.itemQuantity(notedId);
			}
		}
		return total;
	}

	private boolean unnoteItemsViaBank(List<ItemRequirement> items) {
		if (!Rs2Bank.isOpen()) {
			Microbot.status = "Quest helper: walking to bank to un-note items";
			Rs2Bank.walkToBankAndUseBank();
			sleepUntil(Rs2Bank::isOpen, 15_000);
			if (!Rs2Bank.isOpen()) {
				return true;
			}
		}

		if (!Rs2Bank.setWithdrawAsItem()) {
			Rs2Bank.closeBank();
			stopQuesterWithReason("Failed to set bank withdraw mode to Item while un-noting quest items. Toggle it manually and restart.");
			return true;
		}

		for (ItemRequirement ir : items) {
			int needed = remainingQuantityNeeded(ir);
			if (needed <= 0) {
				continue;
			}

			for (Integer unnotedId : ir.getAllIds()) {
				if (unnotedId == null || unnotedId <= 0) {
					continue;
				}
				int notedId = notedVariantId(unnotedId);
				if (notedId <= 0) {
					continue;
				}

				int notesInInventory = Rs2Inventory.itemQuantity(notedId);
				if (notesInInventory <= 0) {
					continue;
				}

				Microbot.status = "Depositing noted " + ir.getName();
				Rs2Bank.depositAll(notedId);
				sleepUntil(() -> Rs2Inventory.itemQuantity(notedId) == 0, 2_000);

				int toWithdraw = Math.min(notesInInventory, needed);
				Microbot.status = "Withdrawing " + ir.getName() + " x" + toWithdraw;
				Rs2Bank.withdrawX(unnotedId, toWithdraw);
				if (!sleepUntil(() -> hasItemRequirementOnPlayer(ir), 2_000)) {
					Microbot.log("Quest helper: un-note withdrawal for " + ir.getName() + " did not land in time",
							Level.WARN);
				}
				break;
			}
		}

		Rs2Bank.closeBank();
		sleepUntil(() -> !Rs2Bank.isOpen(), 3_000);
		return true;
	}

	private boolean hasMatchingGrandExchangeOffer(ItemRequirement itemRequirement) {
		GrandExchangeOffer[] offers = Microbot.getClient().getGrandExchangeOffers();
		if (offers == null) {
			return false;
		}

		Set<Integer> ids = new HashSet<>();
		for (Integer id : itemRequirement.getAllIds()) {
			if (id != null && id > 0) {
				ids.add(id);
			}
		}

		for (GrandExchangeOffer offer : offers) {
			if (offer == null) {
				continue;
			}
			GrandExchangeOfferState state = offer.getState();
			if ((state == GrandExchangeOfferState.BUYING || state == GrandExchangeOfferState.BOUGHT)
					&& ids.contains(offer.getItemId())) {
				return true;
			}
		}

		return false;
	}

	private void stopQuesterWithReason(String reason) {
		Microbot.status = reason;
		Microbot.log("Quest helper stopped: " + reason, Level.ERROR);
		if (Microbot.getConfigManager() != null) {
			Microbot.getConfigManager().setConfiguration(
					QuestHelperConfig.QUEST_HELPER_GROUP, "TurnOn", false);
		}
	}

	private boolean hasItemRequirementOnPlayer(ItemRequirement itemRequirement) {
		if (itemRequirement.mustBeEquipped()) {
			return itemRequirement.checkContainers(QuestContainerManager.getEquippedData());
		}

		return itemRequirement.checkContainers(
				QuestContainerManager.getEquippedData(),
				QuestContainerManager.getInventoryData());
	}

	private boolean attemptToAcquireRequirementItem(DetailedQuestStep questStep, ItemRequirement itemRequirement) {
		notifyMissingRequirement(itemRequirement);

		WorldPoint worldPoint = questStep.getDefinedPoint() != null ? questStep.getDefinedPoint().getWorldPoint() : null;
		int targetItemId = itemRequirement.getAllIds().stream().findFirst().orElse(itemRequirement.getId());

		if (worldPoint != null) {
			if ((Rs2Walker.canReach(worldPoint) && worldPoint.distanceTo(Rs2Player.getWorldLocation()) < 2)
					|| worldPoint.toWorldArea().hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), Rs2Player.getWorldLocation().toWorldArea())
					&& Rs2Camera.isTileOnScreen(LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), worldPoint))) {
				lootGroundItem(targetItemId, 10);
			} else {
				Rs2Walker.walkTo(worldPoint, 2); // full walker (handles transports on long legs)
			}
		} else {
			lootGroundItem(targetItemId, 20);
		}

		return true;
	}

	private void notifyMissingRequirement(ItemRequirement itemRequirement) {
		int key = itemRequirement.getAllIds().stream().findFirst().orElse(itemRequirement.getId());
		long now = System.currentTimeMillis();
		Long lastNotified = lastMissingRequirementNotice.get(key);

		if (lastNotified != null && now - lastNotified < MISSING_REQUIREMENT_NOTIFY_INTERVAL_MS) {
			return;
		}

		lastMissingRequirementNotice.put(key, now);

		String itemName = itemRequirement.getName() != null && !itemRequirement.getName().isEmpty()
				? itemRequirement.getName()
				: "Item " + key;
		int quantity = Math.max(itemRequirement.getQuantity(), 1);

		Microbot.status = "Missing: " + itemName;
		Microbot.log(String.format("Quest helper missing required item: %s x%d", itemName, quantity), Level.WARN);
	}

	private boolean lootGroundItem(int itemId, int radius) {
		Rs2TileItemModel item = new Rs2TileItemQueryable()
				.withId(itemId)
				.within(radius)
				.nearest();

		if (item == null) {
			return false;
		}

		return item.click("");
	}

	@Override
	public void shutdown() {
		super.shutdown();
		reset();
	}

    public static void reset() {
        itemsMissing = new ArrayList<>();
        itemRequirements = new ArrayList<>();
        grandExchangeItems = new ArrayList<>();
        npcsHandled.clear();
        objectsHandeled.clear();
        lastMissingRequirementNotice.clear();
        valeTotemsPromptInFlight.set(false);
        valeTotemsSessionWoodType = null;
        obtainItemsPromptInFlight.set(false);
        obtainItemsSessionChoice = null;
    }

    public boolean applyStep(QuestStep step) {
        if (step == null) return false;

        if (step instanceof ObjectStep) {
            return applyObjectStep((ObjectStep) step);
        } else if (step instanceof NpcStep) {
            return applyNpcStep((NpcStep) step);
        } else if (step instanceof WidgetStep) {
            return applyWidgetStep((WidgetStep) step);
        } else if (step instanceof DigStep) {
            return applyDigStep((DigStep) step);
        } else if (step instanceof PuzzleStep) {
            return applyPuzzleStep((PuzzleStep) step);
        } else if (step instanceof DetailedQuestStep) {
            return applyDetailedQuestStep((DetailedQuestStep) step);
        }
        return true;
    }

    public boolean applyNpcStep(NpcStep step) {
        List<Rs2NpcModel> resolvedNpcs = step.getNpcs().stream()
                .map(Rs2NpcModel::new)
                .collect(Collectors.toList());

        // Fallback when the NpcStep's own scan found nothing: it can miss an NPC that is loaded but
        // beyond its roam/render range (e.g. Nickolaus across the Eagles' Peak chasm), leaving us to
        // blindly walk to the defined point and loop on UNREACHABLE. Pull the target id(s) straight from
        // the live NPC cache so we can still interact.
        boolean usedNpcFallback = false;
        if (resolvedNpcs.isEmpty()) {
            List<Rs2NpcModel> fromCache = new ArrayList<>(
                    Microbot.getRs2NpcCache().query().withId(step.getNpcID()).toListOnClientThread());
            for (Integer altId : step.getAlternateNpcIDs()) {
                if (altId != null) {
                    fromCache.addAll(Microbot.getRs2NpcCache().query().withId(altId).toListOnClientThread());
                }
            }
            resolvedNpcs = fromCache;
            usedNpcFallback = true;
        }

        final List<Rs2NpcModel> npcs = resolvedNpcs;
        Rs2NpcModel npc = npcs.stream().findFirst().orElse(null);

        if (System.currentTimeMillis() - lastDialogueDiagLog > 1500) {
            lastDialogueDiagLog = System.currentTimeMillis();
            boolean cr = npc != null && Rs2Walker.canReach(npc.getWorldLocation());
            boolean os = npc != null && npc.getLocalLocation() != null && Rs2Camera.isTileOnScreen(npc.getLocalLocation());
            boolean los = npc != null && npc.hasLineOfSight();
            Microbot.log(String.format(
                    "[QuestHelper] npcStep id=%d fallback=%s found=%s canReach=%s onScreen=%s LOS=%s instanced=%s",
                    step.getNpcID(), usedNpcFallback, npc != null, cr, os, los,
                    Microbot.getClient().isInInstancedRegion()), Level.WARN);
        }

        if (step.isAllowMultipleHighlights()) {
            npc = npcs.stream()
                    .filter(x -> !npcsHandled.contains(x.getIndex()))
                    .findFirst()
                    .orElseGet(() -> npcs.stream()
                            .min(Comparator.comparing(x -> Rs2Player.getWorldLocation().distanceTo(x.getWorldLocation())))
                            .orElse(null));
        }

        // Decide whether to interact now or walk closer first:
        //  - instanced region: interact (canReach/LOS are unreliable there).
        //  - on screen AND line of sight: interact (a direct click resolves).
        //  - NOT walkable-reachable: interact regardless of on-screen — this covers "interact across a
        //    gap" steps (e.g. shouting to Nickolaus across the chasm). canReach() is false because there
        //    is no path, so walking loops forever on UNREACHABLE; the click works at range, and npc.click()
        //    turns the camera itself if the target is off screen. Do NOT gate this on isTileOnScreen — the
        //    closest reachable tile often leaves the target just off screen, which is what made it loop.
        // Otherwise (reachable but off screen, or on screen but LOS-blocked by a closed door) fall through
        // to walkTo() so the walker approaches / opens the door.
        if (npc != null && npc.getLocalLocation() != null
                && (Microbot.getClient().isInInstancedRegion()
                    || (Rs2Camera.isTileOnScreen(npc.getLocalLocation()) && npc.hasLineOfSight())
                    || !Rs2Walker.canReach(npc.getWorldLocation()))) {
            Rs2Walker.clearWalkingRoute("quest-helper:npc-step-visible-interact");

            if (step.getText().stream().anyMatch(x -> x.toLowerCase().contains("kill"))) {
                if (!Rs2Combat.inCombat()) {
                    npc.click("Attack");
                }
                return true;
            }

            if (step instanceof NpcEmoteStep) {
                var emoteStep = (NpcEmoteStep) step;

                var emoteContainer = Rs2Widget.getWidget(ComponentID.EMOTES_EMOTE_CONTAINER);
                if (emoteContainer == null || emoteContainer.getDynamicChildren() == null) {
                    return false;
                }

                for (Widget emoteWidget : emoteContainer.getDynamicChildren()) {
                    if (emoteWidget.getSpriteId() == emoteStep.getEmote().getSpriteId()) {
                        var id = emoteWidget.getOriginalX() / 42 + ((emoteWidget.getOriginalY() - 6) / 49) * 4;

                        Microbot.doInvoke(new NewMenuEntry()
                                        .option("Perform")
                                        .target(emoteWidget.getText())
                                        .identifier(1)
                                        .type(MenuAction.CC_OP)
                                        .param0(id)
                                        .param1(ComponentID.EMOTES_EMOTE_CONTAINER)
                                , new Rectangle(0, 0, 1, 1));

                        Rs2Player.waitForAnimation();

                        if (Rs2Dialogue.isInDialogue())
                            return false;
                    }
                }
            }

            var itemId = step.getIconItemID();
            if (itemId != -1) {
                Rs2Inventory.use(itemId);
                npc.click("");
            } else {
                npc.click(chooseCorrectNPCOption(step, npc));
            }

            if (step.isAllowMultipleHighlights()) {
                npcsHandled.add(npc.getIndex());
                sleepUntil(Rs2Dialogue::isInDialogue, 3000);
            }
        } else if (npc != null || step.getDefinedPoint() != null) {
            // Not clickable yet — walk toward him, letting the walker CANCEL the instant he's clickable,
            // even if his own tile is unreachable across a gap (walkWithStateUntil checks the completion
            // inside its loop and returns ARRIVED early). Then click him RIGHT THEN, in this same call —
            // a wandering NPC (e.g. Nickolaus) can pace back out of the clickable window before the next
            // tick, so deferring the interact loses the race.
            final NpcStep fStep = step;
            // Walk toward the step's DEFINED POINT (stable), not the wandering NPC. The completion only
            // fires once we've reached the tight approach to it (npcReadyToClick checks distance to the
            // defined point), so we click from the correct chasm-edge tile rather than an early
            // line-of-sight tile from which the server routes the click the wrong way around.
            WorldPoint walkTarget = step.getDefinedPoint() != null
                    ? step.getDefinedPoint().getWorldPoint()
                    : (npc != null ? npc.getWorldLocation() : null);
            if (walkTarget != null
                    && Rs2Walker.walkWithStateUntil(walkTarget, 2, () -> npcReadyToClick(fStep)) == WalkerState.ARRIVED) {
                Rs2Walker.setTarget(null, "quest-helper:npc-arrived-interact");
                Rs2NpcModel target = resolveStepNpcFromCache(fStep);
                if (target != null) {
                    var itemId = step.getIconItemID();
                    if (itemId != -1) {
                        Rs2Inventory.use(itemId);
                        target.click("");
                    } else {
                        target.click(chooseCorrectNPCOption(step, target));
                    }
                }
            }
            return false;
        }
        return true;
    }

    /**
     * True when the step's NPC is loaded and can be clicked now — used as the {@link Rs2Walker#walkUntil}
     * early-exit while approaching. "Clickable" means in an instance, or we have line of sight to it, or
     * its own tile isn't walkable-reachable (interact-across-a-gap, e.g. shouting across a chasm). The
     * click itself turns the camera, so on-screen is deliberately not required here.
     */
    private static final int NPC_APPROACH_MAX_DIST = 8;

    private boolean npcReadyToClick(NpcStep step) {
        Rs2NpcModel n = resolveStepNpcFromCache(step);
        if (n == null || n.getLocalLocation() == null) {
            return false;
        }
        boolean clickable = Microbot.getClient().isInInstancedRegion()
                || n.hasLineOfSight()
                || !Rs2Walker.canReach(n.getWorldLocation());
        if (!clickable) {
            return false;
        }
        // Only click once we've reached the tight approach to the step's target tile. Anchor to the
        // stable defined point (not the wandering NPC) so a NPC that paces around can't trip the click
        // from too far out — clicking from a far tile makes the server path the wrong way around.
        WorldPoint dp = step.getDefinedPoint() != null ? step.getDefinedPoint().getWorldPoint() : null;
        if (dp != null) {
            WorldPoint player = Rs2Player.getWorldLocation();
            return player != null && player.distanceTo(dp) <= NPC_APPROACH_MAX_DIST;
        }
        return true;
    }

    /** Nearest live-cache NPC matching the step's id or its alternates, or null if none is loaded. */
    private Rs2NpcModel resolveStepNpcFromCache(NpcStep step) {
        Rs2NpcModel n = Microbot.getRs2NpcCache().query().withId(step.getNpcID())
                .toListOnClientThread().stream().findFirst().orElse(null);
        if (n != null) {
            return n;
        }
        for (Integer altId : step.getAlternateNpcIDs()) {
            if (altId == null) {
                continue;
            }
            n = Microbot.getRs2NpcCache().query().withId(altId)
                    .toListOnClientThread().stream().findFirst().orElse(null);
            if (n != null) {
                return n;
            }
        }
        return null;
    }


    public boolean applyObjectStep(ObjectStep step) {
        Rs2TileObjectModel object = step.getObjects().stream()
                .filter(Objects::nonNull)
                .map(Rs2TileObjectModel::new)
                .findFirst().orElse(null);
        var itemId = step.getIconItemID();

        List<Rs2TileObjectModel> stepObjects = step.getObjects().stream()
                .filter(Objects::nonNull)
                .map(Rs2TileObjectModel::new)
                .collect(Collectors.toList());

        if (stepObjects.size() > 1) {
            object = stepObjects.stream()
                    .filter(x -> !objectsHandeled.contains(x.getHash()))
                    .findFirst()
                    .orElseGet(() -> stepObjects.stream()
                            .min(Comparator.comparing(x -> Rs2Player.getWorldLocation().distanceTo(x.getWorldLocation())))
                            .orElse(null));
        }

        // Fallback when the questhelper's ObjectStep scan came up empty: it only inspects its exact defined
        // tile, so it misses the target when the real object sits a tile or two away (a common quest-data
        // quirk) or is a multiloc. Query the live object cache near the defined point for the step's target
        // id (matching the multiloc impostor too) so we interact instead of standing idle.
        if (object == null && step.getDefinedPoint() != null && step.getDefinedPoint().getWorldPoint() != null) {
            final WorldPoint dp = step.getDefinedPoint().getWorldPoint();
            final Set<Integer> targetIds = new HashSet<>(step.getAlternateObjectIDs());
            targetIds.add(step.getObjectID());
            object = new Rs2TileObjectQueryable()
                    .where(o -> o.getWorldLocation() != null
                            && o.getWorldLocation().distanceTo(dp) <= 3
                            && objectMatchesIds(o, targetIds))
                    .toList().stream()
                    .min(Comparator.comparing(o -> o.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
                    .orElse(null);
        }

        // Clear a stale "I can't reach that!" flag once the target is reachable, so the recovery below
        // (for genuinely unreachable objects) can't trap us in place next to an already-reachable target.
        if (unreachableTarget && object != null && Rs2Walker.canReach(object.getWorldLocation())) {
            unreachableTarget = false;
            unreachableTargetCheckDist = 1;
        }

        if (object != null && unreachableTarget) {
            var tileObjects = new Rs2TileObjectQueryable()
                    .where(x -> x.getTileObjectType() == TileObjectType.WALL)
                    .toList();

            for (var tile : Rs2Tile.getWalkableTilesAroundTile(object.getWorldLocation(), unreachableTargetCheckDist)) {
                if (tileObjects.stream().noneMatch(x -> x.getWorldLocation().equals(tile))) {
                    // Sidestep toward a reachable tile; clear the flag and re-evaluate next tick.
                    Rs2Walker.walkTo(tile);
                    unreachableTarget = false;
                    unreachableTargetCheckDist = 1;
                    return false;
                }
            }

            unreachableTargetCheckDist++;
            return false;
        }

        // Walk first when more than one tile away AND the target is either unreachable or not in line of
        // sight. canReach() can still return true with a closed door between us; routing through the
        // walker lets it open the door before we try to interact.
        if (step.getDefinedPoint().getWorldPoint() != null && Rs2Player.getWorldLocation().distanceTo2D(step.getDefinedPoint().getWorldPoint()) > 1
                && (object == null || !Rs2Walker.canReach(object.getWorldLocation()) || !hasLineOfSightToObject(object))) {
            WorldPoint targetTile = null;
            WorldPoint stepLocation = object == null ? step.getDefinedPoint().getWorldPoint() : object.getWorldLocation();
            int radius = 0;
            while (targetTile == null) {
                if (mainScheduledFuture.isCancelled())
                    break;
                radius++;
                Rs2TileObjectModel finalObject = object;
                targetTile = Rs2Tile.getWalkableTilesAroundTile(stepLocation, radius)
                        .stream().filter(x -> hasLineOfSightFrom(x, finalObject))
                        .sorted(Comparator.comparing(x -> x.distanceTo(Rs2Player.getWorldLocation()))).findFirst().orElse(null);

                if (radius > 10 && targetTile == null)
                    targetTile = stepLocation;
            }

            // Full walker to the approach tile (handles transports/doors on long legs). Blocks during
            // travel, then the adjacent-click gate below fires once we're next to the object.
            Rs2Walker.walkTo(targetTile, 3);
            return false;
        }

        // Once we're standing next to a reachable object, click it — don't gate on the on-screen/LOS
        // heuristic. Full-tile objects (e.g. a searchable pile of books) block line-of-sight to their own
        // tile, and the snapshot's local/canvas location can be unresolved, so that heuristic goes all-false
        // and we fall through to walkTo() forever, nudging in place next to the target and never interacting.
        boolean adjacentAndReachable = object != null
                && Rs2Walker.canReach(object.getWorldLocation())
                && Rs2Player.getWorldLocation().distanceTo(object.getWorldLocation()) <= 2;

        if (adjacentAndReachable
                || hasLineOfSightToObject(object)
                || object != null && (Rs2Camera.isTileOnScreen(object.getLocalLocation()) || object.getCanvasLocation() != null)) {
            Rs2Walker.clearWalkingRoute("quest-helper:object-step-interact");

            // Re-resolve the object fresh from the live cache on the client thread right before clicking.
            // The step-scan / background snapshot model can be stale, producing a menu action the game
            // silently ignores (observed: repeatedly clicks "Enter" on the cave entrance but never enters).
            // The fresh cache model is what actually works — the same path the agent-server interact uses.
            Rs2TileObjectModel freshObject = Microbot.getRs2TileObjectCache().query()
                    .withId(object.getId()).nearestOnClientThread();
            if (freshObject != null) {
                object = freshObject;
            }

            if (itemId == -1)
                object.click(chooseCorrectObjectOption(step, object));
            else {
                // "Use item on object": select the item first, then wait until it is actually on the
                // cursor before clicking the object. Otherwise object.click() races the selection, sees
                // no widget selected, and performs the object's default action (e.g. Inspect) instead of
                // "Use <item> -> object" — leaving the item unused.
                Rs2Inventory.use(itemId);
                sleepUntil(() -> Microbot.getClient().isWidgetSelected(), 2000);
                object.click("");
            }

            sleepUntil(() -> Rs2Player.isMoving() || Rs2Player.isAnimating(), 2000);
            sleep(100);
            sleepUntil(() -> !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 5000);
            objectsHandeled.add(object.getHash());
        } else if (object != null) {
            Rs2Walker.walkTo(object.getWorldLocation(), 1); // full walker; adjacent-click gate handles arrival
            return false;
        }

        return true;
    }

    private boolean applyDigStep(DigStep step) {
        if (!Rs2Walker.walkTo(step.getDefinedPoint().getWorldPoint()))
            return false;
        else if (!Rs2Player.getWorldLocation().equals(step.getDefinedPoint().getWorldPoint()))
            Rs2Walker.walkFastCanvas(step.getDefinedPoint().getWorldPoint());
        else {
            Rs2Inventory.interact(ItemID.SPADE, "Dig");
            return true;
        }

        return false;
    }

    private boolean applyPuzzleStep(PuzzleStep step) {
        if (!step.getHighlightedButtons().isEmpty()) {
            var widgetDetails = step.getHighlightedButtons().stream().filter(x -> Rs2Widget.isWidgetVisible(x.groupID, x.childID)).findFirst().orElse(null);
            if (widgetDetails != null) {
                Rs2Widget.clickWidget(widgetDetails.groupID, widgetDetails.childID);
                return true;
            }
        }

        return false;
    }

    private boolean objectMatchesIds(Rs2TileObjectModel object, Set<Integer> ids) {
        if (object == null) {
            return false;
        }
        if (ids.contains(object.getId())) {
            return true;
        }
        // The configured id may be the multiloc's impostor rather than the scene's base id.
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            ObjectComposition comp = Microbot.getClient().getObjectDefinition(object.getId());
            if (comp != null && comp.getImpostorIds() != null && comp.getImpostor() != null) {
                return ids.contains(comp.getImpostor().getId());
            }
            return false;
        }).orElse(false);
    }

    private String chooseCorrectObjectOption(QuestStep step, Rs2TileObjectModel object) {
        ObjectComposition objComp = Microbot.getClientThread().runOnClientThreadOptional(() ->
                Microbot.getClient().getObjectDefinition(object.getId())).orElse(null);

        if (objComp == null)
            return "";

        String[] actions;
        if (objComp.getImpostorIds() != null) {
            actions = objComp.getImpostor().getActions();
        } else {
            actions = objComp.getActions();
        }

        for (var action : actions) {
            if (action != null && step.getText().stream().anyMatch(x -> x.toLowerCase().contains(action.toLowerCase())))
                return action;
        }

        // Fallback: first non-empty action (the object's default left-click).
        for (var action : actions) {
            if (action != null && !action.isEmpty())
                return action;
        }

        return "";
    }

    private String chooseCorrectNPCOption(QuestStep step, Rs2NpcModel npc) {
        var npcComp = Microbot.getClientThread().runOnClientThreadOptional(() -> Microbot.getClient().getNpcDefinition(npc.getId()))
                .orElse(null);

        if (npcComp == null)
            return "Talk-to";

        var actions = npcComp.getActions();

        for (var action : actions) {
            if (action != null && step.getText().stream().anyMatch(x -> x.toLowerCase().contains(action.toLowerCase())))
                return action;
        }

        // Fallback: prefer Talk-to if the NPC has it, otherwise the first non-empty action.
        String fallback = null;
        for (var action : actions) {
            if (action == null || action.isEmpty()) continue;
            if ("Talk-to".equalsIgnoreCase(action)) return action;
            if (fallback == null) fallback = action;
        }
        return fallback != null ? fallback : "Talk-to";
    }

	private String chooseCorrectItemOption(QuestStep step, int itemId) {
		var actions = Rs2Inventory.get(itemId).getInventoryActions();

		for (var action : actions) {
			if (action != null && step.getText().stream().anyMatch(x -> x.toLowerCase().contains(action.toLowerCase())))
				return action;
		}

		// Fallback: first non-empty inventory action (the item's default left-click).
		for (var action : actions) {
			if (action != null && !action.isEmpty())
				return action;
		}

		return "use";
	}

	private boolean hasLineOfSightToObject(Rs2TileObjectModel object) {
		if (object == null || object.getWorldLocation() == null || Microbot.getClient().getLocalPlayer() == null) {
			return false;
		}

		WorldArea objectArea = object.getWorldLocation().toWorldArea();
		WorldArea playerArea = Rs2Player.getWorldLocation().toWorldArea();

		return Microbot.getClient().getTopLevelWorldView() != null
				&& playerArea.hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), objectArea);
	}

	private boolean hasLineOfSightFrom(WorldPoint point, Rs2TileObjectModel object) {
		if (point == null || object == null || object.getWorldLocation() == null) {
			return false;
		}

		WorldArea fromArea = point.toWorldArea();
		WorldArea targetArea = object.getWorldLocation().toWorldArea();

		return Microbot.getClient().getTopLevelWorldView() != null
				&& fromArea.hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), targetArea);
	}

    private boolean applyDetailedQuestStep(DetailedQuestStep conditionalStep) {
        if (conditionalStep instanceof NpcStep) return false;

        if (conditionalStep.getIconItemID() != -1
                && conditionalStep.getDefinedPoint().getWorldPoint() != null
                && !conditionalStep.getDefinedPoint().getWorldPoint().toWorldArea().hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), Rs2Player.getWorldLocation())) {
            if (Rs2Tile.areSurroundingTilesWalkable(conditionalStep.getDefinedPoint().getWorldPoint(), 1, 1)) {
                WorldPoint nearestUnreachableWalkableTile = Rs2Tile.getNearestWalkableTileWithLineOfSight(conditionalStep.getDefinedPoint().getWorldPoint());
                if (nearestUnreachableWalkableTile != null) {
                    return Rs2Walker.walkTo(nearestUnreachableWalkableTile, 0);
                }
            }
        }

        boolean usingItems = false;
        for (Requirement requirement : conditionalStep.getRequirements()) {
            if (requirement instanceof ItemRequirement) {
                ItemRequirement itemRequirement = (ItemRequirement) requirement;

				if (itemRequirement.shouldHighlightInInventory(Microbot.getClient())
						&& Rs2Inventory.contains(itemRequirement.getAllIds().stream().mapToInt(i -> i).toArray())) {
					var itemId = itemRequirement.getAllIds().stream().filter(Rs2Inventory::contains).findFirst().orElse(-1);
					Rs2Inventory.interact(itemId, chooseCorrectItemOption(conditionalStep, itemId));
					sleep(100, 200);
					usingItems = true;
					continue;
				}

				if (!hasItemRequirementOnPlayer(itemRequirement)) {
					return attemptToAcquireRequirementItem(conditionalStep, itemRequirement);
				}
			}
		}

        if (!usingItems && conditionalStep.getDefinedPoint().getWorldPoint() != null && !Rs2Walker.walkTo(conditionalStep.getDefinedPoint().getWorldPoint()))
            return true;

		if (conditionalStep.getIconItemID() != -1 && conditionalStep.getDefinedPoint().getWorldPoint() != null
				&& conditionalStep.getDefinedPoint().getWorldPoint().toWorldArea().hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), Rs2Player.getWorldLocation())) {
			if (conditionalStep.getQuestHelper().getQuest() == QuestHelperQuest.ZOGRE_FLESH_EATERS) {
				if (conditionalStep.getIconItemID() == 4836) { // strange potion
					lootGroundItem(ItemID.CUP_OF_TEA_4838, 20);
				}
			}
		}

		return usingItems;
	}

    private boolean applyWidgetStep(WidgetStep step) {
        var widgetDetails = step.getWidgetDetails().get(0);
        var widget = Microbot.getClient().getWidget(widgetDetails.groupID, widgetDetails.childID);

        if (widgetDetails.childChildID != -1) {
            var tmpWidget = widget.getChild(widgetDetails.childChildID);

            if (tmpWidget != null)
                widget = tmpWidget;
        }

        return Rs2Widget.clickWidget(widget.getId());
    }

    protected QuestHelperPlugin getQuestHelperPlugin() {
        return (QuestHelperPlugin) Microbot.getPluginManager().getPlugins().stream().filter(x -> x instanceof QuestHelperPlugin).findFirst().orElse(null);
    }

    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getMessage().equalsIgnoreCase("I can't reach that!"))
            unreachableTarget = true;
    }
}
