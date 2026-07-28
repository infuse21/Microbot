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
    private long lastObjectDiagLog = 0;
    /** Tracks enable→disable transitions so the master pause cleans up exactly once. */
    private boolean wasEnabled = false;
    /** Step-scoped memory of requirements the bank turned out not to stock (prevents bank-trip loops). */
    private QuestStep bankAttemptStep = null;
    private final Set<Integer> bankWithdrawExhausted = new HashSet<>();
    /** Quest whose supplies have already been gathered up front, and what couldn't be sourced. */
    private Integer upfrontGatherQuestId = null;
    private final Set<Integer> upfrontGatherExhausted = new HashSet<>();
    /** Since when the visible dialogue-options widget has had no readable option text (0 = n/a). */
    private long emptyOptionsSinceMs = 0;
    /** Rotates which option gets picked when a populated menu matches no quest dialog step. */
    private int unmatchedOptionRotation = 0;
    /** Rotates which candidate object a highlighted item gets used on at a detailed step's spot. */
    private int detailedUseRotation = 0;
    private QuestStep lastDetailedRotationStep = null;

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
                if (!config.startStopQuestHelper()) {
                    // Master pause: cancel any in-flight walk and drop transient state so nothing keeps
                    // acting after the toggle. Quest progress itself is derived from the quest graph, so
                    // re-enabling resumes exactly where it left off.
                    if (wasEnabled) {
                        wasEnabled = false;
                        Rs2Walker.clearWalkingRoute("quest-helper:disabled");
                        dialogueStartedStep = null;
                        dialogueSpaceStuckStep = null;
                        dialogueCooldownEndsAt = 0;
                        Microbot.status = "Quest helper paused";
                    }
                    return;
                }
                wasEnabled = true;
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (getQuestHelperPlugin().getSelectedQuest() == null) return;
                if (getQuestHelperPlugin().getSelectedQuest().getCurrentStep() == null) return;

                if (Rs2Player.isAnimating())
                    Rs2Player.waitForAnimation(1200); // bounded: waitForAnimation() has an unbounded inner sleepUntil

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
                                        || !dialogueChoiceMatches(dialogChoice.getText(), choice.getChoice()))
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
                if (!config.startStopQuestHelper()) return; // re-check: the toggle may have flipped mid-tick

                // Buy the quest's shopping list before starting it, so a full-auto run isn't interrupted
                // by a shopping trip at every step. No-op once gathered, mid-quest, or when disabled.
                if (shouldObtainMissingItems()
                        && gatherQuestItemsUpfront(getQuestHelperPlugin().getSelectedQuest())) {
                    return;
                }

                if (questLogic != null) {
                    if (!questLogic.executeCustomLogic()) {
                        return;
                    }
                }

                if (getQuestHelperPlugin().getSelectedQuest() != null && !Microbot.getClientThread().runOnClientThreadOptional(() ->
                        getQuestHelperPlugin().getSelectedQuest().isCompleted()).orElse(false)) {
                    if (Rs2Widget.isWidgetVisible(ComponentID.DIALOG_OPTION_OPTIONS) && getQuestHelperPlugin().getSelectedQuest().getQuest().getId() != Quest.COOKS_ASSISTANT.getId() && !Rs2Bank.isOpen()) {
                        // The options widget can be visible with its children still EMPTY (observed at
                        // Drezel: options:[] for over a second). Dismissing then throws away a real
                        // conversation and loops open->dismiss->re-talk forever. Wait while unpopulated;
                        // if it stays unreadable for several seconds, press option 1 as a last resort —
                        // never dismiss a live options menu.
                        if (!Rs2Dialogue.hasSelectAnOption()) {
                            if (emptyOptionsSinceMs == 0) {
                                emptyOptionsSinceMs = System.currentTimeMillis();
                            }
                            if (System.currentTimeMillis() - emptyOptionsSinceMs > 4000) {
                                Microbot.log("[QuestHelper] options menu unreadable for >4s — pressing option 1", Level.WARN);
                                Rs2Dialogue.keyPressForDialogueOption(1);
                                sleep(600, 900);
                                emptyOptionsSinceMs = 0;
                            }
                            return;
                        }
                        emptyOptionsSinceMs = 0;

                        boolean hasOption = Rs2Dialogue.handleQuestOptionDialogueSelection();
                        if (!hasOption) {
                            if (Rs2Dialogue.acceptQuestStartDialogue()) {
                                return;
                            }
                            // A populated menu with no matching quest option means the quest data's
                            // dialog texts are stale (options get reworded). Dismissing loops
                            // open->dismiss->re-talk forever; a human would just pick an option and keep
                            // the conversation moving. Rotate through the options across attempts so a
                            // lore pick can't loop us on the same entry.
                            int optionCount = Math.max(1, Rs2Dialogue.getDialogueOptions().size());
                            int pick = (unmatchedOptionRotation++ % optionCount) + 1;
                            Microbot.log("[QuestHelper] no matching dialog option — picking option "
                                    + pick + "/" + optionCount + " to keep the conversation moving", Level.WARN);
                            Rs2Dialogue.keyPressForDialogueOption(pick);
                            sleep(900, 1400);
                        }
                        return;
                    }
                    emptyOptionsSinceMs = 0;

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
					// Interaction steps (Npc/Object/Dig) usually PRODUCE their required item (feathers from a
					// pile), so the acquire flow stays off them — it used to chase later, unobtainable quest
					// items with blocking walks. But when the missing item is sitting in the BANK (e.g. the
					// rune essence Drezel wants), withdrawing is exactly right, so allow it in that case.
					if (questStep instanceof DetailedQuestStep && shouldObtainMissingItems()) {
						DetailedQuestStep detailed = (DetailedQuestStep) questStep;
						boolean interactionStep = questStep instanceof NpcStep
								|| questStep instanceof ObjectStep || questStep instanceof DigStep;
						if (interactionStep) {
							// Interaction steps usually PRODUCE their item (feathers from a pile), so the
							// full acquire flow (ground loot / GE) stays off them. A bank withdrawal is
							// still right when the step needs stock we already own (Drezel's essence), so
							// try only that — and remember when the bank had nothing so it can't loop.
							if (tryWithdrawMissingFromBank(detailed)) {
								return;
							}
						} else if (handleMissingItemRequirements(detailed)) {
							return;
						}
					}

					/**
					 * This portion is needed when using item on another item in your inventory.
					 * If we do not prioritize this, the script will think we are missing items
					 */
					if (questStep instanceof DetailedQuestStep && !(questStep instanceof NpcStep || questStep instanceof ObjectStep || questStep instanceof DigStep)) {
                        boolean result = applyDetailedQuestStep((DetailedQuestStep) getQuestHelperPlugin().getSelectedQuest().getCurrentStep().getActiveStep());
                        if (result) {
                            sleepUntil(() -> Rs2Player.isInteracting() || Rs2Player.isMoving() || Rs2Player.isAnimating() || Rs2Dialogue.isInDialogue(), 500);
                            sleepUntil(() -> !Rs2Player.isInteracting() && !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 2000);
                            return;
                        }
                    }

                    if (!config.startStopQuestHelper()) return; // re-check before issuing step actions

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
                    sleepUntil(() -> !Rs2Player.isInteracting() && !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 2000);
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
					if (itemRequirement.getAllIds().stream().noneMatch(Rs2Equipment::isWearing)) {
						// Wear it if we hold one — checked directly against the inventory, NOT via
						// hasItemRequirementOnPlayer: an equipped requirement with quantity > 1 (e.g.
						// Eagles' Peak fake beak x2 — one worn, one for Nickolaus) can never show 2 in
						// the equipped container, so the old have-it-first gate skipped equipping forever.
						Integer invId = itemRequirement.getAllIds().stream()
								.filter(Rs2Inventory::contains).findFirst().orElse(null);
						if (invId != null) {
							Rs2Inventory.wear(invId);
							return true; // one equip per tick; re-evaluate next tick
						}
						notifyMissingRequirement(itemRequirement);
					}
				}
			}
		}

		return false;
	}

	/**
	 * Bank-only acquisition for interaction steps: withdraw whatever the STEP itself still needs (never
	 * quest-level extras, which is what used to send us chasing unobtainable items). Returns true when
	 * the tick was consumed. Requirements the bank turns out not to stock are remembered per step, so a
	 * step whose item can't come from a bank (feathers from a pile) makes at most one trip, then falls
	 * through to its normal logic instead of looping.
	 */
	private boolean tryWithdrawMissingFromBank(DetailedQuestStep questStep) {
		if (questStep != bankAttemptStep) {
			bankAttemptStep = questStep;
			bankWithdrawExhausted.clear();
		}

		// Consider quest-level requirements too, not just this step's: the item a step needs is often
		// listed on the quest rather than the step (Prince Ali's "Ned makes a wig from 3 balls of wool"
		// carries no requirement of its own). To keep the old protection, only take items a bank or the
		// GE could actually supply — untradeable quest-progress items (keys, disguises, quest drops) are
		// filtered out here, so we never walk off to shop for something only the quest can produce.
		List<ItemRequirement> missing = new ArrayList<>();
		for (Requirement req : collectAllItemRequirements(questStep)) {
			if (!(req instanceof ItemRequirement)) {
				continue;
			}
			ItemRequirement ir = (ItemRequirement) req;
			if (remainingQuantityNeeded(ir) <= 0 || bankWithdrawExhausted.contains(ir.getId())) {
				continue;
			}
			if (!isItemRequirementTradable(ir) && !bankSnapshotHas(ir)
					&& QuestShopCatalog.lookup(ir.getAllIds()) == null) {
				continue; // no bank stock, not tradeable, no known shop — only the quest can supply it
			}
			missing.add(ir);
		}
		return acquireFromBankThenGrandExchange(missing, bankWithdrawExhausted);
	}

	/**
	 * Buys the whole quest shopping list before the quest starts, so a full-auto run doesn't stop for a
	 * shopping trip at every step. Only runs for quests that are NOT_STARTED (mid-quest, items may
	 * already have been consumed and re-buying them would be wasteful) and only acquires what the bank
	 * or the Grand Exchange can supply — untradeable quest-progress items (keys, disguises) are
	 * obtained by playing the quest, which is exactly what the per-step path is for.
	 *
	 * @return true when the tick was consumed by gathering.
	 */
	private boolean gatherQuestItemsUpfront(QuestHelper quest) {
		if (quest == null || quest.getQuest() == null) {
			return false;
		}
		int questId = quest.getQuest().getId();
		if (upfrontGatherQuestId != null && upfrontGatherQuestId == questId) {
			return false; // already gathered (or nothing left to gather) for this quest
		}

		QuestState state = null;
		try {
			state = quest.getQuest().getState(Microbot.getClient());
		} catch (Exception ignored) {
		}
		if (state != QuestState.NOT_STARTED) {
			upfrontGatherQuestId = questId; // mid-quest: leave it to the per-step top-ups
			return false;
		}

		List<ItemRequirement> wanted = quest.getItemRequirements();
		if (wanted == null || wanted.isEmpty()) {
			upfrontGatherQuestId = questId;
			return false;
		}

		List<ItemRequirement> missing = new ArrayList<>();
		for (ItemRequirement ir : wanted) {
			if (ir == null || remainingQuantityNeeded(ir) <= 0 || upfrontGatherExhausted.contains(ir.getId())) {
				continue;
			}
			missing.add(ir);
		}
		if (missing.isEmpty()) {
			upfrontGatherQuestId = questId;
			Microbot.log("Quest helper: quest supplies ready", Level.INFO);
			return false;
		}

		Microbot.status = "Quest helper: gathering quest supplies";
		return acquireFromBankThenGrandExchange(missing, upfrontGatherExhausted);
	}

	/**
	 * Shared acquisition: withdraw what the bank stocks, buy the tradeable remainder on the Grand
	 * Exchange. Requirements neither source can supply are added to {@code exhausted} so the caller
	 * stops retrying them.
	 *
	 * @return true when the tick was consumed.
	 */
	/**
	 * Buys a requirement from a shop in {@link QuestShopCatalog}: walk to the shop, open it, buy the
	 * outstanding quantity, close. Coins are withdrawn by the bank leg that runs before this.
	 *
	 * @return true when the tick was consumed (travelling or buying).
	 */
	private boolean buyFromShop(ItemRequirement requirement) {
		QuestShopCatalog.ShopSource shop = QuestShopCatalog.lookup(requirement.getAllIds());
		int itemId = QuestShopCatalog.buyableId(requirement.getAllIds());
		if (shop == null || itemId == -1) {
			return false;
		}

		int needed = remainingQuantityNeeded(requirement);
		if (needed <= 0) {
			return false;
		}

		if (!Rs2Shop.isOpen()) {
			WorldPoint player = Rs2Player.getWorldLocation();
			if (player == null || player.distanceTo(shop.getLocation()) > 8) {
				Microbot.status = "Quest helper: walking to shop for " + requirement.getName();
				Rs2Walker.walkTo(shop.getLocation(), 6);
				return true; // still travelling; resume next tick
			}
			Microbot.status = "Buying " + requirement.getName() + " from " + shop.getNpcName();
			if (!Rs2Shop.openShop(shop.getNpcName(), shop.isExactName())) {
				Microbot.log("Quest helper: could not open shop " + shop.getNpcName()
						+ " for " + requirement.getName(), Level.WARN);
				return false;
			}
			sleepUntil(Rs2Shop::isOpen, 5_000);
		}

		if (!Rs2Shop.isOpen()) {
			return false;
		}

		if (!Rs2Shop.hasStock(itemId)) {
			Microbot.log("Quest helper: " + shop.getNpcName() + " has no " + requirement.getName() + " in stock",
					Level.WARN);
			Rs2Shop.closeShop();
			return false;
		}

		Rs2Shop.buyItemOptimally(requirement.getName(), needed);
		sleepUntil(() -> remainingQuantityNeeded(requirement) <= 0, 5_000);
		Rs2Shop.closeShop();
		sleepUntil(() -> !Rs2Shop.isOpen(), 2_000);
		return true;
	}

	/** Whether the quest's bank snapshot shows any stock of this requirement. */
	private boolean bankSnapshotHas(ItemRequirement ir) {
		Item[] bankItems = QuestContainerManager.getBankData().getItems();
		if (bankItems == null) {
			return false;
		}
		Set<Integer> ids = new HashSet<>(ir.getAllIds());
		for (Item item : bankItems) {
			if (item != null && item.getQuantity() > 0 && ids.contains(item.getId())) {
				return true;
			}
		}
		return false;
	}

	private boolean acquireFromBankThenGrandExchange(List<ItemRequirement> missing, Set<Integer> exhausted) {
		if (missing.isEmpty()) {
			return false;
		}

		if (!Rs2Bank.isOpen()) {
			Microbot.status = "Quest helper: withdrawing " + missing.get(0).getName();
			Rs2Bank.walkToBankAndUseBank();
			if (!sleepUntil(Rs2Bank::isOpen, 15_000)) {
				return true; // still travelling; try again next tick
			}
		}

		if (!Rs2Bank.setWithdrawAsItem()) {
			Rs2Bank.closeBank();
			return false;
		}

		boolean withdrewAny = false;
		List<ItemRequirement> needBuy = new ArrayList<>();
		for (ItemRequirement ir : missing) {
			final ItemRequirement req = ir;
			int needed = remainingQuantityNeeded(req);
			int withdrawId = -1;
			for (Integer id : req.getAllIds()) {
				if (id != null && id > 0 && Rs2Bank.count(id) > 0) {
					withdrawId = id;
					break;
				}
			}
			if (withdrawId == -1) {
				// Bank can't supply it — buy it instead if it's tradeable (balls of wool, etc.).
				needBuy.add(req);
				continue;
			}
			Microbot.status = "Withdrawing " + req.getName() + " x" + needed;
			Rs2Bank.withdrawX(withdrawId, needed);
			sleepUntil(() -> hasItemRequirementOnPlayer(req), 3_000);
			withdrewAny = true;
		}

		Rs2Bank.closeBank();
		sleepUntil(() -> !Rs2Bank.isOpen(), 3_000);

		if (withdrewAny) {
			return true;
		}

		// Nothing in the bank: buy the tradeable ones on the Grand Exchange (the same flow the
		// non-interaction steps use, but scoped to THIS step's requirements so we never chase
		// quest-level extras). Anything untradeable is marked exhausted so the step proceeds.
		List<ItemRequirement> buyable = new ArrayList<>();
		for (ItemRequirement req : needBuy) {
			// A shop we know about beats the GE: it's local, cheap and always in stock.
			if (QuestShopCatalog.lookup(req.getAllIds()) != null && buyFromShop(req)) {
				return true;
			}
			if (isItemRequirementTradable(req)) {
				buyable.add(req);
			} else {
				Microbot.log("Quest helper: " + req.getName() + " is not in the bank, not tradeable and not "
						+ "in the shop catalog; letting the step handle it", Level.WARN);
				exhausted.add(req.getId());
			}
		}
		if (!buyable.isEmpty()) {
			return acquireMissingTradableItems(buyable);
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
            // Non-hop cache reads: this tick can run during scene loads where a client-thread hop
            // stalls 10s and throws (see resolveStepNpcFromCache).
            List<Rs2NpcModel> fromCache = new ArrayList<>(
                    Microbot.getRs2NpcCache().query().withId(step.getNpcID()).toList());
            for (Integer altId : step.getAlternateNpcIDs()) {
                if (altId != null) {
                    fromCache.addAll(Microbot.getRs2NpcCache().query().withId(altId).toList());
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

        // Loot a kill step's reward drop before hunting the next NPC: a step like "Kill a Monk of
        // Zamorak for a golden key" carries the key as an ItemRequirement — once the NPC dies the step
        // only completes by PICKING UP the drop, which nothing else does (the acquire flow is gated off
        // NpcSteps, and the drop despawns if we just wait for a respawn to kill again).
        if (!Rs2Combat.inCombat()) {
            for (Requirement req : step.getRequirements()) {
                if (!(req instanceof ItemRequirement)) {
                    continue;
                }
                ItemRequirement ir = (ItemRequirement) req;
                if (hasItemRequirementOnPlayer(ir)) {
                    continue;
                }
                for (Integer groundId : ir.getAllIds()) {
                    if (groundId != null && groundId > 0 && lootGroundItem(groundId, 12)) {
                        Microbot.log("[QuestHelper] looting step-required drop id=" + groundId, Level.WARN);
                        sleepUntil(() -> hasItemRequirementOnPlayer(ir), 4000);
                        return true;
                    }
                }
            }
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

    /**
     * Nearest live-cache NPC matching the step's id or its alternates, or null if none is loaded.
     *
     * <p>Deliberately uses the non-client-thread query variants: this is called from inside
     * {@link Rs2Walker#walkWithStateUntil} completion suppliers while the walker lock is held, and a
     * client-thread hop there is deadlock-shaped (script holds walkerLock and waits on the client
     * thread; anything on the client thread waiting on the walker wedges the whole client) — and at
     * minimum stalls 10s during scene loads. The cache is event-driven and safe to read directly.
     */
    private Rs2NpcModel resolveStepNpcFromCache(NpcStep step) {
        Rs2NpcModel n = Microbot.getRs2NpcCache().query().withId(step.getNpcID())
                .toList().stream().findFirst().orElse(null);
        if (n != null) {
            return n;
        }
        for (Integer altId : step.getAlternateNpcIDs()) {
            if (altId == null) {
                continue;
            }
            n = Microbot.getRs2NpcCache().query().withId(altId)
                    .toList().stream().findFirst().orElse(null);
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
            final boolean instanced = Microbot.getClient().isInInstancedRegion();
            // Resolve via the live object cache by id (same query the agent server / NPC fallback use).
            // Inside an instance an object's getWorldLocation() is its TEMPLATE coord (hundreds of tiles
            // off), so any near-the-defined-point distance filter rejects it — so in instances we take the
            // matching id anywhere in the (small) scene; outside instances we still require it near the
            // defined point so we don't grab a same-id object elsewhere on the map.
            List<Rs2TileObjectModel> matches = new ArrayList<>(
                    Microbot.getRs2TileObjectCache().query().withId(step.getObjectID()).toListOnClientThread());
            for (Integer altId : step.getAlternateObjectIDs()) {
                if (altId != null) {
                    matches.addAll(Microbot.getRs2TileObjectCache().query().withId(altId).toListOnClientThread());
                }
            }
            object = matches.stream()
                    .filter(o -> instanced || (o.getWorldLocation() != null && o.getWorldLocation().distanceTo(dp) <= 3))
                    .min(Comparator.comparing(o -> instanced ? 0
                            : o.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
                    .orElse(null);

            if (object == null) {
                // Stale-id resilience: object ids drift when areas get graphical reworks (e.g. the
                // Paterdomus staircases — quest data says 16671, the live stairs are 61189). The defined
                // point still marks the right tile, so click the nearest actionable object there instead.
                Rs2TileObjectModel atDp = nearestActionableObjectAt(dp);
                if (atDp != null) {
                    Microbot.log("[QuestHelper] stale-id fallback: step object id=" + step.getObjectID()
                            + " not present; using id=" + atDp.getId() + " at " + atDp.getWorldLocation(), Level.WARN);
                    object = atDp;
                }
            }
        }

        if (System.currentTimeMillis() - lastObjectDiagLog > 1500) {
            lastObjectDiagLog = System.currentTimeMillis();
            WorldPoint diagDp = step.getDefinedPoint() != null ? step.getDefinedPoint().getWorldPoint() : null;
            Microbot.log(String.format(
                    "[QuestHelper] objectStep id=%d scan=%d found=%s objPos=%s dp=%s player=%s unreachableFlag=%s instanced=%s",
                    step.getObjectID(), stepObjects.size(), object != null,
                    object == null ? "-" : object.getWorldLocation(),
                    diagDp, Rs2Player.getWorldLocation(), unreachableTarget,
                    Microbot.getClient().isInInstancedRegion()), Level.WARN);
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
        // Skip the walk-to-object approach inside instanced regions: an object's getWorldLocation() there
        // returns its TEMPLATE coordinate (hundreds of tiles from the player's instance position), so
        // walking to it targets a non-walkable tile and loops on UNREACHABLE. In an instance the object is
        // in the loaded scene, so we fall straight through to the on-screen click gate below.
        // Walk only when more than 2 tiles from the target. Within 2, the object is click range even if
        // canReach/LOS fail (e.g. a locked door: its own tile is never reachable, and walking "closer"
        // means routing THROUGH it — the walker's door pipeline then spams Open on a door the step wants
        // an item used on instead).
        boolean approachArrived = false;
        WorldPoint objectStepDp = step.getDefinedPoint() != null ? step.getDefinedPoint().getWorldPoint() : null;
        // A target on another floor must ALWAYS be walked to (the walker takes the stairs/ladders):
        // distanceTo2D ignores plane, so a spindle one tile away on the floor above read as "already
        // there", skipped the walk, and every proximity/LOS check then correctly failed -> dead silence.
        boolean differentPlane = objectStepDp != null
                && objectStepDp.getPlane() != Rs2Player.getWorldLocation().getPlane();
        if (!Microbot.getClient().isInInstancedRegion()
                && objectStepDp != null && (differentPlane || Rs2Player.getWorldLocation().distanceTo2D(objectStepDp) > 2)
                && (object == null || !Rs2Walker.canReach(object.getWorldLocation()) || !hasLineOfSightToObject(object))) {
            WorldPoint targetTile = null;
            WorldPoint stepLocation = object == null ? objectStepDp : object.getWorldLocation();
            // When we're already near the target, restrict approach candidates to tiles that are LOCALLY
            // reachable (no door/wall crossing). Otherwise the search can pick a far-side tile (e.g.
            // behind a locked quest door): the walker's arrival check requires local reachability, so
            // that walk can never finish — its door pipeline just spams Open on the door forever.
            final Map<WorldPoint, Integer> locallyReachable =
                    Rs2Player.getWorldLocation().distanceTo(stepLocation) <= 15
                            ? Rs2Tile.getReachableTilesFromTile(Rs2Player.getWorldLocation(), 15)
                            : null;
            int radius = 0;
            while (targetTile == null) {
                if (mainScheduledFuture.isCancelled())
                    break;
                radius++;
                Rs2TileObjectModel finalObject = object;
                targetTile = Rs2Tile.getWalkableTilesAroundTile(stepLocation, radius)
                        .stream().filter(x -> hasLineOfSightFrom(x, finalObject))
                        .filter(x -> locallyReachable == null || locallyReachable.containsKey(x))
                        .sorted(Comparator.comparing(x -> x.distanceTo(Rs2Player.getWorldLocation()))).findFirst().orElse(null);

                if (radius > 10 && targetTile == null)
                    targetTile = stepLocation;
            }

            // Full walker to the approach tile (handles transports/doors on long legs), but CANCEL the
            // moment we're adjacent to the step's target. Without the completion, a step whose target IS
            // a gated door (e.g. "use the feathers on the stone door") walks onto the door tile itself:
            // the walker's door pipeline then endlessly tries to Open the locked door to route through it,
            // instead of ending the walk so we can use the item on it from the adjacent tile.
            final WorldPoint stepDp = objectStepDp;
            WalkerState approachState = Rs2Walker.walkWithStateUntil(targetTile, 3, () -> {
                WorldPoint p = Rs2Player.getWorldLocation();
                return p != null && stepDp != null && p.distanceTo(stepDp) <= 2;
            });
            // ARRIVED means we're as close as the approach can get — often INSTANTLY, with no log or
            // movement, when we already stand within acceptance of the chosen tile (e.g. 2 tiles from a
            // staircase whose own tile is canReach=false). Returning here re-ran this block forever in
            // total silence; fall through and click instead.
            if (approachState != WalkerState.ARRIVED) {
                return false;
            }
            approachArrived = true;
        }

        // Once we're standing next to a reachable object, click it — don't gate on the on-screen/LOS
        // heuristic. Full-tile objects (e.g. a searchable pile of books) block line-of-sight to their own
        // tile, and the snapshot's local/canvas location can be unresolved, so that heuristic goes all-false
        // and we fall through to walkTo() forever, nudging in place next to the target and never interacting.
        boolean adjacentAndReachable = object != null
                && Rs2Walker.canReach(object.getWorldLocation())
                && Rs2Player.getWorldLocation().distanceTo(object.getWorldLocation()) <= 2;

        // Within click range of the step's own tile: interact even if canReach/LOS say no (locked doors).
        boolean nearDefinedPoint = object != null
                && step.getDefinedPoint() != null && step.getDefinedPoint().getWorldPoint() != null
                && Rs2Player.getWorldLocation().distanceTo(step.getDefinedPoint().getWorldPoint()) <= 2;

        if ((object != null && approachArrived)
                || (object != null && Microbot.getClient().isInInstancedRegion())
                || adjacentAndReachable
                || nearDefinedPoint
                || hasLineOfSightToObject(object)
                || object != null && (Rs2Camera.isTileOnScreen(object.getLocalLocation()) || object.getCanvasLocation() != null)) {
            Rs2Walker.clearWalkingRoute("quest-helper:object-step-interact");

            // Re-resolve the object fresh from the live cache right before clicking, by the STEP's target
            // ids — not the scanned model's getId(), which for a multiloc is the base id and can miss the
            // cache entry. The step-scan model produces a menu action the game silently ignores (observed
            // twice: cave entrance and the instance tunnel), while the cache model resolved by the step id
            // is exactly what the working agent-server interact clicks.
            Rs2TileObjectModel freshObject = resolveStepObjectFromCache(step);
            if (freshObject == null) {
                freshObject = Microbot.getRs2TileObjectCache().query()
                        .withId(object.getId()).nearestOnClientThread();
            }
            if (freshObject != null) {
                object = freshObject;
            }

            // Steps can convey "use item X on this object" without an icon: via an ItemRequirement marked
            // highlight-in-inventory (e.g. Eagles' Peak "Use the feathers on the door" — no icon, just
            // goldFeatherHighlighted etc.). Without this, we'd click the door's default action ("Open")
            // instead of using the feather on it.
            // Trust the icon only if that item is actually in the inventory. Steps can carry a stale or
            // legacy icon id (observed: the feather-door step reported icon 2950 while the real golden
            // feather is 10175) — using it selects nothing and the click degrades to the default action.
            // The highlight-in-inventory requirement reflects what we really hold, so prefer it whenever
            // the icon is unset or not present.
            if (itemId == -1 || !Rs2Inventory.contains(itemId)) {
                int highlighted = firstHighlightedInventoryItemId(step);
                if (highlighted != -1) {
                    itemId = highlighted;
                } else if (itemId != -1 && !Rs2Inventory.contains(itemId)) {
                    itemId = -1; // icon item not held and nothing highlighted — plain click
                }
            }

            Microbot.log(String.format("[QuestHelper] objectClick id=%d itemId=%d reqs=%d obj=%s",
                    object.getId(), itemId, step.getRequirements().size(), object.getWorldLocation()), Level.WARN);

            if (itemId == -1)
                object.click(chooseCorrectObjectOption(step, object));
            else {
                // "Use item on object": select the item first, then wait until it is actually on the
                // cursor before clicking the object. Otherwise object.click() races the selection, sees
                // no widget selected, and performs the object's default action (e.g. Inspect) instead of
                // "Use <item> -> object" — leaving the item unused.
                boolean used = Rs2Inventory.use(itemId);
                boolean selected = sleepUntil(() -> Microbot.getClient().isWidgetSelected(), 2000);
                if (!selected) {
                    // Selection didn't land (menu race) — retry once before clicking, else the click
                    // degrades to the object's default action (e.g. "Open" on a locked door).
                    used = Rs2Inventory.use(itemId);
                    selected = sleepUntil(() -> Microbot.getClient().isWidgetSelected(), 2000);
                }
                Microbot.log(String.format("[QuestHelper] useItemOnObject item=%d used=%s selected=%s -> click %d",
                        itemId, used, selected, object.getId()), Level.WARN);
                if (!selected) {
                    return false; // don't click without a selection; retry next tick
                }
                object.click("");
            }

            sleepUntil(() -> Rs2Player.isMoving() || Rs2Player.isAnimating(), 2000);
            sleep(100);
            sleepUntil(() -> !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 2000);
            objectsHandeled.add(object.getHash());
        } else if (object != null && !Microbot.getClient().isInInstancedRegion()) {
            // Full walker, cancelling once adjacent — see the approach-walk comment above (door-target steps).
            final WorldPoint objLoc = object.getWorldLocation();
            Rs2Walker.walkWithStateUntil(objLoc, 1, () -> {
                WorldPoint p = Rs2Player.getWorldLocation();
                return p != null && p.distanceTo(objLoc) <= 1;
            });
            return false;
        }

        return true;
    }

    private boolean applyDigStep(DigStep step) {
        WorldPoint digDp = step.getDefinedPoint() != null ? step.getDefinedPoint().getWorldPoint() : null;
        if (digDp == null)
            return false;
        if (!Rs2Walker.walkTo(digDp))
            return false;
        else if (!Rs2Player.getWorldLocation().equals(digDp))
            Rs2Walker.walkFastCanvas(digDp);
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

    /**
     * Matches a live dialogue option against a quest-data dialog step. Exact/suffix match first (the
     * original behaviour), then a normalized word-prefix match so slightly-stale quest data still hits
     * after Jagex rewords an option — e.g. data "Yes." vs live "Yes, I think I've heard of it."
     * Word-prefix (not contains) so data "No." can never match inside "...I know it...".
     */
    static boolean dialogueChoiceMatches(String liveOption, String questChoice) {
        if (liveOption == null || questChoice == null || questChoice.isEmpty()) {
            return false;
        }
        if (liveOption.endsWith(questChoice)) {
            return true;
        }
        String live = normalizeDialogueText(liveOption);
        String want = normalizeDialogueText(questChoice);
        if (want.isEmpty() || live.isEmpty()) {
            return false;
        }
        return live.equals(want) || live.startsWith(want + " ");
    }

    /** Lowercase, tags/punctuation stripped, whitespace collapsed. */
    private static String normalizeDialogueText(String text) {
        return text.replaceAll("<[^>]*>", " ")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * First inventory item id from the step's highlight-in-inventory ItemRequirements, or -1. Mirrors
     * applyDetailedQuestStep's highlighted-item handling for object steps that mean "use this item on
     * the object" without setting an icon.
     */
    private int firstHighlightedInventoryItemId(DetailedQuestStep step) {
        for (Requirement requirement : step.getRequirements()) {
            if (!(requirement instanceof ItemRequirement)) {
                continue;
            }
            ItemRequirement ir = (ItemRequirement) requirement;
            if (!ir.shouldHighlightInInventory(Microbot.getClient())) {
                continue;
            }
            Integer id = ir.getAllIds().stream().filter(Rs2Inventory::contains).findFirst().orElse(null);
            if (id != null) {
                return id;
            }
        }
        return -1;
    }

    /** Nearest object within 2 tiles of {@code dp} that exposes at least one menu action, or null. */
    private Rs2TileObjectModel nearestActionableObjectAt(WorldPoint dp) {
        return actionableObjectsAt(dp, 2).stream()
                .min(Comparator.comparing(o -> o.getWorldLocation().distanceTo(dp)))
                .orElse(null);
    }

    /** All actionable objects within {@code radius} of {@code dp}, sorted by distance from it. */
    private List<Rs2TileObjectModel> actionableObjectsAt(WorldPoint dp, int radius) {
        if (dp == null) {
            return new ArrayList<>();
        }
        return Microbot.getRs2TileObjectCache().query()
                .where(o -> o.getWorldLocation() != null && o.getWorldLocation().distanceTo(dp) <= radius)
                .toList().stream()
                .filter(this::objectHasAnyAction)
                .sorted(Comparator.comparing(o -> o.getWorldLocation().distanceTo(dp)))
                .collect(Collectors.toList());
    }

    /** Whether the object's composition (impostor-aware) exposes at least one menu action. */
    private boolean objectHasAnyAction(Rs2TileObjectModel object) {
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            ObjectComposition comp = Microbot.getClient().getObjectDefinition(object.getId());
            if (comp == null) {
                return false;
            }
            if (comp.getImpostorIds() != null && comp.getImpostor() != null) {
                comp = comp.getImpostor();
            }
            String[] actions = comp.getActions();
            if (actions == null) {
                return false;
            }
            for (String a : actions) {
                if (a != null && !a.isEmpty()) {
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    /** Nearest live-cache object matching the step's id or its alternates — the proven clickable model. */
    private Rs2TileObjectModel resolveStepObjectFromCache(ObjectStep step) {
        Rs2TileObjectModel o = Microbot.getRs2TileObjectCache().query()
                .withId(step.getObjectID()).nearestOnClientThread();
        if (o != null) {
            return o;
        }
        for (Integer altId : step.getAlternateObjectIDs()) {
            if (altId == null) {
                continue;
            }
            o = Microbot.getRs2TileObjectCache().query().withId(altId).nearestOnClientThread();
            if (o != null) {
                return o;
            }
        }
        return null;
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

	private static final Set<String> DESTRUCTIVE_ITEM_ACTIONS =
			new HashSet<>(java.util.Arrays.asList("drop", "destroy", "empty", "release", "discard"));

	private String chooseCorrectItemOption(QuestStep step, int itemId) {
		var actions = Rs2Inventory.get(itemId).getInventoryActions();

		for (var action : actions) {
			if (action != null && step.getText().stream().anyMatch(x -> x.toLowerCase().contains(action.toLowerCase())))
				return action;
		}

		// Fallback: first non-destructive inventory action. NEVER blindly Drop/Destroy — quest keys'
		// only composition action is often "Drop", which discarded the Priest in Peril golden key.
		for (var action : actions) {
			if (action != null && !action.isEmpty()
					&& !DESTRUCTIVE_ITEM_ACTIONS.contains(action.toLowerCase()))
				return action;
		}

		return "Use";
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

        // Fresh step, fresh candidate rotation — the first attempt must be the in-front object.
        if (conditionalStep != lastDetailedRotationStep) {
            lastDetailedRotationStep = conditionalStep;
            detailedUseRotation = 0;
        }

        // Steps without a location (pure "use item / read item" steps, e.g. Clock Tower) have a null
        // DefinedPoint — every .getDefinedPoint().getWorldPoint() chain below NPE'd each tick, which the
        // outer catch swallowed, so the step silently never executed.
        WorldPoint detailedDp = conditionalStep.getDefinedPoint() != null
                ? conditionalStep.getDefinedPoint().getWorldPoint() : null;

        if (conditionalStep.getIconItemID() != -1
                && detailedDp != null
                && !detailedDp.toWorldArea().hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), Rs2Player.getWorldLocation())) {
            if (Rs2Tile.areSurroundingTilesWalkable(detailedDp, 1, 1)) {
                WorldPoint nearestUnreachableWalkableTile = Rs2Tile.getNearestWalkableTileWithLineOfSight(detailedDp);
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

					// A highlighted item with a defined point usually means "use this item on the thing
					// AT that spot" (e.g. golden key on the monument). Walk there first, then select the
					// item and click the object at the spot — interacting with the item alone either does
					// nothing or, worse, fell back to a destructive action.
					if (detailedDp != null) {
						if (Rs2Player.getWorldLocation().distanceTo(detailedDp) > 2) {
							Rs2Walker.walkTo(detailedDp, 2);
							return true;
						}
						// Several candidate objects can surround the spot (e.g. the seven monuments ring
						// the room, only one takes the key). The quest's defined point stands the player
						// directly IN FRONT of the correct one, so try nearest-to-player first; rotation
						// only advances on repeat attempts as a safety net.
						List<Rs2TileObjectModel> candidates = actionableObjectsAt(detailedDp, 4).stream()
								.sorted(Comparator.comparing(o -> o.getWorldLocation().distanceTo(Rs2Player.getWorldLocation())))
								.collect(Collectors.toList());
						Rs2TileObjectModel targetObj = candidates.isEmpty() ? null
								: candidates.get(detailedUseRotation++ % candidates.size());
						if (targetObj != null) {
							Rs2Inventory.use(itemId);
							if (sleepUntil(() -> Microbot.getClient().isWidgetSelected(), 2000)) {
								Microbot.log("[QuestHelper] using highlighted item " + itemId
										+ " on object id=" + targetObj.getId() + " at " + targetObj.getWorldLocation(), Level.WARN);
								targetObj.click("");
								sleepUntil(() -> Rs2Player.isMoving() || Rs2Player.isAnimating() || Rs2Dialogue.isInDialogue(), 2000);
								usingItems = true;
								continue;
							}
						}
					}

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

        if (!usingItems && detailedDp != null && !Rs2Walker.walkTo(detailedDp))
            return true;

		if (conditionalStep.getIconItemID() != -1 && detailedDp != null
				&& detailedDp.toWorldArea().hasLineOfSightTo(Microbot.getClient().getTopLevelWorldView(), Rs2Player.getWorldLocation())) {
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
