package net.runelite.client.plugins.microbot.shortestpath;

import net.runelite.client.plugins.microbot.util.walker.navigation.NavigationSnapshot;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.runenergy.RunEnergyPlugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

public class ETAOverlayPanel extends OverlayPanel {
    
    private final ShortestPathPlugin plugin;

    @Inject
    ETAOverlayPanel(ShortestPathPlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.CANVAS_TOP_RIGHT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));
            panelComponent.setPreferredSize(new Dimension(160, 100));

            NavigationSnapshot snapshot = plugin.getNavigationSnapshot();
            RoutePlan routePlan = plugin.getRoutePlan();
            if (snapshot != null && routePlan != null) {
                int progressIndex = Math.max(0, Math.min(snapshot.getRawProgressIndex(),
                        routePlan.getRawPath().size()));
                int remainingPathLength = routePlan.getRawPath().size() - progressIndex;

                String remainingTime = RunEnergyPlugin.calculateTravelTime(remainingPathLength, plugin.getConfig().showInSeconds());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Est. Time till Arrival:")
                        .right(remainingTime)
                        .build());
            } else {
                if (!panelComponent.getChildren().isEmpty()) {
                    panelComponent.getChildren().clear();
                }
            }
        } catch (Exception ex) {
            System.out.println("Error in render: " + ex.getMessage());
        }
        return super.render(graphics);
    }

}
