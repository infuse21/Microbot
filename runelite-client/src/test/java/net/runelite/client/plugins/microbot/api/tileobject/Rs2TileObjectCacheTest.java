package net.runelite.client.plugins.microbot.api.tileobject;

import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.ApiTestClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2TileObjectCacheTest {
    @Test
    public void firstQueryAtTickZeroScansTheScene() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            GroundObject object = mock(GroundObject.class);
            installScene(env, new AtomicReference<>(sceneWith(object)), new AtomicReference<>(GameState.LOGGED_IN));
            when(env.client.getTickCount()).thenReturn(0);

            assertSame(object, Microbot.getRs2TileObjectCache().query().first().getRawTileObject());
        }
    }

    @Test
    public void sceneAndLoginChangesInvalidateWithinTheSameTick() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            GroundObject first = mock(GroundObject.class);
            GroundObject second = mock(GroundObject.class);
            AtomicReference<Scene> scene = new AtomicReference<>(sceneWith(first));
            AtomicReference<GameState> state = new AtomicReference<>(GameState.LOGGED_IN);
            installScene(env, scene, state);
            when(env.client.getTickCount()).thenReturn(1);

            assertSame(first, Microbot.getRs2TileObjectCache().query().first().getRawTileObject());
            scene.set(sceneWith(second));
            assertSame(second, Microbot.getRs2TileObjectCache().query().first().getRawTileObject());
            scene.set(null);
            assertEquals(0, Microbot.getRs2TileObjectCache().query().count());
            state.set(GameState.LOGIN_SCREEN);
            assertNull(Microbot.getRs2TileObjectCache().query().first());
        }
    }

    private static void installScene(ApiTestClient env, AtomicReference<Scene> scene,
                                     AtomicReference<GameState> state) throws Exception {
        Client client = env.client;
        Player player = mock(Player.class);
        WorldView view = mock(WorldView.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getGameState()).thenAnswer(i -> state.get());
        when(client.getWorldView(-1)).thenReturn(view);
        when(view.getId()).thenReturn(-1);
        when(view.getScene()).thenAnswer(i -> scene.get());
        when(view.getPlane()).thenReturn(0);
        Microbot.getWorldViewIds().add(-1);
        env.installCache("rs2TileObjectCache", Rs2TileObjectCache.class);
    }

    private static Scene sceneWith(GroundObject object) {
        Scene scene = mock(Scene.class);
        Tile tile = mock(Tile.class);
        when(tile.getGroundObject()).thenReturn(object);
        when(scene.getTiles()).thenReturn(new Tile[][][]{{{tile}}});
        return scene;
    }
}
