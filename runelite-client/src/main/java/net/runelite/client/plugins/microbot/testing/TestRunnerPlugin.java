package net.runelite.client.plugins.microbot.testing;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.accountselector.AutoLoginPlugin;

import javax.inject.Inject;
import javax.swing.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@PluginDescriptor(
        name = "Test Runner",
        hidden = true,
        alwaysOn = true
)
@Slf4j
public class TestRunnerPlugin extends Plugin {

    private static final String PROP_TEST_MODE = "microbot.test.mode";
    private static final String PROP_TEST_SCRIPT = "microbot.test.script";
    private static final String PROP_TEST_TIMEOUT = "microbot.test.timeout";

    @Inject
    private PluginManager pluginManager;

    private ScheduledExecutorService executor;
    private boolean testStarted = false;

    @Override
    protected void startUp() {
        if (!isTestMode()) return;

        log.info("[TestRunner] Test mode active. Target plugin: {}", getTargetPluginName());
        executor = Executors.newSingleThreadScheduledExecutor();

        enableAutoLogin();

        long timeout = getTimeout();
        executor.schedule(() -> {
            log.error("[TestRunner] Test timed out after {}ms", timeout);
            TestResult result = new TestResult(getTargetPluginName());
            result.complete("timeout");
            TestResultWriter.write(result);
            System.exit(2);
        }, timeout, TimeUnit.MILLISECONDS);
    }

    private void enableAutoLogin() {
        for (Plugin plugin : pluginManager.getPlugins()) {
            if (plugin instanceof AutoLoginPlugin) {
                if (pluginManager.isPluginActive(plugin)) {
                    log.info("[TestRunner] AutoLogin already active");
                    return;
                }

                log.info("[TestRunner] Enabling AutoLogin plugin");
                pluginManager.setPluginEnabled(plugin, true);
                SwingUtilities.invokeLater(() -> {
                    try {
                        pluginManager.startPlugin(plugin);
                        log.info("[TestRunner] AutoLogin started");
                    } catch (PluginInstantiationException e) {
                        log.error("[TestRunner] Failed to start AutoLogin", e);
                        TestResult result = new TestResult(getTargetPluginName());
                        result.addError("Failed to start AutoLogin: " + e.getMessage());
                        result.complete("login_failure");
                        TestResultWriter.write(result);
                        System.exit(4);
                    }
                });
                return;
            }
        }
        log.warn("[TestRunner] AutoLogin plugin not found — login must happen externally");
    }

    @Override
    protected void shutDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (!isTestMode()) return;
        if (testStarted) return;
        if (event.getGameState() != GameState.LOGGED_IN) return;

        testStarted = true;
        log.info("[TestRunner] Logged in. Starting target plugin in 2s...");

        executor.schedule(this::enableTargetPlugin, 2, TimeUnit.SECONDS);
    }

    private void enableTargetPlugin() {
        String targetName = getTargetPluginName();
        log.info("[TestRunner] Looking for plugin: '{}'", targetName);

        for (Plugin plugin : pluginManager.getPlugins()) {
            PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
            if (descriptor == null) continue;

            if (descriptor.name().contains(targetName)) {
                log.info("[TestRunner] Found plugin: {} ({})", descriptor.name(), plugin.getClass().getSimpleName());
                pluginManager.setPluginEnabled(plugin, true);

                SwingUtilities.invokeLater(() -> {
                    try {
                        pluginManager.startPlugin(plugin);
                        log.info("[TestRunner] Started plugin: {}", descriptor.name());
                    } catch (PluginInstantiationException e) {
                        log.error("[TestRunner] Failed to start plugin", e);
                        TestResult result = new TestResult(targetName);
                        result.addError("Failed to start plugin: " + e.getMessage());
                        result.complete("crash");
                        TestResultWriter.write(result);
                        System.exit(3);
                    }
                });
                return;
            }
        }

        log.error("[TestRunner] Plugin '{}' not found! Available plugins:", targetName);
        for (Plugin plugin : pluginManager.getPlugins()) {
            PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
            if (d != null) log.error("  - {}", d.name());
        }

        TestResult result = new TestResult(targetName);
        result.addError("Plugin not found: " + targetName);
        result.complete("crash");
        TestResultWriter.write(result);
        System.exit(3);
    }

    private static boolean isTestMode() {
        return "true".equals(System.getProperty(PROP_TEST_MODE));
    }

    private static String getTargetPluginName() {
        return System.getProperty(PROP_TEST_SCRIPT, "");
    }

    private static long getTimeout() {
        try {
            return Long.parseLong(System.getProperty(PROP_TEST_TIMEOUT, "120000"));
        } catch (NumberFormatException e) {
            return 120000;
        }
    }
}
