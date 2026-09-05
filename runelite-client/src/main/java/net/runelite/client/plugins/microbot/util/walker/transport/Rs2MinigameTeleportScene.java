package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.menu.NewMenuEntry;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.transport.model.MinigameTeleport;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.Rectangle;

/** Non-blocking grouping-widget adapter for minigame teleports. */
public final class Rs2MinigameTeleportScene implements MinigameTeleportScene
{
	private static final int TAB_SWITCH_SCRIPT = 915;
	private static final int DROPDOWN_OPEN_SPRITE_ID = 773;

	@Override
	public MinigameTeleport find(PlannedEdge edge)
	{
		return observe(edge, null);
	}

	@Override
	public MinigameTeleport observe(PlannedEdge edge, String pendingAction)
	{
		Transport transport = findTransport(edge);
		if (transport == null)
		{
			return null;
		}
		String activity = MinigameTeleportPolicy.activityName(transport.getDisplayInfo());
		String destinationOption = MinigameTeleportPolicy.destinationOption(
			transport.getDisplayInfo());
		if (!destinationOption.isEmpty() && Rs2Dialogue.hasDialogueOption(
				destinationOption, false))
		{
			return stage(edge, transport, MinigameTeleport.Stage.SELECT_DESTINATION);
		}
		if (Rs2Dialogue.isInDialogue())
		{
			return stage(edge, transport, MinigameTeleport.Stage.UNAVAILABLE);
		}
		if (Rs2Tab.getCurrentTab() != InterfaceTab.CHAT)
		{
			return stage(edge, transport, MinigameTeleport.Stage.OPEN_GROUPING_TAB);
		}
		Widget grouping = Rs2Widget.getWidget(InterfaceID.Grouping.UNIVERSE);
		if (!visible(grouping))
		{
			return MinigameTeleportPolicy.isTerminalAction(pendingAction) ? null
				: stage(edge, transport, MinigameTeleport.Stage.OPEN_GROUPING);
		}
		if (MinigameTeleportPolicy.isTerminalAction(pendingAction))
		{
			return null;
		}
		Widget current = Rs2Widget.getWidget(InterfaceID.Grouping.CURRENTGAME);
		if (MinigameTeleportPolicy.activityMatches(activity, visibleText(current)))
		{
			return stage(edge, transport, MinigameTeleport.Stage.TELEPORT);
		}
		Widget choices = Rs2Widget.getWidget(InterfaceID.Grouping.DROPDOWN_CONTENTS);
		Widget arrow = Rs2Widget.getWidget(InterfaceID.Grouping.ARROW);
		if (!dropdownOpen(arrow))
		{
			return stage(edge, transport, MinigameTeleport.Stage.OPEN_DROPDOWN);
		}
		Widget destination = findActivityWidget(activity, choices);
		if (destination != null)
		{
			return stage(edge, transport, MinigameTeleport.Stage.SELECT_ACTIVITY);
		}
		return stage(edge, transport, hasActivityChoices(choices)
			? MinigameTeleport.Stage.UNAVAILABLE
			: MinigameTeleport.Stage.WAIT_FOR_ACTIVITY);
	}

	public static boolean dispatch(PlannedEdge edge, String action)
	{
		Transport transport = findTransport(edge);
		if (transport == null || action == null)
		{
			return false;
		}
		if (MinigameTeleportPolicy.OPEN_GROUPING_TAB_ACTION.equals(action))
		{
			return openGroupingTab();
		}
		if (MinigameTeleportPolicy.OPEN_GROUPING_ACTION.equals(action))
		{
			return click(InterfaceID.SideChannels.TAB_3);
		}
		if (MinigameTeleportPolicy.OPEN_DROPDOWN_ACTION.equals(action))
		{
			return click(InterfaceID.Grouping.ARROW);
		}
		if (MinigameTeleportPolicy.isSelectActivityAction(action))
		{
			String activity = action.substring(
				MinigameTeleportPolicy.SELECT_ACTIVITY_PREFIX.length());
			return selectActivity(activity);
		}
		if (MinigameTeleportPolicy.TELEPORT_ACTION.equals(action))
		{
			return click(InterfaceID.Grouping.TELEPORT);
		}
		if (MinigameTeleportPolicy.isSelectDestinationAction(action))
		{
			String destination = action.substring(
				MinigameTeleportPolicy.SELECT_DESTINATION_PREFIX.length());
			return Rs2Dialogue.clickOption(destination, false);
		}
		return false;
	}

	private static boolean openGroupingTab()
	{
		if (Rs2Tab.getCurrentTab() == InterfaceTab.CHAT)
		{
			return true;
		}
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Microbot.getClient().runScript(TAB_SWITCH_SCRIPT,
				InterfaceTab.CHAT.getVarcIntIndex());
			return true;
		}).orElse(false);
	}

	private static boolean selectActivity(String activity)
	{
		Widget choices = Rs2Widget.getWidget(InterfaceID.Grouping.DROPDOWN_CONTENTS);
		ActivityEntry destination = activityEntry(activity, choices);
		if (destination == null)
		{
			return false;
		}
		NewMenuEntry entry = new NewMenuEntry().option("Select").target("")
			.identifier(1).type(MenuAction.CC_OP).param0(destination.index)
			.param1(destination.parentId).forceLeftClick(false);
		Microbot.doInvoke(entry, new Rectangle(1, 1));
		return true;
	}

	private static ActivityEntry activityEntry(String activity, Widget choices)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			Widget destination = findActivityWidgetOnClientThread(activity, choices);
			return destination == null ? null
				: new ActivityEntry(destination.getIndex(), choices.getId());
		}).orElse(null);
	}

	private static Widget findActivityWidget(String activity, Widget choices)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			findActivityWidgetOnClientThread(activity, choices)).orElse(null);
	}

	private static Widget findActivityWidgetOnClientThread(String activity, Widget choices)
	{
		if (choices == null || choices.isHidden() || choices.getDynamicChildren() == null)
		{
			return null;
		}
		for (Widget choice : choices.getDynamicChildren())
		{
			if (choice != null && !choice.isHidden()
				&& MinigameTeleportPolicy.activityMatches(activity, choice.getText()))
			{
				return choice;
			}
		}
		return null;
	}

	private static boolean dropdownOpen(Widget arrow)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			arrow != null && !arrow.isHidden()
				&& arrow.getSpriteId() == DROPDOWN_OPEN_SPRITE_ID).orElse(false);
	}

	private static boolean hasActivityChoices(Widget choices)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			choices != null && choices.getDynamicChildren() != null
				&& choices.getDynamicChildren().length > 0).orElse(false);
	}

	private static boolean click(int componentId)
	{
		Widget widget = Rs2Widget.getWidget(componentId);
		return visible(widget) && Rs2Widget.clickWidget(widget);
	}

	private static boolean visible(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget != null && !widget.isHidden()).orElse(false);
	}

	private static String visibleText(Widget widget)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
			widget == null || widget.isHidden() ? "" : widget.getText()).orElse("");
	}

	private static Transport findTransport(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return null;
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(MinigameTeleportPolicy::isEligible).findFirst().orElse(null);
	}

	private static MinigameTeleport stage(PlannedEdge edge, Transport transport,
		MinigameTeleport.Stage stage)
	{
		return new MinigameTeleport(edge.from(), transport.getDestination(),
			transport.getDisplayInfo(),
			MinigameTeleportPolicy.activityName(transport.getDisplayInfo()),
			MinigameTeleportPolicy.destinationOption(transport.getDisplayInfo()), stage);
	}

	private static final class ActivityEntry
	{
		private final int index;
		private final int parentId;

		private ActivityEntry(int index, int parentId)
		{
			this.index = index;
			this.parentId = parentId;
		}
	}
}
