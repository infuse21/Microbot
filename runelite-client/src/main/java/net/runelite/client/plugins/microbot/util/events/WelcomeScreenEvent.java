package net.runelite.client.plugins.microbot.util.events;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.BlockingEvent;
import net.runelite.client.plugins.microbot.BlockingEventPriority;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.function.Supplier;

import static net.runelite.client.plugins.microbot.util.Global.sleepUntil;

public class WelcomeScreenEvent implements BlockingEvent {
    
    @Override
    public boolean validate() {
        return Rs2Widget.isWidgetVisible(InterfaceID.WelcomeScreen.PLAY);
    }

    @Override
    public boolean execute() {
        if (Microbot.getClientThread().isClientThread() || Thread.currentThread().isInterrupted()) {
            return false;
        }
        // Widget mutations must run on the client thread; this event executes on Microbot-BlockingEvent.
        Widget readyPlayWidget = Microbot.getClientThread().invoke((Supplier<Widget>) () -> {
            Client client = Microbot.getClient();
            Widget updateBottomRibbon = client.getWidget(InterfaceID.WelcomeScreen.URL);
            if (updateBottomRibbon != null) {
                updateBottomRibbon.setOnClickListener((Object[]) null);
                updateBottomRibbon.setOnOpListener((Object[]) null);
            }

            Widget newsBanner = client.getWidget(InterfaceID.WelcomeScreen.BANNER);
            if (newsBanner != null) {
                newsBanner.setHidden(true);
            }

            Widget playWidget = client.getWidget(InterfaceID.WelcomeScreen.PLAY);
            boolean isPlayWidgetVisible = playWidget != null && !playWidget.isHidden();
            boolean wasNewsBannerHandled = newsBanner == null || newsBanner.isHidden();
            boolean wasUpdateRibbonHandled = updateBottomRibbon == null || updateBottomRibbon.getOnOpListener() == null;

            if (playWidget != null && isPlayWidgetVisible && wasUpdateRibbonHandled && wasNewsBannerHandled) {
                return playWidget;
            }
            return null;
        });

        // The helper revalidates the widget on the client thread, then submits mouse input here.
        if (readyPlayWidget == null) {
            // A queued event can outlive the welcome screen; do not keep re-queuing it.
            return !validate();
        }
        if (!Rs2Widget.clickWidget(readyPlayWidget)) {
            return false;
        }
        sleepUntil(() -> !validate());

        return !validate();
    }

    @Override
    public BlockingEventPriority priority() {
        return BlockingEventPriority.HIGHEST;
    }
}
