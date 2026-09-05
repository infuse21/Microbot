package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.WildernessDitch;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed resolver and warning dispatcher for wilderness ditch crossings. */
public final class Rs2WildernessDitchScene implements WildernessDitchScene
{
	private static final int WARNING_GROUP = 475;
	private static final int WARNING_CONFIRM_CHILD = 11;

	@Override
	public WildernessDitch find(PlannedEdge edge)
	{
		for (Transport transport : findTransports(edge))
		{
			WildernessDitch ditch = objectStage(transport);
			if (ditch != null)
			{
				return ditch;
			}
		}
		return null;
	}

	@Override
	public WildernessDitch observe(PlannedEdge edge, String pendingAction,
		int catalogObjectId)
	{
		Transport transport = findTransports(edge).stream()
			.filter(candidate -> candidate.getObjectId() == catalogObjectId)
			.findFirst().orElse(null);
		if (transport == null)
		{
			return null;
		}
		if (warningVisible())
		{
			return new WildernessDitch(null, transport.getOrigin(),
				transport.getObjectId(), WildernessDitchPolicy.WARNING_ACTION,
				transport.getOrigin(), transport.getDestination(),
				WildernessDitch.Stage.WARNING);
		}
		if (WildernessDitchPolicy.WARNING_ACTION.equals(pendingAction))
		{
			return null;
		}
		return objectStage(transport);
	}

	public static boolean interactObject(PlannedEdge edge, String action, int catalogObjectId)
	{
		WildernessDitch ditch = new Rs2WildernessDitchScene().find(edge);
		return ditch != null && ditch.getCatalogObjectId() == catalogObjectId
			&& ditch.getAction().equalsIgnoreCase(action) && ditch.getObject() != null
			&& ditch.getObject().click(action);
	}

	public static boolean confirmWarning()
	{
		return warningVisible() && Rs2Widget.clickWidget(WARNING_GROUP,
			WARNING_CONFIRM_CHILD);
	}

	private static List<Transport> findTransports(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return java.util.Collections.emptyList();
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(WildernessDitchPolicy::isEligible)
			.collect(Collectors.toList());
	}

	private static WildernessDitch objectStage(Transport transport)
	{
		Rs2TileObjectModel object = Microbot.getRs2TileObjectCache().query()
			.withId(transport.getObjectId()).toList().stream()
			.filter(candidate -> liveMatch(transport, candidate))
			.min(java.util.Comparator.comparingInt(candidate -> candidate.getWorldLocation()
				.distanceTo2D(transport.getOrigin())))
			.orElse(null);
		return object == null ? null : new WildernessDitch(object,
			object.getWorldLocation(), transport.getObjectId(), transport.getAction(),
			transport.getOrigin(), transport.getDestination(), WildernessDitch.Stage.OBJECT);
	}

	private static boolean liveMatch(Transport transport, Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition != null && WildernessDitchPolicy.isLiveObjectMatch(transport,
			object.getId(), composition.getName(), actions(composition),
			object.getWorldLocation());
	}

	private static List<String> actions(ObjectComposition composition)
	{
		String[] actions = composition.getActions();
		return actions == null ? java.util.Collections.emptyList() : Arrays.stream(actions)
			.filter(Objects::nonNull).collect(Collectors.toList());
	}

	private static boolean warningVisible()
	{
		return Rs2Widget.isWidgetVisible(WARNING_GROUP, WARNING_CONFIRM_CHILD);
	}
}
