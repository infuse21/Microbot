package net.runelite.client.plugins.microbot.util.walker.transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectComposition;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.api.tileobject.Rs2TileObjectCache;
import net.runelite.client.plugins.microbot.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.microbot.shortestpath.Transport;
import net.runelite.client.plugins.microbot.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2PathApi;
import net.runelite.client.plugins.microbot.util.walker.obstacle.PlannedEdge;
import net.runelite.client.plugins.microbot.util.walker.obstacle.Rs2LiveScene;
import org.junit.Test;
import org.mockito.MockedStatic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrimhavenEntranceSceneTest
{
	@Test
	public void everyApproachUsesOnlyItsCurrentPaymentAndExactObjectState()
	{
		for (Transport row : BrimhavenDungeonEntranceSourceTest.rows())
		{
			Client client = mock(Client.class);
			net.runelite.api.Player localPlayer = mock(net.runelite.api.Player.class);
			net.runelite.api.WorldView world = mock(net.runelite.api.WorldView.class);
			when(client.getLocalPlayer()).thenReturn(localPlayer);
			when(localPlayer.getWorldView()).thenReturn(world);
			ClientThread thread = mock(ClientThread.class);
			when(thread.runOnClientThreadOptional(any())).thenAnswer(call ->
				Optional.ofNullable(((Callable<?>) call.getArgument(0)).call()));
			when(thread.invoke(any(java.util.function.Supplier.class))).thenAnswer(call ->
				((java.util.function.Supplier<?>) call.getArgument(0)).get());
			Rs2TileObjectModel object = mock(Rs2TileObjectModel.class);
			when(object.getWorldView()).thenReturn(world);
			ObjectComposition definition = mock(ObjectComposition.class);
			when(object.getId()).thenReturn(20877);
			when(object.getWorldLocation()).thenReturn(BrimhavenEntrancePolicy.ANCHOR);
			when(object.getObjectComposition()).thenReturn(definition);
			when(definition.getName()).thenReturn("Dungeon entrance");
			when(object.click(any(String.class))).thenReturn(true);
			Rs2TileObjectCache cache = mock(Rs2TileObjectCache.class);
			when(cache.query()).thenCallRealMethod();
			when(cache.getStream()).thenAnswer(call -> List.of(object).stream());
			Widget menu = mock(Widget.class);
			Widget title = mock(Widget.class);
			Widget yes = mock(Widget.class);
			Widget no = mock(Widget.class);
			when(client.getWidget(InterfaceID.DIALOG_OPTION, 1)).thenReturn(menu);
			when(menu.getDynamicChildren()).thenReturn(new Widget[]{title, yes, no});
			when(title.getText()).thenReturn("Pay 875 coins to enter?");
			when(yes.getText()).thenReturn("Yes");
			when(no.getText()).thenReturn("No");
			try (MockedStatic<Microbot> microbot = mockStatic(Microbot.class);
				MockedStatic<Rs2PathApi> path = mockStatic(Rs2PathApi.class);
				MockedStatic<Rs2Player> player = mockStatic(Rs2Player.class);
				MockedStatic<Rs2Inventory> inventory = mockStatic(Rs2Inventory.class);
				MockedStatic<Rs2Dialogue> dialogue = mockStatic(Rs2Dialogue.class);
				MockedStatic<Rs2LiveScene> live = mockStatic(Rs2LiveScene.class))
			{
				microbot.when(Microbot::getClient).thenReturn(client);
				microbot.when(Microbot::getClientThread).thenReturn(thread);
				microbot.when(Microbot::getRs2TileObjectCache).thenReturn(cache);
				path.when(Rs2PathApi::getTransports).thenReturn(Map.of(row.getOrigin(), Set.of(row)));
				player.when(Rs2Player::getWorldLocation).thenReturn(row.getOrigin());
				inventory.when(() -> Rs2Inventory.itemQuantity(ItemID.COINS)).thenReturn(875);
				PlannedEdge edge = new PlannedEdge(row.getOrigin(), row.getDestination());
				Rs2CatalogTransitionScene scene = new Rs2CatalogTransitionScene();
				when(definition.getActions()).thenReturn(new String[]{"Pay"});
				if (row.getCurrencyAmount() == 875)
				{
					assertEquals("Pay", scene.find(edge).getAction());
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
						Rs2CatalogTransitionScene.dispatch(edge, "Pay", 20877));
					dialogue.when(Rs2Dialogue::hasSelectAnOption).thenReturn(true);
					dialogue.when(Rs2Dialogue::getDialogueOptions).thenReturn(List.of(yes, no));
					dialogue.when(() -> Rs2Dialogue.keyPressForDialogueOption(1)).thenReturn(true);
					assertNull(scene.find(edge));
					assertEquals(BrimhavenEntrancePolicy.CONFIRM, scene.observe(edge, "Pay").getAction());
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
						Rs2CatalogTransitionScene.dispatch(edge, BrimhavenEntrancePolicy.CONFIRM, 20877));
					assertNull(scene.observe(edge, BrimhavenEntrancePolicy.CONFIRM));
					inventory.when(() -> Rs2Inventory.itemQuantity(ItemID.COINS)).thenReturn(874);
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
						Rs2CatalogTransitionScene.dispatch(edge, BrimhavenEntrancePolicy.CONFIRM, 20877));
					dialogue.verify(() -> Rs2Dialogue.keyPressForDialogueOption(1), times(1));
					dialogue.when(Rs2Dialogue::hasSelectAnOption).thenReturn(false);
				}
				else assertNull(scene.find(edge));
				inventory.when(() -> Rs2Inventory.itemQuantity(ItemID.COINS)).thenReturn(0);
				for (int state : new int[]{1, 8})
				{
					microbot.when(() -> Microbot.getVarbitValue(8123)).thenReturn(state);
					when(object.getId()).thenReturn(20876);
					when(definition.getActions()).thenReturn(new String[]{"Enter"});
					assertEquals("Enter", scene.observe(edge, BrimhavenEntrancePolicy.CONFIRM).getAction());
					assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
						Rs2CatalogTransitionScene.dispatch(edge, "Enter", 20877));
				}
				verify(object, times(2)).click("Enter");
				Widget speaker = mock(Widget.class);
				Widget speech = mock(Widget.class);
				when(client.getWidget(InterfaceID.DIALOG_NPC, 4)).thenReturn(speaker);
				when(client.getWidget(InterfaceID.DIALOG_NPC, 6)).thenReturn(speech);
				when(speaker.getText()).thenReturn("Saniboch");
				when(speech.getText()).thenReturn(
					"Many thanks. You may now pass the door. May your death be a glorious one!");
				dialogue.when(Rs2Dialogue::hasContinue).thenReturn(true);
				assertNull(scene.find(edge));
				assertEquals(BrimhavenEntrancePolicy.CONTINUE,
					scene.observe(edge, BrimhavenEntrancePolicy.CONFIRM).getAction());
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.ISSUED,
					Rs2CatalogTransitionScene.dispatch(edge, BrimhavenEntrancePolicy.CONTINUE, 20877));
				assertNull(scene.observe(edge, BrimhavenEntrancePolicy.CONTINUE));
				when(speaker.getText()).thenReturn("Other NPC");
				assertEquals(Rs2CatalogTransitionScene.DispatchResult.REJECTED,
					Rs2CatalogTransitionScene.dispatch(edge, BrimhavenEntrancePolicy.CONTINUE, 20877));
				dialogue.verify(Rs2Dialogue::clickContinue, times(1));
				dialogue.when(Rs2Dialogue::hasContinue).thenReturn(false);
				when(object.getId()).thenReturn(999);
				assertNull(scene.find(edge));
				when(object.getId()).thenReturn(20876);
				when(definition.getName()).thenReturn("Other entrance");
				assertNull(scene.find(edge));
				when(definition.getName()).thenReturn("Dungeon entrance");
				when(object.getWorldLocation()).thenReturn(row.getDestination());
				assertNull(scene.find(edge));
			}
		}
	}
}
