package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KaruulmAccessSceneTest
{
	@Test
	public void everyVariantDispatchesOnlyAfterItsCurrentRequirementsAreReady()
	{
		List<Transport> rows = Transport.loadAllFromResources().values().stream().flatMap(Set::stream)
			.filter(row -> KaruulmAccessPolicy.ownsObject(row.getObjectId())).collect(Collectors.toList());
		assertEquals(54, rows.size());
		Client client = mock(Client.class);
		Player actor = mock(Player.class);
		WorldView world = mock(WorldView.class);
		when(client.getLocalPlayer()).thenReturn(actor);
		when(client.getTopLevelWorldView()).thenReturn(world);
		when(actor.getWorldView()).thenReturn(world);
		when(client.getRealSkillLevel(any(Skill.class))).thenReturn(99);
		ClientThread thread = mock(ClientThread.class);
		when(thread.runOnClientThreadOptional(any())).thenAnswer(call -> Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
		when(thread.invoke(any(Supplier.class))).thenAnswer(call -> ((Supplier<?>) call.getArgument(0)).get());
		Widget widget = mock(Widget.class);
		when(client.getWidget(ComponentID.INVENTORY_CONTAINER)).thenReturn(widget);
		when(widget.getChildren()).thenReturn(new Widget[0]);
		Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
		when(object.getWorldView()).thenReturn(world);
		ObjectComposition definition = mock(ObjectComposition.class);
		when(object.getObjectComposition()).thenReturn(definition);
		Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
		when(cache.query()).thenCallRealMethod();
		when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
		try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
			MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
			MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
			MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
			MockedStatic<Rs2Equipment> equipment = mockStatic(Rs2Equipment.class);
			MockedStatic<Rs2Tab> tabs = mockStatic(Rs2Tab.class))
		{
			microbot.when(Microbot::getClient).thenReturn(client);
			microbot.when(Microbot::getClientThread).thenReturn(thread);
			microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
			Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
			for (Transport row : rows)
			{
				clearInvocations(object);
				inventory.clearInvocations();
				boolean boots = !row.getItemIdRequirements().isEmpty();
				boolean diary = !boots && !row.getVarbits().isEmpty();
				microbot.when(() -> Microbot.getVarbitValue(7932)).thenReturn(diary ? 1 : 0);
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(row.getOrigin(), Set.of(row)));
				player.when(Rs2Player::getWorldLocation).thenReturn(row.getOrigin());
				when(object.getId()).thenReturn(row.getObjectId());
				when(object.getWorldLocation()).thenReturn(row.getOrigin());
				when(definition.getName()).thenReturn(row.getName());
				when(definition.getActions()).thenReturn(new String[]{row.getAction()});
				when(object.click(row.getAction())).thenReturn(true);
				PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
				equipment.when(() -> Rs2Equipment.isWearing(any(int[].class))).thenReturn(false);
				if (boots)
				{
					int item = row.getItemIdRequirements().iterator().next().iterator().next();
					assertTrue(net.runelite.client.plugins.microbot.util.walker.banking.Rs2WalkerBankingPlanner.requiresBankPlanning(row));
					inventory.when(() -> Rs2Inventory.hasItem(item)).thenReturn(true);
					inventory.when(() -> Rs2Inventory.equip(item)).thenReturn(true);
					tabs.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(false);
					assertEquals(EquippedSafetyTransitionPolicy.OPEN_INVENTORY, scene.find(edge).getAction());
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.PREPARED,
						Rs2CatalogTransitionScene.dispatch(edge, EquippedSafetyTransitionPolicy.OPEN_INVENTORY, row.getObjectId()));
					tabs.when(() -> Rs2Tab.isCurrentTab(InterfaceTab.INVENTORY)).thenReturn(true);
					assertEquals(EquippedSafetyTransitionPolicy.equipAction(item), scene.find(edge).getAction());
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.PREPARED,
						Rs2CatalogTransitionScene.dispatch(edge, EquippedSafetyTransitionPolicy.equipAction(item), row.getObjectId()));
					inventory.verify(() -> Rs2Inventory.equip(item), times(1));
					verify(object, never()).click(anyString());
					equipment.when(() -> Rs2Equipment.isWearing(any(int[].class))).thenReturn(true);
				}
				assertEquals(row.getAction(), scene.find(edge).getAction());
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
					Rs2CatalogTransitionScene.dispatch(edge, row.getAction(), row.getObjectId()));
				if (boots || diary)
				{
					if (diary) microbot.when(() -> Microbot.getVarbitValue(7932)).thenReturn(0);
					else equipment.when(() -> Rs2Equipment.isWearing(any(int[].class))).thenReturn(false);
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
						Rs2CatalogTransitionScene.dispatch(edge, row.getAction(), row.getObjectId()));
				}
				verify(object, times(1)).click(row.getAction());
			}
		}
	}
}
