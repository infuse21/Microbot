package net.runelite.client.plugins.microbot.api.tileobject.models;

import java.awt.Rectangle;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.ApiTestClient;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class Rs2TileObjectModelTest {
    @Test
    public void worldLocationAndIdAreReadOnTheClientThread() throws Exception {
        try (ApiTestClient env = new ApiTestClient()) {
            GameObject object = mock(GameObject.class);
            WorldPoint location = new WorldPoint(3200, 3200, 0);
            when(object.getId()).thenAnswer(i -> { env.requireClientThread(); return 123; });
            when(object.getWorldLocation()).thenAnswer(i -> { env.requireClientThread(); return location; });
            Rs2TileObjectModel model = new Rs2TileObjectModel(object);

            assertEquals(123, model.getId());
            assertEquals(location, model.getWorldLocation());
        }
    }

    @Test
    public void missingActionFailedSubmissionAndStaleObjectReturnFalse() throws Exception {
        try (ApiTestClient env = new ApiTestClient();
             MockedStatic<Rs2Camera> camera = mockStatic(Rs2Camera.class);
             MockedStatic<Microbot> microbot = mockStatic(Microbot.class, CALLS_REAL_METHODS)) {
            GameObject object = mock(GameObject.class);
            WorldView view = mock(WorldView.class);
            Scene scene = mock(Scene.class);
            Tile tile = mock(Tile.class);
            ObjectComposition composition = mock(ObjectComposition.class);
            when(env.client.getGameState()).thenReturn(GameState.LOGGED_IN);
            when(env.client.getWorldView(1)).thenReturn(view);
            when(env.client.getObjectDefinition(123)).thenReturn(composition);
            when(view.getId()).thenReturn(1);
            when(view.getScene()).thenReturn(scene);
            Tile[][][] tiles = new Tile[1][2][2];
            tiles[0][1][1] = tile;
            when(scene.getTiles()).thenReturn(tiles);
            when(tile.getGameObjects()).thenReturn(new GameObject[]{object});
            when(object.getWorldView()).thenReturn(view);
            when(object.getId()).thenReturn(123);
            when(object.getSceneMinLocation()).thenReturn(new Point(1, 1));
            when(object.getLocalLocation()).thenReturn(new LocalPoint(128, 128, 1));
            when(object.sizeX()).thenReturn(1);
            when(object.sizeY()).thenReturn(1);
            when(composition.getName()).thenReturn("Door");
            when(composition.getActions()).thenReturn(new String[]{"Open", null, null, null, null});
            camera.when(() -> Rs2Camera.isTileOnScreen(any(LocalPoint.class))).thenReturn(true);
            microbot.when(() -> Microbot.tryDoInvoke(any(NewMenuEntry.class), any(Rectangle.class)))
                    .thenReturn(false);
            Rs2TileObjectModel model = new Rs2TileObjectModel(object);

            assertFalse(model.click("Climb"));
            assertFalse(model.click("Open"));
            microbot.when(() -> Microbot.tryDoInvoke(any(NewMenuEntry.class), any(Rectangle.class)))
                    .thenReturn(true);
            assertTrue(model.click("Open"));
            assertTrue(model.click());
            when(tile.getGameObjects()).thenReturn(new GameObject[0]);
            assertFalse(model.click("Open"));
        }
    }
}
