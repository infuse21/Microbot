package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.SpiritTree;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.List;

/** Cache-backed live adapter for non-blocking spirit-tree interaction stages. */
public final class Rs2SpiritTreeScene implements SpiritTreeScene
{
	private static final int OBJECT_SEARCH_RADIUS = 4;
	private static final int MISSING_ORIGIN_CONFIRMATION_RADIUS = 4;
	private static final int LEGACY_ADVENTURE_LOG_GROUP_ID = 187;
	private static final int LEGACY_ADVENTURE_LOG_CHOICES_CHILD_ID = 3;

	@Override
	public SpiritTree find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		SpiritTree destination = destinationStage(transport);
		return destination == null ? objectStage(transport) : destination;
	}

	@Override
	public SpiritTree observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		SpiritTree destination = destinationStage(transport);
		if (destination != null)
		{
			return destination;
		}
		if (SpiritTreePolicy.isDestinationAction(pendingAction))
		{
			return null;
		}
		return objectStage(transport);
	}

	@Override
	public boolean hasLanded(PlannedEdge edge, WorldPoint player)
	{
		Transport transport = findTransport(edge);
		if (transport == null) return false;
		if (!"Your house".equalsIgnoreCase(destinationName(transport)))
		{
			return SpiritTreeScene.super.hasLanded(edge, player);
		}
		Transport houseTree = new Transport(transport.getDestination(), transport.getDestination(),
			"", net.runelite.client.plugins.microbot.shortestpath.TransportType.SPIRIT_TREE,
			true, "Travel", "Spirit tree", 29227);
		TreeObject tree = findTreeObject(houseTree);
		return tree != null && player != null && player.getPlane() == tree.tile.getPlane()
			&& player.distanceTo2D(tree.tile) <= 3;
	}

	public static boolean interactObject(PlannedEdge edge, String expectedAction, int objectId)
	{
		Transport transport = findTransport(edge);
		TreeObject tree = transport == null ? null : findTreeObject(transport);
		return tree != null && tree.id == objectId
			&& tree.action.equalsIgnoreCase(expectedAction)
			&& tree.object.click(tree.action);
	}

	public static boolean selectDestination(PlannedEdge edge, String destination)
	{
		Transport transport = findTransport(edge);
		if (transport == null || !destinationName(transport).equalsIgnoreCase(destination)) return false;
		Widget widget = findDestinationWidget(destination);
		return widget != null && isSelectable(widget) && Rs2Widget.clickWidget(widget);
	}

	private static SpiritTree destinationStage(Transport transport)
	{
		Widget widget = findDestinationWidget(destinationName(transport));
		if (widget == null)
		{
			return null;
		}
		if (!isSelectable(widget))
		{
			if (Rs2PathApi.getPathfinderConfig() != null)
			{
				Rs2PathApi.getPathfinderConfig()
					.markSpiritTreeDestinationUnavailable(transport.getDestination());
			}
			return stage(transport, transport.getOrigin(),
				SpiritTree.Stage.DESTINATION_UNAVAILABLE);
		}
		return stage(transport, transport.getOrigin(), SpiritTree.Stage.DESTINATION);
	}

	private static boolean isSelectable(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget != null && !widget.isHidden()
				&& SpiritTreePolicy.isDestinationSelectable(widget.getText(),
					widget.getTextColor()))
			.orElse(false);
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(SpiritTreePolicy::isEligible).findFirst().orElse(null);
	}

	private static SpiritTree objectStage(Transport transport)
	{
		TreeObject tree = findTreeObject(transport);
		if (tree != null)
		{
			return new SpiritTree(transport.getOrigin(), transport.getDestination(),
				transport.getObjectId(), tree.action, destinationName(transport),
				tree.tile, SpiritTree.Stage.OBJECT);
		}
		WorldPoint player = Rs2Player.getWorldLocation();
		if (player == null || player.getPlane() != transport.getOrigin().getPlane()
			|| player.distanceTo2D(transport.getOrigin()) > MISSING_ORIGIN_CONFIRMATION_RADIUS)
		{
			return null;
		}
		if (Rs2PathApi.getPathfinderConfig() != null)
		{
			Rs2PathApi.getPathfinderConfig()
				.markSpiritTreeDestinationUnavailable(transport.getOrigin());
		}
		return stage(transport, transport.getOrigin(), SpiritTree.Stage.ORIGIN_UNAVAILABLE);
	}

	private static TreeObject findTreeObject(Transport transport)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			boolean house = transport.getObjectId() == 29227;
			if (house && !net.runelite.client.plugins.microbot.util.poh.PohTeleports.isInHouse()) return null;
			TreeObject best = null;
			int bestDistance = Integer.MAX_VALUE;
			var query = Microbot.getRs2TileObjectCache().query();
			if (house) query.fromWorldView();
			else query.withId(transport.getObjectId()).within(transport.getOrigin(), OBJECT_SEARCH_RADIUS);
			for (Rs2TileObjectModel object : query.toList())
			{
				net.runelite.api.ObjectComposition composition = object.getObjectComposition();
				String action = house ? SpiritTreePolicy.pohTreeAction(object.getId()) : transport.getAction();
				if (house && action == null && composition != null) action = SpiritTreePolicy.pohTreeAction(composition.getId());
				if (action == null) continue;
				String[] actions = composition == null ? null : composition.getActions();
				WorldPoint tile = house
					? net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation.templateLocation(object)
					: object.getWorldLocation();
				String expectedAction = action;
				if (tile == null || actions == null || Arrays.stream(actions)
					.filter(java.util.Objects::nonNull)
					.noneMatch(candidate -> candidate.equalsIgnoreCase(expectedAction)))
				{
					continue;
				}
				int distance = tile.distanceTo2D(transport.getOrigin());
				if (distance < bestDistance)
				{
					best = new TreeObject(object, transport.getObjectId(), tile, action);
					bestDistance = distance;
				}
			}
			return best;
		}).orElse(null);
	}

	private static SpiritTree stage(Transport transport, WorldPoint objectTile,
		SpiritTree.Stage stage)
	{
		return new SpiritTree(transport.getOrigin(), transport.getDestination(),
			transport.getObjectId(), transport.getAction(), destinationName(transport),
			objectTile, stage);
	}

	private static String destinationName(Transport transport)
	{
		return SpiritTreePolicy.destinationName(transport.getDisplayInfo());
	}

	private static Widget findDestinationWidget(String destination)
	{
		Widget destinationWidget = findDestinationWidget(destination,
			Rs2Widget.getWidget(InterfaceID.MenuNew.TEXT));
		if (destinationWidget != null)
		{
			return destinationWidget;
		}
		return findDestinationWidget(destination,
			Rs2Widget.getWidget(LEGACY_ADVENTURE_LOG_GROUP_ID,
				LEGACY_ADVENTURE_LOG_CHOICES_CHILD_ID));
	}

	private static Widget findDestinationWidget(String destination, Widget choices)
	{
		return choices == null ? null
			: Rs2Widget.findWidget(destination, List.of(choices), false);
	}

	private static final class TreeObject
	{
		private final Rs2TileObjectModel object;
		private final int id;
		private final WorldPoint tile;
		private final String action;

		private TreeObject(Rs2TileObjectModel object, int id, WorldPoint tile, String action)
		{
			this.object = object;
			this.id = id;
			this.tile = tile;
			this.action = action;
		}
	}
}
