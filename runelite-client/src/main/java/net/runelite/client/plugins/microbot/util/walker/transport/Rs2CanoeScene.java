package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CanoeTransport;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed canoe object resolver and exact staged UI dispatcher. */
public final class Rs2CanoeScene implements CanoeScene
{
	@Override
	public CanoeTransport find(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		try
		{
			return Microbot.getClientThread().runOnClientThreadOptional(
				() -> findOnClientThread(edge)).orElse(null);
		}
		catch (RuntimeException ex)
		{
			if (Thread.currentThread().isInterrupted()
				|| Rs2SceneLocation.clientThreadUnavailable(ex))
			{
				return null;
			}
			throw ex;
		}
	}

	private static CanoeTransport findOnClientThread(PlannedEdge edge)
	{
		for (Transport transport : findTransports(edge))
		{
			CanoeTransport canoe = resolveStage(transport, null);
			if (canoe != null)
			{
				return canoe;
			}
		}
		return null;
	}

	@Override
	public CanoeTransport observe(PlannedEdge edge, String pendingAction,
		int catalogObjectId)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		try
		{
			return Microbot.getClientThread().runOnClientThreadOptional(() ->
				observeOnClientThread(edge, pendingAction, catalogObjectId)).orElse(null);
		}
		catch (RuntimeException ex)
		{
			if (Thread.currentThread().isInterrupted()
				|| Rs2SceneLocation.clientThreadUnavailable(ex))
			{
				return null;
			}
			throw ex;
		}
	}

	private static CanoeTransport observeOnClientThread(PlannedEdge edge,
		String pendingAction, int catalogObjectId)
	{
		Transport transport = findTransports(edge).stream()
			.filter(candidate -> candidate.getObjectId() == catalogObjectId)
			.findFirst().orElse(null);
		return transport == null ? null : resolveStage(transport, pendingAction);
	}

	public static boolean interactObject(PlannedEdge edge, String action, int catalogObjectId)
	{
		CanoeTransport canoe = new Rs2CanoeScene().find(edge);
		return canoe != null && canoe.getStage() == CanoeTransport.Stage.OBJECT
			&& canoe.getCatalogObjectId() == catalogObjectId
			&& canoe.getAction().equalsIgnoreCase(action) && canoe.getObject() != null
			&& canoe.getObject().click(action);
	}

	public static boolean dispatchStage(String action)
	{
		if (CanoePolicy.isShapeAction(action))
		{
			return clickShape(action.substring(CanoePolicy.SHAPE_ACTION_PREFIX.length()));
		}
		if (CanoePolicy.isDestinationAction(action))
		{
			return clickDestination(action.substring(
				CanoePolicy.DESTINATION_ACTION_PREFIX.length()));
		}
		if (CanoePolicy.WARNING_CONTINUE_ACTION.equals(action)
			&& warningVisible())
		{
			Rs2Dialogue.clickContinue();
			return true;
		}
		if (CanoePolicy.WARNING_CONFIRM_ACTION.equals(action))
		{
			return confirmVisible() && Rs2Dialogue.clickOption(CanoePolicy.WARNING_OPTION, true);
		}
		if (CanoePolicy.isArrivalAction(action)
			&& action.equals(CanoePolicy.arrivalAction(Rs2Dialogue.getDialogueText()))
			&& Rs2Dialogue.hasContinue())
		{
			Rs2Dialogue.clickContinue();
			return true;
		}
		return false;
	}

	private static CanoeTransport resolveStage(Transport transport, String pendingAction)
	{
		CanoeTransport warning = warningStage(transport);
		if (warning != null)
		{
			return warning;
		}
		CanoeTransport arrival = arrivalStage(transport);
		if (arrival != null)
		{
			return arrival;
		}
		CanoeTransport shape = shapeStage(transport);
		if (shape != null)
		{
			return shape;
		}
		CanoeTransport destination = destinationStage(transport);
		if (destination != null)
		{
			return destination;
		}
		CanoeTransport object = objectStage(transport);
		if (object == null || CanoePolicy.isDestinationAction(pendingAction)
			|| CanoePolicy.WARNING_CONTINUE_ACTION.equals(pendingAction)
			|| CanoePolicy.WARNING_CONFIRM_ACTION.equals(pendingAction)
			|| CanoePolicy.isArrivalAction(pendingAction))
		{
			return null;
		}
		if (CanoePolicy.isShapeAction(pendingAction)
			&& "Shape-Canoe".equalsIgnoreCase(object.getAction()))
		{
			return null;
		}
		return object;
	}

	private static CanoeTransport shapeStage(Transport transport)
	{
		if (!Rs2Widget.isWidgetVisible(InterfaceID.Canoeing.CONTENT))
		{
			return null;
		}
		String shape = CanoePolicy.shapeForLevel(
			Rs2Player.getRealSkillLevel(Skill.WOODCUTTING));
		if (shape == null)
		{
			return stage(transport, "canoe-shape-unavailable",
				CanoeTransport.Stage.SHAPE_UNAVAILABLE);
		}
		String action = CanoePolicy.shapeAction(shape);
		return stage(transport, action, findShapeWidget(shape) == null
			? CanoeTransport.Stage.SHAPE_UNAVAILABLE : CanoeTransport.Stage.SHAPE);
	}

	private static CanoeTransport destinationStage(Transport transport)
	{
		if (!Rs2Widget.isWidgetVisible(InterfaceID.CanoeMapLum.MAIN_MAP))
		{
			return null;
		}
		String action = CanoePolicy.destinationAction(transport.getDisplayInfo());
		return stage(transport, action, findDestinationWidget(transport.getDisplayInfo()) == null
			? CanoeTransport.Stage.DESTINATION_UNAVAILABLE
			: CanoeTransport.Stage.DESTINATION);
	}

	private static CanoeTransport warningStage(Transport transport)
	{
		if (confirmVisible())
		{
			return stage(transport, CanoePolicy.WARNING_CONFIRM_ACTION,
				CanoeTransport.Stage.CONFIRM);
		}
		if (warningVisible())
		{
			return stage(transport, CanoePolicy.WARNING_CONTINUE_ACTION,
				CanoeTransport.Stage.WARNING);
		}
		return null;
	}

	private static CanoeTransport arrivalStage(Transport transport)
	{
		if (!Rs2Dialogue.hasContinue())
		{
			return null;
		}
		String action = CanoePolicy.arrivalAction(Rs2Dialogue.getDialogueText());
		return action == null ? null : stage(transport, action, CanoeTransport.Stage.ARRIVAL);
	}

	private static CanoeTransport objectStage(Transport transport)
	{
		LiveObject object = Microbot.getRs2TileObjectCache().query()
			.withId(transport.getObjectId()).toList().stream()
			.map(Rs2CanoeScene::snapshot)
			.filter(Objects::nonNull)
			.filter(candidate -> liveMatch(transport, candidate))
			.min(Comparator.comparingInt(candidate -> candidate.location
				.distanceTo2D(transport.getOrigin())))
			.orElse(null);
		if (object == null)
		{
			return null;
		}
		String action = actions(object.actions).stream()
			.filter(CanoePolicy::isObjectAction).findFirst().orElse(null);
		return action == null ? null : new CanoeTransport(object.object, object.location,
			transport.getObjectId(), action, transport.getOrigin(), transport.getDestination(),
			CanoeTransport.Stage.OBJECT);
	}

	private static boolean liveMatch(Transport transport, LiveObject object)
	{
		return object.name != null && CanoePolicy.isLiveObjectMatch(transport,
			object.id, object.name, actions(object.actions), object.location);
	}

	private static List<String> actions(String[] actions)
	{
		if (actions == null)
		{
			return Collections.emptyList();
		}
		return Arrays.stream(actions).filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private static LiveObject snapshot(Rs2TileObjectModel object)
	{
		if (object == null)
		{
			return null;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			ObjectComposition composition = object.getObjectComposition();
			return new LiveObject(object, object.getId(), object.getWorldLocation(),
				composition == null ? null : composition.getName(),
				composition == null ? null : composition.getActions());
		}).orElse(null);
	}

	private static final class LiveObject
	{
		private final Rs2TileObjectModel object;
		private final int id;
		private final net.runelite.api.coords.WorldPoint location;
		private final String name;
		private final String[] actions;

		private LiveObject(Rs2TileObjectModel object, int id,
			net.runelite.api.coords.WorldPoint location, String name, String[] actions)
		{
			this.object = object;
			this.id = id;
			this.location = location;
			this.name = name;
			this.actions = actions;
		}
	}

	private static boolean clickShape(String shape)
	{
		Widget widget = findShapeWidget(shape);
		return widget != null && Rs2Widget.clickWidget(widget);
	}

	private static Widget findShapeWidget(String shape)
	{
		Widget content = Rs2Widget.getWidget(InterfaceID.Canoeing.CONTENT);
		return content == null ? null : Rs2Widget.findWidget("Make " + shape,
			Collections.singletonList(content), false);
	}

	private static boolean clickDestination(String destination)
	{
		Widget widget = findDestinationWidget(destination);
		return widget != null && Rs2Widget.clickWidget(widget);
	}

	private static Widget findDestinationWidget(String destination)
	{
		Widget destinations = Rs2Widget.getWidget(InterfaceID.CanoeMapLum.DESTINATIONS);
		return destinations == null ? null : Rs2Widget.findWidget("Travel to " + destination,
			Collections.singletonList(destinations), false);
	}

	private static boolean warningVisible()
	{
		return Rs2Dialogue.hasContinue()
			&& Rs2Dialogue.hasDialogueText(CanoePolicy.WARNING_TEXT, true);
	}

	private static boolean confirmVisible()
	{
		return Rs2Dialogue.hasQuestion(CanoePolicy.WARNING_QUESTION, true)
			&& Rs2Dialogue.hasDialogueOption(CanoePolicy.WARNING_OPTION, true);
	}

	private static CanoeTransport stage(Transport transport, String action,
		CanoeTransport.Stage stage)
	{
		return new CanoeTransport(null, transport.getOrigin(), transport.getObjectId(),
			action, transport.getOrigin(), transport.getDestination(), stage);
	}

	private static List<Transport> findTransports(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return Collections.emptyList();
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(CanoePolicy::isEligible).collect(Collectors.toList());
	}
}
