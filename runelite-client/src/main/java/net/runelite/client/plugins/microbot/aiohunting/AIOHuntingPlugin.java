package net.runelite.client.plugins.microbot.aiohunting;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiohunting.enums.HuntingMethod;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = PluginDescriptor.Default + "AIO Hunting",
	description = "All-in-one Hunter progression with routing, trap safety and banking",
	tags = {"hunter", "hunting", "skilling", "aio", "progression"},
	authors = {"Infuse"},
	version = AIOHuntingPlugin.VERSION,
	minClientVersion = "1.9.8"
)
@Slf4j
public class AIOHuntingPlugin extends Plugin
{
	public static final String VERSION = "1.9.23";

	private static final long EXPECTED_TRAP_WINDOW_MS = 5000L;

	@Inject
	@Getter
	private AIOHuntingConfig config;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private AIOHuntingOverlay overlay;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private ConfigManager configManager;

	@Getter
	private volatile AIOHuntingScript script;
	private AIOHuntingPanel panel;
	private NavigationButton navigationButton;
	private ExecutorService controlExecutor;

	private final Set<WorldPoint> ownedTraps = ConcurrentHashMap.newKeySet();
	private volatile WorldPoint expectedTrapLocation;
	private volatile long expectedTrapUntil;
	private final Map<HuntingMethod, Integer> catches = new EnumMap<>(HuntingMethod.class);
	private int startHunterXp;
	private int lastHunterXp;
	private long startedAt;

	@Provides
	AIOHuntingConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(AIOHuntingConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		controlExecutor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "AIOHunting-ui");
			thread.setDaemon(true);
			return thread;
		});

		panel = injector.getInstance(AIOHuntingPanel.class);
		navigationButton = NavigationButton.builder()
			.tooltip("AIO Hunting")
			.icon(buildIcon())
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		resetSession();
		refreshPanel();
	}

	@Override
	protected void shutDown()
	{
		AIOHuntingScript runningScript = script;
		if (runningScript != null)
		{
			runningScript.shutdown();
		}
		overlayManager.remove(overlay);
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		if (controlExecutor != null)
		{
			controlExecutor.shutdownNow();
			controlExecutor = null;
		}
		ownedTraps.clear();
	}

	public void startScript()
	{
		submitControl(() -> {
			if (isScriptRunning())
			{
				return;
			}
			resetSession();
			script = injector.getInstance(AIOHuntingScript.class);
			script.run(config, this);
			log.info("AIO Hunting started.");
		});
	}

	public void stopScript()
	{
		submitControl(() -> {
			AIOHuntingScript runningScript = script;
			if (runningScript != null && runningScript.isRunning())
			{
				runningScript.shutdown();
				log.info("AIO Hunting stopped.");
			}
		});
	}

	public void togglePause()
	{
		AIOHuntingScript runningScript = script;
		if (runningScript != null && runningScript.isRunning())
		{
			runningScript.togglePause();
		}
	}

	public boolean isScriptRunning()
	{
		AIOHuntingScript runningScript = script;
		return runningScript != null && runningScript.isRunning();
	}

	public boolean isScriptPaused()
	{
		AIOHuntingScript runningScript = script;
		return runningScript != null && runningScript.isRunning() && runningScript.isPaused();
	}

	public HuntingMethod resolveDisplayMethod()
	{
		AIOHuntingScript runningScript = script;
		if (runningScript != null && runningScript.isRunning())
		{
			return runningScript.getActiveMethod();
		}
		int level = Rs2Player.getRealSkillLevel(Skill.HUNTER);
		return config.autoProgress() ? HuntingMethod.bestFor(level) : config.manualMethod();
	}

	public void expectTrapAt(WorldPoint location)
	{
		expectedTrapLocation = location;
		expectedTrapUntil = System.currentTimeMillis() + EXPECTED_TRAP_WINDOW_MS;
	}

	public boolean hasPendingTrapPlacement()
	{
		WorldPoint expected = expectedTrapLocation;
		return expected != null
			&& System.currentTimeMillis() <= expectedTrapUntil
			&& !ownsTrapNear(expected);
	}

	public void cancelExpectedTrap()
	{
		expectedTrapLocation = null;
		expectedTrapUntil = 0L;
	}

	public boolean ownsTrapNear(WorldPoint location)
	{
		if (location == null)
		{
			return false;
		}
		return ownedTraps.stream().anyMatch(owned -> owned.getPlane() == location.getPlane()
			&& owned.distanceTo(location) <= 1);
	}

	/** Claim an already-present trap (e.g. one the player set manually before starting) as ours. */
	public void adoptTrapAt(WorldPoint location)
	{
		if (location != null)
		{
			ownedTraps.add(location);
		}
	}

	public void clearOwnedTraps()
	{
		ownedTraps.clear();
		expectedTrapLocation = null;
		expectedTrapUntil = 0L;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (!isScriptRunning() || expectedTrapLocation == null
			|| System.currentTimeMillis() > expectedTrapUntil
			|| !TrapData.isHunterTrapObjectId(event.getGameObject().getId()))
		{
			return;
		}
		WorldPoint spawned = event.getGameObject().getWorldLocation();
		if (spawned != null && spawned.getPlane() == expectedTrapLocation.getPlane()
			&& spawned.distanceTo(expectedTrapLocation) <= 2)
		{
			ownedTraps.add(spawned);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.HUNTER)
		{
			return;
		}
		int newXp = event.getXp();
		if (isScriptRunning() && newXp > lastHunterXp)
		{
			HuntingMethod method = script.getActiveMethod();
			synchronized (catches)
			{
				catches.merge(method, 1, Integer::sum);
			}
		}
		lastHunterXp = newXp;
		refreshPanel();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (AIOHuntingConfig.GROUP.equals(event.getGroup()))
		{
			refreshPanel();
		}
	}

	public Map<HuntingMethod, Integer> getCatches()
	{
		synchronized (catches)
		{
			return Collections.unmodifiableMap(new EnumMap<>(catches));
		}
	}

	public int getTotalCatches()
	{
		synchronized (catches)
		{
			return catches.values().stream().mapToInt(Integer::intValue).sum();
		}
	}

	public int getXpGained()
	{
		return Math.max(0,
			Microbot.getClient().getSkillExperience(Skill.HUNTER) - startHunterXp);
	}

	public long getRuntimeMillis()
	{
		return startedAt == 0 ? 0 : System.currentTimeMillis() - startedAt;
	}

	public ConfigManager getConfigManager()
	{
		return configManager;
	}

	private void resetSession()
	{
		int xp = Microbot.getClientThread().runOnClientThreadOptional(
			() -> Microbot.getClient().getSkillExperience(Skill.HUNTER)).orElse(0);
		startHunterXp = xp;
		lastHunterXp = xp;
		startedAt = System.currentTimeMillis();
		ownedTraps.clear();
		expectedTrapLocation = null;
		expectedTrapUntil = 0L;
		synchronized (catches)
		{
			catches.clear();
		}
	}

	private void refreshPanel()
	{
		if (panel == null)
		{
			return;
		}
		int level = Rs2Player.getRealSkillLevel(Skill.HUNTER);
		panel.update(level, resolveDisplayMethod(), config.autoProgress());
	}

	private void submitControl(Runnable action)
	{
		ExecutorService executor = controlExecutor;
		if (executor == null || executor.isShutdown())
		{
			return;
		}
		executor.submit(() -> {
			try
			{
				action.run();
			}
			catch (Exception ex)
			{
				log.error("AIO Hunting control action failed", ex);
			}
		});
	}

	private static BufferedImage buildIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(205, 145, 55));
			graphics.drawOval(2, 2, 11, 11);
			graphics.drawLine(4, 12, 14, 15);
			graphics.setColor(new Color(95, 170, 80));
			graphics.fillOval(6, 6, 4, 4);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}
}
