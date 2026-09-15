package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.shortestpath.PohPanel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SimpleTeleport;

/** Resolves direct teleports from the same enabled catalog snapshot used by pathfinding. */
public final class Rs2SimpleTeleportScene implements SimpleTeleportScene
{
	@Override
	public SimpleTeleport observe(PlannedEdge edge, String pending)
	{
		SimpleTeleport teleport = find(edge);
		return teleport != null && teleport.getType() == net.runelite.client.plugins.microbot.shortestpath.TransportType.TELEPORTATION_SPELL
			? Rs2SpellTeleportScene.observe(teleport, pending) : teleport;
	}
	@Override
	public boolean hasLanded(PlannedEdge edge, WorldPoint player)
	{
		return SimpleTeleportScene.super.hasLanded(edge, player)
			&& (!edge.to().equals(PohPanel.getExitPortalTile()) || PohTeleports.isInHouse());
	}

	@Override
	public SimpleTeleport find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(SimpleTeleportPolicy::isEligible).filter(Rs2SimpleTeleportScene::matchesHousePreference)
			.map(transport -> from(edge, transport)).findFirst().orElse(null);
	}

	public static boolean matchesHousePreference(Transport transport)
	{
		return !("Teleport to House".equals(transport.getDisplayInfo())
			|| "Construction cape: Tele to POH".equals(transport.getDisplayInfo()))
			|| transport.getVarbits().stream().allMatch(gate -> gate.matches(
				net.runelite.client.plugins.microbot.Microbot.getVarbitValue(gate.getVarbitId())));
	}

	private static SimpleTeleport from(PlannedEdge edge, Transport transport)
	{
		return new SimpleTeleport(edge.from(), transport.getDestination(),
			transport.getType(), transport.getDisplayInfo());
	}
}
