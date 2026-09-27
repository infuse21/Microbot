package net.runelite.client.plugins.microbot.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.events.WelcomeScreenEvent;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class WelcomeScreenEventTest
{
    @Test
    public void clientThreadCallerCannotClickOrWait()
    {
        ClientThread bridge = mock(ClientThread.class);
        when(bridge.isClientThread()).thenReturn(true);
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
             MockedStatic<Rs2Widget> widgets = mockStatic(Rs2Widget.class);
             MockedStatic<Global> global = mockStatic(Global.class))
        {
            microbot.when(Microbot::getClientThread).thenReturn(bridge);
            assertFalse(new WelcomeScreenEvent().execute());
            widgets.verifyNoInteractions();
            global.verifyNoInteractions();
        }
    }

    @Test
    public void missingPlayWidgetCannotClickOrWait()
    {
        ClientThread bridge = mock(ClientThread.class);
        Client client = mock(Client.class);
        when(bridge.invoke(any(Supplier.class))).thenAnswer(invocation ->
            ((Supplier<?>) invocation.getArgument(0)).get());
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
             MockedStatic<Rs2Widget> widgets = mockStatic(Rs2Widget.class);
             MockedStatic<Global> global = mockStatic(Global.class))
        {
            microbot.when(Microbot::getClient).thenReturn(client);
            microbot.when(Microbot::getClientThread).thenReturn(bridge);
            assertFalse(new WelcomeScreenEvent().execute());
            widgets.verifyNoInteractions();
            global.verifyNoInteractions();
        }
    }

    @Test
    public void clickRunsOutsideClientCallbackAndWaitsForDismissal()
    {
        exerciseClick(true);
    }

    @Test
    public void rejectedClickDoesNotWaitOrReportSuccess()
    {
        exerciseClick(false);
    }

    private void exerciseClick(boolean accepted)
    {
        Client client = mock(Client.class);
        ClientThread bridge = mock(ClientThread.class);
        Widget play = mock(Widget.class);
        AtomicBoolean inClientCallback = new AtomicBoolean();
        AtomicBoolean visible = new AtomicBoolean(true);
        when(bridge.invoke(any(Supplier.class))).thenAnswer(invocation -> {
            inClientCallback.set(true);
            try {
                return ((Supplier<?>) invocation.getArgument(0)).get();
            } finally {
                inClientCallback.set(false);
            }
        });
        when(client.getWidget(InterfaceID.WelcomeScreen.PLAY)).thenAnswer(invocation -> {
            assertTrue(inClientCallback.get());
            return play;
        });
        when(play.isHidden()).thenAnswer(invocation -> {
            assertTrue(inClientCallback.get());
            return false;
        });
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
             MockedStatic<Rs2Widget> widgets = mockStatic(Rs2Widget.class);
             MockedStatic<Global> global = mockStatic(Global.class))
        {
            microbot.when(Microbot::getClient).thenReturn(client);
            microbot.when(Microbot::getClientThread).thenReturn(bridge);
            widgets.when(() -> Rs2Widget.isWidgetVisible(InterfaceID.WelcomeScreen.PLAY))
                .thenAnswer(invocation -> visible.get());
            widgets.when(() -> Rs2Widget.clickWidget(play)).thenAnswer(invocation -> {
                assertFalse("Mouse input must run outside the client callback", inClientCallback.get());
                visible.set(!accepted);
                return accepted;
            });
            assertTrue(new WelcomeScreenEvent().execute() == accepted);
            widgets.verify(() -> Rs2Widget.clickWidget(play));
            if (!accepted)
            {
                global.verifyNoInteractions();
            }
            else
            {
                global.verify(() -> Global.sleepUntil(any(BooleanSupplier.class)));
            }
        }
    }
}
