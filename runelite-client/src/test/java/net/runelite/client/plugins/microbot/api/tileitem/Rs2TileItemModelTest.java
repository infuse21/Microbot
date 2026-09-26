package net.runelite.client.plugins.microbot.api.tileitem;

import java.util.Optional;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileitem.models.Rs2TileItemModel;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class Rs2TileItemModelTest {
    @Test
    public void unknownDespawnTimeIsNeverImminent() {
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        TileItem tileItem = mock(TileItem.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        when(client.getTickCount()).thenReturn(100);
        when(tileItem.getDespawnTime()).thenReturn(-1);

        try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class)) {
            microbot.when(Microbot::getClientThread).thenReturn(clientThread);
            microbot.when(Microbot::getClient).thenReturn(client);
            Rs2TileItemModel item = new Rs2TileItemModel(
                    mock(Tile.class), tileItem, mock(WorldView.class));

            assertFalse(item.willDespawnWithin(10));
            assertFalse(item.willDespawnWithin(Integer.MAX_VALUE));

            when(tileItem.getDespawnTime()).thenReturn(110);
            assertFalse(item.willDespawnWithin(9));
            assertTrue(item.willDespawnWithin(10));
        }
    }
}
