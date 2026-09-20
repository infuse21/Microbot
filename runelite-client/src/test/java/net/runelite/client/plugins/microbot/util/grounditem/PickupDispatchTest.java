package net.runelite.client.plugins.microbot.util.grounditem;

import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.ApiTestClient;
import java.awt.Canvas;
import java.awt.event.MouseEvent;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.menu.PendingMenuAction;
import net.runelite.client.plugins.microbot.util.mouse.VirtualMouse;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class PickupDispatchTest {
    @Test public void microbotMenuSubscriberRegisters() {
        net.runelite.client.eventbus.EventBus bus = new net.runelite.client.eventbus.EventBus();
        net.runelite.client.plugins.microbot.MicrobotPlugin plugin = new net.runelite.client.plugins.microbot.MicrobotPlugin();
        bus.register(plugin);
        bus.unregister(plugin);
    }

    private NewMenuEntry entry() {
        return new NewMenuEntry().identifier(100).param0(10).param1(20)
                .worldViewId(-1).opcode(MenuAction.GROUND_ITEM_THIRD_OPTION.getId()).option("Take");
    }

    @Test public void acknowledgedClickCleansUpItsMenu() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            Canvas canvas = mock(Canvas.class);
            when(canvas.getLocationOnScreen()).thenReturn(new java.awt.Point(0, 0));
            when(env.client.getCanvas()).thenReturn(canvas);
            NewMenuEntry entry = entry();
            doAnswer(i -> {
                MouseEvent event = i.getArgument(0);
                if (event.getID() == MouseEvent.MOUSE_MOVED) PendingMenuAction.prepared(entry);
                if (event.getID() == MouseEvent.MOUSE_CLICKED) PendingMenuAction.observe(new MenuOptionClicked(entry));
                return null;
            }).when(canvas).dispatchEvent(any(MouseEvent.class));
            assertTrue(new VirtualMouse().tryClick(new Point(1, 1), entry, () -> true));
            assertNull(Microbot.targetMenu);
        }
    }

    @Test public void unpreparedMenuDoesNotPressAndIsCleared() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            Canvas canvas = mock(Canvas.class);
            when(canvas.getLocationOnScreen()).thenReturn(new java.awt.Point(0, 0));
            when(env.client.getCanvas()).thenReturn(canvas);
            assertFalse(new VirtualMouse().tryClick(new Point(1, 1), entry(), () -> true));
            verify(canvas, never()).dispatchEvent(argThat(e -> e.getID() == MouseEvent.MOUSE_PRESSED));
            assertNull(Microbot.targetMenu);
        }
    }

    @Test public void staleTargetAndInterruptedCallerDoNotEmit() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            Canvas canvas = mock(Canvas.class);
            when(canvas.getLocationOnScreen()).thenReturn(new java.awt.Point(0, 0));
            when(env.client.getCanvas()).thenReturn(canvas);
            VirtualMouse mouse = new VirtualMouse();
            assertFalse(mouse.tryClick(new Point(1, 1), entry(), () -> false));
            Thread.currentThread().interrupt();
            try { assertFalse(mouse.tryClick(new Point(1, 1), entry(), () -> true)); }
            finally { Thread.interrupted(); }
            verifyNoInteractions(canvas);
        }
    }

    @Test public void unexpectedEmitterFailureCannotLeaveMenuArmed() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            Canvas canvas = mock(Canvas.class);
            when(canvas.getLocationOnScreen()).thenReturn(new java.awt.Point(0, 0));
            when(env.client.getCanvas()).thenReturn(canvas);
            NewMenuEntry entry = entry();
            doAnswer(i -> {
                MouseEvent event = i.getArgument(0);
                if (event.getID() == MouseEvent.MOUSE_MOVED) PendingMenuAction.prepared(entry);
                if (event.getID() == MouseEvent.MOUSE_PRESSED) throw new IllegalStateException("expected");
                return null;
            }).when(canvas).dispatchEvent(any(MouseEvent.class));
            try { new VirtualMouse().tryClick(new Point(1, 1), entry, () -> true); fail(); }
            catch (IllegalStateException expected) { }
            assertNull(Microbot.targetMenu);
        }
    }

    @Test public void acknowledgementRequiresUnconsumedMatchingViewAndTarget() {
        NewMenuEntry requested = entry();
        try (PendingMenuAction pending = new PendingMenuAction(requested)) {
            PendingMenuAction.observe(new MenuOptionClicked(entry().worldViewId(12)));
            assertFalse(pending.isAcknowledged());
            MenuOptionClicked consumed = new MenuOptionClicked(requested);
            consumed.consume();
            PendingMenuAction.observe(consumed);
            assertFalse(pending.isAcknowledged());
            PendingMenuAction.observe(new MenuOptionClicked(requested));
            assertTrue(pending.isAcknowledged());
        }
    }
}
