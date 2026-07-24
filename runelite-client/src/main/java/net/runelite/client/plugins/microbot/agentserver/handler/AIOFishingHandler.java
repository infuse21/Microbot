package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.aiofishing.AIOFishingPlugin;
import net.runelite.client.plugins.microbot.aiofishing.AIOFishingScript;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only runtime diagnostics for AIO Fishing's internal script. Generic plugin status
 * cannot distinguish an enabled RuneLite plugin from its independently controlled script.
 */
public class AIOFishingHandler extends AgentHandler {

    private static final String PATH = "/aio-fishing";

    public AIOFishingHandler(Gson gson) {
        super(gson);
    }

    @Override
    public String getPath() {
        return PATH;
    }

    @Override
    protected void handleRequest(HttpExchange exchange) throws IOException {
        try {
            requireGet(exchange);
        } catch (HttpMethodException e) {
            sendJson(exchange, 405, errorResponse(e.getMessage()));
            return;
        }

        Object candidate = Microbot.getPlugin(AIOFishingPlugin.class.getName());
        if (!(candidate instanceof AIOFishingPlugin)) {
            sendJson(exchange, 404, errorResponse("AIO Fishing plugin is not available"));
            return;
        }

        AIOFishingPlugin plugin = (AIOFishingPlugin) candidate;
        AIOFishingScript script = plugin.getScript();
        PluginManager pluginManager = Microbot.getPluginManager();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", AIOFishingPlugin.version);
        result.put("pluginActive", pluginManager != null && pluginManager.isActive(plugin));
        result.put("scriptRunning", script.isRunning());
        result.put("futureDone", script.isMainFutureDone());
        result.put("futureCancelled", script.isMainFutureCancelled());
        result.put("futureFailure", script.getMainFutureFailure());
        result.put("lastFatalError", script.getLastFatalError());
        result.put("state", script.getState().name());
        result.put("paused", script.isPaused());
        result.put("activeStage", script.getActiveStage().name());
        result.put("debugMode", plugin.getConfig().debugMode().name());
        result.put("activeDebugMode", script.getActiveDebugMode().name());
        result.put("debugBlockReason", script.getDebugBlockReason());
        result.put("stopReason", script.getStopReason());
        result.put("pendingSaleItem", script.getPendingSaleItem());
        result.put("lastSaleDiagnostic", script.getLastSaleScanDiagnostic());
        result.put("heartbeat",
                ScriptHeartbeatRegistry.getHealth(AIOFishingScript.class.getName()));
        sendJson(exchange, 200, result);
    }
}
