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
import java.util.Comparator;
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

	public static boolean interactObject(PlannedEdge edge, String expectedAction, int objectId)
	{
		Transport transport = findTransport(edge);
		Rs2TileObjectModel tree = transport == null ? null : findTreeObject(transport);
		return tree != null && tree.getId() == objectId
			&& transport.getAction().equalsIgnoreCase(expectedAction)
			&& tree.click(transport.getAction());
	}

	public static boolean selectDestination(String destination)
	{
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
		Rs2TileObjectModel tree = findTreeObject(transport);
		if (tree != null)
		{
			return stage(transport, tree.getWorldLocation(), SpiritTree.Stage.OBJECT);
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

	private static Rs2TileObjectModel findTreeObject(Transport transport)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			Microbot.getRs2TileObjectCache().query().withId(transport.getObjectId())
				.within(transport.getOrigin(), OBJECT_SEARCH_RADIUS).toList().stream()
				.filter(object -> hasAction(object, transport.getAction()))
				.min(Comparator.comparingInt(object -> object.getWorldLocation()
					.distanceTo2D(transport.getOrigin())))
				.orElse(null)).orElse(null);
	}

	private static boolean hasAction(Rs2TileObjectModel object, String expected)
	{
		String[] actions = object.getObjectComposition() == null
			? null : object.getObjectComposition().getActions();
		return actions != null && Arrays.stream(actions).filter(java.util.Objects::nonNull)
			.anyMatch(action -> action.equalsIgnoreCase(expected));
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
}
