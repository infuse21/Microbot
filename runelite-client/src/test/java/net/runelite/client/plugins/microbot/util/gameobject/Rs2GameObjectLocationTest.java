package net.runelite.client.plugins.microbot.util.gameobject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Rs2GameObjectLocationTest
{
    @Test
    public void missingLocationsDoNotHideBankAndSortingUsesOneSnapshot() throws Exception
    {
        Client client = mock(Client.class);
        ClientThread thread = mock(ClientThread.class);
        Player player = mock(Player.class);
        WorldView world = mock(WorldView.class);
        Scene scene = mock(Scene.class);
        Tile tile = mock(Tile.class);
        GameObject invalid = mock(GameObject.class);
        GameObject chest = mock(GameObject.class);
        GameObject farther = mock(GameObject.class);
        LocalPoint anchor = new LocalPoint(128, 128);
        Point scenePoint = new Point(1, 1);
        Tile[][][] tiles = new Tile[1][Constants.SCENE_SIZE][Constants.SCENE_SIZE];
        tiles[0][1][1] = tile;
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldView()).thenReturn(world);
        when(world.getScene()).thenReturn(scene);
        when(scene.getTiles()).thenReturn(tiles);
        when(tile.getSceneLocation()).thenReturn(scenePoint);
        when(tile.getGameObjects()).thenReturn(new GameObject[]{invalid, farther, chest});
        for (GameObject object : Arrays.asList(invalid, farther, chest))
        {
            when(object.getSceneMinLocation()).thenReturn(scenePoint);
        }
        when(chest.getId()).thenReturn(4483);
        when(chest.getLocalLocation()).thenReturn(new LocalPoint(256, 128), (LocalPoint) null);
        when(farther.getLocalLocation()).thenReturn(new LocalPoint(512, 128), (LocalPoint) null);
        when(thread.runOnClientThreadOptional(any(Callable.class))).thenAnswer(invocation ->
            Optional.ofNullable(((Callable<?>) invocation.getArgument(0)).call()));
        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class))
        {
            microbot.when(Microbot::getClient).thenReturn(client);
            microbot.when(Microbot::getClientThread).thenReturn(thread);
            assertEquals(Arrays.asList(chest, farther),
                Rs2GameObject.getGameObjects(object -> true, anchor, 1024));
            verify(chest, times(1)).getLocalLocation();
            verify(farther, times(1)).getLocalLocation();
            when(chest.getLocalLocation()).thenReturn(new LocalPoint(256, 128));
            assertEquals(Collections.singletonList(chest),
                Rs2GameObject.getGameObjects(object -> object.getId() == 4483, anchor, 1024));
            assertTrue(Rs2GameObject.getGameObjects(object -> true, (LocalPoint) null, 1024).isEmpty());
        }
    }
}
