package net.runelite.client.plugins.microbot.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /graphics — dumps active {@link GraphicsObject}s (spotanims / tile graphics such as
 * Hunter trail footprints) with their world tile, id and lifecycle. These are transient
 * client graphics and are NOT in the tile-object cache, so /objects cannot see them.
 *
 * Query params: {@code maxDistance} (default 30, from the player), {@code id} (optional filter).
 */
public class GraphicsHandler extends AgentHandler {

	public GraphicsHandler(Gson gson) {
		super(gson);
	}

	@Override
	public String getPath() {
		return "/graphics";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		try {
			requireGet(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, String> params = parseQuery(exchange.getRequestURI());
		int maxDistance = getIntParam(params, "maxDistance", 30);
		int idFilter = getIntParam(params, "id", -1);

		List<Map<String, Object>> list = Microbot.getClientThread().runOnClientThreadOptional(() -> {
			Client client = Microbot.getClient();
			WorldView wv = client.getTopLevelWorldView();
			if (wv == null) {
				return null;
			}
			Player local = client.getLocalPlayer();
			WorldPoint playerLoc = local != null ? local.getWorldLocation() : null;

			List<Map<String, Object>> out = new ArrayList<>();
			for (GraphicsObject go : wv.getGraphicsObjects()) {
				if (go == null || go.finished()) {
					continue;
				}
				if (idFilter >= 0 && go.getId() != idFilter) {
					continue;
				}
				LocalPoint lp = go.getLocation();
				if (lp == null) {
					continue;
				}
				WorldPoint wp = WorldPoint.fromLocalInstance(client, lp, go.getLevel());
				if (wp == null) {
					continue;
				}
				int dist = playerLoc != null && playerLoc.getPlane() == wp.getPlane()
						? playerLoc.distanceTo(wp) : Integer.MAX_VALUE;
				if (dist > maxDistance) {
					continue;
				}
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", go.getId());
				m.put("distance", dist == Integer.MAX_VALUE ? -1 : dist);
				m.put("startCycle", go.getStartCycle());
				Map<String, Integer> pos = new LinkedHashMap<>();
				pos.put("x", wp.getX());
				pos.put("y", wp.getY());
				pos.put("plane", wp.getPlane());
				m.put("position", pos);
				out.add(m);
			}
			out.sort(Comparator.comparingInt(a -> (int) a.get("distance") < 0
					? Integer.MAX_VALUE : (int) a.get("distance")));
			return out;
		}).orElse(null);

		if (list == null) {
			sendJson(exchange, 503, errorResponse("World view unavailable (not logged in?)"));
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("count", list.size());
		response.put("graphics", list);
		sendJson(exchange, 200, response);
	}
}
