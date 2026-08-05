package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.MenuAction;
import net.runelite.api.NPCComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.CharterShip;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Resolves the NPC, destination-widget, and confirmation stages of a charter route. */
public final class Rs2CharterShipScene implements CharterShipScene
{
	private static final int CHARTER_GROUP = 885;
	private static final int CHARTER_ROOT = 4;

	@Override
	public CharterShip find(PlannedEdge edge)
	{
		Transport transport = findTransport(edge);
		return transport == null ? null : npcStage(transport);
	}

	@Override
	public CharterShip observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		if (Rs2Dialogue.hasSelectAnOption())
		{
			return stage(transport, transport.getOrigin(), CharterShip.Stage.CONFIRM);
		}
		if (findDestinationWidget(transport.getDisplayInfo()) != null)
		{
			return stage(transport, transport.getOrigin(), CharterShip.Stage.DESTINATION);
		}
		if (CharterShipPolicy.isDestinationAction(pendingAction)
			|| CharterShipPolicy.CONFIRM_ACTION.equals(pendingAction))
		{
			return null;
		}
		return npcStage(transport);
	}

	public static boolean selectDestination(String destination)
	{
		Widget widget = findDestinationWidget(destination);
		if (widget == null)
		{
			return false;
		}
		String option = firstAction(widget);
		if (option == null || option.trim().isEmpty())
		{
			option = destination;
		}
		NewMenuEntry menuEntry = new NewMenuEntry()
			.option(option)
			.target("")
			.identifier(1)
			.type(MenuAction.CC_OP)
			.param0(widget.getIndex())
			.param1(widget.getId())
			.forceLeftClick(false);
		Rectangle bounds = widget.getBounds();
		Microbot.doInvoke(menuEntry,
			bounds == null ? Rs2UiHelper.getDefaultRectangle() : bounds);
		return true;
	}

	public static boolean confirmTravel()
	{
		return Rs2Dialogue.hasSelectAnOption()
			&& Rs2Dialogue.clickOption("Yes", true);
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(CharterShipPolicy::isEligible).findFirst().orElse(null);
	}

	private static CharterShip npcStage(Transport transport)
	{
		Rs2NpcModel npc = findNpc(transport);
		if (npc == null)
		{
			return null;
		}
		return stage(transport, npc.getWorldLocation(), CharterShip.Stage.NPC);
	}

	public static Rs2NpcModel findNpc(Transport transport)
	{
		if (!CharterShipPolicy.isEligible(transport))
		{
			return null;
		}
		return Rs2Npc.getNpcs(npc -> CharterShipPolicy.isLiveNpcMatch(transport,
			npc.getName(), actions(npc), npc.getWorldLocation()))
			.min(Comparator
				.comparingInt((Rs2NpcModel npc) -> npc.getId() == transport.getObjectId() ? 0 : 1)
				.thenComparingInt(npc -> npc.getWorldLocation()
					.distanceTo2D(transport.getOrigin())))
			.orElse(null);
	}

	private static java.util.List<String> actions(Rs2NpcModel npc)
	{
		return Stream.of(npc.getComposition(), npc.getTransformedComposition())
			.filter(java.util.Objects::nonNull)
			.map(NPCComposition::getActions)
			.filter(java.util.Objects::nonNull)
			.flatMap(Arrays::stream)
			.filter(java.util.Objects::nonNull)
			.collect(Collectors.toList());
	}

	private static CharterShip stage(Transport transport, net.runelite.api.coords.WorldPoint tile,
		CharterShip.Stage stage)
	{
		return new CharterShip(transport.getOrigin(), transport.getDestination(),
			transport.getObjectId(), transport.getName(), transport.getAction(),
			transport.getDisplayInfo(), tile, stage);
	}

	private static Widget findDestinationWidget(String destination)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget root = Microbot.getClient().getWidget(CHARTER_GROUP, CHARTER_ROOT);
			if (root == null || root.isHidden())
			{
				return null;
			}
			Widget text = findDestinationText(root, destination);
			if (text == null)
			{
				return null;
			}
			Widget clickable = findClickableParent(text, root);
			return clickable == null ? text : clickable;
		}).orElse(null);
	}

	private static Widget findDestinationText(Widget widget, String destination)
	{
		if (widget == null || widget.isHidden())
		{
			return null;
		}
		if (matchesDestination(widget, destination))
		{
			return widget;
		}
		Widget found = findDestinationText(widget.getStaticChildren(), destination);
		if (found == null)
		{
			found = findDestinationText(widget.getDynamicChildren(), destination);
		}
		return found == null
			? findDestinationText(widget.getNestedChildren(), destination) : found;
	}

	private static Widget findDestinationText(Widget[] widgets, String destination)
	{
		if (widgets == null)
		{
			return null;
		}
		for (Widget widget : widgets)
		{
			Widget found = findDestinationText(widget, destination);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	private static boolean matchesDestination(Widget widget, String destination)
	{
		String needle = normalize(destination);
		if (needle.isEmpty())
		{
			return false;
		}
		if (normalize(widget.getText()).contains(needle)
			|| normalize(widget.getName()).contains(needle))
		{
			return true;
		}
		String[] actions = widget.getActions();
		return actions != null && Arrays.stream(actions)
			.filter(java.util.Objects::nonNull)
			.map(Rs2CharterShipScene::normalize)
			.anyMatch(action -> action.contains(needle));
	}

	private static Widget findClickableParent(Widget widget, Widget root)
	{
		Widget current = widget;
		while (current != null)
		{
			if (firstAction(current) != null)
			{
				return current;
			}
			if (current == root)
			{
				return null;
			}
			current = current.getParent();
		}
		return null;
	}

	private static String firstAction(Widget widget)
	{
		String[] actions = widget.getActions();
		return actions == null ? null : Arrays.stream(actions)
			.filter(action -> action != null && !action.isEmpty())
			.findFirst().orElse(null);
	}

	private static String normalize(String text)
	{
		return text == null ? "" : Rs2UiHelper.stripTagsToSpace(text).trim()
			.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}
}
