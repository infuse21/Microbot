package net.runelite.client.plugins.microbot.mining.motherloadmine;

import com.google.inject.Provides;
import java.util.ArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.ObjectID;
import net.runelite.api.WallObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.mining.motherloadmine.enums.MLMStatus;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.List;
import java.awt.*;

@Slf4j
@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "MotherlodeMine",
        description = "A bot that mines paydirt in the motherlode mine",
        tags = {"paydirt", "mine", "motherlode", "mlm"},
        enabledByDefault = false
)
public class MotherloadMinePlugin extends Plugin {
    @Inject
    private MotherloadMineConfig config;
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MotherloadMineOverlay motherloadMineOverlay;
    @Inject
    private MotherloadMineScript motherloadMineScript;

	@Getter
	private List<WorldPoint> blacklistedCrates = new ArrayList<>();

    @Provides
    MotherloadMineConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MotherloadMineConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        overlayManager.add(motherloadMineOverlay);
        motherloadMineScript.run();
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        WallObject wallObject = event.getWallObject();
        try {
            if (wallObject == null || MotherloadMineScript.oreVein == null)
                return;
            if (MotherloadMineScript.status == MLMStatus.MINING && (wallObject.getId() == ObjectID.DEPLETED_VEIN_26665 || wallObject.getId() == ObjectID.DEPLETED_VEIN_26666 || wallObject.getId() == ObjectID.DEPLETED_VEIN_26667 || wallObject.getId() == ObjectID.DEPLETED_VEIN_26668)) {
                if (wallObject.getWorldLocation().equals(MotherloadMineScript.oreVein.getWorldLocation())) {
                    MotherloadMineScript.oreVein = null;
                }
            }
        } catch (Exception e) {
            log.trace("Error while processing wall object event: {} - ", e.getMessage(), e);
        }

    }

    protected void shutDown() {
        motherloadMineScript.shutdown();
        overlayManager.remove(motherloadMineOverlay);
		blacklistedCrates.clear();
    }

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGIN_SCREEN) {
			blacklistedCrates.clear();
		}
	}
}
