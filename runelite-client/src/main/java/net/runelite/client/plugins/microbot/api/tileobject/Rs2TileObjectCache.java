package net.runelite.client.plugins.microbot.api.tileobject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public final class Rs2TileObjectCache {

    private final Client client;
    private final ClientThread clientThread;

    private int lastUpdateObjects = -1;
    private List<Object> lastContext = Collections.emptyList();
    private List<Rs2TileObjectModel> tileObjects = Collections.emptyList();

    @Inject
    public Rs2TileObjectCache(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    public Rs2TileObjectQueryable query() {
        return new Rs2TileObjectQueryable();
    }

    /**
     * Get all tile objects in the current scene
     *
     * @return Stream of Rs2TileObjectModel
     */
    public Stream<Rs2TileObjectModel> getStream() {
        return clientThread.runOnClientThreadOptional(this::snapshot)
                .orElse(Collections.emptyList()).stream();
    }

    // Membership is copied on the client thread; the models remain live wrappers.
    private List<Rs2TileObjectModel> snapshot() {
        Player player = client.getLocalPlayer();
        if (player == null || client.getGameState() != GameState.LOGGED_IN) {
            tileObjects = Collections.emptyList();
            lastContext = Collections.emptyList();
            lastUpdateObjects = -1;
            return tileObjects;
        }

        List<WorldView> views = new ArrayList<>();
        List<Object> context = new ArrayList<>();
        context.add(client.getWorld());
        context.add(player);
        for (int id : Microbot.getWorldViewIds()) {
            WorldView view = client.getWorldView(id);
            if (view == null) continue;
            views.add(view);
            context.add(view);
            context.add(view.getScene());
            context.add(view.getPlane());
            context.add(view.getBaseX());
            context.add(view.getBaseY());
        }
        if (lastUpdateObjects == client.getTickCount() && lastContext.equals(context)) return tileObjects;

        List<Rs2TileObjectModel> result = new ArrayList<>();
        for (WorldView view : views) {
            Scene scene = view.getScene();
            if (scene == null) continue;
            Tile[][][] tiles = scene.getTiles();
            int plane = view.getPlane();
            if (tiles == null || plane < 0 || plane >= tiles.length || tiles[plane] == null) continue;
            for (Tile[] row : tiles[plane]) {
                if (row == null) continue;
                for (Tile tile : row) {
                    if (tile == null) continue;

                    if (tile.getGameObjects() != null) {
                        for (GameObject gameObject : tile.getGameObjects()) {
                            if (gameObject == null) continue;
                            var sceneMin = gameObject.getSceneMinLocation();
                            if (sceneMin != null && sceneMin.equals(tile.getSceneLocation())) {
                                result.add(new Rs2TileObjectModel(gameObject));
                            }
                        }
                    }
                    if (tile.getGroundObject() != null) {
                        result.add(new Rs2TileObjectModel(tile.getGroundObject()));
                    }
                    if (tile.getWallObject() != null) {
                        result.add(new Rs2TileObjectModel(tile.getWallObject()));
                    }
                    if (tile.getDecorativeObject() != null) {
                        result.add(new Rs2TileObjectModel(tile.getDecorativeObject()));
                    }
                }
            }
        }
        tileObjects = Collections.unmodifiableList(result);
        lastContext = context;
        lastUpdateObjects = client.getTickCount();
        return tileObjects;
    }

    /**
     * @deprecated Use {@link Microbot#getRs2TileObjectCache()}.getStream() instead
     */
    @Deprecated(since = "2.1.8", forRemoval = true)
    public static Stream<Rs2TileObjectModel> getObjectsStream() {
        return Microbot.getRs2TileObjectCache().getStream();
    }
}
