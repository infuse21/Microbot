package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.NPCComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.GnomeGlider;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

/** Cache-backed live adapter for non-blocking gnome-glider interaction stages. */
public final class Rs2GnomeGliderScene implements GnomeGliderScene
{
	private static final int NPC_SEARCH_RADIUS = 6;

	@Override
	public GnomeGlider find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		GnomeGlider destination = destinationStage(transport);
		return destination == null ? npcStage(transport) : destination;
	}

	@Override
	public GnomeGlider observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		GnomeGlider destination = destinationStage(transport);
		if (destination != null)
		{
			return destination;
		}
		if (GnomeGliderPolicy.isDestinationAction(pendingAction))
		{
			return null;
		}
		return npcStage(transport);
	}

	public static boolean interactNpc(PlannedEdge edge, String expectedAction, int npcId)
	{
		Transport transport = findTransport(edge);
		Rs2NpcModel npc = transport == null ? null : findNpc(transport);
		return npc != null && transport.getObjectId() == npcId
			&& transport.getAction().equalsIgnoreCase(expectedAction)
			&& npc.click(transport.getAction());
	}

	public static boolean selectDestination(String destination)
	{
		Widget widget = destinationWidget(destination);
		return widget != null && isSelectable(widget) && Rs2Widget.clickWidget(widget);
	}

	private static GnomeGlider destinationStage(Transport transport)
	{
		Widget map = Rs2Widget.getWidget(InterfaceID.Glidermap.UNIVERSE);
		if (map == null || isHidden(map))
		{
			return null;
		}
		Widget destination = destinationWidget(GnomeGliderPolicy.destinationName(transport));
		if (destination == null || !isSelectable(destination))
		{
			if (Rs2PathApi.getPathfinderConfig() != null)
			{
				Rs2PathApi.getPathfinderConfig()
					.markGnomeGliderDestinationUnavailable(transport.getDestination());
			}
			return stage(transport, transport.getOrigin(),
				GnomeGlider.Stage.DESTINATION_UNAVAILABLE);
		}
		return stage(transport, transport.getOrigin(), GnomeGlider.Stage.DESTINATION);
	}

	private static boolean isSelectable(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget != null && GnomeGliderPolicy.isDestinationSelectable(widget.isHidden()))
			.orElse(false);
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
			.stream().filter(GnomeGliderPolicy::isEligible).findFirst().orElse(null);
	}

	private static GnomeGlider npcStage(Transport transport)
	{
		Rs2NpcModel npc = findNpc(transport);
		return npc == null ? null
			: stage(transport, npc.getWorldLocation(), GnomeGlider.Stage.NPC);
	}

	private static Rs2NpcModel findNpc(Transport transport)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Rs2NpcModel best = null;
			int bestScore = Integer.MAX_VALUE;
			for (Rs2NpcModel npc : Microbot.getRs2NpcCache().query()
				.withName(transport.getName())
				.within(transport.getOrigin(), NPC_SEARCH_RADIUS).toList())
			{
					NPCComposition composition = npc.getNpc().getTransformedComposition();
					if (composition == null)
					{
						composition = npc.getNpc().getComposition();
					}
					if (!GnomeGliderPolicy.isLiveNpcMatch(transport, npc.getId(),
						npc.getName(), composition == null ? null : composition.getActions(),
						npc.getWorldLocation()))
					{
						continue;
					}
					int score = (npc.getId() == transport.getObjectId() ? 0 : 100)
						+ npc.getWorldLocation().distanceTo2D(transport.getOrigin());
					if (score < bestScore)
					{
						best = npc;
						bestScore = score;
					}
			}
			return best;
		}).orElse(null);
	}

	private static GnomeGlider stage(Transport transport, WorldPoint npcTile,
		GnomeGlider.Stage stage)
	{
		return new GnomeGlider(transport.getOrigin(), transport.getDestination(),
			transport.getObjectId(), transport.getAction(),
			GnomeGliderPolicy.destinationName(transport), npcTile, stage);
	}

	private static Widget destinationWidget(String destination)
	{
		if (destination == null)
		{
			return null;
		}
		switch (destination)
		{
			case "Ta Quir Priw":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.GRANDTREE_BUTTON);
			case "Sindarpos":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.WHITEWOLFMOUNTAIN_BUTTON);
			case "Lemanto Andra":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.VARROCK_BUTTON);
			case "Kar-Hewo":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.ALKHARID_BUTTON);
			case "Gandius":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.KARAMJA_BUTTON);
			case "Lemantolly Undri":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.OGREAREA_BUTTON);
			case "Ookookolly Undri":
				return Rs2Widget.getWidget(InterfaceID.Glidermap.APEATOLL_BUTTON);
			default:
				return null;
		}
	}
}
