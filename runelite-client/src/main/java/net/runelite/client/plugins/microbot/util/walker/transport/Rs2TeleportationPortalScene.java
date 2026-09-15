package net.runelite.client.plugins.microbot.util.walker.transport;

import net.runelite.api.ObjectComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.shortestpath.TransportEdgeMatcher;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.poh.data.PohPortal;
import net.runelite.client.plugins.microbot.util.poh.data.NexusPortal;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Collections;
import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Cache-backed, non-blocking adapter for deterministic teleportation portals. */
public final class Rs2TeleportationPortalScene implements TeleportationPortalScene
{
	@Override
	public TeleportationPortal find(PlannedEdge edge)
	{
		for (Transport transport : findTransports(edge))
		{
			TeleportationPortal portal = portal(transport);
			if (portal != null)
			{
				return portal;
			}
		}
		return null;
	}

	@Override
	public TeleportationPortal observe(PlannedEdge edge, int catalogObjectId)
	{
		for (Transport transport : findTransports(edge))
		{
			if (transport.getObjectId() == catalogObjectId)
			{
				return portal(transport);
			}
		}
		return null;
	}

	public static boolean interactObject(PlannedEdge edge, String expectedAction,
		int catalogObjectId)
	{
		if (TeleportationPortalPolicy.POH_DESTINATION_UNAVAILABLE.equals(expectedAction))
		{
			return false;
		}
		boolean confirmation = expectedAction != null
			&& expectedAction.startsWith(TeleportationPortalPolicy.POH_CONFIRM_NEXUS_PREFIX);
		if (TeleportationPortalPolicy.isDestinationAction(expectedAction) || confirmation)
		{
			String destination = confirmation
				? expectedAction.substring(TeleportationPortalPolicy.POH_CONFIRM_NEXUS_PREFIX.length())
				: TeleportationPortalPolicy.destinationName(expectedAction);
			for (Transport transport : findTransports(edge))
			{
				if (transport.getObjectId() != catalogObjectId
					|| !TeleportationPortalPolicy.isMenuPoh(transport)
					|| !destination.equals(TeleportationPortalPolicy.pohDestinationName(transport)))
				{
					continue;
				}
				TeleportationPortal current = portal(transport);
				return current != null && expectedAction.equals(current.getAction())
					&& clickDestination(transport, destination);
			}
			return false;
		}
		TeleportationPortal portal = new Rs2TeleportationPortalScene().find(edge);
		return portal != null && portal.getCatalogObjectId() == catalogObjectId
			&& portal.getAction().equalsIgnoreCase(expectedAction)
			&& portal.getObject() != null && portal.getObject().click(
				TeleportationPortalPolicy.POH_OPEN_MENU_ACTION.equals(portal.getAction())
					? "Teleport menu" : portal.getAction());
	}

	private static List<Transport> findTransports(PlannedEdge edge)
	{
		if (edge == null || edge.from() == null || edge.to() == null)
		{
			return Collections.emptyList();
		}
		return TransportEdgeMatcher.find(Rs2PathApi.getTransports(), edge.from(), edge.to())
			.stream().filter(TeleportationPortalPolicy::isEligible)
			.collect(Collectors.toList());
	}

	private static TeleportationPortal portal(Transport transport)
	{
		boolean menuPoh = TeleportationPortalPolicy.isMenuPoh(transport);
		String destination = menuPoh
			? TeleportationPortalPolicy.pohDestinationName(transport) : null;
		MenuDestination menu = menuPoh ? menuDestination(transport, destination)
			: MenuDestination.HIDDEN;
		PortalObject object = Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			PohPortal chamber = TeleportationPortalPolicy.chamberPortal(transport);
			boolean directPoh = TeleportationPortalPolicy.isDirectPoh(transport);
			boolean changedLanding = TeleportationPortalPolicy.pohTeleport(transport)
				== net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox.FARMING_GUILD
				&& !transport.getDestination().equals(
					net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox.FARMING_GUILD.getDestination());
			changedLanding |= TeleportationPortalPolicy.pohTeleport(transport) == NexusPortal.RESPAWN
				&& !transport.getDestination().equals(NexusPortal.RESPAWN.getDestination());
			if (directPoh && !PohTeleports.isInHouse())
			{
				return null;
			}
			PortalObject best = null;
			int bestDistance = Integer.MAX_VALUE;
			int[] ids = directPoh ? TeleportationPortalPolicy.pohObjectIds(transport)
				: new int[]{transport.getObjectId()};
			var query = Microbot.getRs2TileObjectCache().query();
			if (directPoh)
			{
				query.fromWorldView();
			}
			else
			{
				query.withIds(ids);
			}
			for (Rs2TileObjectModel candidate : query.toList())
			{
				ObjectComposition composition = candidate.getObjectComposition();
				int liveId = candidate.getId();
				int activeId = composition == null ? -1 : composition.getId();
				if (directPoh && (composition == null
					|| Arrays.stream(ids).noneMatch(id -> id == liveId || id == activeId)
						&& !(chamber == PohPortal.GRAND_EXCHANGE && Arrays.asList(PohPortal.VARROCK.getObjectIds())
							.contains(candidate.getId()))))
				{
					continue;
				}
				String[] rawActions = composition == null ? null : composition.getActions();
				List<String> actions = rawActions == null ? Collections.emptyList()
					: Arrays.stream(rawActions).filter(Objects::nonNull)
						.collect(Collectors.toList());
				net.runelite.api.coords.WorldPoint tile = directPoh ? Rs2SceneLocation.templateLocation(candidate)
					: candidate.getWorldLocation();
				String action;
				if (menuPoh)
				{
					int destinationId = TeleportationPortalPolicy.pohDestinationObjectId(transport);
					String directAction = liveId == destinationId || activeId == destinationId
						? TeleportationPortalPolicy.exactAction(actions, destination) : null;
					if (directAction != null)
					{
						action = directAction;
					}
					else
					{
						if (menu.visible)
						{
								action = menu.available
									? menu.confirmation ? TeleportationPortalPolicy.POH_CONFIRM_NEXUS_PREFIX + destination
										: TeleportationPortalPolicy.destinationAction(destination)
								: TeleportationPortalPolicy.POH_DESTINATION_UNAVAILABLE;
						}
						else
						{
							action = TeleportationPortalPolicy.exactAction(actions,
								"Teleport menu") == null ? null
								: TeleportationPortalPolicy.POH_OPEN_MENU_ACTION;
						}
					}
				}
				else
				{
					action = TeleportationPortalPolicy.exactAction(actions,
						transport.getAction());
				}
				if (composition == null || tile == null || action == null
					|| !directPoh && !TeleportationPortalPolicy.isLiveObjectMatch(transport,
						candidate.getId(), composition.getName(), actions, tile))
				{
					continue;
				}
				if (changedLanding)
				{
					action = TeleportationPortalPolicy.POH_DESTINATION_UNAVAILABLE;
				}
				int distance = tile.distanceTo2D(transport.getOrigin());
				if (distance < bestDistance)
				{
					best = new PortalObject(candidate, tile, action);
					bestDistance = distance;
				}
			}
			return best;
		}).orElse(null);
		return object == null ? null : new TeleportationPortal(object.object, object.tile,
			transport.getObjectId(), object.action, transport.getOrigin(),
			transport.getDestination());
	}

	private static MenuDestination menuDestination(Transport transport, String destination)
	{
		return Microbot.getClientThread().runOnClientThreadOptional(() ->
		{
			boolean jewellery = TeleportationPortalPolicy.pohTeleport(transport)
				instanceof net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox;
			boolean nexus = TeleportationPortalPolicy.pohTeleport(transport) instanceof NexusPortal;
			if (nexus && (TeleportationPortalPolicy.pohTeleport(transport) == NexusPortal.ANNAKARL
				|| TeleportationPortalPolicy.pohTeleport(transport) == NexusPortal.GHORROCK
				|| TeleportationPortalPolicy.pohTeleport(transport) == NexusPortal.CARRALLANGER
				|| TeleportationPortalPolicy.pohTeleport(transport) == NexusPortal.DAREEYAK
				|| TeleportationPortalPolicy.pohTeleport(transport) == NexusPortal.ICE_PLATEAU))
			{
				Widget warning = Microbot.getClient().getWidget(475, 11);
				if (warning != null && !warning.isHidden())
				{
					return new MenuDestination(true, true, warning.getBounds(), null, true);
				}
			}
			Widget root = nexus ? Microbot.getClient().getWidget(InterfaceID.TELENEXUS_TELEPORT, 0)
				: jewellery ? Microbot.getClient().getWidget(InterfaceID.POH_JEWELLERY_BOX, 0)
				: Microbot.getClient().getWidget(InterfaceID.MENU, 3);
			if (root == null || root.isHidden() || destination == null)
			{
				return MenuDestination.HIDDEN;
			}
			ArrayDeque<Widget> pending = new ArrayDeque<>();
			pending.push(root);
			while (!pending.isEmpty())
			{
				Widget current = pending.pop();
				String text = current.getText();
				String hotkey = nexus ? nexusHotkey(text, destination) : null;
				if (nexus ? hotkey != null : TeleportationPortalPolicy.menuTextMatches(text, destination, jewellery))
				{
					return new MenuDestination(true,
						!text.toLowerCase(java.util.Locale.ROOT).contains("<str>"),
						current.getBounds(), hotkey, false);
				}
				Widget[][] groups = {current.getChildren(), current.getNestedChildren(),
					current.getDynamicChildren(), current.getStaticChildren()};
				for (Widget[] children : groups)
				{
					if (children == null) continue;
					for (Widget child : children)
					{
						if (child != null && !child.isHidden()) pending.push(child);
					}
				}
			}
			return new MenuDestination(true, false, null);
		}).orElse(MenuDestination.HIDDEN);
	}

	private static boolean clickDestination(Transport transport, String destination)
	{
		MenuDestination state = menuDestination(transport, destination);
		if (state.available && state.hotkey != null)
		{
			Rs2Keyboard.typeString(state.hotkey);
			return true;
		}
		Rectangle bounds = state.available ? state.bounds : null;
		if (bounds == null)
		{
			return false;
		}
		Microbot.getMouse().click(bounds);
		return true;
	}

	static String nexusHotkey(String text, String destination)
	{
		if (text == null || destination == null) return null;
		java.util.regex.Matcher key = java.util.regex.Pattern.compile(
			"^\\s*<col=ffffff>([0-9A-Za-z])</col>[.):]?\\s*(.*)$",
			java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
		return key.matches() && key.group(2).replaceAll("<[^>]+>", "").trim()
			.equalsIgnoreCase(destination) ? key.group(1) : null;
	}

	private static final class MenuDestination
	{
		private static final MenuDestination HIDDEN =
			new MenuDestination(false, false, null);
		private final boolean visible;
		private final boolean available;
		private final Rectangle bounds;
		private final String hotkey;
		private final boolean confirmation;

		private MenuDestination(boolean visible, boolean available, Rectangle bounds)
		{
			this(visible, available, bounds, null, false);
		}

		private MenuDestination(boolean visible, boolean available, Rectangle bounds,
			String hotkey, boolean confirmation)
		{
			this.visible = visible;
			this.available = available;
			this.bounds = bounds;
			this.hotkey = hotkey;
			this.confirmation = confirmation;
		}
	}

	private static final class PortalObject
	{
		private final Rs2TileObjectModel object;
		private final net.runelite.api.coords.WorldPoint tile;
		private final String action;

		private PortalObject(Rs2TileObjectModel object,
			net.runelite.api.coords.WorldPoint tile, String action)
		{
			this.object = object;
			this.tile = tile;
			this.action = action;
		}
	}
}
