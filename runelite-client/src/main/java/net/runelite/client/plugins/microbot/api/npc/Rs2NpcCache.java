package net.runelite.client.plugins.microbot.api.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public final class Rs2NpcCache {

    private final Client client;
    private final ClientThread clientThread;

    private int lastUpdateNpcs = 0;
    private List<Rs2NpcModel> npcs = new ArrayList<>();

    @Inject
    public Rs2NpcCache(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    public Rs2NpcQueryable query() {
        return new Rs2NpcQueryable();
    }

    /**
     * Get all NPCs in the current scene across all world views
     *
     * @return Stream of Rs2NpcModel
     */
    public Stream<Rs2NpcModel> getStream() {
        if (lastUpdateNpcs >= client.getTickCount()) {
            return npcs.stream();
        }

        List<Rs2NpcModel> result = new ArrayList<>();

        for (var id : Microbot.getActiveWorldViewIds()) {
            WorldView worldView = client.getWorldView(id);
            if (worldView == null) {
                continue;
            }

            result.addAll(worldView.npcs()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(Rs2NpcModel::new)
                    .collect(Collectors.toList()));
        }

        npcs = result;
        lastUpdateNpcs = client.getTickCount();
        return result.stream();
    }

    /**
     * @deprecated Use {@link Microbot#getRs2NpcCache()}.getStream() instead
     */
    @Deprecated(since = "2.1.8", forRemoval = true)
    public static Stream<Rs2NpcModel> getNpcsStream() {
        return Microbot.getRs2NpcCache().getStream();
    }
}
