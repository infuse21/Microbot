package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MagicMushtreeTransport;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.List;

/** Cache-backed live adapter for non-blocking Magic Mushtree stages. */
public final class Rs2MagicMushtreeScene implements MagicMushtreeScene
{
	private static final int OBJECT_SEARCH_RADIUS = 4;

	@Override
	public MagicMushtreeTransport find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		MagicMushtreeTransport destination = destinationStage(transport);
		return destination == null ? objectStage(transport) : destination;
	}

	@Override
	public MagicMushtreeTransport observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		MagicMushtreeTransport destination = destinationStage(transport);
		if (destination != null)
		{
			return destination;
		}
		if (MagicMushtreePolicy.isDestinationAction(pendingAction))
		{
			return null;
		}
		return objectStage(transport);
	}

	public static boolean interactObject(PlannedEdge edge, String expectedAction, int objectId)
	{
		Transport transport = findTransport(edge);
		MushtreeObject mushtree = transport == null ? null : findObject(transport);
		return mushtree != null && mushtree.id == objectId
			&& transport.getAction().equalsIgnoreCase(expectedAction)
			&& mushtree.object.click(transport.getAction());
	}

	public static boolean selectDestination(String destination)
	{
		Widget widget = findDestinationWidget(destination);
		return widget != null && isVisible(widget) && Rs2Widget.clickWidget(widget);
	}

	private static MagicMushtreeTransport destinationStage(Transport transport)
	{
		Widget root = Rs2Widget.getWidget(InterfaceID.FossilMushtrees.UNIVERSE);
		if (!isVisible(root))
		{
			return null;
		}
		String destinationName = destinationName(transport);
		Widget destination = findDestinationWidget(destinationName);
		if (destination != null)
		{
			return stage(transport, transport.getOrigin(), MagicMushtreeTransport.Stage.DESTINATION);
		}
		if (Rs2Widget.findWidget("Not yet found", List.of(root), false) == null)
		{
			return null;
		}
		if (Rs2PathApi.getPathfinderConfig() != null)
		{
			Rs2PathApi.getPathfinderConfig()
				.markMagicMushtreeDestinationUnavailable(transport.getDestination());
		}
		return stage(transport, transport.getOrigin(),
			MagicMushtreeTransport.Stage.DESTINATION_UNAVAILABLE);
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(MagicMushtreePolicy::isEligible).findFirst().orElse(null);
	}

	private static MagicMushtreeTransport objectStage(Transport transport)
	{
		MushtreeObject mushtree = findObject(transport);
		return mushtree == null ? null : stage(transport, mushtree.tile,
			MagicMushtreeTransport.Stage.OBJECT);
	}

	private static MushtreeObject findObject(Transport transport)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			MushtreeObject best = null;
			int bestDistance = Integer.MAX_VALUE;
			for (Rs2TileObjectModel object : Microbot.getRs2TileObjectCache().query()
				.withId(transport.getObjectId())
				.within(transport.getOrigin(), OBJECT_SEARCH_RADIUS).toList())
			{
				net.runelite.api.ObjectComposition composition = object.getObjectComposition();
				String[] actions = composition == null ? null : composition.getActions();
				WorldPoint tile = object.getWorldLocation();
				if (tile == null || actions == null || Arrays.stream(actions)
					.filter(java.util.Objects::nonNull)
					.noneMatch(action -> action.equalsIgnoreCase(transport.getAction())))
				{
					continue;
				}
				int distance = tile.distanceTo2D(transport.getOrigin());
				if (distance < bestDistance)
				{
					best = new MushtreeObject(object, object.getId(), tile);
					bestDistance = distance;
				}
			}
			return best;
		}).orElse(null);
	}

	private static MagicMushtreeTransport stage(Transport transport, WorldPoint objectTile,
		MagicMushtreeTransport.Stage stage)
	{
		return new MagicMushtreeTransport(transport.getOrigin(), transport.getDestination(),
			transport.getObjectId(), transport.getAction(), destinationName(transport),
			objectTile, stage);
	}

	private static String destinationName(Transport transport)
	{
		return MagicMushtreePolicy.destinationName(transport.getDisplayInfo());
	}

	private static Widget findDestinationWidget(String destination)
	{
		Widget root = Rs2Widget.getWidget(InterfaceID.FossilMushtrees.UNIVERSE);
		return root == null ? null : Rs2Widget.findWidget(destination, List.of(root), false);
	}

	private static boolean isVisible(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget != null && !widget.isHidden()).orElse(false);
	}

	private static final class MushtreeObject
	{
		private final Rs2TileObjectModel object;
		private final int id;
		private final WorldPoint tile;

		private MushtreeObject(Rs2TileObjectModel object, int id, WorldPoint tile)
		{
			this.object = object;
			this.id = id;
			this.tile = tile;
		}
	}
}
