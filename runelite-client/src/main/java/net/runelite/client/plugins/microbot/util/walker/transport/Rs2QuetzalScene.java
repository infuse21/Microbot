package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.Quetzal;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.List;

/** Cache-backed live adapter for non-blocking quetzal interaction stages. */
public final class Rs2QuetzalScene implements QuetzalScene
{
	private static final int NPC_SEARCH_RADIUS = 8;

	@Override
	public Quetzal find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		Quetzal destination = destinationStage(transport);
		return destination == null ? npcStage(transport) : destination;
	}

	@Override
	public Quetzal observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		Quetzal destination = destinationStage(transport);
		if (destination != null)
		{
			return destination;
		}
		if (QuetzalPolicy.isDestinationAction(pendingAction))
		{
			return null;
		}
		return npcStage(transport);
	}

	public static boolean interactNpc(PlannedEdge edge, String expectedAction)
	{
		Transport transport = findTransport(edge);
		Rs2NpcModel npc = transport == null ? null : findNpc(transport);
		return npc != null
			&& QuetzalPolicy.NPC_ACTION.equalsIgnoreCase(expectedAction)
			&& npc.click(QuetzalPolicy.NPC_ACTION);
	}

	public static boolean selectDestination(String destination)
	{
		Widget widget = destinationWidget(destination);
		return widget != null && Rs2Widget.clickWidget(widget);
	}

	private static Quetzal destinationStage(Transport transport)
	{
		Widget map = Rs2Widget.getWidget(InterfaceID.QuetzalMenu.UNIVERSE);
		if (map == null || isHidden(map))
		{
			return null;
		}
		Widget destination = destinationWidget(QuetzalPolicy.destinationName(transport));
		return destination == null ? null
			: stage(transport, transport.getOrigin(), -1, Quetzal.Stage.DESTINATION);
	}

	private static boolean isHidden(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(widget::isHidden)
			.orElse(true);
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(QuetzalPolicy::isEligible).findFirst().orElse(null);
	}

	private static Quetzal npcStage(Transport transport)
	{
		Rs2NpcModel npc = findNpc(transport);
		return npc == null ? null : stage(transport, npc.getWorldLocation(), npc.getId(),
			Quetzal.Stage.NPC);
	}

	private static Rs2NpcModel findNpc(Transport transport)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Rs2NpcModel best = null;
			int bestDistance = Integer.MAX_VALUE;
			for (Rs2NpcModel npc : Microbot.getRs2NpcCache().query()
				.withName(QuetzalPolicy.NPC_NAME)
				.within(transport.getOrigin(), NPC_SEARCH_RADIUS).toList())
			{
				NPCComposition composition = npc.getNpc().getTransformedComposition();
				if (composition == null)
				{
					composition = npc.getNpc().getComposition();
				}
				if (!QuetzalPolicy.isLiveNpcMatch(transport, npc.getName(),
					composition == null ? null : composition.getActions(), npc.getWorldLocation()))
				{
					continue;
				}
				int distance = npc.getWorldLocation().distanceTo2D(transport.getOrigin());
				if (distance < bestDistance)
				{
					best = npc;
					bestDistance = distance;
				}
			}
			return best;
		}).orElse(null);
	}

	private static Quetzal stage(Transport transport, WorldPoint npcTile, int npcId,
		Quetzal.Stage stage)
	{
		return new Quetzal(transport.getOrigin(), transport.getDestination(), npcId,
			QuetzalPolicy.destinationName(transport), npcTile, stage);
	}

	private static Widget destinationWidget(String destination)
	{
		if (destination == null || destination.trim().isEmpty())
		{
			return null;
		}
		int[] roots = {
			InterfaceID.QuetzalMenu.ICONS,
			InterfaceID.QuetzalMenu.MAP,
			InterfaceID.QuetzalMenu.SCROLL,
			InterfaceID.QuetzalMenu.CONTENTS,
			InterfaceID.QuetzalMenu.UNIVERSE,
		};
		for (int rootId : roots)
		{
			Widget root = Rs2Widget.getWidget(rootId);
			if (root == null || isHidden(root))
			{
				continue;
			}
			Widget found = Rs2Widget.findWidget(destination, List.of(root), false);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}
}
