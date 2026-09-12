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
import net.runelite.client.plugins.microbot.util.poh.data.JewelleryBox;
import net.runelite.client.plugins.microbot.util.poh.data.JewelleryBoxType;
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
	public void jewelleryMenuLabelsMatchTheBoxRatherThanSharedItemNames()
	{
		Map<JewelleryBox, String> labels = Map.of(
			JewelleryBox.PVP_ARENA, "Emir's Arena",
			JewelleryBox.CASTLE_WARS, "Castle Wars Arena",
			JewelleryBox.BURTHORPE_GAMES_ROOM, "Burthorpe",
			JewelleryBox.BARBARIAN_ASSAULT, "Barbarian Outpost",
			JewelleryBox.TEARS_OF_GUTHIX, "Chasm of Tears",
			JewelleryBox.COOKING_GUILD, "Cooks' Guild",
			JewelleryBox.DONDAKAN, "Dondakan's Rock");
		for (Map.Entry<JewelleryBox, String> entry : labels.entrySet())
		{
			assertEquals(entry.getValue(), TeleportationPortalPolicy.pohDestinationName(
				new PohTransport(new WorldPoint(1859, 7051, 0), entry.getKey())));
			assertTrue(TeleportationPortalPolicy.menuTextMatches(
				"<col=ffffff>A.</col> " + entry.getValue(), entry.getValue(), true));
			assertFalse(TeleportationPortalPolicy.menuTextMatches(
				"A. " + entry.getValue() + " unrelated", entry.getValue(), true));
		}
		assertFalse(TeleportationPortalPolicy.menuTextMatches("A. Digsite", "Digsite", false));
	}

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
			MockedStatic<net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard> keyboard =
				mockStatic(net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard.class);
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
			routes.addAll(java.util.Arrays.asList(JewelleryBox.values()));
			routes.addAll(java.util.Arrays.asList(net.runelite.client.plugins.microbot.util.poh.data.NexusPortal.values()));
			assertEquals(66, routes.size());
			for (PohTeleport teleport : routes)
			{
				PohTransport row = new PohTransport(anchor, teleport);
				String destination = TeleportationPortalPolicy.pohDestinationName(row);
				boolean jewellery = teleport instanceof JewelleryBox;
				boolean nexus = teleport instanceof net.runelite.client.plugins.microbot.util.poh.data.NexusPortal;
				int group = nexus ? InterfaceID.TELENEXUS_TELEPORT
					: jewellery ? InterfaceID.POH_JEWELLERY_BOX : InterfaceID.MENU;
				int child = jewellery || nexus ? 0 : 3;
				int liveId = jewellery ? JewelleryBoxType.ORNATE.getObjectId() : row.getObjectId();
				assertTrue(TeleportationPortalPolicy.isEligible(row));
				assertTrue(TeleportationPortalPolicy.isDirectPohObjectId(row.getObjectId()));
				PlannedEdge edge = new PlannedEdge(anchor, row.getDestination());
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));
				when(object.getId()).thenReturn(liveId);
				when(definition.getId()).thenReturn(liveId);
				when(definition.getActions()).thenReturn(new String[]{"Teleport menu", "Remove"});
				when(client.getWidget(group, child)).thenReturn(null);
				when(object.click("Teleport menu")).thenReturn(true);
				TeleportationPortal open = new Rs2TeleportationPortalScene().find(edge);
				assertNotNull(teleport.name(), open);
				assertEquals(TeleportationPortalPolicy.POH_OPEN_MENU_ACTION, open.getAction());
				assertTrue(Rs2TeleportationPortalScene.interactObject(edge,
					open.getAction(), row.getObjectId()));

				String displayedDestination = nexus ? "<col=ffffff>A</col> " + destination
					: jewellery ? "<col=ffffff>A.</col> " + destination : destination;
				Widget root = menu(displayedDestination, false);
				when(client.getWidget(group, child)).thenReturn(root);
				TeleportationPortal select = new Rs2TeleportationPortalScene().find(edge);
				assertNotNull(select);
				assertEquals(TeleportationPortalPolicy.destinationAction(destination),
					select.getAction());
				assertTrue(Rs2TeleportationPortalScene.interactObject(edge,
					select.getAction(), row.getObjectId()));
				if (nexus)
				{
					keyboard.verify(() -> net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard.typeString("A"));
					keyboard.clearInvocations();
					org.mockito.Mockito.verifyNoInteractions(mouse);
				}
				else verify(mouse).click(any(Rectangle.class));
				clearInvocations(mouse);
				assertFalse(Rs2TeleportationPortalScene.interactObject(edge,
					select.getAction(), -1));
				assertFalse(Rs2TeleportationPortalScene.interactObject(edge,
					TeleportationPortalPolicy.destinationAction("unrelated destination"), row.getObjectId()));
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of());
				assertFalse(Rs2TeleportationPortalScene.interactObject(edge,
					select.getAction(), row.getObjectId()));
				org.mockito.Mockito.verifyNoInteractions(mouse);
				keyboard.verifyNoInteractions();
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(anchor, Set.of(row)));

				Widget locked = menu(displayedDestination, true);
				when(client.getWidget(group, child)).thenReturn(locked);
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

				when(client.getWidget(group, child)).thenReturn(null);
				if (nexus)
				{
					Widget warning = menu("Enter Wilderness", false);
					when(warning.getBounds()).thenReturn(new Rectangle(10, 10, 20, 20));
					when(client.getWidget(475, 11)).thenReturn(warning);
					TeleportationPortal confirmation = new Rs2TeleportationPortalScene().find(edge);
					boolean wilderness = Set.of("ANNAKARL", "GHORROCK", "CARRALLANGER").contains(teleport.name());
					assertEquals(wilderness, confirmation.getAction().startsWith(TeleportationPortalPolicy.POH_CONFIRM_NEXUS_PREFIX));
					if (wilderness)
					{
						assertTrue(Rs2TeleportationPortalScene.interactObject(edge, confirmation.getAction(), row.getObjectId()));
						verify(mouse).click(any(Rectangle.class));
						clearInvocations(mouse);
					}
					when(client.getWidget(475, 11)).thenReturn(null);
					continue;
				}
				if (jewellery)
				{
					for (JewelleryBoxType tier : JewelleryBoxType.values())
					{
						when(object.getId()).thenReturn(tier.getObjectId());
						when(definition.getId()).thenReturn(tier.getObjectId());
						assertEquals(((JewelleryBox) teleport).isAvailableIn(tier),
							new Rs2TeleportationPortalScene().find(edge) != null);
						int first = tier == JewelleryBoxType.BASIC ? 37492
							: tier == JewelleryBoxType.FANCY ? 37501 : 37520;
						int last = tier == JewelleryBoxType.BASIC ? 37500
							: tier == JewelleryBoxType.FANCY ? 37519 : 37546;
						int colosseum = tier == JewelleryBoxType.BASIC ? 50710
							: tier == JewelleryBoxType.FANCY ? 50711 : 50712;
						for (int activeId : java.util.stream.IntStream.concat(
							java.util.stream.IntStream.rangeClosed(first, last),
							java.util.stream.IntStream.of(colosseum)).toArray())
						{
							when(definition.getId()).thenReturn(activeId);
							assertEquals(tier + ":" + activeId + ":" + teleport.name(),
								((JewelleryBox) teleport).isAvailableIn(tier),
								new Rs2TeleportationPortalScene().find(edge) != null);
						}
					}
					continue;
				}
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
