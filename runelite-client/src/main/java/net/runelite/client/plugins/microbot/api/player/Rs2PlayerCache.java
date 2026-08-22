package net.runelite.client.plugins.microbot.api.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.player.models.Rs2PlayerModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public final class Rs2PlayerCache {

    private final Client client;
    private final ClientThread clientThread;

    private int lastUpdatePlayers = 0;
    private List<Rs2PlayerModel> players = new ArrayList<>();

    @Inject
    public Rs2PlayerCache(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    public Rs2PlayerQueryable query() {
        return new Rs2PlayerQueryable();
    }

    /**
     * Get all players in the current scene
     * @return Stream of Rs2PlayerModel
     */
    public Stream<Rs2PlayerModel> getStream() {
        if (lastUpdatePlayers >= client.getTickCount()) {
            return players.stream();
        }

        List<Rs2PlayerModel> result = new ArrayList<>();

        for (var id : Microbot.getActiveWorldViewIds()) {
            WorldView worldView = client.getWorldView(id);
            if (worldView == null) {
                continue;
            }
            result.addAll(worldView.players()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(Rs2PlayerModel::new)
                    .collect(Collectors.toList()));
        }

        players = result;
        lastUpdatePlayers = client.getTickCount();
        return players.stream();
    }

    /**
     * @deprecated Use {@link Microbot#getRs2PlayerCache()}.getStream() instead
     */
    @Deprecated(since = "2.1.8", forRemoval = true)
    public static Stream<Rs2PlayerModel> getPlayersStream() {
        return Microbot.getRs2PlayerCache().getStream();
    }
}
