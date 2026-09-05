package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationLever;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed resolver and exact dialogue dispatcher for teleportation levers. */
public final class Rs2TeleportationLeverScene implements TeleportationLeverScene
{
	@Override
	public TeleportationLever find(PlannedEdge edge)
	{
		for (Transport transport : findTransports(edge))
		{
			TeleportationLever lever = objectStage(transport);
			if (lever != null)
			{
				return lever;
			}
		}
		return null;
	}

	@Override
	public TeleportationLever observe(PlannedEdge edge, String pendingAction,
		int catalogObjectId)
	{
		Transport transport = findTransports(edge).stream()
			.filter(candidate -> candidate.getObjectId() == catalogObjectId)
			.findFirst().orElse(null);
		if (transport == null)
		{
			return null;
		}
		if (confirmVisible())
		{
			return stage(transport, TeleportationLeverPolicy.CONFIRM_ACTION,
				TeleportationLever.Stage.CONFIRM);
		}
		if (warningVisible())
		{
			return stage(transport, TeleportationLeverPolicy.WARNING_CONTINUE_ACTION,
				TeleportationLever.Stage.WARNING);
		}
		if (TeleportationLeverPolicy.isStageAction(pendingAction))
		{
			// A warning-stage command closes one interface before the next stage or
			// teleport appears. Keep the directed landing pending during that gap.
			return null;
		}
		return objectStage(transport);
	}

	public static boolean interactObject(PlannedEdge edge, String action, int catalogObjectId)
	{
		TeleportationLever lever = new Rs2TeleportationLeverScene().find(edge);
		return lever != null && lever.getCatalogObjectId() == catalogObjectId
			&& lever.getAction().equalsIgnoreCase(action) && lever.getObject() != null
			&& lever.getObject().click(action);
	}

	public static boolean continueWarning()
	{
		if (!warningVisible())
		{
			return false;
		}
		Rs2Dialogue.clickContinue();
		return true;
	}

	public static boolean confirmWarning()
	{
		return confirmVisible()
			&& Rs2Dialogue.clickOption(TeleportationLeverPolicy.CONFIRM_OPTION, true);
	}

	private static List<Transport> findTransports(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return java.util.Collections.emptyList();
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(TeleportationLeverPolicy::isEligible)
			.collect(Collectors.toList());
	}

	private static TeleportationLever objectStage(Transport transport)
	{
		Rs2TileObjectModel object = Microbot.getRs2TileObjectCache().query()
			.withId(transport.getObjectId()).toList().stream()
			.filter(candidate -> liveMatch(transport, candidate))
			.min(java.util.Comparator.comparingInt(candidate -> candidate.getWorldLocation()
				.distanceTo2D(transport.getOrigin())))
			.orElse(null);
		return object == null ? null : new TeleportationLever(object,
			object.getWorldLocation(), transport.getObjectId(), transport.getAction(),
			transport.getOrigin(), transport.getDestination(), TeleportationLever.Stage.OBJECT);
	}

	private static boolean liveMatch(Transport transport, Rs2TileObjectModel object)
	{
		ObjectComposition composition = object.getObjectComposition();
		return composition != null && TeleportationLeverPolicy.isLiveObjectMatch(transport,
			object.getId(), composition.getName(), actions(composition),
			object.getWorldLocation());
	}

	private static List<String> actions(ObjectComposition composition)
	{
		String[] actions = composition.getActions();
		return actions == null ? java.util.Collections.emptyList() : Arrays.stream(actions)
			.filter(Objects::nonNull).collect(Collectors.toList());
	}

	private static TeleportationLever stage(Transport transport, String action,
		TeleportationLever.Stage stage)
	{
		return new TeleportationLever(null, transport.getOrigin(), transport.getObjectId(),
			action, transport.getOrigin(), transport.getDestination(), stage);
	}

	private static boolean warningVisible()
	{
		return Rs2Dialogue.hasContinue() && Rs2Dialogue.hasDialogueText(
			TeleportationLeverPolicy.WARNING_TEXT, true);
	}

	private static boolean confirmVisible()
	{
		return Rs2Dialogue.hasQuestion(TeleportationLeverPolicy.CONFIRM_QUESTION, true)
			&& Rs2Dialogue.hasDialogueOption(TeleportationLeverPolicy.CONFIRM_OPTION, true);
	}
}
