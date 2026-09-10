package net.runelite.client.plugins.microbot.util.walker.transport;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.util.mouse.Mouse;
import net.runelite.client.plugins.microbot.util.poh.PohTeleports;
import net.runelite.client.plugins.microbot.util.poh.PohTransport;
import net.runelite.client.plugins.microbot.util.poh.data.MountedDigsite;
import net.runelite.client.plugins.microbot.util.poh.data.MountedXerics;
import net.runelite.client.plugins.microbot.util.poh.data.PohTeleport;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteEdge;
import net.runelite.client.plugins.microbot.util.walker.navigation.RouteInteraction;
import net.runelite.client.plugins.microbot.util.walker.navigation.RoutePlan;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2SceneLocation;
import net.runelite.client.plugins.microbot.util.walker.transport.model.TeleportationPortal;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PohMountedMenuSceneTest
{
	@Test
	public void mountedMenusAdvanceFromObjectToExactUnlockedDestination()
	{
		ClientThread thread = immediateClientThread();
		net.runelite.api.Client client = mock(net.runelite.api.Client.class);
		net.runelite.api.Player actor = mock(net.runelite.api.Player.class);
		net.runelite.api.WorldView world = mock(net.runelite.api.WorldView.class);
		when(client.getLocalPlayer()).thenReturn(actor);
		when(actor.getWorldView()).thenReturn(world);
		when(world.getId()).thenReturn(1);
		Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		when(object.getWorldView()).thenReturn(world);
		when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
		ObjectComposition definition = mock(ObjectComposition.class);
		when(object.getObjectComposition()).thenReturn(definition);
		WorldPoint anchor = new WorldPoint(1859, 7051, 0);
		WorldPoint room = new WorldPoint(1900, 7100, 0);
		Mouse mouse = mock(Mouse.class);
		when(mouse.click(any(Rectangle.class))).thenReturn(mouse);
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
			MockedStatic<PohTeleports> house = mockStatic(PohTeleports.class);
			MockedStatic<Rs2SceneLocation> locations = mockStatic(Rs2SceneLocation.class))
		{
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
			microbot.when(Microbot::getMouse).thenReturn(mouse);
			house.when(PohTeleports::isInHouse).thenReturn(true);
			locations.when(() -> Rs2SceneLocation.templateLocation(object)).thenReturn(room);
			List<PohTeleport> routes = new java.util.ArrayList<>(
				java.util.Arrays.asList(MountedDigsite.values()));
			routes.addAll(java.util.Arrays.asList(MountedXerics.values()));
			assertEquals(7, routes.size());
			for (PohTeleport teleport : routes)
			{
				PohTransport row = new PohTransport(anchor, teleport);
				PlannedEdge edge = new PlannedEdge(anchor, row.getDestination());
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));
				when(object.getId()).thenReturn(row.getObjectId());
				when(definition.getId()).thenReturn(row.getObjectId());
				when(definition.getActions()).thenReturn(new String[]{"Teleport menu", "Remove"});
				when(client.getWidget(InterfaceID.MENU, 3)).thenReturn(null);
				when(object.click("Teleport menu")).thenReturn(true);
				TeleportationPortal open = new Rs2TeleportationPortalScene().find(edge);
				assertNotNull(teleport.name(), open);
				assertEquals(TeleportationPortalPolicy.POH_OPEN_MENU_ACTION, open.getAction());
				assertTrue(Rs2TeleportationPortalScene.interactObject(edge,
					open.getAction(), row.getObjectId()));

				Widget root = menu(row.getAction(), false);
				when(client.getWidget(InterfaceID.MENU, 3)).thenReturn(root);
				TeleportationPortal select = new Rs2TeleportationPortalScene().find(edge);
				assertNotNull(select);
				assertEquals(TeleportationPortalPolicy.destinationAction(row.getAction()),
					select.getAction());
				assertTrue(Rs2TeleportationPortalScene.interactObject(edge,
					select.getAction(), row.getObjectId()));
				verify(mouse).click(any(Rectangle.class));
				clearInvocations(mouse);

				Widget locked = menu(row.getAction(), true);
				when(client.getWidget(InterfaceID.MENU, 3)).thenReturn(locked);
				TeleportationPortal unavailable = new Rs2TeleportationPortalScene().find(edge);
				assertEquals(TeleportationPortalPolicy.POH_DESTINATION_UNAVAILABLE,
					unavailable.getAction());
				RoutePlan plan = new RoutePlan(1, 1, anchor, Set.of(row.getDestination()),
					List.of(anchor, row.getDestination()), List.of(anchor, row.getDestination()),
					true, List.of(new RouteEdge(0, anchor, row.getDestination(),
						RouteEdge.Kind.TELEPORTATION_PORTAL)));
				RouteInteraction pending = new TeleportationPortalRouteScanner().scan(
					plan, 0, 1, room, new Rs2TeleportationPortalScene(), 13);
				assertEquals(RouteInteraction.Status.UNAVAILABLE, pending.getStatus());
				assertFalse(pending.isReady());

				when(client.getWidget(InterfaceID.MENU, 3)).thenReturn(null);
				when(definition.getId()).thenReturn(teleport instanceof MountedDigsite
					? ((MountedDigsite) teleport).getObjectId()
					: ((MountedXerics) teleport).getObjectId());
				when(definition.getActions()).thenReturn(new String[]{row.getAction(), "Remove"});
				TeleportationPortal direct = new Rs2TeleportationPortalScene().find(edge);
				assertNotNull(direct);
				assertEquals(row.getAction(), direct.getAction());
			}
		}
	}

	private static ClientThread immediateClientThread()
	{
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
			java.util.Optional.ofNullable(((java.util.concurrent.Callable<?>)
				call.getArgument(0)).call()));
		when(thread.invoke(any(java.util.function.Supplier.class))).thenAnswer(call ->
			((java.util.function.Supplier<?>) call.getArgument(0)).get());
		return thread;
	}

	private static Widget menu(String destination, boolean locked)
	{
		Widget root = mock(Widget.class);
		Widget child = mock(Widget.class);
		when(root.isHidden()).thenReturn(false);
		when(root.getText()).thenReturn("");
		when(root.getChildren()).thenReturn(new Widget[]{child});
		when(child.isHidden()).thenReturn(false);
		when(child.getText()).thenReturn(locked ? "<str>" + destination + "</str>"
			: destination);
		when(child.getBounds()).thenReturn(new Rectangle(10, 10, 20, 20));
		return root;
	}
}
