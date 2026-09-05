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
import net.runelite.client.plugins.microbot.util.walker.transport.model.HotAirBalloonTransport;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

/** Cache-backed live adapter for non-blocking hot-air-balloon stages. */
public final class Rs2HotAirBalloonScene implements HotAirBalloonScene
{
	private static final Map<String, Integer> DESTINATION_BUTTONS = Map.of(
		"entrana", InterfaceID.ZepBalloonMap.BTN_ENT,
		"taverley", InterfaceID.ZepBalloonMap.BTN_TAV,
		"crafting guild", InterfaceID.ZepBalloonMap.BTN_CRAFT,
		"varrock", InterfaceID.ZepBalloonMap.BTN_VARR,
		"castle wars", InterfaceID.ZepBalloonMap.BTN_CAST,
		"grand tree", InterfaceID.ZepBalloonMap.BTN_GNO);

	@Override
	public HotAirBalloonTransport find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		HotAirBalloonTransport destination = destinationStage(transport);
		return destination == null ? objectStage(transport) : destination;
	}

	@Override
	public HotAirBalloonTransport observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		HotAirBalloonTransport destination = destinationStage(transport);
		if (destination != null)
		{
			return destination;
		}
		if (HotAirBalloonPolicy.isDestinationAction(pendingAction))
		{
			return null;
		}
		return objectStage(transport);
	}

	public static boolean interactObject(PlannedEdge edge, String expectedAction, int objectId)
	{
		Transport transport = findTransport(edge);
		Rs2TileObjectModel basket = transport == null ? null : findObject(transport);
		return basket != null && basket.getId() == objectId
			&& transport.getAction().equalsIgnoreCase(expectedAction)
			&& basket.click(transport.getAction());
	}

	public static boolean selectDestination(String destination)
	{
		Widget widget = destinationWidget(destination);
		return isVisible(widget) && Rs2Widget.clickWidget(widget);
	}

	private static HotAirBalloonTransport destinationStage(Transport transport)
	{
		Widget root = Rs2Widget.getWidget(InterfaceID.ZepBalloonMap.ROOT_RECT0);
		if (!isVisible(root))
		{
			return null;
		}
		Widget destination = destinationWidget(transport.getDisplayInfo());
		return stage(transport, transport.getOrigin(), isVisible(destination)
			? HotAirBalloonTransport.Stage.DESTINATION
			: HotAirBalloonTransport.Stage.DESTINATION_UNAVAILABLE);
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(HotAirBalloonPolicy::isEligible).findFirst().orElse(null);
	}

	private static HotAirBalloonTransport objectStage(Transport transport)
	{
		Rs2TileObjectModel basket = findObject(transport);
		return basket == null ? null : stage(transport, basket.getId(),
			basket.getWorldLocation(), HotAirBalloonTransport.Stage.OBJECT);
	}

	private static Rs2TileObjectModel findObject(Transport transport)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			Microbot.getRs2TileObjectCache().query()
				.withIds(HotAirBalloonPolicy.basketObjectIds())
				.within(transport.getOrigin(), HotAirBalloonPolicy.LIVE_OBJECT_ORIGIN_TOLERANCE)
				.toList().stream().filter(object -> hasAction(object, transport.getAction()))
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

	private static HotAirBalloonTransport stage(Transport transport, WorldPoint objectTile,
		HotAirBalloonTransport.Stage stage)
	{
		Rs2TileObjectModel basket = findObject(transport);
		int objectId = basket == null ? transport.getObjectId() : basket.getId();
		WorldPoint liveTile = basket == null ? objectTile : basket.getWorldLocation();
		return stage(transport, objectId, liveTile, stage);
	}

	private static HotAirBalloonTransport stage(Transport transport, int objectId,
		WorldPoint objectTile, HotAirBalloonTransport.Stage stage)
	{
		return new HotAirBalloonTransport(transport.getOrigin(), transport.getDestination(),
			objectId, transport.getAction(), transport.getDisplayInfo(), objectTile, stage);
	}

	private static Widget destinationWidget(String destination)
	{
		Integer packedId = DESTINATION_BUTTONS.get(normalize(destination));
		return packedId == null ? null : Rs2Widget.getWidget(packedId);
	}

	private static boolean isVisible(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget != null && !widget.isHidden()).orElse(false);
	}

	private static String normalize(String value)
	{
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", " ");
	}
}
