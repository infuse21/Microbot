package net.runelite.client.plugins.microbot.util.poh;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportType;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;
import net.runelite.client.plugins.microbot.util.poh.data.MountedGlory;
import net.runelite.client.plugins.microbot.util.poh.data.MountedMythical;
import net.runelite.client.plugins.microbot.util.poh.data.MountedDigsite;
import net.runelite.client.plugins.microbot.util.poh.data.MountedXerics;
import net.runelite.api.gameval.ObjectID;

/**
 * Represents a transport mechanism using the Player-Owned House (POH) teleportation system.
 * This class extends the base Transport class and provides specific implementations
 * for POH teleportation.
 */
public class PohTransport extends Transport {

    @Getter
    private final PohTeleport teleport;

    public PohTransport(WorldPoint exitPortalPoint, PohTeleport teleport) {
        super(
                java.util.Objects.requireNonNull(exitPortalPoint, "exitPortalPoint is null"),
                java.util.Objects.requireNonNull(teleport, "teleport is null").getDestination(),
                teleport.displayInfo(), TransportType.POH, true, teleport.getDuration()
        );
        this.teleport = teleport;
        if (teleport == net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox.FORTIS_COLOSSEUM) {
            getVarplayers().add(new net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer(
                    net.runelite.api.gameval.VarPlayerID.COLOSSEUM_GLORY, 11999,
                    net.runelite.client.plugins.microbot.shortestpath.TransportVarPlayer.Operator.GREATER_THAN));
        } else if (teleport == net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox.TEARS_OF_GUTHIX) {
            getQuests().put(net.runelite.api.Quest.TEARS_OF_GUTHIX, net.runelite.api.QuestState.FINISHED);
        } else if (teleport == net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox.MISCELLANIA) {
            getQuests().put(net.runelite.api.Quest.THRONE_OF_MISCELLANIA, net.runelite.api.QuestState.FINISHED);
        } else if (teleport == net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox.DONDAKAN) {
            getQuests().put(net.runelite.api.Quest.BETWEEN_A_ROCK, net.runelite.api.QuestState.FINISHED);
        }
    }

    /**
     * Executes the Transport's PoH teleportation action.
     *
     * @return true on successful teleportation
     */
    public boolean execute() {
        return teleport.execute();
    }

    @Override
    public int getObjectId() {
        if (teleport instanceof net.runelite.client.plugins.microbot.util.poh.data.NexusPortal)
            return net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.PORTAL_IDS[0];
        if (teleport instanceof net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox)
            return ObjectID.POH_JEWELLERY_BOX_1;
        if (teleport instanceof MountedGlory) return ObjectID.POH_TROPHY_AMULETOFGLORY_4;
        if (teleport instanceof MountedMythical) return ((MountedMythical) teleport).getObjectId();
        if (teleport instanceof MountedDigsite) return MountedDigsite.IDS[0];
        if (teleport instanceof MountedXerics) return MountedXerics.IDS[0];
        return teleport instanceof PohPortal ? ((PohPortal) teleport).getObjectIds()[0] : super.getObjectId();
    }

    @Override
    public String getAction() {
        if (teleport instanceof net.runelite.client.plugins.microbot.util.poh.data.NexusPortal)
            return "Teleport menu";
        if (teleport instanceof net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox)
            return "Teleport menu";
        if (teleport instanceof MountedGlory) return ((MountedGlory) teleport).getDestinationName();
        if (teleport instanceof MountedMythical) return "Teleport";
        if (teleport instanceof MountedDigsite) return ((MountedDigsite) teleport).getDestinationName();
        if (teleport instanceof MountedXerics) return ((MountedXerics) teleport).getDestinationName();
        return teleport instanceof PohPortal ? ((PohPortal) teleport).getAction() : super.getAction();
    }

}
