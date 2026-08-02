package net.runelite.client.plugins.microbot.aiofishing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

/**
 * The tackle box: a 35-slot container for fishing gear.
 *
 * <h2>Why this exists at all</h2>
 * Carrying the box does <em>not</em> satisfy a tool requirement. Per the wiki, "it is not
 * possible to use the items whilst they are in the tackle box... players must still have a
 * fishing rod and Sandworms in their inventory". The plugin previously treated a carried box
 * as proof of ownership and never opened it, which meant a boxed rod looked like readiness
 * but caught nothing. Its actual value is holding tools across stage switches so gearing can
 * take them out instead of making a bank trip - which is what this class does.
 *
 * <h2>Interface shape</h2>
 * Confirmed against a live client through the agent server rather than guessed:
 * <ul>
 *   <li>The inventory item offers <em>View</em> to open, alongside Fill / Empty.</li>
 *   <li>Contents live in {@link InterfaceID.TackleBoxMain#ITEMS} as dynamic children;
 *       each carries the stored item's id and name.</li>
 *   <li>Empty slots are not absent - they are present holding
 *       {@link ItemID#BLANKOBJECT}, so they must be filtered out rather than counted.</li>
 *   <li>A stored item's actions are Withdraw-1 / -5 / -X / -All, so a plain left click
 *       withdraws one, and the quantity buttons change what a click does.</li>
 *   <li>The widget tree is <b>not</b> populated while the box is shut, so the contents can
 *       only be read with it open.</li>
 * </ul>
 */
@Slf4j
final class TackleBox {

    /** Placeholder item that fills every unused slot. */
    private static final int EMPTY_SLOT = ItemID.BLANKOBJECT;
    private static final int OPEN_TIMEOUT_MS = 3000;
    private static final int WITHDRAW_TIMEOUT_MS = 2000;
    /** Short wait on the close click before falling back to escape. */
    private static final int CLOSE_CLICK_TIMEOUT_MS = 1200;

    private TackleBox() {
    }

    /** Whether a tackle box is in the inventory at all. */
    static boolean isCarried() {
        return Rs2Inventory.hasItem(ItemID.TACKLE_BOX);
    }

    static boolean isOpen() {
        return Rs2Widget.getWidget(InterfaceID.TackleBoxMain.ITEMS) != null;
    }

    /** Open the box. No-op when it is already open. */
    static boolean open() {
        if (isOpen()) {
            return true;
        }
        if (!isCarried()) {
            return false;
        }
        if (!Rs2Inventory.interact(ItemID.TACKLE_BOX, "View")) {
            return false;
        }
        return sleepUntil(TackleBox::isOpen, OPEN_TIMEOUT_MS);
    }

    /**
     * Shut the box by clicking the title-bar X, falling back to escape.
     *
     * <p>The X is a child of the frame carrying a single "Close" action, and it is found by
     * that action rather than by index so a layout change cannot silently make this click
     * the wrong sprite. It is <em>not</em> the {@code TackleBoxMain.X} constant - that one
     * sits beside {@code _1}, {@code _5} and {@code ALL} and is the Withdraw-X quantity
     * button.</p>
     *
     * <p>Escape is only the fallback because it depends on the player having "Esc closes
     * interfaces" switched on, which is a setting they can turn off.</p>
     */
    static void close() {
        if (!isOpen()) {
            return;
        }
        Widget closeButton = findCloseButton();
        if (closeButton != null) {
            Rs2Widget.clickWidget(closeButton);
            if (sleepUntil(() -> !isOpen(), CLOSE_CLICK_TIMEOUT_MS)) {
                return;
            }
            // A click that lands on nothing is silent, so escape is tried rather than
            // assumed unnecessary - leaving the box open would cover the fishing spots.
            log.debug("Tackle box close button did not take; falling back to escape.");
        }
        Rs2Keyboard.keyPress(KeyEvent.VK_ESCAPE);
        sleepUntil(() -> !isOpen(), OPEN_TIMEOUT_MS);
    }

    /** The frame's child whose only action is "Close". */
    private static Widget findCloseButton() {
        Widget frame = Rs2Widget.getWidget(InterfaceID.TackleBoxMain.FRAME);
        if (frame == null) {
            return null;
        }
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            for (Widget[] group : new Widget[][]{
                    frame.getChildren(), frame.getStaticChildren(), frame.getDynamicChildren()}) {
                if (group == null) {
                    continue;
                }
                for (Widget child : group) {
                    if (child == null || child.getActions() == null) {
                        continue;
                    }
                    for (String action : child.getActions()) {
                        if ("Close".equalsIgnoreCase(action)) {
                            return child;
                        }
                    }
                }
            }
            return null;
        }).orElse(null);
    }

    /**
     * Names of the items stored in the box. Requires the box to be open - the widget tree
     * is empty while it is shut, which is why callers open first and read second.
     */
    static List<String> contents() {
        List<String> names = new ArrayList<>();
        for (Widget slot : slots()) {
            if (slot.getItemId() != EMPTY_SLOT && slot.getName() != null
                    && !slot.getName().isEmpty()) {
                names.add(plainName(slot));
            }
        }
        return names;
    }

    /**
     * Take one of a named item out of the box.
     *
     * <p>A plain click is Withdraw-1, the item's default action, so no menu handling is
     * needed. Matching is by substring on the widget name to stay consistent with how the
     * rest of the plugin names tools ("Barbarian rod" covering the pearl variants).</p>
     *
     * @return true once the item is actually in the inventory
     */
    static boolean withdraw(String itemName) {
        if (itemName == null || itemName.isEmpty() || !open()) {
            return false;
        }
        String wanted = itemName.toLowerCase();
        for (Widget slot : slots()) {
            if (slot.getItemId() == EMPTY_SLOT || slot.getName() == null) {
                continue;
            }
            String name = plainName(slot).toLowerCase();
            if (!name.contains(wanted)) {
                continue;
            }
            Rs2Widget.clickWidget(slot);
            boolean arrived = sleepUntil(() -> Rs2Inventory.hasItem(itemName), WITHDRAW_TIMEOUT_MS);
            if (arrived) {
                log.info("Withdrew '{}' from the tackle box.", itemName);
            }
            return arrived;
        }
        return false;
    }

    /** Widget names can carry colour tags; the plugin matches on plain item names. */
    private static String plainName(Widget slot) {
        String name = slot.getName();
        return name == null ? "" : name.replaceAll("<[^>]*>", "").trim();
    }

    /** Live slot widgets, or an empty list when the box is shut. */
    private static List<Widget> slots() {
        Widget container = Rs2Widget.getWidget(InterfaceID.TackleBoxMain.ITEMS);
        if (container == null) {
            return List.of();
        }
        return Microbot.getClientThread().runOnClientThreadOptional(() -> {
            Widget[] children = container.getDynamicChildren();
            return children == null ? List.<Widget>of() : List.of(children);
        }).orElse(List.of());
    }
}
